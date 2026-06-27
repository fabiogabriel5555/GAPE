package pt.isel.gape.web.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
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
import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.ClassGroupEnrollmentDAO;
import pt.isel.gape.learning.dao.ContentBlockDAO;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.EnrollmentApprovalPolicyDAO;
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseCreateCommand;
import pt.isel.gape.learning.model.CourseEnrollmentCommand;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseSubjectAssociationCommand;
import pt.isel.gape.learning.model.CourseSubjectState;
import pt.isel.gape.learning.model.CourseType;
import pt.isel.gape.learning.model.CourseUpdateCommand;
import pt.isel.gape.learning.model.CurricularTerm;
import pt.isel.gape.learning.model.EnrollmentApprovalMode;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.SubjectState;
import pt.isel.gape.learning.service.ClassGroupService;
import pt.isel.gape.learning.service.CourseService;
import pt.isel.gape.learning.service.CourseSubjectService;
import pt.isel.gape.learning.service.EnrollmentService;
import pt.isel.gape.learning.service.SubjectService;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.structure.service.OrganicUnitService;
import pt.isel.gape.structure.service.OrganizationService;
import pt.isel.gape.web.media.ProfilePhotoStorage;
import pt.isel.gape.web.view.ClassGroupView;
import pt.isel.gape.web.view.CourseFormData;
import pt.isel.gape.web.view.CourseSubjectFormData;
import pt.isel.gape.web.view.CourseSubjectView;
import pt.isel.gape.web.view.CourseView;
import pt.isel.gape.web.view.EnrollmentManagementView;
import pt.isel.gape.web.view.OrganicUnitView;
import pt.isel.gape.web.view.OrganizationView;
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
    private static final String COURSE_FORM_JSP = "/admin/admin/course/admin-course-form.jsp";
    private static final String COURSE_DETAIL_JSP = "/admin/admin/course/admin-course-detail.jsp";
    private static final String COURSE_SUBJECT_FORM_JSP = "/admin/admin/course/admin-course-subject-form.jsp";

    private final CourseService courseService;
    private final SubjectService subjectService;
    private final CourseSubjectService courseSubjectService;
    private final EnrollmentService enrollmentService;
    private final ClassGroupService classGroupService;
    private final OrganizationService organizationService;
    private final OrganicUnitService organicUnitService;
    private final OrganizationDAO organizationDAO;
    private final SubjectDAO subjectDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final ClassGroupDAO classGroupDAO;
    private final EnrollmentApprovalPolicyDAO enrollmentApprovalPolicyDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final LearningViewFactory viewFactory;
    private final ProfilePhotoStorage photoStorage;
    private final ClassGroupActivityViewSupport activityViewSupport;

    public CourseManagementServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private CourseManagementServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new CourseService(connectionProvider, clock),
                new SubjectService(connectionProvider, clock),
                new CourseSubjectService(connectionProvider, clock),
                new EnrollmentService(connectionProvider, clock),
                new ClassGroupService(connectionProvider, clock),
                new OrganizationService(connectionProvider, clock),
                new OrganicUnitService(connectionProvider, clock),
                new OrganizationDAO(connectionProvider),
                new SubjectDAO(connectionProvider),
                new CourseSubjectDAO(connectionProvider),
                new ClassGroupDAO(connectionProvider),
                new EnrollmentApprovalPolicyDAO(connectionProvider),
                new EnrollmentDAO(connectionProvider),
                new LearningViewFactory(
                        new OrganizationDAO(connectionProvider),
                        new OrganicUnitDAO(connectionProvider),
                        new CourseDAO(connectionProvider),
                        new SubjectDAO(connectionProvider),
                        new CourseSubjectDAO(connectionProvider),
                        new EnrollmentDAO(connectionProvider),
                        new ClassGroupDAO(connectionProvider),
                        new ClassGroupEnrollmentDAO(connectionProvider),
                        new ContentBlockDAO(connectionProvider),
                        new UserDAO(connectionProvider),
                        new TeachClassGroupDAO(connectionProvider)
                ),
                new ProfilePhotoStorage(),
                new ClassGroupActivityViewSupport(connectionProvider, clock)
        );
    }

    CourseManagementServlet(
            CourseService courseService,
            SubjectService subjectService,
            CourseSubjectService courseSubjectService,
            EnrollmentService enrollmentService,
            ClassGroupService classGroupService,
            OrganizationService organizationService,
            OrganicUnitService organicUnitService,
            OrganizationDAO organizationDAO,
            SubjectDAO subjectDAO,
            CourseSubjectDAO courseSubjectDAO,
            ClassGroupDAO classGroupDAO,
            EnrollmentApprovalPolicyDAO enrollmentApprovalPolicyDAO,
            EnrollmentDAO enrollmentDAO,
            LearningViewFactory viewFactory,
            ProfilePhotoStorage photoStorage,
            ClassGroupActivityViewSupport activityViewSupport
    ) {
        this.courseService = courseService;
        this.subjectService = subjectService;
        this.courseSubjectService = courseSubjectService;
        this.enrollmentService = enrollmentService;
        this.classGroupService = classGroupService;
        this.organizationService = organizationService;
        this.organicUnitService = organicUnitService;
        this.organizationDAO = organizationDAO;
        this.subjectDAO = subjectDAO;
        this.courseSubjectDAO = courseSubjectDAO;
        this.classGroupDAO = classGroupDAO;
        this.enrollmentApprovalPolicyDAO = enrollmentApprovalPolicyDAO;
        this.enrollmentDAO = enrollmentDAO;
        this.viewFactory = viewFactory;
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
                showCourseForm(request, response, CourseFormData.blank(firstManagedOrganizationId(request)), true, null);
                return;
            }
            if (segments.length == 1) {
                showDetail(request, response, Long.parseLong(segments[0]));
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
            if (segments.length == 3 && "subjects".equals(segments[1]) && "new".equals(segments[2])) {
                if (isCoordinatorCourseRequest(request)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                showAssociationForm(
                        request,
                        response,
                        Long.parseLong(segments[0]),
                        CourseSubjectFormData.blank(Long.parseLong(segments[0])),
                        true,
                        null
                );
                return;
            }
            if (segments.length == 4 && "subjects".equals(segments[1]) && "edit".equals(segments[3])) {
                if (isCoordinatorCourseRequest(request)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                showAssociationEditForm(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]), null);
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
            if (segments.length == 3 && "subjects".equals(segments[1])) {
                updateAssociation(request, response, Long.parseLong(segments[0]), Long.parseLong(segments[2]));
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
            if (segments.length == 4 && "subjects".equals(segments[1])) {
                long courseId = Long.parseLong(segments[0]);
                long subjectId = Long.parseLong(segments[2]);
                switch (segments[3]) {
                    case "archive" -> archiveAssociation(request, response, courseId, subjectId);
                    case "delete" -> deleteAssociation(request, response, courseId, subjectId);
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
        List<Course> rawCourses = organizations.stream()
                .flatMap(organization -> courseService.listCourses(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        organization.id(),
                        request.getRemoteAddr()
                ).stream())
                .sorted(Comparator.comparingLong(Course::id))
                .toList();
        List<CourseView> courses = rawCourses.stream()
                .map(viewFactory::courseView)
                .toList();
        Map<Long, List<CourseSubjectView>> courseSubjectsByCourse = courseSubjectsByCourse(rawCourses, request, actor);
        Map<Long, Map<Long, List<ClassGroupView>>> courseSubjectClassGroups =
                courseSubjectClassGroupsByCourseAndSubject(courseSubjectsByCourse);
        Map<Long, Long> activeEnrollmentCountByCourse = activeCourseEnrollmentCountByCourse(rawCourses);
        Set<Long> subjectIds = subjectIdsFrom(courseSubjectsByCourse);
        Set<Long> classGroupIds = classGroupIdsFrom(courseSubjectClassGroups);

        prepareCourseBasePaths(request);
        request.setAttribute("courses", courses);
        request.setAttribute("canCreateCourses", canCreateAnyCourse(actor, organizations, request));
        request.setAttribute("canModifyCourseById", canModifyCourseById(actor, rawCourses, request));
        request.setAttribute("canManageCourseChildrenById", canManageCourseChildrenById(actor, rawCourses, request));
        request.setAttribute("courseSubjectsByCourse", courseSubjectsByCourse);
        request.setAttribute("courseSubjectClassGroupsByCourseAndSubject", courseSubjectClassGroups);
        request.setAttribute("activeEnrollmentCountByCourse", activeEnrollmentCountByCourse);
        request.setAttribute("canModifySubjectById", canModifySubjectById(actor, subjectIds, request));
        request.setAttribute("canManageSubjectAssociationsById",
                canManageSubjectAssociationsById(actor, subjectIds, request));
        request.setAttribute("canModifyClassGroupById", canModifyClassGroupById(actor, classGroupIds, request));
        request.setAttribute("canManageClassGroupStructureById",
                canManageClassGroupStructureById(actor, classGroupIds, request));
        activityViewSupport.exposeClassGroupActivities(
                request,
                actor,
                currentSessionId(request),
                primaryProfile(actor),
                classGroupIds
        );
        request.setAttribute("courseCount", courses.size());
        request.setAttribute("activeCourses", courses.stream().filter(CourseView::isActive).count());
        request.setAttribute("archivedCourses", courses.stream().filter(CourseView::isArchived).count());
        request.setAttribute("subjectTotal", courses.stream().mapToInt(CourseView::getSubjectCount).sum());
        request.setAttribute(
                "activeCourseStudentTotal",
                activeEnrollmentCountByCourse.values().stream().mapToLong(Long::longValue).sum()
        );
        prepareDashboard(
                request,
                "courses",
                "Courses",
                (Boolean) request.getAttribute("canCreateCourses") ? courseBasePath(request) + "/new" : null,
                (Boolean) request.getAttribute("canCreateCourses") ? "New Course" : null
        );
        forward(request, response, COURSE_LIST_JSP);
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response, long courseId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Course course = courseService.getCourse(actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr());
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
        ) && !courseView.isArchived();
        List<CourseSubjectView> courseSubjects = filterCourseSubjectsForActor(
                request,
                actor,
                viewFactory.courseSubjects(courseId, false, null)
        );
        Set<Long> subjectIds = subjectIdsFrom(courseSubjects);
        List<pt.isel.gape.web.view.ClassGroupView> classGroups = viewFactory.classGroupViews(classGroupsByCourse(courseId))
                .stream()
                .filter(classGroup -> canViewCourseSubject(request, actor, classGroup.getCourseId(), classGroup.getSubjectId()))
                .toList();
        Map<Long, List<ClassGroupView>> classGroupsBySubject = classGroupsBySubject(courseSubjects, classGroups);
        Set<Long> classGroupIds = classGroupIdsFrom(classGroups);
        List<EnrollmentManagementView> courseEnrollments = viewFactory.courseEnrollmentViews(courseId);
        List<UserOptionView> studentOptions = viewFactory.activeStudentOptions(null);
        prepareCourseBasePaths(request);
        request.setAttribute("course", courseView);
        request.setAttribute("courseSubjects", courseSubjects);
        request.setAttribute("classGroups", classGroups);
        request.setAttribute("classGroupsBySubject", classGroupsBySubject);
        request.setAttribute("studentOptions", studentOptions);
        exposeCourseEnrollmentManagement(request, courseEnrollments);
        request.setAttribute("canModifyClassGroupById", canModifyClassGroupById(actor, classGroupIds, request));
        request.setAttribute("canManageClassGroupStructureById",
                canManageClassGroupStructureById(actor, classGroupIds, request));
        request.setAttribute("canModifySubjectById", canModifySubjectById(actor, subjectIds, request));
        request.setAttribute("canManageSubjectAssociationsById",
                canManageSubjectAssociationsById(actor, subjectIds, request));
        request.setAttribute("canModifyCourse", canModifyCourse);
        request.setAttribute("canManageCourseChildren", canManageCourseChildren);
        request.setAttribute("canManageCourseEnrollments",
                !isCoordinatorCourseRequest(request) && canManageCourseChildren);
        activityViewSupport.exposeClassGroupActivities(
                request,
                actor,
                currentSessionId(request),
                primaryProfile(actor),
                classGroupIds
        );
        prepareCourseContext(request, courseView, "detail");
        prepareDashboard(request, "courses", "Course Detail");
        forward(request, response, COURSE_DETAIL_JSP);
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
                !isCoordinatorCourseRequest(request) && canManageCourseChildren && !courseView.isArchived());
        forward(request, response, COURSE_FORM_JSP);
    }

    private void showAssociationEditForm(
            HttpServletRequest request,
            HttpServletResponse response,
            long courseId,
            long subjectId,
            String error
    ) throws ServletException, IOException {
        try {
            CourseSubjectAssociation association = courseSubjectDAO.findByCourseAndSubject(courseId, subjectId)
                    .orElseThrow(() -> new IllegalArgumentException("Course-subject association not found"));
            CourseSubjectFormData form = error == null
                    ? CourseSubjectFormData.from(association)
                    : CourseSubjectFormData.from(request, courseId);
            showAssociationForm(request, response, courseId, form, false, error);
        } catch (RuntimeException | java.sql.SQLException exception) {
            flashError(request, messageFor(exception instanceof RuntimeException runtimeException
                    ? runtimeException
                    : new IllegalStateException(exception.getMessage(), exception)));
            redirect(request, response, "/admin/courses/" + courseId);
        }
    }

    private void showAssociationForm(
            HttpServletRequest request,
            HttpServletResponse response,
            long courseId,
            CourseSubjectFormData form,
            boolean creating,
            String error
    ) throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        Course course = courseService.getCourse(actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr());
        CourseView courseView = viewFactory.courseView(course);
        if (!courseService.canManageCourseChildren(actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, request.getRemoteAddr())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        List<SubjectView> subjects = subjectsByOrganization(course.organizationId())
                .stream()
                .filter(subject -> subject.state() == SubjectState.ACTIVE)
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

        List<CourseSubjectView> courseSubjects = viewFactory.courseSubjects(courseId, false, null)
                .stream()
                .filter(association -> courseSubjectService.canManageAssociation(
                        actor.userId(),
                        currentSessionId(request),
                        primaryProfile(actor),
                        association.getCourseId(),
                        association.getSubjectId(),
                        request.getRemoteAddr()
                ))
                .toList();
        List<Long> associatedSubjectIds = courseSubjects.stream()
                .map(CourseSubjectView::getSubjectId)
                .toList();

        request.setAttribute("course", courseView);
        request.setAttribute("form", form);
        request.setAttribute("creating", creating);
        request.setAttribute("courseSubjects", courseSubjects);
        request.setAttribute("subjectEnrollmentPolicyBySubjectId",
                subjectEnrollmentPolicyBySubjectId(courseId, courseSubjects));
        request.setAttribute("subjectOptions", subjects);
        request.setAttribute("availableSubjectOptions", subjects.stream()
                .filter(subject -> !associatedSubjectIds.contains(subject.getId()))
                .toList());
        request.setAttribute("associationFormAction", creating
                ? request.getContextPath() + courseBasePath(request) + "/" + courseId + "/subjects"
                : request.getContextPath() + courseBasePath(request) + "/" + courseId + "/subjects/" + form.getSubjectId());
        request.setAttribute("canModifyCourse", courseService.canModifyCourse(
                actor.userId(),
                currentSessionId(request),
                primaryProfile(actor),
                courseId,
                request.getRemoteAddr()
        ));
        request.setAttribute("canManageCourseChildren", Boolean.TRUE);
        if (error != null) {
            request.setAttribute("errorMessage", error);
        }
        prepareCourseBasePaths(request);
        prepareCourseContext(request, courseView, creating ? "subject-new" : "subject-edit");
        prepareDashboard(request, "courses", creating ? "Associate Subject" : "Edit Association");
        forward(request, response, COURSE_SUBJECT_FORM_JSP);
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
            redirect(request, response, "/admin/courses");
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
            redirect(request, response, "/admin/courses");
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
                            optionalDate(request, "startDate"),
                            optionalDate(request, "endDate")
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
                    courseEnrollmentState(text(request, "state")),
                    optionalDate(request, "endDate"),
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
                    optionalDate(request, "endDate"),
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
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Course enrollment deleted.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, courseBasePath(request) + "/" + courseId + "#course-students");
    }

    private void associateSubject(HttpServletRequest request, HttpServletResponse response, long courseId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        CourseSubjectFormData form = CourseSubjectFormData.from(request, courseId);
        try {
            long subjectId = longParameter(request, "subjectId");
            EnrollmentApprovalMode approvalMode = requiredApprovalMode(request, "approvalMode");
            courseSubjectService.associateSubject(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    associationCommand(request, courseId, subjectId),
                    request.getRemoteAddr()
            );
            enrollmentService.updateSubjectEnrollmentPolicy(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    courseId,
                    subjectId,
                    approvalMode,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Subject associated with course.");
            redirect(request, response, "/admin/courses/" + courseId + "/subjects/new");
        } catch (RuntimeException exception) {
            showAssociationForm(request, response, courseId, form, true, messageFor(exception));
        }
    }

    private void updateAssociation(HttpServletRequest request, HttpServletResponse response, long courseId, long subjectId)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            EnrollmentApprovalMode approvalMode = requiredApprovalMode(request, "approvalMode");
            courseSubjectService.updateAssociation(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    associationCommand(request, courseId, subjectId),
                    request.getRemoteAddr()
            );
            enrollmentService.updateSubjectEnrollmentPolicy(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    courseId,
                    subjectId,
                    approvalMode,
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Course-subject association updated.");
            redirect(request, response, "/admin/courses/" + courseId + "/subjects/new");
        } catch (RuntimeException exception) {
            showAssociationEditForm(request, response, courseId, subjectId, messageFor(exception));
        }
    }

    private void archiveAssociation(HttpServletRequest request, HttpServletResponse response, long courseId, long subjectId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            courseSubjectService.archiveAssociation(actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, subjectId, request.getRemoteAddr());
            flashSuccess(request, "Course-subject association deactivated.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/admin/courses/" + courseId + "/subjects/new");
    }

    private void deleteAssociation(HttpServletRequest request, HttpServletResponse response, long courseId, long subjectId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            courseSubjectService.deleteAssociation(actor.userId(), currentSessionId(request), primaryProfile(actor), courseId, subjectId, request.getRemoteAddr());
            flashSuccess(request, "Course-subject association deleted.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/admin/courses/" + courseId + "/subjects/new");
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
        request.setAttribute("selectedOrganizationId", selectedOrganizationId);
        request.setAttribute("formAction", creating
                ? request.getContextPath() + courseBasePath(request)
                : request.getContextPath() + courseBasePath(request) + "/" + form.getId());
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
            return associations;
        }
        return associations.stream()
                .filter(association -> canViewCourseSubject(
                        request,
                        actor,
                        association.getCourseId(),
                        association.getSubjectId()
                ))
                .toList();
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
                    filterCourseSubjectsForActor(request, actor, viewFactory.courseSubjects(course.id(), false, null))
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

    private void exposeCourseEnrollmentManagement(
            HttpServletRequest request,
            List<EnrollmentManagementView> enrollments
    ) {
        request.setAttribute("courseEnrollments", enrollments);
        request.setAttribute("activeCourseEnrollments", enrollments.stream()
                .filter(EnrollmentManagementView::isActive)
                .toList());
        request.setAttribute("otherCourseEnrollments", enrollments.stream()
                .filter(enrollment -> !enrollment.isActive())
                .toList());
        request.setAttribute("activeEnrollmentByStudent", activeCourseEnrollmentByStudent(enrollments));
        request.setAttribute("activeCourseEnrollmentCount", activeCourseEnrollmentCount(enrollments));
    }

    private Map<Long, String> subjectEnrollmentPolicyBySubjectId(long courseId, List<CourseSubjectView> associations) {
        Map<Long, String> result = new HashMap<>();
        for (CourseSubjectView association : associations) {
            try {
                result.put(
                        association.getSubjectId(),
                        enrollmentApprovalPolicyDAO.subjectMode(courseId, association.getSubjectId()).toDatabaseValue()
                );
            } catch (SQLException exception) {
                result.put(association.getSubjectId(), EnrollmentApprovalMode.MANUAL.toDatabaseValue());
            }
        }
        return result;
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

    private List<pt.isel.gape.learning.model.ClassGroup> classGroupsByCourse(long courseId) {
        try {
            return classGroupDAO.findByCourse(courseId);
        } catch (java.sql.SQLException exception) {
            throw new IllegalStateException("Failed to load class groups", exception);
        }
    }

    private List<pt.isel.gape.learning.model.ClassGroup> classGroupsByCourseSubject(long courseId, long subjectId) {
        try {
            return classGroupDAO.findByCourseAndSubject(courseId, subjectId);
        } catch (java.sql.SQLException exception) {
            throw new IllegalStateException("Failed to load class groups", exception);
        }
    }

    private List<pt.isel.gape.learning.model.Subject> subjectsByOrganization(long organizationId) {
        try {
            return subjectDAO.findByOrganization(organizationId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load subject options", exception);
        }
    }

    private Long firstManagedOrganizationId(HttpServletRequest request) {
        return managedOrganizations(request).stream().map(Organization::id).findFirst().orElse(null);
    }

    private long selectedOrganizationId(HttpServletRequest request, CourseFormData form) {
        Long submitted = parseOptionalLong(form.getOrganizationId());
        if (submitted != null) {
            return submitted;
        }
        return firstManagedOrganizationId(request) == null ? 0 : firstManagedOrganizationId(request);
    }

    private CourseCreateCommand courseCreateCommand(HttpServletRequest request) {
        return new CourseCreateCommand(
                longParameter(request, "organizationId"),
                optionalLong(request, "organicUnitId"),
                text(request, "name"),
                text(request, "acronym"),
                text(request, "photo"),
                text(request, "description"),
                optionalBigDecimal(request, "ects"),
                text(request, "duration"),
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
                optionalBigDecimal(request, "ects"),
                text(request, "duration"),
                courseType(text(request, "type")),
                courseState(text(request, "state"))
        );
    }

    private CourseSubjectAssociationCommand associationCommand(HttpServletRequest request, long courseId) {
        return associationCommand(request, courseId, longParameter(request, "subjectId"));
    }

    private CourseSubjectAssociationCommand associationCommand(HttpServletRequest request, long courseId, long subjectId) {
        return new CourseSubjectAssociationCommand(
                courseId,
                subjectId,
                optionalInteger(request, "curricularYear"),
                curricularTerm(text(request, "term")),
                request.getParameter("mandatory") != null,
                courseSubjectState(text(request, "state"))
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

    private static String courseBasePath(HttpServletRequest request) {
        return isCoordinatorCourseRequest(request) ? "/coordinator/courses" : "/admin/courses";
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

    private static CourseState courseState(String value) {
        return enumValue(CourseState.class, value, CourseState.ACTIVE, "course state");
    }

    private static CourseSubjectState courseSubjectState(String value) {
        return enumValue(CourseSubjectState.class, value, CourseSubjectState.ACTIVE, "association state");
    }

    private static EnrollmentState courseEnrollmentState(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Enrollment state is required");
        }
        return EnrollmentState.parse(value);
    }

    private static EnrollmentApprovalMode requiredApprovalMode(HttpServletRequest request, String name) {
        String value = text(request, name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Enrollment approval mode is required");
        }
        try {
            return EnrollmentApprovalMode.parse(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid enrollment approval mode: " + value, exception);
        }
    }

    private static CurricularTerm curricularTerm(String value) {
        return enumValue(CurricularTerm.class, value, null, "curricular term");
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

    private static LocalDate optionalDate(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : LocalDate.parse(value);
    }

    private static BigDecimal optionalBigDecimal(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : new BigDecimal(value);
    }

    private static Part courseImagePart(HttpServletRequest request) throws IOException, ServletException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            return null;
        }
        return request.getPart("courseImage");
    }
}
