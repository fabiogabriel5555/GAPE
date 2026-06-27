package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.AttendanceRecordDAO;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.LessonDAO;
import pt.isel.gape.learning.model.AttendanceRecord;
import pt.isel.gape.learning.model.AttendanceRecordCommand;
import pt.isel.gape.learning.model.AttendanceSource;
import pt.isel.gape.learning.model.AttendanceState;
import pt.isel.gape.learning.model.AttendanceStatus;
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class AttendanceRecordService {

    private static final int NOTES_MAX_LENGTH = 500;

    private final ConnectionProvider connectionProvider;
    private final AttendanceRecordDAO attendanceRecordDAO;
    private final LessonDAO lessonDAO;
    private final ScheduleAccessPolicy accessPolicy;
    private final AuditService auditService;

    public AttendanceRecordService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new AttendanceRecordDAO(connectionProvider),
                new LessonDAO(connectionProvider),
                new ClassGroupDAO(connectionProvider),
                new AssessmentDAO(connectionProvider),
                new PermissionDAO(connectionProvider),
                clock
        );
    }

    public AttendanceRecordService(
            ConnectionProvider connectionProvider,
            AttendanceRecordDAO attendanceRecordDAO,
            LessonDAO lessonDAO,
            ClassGroupDAO classGroupDAO,
            AssessmentDAO assessmentDAO,
            PermissionDAO permissionDAO,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.attendanceRecordDAO = Objects.requireNonNull(attendanceRecordDAO, "attendanceRecordDAO is required");
        this.lessonDAO = Objects.requireNonNull(lessonDAO, "lessonDAO is required");
        Objects.requireNonNull(classGroupDAO, "classGroupDAO is required");
        Objects.requireNonNull(assessmentDAO, "assessmentDAO is required");
        Objects.requireNonNull(permissionDAO, "permissionDAO is required");
        Objects.requireNonNull(clock, "clock is required");
        this.accessPolicy = new ScheduleAccessPolicy(
                classGroupDAO,
                lessonDAO,
                assessmentDAO,
                new PermissionChecker(connectionProvider),
                permissionDAO
        );
        this.auditService = new AuditService(new ActivityLogDAO(connectionProvider), clock);
    }

    public AttendanceRecord recordAttendance(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            AttendanceRecordCommand command,
            String sourceIp
    ) {
        try {
            validateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Lesson lesson = lessonDAO.lockById(connection, command.lessonId())
                            .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + command.lessonId()));
                    accessPolicy.requireLessonManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            lesson,
                            sourceIp
                    );
                    if (!lessonDAO.hasCurrentStudentClassGroupAccess(
                            connection,
                            command.studentUserId(),
                            lesson.classGroupId()
                    )) {
                        throw new SecurityException("Student is not enrolled in the lesson class group");
                    }
                    if (attendanceRecordDAO.hasActiveRecord(
                            connection,
                            command.lessonId(),
                            command.studentUserId()
                    )) {
                        throw new IllegalStateException("An active attendance record already exists for the student and lesson");
                    }
                    long recordId = attendanceRecordDAO.create(connection, command);
                    auditService.record(connection, actorUserId, sessionId, "ATTENDANCE_RECORD_CREATE",
                            "attendance_record", Long.toString(recordId), "success", sourceIp);
                    AttendanceRecord record = requireRecord(connection, recordId);
                    connection.commit();
                    return record;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ATTENDANCE_RECORD_CREATE", "new", sourceIp);
            throw wrap(exception, "Failed to create attendance record");
        }
    }

    public AttendanceRecord getAttendanceRecord(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long recordId,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            AttendanceRecord record = requireRecord(connection, recordId);
            Lesson lesson = lessonDAO.findById(connection, record.lessonId())
                    .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + record.lessonId()));
            if (actorProfileType == AccessProfileType.STUDENT) {
                if (record.studentUserId() != actorUserId) {
                    throw new SecurityException("Students can only access their own attendance records");
                }
                accessPolicy.requireStudentClassGroupAccess(
                        connection,
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        lesson.classGroupId(),
                        sourceIp
                );
                return record;
            }
            accessPolicy.requireLessonManager(connection, actorUserId, sessionId, actorProfileType, lesson, sourceIp);
            return record;
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to read attendance record");
        }
    }

    public List<AttendanceRecord> listAttendanceForLesson(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long lessonId,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            Lesson lesson = lessonDAO.findById(connection, lessonId)
                    .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + lessonId));
            accessPolicy.requireLessonManager(connection, actorUserId, sessionId, actorProfileType, lesson, sourceIp);
            return attendanceRecordDAO.findByLesson(connection, lessonId);
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to list lesson attendance");
        }
    }

    public List<AttendanceRecord> listOwnAttendance(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            accessPolicy.requireStudentProfile(actorUserId, sessionId, actorProfileType, sourceIp);
            return attendanceRecordDAO.findByStudent(connection, actorUserId);
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to list student attendance");
        }
    }

    public List<AttendanceRecord> listVisibleAttendance(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            return switch (actorProfileType) {
                case ADMINISTRATOR -> attendanceRecordDAO.findVisibleForAdministrator(connection, actorUserId);
                case COORDINATOR -> attendanceRecordDAO.findVisibleForCoordinator(connection, actorUserId);
                case TEACHER -> attendanceRecordDAO.findVisibleForTeacher(connection, actorUserId);
                case STUDENT -> {
                    accessPolicy.requireStudentProfile(actorUserId, sessionId, actorProfileType, sourceIp);
                    yield attendanceRecordDAO.findByStudent(connection, actorUserId);
                }
            };
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to list visible attendance");
        }
    }

    public static long calculatePermanenceMinutes(LocalDateTime checkIn, LocalDateTime checkOut) {
        return AttendanceRecordDAO.permanenceMinutes(checkIn, checkOut);
    }

    private AttendanceRecord requireRecord(Connection connection, long recordId) throws SQLException {
        return attendanceRecordDAO.findById(connection, recordId)
                .orElseThrow(() -> new IllegalArgumentException("Attendance record not found: " + recordId));
    }

    private static void validateCommand(AttendanceRecordCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.lessonId() <= 0) {
            throw new IllegalArgumentException("Lesson id must be positive");
        }
        if (command.studentUserId() <= 0) {
            throw new IllegalArgumentException("Student id must be positive");
        }
        Objects.requireNonNull(command.status(), "attendance status is required");
        Objects.requireNonNull(command.source(), "attendance source is required");
        Objects.requireNonNull(command.state(), "attendance state is required");
        if (command.status() == AttendanceStatus.JUSTIFIED) {
            throw new IllegalArgumentException("Justified attendance must be produced by an approved justification");
        }
        if (command.source() == AttendanceSource.AUTOMATIC) {
            throw new IllegalArgumentException("Automatic attendance must be produced by platform attendance integration");
        }
        if (command.checkOut() != null && command.checkIn() == null) {
            throw new IllegalArgumentException("Attendance check-out requires check-in");
        }
        if (command.checkIn() != null && command.checkOut() != null && command.checkOut().isBefore(command.checkIn())) {
            throw new IllegalArgumentException("Attendance check-out cannot be before check-in");
        }
        if (command.status() == AttendanceStatus.ABSENT && (command.checkIn() != null || command.checkOut() != null)) {
            throw new IllegalArgumentException("Absent attendance cannot include permanence timestamps");
        }
        if (command.state() == AttendanceState.CANCELLED) {
            throw new IllegalArgumentException("New attendance records cannot be cancelled");
        }
        if (command.notes() != null && command.notes().trim().length() > NOTES_MAX_LENGTH) {
            throw new IllegalArgumentException("Attendance notes are too long");
        }
    }

    private void auditFailure(
            long actorUserId,
            Long sessionId,
            String operationType,
            String affectedIdentifier,
            String sourceIp
    ) {
        auditService.record(actorUserId, sessionId, operationType,
                "attendance_record", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
