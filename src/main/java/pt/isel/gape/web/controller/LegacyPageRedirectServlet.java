package pt.isel.gape.web.controller;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Keeps old template URLs from rendering deleted JSPs. The application routes
 * are owned by the current controllers; these mappings only provide a safe
 * migration path for bookmarks and stale links.
 */
@WebServlet(
        name = "legacyPageRedirectServlet",
        urlPatterns = {
                "/courses.jsp",
                "/course.jsp",
                "/course-list-view.jsp",
                "/course-details.jsp",
                "/messages.jsp",
                "/dashbord.jsp"
        }
)
public final class LegacyPageRedirectServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String target = switch (request.getServletPath()) {
            case "/messages.jsp" -> "/messages";
            case "/dashbord.jsp" -> "/dashboard";
            case "/courses.jsp", "/course.jsp", "/course-list-view.jsp", "/course-details.jsp" -> {
                String courseId = firstNonBlank(request.getParameter("courseId"), request.getParameter("id"));
                yield courseId != null && courseId.matches("\\d+")
                        ? "/courses/" + courseId
                        : "/courses";
            }
            default -> "/login.jsp";
        };
        response.sendRedirect(request.getContextPath() + target);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null || second.isBlank() ? null : second;
    }
}
