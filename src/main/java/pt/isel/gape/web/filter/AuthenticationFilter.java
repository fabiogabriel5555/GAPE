package pt.isel.gape.web.filter;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.model.Session;
import pt.isel.gape.access.service.SessionService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.security.session.SessionManager;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.security.authorization.AuthorizationPolicy;

public final class AuthenticationFilter implements Filter {

    private final SessionService sessionService;
    private final SessionManager sessionManager;

    public AuthenticationFilter() {
        this(
                new SessionService(ConnectionProvider.defaultProvider(), ApplicationClock.system()),
                new SessionManager()
        );
    }

    AuthenticationFilter(SessionService sessionService, SessionManager sessionManager) {
        this.sessionService = Objects.requireNonNull(sessionService, "sessionService is required");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager is required");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)
                || !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        String servletPath = normalizePath(httpRequest.getServletPath());
        if (!AuthorizationPolicy.isProtected(servletPath)) {
            chain.doFilter(request, response);
            return;
        }

        Optional<SessionUser> sessionUser = sessionManager.getSessionUser(httpRequest);
        OptionalLong sessionId = sessionManager.getDatabaseSessionId(httpRequest);
        Optional<String> sessionToken = sessionManager.getDatabaseSessionToken(httpRequest);

        if (sessionUser.isEmpty() || sessionId.isEmpty() || sessionToken.isEmpty()) {
            redirectToLogin(httpRequest, httpResponse, "required");
            return;
        }

        Optional<Session> persistedSession = sessionService.findById(sessionId.getAsLong());
        if (persistedSession.isEmpty()) {
            sessionManager.clearSession(httpRequest);
            redirectToLogin(httpRequest, httpResponse, "missing");
            return;
        }

        Session session = persistedSession.get();
        if (!sessionService.matchesToken(session, sessionToken.get()) || session.userId() != sessionUser.get().userId()) {
            sessionManager.clearSession(httpRequest);
            redirectToLogin(httpRequest, httpResponse, "invalid");
            return;
        }

        if (sessionService.isExpired(session)) {
            sessionService.expire(session, httpRequest.getRemoteAddr());
            sessionManager.clearSession(httpRequest);
            redirectToLogin(httpRequest, httpResponse, "expired");
            return;
        }

        sessionManager.refreshAuthenticatedSession(httpRequest, sessionUser.get());
        sessionService.registerActivity(session);
        chain.doFilter(request, response);
    }

    private static void redirectToLogin(HttpServletRequest request, HttpServletResponse response, String reason)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/login.jsp?auth=" + reason);
    }

    private static String normalizePath(String servletPath) {
        if (servletPath == null || servletPath.isBlank()) {
            return "/";
        }
        return servletPath.startsWith("/") ? servletPath : "/" + servletPath;
    }
}
