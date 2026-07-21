package pt.isel.gape.web.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import pt.isel.gape.access.model.AccessProfile;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.model.UserState;
import pt.isel.gape.access.service.UserService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseSubjectAssociationCommand;
import pt.isel.gape.learning.model.CurricularTerm;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectCreateCommand;
import pt.isel.gape.learning.model.SubjectState;
import pt.isel.gape.learning.model.SubjectUpdateCommand;
import pt.isel.gape.learning.service.ClassGroupService;
import pt.isel.gape.learning.service.CourseService;
import pt.isel.gape.learning.service.CourseSubjectService;
import pt.isel.gape.learning.service.CertificateService;
import pt.isel.gape.learning.service.GradeCertificateReadService;
import pt.isel.gape.learning.service.GradeRecordService;
import pt.isel.gape.learning.service.GradeSheetService;
import pt.isel.gape.learning.service.SubjectService;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.structure.model.RoleAssignmentState;
import pt.isel.gape.structure.service.OrganicUnitService;
import pt.isel.gape.structure.service.OrganizationService;
import pt.isel.gape.transversal.service.ApplicationReadService;
import pt.isel.gape.web.media.ProfilePhotoStorage;
import pt.isel.gape.web.view.ClassGroupView;
import pt.isel.gape.web.view.CoordinatorAssignmentView;
import pt.isel.gape.web.view.CourseView;
import pt.isel.gape.web.view.ClassGroupOccurrenceGroupView;
import pt.isel.gape.web.view.OrganizationView;
import pt.isel.gape.web.view.OrganicUnitView;
import pt.isel.gape.web.view.SubjectCourseView;
import pt.isel.gape.web.view.SubjectFormData;
import pt.isel.gape.web.view.SubjectView;
import pt.isel.gape.web.view.UserOptionView;

@WebServlet(name = "subjectManagementServlet", urlPatterns = {
        "/admin/subjects",
        "/admin/subjects/*",
        "/coordinator/subjects",
        "/coordinator/subjects/*",
        "/instructor/subjects",
        "/instructor/subjects/*"
})
@MultipartConfig(maxFileSize = 50L * 1024L * 1024L, maxRequestSize = 52L * 1024L * 1024L)
public final class SubjectManagementServlet extends DashboardServletSupport {

    private static final String ADMIN_SUBJECT_LIST_JSP = "/admin/admin/subject/admin-subjects.jsp";
    private static final int LIST_PAGE_SIZE = 10;
    private static final String ADMIN_SUBJECT_FORM_JSP = "/admin/admin/subject/admin-subject-form.jsp";
    private static final String ADMIN_SUBJECT_DETAIL_JSP = "/admin/admin/subject/admin-subject-detail.jsp";
    private static final String SUBJECT_ALLOCATIONS_FRAGMENT_JSP =
            "/WEB-INF/fragments/subject-allocations-panel.jsp";
    private static final String SUBJECT_GRADE_SHEET_FRAGMENT_JSP =
            "/WEB-INF/fragments/subject-grade-sheet-panel.jsp";
    private static final String CLASS_GROUP_ACTIVITIES_FRAGMENT_JSP =
            "/WEB-INF/fragments/class-group-activities-fragment.jsp";
    private static final String COORDINATOR_SUBJECT_LIST_JSP =
            "/coordinator/coordinator/subject/coordinator-subjects.jsp";
    private static final String COORDINATOR_SUBJECT_FORM_JSP =
            "/coordinator/coordinator/subject/coordinator-subject-form.jsp";
    private static final String COORDINATOR_SUBJECT_DETAIL_JSP =
            "/coordinator/coordinator/subject/coordinator-subject-detail.jsp";
    private static final String INSTRUCTOR_SUBJECT_LIST_JSP =
            "/instructor/instructor/subject/instructor-subjects.jsp";
    private static final String INSTRUCTOR_SUBJECT_FORM_JSP =
            "/instructor/instructor/subject/instructor-subject-form.jsp";
    private static final String INSTRUCTOR_SUBJECT_DETAIL_JSP =
            "/instructor/instructor/subject/instructor-subject-detail.jsp";

    private final SubjectService subjectService;
    private final CourseService courseService;
    private final CourseSubjectService courseSubjectService;
    private final ClassGroupService classGroupService;
    private final OrganizationService organizationService;
    private final OrganicUnitService organicUnitService;
    private final UserService userService;
    private final ApplicationReadService.Courses courseDAO;
    private final ApplicationReadService.CourseSubjects courseSubjectDAO;
    private final ApplicationReadService.ClassGroups classGroupDAO;
    private final ApplicationReadService.Subjects subjectDAO;
    private final ApplicationReadService.CoordinateSubjects coordinateSubjectDAO;
    private final ApplicationReadService.Organizations organizationDAO;
    private final LearningViewFactory viewFactory;
    private final ProfilePhotoStorage photoStorage;
    private final ClassGroupActivityViewSupport activityViewSupport;
    private final GradeCertificateServlet gradeCertificateServlet;

    public SubjectManagementServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private SubjectManagementServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new ApplicationReadService(connectionProvider),
                new SubjectService(connectionProvider, clock),
                new CourseService(connectionProvider, clock),
                new CourseSubjectService(connectionProvider, clock),
                new ClassGroupService(connectionProvider, clock),
                new OrganizationService(connectionProvider, clock),
                new OrganicUnitService(connectionProvider, clock),
                new UserService(connectionProvider, clock),
                new ProfilePhotoStorage(),
                new ClassGroupActivityViewSupport(connectionProvider, clock),
                new GradeCertificateServlet(
                        connectionProvider,
                        new GradeSheetService(connectionProvider, clock),
                        new GradeRecordService(connectionProvider, clock),
                        new CertificateService(connectionProvider, clock),
                        new GradeCertificateReadService(connectionProvider),
                        clock
                )
        );
    }

    SubjectManagementServlet(
            ApplicationReadService readService,
            SubjectService subjectService,
            CourseService courseService,
            CourseSubjectService courseSubjectService,
            ClassGroupService classGroupService,
            OrganizationService organizationService,
            OrganicUnitService organicUnitService,
            UserService userService,
            ProfilePhotoStorage photoStorage,
            ClassGroupActivityViewSupport activityViewSupport,
            GradeCertificateServlet gradeCertificateServlet
    ) {
        this.subjectService = subjectService;
        this.courseService = courseService;
        this.courseSubjectService = courseSubjectService;
        this.classGroupService = classGroupService;
        this.organizationService = organizationService;
        this.organicUnitService = organicUnitService;
        this.userService = userService;
        this.courseDAO = readService.courses();
        this.courseSubjectDAO = readService.courseSubjects();
        this.classGroupDAO = readService.classGroups();
        this.subjectDAO = readService.subjects();
        this.coordinateSubjectDAO = readService.coordinateSubjects();
        this.organizationDAO = readService.organizations();
        this.viewFactory = new LearningViewFactory(
                readService.organizations(),
                readService.organicUnits(),
                readService.courses(),
                readService.courseOccurrences(),
                readService.subjects(),
                readService.courseSubjects(),
                readService.enrollments(),
                readService.classGroups(),
                readService.classGroupEnrollments(),
                readService.contentBlocks(),
                readService.users(),
                readService.teachClassGroups()
        );
        this.photoStorage = photoStorage;
        this.activityViewSupport = activityViewSupport;
        this.gradeCertificateServlet = gradeCertificateServlet;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        try {
            if (segments.length == 0) {
                showList(request, response);
                return;
            }
            if (segments.length == 1 && "new".equals(segments[0])) {
                if (isLimitedSubjectRequest(request)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                showSubjectForm(request, response, newSubjectFormData(request), true, null);
                return;
            }
            if (segments.length == 1) {
                showDetail(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2 && "allocations".equals(segments[1])) {
                showAllocationsFragment(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2 && "associations".equals(segments[1])) {
                showAssociationsFragment(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2 && "grade-sheet".equals(segments[1])) {
                showGradeSheetFragment(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 4
                    && "class-groups".equals(segments[1])
                    && "activities".equals(segments[3])) {
                showClassGroupActivitiesFragment(
                        request,
                        response,
                        Long.parseLong(segments[0]),
                        Long.parseLong(segments[2])
                );
                return;
            }
            if (segments.length == 2 && "edit".equals(segments[1])) {
                if (isTeacherSubjectRequest(request)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                showEditForm(request, response, Long.parseLong(segments[0]), null);
                return;
            }
            if (segments.length == 2 && "courses".equals(segments[1])) {
                if (isTeacherSubjectRequest(request)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                redirect(request, response, subjectBasePath(request) + "/" + Long.parseLong(segments[0]) + "#course-associations");
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        try {
            if (isTeacherSubjectRequest(request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (segments.length == 0) {
                if (isCoordinatorSubjectRequest(request)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                createSubject(request, response);
                return;
            }
            if (segments.length == 1) {
                updateSubject(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2) {
                long subjectId = Long.parseLong(segments[0]);
                switch (segments[1]) {
                    case "assign-coordinator" -> assignCoordinator(request, response, subjectId);
                    case "courses" -> addCourseAssociation(request, response, subjectId);
                    case "archive" -> archiveSubject(request, response, subjectId);
                    case "unarchive" -> unarchiveSubject(request, response, subjectId);
                    case "delete" -> deleteSubject(request, response, subjectId);
                    default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                return;
            }
            if (segments.length == 3 && "courses".equals(segments[1])) {
                updateCourseAssociation(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4 && "coordinators".equals(segments[1]) && "update".equals(segments[3])) {
                updateCoordinatorAssignment(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4 && "coordinators".equals(segments[1]) && "delete".equals(segments[3])) {
                removeCoordinatorAssignment(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
                return;
            }
            if (segments.length == 4 && "courses".equals(segments[1]) && "delete".equals(segments[3])) {
                removeCourseAssociation(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        boolean coordinatorMode = isCoordinatorSubjectRequest(request);
        boolean teacherMode = isTeacherSubjectRequest(request);
        List<Organization> organizations = coordinatorMode || teacherMode
                ? List.of()
                : managedOrganizations(request);
        if (isListRowsFragmentRequest(request)) {
            prepareSubjectListRows(
                    request,
                    actor,
                    organizations,
                    coordinatorMode,
                    teacherMode,
                    requestedListOffset(request),
                    requestedListLoadAll(request)
            );
            forward(request, response, "/WEB-INF/fragments/subject-list-rows.jsp");
            return;
        }
        prepareSubjectListRows(request, actor, organizations, coordinatorMode, teacherMode, 0, false);
        prepareSubjectBasePath(request);
        request.setAttribute("canCreateSubjects", canCreateAnySubject(actor, organizations, request));
        prepareDashboard(
                request,
                "subjects",
                "Subjects",
                (Boolean) request.getAttribute("canCreateSubjects") ? subjectBasePath(request) + "/new" : null,
                (Boolean) request.getAttribute("canCreateSubjects") ? "New Subject" : null
        );
        forward(request, response, subjectListJsp(request));
    }

    private void prepareSubjectListRows(
            HttpServletRequest request,
            SessionUser actor,
            List<Organization> organizations,
            boolean coordinatorMode,
            boolean teacherMode,
            int requestedOffset,
            boolean loadAll
    ) {
        List<Subject> rawSubjects = coordinatorMode
                ? coordinatorSubjects(actor, organizations, request)
                : teacherMode
                        ? teacherSubjects(actor)
                        : organizations.stream()
                        .flatMap(organization -> subjectService.listSubjects(
                                actor.userId(),
                                currentSessionId(request),
                                primaryProfile(actor),
                                organization.id(),
                                request.getRemoteAddr()
                        ).stream())
                .sorted(Comparator.comparingLong(Subject::id).reversed())
                .toList();
        int total = rawSubjects.size();
        int fromIndex = Math.min(Math.max(0, requestedOffset), total);
        int toIndex = loadAll ? total : Math.min(fromIndex + LIST_PAGE_SIZE, total);
        List<Subject> pageSubjects = rawSubjects.subList(fromIndex, toIndex);
        List<SubjectView> subjects = pageSubjects.stream()
                .map(viewFactory::subjectView)
                .toList();
        Map<Long, List<SubjectCourseView>> subjectCoursesBySubject = subjectCoursesBySubject(actor, subjects, request);
        Map<Long, Integer> associationCountBySubject = new HashMap<>();
        subjectCoursesBySubject.forEach((subjectId, associations) ->
                associationCountBySubject.put(subjectId, associations.size()));

        request.setAttribute("subjects", subjects);
        prepareSubjectBasePath(request);
        request.setAttribute("canCreateSubjects", canCreateAnySubject(actor, organizations, request));
        request.setAttribute("canModifySubjectById", canModifySubjectById(actor, pageSubjects, request));
        request.setAttribute("subjectCoursesBySubject", subjectCoursesBySubject);
        request.setAttribute("associationCountBySubject", associationCountBySubject);
        request.setAttribute("subjectCount", total);
        request.setAttribute("activeSubjects", rawSubjects.stream().filter(subject -> subject.state() == SubjectState.ACTIVE).count());
        request.setAttribute("inactiveSubjects", rawSubjects.stream().filter(subject -> subject.state() == SubjectState.INACTIVE).count());
        request.setAttribute("subjectListOffset", fromIndex);
        request.setAttribute("subjectListCurrentPage", fromIndex / LIST_PAGE_SIZE + 1);
        request.setAttribute("subjectListLoadAll", loadAll);
        request.setAttribute("subjectListHasMore", !loadAll && toIndex < total);
        request.setAttribute("subjectListNextOffset", toIndex);
    }

    private SubjectFormData newSubjectFormData(HttpServletRequest request) {
        Long requestedCourseId = parseOptionalLong(request.getParameter("courseId"));
        if (requestedCourseId != null) {
            Course requestedCourse = course(requestedCourseId);
            if (requestedCourse != null && canCreateSubjectInCourseOrganization(request, requestedCourse)) {
                return SubjectFormData.blank(requestedCourse.organizationId());
            }
        }
        // Keep the Organic Unit field disabled until the user has chosen the
        // Subject's Organization. A subject opened from a Course keeps that
        // explicit course context above; the standalone create form does not
        // silently choose the first managed organization.
        return SubjectFormData.blank(null);
    }

    private Course course(long courseId) {
        try {
            return courseDAO.findById(courseId).orElse(null);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load course", exception);
        }
    }

    private boolean canCreateSubjectInCourseOrganization(HttpServletRequest request, Course course) {
        if (course.state() != CourseState.ACTIVE) {
            return false;
        }
        for (Organization organization : managedOrganizations(request)) {
            if (organization.id() == course.organizationId()) {
                return true;
            }
        }
        return false;
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response, long subjectId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Subject subject = subjectService.getSubject(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        );
        markLearningEventsReadForCurrentUser(request, "/admin/subjects/" + subject.id(), true);
        SubjectView subjectView = viewFactory.subjectView(subject);
        boolean canModifySubject = subjectService.canModifySubject(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        );
        List<CourseSubjectAssociation> rawSubjectCourseAssociations = subjectCourseAssociations(subjectId);
        boolean canManageSubjectAssociations = canManageSubjectAssociations(
                actor,
                subjectId,
                rawSubjectCourseAssociations,
                request
        );
        List<SubjectCourseView> subjectCourseAssociations = subjectCourseViews(rawSubjectCourseAssociations).stream()
                .filter(association -> canReadSubjectCourse(actor, subjectId, association.getCourseId(), request))
                .sorted(Comparator.comparingLong(SubjectCourseView::getCourseId).reversed())
                .toList();
        List<ClassGroupView> classGroups = viewFactory.classGroupViews(classGroupsBySubject(subjectId).stream()
                .filter(classGroup -> canReadClassGroup(request, actor, classGroup.id()))
                .toList());
        Set<Long> classGroupIds = classGroupIdsFrom(classGroups);
        exposeSubjectGradeSheetSummary(request, actor, primaryProfile(actor), subjectId, request.getRemoteAddr());
        prepareSubjectBasePath(request);
        request.setAttribute("subject", subjectView);
        request.setAttribute("subjectCourseAssociations", subjectCourseAssociations);
        request.setAttribute("coordinatorAssignments", coordinatorAssignments(subjectId));
        request.setAttribute("classGroups", classGroups);
        request.setAttribute(
                "activeClassGroupCount",
                classGroups.stream().filter(ClassGroupView::isActive).count()
        );
        request.setAttribute(
                "activeClassGroupOccurrenceGroups",
                classGroupOccurrenceGroups(classGroups.stream()
                        .filter(classGroup -> !classGroup.isCompleted())
                        .toList())
        );
        request.setAttribute(
                "completedClassGroupOccurrenceGroups",
                classGroupOccurrenceGroups(classGroups.stream()
                        .filter(ClassGroupView::isCompleted)
                        .toList())
        );
        request.setAttribute(
                "completedClassGroupCount",
                classGroups.stream().filter(ClassGroupView::isCompleted).count()
        );
        ClassGroupCreationAvailability classGroupCreationAvailability =
                classGroupCreationAvailabilityForSubject(
                        actor,
                        subjectId,
                        rawSubjectCourseAssociations,
                        request
                );
        request.setAttribute("canModifySubject", canModifySubject);
        request.setAttribute("canManageSubjectAssociations", canManageSubjectAssociations);
        request.setAttribute("canCreateClassGroupsForSubject",
                classGroupCreationAvailability.allowed());
        request.setAttribute("classGroupCreationUnavailableReason",
                classGroupCreationAvailability.unavailableReason());
        request.setAttribute(
                "canAssignSubjectCoordinators",
                canModifySubject && subject.state() == SubjectState.ACTIVE
        );
        request.setAttribute("canModifyClassGroupById", canModifyClassGroupById(actor, classGroupIds, request));
        request.setAttribute("canManageClassGroupStructureById",
                canManageClassGroupStructureById(actor, classGroupIds, request));
        activityViewSupport.exposeClassGroupActivityCounts(request, classGroupIds);
        prepareSubjectContext(request, subjectView, "detail");
        prepareDashboard(request, "subjects", "Subject Details");
        forward(request, response, subjectDetailJsp(request));
    }

    private void exposeSubjectGradeSheetSummary(
            HttpServletRequest request,
            SessionUser actor,
            AccessProfileType profile,
            long subjectId,
            String sourceIp
    ) throws ServletException {
        int sheetCount;
        try {
            sheetCount = gradeCertificateServlet.visibleSubjectGradeSheetCount(
                    actor,
                    profile,
                    sourceIp,
                    subjectId
            );
        } catch (SQLException exception) {
            throw new ServletException("Failed to load subject grade sheet summary", exception);
        }
        request.setAttribute("subjectGradeSheetSheetCount", sheetCount);
        request.setAttribute("subjectGradeSheetAvailable", sheetCount > 0);
    }

    private void exposeSubjectGradeSheets(
            HttpServletRequest request,
            SessionUser actor,
            AccessProfileType profile,
            long subjectId,
            String sourceIp,
            List<ClassGroupView> classGroups
    ) throws ServletException {
        List<GradeCertificateServlet.GradeSheetSubjectGroupView> occurrenceGroups;
        try {
            occurrenceGroups = gradeCertificateServlet.visibleSubjectGradeSheetOccurrenceGroups(
                    actor,
                    profile,
                    sourceIp,
                    subjectId
            );
        } catch (SQLException exception) {
            throw new ServletException("Failed to load subject grade sheets", exception);
        }
        List<GradeCertificateServlet.GradeSheetSubjectGroupView> activeOccurrenceGroups = occurrenceGroups.stream()
                .filter(group -> !group.isPublished())
                .toList();
        List<GradeCertificateServlet.GradeSheetSubjectGroupView> publishedOccurrenceGroups = occurrenceGroups.stream()
                .filter(GradeCertificateServlet.GradeSheetSubjectGroupView::isPublished)
                .toList();
        // Subject Details represents one consolidated grade sheet per concrete
        // subject occurrence.  The class-group sheets remain source records
        // within that entry and must not inflate the card or completed count.
        int sheetCount = occurrenceGroups.size();
        int studentCount = occurrenceGroups.stream()
                .map(GradeCertificateServlet.GradeSheetSubjectGroupView::getSubjectDocument)
                .mapToInt(document -> document.getRows().size())
                .sum();
        int publishedSheetCount = publishedOccurrenceGroups.size();
        request.setAttribute("subjectGradeSheetOccurrenceGroups", occurrenceGroups);
        request.setAttribute("activeSubjectGradeSheetOccurrenceGroups", activeOccurrenceGroups);
        request.setAttribute("publishedSubjectGradeSheetOccurrenceGroups", publishedOccurrenceGroups);
        request.setAttribute("subjectGradeSheetOccurrenceDisplayLabels",
                subjectGradeSheetOccurrenceLabels(occurrenceGroups, classGroups));
        request.setAttribute("subjectGradeSheetCourseCount", occurrenceGroups.stream()
                .map(GradeCertificateServlet.GradeSheetSubjectGroupView::getCourseId)
                .distinct()
                .count());
        request.setAttribute("subjectGradeSheetSheetCount", sheetCount);
        request.setAttribute("subjectGradeSheetStudentCount", studentCount);
        request.setAttribute("publishedSubjectGradeSheetCount", publishedSheetCount);
        request.setAttribute("subjectGradeSheetAvailable", !occurrenceGroups.isEmpty());
    }

    private void showAllocationsFragment(
            HttpServletRequest request,
            HttpServletResponse response,
            long subjectId
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Subject subject = subjectService.getSubject(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        );
        boolean canModifySubject = subjectService.canModifySubject(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        );
        request.setAttribute("subject", viewFactory.subjectView(subject));
        request.setAttribute("coordinatorAssignments", coordinatorAssignments(subjectId));
        boolean canAssignSubjectCoordinators = canModifySubject && subject.state() == SubjectState.ACTIVE;
        request.setAttribute(
                "coordinatorOptions",
                canAssignSubjectCoordinators ? coordinatorOptions(request, null) : List.of()
        );
        request.setAttribute("canAssignSubjectCoordinators", canAssignSubjectCoordinators);
        prepareSubjectFragment(request, subjectId);
        forward(request, response, SUBJECT_ALLOCATIONS_FRAGMENT_JSP);
    }

    private void showAssociationsFragment(
            HttpServletRequest request,
            HttpServletResponse response,
            long subjectId
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Subject subject = subjectService.getSubject(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        );
        List<CourseSubjectAssociation> rawAssociations = subjectCourseAssociations(subjectId);
        boolean canManageAssociations = canManageSubjectAssociations(
                actor,
                subjectId,
                rawAssociations,
                request
        );
        List<SubjectCourseView> associations = subjectCourseViews(rawAssociations).stream()
                .filter(association -> canReadSubjectCourse(actor, subjectId, association.getCourseId(), request))
                .sorted(Comparator.comparingLong(SubjectCourseView::getCourseId).reversed())
                .toList();
        request.setAttribute("subject", viewFactory.subjectView(subject));
        request.setAttribute("subjectCourseAssociations", associations);
        request.setAttribute("availableCourseOptions", availableCourseOptions(request, subject, associations));
        request.setAttribute("canManageSubjectAssociations", canManageAssociations);
        request.setAttribute("subjectAssociationEmbedded", Boolean.TRUE);
        prepareSubjectFragment(request, subjectId);
        forward(request, response, "/WEB-INF/fragments/subject-associations-panel.jsp");
    }

    private void showGradeSheetFragment(
            HttpServletRequest request,
            HttpServletResponse response,
            long subjectId
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Subject subject = subjectService.getSubject(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        );
        request.setAttribute("subject", viewFactory.subjectView(subject));
        List<ClassGroupView> classGroups = viewFactory.classGroupViews(classGroupsBySubject(subjectId).stream()
                .filter(classGroup -> canReadClassGroup(request, actor, classGroup.id()))
                .toList());
        exposeSubjectGradeSheets(
                request,
                actor,
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr(),
                classGroups
        );
        prepareSubjectFragment(request, subjectId);
        forward(request, response, SUBJECT_GRADE_SHEET_FRAGMENT_JSP);
    }

    private void showClassGroupActivitiesFragment(
            HttpServletRequest request,
            HttpServletResponse response,
            long subjectId,
            long classGroupId
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        subjectService.getSubject(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        );
        pt.isel.gape.learning.model.ClassGroup classGroup;
        try {
            classGroup = classGroupDAO.findById(classGroupId).orElse(null);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load class group activities", exception);
        }
        if (classGroup == null || classGroup.subjectId() != subjectId) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!canReadClassGroup(request, actor, classGroupId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        request.setAttribute("classGroup", viewFactory.classGroupViews(List.of(classGroup)).get(0));
        request.setAttribute("canModifyClassGroup", classGroupService.canModifyClassGroup(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                classGroupId,
                request.getRemoteAddr()
        ));
        activityViewSupport.exposeClassGroupActivities(
                request,
                actor,
                currentSessionId(request),
                primaryProfile(actor),
                List.of(classGroupId)
        );
        prepareSubjectFragment(request, subjectId);
        forward(request, response, CLASS_GROUP_ACTIVITIES_FRAGMENT_JSP);
    }

    private void prepareSubjectFragment(HttpServletRequest request, long subjectId) {
        prepareSubjectBasePath(request);
        String returnTo = subjectBasePath(request) + "/" + subjectId;
        request.setAttribute("currentReturnTo", returnTo);
        request.setAttribute("currentReturnToParam", URLEncoder.encode(returnTo, StandardCharsets.UTF_8));
        request.setAttribute("mediaCacheVersion", Long.toString(System.currentTimeMillis()));
    }

    private void showSubjectForm(
            HttpServletRequest request,
            HttpServletResponse response,
            SubjectFormData form,
            boolean creating,
            String error
    ) throws ServletException, IOException {
        prepareSubjectForm(request, form, creating, error, null);
        forward(request, response, subjectFormJsp(request));
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response, long subjectId, String error)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Subject subject = subjectService.getSubject(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        );
        if (!subjectService.canModifySubject(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        )) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        SubjectFormData form = error == null ? SubjectFormData.from(subject) : SubjectFormData.from(request, subjectId);
        prepareSubjectForm(request, form, false, error, subject);
        SubjectView subjectView = viewFactory.subjectView(subject);
        request.setAttribute("subject", subjectView);
        prepareSubjectContext(request, subjectView, "edit");
        request.setAttribute("canModifySubject", Boolean.TRUE);
        forward(request, response, subjectFormJsp(request));
    }

    private void createSubject(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        SubjectFormData form = SubjectFormData.from(request, null);
        Part subjectImage;
        try {
            subjectImage = subjectImagePart(request);
        } catch (IOException | ServletException exception) {
            showSubjectForm(request, response, form, true, "The uploaded file could not be processed. Please try again.");
            return;
        }
        try {
            Subject created = subjectService.createSubject(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    new SubjectCreateCommand(
                            longParameter(request, "organizationId"),
                            parseOptionalLong(text(request, "organicUnitId")),
                            text(request, "name"),
                            text(request, "acronym"),
                            text(request, "photo"),
                            text(request, "description"),
                            requiredBigDecimal(request, "ects"),
                            requiredBigDecimal(request, "finalGradeMax"),
                            optionalInteger(request, "workloadHours"),
                            subjectState(text(request, "state"))
                    ),
                    request.getRemoteAddr()
            );
            try {
                created = attachUploadedPhotoToCreatedSubject(request, actor, created, subjectImage);
            } catch (IOException exception) {
                flashError(request, "Subject created, but the uploaded image could not be processed. Please edit the subject and try again.");
                redirect(request, response, subjectBasePath(request) + "/" + created.id());
                return;
            } catch (RuntimeException exception) {
                flashError(request, "Subject created, but " + messageFor(exception));
                redirect(request, response, subjectBasePath(request) + "/" + created.id());
                return;
            }
            flashSuccess(request, "Subject created successfully.");
            redirect(request, response, subjectBasePath(request) + "/" + created.id());
        } catch (RuntimeException exception) {
            showSubjectForm(request, response, form, true, messageFor(exception));
        }
    }

    private Subject attachUploadedPhotoToCreatedSubject(
            HttpServletRequest request,
            SessionUser actor,
            Subject created,
            Part subjectImage
    ) throws IOException {
        String uploadedPhoto = photoStorage.saveSubjectPhoto(created.id(), subjectImage, getServletContext());
        if (uploadedPhoto == null) {
            return created;
        }
        return subjectService.attachCreatedSubjectPhoto(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                created.id(),
                uploadedPhoto,
                request.getRemoteAddr()
        );
    }

    private void updateSubject(HttpServletRequest request, HttpServletResponse response, long subjectId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            Subject existing = subjectService.getSubject(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    subjectId,
                    request.getRemoteAddr()
            );
            String uploadedPhoto;
            try {
                uploadedPhoto = photoStorage.saveSubjectPhoto(
                        subjectId,
                        subjectImagePart(request),
                        getServletContext()
                );
            } catch (IOException | ServletException exception) {
                showEditForm(request, response, subjectId, "The uploaded file could not be processed. Please try again.");
                return;
            }
            String submittedPhoto = text(request, "photo");
            String photo = uploadedPhoto != null ? uploadedPhoto : (submittedPhoto != null ? submittedPhoto : existing.photo());
            subjectService.updateSubject(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    subjectId,
                    new SubjectUpdateCommand(
                            parseOptionalLong(text(request, "organicUnitId")),
                            text(request, "name"),
                            text(request, "acronym"),
                            photo,
                            text(request, "description"),
                            requiredBigDecimal(request, "ects"),
                            requiredBigDecimal(request, "finalGradeMax"),
                            optionalInteger(request, "workloadHours"),
                            subjectState(text(request, "state"))
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Subject updated successfully.");
            redirect(request, response, subjectBasePath(request) + "/" + subjectId);
        } catch (RuntimeException exception) {
            showEditForm(request, response, subjectId, messageFor(exception));
        }
    }

    private void assignCoordinator(HttpServletRequest request, HttpServletResponse response, long subjectId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            subjectService.assignCoordinator(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    subjectId,
                    longParameter(request, "coordinatorUserId"),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Coordinator assigned successfully.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, subjectBasePath(request) + "/" + subjectId + "#subject-coordinators");
    }

    private void updateCoordinatorAssignment(
            HttpServletRequest request,
            HttpServletResponse response,
            long subjectId,
            long coordinatorUserId
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            subjectService.updateCoordinatorAssignment(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    subjectId,
                    coordinatorUserId,
                    roleAssignmentState(text(request, "state")),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Coordinator assignment updated.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, subjectBasePath(request) + "/" + subjectId + "#subject-coordinators");
    }

    private void removeCoordinatorAssignment(
            HttpServletRequest request,
            HttpServletResponse response,
            long subjectId,
            long coordinatorUserId
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            subjectService.removeCoordinatorAssignment(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    subjectId,
                    coordinatorUserId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Coordinator assignment removed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, subjectBasePath(request) + "/" + subjectId + "#subject-coordinators");
    }

    private void archiveSubject(HttpServletRequest request, HttpServletResponse response, long subjectId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            subjectService.archiveSubject(actor.userId(), currentSessionId(request), primaryProfile(actor), subjectId, request.getRemoteAddr());
            flashSuccess(request, "Subject deactivated.");
            redirect(request, response, isAjaxRequest(request)
                    ? subjectBasePath(request) + "/" + subjectId
                    : subjectBasePath(request));
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirect(request, response, subjectBasePath(request) + "/" + subjectId);
        }
    }

    private void unarchiveSubject(HttpServletRequest request, HttpServletResponse response, long subjectId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            subjectService.unarchiveSubject(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    subjectId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Subject activated.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        if (isAjaxRequest(request)) {
            redirect(request, response, subjectBasePath(request) + "/" + subjectId);
        } else {
            redirectToReturnPath(request, response, subjectBasePath(request) + "/" + subjectId);
        }
    }

    private void deleteSubject(HttpServletRequest request, HttpServletResponse response, long subjectId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            subjectService.deleteSubject(actor.userId(), currentSessionId(request), primaryProfile(actor), subjectId, request.getRemoteAddr());
            flashSuccess(request, "Subject deleted.");
            redirect(request, response, subjectBasePath(request));
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirect(request, response, subjectBasePath(request) + "/" + subjectId);
        }
    }

    private void addCourseAssociation(HttpServletRequest request, HttpServletResponse response, long subjectId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            long courseId = longParameter(request, "courseId");
            courseSubjectService.associateSubject(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    new CourseSubjectAssociationCommand(
                            courseId,
                            subjectId,
                            optionalInteger(request, "curricularYear"),
                            curricularTerm(text(request, "term")),
                            request.getParameter("mandatory") != null
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Course associated with subject.");
            redirectToReturnPath(request, response, subjectBasePath(request) + "/" + subjectId + "#course-associations");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirectToReturnPath(request, response, subjectBasePath(request) + "/" + subjectId + "/edit#course-associations");
        }
    }

    private void updateCourseAssociation(HttpServletRequest request, HttpServletResponse response, long subjectId, long courseId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            courseSubjectService.updateAssociation(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    new CourseSubjectAssociationCommand(
                            courseId,
                            subjectId,
                            optionalInteger(request, "curricularYear"),
                            curricularTerm(text(request, "term")),
                            request.getParameter("mandatory") != null
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Course association updated.");
            redirectToReturnPath(request, response, subjectBasePath(request) + "/" + subjectId + "#course-associations");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirectToReturnPath(request, response, subjectBasePath(request) + "/" + subjectId + "/edit#course-associations");
        }
    }

    private void removeCourseAssociation(HttpServletRequest request, HttpServletResponse response, long subjectId, long courseId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            courseSubjectService.deleteAssociation(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    courseId,
                    subjectId,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Course association removed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, subjectBasePath(request) + "/" + subjectId + "#course-associations");
    }

    private void prepareSubjectForm(
            HttpServletRequest request,
            SubjectFormData form,
            boolean creating,
            String error,
            Subject subjectForEdit
    ) {
        prepareSubjectBasePath(request);
        SessionUser actor = requireCurrentUser(request);
        List<Organization> organizations = isCoordinatorSubjectRequest(request)
                ? coordinatorSubjectOrganizations(actor, subjectForEdit)
                : managedOrganizations(request);
        long selectedOrganizationId = selectedOrganizationId(organizations, form);
        if (!creating && subjectForEdit != null && error == null) {
            form = SubjectFormData.from(subjectForEdit);
        }
        request.setAttribute("form", form);
        request.setAttribute("creating", creating);
        request.setAttribute("organizationOptions", organizations.stream()
                .map(organization -> OrganizationView.from(organization, 0))
                .toList());
        request.setAttribute("organicUnitOptions", organizations.stream()
                .flatMap(organization -> organicUnits(request, organization.id()).stream())
                .map(unit -> OrganicUnitView.from(unit, null, 0))
                .toList());
        request.setAttribute("selectedOrganizationId", selectedOrganizationId);
        request.setAttribute("formAction", creating
                ? request.getContextPath() + subjectBasePath(request)
                : request.getContextPath() + subjectBasePath(request) + "/" + form.getId());
        if (creating) {
            request.setAttribute("adminSubjectActiveChild", "new");
            request.setAttribute("coordinatorSubjectActiveChild", "new");
            request.setAttribute("teacherSubjectActiveChild", "new");
        }
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareDashboard(request, "subjects", creating ? "Create Subject" : "Edit Subject");
    }

    private List<Organization> managedOrganizations(HttpServletRequest request) {
        SessionUser actor = requireCurrentUser(request);
        return organizationService.listManagedOrganizations(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                AuthorizationPolicy.MANAGE_SUBJECTS,
                request.getRemoteAddr()
        );
    }

    private List<OrganicUnit> organicUnits(HttpServletRequest request, long organizationId) {
        if (organizationId <= 0) {
            return List.of();
        }
        SessionUser actor = requireCurrentUser(request);
        return organicUnitService.listOrganicUnits(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                organizationId,
                request.getRemoteAddr()
        );
    }

    private List<Subject> coordinatorSubjects(
            SessionUser actor,
            List<Organization> organizations,
            HttpServletRequest request
    ) {
        try {
            return coordinateSubjectDAO.findActiveSubjectIdsByCoordinator(actor.userId()).stream()
                    .map(subjectId -> subjectService.getSubject(
                            actor.userId(),
                            currentSessionId(request),
                            primaryProfile(actor),
                            subjectId,
                            request.getRemoteAddr()
                    ))
                    .sorted(Comparator.comparingLong(Subject::id).reversed())
                    .toList();
        } catch (RuntimeException | SQLException exception) {
            throw new IllegalStateException("Failed to load coordinator subjects", exception);
        }
    }

    private List<Subject> teacherSubjects(SessionUser actor) {
        try {
            return subjectDAO.findByTeacher(actor.userId()).stream()
                    .sorted(Comparator.comparingLong(Subject::id).reversed())
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load teacher subjects", exception);
        }
    }

    private List<Organization> coordinatorManagedOrganizations(SessionUser actor) {
        return List.of();
    }

    private List<Organization> coordinatorSubjectOrganizations(SessionUser actor, Subject subjectForEdit) {
        if (subjectForEdit == null) {
            return List.of();
        }
        try {
            return organizationDAO.findById(subjectForEdit.organizationId())
                    .map(organization -> List.of(organization))
                    .orElseGet(List::of);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load subject organization", exception);
        }
    }

    private void prepareSubjectBasePath(HttpServletRequest request) {
        request.setAttribute("subjectBasePath", subjectBasePath(request));
        request.setAttribute("subjectCourseBasePath", subjectCourseBasePath(request));
    }

    private static String subjectBasePath(HttpServletRequest request) {
        if (isCoordinatorSubjectRequest(request)) {
            return "/coordinator/subjects";
        }
        return isTeacherSubjectRequest(request) ? "/instructor/subjects" : "/admin/subjects";
    }

    private static boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    private static boolean isListRowsFragmentRequest(HttpServletRequest request) {
        return "rows".equals(request.getParameter("fragment"));
    }

    private static boolean requestedListLoadAll(HttpServletRequest request) {
        return Boolean.parseBoolean(request.getParameter("loadAll"));
    }

    private static int requestedListOffset(HttpServletRequest request) {
        try {
            return Math.max(0, Integer.parseInt(request.getParameter("offset")));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String subjectCourseBasePath(HttpServletRequest request) {
        return isLimitedSubjectRequest(request) ? "/courses" : "/admin/courses";
    }

    private static String subjectListJsp(HttpServletRequest request) {
        if (isCoordinatorSubjectRequest(request)) {
            return COORDINATOR_SUBJECT_LIST_JSP;
        }
        return isTeacherSubjectRequest(request) ? INSTRUCTOR_SUBJECT_LIST_JSP : ADMIN_SUBJECT_LIST_JSP;
    }

    private static String subjectFormJsp(HttpServletRequest request) {
        if (isCoordinatorSubjectRequest(request)) {
            return COORDINATOR_SUBJECT_FORM_JSP;
        }
        return isTeacherSubjectRequest(request) ? INSTRUCTOR_SUBJECT_FORM_JSP : ADMIN_SUBJECT_FORM_JSP;
    }

    private static String subjectDetailJsp(HttpServletRequest request) {
        if (isCoordinatorSubjectRequest(request)) {
            return COORDINATOR_SUBJECT_DETAIL_JSP;
        }
        return isTeacherSubjectRequest(request) ? INSTRUCTOR_SUBJECT_DETAIL_JSP : ADMIN_SUBJECT_DETAIL_JSP;
    }

    private static boolean isCoordinatorSubjectRequest(HttpServletRequest request) {
        return request.getServletPath() != null && request.getServletPath().startsWith("/coordinator/subjects");
    }

    private static boolean isTeacherSubjectRequest(HttpServletRequest request) {
        return request.getServletPath() != null && request.getServletPath().startsWith("/instructor/subjects");
    }

    private static boolean isLimitedSubjectRequest(HttpServletRequest request) {
        return isCoordinatorSubjectRequest(request) || isTeacherSubjectRequest(request);
    }

    private boolean canCreateAnySubject(SessionUser actor, List<Organization> organizations, HttpServletRequest request) {
        for (Organization organization : organizations) {
            if (subjectService.canCreateSubject(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    organization.id(),
                    request.getRemoteAddr()
            )) {
                return true;
            }
        }
        return false;
    }

    private Map<Long, Boolean> canModifySubjectById(
            SessionUser actor,
            List<Subject> subjects,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Subject subject : subjects) {
            permissions.put(subject.id(), subjectService.canModifySubject(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    subject.id(),
                    request.getRemoteAddr()
            ));
        }
        return permissions;
    }

    private Map<Long, Boolean> canManageSubjectAssociationsById(
            SessionUser actor,
            List<Subject> subjects,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Subject subject : subjects) {
            permissions.put(subject.id(), canManageSubjectAssociations(actor, subject.id(), request));
        }
        return permissions;
    }

    private boolean canManageSubjectAssociations(
            SessionUser actor,
            long subjectId,
            HttpServletRequest request
    ) {
        return canManageSubjectAssociations(actor, subjectId, subjectCourseAssociations(subjectId), request);
    }

    private boolean canManageSubjectAssociations(
            SessionUser actor,
            long subjectId,
            List<CourseSubjectAssociation> associations,
            HttpServletRequest request
    ) {
        if (courseSubjectService.canManageSubjectAssociations(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                subjectId,
                request.getRemoteAddr()
        )) {
            return true;
        }
        return associations.stream()
                .anyMatch(association -> courseSubjectService.canManageAssociation(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        association.courseId(),
                        subjectId,
                        request.getRemoteAddr()
                ));
    }

    private Map<Long, Map<Long, List<ClassGroupView>>> subjectCourseClassGroupsBySubjectAndCourse(
            SessionUser actor,
            Map<Long, List<SubjectCourseView>> coursesBySubject,
            HttpServletRequest request
    ) {
        Map<Long, Map<Long, List<ClassGroupView>>> classGroupsBySubjectAndCourse = new HashMap<>();
        for (Map.Entry<Long, List<SubjectCourseView>> subjectEntry : coursesBySubject.entrySet()) {
            Map<Long, List<ClassGroupView>> classGroupsByCourse = new HashMap<>();
            for (SubjectCourseView association : subjectEntry.getValue()) {
                classGroupsByCourse.put(
                        association.getCourseId(),
                        viewFactory.classGroupViews(classGroupsByCourseSubject(
                                association.getCourseId(),
                                subjectEntry.getKey()
                        ).stream()
                                .filter(classGroup -> canReadClassGroup(request, actor, classGroup.id()))
                                .toList())
                );
            }
            classGroupsBySubjectAndCourse.put(subjectEntry.getKey(), classGroupsByCourse);
        }
        return classGroupsBySubjectAndCourse;
    }

    private static Map<Long, List<ClassGroupView>> classGroupsBySubject(
            Map<Long, List<SubjectCourseView>> coursesBySubject,
            Map<Long, Map<Long, List<ClassGroupView>>> classGroupsBySubjectAndCourse
    ) {
        Map<Long, List<ClassGroupView>> classGroupsBySubject = new HashMap<>();
        for (Map.Entry<Long, List<SubjectCourseView>> subjectEntry : coursesBySubject.entrySet()) {
            List<ClassGroupView> classGroups = new ArrayList<>();
            Map<Long, List<ClassGroupView>> classGroupsByCourse =
                    classGroupsBySubjectAndCourse.getOrDefault(subjectEntry.getKey(), Map.of());
            for (SubjectCourseView association : subjectEntry.getValue()) {
                classGroups.addAll(classGroupsByCourse.getOrDefault(association.getCourseId(), List.of()));
            }
            classGroupsBySubject.put(subjectEntry.getKey(), classGroups);
        }
        return classGroupsBySubject;
    }

    private static Set<Long> courseIdsFrom(Map<Long, List<SubjectCourseView>> coursesBySubject) {
        Set<Long> courseIds = new HashSet<>();
        for (List<SubjectCourseView> associations : coursesBySubject.values()) {
            for (SubjectCourseView association : associations) {
                courseIds.add(association.getCourseId());
            }
        }
        return courseIds;
    }

    private static Set<Long> classGroupIdsFrom(
            Map<Long, Map<Long, List<ClassGroupView>>> classGroupsBySubjectAndCourse
    ) {
        Set<Long> classGroupIds = new HashSet<>();
        for (Map<Long, List<ClassGroupView>> classGroupsByCourse : classGroupsBySubjectAndCourse.values()) {
            for (List<ClassGroupView> classGroups : classGroupsByCourse.values()) {
                for (ClassGroupView classGroup : classGroups) {
                    classGroupIds.add(classGroup.getId());
                }
            }
        }
        return classGroupIds;
    }

    private static Set<Long> classGroupIdsFrom(List<ClassGroupView> classGroups) {
        Set<Long> classGroupIds = new HashSet<>();
        for (ClassGroupView classGroup : classGroups) {
            classGroupIds.add(classGroup.getId());
        }
        return classGroupIds;
    }

    private static Map<Long, String> subjectGradeSheetOccurrenceLabels(
            List<GradeCertificateServlet.GradeSheetSubjectGroupView> occurrenceGroups,
            List<ClassGroupView> classGroups
    ) {
        Map<Long, String> labels = new HashMap<>();
        for (GradeCertificateServlet.GradeSheetSubjectGroupView occurrenceGroup : occurrenceGroups) {
            Set<Long> sourceClassGroupIds = new HashSet<>();
            occurrenceGroup.getClassGroupSheets().forEach(sheet -> sourceClassGroupIds.addAll(sheet.getClassGroupIds()));
            ClassGroupView sourceClassGroup = classGroups.stream()
                    .filter(classGroup -> classGroup.getCourseOccurrenceId() == occurrenceGroup.getOccurrenceId())
                    .filter(classGroup -> classGroup.getCourseId() == occurrenceGroup.getCourseId())
                    .filter(classGroup -> sourceClassGroupIds.contains(classGroup.getId()))
                    .findFirst()
                    .orElse(null);
            String label = sourceClassGroup == null
                    ? occurrenceGroup.getOccurrenceLabel() + " — " + occurrenceGroup.getCourseName()
                    : occurrenceStructureLabel(sourceClassGroup);
            labels.put(occurrenceGroup.getOccurrenceId(), label);
        }
        return Map.copyOf(labels);
    }

    private static List<ClassGroupOccurrenceGroupView> classGroupOccurrenceGroups(List<ClassGroupView> classGroups) {
        Map<Long, List<ClassGroupView>> byOccurrencePeriod = new LinkedHashMap<>();
        for (ClassGroupView classGroup : classGroups) {
            byOccurrencePeriod.computeIfAbsent(classGroup.getCourseOccurrencePeriodId(), ignored -> new ArrayList<>())
                    .add(classGroup);
        }
        List<ClassGroupOccurrenceGroupView> groups = new ArrayList<>();
        for (List<ClassGroupView> occurrenceClassGroups : byOccurrencePeriod.values()) {
            if (occurrenceClassGroups.isEmpty()) {
                continue;
            }
            ClassGroupView first = occurrenceClassGroups.get(0);
            groups.add(new ClassGroupOccurrenceGroupView(
                    first.getCourseOccurrenceId(),
                    first.getCourseOccurrencePeriodId(),
                    occurrenceStructureLabel(first),
                    first.getOccurrencePeriodDateRangeLabel(),
                    first.getOccurrencePeriodStateLabel(),
                    first.getOccurrencePeriodStateBadgeClass(),
                    occurrenceClassGroups
            ));
        }
        return List.copyOf(groups);
    }

    private static String occurrenceStructureLabel(ClassGroupView classGroup) {
        String occurrenceLabel = classGroup.getOccurrenceLabel();
        String periodLabel = classGroup.getOccurrencePeriodLabel();
        if (periodLabel == null || periodLabel.isBlank() || "-".equals(periodLabel)) {
            return occurrenceLabel + " — " + classGroup.getCourseName();
        }
        return occurrenceLabel + " - " + periodLabel + " — " + classGroup.getCourseName();
    }

    private Map<Long, Boolean> canModifyCourseById(
            SessionUser actor,
            Set<Long> courseIds,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Long courseId : courseIds) {
            permissions.put(courseId, courseService.canModifyCourse(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    courseId,
                    request.getRemoteAddr()
            ));
        }
        return permissions;
    }

    private Map<Long, Boolean> canManageCourseChildrenById(
            SessionUser actor,
            Set<Long> courseIds,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Long courseId : courseIds) {
            permissions.put(courseId, courseService.canManageCourseChildren(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    courseId,
                    request.getRemoteAddr()
            ));
        }
        return permissions;
    }

    private Map<Long, Boolean> canModifyClassGroupById(
            SessionUser actor,
            Set<Long> classGroupIds,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Long classGroupId : classGroupIds) {
            permissions.put(classGroupId, classGroupService.canModifyClassGroup(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    classGroupId,
                    request.getRemoteAddr()
            ));
        }
        return permissions;
    }

    private Map<Long, Boolean> canManageClassGroupStructureById(
            SessionUser actor,
            Set<Long> classGroupIds,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Long classGroupId : classGroupIds) {
            permissions.put(classGroupId, classGroupService.canManageClassGroupStructure(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    classGroupId,
                    request.getRemoteAddr()
            ));
        }
        return permissions;
    }

    private ClassGroupCreationAvailability classGroupCreationAvailabilityForSubject(
            SessionUser actor,
            long subjectId,
            List<CourseSubjectAssociation> associations,
            HttpServletRequest request
    ) {
        if (associations.isEmpty()) {
            return ClassGroupCreationAvailability.unavailable(
                    "Associate this subject with an active course before creating a class group."
            );
        }
        try {
            for (CourseSubjectAssociation association : associations) {
                if (classGroupService.canCreateClassGroup(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        association.courseId(),
                        subjectId,
                        request.getRemoteAddr()
                )) {
                    return ClassGroupCreationAvailability.available();
                }
            }
            return ClassGroupCreationAvailability.unavailable(
                    "You do not have permission to create a class group for this subject's active course associations."
            );
        } catch (RuntimeException exception) {
            return ClassGroupCreationAvailability.unavailable(
                    "Class group creation is temporarily unavailable."
            );
        }
    }

    private record ClassGroupCreationAvailability(boolean allowed, String unavailableReason) {

        private static ClassGroupCreationAvailability available() {
            return new ClassGroupCreationAvailability(true, "");
        }

        private static ClassGroupCreationAvailability unavailable(String reason) {
            return new ClassGroupCreationAvailability(false, reason);
        }
    }

    private List<CourseView> availableCourseOptions(
            HttpServletRequest request,
            Subject subject,
            List<SubjectCourseView> associations
    ) {
        SessionUser actor = requireCurrentUser(request);
        Set<Long> associatedCourseIds = new HashSet<>();
        for (SubjectCourseView association : associations) {
            associatedCourseIds.add(association.getCourseId());
        }
        return coursesByOrganization(subject.organizationId()).stream()
                .filter(course -> course.state() == CourseState.ACTIVE)
                .filter(course -> !associatedCourseIds.contains(course.id()))
                .filter(course -> courseSubjectService.canManageAssociation(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        course.id(),
                        subject.id(),
                        request.getRemoteAddr()
                ))
                .map(viewFactory::courseView)
                .sorted(Comparator.comparing(CourseView::getName))
                .toList();
    }

    private List<Course> coursesByOrganization(long organizationId) {
        try {
            return courseDAO.findByOrganization(organizationId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load course options", exception);
        }
    }

    private List<pt.isel.gape.learning.model.ClassGroup> classGroupsBySubject(long subjectId) {
        try {
            return classGroupDAO.findBySubject(subjectId).stream()
                    .sorted(Comparator.comparingLong(pt.isel.gape.learning.model.ClassGroup::id).reversed())
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load subject class groups", exception);
        }
    }

    private List<pt.isel.gape.learning.model.ClassGroup> classGroupsByCourseSubject(long courseId, long subjectId) {
        try {
            return classGroupDAO.findByCourseAndSubject(courseId, subjectId).stream()
                    .sorted(Comparator.comparingLong(pt.isel.gape.learning.model.ClassGroup::id).reversed())
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load subject class groups", exception);
        }
    }

    private Map<Long, List<SubjectCourseView>> subjectCoursesBySubject(
            SessionUser actor,
            List<SubjectView> subjects,
            HttpServletRequest request
    ) {
        Map<Long, List<SubjectCourseView>> coursesBySubject = new HashMap<>();
        for (SubjectView subject : subjects) {
            coursesBySubject.put(subject.getId(), subjectCourseViews(subject.getId()).stream()
                    .filter(association -> canReadSubjectCourse(
                            actor,
                            subject.getId(),
                            association.getCourseId(),
                            request
                    ))
                    .toList());
        }
        return coursesBySubject;
    }

    private boolean canReadSubjectCourse(
            SessionUser actor,
            long subjectId,
            long courseId,
            HttpServletRequest request
    ) {
        if (primaryProfile(actor) != AccessProfileType.TEACHER) {
            return true;
        }
        return classGroupsByCourseSubject(courseId, subjectId).stream()
                .anyMatch(classGroup -> canReadClassGroup(request, actor, classGroup.id()));
    }

    private boolean canReadClassGroup(HttpServletRequest request, SessionUser actor, long classGroupId) {
        try {
            return classGroupService.canReadClassGroup(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    classGroupId,
                    request.getRemoteAddr()
            );
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private List<SubjectCourseView> subjectCourseViews(long subjectId) {
        return subjectCourseViews(subjectCourseAssociations(subjectId));
    }

    private List<SubjectCourseView> subjectCourseViews(List<CourseSubjectAssociation> associations) {
        try {
            Map<Long, Course> coursesById = new HashMap<>();
            for (Course course : courseDAO.findByIds(associations.stream().map(CourseSubjectAssociation::courseId).toList())) {
                coursesById.put(course.id(), course);
            }
            return associations.stream()
                    .map(association -> {
                        Course course = coursesById.get(association.courseId());
                        if (course == null) {
                            throw new IllegalArgumentException("Course not found: " + association.courseId());
                        }
                        return SubjectCourseView.from(association, viewFactory.courseView(course));
                    })
                    .sorted(Comparator.comparingLong(SubjectCourseView::getCourseId).reversed())
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load subject course associations", exception);
        }
    }

    private List<CourseSubjectAssociation> subjectCourseAssociations(long subjectId) {
        try {
            return courseSubjectDAO.findBySubject(subjectId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load subject course associations", exception);
        }
    }

    private List<UserOptionView> coordinatorOptions(HttpServletRequest request, Long selectedId) {
        SessionUser actor = requireCurrentUser(request);
        try {
            return userService.listUsers(actor.userId(), currentSessionId(request), primaryProfile(actor), request.getRemoteAddr())
                    .stream()
                    .filter(SubjectManagementServlet::isActiveCoordinator)
                    .map(user -> UserOptionView.from(user, selectedId != null && selectedId == user.id()))
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private List<CoordinatorAssignmentView> coordinatorAssignments(long subjectId) {
        try {
            return coordinateSubjectDAO.findBySubject(subjectId).stream()
                    .map(CoordinatorAssignmentView::from)
                    .sorted(Comparator.comparingLong(CoordinatorAssignmentView::getCoordinatorUserId).reversed())
                    .toList();
        } catch (RuntimeException | SQLException exception) {
            return List.of();
        }
    }

    private long selectedOrganizationId(HttpServletRequest request, SubjectFormData form) {
        Long submitted = parseOptionalLong(form.getOrganizationId());
        if (submitted != null) {
            return submitted;
        }
        return 0;
    }

    private static long selectedOrganizationId(List<Organization> organizations, SubjectFormData form) {
        Long submitted = parseOptionalLong(form.getOrganizationId());
        if (submitted != null) {
            return submitted;
        }
        return 0L;
    }

    private static boolean isActiveCoordinator(User user) {
        return user.state() == UserState.ACTIVE
                && user.accessProfiles().stream()
                .map(AccessProfile::type)
                .anyMatch(AccessProfileType.COORDINATOR::equals);
    }

    private static void prepareSubjectContext(HttpServletRequest request, SubjectView subject, String activeChild) {
        request.setAttribute("adminSubjectContextId", subject.getId());
        request.setAttribute("adminSubjectContextName", subject.getName());
        request.setAttribute("adminSubjectActiveChild", activeChild);
        request.setAttribute("coordinatorSubjectContextId", subject.getId());
        request.setAttribute("coordinatorSubjectContextName", subject.getName());
        request.setAttribute("coordinatorSubjectActiveChild", activeChild);
        request.setAttribute("teacherSubjectContextId", subject.getId());
        request.setAttribute("teacherSubjectContextName", subject.getName());
        request.setAttribute("teacherSubjectActiveChild", activeChild);
    }

    private static SubjectState subjectState(String value) {
        return enumValue(SubjectState.class, value, SubjectState.ACTIVE, "subject state");
    }

    private static CurricularTerm curricularTerm(String value) {
        return enumValue(CurricularTerm.class, value, null, "curricular term");
    }

    private static RoleAssignmentState roleAssignmentState(String value) {
        if (value == null || value.isBlank()) {
            return RoleAssignmentState.ACTIVE;
        }
        try {
            return RoleAssignmentState.fromDatabaseValue(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid coordinator assignment state: " + value, exception);
        }
    }

    private static <T extends Enum<T>> T enumValue(Class<T> enumType, String value, T defaultValue, String fieldLabel) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid " + fieldLabel + ": " + value, exception);
        }
    }

    private static LocalDate optionalDate(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : ApplicationDateTimeFormat.parseUserDate(value);
    }

    private static Integer optionalInteger(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : Integer.parseInt(value);
    }

    private static BigDecimal optionalBigDecimal(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : new BigDecimal(value);
    }

    private static BigDecimal requiredBigDecimal(HttpServletRequest request, String name) {
        BigDecimal value = optionalBigDecimal(request, name);
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static Part subjectImagePart(HttpServletRequest request) throws IOException, ServletException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            return null;
        }
        return request.getPart("subjectImage");
    }

    private static Long parseOptionalLong(String value) {
        return value == null || value.isBlank() ? null : Long.parseLong(value);
    }
}
