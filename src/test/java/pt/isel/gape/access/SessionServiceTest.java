package pt.isel.gape.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pt.isel.gape.access.dao.SessionDAO;
import pt.isel.gape.access.model.Session;
import pt.isel.gape.access.model.SessionState;
import pt.isel.gape.access.service.SessionService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.transversal.DatabaseTestSupport;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

class SessionServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private ConnectionProvider connectionProvider;
    private SessionService sessionService;

    @BeforeEach
    void setUp() throws Exception {
        connectionProvider = DatabaseTestSupport::openConnection;

        try (Connection connection = DatabaseTestSupport.openConnection()) {
            DatabaseTestSupport.resetDatabase(connection);
            insertUser(connection, 300L, "session-user@gape.local");
        }

        sessionService = new SessionService(
                new SessionDAO(connectionProvider),
                new AuditService(new ActivityLogDAO(connectionProvider), FIXED_CLOCK),
                FIXED_CLOCK
        );
    }

    @Test
    void createsSessionAndUpdatesLastActivity() {
        Session created = sessionService.createSession(300L);
        Session touched = sessionService.registerActivity(created);

        assertEquals(SessionState.ACTIVE, created.state());
        assertEquals(SessionState.ACTIVE, touched.state());
        assertEquals(created.id(), touched.id());
        assertTrue(!touched.lastActivity().isBefore(created.startAt()));
    }

    @Test
    void recentSessionRemainsValidUnderRestriction18() throws Exception {
        long sessionId = insertSession(300L, "tok-recent", "2026-06-04 10:00:00", "2026-06-04 10:05:00", "active", null);
        Session session = sessionService.findById(sessionId).orElseThrow();

        assertFalse(sessionService.isExpired(session));
    }

    @Test
    void expiresSessionAfterThirtyMinutesOfInactivity() throws Exception {
        long sessionId = insertSession(300L, "tok-old", "2026-06-04 08:00:00", "2026-06-04 08:30:00", "active", null);
        Session session = sessionService.findById(sessionId).orElseThrow();

        assertTrue(sessionService.isExpired(session));

        Session expired = sessionService.expire(session, "127.0.0.1");
        assertEquals(SessionState.EXPIRED, expired.state());
        assertEquals(session.lastActivity(), expired.lastActivity());
        assertTrue(expired.endAt() != null);

        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM activity_log WHERE operation_type = 'SESSION_EXPIRED' AND id_session = ?"
             )) {
            statement.setLong(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                assertEquals(1, resultSet.getInt(1));
            }
        }
    }

    @Test
    void expirationBoundaryFollowsThirtyMinuteInactivityPolicy() throws Exception {
        LocalDateTime startAt = LocalDateTime.parse("2026-06-04T09:40:00");

        assertFalse(sessionService.isExpired(boundarySession(1L, startAt, "2026-06-04T09:45:31")));
        assertTrue(sessionService.isExpired(boundarySession(2L, startAt, "2026-06-04T09:45:30")));
        assertTrue(sessionService.isExpired(boundarySession(3L, startAt, "2026-06-04T09:45:29")));
    }

    @Test
    void logoutClosesSessionAndRegistersAudit() throws Exception {
        long sessionId = insertSession(300L, "tok-close", "2026-06-04 10:00:00", "2026-06-04 10:10:00", "active", null);
        Session session = sessionService.findById(sessionId).orElseThrow();

        Session closed = sessionService.close(session, "127.0.0.1");

        assertEquals(SessionState.CLOSED, closed.state());
        assertEquals(closed.endAt(), closed.lastActivity());

        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM activity_log WHERE operation_type = 'LOGOUT' AND id_session = ?"
             )) {
            statement.setLong(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                assertEquals(1, resultSet.getInt(1));
            }
        }
    }

    private void insertUser(Connection connection, long userId, String email) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO user_account (
                    id_user, name, email, state, language, created_at, credential_hash, credential_salt
                ) VALUES (?, 'Session User', ?, 'active', 'pt-PT', '2026-06-01 10:00:00', 'hash', 'salt')
                """)) {
            statement.setLong(1, userId);
            statement.setString(2, email);
            statement.executeUpdate();
        }
    }

    private long insertSession(
            long userId,
            String token,
            String startAt,
            String lastActivity,
            String state,
            String endAt
    ) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO user_session (id_user, token, state, start_at, last_activity, end_at)
                     VALUES (?, ?, ?, ?, ?, ?)
                     """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, userId);
            statement.setString(2, token);
            statement.setString(3, state);
            statement.setString(4, startAt);
            statement.setString(5, lastActivity);
            if (endAt == null) {
                statement.setNull(6, java.sql.Types.TIMESTAMP);
            } else {
                statement.setString(6, endAt);
            }
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private Session boundarySession(long id, LocalDateTime startAt, String lastActivity) {
        return new Session(
                id,
                300L,
                "tok-boundary-" + id,
                SessionState.ACTIVE,
                startAt,
                LocalDateTime.parse(lastActivity),
                null
        );
    }
}
