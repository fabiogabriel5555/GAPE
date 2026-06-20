package pt.isel.gape.web.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
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
import pt.isel.gape.access.model.AccessProfileContextAssignment;
import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AdministratorPermissionAssignment;
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
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectEnrollment;
import pt.isel.gape.learning.model.SubjectState;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.structure.model.ClassGroupContext;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.web.media.ProfilePhotoStorage;
import pt.isel.gape.web.view.AdminPermissionContextOptionView;
import pt.isel.gape.web.view.OrganizationView;
import pt.isel.gape.web.view.ProfileContextOptionView;
import pt.isel.gape.web.view.UserContextAssignmentView;
import pt.isel.gape.web.view.UserFormData;
import pt.isel.gape.web.view.UserView;

@WebServlet(name = "userManagementServlet", urlPatterns = {"/admin/users", "/admin/users/*"})
@MultipartConfig(maxFileSize = 50L * 1024L * 1024L, maxRequestSize = 52L * 1024L * 1024L)
public final class UserManagementServlet extends DashboardServletSupport {

    private static final String USERS_LIST_JSP = "/admin/admin/user/admin-users.jsp";
    private static final String USER_FORM_JSP = "/admin/admin/user/admin-user-form.jsp";
    private static final String USER_DETAIL_JSP = "/admin/admin/user/admin-user-detail.jsp";

    private final UserService userService;
    private final PermissionDAO permissionDAO;
    private final OrganizationDAO organizationDAO;
    private final OrganicUnitDAO organicUnitDAO;
    private final CourseDAO courseDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final SubjectDAO subjectDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final CoordinateSubjectDAO coordinateSubjectDAO;
    private final TeachClassGroupDAO teachClassGroupDAO;
    private final ManageOrganizationDAO manageOrganizationDAO;
    private final PasswordHasher passwordHasher;
    private final ProfilePhotoStorage profilePhotoStorage;

    public UserManagementServlet() {
        this(
                new UserService(ConnectionProvider.defaultProvider()),
                new PermissionDAO(ConnectionProvider.defaultProvider()),
                new OrganizationDAO(ConnectionProvider.defaultProvider()),
                new OrganicUnitDAO(ConnectionProvider.defaultProvider()),
                new CourseDAO(ConnectionProvider.defaultProvider()),
                new CourseSubjectDAO(ConnectionProvider.defaultProvider()),
                new SubjectDAO(ConnectionProvider.defaultProvider()),
                new EnrollmentDAO(ConnectionProvider.defaultProvider()),
                new CoordinateSubjectDAO(ConnectionProvider.defaultProvider()),
                new TeachClassGroupDAO(ConnectionProvider.defaultProvider()),
                new ManageOrganizationDAO(ConnectionProvider.defaultProvider()),
                new PasswordHasher(),
                new ProfilePhotoStorage()
        );
    }

    UserManagementServlet(UserService userService, PasswordHasher passwordHasher) {
        this(
                userService,
                new PermissionDAO(ConnectionProvider.defaultProvider()),
                new OrganizationDAO(ConnectionProvider.defaultProvider()),
                new OrganicUnitDAO(ConnectionProvider.defaultProvider()),
                new CourseDAO(ConnectionProvider.defaultProvider()),
                new CourseSubjectDAO(ConnectionProvider.defaultProvider()),
                new SubjectDAO(ConnectionProvider.defaultProvider()),
                new EnrollmentDAO(ConnectionProvider.defaultProvider()),
                new CoordinateSubjectDAO(ConnectionProvider.defaultProvider()),
                new TeachClassGroupDAO(ConnectionProvider.defaultProvider()),
                new ManageOrganizationDAO(ConnectionProvider.defaultProvider()),
                passwordHasher,
                new ProfilePhotoStorage()
        );
    }

    UserManagementServlet(
            UserService userService,
            PermissionDAO permissionDAO,
            OrganizationDAO organizationDAO,
            OrganicUnitDAO organicUnitDAO,
            CourseDAO courseDAO,
            CourseSubjectDAO courseSubjectDAO,
            SubjectDAO subjectDAO,
            EnrollmentDAO enrollmentDAO,
            CoordinateSubjectDAO coordinateSubjectDAO,
            TeachClassGroupDAO teachClassGroupDAO,
            ManageOrganizationDAO manageOrganizationDAO,
            PasswordHasher passwordHasher,
            ProfilePhotoStorage profilePhotoStorage
    ) {
        this.userService = userService;
        this.permissionDAO = permissionDAO;
        this.organizationDAO = organizationDAO;
        this.organicUnitDAO = organicUnitDAO;
        this.courseDAO = courseDAO;
        this.courseSubjectDAO = courseSubjectDAO;
        this.subjectDAO = subjectDAO;
        this.enrollmentDAO = enrollmentDAO;
        this.coordinateSubjectDAO = coordinateSubjectDAO;
        this.teachClassGroupDAO = teachClassGroupDAO;
        this.manageOrganizationDAO = manageOrganizationDAO;
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
        request.setAttribute("accessProfileDetails", accessProfileDetails(user));
        request.setAttribute("administratorPermissionDetails", administratorPermissionDetails(userId));
        request.setAttribute("profileContextDetails", profileContextDetails(userId));
        prepareUserContext(request, userView, "detail");
        prepareDashboard(request, "users", "User Detail");
        forward(request, response, USER_DETAIL_JSP);
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response, UserFormData form, String error)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        request.setAttribute("form", form);
        request.setAttribute("languageOptions", SupportedLanguageCatalog.all());
        request.setAttribute("documentTypeOptions", SupportedDocumentTypeCatalog.all());
        prepareAdminPermissionOptions(request, actor);
        prepareProfileContextOptions(request, actor);
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
        UserFormData form = error == null
                ? UserFormData.from(
                        user,
                        activeOrganizationIds(userId),
                        activeAdminPermissionAssignments(userId),
                        activeProfileContextAssignments(userId)
                )
                : UserFormData.from(request, userId);
        request.setAttribute("form", form);
        request.setAttribute("languageOptions", SupportedLanguageCatalog.all());
        request.setAttribute("documentTypeOptions", SupportedDocumentTypeCatalog.all());
        prepareAdminPermissionOptions(request, actor);
        prepareProfileContextOptions(request, actor);
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
        Part profileImage;
        try {
            profileImage = profileImagePart(request);
        } catch (IOException | ServletException exception) {
            showCreateForm(request, response, form, "The uploaded file could not be processed. Please try again.");
            return;
        }
        try {
            String password = required(text(request, "password"), "The initial password is required.");
            validateInitialPassword(password);
            PasswordHasher.PasswordHash passwordHash = passwordHasher.createHash(password);
            List<AdministratorPermissionAssignment> adminPermissionAssignments = form.getAdminPermissionAssignments().stream().toList();
            List<AccessProfileContextAssignment> profileContextAssignments = form.getProfileContextAssignments().stream().toList();
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
                    adminPermissionAssignments,
                    profileContextAssignments,
                    request.getRemoteAddr()
            );
            try {
                created = attachUploadedPhotoToCreatedUser(request, actor, created, profileImage);
            } catch (IOException exception) {
                flashError(request, "User created, but the uploaded image could not be processed. Please edit the user and try again.");
                redirect(request, response, "/admin/users/" + created.id());
                return;
            } catch (RuntimeException exception) {
                flashError(request, "User created, but " + messageFor(exception));
                redirect(request, response, "/admin/users/" + created.id());
                return;
            }
            flashSuccess(request, "User created successfully.");
            redirect(request, response, "/admin/users/" + created.id());
        } catch (RuntimeException exception) {
            showCreateForm(request, response, form, messageFor(exception));
        }
    }

    private User attachUploadedPhotoToCreatedUser(
            HttpServletRequest request,
            SessionUser actor,
            User created,
            Part profileImage
    ) throws IOException {
        String uploadedPhoto = profilePhotoStorage.saveProfilePhoto(
                created.id(),
                profileImage,
                getServletContext()
        );
        if (uploadedPhoto == null) {
            return created;
        }
        return userService.attachCreatedUserPhoto(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                created.id(),
                uploadedPhoto,
                request.getRemoteAddr()
        );
    }

    private void updateUser(HttpServletRequest request, HttpServletResponse response, long userId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        UserFormData submittedForm = UserFormData.from(request, userId);
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
            } catch (IOException | ServletException exception) {
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
                    submittedForm.getAdminPermissionAssignments().stream().toList(),
                    submittedForm.getProfileContextAssignments().stream().toList(),
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

    private List<UserContextAssignmentView> accessProfileDetails(User user) {
        return user.accessProfiles().stream()
                .sorted(Comparator.comparing(profile -> profile.type().ordinal()))
                .map(profile -> new UserContextAssignmentView(
                        "Access Profile",
                        profileLabel(profile.type()),
                        profile.code()
                ))
                .toList();
    }

    private List<UserContextAssignmentView> administratorPermissionDetails(long userId) throws ServletException {
        try {
            List<UserContextAssignmentView> details = new ArrayList<>();
            List<AdministratorPermissionAssignment> assignments = permissionDAO.findActiveAdministratorAssignments(userId).stream()
                    .sorted(Comparator
                            .comparing(AdministratorPermissionAssignment::permissionCode)
                            .thenComparing(assignment -> assignment.contextType().name())
                            .thenComparingLong(AdministratorPermissionAssignment::contextId))
                    .toList();
            for (AdministratorPermissionAssignment assignment : assignments) {
                if (isSubjectAssignmentCoveredByCourseAssignment(assignment, assignments)) {
                    continue;
                }
                details.add(new UserContextAssignmentView(
                            "Administrator Permission",
                            adminPermissionLabel(assignment.permissionCode()),
                            adminContextDetail(assignment)
                ));
            }
            return details;
        } catch (SQLException exception) {
            throw new ServletException("Could not load administrator permission details", exception);
        }
    }

    private boolean isSubjectAssignmentCoveredByCourseAssignment(
            AdministratorPermissionAssignment assignment,
            List<AdministratorPermissionAssignment> assignments
    ) throws SQLException {
        if (assignment.contextType() != AccessEntityType.SUBJECT) {
            return false;
        }
        Set<Long> assignedCourseIds = new HashSet<>();
        for (AdministratorPermissionAssignment candidate : assignments) {
            if (candidate.permissionCode().equals(assignment.permissionCode())
                    && candidate.contextType() == AccessEntityType.COURSE) {
                assignedCourseIds.add(candidate.contextId());
            }
        }
        if (assignedCourseIds.isEmpty()) {
            return false;
        }
        return courseSubjectDAO.findBySubject(assignment.contextId()).stream()
                .map(CourseSubjectAssociation::courseId)
                .anyMatch(assignedCourseIds::contains);
    }

    private List<UserContextAssignmentView> profileContextDetails(long userId) throws ServletException {
        try {
            List<UserContextAssignmentView> details = new ArrayList<>();
            for (AccessProfileContextAssignment assignment : activeProfileContextAssignments(userId)) {
                details.add(new UserContextAssignmentView(
                        "Profile Context",
                        profileLabel(assignment.profileType()),
                        profileContextDetail(assignment),
                        profileContextDetailHtml(assignment)
                ));
            }
            details.sort(Comparator
                    .comparing(UserContextAssignmentView::getLabel)
                    .thenComparing(UserContextAssignmentView::getDetail));
            return details;
        } catch (SQLException exception) {
            throw new ServletException("Could not load profile context details", exception);
        }
    }

    private String adminContextDetail(AdministratorPermissionAssignment assignment) throws SQLException {
        if (assignment.contextType() == AccessEntityType.GLOBAL) {
            return "Global";
        }
        return contextLabel(assignment.contextType(), assignment.contextId(), null);
    }

    private String profileContextDetail(AccessProfileContextAssignment assignment) throws SQLException {
        return contextLabel(assignment.contextType(), assignment.contextId(), assignment.parentContextId());
    }

    private String profileContextDetailHtml(AccessProfileContextAssignment assignment) throws SQLException {
        return contextLabelHtml(assignment.contextType(), assignment.contextId(), assignment.parentContextId());
    }

    private String contextLabel(AccessEntityType contextType, long contextId, Long parentContextId)
            throws SQLException {
        return switch (contextType) {
            case ORGANIZATION -> organizationDAO.findById(contextId)
                    .map(organization -> "Organization: " + organization.name())
                    .orElse("Organization #" + contextId);
            case ORGANIC_UNIT -> {
                OrganicUnit unit = organicUnitDAO.findById(contextId).orElse(null);
                yield unit == null
                        ? "Organic unit #" + contextId
                        : "Organic unit: " + unit.name() + " | " + organizationToken(unit.organizationId());
            }
            case COURSE -> {
                Course course = courseDAO.findById(contextId).orElse(null);
                yield course == null
                        ? "Course #" + contextId
                        : "Course: " + course.name() + compactCourseAncestors(course);
            }
            case SUBJECT -> {
                Subject subject = subjectDAO.findById(contextId).orElse(null);
                String subjectLabel = subject == null ? "Subject #" + contextId : "Subject: " + subject.name();
                if (parentContextId == null || parentContextId <= 0) {
                    yield subject == null ? subjectLabel : subjectLabel + " | " + organizationToken(subject.organizationId());
                }
                Course course = courseDAO.findById(parentContextId).orElse(null);
                yield subjectLabel + (course == null
                        ? " | Course #" + parentContextId
                        : " | " + acronymOrName(course.acronym(), course.name()) + " | " + organizationToken(course.organizationId()));
            }
            case CLASS_GROUP -> {
                ClassGroupContext classGroup = teachClassGroupDAO.requireContext(contextId);
                yield "Class group: " + classGroup.label()
                        + " | " + acronymOrName(classGroup.subjectAcronym(), classGroup.subjectName())
                        + " | " + acronymOrName(classGroup.courseAcronym(), classGroup.courseName())
                        + " | " + acronymOrName(classGroup.organizationAcronym(), classGroup.organizationName());
            }
            case GLOBAL -> "Global";
            case SELF -> "Self";
        };
    }

    private String contextLabelHtml(AccessEntityType contextType, long contextId, Long parentContextId)
            throws SQLException {
        return switch (contextType) {
            case ORGANIZATION -> organizationDAO.findById(contextId)
                    .map(organization -> "Organization: " + escapeHtml(organization.name()))
                    .orElse("Organization #" + contextId);
            case ORGANIC_UNIT -> {
                OrganicUnit unit = organicUnitDAO.findById(contextId).orElse(null);
                yield unit == null
                        ? "Organic unit #" + contextId
                        : "Organic unit: " + escapeHtml(unit.name()) + " | " + organizationTokenHtml(unit.organizationId());
            }
            case COURSE -> {
                Course course = courseDAO.findById(contextId).orElse(null);
                yield course == null
                        ? "Course #" + contextId
                        : "Course: " + escapeHtml(course.name()) + compactCourseAncestorsHtml(course);
            }
            case SUBJECT -> {
                Subject subject = subjectDAO.findById(contextId).orElse(null);
                String subjectLabel = subject == null ? "Subject #" + contextId : "Subject: " + escapeHtml(subject.name());
                if (parentContextId == null || parentContextId <= 0) {
                    yield subject == null ? subjectLabel : subjectLabel + " | " + organizationTokenHtml(subject.organizationId());
                }
                Course course = courseDAO.findById(parentContextId).orElse(null);
                yield subjectLabel + (course == null
                        ? " | Course #" + parentContextId
                        : " | " + acronymToken(course.acronym(), course.name()) + " | " + organizationTokenHtml(course.organizationId()));
            }
            case CLASS_GROUP -> {
                ClassGroupContext classGroup = teachClassGroupDAO.requireContext(contextId);
                yield "Class group: " + escapeHtml(classGroup.label())
                        + " | " + acronymToken(classGroup.subjectAcronym(), classGroup.subjectName())
                        + " | " + acronymToken(classGroup.courseAcronym(), classGroup.courseName())
                        + " | " + acronymToken(classGroup.organizationAcronym(), classGroup.organizationName());
            }
            case GLOBAL -> "Global";
            case SELF -> "Self";
        };
    }

    private String compactCourseAncestors(Course course) throws SQLException {
        StringBuilder label = new StringBuilder();
        if (course.organicUnitId() != null) {
            organicUnitDAO.findById(course.organicUnitId()).ifPresent(unit ->
                    label.append(" | ").append(acronymOrName(unit.acronym(), unit.name())));
        }
        label.append(" | ").append(organizationToken(course.organizationId()));
        return label.toString();
    }

    private String compactCourseAncestorsHtml(Course course) throws SQLException {
        StringBuilder label = new StringBuilder();
        if (course.organicUnitId() != null) {
            organicUnitDAO.findById(course.organicUnitId()).ifPresent(unit ->
                    label.append(" | ").append(acronymToken(unit.acronym(), unit.name())));
        }
        label.append(" | ").append(organizationTokenHtml(course.organizationId()));
        return label.toString();
    }

    private String organizationToken(long organizationId) throws SQLException {
        return organizationDAO.findById(organizationId)
                .map(organization -> acronymOrName(organization.acronym(), organization.name()))
                .orElse("Organization #" + organizationId);
    }

    private String organizationTokenHtml(long organizationId) throws SQLException {
        return organizationDAO.findById(organizationId)
                .map(organization -> acronymToken(organization.acronym(), organization.name()))
                .orElse("Organization #" + organizationId);
    }

    private static String acronymOrName(String acronym, String name) {
        return acronym == null || acronym.isBlank() ? name : acronym;
    }

    private static String acronymToken(String acronym, String name) {
        if (acronym == null || acronym.isBlank()) {
            return escapeHtml(name);
        }
        return "<span class=\"gape-acronym-token\" tabindex=\"0\" title=\""
                + escapeHtml(name) + "\">" + escapeHtml(acronym) + "</span>";
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String adminPermissionLabel(String permissionCode) {
        return switch (permissionCode) {
            case AuthorizationPolicy.MANAGE_ALL -> "Full access";
            case AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE -> "Organization structure";
            case AuthorizationPolicy.MANAGE_LEARNING -> "Learning";
            case AuthorizationPolicy.MANAGE_ENROLLMENTS -> "Enrollments";
            default -> permissionCode;
        };
    }

    private static String profileLabel(AccessProfileType profileType) {
        return switch (profileType) {
            case ADMINISTRATOR -> "Administrator";
            case COORDINATOR -> "Coordinator";
            case TEACHER -> "Teacher";
            case STUDENT -> "Student";
        };
    }

    private List<OrganizationView> managedOrganizationOptions(SessionUser actor) throws ServletException {
        try {
            return organizationDAO.findByAdministrator(actor.userId()).stream()
                    .map(organization -> OrganizationView.from(organization, 0))
                    .toList();
        } catch (SQLException exception) {
            throw new ServletException("Could not load managed organizations", exception);
        }
    }

    private void prepareAdminPermissionOptions(HttpServletRequest request, SessionUser actor) throws ServletException {
        List<Organization> organizations = assignableOrganizations(actor);
        request.setAttribute("manageAllPermissionCode", AuthorizationPolicy.MANAGE_ALL);
        request.setAttribute("manageOrganizationStructurePermissionCode", AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE);
        request.setAttribute("organizationStructureContextOptions", organizationStructureContextOptions(organizations));
    }

    private void prepareProfileContextOptions(HttpServletRequest request, SessionUser actor) throws ServletException {
        List<Organization> organizations = assignableOrganizations(actor);
        request.setAttribute("coordinatorContextOptions", coordinatorContextOptions(organizations));
        request.setAttribute("teacherContextOptions", teacherContextOptions(organizations));
        request.setAttribute("studentContextOptions", studentContextOptions(organizations));
    }

    private List<Organization> assignableOrganizations(SessionUser actor)
            throws ServletException {
        try {
            if (actor.hasPermission(AuthorizationPolicy.MANAGE_ALL)) {
                return organizationDAO.findActive();
            }
            return organizationDAO.findByAdministrator(actor.userId());
        } catch (SQLException exception) {
            throw new ServletException("Could not load assignable organizations", exception);
        }
    }

    private List<AdminPermissionContextOptionView> organizationStructureContextOptions(
            List<Organization> organizations
    ) throws ServletException {
        try {
            List<AdminPermissionContextOptionView> options = new ArrayList<>();
            for (Organization organization : organizations) {
                appendOrganizationOption(
                        options,
                        AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE,
                        organization,
                        "Organization"
                );
                appendOrganicUnitOptions(
                        options,
                        AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE,
                        organization.id(),
                        organicUnitDAO.findByOrganization(organization.id())
                );
            }
            return options;
        } catch (SQLException exception) {
            throw new ServletException("Could not load organization permission contexts", exception);
        }
    }

    private List<ProfileContextOptionView> coordinatorContextOptions(List<Organization> organizations)
            throws ServletException {
        try {
            List<ProfileContextOptionView> options = new ArrayList<>();
            for (Organization organization : organizations) {
                List<Course> courses = activeProfileCourses(organization.id());
                Map<Long, List<Subject>> subjectsByCourse = activeSubjectsByCourse(courses);
                boolean hasSubjects = subjectsByCourse.values().stream().anyMatch(subjects -> !subjects.isEmpty());
                if (!hasSubjects) {
                    continue;
                }
                String organizationKey = profileNodeKey("COORDINATOR_ORGANIZATION", organization.id());
                options.add(profileHeading(
                        AccessProfileType.COORDINATOR,
                        AccessEntityType.ORGANIZATION,
                        organization.id(),
                        organization.name(),
                        "Organization",
                        organizationKey,
                        "",
                        0
                ));
                for (Course course : courses) {
                    List<Subject> subjects = subjectsByCourse.getOrDefault(course.id(), List.of());
                    if (subjects.isEmpty()) {
                        continue;
                    }
                    String courseKey = profileNodeKey("COORDINATOR_COURSE", course.id());
                    options.add(profileHeading(
                            AccessProfileType.COORDINATOR,
                            AccessEntityType.COURSE,
                            course.id(),
                            courseProfileLabel(course),
                            "Course",
                            courseKey,
                            organizationKey,
                            1
                    ));
                    for (Subject subject : subjects) {
                        options.add(new ProfileContextOptionView(
                                AccessProfileType.COORDINATOR,
                                AccessEntityType.SUBJECT,
                                subject.id(),
                                null,
                                subject.name(),
                                "Subject",
                                profileNodeKey("COORDINATOR_COURSE_SUBJECT", course.id() + ":" + subject.id()),
                                courseKey,
                                2,
                                true
                        ));
                    }
                }
            }
            return options;
        } catch (SQLException exception) {
            throw new ServletException("Could not load coordinator profile contexts", exception);
        }
    }

    private List<ProfileContextOptionView> teacherContextOptions(List<Organization> organizations)
            throws ServletException {
        try {
            List<Long> organizationIds = organizations.stream().map(Organization::id).toList();
            Map<Long, Organization> organizationById = new HashMap<>();
            for (Organization organization : organizations) {
                organizationById.put(organization.id(), organization);
            }
            List<ProfileContextOptionView> options = new ArrayList<>();
            Set<String> emittedHeadings = new HashSet<>();
            for (ClassGroupContext classGroup : teachClassGroupDAO.findActiveByOrganizations(organizationIds)) {
                Organization organization = organizationById.get(classGroup.organizationId());
                if (organization == null) {
                    continue;
                }
                String organizationKey = profileNodeKey("TEACHER_ORGANIZATION", organization.id());
                if (emittedHeadings.add(organizationKey)) {
                    options.add(profileHeading(
                            AccessProfileType.TEACHER,
                            AccessEntityType.ORGANIZATION,
                            organization.id(),
                            organization.name(),
                            "Organization",
                            organizationKey,
                            "",
                            0
                    ));
                }
                String courseKey = profileNodeKey("TEACHER_COURSE", classGroup.courseId());
                if (emittedHeadings.add(courseKey)) {
                    options.add(profileHeading(
                            AccessProfileType.TEACHER,
                            AccessEntityType.COURSE,
                            classGroup.courseId(),
                            classGroup.courseName(),
                            "Course",
                            courseKey,
                            organizationKey,
                            1
                    ));
                }
                String subjectKey = profileNodeKey(
                        "TEACHER_COURSE_SUBJECT",
                        classGroup.courseId() + ":" + classGroup.subjectId()
                );
                if (emittedHeadings.add(subjectKey)) {
                    options.add(profileHeading(
                            AccessProfileType.TEACHER,
                            AccessEntityType.SUBJECT,
                            classGroup.subjectId(),
                            classGroup.subjectName(),
                            "Subject",
                            subjectKey,
                            courseKey,
                            2
                    ));
                }
                options.add(new ProfileContextOptionView(
                        AccessProfileType.TEACHER,
                        AccessEntityType.CLASS_GROUP,
                        classGroup.id(),
                        null,
                        classGroup.label(),
                        classGroup.detail(),
                        profileNodeKey("TEACHER_CLASS_GROUP", classGroup.id()),
                        subjectKey,
                        3,
                        true
                ));
            }
            return options;
        } catch (SQLException exception) {
            throw new ServletException("Could not load teacher profile contexts", exception);
        }
    }

    private List<ProfileContextOptionView> studentContextOptions(List<Organization> organizations)
            throws ServletException {
        try {
            List<ProfileContextOptionView> options = new ArrayList<>();
            for (Organization organization : organizations) {
                List<Course> courses = activeProfileCourses(organization.id());
                if (courses.isEmpty()) {
                    continue;
                }
                String organizationKey = profileNodeKey("STUDENT_ORGANIZATION", organization.id());
                options.add(profileHeading(
                        AccessProfileType.STUDENT,
                        AccessEntityType.ORGANIZATION,
                        organization.id(),
                        organization.name(),
                        "Organization",
                        organizationKey,
                        "",
                        0
                ));
                Map<Long, List<Subject>> subjectsByCourse = activeSubjectsByCourse(courses);
                for (Course course : courses) {
                    String courseKey = profileNodeKey("STUDENT_COURSE", course.id());
                    options.add(new ProfileContextOptionView(
                            AccessProfileType.STUDENT,
                            AccessEntityType.COURSE,
                            course.id(),
                            null,
                            course.name(),
                            "Course",
                            courseKey,
                            organizationKey,
                            1,
                            true
                    ));
                    for (Subject subject : subjectsByCourse.getOrDefault(course.id(), List.of())) {
                        options.add(new ProfileContextOptionView(
                                AccessProfileType.STUDENT,
                                AccessEntityType.SUBJECT,
                                subject.id(),
                                course.id(),
                                subject.name(),
                                "Subject",
                                profileNodeKey("STUDENT_COURSE_SUBJECT", course.id() + ":" + subject.id()),
                                courseKey,
                                2,
                                true
                        ));
                    }
                }
            }
            return options;
        } catch (SQLException exception) {
            throw new ServletException("Could not load student profile contexts", exception);
        }
    }

    private List<Course> activeCourses(long organizationId) throws SQLException {
        return courseDAO.findByOrganization(organizationId).stream()
                .filter(course -> course.state() != CourseState.ARCHIVED)
                .sorted(Comparator.comparing(Course::name))
                .toList();
    }

    private List<Course> activeProfileCourses(long organizationId) throws SQLException {
        return courseDAO.findByOrganization(organizationId).stream()
                .filter(course -> course.state() == CourseState.ACTIVE)
                .sorted(Comparator.comparing(Course::name))
                .toList();
    }

    private Map<Long, List<Subject>> activeSubjectsByCourse(List<Course> courses) throws SQLException {
        Map<Long, List<Subject>> subjectsByCourse = new HashMap<>();
        for (Course course : courses) {
            List<Subject> subjects = new ArrayList<>();
            for (CourseSubjectAssociation association : courseSubjectDAO.findActiveByCourse(course.id())) {
                subjectDAO.findById(association.subjectId())
                        .filter(subject -> subject.state() == SubjectState.ACTIVE)
                        .ifPresent(subjects::add);
            }
            subjects.sort(Comparator.comparing(Subject::name));
            subjectsByCourse.put(course.id(), subjects);
        }
        return subjectsByCourse;
    }

    private static String courseProfileLabel(Course course) {
        if (course.acronym() == null || course.acronym().isBlank()) {
            return course.name();
        }
        return course.name() + " (" + course.acronym() + ")";
    }

    private Map<String, List<ClassGroupContext>> activeClassGroupsByCourseSubject(List<Long> organizationIds)
            throws SQLException {
        Map<String, List<ClassGroupContext>> classGroupsByCourseSubject = new HashMap<>();
        for (ClassGroupContext classGroup : teachClassGroupDAO.findActiveByOrganizations(organizationIds)) {
            classGroupsByCourseSubject
                    .computeIfAbsent(courseSubjectKey(classGroup.courseId(), classGroup.subjectId()), ignored -> new ArrayList<>())
                    .add(classGroup);
        }
        classGroupsByCourseSubject.values().forEach(classGroups -> classGroups.sort(Comparator
                .comparing(ClassGroupContext::courseName)
                .thenComparing(ClassGroupContext::subjectName)
                .thenComparing(ClassGroupContext::label)));
        return classGroupsByCourseSubject;
    }

    private static void appendOrganizationOption(
            List<AdminPermissionContextOptionView> options,
            String permissionCode,
            Organization organization,
            String detail
    ) {
        options.add(new AdminPermissionContextOptionView(
                permissionCode,
                "ORGANIZATION",
                organization.id(),
                organization.name(),
                detail,
                "",
                0
        ));
    }

    private static void appendOrganizationHeading(
            List<AdminPermissionContextOptionView> options,
            String permissionCode,
            Organization organization,
            String detail
    ) {
        options.add(new AdminPermissionContextOptionView(
                permissionCode,
                "ORGANIZATION",
                organization.id(),
                organization.name(),
                detail,
                nodeKey("ORGANIZATION", organization.id()),
                "",
                0,
                false
        ));
    }

    private static ProfileContextOptionView profileHeading(
            AccessProfileType profileType,
            AccessEntityType contextType,
            long contextId,
            String label,
            String detail,
            String nodeKey,
            String parentKey,
            int depth
    ) {
        return new ProfileContextOptionView(
                profileType,
                contextType,
                contextId,
                null,
                label,
                detail,
                nodeKey,
                parentKey,
                depth,
                false
        );
    }

    private static void appendOrganicUnitOptions(
            List<AdminPermissionContextOptionView> options,
            String permissionCode,
            long organizationId,
            List<OrganicUnit> units
    ) {
        Map<Long, List<OrganicUnit>> childrenByParent = childrenByParent(units);
        Set<Long> visited = new HashSet<>();
        appendOrganicUnitChildren(
                options,
                permissionCode,
                0L,
                nodeKey("ORGANIZATION", organizationId),
                1,
                childrenByParent,
                visited
        );
        for (OrganicUnit unit : units.stream().sorted(unitComparator()).toList()) {
            if (!visited.contains(unit.id())) {
                appendOrganicUnitBranch(
                        options,
                        permissionCode,
                        unit,
                        nodeKey("ORGANIZATION", organizationId),
                        1,
                        childrenByParent,
                        visited
                );
            }
        }
    }

    private static void appendLearningScopeOptions(
            List<AdminPermissionContextOptionView> options,
            String permissionCode,
            long organizationId,
            List<OrganicUnit> units,
            List<Course> courses
    ) {
        Map<Long, List<OrganicUnit>> childrenByParent = childrenByParent(units);
        Map<Long, List<Course>> coursesByUnit = coursesByUnit(courses);
        Set<Long> visited = new HashSet<>();
        String organizationKey = nodeKey("ORGANIZATION", organizationId);

        appendCourses(options, permissionCode, coursesByUnit.get(0L), organizationKey, 1);
        appendLearningUnitChildren(
                options,
                permissionCode,
                0L,
                organizationKey,
                1,
                childrenByParent,
                coursesByUnit,
                visited
        );
        for (OrganicUnit unit : units.stream().sorted(unitComparator()).toList()) {
            if (!visited.contains(unit.id()) && branchHasCourses(unit.id(), childrenByParent, coursesByUnit)) {
                appendLearningUnitBranch(
                        options,
                        permissionCode,
                        unit,
                        organizationKey,
                        1,
                        childrenByParent,
                        coursesByUnit,
                        visited
                );
            }
        }
    }

    private static void appendEnrollmentScopeOptions(
            List<AdminPermissionContextOptionView> options,
            String permissionCode,
            long organizationId,
            List<OrganicUnit> units,
            List<Course> courses,
            Map<Long, List<Subject>> subjectsByCourse,
            Map<String, List<ClassGroupContext>> classGroupsByCourseSubject
    ) {
        Map<Long, List<OrganicUnit>> childrenByParent = childrenByParent(units);
        Map<Long, List<Course>> coursesByUnit = coursesByUnit(courses);
        Set<Long> visited = new HashSet<>();
        String organizationKey = nodeKey("ORGANIZATION", organizationId);

        appendEnrollmentCourses(
                options,
                permissionCode,
                coursesByUnit.get(0L),
                organizationKey,
                1,
                subjectsByCourse,
                classGroupsByCourseSubject
        );
        appendEnrollmentUnitChildren(
                options,
                permissionCode,
                0L,
                organizationKey,
                1,
                childrenByParent,
                coursesByUnit,
                subjectsByCourse,
                classGroupsByCourseSubject,
                visited
        );
        for (OrganicUnit unit : units.stream().sorted(unitComparator()).toList()) {
            if (!visited.contains(unit.id()) && branchHasCourses(unit.id(), childrenByParent, coursesByUnit)) {
                appendEnrollmentUnitBranch(
                        options,
                        permissionCode,
                        unit,
                        organizationKey,
                        1,
                        childrenByParent,
                        coursesByUnit,
                        subjectsByCourse,
                        classGroupsByCourseSubject,
                        visited
                );
            }
        }
    }

    private static void appendOrganicUnitChildren(
            List<AdminPermissionContextOptionView> options,
            String permissionCode,
            long parentId,
            String parentKey,
            int depth,
            Map<Long, List<OrganicUnit>> childrenByParent,
            Set<Long> visited
    ) {
        for (OrganicUnit unit : childrenByParent.getOrDefault(parentId, List.of())) {
            appendOrganicUnitBranch(options, permissionCode, unit, parentKey, depth, childrenByParent, visited);
        }
    }

    private static void appendOrganicUnitBranch(
            List<AdminPermissionContextOptionView> options,
            String permissionCode,
            OrganicUnit unit,
            String parentKey,
            int depth,
            Map<Long, List<OrganicUnit>> childrenByParent,
            Set<Long> visited
    ) {
        if (!visited.add(unit.id())) {
            return;
        }
        options.add(new AdminPermissionContextOptionView(
                permissionCode,
                "ORGANIC_UNIT",
                unit.id(),
                unit.name(),
                "Organic unit",
                parentKey,
                depth
        ));
        appendOrganicUnitChildren(
                options,
                permissionCode,
                unit.id(),
                nodeKey("ORGANIC_UNIT", unit.id()),
                depth + 1,
                childrenByParent,
                visited
        );
    }

    private static void appendLearningUnitChildren(
            List<AdminPermissionContextOptionView> options,
            String permissionCode,
            long parentId,
            String parentKey,
            int depth,
            Map<Long, List<OrganicUnit>> childrenByParent,
            Map<Long, List<Course>> coursesByUnit,
            Set<Long> visited
    ) {
        for (OrganicUnit unit : childrenByParent.getOrDefault(parentId, List.of())) {
            if (!branchHasCourses(unit.id(), childrenByParent, coursesByUnit)) {
                continue;
            }
            appendLearningUnitBranch(
                    options,
                    permissionCode,
                    unit,
                    parentKey,
                    depth,
                    childrenByParent,
                    coursesByUnit,
                    visited
            );
        }
    }

    private static void appendLearningUnitBranch(
            List<AdminPermissionContextOptionView> options,
            String permissionCode,
            OrganicUnit unit,
            String parentKey,
            int depth,
            Map<Long, List<OrganicUnit>> childrenByParent,
            Map<Long, List<Course>> coursesByUnit,
            Set<Long> visited
    ) {
        if (!visited.add(unit.id())) {
            return;
        }
        String unitKey = nodeKey("ORGANIC_UNIT", unit.id());
        options.add(new AdminPermissionContextOptionView(
                permissionCode,
                "ORGANIC_UNIT",
                unit.id(),
                unit.name(),
                "Organic unit",
                parentKey,
                depth
        ));
        appendCourses(options, permissionCode, coursesByUnit.get(unit.id()), unitKey, depth + 1);
        appendLearningUnitChildren(
                options,
                permissionCode,
                unit.id(),
                unitKey,
                depth + 1,
                childrenByParent,
                coursesByUnit,
                visited
        );
    }

    private static void appendEnrollmentUnitChildren(
            List<AdminPermissionContextOptionView> options,
            String permissionCode,
            long parentId,
            String parentKey,
            int depth,
            Map<Long, List<OrganicUnit>> childrenByParent,
            Map<Long, List<Course>> coursesByUnit,
            Map<Long, List<Subject>> subjectsByCourse,
            Map<String, List<ClassGroupContext>> classGroupsByCourseSubject,
            Set<Long> visited
    ) {
        for (OrganicUnit unit : childrenByParent.getOrDefault(parentId, List.of())) {
            if (!branchHasCourses(unit.id(), childrenByParent, coursesByUnit)) {
                continue;
            }
            appendEnrollmentUnitBranch(
                    options,
                    permissionCode,
                    unit,
                    parentKey,
                    depth,
                    childrenByParent,
                    coursesByUnit,
                    subjectsByCourse,
                    classGroupsByCourseSubject,
                    visited
            );
        }
    }

    private static void appendEnrollmentUnitBranch(
            List<AdminPermissionContextOptionView> options,
            String permissionCode,
            OrganicUnit unit,
            String parentKey,
            int depth,
            Map<Long, List<OrganicUnit>> childrenByParent,
            Map<Long, List<Course>> coursesByUnit,
            Map<Long, List<Subject>> subjectsByCourse,
            Map<String, List<ClassGroupContext>> classGroupsByCourseSubject,
            Set<Long> visited
    ) {
        if (!visited.add(unit.id())) {
            return;
        }
        String unitKey = nodeKey("ORGANIC_UNIT", unit.id());
        options.add(new AdminPermissionContextOptionView(
                permissionCode,
                "ORGANIC_UNIT",
                unit.id(),
                unit.name(),
                "Organic unit",
                unitKey,
                parentKey,
                depth,
                false
        ));
        appendEnrollmentCourses(
                options,
                permissionCode,
                coursesByUnit.get(unit.id()),
                unitKey,
                depth + 1,
                subjectsByCourse,
                classGroupsByCourseSubject
        );
        appendEnrollmentUnitChildren(
                options,
                permissionCode,
                unit.id(),
                unitKey,
                depth + 1,
                childrenByParent,
                coursesByUnit,
                subjectsByCourse,
                classGroupsByCourseSubject,
                visited
        );
    }

    private static void appendCourses(
            List<AdminPermissionContextOptionView> options,
            String permissionCode,
            List<Course> courses,
            String parentKey,
            int depth
    ) {
        appendCourses(options, permissionCode, courses, parentKey, depth, Map.of());
    }

    private static void appendCourses(
            List<AdminPermissionContextOptionView> options,
            String permissionCode,
            List<Course> courses,
            String parentKey,
            int depth,
            Map<Long, List<Subject>> subjectsByCourse
    ) {
        if (courses == null || courses.isEmpty()) {
            return;
        }
        for (Course course : courses.stream().sorted(Comparator.comparing(Course::name)).toList()) {
            options.add(new AdminPermissionContextOptionView(
                    permissionCode,
                    "COURSE",
                    course.id(),
                    course.name(),
                    "Course",
                    parentKey,
                    depth
            ));
            appendSubjects(
                    options,
                    permissionCode,
                    course.id(),
                    subjectsByCourse.get(course.id()),
                    nodeKey("COURSE", course.id()),
                    depth + 1
            );
        }
    }

    private static void appendEnrollmentCourses(
            List<AdminPermissionContextOptionView> options,
            String permissionCode,
            List<Course> courses,
            String parentKey,
            int depth,
            Map<Long, List<Subject>> subjectsByCourse,
            Map<String, List<ClassGroupContext>> classGroupsByCourseSubject
    ) {
        if (courses == null || courses.isEmpty()) {
            return;
        }
        for (Course course : courses.stream().sorted(Comparator.comparing(Course::name)).toList()) {
            String courseKey = nodeKey("COURSE", course.id());
            options.add(new AdminPermissionContextOptionView(
                    permissionCode,
                    "COURSE",
                    course.id(),
                    course.name(),
                    "Course enrollments",
                    parentKey,
                    depth
            ));
            appendEnrollmentSubjects(
                    options,
                    permissionCode,
                    course.id(),
                    subjectsByCourse.get(course.id()),
                    courseKey,
                    depth + 1,
                    classGroupsByCourseSubject
            );
        }
    }

    private static void appendEnrollmentSubjects(
            List<AdminPermissionContextOptionView> options,
            String permissionCode,
            long courseId,
            List<Subject> subjects,
            String parentKey,
            int depth,
            Map<String, List<ClassGroupContext>> classGroupsByCourseSubject
    ) {
        if (subjects == null || subjects.isEmpty()) {
            return;
        }
        for (Subject subject : subjects.stream().sorted(Comparator.comparing(Subject::name)).toList()) {
            String subjectKey = nodeKey("COURSE_SUBJECT", courseId + ":" + subject.id());
            options.add(new AdminPermissionContextOptionView(
                    permissionCode,
                    "SUBJECT",
                    subject.id(),
                    subject.name(),
                    "Subject enrollments",
                    subjectKey,
                    parentKey,
                    depth
            ));
            appendEnrollmentClassGroups(
                    options,
                    permissionCode,
                    classGroupsByCourseSubject.get(courseSubjectKey(courseId, subject.id())),
                    subjectKey,
                    depth + 1
            );
        }
    }

    private static void appendEnrollmentClassGroups(
            List<AdminPermissionContextOptionView> options,
            String permissionCode,
            List<ClassGroupContext> classGroups,
            String parentKey,
            int depth
    ) {
        if (classGroups == null || classGroups.isEmpty()) {
            return;
        }
        for (ClassGroupContext classGroup : classGroups.stream()
                .sorted(Comparator.comparing(ClassGroupContext::label))
                .toList()) {
            options.add(new AdminPermissionContextOptionView(
                    permissionCode,
                    "CLASS_GROUP",
                    classGroup.id(),
                    classGroup.label(),
                    "Class group enrollments",
                    parentKey,
                    depth
            ));
        }
    }

    private static void appendSubjects(
            List<AdminPermissionContextOptionView> options,
            String permissionCode,
            long courseId,
            List<Subject> subjects,
            String parentKey,
            int depth
    ) {
        if (subjects == null || subjects.isEmpty()) {
            return;
        }
        for (Subject subject : subjects.stream().sorted(Comparator.comparing(Subject::name)).toList()) {
            options.add(new AdminPermissionContextOptionView(
                    permissionCode,
                    "SUBJECT",
                    subject.id(),
                    subject.name(),
                    "Subject",
                    nodeKey("COURSE_SUBJECT", courseId + ":" + subject.id()),
                    parentKey,
                    depth
            ));
        }
    }

    private static boolean branchHasCourses(
            long unitId,
            Map<Long, List<OrganicUnit>> childrenByParent,
            Map<Long, List<Course>> coursesByUnit
    ) {
        if (!coursesByUnit.getOrDefault(unitId, List.of()).isEmpty()) {
            return true;
        }
        for (OrganicUnit child : childrenByParent.getOrDefault(unitId, List.of())) {
            if (branchHasCourses(child.id(), childrenByParent, coursesByUnit)) {
                return true;
            }
        }
        return false;
    }

    private static Map<Long, List<OrganicUnit>> childrenByParent(List<OrganicUnit> units) {
        Map<Long, List<OrganicUnit>> childrenByParent = new HashMap<>();
        for (OrganicUnit unit : units) {
            Long parentId = unit.parentOrganicUnitId();
            childrenByParent.computeIfAbsent(parentId == null ? 0L : parentId, ignored -> new ArrayList<>()).add(unit);
        }
        childrenByParent.values().forEach(children -> children.sort(unitComparator()));
        return childrenByParent;
    }

    private static Map<Long, List<Course>> coursesByUnit(List<Course> courses) {
        Map<Long, List<Course>> coursesByUnit = new HashMap<>();
        for (Course course : courses) {
            Long organicUnitId = course.organicUnitId();
            coursesByUnit.computeIfAbsent(organicUnitId == null ? 0L : organicUnitId, ignored -> new ArrayList<>()).add(course);
        }
        coursesByUnit.values().forEach(children -> children.sort(Comparator.comparing(Course::name)));
        return coursesByUnit;
    }

    private static Comparator<OrganicUnit> unitComparator() {
        return Comparator.comparing(OrganicUnit::code).thenComparing(OrganicUnit::name);
    }

    private static String nodeKey(String contextType, long contextId) {
        return contextType + ":" + contextId;
    }

    private static String nodeKey(String contextType, String contextId) {
        return contextType + ":" + contextId;
    }

    private static String profileNodeKey(String contextType, long contextId) {
        return contextType + ":" + contextId;
    }

    private static String profileNodeKey(String contextType, String contextId) {
        return contextType + ":" + contextId;
    }

    private static String courseSubjectKey(long courseId, long subjectId) {
        return courseId + ":" + subjectId;
    }

    private Set<Long> activeOrganizationIds(long userId) throws ServletException {
        try {
            return manageOrganizationDAO.findActiveOrganizationIdsByAdministrator(userId);
        } catch (SQLException exception) {
            throw new ServletException("Could not load user organization assignments", exception);
        }
    }

    private Set<AdministratorPermissionAssignment> activeAdminPermissionAssignments(long userId)
            throws ServletException {
        try {
            return permissionDAO.findActiveAdministratorAssignments(userId);
        } catch (SQLException exception) {
            throw new ServletException("Could not load administrator permission assignments", exception);
        }
    }

    private Set<AccessProfileContextAssignment> activeProfileContextAssignments(long userId)
            throws ServletException {
        try {
            Set<AccessProfileContextAssignment> assignments = new LinkedHashSet<>();
            for (Long subjectId : coordinateSubjectDAO.findActiveSubjectIdsByCoordinator(userId)) {
                assignments.add(new AccessProfileContextAssignment(
                        AccessProfileType.COORDINATOR,
                        AccessEntityType.SUBJECT,
                        subjectId,
                        null
                ));
            }
            for (Long classGroupId : teachClassGroupDAO.findActiveClassGroupIdsByTeacher(userId)) {
                assignments.add(new AccessProfileContextAssignment(
                        AccessProfileType.TEACHER,
                        AccessEntityType.CLASS_GROUP,
                        classGroupId,
                        null
                ));
            }
            for (Long courseId : enrollmentDAO.findActiveCourseIdsByStudent(userId)) {
                assignments.add(new AccessProfileContextAssignment(
                        AccessProfileType.STUDENT,
                        AccessEntityType.COURSE,
                        courseId,
                        null
                ));
            }
            for (SubjectEnrollment enrollment : enrollmentDAO.findActiveSubjectEnrollmentsByStudent(userId)) {
                assignments.add(new AccessProfileContextAssignment(
                        AccessProfileType.STUDENT,
                        AccessEntityType.SUBJECT,
                        enrollment.subjectId(),
                        enrollment.courseId()
                ));
            }
            return Set.copyOf(assignments);
        } catch (SQLException exception) {
            throw new ServletException("Could not load access profile context assignments", exception);
        }
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

    private static void validateInitialPassword(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("Initial password must contain at least 8 characters.");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Initial password must contain at least 1 lower letter.");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Initial password must contain at least 1 uppercase letter.");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Initial password must contain at least 1 number.");
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            throw new IllegalArgumentException("Initial password must contain at least 1 special character.");
        }
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
