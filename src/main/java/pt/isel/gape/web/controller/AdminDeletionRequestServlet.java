package pt.isel.gape.web.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.model.DeletionRequestState;
import pt.isel.gape.access.service.DeletionRequestService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.web.view.DeletionRequestView;

@WebServlet(name = "adminDeletionRequestServlet", urlPatterns = {"/admin/deletion-requests", "/admin/deletion-requests/*"})
public final class AdminDeletionRequestServlet extends DashboardServletSupport {

    private static final String ADMIN_DELETION_JSP = "/admin/admin/user/admin-deletion-requests.jsp";

    private final DeletionRequestService deletionRequestService;

    public AdminDeletionRequestServlet() {
        this(new DeletionRequestService(ConnectionProvider.defaultProvider()));
    }

    AdminDeletionRequestServlet(DeletionRequestService deletionRequestService) {
        this.deletionRequestService = deletionRequestService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        showRequests(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String[] segments = pathSegments(request.getPathInfo());
        if (segments.length != 2 || !"process".equals(segments[1])) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        SessionUser actor = requireCurrentUser(request);
        long requestId = Long.parseLong(segments[0]);
        try {
            deletionRequestService.processDeletionRequest(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    requestId,
                    DeletionRequestState.valueOf(required(text(request, "state"), "The state is required.")),
                    processedAtFrom(text(request, "processedAt")),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Deletion request processed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/admin/deletion-requests");
    }

    private void showRequests(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        markLearningEventsReadForCurrentUser(request, "/admin/deletion-requests", false);
        Long targetUserId = optionalLongParameter(request, "userId");
        List<DeletionRequestView> requests = (targetUserId == null
                        ? deletionRequestService.listRequests(actor.userId(), primaryProfile(actor))
                        : deletionRequestService.listRequestsForUser(actor.userId(), primaryProfile(actor), targetUserId))
                .stream()
                .map(DeletionRequestView::from)
                .toList();
        request.setAttribute("requests", requests);
        if (targetUserId != null) {
            request.setAttribute("filteredUserId", targetUserId);
            request.setAttribute("adminUserContextId", targetUserId);
            request.setAttribute("adminUserContextName", "User " + targetUserId);
            request.setAttribute("adminUserActiveChild", "deletion");
            request.setAttribute("adminReturnTo", "/admin/deletion-requests?userId=" + targetUserId);
            prepareDashboard(request, "users", "User Deletion");
        } else {
            request.setAttribute("adminReturnTo", "/admin/deletion-requests");
            prepareDashboard(request, "deletion-admin", "Deletion Processing");
        }
        forward(request, response, ADMIN_DELETION_JSP);
    }

    private static Long optionalLongParameter(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : Long.parseLong(value);
    }

    private static LocalDateTime processedAtFrom(String value) {
        return value == null || value.isBlank() ? null : ApplicationDateTimeFormat.parseUserDateTime(value);
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
