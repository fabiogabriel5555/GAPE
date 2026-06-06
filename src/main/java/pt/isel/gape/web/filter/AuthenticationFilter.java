package pt.isel.gape.web.filter;

import java.io.IOException;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.Session;
import pt.isel.gape.access.service.SessionService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.session.SessionManager;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.web.navigation.DashboardNavigation;

@WebFilter(filterName = "authenticationFilter", urlPatterns = "*.jsp")
public final class AuthenticationFilter implements Filter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/",
            "/index.jsp",
            "/login.jsp",
            "/sign-in.jsp",
            "/sign-up.jsp",
            "/contact.jsp",
            "/courses.jsp",
            "/course.jsp",
            "/course-details.jsp",
            "/about-four.jsp",
            "/instructor/instructor.jsp",
            "/instructor/instructor-details.jsp",
            "/tutor.jsp",
            "/tutor-details.jsp",
            "/events.jsp",
            "/event-details.jsp",
            "/apply-admission.jsp",
            "/privacy-policy.jsp",
            "/error-404.jsp",
            "/error-500.jsp"
    );

    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/content.jsp",
            "/messages.jsp",
            "/forms.jsp",
            "/tables.jsp",
            "/profile.jsp",
            "/my-propyl.jsp",
            "/dashbord.jsp",
            "/lesson-details.jsp"
    );

    private final SessionService sessionService;
    private final SessionManager sessionManager;

    public AuthenticationFilter() {
        this(
                new SessionService(ConnectionProvider.defaultProvider(), Clock.systemUTC()),
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
        if (!isProtected(servletPath)) {
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
        if (!session.token().equals(sessionToken.get()) || session.userId() != sessionUser.get().userId()) {
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

        if (!isAuthorized(sessionUser.get(), servletPath)) {
            redirectToAllowedDashboard(httpRequest, httpResponse, sessionUser.get());
            return;
        }

        sessionManager.refreshAuthenticatedSession(httpRequest, sessionUser.get());
        sessionService.registerActivity(session);
        chain.doFilter(request, response);
    }

    private static boolean isProtected(String servletPath) {
        if (PUBLIC_PATHS.contains(servletPath)) {
            return false;
        }
        if (servletPath.startsWith("/admin/")
                || servletPath.startsWith("/coordinator/")
                || servletPath.startsWith("/instructor/")
                || servletPath.startsWith("/student/")) {
            return true;
        }
        return PROTECTED_PATHS.contains(servletPath);
    }

    private static boolean isAuthorized(SessionUser sessionUser, String servletPath) {
        Optional<AccessProfileType> requiredProfile = requiredProfileFor(servletPath);
        return requiredProfile.isEmpty() || sessionUser.profileTypes().contains(requiredProfile.get());
    }

    private static Optional<AccessProfileType> requiredProfileFor(String servletPath) {
        if (servletPath.startsWith("/admin/")) {
            return Optional.of(AccessProfileType.ADMINISTRATOR);
        }
        if (servletPath.startsWith("/coordinator/")) {
            return Optional.of(AccessProfileType.COORDINATOR);
        }
        if (servletPath.startsWith("/instructor/")) {
            return Optional.of(AccessProfileType.TEACHER);
        }
        if (servletPath.startsWith("/student/")) {
            return Optional.of(AccessProfileType.STUDENT);
        }
        return Optional.empty();
    }

    private static void redirectToLogin(HttpServletRequest request, HttpServletResponse response, String reason)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/login.jsp?auth=" + reason);
    }

    private static void redirectToAllowedDashboard(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser sessionUser
    ) throws IOException {
        Optional<String> landingPage = DashboardNavigation.landingPageFor(sessionUser);
        if (landingPage.isPresent()) {
            response.sendRedirect(request.getContextPath() + landingPage.get());
            return;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    private static String normalizePath(String servletPath) {
        if (servletPath == null || servletPath.isBlank()) {
            return "/";
        }
        return servletPath.startsWith("/") ? servletPath : "/" + servletPath;
    }
}
