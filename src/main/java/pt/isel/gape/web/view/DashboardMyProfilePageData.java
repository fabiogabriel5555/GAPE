package pt.isel.gape.web.view;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pt.isel.gape.access.model.AccessProfileContextAssignment;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.AdministratorPermissionAssignment;
import pt.isel.gape.access.model.DeletionRequest;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.service.DeletionRequestService;
import pt.isel.gape.access.service.UserService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.config.SupportedDocumentTypeCatalog;
import pt.isel.gape.common.config.SupportedLanguageCatalog;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.session.SessionManager;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.model.ClassGroupContext;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.transversal.service.ApplicationReadService;

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
        ApplicationReadService readService = new ApplicationReadService(connectionProvider);

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
        request.setAttribute("administratorAccessDetails", administratorAccessDetails(readService, actor));
        request.setAttribute("profileContextDetails", profileContextDetails(readService, actor.userId()));
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
            ApplicationReadService readService,
            SessionUser actor
    ) {
        if (!actor.profileTypes().contains(AccessProfileType.ADMINISTRATOR)) {
            return List.of();
        }
        ContextLabelResolver contextLabels = new ContextLabelResolver(readService);
        try {
            List<UserContextAssignmentView> details = new ArrayList<>();
            for (AdministratorPermissionAssignment assignment : readService.permissions()
                    .findActiveAdministratorAssignments(actor.userId())) {
                details.add(new UserContextAssignmentView(
                        "Access Level",
                        permissionLabel(assignment.permissionCode()),
                        contextLabels.labelFor(assignment.contextType(), assignment.contextId(), null),
                        contextLabels.labelHtmlFor(assignment.contextType(), assignment.contextId(), null)
                ));
            }
            return List.copyOf(details);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load administrator access details", exception);
        }
    }

    private static List<UserContextAssignmentView> profileContextDetails(
            ApplicationReadService readService,
            long userId
    ) {
        ContextLabelResolver contextLabels = new ContextLabelResolver(readService);
        try {
            List<UserContextAssignmentView> details = new ArrayList<>();
            for (AccessProfileContextAssignment assignment : activeProfileContextAssignments(readService, userId)) {
                details.add(new UserContextAssignmentView(
                        "Profile Context",
                        profileLabel(assignment.profileType()),
                        contextLabels.labelFor(assignment.contextType(), assignment.contextId(), assignment.parentContextId()),
                        contextLabels.labelHtmlFor(assignment.contextType(), assignment.contextId(), assignment.parentContextId())
                ));
            }
            details.sort(Comparator
                    .comparing(UserContextAssignmentView::getLabel)
                    .thenComparing(UserContextAssignmentView::getDetail));
            return List.copyOf(details);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load profile context details", exception);
        }
    }

    private static Set<AccessProfileContextAssignment> activeProfileContextAssignments(
            ApplicationReadService readService,
            long userId
    ) throws SQLException {
        Set<AccessProfileContextAssignment> assignments = new LinkedHashSet<>();
        for (Long subjectId : readService.coordinateSubjects().findActiveSubjectIdsByCoordinator(userId)) {
            assignments.add(new AccessProfileContextAssignment(
                    AccessProfileType.COORDINATOR,
                    AccessEntityType.SUBJECT,
                    subjectId,
                    null
            ));
        }
        for (Long classGroupId : readService.teachClassGroups().findActiveClassGroupIdsByTeacher(userId)) {
            assignments.add(new AccessProfileContextAssignment(
                    AccessProfileType.TEACHER,
                    AccessEntityType.CLASS_GROUP,
                    classGroupId,
                    null
            ));
        }
        for (Long courseId : readService.enrollments().findActiveCourseIdsByStudent(userId)) {
            assignments.add(new AccessProfileContextAssignment(
                    AccessProfileType.STUDENT,
                    AccessEntityType.COURSE,
                    courseId,
                    null
            ));
        }
        return Set.copyOf(assignments);
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

        private final ApplicationReadService.Organizations organizationDAO;
        private final ApplicationReadService.OrganicUnits organicUnitDAO;
        private final ApplicationReadService.Courses courseDAO;
        private final ApplicationReadService.Subjects subjectDAO;
        private final ApplicationReadService.TeachClassGroups teachClassGroupDAO;

        private ContextLabelResolver(ApplicationReadService readService) {
            this.organizationDAO = readService.organizations();
            this.organicUnitDAO = readService.organicUnits();
            this.courseDAO = readService.courses();
            this.subjectDAO = readService.subjects();
            this.teachClassGroupDAO = readService.teachClassGroups();
        }

        private String labelFor(AccessEntityType contextType, long contextId, Long parentContextId)
                throws SQLException {
            return switch (contextType) {
                case GLOBAL -> "Global";
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
                case SELF -> "Self";
            };
        }

        private String labelHtmlFor(AccessEntityType contextType, long contextId, Long parentContextId)
                throws SQLException {
            return switch (contextType) {
                case GLOBAL -> "Global";
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
}
