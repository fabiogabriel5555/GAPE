package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import pt.isel.gape.learning.model.GradeRecord;
import pt.isel.gape.learning.model.GradeRecordResult;
import pt.isel.gape.learning.model.GradeSheet;
import pt.isel.gape.learning.model.GradeSheetCreateCommand;
import pt.isel.gape.learning.model.GradeSheetState;
import pt.isel.gape.learning.model.GradeSheetType;
import pt.isel.gape.learning.model.GradeSheetUpdateCommand;
import pt.isel.gape.learning.service.CorrectionService;
import pt.isel.gape.learning.service.GradeRecordService;
import pt.isel.gape.learning.service.GradeSheetService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class GradeRecordServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-11T10:15:30Z"), ZoneOffset.UTC);
    private static final String IP = "127.0.0.1";

    private GradeSheetService gradeSheetService;
    private GradeRecordService gradeRecordService;
    private CorrectionService correctionService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        gradeSheetService = new GradeSheetService(connectionProvider, FIXED_CLOCK);
        gradeRecordService = new GradeRecordService(connectionProvider, FIXED_CLOCK);
        correctionService = new CorrectionService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void automaticCorrectionKeepsSheetDraftWhenAnotherAssessmentIsMissing() {
        correctionService.autoCorrectAttempt(3L, null, AccessProfileType.TEACHER, 120L, IP);

        assertEquals(GradeSheetState.DRAFT, gradeSheetState(170L));
        assertEquals(0L, activeGradeRecordCount(170L, 4L));
    }

    @Test
    void weightsRecalculateAutomaticFinalGrade() {
        completeAttemptScore(120L, bd("14.00"));
        completeAssessmentAttempt(92L, bd("18.00"));

        gradeSheetService.updateGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                170L,
                new GradeSheetUpdateCommand(
                        40L,
                        "Weighted Grade Sheet",
                        GradeSheetType.PARTIAL,
                        bd("20.00"),
                        bd("9.50"),
                        List.of(50L),
                        List.of(
                                new GradeAssessmentWeight(90L, bd("30.00")),
                                new GradeAssessmentWeight(92L, bd("70.00"))
                        )
                ),
                IP
        );

        assertEquals(GradeSheetState.PUBLISHED, gradeSheetState(170L));
        assertEquals(0, bd("16.80").compareTo(activeGradeRecordValue(170L, 4L)));
        assertEquals("AUTO-170-4", activeGradeRecordCode(170L, 4L));
        assertNull(activeGradeRecordAttemptId(170L, 4L));
    }

    @Test
    void missingAssessmentScoreKeepsSheetDraftAndDoesNotCreateFinalGrade() {
        GradeSheet gradeSheet = gradeSheetService.createGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                new GradeSheetCreateCommand(
                        40L,
                        "Missing Assessment Score Sheet",
                        GradeSheetType.PARTIAL,
                        bd("20.00"),
                        bd("9.50"),
                        GradeSheetState.DRAFT,
                        List.of(50L),
                        List.of(
                                new GradeAssessmentWeight(90L, bd("50.00")),
                                new GradeAssessmentWeight(92L, bd("50.00"))
                        )
                ),
                IP
        );

        assertEquals(GradeSheetState.DRAFT, gradeSheetState(gradeSheet.id()));
        assertEquals(0L, activeGradeRecordCount(gradeSheet.id(), 4L));
    }

    @Test
    void studentCanConsultOnlyOwnPublishedAutomaticGradeRecords() {
        publishWeightedBaseSheet();
        long recordId = activeGradeRecordId(170L, 4L);

        GradeRecord ownRecord = gradeRecordService.getGradeRecord(
                4L,
                null,
                AccessProfileType.STUDENT,
                recordId,
                IP
        );

        assertEquals(recordId, ownRecord.id());
        assertTrue(gradeRecordService.listOwnGradeRecords(4L, null, AccessProfileType.STUDENT, IP)
                .stream()
                .anyMatch(visible -> visible.id() == recordId));
    }

    @Test
    void studentCannotReadDraftGradeRecord() {
        assertThrows(SecurityException.class, () -> gradeRecordService.getGradeRecord(
                4L,
                null,
                AccessProfileType.STUDENT,
                180L,
                IP
        ));
    }

    @Test
    void studentCannotReadAnotherStudentsGradeRecord() throws Exception {
        addActiveStudent(6L);
        publishWeightedBaseSheet();
        long recordId = activeGradeRecordId(170L, 4L);

        assertThrows(SecurityException.class, () -> gradeRecordService.getGradeRecord(
                6L,
                null,
                AccessProfileType.STUDENT,
                recordId,
                IP
        ));
    }

    @Test
    void nonStudentCannotListOwnGradeRecords() {
        assertThrows(SecurityException.class, () -> gradeRecordService.listOwnGradeRecords(
                3L,
                null,
                AccessProfileType.TEACHER,
                IP
        ));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private void publishWeightedBaseSheet() {
        completeAttemptScore(120L, bd("14.00"));
        completeAssessmentAttempt(92L, bd("18.00"));
        gradeSheetService.updateGradeSheet(
                3L,
                null,
                AccessProfileType.TEACHER,
                170L,
                new GradeSheetUpdateCommand(
                        40L,
                        "Weighted Grade Sheet",
                        GradeSheetType.PARTIAL,
                        bd("20.00"),
                        bd("9.50"),
                        List.of(50L),
                        List.of(
                                new GradeAssessmentWeight(90L, bd("30.00")),
                                new GradeAssessmentWeight(92L, bd("70.00"))
                        )
                ),
                IP
        );
    }

    private static GradeSheetState gradeSheetState(long gradeSheetId) {
        return queryOne("""
                SELECT state
                FROM grade_sheet
                WHERE id_grade_sheet = ?
                """, gradeSheetId, value -> GradeSheetState.fromDatabaseValue(value.getString(1)));
    }

    private static long activeGradeRecordId(long gradeSheetId, long studentUserId) {
        return queryOne("""
                SELECT id_grade_record
                FROM grade_record
                WHERE id_grade_sheet = ?
                  AND id_user_student = ?
                  AND state IN ('draft', 'published', 'corrected')
                """, gradeSheetId, studentUserId, value -> value.getLong(1));
    }

    private static BigDecimal activeGradeRecordValue(long gradeSheetId, long studentUserId) {
        return queryOne("""
                SELECT value
                FROM grade_record
                WHERE id_grade_sheet = ?
                  AND id_user_student = ?
                  AND state IN ('draft', 'published', 'corrected')
                """, gradeSheetId, studentUserId, value -> value.getBigDecimal(1));
    }

    private static String activeGradeRecordCode(long gradeSheetId, long studentUserId) {
        return queryOne("""
                SELECT cod_grade_record
                FROM grade_record
                WHERE id_grade_sheet = ?
                  AND id_user_student = ?
                  AND state IN ('draft', 'published', 'corrected')
                """, gradeSheetId, studentUserId, value -> value.getString(1));
    }

    private static Long activeGradeRecordAttemptId(long gradeSheetId, long studentUserId) {
        return queryOne("""
                SELECT id_attempt
                FROM grade_record
                WHERE id_grade_sheet = ?
                  AND id_user_student = ?
                  AND state IN ('draft', 'published', 'corrected')
                """, gradeSheetId, studentUserId, value -> {
            long attemptId = value.getLong(1);
            return value.wasNull() ? null : attemptId;
        });
    }

    private static GradeRecordResult activeGradeRecordResult(long gradeSheetId, long studentUserId) {
        return queryOne("""
                SELECT result
                FROM grade_record
                WHERE id_grade_sheet = ?
                  AND id_user_student = ?
                  AND state IN ('draft', 'published', 'corrected')
                """, gradeSheetId, studentUserId, value -> GradeRecordResult.fromDatabaseValue(value.getString(1)));
    }

    private static long activeGradeRecordCount(long gradeSheetId, long studentUserId) {
        return queryOne("""
                SELECT COUNT(*)
                FROM grade_record
                WHERE id_grade_sheet = ?
                  AND id_user_student = ?
                  AND state IN ('draft', 'published', 'corrected')
                """, gradeSheetId, studentUserId, value -> value.getLong(1));
    }

    private static void completeAttemptScore(long attemptId, BigDecimal score) {
        execute("""
                UPDATE attempt
                SET score = ?,
                    state = 'corrected',
                    submitted_at = '2026-02-11 10:10:00'
                WHERE id_attempt = ?
                """, statement -> {
            statement.setBigDecimal(1, score);
            statement.setLong(2, attemptId);
        });
    }

    private static void completeAssessmentAttempt(long assessmentId, BigDecimal score) {
        execute("""
                INSERT INTO enroll_assessment (id_student_user, id_assessment, state)
                VALUES (4, ?, 'active')
                ON DUPLICATE KEY UPDATE state = 'active'
                """, statement -> statement.setLong(1, assessmentId));
        execute("""
                INSERT INTO attempt (
                    id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
                ) VALUES (4, ?, 1, ?, 'corrected', '2026-06-20 09:00:00', '2026-06-20 10:30:00')
                """, statement -> {
            statement.setLong(1, assessmentId);
            statement.setBigDecimal(2, score);
        });
    }

    private static void addActiveStudent(long userId) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement user = connection.prepareStatement("""
                    INSERT INTO user_account (
                        id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt
                    ) VALUES (?, ?, ?, 'active', 'pt-PT', NULL, '2026-01-01 10:00:00', 'hash', 'salt')
                    """)) {
                user.setLong(1, userId);
                user.setString(2, "Grade Student " + userId);
                user.setString(3, "grade.student" + userId + "@gape.local");
                user.executeUpdate();
            }
            try (PreparedStatement profile = connection.prepareStatement("""
                    INSERT INTO student_profile (id_user, cod_student)
                    VALUES (?, ?)
                    """)) {
                profile.setLong(1, userId);
                profile.setString(2, "STD-GRADE-" + userId);
                profile.executeUpdate();
            }
        }
    }

    private static <T> T queryOne(String sql, long firstId, ResultMapper<T> mapper) {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, firstId);
            try (java.sql.ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return mapper.map(resultSet);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query test fixture", exception);
        }
    }

    private static <T> T queryOne(String sql, long firstId, long secondId, ResultMapper<T> mapper) {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, firstId);
            statement.setLong(2, secondId);
            try (java.sql.ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return mapper.map(resultSet);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query test fixture", exception);
        }
    }

    private static void execute(String sql, SqlBinder binder) {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update test fixture", exception);
        }
    }

    @FunctionalInterface
    private interface ResultMapper<T> {
        T map(java.sql.ResultSet resultSet) throws SQLException;
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
