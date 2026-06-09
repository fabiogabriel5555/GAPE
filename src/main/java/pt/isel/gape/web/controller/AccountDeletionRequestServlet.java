package pt.isel.gape.web.controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.service.DeletionRequestService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.web.navigation.DashboardNavigation;

@WebServlet(name = "accountDeletionRequestServlet", urlPatterns = "/account/deletion-requests")
public final class AccountDeletionRequestServlet extends DashboardServletSupport {

    private final DeletionRequestService deletionRequestService;

    public AccountDeletionRequestServlet() {
        this(new DeletionRequestService(ConnectionProvider.defaultProvider()));
    }

    AccountDeletionRequestServlet(DeletionRequestService deletionRequestService) {
        this.deletionRequestService = deletionRequestService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        redirect(request, response, profileFallback(requireCurrentUser(request)));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            if (request.getParameter("confirmDeletionRequest") == null) {
                throw new IllegalArgumentException("Confirm that you want to submit the request.");
            }
            deletionRequestService.submitDeletionRequest(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    text(request, "reason"),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Account deletion request submitted.");
            redirectToReturnPath(request, response, profileFallback(actor));
        } catch (RuntimeException exception) {
            flashError(request, accountMessageFor(exception));
            redirectToReturnPath(request, response, profileFallback(actor));
        }
    }

    private static String profileFallback(SessionUser actor) {
        return DashboardNavigation.profilePageFor(actor).orElse("/dashboard");
    }

    private static String accountMessageFor(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "The operation could not be completed.";
        }
        if (message.contains("Unknown")) {
            return "The requested record does not exist.";
        }
        return message;
    }
}
