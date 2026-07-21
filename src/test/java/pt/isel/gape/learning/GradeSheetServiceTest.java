package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
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
import pt.isel.gape.learning.model.GradeAssessmentWeight;
import pt.isel.gape.learning.model.GradeSheet;
import pt.isel.gape.learning.model.GradeSheetCreateCommand;
import pt.isel.gape.learning.model.GradeSheetState;
import pt.isel.gape.learning.model.GradeSheetType;
import pt.isel.gape.learning.model.GradeSheetUpdateCommand;
import pt.isel.gape.learning.service.GradeSheetService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class GradeSheetServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-11T10:15:30Z"), ZoneOffset.UTC);
    private static final String IP = "127.0.0.1";

    private GradeSheetService gradeSheetService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        gradeSheetService = new GradeSheetService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void teacherCreatesValidGradeSheetForManagedClassGroup() {
        GradeSheet gradeSheet = gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                validCreateCommand("Project Grade Sheet"),
                IP
        );

        assertEquals(40L, gradeSheet.subjectId());
        assertEquals(GradeSheetState.DRAFT, gradeSheet.state());
        assertEquals(List.of(50L), gradeSheet.classGroupIds());
        assertEquals(2, gradeSheet.assessmentWeights().size());
        assertEquals(new GradeAssessmentWeight(90L, bd("50.00")), gradeSheet.assessmentWeights().get(0));
        assertEquals(new GradeAssessmentWeight(92L, bd("50.00")), gradeSheet.assessmentWeights().get(1));
        assertNull(gradeSheet.weightAlert());
    }

    @Test
    void coordinatorAndAdministratorCreateGradeSheetsInContext() {
        GradeSheet coordinatorSheet = gradeSheetService.createGradeSheet(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                validCreateCommand("Coordinator Grade Sheet"),
                IP
        );
        GradeSheet administratorSheet = gradeSheetService.createGradeSheet(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                validCreateCommand("Administrator Grade Sheet"),
                IP
        );

        assertEquals(40L, coordinatorSheet.subjectId());
        assertEquals(40L, administratorSheet.subjectId());
    }

    @Test
    void assessmentWeightsAboveOneHundredAreSavedWithWarning() {
        GradeSheetCreateCommand command = new GradeSheetCreateCommand(
                40L,
                "Invalid Weights",
                GradeSheetType.PARTIAL,
                bd("20.00"),
                bd("9.50"),
                GradeSheetState.DRAFT,
                List.of(50L),
                List.of(
                        new GradeAssessmentWeight(90L, bd("80.00")),
                        new GradeAssessmentWeight(92L, bd("30.00"))
                )
        );

        GradeSheet gradeSheet = gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                command,
                IP
        );

        assertEquals(List.of(
                new GradeAssessmentWeight(90L, bd("80.00")),
                new GradeAssessmentWeight(92L, bd("30.00"))
        ), gradeSheet.assessmentWeights());
        assertTrue(gradeSheet.weightAlert().contains("total 110"));
    }

    @Test
    void publishedGradeSheetAllowsWeightUpdates() {
        GradeSheet gradeSheet = gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                validCreateCommand("Publish Grade Sheet"),
                IP
        );
        completeBaseGradeSheet(gradeSheet);
        GradeSheet published = gradeSheetService.publishGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                gradeSheet.id(),
                IP
        );

        assertEquals(GradeSheetState.PUBLISHED, published.state());
        assertNotNull(published.releasedAt());

        GradeSheetUpdateCommand update = new GradeSheetUpdateCommand(
                40L,
                "Changed After Publish",
                GradeSheetType.PARTIAL,
                bd("20.00"),
                bd("9.50"),
                List.of(50L),
                List.of(
                        new GradeAssessmentWeight(90L, bd("70.00")),
                        new GradeAssessmentWeight(92L, bd("30.00"))
                )
        );

        GradeSheet updated = gradeSheetService.updateGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                gradeSheet.id(),
                update,
                IP
        );

        assertEquals(GradeSheetState.PUBLISHED, updated.state());
        assertEquals(List.of(
                new GradeAssessmentWeight(90L, bd("70.00")),
                new GradeAssessmentWeight(92L, bd("30.00"))
        ), updated.assessmentWeights());
    }

    @Test
    void manualWeightUpdatesOutsideOneHundredAreSavedWithWarning() {
        GradeSheet gradeSheet = gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                validCreateCommand("Invalid Weight Update"),
                IP
        );
        GradeSheetUpdateCommand update = new GradeSheetUpdateCommand(
                40L,
                "Invalid Weight Update",
                GradeSheetType.PARTIAL,
                bd("20.00"),
                bd("9.50"),
                List.of(50L),
                List.of(
                        new GradeAssessmentWeight(90L, bd("80.00")),
                        new GradeAssessmentWeight(92L, bd("30.00"))
                )
        );

        GradeSheet updated = gradeSheetService.updateGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                gradeSheet.id(),
                update,
                IP
        );

        assertEquals(List.of(
                new GradeAssessmentWeight(90L, bd("80.00")),
                new GradeAssessmentWeight(92L, bd("30.00"))
        ), updated.assessmentWeights());
        assertTrue(updated.weightAlert().contains("total 110"));
    }

    @Test
    void manualWeightUpdatesMustIncludeEveryClassAssessment() {
        GradeSheet gradeSheet = gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                validCreateCommand("Missing Weight Update"),
                IP
        );
        GradeSheetUpdateCommand update = new GradeSheetUpdateCommand(
                40L,
                "Missing Weight Update",
                GradeSheetType.PARTIAL,
                bd("20.00"),
                bd("9.50"),
                List.of(50L),
                List.of(new GradeAssessmentWeight(90L, bd("100.00")))
        );

        assertThrows(IllegalArgumentException.class, () -> gradeSheetService.updateGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                gradeSheet.id(),
                update,
                IP
        ));
    }

    @Test
    void gradeSheetWithoutManualWeightsUsesAbsoluteAverageAndCanBePublished() {
        GradeSheetCreateCommand command = new GradeSheetCreateCommand(
                40L,
                "Absolute Average Sheet",
                GradeSheetType.PARTIAL,
                bd("20.00"),
                bd("9.50"),
                GradeSheetState.DRAFT,
                List.of(50L),
                List.of()
        );

        GradeSheet gradeSheet = gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                command,
                IP
        );
        completeBaseGradeSheet(gradeSheet);
        GradeSheet published = gradeSheetService.publishGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                gradeSheet.id(),
                IP
        );

        assertEquals(List.of(
                new GradeAssessmentWeight(90L, bd("50.00")),
                new GradeAssessmentWeight(92L, bd("50.00"))
        ), gradeSheet.assessmentWeights());
        assertNull(gradeSheet.weightAlert());
        assertEquals(GradeSheetState.PUBLISHED, published.state());
    }

    @Test
    void zeroWeightedAssessmentDoesNotBlockPublication() {
        GradeSheet gradeSheet = gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                new GradeSheetCreateCommand(
                        40L,
                        "Zero Weighted Assessment",
                        GradeSheetType.PARTIAL,
                        bd("20.00"),
                        bd("9.50"),
                        GradeSheetState.DRAFT,
                        List.of(50L),
                        List.of(
                                new GradeAssessmentWeight(90L, bd("100.00")),
                                new GradeAssessmentWeight(92L, bd("0.00"))
                        )
                ),
                IP
        );

        assertEquals(GradeSheetState.PUBLISHED, gradeSheet.state());
    }

    @Test
    void zeroScoreCountsAsACompletedPositiveWeightGrade() {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE attempt
                     SET score = 0.00, state = 'corrected'
                     WHERE id_attempt = 120
                     """)) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to prepare zero-score fixture", exception);
        }

        GradeSheet gradeSheet = gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                new GradeSheetCreateCommand(
                        40L,
                        "Zero Score Assessment",
                        GradeSheetType.PARTIAL,
                        bd("20.00"),
                        bd("9.50"),
                        GradeSheetState.DRAFT,
                        List.of(50L),
                        List.of(
                                new GradeAssessmentWeight(90L, bd("100.00")),
                                new GradeAssessmentWeight(92L, bd("0.00"))
                        )
                ),
                IP
        );

        assertEquals(GradeSheetState.PUBLISHED, gradeSheet.state());
    }

    @Test
    void publishedGradeSheetReturnsToDraftWhenPositiveWeightGradeIsRemoved() {
        GradeSheet gradeSheet = gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                new GradeSheetCreateCommand(
                        40L,
                        "Dynamic Publication",
                        GradeSheetType.PARTIAL,
                        bd("20.00"),
                        bd("9.50"),
                        GradeSheetState.DRAFT,
                        List.of(50L),
                        List.of(
                                new GradeAssessmentWeight(90L, bd("100.00")),
                                new GradeAssessmentWeight(92L, bd("0.00"))
                        )
                ),
                IP
        );
        assertEquals(GradeSheetState.PUBLISHED, gradeSheet.state());

        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE attempt
                     SET score = NULL, state = 'submitted'
                     WHERE id_attempt = 120
                     """)) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to remove positive-weight score", exception);
        }

        GradeSheet updated = gradeSheetService.updateGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                gradeSheet.id(),
                new GradeSheetUpdateCommand(
                        40L,
                        "Dynamic Publication",
                        GradeSheetType.PARTIAL,
                        bd("20.00"),
                        bd("9.50"),
                        List.of(50L),
                        List.of(
                                new GradeAssessmentWeight(90L, bd("100.00")),
                                new GradeAssessmentWeight(92L, bd("0.00"))
                        )
                ),
                IP
        );

        assertEquals(GradeSheetState.DRAFT, updated.state());
    }

    @Test
    void incompleteGradeSheetCannotBePublished() {
        GradeSheet gradeSheet = gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                validCreateCommand("Incomplete Publish Block"),
                IP
        );

        assertThrows(IllegalStateException.class, () -> gradeSheetService.publishGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                gradeSheet.id(),
                IP
        ));
    }

    @Test
    void publishedSheetWithMissingGradesReturnsToDraftOnRead() {
        GradeSheet gradeSheet = gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                validCreateCommand("Stale Published Incomplete Sheet"),
                IP
        );
        forcePublishedState(gradeSheet.id());
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE attempt
                     SET score = NULL, state = 'submitted'
                     WHERE id_attempt = 120
                     """)) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to remove grade fixture", exception);
        }

        GradeSheet synchronizedSheet = gradeSheetService.getGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                gradeSheet.id(),
                IP
        );

        assertEquals(GradeSheetState.DRAFT, synchronizedSheet.state());
    }

    @Test
    void completedPeriodPublishesIncompleteGradeSheetWithPendingExplanation() throws Exception {
        GradeSheetService completedPeriodService = new GradeSheetService(
                DatabaseTestSupport::openConnection,
                Clock.fixed(Instant.parse("2026-08-01T10:15:30Z"), ZoneOffset.UTC)
        );
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            completedPeriodService.synchronizeCompletedPeriodPublications(connection);
        }

        GradeSheet published = completedPeriodService.getGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                170L,
                IP
        );

        assertEquals(GradeSheetState.PUBLISHED, published.state());
        assertNotNull(published.releasedAt());
        assertNotNull(published.publicationExplanation());
        assertTrue(published.publicationExplanation().contains("associated course occurrence period is completed"));
        assertTrue(published.publicationExplanation().contains("Missing grade values are displayed as '-'"));
    }

    @Test
    void subjectGradeSheetReturnsToDraftWhenAnotherClassGroupSheetIsStillPending() {
        GradeSheet classGroupSheet = gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                finalClassGroupCommand(50L, "Project T1 Final Grade Sheet"),
                IP
        );
        long subjectGradeSheetId = subjectGradeSheetId(40L, 300L);

        assertEquals(GradeSheetState.DRAFT, gradeSheetState(subjectGradeSheetId));

        completeBaseGradeSheet(classGroupSheet);
        GradeSheet publishedClassGroupSheet = gradeSheetService.publishGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                classGroupSheet.id(),
                IP
        );
        GradeSheet consolidatedSubjectSheet = gradeSheetService.getGradeSheet(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                subjectGradeSheetId,
                IP
        );

        assertEquals(GradeSheetState.PUBLISHED, publishedClassGroupSheet.state());
        assertEquals(GradeSheetState.PUBLISHED, consolidatedSubjectSheet.state());
        assertEquals(List.of(), consolidatedSubjectSheet.assessmentWeights());
        assertEquals(classGroupFinalGrade(classGroupSheet.id(), 4L), classGroupFinalGrade(subjectGradeSheetId, 4L));

        insertSecondProjectClassGroup();
        gradeSheetService.createGradeSheet(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                finalClassGroupCommand(53L, "Project T2 Final Grade Sheet"),
                IP
        );

        assertEquals(GradeSheetState.DRAFT, gradeSheetState(subjectGradeSheetId));
    }

    @Test
    void subjectGradeSheetKeepsTheHighestClassificationWhenStudentHasMultipleClassGroups() {
        publishBaseProjectGradeRecord();
        insertSecondProjectClassGroup();
        insertSecondProjectExamFixture();

        GradeSheet secondClassGroupSheet = gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                new GradeSheetCreateCommand(
                        40L,
                        "Project T2 Highest Classification",
                        GradeSheetType.FINAL,
                        bd("20.00"),
                        bd("9.50"),
                        GradeSheetState.DRAFT,
                        List.of(53L),
                        List.of(new GradeAssessmentWeight(950L, bd("100.00")))
                ),
                IP
        );

        assertEquals(GradeSheetState.PUBLISHED, secondClassGroupSheet.state());
        GradeSheet subjectSheet = gradeSheetService.getGradeSheet(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                subjectGradeSheetId(40L, 300L),
                IP
        );

        assertEquals(GradeSheetState.PUBLISHED, subjectSheet.state());
        assertEquals(bd("16.00"), classGroupFinalGrade(subjectSheet.id(), 4L));
    }

    @Test
    void teacherCannotCreateGradeSheetForUnmanagedClassGroup() {
        GradeSheetCreateCommand command = new GradeSheetCreateCommand(
                41L,
                "Unauthorized Mathematics Sheet",
                GradeSheetType.PARTIAL,
                bd("20.00"),
                bd("10.00"),
                GradeSheetState.DRAFT,
                List.of(52L),
                List.of(new GradeAssessmentWeight(91L, bd("100.00")))
        );

        assertThrows(SecurityException.class, () -> gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                command,
                IP
        ));
    }

    @Test
    void studentCannotReadGradeSheetDefinitionsDirectly() {
        assertThrows(SecurityException.class, () -> gradeSheetService.getGradeSheet(
                4L,
                null,
                AccessProfileType.STUDENT,
                170L,
                IP
        ));
    }

    private static GradeSheetCreateCommand validCreateCommand(String title) {
        return new GradeSheetCreateCommand(
                40L,
                title,
                GradeSheetType.PARTIAL,
                bd("20.00"),
                bd("9.50"),
                GradeSheetState.DRAFT,
                List.of(50L),
                List.of(new GradeAssessmentWeight(90L, bd("100.00")))
        );
    }

    private static GradeSheetCreateCommand finalClassGroupCommand(long classGroupId, String title) {
        return new GradeSheetCreateCommand(
                40L,
                title,
                GradeSheetType.FINAL,
                bd("20.00"),
                bd("9.50"),
                GradeSheetState.DRAFT,
                List.of(classGroupId),
                List.of(
                        new GradeAssessmentWeight(90L, bd("50.00")),
                        new GradeAssessmentWeight(92L, bd("50.00"))
                )
        );
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static void completeBaseGradeSheet(GradeSheet gradeSheet) {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE assessment
                    SET state = 'active'
                    WHERE id_assessment = 92
                    """)) {
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO enroll_assessment (id_student_user, id_assessment, state)
                    VALUES (4, 92, 'active')
                    ON DUPLICATE KEY UPDATE state = 'active'
                    """)) {
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE attempt
                    SET score = 10.00,
                        state = 'corrected',
                        submitted_at = '2026-02-11 10:10:00'
                    WHERE id_attempt = 120
                    """)) {
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO attempt (
                        id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
                    ) VALUES (4, 92, 1, 12.00, 'corrected', '2026-06-20 09:00:00', '2026-06-20 10:30:00')
                    """)) {
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO grade_record (
                        id_grade_sheet, id_user_student, id_attempt, cod_grade_record, value, result, recorded_at, notes
                    ) VALUES (?, 4, NULL, ?, 12.00, 'approved', '2026-06-20 11:00:00', 'Complete grade sheet fixture')
                    """)) {
                statement.setLong(1, gradeSheet.id());
                statement.setString(2, "AUTO-" + gradeSheet.id() + "-4");
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to complete grade sheet fixture", exception);
        }
    }

    private static void forcePublishedState(long gradeSheetId) {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE grade_sheet
                     SET state = 'published',
                         released_at = '2026-02-11 10:15:30'
                     WHERE id_grade_sheet = ?
                     """)) {
            statement.setLong(1, gradeSheetId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to force published grade sheet fixture", exception);
        }
    }

    private static long subjectGradeSheetId(long subjectId, long courseOccurrenceId) {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT gs.id_grade_sheet
                     FROM grade_sheet gs
                     WHERE gs.id_subject = ?
                       AND gs.id_course_occurrence = ?
                       AND NOT EXISTS (
                             SELECT 1
                             FROM associate_grade_sheet_class_group agscg
                             WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                       )
                     ORDER BY gs.id_grade_sheet
                     LIMIT 1
                     """)) {
            statement.setLong(1, subjectId);
            statement.setLong(2, courseOccurrenceId);
            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Subject grade sheet fixture was not found");
                }
                return resultSet.getLong(1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load subject grade sheet fixture", exception);
        }
    }

    private static GradeSheetState gradeSheetState(long gradeSheetId) {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT state FROM grade_sheet WHERE id_grade_sheet = ?")) {
            statement.setLong(1, gradeSheetId);
            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Grade sheet fixture was not found");
                }
                return GradeSheetState.fromDatabaseValue(resultSet.getString(1));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load grade sheet state", exception);
        }
    }

    private static BigDecimal classGroupFinalGrade(long gradeSheetId, long studentUserId) {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT value
                     FROM grade_record
                     WHERE id_grade_sheet = ?
                       AND id_user_student = ?
                       AND state = 'published'
                     ORDER BY recorded_at DESC, id_grade_record DESC
                     LIMIT 1
                     """)) {
            statement.setLong(1, gradeSheetId);
            statement.setLong(2, studentUserId);
            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Published grade record fixture was not found");
                }
                return resultSet.getBigDecimal(1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load published grade record", exception);
        }
    }

    private static void insertSecondProjectClassGroup() {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO class_group (
                         id_class_group, id_subject, id_course, id_course_occurrence, id_course_occurrence_period,
                         cod_class_group, modality, state, min_students, max_students, starts_at, ends_at, shift
                     ) VALUES (53, 40, 30, 300, 3001, 'PRJ-T2', 'onsite', 'active', 5, 30,
                               '2026-01-01', '2026-06-30', 'morning')
                     """)) {
            statement.executeUpdate();
            try (PreparedStatement teaching = connection.prepareStatement("""
                    INSERT INTO teach_class_group (id_teacher_user, id_class_group, state, start_date, end_date)
                    VALUES (3, 53, 'active', '2026-01-01', NULL)
                    """)) {
                teaching.executeUpdate();
            }
            try (PreparedStatement enrollment = connection.prepareStatement("""
                    INSERT INTO enroll_class_group (id_student_user, id_class_group, state, start_date, end_date)
                    VALUES (4, 53, 'active', '2026-01-01', '2026-06-30')
                    """)) {
                enrollment.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create second class group fixture", exception);
        }
    }

    private static void publishBaseProjectGradeRecord() {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE grade_record
                     SET cod_grade_record = 'AUTO-170-4',
                         value = 12.00,
                         result = 'approved',
                         state = 'published'
                     WHERE id_grade_sheet = 170
                       AND id_user_student = 4
                     """)) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to publish base project grade record fixture", exception);
        }
    }

    private static void insertSecondProjectExamFixture() {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement assessment = connection.prepareStatement("""
                    INSERT INTO assessment (
                        id_assessment, id_subject, id_content_block, cod_physical_room, title, description,
                        type, mode, correction_mode, max_grade, passing_grade, final_grade_weight,
                        attempts_limit, state, available_from, available_until
                    ) VALUES (950, 40, NULL, NULL, 'Project T2 Exam', 'Higher classification fixture',
                              'exam', 'online', 'manual', 20.00, 9.50, 100.00, 1,
                              'active', '2026-02-10 00:00:00', '2026-06-30 23:59:59')
                    """)) {
                assessment.executeUpdate();
            }
            try (PreparedStatement association = connection.prepareStatement("""
                    INSERT INTO assessment_class_group (id_assessment, id_class_group)
                    VALUES (950, 53)
                    """)) {
                association.executeUpdate();
            }
            try (PreparedStatement enrollment = connection.prepareStatement("""
                    INSERT INTO enroll_assessment (id_student_user, id_assessment, state, start_date, end_date)
                    VALUES (4, 950, 'active', '2026-02-10', '2026-06-30')
                    """)) {
                enrollment.executeUpdate();
            }
            try (PreparedStatement attempt = connection.prepareStatement("""
                    INSERT INTO attempt (
                        id_attempt, id_student_user, id_assessment, attempt_number, score, state,
                        started_at, submitted_at
                    ) VALUES (951, 4, 950, 1, 16.00, 'corrected',
                              '2026-02-11 09:00:00', '2026-02-11 10:00:00')
                    """)) {
                attempt.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create higher classification fixture", exception);
        }
    }
}
