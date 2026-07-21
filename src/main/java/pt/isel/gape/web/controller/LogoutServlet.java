package pt.isel.gape.web.controller;

import java.io.IOException;
import java.util.Optional;
import java.util.OptionalLong;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.model.Session;
import pt.isel.gape.access.service.SessionService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.security.session.SessionManager;

@WebServlet(name = "logoutServlet", urlPatterns = "/auth/logout")
public final class LogoutServlet extends HttpServlet {

    private final SessionService sessionService;
    private final SessionManager sessionManager;

    public LogoutServlet() {
        this(
                new SessionService(ConnectionProvider.defaultProvider(), ApplicationClock.system()),
                new SessionManager()
        );
    }

    LogoutServlet(SessionService sessionService, SessionManager sessionManager) {
        this.sessionService = sessionService;
        this.sessionManager = sessionManager;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!sessionManager.isValidLogoutCsrfToken(request, request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        handleLogout(request, response);
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        OptionalLong sessionId = sessionManager.getDatabaseSessionId(request);
        Optional<String> sessionToken = sessionManager.getDatabaseSessionToken(request);

        if (sessionId.isPresent() && sessionToken.isPresent()) {
            Optional<Session> currentSession = sessionService.findById(sessionId.getAsLong());
            currentSession
                    .filter(session -> sessionService.matchesToken(session, sessionToken.get()))
                    .ifPresent(session -> sessionService.close(session, request.getRemoteAddr()));
        }

        sessionManager.clearSession(request);
        response.sendRedirect(request.getContextPath() + "/login.jsp?logout=1");
    }
}
