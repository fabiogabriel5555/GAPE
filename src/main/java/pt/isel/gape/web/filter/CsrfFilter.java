package pt.isel.gape.web.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import pt.isel.gape.security.session.SessionManager;

public final class CsrfFilter implements Filter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final Set<String> LOGIN_GET_PATHS = Set.of("/login.jsp", "/sign-in.jsp", "/auth/login");

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
        if (method != null && SAFE_METHODS.contains(method.toUpperCase(Locale.ROOT))) {
            if ("GET".equalsIgnoreCase(method)
                    && LOGIN_GET_PATHS.contains(normalizePath(httpRequest.getServletPath()))) {
                sessionManager.ensureCsrfToken(httpRequest);
            }
            chain.doFilter(request, response);
            return;
        }

        if (!sessionManager.isValidCsrfToken(httpRequest, csrfToken(httpRequest))) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(request, response);
    }

    private static String csrfToken(HttpServletRequest request) throws IOException, ServletException {
        String headerToken = request.getHeader("X-CSRF-Token");
        if (headerToken != null && !headerToken.isBlank()) {
            return headerToken;
        }
        String token = request.getParameter("csrfToken");
        if (token != null || !isMultipart(request)) {
            return token;
        }
        Part part = request.getPart("csrfToken");
        if (part == null || part.getSize() <= 0 || part.getSize() > 4096) {
            return null;
        }
        try (var inputStream = part.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    private static boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }

    private static String normalizePath(String servletPath) {
        if (servletPath == null || servletPath.isBlank()) {
            return "/";
        }
        return servletPath.startsWith("/") ? servletPath : "/" + servletPath;
    }
}
