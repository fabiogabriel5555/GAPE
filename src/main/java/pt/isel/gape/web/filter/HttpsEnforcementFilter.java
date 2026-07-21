package pt.isel.gape.web.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.security.transport.HttpsConfiguration;

/** Enforces the explicitly configured HTTPS deployment policy. */
public final class HttpsEnforcementFilter implements Filter {

    private static final int HTTP_PERMANENT_REDIRECT = 308;

    private final HttpsConfiguration httpsConfiguration;

    public HttpsEnforcementFilter() {
        this(HttpsConfiguration.fromRuntimeConfiguration());
    }

    HttpsEnforcementFilter(HttpsConfiguration httpsConfiguration) {
        this.httpsConfiguration = httpsConfiguration;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)
                || !(response instanceof HttpServletResponse httpResponse)
                || !httpsConfiguration.requiresHttps()
                || httpsConfiguration.isSecure(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        httpResponse.setStatus(HTTP_PERMANENT_REDIRECT);
        httpResponse.setHeader("Location", httpsConfiguration.redirectTarget(httpRequest));
    }
}
