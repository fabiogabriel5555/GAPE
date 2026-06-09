package pt.isel.gape.web.controller;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.service.ActivityLogService;
import pt.isel.gape.web.view.ActivityLogView;

@WebServlet(name = "adminActivityLogServlet", urlPatterns = "/admin/activity-log")
public final class AdminActivityLogServlet extends DashboardServletSupport {

    private static final String ACTIVITY_LOG_JSP = "/admin/admin/admin-audit.jsp";

    private final ActivityLogService activityLogService;

    public AdminActivityLogServlet() {
        this(new ActivityLogService(ConnectionProvider.defaultProvider()));
    }

    AdminActivityLogServlet(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Long targetUserId = optionalLongParameter(request, "userId");
        List<ActivityLogView> logs = (targetUserId == null
                        ? activityLogService.listForActor(actor.userId(), primaryProfile(actor))
                        : activityLogService.listForUserAudit(actor.userId(), primaryProfile(actor), targetUserId))
                .stream()
                .map(ActivityLogView::from)
                .toList();
        request.setAttribute("logs", logs);
        if (targetUserId != null) {
            request.setAttribute("filteredUserId", targetUserId);
            request.setAttribute("adminUserContextId", targetUserId);
            request.setAttribute("adminUserContextName", "User " + targetUserId);
            request.setAttribute("adminUserActiveChild", "audit");
            prepareDashboard(request, "users", "User Audit");
        } else {
            prepareDashboard(request, "audit", "Audit");
        }
        forward(request, response, ACTIVITY_LOG_JSP);
    }

    private static Long optionalLongParameter(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : Long.parseLong(value);
    }
}
