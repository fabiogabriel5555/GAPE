package pt.isel.gape.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class LegacyPageRedirectServletTest {

    private final LegacyPageRedirectServlet servlet = new LegacyPageRedirectServlet();

    @Test
    void oldCourseGridAndListAliasesRedirectToCatalog() throws Exception {
        assertRedirect("/course.jsp", Map.of(), "/ctx/courses");
        assertRedirect("/course-list-view.jsp", Map.of(), "/ctx/courses");
        assertRedirect("/courses.jsp", Map.of(), "/ctx/courses");
    }

    @Test
    void oldCourseDetailAliasPreservesNumericCourseId() throws Exception {
        assertRedirect("/course-details.jsp", Map.of("courseId", "42"), "/ctx/courses/42");
    }

    @Test
    void oldMessagesAndDashboardAliasesUseCanonicalRoutes() throws Exception {
        assertRedirect("/messages.jsp", Map.of(), "/ctx/messages");
        assertRedirect("/dashbord.jsp", Map.of(), "/ctx/dashboard");
    }

    private void assertRedirect(String servletPath, Map<String, String> parameters, String expected)
            throws Exception {
        TestResponse responseState = new TestResponse();
        servlet.doGet(request(servletPath, parameters), response(responseState));
        assertEquals(expected, responseState.location);
    }

    private HttpServletRequest request(String servletPath, Map<String, String> parameters) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getServletPath" -> servletPath;
                    case "getContextPath" -> "/ctx";
                    case "getParameter" -> parameters.get((String) args[0]);
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private HttpServletResponse response(TestResponse state) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> {
                    if ("sendRedirect".equals(method.getName())) {
                        state.location = (String) args[0];
                    }
                    return defaultValue(method.getReturnType());
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
        private String location;
    }
}
