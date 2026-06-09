package pt.isel.gape.web.controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.security.auth.AuthService;
import pt.isel.gape.security.auth.AuthenticationException;
import pt.isel.gape.security.auth.AuthenticationFailureReason;
import pt.isel.gape.security.session.SessionManager;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.web.navigation.DashboardNavigation;

@WebServlet(name = "loginServlet", urlPatterns = "/auth/login")
public final class LoginServlet extends HttpServlet {

    private final AuthService authService;
    private final SessionManager sessionManager;

    public LoginServlet() {
        this(
                new AuthService(ConnectionProvider.defaultProvider(), ApplicationClock.system()),
                new SessionManager()
        );
    }

    LoginServlet(AuthService authService, SessionManager sessionManager) {
        this.authService = authService;
        this.sessionManager = sessionManager;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String sourceIp = request.getRemoteAddr();

        try {
            AuthService.AuthenticatedSession authenticatedSession = authService.authenticate(email, password, sourceIp);
            sessionManager.startAuthenticatedSession(
                    request,
                    authenticatedSession.sessionUser(),
                    authenticatedSession.session()
            );
            response.sendRedirect(request.getContextPath() + resolveLandingPage(authenticatedSession.sessionUser()));
        } catch (AuthenticationException exception) {
            request.getSession(true).setAttribute("authError", mapFailure(exception.reason()));
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=" + exception.reason().name().toLowerCase());
        }
    }

    private static String resolveLandingPage(SessionUser sessionUser) {
        return DashboardNavigation.landingPageFor(sessionUser).orElse("/login.jsp?auth=required");
    }

    private static String mapFailure(AuthenticationFailureReason reason) {
        return switch (reason) {
            case INVALID_CREDENTIALS -> "Invalid credentials.";
            case USER_INACTIVE -> "The account is inactive.";
            case USER_BLOCKED -> "The account is blocked.";
            case USER_WITHOUT_PROFILE -> "The account has no access profile assigned.";
        };
    }
}
