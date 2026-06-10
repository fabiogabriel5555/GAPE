package pt.isel.gape.web.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfile;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.model.UserCreateCommand;
import pt.isel.gape.access.model.UserState;
import pt.isel.gape.access.model.UserUpdateCommand;
import pt.isel.gape.access.service.UserService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.config.SupportedDocumentTypeCatalog;
import pt.isel.gape.common.config.SupportedLanguageCatalog;
import pt.isel.gape.security.crypto.PasswordHasher;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.web.media.ProfilePhotoStorage;
import pt.isel.gape.web.view.UserFormData;
import pt.isel.gape.web.view.UserView;

@WebServlet(name = "userManagementServlet", urlPatterns = {"/admin/users", "/admin/users/*"})
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 12 * 1024 * 1024)
public final class UserManagementServlet extends DashboardServletSupport {

    private static final String USERS_LIST_JSP = "/admin/admin/user/admin-users.jsp";
    private static final String USER_FORM_JSP = "/admin/admin/user/admin-user-form.jsp";
    private static final String USER_DETAIL_JSP = "/admin/admin/user/admin-user-detail.jsp";

    private final UserService userService;
    private final PermissionDAO permissionDAO;
    private final PasswordHasher passwordHasher;
    private final ProfilePhotoStorage profilePhotoStorage;

    public UserManagementServlet() {
        this(
                new UserService(ConnectionProvider.defaultProvider()),
                new PermissionDAO(ConnectionProvider.defaultProvider()),
                new PasswordHasher(),
                new ProfilePhotoStorage()
        );
    }

    UserManagementServlet(UserService userService, PasswordHasher passwordHasher) {
        this(userService, new PermissionDAO(ConnectionProvider.defaultProvider()), passwordHasher, new ProfilePhotoStorage());
    }

    UserManagementServlet(
            UserService userService,
            PermissionDAO permissionDAO,
            PasswordHasher passwordHasher,
            ProfilePhotoStorage profilePhotoStorage
    ) {
        this.userService = userService;
        this.permissionDAO = permissionDAO;
        this.passwordHasher = passwordHasher;
        this.profilePhotoStorage = profilePhotoStorage;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        if (segments.length == 0) {
            showList(request, response);
            return;
        }
        if (segments.length == 1 && "new".equals(segments[0])) {
            showCreateForm(request, response, UserFormData.blank(), null);
            return;
        }
        if (segments.length == 1) {
            showDetail(request, response, Long.parseLong(segments[0]));
            return;
        }
        if (segments.length == 2 && "edit".equals(segments[1])) {
            showEditForm(request, response, Long.parseLong(segments[0]), null);
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        if (segments.length == 0) {
            createUser(request, response);
            return;
        }
        if (segments.length == 1) {
            updateUser(request, response, Long.parseLong(segments[0]));
            return;
        }
        if (segments.length == 2) {
            long userId = Long.parseLong(segments[0]);
            switch (segments[1]) {
                case "block" -> blockUser(request, response, userId);
                case "unblock" -> unblockUser(request, response, userId);
                case "activate" -> activateUser(request, response, userId);
                case "inactivate" -> inactivateUser(request, response, userId);
                case "delete" -> deleteUser(request, response, userId);
                default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        List<UserView> users = userService.listUsers(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        request.getRemoteAddr()
                )
                .stream()
                .map(UserView::from)
                .toList();
        request.setAttribute("users", users);
        request.setAttribute("userCount", users.size());
        request.setAttribute("activeUsers", users.stream().filter(UserView::isActive).count());
        request.setAttribute("inactiveUsers", users.stream().filter(UserView::isInactive).count());
        request.setAttribute("blockedUsers", users.stream().filter(UserView::isBlocked).count());
        prepareDashboard(request, "users", "Users", "/admin/users/new", "New User");
        forward(request, response, USERS_LIST_JSP);
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response, long userId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        User user = userService.readPersonalData(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                userId,
                request.getRemoteAddr()
        );
        UserView userView = UserView.from(user);
        request.setAttribute("user", userView);
        prepareUserContext(request, userView, "detail");
        prepareDashboard(request, "users", "User Detail");
        forward(request, response, USER_DETAIL_JSP);
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response, UserFormData form, String error)
            throws ServletException, IOException {
        request.setAttribute("form", form);
        request.setAttribute("languageOptions", SupportedLanguageCatalog.all());
        request.setAttribute("documentTypeOptions", SupportedDocumentTypeCatalog.all());
        request.setAttribute("creating", Boolean.TRUE);
        request.setAttribute("formAction", request.getContextPath() + "/admin/users");
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareDashboard(request, "users", "Create User");
        forward(request, response, USER_FORM_JSP);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response, long userId, String error)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        User user = userService.readPersonalData(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                userId,
                request.getRemoteAddr()
        );
        UserFormData form = error == null ? UserFormData.from(user) : UserFormData.from(request, userId);
        request.setAttribute("form", form);
        request.setAttribute("languageOptions", SupportedLanguageCatalog.all());
        request.setAttribute("documentTypeOptions", SupportedDocumentTypeCatalog.all());
        request.setAttribute("creating", Boolean.FALSE);
        request.setAttribute("formAction", request.getContextPath() + "/admin/users/" + userId);
        prepareUserContext(request, UserView.from(user), "edit");
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareDashboard(request, "users", "Edit User");
        forward(request, response, USER_FORM_JSP);
    }

    private void createUser(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        UserFormData form = UserFormData.from(request, null);
        try {
            String password = required(text(request, "password"), "The initial password is required.");
            PasswordHasher.PasswordHash passwordHash = passwordHasher.createHash(password);
            User created = userService.createUser(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    new UserCreateCommand(
                            text(request, "name"),
                            text(request, "email"),
                            stateFrom(text(request, "state")),
                            text(request, "language"),
                            text(request, "photo"),
                            passwordHash.hashBase64(),
                            passwordHash.saltBase64(),
                            text(request, "documentType"),
                            text(request, "documentNumber"),
                            profilesFrom(request)
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "User created successfully.");
            redirect(request, response, "/admin/users/" + created.id());
        } catch (RuntimeException exception) {
            showCreateForm(request, response, form, messageFor(exception));
        }
    }

    private void updateUser(HttpServletRequest request, HttpServletResponse response, long userId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            User existing = userService.readPersonalData(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    userId,
                    request.getRemoteAddr()
            );
            String uploadedPhoto;
            try {
                uploadedPhoto = profilePhotoStorage.saveProfilePhoto(
                        userId,
                        profileImagePart(request),
                        getServletContext()
                );
            } catch (IOException exception) {
                showEditForm(
                        request,
                        response,
                        userId,
                        "The uploaded file could not be processed. Please try again."
                );
                return;
            }
            String submittedPhoto = text(request, "photo");
            String photo = uploadedPhoto != null ? uploadedPhoto : (submittedPhoto != null ? submittedPhoto : existing.photo());
            User updated = userService.updateUser(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    userId,
                    new UserUpdateCommand(
                            text(request, "name"),
                            text(request, "email"),
                            stateFrom(text(request, "state")),
                            text(request, "language"),
                            photo,
                            text(request, "documentType"),
                            text(request, "documentNumber"),
                            profilesFrom(request)
                    ),
                    request.getRemoteAddr()
            );
            if (actor.userId() == userId) {
                refreshCurrentUserAfterProfileChange(request, updated);
                flashSuccess(request, "User updated successfully.");
                redirect(request, response, "/dashboard");
                return;
            }
            flashSuccess(request, "User updated successfully.");
            redirect(request, response, "/admin/users/" + userId);
        } catch (RuntimeException exception) {
            showEditForm(request, response, userId, messageFor(exception));
        }
    }

    private static Part profileImagePart(HttpServletRequest request) throws IOException, ServletException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            return null;
        }
        return request.getPart("profileImage");
    }

    private void blockUser(HttpServletRequest request, HttpServletResponse response, long userId) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            if (actor.userId() == userId) {
                throw new IllegalArgumentException("You cannot block your own account.");
            }
            userService.blockUser(actor.userId(), currentSessionId(request), primaryProfile(actor), userId, request.getRemoteAddr());
            flashSuccess(request, "User blocked.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/admin/users");
    }

    private void unblockUser(HttpServletRequest request, HttpServletResponse response, long userId) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            userService.unblockUser(actor.userId(), currentSessionId(request), primaryProfile(actor), userId, request.getRemoteAddr());
            flashSuccess(request, "User unblocked.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/admin/users");
    }

    private void activateUser(HttpServletRequest request, HttpServletResponse response, long userId) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            userService.activateUser(actor.userId(), currentSessionId(request), primaryProfile(actor), userId, request.getRemoteAddr());
            flashSuccess(request, "User activated.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/admin/users");
    }

    private void inactivateUser(HttpServletRequest request, HttpServletResponse response, long userId) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            if (actor.userId() == userId) {
                throw new IllegalArgumentException("You cannot inactivate your own account.");
            }
            userService.inactivateUser(actor.userId(), currentSessionId(request), primaryProfile(actor), userId, request.getRemoteAddr());
            flashSuccess(request, "User inactivated.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/admin/users");
    }

    private void deleteUser(HttpServletRequest request, HttpServletResponse response, long userId) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            if (actor.userId() == userId) {
                throw new IllegalArgumentException("You cannot delete your own account.");
            }
            userService.deleteUser(actor.userId(), currentSessionId(request), primaryProfile(actor), userId, request.getRemoteAddr());
            flashSuccess(request, "User deleted.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/admin/users");
    }

    private static void prepareUserContext(HttpServletRequest request, UserView user, String activeChild) {
        request.setAttribute("adminUserContextId", user.getId());
        request.setAttribute("adminUserContextName", user.getName());
        request.setAttribute("adminUserActiveChild", activeChild);
    }

    private static UserState stateFrom(String value) {
        if (value == null || value.isBlank()) {
            return UserState.ACTIVE;
        }
        return UserState.valueOf(value.trim().toUpperCase());
    }

    private static Set<AccessProfile> profilesFrom(HttpServletRequest request) {
        Set<AccessProfile> profiles = new LinkedHashSet<>();
        addProfile(request, profiles, "administratorProfile", AccessProfileType.ADMINISTRATOR);
        addProfile(request, profiles, "coordinatorProfile", AccessProfileType.COORDINATOR);
        addProfile(request, profiles, "teacherProfile", AccessProfileType.TEACHER);
        addProfile(request, profiles, "studentProfile", AccessProfileType.STUDENT);
        if (profiles.isEmpty()) {
            throw new IllegalArgumentException("At least one access profile is required.");
        }
        return profiles;
    }

    private static void addProfile(
            HttpServletRequest request,
            Set<AccessProfile> profiles,
            String checkboxName,
            AccessProfileType type
    ) {
        if (request.getParameter(checkboxName) == null) {
            return;
        }
        profiles.add(new AccessProfile(type, ""));
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private void refreshCurrentUserAfterProfileChange(HttpServletRequest request, User updatedUser) {
        Map<AccessProfileType, Set<String>> permissionsByProfile = new EnumMap<>(AccessProfileType.class);
        for (AccessProfile accessProfile : updatedUser.accessProfiles()) {
            try {
                permissionsByProfile.put(
                        accessProfile.type(),
                        permissionDAO.findActivePermissionCodes(updatedUser.id(), accessProfile.type())
                );
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not refresh current user permissions", exception);
            }
        }
        sessionManager.refreshAuthenticatedSession(
                request,
                new SessionUser(
                        updatedUser.id(),
                        updatedUser.name(),
                        updatedUser.email(),
                        updatedUser.photo(),
                        updatedUser.accessProfiles().stream()
                                .map(AccessProfile::type)
                                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)),
                        permissionsByProfile
                )
        );
    }
}
