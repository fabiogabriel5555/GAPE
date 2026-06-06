package pt.isel.gape.security.auth;

import java.time.Clock;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.model.UserState;
import pt.isel.gape.access.service.SessionService;
import pt.isel.gape.access.service.UserService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.crypto.PasswordHasher;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.service.AuditService;

public final class AuthService {

    private final UserService userService;
    private final SessionService sessionService;
    private final PasswordHasher passwordHasher;
    private final AuditService auditService;
    private final PermissionDAO permissionDAO;

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
        this.userService = Objects.requireNonNull(userService, "userService is required");
        this.sessionService = Objects.requireNonNull(sessionService, "sessionService is required");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.permissionDAO = permissionDAO;
    }

    public AuthService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new UserService(connectionProvider),
                new SessionService(connectionProvider, clock),
                new PasswordHasher(),
                new AuditService(connectionProvider, clock),
                new PermissionDAO(connectionProvider)
        );
    }

    public AuthenticatedSession authenticate(String email, String password, String sourceIp) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw invalidCredentials(email, sourceIp);
        }

        User user = userService.findByEmail(email.trim())
                .orElseThrow(() -> invalidCredentials(email, sourceIp));

        if (user.state() == UserState.INACTIVE) {
            auditService.record(
                    user.id(),
                    null,
                    "LOGIN",
                    "user_account",
                    String.valueOf(user.id()),
                    "denied",
                    sourceIp
            );
            throw new AuthenticationException(AuthenticationFailureReason.USER_INACTIVE, "User account is inactive");
        }

        if (user.state() == UserState.BLOCKED) {
            auditService.record(
                    user.id(),
                    null,
                    "LOGIN",
                    "user_account",
                    String.valueOf(user.id()),
                    "denied",
                    sourceIp
            );
            throw new AuthenticationException(AuthenticationFailureReason.USER_BLOCKED, "User account is blocked");
        }

        if (!passwordHasher.matches(password, user.credentialHash(), user.credentialSalt())) {
            auditService.record(
                    user.id(),
                    null,
                    "LOGIN",
                    "user_account",
                    String.valueOf(user.id()),
                    "denied",
                    sourceIp
            );
            throw new AuthenticationException(AuthenticationFailureReason.INVALID_CREDENTIALS, "Invalid credentials");
        }

        SessionUser sessionUser = buildSessionUser(user);
        if (sessionUser.profileTypes().isEmpty()) {
            auditService.record(
                    user.id(),
                    null,
                    "LOGIN",
                    "user_account",
                    String.valueOf(user.id()),
                    "denied",
                    sourceIp
            );
            throw new AuthenticationException(AuthenticationFailureReason.USER_WITHOUT_PROFILE, "User has no access profile");
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

    private AuthenticationException invalidCredentials(String email, String sourceIp) {
        auditService.record(
                null,
                null,
                "LOGIN",
                "user_account",
                email == null || email.isBlank() ? "unknown" : email.trim(),
                "denied",
                sourceIp
        );
        return new AuthenticationException(AuthenticationFailureReason.INVALID_CREDENTIALS, "Invalid credentials");
    }

    public record AuthenticatedSession(
            User user,
            pt.isel.gape.access.model.Session session,
            SessionUser sessionUser
    ) {
    }
}
