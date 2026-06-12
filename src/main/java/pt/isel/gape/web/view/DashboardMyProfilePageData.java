package pt.isel.gape.web.view;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.Predicate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.AdministratorPermissionAssignment;
import pt.isel.gape.access.model.DeletionRequest;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.service.DeletionRequestService;
import pt.isel.gape.access.service.UserService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.config.SupportedDocumentTypeCatalog;
import pt.isel.gape.common.config.SupportedLanguageCatalog;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.session.SessionManager;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;

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
        request.setAttribute("profileSummaryDetails", profileSummaryDetails(user));
        request.setAttribute("administratorAccessDetails", administratorAccessDetails(connectionProvider, actor));
        request.setAttribute("languageOptions", SupportedLanguageCatalog.all());
        request.setAttribute("documentTypeOptions", SupportedDocumentTypeCatalog.all());
        request.setAttribute("returnTo", request.getServletPath());
        request.setAttribute("deletionRequests", deletionRequests);
        request.setAttribute("hasDeletionRequests", !deletionRequests.isEmpty());
        request.setAttribute("latestDeletionRequest", deletionRequests.isEmpty() ? null : deletionRequests.getLast());
        return true;
    }

    private static List<UserContextAssignmentView> profileSummaryDetails(User user) {
        return user.accessProfiles().stream()
                .map(profile -> new UserContextAssignmentView(
                        "Profile Type",
                        profileLabel(profile.type()),
                        profile.code()
                ))
                .toList();
    }

    private static List<UserContextAssignmentView> administratorAccessDetails(
            ConnectionProvider connectionProvider,
            SessionUser actor
    ) {
        if (!actor.profileTypes().contains(AccessProfileType.ADMINISTRATOR)) {
            return List.of();
        }
        PermissionDAO permissionDAO = new PermissionDAO(connectionProvider);
        ContextLabelResolver contextLabels = new ContextLabelResolver(connectionProvider);
        try {
            List<UserContextAssignmentView> details = new ArrayList<>();
            for (AdministratorPermissionAssignment assignment : permissionDAO.findActiveAdministratorAssignments(actor.userId())) {
                details.add(new UserContextAssignmentView(
                        "Access Level",
                        permissionLabel(assignment.permissionCode()),
                        contextLabels.labelFor(assignment.contextType(), assignment.contextId())
                ));
            }
            return List.copyOf(details);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load administrator access details", exception);
        }
    }

    private static String profileLabel(AccessProfileType profileType) {
        return switch (profileType) {
            case ADMINISTRATOR -> "Administrator";
            case COORDINATOR -> "Coordinator";
            case TEACHER -> "Teacher";
            case STUDENT -> "Student";
        };
    }

    private static String permissionLabel(String permissionCode) {
        return switch (AuthorizationPolicy.canonicalAdminPermission(permissionCode)) {
            case AuthorizationPolicy.MANAGE_ALL -> "Full access";
            case AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE -> "Organization structure";
            case AuthorizationPolicy.MANAGE_LEARNING -> "Learning";
            case AuthorizationPolicy.MANAGE_ENROLLMENTS -> "Enrollments";
            default -> permissionCode;
        };
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

    private static final class ContextLabelResolver {

        private final OrganizationDAO organizationDAO;
        private final OrganicUnitDAO organicUnitDAO;
        private final CourseDAO courseDAO;
        private final SubjectDAO subjectDAO;

        private ContextLabelResolver(ConnectionProvider connectionProvider) {
            this.organizationDAO = new OrganizationDAO(connectionProvider);
            this.organicUnitDAO = new OrganicUnitDAO(connectionProvider);
            this.courseDAO = new CourseDAO(connectionProvider);
            this.subjectDAO = new SubjectDAO(connectionProvider);
        }

        private String labelFor(AccessEntityType contextType, long contextId) throws SQLException {
            return switch (contextType) {
                case GLOBAL -> "Global";
                case ORGANIZATION -> organizationDAO.findById(contextId)
                        .map(organization -> organization.name())
                        .orElse("Organization #" + contextId);
                case ORGANIC_UNIT -> organicUnitDAO.findById(contextId)
                        .map(unit -> unit.name())
                        .orElse("Organic unit #" + contextId);
                case COURSE -> courseDAO.findById(contextId)
                        .map(course -> course.name())
                        .orElse("Course #" + contextId);
                case SUBJECT -> subjectDAO.findById(contextId)
                        .map(subject -> subject.name())
                        .orElse("Subject #" + contextId);
                case CLASS_GROUP -> "Class group #" + contextId;
                case SELF -> "Own profile";
            };
        }
    }
}
