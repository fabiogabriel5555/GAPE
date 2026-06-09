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
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.security.authorization.AuthorizationDecision;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.authorization.AuthorizationRule;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.security.session.SessionManager;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.service.AuditService;
import pt.isel.gape.web.navigation.DashboardNavigation;

public final class AuthorizationFilter implements Filter {

    private final PermissionChecker permissionChecker;
    private final SessionManager sessionManager;
    private final AuditService auditService;

    public AuthorizationFilter() {
        this(
                new PermissionChecker(ConnectionProvider.defaultProvider()),
                new SessionManager(),
                new AuditService(ConnectionProvider.defaultProvider(), ApplicationClock.system())
        );
    }

    AuthorizationFilter(PermissionChecker permissionChecker, SessionManager sessionManager, AuditService auditService) {
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
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
        if (sessionUser.isEmpty()) {
            redirectToLogin(httpRequest, httpResponse);
            return;
        }

        Optional<AuthorizationRule> rule = AuthorizationPolicy.ruleFor(servletPath);
        if (rule.isEmpty()) {
            deny(httpRequest, httpResponse, sessionUser.get(), "missing_authorization_policy");
            return;
        }

        AuthorizationDecision decision = authorize(httpRequest, sessionUser.get(), rule.get());
        if (!decision.allowed()) {
            if (shouldRedirectToAuthorizedDashboard(decision)) {
                redirectToAuthorizedDashboard(httpRequest, httpResponse, sessionUser.get(), decision.reason());
            } else {
                deny(httpRequest, httpResponse, sessionUser.get(), decision.reason());
            }
            return;
        }

        chain.doFilter(request, response);
    }

    private AuthorizationDecision authorize(
            HttpServletRequest request,
            SessionUser sessionUser,
            AuthorizationRule rule
    ) {
        OptionalLong databaseSessionId = sessionManager.getDatabaseSessionId(request);
        Long sessionId = databaseSessionId.isPresent() ? databaseSessionId.getAsLong() : null;
        AuthorizationDecision deniedForOwnedProfile = null;
        for (AccessProfileType profileType : sessionUser.profileTypes()) {
            if (!rule.acceptsProfile(profileType)) {
                continue;
            }
            AccessContext context = new AccessContext(
                    sessionUser.userId(),
                    sessionId,
                    profileType,
                    rule.permissionCode(),
                    rule.entityType(),
                    null,
                    request.getRemoteAddr()
            );
            AuthorizationDecision decision = permissionChecker.check(context);
            if (decision.allowed()) {
                return decision;
            }
            deniedForOwnedProfile = decision;
        }
        if (deniedForOwnedProfile != null) {
            return deniedForOwnedProfile;
        }
        return AuthorizationDecision.deny("profile_area_not_owned");
    }

    private boolean shouldRedirectToAuthorizedDashboard(AuthorizationDecision decision) {
        return "profile_area_not_owned".equals(decision.reason());
    }

    private void redirectToAuthorizedDashboard(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser sessionUser,
            String reason
    ) throws IOException {
        Optional<String> landingPage = DashboardNavigation.landingPageFor(sessionUser);
        if (landingPage.isEmpty()) {
            deny(request, response, sessionUser, reason);
            return;
        }
        recordAuthorizationRedirect(request, sessionUser, reason);
        response.sendRedirect(request.getContextPath() + landingPage.get());
    }

    private void deny(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionUser sessionUser,
            String reason
    ) throws IOException {
        OptionalLong sessionId = sessionManager.getDatabaseSessionId(request);
        auditService.record(
                sessionUser.userId(),
                sessionId.isPresent() ? sessionId.getAsLong() : null,
                "AUTHORIZATION_DENIED",
                "request_path",
                normalizePath(request.getServletPath()) + ":" + reason,
                "denied",
                request.getRemoteAddr()
        );
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    private void recordAuthorizationRedirect(HttpServletRequest request, SessionUser sessionUser, String reason) {
        OptionalLong sessionId = sessionManager.getDatabaseSessionId(request);
        auditService.record(
                sessionUser.userId(),
                sessionId.isPresent() ? sessionId.getAsLong() : null,
                "AUTHORIZATION_REDIRECTED",
                "request_path",
                normalizePath(request.getServletPath()) + ":" + reason,
                "redirected",
                request.getRemoteAddr()
        );
    }

    private static void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/login.jsp?auth=required");
    }

    private static String normalizePath(String servletPath) {
        if (servletPath == null || servletPath.isBlank()) {
            return "/";
        }
        return servletPath.startsWith("/") ? servletPath : "/" + servletPath;
    }
}
