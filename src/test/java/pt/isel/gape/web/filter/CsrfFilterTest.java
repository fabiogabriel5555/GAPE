package pt.isel.gape.web.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

class CsrfFilterTest {

    private final CsrfFilter filter = new CsrfFilter();

    @Test
    void protectedPostWithoutCsrfTokenIsForbidden() throws Exception {
        TestHttpSession session = new TestHttpSession();
        session.setAttribute("gape.auth.csrfToken", "csrf-123");
        TestHttpServletResponse response = new TestHttpServletResponse();
        TestFilterChain chain = new TestFilterChain();

        filter.doFilter(
                requestProxy("POST", "/admin/users/4/block", session, null),
                responseProxy(response),
                chainProxy(chain)
        );

        assertFalse(chain.called);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.errorStatus);
    }

    @Test
    void protectedPostWithValidCsrfTokenContinues() throws Exception {
        TestHttpSession session = new TestHttpSession();
        session.setAttribute("gape.auth.csrfToken", "csrf-123");
        TestHttpServletResponse response = new TestHttpServletResponse();
        TestFilterChain chain = new TestFilterChain();

        filter.doFilter(
                requestProxy("POST", "/admin/users/4/block", session, "csrf-123"),
                responseProxy(response),
                chainProxy(chain)
        );

        assertTrue(chain.called);
        assertEquals(null, response.errorStatus);
    }

    @Test
    void contentUploadPostWithoutCsrfTokenIsForbidden() throws Exception {
        TestHttpSession session = new TestHttpSession();
        session.setAttribute("gape.auth.csrfToken", "csrf-123");
        TestHttpServletResponse response = new TestHttpServletResponse();
        TestFilterChain chain = new TestFilterChain();

        filter.doFilter(
                requestProxy("POST", "/contents/upload", session, null),
                responseProxy(response),
                chainProxy(chain)
        );

        assertFalse(chain.called);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.errorStatus);
    }

    @Test
    void publicPostDoesNotRequireCsrfToken() throws Exception {
        TestHttpServletResponse response = new TestHttpServletResponse();
        TestFilterChain chain = new TestFilterChain();

        filter.doFilter(
                requestProxy("POST", "/auth", null, null),
                responseProxy(response),
                chainProxy(chain)
        );

        assertTrue(chain.called);
        assertEquals(null, response.errorStatus);
    }

    @Test
    void protectedTemplatePostOutsideMutatingEndpointsDoesNotRequireCsrfToken() throws Exception {
        TestHttpServletResponse response = new TestHttpServletResponse();
        TestFilterChain chain = new TestFilterChain();

        filter.doFilter(
                requestProxy("POST", "/admin/admin-courses.jsp", null, null),
                responseProxy(response),
                chainProxy(chain)
        );

        assertTrue(chain.called);
        assertEquals(null, response.errorStatus);
    }

    private static HttpServletRequest requestProxy(
            String method,
            String servletPath,
            TestHttpSession session,
            String csrfToken
    ) {
        HttpSession httpSession = session == null ? null : session.proxy();
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class },
                (proxy, methodCall, args) -> switch (methodCall.getName()) {
                    case "getMethod" -> method;
                    case "getServletPath" -> servletPath;
                    case "getSession" -> httpSession;
                    case "getParameter" -> "csrfToken".equals(args[0]) ? csrfToken : null;
                    default -> defaultValue(methodCall.getReturnType());
                }
        );
    }

    private static HttpServletResponse responseProxy(TestHttpServletResponse state) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[] { HttpServletResponse.class },
                (proxy, methodCall, args) -> {
                    if ("sendError".equals(methodCall.getName())) {
                        state.errorStatus = (Integer) args[0];
                        return null;
                    }
                    return defaultValue(methodCall.getReturnType());
                }
        );
    }

    private static FilterChain chainProxy(TestFilterChain state) {
        return (FilterChain) Proxy.newProxyInstance(
                FilterChain.class.getClassLoader(),
                new Class<?>[] { FilterChain.class },
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

    private static final class TestHttpSession {
        private final Map<String, Object> attributes = new HashMap<>();

        void setAttribute(String name, Object value) {
            attributes.put(name, value);
        }

        HttpSession proxy() {
            return (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(),
                    new Class<?>[] { HttpSession.class },
                    (proxy, methodCall, args) -> switch (methodCall.getName()) {
                        case "getAttribute" -> attributes.get(args[0]);
                        case "setAttribute" -> {
                            attributes.put((String) args[0], args[1]);
                            yield null;
                        }
                        default -> defaultValue(methodCall.getReturnType());
                    }
            );
        }
    }

    private static final class TestHttpServletResponse {
        private Integer errorStatus;
    }

    private static final class TestFilterChain {
        private boolean called;
    }
}
