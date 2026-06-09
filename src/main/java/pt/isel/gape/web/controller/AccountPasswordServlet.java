package pt.isel.gape.web.controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.service.UserService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.crypto.PasswordHasher;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.web.navigation.DashboardNavigation;

@WebServlet(name = "accountPasswordServlet", urlPatterns = "/account/password")
public final class AccountPasswordServlet extends DashboardServletSupport {

    private final UserService userService;
    private final PasswordHasher passwordHasher;

    public AccountPasswordServlet() {
        this(new UserService(ConnectionProvider.defaultProvider()), new PasswordHasher());
    }

    AccountPasswordServlet(UserService userService, PasswordHasher passwordHasher) {
        this.userService = userService;
        this.passwordHasher = passwordHasher;
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
            String newPassword = text(request, "newPassword");
            String confirmPassword = text(request, "confirmPassword");
            validatePassword(newPassword, confirmPassword);

            PasswordHasher.PasswordHash passwordHash = passwordHasher.createHash(newPassword);
            User updated = userService.changePassword(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    actor.userId(),
                    passwordHash.hashBase64(),
                    passwordHash.saltBase64(),
                    request.getRemoteAddr()
            );
            refreshCurrentUser(request, actor, updated);
            flashSuccess(request, "Password saved.");
            redirectToReturnPath(request, response, profileFallback(actor));
        } catch (RuntimeException exception) {
            flashError(request, accountMessageFor(exception));
            redirectToReturnPath(request, response, profileFallback(actor));
        }
    }

    private static void validatePassword(String newPassword, String confirmPassword) {
        if (newPassword == null) {
            throw new IllegalArgumentException("New password is required.");
        }
        if (confirmPassword == null) {
            throw new IllegalArgumentException("Confirm password is required.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Confirm password must match the new password.");
        }
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must contain at least 8 characters.");
        }
        if (!newPassword.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("New password must contain at least 1 lower letter.");
        }
        if (!newPassword.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("New password must contain at least 1 uppercase letter.");
        }
        if (!newPassword.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("New password must contain at least 1 number.");
        }
        if (!newPassword.matches(".*[^A-Za-z0-9].*")) {
            throw new IllegalArgumentException("New password must contain at least 1 special character.");
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
        if (message.contains("permission is required")) {
            return "You do not have permission to perform this operation.";
        }
        return message;
    }
}
