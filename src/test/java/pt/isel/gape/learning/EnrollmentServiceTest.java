package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.CourseEnrollmentCommand;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.service.EnrollmentService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class EnrollmentServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private EnrollmentService enrollmentService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        enrollmentService = new EnrollmentService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void administratorCanEnrollStudentInCourseOccurrence() {
        CourseEnrollment enrollment = enrollmentService.enrollStudentInCourse(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new CourseEnrollmentCommand(
                        4L,
                        31L,
                        310L
                ),
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.ACTIVE, enrollment.state());
        assertEquals(31L, enrollment.courseId());
        assertEquals(310L, enrollment.courseOccurrenceId());
        assertEquals("draft", certificateState(4L, 31L, 310L));
        // A course enrollment alone must not invent a subject-occurrence grade sheet.
        // That sheet is created only when a real class group exists in this exact
        // subject/course-occurrence context.
        assertNull(subjectGradeSheetState(41L, 310L));
    }

    @Test
    void administratorCanEnrollStudentInScheduledCourseOccurrence() throws Exception {
        long scheduledOccurrenceId = insertScheduledCourseOccurrence();

        CourseEnrollment enrollment = enrollmentService.enrollStudentInCourse(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new CourseEnrollmentCommand(4L, 30L, scheduledOccurrenceId),
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.ACTIVE, enrollment.state());
        assertEquals(scheduledOccurrenceId, enrollment.courseOccurrenceId());
        assertEquals(LocalDate.of(2027, 1, 1), enrollment.startDate());
        assertEquals(LocalDate.of(2027, 12, 31), enrollment.endDate());
    }

    @Test
    void overlappingActiveCourseEnrollmentIsRejected() {
        assertThrows(
                IllegalStateException.class,
                () -> enrollmentService.enrollStudentInCourse(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new CourseEnrollmentCommand(
                                4L,
                                30L,
                                300L
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void administratorCanWithdrawStudentFromCourse() {
        CourseEnrollment enrollment = enrollmentService.withdrawStudentFromCourse(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                4L,
                30L,
                300L,
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.WITHDRAWN, enrollment.state());
        assertEquals(LocalDate.of(2026, 12, 31), enrollment.endDate());
    }

    @Test
    void administratorCanUpdateStudentCourseEnrollment() {
        CourseEnrollment enrollment = enrollmentService.updateCourseEnrollment(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                4L,
                30L,
                300L,
                EnrollmentState.INACTIVE,
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.INACTIVE, enrollment.state());
        assertEquals(LocalDate.of(2026, 12, 31), enrollment.endDate());
    }

    @Test
    void completedCourseEnrollmentStateIsManagedAutomatically() {
        assertThrows(
                IllegalArgumentException.class,
                () -> enrollmentService.updateCourseEnrollment(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        4L,
                        30L,
                        300L,
                        EnrollmentState.COMPLETED,
                        "127.0.0.1"
                )
        );
    }

    @Test
    void administratorCanDeleteStudentCourseEnrollment() throws Exception {
        enrollmentService.deleteCourseEnrollment(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                4L,
                30L,
                300L,
                "127.0.0.1"
        );

        assertTrue(isCourseEnrollmentMissing(4L, 30L));
        assertTrue(isClassGroupEnrollmentMissing(4L, 50L));
    }

    @Test
    void activeCourseEnrollmentCountIncludesActiveRowsOutsideCurrentDateWindow() throws Exception {
        insertCourseEnrollment(4L, 31L, 310L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1));

        EnrollmentDAO enrollmentDAO = new EnrollmentDAO(DatabaseTestSupport::openConnection);

        assertEquals(1L, enrollmentDAO.countActiveCourseEnrollments(31L));
    }

    @Test
    void withdrawingCourseWithdrawsActiveClassGroupEnrollmentsInThatCourse() throws Exception {
        enrollmentService.withdrawStudentFromCourse(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                4L,
                30L,
                300L,
                "127.0.0.1"
        );

        assertEquals("withdrawn", classGroupEnrollmentState(4L, 50L));
    }

    @Test
    void operationWithoutPermissionIsRejected() {
        assertThrows(
                SecurityException.class,
                () -> enrollmentService.enrollStudentInCourse(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        new CourseEnrollmentCommand(
                                4L,
                                31L,
                                310L
                        ),
                        "127.0.0.1"
                )
        );
    }

    private static void insertCourseEnrollment(
            long studentUserId,
            long courseId,
            long courseOccurrenceId,
            LocalDate startDate,
            LocalDate endDate
    ) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO enroll_course (
                         id_student_user, id_course, id_course_occurrence, state, start_date, end_date
                     ) VALUES (?, ?, ?, 'active', ?, ?)
                     """)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            statement.setLong(3, courseOccurrenceId);
            statement.setDate(4, java.sql.Date.valueOf(startDate));
            if (endDate == null) {
                statement.setNull(5, java.sql.Types.DATE);
            } else {
                statement.setDate(5, java.sql.Date.valueOf(endDate));
            }
            statement.executeUpdate();
        }
    }

    private static long insertScheduledCourseOccurrence() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO course_occurrence (
                         id_course_occurrence, id_course, reference_year, label, starts_at, ends_at, state
                     ) VALUES (301, 30, 2027, '2027', '2027-01-01', '2027-12-31', 'scheduled')
                     """)) {
            statement.executeUpdate();
            return 301L;
        }
    }

    private static boolean isCourseEnrollmentMissing(long studentUserId, long courseId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM enroll_course
                     WHERE id_student_user = ?
                       AND id_course = ?
                     """)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) == 0L;
            }
        }
    }

    private static boolean isClassGroupEnrollmentMissing(long studentUserId, long classGroupId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM enroll_class_group
                     WHERE id_student_user = ?
                       AND id_class_group = ?
                     """)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) == 0L;
            }
        }
    }

    private static String classGroupEnrollmentState(long studentUserId, long classGroupId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT state
                     FROM enroll_class_group
                     WHERE id_student_user = ?
                       AND id_class_group = ?
                     """)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("state") : null;
            }
        }
    }

    private static String certificateState(long studentUserId, long courseId, long courseOccurrenceId) {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT state
                     FROM certificate
                     WHERE id_user_student = ?
                       AND id_course = ?
                       AND id_course_occurrence = ?
                     """)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            statement.setLong(3, courseOccurrenceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("state") : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load certificate state", exception);
        }
    }

    private static String subjectGradeSheetState(long subjectId, long courseOccurrenceId) {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT gs.state
                     FROM grade_sheet gs
                     WHERE gs.id_subject = ?
                       AND gs.id_course_occurrence = ?
                       AND NOT EXISTS (
                             SELECT 1
                             FROM associate_grade_sheet_class_group agscg
                             WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                       )
                     ORDER BY gs.id_grade_sheet DESC
                     LIMIT 1
                     """)) {
            statement.setLong(1, subjectId);
            statement.setLong(2, courseOccurrenceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("state") : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load grade sheet state", exception);
        }
    }
}
