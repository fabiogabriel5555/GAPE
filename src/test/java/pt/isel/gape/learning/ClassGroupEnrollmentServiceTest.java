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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        classGroupEnrollmentService = new ClassGroupEnrollmentService(connectionProvider, FIXED_CLOCK);
        enrollmentService = new EnrollmentService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void studentCanRequestSelfClassGroupEnrollmentWhenEnrolledInSubject() throws Exception {
        insertSubjectEnrollment41();

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
    }

    @Test
    void classGroupRequestIsAutoApprovedWhenPolicyIsAutoApprove() throws Exception {
        insertSubjectEnrollment41();
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
    void classGroupEnrollmentRequiresSubjectEnrollment() {
        assertThrows(
                IllegalStateException.class,
                () -> classGroupEnrollmentService.requestStudentInClassGroup(
                        4L,
                        null,
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
    void classGroupEnrollmentCannotExceedCapacity() throws Exception {
        insertSubjectEnrollment41();
        setClassGroupCapacityToZero(52L);

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
        insertSubjectEnrollment41();
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
    void studentCannotEnrollAnotherStudentInClassGroup() throws Exception {
        insertSubjectEnrollment41();

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
                LocalDate.of(2026, 5, 31),
                "127.0.0.1"
        );

        assertEquals(EnrollmentState.WITHDRAWN, enrollment.state());
        assertEquals("withdrawn", classGroupEnrollmentState(4L, 50L));
    }

    @Test
    void withdrawingSubjectWithdrawsActiveClassGroupEnrollments() throws Exception {
        enrollmentService.withdrawStudentFromSubject(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                4L,
                30L,
                40L,
                LocalDate.of(2026, 5, 31),
                "127.0.0.1"
        );

        assertEquals("withdrawn", classGroupEnrollmentState(4L, 50L));
    }

    private static void insertSubjectEnrollment41() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO enroll_subject (id_student_user, id_course, id_subject, state, start_date, end_date)
                     VALUES (4, 30, 41, 'active', '2026-02-01', NULL)
                     """)) {
            statement.executeUpdate();
        }
    }

    private static void insertPrjParallelClassGroup() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO class_group (
                         id_class_group, id_subject, id_course, cod_class_group, modality, state,
                         min_students, max_students, starts_at, ends_at, shift
                     ) VALUES (95, 40, 30, 'PRJ-T2', 'onsite', 'active', 0, 30,
                               '2026-02-01', '2026-06-30', 'afternoon')
                     """)) {
            statement.executeUpdate();
        }
    }

    private static void setClassGroupCapacityToZero(long classGroupId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE class_group
                     SET min_students = 0,
                         max_students = 0
                     WHERE id_class_group = ?
                     """)) {
            statement.setLong(1, classGroupId);
            statement.executeUpdate();
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
