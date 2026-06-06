package pt.isel.gape.web.controller;

import java.io.IOException;
import java.time.Clock;
import java.util.Optional;
import java.util.OptionalLong;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.model.Session;
import pt.isel.gape.access.service.SessionService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.session.SessionManager;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.web.navigation.DashboardNavigation;

@WebServlet(name = "profileServlet", urlPatterns = "/profile")
public final class ProfileServlet extends HttpServlet {

    private final SessionService sessionService;
    private final SessionManager sessionManager;

    public ProfileServlet() {
        this(
                new SessionService(ConnectionProvider.defaultProvider(), Clock.systemUTC()),
                new SessionManager()
        );
    }

    ProfileServlet(SessionService sessionService, SessionManager sessionManager) {
        this.sessionService = sessionService;
        this.sessionManager = sessionManager;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<SessionUser> sessionUser = sessionManager.getSessionUser(request);
        OptionalLong sessionId = sessionManager.getDatabaseSessionId(request);
        Optional<String> sessionToken = sessionManager.getDatabaseSessionToken(request);

        if (sessionUser.isEmpty() || sessionId.isEmpty() || sessionToken.isEmpty()) {
            redirectToLogin(request, response, "required");
            return;
        }

        Optional<Session> persistedSession = sessionService.findById(sessionId.getAsLong());
        if (persistedSession.isEmpty()) {
            sessionManager.clearSession(request);
            redirectToLogin(request, response, "missing");
            return;
        }

        Session session = persistedSession.get();
        if (!session.token().equals(sessionToken.get()) || session.userId() != sessionUser.get().userId()) {
            sessionManager.clearSession(request);
            redirectToLogin(request, response, "invalid");
            return;
        }

        if (sessionService.isExpired(session)) {
            sessionService.expire(session, request.getRemoteAddr());
            sessionManager.clearSession(request);
            redirectToLogin(request, response, "expired");
            return;
        }

        response.sendRedirect(request.getContextPath() + resolveProfilePage(sessionUser.get()));
    }

    private static String resolveProfilePage(SessionUser sessionUser) {
        return DashboardNavigation.profilePageFor(sessionUser).orElse("/dashboard");
    }

    private static void redirectToLogin(HttpServletRequest request, HttpServletResponse response, String reason)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/login.jsp?auth=" + reason);
    }
}
