package pt.isel.gape.web.controller;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.model.UserPersonalProfileUpdate;
import pt.isel.gape.access.service.UserService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.web.media.ProfilePhotoStorage;
import pt.isel.gape.web.navigation.DashboardNavigation;

@WebServlet(name = "accountProfileServlet", urlPatterns = "/account/profile")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 12 * 1024 * 1024)
public final class AccountProfileServlet extends DashboardServletSupport {

    private final UserService userService;
    private final ProfilePhotoStorage profilePhotoStorage;

    public AccountProfileServlet() {
        this(new UserService(ConnectionProvider.defaultProvider()), new ProfilePhotoStorage());
    }

    AccountProfileServlet(UserService userService) {
        this(userService, new ProfilePhotoStorage());
    }

    AccountProfileServlet(UserService userService, ProfilePhotoStorage profilePhotoStorage) {
        this.userService = userService;
        this.profilePhotoStorage = profilePhotoStorage;
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
            User existing = userService.readPersonalData(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    actor.userId(),
                    request.getRemoteAddr()
            );
            String uploadedPhoto = profilePhotoStorage.saveProfilePhoto(
                    actor.userId(),
                    profileImagePart(request),
                    getServletContext()
            );
            String submittedPhoto = text(request, "photo");
            String photo = uploadedPhoto != null ? uploadedPhoto : (submittedPhoto != null ? submittedPhoto : existing.photo());
            User updated = userService.editPersonalProfile(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    actor.userId(),
                    new UserPersonalProfileUpdate(
                            text(request, "name"),
                            text(request, "email"),
                            text(request, "language"),
                            photo,
                            text(request, "documentType"),
                            text(request, "documentNumber")
                    ),
                    request.getRemoteAddr()
            );
            refreshCurrentUser(request, actor, updated);
            flashSuccess(request, "Account settings saved.");
            redirectToReturnPath(request, response, profileFallback(actor));
        } catch (RuntimeException exception) {
            flashError(request, accountMessageFor(exception));
            redirectToReturnPath(request, response, profileFallback(actor));
        }
    }

    private static Part profileImagePart(HttpServletRequest request) throws IOException, ServletException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            return null;
        }
        return request.getPart("profileImage");
    }

    private static String profileFallback(SessionUser actor) {
        return DashboardNavigation.profilePageFor(actor).orElse("/dashboard");
    }

    private static String accountMessageFor(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "The operation could not be completed.";
        }
        if (message.contains("Duplicate email")) {
            return "There is already a user with this email.";
        }
        if (message.contains("Duplicate document")) {
            return "There is already a user with this document.";
        }
        if (message.contains("Document type and number")) {
            return "Document type and document number must be filled together.";
        }
        if (message.contains("Unsupported document type")) {
            return "The selected document type is not supported.";
        }
        if (message.contains("Invalid document number")) {
            return "The document number does not match the selected document type.";
        }
        if (message.contains("Unsupported language")) {
            return "The selected language is not supported.";
        }
        if (message.contains("not a supported image")) {
            return "The uploaded file is not a supported image.";
        }
        if (message.contains("WebP native") || message.contains("native WebP")) {
            return "The server cannot convert the uploaded image to WebP.";
        }
        if (message.contains("WebP image writer")) {
            return "The server cannot convert the uploaded image to WebP.";
        }
        return message;
    }
}
