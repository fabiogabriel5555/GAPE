package pt.isel.gape.web.view;

import java.io.IOException;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.Predicate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.DeletionRequest;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.service.DeletionRequestService;
import pt.isel.gape.access.service.UserService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.config.SupportedDocumentTypeCatalog;
import pt.isel.gape.common.config.SupportedLanguageCatalog;
import pt.isel.gape.security.session.SessionManager;
import pt.isel.gape.security.session.SessionUser;

public final class DashboardMyProfilePageData {

    private static final String FLASH_SUCCESS = "gape.flash.success";
    private static final String FLASH_ERROR = "gape.flash.error";

    private DashboardMyProfilePageData() {
    }

    public static boolean prepare(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionManager sessionManager = new SessionManager();
        SessionUser actor = sessionManager.getSessionUser(request).orElse(null);
        if (actor == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return false;
        }

        AccessProfileType profileType = actor.primaryProfileType()
                .orElseThrow(() -> new SecurityException("Authenticated user has no profile"));
        OptionalLong sessionId = sessionManager.getDatabaseSessionId(request);
        Long currentSessionId = sessionId.isPresent() ? sessionId.getAsLong() : null;
        ConnectionProvider connectionProvider = ConnectionProvider.defaultProvider();

        UserService userService = new UserService(connectionProvider);
        DeletionRequestService deletionRequestService = new DeletionRequestService(connectionProvider);

        User user = userService.readPersonalData(
                actor.userId(),
                currentSessionId,
                profileType,
                actor.userId(),
                request.getRemoteAddr()
        );
        List<DeletionRequestView> deletionRequests = deletionRequestService
                .listRequests(actor.userId(), profileType)
                .stream()
                .filter(requestFor(actor.userId()))
                .map(DeletionRequestView::from)
                .toList();

        consumeFlash(request);
        request.setAttribute("user", UserView.from(user));
        request.setAttribute("form", UserFormData.from(user));
        request.setAttribute("languageOptions", SupportedLanguageCatalog.all());
        request.setAttribute("documentTypeOptions", SupportedDocumentTypeCatalog.all());
        request.setAttribute("returnTo", request.getServletPath());
        request.setAttribute("deletionRequests", deletionRequests);
        request.setAttribute("hasDeletionRequests", !deletionRequests.isEmpty());
        request.setAttribute("latestDeletionRequest", deletionRequests.isEmpty() ? null : deletionRequests.getLast());
        return true;
    }

    private static Predicate<DeletionRequest> requestFor(long userId) {
        return deletionRequest -> deletionRequest.submitterUserId() == userId;
    }

    private static void consumeFlash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        moveFlash(session, request, FLASH_SUCCESS, "successMessage");
        moveFlash(session, request, FLASH_ERROR, "errorMessage");
    }

    private static void moveFlash(HttpSession session, HttpServletRequest request, String sessionKey, String requestKey) {
        Object value = session.getAttribute(sessionKey);
        if (value != null) {
            request.setAttribute(requestKey, value);
            session.removeAttribute(sessionKey);
        }
    }
}
