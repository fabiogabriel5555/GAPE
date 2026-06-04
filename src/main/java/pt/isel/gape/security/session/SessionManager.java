package pt.isel.gape.security.session;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.Session;

public final class SessionManager {

    static final String SESSION_USER_ATTRIBUTE = "gape.auth.user";
    static final String DATABASE_SESSION_ID_ATTRIBUTE = "gape.auth.sessionId";
    static final String DATABASE_SESSION_TOKEN_ATTRIBUTE = "gape.auth.sessionToken";
    static final String SESSION_USER_NAME_ATTRIBUTE = "gape.auth.userName";
    static final String SESSION_USER_EMAIL_ATTRIBUTE = "gape.auth.userEmail";
    static final String SESSION_USER_PHOTO_ATTRIBUTE = "gape.auth.userPhoto";
    static final String SESSION_USER_PROFILE_ATTRIBUTE = "gape.auth.userProfile";
    static final String SESSION_AUTHENTICATED_ATTRIBUTE = "gape.auth.authenticated";

    private static final int HTTP_SESSION_TIMEOUT_SECONDS = (int) Duration.ofMinutes(35).toSeconds();

    public void startAuthenticatedSession(HttpServletRequest request, SessionUser sessionUser, Session session) {
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(sessionUser, "sessionUser is required");
        Objects.requireNonNull(session, "session is required");

        clearSession(request);

        HttpSession httpSession = request.getSession(true);
        applySessionUserAttributes(httpSession, sessionUser);
        httpSession.setAttribute(DATABASE_SESSION_ID_ATTRIBUTE, session.id());
        httpSession.setAttribute(DATABASE_SESSION_TOKEN_ATTRIBUTE, session.token());
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

    public void clearSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private static void applySessionUserAttributes(HttpSession httpSession, SessionUser sessionUser) {
        httpSession.setAttribute(SESSION_USER_ATTRIBUTE, sessionUser);
        httpSession.setAttribute(SESSION_USER_NAME_ATTRIBUTE, sessionUser.name());
        httpSession.setAttribute(SESSION_USER_EMAIL_ATTRIBUTE, sessionUser.email());
        if (sessionUser.hasPhoto()) {
            httpSession.setAttribute(SESSION_USER_PHOTO_ATTRIBUTE, sessionUser.photo());
        } else {
            httpSession.removeAttribute(SESSION_USER_PHOTO_ATTRIBUTE);
        }
        httpSession.setAttribute(SESSION_USER_PROFILE_ATTRIBUTE, resolveProfileLabel(sessionUser));
        httpSession.setAttribute(SESSION_AUTHENTICATED_ATTRIBUTE, Boolean.TRUE);
    }

    private static String resolveProfileLabel(SessionUser sessionUser) {
        if (sessionUser.profileTypes().isEmpty()) {
            return "Authenticated user";
        }

        AccessProfileType firstProfile = sessionUser.profileTypes().iterator().next();
        return switch (firstProfile) {
            case ADMINISTRATOR -> "Administrator";
            case COORDINATOR -> "Coordinator";
            case TEACHER -> "Teacher";
            case STUDENT -> "Student";
        };
    }
}
