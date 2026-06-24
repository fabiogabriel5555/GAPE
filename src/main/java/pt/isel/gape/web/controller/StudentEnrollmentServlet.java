package pt.isel.gape.web.controller;

import java.io.IOException;
import java.sql.SQLException;
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
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.ClassGroupEnrollmentDAO;
import pt.isel.gape.learning.dao.ContentBlockDAO;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupEnrollment;
import pt.isel.gape.learning.model.ClassGroupEnrollmentCommand;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.CourseEnrollmentCommand;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.SubjectEnrollment;
import pt.isel.gape.learning.model.SubjectEnrollmentCommand;
import pt.isel.gape.learning.service.ClassGroupEnrollmentService;
import pt.isel.gape.learning.service.ContentAssociationService;
import pt.isel.gape.learning.service.EnrollmentService;
import pt.isel.gape.learning.service.LessonService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.web.view.ClassGroupEnrollmentView;
import pt.isel.gape.web.view.ClassGroupView;
import pt.isel.gape.web.view.BlockActivityView;
import pt.isel.gape.web.view.BlockContentItemView;
import pt.isel.gape.web.view.ContentBlockView;
import pt.isel.gape.web.view.CourseSubjectView;
import pt.isel.gape.web.view.CourseView;
import pt.isel.gape.web.view.EnrollmentView;
import pt.isel.gape.web.view.StudentClassGroupCourseGroupView;
import pt.isel.gape.web.view.StudentClassGroupCourseSubjectGroupView;
import pt.isel.gape.web.view.StudentClassGroupView;
import pt.isel.gape.web.view.StudentSubjectCourseGroupView;

@WebServlet(name = "studentEnrollmentServlet", urlPatterns = {
        "/student/enrollments",
        "/student/enrollments/*",
        "/student/courses",
        "/student/courses/*",
        "/student/subjects",
        "/student/subjects/*",
        "/student/class-groups",
        "/student/class-groups/*"
})
public final class StudentEnrollmentServlet extends DashboardServletSupport {

    private static final String STUDENT_COURSES_JSP = "/student/student/course/student-courses.jsp";
    private static final String STUDENT_COURSE_DETAIL_JSP = "/student/student/course/student-course-detail.jsp";
    private static final String STUDENT_SUBJECTS_JSP = "/student/student/subject/student-subjects.jsp";
    private static final String STUDENT_SUBJECT_DETAIL_JSP = "/student/student/subject/student-subject-detail.jsp";
    private static final String STUDENT_CLASS_GROUPS_JSP = "/student/student/class-group/student-class-groups.jsp";
    private static final String STUDENT_CLASS_GROUP_DETAIL_JSP = "/student/student/class-group/student-class-group-detail.jsp";

    private final EnrollmentService enrollmentService;
    private final ClassGroupEnrollmentService classGroupEnrollmentService;
    private final ContentAssociationService contentAssociationService;
    private final LessonService lessonService;
    private final CourseDAO courseDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final ClassGroupDAO classGroupDAO;
    private final ClassGroupEnrollmentDAO classGroupEnrollmentDAO;
    private final ContentBlockDAO contentBlockDAO;
    private final LearningViewFactory viewFactory;
    private final Clock clock;

    public StudentEnrollmentServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private StudentEnrollmentServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new EnrollmentService(connectionProvider, clock),
                new ClassGroupEnrollmentService(connectionProvider, clock),
                new ContentAssociationService(connectionProvider, clock),
                new LessonService(connectionProvider, clock),
                new CourseDAO(connectionProvider),
                new CourseSubjectDAO(connectionProvider),
                new EnrollmentDAO(connectionProvider),
                new ClassGroupDAO(connectionProvider),
                new ClassGroupEnrollmentDAO(connectionProvider),
                new ContentBlockDAO(connectionProvider),
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
                clock
        );
    }

    StudentEnrollmentServlet(
            EnrollmentService enrollmentService,
            ClassGroupEnrollmentService classGroupEnrollmentService,
            ContentAssociationService contentAssociationService,
            LessonService lessonService,
            CourseDAO courseDAO,
            CourseSubjectDAO courseSubjectDAO,
            EnrollmentDAO enrollmentDAO,
            ClassGroupDAO classGroupDAO,
            ClassGroupEnrollmentDAO classGroupEnrollmentDAO,
            ContentBlockDAO contentBlockDAO,
            LearningViewFactory viewFactory,
            Clock clock
    ) {
        this.enrollmentService = enrollmentService;
        this.classGroupEnrollmentService = classGroupEnrollmentService;
        this.contentAssociationService = contentAssociationService;
        this.lessonService = lessonService;
        this.courseDAO = courseDAO;
        this.courseSubjectDAO = courseSubjectDAO;
        this.enrollmentDAO = enrollmentDAO;
        this.classGroupDAO = classGroupDAO;
        this.classGroupEnrollmentDAO = classGroupEnrollmentDAO;
        this.contentBlockDAO = contentBlockDAO;
        this.viewFactory = viewFactory;
        this.clock = clock;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("/student/enrollments".equals(request.getServletPath()) && request.getPathInfo() != null) {
            String[] segments = pathSegments(request.getPathInfo());
            if (segments.length == 1 && "updates".equals(segments[0])) {
                synchronizeEnrollmentTargets(request, response);
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if ("/student/enrollments".equals(request.getServletPath())) {
            response.sendRedirect(request.getContextPath() + "/student/courses");
            return;
        }
        if ("/student/courses".equals(request.getServletPath()) && request.getPathInfo() != null) {
            showCourseDetail(request, response);
            return;
        }
        if ("/student/subjects".equals(request.getServletPath()) && request.getPathInfo() != null) {
            showSubjectDetail(request, response);
            return;
        }
        if ("/student/class-groups".equals(request.getServletPath()) && request.getPathInfo() != null) {
            showClassGroupDetail(request, response);
            return;
        }
        showEnrollments(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        try {
            if (segments.length == 2 && "courses".equals(segments[0])) {
                enrollCourse(request, response, Long.parseLong(segments[1]));
                return;
            }
            if (segments.length == 3 && "courses".equals(segments[0]) && "withdraw".equals(segments[2])) {
                withdrawCourse(request, response, Long.parseLong(segments[1]));
                return;
            }
            if (segments.length == 4 && "courses".equals(segments[0]) && "subjects".equals(segments[2])) {
                enrollSubject(request, response, Long.parseLong(segments[1]), Long.parseLong(segments[3]));
                return;
            }
            if (segments.length == 5
                    && "courses".equals(segments[0])
                    && "subjects".equals(segments[2])
                    && "withdraw".equals(segments[4])) {
                withdrawSubject(request, response, Long.parseLong(segments[1]), Long.parseLong(segments[3]));
                return;
            }
            if (segments.length == 2 && "class-groups".equals(segments[0])) {
                enrollClassGroup(request, response, Long.parseLong(segments[1]));
                return;
            }
            if (segments.length == 3 && "class-groups".equals(segments[0]) && "withdraw".equals(segments[2])) {
                withdrawClassGroup(request, response, Long.parseLong(segments[1]));
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void showEnrollments(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            SessionUser actor = requireCurrentUser(request);
            long studentUserId = actor.userId();

            List<EnrollmentView> courseEnrollments = new ArrayList<>();
            for (CourseEnrollment enrollment : enrollmentDAO.findCourseEnrollmentsByStudent(studentUserId)) {
                courseDAO.findById(enrollment.courseId())
                        .map(course -> viewFactory.courseView(course, enrollment))
                        .map(EnrollmentView::course)
                        .ifPresent(courseEnrollments::add);
            }

            List<EnrollmentView> subjectEnrollments = new ArrayList<>();
            List<EnrollmentView> availableSubjects = new ArrayList<>();
            for (SubjectEnrollment enrollment : enrollmentDAO.findSubjectEnrollmentsByStudent(studentUserId)) {
                Course course = courseDAO.findById(enrollment.courseId()).orElse(null);
                CourseSubjectAssociation association = courseSubjectDAO
                        .findByCourseAndSubject(enrollment.courseId(), enrollment.subjectId())
                        .orElse(null);
                if (course == null || association == null) {
                    continue;
                }
                CourseView courseView = viewFactory.courseView(
                        course,
                        enrollmentDAO.findCourseEnrollment(studentUserId, course.id()).orElse(null)
                );
                CourseSubjectView subjectView = viewFactory.courseSubjectView(association, studentUserId);
                subjectEnrollments.add(EnrollmentView.subject(courseView, subjectView));
            }
            for (CourseEnrollment courseEnrollment : enrollmentDAO.findCourseEnrollmentsByStudent(studentUserId)) {
                if (courseEnrollment.state() != EnrollmentState.ACTIVE) {
                    continue;
                }
                Course course = courseDAO.findById(courseEnrollment.courseId()).orElse(null);
                if (course == null) {
                    continue;
                }
                CourseView courseView = viewFactory.courseView(course, courseEnrollment);
                for (CourseSubjectAssociation association : courseSubjectDAO.findActiveByCourse(course.id())) {
                    CourseSubjectView subjectView = viewFactory.courseSubjectView(association, studentUserId);
                    if (subjectView.isActiveEnrollment() || subjectView.isPendingEnrollment()) {
                        continue;
                    }
                    availableSubjects.add(EnrollmentView.subject(courseView, subjectView));
                }
            }

            List<StudentClassGroupView> studentClassGroups = studentClassGroupViews(actor, request);

            List<CourseView> availableCourses = new ArrayList<>();
            for (Course course : courseDAO.findCatalogCourses(null, null, null)) {
                availableCourses.add(viewFactory.courseView(
                        course,
                        enrollmentDAO.findCourseEnrollment(studentUserId, course.id()).orElse(null)
                ));
            }

            request.setAttribute("courseEnrollments", courseEnrollments);
            request.setAttribute("subjectEnrollments", subjectEnrollments);
            request.setAttribute("availableSubjects", availableSubjects);
            request.setAttribute("subjectCourseGroups", subjectCourseGroups(
                    courseEnrollments,
                    subjectEnrollments,
                    availableSubjects
            ));
            List<StudentClassGroupCourseSubjectGroupView> studentClassGroupGroups = studentClassGroupGroups(studentClassGroups);
            request.setAttribute("studentClassGroups", studentClassGroups);
            request.setAttribute("studentClassGroupGroups", studentClassGroupGroups);
            request.setAttribute("studentClassGroupCourseGroups", studentClassGroupCourseGroups(studentClassGroupGroups));
            request.setAttribute("availableCourses", availableCourses);
            String servletPath = request.getServletPath();
            if ("/student/subjects".equals(servletPath)) {
                prepareDashboard(request, "subjects", "Subjects");
                forward(request, response, STUDENT_SUBJECTS_JSP);
                return;
            }
            if ("/student/class-groups".equals(servletPath)) {
                prepareDashboard(request, "class-groups", "Class Groups");
                forward(request, response, STUDENT_CLASS_GROUPS_JSP);
                return;
            }
            prepareDashboard(request, "courses", "Courses");
            forward(request, response, STUDENT_COURSES_JSP);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load student enrollments", exception);
        }
    }

    private static List<StudentSubjectCourseGroupView> subjectCourseGroups(
            List<EnrollmentView> courseEnrollments,
            List<EnrollmentView> subjectEnrollments,
            List<EnrollmentView> availableSubjects
    ) {
        Map<Long, CourseView> coursesById = new LinkedHashMap<>();
        Map<Long, List<EnrollmentView>> subjectsByCourse = new LinkedHashMap<>();
        for (EnrollmentView courseEnrollment : courseEnrollments) {
            CourseView course = courseEnrollment.getCourse();
            coursesById.putIfAbsent(course.getId(), course);
            subjectsByCourse.putIfAbsent(course.getId(), new ArrayList<>());
        }

        Set<String> seenSubjects = new HashSet<>();
        for (EnrollmentView enrollment : subjectEnrollments) {
            addSubjectGroupItem(coursesById, subjectsByCourse, seenSubjects, enrollment);
        }
        for (EnrollmentView enrollment : availableSubjects) {
            addSubjectGroupItem(coursesById, subjectsByCourse, seenSubjects, enrollment);
        }

        List<StudentSubjectCourseGroupView> groups = new ArrayList<>();
        for (Map.Entry<Long, List<EnrollmentView>> entry : subjectsByCourse.entrySet()) {
            CourseView course = coursesById.get(entry.getKey());
            if (course != null && !entry.getValue().isEmpty()) {
                groups.add(new StudentSubjectCourseGroupView(course, entry.getValue()));
            }
        }
        return groups;
    }

    private static void addSubjectGroupItem(
            Map<Long, CourseView> coursesById,
            Map<Long, List<EnrollmentView>> subjectsByCourse,
            Set<String> seenSubjects,
            EnrollmentView enrollment
    ) {
        CourseView course = enrollment.getCourse();
        CourseSubjectView subject = enrollment.getSubject();
        if (course == null || subject == null) {
            return;
        }
        String key = subjectContextKey(course.getId(), subject.getSubjectId());
        if (!seenSubjects.add(key)) {
            return;
        }
        coursesById.putIfAbsent(course.getId(), course);
        subjectsByCourse.computeIfAbsent(course.getId(), ignored -> new ArrayList<>()).add(enrollment);
    }

    private void showSubjectDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        if (segments.length != 2) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            SessionUser actor = requireCurrentUser(request);
            long studentUserId = actor.userId();
            long courseId = Long.parseLong(segments[0]);
            long subjectId = Long.parseLong(segments[1]);
            Course course = courseDAO.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found"));
            CourseSubjectAssociation association = courseSubjectDAO
                    .findByCourseAndSubject(courseId, subjectId)
                    .orElseThrow(() -> new IllegalArgumentException("Subject is not associated with the course"));
            CourseView courseView = viewFactory.courseView(
                    course,
                    enrollmentDAO.findCourseEnrollment(studentUserId, course.id()).orElse(null)
            );
            CourseSubjectView subjectView = viewFactory.courseSubjectView(association, studentUserId);
            EnrollmentView enrollment = EnrollmentView.subject(courseView, subjectView);
            List<StudentClassGroupView> subjectClassGroups = studentCourseClassGroups(
                    actor,
                    request,
                    List.of(subjectView)
            ).getOrDefault(subjectId, List.of());

            request.setAttribute("course", courseView);
            request.setAttribute("subjectEnrollment", enrollment);
            request.setAttribute("subject", subjectView);
            request.setAttribute("subjectClassGroups", subjectClassGroups);
            request.setAttribute("studentSubjectContextCourseId", courseView.getId());
            request.setAttribute("studentSubjectContextId", subjectView.getSubjectId());
            request.setAttribute("studentSubjectContextName", subjectView.getSubjectName());
            request.setAttribute("studentSubjectActiveChild", "detail");
            prepareDashboard(request, "subjects", subjectView.getSubjectName());
            forward(request, response, STUDENT_SUBJECT_DETAIL_JSP);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (RuntimeException | SQLException exception) {
            throw new ServletException("Failed to load student subject detail", exception);
        }
    }

    private void showClassGroupDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        if (segments.length != 1) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        long classGroupId;
        try {
            classGroupId = Long.parseLong(segments[0]);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            SessionUser actor = requireCurrentUser(request);
            long studentUserId = actor.userId();
            ClassGroup classGroup = classGroupDAO.findById(classGroupId).orElse(null);
            if (classGroup == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Course course = courseDAO.findById(classGroup.courseId()).orElse(null);
            CourseSubjectAssociation association = courseSubjectDAO
                    .findByCourseAndSubject(classGroup.courseId(), classGroup.subjectId())
                    .orElse(null);
            if (course == null || association == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            CourseView courseView = viewFactory.courseView(
                    course,
                    enrollmentDAO.findCourseEnrollment(studentUserId, course.id()).orElse(null)
            );
            CourseSubjectView subjectView = viewFactory.courseSubjectView(association, studentUserId);
            ClassGroupEnrollment enrollment = classGroupEnrollmentDAO
                    .findEnrollment(studentUserId, classGroup.id())
                    .orElse(null);
            Set<String> activeSubjectContexts = new HashSet<>();
            for (SubjectEnrollment subjectEnrollment : enrollmentDAO.findActiveSubjectEnrollmentsByStudent(studentUserId)) {
                activeSubjectContexts.add(subjectContextKey(subjectEnrollment.courseId(), subjectEnrollment.subjectId()));
            }
            StudentClassGroupView classGroupItem = studentClassGroupView(
                    actor,
                    request,
                    classGroup,
                    enrollment,
                    canEnrollInClassGroupContext(classGroup, activeSubjectContexts)
            );
            List<pt.isel.gape.web.view.LessonView> lessons = classGroupItem.isActiveEnrollment()
                    ? lessonService.listLessonsByClassGroup(
                                    studentUserId,
                                    currentSessionId(request),
                                    AccessProfileType.STUDENT,
                                    classGroup.id(),
                                    request.getRemoteAddr()
                            )
                            .stream()
                            .map(viewFactory::lessonView)
                            .toList()
                    : List.of();

            request.setAttribute("course", courseView);
            request.setAttribute("subject", subjectView);
            request.setAttribute("classGroupItem", classGroupItem);
            request.setAttribute("classGroup", classGroupItem.getClassGroup());
            request.setAttribute("lessons", lessons);
            request.setAttribute("contentBlocks", classGroupItem.getContentBlocks());
            request.setAttribute("blockContentsByBlock", classGroupItem.getBlockContentsByBlock());
            request.setAttribute("blockLessonsByBlock", classGroupItem.getBlockLessonsByBlock());
            request.setAttribute("blockActivitiesByBlock", classGroupItem.getBlockActivitiesByBlock());
            request.setAttribute("returnTo", "/student/class-groups/" + classGroupId);
            request.setAttribute("studentClassGroupContextId", classGroupItem.getClassGroup().getId());
            request.setAttribute("studentClassGroupContextName", classGroupItem.getClassGroup().getCode());
            request.setAttribute("studentClassGroupActiveChild", "detail");
            request.setAttribute("studentPageTitle", classGroupItem.getClassGroup().getCode());
            request.setAttribute(
                    "studentPageDescription",
                    "Class group details, lessons and visible materials for your student profile."
            );
            prepareDashboard(request, "class-groups", classGroupItem.getClassGroup().getCode());
            forward(request, response, STUDENT_CLASS_GROUP_DETAIL_JSP);
        } catch (RuntimeException | SQLException exception) {
            throw new ServletException("Failed to load student class group detail", exception);
        }
    }

    private void showCourseDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        if (segments.length != 1) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        long courseId;
        try {
            courseId = Long.parseLong(segments[0]);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            SessionUser actor = requireCurrentUser(request);
            long studentUserId = actor.userId();
            Course course = courseDAO.findById(courseId).orElse(null);
            if (course == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            CourseEnrollment courseEnrollment = enrollmentDAO.findCourseEnrollment(studentUserId, courseId).orElse(null);
            if (courseEnrollment == null) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            CourseView courseView = viewFactory.courseView(course, courseEnrollment);
            List<CourseSubjectView> courseSubjects = viewFactory.courseSubjects(courseId, true, studentUserId);
            Map<Long, List<StudentClassGroupView>> classGroupsBySubject = studentCourseClassGroups(
                    actor,
                    request,
                    courseSubjects
            );
            long activeSubjectCount = courseSubjects.stream()
                    .filter(CourseSubjectView::isActiveEnrollment)
                    .count();
            long classGroupCount = classGroupsBySubject.values().stream()
                    .mapToLong(List::size)
                    .sum();
            long activeClassGroupCount = classGroupsBySubject.values().stream()
                    .flatMap(List::stream)
                    .filter(StudentClassGroupView::isActiveEnrollment)
                    .count();

            request.setAttribute("course", courseView);
            request.setAttribute("courseSubjects", courseSubjects);
            request.setAttribute("classGroupsBySubject", classGroupsBySubject);
            request.setAttribute("activeSubjectCount", activeSubjectCount);
            request.setAttribute("classGroupCount", classGroupCount);
            request.setAttribute("activeClassGroupCount", activeClassGroupCount);
            request.setAttribute("returnTo", "/student/courses/" + courseId);
            request.setAttribute("studentCourseContextId", courseView.getId());
            request.setAttribute("studentCourseContextName", courseView.getName());
            request.setAttribute("studentCourseActiveChild", "detail");
            request.setAttribute("studentPageTitle", courseView.getName());
            request.setAttribute(
                    "studentPageDescription",
                    "Course details, subject structure and class groups available in your student profile."
            );
            prepareDashboard(request, "courses", courseView.getName());
            forward(request, response, STUDENT_COURSE_DETAIL_JSP);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load student course detail", exception);
        }
    }

    private void enrollCourse(HttpServletRequest request, HttpServletResponse response, long courseId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            String message = "Course enrollment is managed by authorized profiles.";
            if (wantsStudentEnrollmentJson(request)) {
                writeStudentEnrollmentResponse(request, response, false, message, List.of());
                return;
            }
            flashError(request, message);
        } catch (RuntimeException exception) {
            if (wantsStudentEnrollmentJson(request)) {
                writeStudentEnrollmentResponse(request, response, false, messageFor(exception), List.of());
                return;
            }
            flashError(request, messageForEnrollmentException(exception));
        }
        redirectToReturnPath(request, response, "/student/enrollments");
    }

    private void withdrawCourse(HttpServletRequest request, HttpServletResponse response, long courseId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.withdrawStudentFromCourse(
                    actor.userId(),
                    currentSessionId(request),
                    AccessProfileType.STUDENT,
                    actor.userId(),
                    courseId,
                    optionalDate(request, "endDate"),
                    request.getRemoteAddr()
            );
            String message = "You left this course.";
            if (wantsStudentEnrollmentJson(request)) {
                writeStudentEnrollmentResponse(
                        request,
                        response,
                        true,
                        message,
                        courseEnrollmentUpdates(actor, request, courseId)
                );
                return;
            }
            flashSuccess(request, message);
        } catch (RuntimeException | SQLException exception) {
            if (wantsStudentEnrollmentJson(request)) {
                writeStudentEnrollmentResponse(request, response, false, messageForEnrollmentException(exception), List.of());
                return;
            }
            flashError(request, messageForEnrollmentException(exception));
        }
        redirectToReturnPath(request, response, "/student/enrollments");
    }

    private void enrollSubject(HttpServletRequest request, HttpServletResponse response, long courseId, long subjectId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.requestStudentInSubject(
                    actor.userId(),
                    currentSessionId(request),
                    new SubjectEnrollmentCommand(actor.userId(), subjectId, courseId, optionalDate(request, "startDate"), optionalDate(request, "endDate")),
                    request.getRemoteAddr()
            );
            String message = "Subject enrollment request submitted.";
            if (wantsStudentEnrollmentJson(request)) {
                writeStudentEnrollmentResponse(
                        request,
                        response,
                        true,
                        message,
                        subjectEnrollmentUpdates(actor, request, courseId, subjectId)
                );
                return;
            }
            flashSuccess(request, message);
        } catch (RuntimeException | SQLException exception) {
            if (wantsStudentEnrollmentJson(request)) {
                writeStudentEnrollmentResponse(request, response, false, messageForEnrollmentException(exception), List.of());
                return;
            }
            flashError(request, messageForEnrollmentException(exception));
        }
        redirectToReturnPath(request, response, "/student/enrollments");
    }

    private void withdrawSubject(HttpServletRequest request, HttpServletResponse response, long courseId, long subjectId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.withdrawStudentFromSubject(
                    actor.userId(),
                    currentSessionId(request),
                    AccessProfileType.STUDENT,
                    actor.userId(),
                    courseId,
                    subjectId,
                    optionalDate(request, "endDate"),
                    request.getRemoteAddr()
            );
            String message = "You left this subject.";
            if (wantsStudentEnrollmentJson(request)) {
                writeStudentEnrollmentResponse(
                        request,
                        response,
                        true,
                        message,
                        subjectEnrollmentUpdates(actor, request, courseId, subjectId)
                );
                return;
            }
            flashSuccess(request, message);
        } catch (RuntimeException | SQLException exception) {
            if (wantsStudentEnrollmentJson(request)) {
                writeStudentEnrollmentResponse(request, response, false, messageForEnrollmentException(exception), List.of());
                return;
            }
            flashError(request, messageForEnrollmentException(exception));
        }
        redirectToReturnPath(request, response, "/student/enrollments");
    }

    private void enrollClassGroup(HttpServletRequest request, HttpServletResponse response, long classGroupId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            classGroupEnrollmentService.requestStudentInClassGroup(
                    actor.userId(),
                    currentSessionId(request),
                    new ClassGroupEnrollmentCommand(
                            actor.userId(),
                            classGroupId,
                            optionalDate(request, "startDate"),
                            optionalDate(request, "endDate")
                    ),
                    request.getRemoteAddr()
            );
            String message = "Class group enrollment request submitted.";
            if (wantsStudentEnrollmentJson(request)) {
                writeStudentEnrollmentResponse(
                        request,
                        response,
                        true,
                        message,
                        classGroupEnrollmentUpdates(actor, request, classGroupId)
                );
                return;
            }
            flashSuccess(request, message);
        } catch (RuntimeException | SQLException exception) {
            if (wantsStudentEnrollmentJson(request)) {
                writeStudentEnrollmentResponse(request, response, false, messageForEnrollmentException(exception), List.of());
                return;
            }
            flashError(request, messageForEnrollmentException(exception));
        }
        redirectToReturnPath(request, response, "/student/enrollments");
    }

    private void withdrawClassGroup(HttpServletRequest request, HttpServletResponse response, long classGroupId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            classGroupEnrollmentService.withdrawStudentFromClassGroup(
                    actor.userId(),
                    currentSessionId(request),
                    AccessProfileType.STUDENT,
                    actor.userId(),
                    classGroupId,
                    optionalDate(request, "endDate"),
                    request.getRemoteAddr()
            );
            String message = "You left this class group.";
            if (wantsStudentEnrollmentJson(request)) {
                writeStudentEnrollmentResponse(
                        request,
                        response,
                        true,
                        message,
                        classGroupEnrollmentUpdates(actor, request, classGroupId)
                );
                return;
            }
            flashSuccess(request, message);
        } catch (RuntimeException | SQLException exception) {
            if (wantsStudentEnrollmentJson(request)) {
                writeStudentEnrollmentResponse(request, response, false, messageForEnrollmentException(exception), List.of());
                return;
            }
            flashError(request, messageForEnrollmentException(exception));
        }
        redirectToReturnPath(request, response, "/student/enrollments");
    }

    private List<StudentEnrollmentUpdate> courseEnrollmentUpdates(
            SessionUser actor,
            HttpServletRequest request,
            long courseId
    ) throws SQLException {
        List<StudentEnrollmentUpdate> updates = new ArrayList<>();
        for (CourseSubjectAssociation association : courseSubjectDAO.findActiveByCourse(courseId)) {
            updates.addAll(subjectEnrollmentUpdates(actor, request, courseId, association.subjectId()));
        }
        return updates;
    }

    private List<StudentEnrollmentUpdate> subjectEnrollmentUpdates(
            SessionUser actor,
            HttpServletRequest request,
            long courseId,
            long subjectId
    ) throws SQLException {
        long studentUserId = actor.userId();
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        CourseSubjectAssociation association = courseSubjectDAO
                .findByCourseAndSubject(courseId, subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject is not associated with the course"));
        CourseView courseView = viewFactory.courseView(
                course,
                enrollmentDAO.findCourseEnrollment(studentUserId, courseId).orElse(null)
        );
        CourseSubjectView subjectView = viewFactory.courseSubjectView(association, studentUserId);
        List<StudentEnrollmentUpdate> updates = new ArrayList<>();
        updates.add(subjectUpdate(request, courseView, subjectView));
        studentCourseClassGroups(actor, request, List.of(subjectView))
                .getOrDefault(subjectId, List.of())
                .stream()
                .map(item -> classGroupUpdate(request, item))
                .forEach(updates::add);
        return updates;
    }

    private List<StudentEnrollmentUpdate> classGroupEnrollmentUpdates(
            SessionUser actor,
            HttpServletRequest request,
            long classGroupId
    ) throws SQLException {
        ClassGroup classGroup = classGroupDAO.findById(classGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Class group not found"));
        CourseSubjectAssociation association = courseSubjectDAO
                .findByCourseAndSubject(classGroup.courseId(), classGroup.subjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject is not associated with the course"));
        CourseSubjectView subjectView = viewFactory.courseSubjectView(association, actor.userId());
        return studentCourseClassGroups(actor, request, List.of(subjectView))
                .getOrDefault(classGroup.subjectId(), List.of())
                .stream()
                .map(item -> classGroupUpdate(request, item))
                .toList();
    }

    private void synchronizeEnrollmentTargets(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            SessionUser actor = requireCurrentUser(request);
            List<StudentEnrollmentUpdate> updates = new ArrayList<>();
            Set<String> emittedTargets = new HashSet<>();
            for (String target : requestedEnrollmentTargets(request)) {
                addEnrollmentTargetUpdates(actor, request, target, updates, emittedTargets);
            }
            writeStudentEnrollmentResponse(request, response, true, "", updates);
        } catch (RuntimeException | SQLException exception) {
            writeStudentEnrollmentResponse(request, response, false, messageForEnrollmentException(exception), List.of());
        }
    }

    private void addEnrollmentTargetUpdates(
            SessionUser actor,
            HttpServletRequest request,
            String target,
            List<StudentEnrollmentUpdate> updates,
            Set<String> emittedTargets
    ) throws SQLException {
        if (target == null || target.isBlank()) {
            return;
        }
        if (target.startsWith("subject-")) {
            String[] parts = target.substring("subject-".length()).split("-", -1);
            if (parts.length != 2) {
                return;
            }
            long courseId;
            long subjectId;
            try {
                courseId = Long.parseLong(parts[0]);
                subjectId = Long.parseLong(parts[1]);
            } catch (NumberFormatException exception) {
                return;
            }
            addUniqueEnrollmentUpdates(
                    updates,
                    emittedTargets,
                    subjectEnrollmentUpdates(actor, request, courseId, subjectId)
            );
            return;
        }
        if (target.startsWith("class-group-")) {
            long classGroupId;
            try {
                classGroupId = Long.parseLong(target.substring("class-group-".length()));
            } catch (NumberFormatException exception) {
                return;
            }
            addUniqueEnrollmentUpdates(
                    updates,
                    emittedTargets,
                    classGroupEnrollmentUpdates(actor, request, classGroupId)
            );
        }
    }

    private static List<String> requestedEnrollmentTargets(HttpServletRequest request) {
        List<String> targets = new ArrayList<>();
        String[] values = request.getParameterValues("target");
        if (values == null) {
            return targets;
        }
        for (String value : values) {
            if (value == null) {
                continue;
            }
            for (String target : value.split(",")) {
                String normalized = target.trim();
                if (!normalized.isEmpty()) {
                    targets.add(normalized);
                }
            }
        }
        return targets;
    }

    private static void addUniqueEnrollmentUpdates(
            List<StudentEnrollmentUpdate> updates,
            Set<String> emittedTargets,
            List<StudentEnrollmentUpdate> candidates
    ) {
        for (StudentEnrollmentUpdate candidate : candidates) {
            if (emittedTargets.add(candidate.target())) {
                updates.add(candidate);
            }
        }
    }

    private static List<StudentClassGroupCourseSubjectGroupView> studentClassGroupGroups(
            List<StudentClassGroupView> classGroups
    ) {
        Map<String, ClassGroupView> contexts = new LinkedHashMap<>();
        Map<String, List<StudentClassGroupView>> grouped = new LinkedHashMap<>();
        for (StudentClassGroupView item : classGroups) {
            ClassGroupView classGroup = item.getClassGroup();
            String key = subjectContextKey(classGroup.getCourseId(), classGroup.getSubjectId());
            contexts.putIfAbsent(key, classGroup);
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
        }

        List<StudentClassGroupCourseSubjectGroupView> groups = new ArrayList<>();
        for (Map.Entry<String, List<StudentClassGroupView>> entry : grouped.entrySet()) {
            groups.add(new StudentClassGroupCourseSubjectGroupView(
                    contexts.get(entry.getKey()),
                    entry.getValue()
            ));
        }
        return groups;
    }

    private static List<StudentClassGroupCourseGroupView> studentClassGroupCourseGroups(
            List<StudentClassGroupCourseSubjectGroupView> subjectGroups
    ) {
        Map<Long, String> courseNames = new LinkedHashMap<>();
        Map<Long, List<StudentClassGroupCourseSubjectGroupView>> grouped = new LinkedHashMap<>();
        for (StudentClassGroupCourseSubjectGroupView group : subjectGroups) {
            courseNames.putIfAbsent(group.getCourseId(), group.getCourseName());
            grouped.computeIfAbsent(group.getCourseId(), ignored -> new ArrayList<>()).add(group);
        }

        List<StudentClassGroupCourseGroupView> courseGroups = new ArrayList<>();
        for (Map.Entry<Long, List<StudentClassGroupCourseSubjectGroupView>> entry : grouped.entrySet()) {
            courseGroups.add(new StudentClassGroupCourseGroupView(
                    entry.getKey(),
                    courseNames.getOrDefault(entry.getKey(), "Course " + entry.getKey()),
                    entry.getValue()
            ));
        }
        return courseGroups;
    }

    private StudentEnrollmentUpdate subjectUpdate(
            HttpServletRequest request,
            CourseView course,
            CourseSubjectView subject
    ) {
        Map<String, String> actions = new LinkedHashMap<>();
        actions.put("subject-open", subjectOpenActionsHtml(request, course, subject, "px-16 py-9 text-14"));
        actions.put("subject-open-compact", subjectOpenActionsHtml(request, course, subject, "px-12 py-7 text-12"));
        actions.put("subject-leave", subjectLeaveActionsHtml(request, course, subject));
        return new StudentEnrollmentUpdate(
                "subject-" + course.getId() + "-" + subject.getSubjectId(),
                subject.getEnrollmentStateLabel(),
                subject.getEnrollmentBadgeClass(),
                actions
        );
    }

    private StudentEnrollmentUpdate classGroupUpdate(HttpServletRequest request, StudentClassGroupView item) {
        Map<String, String> actions = new LinkedHashMap<>();
        actions.put("class-group-detail", classGroupDetailActionsHtml(request, item));
        actions.put("class-group-detail-page", classGroupDetailPageActionsHtml(request, item));
        actions.put("class-group-list", classGroupListActionsHtml(request, item));
        return new StudentEnrollmentUpdate(
                "class-group-" + item.getClassGroup().getId(),
                item.getEnrollmentStateLabel(),
                item.getEnrollmentBadgeClass(),
                actions
        );
    }

    private String subjectOpenActionsHtml(
            HttpServletRequest request,
            CourseView course,
            CourseSubjectView subject,
            String sizeClass
    ) {
        if (subject.isActiveEnrollment()) {
            return linkHtml(
                    request.getContextPath() + "/student/subjects/" + course.getId() + "/" + subject.getSubjectId(),
                    "gape-student-card-icon-button",
                    "Open subject",
                    "ph ph-eye"
            );
        }
        if (course.isActiveEnrollment() && !subject.isPendingEnrollment()) {
            return subjectRequestFormHtml(request, course.getId(), subject.getSubjectId(), sizeClass);
        }
        return "";
    }

    private String subjectLeaveActionsHtml(
            HttpServletRequest request,
            CourseView course,
            CourseSubjectView subject
    ) {
        if (subject.isActiveEnrollment()) {
            return subjectLeaveFormHtml(request, course.getId(), subject.getSubjectId(), "px-16 py-9 text-14");
        }
        if (course.isActiveEnrollment() && !subject.isPendingEnrollment()) {
            return subjectRequestFormHtml(request, course.getId(), subject.getSubjectId(), "px-16 py-9 text-14");
        }
        return "";
    }

    private String classGroupDetailActionsHtml(HttpServletRequest request, StudentClassGroupView item) {
        if (item.isActiveEnrollment()) {
            return linkHtml(
                    request.getContextPath() + "/student/class-groups/" + item.getClassGroup().getId(),
                    "gape-student-card-icon-button",
                    "Open class group",
                    "ph ph-eye"
            ) + classGroupLeaveFormHtml(request, item.getClassGroup().getId(), "px-12 py-7 text-12");
        }
        if (item.isCanEnroll()) {
            return classGroupRequestFormHtml(request, item.getClassGroup().getId(), "px-12 py-7 text-12");
        }
        return classGroupUnavailableActionHtml(item, "px-12 py-7 text-12");
    }

    private String classGroupDetailPageActionsHtml(HttpServletRequest request, StudentClassGroupView item) {
        if (item.isActiveEnrollment()) {
            return linkHtml(
                    "#student-class-group-structure",
                    "gape-student-card-icon-button",
                    "Open structure",
                    "ph ph-stack"
            ) + classGroupLeaveFormHtml(request, item.getClassGroup().getId(), "px-14 py-8 text-13");
        }
        if (item.isCanEnroll()) {
            return classGroupRequestFormHtml(request, item.getClassGroup().getId(), "px-14 py-8 text-13");
        }
        return classGroupUnavailableActionHtml(item, "px-14 py-8 text-13");
    }

    private String classGroupListActionsHtml(HttpServletRequest request, StudentClassGroupView item) {
        if (item.isActiveEnrollment()) {
            return linkHtml(
                    request.getContextPath() + "/student/class-groups/" + item.getClassGroup().getId(),
                    "gape-student-card-icon-button",
                    "Open class group",
                    "ph ph-eye"
            ) + classGroupLeaveFormHtml(request, item.getClassGroup().getId(), "px-14 py-8 text-13");
        }
        if (item.isCanEnroll()) {
            return classGroupRequestFormHtml(request, item.getClassGroup().getId(), "px-14 py-8 text-13");
        }
        return classGroupUnavailableActionHtml(item, "px-14 py-8 text-13");
    }

    private String subjectRequestFormHtml(HttpServletRequest request, long courseId, long subjectId, String sizeClass) {
        return formHtml(
                request,
                "/student/enrollments/courses/" + courseId + "/subjects/" + subjectId,
                "gape-student-card-icon-button gape-student-card-icon-button--request",
                "Request enrollment",
                "ph ph-user-plus"
        );
    }

    private String subjectLeaveFormHtml(HttpServletRequest request, long courseId, long subjectId, String sizeClass) {
        return formHtml(
                request,
                "/student/enrollments/courses/" + courseId + "/subjects/" + subjectId + "/withdraw",
                "gape-student-card-icon-button gape-student-card-icon-button--danger",
                "Leave subject",
                "ph ph-sign-out"
        );
    }

    private String classGroupRequestFormHtml(HttpServletRequest request, long classGroupId, String sizeClass) {
        return formHtml(
                request,
                "/student/enrollments/class-groups/" + classGroupId,
                "gape-student-card-icon-button gape-student-card-icon-button--request",
                "Request enrollment",
                "ph ph-user-plus"
        );
    }

    private String classGroupLeaveFormHtml(HttpServletRequest request, long classGroupId, String sizeClass) {
        return formHtml(
                request,
                "/student/enrollments/class-groups/" + classGroupId + "/withdraw",
                "gape-student-card-icon-button gape-student-card-icon-button--danger",
                "Leave class group",
                "ph ph-sign-out"
        );
    }

    private static String classGroupUnavailableActionHtml(StudentClassGroupView item, String sizeClass) {
        return "<span class=\"border border-neutral-30 " + htmlAttribute(sizeClass)
                + " rounded-8 fw-semibold text-neutral-500 bg-neutral-20\">"
                + html(item.getUnavailableActionLabel())
                + "</span>";
    }

    private String formHtml(HttpServletRequest request, String action, String buttonClass, String label, String iconClass) {
        return "<form action=\"" + htmlAttribute(request.getContextPath() + action) + "\" method=\"post\" class=\"m-0\">"
                + "<input type=\"hidden\" name=\"csrfToken\" value=\"" + htmlAttribute(csrfToken(request)) + "\">"
                + "<input type=\"hidden\" name=\"returnTo\" value=\"" + htmlAttribute(dynamicReturnTo(request)) + "\">"
                + "<button type=\"submit\" class=\"" + htmlAttribute(buttonClass) + "\" aria-label=\"" + htmlAttribute(label)
                + "\" title=\"" + htmlAttribute(label) + "\"><i class=\"" + htmlAttribute(iconClass) + "\"></i></button>"
                + "</form>";
    }

    private static String linkHtml(String href, String cssClass, String label, String iconClass) {
        return "<a href=\"" + htmlAttribute(href) + "\" class=\"" + htmlAttribute(cssClass)
                + "\" aria-label=\"" + htmlAttribute(label) + "\" title=\"" + htmlAttribute(label) + "\">"
                + "<i class=\"" + htmlAttribute(iconClass) + "\"></i>"
                + "</a>";
    }

    private static String dynamicReturnTo(HttpServletRequest request) {
        String returnTo = text(request, "returnTo");
        return returnTo == null || !returnTo.startsWith("/") || returnTo.startsWith("//") || returnTo.contains("\\") || returnTo.contains(":")
                ? "/student/enrollments"
                : returnTo;
    }

    private static String csrfToken(HttpServletRequest request) {
        Object token = request.getSession(false) == null ? null : request.getSession(false).getAttribute("gape.auth.csrfToken");
        return token == null ? "" : token.toString();
    }

    private static boolean wantsStudentEnrollmentJson(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "fetch".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.toLowerCase(java.util.Locale.ROOT).contains("application/json"));
    }

    private static String messageForEnrollmentException(Exception exception) {
        if (exception instanceof RuntimeException runtimeException) {
            return messageFor(runtimeException);
        }
        return "The operation was saved, but the updated enrollment state could not be loaded.";
    }

    private static void writeStudentEnrollmentResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            boolean success,
            String message,
            List<StudentEnrollmentUpdate> updates
    ) throws IOException {
        response.setStatus(success ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(studentEnrollmentJson(success, message, updates));
    }

    private static String studentEnrollmentJson(
            boolean success,
            String message,
            List<StudentEnrollmentUpdate> updates
    ) {
        StringBuilder json = new StringBuilder();
        json.append('{');
        json.append("\"success\":").append(success);
        json.append(",\"message\":").append(jsonString(message));
        json.append(",\"updates\":[");
        for (int i = 0; i < updates.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            StudentEnrollmentUpdate update = updates.get(i);
            json.append('{');
            json.append("\"target\":").append(jsonString(update.target()));
            json.append(",\"stateLabel\":").append(jsonString(update.stateLabel()));
            json.append(",\"badgeClass\":").append(jsonString(update.badgeClass()));
            json.append(",\"actions\":{");
            int actionIndex = 0;
            for (Map.Entry<String, String> action : update.actions().entrySet()) {
                if (actionIndex++ > 0) {
                    json.append(',');
                }
                json.append(jsonString(action.getKey())).append(':').append(jsonString(action.getValue()));
            }
            json.append("}}");
        }
        json.append("]}");
        return json.toString();
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (current < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) current));
                    } else {
                        escaped.append(current);
                    }
                }
            }
        }
        escaped.append('"');
        return escaped.toString();
    }

    private static String htmlAttribute(String value) {
        return html(value);
    }

    private static String html(String value) {
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

    private record StudentEnrollmentUpdate(
            String target,
            String stateLabel,
            String badgeClass,
            Map<String, String> actions
    ) {
    }

    private List<StudentClassGroupView> studentClassGroupViews(SessionUser actor, HttpServletRequest request) throws SQLException {
        long studentUserId = actor.userId();
        Map<Long, ClassGroupEnrollment> enrollmentsByClassGroup = new HashMap<>();
        for (ClassGroupEnrollment enrollment : classGroupEnrollmentDAO.findByStudent(studentUserId)) {
            enrollmentsByClassGroup.put(enrollment.classGroupId(), enrollment);
        }

        Set<String> activeSubjectContexts = new HashSet<>();
        Map<Long, ClassGroup> classGroupsById = new HashMap<>();
        for (SubjectEnrollment enrollment : enrollmentDAO.findActiveSubjectEnrollmentsByStudent(studentUserId)) {
            activeSubjectContexts.add(subjectContextKey(enrollment.courseId(), enrollment.subjectId()));
            for (ClassGroup classGroup : classGroupDAO.findByCourseAndSubject(
                    enrollment.courseId(),
                    enrollment.subjectId()
            )) {
                classGroupsById.put(classGroup.id(), classGroup);
            }
        }

        for (long classGroupId : enrollmentsByClassGroup.keySet()) {
            classGroupDAO.findById(classGroupId).ifPresent(classGroup -> classGroupsById.putIfAbsent(
                    classGroup.id(),
                    classGroup
            ));
        }

        return classGroupsById.values()
                .stream()
                .sorted(Comparator.comparing(ClassGroup::courseId)
                        .thenComparing(ClassGroup::subjectId)
                        .thenComparing(ClassGroup::code))
                .map(classGroup -> studentClassGroupView(
                        actor,
                        request,
                        classGroup,
                        enrollmentsByClassGroup.get(classGroup.id()),
                        canEnrollInClassGroupContext(classGroup, activeSubjectContexts)
                ))
                .toList();
    }

    private Map<Long, List<StudentClassGroupView>> studentCourseClassGroups(
            SessionUser actor,
            HttpServletRequest request,
            List<CourseSubjectView> courseSubjects
    ) throws SQLException {
        long studentUserId = actor.userId();
        Map<Long, ClassGroupEnrollment> enrollmentsByClassGroup = new HashMap<>();
        for (ClassGroupEnrollment enrollment : classGroupEnrollmentDAO.findByStudent(studentUserId)) {
            enrollmentsByClassGroup.put(enrollment.classGroupId(), enrollment);
        }

        Set<String> activeSubjectContexts = new HashSet<>();
        for (SubjectEnrollment enrollment : enrollmentDAO.findActiveSubjectEnrollmentsByStudent(studentUserId)) {
            activeSubjectContexts.add(subjectContextKey(enrollment.courseId(), enrollment.subjectId()));
        }

        Map<Long, List<StudentClassGroupView>> classGroupsBySubject = new LinkedHashMap<>();
        for (CourseSubjectView subject : courseSubjects) {
            List<StudentClassGroupView> classGroups = classGroupDAO
                    .findByCourseAndSubject(subject.getCourseId(), subject.getSubjectId())
                    .stream()
                    .sorted(Comparator.comparing(ClassGroup::code))
                    .map(classGroup -> studentClassGroupView(
                            actor,
                            request,
                            classGroup,
                            enrollmentsByClassGroup.get(classGroup.id()),
                            canEnrollInClassGroupContext(classGroup, activeSubjectContexts)
                    ))
                    .toList();
            classGroupsBySubject.put(subject.getSubjectId(), classGroups);
        }
        return classGroupsBySubject;
    }

    private StudentClassGroupView studentClassGroupView(
            SessionUser actor,
            HttpServletRequest request,
            ClassGroup classGroup,
            ClassGroupEnrollment enrollment,
            boolean eligibleForEnrollment
    ) {
        ClassGroupView classGroupView = viewFactory.classGroupView(classGroup);
        ClassGroupEnrollmentView enrollmentView = enrollment == null
                ? null
                : viewFactory.classGroupEnrollmentView(enrollment);
        List<ContentBlockView> contentBlocks = enrollmentView != null && enrollmentView.isActive()
                ? studentContentBlocks(classGroup.id())
                : List.of();
        Map<Long, List<BlockContentItemView>> blockContentsByBlock = enrollmentView != null && enrollmentView.isActive()
                ? visibleStudentBlockContents(actor, request, contentBlocks)
                : Map.of();
        Map<Long, List<pt.isel.gape.web.view.LessonView>> blockLessonsByBlock = enrollmentView != null && enrollmentView.isActive()
                ? visibleStudentBlockLessons(actor, request, contentBlocks)
                : Map.of();
        Map<Long, List<BlockActivityView>> blockActivitiesByBlock = enrollmentView != null && enrollmentView.isActive()
                ? visibleStudentBlockActivities(contentBlocks, blockContentsByBlock, blockLessonsByBlock)
                : Map.of();
        return StudentClassGroupView.of(
                classGroupView,
                enrollmentView,
                contentBlocks,
                blockContentsByBlock,
                blockLessonsByBlock,
                blockActivitiesByBlock,
                eligibleForEnrollment
        );
    }

    private Map<Long, List<BlockActivityView>> visibleStudentBlockActivities(
            List<ContentBlockView> contentBlocks,
            Map<Long, List<BlockContentItemView>> blockContentsByBlock,
            Map<Long, List<pt.isel.gape.web.view.LessonView>> blockLessonsByBlock
    ) {
        Map<Long, List<BlockActivityView>> result = new LinkedHashMap<>();
        for (ContentBlockView block : contentBlocks) {
            List<BlockActivityView> activities = new ArrayList<>();
            blockContentsByBlock.getOrDefault(block.getId(), List.of()).stream()
                    .map(BlockActivityView::fromContent)
                    .forEach(activities::add);
            blockLessonsByBlock.getOrDefault(block.getId(), List.of()).stream()
                    .map(BlockActivityView::fromLesson)
                    .forEach(activities::add);
            activities.sort(Comparator
                    .comparing(BlockActivityView::getSortDate)
                    .thenComparingInt(BlockActivityView::getSortKind)
                    .thenComparingLong(BlockActivityView::getSortId));
            result.put(block.getId(), activities);
        }
        return result;
    }

    private Map<Long, List<pt.isel.gape.web.view.LessonView>> visibleStudentBlockLessons(
            SessionUser actor,
            HttpServletRequest request,
            List<ContentBlockView> contentBlocks
    ) {
        Map<Long, List<pt.isel.gape.web.view.LessonView>> result = new LinkedHashMap<>();
        for (ContentBlockView block : contentBlocks) {
            List<pt.isel.gape.web.view.LessonView> lessons = lessonService.listLessonsByContentBlock(
                            actor.userId(),
                            currentSessionId(request),
                            AccessProfileType.STUDENT,
                            block.getId(),
                            request.getRemoteAddr()
                    )
                    .stream()
                    .map(viewFactory::lessonView)
                    .toList();
            result.put(block.getId(), lessons);
        }
        return result;
    }

    private Map<Long, List<BlockContentItemView>> visibleStudentBlockContents(
            SessionUser actor,
            HttpServletRequest request,
            List<ContentBlockView> contentBlocks
    ) {
        Map<Long, List<BlockContentItemView>> result = new LinkedHashMap<>();
        for (ContentBlockView block : contentBlocks) {
            List<BlockContentItemView> items = contentAssociationService.listBlockContentItems(
                            actor.userId(),
                            currentSessionId(request),
                            AccessProfileType.STUDENT,
                            block.getId(),
                            request.getRemoteAddr()
                    )
                    .stream()
                    .map(BlockContentItemView::from)
                    .filter(BlockContentItemView::isActive)
                    .toList();
            result.put(block.getId(), items);
        }
        return result;
    }

    private List<ContentBlockView> studentContentBlocks(long classGroupId) {
        try {
            return contentBlockDAO.findByClassGroup(classGroupId)
                    .stream()
                    .map(viewFactory::contentBlockView)
                    .filter(ContentBlockView::isActive)
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load class group content blocks", exception);
        }
    }

    private static String subjectContextKey(long courseId, long subjectId) {
        return courseId + ":" + subjectId;
    }

    private static boolean canEnrollInClassGroupContext(
            ClassGroup classGroup,
            Set<String> activeSubjectContexts
    ) {
        String key = subjectContextKey(classGroup.courseId(), classGroup.subjectId());
        return activeSubjectContexts.contains(key);
    }

    private static LocalDate optionalDate(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : LocalDate.parse(value);
    }
}
