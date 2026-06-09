package pt.isel.gape.web.controller;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.security.session.SessionManager;
import pt.isel.gape.security.session.SessionUser;

abstract class DashboardServletSupport extends HttpServlet {

    private static final String FLASH_SUCCESS = "gape.flash.success";
    private static final String FLASH_ERROR = "gape.flash.error";

    protected final SessionManager sessionManager;

    DashboardServletSupport() {
        this(new SessionManager());
    }

    DashboardServletSupport(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    protected SessionUser requireCurrentUser(HttpServletRequest request) {
        return sessionManager.getSessionUser(request)
                .orElseThrow(() -> new SecurityException("Authenticated user is required"));
    }

    protected Long currentSessionId(HttpServletRequest request) {
        OptionalLong sessionId = sessionManager.getDatabaseSessionId(request);
        return sessionId.isPresent() ? sessionId.getAsLong() : null;
    }

    protected AccessProfileType primaryProfile(SessionUser sessionUser) {
        return sessionUser.primaryProfileType()
                .orElseThrow(() -> new SecurityException("Authenticated user has no profile"));
    }

    protected void refreshCurrentUser(HttpServletRequest request, SessionUser currentUser, User updatedUser) {
        Map<AccessProfileType, Set<String>> permissionsByProfile = new EnumMap<>(AccessProfileType.class);
        for (AccessProfileType profileType : currentUser.profileTypes()) {
            permissionsByProfile.put(profileType, currentUser.permissionCodesFor(profileType));
        }
        SessionUser refreshed = new SessionUser(
                updatedUser.id(),
                updatedUser.name(),
                updatedUser.email(),
                updatedUser.photo(),
                currentUser.profileTypes(),
                permissionsByProfile
        );
        sessionManager.refreshAuthenticatedSession(request, refreshed);
    }

    protected void prepareDashboard(
            HttpServletRequest request,
            String activeMenu,
            String pageTitle,
            String topActionHref,
            String topActionLabel
    ) {
        request.setAttribute("activeMenu", activeMenu);
        request.setAttribute("pageTitle", pageTitle);
        request.setAttribute("topActionHref", topActionHref);
        request.setAttribute("topActionLabel", topActionLabel);
        consumeFlash(request);
    }

    protected void prepareDashboard(HttpServletRequest request, String activeMenu, String pageTitle) {
        prepareDashboard(request, activeMenu, pageTitle, null, null);
    }

    protected void flashSuccess(HttpServletRequest request, String message) {
        request.getSession(true).setAttribute(FLASH_SUCCESS, message);
    }

    protected void flashError(HttpServletRequest request, String message) {
        request.getSession(true).setAttribute(FLASH_ERROR, message);
    }

    protected void forward(HttpServletRequest request, HttpServletResponse response, String jsp)
            throws ServletException, IOException {
        request.getRequestDispatcher(jsp).forward(request, response);
    }

    protected void redirect(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    protected void redirectToReturnPath(HttpServletRequest request, HttpServletResponse response, String defaultPath)
            throws IOException {
        String returnTo = text(request, "returnTo");
        if (isSafeReturnPath(returnTo)) {
            redirect(request, response, returnTo);
            return;
        }
        redirect(request, response, defaultPath);
    }

    protected boolean hasSafeReturnPath(HttpServletRequest request) {
        return isSafeReturnPath(text(request, "returnTo"));
    }

    protected static String text(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static boolean isSafeReturnPath(String path) {
        return path != null
                && path.startsWith("/")
                && !path.startsWith("//")
                && !path.contains("\\")
                && !path.contains(":");
    }

    protected static long longParameter(HttpServletRequest request, String name) {
        String value = text(request, name);
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return Long.parseLong(value);
    }

    protected static Long pathLong(String pathInfo, int segmentIndex) {
        String[] segments = pathSegments(pathInfo);
        if (segments.length <= segmentIndex) {
            return null;
        }
        return Long.parseLong(segments[segmentIndex]);
    }

    protected static String[] pathSegments(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
            return new String[0];
        }
        String normalized = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        return normalized.isBlank() ? new String[0] : normalized.split("/");
    }

    protected static String messageFor(RuntimeException exception) {
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
        if (message.contains("permission is required")) {
            return "You do not have permission to perform this operation.";
        }
        if (message.contains("not a supported image")) {
            return "The uploaded file is not a supported image.";
        }
        if (message.contains("WebP image writer")) {
            return "The server cannot convert the uploaded image to WebP.";
        }
        if (message.contains("processedAt cannot be before submittedAt")) {
            return "The processing date cannot be before the submission date.";
        }
        if (message.contains("Final deletion request state requires processedAt")) {
            return "A final state requires a processing date.";
        }
        if (message.contains("Unknown")) {
            return "The selected record does not exist.";
        }
        return message;
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
