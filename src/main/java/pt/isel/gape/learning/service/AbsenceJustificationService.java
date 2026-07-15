package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.AbsenceJustificationDAO;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.AttendanceRecordDAO;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.LessonDAO;
import pt.isel.gape.learning.model.AbsenceJustification;
import pt.isel.gape.learning.model.AbsenceJustificationCreateCommand;
import pt.isel.gape.learning.model.AbsenceJustificationProcessCommand;
import pt.isel.gape.learning.model.AbsenceJustificationState;
import pt.isel.gape.learning.model.AttendanceRecord;
import pt.isel.gape.learning.model.AttendanceState;
import pt.isel.gape.learning.model.AttendanceStatus;
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class AbsenceJustificationService {

    private static final int REASON_MAX_LENGTH = 300;
    private static final int ATTACHMENT_MAX_LENGTH = 255;
    private static final int DECISION_NOTES_MAX_LENGTH = 500;

    private final ConnectionProvider connectionProvider;
    private final AbsenceJustificationDAO justificationDAO;
    private final AttendanceRecordDAO attendanceRecordDAO;
    private final LessonDAO lessonDAO;
    private final ScheduleAccessPolicy accessPolicy;
    private final AuditService auditService;
    private final Clock clock;

    public AbsenceJustificationService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new AbsenceJustificationDAO(connectionProvider),
                new AttendanceRecordDAO(connectionProvider),
                new LessonDAO(connectionProvider),
                new ClassGroupDAO(connectionProvider),
                new AssessmentDAO(connectionProvider),
                new PermissionDAO(connectionProvider),
                clock
        );
    }

    public AbsenceJustificationService(
            ConnectionProvider connectionProvider,
            AbsenceJustificationDAO justificationDAO,
            AttendanceRecordDAO attendanceRecordDAO,
            LessonDAO lessonDAO,
            ClassGroupDAO classGroupDAO,
            AssessmentDAO assessmentDAO,
            PermissionDAO permissionDAO,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.justificationDAO = Objects.requireNonNull(justificationDAO, "justificationDAO is required");
        this.attendanceRecordDAO = Objects.requireNonNull(attendanceRecordDAO, "attendanceRecordDAO is required");
        this.lessonDAO = Objects.requireNonNull(lessonDAO, "lessonDAO is required");
        Objects.requireNonNull(classGroupDAO, "classGroupDAO is required");
        Objects.requireNonNull(assessmentDAO, "assessmentDAO is required");
        Objects.requireNonNull(permissionDAO, "permissionDAO is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
        this.accessPolicy = new ScheduleAccessPolicy(
                classGroupDAO,
                lessonDAO,
                assessmentDAO,
                new PermissionChecker(connectionProvider),
                permissionDAO
        );
        this.auditService = new AuditService(new ActivityLogDAO(connectionProvider), clock);
    }

    public AbsenceJustification submitJustification(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            AbsenceJustificationCreateCommand command,
            String sourceIp
    ) {
        try {
            validateSubmitCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    accessPolicy.requireStudentProfile(actorUserId, sessionId, actorProfileType, sourceIp);
                    AttendanceRecord record = attendanceRecordDAO.lockById(connection, command.attendanceRecordId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Attendance record not found: " + command.attendanceRecordId()));
                    if (record.studentUserId() != actorUserId) {
                        throw new SecurityException("Students can only justify their own attendance records");
                    }
                    Lesson lesson = lessonDAO.findById(connection, record.lessonId())
                            .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + record.lessonId()));
                    accessPolicy.requireStudentClassGroupAccess(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            lesson.classGroupId(),
                            sourceIp
                    );
                    if (record.state() == AttendanceState.CANCELLED || !record.status().allowsJustification()) {
                        throw new IllegalArgumentException("Attendance record is not compatible with absence justification");
                    }
                    if (justificationDAO.findByAttendanceRecord(connection, record.id()).isPresent()) {
                        throw new IllegalStateException("Attendance record already has an absence justification");
                    }
                    LocalDateTime submittedAt = currentMinute();
                    long justificationId = justificationDAO.create(
                            connection,
                            actorUserId,
                            new AbsenceJustificationCreateCommand(
                                    command.attendanceRecordId(),
                                    command.reason(),
                                    command.attachment(),
                                    submittedAt
                            )
                    );
                    auditService.record(connection, actorUserId, sessionId, "ABSENCE_JUSTIFICATION_SUBMIT",
                            "absence_justification", Long.toString(justificationId), "success", sourceIp);
                    AbsenceJustification justification = requireJustification(connection, justificationId);
                    connection.commit();
                    return justification;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ABSENCE_JUSTIFICATION_SUBMIT", "new", sourceIp);
            throw wrap(exception, "Failed to submit absence justification");
        }
    }

    public AbsenceJustification processJustification(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long justificationId,
            AbsenceJustificationProcessCommand command,
            String sourceIp
    ) {
        try {
            validateProcessCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    AbsenceJustification justification = justificationDAO.lockById(connection, justificationId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Absence justification not found: " + justificationId));
                    if (justification.state() == AbsenceJustificationState.CANCELLED) {
                        throw new IllegalStateException("Absence justification is cancelled");
                    }
                    AttendanceRecord record = attendanceRecordDAO.lockById(connection, justification.attendanceRecordId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Attendance record not found: " + justification.attendanceRecordId()));
                    Lesson lesson = lessonDAO.findById(connection, record.lessonId())
                            .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + record.lessonId()));
                    accessPolicy.requireLessonManager(connection, actorUserId, sessionId, actorProfileType, lesson, sourceIp);
                    LocalDateTime processedAt = currentMinute();
                    if (processedAt.isBefore(justification.submittedAt())) {
                        throw new IllegalArgumentException("Absence justification processed date cannot be before submission");
                    }
                    if (command.decision() == AbsenceJustificationState.REJECTED
                            && record.status() == AttendanceStatus.JUSTIFIED) {
                        attendanceRecordDAO.updateStatus(
                                connection,
                                record.id(),
                                AttendanceStatus.ABSENT,
                                AttendanceState.CORRECTED
                        );
                    }
                    justificationDAO.process(
                            connection,
                            justificationId,
                            actorUserId,
                            command.decision(),
                            processedAt,
                            command.decisionNotes()
                    );
                    if (command.decision() == AbsenceJustificationState.APPROVED) {
                        attendanceRecordDAO.updateStatus(
                                connection,
                                record.id(),
                                AttendanceStatus.JUSTIFIED,
                                AttendanceState.CORRECTED
                        );
                    }
                    String operation = command.decision() == AbsenceJustificationState.APPROVED
                            ? "ABSENCE_JUSTIFICATION_APPROVE"
                            : "ABSENCE_JUSTIFICATION_REJECT";
                    auditService.record(connection, actorUserId, sessionId, operation,
                            "absence_justification", Long.toString(justificationId), "success", sourceIp);
                    AbsenceJustification processed = requireJustification(connection, justificationId);
                    connection.commit();
                    return processed;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ABSENCE_JUSTIFICATION_PROCESS",
                    Long.toString(justificationId), sourceIp);
            throw wrap(exception, "Failed to process absence justification");
        }
    }

    public List<AbsenceJustification> listOwnJustifications(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            accessPolicy.requireStudentProfile(actorUserId, sessionId, actorProfileType, sourceIp);
            return justificationDAO.findByStudent(connection, actorUserId);
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to list absence justifications");
        }
    }

    public List<AbsenceJustification> listVisibleJustifications(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            return switch (actorProfileType) {
                case ADMINISTRATOR -> justificationDAO.findVisibleForAdministrator(connection, actorUserId);
                case COORDINATOR -> justificationDAO.findVisibleForCoordinator(connection, actorUserId);
                case TEACHER -> justificationDAO.findVisibleForTeacher(connection, actorUserId);
                case STUDENT -> {
                    accessPolicy.requireStudentProfile(actorUserId, sessionId, actorProfileType, sourceIp);
                    yield justificationDAO.findByStudent(connection, actorUserId);
                }
            };
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to list visible absence justifications");
        }
    }

    private AbsenceJustification requireJustification(Connection connection, long justificationId) throws SQLException {
        return justificationDAO.findById(connection, justificationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Absence justification not found: " + justificationId));
    }

    private static void validateSubmitCommand(AbsenceJustificationCreateCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.attendanceRecordId() <= 0) {
            throw new IllegalArgumentException("Attendance record id must be positive");
        }
        requireText(command.reason(), "Absence justification reason is required");
        requireMaxLength(command.reason(), REASON_MAX_LENGTH, "Absence justification reason is too long");
        validateAttachment(command.attachment());
    }

    private static void validateProcessCommand(AbsenceJustificationProcessCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.decision() == null) {
            throw new IllegalArgumentException("Absence justification decision is required");
        }
        if (!command.decision().isFinalDecision()) {
            throw new IllegalArgumentException("Absence justification processing requires a final decision");
        }
        requireMaxLength(command.decisionNotes(), DECISION_NOTES_MAX_LENGTH, "Decision notes are too long");
    }

    private static void validateAttachment(String attachment) {
        if (attachment == null || attachment.isBlank()) {
            return;
        }
        requireMaxLength(attachment, ATTACHMENT_MAX_LENGTH, "Attachment path is too long");
        if (attachment.contains("..") || attachment.contains("\\") || attachment.startsWith("/")) {
            throw new IllegalArgumentException("Attachment path is not allowed");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireMaxLength(String value, int maxLength, String message) {
        if (value != null && value.trim().length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
    }

    private LocalDateTime currentMinute() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
    }

    private void auditFailure(
            long actorUserId,
            Long sessionId,
            String operationType,
            String affectedIdentifier,
            String sourceIp
    ) {
        auditService.record(actorUserId, sessionId, operationType,
                "absence_justification", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
