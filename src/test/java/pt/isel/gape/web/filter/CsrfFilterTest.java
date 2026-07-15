package pt.isel.gape.web.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

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
    void protectedMultipartPostReadsCsrfTokenPart() throws Exception {
        TestHttpSession session = new TestHttpSession();
        session.setAttribute("gape.auth.csrfToken", "csrf-123");
        TestHttpServletResponse response = new TestHttpServletResponse();
        TestFilterChain chain = new TestFilterChain();

        filter.doFilter(
                multipartRequestProxy("POST", "/messages/send", session, "csrf-123"),
                responseProxy(response),
                chainProxy(chain)
        );

        assertTrue(chain.called);
        assertEquals(null, response.errorStatus);
    }

    @Test
    void protectedAjaxMutationAcceptsCsrfHeader() throws Exception {
        TestHttpSession session = new TestHttpSession();
        session.setAttribute("gape.auth.csrfToken", "csrf-123");
        TestHttpServletResponse response = new TestHttpServletResponse();
        TestFilterChain chain = new TestFilterChain();

        filter.doFilter(
                headerRequestProxy("DELETE", "/student/events/read-by-href", session, "csrf-123"),
                responseProxy(response),
                chainProxy(chain)
        );

        assertTrue(chain.called);
        assertEquals(null, response.errorStatus);
    }

    @Test
    void loginPostWithoutAnonymousCsrfTokenIsForbidden() throws Exception {
        TestHttpServletResponse response = new TestHttpServletResponse();
        TestFilterChain chain = new TestFilterChain();

        filter.doFilter(
                requestProxy("POST", "/auth/login", null, null),
                responseProxy(response),
                chainProxy(chain)
        );

        assertFalse(chain.called);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.errorStatus);
    }

    @Test
    void loginGetCreatesAnonymousTokenAndLoginPostAcceptsIt() throws Exception {
        TestHttpSession session = new TestHttpSession();
        TestHttpServletResponse getResponse = new TestHttpServletResponse();
        TestFilterChain getChain = new TestFilterChain();

        filter.doFilter(
                requestProxy("GET", "/login.jsp", session, null),
                responseProxy(getResponse),
                chainProxy(getChain)
        );

        String token = (String) session.getAttribute("gape.auth.csrfToken");
        assertTrue(getChain.called);
        assertTrue(token != null && !token.isBlank());

        TestHttpServletResponse postResponse = new TestHttpServletResponse();
        TestFilterChain postChain = new TestFilterChain();
        filter.doFilter(
                requestProxy("POST", "/auth/login", session, token),
                responseProxy(postResponse),
                chainProxy(postChain)
        );

        assertTrue(postChain.called);
        assertEquals(null, postResponse.errorStatus);
    }

    @Test
    void loginGetKeepsExistingAuthenticatedCsrfToken() throws Exception {
        TestHttpSession session = new TestHttpSession();
        session.setAttribute("gape.auth.csrfToken", "authenticated-token");
        TestFilterChain chain = new TestFilterChain();

        filter.doFilter(
                requestProxy("GET", "/sign-in.jsp", session, null),
                responseProxy(new TestHttpServletResponse()),
                chainProxy(chain)
        );

        assertTrue(chain.called);
        assertEquals("authenticated-token", session.getAttribute("gape.auth.csrfToken"));
    }

    @Test
    void nonPostLoginMutationRequiresCsrf() throws Exception {
        TestHttpServletResponse response = new TestHttpServletResponse();
        TestFilterChain chain = new TestFilterChain();

        filter.doFilter(
                requestProxy("DELETE", "/auth/login", null, null),
                responseProxy(response),
                chainProxy(chain)
        );

        assertFalse(chain.called);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.errorStatus);
    }

    @Test
    void missingHttpMethodFailsClosed() throws Exception {
        TestHttpServletResponse response = new TestHttpServletResponse();
        TestFilterChain chain = new TestFilterChain();

        filter.doFilter(
                requestProxy(null, "/admin/users/4/block", null, null),
                responseProxy(response),
                chainProxy(chain)
        );

        assertFalse(chain.called);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.errorStatus);
    }

    @Test
    void unknownMutatingEndpointFailsClosedWithoutCsrfToken() throws Exception {
        TestHttpServletResponse response = new TestHttpServletResponse();
        TestFilterChain chain = new TestFilterChain();

        filter.doFilter(
                requestProxy("POST", "/admin/admin-courses.jsp", null, null),
                responseProxy(response),
                chainProxy(chain)
        );

        assertFalse(chain.called);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.errorStatus);
    }

    @Test
    void studentAliasPostsCannotBypassCsrfProtection() throws Exception {
        String[] aliasPaths = {
                "/student/courses/31/enroll",
                "/student/enrollments/class-groups/42",
                "/student/class-groups/51/withdraw",
                "/student/events/read-by-href"
        };

        for (String aliasPath : aliasPaths) {
            TestHttpServletResponse response = new TestHttpServletResponse();
            TestFilterChain chain = new TestFilterChain();

            filter.doFilter(
                    requestProxy("POST", aliasPath, null, null),
                    responseProxy(response),
                    chainProxy(chain)
            );

            assertFalse(chain.called, aliasPath);
            assertEquals(HttpServletResponse.SC_FORBIDDEN, response.errorStatus, aliasPath);
        }
    }

    private static HttpServletRequest requestProxy(
            String method,
            String servletPath,
            TestHttpSession session,
            String csrfToken
    ) {
        return requestProxy(method, servletPath, session, csrfToken, null, null, null);
    }

    private static HttpServletRequest multipartRequestProxy(
            String method,
            String servletPath,
            TestHttpSession session,
            String multipartCsrfToken
    ) {
        return requestProxy(
                method,
                servletPath,
                session,
                null,
                "multipart/form-data; boundary=test",
                multipartCsrfToken,
                null
        );
    }

    private static HttpServletRequest headerRequestProxy(
            String method,
            String servletPath,
            TestHttpSession session,
            String headerCsrfToken
    ) {
        return requestProxy(method, servletPath, session, null, null, null, headerCsrfToken);
    }

    private static HttpServletRequest requestProxy(
            String method,
            String servletPath,
            TestHttpSession session,
            String csrfToken,
            String contentType,
            String multipartCsrfToken,
            String headerCsrfToken
    ) {
        HttpSession httpSession = session == null ? null : session.proxy();
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class },
                (proxy, methodCall, args) -> switch (methodCall.getName()) {
                    case "getMethod" -> method;
                    case "getServletPath" -> servletPath;
                    case "getSession" -> httpSession;
                    case "getContentType" -> contentType;
                    case "getHeader" -> "X-CSRF-Token".equals(args[0]) ? headerCsrfToken : null;
                    case "getPart" -> "csrfToken".equals(args[0]) && multipartCsrfToken != null
                            ? partProxy(multipartCsrfToken)
                            : null;
                    case "getParameter" -> "csrfToken".equals(args[0]) ? csrfToken : null;
                    default -> defaultValue(methodCall.getReturnType());
                }
        );
    }

    private static Part partProxy(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        return (Part) Proxy.newProxyInstance(
                Part.class.getClassLoader(),
                new Class<?>[] { Part.class },
                (proxy, methodCall, args) -> switch (methodCall.getName()) {
                    case "getSize" -> (long) bytes.length;
                    case "getInputStream" -> new ByteArrayInputStream(bytes);
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

        Object getAttribute(String name) {
            return attributes.get(name);
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
