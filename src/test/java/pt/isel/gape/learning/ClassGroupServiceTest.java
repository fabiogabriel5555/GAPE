package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.List;
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
import pt.isel.gape.learning.dao.GradeSheetDAO;
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
    private static final LocalDate FUTURE_START = LocalDate.of(2026, 6, 10);
    private static final LocalDate FUTURE_END = LocalDate.of(2026, 6, 30);

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
    void administratorCanCreateClassGroupWithRequiredCapacityAndDates() {
        ClassGroup classGroup = classGroupService.createClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new ClassGroupCreateCommand(
                        41L,
                        31L,
                        "MAT-AD-T1",
                        ClassGroupModality.ONLINE,
                        ClassGroupState.SCHEDULED,
                        5,
                        25,
                        FUTURE_START,
                        FUTURE_END,
                        ClassGroupShift.AFTERNOON,
                        false
                ),
                "127.0.0.1"
        );

        assertEquals(31L, classGroup.courseId());
        assertEquals(41L, classGroup.subjectId());
        assertEquals(5, classGroup.minStudents());
        assertEquals(25, classGroup.maxStudents());
        assertFalse(classGroup.showContentThumbnails());
    }

    @Test
    void administratorCanCreateClassGroupWithThumbnailsEnabled() {
        ClassGroup classGroup = classGroupService.createClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new ClassGroupCreateCommand(
                        41L,
                        31L,
                        "MAT-AD-TN",
                        ClassGroupModality.ONLINE,
                        ClassGroupState.SCHEDULED,
                        5,
                        25,
                        FUTURE_START,
                        FUTURE_END,
                        ClassGroupShift.AFTERNOON,
                        true
                ),
                "127.0.0.1"
        );

        assertTrue(classGroup.showContentThumbnails());
    }

    @Test
    void classGroupCanBelongToOneCourseWhenTheSubjectAlsoHasADifferentCalendar() throws Exception {
        insertCourseWithAnnualCalendarForProjectSubject();

        ClassGroup semesterClassGroup = classGroupService.createClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new ClassGroupCreateCommand(
                        40L,
                        30L,
                        "PRJ-SINGLE-COURSE",
                        ClassGroupModality.ONSITE,
                        ClassGroupState.SCHEDULED,
                        5,
                        30,
                        FUTURE_START,
                        FUTURE_END,
                        ClassGroupShift.EVENING,
                        false
                ),
                "127.0.0.1"
        );

        ClassGroup annualClassGroup = classGroupService.createClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new ClassGroupCreateCommand(
                        40L,
                        90L,
                        "PRJ-ANNUAL-CONTEXT",
                        ClassGroupModality.ONSITE,
                        ClassGroupState.SCHEDULED,
                        5,
                        30,
                        FUTURE_START,
                        FUTURE_END,
                        ClassGroupShift.EVENING,
                        false
                ),
                "127.0.0.1"
        );
        ClassGroup secondAnnualClassGroup = classGroupService.createClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new ClassGroupCreateCommand(
                        40L,
                        90L,
                        "PRJ-ANNUAL-CONTEXT-2",
                        ClassGroupModality.ONLINE,
                        ClassGroupState.SCHEDULED,
                        5,
                        30,
                        FUTURE_START,
                        FUTURE_END,
                        ClassGroupShift.MORNING,
                        false
                ),
                "127.0.0.1"
        );

        assertEquals(30L, semesterClassGroup.courseId());
        assertEquals(300L, semesterClassGroup.courseOccurrenceId());
        assertEquals(3001L, semesterClassGroup.courseOccurrencePeriodId());
        assertEquals(90L, annualClassGroup.courseId());
        assertEquals(900L, annualClassGroup.courseOccurrenceId());
        assertEquals(9001L, annualClassGroup.courseOccurrencePeriodId());
        assertEquals(900L, secondAnnualClassGroup.courseOccurrenceId());
        assertEquals(9001L, secondAnnualClassGroup.courseOccurrencePeriodId());

        try (Connection connection = DatabaseTestSupport.openConnection()) {
            GradeSheetDAO gradeSheetDAO = new GradeSheetDAO(DatabaseTestSupport::openConnection);
            assertEquals(
                    List.of(),
                    gradeSheetDAO.findAssessmentIdsForSheetContext(connection, 40L, 900L, List.of())
            );
            assertEquals(1L, countGradeSheets(connection, 40L, 300L, "subject_occurrence"));
            assertEquals(1L, countGradeSheets(connection, 40L, 900L, "subject_occurrence"));
            assertEquals(1L, countFinalSourceSheets(connection, semesterClassGroup.id()));
            assertEquals(1L, countFinalSourceSheets(connection, annualClassGroup.id()));
            assertEquals(1L, countFinalSourceSheets(connection, secondAnnualClassGroup.id()));
            List<Long> assessmentSheets = gradeSheetDAO.findGradeSheetIdsForAssessmentContext(connection, 90L);
            assertTrue(assessmentSheets.contains(subjectOccurrenceGradeSheetId(connection, 40L, 300L)));
            assertFalse(assessmentSheets.contains(subjectOccurrenceGradeSheetId(connection, 40L, 900L)));
        }
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
                                ClassGroupState.SCHEDULED,
                                5,
                                20,
                                FUTURE_START,
                                FUTURE_END,
                                ClassGroupShift.EVENING,
                                false
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void databaseRejectsNewClassGroupForHistoricalCourseSubjectAssociation() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement closeAssociation = connection.prepareStatement("""
                     UPDATE integrate_subject
                     SET state = 'historical', ended_at = '2026-06-04'
                     WHERE id_course = 31 AND id_subject = 41
                     """)) {
            closeAssociation.executeUpdate();
            try (PreparedStatement classGroup = connection.prepareStatement("""
                    INSERT INTO class_group (
                        id_class_group, id_subject, id_course, id_course_occurrence, id_course_occurrence_period,
                        cod_class_group, modality, state, min_students, max_students, starts_at, ends_at, shift
                    ) VALUES (9103, 41, 31, 310, 3101, 'MAT-HISTORICAL', 'online', 'completed',
                              5, 25, '2026-01-01', '2026-06-30', 'morning')
                    """)) {
                SQLException exception = assertThrows(SQLException.class, classGroup::executeUpdate);
                DatabaseTestSupport.assertIntegrityException(exception);
            }
        }
    }

    @Test
    void maxStudentsMustBeGreaterThanMinStudents() {
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
                                ClassGroupState.SCHEDULED,
                                20,
                                20,
                                null,
                                null,
                                ClassGroupShift.MORNING,
                                false
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void classGroupCapacityRequiresPositiveMinimumAndBothBounds() {
        IllegalArgumentException missingMinimum = assertThrows(
                IllegalArgumentException.class,
                () -> classGroupService.createClassGroup(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new ClassGroupCreateCommand(
                                41L,
                                31L,
                                "MAT-AD-NO-MIN",
                                ClassGroupModality.ONLINE,
                                ClassGroupState.SCHEDULED,
                                null,
                                25,
                                FUTURE_START,
                                FUTURE_END,
                                ClassGroupShift.MORNING,
                                false
                        ),
                        "127.0.0.1"
                )
        );
        assertEquals("Class group minimum students is required", missingMinimum.getMessage());

        IllegalArgumentException invalidMinimum = assertThrows(
                IllegalArgumentException.class,
                () -> classGroupService.createClassGroup(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new ClassGroupCreateCommand(
                                41L,
                                31L,
                                "MAT-AD-ZERO-MIN",
                                ClassGroupModality.ONLINE,
                                ClassGroupState.SCHEDULED,
                                0,
                                25,
                                FUTURE_START,
                                FUTURE_END,
                                ClassGroupShift.MORNING,
                                false
                        ),
                        "127.0.0.1"
                )
        );
        assertEquals("Class group minimum students must be greater than zero", invalidMinimum.getMessage());
    }

    @Test
    void classGroupStartDateIsRequired() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> classGroupService.createClassGroup(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new ClassGroupCreateCommand(
                                41L,
                                31L,
                                "MAT-AD-NO-START",
                                ClassGroupModality.ONLINE,
                                ClassGroupState.SCHEDULED,
                                5,
                                25,
                                null,
                                FUTURE_END,
                                ClassGroupShift.AFTERNOON,
                                false
                        ),
                        "127.0.0.1"
                )
        );

        assertEquals("Class group start date is required", exception.getMessage());
    }

    @Test
    void classGroupEndDateIsRequired() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> classGroupService.createClassGroup(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new ClassGroupCreateCommand(
                                41L,
                                31L,
                                "MAT-AD-NO-END",
                                ClassGroupModality.ONLINE,
                                ClassGroupState.SCHEDULED,
                                5,
                                25,
                                FUTURE_START,
                                null,
                                ClassGroupShift.AFTERNOON,
                                false
                        ),
                        "127.0.0.1"
                )
        );

        assertEquals("Class group end date is required", exception.getMessage());
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
                                ClassGroupState.SCHEDULED,
                                5,
                                25,
                                LocalDate.of(2026, 6, 30),
                                FUTURE_START,
                                ClassGroupShift.AFTERNOON,
                                false
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
                        ClassGroupState.SCHEDULED,
                        5,
                        30,
                        FUTURE_START,
                        FUTURE_END,
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
                                ClassGroupState.SCHEDULED,
                                5,
                                30,
                                FUTURE_START,
                                FUTURE_END,
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
                        ClassGroupState.SCHEDULED,
                        5,
                        30,
                        FUTURE_START,
                        FUTURE_END,
                        ClassGroupShift.EVENING,
                        true
                ),
                "127.0.0.1"
        );

        assertTrue(classGroup.showContentThumbnails());
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
    void completedClassGroupCanBeUpdated() throws Exception {
        long classGroupId = 9104L;
        insertCompletedClassGroup(classGroupId, "MAT-PAST-EDIT");
        ClassGroup updated = classGroupService.updateClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                classGroupId,
                new ClassGroupUpdateCommand(
                        41L,
                        30L,
                        "MAT-PAST-EDIT",
                        ClassGroupModality.ONLINE,
                        ClassGroupState.COMPLETED,
                        1,
                        20,
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 6, 30),
                        ClassGroupShift.EVENING,
                        true
                ),
                "127.0.0.1"
        );
        assertEquals(ClassGroupShift.EVENING, updated.shift());
    }

    @Test
    void completedClassGroupWithPastDatesCanBeUpdated() throws Exception {
        long classGroupId = 9100L;
        insertCompletedClassGroup(classGroupId, "MAT-PAST-UPD");

        ClassGroup updated = classGroupService.updateClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                classGroupId,
                new ClassGroupUpdateCommand(
                        41L,
                        30L,
                        "MAT-PAST-UPD",
                        ClassGroupModality.ONLINE,
                        ClassGroupState.COMPLETED,
                        1,
                        20,
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 3, 1),
                        ClassGroupShift.EVENING,
                        true
                ),
                "127.0.0.1"
        );

        assertEquals(ClassGroupState.ACTIVE, updated.state());
        assertEquals(ClassGroupShift.EVENING, updated.shift());
        assertTrue(updated.showContentThumbnails());
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
    void completedClassGroupWithOnlyStructuralDependenciesCanBeDeleted() throws Exception {
        long classGroupId = 9101L;
        long contentBlockId = 9101L;
        insertCompletedClassGroup(classGroupId, "MAT-PAST-DEL");
        insertContentBlock(contentBlockId, classGroupId, "MAT-PAST-BLK");
        insertTeacherAssignment(3L, classGroupId);
        insertClassGroupContentAssociation(classGroupId, 70L);

        classGroupService.deleteClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                classGroupId,
                "127.0.0.1"
        );

        assertFalse(classGroupExists(classGroupId));
    }

    @Test
    void completedClassGroupWithAcademicHistoryCannotBeDeleted() throws Exception {
        long classGroupId = 9102L;
        long contentBlockId = 9102L;
        insertCompletedClassGroup(classGroupId, "MAT-PAST-HIST");
        insertContentBlock(contentBlockId, classGroupId, "MAT-HIST-BLK");
        insertCompletedOnlineLesson(9102L, classGroupId, contentBlockId);

        assertThrows(
                IllegalStateException.class,
                () -> classGroupService.deleteClassGroup(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        classGroupId,
                        "127.0.0.1"
                )
        );
        assertTrue(classGroupExists(classGroupId));
    }

    @Test
    void administratorCannotDeleteClassGroupWithAutomaticGradeSheetDependency() {
        ClassGroup classGroup = classGroupService.createClassGroup(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new ClassGroupCreateCommand(
                        41L,
                        31L,
                        "MAT-AD-DEL",
                        ClassGroupModality.ONLINE,
                        ClassGroupState.SCHEDULED,
                        5,
                        25,
                        FUTURE_START,
                        FUTURE_END,
                        ClassGroupShift.AFTERNOON,
                        false
                ),
                "127.0.0.1"
        );

        assertThrows(
                IllegalStateException.class,
                () -> classGroupService.deleteClassGroup(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        classGroup.id(),
                        "127.0.0.1"
                )
        );
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
                                ClassGroupState.SCHEDULED,
                                5,
                                30,
                                FUTURE_START,
                                FUTURE_END,
                                ClassGroupShift.EVENING,
                                false
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
                ClassGroupState.SCHEDULED,
                5,
                30,
                FUTURE_START,
                FUTURE_END,
                shift,
                false
        );
    }

    private static void insertCourseWithAnnualCalendarForProjectSubject() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement course = connection.prepareStatement("""
                     INSERT INTO course (
                         id_course, id_organization, id_organic_unit, name, acronym,
                         ects, duration, frequency, type, state
                     ) VALUES (90, 10, 20, 'Project Annual Track', 'PAT', 30.00, '1', 'annual', 'degree', 'active')
                     """)) {
            course.executeUpdate();
            try (PreparedStatement template = connection.prepareStatement("""
                    INSERT INTO course_period_template (
                        id_course, curricular_year, term, starts_month, starts_day, ends_month, ends_day
                    ) VALUES (90, 1, 'annual', 1, 1, 12, 31)
                    """)) {
                template.executeUpdate();
            }
            try (PreparedStatement occurrence = connection.prepareStatement("""
                    INSERT INTO course_occurrence (
                        id_course_occurrence, id_course, reference_year, label, starts_at, ends_at, state
                    ) VALUES (900, 90, 2026, '2026', '2026-01-01', '2026-12-31', 'active')
                    """)) {
                occurrence.executeUpdate();
            }
            try (PreparedStatement period = connection.prepareStatement("""
                    INSERT INTO course_occurrence_period (
                        id_course_occurrence_period, id_course_occurrence, curricular_year, term,
                        starts_at, ends_at, state
                    ) VALUES (9001, 900, 1, 'annual', '2026-01-01', '2026-12-31', 'active')
                    """)) {
                period.executeUpdate();
            }
            try (PreparedStatement association = connection.prepareStatement("""
                    INSERT INTO integrate_subject (
                        id_course, id_subject, curricular_year, term, mandatory, state, ended_at
                    ) VALUES (90, 40, 1, 'annual', 1, 'active', NULL)
                    """)) {
                association.executeUpdate();
            }
        }
    }

    private static void insertCompletedClassGroup(long classGroupId, String code) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO class_group (
                         id_class_group, id_subject, id_course, id_course_occurrence, id_course_occurrence_period,
                         cod_class_group, modality, state,
                         min_students, max_students, starts_at, ends_at, shift, show_content_thumbnails
                    ) VALUES (?, 41, 30, 299, 2991, ?, 'online', 'completed', 1, 20, '2025-01-01', '2025-06-30', 'morning', 0)
                     """)) {
            statement.setLong(1, classGroupId);
            statement.setString(2, code);
            statement.executeUpdate();
        }
    }

    private static long countGradeSheets(
            Connection connection,
            long subjectId,
            long courseOccurrenceId,
            String scope
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM grade_sheet
                WHERE id_subject = ?
                  AND id_course_occurrence = ?
                  AND scope = ?
                """)) {
            statement.setLong(1, subjectId);
            statement.setLong(2, courseOccurrenceId);
            statement.setString(3, scope);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static long countFinalSourceSheets(Connection connection, long classGroupId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM grade_sheet gs
                JOIN associate_grade_sheet_class_group agscg
                  ON agscg.id_grade_sheet = gs.id_grade_sheet
                WHERE agscg.id_class_group = ?
                  AND gs.scope = 'class_group'
                  AND gs.type = 'final'
                """)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static long subjectOccurrenceGradeSheetId(
            Connection connection,
            long subjectId,
            long courseOccurrenceId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_grade_sheet
                FROM grade_sheet
                WHERE id_subject = ?
                  AND id_course_occurrence = ?
                  AND scope = 'subject_occurrence'
                """)) {
            statement.setLong(1, subjectId);
            statement.setLong(2, courseOccurrenceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Subject occurrence grade sheet fixture was not found");
                }
                return resultSet.getLong(1);
            }
        }
    }

    private static void insertContentBlock(long contentBlockId, long classGroupId, String code) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO content_block (
                         id_content_block, id_class_group, cod_content_block, name, description, order_no, state
                     ) VALUES (?, ?, ?, 'Structural block', NULL, 1, 'active')
                     """)) {
            statement.setLong(1, contentBlockId);
            statement.setLong(2, classGroupId);
            statement.setString(3, code);
            statement.executeUpdate();
        }
    }

    private static void insertTeacherAssignment(long teacherUserId, long classGroupId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO teach_class_group (id_teacher_user, id_class_group, state, start_date, end_date)
                     VALUES (?, ?, 'inactive', '2026-02-01', '2026-03-01')
                     """)) {
            statement.setLong(1, teacherUserId);
            statement.setLong(2, classGroupId);
            statement.executeUpdate();
        }
    }

    private static void insertClassGroupContentAssociation(long classGroupId, long contentItemId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO associate_class_group_content (id_class_group, id_content_item, role)
                     VALUES (?, ?, 'support')
                     """)) {
            statement.setLong(1, classGroupId);
            statement.setLong(2, contentItemId);
            statement.executeUpdate();
        }
    }

    private static void insertCompletedOnlineLesson(
            long lessonId,
            long classGroupId,
            long contentBlockId
    ) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO lesson (
                         id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type,
                         provider, access_url, attendance_required, state, starts_at, ends_at
                     ) VALUES (?, ?, ?, NULL, 'Historical lesson', NULL, 'online',
                         'Teams', 'https://teams.example.test/history', 0, 'completed',
                         '2026-02-10 10:00:00', '2026-02-10 11:00:00')
                     """)) {
            statement.setLong(1, lessonId);
            statement.setLong(2, classGroupId);
            statement.setLong(3, contentBlockId);
            statement.executeUpdate();
        }
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
