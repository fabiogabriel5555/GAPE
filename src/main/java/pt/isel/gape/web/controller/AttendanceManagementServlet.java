package pt.isel.gape.web.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.AssessmentEnrollmentDAO;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.ClassGroupEnrollmentDAO;
import pt.isel.gape.learning.dao.ContentBlockDAO;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.dao.LessonDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.AbsenceJustification;
import pt.isel.gape.learning.model.AbsenceJustificationCreateCommand;
import pt.isel.gape.learning.model.AbsenceJustificationProcessCommand;
import pt.isel.gape.learning.model.AbsenceJustificationState;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AssessmentEnrollment;
import pt.isel.gape.learning.model.AttendanceRecord;
import pt.isel.gape.learning.model.AttendanceRecordCommand;
import pt.isel.gape.learning.model.AttendanceSource;
import pt.isel.gape.learning.model.AttendanceState;
import pt.isel.gape.learning.model.AttendanceStatus;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupEnrollment;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectEnrollment;
import pt.isel.gape.learning.service.AbsenceJustificationService;
import pt.isel.gape.learning.service.AttendanceRecordService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.web.view.AbsenceJustificationView;
import pt.isel.gape.web.view.AttendanceRecordView;
import pt.isel.gape.web.view.SelectOptionView;
import pt.isel.gape.web.media.JustificationAttachmentStorage;

@MultipartConfig(
        fileSizeThreshold = 1048576,
        maxFileSize = 10485760,
        maxRequestSize = 12582912
)
@WebServlet(name = "attendanceManagementServlet", urlPatterns = {
        "/learning/attendance",
        "/learning/attendance/*",
        "/student/attendance",
        "/student/attendance/*"
})
public final class AttendanceManagementServlet extends DashboardServletSupport {

    private static final String LEARNING_ATTENDANCE_JSP = "/WEB-INF/views/learning/attendance.jsp";
    private static final String STUDENT_ATTENDANCE_JSP = "/student/student/attendance/student-attendance.jsp";
    private static final DateTimeFormatter INPUT_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final AttendanceRecordService attendanceRecordService;
    private final AbsenceJustificationService justificationService;
    private final ScheduleAttendanceViewFactory viewFactory;
    private final GradeCertificateServlet gradeCertificateServlet;
    private final JustificationAttachmentStorage attachmentStorage;
    private final EnrollmentDAO enrollmentDAO;
    private final ClassGroupEnrollmentDAO classGroupEnrollmentDAO;
    private final AssessmentEnrollmentDAO assessmentEnrollmentDAO;
    private final CourseDAO courseDAO;
    private final SubjectDAO subjectDAO;
    private final ClassGroupDAO classGroupDAO;
    private final AssessmentDAO assessmentDAO;
    private final UserDAO userDAO;
    private final Clock clock;

    public AttendanceManagementServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private AttendanceManagementServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new AttendanceRecordService(connectionProvider, clock),
                new AbsenceJustificationService(connectionProvider, clock),
                new ScheduleAttendanceViewFactory(
                        new ClassGroupDAO(connectionProvider),
                        new LessonDAO(connectionProvider),
                        new UserDAO(connectionProvider),
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
                        )
                ),
                new GradeCertificateServlet(),
                new JustificationAttachmentStorage(),
                new EnrollmentDAO(connectionProvider),
                new ClassGroupEnrollmentDAO(connectionProvider),
                new AssessmentEnrollmentDAO(connectionProvider),
                new CourseDAO(connectionProvider),
                new SubjectDAO(connectionProvider),
                new ClassGroupDAO(connectionProvider),
                new AssessmentDAO(connectionProvider),
                new UserDAO(connectionProvider),
                clock
        );
    }

    AttendanceManagementServlet(
            AttendanceRecordService attendanceRecordService,
            AbsenceJustificationService justificationService,
            ScheduleAttendanceViewFactory viewFactory,
            Clock clock
    ) {
        this(attendanceRecordService, justificationService, viewFactory, new GradeCertificateServlet(),
                new JustificationAttachmentStorage(),
                new EnrollmentDAO(ConnectionProvider.defaultProvider()),
                new ClassGroupEnrollmentDAO(ConnectionProvider.defaultProvider()),
                new AssessmentEnrollmentDAO(ConnectionProvider.defaultProvider()),
                new CourseDAO(ConnectionProvider.defaultProvider()),
                new SubjectDAO(ConnectionProvider.defaultProvider()),
                new ClassGroupDAO(ConnectionProvider.defaultProvider()),
                new AssessmentDAO(ConnectionProvider.defaultProvider()),
                new UserDAO(ConnectionProvider.defaultProvider()),
                clock);
    }

    AttendanceManagementServlet(
            AttendanceRecordService attendanceRecordService,
            AbsenceJustificationService justificationService,
            ScheduleAttendanceViewFactory viewFactory,
            GradeCertificateServlet gradeCertificateServlet,
            JustificationAttachmentStorage attachmentStorage,
            EnrollmentDAO enrollmentDAO,
            ClassGroupEnrollmentDAO classGroupEnrollmentDAO,
            AssessmentEnrollmentDAO assessmentEnrollmentDAO,
            CourseDAO courseDAO,
            SubjectDAO subjectDAO,
            ClassGroupDAO classGroupDAO,
            AssessmentDAO assessmentDAO,
            UserDAO userDAO,
            Clock clock
    ) {
        this.attendanceRecordService = attendanceRecordService;
        this.justificationService = justificationService;
        this.viewFactory = viewFactory;
        this.gradeCertificateServlet = gradeCertificateServlet;
        this.attachmentStorage = attachmentStorage;
        this.enrollmentDAO = enrollmentDAO;
        this.classGroupEnrollmentDAO = classGroupEnrollmentDAO;
        this.assessmentEnrollmentDAO = assessmentEnrollmentDAO;
        this.courseDAO = courseDAO;
        this.subjectDAO = subjectDAO;
        this.classGroupDAO = classGroupDAO;
        this.assessmentDAO = assessmentDAO;
        this.userDAO = userDAO;
        this.clock = clock;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        if (!isStudentRequest(request)
                && segments.length == 2
                && "enrollments".equals(segments[0])
                && "download".equals(segments[1])) {
            downloadEnrollment(request, response);
            return;
        }
        if (segments.length != 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (isStudentRequest(request)) {
            showStudentAttendance(request, response);
            return;
        }
        showLearningAttendance(request, response);
    }

    private void downloadEnrollment(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        if (primaryProfile(actor) == AccessProfileType.STUDENT) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String type = defaultText(request, "type", "enrollment");
        String studentUserId = defaultText(request, "studentUserId", "-");
        String contextId = defaultText(request, "contextId", "-");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"enrollment-" + type + "-" + studentUserId + "-" + contextId + ".txt\"");
        var writer = response.getWriter();
        writer.println("Enrollment detail");
        writer.println("Type: " + type);
        writer.println("Student user id: " + studentUserId);
        writer.println("Context id: " + contextId);
        writer.println("Parent context id: " + defaultText(request, "parentContextId", "-"));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
        if (isStudentRequest(request)) {
            if (segments.length == 1 && "justifications".equals(segments[0])) {
                submitJustification(request, response);
                return;
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (segments.length == 0) {
            recordAttendance(request, response);
            return;
        }
        if (segments.length == 3 && "justifications".equals(segments[0])) {
            try {
                processJustification(request, response, Long.parseLong(segments[1]), segments[2]);
            } catch (NumberFormatException exception) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void showLearningAttendance(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        AccessProfileType profile = primaryProfile(actor);
        if (profile == AccessProfileType.STUDENT) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        List<AbsenceJustification> justifications = justificationService.listVisibleJustifications(
                actor.userId(),
                currentSessionId(request),
                profile,
                request.getRemoteAddr()
        );
        Set<Long> justifiedRecordIds = justifications.stream()
                .map(AbsenceJustification::attendanceRecordId)
                .collect(Collectors.toSet());
        List<AttendanceRecord> records = attendanceRecordService.listVisibleAttendance(
                actor.userId(),
                currentSessionId(request),
                profile,
                request.getRemoteAddr()
        );
        List<AttendanceRecordView> allAttendanceViews = viewFactory.attendanceRecordViews(records, justifiedRecordIds);
        List<AbsenceJustificationView> allJustificationViews =
                viewFactory.justificationViews(justifications, allAttendanceViews);

        String selectedStatus = normalizedFilter(request, "status");
        String selectedJustificationState = normalizedFilter(request, "justificationState");
        List<AttendanceRecordView> attendanceViews = allAttendanceViews.stream()
                .filter(record -> selectedStatus == null || selectedStatus.equals(record.getStatusValue()))
                .toList();
        List<AbsenceJustificationView> justificationViews = allJustificationViews.stream()
                .filter(justification -> selectedJustificationState == null
                        || selectedJustificationState.equals(justification.getStateValue()))
                .toList();

        request.setAttribute("attendanceRecords", attendanceViews);
        request.setAttribute("justifications", justificationViews);
        request.setAttribute("attendanceCount", attendanceViews.size());
        request.setAttribute("absenceCount", attendanceViews.stream()
                .filter(record -> "absent".equals(record.getStatusValue()))
                .count());
        request.setAttribute("lateOrPartialCount", attendanceViews.stream()
                .filter(record -> "late".equals(record.getStatusValue()) || "partial".equals(record.getStatusValue()))
                .count());
        request.setAttribute("pendingJustificationCount", allJustificationViews.stream()
                .filter(AbsenceJustificationView::isSubmitted)
                .count());
        request.setAttribute("selectedStatus", selectedStatus);
        request.setAttribute("selectedJustificationState", selectedJustificationState);
        request.setAttribute("attendanceStatusOptions", attendanceStatusOptions(selectedStatus));
        request.setAttribute("attendanceCreateStatusOptions", attendanceCreationStatusOptions());
        request.setAttribute("attendanceCreateSourceOptions", attendanceCreationSourceOptions());
        request.setAttribute("justificationStateOptions", justificationStateOptions(selectedJustificationState));
        request.setAttribute("defaultCheckIn", INPUT_DATE_TIME.format(currentMinute()));
        try {
            populateEnrollmentOverviewAttributes(request);
            gradeCertificateServlet.populateManagementAttributes(request, actor, profile, request.getRemoteAddr());
            prepareDashboard(request, "attendance", "Enrollments & Certificates");
            forward(request, response, LEARNING_ATTENDANCE_JSP);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load attendance management", exception);
        }
    }

    private void showStudentAttendance(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser actor = requireCurrentUser(request);
        List<AbsenceJustification> justifications = justificationService.listOwnJustifications(
                actor.userId(),
                currentSessionId(request),
                AccessProfileType.STUDENT,
                request.getRemoteAddr()
        );
        Set<Long> justifiedRecordIds = justifications.stream()
                .map(AbsenceJustification::attendanceRecordId)
                .collect(Collectors.toSet());
        List<AttendanceRecord> records = attendanceRecordService.listOwnAttendance(
                actor.userId(),
                currentSessionId(request),
                AccessProfileType.STUDENT,
                request.getRemoteAddr()
        );
        List<AttendanceRecordView> attendanceViews = viewFactory.attendanceRecordViews(records, justifiedRecordIds);
        List<AbsenceJustificationView> justificationViews =
                viewFactory.justificationViews(justifications, attendanceViews);

        request.setAttribute("attendanceRecords", attendanceViews);
        request.setAttribute("justifications", justificationViews);
        request.setAttribute("attendanceCount", attendanceViews.size());
        request.setAttribute("absenceNotificationCount", attendanceViews.stream()
                .filter(AttendanceRecordView::isCanSubmitJustification)
                .count());
        try {
            gradeCertificateServlet.populateStudentAttributes(request, actor);
            prepareDashboard(request, "attendance", "Enrollments & Certificates");
            forward(request, response, STUDENT_ATTENDANCE_JSP);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load student grades and certificates", exception);
        }
    }

    private void populateEnrollmentOverviewAttributes(HttpServletRequest request) throws SQLException {
        Map<Long, Course> courses = mapCourses(courseDAO.findCatalogCourses(null, null, null));
        Map<Long, Subject> subjects = mapSubjects(subjectDAO.findAll());
        Map<Long, ClassGroup> classGroups = mapClassGroups(classGroupDAO.findAll());
        Map<Long, Assessment> assessments = mapAssessments(assessmentDAO.findAll());
        Map<Long, User> users = mapUsers(userDAO.findAll());

        List<EnrollmentOverviewView> rows = new ArrayList<>();
        Set<String> studentClassGroupKeys = new HashSet<>();
        for (Course course : courses.values()) {
            for (CourseEnrollment enrollment : enrollmentDAO.findCourseEnrollmentsByCourse(course.id())) {
                rows.add(enrollmentRow(
                        1,
                        "Course",
                        courseLabel(course),
                        null,
                        "course",
                        course.id(),
                        null,
                        course.id(),
                        null,
                        null,
                        enrollment.studentUserId(),
                        users,
                        enrollment.state(),
                        enrollment.startDate(),
                        enrollment.endDate(),
                        "/admin/courses/" + course.id() + "/enrollments/" + enrollment.studentUserId() + "/update",
                        "/admin/courses/" + course.id() + "/enrollments/" + enrollment.studentUserId() + "/delete"
                ));
            }
        }
        for (Subject subject : subjects.values()) {
            for (SubjectEnrollment enrollment : enrollmentDAO.findSubjectEnrollmentsBySubject(subject.id())) {
                rows.add(enrollmentRow(
                        2,
                        "Subject",
                        subjectLabel(subject),
                        courseLabel(courses.get(enrollment.courseId())),
                        "subject",
                        subject.id(),
                        enrollment.courseId(),
                        enrollment.courseId(),
                        subject.id(),
                        null,
                        enrollment.studentUserId(),
                        users,
                        enrollment.state(),
                        enrollment.startDate(),
                        enrollment.endDate(),
                        "/admin/subjects/" + subject.id() + "/enrollments/" + enrollment.courseId()
                                + "/" + enrollment.studentUserId() + "/update",
                        "/admin/subjects/" + subject.id() + "/enrollments/" + enrollment.courseId()
                                + "/" + enrollment.studentUserId() + "/delete"
                ));
            }
        }
        for (ClassGroup classGroup : classGroups.values()) {
            for (ClassGroupEnrollment enrollment : classGroupEnrollmentDAO.findByClassGroup(classGroup.id())) {
                studentClassGroupKeys.add(enrollmentKey(enrollment.studentUserId(), classGroup.id()));
                rows.add(enrollmentRow(
                        3,
                        "Class Group",
                        classGroup.code(),
                        classGroupContextLabel(classGroup, subjects, courses),
                        "class-group",
                        classGroup.id(),
                        classGroup.subjectId(),
                        classGroup.courseId(),
                        classGroup.subjectId(),
                        classGroup.id(),
                        enrollment.studentUserId(),
                        users,
                        enrollment.state(),
                        enrollment.startDate(),
                        enrollment.endDate(),
                        "/learning/class-groups/" + classGroup.id() + "/enrollments/"
                                + enrollment.studentUserId() + "/update",
                        "/learning/class-groups/" + classGroup.id() + "/enrollments/"
                                + enrollment.studentUserId() + "/delete"
                ));
            }
        }
        for (Assessment assessment : assessments.values()) {
            List<Long> applicableClassGroupIds = assessmentDAO.findApplicableClassGroupIds(assessment.id());
            for (AssessmentEnrollment enrollment : assessmentEnrollmentDAO.findByAssessment(assessment.id())) {
                boolean attached = false;
                for (Long classGroupId : applicableClassGroupIds) {
                    ClassGroup classGroup = classGroups.get(classGroupId);
                    if (classGroup == null
                            || !studentClassGroupKeys.contains(enrollmentKey(enrollment.studentUserId(), classGroupId))) {
                        continue;
                    }
                    rows.add(enrollmentRow(
                            4,
                            "Assessment",
                            assessment.title(),
                            assessmentContextLabel(assessment, subjects) + " | " + classGroup.code(),
                            "assessment",
                            assessment.id(),
                            classGroup.id(),
                            classGroup.courseId(),
                            classGroup.subjectId(),
                            classGroup.id(),
                            enrollment.studentUserId(),
                            users,
                            enrollment.state(),
                            null,
                            null,
                            "/learning/assessments/" + assessment.id() + "/enrollments/"
                                    + enrollment.studentUserId() + "/update",
                            "/learning/assessments/" + assessment.id() + "/enrollments/"
                                    + enrollment.studentUserId() + "/delete"
                    ));
                    attached = true;
                }
                if (!attached) {
                    rows.add(enrollmentRow(
                            4,
                            "Assessment",
                            assessment.title(),
                            assessmentContextLabel(assessment, subjects),
                            "assessment",
                            assessment.id(),
                            assessment.subjectId(),
                            null,
                            assessment.subjectId(),
                            null,
                            enrollment.studentUserId(),
                            users,
                            enrollment.state(),
                            null,
                            null,
                            "/learning/assessments/" + assessment.id() + "/enrollments/"
                                    + enrollment.studentUserId() + "/update",
                            "/learning/assessments/" + assessment.id() + "/enrollments/"
                                    + enrollment.studentUserId() + "/delete"
                    ));
                }
            }
        }

        rows.sort(Comparator
                .comparing(EnrollmentOverviewView::getStudentName, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(EnrollmentOverviewView::getContextOrder)
                .thenComparing(EnrollmentOverviewView::getContextLabel, String.CASE_INSENSITIVE_ORDER));

        request.setAttribute("enrollmentRows", rows);
        request.setAttribute("enrollmentGroups", enrollmentGroups(rows));
        request.setAttribute("enrollmentCount", rows.size());
        request.setAttribute("activeEnrollmentCount", rows.stream().filter(EnrollmentOverviewView::isActive).count());
        request.setAttribute("pendingEnrollmentCount", rows.stream().filter(EnrollmentOverviewView::isPending).count());
    }

    private static EnrollmentOverviewView enrollmentRow(
            int contextOrder,
            String contextType,
            String contextLabel,
            String contextDetail,
            String contextKey,
            long contextId,
            Long parentContextId,
            Long courseId,
            Long subjectId,
            Long classGroupId,
            long studentUserId,
            Map<Long, User> users,
            EnrollmentState state,
            LocalDate startDate,
            LocalDate endDate,
            String updateAction,
            String deleteAction
    ) {
        User student = users.get(studentUserId);
        String studentName = student == null ? "Student #" + studentUserId : student.name();
        String studentEmail = student == null ? "-" : student.email();
        return new EnrollmentOverviewView(
                contextOrder,
                contextType,
                contextLabel == null || contextLabel.isBlank() ? "-" : contextLabel,
                contextDetail == null || contextDetail.isBlank() ? "-" : contextDetail,
                contextKey,
                contextId,
                parentContextId,
                courseId,
                subjectId,
                classGroupId,
                studentUserId,
                studentName,
                studentUserId + " - " + firstAndLastName(studentName),
                studentEmail,
                state.toDatabaseValue(),
                stateLabel(state),
                stateBadgeClass(state),
                periodLabel(startDate, endDate),
                startDate == null ? "" : startDate.toString(),
                endDate == null ? "" : endDate.toString(),
                state == EnrollmentState.ACTIVE,
                state == EnrollmentState.PENDING,
                updateAction,
                deleteAction,
                "/learning/attendance/enrollments/download?type=" + contextKey
                        + "&studentUserId=" + studentUserId
                        + "&contextId=" + contextId
                        + (parentContextId == null ? "" : "&parentContextId=" + parentContextId)
        );
    }

    private static String enrollmentKey(long studentUserId, long classGroupId) {
        return studentUserId + ":" + classGroupId;
    }

    private static List<EnrollmentStudentGroupView> enrollmentGroups(List<EnrollmentOverviewView> rows) {
        Map<Long, List<EnrollmentOverviewView>> byStudent = new LinkedHashMap<>();
        rows.stream()
                .sorted(Comparator
                        .comparing(EnrollmentOverviewView::getStudentName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(EnrollmentOverviewView::getStartDateSort)
                        .thenComparingInt(EnrollmentOverviewView::getContextOrder)
                        .thenComparing(EnrollmentOverviewView::getContextLabel, String.CASE_INSENSITIVE_ORDER))
                .forEach(row -> byStudent.computeIfAbsent(row.getStudentUserId(), ignored -> new ArrayList<>()).add(row));
        List<EnrollmentStudentGroupView> groups = new ArrayList<>();
        for (List<EnrollmentOverviewView> studentRows : byStudent.values()) {
            groups.add(new EnrollmentStudentGroupView(List.copyOf(studentRows)));
        }
        return List.copyOf(groups);
    }

    private static Map<Long, Course> mapCourses(List<Course> values) {
        Map<Long, Course> result = new LinkedHashMap<>();
        for (Course value : values) {
            result.put(value.id(), value);
        }
        return result;
    }

    private static Map<Long, Subject> mapSubjects(List<Subject> values) {
        Map<Long, Subject> result = new LinkedHashMap<>();
        for (Subject value : values) {
            result.put(value.id(), value);
        }
        return result;
    }

    private static Map<Long, ClassGroup> mapClassGroups(List<ClassGroup> values) {
        Map<Long, ClassGroup> result = new LinkedHashMap<>();
        for (ClassGroup value : values) {
            result.put(value.id(), value);
        }
        return result;
    }

    private static Map<Long, Assessment> mapAssessments(List<Assessment> values) {
        Map<Long, Assessment> result = new LinkedHashMap<>();
        for (Assessment value : values) {
            result.put(value.id(), value);
        }
        return result;
    }

    private static Map<Long, User> mapUsers(List<User> values) {
        Map<Long, User> result = new LinkedHashMap<>();
        for (User value : values) {
            result.put(value.id(), value);
        }
        return result;
    }

    private static String classGroupContextLabel(
            ClassGroup classGroup,
            Map<Long, Subject> subjects,
            Map<Long, Course> courses
    ) {
        return subjectLabel(subjects.get(classGroup.subjectId())) + " | " + courseLabel(courses.get(classGroup.courseId()));
    }

    private static String assessmentContextLabel(Assessment assessment, Map<Long, Subject> subjects) {
        if (assessment.subjectId() == null) {
            return "Repository or content block assessment";
        }
        return subjectLabel(subjects.get(assessment.subjectId()));
    }

    private static String courseLabel(Course course) {
        if (course == null) {
            return "-";
        }
        return course.acronym() == null || course.acronym().isBlank()
                ? course.name()
                : course.acronym() + " - " + course.name();
    }

    private static String subjectLabel(Subject subject) {
        if (subject == null) {
            return "-";
        }
        return subject.acronym() == null || subject.acronym().isBlank()
                ? subject.name()
                : subject.acronym() + " - " + subject.name();
    }

    private static String firstAndLastName(String name) {
        if (name == null || name.isBlank()) {
            return "Student";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        return parts[0] + " " + parts[parts.length - 1];
    }

    private static String periodLabel(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "No dates";
        }
        if (startDate == null) {
            return "Until " + endDate;
        }
        if (endDate == null) {
            return "From " + startDate;
        }
        return startDate + " to " + endDate;
    }

    private static String stateLabel(EnrollmentState state) {
        return switch (state) {
            case PENDING -> "Pending";
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case REJECTED -> "Rejected";
            case COMPLETED -> "Completed";
            case WITHDRAWN -> "Withdrawn";
        };
    }

    private static String stateBadgeClass(EnrollmentState state) {
        return switch (state) {
            case PENDING -> "bg-warning-50 text-warning-600";
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-neutral-30 text-neutral-600";
            case REJECTED -> "bg-danger-50 text-danger-600";
            case COMPLETED -> "bg-main-50 text-main-600";
            case WITHDRAWN -> "bg-neutral-30 text-neutral-600";
        };
    }

    private static List<EnrollmentStateSummaryView> stateSummaries(List<EnrollmentOverviewView> enrollments) {
        if (enrollments == null || enrollments.isEmpty()) {
            return List.of();
        }
        List<EnrollmentStateSummaryView> summaries = new ArrayList<>();
        for (EnrollmentState state : EnrollmentState.values()) {
            String value = state.toDatabaseValue();
            int count = 0;
            for (EnrollmentOverviewView enrollment : enrollments) {
                if (value.equals(enrollment.getStateValue())) {
                    count++;
                }
            }
            if (count > 0) {
                summaries.add(new EnrollmentStateSummaryView(count, stateLabel(state), stateBadgeClass(state)));
            }
        }
        return List.copyOf(summaries);
    }

    private void recordAttendance(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            attendanceRecordService.recordAttendance(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    attendanceCommand(request),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Attendance record saved.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/learning/attendance");
    }

    private void submitJustification(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            long attendanceRecordId = longParameter(request, "attendanceRecordId");
            justificationService.submitJustification(
                    actor.userId(),
                    currentSessionId(request),
                    AccessProfileType.STUDENT,
                    new AbsenceJustificationCreateCommand(
                            attendanceRecordId,
                            text(request, "reason"),
                            uploadedAttachment(request, actor.userId(), attendanceRecordId),
                            null
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Justification submitted.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        } catch (ServletException | IOException exception) {
            flashError(request, "Attachment upload failed.");
        }
        redirect(request, response, "/student/attendance");
    }

    private void processJustification(
            HttpServletRequest request,
            HttpServletResponse response,
            long justificationId,
            String action
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        try {
            justificationService.processJustification(
                    actor.userId(),
                    currentSessionId(request),
                    primaryProfile(actor),
                    justificationId,
                    new AbsenceJustificationProcessCommand(
                            decisionFor(action),
                            null,
                            text(request, "decisionNotes")
                    ),
                    request.getRemoteAddr()
            );
            flashSuccess(request, "Justification processed.");
        } catch (RuntimeException exception) {
            flashError(request, messageFor(exception));
        }
        redirect(request, response, "/learning/attendance");
    }

    private AttendanceRecordCommand attendanceCommand(HttpServletRequest request) {
        return new AttendanceRecordCommand(
                longParameter(request, "lessonId"),
                longParameter(request, "studentUserId"),
                AttendanceStatus.parse(defaultText(request, "status", "present")),
                AttendanceSource.parse(defaultText(request, "source", "manual")),
                optionalDateTime(request, "checkIn", null),
                optionalDateTime(request, "checkOut", null),
                text(request, "notes"),
                AttendanceState.ACTIVE
        );
    }

    private AbsenceJustificationState decisionFor(String action) {
        return switch (action) {
            case "approve" -> AbsenceJustificationState.APPROVED;
            case "reject" -> AbsenceJustificationState.REJECTED;
            default -> throw new IllegalArgumentException("Unsupported justification action");
        };
    }

    private static boolean isStudentRequest(HttpServletRequest request) {
        return "/student/attendance".equals(request.getServletPath());
    }

    private LocalDateTime currentMinute() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
    }

    private static LocalDateTime optionalDateTime(
            HttpServletRequest request,
            String name,
            LocalDateTime defaultValue
    ) {
        String value = text(request, name);
        return value == null ? defaultValue : LocalDateTime.parse(value);
    }

    private static String defaultText(HttpServletRequest request, String name, String fallback) {
        String value = text(request, name);
        return value == null ? fallback : value;
    }

    private static String normalizedFilter(HttpServletRequest request, String name) {
        String value = text(request, name);
        return value == null || "all".equals(value) ? null : value;
    }

    private static String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static List<SelectOptionView> attendanceStatusOptions(String selected) {
        return Arrays.stream(AttendanceStatus.values())
                .map(status -> new SelectOptionView(
                        status.toDatabaseValue(),
                        statusLabel(status),
                        status.toDatabaseValue().equals(selected)
                ))
                .toList();
    }

    private static List<SelectOptionView> attendanceCreationStatusOptions() {
        return Arrays.stream(AttendanceStatus.values())
                .filter(status -> status != AttendanceStatus.JUSTIFIED)
                .map(status -> new SelectOptionView(status.toDatabaseValue(), statusLabel(status), false))
                .toList();
    }

    private static List<SelectOptionView> attendanceCreationSourceOptions() {
        return Arrays.stream(AttendanceSource.values())
                .filter(source -> source != AttendanceSource.AUTOMATIC)
                .map(source -> new SelectOptionView(source.toDatabaseValue(), sourceLabel(source), false))
                .toList();
    }

    private String uploadedAttachment(HttpServletRequest request, long studentUserId, long attendanceRecordId)
            throws IOException, ServletException {
        if (!isMultipart(request)) {
            return nullIfBlank(text(request, "attachment"));
        }
        Part attachmentPart = request.getPart("attachmentFile");
        return attachmentStorage.saveAttachment(studentUserId, attendanceRecordId, attachmentPart, getServletContext());
    }

    private static boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null
                && contentType.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/");
    }

    private static List<SelectOptionView> justificationStateOptions(String selected) {
        return Arrays.stream(AbsenceJustificationState.values())
                .map(state -> new SelectOptionView(
                        state.toDatabaseValue(),
                        justificationStateLabel(state),
                        state.toDatabaseValue().equals(selected)
                ))
                .toList();
    }

    private static String statusLabel(AttendanceStatus status) {
        return switch (status) {
            case PRESENT -> "Present";
            case ABSENT -> "Absent";
            case JUSTIFIED -> "Justified";
            case LATE -> "Late";
            case PARTIAL -> "Partial";
        };
    }

    private static String sourceLabel(AttendanceSource source) {
        return switch (source) {
            case MANUAL -> "Manual";
            case AUTOMATIC -> "Automatic";
            case OTHER -> "Other";
        };
    }

    private static String justificationStateLabel(AbsenceJustificationState state) {
        return switch (state) {
            case SUBMITTED -> "Submitted";
            case UNDER_REVIEW -> "Under review";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
            case CANCELLED -> "Cancelled";
        };
    }

    public static final class EnrollmentOverviewView {
        private final int contextOrder;
        private final String contextType;
        private final String contextLabel;
        private final String contextDetail;
        private final String contextKey;
        private final long contextId;
        private final Long parentContextId;
        private final Long courseId;
        private final Long subjectId;
        private final Long classGroupId;
        private final long studentUserId;
        private final String studentName;
        private final String studentDisplayLabel;
        private final String studentEmail;
        private final String stateValue;
        private final String stateLabel;
        private final String stateBadgeClass;
        private final String periodLabel;
        private final String startDateValue;
        private final String endDateValue;
        private final boolean active;
        private final boolean pending;
        private final String updateAction;
        private final String deleteAction;
        private final String downloadHref;

        private EnrollmentOverviewView(
                int contextOrder,
                String contextType,
                String contextLabel,
                String contextDetail,
                String contextKey,
                long contextId,
                Long parentContextId,
                Long courseId,
                Long subjectId,
                Long classGroupId,
                long studentUserId,
                String studentName,
                String studentDisplayLabel,
                String studentEmail,
                String stateValue,
                String stateLabel,
                String stateBadgeClass,
                String periodLabel,
                String startDateValue,
                String endDateValue,
                boolean active,
                boolean pending,
                String updateAction,
                String deleteAction,
                String downloadHref
        ) {
            this.contextOrder = contextOrder;
            this.contextType = contextType;
            this.contextLabel = contextLabel;
            this.contextDetail = contextDetail;
            this.contextKey = contextKey;
            this.contextId = contextId;
            this.parentContextId = parentContextId;
            this.courseId = courseId;
            this.subjectId = subjectId;
            this.classGroupId = classGroupId;
            this.studentUserId = studentUserId;
            this.studentName = studentName;
            this.studentDisplayLabel = studentDisplayLabel;
            this.studentEmail = studentEmail;
            this.stateValue = stateValue;
            this.stateLabel = stateLabel;
            this.stateBadgeClass = stateBadgeClass;
            this.periodLabel = periodLabel;
            this.startDateValue = startDateValue;
            this.endDateValue = endDateValue;
            this.active = active;
            this.pending = pending;
            this.updateAction = updateAction;
            this.deleteAction = deleteAction;
            this.downloadHref = downloadHref;
        }

        public int getContextOrder() {
            return contextOrder;
        }

        public String getContextType() {
            return contextType;
        }

        public String getContextLabel() {
            return contextLabel;
        }

        public String getContextDetail() {
            return contextDetail;
        }

        public String getContextKey() {
            return contextKey;
        }

        public long getContextId() {
            return contextId;
        }

        public Long getParentContextId() {
            return parentContextId;
        }

        public Long getCourseId() {
            return courseId;
        }

        public Long getSubjectId() {
            return subjectId;
        }

        public Long getClassGroupId() {
            return classGroupId;
        }

        public long getStudentUserId() {
            return studentUserId;
        }

        public String getStudentName() {
            return studentName;
        }

        public String getStudentDisplayLabel() {
            return studentDisplayLabel;
        }

        public String getStudentEmail() {
            return studentEmail;
        }

        public String getStateValue() {
            return stateValue;
        }

        public String getStateLabel() {
            return stateLabel;
        }

        public String getStateBadgeClass() {
            return stateBadgeClass;
        }

        public String getPeriodLabel() {
            return periodLabel;
        }

        public String getStartDateValue() {
            return startDateValue;
        }

        public String getEndDateValue() {
            return endDateValue;
        }

        public String getStartDateSort() {
            return startDateValue == null || startDateValue.isBlank() ? "9999-12-31" : startDateValue;
        }

        public boolean isActive() {
            return active;
        }

        public boolean isPending() {
            return pending;
        }

        public String getUpdateAction() {
            return updateAction;
        }

        public String getDeleteAction() {
            return deleteAction;
        }

        public String getDownloadHref() {
            return downloadHref;
        }

        public boolean isCourse() {
            return "course".equals(contextKey);
        }

        public boolean isSubject() {
            return "subject".equals(contextKey);
        }

        public boolean isClassGroup() {
            return "class-group".equals(contextKey);
        }

        public boolean isAssessment() {
            return "assessment".equals(contextKey);
        }

        public boolean isExpandable() {
            return !isAssessment();
        }

        public boolean isEditable() {
            return isCourse() || isSubject();
        }

        public String getModalKey() {
            return contextKey.replace("-", "") + contextId + "_"
                    + (parentContextId == null ? 0 : parentContextId) + "_"
                    + (classGroupId == null ? 0 : classGroupId) + "_"
                    + studentUserId;
        }
    }

    public static final class EnrollmentStateSummaryView {
        private final int count;
        private final String label;
        private final String badgeClass;

        private EnrollmentStateSummaryView(int count, String label, String badgeClass) {
            this.count = count;
            this.label = label;
            this.badgeClass = badgeClass;
        }

        public int getCount() {
            return count;
        }

        public String getLabel() {
            return label;
        }

        public String getBadgeClass() {
            return badgeClass;
        }
    }

    public static final class EnrollmentStudentGroupView {
        private final List<EnrollmentOverviewView> enrollments;
        private final List<EnrollmentCourseGroupView> courseGroups;
        private final List<EnrollmentStateSummaryView> courseStateSummaries;

        private EnrollmentStudentGroupView(List<EnrollmentOverviewView> enrollments) {
            this.enrollments = enrollments;
            this.courseGroups = courseGroups(enrollments);
            this.courseStateSummaries = stateSummaries(this.courseGroups.stream()
                    .map(EnrollmentCourseGroupView::getEnrollment)
                    .toList());
        }

        public long getStudentUserId() {
            return primaryEnrollment().getStudentUserId();
        }

        public String getStudentName() {
            return primaryEnrollment().getStudentName();
        }

        public String getStudentDisplayLabel() {
            return primaryEnrollment().getStudentDisplayLabel();
        }

        public String getStudentEmail() {
            return primaryEnrollment().getStudentEmail();
        }

        public int getEnrollmentCount() {
            return enrollments.size();
        }

        public int getCourseEnrollmentCount() {
            return courseGroups.size();
        }

        public EnrollmentOverviewView getPrimaryEnrollment() {
            return primaryEnrollment();
        }

        public List<EnrollmentOverviewView> getEnrollments() {
            return enrollments;
        }

        public List<EnrollmentCourseGroupView> getCourseGroups() {
            return courseGroups;
        }

        public List<EnrollmentStateSummaryView> getCourseStateSummaries() {
            return courseStateSummaries;
        }

        private EnrollmentOverviewView primaryEnrollment() {
            return enrollments.get(enrollments.size() - 1);
        }

        private static List<EnrollmentCourseGroupView> courseGroups(List<EnrollmentOverviewView> enrollments) {
            List<EnrollmentCourseGroupView> groups = new ArrayList<>();
            enrollments.stream()
                    .filter(EnrollmentOverviewView::isCourse)
                    .sorted(enrollmentComparator())
                    .forEach(course -> groups.add(new EnrollmentCourseGroupView(course, enrollments)));
            return List.copyOf(groups);
        }
    }

    public static final class EnrollmentCourseGroupView {
        private final EnrollmentOverviewView enrollment;
        private final List<EnrollmentSubjectGroupView> subjectGroups;
        private final List<EnrollmentStateSummaryView> subjectStateSummaries;

        private EnrollmentCourseGroupView(EnrollmentOverviewView enrollment, List<EnrollmentOverviewView> allEnrollments) {
            this.enrollment = enrollment;
            this.subjectGroups = subjectGroups(enrollment, allEnrollments);
            this.subjectStateSummaries = stateSummaries(this.subjectGroups.stream()
                    .map(EnrollmentSubjectGroupView::getEnrollment)
                    .toList());
        }

        public EnrollmentOverviewView getEnrollment() {
            return enrollment;
        }

        public List<EnrollmentSubjectGroupView> getSubjectGroups() {
            return subjectGroups;
        }

        public int getSubjectEnrollmentCount() {
            return subjectGroups.size();
        }

        public List<EnrollmentStateSummaryView> getSubjectStateSummaries() {
            return subjectStateSummaries;
        }

        private static List<EnrollmentSubjectGroupView> subjectGroups(
                EnrollmentOverviewView course,
                List<EnrollmentOverviewView> allEnrollments
        ) {
            List<EnrollmentSubjectGroupView> groups = new ArrayList<>();
            allEnrollments.stream()
                    .filter(EnrollmentOverviewView::isSubject)
                    .filter(subject -> course.getCourseId() != null && course.getCourseId().equals(subject.getCourseId()))
                    .sorted(enrollmentComparator())
                    .forEach(subject -> groups.add(new EnrollmentSubjectGroupView(subject, allEnrollments)));
            return List.copyOf(groups);
        }
    }

    public static final class EnrollmentSubjectGroupView {
        private final EnrollmentOverviewView enrollment;
        private final List<EnrollmentClassGroupView> classGroups;
        private final List<EnrollmentStateSummaryView> classGroupStateSummaries;

        private EnrollmentSubjectGroupView(EnrollmentOverviewView enrollment, List<EnrollmentOverviewView> allEnrollments) {
            this.enrollment = enrollment;
            this.classGroups = classGroups(enrollment, allEnrollments);
            this.classGroupStateSummaries = stateSummaries(this.classGroups.stream()
                    .map(EnrollmentClassGroupView::getEnrollment)
                    .toList());
        }

        public EnrollmentOverviewView getEnrollment() {
            return enrollment;
        }

        public List<EnrollmentClassGroupView> getClassGroups() {
            return classGroups;
        }

        public int getClassGroupEnrollmentCount() {
            return classGroups.size();
        }

        public List<EnrollmentStateSummaryView> getClassGroupStateSummaries() {
            return classGroupStateSummaries;
        }

        private static List<EnrollmentClassGroupView> classGroups(
                EnrollmentOverviewView subject,
                List<EnrollmentOverviewView> allEnrollments
        ) {
            List<EnrollmentClassGroupView> groups = new ArrayList<>();
            allEnrollments.stream()
                    .filter(EnrollmentOverviewView::isClassGroup)
                    .filter(classGroup -> subject.getCourseId() != null
                            && subject.getCourseId().equals(classGroup.getCourseId())
                            && subject.getSubjectId() != null
                            && subject.getSubjectId().equals(classGroup.getSubjectId()))
                    .sorted(enrollmentComparator())
                    .forEach(classGroup -> groups.add(new EnrollmentClassGroupView(classGroup, allEnrollments)));
            return List.copyOf(groups);
        }
    }

    public static final class EnrollmentClassGroupView {
        private final EnrollmentOverviewView enrollment;
        private final List<EnrollmentOverviewView> assessmentEnrollments;
        private final List<EnrollmentStateSummaryView> assessmentStateSummaries;

        private EnrollmentClassGroupView(EnrollmentOverviewView enrollment, List<EnrollmentOverviewView> allEnrollments) {
            this.enrollment = enrollment;
            this.assessmentEnrollments = assessmentEnrollments(enrollment, allEnrollments);
            this.assessmentStateSummaries = stateSummaries(this.assessmentEnrollments);
        }

        public EnrollmentOverviewView getEnrollment() {
            return enrollment;
        }

        public List<EnrollmentOverviewView> getAssessmentEnrollments() {
            return assessmentEnrollments;
        }

        public int getAssessmentEnrollmentCount() {
            return assessmentEnrollments.size();
        }

        public List<EnrollmentStateSummaryView> getAssessmentStateSummaries() {
            return assessmentStateSummaries;
        }

        private static List<EnrollmentOverviewView> assessmentEnrollments(
                EnrollmentOverviewView classGroup,
                List<EnrollmentOverviewView> allEnrollments
        ) {
            return allEnrollments.stream()
                    .filter(EnrollmentOverviewView::isAssessment)
                    .filter(assessment -> classGroup.getClassGroupId() != null
                            && classGroup.getClassGroupId().equals(assessment.getClassGroupId()))
                    .sorted(enrollmentComparator())
                    .toList();
        }
    }

    private static Comparator<EnrollmentOverviewView> enrollmentComparator() {
        return Comparator
                .comparing(EnrollmentOverviewView::getStartDateSort)
                .thenComparingInt(EnrollmentOverviewView::getContextOrder)
                .thenComparing(EnrollmentOverviewView::getContextLabel, String.CASE_INSENSITIVE_ORDER);
    }
}
