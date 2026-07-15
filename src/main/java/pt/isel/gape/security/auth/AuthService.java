package pt.isel.gape.security.auth;

import java.time.Clock;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.model.UserState;
import pt.isel.gape.access.service.SessionService;
import pt.isel.gape.access.service.UserService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.security.crypto.PasswordHasher;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.service.AuditService;

public final class AuthService {

    private static final String DUMMY_CREDENTIAL_HASH = "ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=";
    private static final String DUMMY_CREDENTIAL_SALT = "8scgIe5H/ymYYNE9mx/Zzw==";

    private final UserService userService;
    private final SessionService sessionService;
    private final PasswordHasher passwordHasher;
    private final AuditService auditService;
    private final PermissionDAO permissionDAO;
    private final LoginAttemptLimiter loginAttemptLimiter;

    public AuthService(
            UserService userService,
            SessionService sessionService,
            PasswordHasher passwordHasher,
            AuditService auditService
    ) {
        this(userService, sessionService, passwordHasher, auditService, null);
    }

    public AuthService(
            UserService userService,
            SessionService sessionService,
            PasswordHasher passwordHasher,
            AuditService auditService,
            PermissionDAO permissionDAO
    ) {
        this(
                userService,
                sessionService,
                passwordHasher,
                auditService,
                permissionDAO,
                new LoginAttemptLimiter(ApplicationClock.system())
        );
    }

    AuthService(
            UserService userService,
            SessionService sessionService,
            PasswordHasher passwordHasher,
            AuditService auditService,
            PermissionDAO permissionDAO,
            LoginAttemptLimiter loginAttemptLimiter
    ) {
        this.userService = Objects.requireNonNull(userService, "userService is required");
        this.sessionService = Objects.requireNonNull(sessionService, "sessionService is required");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.permissionDAO = permissionDAO;
        this.loginAttemptLimiter = Objects.requireNonNull(loginAttemptLimiter, "loginAttemptLimiter is required");
    }

    public AuthService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new UserService(connectionProvider),
                new SessionService(connectionProvider, clock),
                new PasswordHasher(),
                new AuditService(connectionProvider, clock),
                new PermissionDAO(connectionProvider),
                new LoginAttemptLimiter(clock)
        );
    }

    public AuthenticatedSession authenticate(String email, String password, String sourceIp) {
        String normalizedEmail = normalizeEmail(email);
        loginAttemptLimiter.requireAllowed(normalizedEmail, sourceIp);

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            performDummyCredentialCheck(password);
            throw deniedCredentials(Optional.empty(), normalizedEmail, sourceIp);
        }

        Optional<User> userCandidate = userService.findByEmail(normalizedEmail);
        boolean passwordMatches = userCandidate
                .map(user -> passwordHasher.matches(password, user.credentialHash(), user.credentialSalt()))
                .orElseGet(() -> performDummyCredentialCheck(password));
        if (!passwordMatches || userCandidate.isEmpty()) {
            throw deniedCredentials(userCandidate, normalizedEmail, sourceIp);
        }

        User user = userCandidate.orElseThrow();

        if (user.state() != UserState.ACTIVE) {
            throw deniedCredentials(userCandidate, normalizedEmail, sourceIp);
        }

        SessionUser sessionUser = buildSessionUser(user);
        if (sessionUser.profileTypes().isEmpty()) {
            throw deniedCredentials(userCandidate, normalizedEmail, sourceIp);
        }

        pt.isel.gape.access.model.Session session = sessionService.createSession(user.id());
        auditService.record(
                user.id(),
                session.id(),
                "LOGIN",
                "user_session",
                String.valueOf(session.id()),
                "success",
                sourceIp
        );
        loginAttemptLimiter.recordSuccess(normalizedEmail, sourceIp);

        return new AuthenticatedSession(user, session, sessionUser);
    }

    private SessionUser buildSessionUser(User user) {
        SessionUser baseSessionUser = SessionUser.fromUser(user);
        if (permissionDAO == null || baseSessionUser.profileTypes().isEmpty()) {
            return baseSessionUser;
        }

        Map<AccessProfileType, Set<String>> permissionCodesByProfile = new EnumMap<>(AccessProfileType.class);
        try {
            for (AccessProfileType profileType : baseSessionUser.profileTypes()) {
                permissionCodesByProfile.put(
                        profileType,
                        permissionDAO.findActivePermissionCodes(user.id(), profileType)
                );
            }
        } catch (SQLException exception) {
            throw new AuthenticationException(
                    AuthenticationFailureReason.INVALID_CREDENTIALS,
                    "Could not load user permissions",
                    exception
            );
        }
        return new SessionUser(
                user.id(),
                user.name(),
                user.email(),
                user.photo(),
                baseSessionUser.profileTypes(),
                permissionCodesByProfile
        );
    }

    private AuthenticationException deniedCredentials(
            Optional<User> user,
            String normalizedEmail,
            String sourceIp
    ) {
        loginAttemptLimiter.recordFailure(normalizedEmail, sourceIp);
        auditService.record(
                user.map(User::id).orElse(null),
                null,
                "LOGIN",
                "user_account",
                user.map(value -> String.valueOf(value.id())).orElse("unknown"),
                "denied",
                sourceIp
        );
        return new AuthenticationException(AuthenticationFailureReason.INVALID_CREDENTIALS, "Invalid credentials");
    }

    private boolean performDummyCredentialCheck(String password) {
        return passwordHasher.matches(
                password == null ? "" : password,
                DUMMY_CREDENTIAL_HASH,
                DUMMY_CREDENTIAL_SALT
        );
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public record AuthenticatedSession(
            User user,
            pt.isel.gape.access.model.Session session,
            SessionUser sessionUser
    ) {
    }
}
