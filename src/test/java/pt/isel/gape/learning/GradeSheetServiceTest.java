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
    void stalePublishedSheetWithMissingGradesIsSynchronizedBackToDraftOnRead() {
        GradeSheet gradeSheet = gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                validCreateCommand("Stale Published Incomplete Sheet"),
                IP
        );
        forcePublishedState(gradeSheet.id());

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
}
