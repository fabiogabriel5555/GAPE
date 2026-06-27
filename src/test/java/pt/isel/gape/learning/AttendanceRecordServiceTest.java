package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import pt.isel.gape.learning.model.AttendanceRecord;
import pt.isel.gape.learning.model.AttendanceRecordCommand;
import pt.isel.gape.learning.model.AttendanceSource;
import pt.isel.gape.learning.model.AttendanceState;
import pt.isel.gape.learning.model.AttendanceStatus;
import pt.isel.gape.learning.service.AttendanceRecordService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class AttendanceRecordServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-11T10:15:30Z"), ZoneOffset.UTC);
    private static final String IP = "127.0.0.1";

    private AttendanceRecordService attendanceRecordService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        attendanceRecordService = new AttendanceRecordService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void teacherRegistersAttendanceForEnrolledStudent() throws Exception {
        LocalDateTime startsAt = LocalDateTime.of(2026, 2, 12, 18, 0);
        long lessonId = insertOnlineLesson("Attendance Valid Lesson", startsAt, startsAt.plusHours(2));

        AttendanceRecord record = attendanceRecordService.recordAttendance(
                3L,
                null,
                AccessProfileType.TEACHER,
                presentCommand(lessonId, startsAt, startsAt.plusMinutes(90)),
                IP
        );

        assertEquals(AttendanceStatus.PRESENT, record.status());
        assertEquals(AttendanceSource.MANUAL, record.source());
        assertEquals(90L, record.permanenceMinutes());
    }

    @Test
    void permanenceIsCalculatedFromCheckInAndCheckOut() {
        AttendanceRecord record = attendanceRecordService.getAttendanceRecord(
                4L,
                null,
                AccessProfileType.STUDENT,
                150L,
                IP
        );

        assertEquals(119L, record.permanenceMinutes());
        assertEquals(119L, AttendanceRecordService.calculatePermanenceMinutes(record.checkIn(), record.checkOut()));
    }

    @Test
    void attendanceForStudentNotEnrolledInLessonClassGroupIsRejected() throws Exception {
        addActiveStudent(6L);
        LocalDateTime startsAt = LocalDateTime.of(2026, 2, 12, 18, 0);
        long lessonId = insertOnlineLesson("Attendance Non Enrolled", startsAt, startsAt.plusHours(1));

        assertThrows(SecurityException.class, () -> attendanceRecordService.recordAttendance(
                3L,
                null,
                AccessProfileType.TEACHER,
                new AttendanceRecordCommand(
                        lessonId,
                        6L,
                        AttendanceStatus.PRESENT,
                        AttendanceSource.MANUAL,
                        startsAt,
                        startsAt.plusMinutes(30),
                        null,
                        AttendanceState.ACTIVE
                ),
                IP
        ));
    }

    @Test
    void directJustifiedAttendanceCreationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> attendanceRecordService.recordAttendance(
                3L,
                null,
                AccessProfileType.TEACHER,
                new AttendanceRecordCommand(
                        80L,
                        4L,
                        AttendanceStatus.JUSTIFIED,
                        AttendanceSource.MANUAL,
                        null,
                        null,
                        "Direct justified status",
                        AttendanceState.ACTIVE
                ),
                IP
        ));
    }

    @Test
    void automaticAttendanceCannotBeRecordedThroughManualService() {
        LocalDateTime checkIn = LocalDateTime.of(2026, 2, 5, 18, 0);

        assertThrows(IllegalArgumentException.class, () -> attendanceRecordService.recordAttendance(
                3L,
                null,
                AccessProfileType.TEACHER,
                new AttendanceRecordCommand(
                        80L,
                        4L,
                        AttendanceStatus.PRESENT,
                        AttendanceSource.AUTOMATIC,
                        checkIn,
                        checkIn.plusMinutes(30),
                        "Automatic without platform proof",
                        AttendanceState.ACTIVE
                ),
                IP
        ));
    }

    @Test
    void duplicateActiveAttendanceForSameStudentAndLessonIsRejected() {
        assertThrows(IllegalStateException.class, () -> attendanceRecordService.recordAttendance(
                3L,
                null,
                AccessProfileType.TEACHER,
                presentCommand(80L, LocalDateTime.of(2026, 2, 5, 18, 0), LocalDateTime.of(2026, 2, 5, 20, 0)),
                IP
        ));
    }

    @Test
    void correctedAttendanceDoesNotBlockNewActiveRecordForSameStudentAndLesson() throws Exception {
        LocalDateTime startsAt = LocalDateTime.of(2026, 2, 12, 18, 0);
        long lessonId = insertOnlineLesson("Attendance Corrected Replaced", startsAt, startsAt.plusHours(1));
        insertStoredAttendance(
                lessonId,
                4L,
                AttendanceStatus.PRESENT,
                AttendanceState.CORRECTED,
                startsAt,
                startsAt.plusMinutes(20)
        );

        AttendanceRecord record = attendanceRecordService.recordAttendance(
                3L,
                null,
                AccessProfileType.TEACHER,
                presentCommand(lessonId, startsAt.plusMinutes(5), startsAt.plusMinutes(55)),
                IP
        );

        assertEquals(AttendanceState.ACTIVE, record.state());
        assertEquals(AttendanceStatus.PRESENT, record.status());
    }

    @Test
    void checkOutBeforeCheckInIsRejected() throws Exception {
        LocalDateTime startsAt = LocalDateTime.of(2026, 2, 12, 18, 0);
        long lessonId = insertOnlineLesson("Attendance Invalid Hours", startsAt, startsAt.plusHours(1));

        assertThrows(IllegalArgumentException.class, () -> attendanceRecordService.recordAttendance(
                3L,
                null,
                AccessProfileType.TEACHER,
                presentCommand(lessonId, startsAt.plusMinutes(30), startsAt),
                IP
        ));
    }

    @Test
    void studentCannotRecordAttendance() throws Exception {
        LocalDateTime startsAt = LocalDateTime.of(2026, 2, 12, 18, 0);
        long lessonId = insertOnlineLesson("Attendance Student Actor", startsAt, startsAt.plusHours(1));

        assertThrows(SecurityException.class, () -> attendanceRecordService.recordAttendance(
                4L,
                null,
                AccessProfileType.STUDENT,
                presentCommand(lessonId, startsAt, startsAt.plusMinutes(45)),
                IP
        ));
    }

    @Test
    void coordinatorAndAdministratorCanRecordAttendanceInContext() throws Exception {
        LocalDateTime startsAt = LocalDateTime.of(2026, 2, 12, 18, 0);
        long coordinatorLessonId = insertOnlineLesson("Attendance Coordinator", startsAt, startsAt.plusHours(1));
        long administratorLessonId = insertOnlineLesson(
                "Attendance Administrator",
                startsAt.plusHours(2),
                startsAt.plusHours(3)
        );

        AttendanceRecord coordinatorRecord = attendanceRecordService.recordAttendance(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                presentCommand(coordinatorLessonId, startsAt, startsAt.plusMinutes(45)),
                IP
        );
        AttendanceRecord administratorRecord = attendanceRecordService.recordAttendance(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                presentCommand(administratorLessonId, startsAt.plusHours(2), startsAt.plusHours(3)),
                IP
        );

        assertEquals(AttendanceStatus.PRESENT, coordinatorRecord.status());
        assertEquals(AttendanceStatus.PRESENT, administratorRecord.status());
    }

    @Test
    void listAttendanceByContextAndOwnAttendance() {
        List<AttendanceRecord> lessonRecords = attendanceRecordService.listAttendanceForLesson(
                3L,
                null,
                AccessProfileType.TEACHER,
                80L,
                IP
        );
        List<AttendanceRecord> ownRecords = attendanceRecordService.listOwnAttendance(
                4L,
                null,
                AccessProfileType.STUDENT,
                IP
        );

        assertTrue(lessonRecords.stream().anyMatch(record -> record.id() == 150L));
        assertTrue(ownRecords.stream().anyMatch(record -> record.id() == 150L));
    }

    @Test
    void administratorOrganizationGrantCanSeeAttendanceRecords() throws Exception {
        addScopedAdministrator(7L, "ORGANIZATION", 10L);

        List<AttendanceRecord> records = attendanceRecordService.listVisibleAttendance(
                7L,
                null,
                AccessProfileType.ADMINISTRATOR,
                IP
        );

        assertTrue(records.stream().anyMatch(record -> record.id() == 150L));
    }

    private static AttendanceRecordCommand presentCommand(
            long lessonId,
            LocalDateTime checkIn,
            LocalDateTime checkOut
    ) {
        return new AttendanceRecordCommand(
                lessonId,
                4L,
                AttendanceStatus.PRESENT,
                AttendanceSource.MANUAL,
                checkIn,
                checkOut,
                "Attendance recorded by test",
                AttendanceState.ACTIVE
        );
    }

    private static long insertOnlineLesson(String title, LocalDateTime startsAt, LocalDateTime endsAt)
            throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO lesson (
                         id_class_group, id_content_block, cod_physical_room, title, description,
                         type, provider, access_url, attendance_required, state, starts_at, ends_at
                     ) VALUES (50, 60, NULL, ?, 'Lesson created by attendance test', 'online',
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

    private static void insertStoredAttendance(
            long lessonId,
            long studentUserId,
            AttendanceStatus status,
            AttendanceState state,
            LocalDateTime checkIn,
            LocalDateTime checkOut
    ) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO attendance_record (
                         id_lesson, id_user_student, status, source, check_in, check_out, notes, state
                     ) VALUES (?, ?, ?, 'manual', ?, ?, 'Stored attendance by test', ?)
                     """)) {
            statement.setLong(1, lessonId);
            statement.setLong(2, studentUserId);
            statement.setString(3, status.toDatabaseValue());
            statement.setTimestamp(4, Timestamp.valueOf(checkIn));
            statement.setTimestamp(5, Timestamp.valueOf(checkOut));
            statement.setString(6, state.toDatabaseValue());
            statement.executeUpdate();
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
                user.setString(2, "Attendance Student " + userId);
                user.setString(3, "attendance.student" + userId + "@gape.local");
                user.executeUpdate();
            }
            try (PreparedStatement profile = connection.prepareStatement("""
                    INSERT INTO student_profile (id_user, cod_student)
                    VALUES (?, ?)
                    """)) {
                profile.setLong(1, userId);
                profile.setString(2, "STD-ATT-" + userId);
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
                user.setString(2, "Attendance Administrator " + userId);
                user.setString(3, "attendance.admin" + userId + "@gape.local");
                user.executeUpdate();
            }
            try (PreparedStatement profile = connection.prepareStatement("""
                    INSERT INTO administrator_profile (id_user, cod_administrator)
                    VALUES (?, ?)
                    """)) {
                profile.setLong(1, userId);
                profile.setString(2, "ADM-ATT-" + userId);
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
