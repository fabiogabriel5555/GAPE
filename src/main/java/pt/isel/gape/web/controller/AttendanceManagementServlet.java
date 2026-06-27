package pt.isel.gape.web.controller;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
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
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
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
import pt.isel.gape.learning.model.AttendanceRecord;
import pt.isel.gape.learning.model.AttendanceRecordCommand;
import pt.isel.gape.learning.model.AttendanceSource;
import pt.isel.gape.learning.model.AttendanceState;
import pt.isel.gape.learning.model.AttendanceStatus;
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
    private final JustificationAttachmentStorage attachmentStorage;
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
                new JustificationAttachmentStorage(),
                clock
        );
    }

    AttendanceManagementServlet(
            AttendanceRecordService attendanceRecordService,
            AbsenceJustificationService justificationService,
            ScheduleAttendanceViewFactory viewFactory,
            Clock clock
    ) {
        this(attendanceRecordService, justificationService, viewFactory, new JustificationAttachmentStorage(), clock);
    }

    AttendanceManagementServlet(
            AttendanceRecordService attendanceRecordService,
            AbsenceJustificationService justificationService,
            ScheduleAttendanceViewFactory viewFactory,
            JustificationAttachmentStorage attachmentStorage,
            Clock clock
    ) {
        this.attendanceRecordService = attendanceRecordService;
        this.justificationService = justificationService;
        this.viewFactory = viewFactory;
        this.attachmentStorage = attachmentStorage;
        this.clock = clock;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] segments = pathSegments(request.getPathInfo());
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
        prepareDashboard(request, "attendance", "Attendance");
        forward(request, response, LEARNING_ATTENDANCE_JSP);
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
        prepareDashboard(request, "attendance", "Attendance");
        forward(request, response, STUDENT_ATTENDANCE_JSP);
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
}
