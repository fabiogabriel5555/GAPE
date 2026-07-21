package pt.isel.gape.web.controller;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Compatibility route for old bookmarked user-audit links.
 *
 * <p>The audit UI is deliberately rendered only by {@link DashboardServlet}
 * in its Logs tab. This servlet never reads audit data or forwards to a JSP.</p>
 */
@WebServlet(name = "adminActivityLogServlet", urlPatterns = "/admin/activity-log")
public final class AdminActivityLogServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        StringBuilder target = new StringBuilder(request.getContextPath()).append("/dashboard?tab=logs");
        Long userId = positiveUserId(request.getParameter("userId"));
        if (userId != null) {
            target.append("&userId=").append(userId);
        }
        response.sendRedirect(target.toString());
    }

    private static Long positiveUserId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long value = Long.parseLong(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
