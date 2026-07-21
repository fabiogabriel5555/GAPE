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
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.learning.model.AbsenceJustification;
import pt.isel.gape.learning.model.AbsenceJustificationCreateCommand;
import pt.isel.gape.learning.model.AbsenceJustificationProcessCommand;
import pt.isel.gape.learning.model.AbsenceJustificationState;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AssessmentEnrollment;
import pt.isel.gape.learning.model.Attempt;
import pt.isel.gape.learning.model.AttendanceRecord;
import pt.isel.gape.learning.model.AttendanceRecordCommand;
import pt.isel.gape.learning.model.AttendanceSource;
import pt.isel.gape.learning.model.AttendanceState;
import pt.isel.gape.learning.model.AttendanceStatus;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupEnrollment;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.CourseOccurrence;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.service.AbsenceJustificationService;
import pt.isel.gape.learning.service.AttendanceRecordService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.service.ApplicationReadService;
import pt.isel.gape.web.view.AbsenceJustificationView;
import pt.isel.gape.web.view.AttendanceRecordView;
import pt.isel.gape.web.view.ClassGroupView;
import pt.isel.gape.web.view.LessonView;
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
        "/learning/lessons/attendance",
        "/learning/lessons/attendance/*",
        "/student/attendance",
        "/student/attendance/*"
})
public final class AttendanceManagementServlet extends DashboardServletSupport {

    private static final String LEARNING_ATTENDANCE_JSP = "/WEB-INF/views/learning/attendance.jsp";
    private static final String ADMIN_ATTENDANCE_JSP = "/admin/admin/attendance/admin-attendance.jsp";
    private static final String COORDINATOR_ATTENDANCE_JSP = "/coordinator/coordinator/attendance/coordinator-attendance.jsp";
    private static final String INSTRUCTOR_ATTENDANCE_JSP = "/instructor/instructor/attendance/instructor-attendance.jsp";
    private static final String STUDENT_ATTENDANCE_JSP = "/student/student/attendance/student-attendance.jsp";
    private static final DateTimeFormatter INPUT_DATE_TIME = ApplicationDateTimeFormat.TECHNICAL_DATE_TIME;
    private static final DateTimeFormatter SHORT_PERIOD_DATE = DateTimeFormatter.ofPattern("dd-MM-yy");
    private static final int ATTENDANCE_MANAGEMENT_PAGE_SIZE = 10;
    private static final int ENROLLMENT_MANAGEMENT_PAGE_SIZE = 10;

    private static String shortPeriod(LocalDate start, LocalDate end) {
        String startLabel = start == null ? "-" : SHORT_PERIOD_DATE.format(start);
        String endLabel = end == null ? "-" : SHORT_PERIOD_DATE.format(end);
        return startLabel + " to " + endLabel;
    }

    private String managementAttendanceJsp(HttpServletRequest request) {
        return switch (primaryProfile(requireCurrentUser(request))) {
            case COORDINATOR -> COORDINATOR_ATTENDANCE_JSP;
            case TEACHER -> INSTRUCTOR_ATTENDANCE_JSP;
            default -> ADMIN_ATTENDANCE_JSP;
        };
    }

    private final AttendanceRecordService attendanceRecordService;
    private final AbsenceJustificationService justificationService;
    private final ScheduleAttendanceViewFactory viewFactory;
    private final GradeCertificateServlet gradeCertificateServlet;
    private final JustificationAttachmentStorage attachmentStorage;
    private final ApplicationReadService.Enrollments enrollmentDAO;
    private final ApplicationReadService.CourseOccurrences courseOccurrenceDAO;
    private final ApplicationReadService.ClassGroupEnrollments classGroupEnrollmentDAO;
    private final ApplicationReadService.AssessmentEnrollments assessmentEnrollmentDAO;
    private final ApplicationReadService.Courses courseDAO;
    private final ApplicationReadService.Subjects subjectDAO;
    private final ApplicationReadService.ClassGroups classGroupDAO;
    private final ApplicationReadService.Assessments assessmentDAO;
    private final ApplicationReadService.Attempts attemptDAO;
    private final ApplicationReadService.Users userDAO;
    private final Clock clock;

    public AttendanceManagementServlet() {
        this(ConnectionProvider.defaultProvider(), ApplicationClock.system());
    }

    private AttendanceManagementServlet(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new ApplicationReadService(connectionProvider),
                new AttendanceRecordService(connectionProvider, clock),
                new AbsenceJustificationService(connectionProvider, clock),
                new GradeCertificateServlet(),
                new JustificationAttachmentStorage(),
                clock
        );
    }

    AttendanceManagementServlet(
            ApplicationReadService readService,
            AttendanceRecordService attendanceRecordService,
            AbsenceJustificationService justificationService,
            GradeCertificateServlet gradeCertificateServlet,
            JustificationAttachmentStorage attachmentStorage,
            Clock clock
    ) {
        this.attendanceRecordService = attendanceRecordService;
        this.justificationService = justificationService;
        this.viewFactory = new ScheduleAttendanceViewFactory(
                readService.classGroups(),
                readService.lessons(),
                readService.users(),
                new LearningViewFactory(
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
                )
        );
        this.gradeCertificateServlet = gradeCertificateServlet;
        this.attachmentStorage = attachmentStorage;
        this.enrollmentDAO = readService.enrollments();
        this.courseOccurrenceDAO = readService.courseOccurrences();
        this.classGroupEnrollmentDAO = readService.classGroupEnrollments();
        this.assessmentEnrollmentDAO = readService.assessmentEnrollments();
        this.courseDAO = readService.courses();
        this.subjectDAO = readService.subjects();
        this.classGroupDAO = readService.classGroups();
        this.assessmentDAO = readService.assessments();
        this.attemptDAO = readService.attempts();
        this.userDAO = readService.users();
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
        boolean lessonsAttendance = isLessonsAttendanceRequest(request);
        String attendanceBasePath = lessonsAttendance ? "/learning/lessons/attendance" : "/learning/attendance";
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
        Map<Long, AbsenceJustificationView> allJustificationByRecordId = new LinkedHashMap<>();
        for (AbsenceJustificationView justification : allJustificationViews) {
            allJustificationByRecordId.putIfAbsent(justification.getAttendanceRecordId(), justification);
        }

        LocalDateTime now = currentMinute();
        String selectedAttendanceState = normalizedFilter(request, "attendanceState");
        List<AttendanceRecordView> attendanceViews = allAttendanceViews.stream()
                .filter(record -> isFinishedActivity(record, now))
                .toList();
        Map<Long, AbsenceJustificationView> justificationByRecordId = new LinkedHashMap<>();
        for (AttendanceRecordView record : attendanceViews) {
            AbsenceJustificationView justification = allJustificationByRecordId.get(record.getId());
            if (justification != null) {
                justificationByRecordId.put(record.getId(), justification);
            }
        }
        List<AttendanceActivityView> allActivities = new ArrayList<>(attendanceActivities(
                attendanceViews,
                justificationByRecordId
        ));
        try {
            allActivities.addAll(assessmentActivities(now));
        } catch (SQLException exception) {
            throw new ServletException("Failed to load assessment attendance activities", exception);
        }
        List<AttendanceActivityView> attendanceActivities = allActivities.stream()
                .filter(activity -> selectedAttendanceState == null
                        || selectedAttendanceState.equals(activity.getStateValue()))
                .sorted(Comparator.comparing(
                                AttendanceActivityView::getSortDateRaw,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Comparator.comparingLong(AttendanceActivityView::getId).reversed()))
                .toList();
        List<AbsenceJustificationView> justificationViews = attendanceActivities.stream()
                .map(AttendanceActivityView::getJustification)
                .filter(java.util.Objects::nonNull)
                .toList();

        request.setAttribute("attendanceRecords", attendanceActivities.stream()
                .map(AttendanceActivityView::getRecord)
                .filter(java.util.Objects::nonNull)
                .toList());
        request.setAttribute("attendanceActivities", attendanceActivities);
        request.setAttribute("attendanceStudentGroups", attendanceStudentGroups(attendanceActivities));
        request.setAttribute("justifications", justificationViews);
        request.setAttribute("attendanceCount", attendanceActivities.size());
        request.setAttribute("absenceCount", attendanceActivities.stream()
                .filter(activity -> "absent".equals(activity.getStateValue())
                        || "requested".equals(activity.getStateValue())
                        || "rejected".equals(activity.getStateValue()))
                .count());
        request.setAttribute("lateOrPartialCount", attendanceActivities.stream()
                .filter(activity -> "requested".equals(activity.getStateValue()))
                .count());
        request.setAttribute("pendingJustificationCount", allJustificationViews.stream()
                .filter(AbsenceJustificationView::isSubmitted)
                .count());
        request.setAttribute("selectedAttendanceState", selectedAttendanceState);
        request.setAttribute("attendanceStateOptions", attendanceStateOptions(selectedAttendanceState));
        request.setAttribute("attendanceCreateStateOptions", attendanceCreationStateOptions());
        request.setAttribute("attendanceCreateSourceOptions", attendanceCreationSourceOptions());
        request.setAttribute("defaultCheckIn", INPUT_DATE_TIME.format(currentMinute()));
        request.setAttribute("attendanceOnly", lessonsAttendance);
        request.setAttribute("attendanceBasePath", attendanceBasePath);
        request.setAttribute("attendanceFilterPath", attendanceBasePath);
        request.setAttribute("attendanceActionPath", attendanceBasePath);
        request.setAttribute("attendanceReturnTo", attendanceBasePath + "#attendance");
        applyAttendanceManagementPagination(request, attendanceActivities, selectedAttendanceState);
        try {
            populateEnrollmentOverviewAttributes(request);
            gradeCertificateServlet.populateManagementAttributes(request, actor, profile, request.getRemoteAddr());
            prepareDashboard(
                    request,
                    lessonsAttendance ? "lessons" : "attendance",
                    lessonsAttendance ? "Attendance" : "Enrollments & Certificates"
            );
            forward(request, response, managementAttendanceJsp(request));
        } catch (SQLException exception) {
            throw new ServletException("Failed to load attendance management", exception);
        }
    }

    void populateLearningAttendanceAttributes(
            HttpServletRequest request,
            SessionUser actor,
            AccessProfileType profile,
            String attendanceFilterPath,
            String attendanceActionPath,
            String attendanceReturnTo
    ) throws ServletException {
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
        Map<Long, AbsenceJustificationView> allJustificationByRecordId = new LinkedHashMap<>();
        for (AbsenceJustificationView justification : allJustificationViews) {
            allJustificationByRecordId.putIfAbsent(justification.getAttendanceRecordId(), justification);
        }

        LocalDateTime now = currentMinute();
        String selectedAttendanceState = normalizedFilter(request, "attendanceState");
        List<AttendanceRecordView> attendanceViews = allAttendanceViews.stream()
                .filter(record -> isFinishedActivity(record, now))
                .toList();
        Map<Long, AbsenceJustificationView> justificationByRecordId = new LinkedHashMap<>();
        for (AttendanceRecordView record : attendanceViews) {
            AbsenceJustificationView justification = allJustificationByRecordId.get(record.getId());
            if (justification != null) {
                justificationByRecordId.put(record.getId(), justification);
            }
        }
        List<AttendanceActivityView> allActivities = new ArrayList<>(attendanceActivities(
                attendanceViews,
                justificationByRecordId
        ));
        try {
            allActivities.addAll(assessmentActivities(now));
        } catch (SQLException exception) {
            throw new ServletException("Failed to load assessment attendance activities", exception);
        }
        List<AttendanceActivityView> attendanceActivities = allActivities.stream()
                .filter(activity -> selectedAttendanceState == null
                        || selectedAttendanceState.equals(activity.getStateValue()))
                .toList();
        List<AbsenceJustificationView> justificationViews = attendanceActivities.stream()
                .map(AttendanceActivityView::getJustification)
                .filter(java.util.Objects::nonNull)
                .toList();

        request.setAttribute("attendanceRecords", attendanceActivities.stream()
                .map(AttendanceActivityView::getRecord)
                .filter(java.util.Objects::nonNull)
                .toList());
        request.setAttribute("attendanceActivities", attendanceActivities);
        request.setAttribute("attendanceStudentGroups", attendanceStudentGroups(attendanceActivities));
        request.setAttribute("justifications", justificationViews);
        request.setAttribute("attendanceCount", attendanceActivities.size());
        request.setAttribute("absenceCount", attendanceActivities.stream()
                .filter(activity -> "absent".equals(activity.getStateValue())
                        || "requested".equals(activity.getStateValue())
                        || "rejected".equals(activity.getStateValue()))
                .count());
        request.setAttribute("lateOrPartialCount", attendanceActivities.stream()
                .filter(activity -> "requested".equals(activity.getStateValue()))
                .count());
        request.setAttribute("pendingJustificationCount", allJustificationViews.stream()
                .filter(AbsenceJustificationView::isSubmitted)
                .count());
        request.setAttribute("selectedAttendanceState", selectedAttendanceState);
        request.setAttribute("attendanceStateOptions", attendanceStateOptions(selectedAttendanceState));
        request.setAttribute("attendanceCreateStateOptions", attendanceCreationStateOptions());
        request.setAttribute("attendanceCreateSourceOptions", attendanceCreationSourceOptions());
        request.setAttribute("defaultCheckIn", INPUT_DATE_TIME.format(currentMinute()));
        request.setAttribute("attendanceFilterPath", attendanceFilterPath);
        request.setAttribute("attendanceActionPath", attendanceActionPath);
        request.setAttribute("attendanceBasePath", attendanceActionPath);
        request.setAttribute("attendanceReturnTo", attendanceReturnTo);
        applyAttendanceManagementPagination(request, attendanceActivities, selectedAttendanceState);
        if (request.getAttribute("classGroupById") == null) {
            try {
                request.setAttribute("classGroupById", classGroupById());
            } catch (SQLException exception) {
                throw new ServletException("Failed to load attendance class groups", exception);
            }
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
            /*
             * The student enrollment archive must be built from the student's
             * own rows, not from the active course catalog.  The catalog
             * intentionally hides inactive courses, which used to make their
             * Inactive/Withdrawn enrollments disappear from Completed
             * Enrollments as well.
             */
            List<CourseEnrollment> ownCourseEnrollments = enrollmentDAO.findCourseEnrollmentsByStudent(actor.userId());
            Map<Long, Course> coursesById = courseDAO.findByIds(ownCourseEnrollments.stream()
                            .map(CourseEnrollment::courseId)
                            .collect(Collectors.toSet()))
                    .stream()
                    .collect(Collectors.toMap(Course::id, course -> course));
            Map<Long, CourseOccurrence> occurrencesById = courseOccurrenceDAO.findAll().stream()
                    .collect(Collectors.toMap(CourseOccurrence::id, occurrence -> occurrence));
            List<StudentCourseEnrollmentView> studentCourseEnrollments = ownCourseEnrollments.stream()
                    .sorted(Comparator.comparing(CourseEnrollment::startDate))
                    .map(enrollment -> new StudentCourseEnrollmentView(
                            enrollment,
                            coursesById.get(enrollment.courseId()),
                            occurrencesById.get(enrollment.courseOccurrenceId())
                    ))
                    .toList();
            request.setAttribute("studentCourseEnrollments", studentCourseEnrollments);
            request.setAttribute("studentActiveCourseEnrollments", studentCourseEnrollments.stream()
                    .filter(enrollment -> enrollment.getState() != EnrollmentState.INACTIVE
                            && enrollment.getState() != EnrollmentState.COMPLETED)
                    .toList());
            request.setAttribute("studentCompletedCourseEnrollments", studentCourseEnrollments.stream()
                    .filter(enrollment -> enrollment.getState() == EnrollmentState.COMPLETED
                            || enrollment.getState() == EnrollmentState.INACTIVE)
                    .toList());
            Map<Long, ClassGroupView> studentClassGroupsById = viewFactory.classGroupViews(classGroupDAO.findAll())
                    .stream().collect(Collectors.toMap(ClassGroupView::getId, java.util.function.Function.identity()));
            List<StudentClassEnrollmentView> allStudentClassEnrollments = classGroupEnrollmentDAO.findByStudent(actor.userId())
                    .stream()
                    .map(enrollment -> new StudentClassEnrollmentView(enrollment, studentClassGroupsById.get(enrollment.classGroupId())))
                    .sorted(Comparator.comparing(StudentClassEnrollmentView::getOccurrenceLabel, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).reversed())
                    .toList();
            List<StudentClassEnrollmentView> studentClassEnrollments = allStudentClassEnrollments.stream()
                    .filter(enrollment -> enrollment.getState() != EnrollmentState.INACTIVE
                            && enrollment.getState() != EnrollmentState.COMPLETED)
                    .toList();
            List<StudentClassEnrollmentView> studentCompletedClassEnrollments = allStudentClassEnrollments.stream()
                    .filter(enrollment -> enrollment.getState() == EnrollmentState.INACTIVE
                            || enrollment.getState() == EnrollmentState.COMPLETED)
                    .toList();
            request.setAttribute("studentClassEnrollments", studentClassEnrollments);
            request.setAttribute("studentActiveClassEnrollments", studentClassEnrollments);
            request.setAttribute("studentCompletedClassEnrollments", studentCompletedClassEnrollments);
            request.setAttribute("studentClassEnrollmentGroups", enrollmentGroups(studentClassEnrollments, List.of()));
            request.setAttribute("studentCompletedClassEnrollmentGroups", enrollmentGroups(studentCompletedClassEnrollments, List.of()));
            List<StudentAssessmentEnrollmentView> allStudentAssessmentEnrollments = new ArrayList<>();
            for (Assessment assessment : assessmentDAO.findAll()) {
                assessmentEnrollmentDAO.findEnrollment(actor.userId(), assessment.id()).ifPresent(enrollment -> {
                    try {
                        List<Long> applicable = assessmentDAO.findApplicableClassGroupIds(assessment.id());
                        if (applicable.isEmpty()) {
                            allStudentAssessmentEnrollments.add(new StudentAssessmentEnrollmentView(assessment, enrollment.state(), null));
                        } else {
                            for (Long classGroupId : applicable) {
                                allStudentAssessmentEnrollments.add(new StudentAssessmentEnrollmentView(
                                        assessment,
                                        enrollment.state(),
                                        studentClassGroupsById.get(classGroupId)
                                ));
                            }
                        }
                    } catch (SQLException exception) {
                        throw new IllegalStateException("Failed to load assessment enrollment context", exception);
                    }
                });
            }
            List<StudentAssessmentEnrollmentView> studentAssessmentEnrollments = allStudentAssessmentEnrollments.stream()
                    .filter(enrollment -> enrollment.getState() != EnrollmentState.INACTIVE
                            && enrollment.getState() != EnrollmentState.COMPLETED)
                    .toList();
            List<StudentAssessmentEnrollmentView> studentCompletedAssessmentEnrollments = allStudentAssessmentEnrollments.stream()
                    .filter(enrollment -> enrollment.getState() == EnrollmentState.INACTIVE
                            || enrollment.getState() == EnrollmentState.COMPLETED)
                    .toList();
            request.setAttribute("studentAssessmentEnrollments", studentAssessmentEnrollments);
            request.setAttribute("studentActiveAssessmentEnrollments", studentAssessmentEnrollments);
            request.setAttribute("studentCompletedAssessmentEnrollments", studentCompletedAssessmentEnrollments);
            request.setAttribute("studentAssessmentEnrollmentGroups", enrollmentGroups(List.of(), studentAssessmentEnrollments));
            request.setAttribute("studentCompletedAssessmentEnrollmentGroups", enrollmentGroups(List.of(), studentCompletedAssessmentEnrollments));
            List<StudentCompletedEnrollmentCourseGroupView> completedEnrollmentGroups = completedEnrollmentGroups(
                    studentCourseEnrollments,
                    allStudentClassEnrollments,
                    allStudentAssessmentEnrollments
            );
            request.setAttribute("studentCompletedEnrollmentGroups", completedEnrollmentGroups);
            request.setAttribute("studentCompletedEnrollmentCount", studentCourseEnrollments.stream()
                    .filter(enrollment -> enrollment.getState() == EnrollmentState.COMPLETED
                            || enrollment.getState() == EnrollmentState.INACTIVE)
                    .count() + studentCompletedClassEnrollments.size()
                    + studentCompletedAssessmentEnrollments.size());
            gradeCertificateServlet.populateStudentAttributes(request, actor);
            prepareDashboard(request, "attendance", "Enrollments & Certificates");
            forward(request, response, STUDENT_ATTENDANCE_JSP);
        } catch (SQLException exception) {
            throw new ServletException("Failed to load student grades and certificates", exception);
        }
    }

    private void populateEnrollmentOverviewAttributes(HttpServletRequest request) throws SQLException {
        Map<Long, Course> courses = mapCourses(courseDAO.findCatalogCourses(null, null, null));
        Map<Long, CourseOccurrence> courseOccurrences = courseOccurrencesById();
        Map<Long, Subject> subjects = mapSubjects(subjectDAO.findAll());
        Map<Long, ClassGroup> classGroups = mapClassGroups(classGroupDAO.findAll());
        Map<Long, Assessment> assessments = mapAssessments(assessmentDAO.findAll());
        Map<Long, User> users = mapUsers(userDAO.findAll());

        List<EnrollmentOverviewView> rows = new ArrayList<>();
        Set<String> studentClassGroupKeys = new HashSet<>();
        for (Course course : courses.values()) {
            for (CourseEnrollment enrollment : enrollmentDAO.findCourseEnrollmentsByCourse(course.id())) {
                CourseOccurrence occurrence = courseOccurrences.get(enrollment.courseOccurrenceId());
                rows.add(enrollmentRow(
                        1,
                        "Course",
                        courseOccurrenceLabel(occurrence, course),
                        occurrencePeriodLabel(occurrence, enrollment.startDate(), enrollment.endDate()),
                        "course",
                        course.id(),
                        null,
                        course.id(),
                        occurrence == null ? null : occurrence.id(),
                        null,
                        null,
                        enrollment.studentUserId(),
                        users,
                        enrollment.state(),
                        occurrenceStartDate(occurrence, enrollment.startDate()),
                        occurrenceEndDate(occurrence, enrollment.endDate()),
                        "/admin/courses/" + course.id() + "/enrollments/" + enrollment.studentUserId() + "/update",
                        "/admin/courses/" + course.id() + "/enrollments/" + enrollment.studentUserId() + "/delete"
                ));
            }
        }
        for (ClassGroup classGroup : classGroups.values()) {
            for (ClassGroupEnrollment enrollment : classGroupEnrollmentDAO.findByClassGroup(classGroup.id())) {
                CourseOccurrence occurrence = courseOccurrences.get(classGroup.courseOccurrenceId());
                studentClassGroupKeys.add(enrollmentKey(enrollment.studentUserId(), classGroup.id()));
                rows.add(enrollmentRow(
                        3,
                        "Class Group",
                        classGroup.code(),
                        classGroupContextLabel(classGroup, subjects, courses, occurrence),
                        "class-group",
                        classGroup.id(),
                        classGroup.subjectId(),
                        classGroup.courseId(),
                        occurrence == null ? null : occurrence.id(),
                        classGroup.subjectId(),
                        classGroup.id(),
                        enrollment.studentUserId(),
                        users,
                        enrollment.state(),
                        occurrenceStartDate(occurrence, enrollment.startDate()),
                        occurrenceEndDate(occurrence, enrollment.endDate()),
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
                    CourseOccurrence occurrence = courseOccurrences.get(classGroup.courseOccurrenceId());
                    rows.add(enrollmentRow(
                            4,
                            "Assessment",
                            assessment.title(),
                            assessmentContextLabel(assessment, subjects) + " | "
                                    + classGroup.code() + " | " + courseOccurrenceLabel(occurrence, courses.get(classGroup.courseId())),
                            "assessment",
                            assessment.id(),
                            classGroup.id(),
                            classGroup.courseId(),
                            occurrence == null ? null : occurrence.id(),
                            classGroup.subjectId(),
                            classGroup.id(),
                            enrollment.studentUserId(),
                            users,
                            enrollment.state(),
                            occurrenceStartDate(occurrence, null),
                            occurrenceEndDate(occurrence, null),
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

        EnrollmentScopeRows enrollmentScopes = enrollmentScopes(rows);
        List<EnrollmentStudentGroupView> activeEnrollmentGroups = enrollmentGroups(enrollmentScopes.activeRows());
        List<EnrollmentStudentGroupView> completedEnrollmentGroups = enrollmentGroups(enrollmentScopes.completedRows());
        boolean completedScope = "completed".equals(text(request, "enrollmentsScope"));
        OverviewPage<EnrollmentStudentGroupView> enrollmentPage = enrollmentManagementPage(
                request,
                completedScope ? completedEnrollmentGroups : activeEnrollmentGroups
        );

        request.setAttribute("enrollmentRows", rows);
        request.setAttribute("enrollmentGroups", enrollmentPage.rows());
        request.setAttribute("enrollmentCount", rows.size());
        request.setAttribute("activeEnrollmentCount", rows.stream().filter(EnrollmentOverviewView::isActive).count());
        request.setAttribute("pendingEnrollmentCount", rows.stream().filter(EnrollmentOverviewView::isPending).count());
        request.setAttribute("enrollmentManagementScope", completedScope ? "completed" : "active");
        request.setAttribute("enrollmentManagementTotal", enrollmentPage.total());
        // The management list renders one student group per row.  Keep the
        // archive counter at that same granularity; counting the underlying
        // enrollment records made the Completed Enrollments row disagree with
        // the collection it opens and with Class Group Management.
        request.setAttribute("enrollmentManagementCompletedTotal", completedEnrollmentGroups.size());
        request.setAttribute("enrollmentManagementCurrentPage", enrollmentPage.currentPage());
        request.setAttribute("enrollmentManagementPageCount", enrollmentPage.pageCount());
        request.setAttribute("enrollmentManagementHasPreviousPage", enrollmentPage.currentPage() > 1);
        request.setAttribute("enrollmentManagementHasNextPage", enrollmentPage.currentPage() < enrollmentPage.pageCount());
        request.setAttribute("enrollmentManagementPreviousPage", Math.max(1, enrollmentPage.currentPage() - 1));
        request.setAttribute("enrollmentManagementNextPage", Math.min(enrollmentPage.pageCount(), enrollmentPage.currentPage() + 1));
        request.setAttribute("enrollmentManagementLoadAll", enrollmentPage.loadAll());
    }

    private static <T> OverviewPage<T> enrollmentManagementPage(HttpServletRequest request, List<T> items) {
        int total = items.size();
        boolean loadAll = Boolean.parseBoolean(text(request, "enrollmentsLoadAll"));
        int pageCount = Math.max(1, (total + ENROLLMENT_MANAGEMENT_PAGE_SIZE - 1) / ENROLLMENT_MANAGEMENT_PAGE_SIZE);
        int currentPage = loadAll
                ? 1
                : Math.min(pageCount, Math.max(1, integerParameter(request, "enrollmentsPage", 1)));
        int fromIndex = loadAll ? 0 : Math.min((currentPage - 1) * ENROLLMENT_MANAGEMENT_PAGE_SIZE, total);
        int toIndex = loadAll ? total : Math.min(fromIndex + ENROLLMENT_MANAGEMENT_PAGE_SIZE, total);
        return new OverviewPage<>(List.copyOf(items.subList(fromIndex, toIndex)), total, currentPage, pageCount, loadAll);
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
            Long courseOccurrenceId,
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
                courseOccurrenceId,
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

    /**
     * A course-occurrence enrollment is the lifecycle root for its class-group
     * and assessment enrollments.  Keeping the whole occurrence in one scope
     * makes the current and completed views both internally coherent, even
     * when a student has another occurrence with a different state.
     */
    private static EnrollmentScopeRows enrollmentScopes(List<EnrollmentOverviewView> rows) {
        Map<String, Boolean> completedByOccurrence = new LinkedHashMap<>();
        Map<String, Boolean> completedByLegacyCourse = new LinkedHashMap<>();
        rows.stream()
                .filter(EnrollmentOverviewView::isCourse)
                .forEach(row -> {
                    completedByOccurrence.put(row.getOccurrenceScopeKey(), row.isCompleted());
                    if (row.getCourseOccurrenceId() == null) {
                        completedByLegacyCourse.put(row.getCourseScopeKey(), row.isCompleted());
                    }
                });
        List<EnrollmentOverviewView> activeRows = new ArrayList<>();
        List<EnrollmentOverviewView> completedRows = new ArrayList<>();
        for (EnrollmentOverviewView row : rows) {
            Boolean rootCompletion = completedByOccurrence.get(row.getOccurrenceScopeKey());
            if (rootCompletion == null) {
                rootCompletion = completedByLegacyCourse.get(row.getCourseScopeKey());
            }
            boolean completed = rootCompletion == null ? row.isCompleted() : rootCompletion;
            (completed ? completedRows : activeRows).add(row);
        }
        return new EnrollmentScopeRows(List.copyOf(activeRows), List.copyOf(completedRows));
    }

    private static Map<Long, Course> mapCourses(List<Course> values) {
        Map<Long, Course> result = new LinkedHashMap<>();
        for (Course value : values) {
            result.put(value.id(), value);
        }
        return result;
    }

    private Map<Long, CourseOccurrence> courseOccurrencesById() throws SQLException {
        Map<Long, CourseOccurrence> result = new LinkedHashMap<>();
        for (CourseOccurrence occurrence : courseOccurrenceDAO.findAll()) {
            result.put(occurrence.id(), occurrence);
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
            Map<Long, Course> courses,
            CourseOccurrence occurrence
    ) {
        return courseOccurrenceLabel(occurrence, courses.get(classGroup.courseId()))
                + " | " + subjectLabel(subjects.get(classGroup.subjectId()));
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

    private static String courseOccurrenceLabel(CourseOccurrence occurrence, Course course) {
        if (occurrence == null) {
            return courseLabel(course);
        }
        String occurrenceCode = occurrence.code() == null || occurrence.code().isBlank()
                ? "Occurrence #" + occurrence.id()
                : occurrence.code();
        return occurrenceCode + " — " + courseLabel(course);
    }

    private static String occurrencePeriodLabel(
            CourseOccurrence occurrence,
            LocalDate fallbackStart,
            LocalDate fallbackEnd
    ) {
        return periodLabel(occurrenceStartDate(occurrence, fallbackStart), occurrenceEndDate(occurrence, fallbackEnd));
    }

    private static LocalDate occurrenceStartDate(CourseOccurrence occurrence, LocalDate fallback) {
        return occurrence == null || occurrence.startsAt() == null ? fallback : occurrence.startsAt();
    }

    private static LocalDate occurrenceEndDate(CourseOccurrence occurrence, LocalDate fallback) {
        return occurrence == null || occurrence.endsAt() == null ? fallback : occurrence.endsAt();
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
            return "Until " + ApplicationDateTimeFormat.date(endDate);
        }
        if (endDate == null) {
            return "From " + ApplicationDateTimeFormat.date(startDate);
        }
        return ApplicationDateTimeFormat.date(startDate) + " to " + ApplicationDateTimeFormat.date(endDate);
    }

    private static String formatActivityPeriod(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt == null && endsAt == null) {
            return "-";
        }
        if (startsAt == null) {
            return "Until " + formatActivityDateTime(endsAt);
        }
        if (endsAt == null) {
            return "From " + formatActivityDateTime(startsAt);
        }
        return formatActivityDateTime(startsAt) + " to " + formatActivityDateTime(endsAt);
    }

    private static String formatActivityDateTime(LocalDateTime value) {
        return value == null ? "-" : ApplicationDateTimeFormat.dateTime(value);
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
            case INACTIVE -> "bg-danger-50 text-danger-600";
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
        redirectToReturnPath(request, response, learningAttendanceFallback(request));
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
        redirectToReturnPath(request, response, "/student/attendance");
    }

    private void processJustification(
            HttpServletRequest request,
            HttpServletResponse response,
            long justificationId,
            String action
    ) throws IOException {
        SessionUser actor = requireCurrentUser(request);
        boolean ajaxRequest = isAjaxRequest(request);
        try {
            AbsenceJustification processed = justificationService.processJustification(
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
            if (ajaxRequest) {
                writeJustificationProcessResponse(response, processed);
                return;
            }
            flashSuccess(request, "Justification processed.");
        } catch (RuntimeException exception) {
            if (ajaxRequest) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                        "{\"success\":false,\"message\":\"" + jsonEscape(messageFor(exception)) + "\"}");
                return;
            }
            flashError(request, messageFor(exception));
        }
        redirectToReturnPath(request, response, learningAttendanceFallback(request));
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

    private static boolean isLessonsAttendanceRequest(HttpServletRequest request) {
        return "/learning/lessons/attendance".equals(request.getServletPath());
    }

    private static String learningAttendanceFallback(HttpServletRequest request) {
        return isLessonsAttendanceRequest(request) ? "/learning/lessons#attendance" : "/learning/attendance#attendance";
    }

    private static boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    private static void writeJustificationProcessResponse(
            HttpServletResponse response,
            AbsenceJustification justification
    ) throws IOException {
        AttendanceDisplayState displayState = displayStateForJustification(justification.state());
        String payload = "{"
                + "\"success\":true,"
                + "\"activityId\":" + justification.attendanceRecordId() + ","
                + "\"stateValue\":\"" + jsonEscape(displayState.value()) + "\","
                + "\"stateLabel\":\"" + jsonEscape(displayState.label()) + "\","
                + "\"badgeClass\":\"" + jsonEscape(displayState.badgeClass()) + "\","
                + "\"canApprove\":" + canApproveJustification(displayState.value()) + ","
                + "\"canReject\":" + canRejectJustification(displayState.value())
                + "}";
        writeJson(response, HttpServletResponse.SC_OK, payload);
    }

    private static void writeJson(HttpServletResponse response, int status, String payload) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(payload);
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
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
        return escaped.toString();
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
        return value == null ? defaultValue : ApplicationDateTimeFormat.parseUserDateTime(value);
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

    private static boolean isFinishedActivity(AttendanceRecordView record, LocalDateTime now) {
        LocalDateTime endsAt = record.getActivityEndsAtRaw();
        return endsAt == null || !endsAt.isAfter(now);
    }

    private static List<AttendanceActivityView> attendanceActivities(
            List<AttendanceRecordView> records,
            Map<Long, AbsenceJustificationView> justificationByRecordId
    ) {
        return records.stream()
                .map(record -> new AttendanceActivityView(record, justificationByRecordId.get(record.getId())))
                .toList();
    }

    private List<AttendanceActivityView> assessmentActivities(LocalDateTime now) throws SQLException {
        Map<Long, User> users = mapUsers(userDAO.findAll());
        List<AttendanceActivityView> activities = new ArrayList<>();
        for (Assessment assessment : assessmentDAO.findAll()) {
            if (!isFinishedAssessment(assessment, now)) {
                continue;
            }
            List<Long> applicableClassGroupIds = assessmentDAO.findApplicableClassGroupIds(assessment.id());
            Map<Long, Attempt> latestAttemptByStudent = latestAttemptByStudent(attemptDAO.findByAssessment(assessment.id()));
            for (AssessmentEnrollment enrollment : assessmentEnrollmentDAO.findByAssessment(assessment.id())) {
                if (!isRelevantAssessmentEnrollment(enrollment)) {
                    continue;
                }
                activities.add(new AttendanceActivityView(
                        new AssessmentAttendanceSource(
                                assessment,
                                enrollment,
                                users.get(enrollment.studentUserId()),
                                latestAttemptByStudent.get(enrollment.studentUserId()),
                                contextClassGroupId(enrollment.studentUserId(), applicableClassGroupIds)
                        )
                ));
            }
        }
        return List.copyOf(activities);
    }

    private Long contextClassGroupId(long studentUserId, List<Long> applicableClassGroupIds) throws SQLException {
        if (applicableClassGroupIds == null || applicableClassGroupIds.isEmpty()) {
            return null;
        }
        for (Long classGroupId : applicableClassGroupIds) {
            if (classGroupId == null) {
                continue;
            }
            ClassGroupEnrollment enrollment = classGroupEnrollmentDAO
                    .findEnrollment(studentUserId, classGroupId)
                    .orElse(null);
            if (enrollment != null && isRelevantClassGroupEnrollment(enrollment)) {
                return classGroupId;
            }
        }
        return applicableClassGroupIds.get(0);
    }

    private static boolean isRelevantClassGroupEnrollment(ClassGroupEnrollment enrollment) {
        return enrollment.state() == EnrollmentState.ACTIVE || enrollment.state() == EnrollmentState.COMPLETED;
    }

    private static boolean isFinishedAssessment(Assessment assessment, LocalDateTime now) {
        if (assessment.state() == pt.isel.gape.learning.model.AssessmentState.COMPLETED) {
            return true;
        }
        return assessment.availableUntil() != null && !assessment.availableUntil().isAfter(now);
    }

    private static boolean isRelevantAssessmentEnrollment(AssessmentEnrollment enrollment) {
        return enrollment.state() == EnrollmentState.ACTIVE || enrollment.state() == EnrollmentState.COMPLETED;
    }

    private static Map<Long, Attempt> latestAttemptByStudent(List<Attempt> attempts) {
        Map<Long, Attempt> result = new LinkedHashMap<>();
        for (Attempt attempt : attempts) {
            result.putIfAbsent(attempt.studentUserId(), attempt);
        }
        return result;
    }

    private static List<AttendanceStudentGroupView> attendanceStudentGroups(List<AttendanceActivityView> activities) {
        Map<Long, List<AttendanceActivityView>> byStudent = new LinkedHashMap<>();
        for (AttendanceActivityView activity : activities) {
            byStudent.computeIfAbsent(activity.getStudentUserId(), ignored -> new ArrayList<>()).add(activity);
        }
        return byStudent.values().stream()
                .map(group -> new AttendanceStudentGroupView(List.copyOf(group)))
                .sorted(Comparator.comparing(
                                AttendanceStudentGroupView::getSortDateRaw,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AttendanceStudentGroupView::getStudentLabel, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Attendance details and their dialogs are only rendered for the current
     * student page.  This keeps the Lessons and Enrollments dashboards quick
     * even when their audit history contains thousands of activity records.
     */
    private static void applyAttendanceManagementPagination(
            HttpServletRequest request,
            List<AttendanceActivityView> activities,
            String selectedAttendanceState
    ) {
        List<AttendanceStudentGroupView> allGroups = attendanceStudentGroups(activities);
        int total = allGroups.size();
        boolean loadAll = Boolean.parseBoolean(text(request, "attendanceLoadAll"));
        int pageCount = Math.max(1, (total + ATTENDANCE_MANAGEMENT_PAGE_SIZE - 1) / ATTENDANCE_MANAGEMENT_PAGE_SIZE);
        int currentPage = loadAll
                ? 1
                : Math.min(pageCount, Math.max(1, integerParameter(request, "attendancePage", 1)));
        int fromIndex = loadAll ? 0 : Math.min((currentPage - 1) * ATTENDANCE_MANAGEMENT_PAGE_SIZE, total);
        int toIndex = loadAll ? total : Math.min(fromIndex + ATTENDANCE_MANAGEMENT_PAGE_SIZE, total);
        List<AttendanceStudentGroupView> pageGroups = allGroups.subList(fromIndex, toIndex);
        Set<Long> pageStudentIds = pageGroups.stream()
                .map(AttendanceStudentGroupView::getStudentUserId)
                .collect(Collectors.toSet());

        request.setAttribute("attendanceStudentGroups", List.copyOf(pageGroups));
        request.setAttribute("attendanceActivities", activities.stream()
                .filter(activity -> pageStudentIds.contains(activity.getStudentUserId()))
                .toList());
        request.setAttribute("attendanceManagementTotal", total);
        request.setAttribute("attendanceManagementCurrentPage", currentPage);
        request.setAttribute("attendanceManagementPageCount", pageCount);
        request.setAttribute("attendanceManagementHasPreviousPage", currentPage > 1);
        request.setAttribute("attendanceManagementHasNextPage", currentPage < pageCount);
        request.setAttribute("attendanceManagementPreviousPage", Math.max(1, currentPage - 1));
        request.setAttribute("attendanceManagementNextPage", Math.min(pageCount, currentPage + 1));
        request.setAttribute("attendanceManagementLoadAll", loadAll);
        request.setAttribute(
                "attendancePaginationStateQuery",
                selectedAttendanceState == null || selectedAttendanceState.isBlank()
                        ? ""
                        : "&attendanceState=" + selectedAttendanceState
        );
    }

    private static int integerParameter(HttpServletRequest request, String name, int fallback) {
        String value = text(request, name);
        if (value == null) {
            return fallback;
        }
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private Map<Long, ClassGroupView> classGroupById() throws SQLException {
        Map<Long, ClassGroupView> byId = new LinkedHashMap<>();
        for (ClassGroupView classGroup : viewFactory.classGroupViews(classGroupDAO.findAll())) {
            byId.put(classGroup.getId(), classGroup);
        }
        return byId;
    }

    private static List<SelectOptionView> attendanceStateOptions(String selected) {
        return attendanceDisplayStates().stream()
                .map(state -> new SelectOptionView(
                        state.value(),
                        state.label(),
                        state.value().equals(selected)
                ))
                .toList();
    }

    private static List<SelectOptionView> attendanceCreationStateOptions() {
        return Arrays.stream(AttendanceStatus.values())
                .filter(status -> status == AttendanceStatus.PRESENT || status == AttendanceStatus.ABSENT)
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

    private static List<AttendanceDisplayState> attendanceDisplayStates() {
        return List.of(
                new AttendanceDisplayState("present", "Present", "bg-success-50 text-success-600"),
                new AttendanceDisplayState("absent", "Absent", "bg-danger-50 text-danger-600"),
                new AttendanceDisplayState("requested", "Requested", "bg-warning-50 text-warning-600"),
                new AttendanceDisplayState("justified", "Justified", "bg-main-50 text-main-600"),
                new AttendanceDisplayState("rejected", "Rejected", "bg-danger-50 text-danger-600")
        );
    }

    private static AttendanceDisplayState displayStateForJustification(AbsenceJustificationState state) {
        return switch (state) {
            case SUBMITTED, UNDER_REVIEW -> attendanceDisplayStates().get(2);
            case APPROVED -> attendanceDisplayStates().get(3);
            case REJECTED -> attendanceDisplayStates().get(4);
            case CANCELLED -> attendanceDisplayStates().get(1);
        };
    }

    private static boolean canApproveJustification(String activityStateValue) {
        return "requested".equals(activityStateValue) || "rejected".equals(activityStateValue);
    }

    private static boolean canRejectJustification(String activityStateValue) {
        return "requested".equals(activityStateValue) || "justified".equals(activityStateValue);
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

    private record AttendanceDisplayState(String value, String label, String badgeClass) {
    }

    private record OverviewPage<T>(List<T> rows, int total, int currentPage, int pageCount, boolean loadAll) {
    }

    private record EnrollmentScopeRows(
            List<EnrollmentOverviewView> activeRows,
            List<EnrollmentOverviewView> completedRows
    ) {
    }

    private record AssessmentAttendanceSource(
            Assessment assessment,
            AssessmentEnrollment enrollment,
            User student,
            Attempt attempt,
            Long classGroupId
    ) {
    }

    public static final class AttendanceStudentGroupView {

        private final List<AttendanceActivityView> activities;
        private final List<AttendanceStateSummaryView> stateSummaries;
        private final long permanenceMinutes;

        private AttendanceStudentGroupView(List<AttendanceActivityView> activities) {
            this.activities = activities;
            this.stateSummaries = attendanceStateSummaries(activities);
            this.permanenceMinutes = activities.stream()
                    .mapToLong(AttendanceActivityView::getPermanenceMinutes)
                    .sum();
        }

        public long getStudentUserId() {
            return primaryActivity().getStudentUserId();
        }

        public String getStudentLabel() {
            return primaryActivity().getStudentLabel();
        }

        public String getStudentEmail() {
            return primaryActivity().getStudentEmail();
        }

        public int getActivityCount() {
            return activities.size();
        }

        public String getActivityCountLabel() {
            return activities.size() == 1 ? "1 activity" : activities.size() + " activities";
        }

        public String getTimeLabel() {
            return "Shown per activity";
        }

        public String getPermanenceLabel() {
            return "Shown per activity";
        }

        public List<AttendanceStateSummaryView> getStateSummaries() {
            return stateSummaries;
        }

        public List<AttendanceActivityView> getActivities() {
            return activities;
        }

        public LocalDateTime getSortDateRaw() {
            return activities.stream()
                    .map(AttendanceActivityView::getSortDateRaw)
                    .filter(java.util.Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
        }

        public String getSortDateValue() {
            LocalDateTime sortDate = getSortDateRaw();
            return sortDate == null ? "" : sortDate.toString();
        }

        public String getPrimaryStateLabel() {
            return primaryActivity().getStateLabel();
        }

        public String getPrimaryActivityLabel() {
            return primaryActivity().getActivityLabel();
        }

        public Long getClassGroupId() {
            return primaryActivity().getClassGroupId();
        }

        private AttendanceActivityView primaryActivity() {
            return activities.get(0);
        }
    }

    public static final class AttendanceActivityView {

        private final AttendanceRecordView record;
        private final AssessmentAttendanceSource assessmentSource;
        private final AbsenceJustificationView justification;
        private final AttendanceDisplayState state;

        private AttendanceActivityView(AttendanceRecordView record, AbsenceJustificationView justification) {
            this.record = record;
            this.assessmentSource = null;
            this.justification = justification;
            this.state = resolveAttendanceState(record, justification);
        }

        private AttendanceActivityView(AssessmentAttendanceSource assessmentSource) {
            this.record = null;
            this.assessmentSource = assessmentSource;
            this.justification = null;
            this.state = assessmentSource.attempt() == null
                    ? attendanceDisplayStates().get(1)
                    : attendanceDisplayStates().get(0);
        }

        public AttendanceRecordView getRecord() {
            return record;
        }

        public AbsenceJustificationView getJustification() {
            return justification;
        }

        public long getId() {
            return record == null
                    ? -(assessmentSource.assessment().id() * 100000L + assessmentSource.enrollment().studentUserId())
                    : record.getId();
        }

        public long getStudentUserId() {
            return record == null ? assessmentSource.enrollment().studentUserId() : record.getStudentUserId();
        }

        public String getStudentLabel() {
            if (record != null) {
                return record.getStudentLabel();
            }
            return getStudentUserId() + " - " + studentName();
        }

        public String getStudentEmail() {
            if (record != null) {
                return record.getStudentEmail();
            }
            return assessmentSource.student() == null ? "-" : assessmentSource.student().email();
        }

        public String getActivityLabel() {
            return record == null
                    ? "Assessment - " + safeAssessmentTitle()
                    : record.getActivityLabel();
        }

        public String getActivityMeta() {
            return record == null
                    ? "Assessment #" + assessmentSource.assessment().id()
                    : record.getActivityTypeLabel() + " #" + record.getLessonId();
        }

        public String getActivityScheduleLabel() {
            if (record != null) {
                return record.getActivityScheduleLabel();
            }
            return formatActivityPeriod(
                    assessmentSource.assessment().availableFrom(),
                    assessmentSource.assessment().availableUntil()
            );
        }

        public LocalDateTime getSortDateRaw() {
            if (record != null) {
                return record.getActivityEndsAtRaw();
            }
            if (assessmentSource.assessment().availableUntil() != null) {
                return assessmentSource.assessment().availableUntil();
            }
            return assessmentSource.assessment().availableFrom();
        }

        public String getSortDateValue() {
            LocalDateTime sortDate = getSortDateRaw();
            return sortDate == null ? "" : sortDate.toString();
        }

        public Long getClassGroupId() {
            if (record != null) {
                LessonView lesson = record.getLesson();
                return lesson == null ? null : lesson.getClassGroupId();
            }
            return assessmentSource.classGroupId();
        }

        public String getStateValue() {
            return state.value();
        }

        public String getStateLabel() {
            return state.label();
        }

        public String getStateBadgeClass() {
            return state.badgeClass();
        }

        public String getTimeLabel() {
            if (record != null) {
                return record.getTimeLabel();
            }
            Attempt attempt = assessmentSource.attempt();
            if (attempt == null) {
                return "Assessment: " + getActivityScheduleLabel();
            }
            return "Started: " + formatActivityDateTime(attempt.startedAt())
                    + " | Submitted: " + formatActivityDateTime(attempt.submittedAt());
        }

        public String getPermanenceLabel() {
            return AttendanceRecordView.durationLabel(getPermanenceMinutes());
        }

        public long getPermanenceMinutes() {
            if (record != null) {
                return record.getPermanenceMinutes();
            }
            Attempt attempt = assessmentSource.attempt();
            if (attempt == null || attempt.startedAt() == null || attempt.submittedAt() == null) {
                return 0;
            }
            return Math.max(0, ChronoUnit.MINUTES.between(attempt.startedAt(), attempt.submittedAt()));
        }

        public String getSourceLabel() {
            return record == null ? "Assessment" : record.getSourceLabel();
        }

        public String getNotes() {
            if (record != null) {
                return record.getNotes();
            }
            Attempt attempt = assessmentSource.attempt();
            return attempt == null
                    ? "No attempt was recorded before the assessment ended."
                    : "Attempt #" + attempt.attemptNumber() + " - " + attempt.state().toDatabaseValue();
        }

        public boolean isHasJustification() {
            return justification != null;
        }

        public boolean isHasSettings() {
            return justification != null
                    && ("requested".equals(getStateValue())
                    || "justified".equals(getStateValue())
                    || "rejected".equals(getStateValue()));
        }

        public boolean isCanApprove() {
            return canApproveJustification(getStateValue());
        }

        public boolean isCanReject() {
            return canRejectJustification(getStateValue());
        }

        public String getDetailModalId() {
            if (record != null) {
                return "attendanceDetail" + getId();
            }
            return "attendanceAssessmentDetail"
                    + assessmentSource.assessment().id()
                    + "Student"
                    + assessmentSource.enrollment().studentUserId();
        }

        public String getSettingsModalId() {
            return "attendanceSettings" + getId();
        }

        private String studentName() {
            User student = assessmentSource.student();
            return student == null || student.name() == null || student.name().isBlank()
                    ? "Student " + getStudentUserId()
                    : student.name();
        }

        private String safeAssessmentTitle() {
            String title = assessmentSource.assessment().title();
            return title == null || title.isBlank()
                    ? "Assessment " + assessmentSource.assessment().id()
                    : title;
        }
    }

    public static final class AttendanceStateSummaryView {

        private final int count;
        private final String label;
        private final String badgeClass;

        private AttendanceStateSummaryView(int count, String label, String badgeClass) {
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

    private static AttendanceDisplayState resolveAttendanceState(
            AttendanceRecordView record,
            AbsenceJustificationView justification
    ) {
        if (justification != null) {
            return switch (justification.getStateValue()) {
                case "submitted", "under_review" -> attendanceDisplayStates().get(2);
                case "approved" -> attendanceDisplayStates().get(3);
                case "rejected" -> attendanceDisplayStates().get(4);
                default -> baseAttendanceState(record);
            };
        }
        return baseAttendanceState(record);
    }

    private static AttendanceDisplayState baseAttendanceState(AttendanceRecordView record) {
        if ("present".equals(record.getStatusValue())) {
            return attendanceDisplayStates().get(0);
        }
        if ("justified".equals(record.getStatusValue())) {
            return attendanceDisplayStates().get(3);
        }
        return attendanceDisplayStates().get(1);
    }

    private static List<AttendanceStateSummaryView> attendanceStateSummaries(List<AttendanceActivityView> activities) {
        List<AttendanceStateSummaryView> summaries = new ArrayList<>();
        for (AttendanceDisplayState state : attendanceDisplayStates()) {
            int count = 0;
            for (AttendanceActivityView activity : activities) {
                if (state.value().equals(activity.getStateValue())) {
                    count++;
                }
            }
            if (count > 0) {
                summaries.add(new AttendanceStateSummaryView(count, state.label(), state.badgeClass()));
            }
        }
        return List.copyOf(summaries);
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
        private final Long courseOccurrenceId;
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
                Long courseOccurrenceId,
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
            this.courseOccurrenceId = courseOccurrenceId;
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

        public Long getCourseOccurrenceId() {
            return courseOccurrenceId;
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

        public boolean isCompleted() {
            return "completed".equals(stateValue);
        }

        public String getOccurrenceScopeKey() {
            if (courseOccurrenceId != null && courseOccurrenceId > 0) {
                return studentUserId + ":occurrence:" + courseOccurrenceId;
            }
            return getCourseScopeKey();
        }

        public String getCourseScopeKey() {
            if (courseId != null && courseId > 0) {
                return studentUserId + ":course:" + courseId;
            }
            return studentUserId + ":" + contextKey + ":" + contextId;
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
            return isCourse();
        }

        public String getModalKey() {
            return contextKey.replace("-", "") + contextId + "_"
                    + (parentContextId == null ? 0 : parentContextId) + "_"
                    + (courseOccurrenceId == null ? 0 : courseOccurrenceId) + "_"
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
        private final List<EnrollmentOverviewView> unattachedEnrollments;
        private final List<EnrollmentStateSummaryView> stateSummaries;

        private EnrollmentStudentGroupView(List<EnrollmentOverviewView> enrollments) {
            this.enrollments = enrollments;
            this.courseGroups = courseGroups(enrollments);
            this.unattachedEnrollments = unattachedEnrollments(enrollments, courseGroups);
            this.stateSummaries = stateSummaries(enrollments);
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

        public List<EnrollmentStateSummaryView> getStateSummaries() {
            return stateSummaries;
        }

        public List<EnrollmentOverviewView> getUnattachedEnrollments() {
            return unattachedEnrollments;
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

        private static List<EnrollmentOverviewView> unattachedEnrollments(
                List<EnrollmentOverviewView> enrollments,
                List<EnrollmentCourseGroupView> courseGroups
        ) {
            Set<String> attachedKeys = new HashSet<>();
            for (EnrollmentCourseGroupView courseGroup : courseGroups) {
                attachedKeys.add(courseGroup.getEnrollment().getModalKey());
                for (EnrollmentClassGroupView classGroup : courseGroup.getClassGroups()) {
                    attachedKeys.add(classGroup.getEnrollment().getModalKey());
                    for (EnrollmentOverviewView assessment : classGroup.getAssessmentEnrollments()) {
                        attachedKeys.add(assessment.getModalKey());
                    }
                }
            }
            return enrollments.stream()
                    .filter(enrollment -> !attachedKeys.contains(enrollment.getModalKey()))
                    .sorted(enrollmentComparator())
                    .toList();
        }
    }

    public static final class EnrollmentCourseGroupView {
        private final EnrollmentOverviewView enrollment;
        private final List<EnrollmentClassGroupView> classGroups;
        private final List<EnrollmentStateSummaryView> classGroupStateSummaries;

        private EnrollmentCourseGroupView(EnrollmentOverviewView enrollment, List<EnrollmentOverviewView> allEnrollments) {
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
                EnrollmentOverviewView course,
                List<EnrollmentOverviewView> allEnrollments
        ) {
            List<EnrollmentClassGroupView> groups = new ArrayList<>();
            allEnrollments.stream()
                    .filter(EnrollmentOverviewView::isClassGroup)
                    .filter(classGroup -> course.getCourseId() != null
                            && course.getCourseId().equals(classGroup.getCourseId()))
                    .filter(classGroup -> sameCourseOccurrence(course, classGroup))
                    .sorted(enrollmentComparator())
                    .forEach(classGroup -> groups.add(new EnrollmentClassGroupView(classGroup, allEnrollments)));
            return List.copyOf(groups);
        }

        private static boolean sameCourseOccurrence(
                EnrollmentOverviewView course,
                EnrollmentOverviewView classGroup
        ) {
            Long courseOccurrenceId = course.getCourseOccurrenceId();
            Long classGroupOccurrenceId = classGroup.getCourseOccurrenceId();
            return courseOccurrenceId == null
                    || classGroupOccurrenceId == null
                    || courseOccurrenceId.equals(classGroupOccurrenceId);
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

    public static final class StudentCourseEnrollmentView {
        private final CourseEnrollment enrollment;
        private final Course course;
        private final CourseOccurrence occurrence;

        private StudentCourseEnrollmentView(
                CourseEnrollment enrollment,
                Course course,
                CourseOccurrence occurrence
        ) {
            this.enrollment = enrollment;
            this.course = course;
            this.occurrence = occurrence;
        }

        public long getCourseId() {
            return enrollment.courseId();
        }

        public long getCourseOccurrenceId() {
            return enrollment.courseOccurrenceId();
        }

        public String getCourseName() {
            return course == null ? "Course" : course.name();
        }

        public String getCourseEctsLabel() {
            return course == null || course.ects() == null ? "-" : course.ects().stripTrailingZeros().toPlainString() + " ECTS";
        }

        public String getCourseSubjectCountLabel() {
            return "Course enrollment";
        }

        public String getOccurrenceLabel() {
            return occurrence == null ? "-" : occurrence.label();
        }

        public EnrollmentState getState() {
            return enrollment.state();
        }

        public LocalDate getStartDate() {
            return enrollment.startDate();
        }

        public LocalDate getEndDate() {
            return enrollment.endDate();
        }

        public String getPeriodLabel() {
            return shortPeriod(enrollment.startDate(), enrollment.endDate());
        }

        public String getStateLabel() {
            return switch (enrollment.state()) {
                case PENDING -> "Pending approval";
                case ACTIVE -> "Active";
                case INACTIVE -> "Inactive";
                case REJECTED -> "Rejected";
                case COMPLETED -> "Completed";
                case WITHDRAWN -> "Withdrawn";
            };
        }

        public String getStateBadgeClass() {
            return enrollment.state() == EnrollmentState.ACTIVE
                    ? "bg-success-50 text-success-600"
                    : enrollment.state() == EnrollmentState.COMPLETED
                    ? "bg-info-50 text-info-600" : "bg-warning-30 text-warning-600";
        }
    }

    public static final class StudentClassEnrollmentView {
        private final ClassGroupEnrollment enrollment;
        private final ClassGroupView classGroup;
        StudentClassEnrollmentView(ClassGroupEnrollment enrollment, ClassGroupView classGroup) {
            this.enrollment = enrollment; this.classGroup = classGroup;
        }
        public long getId() { return enrollment.classGroupId(); }
        public EnrollmentState getState() { return enrollment.state(); }
        public String getCourseName() { return classGroup == null ? "Course" : classGroup.getCourseName(); }
        public String getSubjectName() { return classGroup == null ? "Subject" : classGroup.getSubjectName(); }
        public String getCode() { return classGroup == null ? "Class group" : classGroup.getCode(); }
        public String getOccurrenceLabel() {
            String label = classGroup == null ? "-" : classGroup.getOccurrenceLabel();
            return label + " · Period: " + getPeriodLabel();
        }
        public String getStateLabel() {
            return switch (enrollment.state()) {
                case PENDING -> "Pending approval"; case ACTIVE -> "Active"; case INACTIVE -> "Inactive";
                case REJECTED -> "Rejected"; case COMPLETED -> "Completed"; case WITHDRAWN -> "Withdrawn";
            };
        }
        public String getStateBadgeClass() { return enrollment.state() == EnrollmentState.ACTIVE ? "bg-success-50 text-success-600" : enrollment.state() == EnrollmentState.COMPLETED ? "bg-info-50 text-info-600" : "bg-warning-30 text-warning-600"; }
        public String getStartDate() { return enrollment.startDate() == null ? "-" : ApplicationDateTimeFormat.date(enrollment.startDate()); }
        public String getEndDate() { return enrollment.endDate() == null ? "-" : ApplicationDateTimeFormat.date(enrollment.endDate()); }
        public String getPeriodLabel() { return shortPeriod(enrollment.startDate(), enrollment.endDate()); }
        public String getOccurrenceGroupLabel() { return classGroup == null ? "-" : classGroup.getOccurrenceLabel(); }
        public String getModalityLabel() { return classGroup == null ? "-" : classGroup.getModalityLabel(); }
        public String getShiftLabel() { return classGroup == null ? "-" : classGroup.getShift(); }
    }

    public static final class StudentAssessmentEnrollmentView {
        private final Assessment assessment;
        private final EnrollmentState state;
        private final ClassGroupView classGroup;
        StudentAssessmentEnrollmentView(Assessment assessment, EnrollmentState state, ClassGroupView classGroup) { this.assessment = assessment; this.state = state; this.classGroup = classGroup; }
        /** Legacy hidden cards need a context-aware key as one assessment can
         * be applicable to several class groups. */
        public long getId() { return assessment.id() * 1_000_000L + (classGroup == null ? 0 : classGroup.getId()); }
        public EnrollmentState getState() { return state; }
        public String getTitle() { return assessment.title(); }
        public String getCourseName() { return classGroup == null ? "Course" : classGroup.getCourseName(); }
        public String getSubjectName() { return classGroup == null ? "Subject" : classGroup.getSubjectName(); }
        public String getClassGroupCode() { return classGroup == null ? "-" : classGroup.getCode(); }
        public String getOccurrenceLabel() { return classGroup == null ? "-" : classGroup.getOccurrenceLabel(); }
        public String getOccurrenceGroupLabel() { return classGroup == null ? "-" : classGroup.getOccurrenceLabel(); }
        public String getPeriodLabel() { return shortPeriod(assessment.availableFrom() == null ? null : assessment.availableFrom().toLocalDate(), assessment.availableUntil() == null ? null : assessment.availableUntil().toLocalDate()); }
        public String getModalKey() { return assessment.id() + "_" + (classGroup == null ? 0 : classGroup.getId()); }
        public String getStateLabel() { return switch (state) { case PENDING -> "Pending approval"; case ACTIVE -> "Active"; case INACTIVE -> "Inactive"; case REJECTED -> "Rejected"; case COMPLETED -> "Completed"; case WITHDRAWN -> "Withdrawn"; }; }
        public String getStateBadgeClass() { return state == EnrollmentState.ACTIVE ? "bg-success-50 text-success-600" : state == EnrollmentState.COMPLETED ? "bg-info-50 text-info-600" : "bg-warning-30 text-warning-600"; }
    }

    private static List<StudentEnrollmentCourseGroupView> enrollmentGroups(
            List<StudentClassEnrollmentView> classEnrollments,
            List<StudentAssessmentEnrollmentView> assessmentEnrollments
    ) {
        Map<String, StudentEnrollmentCourseGroupView> courses = new LinkedHashMap<>();
        List<String> courseKeys = new ArrayList<>();
        classEnrollments.forEach(enrollment -> courseKeys.add(enrollment.getCourseName()));
        assessmentEnrollments.forEach(enrollment -> courseKeys.add(enrollment.getCourseName()));
        courseKeys.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).forEach(courseName ->
                courses.put(courseName, new StudentEnrollmentCourseGroupView(courseName)));
        classEnrollments.forEach(enrollment -> courses.get(enrollment.getCourseName())
                .subject(enrollment.getSubjectName()).addClassEnrollment(enrollment));
        assessmentEnrollments.forEach(enrollment -> courses.get(enrollment.getCourseName())
                .subject(enrollment.getSubjectName()).addAssessmentEnrollment(enrollment));
        courses.values().forEach(StudentEnrollmentCourseGroupView::sort);
        return courses.values().stream()
                .sorted(Comparator.comparing(StudentEnrollmentCourseGroupView::getCourseName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static List<StudentCompletedEnrollmentCourseGroupView> completedEnrollmentGroups(
            List<StudentCourseEnrollmentView> courseEnrollments,
            List<StudentClassEnrollmentView> classEnrollments,
            List<StudentAssessmentEnrollmentView> assessmentEnrollments
    ) {
        Map<String, StudentCompletedEnrollmentCourseGroupView> courses = new LinkedHashMap<>();
        courseEnrollments.stream().filter(enrollment -> enrollment.getState() == EnrollmentState.COMPLETED
                        || enrollment.getState() == EnrollmentState.INACTIVE).forEach(enrollment ->
                courses.computeIfAbsent(enrollment.getCourseName(), StudentCompletedEnrollmentCourseGroupView::new)
                        .occurrence(enrollment.getOccurrenceLabel()).add(new StudentCompletedEnrollmentView(
                                "Course", enrollment.getCourseName(), "-", "-", enrollment.getOccurrenceLabel(), enrollment.getStateLabel(), enrollment.getPeriodLabel())));
        classEnrollments.stream().filter(enrollment -> enrollment.getState() == EnrollmentState.COMPLETED
                        || enrollment.getState() == EnrollmentState.INACTIVE).forEach(enrollment ->
                courses.computeIfAbsent(enrollment.getCourseName(), StudentCompletedEnrollmentCourseGroupView::new)
                        .occurrence(enrollment.getOccurrenceLabel()).add(new StudentCompletedEnrollmentView(
                                "Class group", enrollment.getCode(), enrollment.getSubjectName(), enrollment.getCode(), enrollment.getOccurrenceLabel(), enrollment.getStateLabel(), enrollment.getPeriodLabel())));
        assessmentEnrollments.stream().filter(enrollment -> enrollment.getState() == EnrollmentState.COMPLETED
                        || enrollment.getState() == EnrollmentState.INACTIVE).forEach(enrollment ->
                courses.computeIfAbsent(enrollment.getCourseName(), StudentCompletedEnrollmentCourseGroupView::new)
                        .occurrence(enrollment.getOccurrenceLabel()).add(new StudentCompletedEnrollmentView(
                                "Assessment", enrollment.getTitle(), enrollment.getSubjectName(), enrollment.getClassGroupCode(), enrollment.getOccurrenceLabel(), enrollment.getStateLabel(), "-")));
        courses.values().forEach(StudentCompletedEnrollmentCourseGroupView::sort);
        return courses.values().stream().sorted(Comparator.comparing(StudentCompletedEnrollmentCourseGroupView::getCourseName, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public static final class StudentEnrollmentCourseGroupView {
        private final String courseName;
        private final Map<String, StudentEnrollmentSubjectGroupView> subjects = new LinkedHashMap<>();
        StudentEnrollmentCourseGroupView(String courseName) { this.courseName = courseName; }
        public String getCourseName() { return courseName; }
        public List<StudentEnrollmentSubjectGroupView> getSubjects() { return List.copyOf(subjects.values()); }
        StudentEnrollmentSubjectGroupView subject(String subjectName) {
            return subjects.computeIfAbsent(subjectName, StudentEnrollmentSubjectGroupView::new);
        }
        void sort() {
            subjects.values().forEach(StudentEnrollmentSubjectGroupView::sort);
            List<Map.Entry<String, StudentEnrollmentSubjectGroupView>> ordered = new ArrayList<>(subjects.entrySet());
            ordered.sort(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER));
            subjects.clear();
            ordered.forEach(entry -> subjects.put(entry.getKey(), entry.getValue()));
        }
    }

    public static final class StudentEnrollmentSubjectGroupView {
        private final String subjectName;
        private final List<StudentClassEnrollmentView> classEnrollments = new ArrayList<>();
        private final Map<String, StudentEnrollmentOccurrenceGroupView> classOccurrences = new LinkedHashMap<>();
        private final List<StudentAssessmentEnrollmentView> assessmentEnrollments = new ArrayList<>();
        private final Map<String, StudentEnrollmentAssessmentOccurrenceGroupView> assessmentOccurrences = new LinkedHashMap<>();
        StudentEnrollmentSubjectGroupView(String subjectName) { this.subjectName = subjectName; }
        public String getSubjectName() { return subjectName; }
        public List<StudentClassEnrollmentView> getClassEnrollments() { return List.copyOf(classEnrollments); }
        public List<StudentEnrollmentOccurrenceGroupView> getClassOccurrences() { return List.copyOf(classOccurrences.values()); }
        public List<StudentAssessmentEnrollmentView> getAssessmentEnrollments() { return List.copyOf(assessmentEnrollments); }
        public List<StudentEnrollmentAssessmentOccurrenceGroupView> getAssessmentOccurrences() { return List.copyOf(assessmentOccurrences.values()); }
        void addClassEnrollment(StudentClassEnrollmentView enrollment) {
            classEnrollments.add(enrollment);
            classOccurrences.computeIfAbsent(enrollment.getOccurrenceGroupLabel(), StudentEnrollmentOccurrenceGroupView::new)
                    .add(enrollment);
        }
        void addAssessmentEnrollment(StudentAssessmentEnrollmentView enrollment) {
            assessmentEnrollments.add(enrollment);
            assessmentOccurrences.computeIfAbsent(enrollment.getOccurrenceGroupLabel(), StudentEnrollmentAssessmentOccurrenceGroupView::new)
                    .add(enrollment);
        }
        void sort() {
            classEnrollments.sort(Comparator.comparing(StudentClassEnrollmentView::getCode, String.CASE_INSENSITIVE_ORDER));
            classOccurrences.values().forEach(StudentEnrollmentOccurrenceGroupView::sort);
            List<Map.Entry<String, StudentEnrollmentOccurrenceGroupView>> orderedOccurrences = new ArrayList<>(classOccurrences.entrySet());
            orderedOccurrences.sort(Map.Entry.<String, StudentEnrollmentOccurrenceGroupView>comparingByKey(
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).reversed());
            classOccurrences.clear();
            orderedOccurrences.forEach(entry -> classOccurrences.put(entry.getKey(), entry.getValue()));
            assessmentEnrollments.sort(Comparator.comparing(StudentAssessmentEnrollmentView::getTitle, String.CASE_INSENSITIVE_ORDER));
            assessmentOccurrences.values().forEach(StudentEnrollmentAssessmentOccurrenceGroupView::sort);
            List<Map.Entry<String, StudentEnrollmentAssessmentOccurrenceGroupView>> orderedAssessmentOccurrences = new ArrayList<>(assessmentOccurrences.entrySet());
            orderedAssessmentOccurrences.sort(Map.Entry.<String, StudentEnrollmentAssessmentOccurrenceGroupView>comparingByKey(
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).reversed());
            assessmentOccurrences.clear();
            orderedAssessmentOccurrences.forEach(entry -> assessmentOccurrences.put(entry.getKey(), entry.getValue()));
        }
    }

    public static final class StudentEnrollmentOccurrenceGroupView {
        private final String label;
        private final List<StudentClassEnrollmentView> classEnrollments = new ArrayList<>();
        StudentEnrollmentOccurrenceGroupView(String label) { this.label = label; }
        public String getLabel() { return label; }
        public List<StudentClassEnrollmentView> getClassEnrollments() { return List.copyOf(classEnrollments); }
        public int getEnrollmentCount() { return classEnrollments.size(); }
        void add(StudentClassEnrollmentView enrollment) { classEnrollments.add(enrollment); }
        void sort() { classEnrollments.sort(Comparator.comparing(StudentClassEnrollmentView::getCode, String.CASE_INSENSITIVE_ORDER)); }
    }

    public static final class StudentEnrollmentAssessmentOccurrenceGroupView {
        private final String label;
        private final Map<String, StudentEnrollmentAssessmentClassGroupView> classGroups = new LinkedHashMap<>();
        StudentEnrollmentAssessmentOccurrenceGroupView(String label) { this.label = label; }
        public String getLabel() { return label; }
        public List<StudentEnrollmentAssessmentClassGroupView> getClassGroups() { return List.copyOf(classGroups.values()); }
        public int getEnrollmentCount() { return classGroups.values().stream().mapToInt(StudentEnrollmentAssessmentClassGroupView::getEnrollmentCount).sum(); }
        void add(StudentAssessmentEnrollmentView enrollment) {
            classGroups.computeIfAbsent(enrollment.getClassGroupCode(), StudentEnrollmentAssessmentClassGroupView::new)
                    .add(enrollment);
        }
        void sort() {
            classGroups.values().forEach(StudentEnrollmentAssessmentClassGroupView::sort);
            List<Map.Entry<String, StudentEnrollmentAssessmentClassGroupView>> ordered = new ArrayList<>(classGroups.entrySet());
            ordered.sort(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER));
            classGroups.clear();
            ordered.forEach(entry -> classGroups.put(entry.getKey(), entry.getValue()));
        }
    }

    public static final class StudentEnrollmentAssessmentClassGroupView {
        private final String code;
        private final List<StudentAssessmentEnrollmentView> assessmentEnrollments = new ArrayList<>();
        StudentEnrollmentAssessmentClassGroupView(String code) { this.code = code; }
        public String getCode() { return code; }
        public List<StudentAssessmentEnrollmentView> getAssessmentEnrollments() { return List.copyOf(assessmentEnrollments); }
        public int getEnrollmentCount() { return assessmentEnrollments.size(); }
        void add(StudentAssessmentEnrollmentView enrollment) { assessmentEnrollments.add(enrollment); }
        void sort() { assessmentEnrollments.sort(Comparator.comparing(StudentAssessmentEnrollmentView::getTitle, String.CASE_INSENSITIVE_ORDER)); }
    }

    public static final class StudentCompletedEnrollmentCourseGroupView {
        private final String courseName;
        private final Map<String, StudentCompletedEnrollmentOccurrenceGroupView> occurrences = new LinkedHashMap<>();
        StudentCompletedEnrollmentCourseGroupView(String courseName) { this.courseName = courseName; }
        public String getCourseName() { return courseName; }
        public List<StudentCompletedEnrollmentOccurrenceGroupView> getOccurrences() { return List.copyOf(occurrences.values()); }
        public int getEnrollmentCount() {
            return occurrences.values().stream()
                    .mapToInt(StudentCompletedEnrollmentOccurrenceGroupView::getEnrollmentCount)
                    .sum();
        }
        StudentCompletedEnrollmentOccurrenceGroupView occurrence(String label) {
            return occurrences.computeIfAbsent(label == null ? "-" : label, StudentCompletedEnrollmentOccurrenceGroupView::new);
        }
        void sort() {
            occurrences.values().forEach(StudentCompletedEnrollmentOccurrenceGroupView::sort);
            List<Map.Entry<String, StudentCompletedEnrollmentOccurrenceGroupView>> ordered = new ArrayList<>(occurrences.entrySet());
            ordered.sort(Map.Entry.<String, StudentCompletedEnrollmentOccurrenceGroupView>comparingByKey(
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).reversed());
            occurrences.clear();
            ordered.forEach(entry -> occurrences.put(entry.getKey(), entry.getValue()));
        }
    }

    public static final class StudentCompletedEnrollmentOccurrenceGroupView {
        private final String label;
        private final Map<String, StudentCompletedEnrollmentSubjectGroupView> subjects = new LinkedHashMap<>();
        StudentCompletedEnrollmentOccurrenceGroupView(String label) { this.label = label; }
        public String getLabel() { return label; }
        public List<StudentCompletedEnrollmentSubjectGroupView> getSubjects() { return List.copyOf(subjects.values()); }
        public int getEnrollmentCount() { return subjects.values().stream().mapToInt(StudentCompletedEnrollmentSubjectGroupView::getEnrollmentCount).sum(); }
        void add(StudentCompletedEnrollmentView enrollment) {
            subjects.computeIfAbsent(enrollment.getSubjectName(), StudentCompletedEnrollmentSubjectGroupView::new).add(enrollment);
        }
        void sort() {
            subjects.values().forEach(StudentCompletedEnrollmentSubjectGroupView::sort);
            List<Map.Entry<String, StudentCompletedEnrollmentSubjectGroupView>> ordered = new ArrayList<>(subjects.entrySet());
            ordered.sort(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER));
            subjects.clear();
            ordered.forEach(entry -> subjects.put(entry.getKey(), entry.getValue()));
        }
    }

    public static final class StudentCompletedEnrollmentSubjectGroupView {
        private final String subjectName;
        private final List<StudentCompletedEnrollmentView> enrollments = new ArrayList<>();
        StudentCompletedEnrollmentSubjectGroupView(String subjectName) { this.subjectName = subjectName; }
        public String getSubjectName() { return subjectName; }
        public List<StudentCompletedEnrollmentView> getEnrollments() { return List.copyOf(enrollments); }
        public int getEnrollmentCount() { return enrollments.size(); }
        void add(StudentCompletedEnrollmentView enrollment) { enrollments.add(enrollment); }
        void sort() { enrollments.sort(Comparator.comparing(StudentCompletedEnrollmentView::getType).thenComparing(StudentCompletedEnrollmentView::getTitle, String.CASE_INSENSITIVE_ORDER)); }
    }

    public static final class StudentCompletedEnrollmentView {
        private final String type;
        private final String title;
        private final String subjectName;
        private final String className;
        private final String occurrenceLabel;
        private final String stateLabel;
        private final String periodLabel;
        StudentCompletedEnrollmentView(String type, String title, String subjectName, String className, String occurrenceLabel, String stateLabel, String periodLabel) {
            this.type = type; this.title = title; this.subjectName = subjectName; this.className = className; this.occurrenceLabel = occurrenceLabel; this.stateLabel = stateLabel; this.periodLabel = periodLabel;
        }
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getSubjectName() { return subjectName; }
        public String getClassName() { return className; }
        public String getOccurrenceLabel() { return occurrenceLabel; }
        public String getStateLabel() { return stateLabel; }
        public String getPeriodLabel() { return periodLabel; }
    }

    private static Comparator<EnrollmentOverviewView> enrollmentComparator() {
        return Comparator
                .comparing(EnrollmentOverviewView::getStartDateSort)
                .thenComparingInt(EnrollmentOverviewView::getContextOrder)
                .thenComparing(EnrollmentOverviewView::getContextLabel, String.CASE_INSENSITIVE_ORDER);
    }
}
