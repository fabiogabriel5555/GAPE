package pt.isel.gape.web.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseCreateCommand;
import pt.isel.gape.learning.model.CourseEnrollmentCommand;
import pt.isel.gape.learning.model.CourseFrequency;
import pt.isel.gape.learning.model.CourseOccurrenceCreateCommand;
import pt.isel.gape.learning.model.CoursePeriodTemplateCommand;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.CourseSubjectAssociationCommand;
import pt.isel.gape.learning.model.CourseType;
import pt.isel.gape.learning.model.CourseUpdateCommand;
import pt.isel.gape.learning.model.CurricularTerm;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.SubjectState;
import pt.isel.gape.learning.service.ClassGroupService;
import pt.isel.gape.learning.service.CourseOccurrenceService;
import pt.isel.gape.learning.service.CourseService;
import pt.isel.gape.learning.service.CourseSubjectService;
import pt.isel.gape.learning.service.EnrollmentService;
import pt.isel.gape.learning.service.SubjectService;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.structure.service.OrganicUnitService;
import pt.isel.gape.structure.service.OrganizationService;
import pt.isel.gape.transversal.service.ApplicationReadService;
import pt.isel.gape.web.media.ProfilePhotoStorage;
import pt.isel.gape.web.view.ClassGroupView;
import pt.isel.gape.web.view.CourseOccurrenceView;
import pt.isel.gape.web.view.CourseFormData;
import pt.isel.gape.web.view.CoursePeriodTemplateView;
import pt.isel.gape.web.view.CourseSubjectView;
import pt.isel.gape.web.view.CourseView;
import pt.isel.gape.web.view.EnrollmentManagementView;
import pt.isel.gape.web.view.OrganicUnitView;
import pt.isel.gape.web.view.OrganizationView;
import pt.isel.gape.web.view.SelectOptionView;
import pt.isel.gape.web.view.SubjectView;
import pt.isel.gape.web.view.UserOptionView;

@WebServlet(name = "courseManagementServlet", urlPatterns = {
        "/admin/courses",
        "/admin/courses/*",
        "/coordinator/courses",
        "/coordinator/courses/*"
})
@MultipartConfig(maxFileSize = 50L * 1024L * 1024L, maxRequestSize = 52L * 1024L * 1024L)
public final class CourseManagementServlet extends DashboardServletSupport {

    private static final String COURSE_LIST_JSP = "/admin/admin/course/admin-courses.jsp";
    private static final int LIST_PAGE_SIZE = 10;
    private static final String COURSE_FORM_JSP = "/admin/admin/course/admin-course-form.jsp";
    private static final String COURSE_DETAIL_JSP = "/admin/admin/course/admin-course-detail.jsp";
    private static final String COURSE_OCCURRENCES_FRAGMENT_JSP = "/WEB-INF/fragments/course-occurrences-panel.jsp";
    private static final String COURSE_ENROLLMENTS_FRAGMENT_JSP = "/WEB-INF/fragments/course-enrollments-panel.jsp";
    private static final String COURSE_ASSOCIATE_SUBJECT_MODAL_FRAGMENT_JSP =
            "/WEB-INF/fragments/course-associate-subject-modal.jsp";
    private static final String COURSE_CLASS_GROUP_ACTIVITIES_FRAGMENT_JSP =
            "/WEB-INF/fragments/course-class-group-activities.jsp";
    private static final String COURSE_SUBJECT_CLASS_GROUPS_FRAGMENT_JSP =
            "/WEB-INF/fragments/course-subject-class-groups.jsp";
    private static final String COURSE_PERIOD_TEMPLATE_PAYLOAD = "periodTemplates";

    private final CourseService courseService;
    private final CourseOccurrenceService courseOccurrenceService;
    private final SubjectService subjectService;
    private final CourseSubjectService courseSubjectService;
    private final EnrollmentService enrollmentService;
    private final ClassGroupService classGroupService;
    private final OrganizationService organizationService;
    private final OrganicUnitService organicUnitService;
    private final ApplicationReadService.Organizations organizationDAO;
    private final ApplicationReadService.Subjects subjectDAO;
    private final ApplicationReadService.CourseSubjects courseSubjectDAO;
    private final ApplicationReadService.CoursePeriodTemplates coursePeriodTemplateDAO;
    private final ApplicationReadService.ClassGroups classGroupDAO;
    private final ApplicationReadService.Enrollments enrollmentDAO;
    private final LearningViewFactory viewFactory;
    private final ProfilePhotoStorage photoStorage;
    private final ClassGroupActivityViewSupport activityViewSupport;

    public CourseManagementServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private CourseManagementServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new ApplicationReadService(connectionProvider),
                new CourseService(connectionProvider, clock),
                new CourseOccurrenceService(connectionProvider, clock),
                new SubjectService(connectionProvider, clock),
                new CourseSubjectService(connectionProvider, clock),
                new EnrollmentService(connectionProvider, clock),
                new ClassGroupService(connectionProvider, clock),
                new OrganizationService(connectionProvider, clock),
                new OrganicUnitService(connectionProvider, clock),
                new ProfilePhotoStorage(),
                new ClassGroupActivityViewSupport(connectionProvider, clock)
        );
    }

    CourseManagementServlet(
            ApplicationReadService readService,
            CourseService courseService,
            CourseOccurrenceService courseOccurrenceService,
            SubjectService subjectService,
            CourseSubjectService courseSubjectService,
            EnrollmentService enrollmentService,
            ClassGroupService classGroupService,
            OrganizationService organizationService,
            OrganicUnitService organicUnitService,
            ProfilePhotoStorage photoStorage,
            ClassGroupActivityViewSupport activityViewSupport
    ) {
        this.courseService = courseService;
        this.courseOccurrenceService = courseOccurrenceService;
        this.subjectService = subjectService;
        this.courseSubjectService = courseSubjectService;
        this.enrollmentService = enrollmentService;
        this.classGroupService = classGroupService;
        this.organizationService = organizationService;
        this.organicUnitService = organicUnitService;
        this.organizationDAO = readService.organizations();
        this.subjectDAO = readService.subjects();
        this.courseSubjectDAO = readService.courseSubjects();
        this.coursePeriodTemplateDAO = readService.coursePeriodTemplates();
        this.classGroupDAO = readService.classGroups();
        this.enrollmentDAO = readService.enrollments();
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
                if (isCoordinatorCourseRequest(request)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                // A new course starts without context. The Organic Unit control is
                // intentionally unavailable until the user explicitly chooses an
                // Organization in the form.
                showCourseForm(request, response, CourseFormData.blank(null), true, null);
                return;
            }
            if (segments.length == 1) {
                showDetail(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2 && "occurrences".equals(segments[1])) {
                showOccurrencesFragment(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2 && "enrollments".equals(segments[1])) {
                showEnrollmentsFragment(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2 && "associate-subject".equals(segments[1])) {
                showAssociateSubjectModalFragment(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 4
                    && "subjects".equals(segments[1])
                    && "class-groups".equals(segments[3])) {
                showSubjectClassGroupsFragment(
                        request,
                        response,
                        Long.parseLong(segments[0]),
                        Long.parseLong(segments[2])
                );
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
                if (isCoordinatorCourseRequest(request)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                showEditForm(request, response, Long.parseLong(segments[0]), null);
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
        if (isCoordinatorCourseRequest(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String[] segments = pathSegments(request.getPathInfo());
        try {
            if (segments.length == 0) {
                createCourse(request, response);
                return;
            }
            if (segments.length == 1) {
                updateCourse(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2 && "subjects".equals(segments[1])) {
                associateSubject(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 3 && "subjects".equals(segments[1])) {
                updateSubjectAssociation(
                        request,
                        response,
                        Long.parseLong(segments[0]),
                        Long.parseLong(segments[2])
                );
                return;
            }
            if (segments.length == 2 && "occurrences".equals(segments[1])) {
                createCourseOccurrence(request, response, Long.parseLong(segments[0]));
                return;
            }
            if (segments.length == 2) {
                long courseId = Long.parseLong(segments[0]);
                switch (segments[1]) {
                    case "archive" -> archiveCourse(request, response, courseId);
                    case "unarchive" -> unarchiveCourse(request, response, courseId);
                    case "delete" -> deleteCourse(request, response, courseId);
                    case "enrollments" -> enrollStudentInCourse(request, response, courseId);
                    default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                return;
            }
            if (segments.length == 4 && "enrollments".equals(segments[1])) {
                long courseId = Long.parseLong(segments[0]);
                long studentUserId = Long.parseLong(segments[2]);
                switch (segments[3]) {
                    case "update" -> updateCourseEnrollment(request, response, courseId, studentUserId);
                    case "delete" -> deleteCourseEnrollment(request, response, courseId, studentUserId);
                    case "withdraw" -> withdrawStudentFromCourse(request, response, courseId, studentUserId);
                    default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
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
        List<Organization> organizations = courseOrganizations(request);
        if (isListRowsFragmentRequest(request)) {
            prepareCourseListRows(
                    request,
                    actor,
                    organizations,
                    requestedListOffset(request),
                    requestedListLoadAll(request)
            );
            forward(request, response, "/WEB-INF/fragments/course-list-rows.jsp");
            return;
        }
        prepareCourseListRows(request, actor, organizations, 0, false);
        prepareCourseBasePaths(request);
        request.setAttribute("canCreateCourses", canCreateAnyCourse(actor, organizations, request));
        prepareDashboard(
                request,
                "courses",
                "Courses",
                (Boolean) request.getAttribute("canCreateCourses") ? courseBasePath(request) + "/new" : null,
                (Boolean) request.getAttribute("canCreateCourses") ? "New Course" : null
        );
        forward(request, response, COURSE_LIST_JSP);
    }

    private void prepareCourseListRows(
            HttpServletRequest request,
            SessionUser actor,
            List<Organization> organizations,
            int requestedOffset,
            boolean loadAll
    ) {
        List<Course> rawCourses = organizations.stream()
                .flatMap(organization -> courseService.listCourses(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        organization.id(),
                        request.getRemoteAddr()
                ).stream())
                .sorted(Comparator.comparingLong(Course::id).reversed())
                .toList();
        int total = rawCourses.size();
        int fromIndex = Math.min(Math.max(0, requestedOffset), total);
        int toIndex = loadAll ? total : Math.min(fromIndex + LIST_PAGE_SIZE, total);
        List<Course> pageCourses = rawCourses.subList(fromIndex, toIndex);
        List<CourseView> courses = pageCourses.stream()
                .map(viewFactory::courseView)
                .toList();
        Map<Long, Long> activeEnrollmentCountByCourse = activeCourseEnrollmentCountByCourse(pageCourses);

        prepareCourseBasePaths(request);
        request.setAttribute("courses", courses);
        request.setAttribute("canCreateCourses", canCreateAnyCourse(actor, organizations, request));
        request.setAttribute("canModifyCourseById", canModifyCourseById(actor, pageCourses, request));
        request.setAttribute("activeEnrollmentCountByCourse", activeEnrollmentCountByCourse);
        request.setAttribute("courseCount", total);
        request.setAttribute("activeCourses", rawCourses.stream().filter(course -> course.state() == CourseState.ACTIVE).count());
        request.setAttribute("inactiveCourses", rawCourses.stream().filter(course -> course.state() == CourseState.INACTIVE).count());
        request.setAttribute("courseListOffset", fromIndex);
        request.setAttribute("courseListCurrentPage", fromIndex / LIST_PAGE_SIZE + 1);
        request.setAttribute("courseListLoadAll", loadAll);
        request.setAttribute("courseListHasMore", !loadAll && toIndex < total);
        request.setAttribute("courseListNextOffset", toIndex);
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response, long courseId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Course course = courseService.getCourse(actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr());
        markLearningEventsReadForCurrentUser(request, "/admin/courses/" + course.id(), true);
        CourseView courseView = viewFactory.courseView(course);
        boolean canModifyCourse = courseService.canModifyCourse(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                courseId,
                request.getRemoteAddr()
        );
        boolean canManageCourseChildren = courseService.canManageCourseChildren(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                courseId,
                request.getRemoteAddr()
        ) && !courseView.isInactive();
        List<CourseSubjectView> courseSubjects = filterCourseSubjectsForActor(
                request,
                actor,
                viewFactory.courseSubjects(courseId, null)
        );
        BigDecimal subjectEctsTotal = subjectEctsTotal(courseSubjects);
        boolean courseEctsConsistent = course.ects() != null && subjectEctsTotal.compareTo(course.ects()) == 0;
        Set<Long> subjectIds = subjectIdsFrom(courseSubjects);
        Set<Long> visibleSubjectIds = subjectIdsFrom(courseSubjects);
        Map<Long, Long> activeClassGroupCountBySubject = new HashMap<>();
        for (Long subjectId : visibleSubjectIds) {
            activeClassGroupCountBySubject.put(subjectId, 0L);
        }
        for (pt.isel.gape.learning.model.ClassGroup classGroup : classGroupsByCourse(courseId)) {
            if (visibleSubjectIds.contains(classGroup.subjectId())
                    && classGroup.state() == pt.isel.gape.learning.model.ClassGroupState.ACTIVE) {
                activeClassGroupCountBySubject.merge(classGroup.subjectId(), 1L, Long::sum);
            }
        }
        List<pt.isel.gape.learning.model.CourseOccurrence> courseOccurrenceSummary =
                viewFactory.courseOccurrenceSummary(courseId);
        long activeCourseEnrollmentCount = activeCourseEnrollmentCountByCourse(List.of(course))
                .getOrDefault(courseId, 0L);
        prepareCourseBasePaths(request);
        request.setAttribute("course", courseView);
        request.setAttribute("courseSubjects", courseSubjects);
        request.setAttribute("courseSubjectEctsTotalLabel", formatDecimal(subjectEctsTotal) + " ECTS");
        request.setAttribute("courseEctsTargetLabel", courseView.getEctsLabel());
        request.setAttribute("courseEctsConsistent", courseEctsConsistent);
        request.setAttribute("activeClassGroupCountBySubject", activeClassGroupCountBySubject);
        request.setAttribute("courseOccurrenceCount", courseOccurrenceSummary.size());
        request.setAttribute("activeCourseOccurrenceCount", courseOccurrenceSummary.stream()
                .filter(occurrence -> occurrence.state() == pt.isel.gape.learning.model.CourseOccurrenceState.ACTIVE)
                .count());
        request.setAttribute("activeCourseEnrollmentCount", activeCourseEnrollmentCount);
        request.setAttribute("canModifySubjectById", canModifySubjectById(actor, subjectIds, request));
        request.setAttribute("canManageSubjectAssociationsById",
                canManageSubjectAssociationsById(actor, subjectIds, request));
        request.setAttribute("canModifyCourse", canModifyCourse);
        request.setAttribute("canManageCourseChildren", canManageCourseChildren);
        request.setAttribute("canManageCourseEnrollments",
                !isCoordinatorCourseRequest(request) && canManageCourseChildren);
        prepareCourseContext(request, courseView, "detail");
        prepareDashboard(request, "courses", "Course Details");
        forward(request, response, COURSE_DETAIL_JSP);
    }

    private void showOccurrencesFragment(
            HttpServletRequest request,
            HttpServletResponse response,
            long courseId
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Course course = courseService.getCourse(
                actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr()
        );
        CourseView courseView = viewFactory.courseView(course);
        boolean canManageCourseChildren = courseService.canManageCourseChildren(
                actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr()
        ) && !courseView.isInactive();
        prepareCourseDetailFragment(request, courseId);
        request.setAttribute("course", courseView);
        request.setAttribute("courseOccurrences", courseOccurrenceViewsNewestFirst(courseId));
        request.setAttribute("coursePeriodTemplates", coursePeriodTemplateViews(courseId, course));
        request.setAttribute("canManageCourseChildren", canManageCourseChildren);
        forward(request, response, COURSE_OCCURRENCES_FRAGMENT_JSP);
    }

    private void showEnrollmentsFragment(
            HttpServletRequest request,
            HttpServletResponse response,
            long courseId
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Course course = courseService.getCourse(
                actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr()
        );
        CourseView courseView = viewFactory.courseView(course);
        boolean canManageCourseChildren = courseService.canManageCourseChildren(
                actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr()
        ) && !courseView.isInactive();
        boolean canManageCourseEnrollments = !isCoordinatorCourseRequest(request) && canManageCourseChildren;
        List<EnrollmentManagementView> courseEnrollments = courseEnrollmentViewsNewestFirst(courseId);
        prepareCourseDetailFragment(request, courseId);
        request.setAttribute("course", courseView);
        request.setAttribute("courseOccurrences", courseOccurrenceViewsNewestFirst(courseId));
        request.setAttribute("studentOptions", canManageCourseEnrollments ? viewFactory.activeStudentOptions(null) : List.of());
        request.setAttribute("canManageCourseEnrollments", canManageCourseEnrollments);
        request.setAttribute("courseEnrollmentEmbedded", Boolean.TRUE);
        exposeCourseEnrollmentManagement(request, courseEnrollments);
        forward(request, response, COURSE_ENROLLMENTS_FRAGMENT_JSP);
    }

    private void showAssociateSubjectModalFragment(
            HttpServletRequest request,
            HttpServletResponse response,
            long courseId
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Course course = courseService.getCourse(
                actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr()
        );
        boolean canManageCourseChildren = courseService.canManageCourseChildren(
                actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr()
        ) && course.state() != CourseState.INACTIVE;
        if (!canManageCourseChildren) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        Set<Long> associatedSubjectIds = viewFactory.courseSubjects(courseId, null).stream()
                .map(CourseSubjectView::getSubjectId)
                .collect(java.util.stream.Collectors.toSet());
        List<SubjectView> availableSubjectOptions = availableSubjects().stream()
                .filter(subject -> subject.state() == SubjectState.ACTIVE)
                .filter(subject -> !associatedSubjectIds.contains(subject.id()))
                .filter(subject -> courseSubjectService.canManageAssociation(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        courseId,
                        subject.id(),
                        request.getRemoteAddr()
                ))
                .map(viewFactory::subjectView)
                .toList();
        prepareCourseDetailFragment(request, courseId);
        request.setAttribute("course", viewFactory.courseView(course));
        request.setAttribute("availableSubjectOptions", availableSubjectOptions);
        request.setAttribute("coursePeriodTemplates", coursePeriodTemplateViews(courseId, course));
        forward(request, response, COURSE_ASSOCIATE_SUBJECT_MODAL_FRAGMENT_JSP);
    }

    private void showClassGroupActivitiesFragment(
            HttpServletRequest request,
            HttpServletResponse response,
            long courseId,
            long classGroupId
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        courseService.getCourse(
                actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr()
        );
        pt.isel.gape.learning.model.ClassGroup classGroup;
        try {
            classGroup = classGroupDAO.findById(classGroupId).orElse(null);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load class group activities", exception);
        }
        if (classGroup == null || classGroup.courseId() != courseId) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!canViewCourseSubject(request, actor, courseId, classGroup.subjectId())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        prepareCourseDetailFragment(request, courseId);
        request.setAttribute("classGroup", viewFactory.classGroupView(classGroup));
        activityViewSupport.exposeClassGroupActivities(
                request,
                actor,
                currentSessionId(request),
                primaryProfile(actor),
                List.of(classGroupId)
        );
        forward(request, response, COURSE_CLASS_GROUP_ACTIVITIES_FRAGMENT_JSP);
    }

    private void showSubjectClassGroupsFragment(
            HttpServletRequest request,
            HttpServletResponse response,
            long courseId,
            long subjectId
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Course course = courseService.getCourse(
                actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr()
        );
        CourseSubjectView association = filterCourseSubjectsForActor(
                request,
                actor,
                viewFactory.courseSubjects(courseId, null)
        ).stream().filter(item -> item.getSubjectId() == subjectId).findFirst().orElse(null);
        if (association == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        List<ClassGroupView> classGroups = viewFactory.classGroupViews(classGroupsByCourseSubject(courseId, subjectId));
        Set<Long> classGroupIds = classGroupIdsFrom(classGroups);
        prepareCourseDetailFragment(request, courseId);
        request.setAttribute("course", viewFactory.courseView(course));
        request.setAttribute("association", association);
        request.setAttribute("subjectClassGroups", classGroups);
        request.setAttribute("canManageCourseChildren", courseService.canManageCourseChildren(
                actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr()
        ) && course.state() != CourseState.INACTIVE);
        request.setAttribute("canModifyClassGroupById", canModifyClassGroupById(actor, classGroupIds, request));
        request.setAttribute("canManageClassGroupStructureById",
                canManageClassGroupStructureById(actor, classGroupIds, request));
        activityViewSupport.exposeClassGroupActivityCounts(request, classGroupIds);
        forward(request, response, COURSE_SUBJECT_CLASS_GROUPS_FRAGMENT_JSP);
    }

    private void showCourseForm(
            HttpServletRequest request,
            HttpServletResponse response,
            CourseFormData form,
            boolean creating,
            String error
    ) throws ServletException, IOException {
        prepareCourseForm(request, form, creating, error);
        forward(request, response, COURSE_FORM_JSP);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response, long courseId, String error)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Course course = courseService.getCourse(actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr());
        if (!courseService.canModifyCourse(actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        CourseFormData form = error == null ? CourseFormData.from(course) : CourseFormData.from(request, courseId);
        prepareCourseForm(request, form, false, error);
        CourseView courseView = viewFactory.courseView(course);
        List<EnrollmentManagementView> courseEnrollments = viewFactory.courseEnrollmentViews(courseId);
        prepareCourseContext(request, courseView, "edit");
        request.setAttribute("course", courseView);
        request.setAttribute("studentOptions", viewFactory.activeStudentOptions(null));
        exposeCourseEnrollmentManagement(request, courseEnrollments);
        request.setAttribute("canModifyCourse", Boolean.TRUE);
        boolean canManageCourseChildren = courseService.canManageCourseChildren(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                courseId,
                request.getRemoteAddr()
        );
        request.setAttribute("canManageCourseChildren", canManageCourseChildren);
        request.setAttribute("canManageCourseEnrollments",
                !isCoordinatorCourseRequest(request) && canManageCourseChildren && !courseView.isInactive());
        forward(request, response, COURSE_FORM_JSP);
    }

    private void createCourse(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        CourseFormData form = CourseFormData.from(request, null);
        Part courseImage;
        try {
            courseImage = courseImagePart(request);
        } catch (IOException | ServletException exception) {
            showCourseForm(request, response, form, true, "The uploaded file could not be processed. Please try again.");
            return;
        }
        try {
            Course created = courseService.createCourse(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    courseCreateCommand(request),
                    request.getRemoteAddr()
            );
            try {
                created = attachUploadedPhotoToCreatedCourse(request, actor, created, courseImage);
            } catch (IOException exception) {
                flashError(request, "Course created, but the uploaded image could not be processed. Please edit the course and try again.");
                redirect(request, response, "/admin/courses/" + created.id());
                return;
            } catch (RuntimeException exception) {
                flashError(request, "Course created, but " + messageFor(exception));
                redirect(request, response, "/admin/courses/" + created.id());
                return;
            }
            flashSuccess(request, "Course created successfully.");
            redirect(request, response, "/admin/courses/" + created.id());
        } catch (RuntimeException exception) {
            showCourseForm(request, response, form, true, messageFor(exception));
        }
    }

    private Course attachUploadedPhotoToCreatedCourse(
            HttpServletRequest request,
            SessionUser actor,
            Course created,
            Part courseImage
    ) throws IOException {
        String uploadedPhoto = photoStorage.saveCoursePhoto(created.id(), courseImage, getServletContext());
        if (uploadedPhoto == null) {
            return created;
        }
        return courseService.attachCreatedCoursePhoto(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                created.id(),
                uploadedPhoto,
                request.getRemoteAddr()
        );
    }

    private void updateCourse(HttpServletRequest request, HttpServletResponse response, long courseId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            Course existing = courseService.getCourse(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    courseId,
                    request.getRemoteAddr()
            );
            String uploadedPhoto;
            try {
                uploadedPhoto = photoStorage.saveCoursePhoto(
                        courseId,
                        courseImagePart(request),
                        getServletContext()
                );
            } catch (IOException | ServletException exception) {
                showEditForm(request, response, courseId, "The uploaded file could not be processed. Please try again.");
                return;
            }
            String submittedPhoto = text(request, "photo");
            String photo = uploadedPhoto != null ? uploadedPhoto : (submittedPhoto != null ? submittedPhoto : existing.photo());
            courseService.updateCourse(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    courseId,
                    courseUpdateCommand(request, photo),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Course updated successfully.");
            redirect(request, response, "/admin/courses/" + courseId);
        } catch (RuntimeException exception) {
            showEditForm(request, response, courseId, messageFor(exception));
        }
    }

    private void archiveCourse(HttpServletRequest request, HttpServletResponse response, long courseId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            courseService.archiveCourse(actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr());
            flashSuccess(request, "Course deactivated.");
            redirect(request, response, isAjaxRequest(request)
                    ? courseBasePath(request) + "/" + courseId
                    : courseBasePath(request));
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirect(request, response, "/admin/courses/" + courseId);
        }
    }

    private void unarchiveCourse(HttpServletRequest request, HttpServletResponse response, long courseId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            courseService.unarchiveCourse(actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr());
            flashSuccess(request, "Course activated.");
            if (isAjaxRequest(request)) {
                redirect(request, response, courseBasePath(request) + "/" + courseId);
            } else {
                redirectToReturnPath(request, response, courseBasePath(request) + "/" + courseId);
            }
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirect(request, response, "/admin/courses/" + courseId);
        }
    }

    private void deleteCourse(HttpServletRequest request, HttpServletResponse response, long courseId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            courseService.deleteCourse(actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr());
            flashSuccess(request, "Course deleted.");
            redirect(request, response, "/admin/courses");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
            redirect(request, response, "/admin/courses/" + courseId);
        }
    }

    private void enrollStudentInCourse(HttpServletRequest request, HttpServletResponse response, long courseId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.enrollStudentInCourse(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    new CourseEnrollmentCommand(
                            longParameter(request, "studentUserId"),
                            courseId,
                            longParameter(request, "courseOccurrenceId")
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Student enrolled in course.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, courseBasePath(request) + "/" + courseId + "#course-students");
    }

    private void updateCourseEnrollment(
            HttpServletRequest request,
            HttpServletResponse response,
            long courseId,
            long studentUserId
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.updateCourseEnrollment(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    studentUserId,
                    courseId,
                    longParameter(request, "courseOccurrenceId"),
                    courseEnrollmentState(text(request, "state")),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Course enrollment updated.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, courseBasePath(request) + "/" + courseId + "#course-students");
    }

    private void withdrawStudentFromCourse(
            HttpServletRequest request,
            HttpServletResponse response,
            long courseId,
            long studentUserId
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.withdrawStudentFromCourse(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    studentUserId,
                    courseId,
                    longParameter(request, "courseOccurrenceId"),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Student removed from course.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, courseBasePath(request) + "/" + courseId + "#course-students");
    }

    private void deleteCourseEnrollment(
            HttpServletRequest request,
            HttpServletResponse response,
            long courseId,
            long studentUserId
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.deleteCourseEnrollment(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    studentUserId,
                    courseId,
                    longParameter(request, "courseOccurrenceId"),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Course enrollment deleted.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, courseBasePath(request) + "/" + courseId + "#course-students");
    }

    private void createCourseOccurrence(HttpServletRequest request, HttpServletResponse response, long courseId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            courseOccurrenceService.createOccurrence(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    new CourseOccurrenceCreateCommand(
                            courseId,
                            requiredInteger(request, "referenceYear")
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Course occurrence created.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, courseBasePath(request) + "/" + courseId + "#course-occurrences");
    }

    private void associateSubject(HttpServletRequest request, HttpServletResponse response, long courseId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            long subjectId = longParameter(request, "subjectId");
            courseSubjectService.associateSubject(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    associationCommand(request, courseId, subjectId),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Subject associated with course.");
            redirectToReturnPath(request, response, courseBasePath(request) + "/" + courseId + "#course-structure");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, courseBasePath(request) + "/" + courseId + "#course-structure");
    }

    private void updateSubjectAssociation(
            HttpServletRequest request,
            HttpServletResponse response,
            long courseId,
            long subjectId
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            courseSubjectService.updateAssociation(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    associationCommand(request, courseId, subjectId),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Subject association updated.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, courseBasePath(request) + "/" + courseId + "#course-structure");
    }

    private void prepareCourseForm(HttpServletRequest request, CourseFormData form, boolean creating, String error) {
        long selectedOrganizationId = selectedOrganizationId(request, form);
        List<Organization> managedOrganizations = managedOrganizations(request);
        List<OrganizationView> organizations = managedOrganizations.stream()
                .map(organization -> OrganizationView.from(organization, 0))
                .toList();
        List<OrganicUnitView> units = managedOrganizations.stream()
                .flatMap(organization -> organicUnits(request, organization.id()).stream())
                .map(unit -> OrganicUnitView.from(unit, null, 0))
                .toList();
        request.setAttribute("form", form);
        request.setAttribute("creating", creating);
        request.setAttribute("organizationOptions", organizations);
        request.setAttribute("organicUnitOptions", units);
        request.setAttribute("courseFrequencyOptions", courseFrequencyOptions(form));
        request.setAttribute("coursePeriodTemplatesForForm", coursePeriodTemplateViewsForForm(request, form));
        request.setAttribute("selectedOrganizationId", selectedOrganizationId);
        request.setAttribute("formAction", creating
                ? request.getContextPath() + courseBasePath(request)
                : request.getContextPath() + courseBasePath(request) + "/" + form.getId());
        if (creating) {
            request.setAttribute("adminCourseActiveChild", "new");
        }
        prepareCourseBasePaths(request);
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareDashboard(request, "courses", creating ? "Create Course" : "Edit Course");
    }

    private List<Organization> courseOrganizations(HttpServletRequest request) {
        if (isCoordinatorCourseRequest(request)) {
            try {
                return organizationDAO.findActive();
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to load coordinator course organizations", exception);
            }
        }
        return managedOrganizations(request);
    }

    private List<Organization> managedOrganizations(HttpServletRequest request) {
        SessionUser actor = requireCurrentUser(request);
        return organizationService.listManagedOrganizations(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                AuthorizationPolicy.MANAGE_COURSES,
                request.getRemoteAddr()
        );
    }

    private List<CourseSubjectView> filterCourseSubjectsForActor(
            HttpServletRequest request,
            SessionUser actor,
            List<CourseSubjectView> associations
    ) {
        if (!isCoordinatorCourseRequest(request)) {
            return newestFirstCourseSubjects(associations);
        }
        return newestFirstCourseSubjects(associations.stream()
                .filter(association -> canViewCourseSubject(
                        request,
                        actor,
                        association.getCourseId(),
                        association.getSubjectId()
                ))
                .toList());
    }

    private boolean canViewCourseSubject(
            HttpServletRequest request,
            SessionUser actor,
            long courseId,
            long subjectId
    ) {
        if (!isCoordinatorCourseRequest(request)) {
            return true;
        }
        return courseSubjectService.canManageAssociation(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                courseId,
                subjectId,
                request.getRemoteAddr()
        );
    }

    private Map<Long, List<CourseSubjectView>> courseSubjectsByCourse(
            List<Course> courses,
            HttpServletRequest request,
            SessionUser actor
    ) {
        Map<Long, List<CourseSubjectView>> subjectsByCourse = new HashMap<>();
        for (Course course : courses) {
            subjectsByCourse.put(
                    course.id(),
                    filterCourseSubjectsForActor(request, actor, viewFactory.courseSubjects(course.id(), null))
            );
        }
        return subjectsByCourse;
    }

    private Map<Long, Map<Long, List<ClassGroupView>>> courseSubjectClassGroupsByCourseAndSubject(
            Map<Long, List<CourseSubjectView>> subjectsByCourse
    ) {
        Map<Long, Map<Long, List<ClassGroupView>>> classGroupsByCourseAndSubject = new HashMap<>();
        for (Map.Entry<Long, List<CourseSubjectView>> courseEntry : subjectsByCourse.entrySet()) {
            Map<Long, List<ClassGroupView>> classGroupsBySubject = new HashMap<>();
            for (CourseSubjectView association : courseEntry.getValue()) {
                classGroupsBySubject.put(
                        association.getSubjectId(),
                        viewFactory.classGroupViews(classGroupsByCourseSubject(
                                courseEntry.getKey(),
                                association.getSubjectId()
                        ))
                );
            }
            classGroupsByCourseAndSubject.put(courseEntry.getKey(), classGroupsBySubject);
        }
        return classGroupsByCourseAndSubject;
    }

    private static Set<Long> subjectIdsFrom(Map<Long, List<CourseSubjectView>> subjectsByCourse) {
        Set<Long> subjectIds = new HashSet<>();
        for (List<CourseSubjectView> associations : subjectsByCourse.values()) {
            for (CourseSubjectView association : associations) {
                subjectIds.add(association.getSubjectId());
            }
        }
        return subjectIds;
    }

    private static Set<Long> subjectIdsFrom(List<CourseSubjectView> associations) {
        Set<Long> subjectIds = new HashSet<>();
        for (CourseSubjectView association : associations) {
            subjectIds.add(association.getSubjectId());
        }
        return subjectIds;
    }

    private static Set<Long> classGroupIdsFrom(
            Map<Long, Map<Long, List<ClassGroupView>>> classGroupsByCourseAndSubject
    ) {
        Set<Long> classGroupIds = new HashSet<>();
        for (Map<Long, List<ClassGroupView>> classGroupsBySubject : classGroupsByCourseAndSubject.values()) {
            for (List<ClassGroupView> classGroups : classGroupsBySubject.values()) {
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

    private Map<Long, Long> activeCourseEnrollmentCountByCourse(List<Course> courses) {
        Map<Long, Long> result = new HashMap<>();
        for (Course course : courses) {
            try {
                result.put(course.id(), enrollmentDAO.countActiveCourseEnrollments(course.id()));
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to count course enrollments", exception);
            }
        }
        return result;
    }

    private static long activeCourseEnrollmentCount(List<EnrollmentManagementView> enrollments) {
        return enrollments.stream()
                .filter(EnrollmentManagementView::isActive)
                .count();
    }

    private static BigDecimal subjectEctsTotal(List<CourseSubjectView> courseSubjects) {
        BigDecimal total = BigDecimal.ZERO;
        for (CourseSubjectView courseSubject : courseSubjects) {
            if (courseSubject.getSubjectEcts() != null) {
                total = total.add(courseSubject.getSubjectEcts());
            }
        }
        return total;
    }

    private static List<CourseSubjectView> newestFirstCourseSubjects(List<CourseSubjectView> associations) {
        return associations.stream()
                .sorted(Comparator.comparingLong(CourseSubjectView::getSubjectId).reversed())
                .toList();
    }

    private List<CourseOccurrenceView> courseOccurrenceViewsNewestFirst(long courseId) {
        return viewFactory.courseOccurrenceViews(courseId).stream()
                .sorted(Comparator.comparingLong(CourseOccurrenceView::getId).reversed())
                .toList();
    }

    private List<EnrollmentManagementView> courseEnrollmentViewsNewestFirst(long courseId) {
        return viewFactory.courseEnrollmentViews(courseId).stream()
                .sorted(Comparator.comparingLong(EnrollmentManagementView::getCourseOccurrenceId).reversed()
                        .thenComparing(Comparator.comparingLong(EnrollmentManagementView::getStudentUserId).reversed()))
                .toList();
    }

    private static String formatDecimal(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private void exposeCourseEnrollmentManagement(
            HttpServletRequest request,
            List<EnrollmentManagementView> enrollments
    ) {
        List<EnrollmentManagementView> newestFirstEnrollments = enrollments.stream()
                .sorted(Comparator.comparingLong(EnrollmentManagementView::getCourseOccurrenceId).reversed()
                        .thenComparing(Comparator.comparingLong(EnrollmentManagementView::getStudentUserId).reversed()))
                .toList();
        request.setAttribute("courseEnrollments", newestFirstEnrollments);
        request.setAttribute("activeCourseEnrollments", newestFirstEnrollments.stream()
                .filter(EnrollmentManagementView::isActive)
                .toList());
        request.setAttribute("currentCourseEnrollments", newestFirstEnrollments.stream()
                .filter(enrollment -> !enrollment.isCompleted())
                .toList());
        request.setAttribute("completedCourseEnrollments", newestFirstEnrollments.stream()
                .filter(EnrollmentManagementView::isCompleted)
                .toList());
        request.setAttribute("activeEnrollmentByStudent", activeCourseEnrollmentByStudent(newestFirstEnrollments));
        request.setAttribute("activeCourseEnrollmentCount", activeCourseEnrollmentCount(newestFirstEnrollments));
    }

    private static Map<Long, List<ClassGroupView>> classGroupsBySubject(
            List<CourseSubjectView> courseSubjects,
            List<ClassGroupView> classGroups
    ) {
        Map<Long, List<ClassGroupView>> result = new HashMap<>();
        for (CourseSubjectView association : courseSubjects) {
            long subjectId = association.getSubjectId();
            result.put(
                    subjectId,
                    classGroups.stream()
                            .filter(classGroup -> classGroup.getSubjectId() == subjectId)
                            .toList()
            );
        }
        return result;
    }

    private Map<Long, Boolean> activeCourseEnrollmentByStudent(List<EnrollmentManagementView> enrollments) {
        Map<Long, Boolean> result = new HashMap<>();
        for (EnrollmentManagementView enrollment : enrollments) {
            result.put(enrollment.getStudentUserId(), enrollment.isActive());
        }
        return result;
    }

    private boolean canCreateAnyCourse(SessionUser actor, List<Organization> organizations, HttpServletRequest request) {
        if (isCoordinatorCourseRequest(request)) {
            return false;
        }
        for (Organization organization : organizations) {
            if (courseService.canCreateCourse(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    organization.id(),
                    null,
                    request.getRemoteAddr()
            )) {
                return true;
            }
            for (OrganicUnit unit : organicUnits(request, organization.id())) {
                if (courseService.canCreateCourse(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        organization.id(),
                        unit.id(),
                        request.getRemoteAddr()
                )) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<Long, Boolean> canModifyCourseById(
            SessionUser actor,
            List<Course> courses,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Course course : courses) {
            permissions.put(course.id(), courseService.canModifyCourse(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    course.id(),
                    request.getRemoteAddr()
            ));
        }
        return permissions;
    }

    private Map<Long, Boolean> canManageCourseChildrenById(
            SessionUser actor,
            List<Course> courses,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Course course : courses) {
            permissions.put(course.id(), courseService.canManageCourseChildren(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    course.id(),
                    request.getRemoteAddr()
            ));
        }
        return permissions;
    }

    private Map<Long, Boolean> canModifySubjectById(
            SessionUser actor,
            Set<Long> subjectIds,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Long subjectId : subjectIds) {
            permissions.put(subjectId, subjectService.canModifySubject(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    subjectId,
                    request.getRemoteAddr()
            ));
        }
        return permissions;
    }

    private Map<Long, Boolean> canManageSubjectAssociationsById(
            SessionUser actor,
            Set<Long> subjectIds,
            HttpServletRequest request
    ) {
        Map<Long, Boolean> permissions = new HashMap<>();
        for (Long subjectId : subjectIds) {
            permissions.put(subjectId, canManageSubjectAssociations(actor, subjectId, request));
        }
        return permissions;
    }

    private boolean canManageSubjectAssociations(
            SessionUser actor,
            long subjectId,
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
        try {
            return courseSubjectDAO.findBySubject(subjectId).stream()
                    .anyMatch(association -> courseSubjectService.canManageAssociation(
                            actor.userId(),
                            currentSessionId(request),
                            primaryProfile(actor),
                            association.courseId(),
                            subjectId,
                            request.getRemoteAddr()
                    ));
        } catch (SQLException exception) {
            return false;
        }
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

    private List<CoursePeriodTemplateView> coursePeriodTemplateViews(long courseId, Course course) {
        try {
            if (coursePeriodTemplateDAO != null) {
                List<CoursePeriodTemplateView> configured = coursePeriodTemplateDAO.findByCourse(courseId).stream()
                        .map(CoursePeriodTemplateView::from)
                        .toList();
                if (!configured.isEmpty()) {
                    return configured;
                }
            }
            return defaultCoursePeriodTemplateViews(course.duration(), course.frequency());
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load course period templates", exception);
        }
    }

    private List<CoursePeriodTemplateView> coursePeriodTemplateViewsForForm(
            HttpServletRequest request,
            CourseFormData form
    ) {
        if (hasSubmittedCoursePeriodTemplates(request)) {
            try {
                return coursePeriodTemplateCommands(request).stream()
                        .map(CoursePeriodTemplateView::from)
                        .toList();
            } catch (RuntimeException exception) {
                return defaultCoursePeriodTemplateViews(form.getDuration(), courseFrequencyOrNull(form.getFrequency()));
            }
        }
        if (form.getId() != null && coursePeriodTemplateDAO != null) {
            try {
                List<CoursePeriodTemplateView> configured = coursePeriodTemplateDAO.findByCourse(form.getId()).stream()
                        .map(CoursePeriodTemplateView::from)
                        .toList();
                if (!configured.isEmpty()) {
                    return configured;
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to load course period templates", exception);
            }
        }
        return defaultCoursePeriodTemplateViews(form.getDuration(), courseFrequencyOrNull(form.getFrequency()));
    }

    private static List<CoursePeriodTemplateView> defaultCoursePeriodTemplateViews(
            String duration,
            CourseFrequency frequency
    ) {
        int durationYears = parseDurationOrZero(duration);
        if (durationYears <= 0 || frequency == null) {
            return List.of();
        }
        return CourseService.defaultPeriodTemplates(durationYears, frequency).stream()
                .map(CoursePeriodTemplateView::from)
                .toList();
    }

    private static List<SelectOptionView> courseFrequencyOptions(CourseFormData form) {
        CourseFrequency selected = courseFrequencyOrNull(form.getFrequency());
        List<SelectOptionView> options = new ArrayList<>();
        for (CourseFrequency frequency : CourseFrequency.values()) {
            options.add(new SelectOptionView(
                    frequency.name(),
                    frequency.label(),
                    frequency.periodsPerYear() == 1
                            ? "1 recurring period per course year"
                            : frequency.periodsPerYear() + " recurring periods per course year",
                    frequency == selected
            ));
        }
        return List.copyOf(options);
    }

    private List<pt.isel.gape.learning.model.ClassGroup> classGroupsByCourse(long courseId) {
        try {
            return classGroupDAO.findByCourse(courseId).stream()
                    .sorted(Comparator.comparingLong(pt.isel.gape.learning.model.ClassGroup::id).reversed())
                    .toList();
        } catch (java.sql.SQLException exception) {
            throw new IllegalStateException("Failed to load class groups", exception);
        }
    }

    private List<pt.isel.gape.learning.model.ClassGroup> classGroupsByCourseSubject(long courseId, long subjectId) {
        try {
            return classGroupDAO.findByCourseAndSubject(courseId, subjectId).stream()
                    .sorted(Comparator.comparingLong(pt.isel.gape.learning.model.ClassGroup::id).reversed())
                    .toList();
        } catch (java.sql.SQLException exception) {
            throw new IllegalStateException("Failed to load class groups", exception);
        }
    }

    private List<pt.isel.gape.learning.model.Subject> availableSubjects() {
        try {
            return subjectDAO.findAll();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load subject options", exception);
        }
    }

    private long selectedOrganizationId(HttpServletRequest request, CourseFormData form) {
        Long submitted = parseOptionalLong(form.getOrganizationId());
        if (submitted != null) {
            return submitted;
        }
        return 0;
    }

    private CourseCreateCommand courseCreateCommand(HttpServletRequest request) {
        return new CourseCreateCommand(
                longParameter(request, "organizationId"),
                optionalLong(request, "organicUnitId"),
                text(request, "name"),
                text(request, "acronym"),
                text(request, "photo"),
                text(request, "description"),
                requiredBigDecimal(request, "ects"),
                requiredBigDecimal(request, "certificateMaxGrade"),
                text(request, "duration"),
                requiredCourseFrequency(text(request, "frequency")),
                coursePeriodTemplateCommands(request),
                courseType(text(request, "type")),
                courseState(text(request, "state"))
        );
    }

    private CourseUpdateCommand courseUpdateCommand(HttpServletRequest request, String photo) {
        return new CourseUpdateCommand(
                longParameter(request, "organizationId"),
                optionalLong(request, "organicUnitId"),
                text(request, "name"),
                text(request, "acronym"),
                photo,
                text(request, "description"),
                requiredBigDecimal(request, "ects"),
                requiredBigDecimal(request, "certificateMaxGrade"),
                text(request, "duration"),
                requiredCourseFrequency(text(request, "frequency")),
                coursePeriodTemplateCommands(request),
                courseType(text(request, "type")),
                courseState(text(request, "state"))
        );
    }

    private CourseSubjectAssociationCommand associationCommand(HttpServletRequest request, long courseId, long subjectId) {
        return new CourseSubjectAssociationCommand(
                courseId,
                subjectId,
                optionalInteger(request, "curricularYear"),
                curricularTerm(text(request, "term")),
                request.getParameter("mandatory") != null
        );
    }

    private static void prepareCourseContext(HttpServletRequest request, CourseView course, String activeChild) {
        request.setAttribute("adminCourseContextId", course.getId());
        request.setAttribute("adminCourseContextName", course.getName());
        request.setAttribute("adminCourseActiveChild", activeChild);
    }

    private void prepareCourseBasePaths(HttpServletRequest request) {
        request.setAttribute("courseBasePath", courseBasePath(request));
        request.setAttribute("subjectBasePath", subjectBasePath(request));
    }

    private void prepareCourseDetailFragment(HttpServletRequest request, long courseId) {
        prepareCourseBasePaths(request);
        String returnTo = courseBasePath(request) + "/" + courseId;
        request.setAttribute("currentReturnTo", returnTo);
        request.setAttribute("currentReturnToParam", URLEncoder.encode(returnTo, StandardCharsets.UTF_8));
        request.setAttribute("mediaCacheVersion", Long.toString(System.currentTimeMillis()));
    }

    private static String courseBasePath(HttpServletRequest request) {
        return isCoordinatorCourseRequest(request) ? "/coordinator/courses" : "/admin/courses";
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

    private static String subjectBasePath(HttpServletRequest request) {
        return isCoordinatorCourseRequest(request) ? "/coordinator/subjects" : "/admin/subjects";
    }

    private static boolean isCoordinatorCourseRequest(HttpServletRequest request) {
        return request.getServletPath() != null && request.getServletPath().startsWith("/coordinator/courses");
    }

    private static CourseType courseType(String value) {
        return enumValue(CourseType.class, value, CourseType.DEGREE, "course type");
    }

    private static CourseFrequency requiredCourseFrequency(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Course frequency is required");
        }
        return CourseFrequency.parse(value);
    }

    private static CourseFrequency courseFrequencyOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return CourseFrequency.parse(value);
    }

    private static CourseState courseState(String value) {
        return enumValue(CourseState.class, value, CourseState.ACTIVE, "course state");
    }

    private static EnrollmentState courseEnrollmentState(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Enrollment state is required");
        }
        EnrollmentState state = EnrollmentState.parse(value);
        if (state != EnrollmentState.ACTIVE
                && state != EnrollmentState.INACTIVE
                && state != EnrollmentState.WITHDRAWN) {
            throw new IllegalArgumentException("Course enrollment state is managed automatically: " + state);
        }
        return state;
    }

    private static CurricularTerm curricularTerm(String value) {
        return enumValue(CurricularTerm.class, value, null, "curricular term");
    }

    private static boolean hasSubmittedCoursePeriodTemplates(HttpServletRequest request) {
        return text(request, COURSE_PERIOD_TEMPLATE_PAYLOAD) != null
                || request.getParameterValues("periodYear") != null;
    }

    private static List<CoursePeriodTemplateCommand> coursePeriodTemplateCommands(HttpServletRequest request) {
        String payload = text(request, COURSE_PERIOD_TEMPLATE_PAYLOAD);
        if (payload != null) {
            return coursePeriodTemplateCommands(payload);
        }
        String[] years = request.getParameterValues("periodYear");
        if (years == null || years.length == 0) {
            return List.of();
        }
        String[] terms = requiredArray(request, "periodTerm", years.length);
        String[] starts = requiredArray(request, "periodStart", years.length);
        String[] ends = requiredArray(request, "periodEnd", years.length);
        List<CoursePeriodTemplateCommand> commands = new ArrayList<>(years.length);
        for (int index = 0; index < years.length; index++) {
            int[] start = parseMonthDay(starts[index], "Course period start date");
            int[] end = parseMonthDay(ends[index], "Course period end date");
            commands.add(new CoursePeriodTemplateCommand(
                    Integer.parseInt(years[index]),
                    curricularTerm(terms[index]),
                    start[0],
                    start[1],
                    end[0],
                    end[1]
            ));
        }
        return List.copyOf(commands);
    }

    private static List<CoursePeriodTemplateCommand> coursePeriodTemplateCommands(String payload) {
        List<CoursePeriodTemplateCommand> commands = new ArrayList<>();
        for (String line : payload.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\|", -1);
            if (parts.length != 4) {
                throw new IllegalArgumentException("Course period data is incomplete");
            }
            int[] start = parseMonthDay(parts[2].trim(), "Course period start date");
            int[] end = parseMonthDay(parts[3].trim(), "Course period end date");
            commands.add(new CoursePeriodTemplateCommand(
                    parsePeriodYear(parts[0].trim()),
                    curricularTerm(parts[1].trim()),
                    start[0],
                    start[1],
                    end[0],
                    end[1]
            ));
        }
        return List.copyOf(commands);
    }

    private static int parsePeriodYear(String value) {
        try {
            int year = Integer.parseInt(value);
            if (year <= 0) {
                throw new IllegalArgumentException("Course period course year must be greater than zero");
            }
            return year;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Course period course year must be numeric", exception);
        }
    }

    private static String[] requiredArray(HttpServletRequest request, String name, int expectedLength) {
        String[] values = request.getParameterValues(name);
        if (values == null || values.length != expectedLength) {
            throw new IllegalArgumentException("Course period data is incomplete");
        }
        return values;
    }

    static int[] parseMonthDay(String value, String label) {
        if (value == null || !value.matches("\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException(label + " must use DD-MM format");
        }
        String[] parts = value.split("-");
        int day = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        try {
            MonthDay.of(month, day);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException(label + " must be a valid DD-MM date", exception);
        }
        return new int[]{month, day};
    }

    private static int parseDurationOrZero(String value) {
        if (value == null || !value.trim().matches("\\d+")) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return 0;
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

    private static Long optionalLong(HttpServletRequest request, String name) {
        return parseOptionalLong(text(request, name));
    }

    private static Long parseOptionalLong(String value) {
        return value == null || value.isBlank() ? null : Long.parseLong(value);
    }

    private static Integer optionalInteger(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : Integer.parseInt(value);
    }

    private static int requiredInteger(HttpServletRequest request, String name) {
        Integer value = optionalInteger(request, name);
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static LocalDate optionalDate(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : ApplicationDateTimeFormat.parseUserDate(value);
    }

    private static LocalDate requiredDate(HttpServletRequest request, String name) {
        LocalDate value = optionalDate(request, name);
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
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

    private static Part courseImagePart(HttpServletRequest request) throws IOException, ServletException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            return null;
        }
        return request.getPart("courseImage");
    }
}
