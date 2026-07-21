package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.ClassGroupEnrollment;
import pt.isel.gape.learning.model.ClassGroupEnrollmentCommand;
import pt.isel.gape.learning.model.EnrollmentApprovalMode;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.service.ClassGroupEnrollmentService;
import pt.isel.gape.learning.service.EnrollmentService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class ClassGroupEnrollmentServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private ClassGroupEnrollmentService classGroupEnrollmentService;
    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() throws Exception {
        // The services under test commit their own units of work. Rebuilding
        // the base fixture before every case keeps those commits from leaking
        // into the next randomly ordered case in the full Maven suite.
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        classGroupEnrollmentService = new ClassGroupEnrollmentService(connectionProvider, FIXED_CLOCK);
        enrollmentService = new EnrollmentService(connectionProvider, FIXED_CLOCK);
    }

    @Test
    void studentCanRequestSelfClassGroupEnrollmentWhenEnrolledInCourseOccurrence() {
        ClassGroupEnrollment enrollment = classGroupEnrollmentService.requestStudentInClassGroup(
                4L,
                null,
                new ClassGroupEnrollmentCommand(
                        4L,
                        52L,
                        LocalDate.of(2026, 3, 1),
                        null
                ),
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.PENDING, enrollment.state());
        assertEquals(52L, enrollment.classGroupId());
        assertEquals(LocalDate.of(2026, 1, 1), enrollment.startDate());
        assertEquals(LocalDate.of(2026, 6, 30), enrollment.endDate());
    }

    @Test
    void classGroupEnrollmentPeriodIsDerivedEvenWhenTheCallerSuppliesOtherDates() {
        ClassGroupEnrollment enrollment = classGroupEnrollmentService.requestStudentInClassGroup(
                4L,
                null,
                new ClassGroupEnrollmentCommand(
                        4L,
                        52L,
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 3, 2)
                ),
                "127.0.0.1"
        );

        assertEquals(LocalDate.of(2026, 1, 1), enrollment.startDate());
        assertEquals(LocalDate.of(2026, 6, 30), enrollment.endDate());
    }

    @Test
    void approvalUsesTheClassGroupOccurrencePeriod() {
        classGroupEnrollmentService.requestStudentInClassGroup(
                4L,
                null,
                new ClassGroupEnrollmentCommand(
                        4L,
                        52L,
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 3, 2)
                ),
                "127.0.0.1"
        );

        ClassGroupEnrollment enrollment = classGroupEnrollmentService.approveClassGroupEnrollment(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                4L,
                52L,
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.ACTIVE, enrollment.state());
        assertEquals(LocalDate.of(2026, 1, 1), enrollment.startDate());
        assertEquals(LocalDate.of(2026, 6, 30), enrollment.endDate());
    }

    @Test
    void classGroupRequestIsAutoApprovedWhenPolicyIsAutoApprove() throws Exception {
        classGroupEnrollmentService.updateClassGroupEnrollmentPolicy(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                52L,
                EnrollmentApprovalMode.AUTO_APPROVE,
                "127.0.0.1"
        );

        ClassGroupEnrollment enrollment = classGroupEnrollmentService.requestStudentInClassGroup(
                4L,
                null,
                new ClassGroupEnrollmentCommand(
                        4L,
                        52L,
                        LocalDate.of(2026, 3, 1),
                        null
                ),
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.ACTIVE, enrollment.state());
        assertEquals(52L, enrollment.classGroupId());
        assertEquals("draft", classGroupGradeSheetState(52L));
    }

    @Test
    void classGroupEnrollmentRequiresCourseOccurrenceEnrollment() throws Exception {
        insertDataAnalysisClassGroup();

        assertThrows(
                IllegalStateException.class,
                () -> classGroupEnrollmentService.requestStudentInClassGroup(
                        4L,
                        null,
                        new ClassGroupEnrollmentCommand(
                                4L,
                                96L,
                                LocalDate.of(2026, 3, 1),
                                null
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void classGroupEnrollmentCannotExceedCapacity() throws Exception {
        fillClassGroupToCapacity(52L);

        assertThrows(
                IllegalStateException.class,
                () -> classGroupEnrollmentService.enrollStudentInClassGroup(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new ClassGroupEnrollmentCommand(
                                4L,
                                52L,
                                LocalDate.of(2026, 3, 1),
                                null
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void assignedTeacherCanEnrollStudentInClassGroup() throws Exception {
        assignTeacherToClassGroup52();

        ClassGroupEnrollment enrollment = classGroupEnrollmentService.enrollStudentInClassGroup(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ClassGroupEnrollmentCommand(
                        4L,
                        52L,
                        LocalDate.of(2026, 3, 1),
                        null
                ),
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.ACTIVE, enrollment.state());
    }

    @Test
    void duplicateClassGroupEnrollmentIsRejected() {
        assertThrows(
                IllegalStateException.class,
                () -> classGroupEnrollmentService.requestStudentInClassGroup(
                        4L,
                        null,
                        new ClassGroupEnrollmentCommand(
                                4L,
                                50L,
                                LocalDate.of(2026, 3, 1),
                                null
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void studentCanRequestAnotherClassGroupInSameCourseSubject() throws Exception {
        insertPrjParallelClassGroup();

        ClassGroupEnrollment enrollment = classGroupEnrollmentService.requestStudentInClassGroup(
                4L,
                null,
                new ClassGroupEnrollmentCommand(
                        4L,
                        95L,
                        LocalDate.of(2026, 3, 1),
                        null
                ),
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.PENDING, enrollment.state());
        assertEquals(95L, enrollment.classGroupId());
    }

    @Test
    void studentCanBeActivelyEnrolledInAnotherClassGroupInSameCourseOccurrence() throws Exception {
        insertPrjParallelClassGroup();

        ClassGroupEnrollment enrollment = classGroupEnrollmentService.enrollStudentInClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new ClassGroupEnrollmentCommand(
                        4L,
                        95L,
                        LocalDate.of(2026, 3, 1),
                        null
                ),
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.ACTIVE, enrollment.state());
        assertEquals(95L, enrollment.classGroupId());
        assertEquals("active", classGroupEnrollmentState(4L, 50L));
    }

    @Test
    void studentCannotEnrollAnotherStudentInClassGroup() throws Exception {
        assertThrows(
                SecurityException.class,
                () -> classGroupEnrollmentService.requestStudentInClassGroup(
                        4L,
                        null,
                        new ClassGroupEnrollmentCommand(
                                5L,
                                52L,
                                LocalDate.of(2026, 3, 1),
                                null
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void studentCanWithdrawSelfFromClassGroup() throws Exception {
        ClassGroupEnrollment enrollment = classGroupEnrollmentService.withdrawStudentFromClassGroup(
                4L,
                null,
                AccessProfileType.STUDENT,
                4L,
                50L,
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.WITHDRAWN, enrollment.state());
        assertEquals("withdrawn", classGroupEnrollmentState(4L, 50L));
        assertEquals(LocalDate.of(2026, 6, 4), enrollment.endDate());
    }

    @Test
    void databaseGuardDerivesTheClassGroupOccurrencePeriod() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO enroll_class_group (
                         id_student_user, id_class_group, state, start_date, end_date
                     ) VALUES (4, 52, 'pending', '2026-03-01', '2026-03-02')
                     """)) {
            statement.executeUpdate();
        }

        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT start_date, end_date
                     FROM enroll_class_group
                     WHERE id_student_user = 4 AND id_class_group = 52
                     """)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                assertEquals(LocalDate.of(2026, 1, 1), resultSet.getDate("start_date").toLocalDate());
                assertEquals(LocalDate.of(2026, 6, 30), resultSet.getDate("end_date").toLocalDate());
            }
        }
    }

    @Test
    void withdrawingCourseWithdrawsActiveClassGroupEnrollments() throws Exception {
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

    private static void insertPrjParallelClassGroup() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO class_group (
                         id_class_group, id_subject, id_course, id_course_occurrence, id_course_occurrence_period,
                         cod_class_group, modality, state,
                         min_students, max_students, starts_at, ends_at, shift
                      ) VALUES (95, 40, 30, 300, 3001, 'PRJ-T2', 'onsite', 'active', 1, 30,
                               '2026-01-01', '2026-06-30', 'afternoon')
                     """)) {
            statement.executeUpdate();
        }
    }

    private static void insertDataAnalysisClassGroup() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO class_group (
                         id_class_group, id_subject, id_course, id_course_occurrence, id_course_occurrence_period,
                         cod_class_group, modality, state,
                         min_students, max_students, starts_at, ends_at, shift
                      ) VALUES (96, 41, 31, 310, 3101, 'MAT-AD-T1', 'online', 'active', 1, 30,
                               '2026-01-01', '2026-06-30', 'morning')
                     """)) {
            statement.executeUpdate();
        }
    }

    private static void fillClassGroupToCapacity(long classGroupId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                      UPDATE class_group
                      SET min_students = 1,
                          max_students = 2
                      WHERE id_class_group = ?
                      """)) {
            statement.setLong(1, classGroupId);
            statement.executeUpdate();
        }
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE user_account
                     SET state = 'active'
                     WHERE id_user = 5
                     """)) {
            statement.executeUpdate();
        }
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement courseEnrollment = connection.prepareStatement("""
                     INSERT INTO enroll_course (
                         id_student_user, id_course, id_course_occurrence, state, start_date, end_date
                     ) VALUES (5, 30, 300, 'active', '2026-01-01', '2026-12-31')
                     ON DUPLICATE KEY UPDATE state = 'active', start_date = VALUES(start_date), end_date = VALUES(end_date)
                     """)) {
            courseEnrollment.executeUpdate();
        }
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement classGroupEnrollment = connection.prepareStatement("""
                     INSERT INTO enroll_class_group (
                         id_student_user, id_class_group, state, start_date, end_date
                     ) VALUES
                         (4, ?, 'active', '2026-01-01', '2026-06-30'),
                         (5, ?, 'active', '2026-01-01', '2026-06-30')
                     """)) {
            classGroupEnrollment.setLong(1, classGroupId);
            classGroupEnrollment.setLong(2, classGroupId);
            classGroupEnrollment.executeUpdate();
        }
    }

    private static void assignTeacherToClassGroup52() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO teach_class_group (id_teacher_user, id_class_group, state, start_date, end_date)
                     VALUES (3, 52, 'active', '2026-02-01', NULL)
                     """)) {
            statement.executeUpdate();
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
                resultSet.next();
                return resultSet.getString("state");
            }
        }
    }

    private static String classGroupGradeSheetState(long classGroupId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT gs.state
                     FROM grade_sheet gs
                     JOIN associate_grade_sheet_class_group agscg
                       ON agscg.id_grade_sheet = gs.id_grade_sheet
                     WHERE agscg.id_class_group = ?
                     ORDER BY gs.id_grade_sheet DESC
                     LIMIT 1
                     """)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("state") : null;
            }
        }
    }
}
