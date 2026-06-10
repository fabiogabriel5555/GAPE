package pt.isel.gape.web.filter;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.security.session.SessionManager;

public final class CsrfFilter implements Filter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final Set<String> PROTECTED_MUTATING_PREFIXES = Set.of(
            "/account",
            "/admin/users",
            "/admin/organizations",
            "/admin/deletion-requests",
            "/auth/logout"
    );

    private final SessionManager sessionManager;

    public CsrfFilter() {
        this(new SessionManager());
    }

    CsrfFilter(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)
                || !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        String method = httpRequest.getMethod();
        if (method == null || SAFE_METHODS.contains(method.toUpperCase())) {
            chain.doFilter(request, response);
            return;
        }

        String servletPath = normalizePath(httpRequest.getServletPath());
        if (!requiresCsrf(servletPath)) {
            chain.doFilter(request, response);
            return;
        }

        if (!sessionManager.isValidCsrfToken(httpRequest, httpRequest.getParameter("csrfToken"))) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(request, response);
    }

    private static String normalizePath(String servletPath) {
        if (servletPath == null || servletPath.isBlank()) {
            return "/";
        }
        return servletPath.startsWith("/") ? servletPath : "/" + servletPath;
    }

    private static boolean requiresCsrf(String path) {
        for (String prefix : PROTECTED_MUTATING_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }
}
