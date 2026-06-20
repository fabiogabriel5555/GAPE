package pt.isel.gape.web.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.ContentBlockAccessMode;
import pt.isel.gape.learning.model.ContentBlockState;
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
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.web.view.ClassGroupEnrollmentView;
import pt.isel.gape.web.view.ClassGroupView;
import pt.isel.gape.web.view.BlockContentItemView;
import pt.isel.gape.web.view.ContentBlockView;
import pt.isel.gape.web.view.CourseSubjectView;
import pt.isel.gape.web.view.CourseView;
import pt.isel.gape.web.view.EnrollmentView;
import pt.isel.gape.web.view.StudentClassGroupView;

@WebServlet(name = "studentEnrollmentServlet", urlPatterns = {"/student/enrollments", "/student/enrollments/*"})
public final class StudentEnrollmentServlet extends DashboardServletSupport {

    private static final String STUDENT_ENROLLMENTS_JSP = "/student/student-enrolled-courses.jsp";

    private final EnrollmentService enrollmentService;
    private final ClassGroupEnrollmentService classGroupEnrollmentService;
    private final ContentAssociationService contentAssociationService;
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
            request.setAttribute("studentClassGroups", studentClassGroups);
            request.setAttribute("availableCourses", availableCourses);
            prepareDashboard(request, "courses", "My Courses");
            forward(request, response, STUDENT_ENROLLMENTS_JSP);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load student enrollments", exception);
        }
    }

    private void enrollCourse(HttpServletRequest request, HttpServletResponse response, long courseId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.enrollStudentInCourse(
                    actor.userId(),
                    currentSessionId(request),
                    AccessProfileType.STUDENT,
                    new CourseEnrollmentCommand(actor.userId(), courseId, optionalDate(request, "startDate"), optionalDate(request, "endDate")),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Course enrollment completed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
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
            flashSuccess(request, "Course withdrawal completed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/student/enrollments");
    }

    private void enrollSubject(HttpServletRequest request, HttpServletResponse response, long courseId, long subjectId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            enrollmentService.enrollStudentInSubject(
                    actor.userId(),
                    currentSessionId(request),
                    AccessProfileType.STUDENT,
                    new SubjectEnrollmentCommand(actor.userId(), subjectId, courseId, optionalDate(request, "startDate"), optionalDate(request, "endDate")),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Subject enrollment completed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
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
            flashSuccess(request, "Subject withdrawal completed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/student/enrollments");
    }

    private void enrollClassGroup(HttpServletRequest request, HttpServletResponse response, long classGroupId)
            throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            classGroupEnrollmentService.enrollStudentInClassGroup(
                    actor.userId(),
                    currentSessionId(request),
                    AccessProfileType.STUDENT,
                    new ClassGroupEnrollmentCommand(
                            actor.userId(),
                            classGroupId,
                            optionalDate(request, "startDate"),
                            optionalDate(request, "endDate")
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Class group enrollment completed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
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
            flashSuccess(request, "Class group withdrawal completed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, "/student/enrollments");
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

        Set<String> activeClassGroupContexts = new HashSet<>();
        for (ClassGroupEnrollment enrollment : enrollmentsByClassGroup.values()) {
            if (enrollment.state() != EnrollmentState.ACTIVE) {
                continue;
            }
            ClassGroup classGroup = classGroupsById.get(enrollment.classGroupId());
            if (classGroup != null) {
                activeClassGroupContexts.add(subjectContextKey(classGroup.courseId(), classGroup.subjectId()));
            }
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
                        canEnrollInClassGroupContext(classGroup, activeSubjectContexts, activeClassGroupContexts)
                ))
                .toList();
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
                ? visibleStudentContentBlocks(classGroup.id())
                : List.of();
        Map<Long, List<BlockContentItemView>> blockContentsByBlock = enrollmentView != null && enrollmentView.isActive()
                ? visibleStudentBlockContents(actor, request, contentBlocks)
                : Map.of();
        return StudentClassGroupView.of(
                classGroupView,
                enrollmentView,
                contentBlocks,
                blockContentsByBlock,
                eligibleForEnrollment
        );
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

    private List<ContentBlockView> visibleStudentContentBlocks(long classGroupId) {
        try {
            LocalDateTime now = LocalDateTime.now(clock);
            return contentBlockDAO.findByClassGroup(classGroupId)
                    .stream()
                    .filter(block -> isVisibleToStudent(block, now))
                    .map(viewFactory::contentBlockView)
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load class group content blocks", exception);
        }
    }

    private static boolean isVisibleToStudent(ContentBlock block, LocalDateTime now) {
        if (block.state() != ContentBlockState.ACTIVE) {
            return false;
        }
        if (block.accessMode() != ContentBlockAccessMode.SCHEDULED) {
            return true;
        }
        return block.availableFrom() != null
                && !block.availableFrom().isAfter(now)
                && (block.availableUntil() == null || !block.availableUntil().isBefore(now));
    }

    private static String subjectContextKey(long courseId, long subjectId) {
        return courseId + ":" + subjectId;
    }

    private static boolean canEnrollInClassGroupContext(
            ClassGroup classGroup,
            Set<String> activeSubjectContexts,
            Set<String> activeClassGroupContexts
    ) {
        String key = subjectContextKey(classGroup.courseId(), classGroup.subjectId());
        return activeSubjectContexts.contains(key) && !activeClassGroupContexts.contains(key);
    }

    private static LocalDate optionalDate(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null ? null : LocalDate.parse(value);
    }
}
