package pt.isel.gape.web.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.security.transport.HttpsConfiguration;

class SecurityHeadersFilterTest {

    private final SecurityHeadersFilter filter = new SecurityHeadersFilter();

    @Test
    void addsBrowserSecurityHeadersAndDisablesCachingForProtectedPages() throws Exception {
        TestResponse response = new TestResponse();
        TestChain chain = new TestChain();

        filter.doFilter(
                requestProxy("GET", "/dashboard", true),
                responseProxy(response),
                chainProxy(chain)
        );

        assertTrue(chain.called);
        assertEquals("nosniff", response.headers.get("X-Content-Type-Options"));
        assertEquals("DENY", response.headers.get("X-Frame-Options"));
        assertEquals("strict-origin-when-cross-origin", response.headers.get("Referrer-Policy"));
        assertEquals("same-origin", response.headers.get("Cross-Origin-Opener-Policy"));
        assertEquals("same-origin", response.headers.get("Cross-Origin-Resource-Policy"));
        assertEquals("no-store", response.headers.get("Cache-Control"));
        assertEquals("no-cache", response.headers.get("Pragma"));
        if (HttpsConfiguration.fromRuntimeConfiguration().requiresHttps()) {
            assertEquals(
                    "max-age=31536000; includeSubDomains",
                    response.headers.get("Strict-Transport-Security")
            );
        } else {
            assertNull(response.headers.get("Strict-Transport-Security"));
        }
        String contentSecurityPolicy = response.headers.get("Content-Security-Policy");
        assertTrue(contentSecurityPolicy.contains("frame-ancestors 'none'"));
        assertTrue(contentSecurityPolicy.contains(
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://unpkg.com"
        ));
        assertTrue(contentSecurityPolicy.contains(
                "font-src 'self' data: https://fonts.gstatic.com https://unpkg.com"
        ));
        assertTrue(contentSecurityPolicy.contains("media-src 'self' blob: https://cdn.plyr.io"));
        assertTrue(contentSecurityPolicy.contains("connect-src 'self'"));
    }

    @Test
    void doesNotSendHstsOverPlainHttpOrDisableCachingForPublicAssets() throws Exception {
        TestResponse response = new TestResponse();
        TestChain chain = new TestChain();

        filter.doFilter(
                requestProxy("GET", "/assets/css/main.css", false),
                responseProxy(response),
                chainProxy(chain)
        );

        assertTrue(chain.called);
        assertNull(response.headers.get("Strict-Transport-Security"));
        assertNull(response.headers.get("Cache-Control"));
    }

    @Test
    void permitsOnlyTheSameOriginLessonDialogToBeFramed() throws Exception {
        TestResponse response = new TestResponse();

        filter.doFilter(
                requestProxy("GET", "/learning/lessons", "/new", Map.of("modal", "1"), true),
                responseProxy(response),
                chainProxy(new TestChain())
        );

        assertEquals("SAMEORIGIN", response.headers.get("X-Frame-Options"));
        assertTrue(response.headers.get("Content-Security-Policy").contains("frame-ancestors 'self'"));
    }

    @Test
    void permitsModalCreationResponsesWithoutPathInfoToBeFramed() throws Exception {
        TestResponse response = new TestResponse();

        filter.doFilter(
                requestProxy("POST", "/learning/assessments", null, Map.of("modal", "1"), true),
                responseProxy(response),
                chainProxy(new TestChain())
        );

        assertEquals("SAMEORIGIN", response.headers.get("X-Frame-Options"));
        assertTrue(response.headers.get("Content-Security-Policy").contains("frame-ancestors 'self'"));
    }

    @Test
    void rejectsTraceRequestsBeforeTheyReachTheApplication() throws Exception {
        TestResponse response = new TestResponse();
        TestChain chain = new TestChain();

        filter.doFilter(
                requestProxy("TRACE", "/", false),
                responseProxy(response),
                chainProxy(chain)
        );

        assertFalse(chain.called);
        assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, response.errorStatus);
    }

    private static HttpServletRequest requestProxy(String method, String servletPath, boolean secure) {
        return requestProxy(method, servletPath, null, Map.of(), secure);
    }

    private static HttpServletRequest requestProxy(
            String method,
            String servletPath,
            String pathInfo,
            Map<String, String> parameters,
            boolean secure
    ) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, methodCall, args) -> switch (methodCall.getName()) {
                    case "getMethod" -> method;
                    case "getServletPath" -> servletPath;
                    case "getPathInfo" -> pathInfo;
                    case "getParameter" -> parameters.get((String) args[0]);
                    case "isSecure" -> secure;
                    default -> defaultValue(methodCall.getReturnType());
                }
        );
    }

    private static HttpServletResponse responseProxy(TestResponse state) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, methodCall, args) -> switch (methodCall.getName()) {
                    case "setHeader" -> {
                        state.headers.put((String) args[0], (String) args[1]);
                        yield null;
                    }
                    case "sendError" -> {
                        state.errorStatus = (Integer) args[0];
                        yield null;
                    }
                    default -> defaultValue(methodCall.getReturnType());
                }
        );
    }

    private static FilterChain chainProxy(TestChain state) {
        return (FilterChain) Proxy.newProxyInstance(
                FilterChain.class.getClassLoader(),
                new Class<?>[]{FilterChain.class},
                (proxy, methodCall, args) -> {
                    if ("doFilter".equals(methodCall.getName())) {
                        state.called = true;
                    }
                    return null;
                }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }

    private static final class TestResponse {
        private final Map<String, String> headers = new HashMap<>();
        private int errorStatus;
    }

    private static final class TestChain {
        private boolean called;
    }
}
