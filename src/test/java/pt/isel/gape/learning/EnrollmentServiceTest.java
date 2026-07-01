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
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.CourseEnrollmentCommand;
import pt.isel.gape.learning.model.EnrollmentApprovalMode;
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
        assertEquals("draft", certificateState(4L, 31L));
        assertEquals("draft", subjectGradeSheetState(41L));
    }

    @Test
    void studentCanRequestSelfSubjectEnrollmentIntegratedInActiveCourseEnrollment() {
        SubjectEnrollment enrollment = enrollmentService.requestStudentInSubject(
                4L,
                null,
                new SubjectEnrollmentCommand(
                        4L,
                        41L,
                        30L,
                        LocalDate.of(2026, 3, 1),
                        null
                ),
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.PENDING, enrollment.state());
        assertEquals(30L, enrollment.courseId());
        assertEquals(41L, enrollment.subjectId());
    }

    @Test
    void subjectRequestIsAutoApprovedWhenPolicyIsAutoApprove() {
        enrollmentService.updateSubjectEnrollmentPolicy(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                30L,
                41L,
                EnrollmentApprovalMode.AUTO_APPROVE,
                "127.0.0.1"
        );

        SubjectEnrollment enrollment = enrollmentService.requestStudentInSubject(
                4L,
                null,
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
        assertEquals("draft", subjectGradeSheetState(41L));
    }

    @Test
    void autoApprovedSubjectRequestAllowsSameSubjectInAnotherCourse() throws Exception {
        integrateSubject(31L, 40L);
        insertCourseEnrollment(4L, 31L, LocalDate.of(2026, 3, 1), null);
        enrollmentService.updateSubjectEnrollmentPolicy(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                31L,
                40L,
                EnrollmentApprovalMode.AUTO_APPROVE,
                "127.0.0.1"
        );

        SubjectEnrollment enrollment = enrollmentService.requestStudentInSubject(
                4L,
                null,
                new SubjectEnrollmentCommand(
                        4L,
                        40L,
                        31L,
                        LocalDate.of(2026, 3, 1),
                        null
                ),
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.ACTIVE, enrollment.state());
        assertEquals(31L, enrollment.courseId());
        assertEquals(40L, enrollment.subjectId());
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
    void administratorCanEnrollStudentInSameSubjectThroughAnotherCourse() throws Exception {
        integrateSubject(31L, 40L);
        insertCourseEnrollment(4L, 31L, LocalDate.of(2026, 3, 1), null);

        SubjectEnrollment enrollment = enrollmentService.enrollStudentInSubject(
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
        );

        assertEquals(EnrollmentState.ACTIVE, enrollment.state());
        assertEquals(31L, enrollment.courseId());
        assertEquals(40L, enrollment.subjectId());
    }

    @Test
    void administratorCanApproveSameSubjectEnrollmentThroughAnotherCourse() throws Exception {
        insertSubjectEnrollment(
                4L,
                30L,
                41L,
                EnrollmentState.ACTIVE,
                LocalDate.of(2026, 3, 1),
                null
        );
        insertCourseEnrollment(4L, 31L, LocalDate.of(2026, 3, 1), null);
        insertSubjectEnrollment(
                4L,
                31L,
                41L,
                EnrollmentState.PENDING,
                LocalDate.of(2026, 3, 1),
                null
        );

        SubjectEnrollment enrollment = enrollmentService.approveSubjectEnrollment(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                4L,
                31L,
                41L,
                null,
                null,
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.ACTIVE, enrollment.state());
        assertEquals(31L, enrollment.courseId());
        assertEquals(41L, enrollment.subjectId());
    }

    @Test
    void withdrawnSubjectEnrollmentCanBeRequestedAgainWhenApprovalIsManual() throws Exception {
        insertSubjectEnrollment(
                4L,
                30L,
                41L,
                EnrollmentState.ACTIVE,
                LocalDate.of(2026, 3, 1),
                null
        );
        insertCourseEnrollment(4L, 31L, LocalDate.of(2026, 3, 1), null);
        insertSubjectEnrollment(
                4L,
                31L,
                41L,
                EnrollmentState.WITHDRAWN,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 2)
        );

        SubjectEnrollment enrollment = enrollmentService.requestStudentInSubject(
                4L,
                null,
                new SubjectEnrollmentCommand(
                        4L,
                        41L,
                        31L,
                        LocalDate.of(2026, 3, 3),
                        null
                ),
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.PENDING, enrollment.state());
        assertEquals(31L, enrollment.courseId());
        assertEquals(41L, enrollment.subjectId());
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
    void administratorCanUpdateStudentCourseEnrollment() {
        CourseEnrollment enrollment = enrollmentService.updateCourseEnrollment(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                4L,
                30L,
                EnrollmentState.INACTIVE,
                LocalDate.of(2026, 5, 15),
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.INACTIVE, enrollment.state());
        assertEquals(LocalDate.of(2026, 5, 15), enrollment.endDate());
    }

    @Test
    void administratorCanDeleteStudentCourseEnrollment() throws Exception {
        enrollmentService.deleteCourseEnrollment(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                4L,
                30L,
                "127.0.0.1"
        );

        assertTrue(isCourseEnrollmentMissing(4L, 30L));
        assertTrue(isSubjectEnrollmentMissing(4L, 30L, 40L));
        assertTrue(isClassGroupEnrollmentMissing(4L, 50L));
    }

    @Test
    void activeCourseEnrollmentCountIncludesActiveRowsOutsideCurrentDateWindow() throws Exception {
        insertCourseEnrollment(4L, 31L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1));

        EnrollmentDAO enrollmentDAO = new EnrollmentDAO(DatabaseTestSupport::openConnection);

        assertEquals(1L, enrollmentDAO.countActiveCourseEnrollments(31L));
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

    private static void insertSubjectEnrollment(
            long studentUserId,
            long courseId,
            long subjectId,
            EnrollmentState state,
            LocalDate startDate,
            LocalDate endDate
    ) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO enroll_subject (id_student_user, id_course, id_subject, state, start_date, end_date)
                     VALUES (?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            statement.setLong(3, subjectId);
            statement.setString(4, state.toDatabaseValue());
            if (startDate == null) {
                statement.setNull(5, java.sql.Types.DATE);
            } else {
                statement.setDate(5, java.sql.Date.valueOf(startDate));
            }
            if (endDate == null) {
                statement.setNull(6, java.sql.Types.DATE);
            } else {
                statement.setDate(6, java.sql.Date.valueOf(endDate));
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

    private static boolean isSubjectEnrollmentMissing(long studentUserId, long courseId, long subjectId)
            throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM enroll_subject
                     WHERE id_student_user = ?
                       AND id_course = ?
                       AND id_subject = ?
                     """)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            statement.setLong(3, subjectId);
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

    private static String certificateState(long studentUserId, long courseId) {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT state
                     FROM certificate
                     WHERE id_user_student = ?
                       AND id_course = ?
                       AND state <> 'revoked'
                     """)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("state") : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load certificate state", exception);
        }
    }

    private static String subjectGradeSheetState(long subjectId) {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT gs.state
                     FROM grade_sheet gs
                     WHERE gs.id_subject = ?
                       AND NOT EXISTS (
                             SELECT 1
                             FROM associate_grade_sheet_class_group agscg
                             WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                       )
                     ORDER BY gs.id_grade_sheet DESC
                     LIMIT 1
                     """)) {
            statement.setLong(1, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("state") : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load subject grade sheet state", exception);
        }
    }
}
