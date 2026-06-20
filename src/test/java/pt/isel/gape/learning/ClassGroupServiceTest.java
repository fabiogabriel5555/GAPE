package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupCreateCommand;
import pt.isel.gape.learning.model.ClassGroupModality;
import pt.isel.gape.learning.model.ClassGroupShift;
import pt.isel.gape.learning.model.ClassGroupState;
import pt.isel.gape.learning.model.ClassGroupUpdateCommand;
import pt.isel.gape.learning.service.ClassGroupService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class ClassGroupServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private ClassGroupService classGroupService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        classGroupService = new ClassGroupService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void administratorCanCreateClassGroupWithPartialCapacityAndDates() {
        ClassGroup classGroup = classGroupService.createClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new ClassGroupCreateCommand(
                        41L,
                        31L,
                        "MAT-AD-T1",
                        ClassGroupModality.ONLINE,
                        ClassGroupState.ACTIVE,
                        null,
                        25,
                        LocalDate.of(2026, 3, 1),
                        null,
                        ClassGroupShift.AFTERNOON
                ),
                "127.0.0.1"
        );

        assertEquals(31L, classGroup.courseId());
        assertEquals(41L, classGroup.subjectId());
        assertNull(classGroup.minStudents());
        assertEquals(25, classGroup.maxStudents());
        assertFalse(classGroup.showContentThumbnails());
    }

    @Test
    void classGroupRequiresCourseSubjectAssociation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> classGroupService.createClassGroup(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new ClassGroupCreateCommand(
                                40L,
                                31L,
                                "PRJ-AD-T1",
                                ClassGroupModality.HYBRID,
                                ClassGroupState.ACTIVE,
                                5,
                                20,
                                LocalDate.of(2026, 3, 1),
                                LocalDate.of(2026, 6, 30),
                                ClassGroupShift.EVENING
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void minStudentsCannotExceedMaxStudents() {
        assertThrows(
                IllegalArgumentException.class,
                () -> classGroupService.createClassGroup(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new ClassGroupCreateCommand(
                                41L,
                                31L,
                                "MAT-AD-T2",
                                ClassGroupModality.ONLINE,
                                ClassGroupState.ACTIVE,
                                20,
                                10,
                                null,
                                null,
                                ClassGroupShift.MORNING
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void classGroupEndDateCannotBeBeforeStartDate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> classGroupService.createClassGroup(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new ClassGroupCreateCommand(
                                41L,
                                31L,
                                "MAT-AD-BAD-DATE",
                                ClassGroupModality.ONLINE,
                                ClassGroupState.ACTIVE,
                                null,
                                25,
                                LocalDate.of(2026, 6, 30),
                                LocalDate.of(2026, 3, 1),
                                ClassGroupShift.AFTERNOON
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void coordinatorCanUpdateClassGroupForCoordinatedSubject() {
        ClassGroup classGroup = classGroupService.updateClassGroup(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                50L,
                updatePrjCommand(ClassGroupModality.HYBRID, ClassGroupShift.EVENING),
                "127.0.0.1"
        );

        assertEquals(ClassGroupModality.HYBRID, classGroup.modality());
    }

    @Test
    void administratorCanChangeClassGroupCourseAndSubject() {
        ClassGroup classGroup = classGroupService.updateClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                50L,
                new ClassGroupUpdateCommand(
                        41L,
                        31L,
                        "PRJ-T1",
                        ClassGroupModality.HYBRID,
                        ClassGroupState.ACTIVE,
                        5,
                        30,
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 6, 30),
                        ClassGroupShift.EVENING,
                        false
                ),
                "127.0.0.1"
        );

        assertEquals(31L, classGroup.courseId());
        assertEquals(41L, classGroup.subjectId());
        assertEquals(ClassGroupModality.HYBRID, classGroup.modality());
    }

    @Test
    void nonAdministratorCannotChangeClassGroupCourseAndSubject() {
        assertThrows(
                SecurityException.class,
                () -> classGroupService.updateClassGroup(
                        2L,
                        null,
                        AccessProfileType.COORDINATOR,
                        50L,
                        new ClassGroupUpdateCommand(
                                41L,
                                31L,
                                "PRJ-T1",
                                ClassGroupModality.HYBRID,
                                ClassGroupState.ACTIVE,
                                5,
                                30,
                                LocalDate.of(2026, 2, 1),
                                LocalDate.of(2026, 6, 30),
                                ClassGroupShift.EVENING,
                                false
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void assignedTeacherCanUpdateClassGroupOperationalData() {
        ClassGroup classGroup = classGroupService.updateClassGroup(
                3L,
                null,
                AccessProfileType.TEACHER,
                50L,
                updatePrjCommand(ClassGroupModality.ONSITE, ClassGroupShift.MIXED),
                "127.0.0.1"
        );

        assertEquals(ClassGroupShift.MIXED, classGroup.shift());
    }

    @Test
    void classGroupUpdateCanEnableContentThumbnails() {
        ClassGroup classGroup = classGroupService.updateClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                50L,
                new ClassGroupUpdateCommand(
                        40L,
                        30L,
                        "PRJ-T1",
                        ClassGroupModality.ONSITE,
                        ClassGroupState.ACTIVE,
                        5,
                        30,
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 6, 30),
                        ClassGroupShift.EVENING,
                        true
                ),
                "127.0.0.1"
        );

        assertTrue(classGroup.showContentThumbnails());
    }

    @Test
    void teacherCannotArchiveClassGroup() {
        assertThrows(
                SecurityException.class,
                () -> classGroupService.archiveClassGroup(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        50L,
                        "127.0.0.1"
                )
        );
    }

    @Test
    void administratorCanAssignTeacherUsingClassGroupTeachingMechanism() throws Exception {
        classGroupService.assignTeacherToClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                52L,
                3L,
                LocalDate.of(2026, 2, 1),
                null,
                "127.0.0.1"
        );

        assertTrue(hasActiveTeacherAssignment(3L, 52L));
    }

    @Test
    void administratorCanRemoveTeacherFromClassGroup() throws Exception {
        classGroupService.assignTeacherToClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                52L,
                3L,
                LocalDate.of(2026, 2, 1),
                null,
                "127.0.0.1"
        );

        classGroupService.removeTeacherFromClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                52L,
                3L,
                LocalDate.of(2026, 4, 15),
                "127.0.0.1"
        );

        assertFalse(hasActiveTeacherAssignment(3L, 52L));
        assertEquals("inactive", teacherAssignmentState(3L, 52L));
        assertEquals(LocalDate.of(2026, 4, 15), teacherAssignmentEndDate(3L, 52L));
    }

    @Test
    void archivedClassGroupCannotBeUpdated() {
        classGroupService.archiveClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                50L,
                "127.0.0.1"
        );

        assertThrows(
                IllegalStateException.class,
                () -> classGroupService.updateClassGroup(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        50L,
                        updatePrjCommand(ClassGroupModality.ONSITE, ClassGroupShift.EVENING),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void administratorCanArchiveClassGroup() throws Exception {
        ClassGroup classGroup = classGroupService.createClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new ClassGroupCreateCommand(
                        41L,
                        31L,
                        "MAT-AD-ARCH",
                        ClassGroupModality.ONLINE,
                        ClassGroupState.ACTIVE,
                        null,
                        25,
                        LocalDate.of(2026, 3, 1),
                        null,
                        ClassGroupShift.AFTERNOON
                ),
                "127.0.0.1"
        );

        classGroupService.archiveClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                classGroup.id(),
                "127.0.0.1"
        );

        assertEquals("archived", classGroupState(classGroup.id()));
    }

    @Test
    void classGroupWithDependenciesCannotBeDeleted() {
        assertThrows(
                IllegalStateException.class,
                () -> classGroupService.deleteClassGroup(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        50L,
                        "127.0.0.1"
                )
        );
    }

    @Test
    void administratorCanDeleteClassGroupWithoutDependencies() throws Exception {
        ClassGroup classGroup = classGroupService.createClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new ClassGroupCreateCommand(
                        41L,
                        31L,
                        "MAT-AD-DEL",
                        ClassGroupModality.ONLINE,
                        ClassGroupState.ACTIVE,
                        null,
                        25,
                        LocalDate.of(2026, 3, 1),
                        null,
                        ClassGroupShift.AFTERNOON
                ),
                "127.0.0.1"
        );

        classGroupService.deleteClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                classGroup.id(),
                "127.0.0.1"
        );

        assertFalse(classGroupExists(classGroup.id()));
    }

    @Test
    void duplicateClassGroupCodeInSubjectIsRejected() {
        assertThrows(
                RuntimeException.class,
                () -> classGroupService.createClassGroup(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new ClassGroupCreateCommand(
                                40L,
                                30L,
                                "PRJ-T1",
                                ClassGroupModality.ONSITE,
                                ClassGroupState.ACTIVE,
                                5,
                                30,
                                LocalDate.of(2026, 2, 1),
                                LocalDate.of(2026, 6, 30),
                                ClassGroupShift.EVENING
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void coordinatorLearningListsClassGroupsByCourse() {
        Set<Long> classGroupIds = classGroupService.listClassGroupsByCourse(
                        2L,
                        null,
                        AccessProfileType.COORDINATOR,
                        30L,
                        "127.0.0.1"
                )
                .stream()
                .map(ClassGroup::id)
                .collect(Collectors.toSet());

        assertEquals(Set.of(50L), classGroupIds);
    }

    @Test
    void coordinatorLearningCanReadAndManageClassGroup() {
        assertTrue(classGroupService.canReadClassGroup(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                50L,
                "127.0.0.1"
        ));
        assertTrue(classGroupService.canManageClassGroupEnrollments(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                50L,
                "127.0.0.1"
        ));
        assertTrue(classGroupService.canModifyClassGroup(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                50L,
                "127.0.0.1"
        ));

        Set<Long> classGroupIds = classGroupService.listClassGroupsByCourse(
                        2L,
                        null,
                        AccessProfileType.COORDINATOR,
                        30L,
                        "127.0.0.1"
                )
                .stream()
                .map(ClassGroup::id)
                .collect(Collectors.toSet());

        assertEquals(Set.of(50L), classGroupIds);
    }

    private static ClassGroupUpdateCommand updatePrjCommand(ClassGroupModality modality, ClassGroupShift shift) {
        return new ClassGroupUpdateCommand(
                40L,
                30L,
                "PRJ-T1",
                modality,
                ClassGroupState.ACTIVE,
                5,
                30,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 6, 30),
                shift,
                false
        );
    }

    private static boolean hasActiveTeacherAssignment(long teacherUserId, long classGroupId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM teach_class_group
                     WHERE id_teacher_user = ?
                       AND id_class_group = ?
                       AND state = 'active'
                     """)) {
            statement.setLong(1, teacherUserId);
            statement.setLong(2, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    private static String teacherAssignmentState(long teacherUserId, long classGroupId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT state
                     FROM teach_class_group
                     WHERE id_teacher_user = ?
                       AND id_class_group = ?
                     """)) {
            statement.setLong(1, teacherUserId);
            statement.setLong(2, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getString("state");
            }
        }
    }

    private static LocalDate teacherAssignmentEndDate(long teacherUserId, long classGroupId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT end_date
                     FROM teach_class_group
                     WHERE id_teacher_user = ?
                       AND id_class_group = ?
                     """)) {
            statement.setLong(1, teacherUserId);
            statement.setLong(2, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getDate("end_date").toLocalDate();
            }
        }
    }

    private static String classGroupState(long classGroupId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT state
                     FROM class_group
                     WHERE id_class_group = ?
                     """)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getString("state");
            }
        }
    }

    private static boolean classGroupExists(long classGroupId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM class_group
                     WHERE id_class_group = ?
                     """)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    private void addAdministrator(
            long userId,
            String administratorCode,
            String permissionCode,
            String contextType,
            long contextId
    ) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement user = connection.prepareStatement("""
                    INSERT INTO user_account (
                        id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt
                    ) VALUES (?, ?, ?, 'active', 'pt-PT', NULL, '2026-01-01 10:00:00', 'hash', 'salt')
                    """)) {
                user.setLong(1, userId);
                user.setString(2, "Scoped Class Group Admin");
                user.setString(3, "scoped.class.admin@gape.local");
                user.executeUpdate();
            }
            try (PreparedStatement profile = connection.prepareStatement("""
                    INSERT INTO administrator_profile (id_user, cod_administrator)
                    VALUES (?, ?)
                    """)) {
                profile.setLong(1, userId);
                profile.setString(2, administratorCode);
                profile.executeUpdate();
            }
            try (PreparedStatement grant = connection.prepareStatement("""
                    INSERT INTO grant_administrator (id_admin_user, cod_permission, context_type, context_id)
                    VALUES (?, ?, ?, ?)
                    """)) {
                grant.setLong(1, userId);
                grant.setString(2, permissionCode);
                grant.setString(3, contextType);
                grant.setLong(4, contextId);
                grant.executeUpdate();
            }
        }
    }
}
