package pt.isel.gape.security.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.Session;
import pt.isel.gape.security.authorization.AuthorizationPolicy;

public final class SessionManager {

    static final String SESSION_USER_ATTRIBUTE = "gape.auth.user";
    static final String DATABASE_SESSION_ID_ATTRIBUTE = "gape.auth.sessionId";
    static final String DATABASE_SESSION_TOKEN_ATTRIBUTE = "gape.auth.sessionToken";
    static final String SESSION_USER_ID_ATTRIBUTE = "gape.auth.userId";
    static final String SESSION_USER_NAME_ATTRIBUTE = "gape.auth.userName";
    static final String SESSION_USER_EMAIL_ATTRIBUTE = "gape.auth.userEmail";
    static final String SESSION_USER_PHOTO_ATTRIBUTE = "gape.auth.userPhoto";
    static final String SESSION_USER_PROFILE_ATTRIBUTE = "gape.auth.userProfile";
    static final String SESSION_AUTHENTICATED_ATTRIBUTE = "gape.auth.authenticated";
    static final String CSRF_TOKEN_ATTRIBUTE = "gape.auth.csrfToken";
    static final String LOGOUT_CSRF_TOKEN_ATTRIBUTE = "gape.auth.logoutCsrfToken";
    static final String SESSION_PERMISSION_CODES_ATTRIBUTE = "gape.auth.permissions";
    static final String HAS_ADMINISTRATOR_PROFILE_ATTRIBUTE = "gape.auth.hasAdministratorProfile";
    static final String HAS_COORDINATOR_PROFILE_ATTRIBUTE = "gape.auth.hasCoordinatorProfile";
    static final String HAS_TEACHER_PROFILE_ATTRIBUTE = "gape.auth.hasTeacherProfile";
    static final String HAS_STUDENT_PROFILE_ATTRIBUTE = "gape.auth.hasStudentProfile";
    static final String CAN_VIEW_REPORTS_ATTRIBUTE = "gape.auth.canViewReports";
    static final String CAN_MANAGE_USERS_ATTRIBUTE = "gape.auth.canManageUsers";
    static final String CAN_MANAGE_PERMISSIONS_ATTRIBUTE = "gape.auth.canManagePermissions";
    static final String CAN_MANAGE_ORGANIZATIONS_ATTRIBUTE = "gape.auth.canManageOrganizations";
    static final String CAN_MANAGE_COURSES_ATTRIBUTE = "gape.auth.canManageCourses";
    static final String CAN_MANAGE_SUBJECTS_ATTRIBUTE = "gape.auth.canManageSubjects";
    static final String CAN_MANAGE_ENROLLMENTS_ATTRIBUTE = "gape.auth.canManageEnrollments";
    static final String CAN_MANAGE_SETTINGS_ATTRIBUTE = "gape.auth.canManageSettings";
    static final String CAN_VIEW_PERSONAL_DATA_ATTRIBUTE = "gape.auth.canViewPersonalData";
    static final String CAN_MANAGE_PERSONAL_DATA_ATTRIBUTE = "gape.auth.canManagePersonalData";
    static final String CAN_PROCESS_DELETION_REQUESTS_ATTRIBUTE = "gape.auth.canProcessDeletionRequests";

    private static final int HTTP_SESSION_TIMEOUT_SECONDS = (int) Duration.ofMinutes(35).toSeconds();
    private static final int CSRF_TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public void startAuthenticatedSession(HttpServletRequest request, SessionUser sessionUser, Session session) {
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(sessionUser, "sessionUser is required");
        Objects.requireNonNull(session, "session is required");

        clearSession(request);

        HttpSession httpSession = request.getSession(true);
        applySessionUserAttributes(httpSession, sessionUser);
        httpSession.setAttribute(DATABASE_SESSION_ID_ATTRIBUTE, session.id());
        httpSession.setAttribute(DATABASE_SESSION_TOKEN_ATTRIBUTE, session.token());
        String csrfToken = generateCsrfToken();
        httpSession.setAttribute(CSRF_TOKEN_ATTRIBUTE, csrfToken);
        httpSession.setAttribute(LOGOUT_CSRF_TOKEN_ATTRIBUTE, csrfToken);
        httpSession.setMaxInactiveInterval(HTTP_SESSION_TIMEOUT_SECONDS);
    }

    public void refreshAuthenticatedSession(HttpServletRequest request, SessionUser sessionUser) {
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(sessionUser, "sessionUser is required");

        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            applySessionUserAttributes(httpSession, sessionUser);
        }
    }

    public Optional<SessionUser> getSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        Object attribute = session.getAttribute(SESSION_USER_ATTRIBUTE);
        if (attribute instanceof SessionUser sessionUser) {
            return Optional.of(sessionUser);
        }
        return Optional.empty();
    }

    public OptionalLong getDatabaseSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return OptionalLong.empty();
        }

        Object attribute = session.getAttribute(DATABASE_SESSION_ID_ATTRIBUTE);
        if (attribute instanceof Long value) {
            return OptionalLong.of(value);
        }
        if (attribute instanceof Number number) {
            return OptionalLong.of(number.longValue());
        }
        return OptionalLong.empty();
    }

    public Optional<String> getDatabaseSessionToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        Object attribute = session.getAttribute(DATABASE_SESSION_TOKEN_ATTRIBUTE);
        return attribute instanceof String value ? Optional.of(value) : Optional.empty();
    }

    public boolean isValidLogoutCsrfToken(HttpServletRequest request, String submittedToken) {
        return isValidCsrfToken(request, submittedToken);
    }

    public String ensureCsrfToken(HttpServletRequest request) {
        Objects.requireNonNull(request, "request is required");
        HttpSession session = request.getSession(true);
        synchronized (session) {
            Object existing = session.getAttribute(CSRF_TOKEN_ATTRIBUTE);
            if (existing instanceof String token && !token.isBlank()) {
                return token;
            }
            String token = generateCsrfToken();
            session.setAttribute(CSRF_TOKEN_ATTRIBUTE, token);
            return token;
        }
    }

    public boolean isValidCsrfToken(HttpServletRequest request, String submittedToken) {
        if (submittedToken == null || submittedToken.isBlank()) {
            return false;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        Object attribute = session.getAttribute(CSRF_TOKEN_ATTRIBUTE);
        if (!(attribute instanceof String)) {
            attribute = session.getAttribute(LOGOUT_CSRF_TOKEN_ATTRIBUTE);
        }
        if (!(attribute instanceof String expectedToken)) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                submittedToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    public void clearSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private static void applySessionUserAttributes(HttpSession httpSession, SessionUser sessionUser) {
        httpSession.setAttribute(SESSION_USER_ATTRIBUTE, sessionUser);
        httpSession.setAttribute(SESSION_USER_ID_ATTRIBUTE, sessionUser.userId());
        httpSession.setAttribute(SESSION_USER_NAME_ATTRIBUTE, sessionUser.name());
        httpSession.setAttribute(SESSION_USER_EMAIL_ATTRIBUTE, sessionUser.email());
        if (sessionUser.hasPhoto()) {
            httpSession.setAttribute(SESSION_USER_PHOTO_ATTRIBUTE, sessionUser.photo());
        } else {
            httpSession.removeAttribute(SESSION_USER_PHOTO_ATTRIBUTE);
        }
        httpSession.setAttribute(SESSION_USER_PROFILE_ATTRIBUTE, resolveProfileLabel(sessionUser));
        httpSession.setAttribute(SESSION_AUTHENTICATED_ATTRIBUTE, Boolean.TRUE);
        httpSession.setAttribute(SESSION_PERMISSION_CODES_ATTRIBUTE, sessionUser.permissionCodes());
        httpSession.setAttribute(
                HAS_ADMINISTRATOR_PROFILE_ATTRIBUTE,
                sessionUser.profileTypes().contains(AccessProfileType.ADMINISTRATOR)
        );
        httpSession.setAttribute(
                HAS_COORDINATOR_PROFILE_ATTRIBUTE,
                sessionUser.profileTypes().contains(AccessProfileType.COORDINATOR)
        );
        httpSession.setAttribute(
                HAS_TEACHER_PROFILE_ATTRIBUTE,
                sessionUser.profileTypes().contains(AccessProfileType.TEACHER)
        );
        httpSession.setAttribute(
                HAS_STUDENT_PROFILE_ATTRIBUTE,
                sessionUser.profileTypes().contains(AccessProfileType.STUDENT)
        );
        boolean hasAdministratorProfile = sessionUser.profileTypes().contains(AccessProfileType.ADMINISTRATOR);
        boolean canManageAll = sessionUser.hasPermission(AuthorizationPolicy.MANAGE_ALL);
        boolean canManageOrganizationStructure = canManageAll
                || sessionUser.hasPermission(AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE);
        boolean canManageLearning = canManageAll
                || sessionUser.hasPermission(AuthorizationPolicy.MANAGE_LEARNING);
        boolean canManageEnrollments = canManageLearning
                || sessionUser.hasPermission(AuthorizationPolicy.MANAGE_ENROLLMENTS);

        httpSession.setAttribute(CAN_VIEW_REPORTS_ATTRIBUTE, Boolean.TRUE);
        httpSession.setAttribute(CAN_MANAGE_USERS_ATTRIBUTE, canManageAll);
        httpSession.setAttribute(
                CAN_MANAGE_PERMISSIONS_ATTRIBUTE,
                canManageAll
        );
        httpSession.setAttribute(
                CAN_MANAGE_ORGANIZATIONS_ATTRIBUTE,
                canManageOrganizationStructure
        );
        httpSession.setAttribute(CAN_MANAGE_COURSES_ATTRIBUTE, canManageAll);
        httpSession.setAttribute(CAN_MANAGE_SUBJECTS_ATTRIBUTE, canManageAll);
        httpSession.setAttribute(
                CAN_MANAGE_ENROLLMENTS_ATTRIBUTE,
                canManageEnrollments
        );
        httpSession.setAttribute(CAN_MANAGE_SETTINGS_ATTRIBUTE, canManageAll);
        httpSession.setAttribute(
                CAN_VIEW_PERSONAL_DATA_ATTRIBUTE,
                canManageAll
        );
        httpSession.setAttribute(
                CAN_MANAGE_PERSONAL_DATA_ATTRIBUTE,
                canManageAll || hasAdministratorProfile
        );
        httpSession.setAttribute(
                CAN_PROCESS_DELETION_REQUESTS_ATTRIBUTE,
                canManageAll
        );
    }

    private static String resolveProfileLabel(SessionUser sessionUser) {
        return sessionUser.primaryProfileType().map(profileType -> switch (profileType) {
            case ADMINISTRATOR -> "Administrator";
            case COORDINATOR -> "Coordinator";
            case TEACHER -> "Teacher";
            case STUDENT -> "Student";
        }).orElse("Authenticated user");
    }

    private static String generateCsrfToken() {
        byte[] token = new byte[CSRF_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }
}
