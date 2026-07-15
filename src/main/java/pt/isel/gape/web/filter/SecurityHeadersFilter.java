package pt.isel.gape.web.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.security.authorization.AuthorizationPolicy;

public final class SecurityHeadersFilter implements Filter {

    private static final String CONTENT_SECURITY_POLICY = String.join(" ",
            "default-src 'self';",
            "base-uri 'self';",
            "object-src 'none';",
            "frame-ancestors 'none';",
            "form-action 'self';",
            "script-src 'self' 'unsafe-inline';",
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://unpkg.com;",
            "img-src 'self' data: blob:;",
            "font-src 'self' data: https://fonts.gstatic.com https://unpkg.com;",
            "media-src 'self' blob: https://cdn.plyr.io;",
            "connect-src 'self';",
            "worker-src 'self' blob:;",
            "frame-src 'self' blob:;"
    );

    private static final String SAME_ORIGIN_LESSON_MODAL_POLICY =
            CONTENT_SECURITY_POLICY.replace("frame-ancestors 'none';", "frame-ancestors 'self';");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)
                || !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        applySecurityHeaders(httpRequest, httpResponse);
        if ("TRACE".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        chain.doFilter(request, response);
    }

    private static void applySecurityHeaders(HttpServletRequest request, HttpServletResponse response) {
        boolean sameOriginLessonModal = isSameOriginLessonModal(request);
        response.setHeader(
                "Content-Security-Policy",
                sameOriginLessonModal ? SAME_ORIGIN_LESSON_MODAL_POLICY : CONTENT_SECURITY_POLICY
        );
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", sameOriginLessonModal ? "SAMEORIGIN" : "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader(
                "Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=(), usb=()"
        );
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        if (AuthorizationPolicy.isProtected(request.getServletPath())) {
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
        }
    }

    /**
     * Lesson create, edit and detail views are intentionally rendered in a fixed,
     * same-origin dialog. All other responses remain non-frameable.
     */
    private static boolean isSameOriginLessonModal(HttpServletRequest request) {
        return "/learning/lessons".equals(request.getServletPath())
                && request.getPathInfo() != null
                && !request.getPathInfo().isBlank()
                && "1".equals(request.getParameter("modal"));
    }
}
