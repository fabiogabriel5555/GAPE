package pt.isel.gape.access.service;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

import pt.isel.gape.access.dao.SessionDAO;
import pt.isel.gape.access.model.Session;
import pt.isel.gape.access.model.SessionState;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.crypto.SessionTokenHasher;
import pt.isel.gape.transversal.service.AuditService;

public final class SessionService {

    public static final Duration INACTIVITY_TIMEOUT = Duration.ofMinutes(30);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SessionDAO sessionDAO;
    private final AuditService auditService;
    private final Clock clock;
    private final SessionTokenHasher sessionTokenHasher;

    public SessionService(SessionDAO sessionDAO, AuditService auditService, Clock clock) {
        this(sessionDAO, auditService, clock, new SessionTokenHasher());
    }

    SessionService(
            SessionDAO sessionDAO,
            AuditService auditService,
            Clock clock,
            SessionTokenHasher sessionTokenHasher
    ) {
        this.sessionDAO = Objects.requireNonNull(sessionDAO, "sessionDAO is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
        this.sessionTokenHasher = Objects.requireNonNull(sessionTokenHasher, "sessionTokenHasher is required");
    }

    public SessionService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new SessionDAO(connectionProvider),
                new AuditService(connectionProvider, clock),
                clock
        );
    }

    public Session createSession(long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        String token = generateToken();
        try {
            Session persisted = sessionDAO.create(userId, sessionTokenHasher.hash(token), now, now);
            return withBearerToken(persisted, token);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create session for user " + userId, exception);
        }
    }

    public Optional<Session> findById(long sessionId) {
        try {
            return sessionDAO.findById(sessionId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load session " + sessionId, exception);
        }
    }

    public Optional<Session> findByToken(String token) {
        try {
            Optional<Session> hashedSession = sessionDAO.findByStoredToken(sessionTokenHasher.hash(token));
            if (hashedSession.isPresent()) {
                return hashedSession.map(session -> withBearerToken(session, token));
            }
            // Temporary compatibility with sessions that were persisted before
            // V032/runtime migration. A successful lookup still returns the
            // caller's bearer token, never the stored representation.
            return sessionDAO.findByStoredToken(token).map(session -> withBearerToken(session, token));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load session by token", exception);
        }
    }

    /**
     * Verifies a browser-held bearer token against the one-way database
     * representation. Existing raw tokens are accepted only until the startup
     * data migration replaces them.
     */
    public boolean matchesToken(Session session, String bearerToken) {
        Objects.requireNonNull(session, "session is required");
        return sessionTokenHasher.matches(session.token(), bearerToken);
    }

    public boolean isExpired(Session session) {
        if (session.state() != SessionState.ACTIVE) {
            return session.state() == SessionState.EXPIRED || session.state() == SessionState.CLOSED;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        return !session.lastActivity().plus(INACTIVITY_TIMEOUT).isAfter(now);
    }

    public Session registerActivity(Session session) {
        ensureSessionCanBeUpdated(session);

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime effectiveActivity = max(session.startAt(), now);
        try {
            return sessionDAO.updateLastActivity(session.id(), effectiveActivity);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update last_activity for session " + session.id(), exception);
        }
    }

    public Session expire(Session session, String sourceIp) {
        if (session.state() != SessionState.ACTIVE) {
            return session;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime effectiveEnd = max(session.startAt(), now);
        try {
            Session expired = sessionDAO.updateState(session.id(), SessionState.EXPIRED, session.lastActivity(), effectiveEnd);
            auditService.record(
                    session.userId(),
                    session.id(),
                    "SESSION_EXPIRED",
                    "user_session",
                    String.valueOf(session.id()),
                    "success",
                    sourceIp
            );
            return expired;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to expire session " + session.id(), exception);
        }
    }

    public Session close(Session session, String sourceIp) {
        if (session.state() == SessionState.CLOSED) {
            return session;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime effectiveTime = max(session.startAt(), now);
        try {
            Session closed = sessionDAO.updateState(session.id(), SessionState.CLOSED, effectiveTime, effectiveTime);
            auditService.record(
                    session.userId(),
                    session.id(),
                    "LOGOUT",
                    "user_session",
                    String.valueOf(session.id()),
                    "success",
                    sourceIp
            );
            return closed;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to close session " + session.id(), exception);
        }
    }

    private void ensureSessionCanBeUpdated(Session session) {
        if (session.state() != SessionState.ACTIVE) {
            throw new IllegalStateException("Only active sessions can be updated");
        }
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static Session withBearerToken(Session persisted, String bearerToken) {
        return new Session(
                persisted.id(),
                persisted.userId(),
                bearerToken,
                persisted.state(),
                persisted.startAt(),
                persisted.lastActivity(),
                persisted.endAt()
        );
    }

    private static LocalDateTime max(LocalDateTime left, LocalDateTime right) {
        return left.isAfter(right) ? left : right;
    }
}
