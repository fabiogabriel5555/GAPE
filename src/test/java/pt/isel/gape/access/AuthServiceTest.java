package pt.isel.gape.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pt.isel.gape.common.config.DatabaseBootstrapMode;
import pt.isel.gape.common.config.DatabaseBootstrapService;
import pt.isel.gape.access.dao.SessionDAO;
import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.access.model.Session;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.service.SessionService;
import pt.isel.gape.access.service.UserService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.auth.AuthService;
import pt.isel.gape.security.auth.AuthenticationException;
import pt.isel.gape.security.auth.AuthenticationFailureReason;
import pt.isel.gape.security.crypto.PasswordHasher;
import pt.isel.gape.transversal.DatabaseTestSupport;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

class AuthServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private ConnectionProvider connectionProvider;
    private PasswordHasher passwordHasher;
    private AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        connectionProvider = DatabaseTestSupport::openConnection;
        passwordHasher = new PasswordHasher();

        try (Connection connection = DatabaseTestSupport.openConnection()) {
            DatabaseTestSupport.resetDatabase(connection);
        }

        AuditService auditService = new AuditService(new ActivityLogDAO(connectionProvider), FIXED_CLOCK);
        authService = new AuthService(
                new UserService(new UserDAO(connectionProvider)),
                new SessionService(new SessionDAO(connectionProvider), auditService, FIXED_CLOCK),
                passwordHasher,
                auditService
        );
    }

    @Test
    void validLoginCreatesSessionAndAuditRecord() throws Exception {
        insertUserWithPassword(200, "Valid User", "valid@gape.local", "active", "Password#2026");
        insertStudentProfile(200, "STD-200");

        AuthService.AuthenticatedSession authenticatedSession =
                authService.authenticate("valid@gape.local", "Password#2026", "127.0.0.1");

        assertEquals(200L, authenticatedSession.user().id());
        assertEquals(200L, authenticatedSession.session().userId());
        assertNotNull(authenticatedSession.session().token());
        assertTrue(authenticatedSession.sessionUser().profileTypes().contains(AccessProfileType.STUDENT));

        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM activity_log WHERE operation_type = 'LOGIN' AND outcome = 'success' AND id_user = ?"
             )) {
            statement.setLong(1, 200L);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                assertEquals(1, resultSet.getInt(1));
            }
        }
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        insertUserWithPassword(201, "Wrong Password", "wrong@gape.local", "active", "Correct#2026");

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authService.authenticate("wrong@gape.local", "Incorrect#2026", "127.0.0.1")
        );

        assertEquals(AuthenticationFailureReason.INVALID_CREDENTIALS, exception.reason());
    }

    @Test
    void activeUserWithoutProfileIsRejectedWithoutCreatingSession() throws Exception {
        insertUserWithPassword(204, "No Profile User", "noprofile@gape.local", "active", "Password#2026");

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authService.authenticate("noprofile@gape.local", "Password#2026", "127.0.0.1")
        );

        assertEquals(AuthenticationFailureReason.USER_WITHOUT_PROFILE, exception.reason());

        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM user_session WHERE id_user = ?"
             )) {
            statement.setLong(1, 204L);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                assertEquals(0, resultSet.getInt(1));
            }
        }
    }

    @Test
    void missingEmailIsRejectedAndDoesNotAuditSubmittedEmail() throws Exception {
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authService.authenticate("missing@gape.local", "Any#2026", "127.0.0.1")
        );

        assertEquals(AuthenticationFailureReason.INVALID_CREDENTIALS, exception.reason());

        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT affected_entity_identifier
                     FROM activity_log
                     WHERE operation_type = 'LOGIN' AND outcome = 'denied'
                     """)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                assertEquals("unknown", resultSet.getString(1));
            }
        }
    }

    @Test
    void inactiveOrBlockedUsersAreRejected() throws Exception {
        insertUserWithPassword(202, "Inactive User", "inactive2@gape.local", "inactive", "Password#2026");
        insertUserWithPassword(203, "Blocked User", "blocked@gape.local", "blocked", "Password#2026");

        AuthenticationException inactive = assertThrows(
                AuthenticationException.class,
                () -> authService.authenticate("inactive2@gape.local", "Password#2026", "127.0.0.1")
        );
        AuthenticationException blocked = assertThrows(
                AuthenticationException.class,
                () -> authService.authenticate("blocked@gape.local", "Password#2026", "127.0.0.1")
        );

        assertEquals(AuthenticationFailureReason.USER_INACTIVE, inactive.reason());
        assertEquals(AuthenticationFailureReason.USER_BLOCKED, blocked.reason());
    }

    @Test
    void demoSeedUsersAuthenticateWithValidHashes() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            new DatabaseBootstrapService().initialize(connection, DatabaseBootstrapMode.FULL);
        }

        AuthService.AuthenticatedSession admin =
                authService.authenticate("admin@gape.local", "Password#2026", "127.0.0.1");
        AuthService.AuthenticatedSession coordinator =
                authService.authenticate("coord@gape.local", "Password#2026", "127.0.0.1");
        AuthService.AuthenticatedSession teacher =
                authService.authenticate("teacher@gape.local", "Password#2026", "127.0.0.1");
        AuthService.AuthenticatedSession student =
                authService.authenticate("student@gape.local", "Password#2026", "127.0.0.1");

        assertTrue(admin.sessionUser().profileTypes().contains(AccessProfileType.ADMINISTRATOR));
        assertTrue(coordinator.sessionUser().profileTypes().contains(AccessProfileType.COORDINATOR));
        assertTrue(teacher.sessionUser().profileTypes().contains(AccessProfileType.TEACHER));
        assertTrue(student.sessionUser().profileTypes().contains(AccessProfileType.STUDENT));

        AuthenticationException inactive = assertThrows(
                AuthenticationException.class,
                () -> authService.authenticate("inactive@gape.local", "Password#2026", "127.0.0.1")
        );
        assertEquals(AuthenticationFailureReason.USER_INACTIVE, inactive.reason());
    }

    private void insertUserWithPassword(long userId, String name, String email, String state, String password) throws Exception {
        PasswordHasher.PasswordHash passwordHash = passwordHasher.createHash(password);

        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO user_account (
                         id_user, name, email, state, language, created_at, credential_hash, credential_salt
                     ) VALUES (?, ?, ?, ?, 'pt-PT', '2026-06-01 10:00:00', ?, ?)
                     """)) {
            statement.setLong(1, userId);
            statement.setString(2, name);
            statement.setString(3, email);
            statement.setString(4, state);
            statement.setString(5, passwordHash.hashBase64());
            statement.setString(6, passwordHash.saltBase64());
            statement.executeUpdate();
        }
    }

    private void insertStudentProfile(long userId, String code) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO student_profile (id_user, cod_student) VALUES (?, ?)"
             )) {
            statement.setLong(1, userId);
            statement.setString(2, code);
            statement.executeUpdate();
        }
    }
}
