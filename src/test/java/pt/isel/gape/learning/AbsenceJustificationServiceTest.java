package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
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
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class AbsenceJustificationServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-11T10:15:30Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 2, 11, 10, 15);
    private static final String IP = "127.0.0.1";

    private AttendanceRecordService attendanceRecordService;
    private AbsenceJustificationService justificationService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        attendanceRecordService = new AttendanceRecordService(connectionProvider, FIXED_CLOCK);
        justificationService = new AbsenceJustificationService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void studentSubmitsValidJustificationForOwnAbsentRecord() throws Exception {
        AttendanceRecord record = createAttendance(AttendanceStatus.ABSENT);

        AbsenceJustification justification = justificationService.submitJustification(
                4L,
                null,
                AccessProfileType.STUDENT,
                new AbsenceJustificationCreateCommand(
                        record.id(),
                        "Medical appointment",
                        "attendance/medical-appointment.pdf",
                        LocalDateTime.of(2026, 2, 15, 10, 0)
                ),
                IP
        );

        assertEquals(record.id(), justification.attendanceRecordId());
        assertEquals(4L, justification.studentSubmitterUserId());
        assertEquals(AbsenceJustificationState.SUBMITTED, justification.state());
        assertEquals(NOW, justification.submittedAt());
        assertNull(justification.processorUserId());
    }

    @Test
    void justificationForPresentRecordIsRejected() throws Exception {
        AttendanceRecord record = createAttendance(AttendanceStatus.PRESENT);

        assertThrows(IllegalArgumentException.class, () -> justificationService.submitJustification(
                4L,
                null,
                AccessProfileType.STUDENT,
                new AbsenceJustificationCreateCommand(record.id(), "I was present", null, null),
                IP
        ));
    }

    @Test
    void justificationByAnotherStudentIsRejected() throws Exception {
        addActiveStudent(6L);
        AttendanceRecord record = createAttendance(AttendanceStatus.LATE);

        assertThrows(SecurityException.class, () -> justificationService.submitJustification(
                6L,
                null,
                AccessProfileType.STUDENT,
                new AbsenceJustificationCreateCommand(record.id(), "Other student reason", null, null),
                IP
        ));
    }

    @Test
    void duplicateJustificationForAttendanceRecordIsRejected() {
        assertThrows(IllegalStateException.class, () -> justificationService.submitJustification(
                4L,
                null,
                AccessProfileType.STUDENT,
                new AbsenceJustificationCreateCommand(150L, "Duplicate reason", null, null),
                IP
        ));
    }

    @Test
    void teacherProcessesJustificationAsApproved() {
        AbsenceJustification processed = justificationService.processJustification(
                3L,
                null,
                AccessProfileType.TEACHER,
                160L,
                new AbsenceJustificationProcessCommand(
                        AbsenceJustificationState.APPROVED,
                        NOW.plusDays(3),
                        "Accepted"
                ),
                IP
        );
        AttendanceRecord attendance = attendanceRecordService.getAttendanceRecord(
                3L,
                null,
                AccessProfileType.TEACHER,
                150L,
                IP
        );

        assertEquals(AbsenceJustificationState.APPROVED, processed.state());
        assertEquals(3L, processed.processorUserId());
        assertEquals(NOW, processed.processedAt());
        assertEquals(AttendanceStatus.JUSTIFIED, attendance.status());
        assertEquals(AttendanceState.CORRECTED, attendance.state());
    }

    @Test
    void processedJustificationCanChangeBetweenFinalDecisions() {
        justificationService.processJustification(
                3L,
                null,
                AccessProfileType.TEACHER,
                160L,
                new AbsenceJustificationProcessCommand(
                        AbsenceJustificationState.APPROVED,
                        NOW.plusDays(3),
                        "Accepted"
                ),
                IP
        );

        AbsenceJustification rejected = justificationService.processJustification(
                3L,
                null,
                AccessProfileType.TEACHER,
                160L,
                new AbsenceJustificationProcessCommand(
                        AbsenceJustificationState.REJECTED,
                        NOW.plusDays(3),
                        "Rejected after review"
                ),
                IP
        );
        AttendanceRecord rejectedAttendance = attendanceRecordService.getAttendanceRecord(
                3L,
                null,
                AccessProfileType.TEACHER,
                150L,
                IP
        );

        assertEquals(AbsenceJustificationState.REJECTED, rejected.state());
        assertEquals(AttendanceStatus.ABSENT, rejectedAttendance.status());
        assertEquals(AttendanceState.CORRECTED, rejectedAttendance.state());

        AbsenceJustification approvedAgain = justificationService.processJustification(
                3L,
                null,
                AccessProfileType.TEACHER,
                160L,
                new AbsenceJustificationProcessCommand(
                        AbsenceJustificationState.APPROVED,
                        NOW.plusDays(3),
                        "Accepted after review"
                ),
                IP
        );
        AttendanceRecord approvedAttendance = attendanceRecordService.getAttendanceRecord(
                3L,
                null,
                AccessProfileType.TEACHER,
                150L,
                IP
        );

        assertEquals(AbsenceJustificationState.APPROVED, approvedAgain.state());
        assertEquals(AttendanceStatus.JUSTIFIED, approvedAttendance.status());
        assertEquals(AttendanceState.CORRECTED, approvedAttendance.state());
    }

    @Test
    void processingDateBeforeSubmissionIsRejected() throws Exception {
        AttendanceRecord record = createAttendance(AttendanceStatus.PARTIAL);
        long justificationId = insertSubmittedJustification(record.id(), LocalDateTime.of(2026, 2, 15, 10, 0));

        assertThrows(IllegalArgumentException.class, () -> justificationService.processJustification(
                3L,
                null,
                AccessProfileType.TEACHER,
                justificationId,
                new AbsenceJustificationProcessCommand(
                        AbsenceJustificationState.REJECTED,
                        NOW,
                        "Too early"
                ),
                IP
        ));
    }

    @Test
    void processingWithoutPermissionIsRejected() {
        assertThrows(SecurityException.class, () -> justificationService.processJustification(
                4L,
                null,
                AccessProfileType.STUDENT,
                160L,
                new AbsenceJustificationProcessCommand(
                        AbsenceJustificationState.APPROVED,
                        NOW,
                        "Student cannot process"
                ),
                IP
        ));
    }

    @Test
    void processingRequiresFinalDecision() {
        assertThrows(IllegalArgumentException.class, () -> justificationService.processJustification(
                3L,
                null,
                AccessProfileType.TEACHER,
                160L,
                new AbsenceJustificationProcessCommand(
                        AbsenceJustificationState.UNDER_REVIEW,
                        null,
                        "Not final"
                ),
                IP
        ));
    }

    @Test
    void administratorOrganizationGrantCanSeeJustifications() throws Exception {
        addScopedAdministrator(7L, "ORGANIZATION", 10L);

        List<AbsenceJustification> justifications = justificationService.listVisibleJustifications(
                7L,
                null,
                AccessProfileType.ADMINISTRATOR,
                IP
        );

        assertEquals(1L, justifications.stream().filter(justification -> justification.id() == 160L).count());
    }

    private AttendanceRecord createAttendance(AttendanceStatus status) throws SQLException {
        LocalDateTime startsAt = LocalDateTime.of(2026, 2, 12, 18, 0);
        long lessonId = insertOnlineLesson("Justification Lesson " + status.name(), startsAt, startsAt.plusHours(1));
        LocalDateTime checkIn = status == AttendanceStatus.ABSENT ? null : startsAt.plusMinutes(10);
        LocalDateTime checkOut = status == AttendanceStatus.ABSENT ? null : startsAt.plusMinutes(45);
        return attendanceRecordService.recordAttendance(
                3L,
                null,
                AccessProfileType.TEACHER,
                new AttendanceRecordCommand(
                        lessonId,
                        4L,
                        status,
                        AttendanceSource.MANUAL,
                        checkIn,
                        checkOut,
                        "Attendance for justification test",
                        AttendanceState.ACTIVE
                ),
                IP
        );
    }

    private static long insertOnlineLesson(String title, LocalDateTime startsAt, LocalDateTime endsAt)
            throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO lesson (
                         id_class_group, id_content_block, cod_physical_room, title, description,
                         type, provider, access_url, attendance_required, state, starts_at, ends_at
                     ) VALUES (50, 60, NULL, ?, 'Lesson created by justification test', 'online',
                         'Meet', ?, 1, 'active', ?, ?)
                     """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, title);
            statement.setString(2, "https://meet.google.com/" + title.toLowerCase().replace(' ', '-'));
            statement.setTimestamp(3, Timestamp.valueOf(startsAt));
            statement.setTimestamp(4, Timestamp.valueOf(endsAt));
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                generatedKeys.next();
                return generatedKeys.getLong(1);
            }
        }
    }

    private static long insertSubmittedJustification(long attendanceRecordId, LocalDateTime submittedAt)
            throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO absence_justification (
                         id_attendance_record, id_user_student_submitter, id_user_processor,
                         submitted_at, reason, attachment, processed_at, decision_notes, state
                     ) VALUES (?, 4, NULL, ?, 'Future submitted reason', NULL, NULL, NULL, 'submitted')
                     """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, attendanceRecordId);
            statement.setTimestamp(2, Timestamp.valueOf(submittedAt));
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                generatedKeys.next();
                return generatedKeys.getLong(1);
            }
        }
    }

    private static void addActiveStudent(long userId) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement user = connection.prepareStatement("""
                    INSERT INTO user_account (
                        id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt
                    ) VALUES (?, ?, ?, 'active', 'pt-PT', NULL, '2026-01-01 10:00:00', 'hash', 'salt')
                    """)) {
                user.setLong(1, userId);
                user.setString(2, "Justification Student " + userId);
                user.setString(3, "justification.student" + userId + "@gape.local");
                user.executeUpdate();
            }
            try (PreparedStatement profile = connection.prepareStatement("""
                    INSERT INTO student_profile (id_user, cod_student)
                    VALUES (?, ?)
                    """)) {
                profile.setLong(1, userId);
                profile.setString(2, "STD-JUS-" + userId);
                profile.executeUpdate();
            }
        }
    }

    private static void addScopedAdministrator(long userId, String contextType, long contextId) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement user = connection.prepareStatement("""
                    INSERT INTO user_account (
                        id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt
                    ) VALUES (?, ?, ?, 'active', 'pt-PT', NULL, '2026-01-01 10:00:00', 'hash', 'salt')
                    """)) {
                user.setLong(1, userId);
                user.setString(2, "Justification Administrator " + userId);
                user.setString(3, "justification.admin" + userId + "@gape.local");
                user.executeUpdate();
            }
            try (PreparedStatement profile = connection.prepareStatement("""
                    INSERT INTO administrator_profile (id_user, cod_administrator)
                    VALUES (?, ?)
                    """)) {
                profile.setLong(1, userId);
                profile.setString(2, "ADM-JUS-" + userId);
                profile.executeUpdate();
            }
            try (PreparedStatement grant = connection.prepareStatement("""
                    INSERT INTO grant_administrator (id_admin_user, cod_permission, context_type, context_id)
                    VALUES (?, 'MANAGE_LEARNING', ?, ?)
                    """)) {
                grant.setLong(1, userId);
                grant.setString(2, contextType);
                grant.setLong(3, contextId);
                grant.executeUpdate();
            }
        }
    }
}
