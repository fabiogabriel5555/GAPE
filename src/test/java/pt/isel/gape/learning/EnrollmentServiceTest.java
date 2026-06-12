package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.CourseEnrollmentCommand;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.SubjectEnrollment;
import pt.isel.gape.learning.model.SubjectEnrollmentCommand;
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
    void administratorCanEnrollStudentInCourse() {
        CourseEnrollment enrollment = enrollmentService.enrollStudentInCourse(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new CourseEnrollmentCommand(
                        4L,
                        31L,
                        LocalDate.of(2026, 3, 1),
                        null
                ),
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.ACTIVE, enrollment.state());
        assertEquals(31L, enrollment.courseId());
    }

    @Test
    void studentCanEnrollSelfInSubjectIntegratedInActiveCourseEnrollment() {
        SubjectEnrollment enrollment = enrollmentService.enrollStudentInSubject(
                4L,
                null,
                AccessProfileType.STUDENT,
                new SubjectEnrollmentCommand(
                        4L,
                        41L,
                        30L,
                        LocalDate.of(2026, 3, 1),
                        null
                ),
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.ACTIVE, enrollment.state());
        assertEquals(30L, enrollment.courseId());
        assertEquals(41L, enrollment.subjectId());
    }

    @Test
    void subjectEnrollmentRequiresIntegratedSubject() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO subject (
                         id_subject, id_organization, name, acronym, description, ects, workload_hours, state
                     ) VALUES (42, 10, 'Sistemas Distribuidos', 'SD', NULL, 6.00, 70, 'active')
                     """)) {
            statement.executeUpdate();
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> enrollmentService.enrollStudentInSubject(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new SubjectEnrollmentCommand(
                                4L,
                                42L,
                                30L,
                                LocalDate.of(2026, 3, 1),
                                null
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void subjectEnrollmentRequiresCourseEnrollment() {
        assertThrows(
                IllegalStateException.class,
                () -> enrollmentService.enrollStudentInSubject(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new SubjectEnrollmentCommand(
                                4L,
                                41L,
                                31L,
                                LocalDate.of(2026, 3, 1),
                                null
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void subjectEnrollmentRequiresCourseEnrollmentForFullSubjectPeriod() throws Exception {
        integrateSubject(31L, 41L);
        insertCourseEnrollment(4L, 31L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1));

        assertThrows(
                IllegalStateException.class,
                () -> enrollmentService.enrollStudentInSubject(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new SubjectEnrollmentCommand(
                                4L,
                                41L,
                                31L,
                                LocalDate.of(2026, 3, 1),
                                LocalDate.of(2026, 5, 1)
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void overlappingActiveSubjectEnrollmentAcrossCoursesIsRejected() throws Exception {
        integrateSubject(31L, 40L);
        insertCourseEnrollment(4L, 31L, LocalDate.of(2026, 3, 1), null);

        assertThrows(
                IllegalStateException.class,
                () -> enrollmentService.enrollStudentInSubject(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new SubjectEnrollmentCommand(
                                4L,
                                40L,
                                31L,
                                LocalDate.of(2026, 3, 1),
                                null
                        ),
                        "127.0.0.1"
                )
        );
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
                                LocalDate.of(2026, 3, 1),
                                null
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
                LocalDate.of(2026, 5, 31),
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.WITHDRAWN, enrollment.state());
        assertEquals(LocalDate.of(2026, 5, 31), enrollment.endDate());
    }

    @Test
    void withdrawingCourseWithdrawsActiveSubjectEnrollmentsInThatCourse() throws Exception {
        enrollmentService.withdrawStudentFromCourse(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                4L,
                30L,
                LocalDate.of(2026, 5, 31),
                "127.0.0.1"
        );

        assertTrue(isSubjectEnrollmentWithdrawn(4L, 30L, 40L));
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
                                LocalDate.of(2026, 3, 1),
                                null
                        ),
                        "127.0.0.1"
                )
        );
    }

    private static void integrateSubject(long courseId, long subjectId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO integrate_subject (id_course, id_subject, curricular_year, term, mandatory, state)
                     VALUES (?, ?, 1, 'semester_1', 1, 'active')
                     ON DUPLICATE KEY UPDATE state = 'active'
                     """)) {
            statement.setLong(1, courseId);
            statement.setLong(2, subjectId);
            statement.executeUpdate();
        }
    }

    private static void insertCourseEnrollment(
            long studentUserId,
            long courseId,
            LocalDate startDate,
            LocalDate endDate
    ) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO enroll_course (id_student_user, id_course, state, start_date, end_date)
                     VALUES (?, ?, 'active', ?, ?)
                     """)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            statement.setDate(3, java.sql.Date.valueOf(startDate));
            if (endDate == null) {
                statement.setNull(4, java.sql.Types.DATE);
            } else {
                statement.setDate(4, java.sql.Date.valueOf(endDate));
            }
            statement.executeUpdate();
        }
    }

    private static boolean isSubjectEnrollmentWithdrawn(long studentUserId, long courseId, long subjectId)
            throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT state
                     FROM enroll_subject
                     WHERE id_student_user = ?
                       AND id_course = ?
                       AND id_subject = ?
                     """)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            statement.setLong(3, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && "withdrawn".equals(resultSet.getString("state"));
            }
        }
    }
}
