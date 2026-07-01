package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import pt.isel.gape.common.config.DatabaseBootstrapMode;
import pt.isel.gape.common.config.DatabaseBootstrapService;

class DatabaseBootstrapServiceTest {

    @Test
    void shouldBootstrapSchemaOnly() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            new DatabaseBootstrapService().initialize(connection, DatabaseBootstrapMode.SCHEMA);

            assertTrue(DatabaseTestSupport.countRows(connection, "user_account") == 0);
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "user_account", "uq_user_account_email", "UNIQUE"));
        }
    }

    @Test
    void shouldBootstrapDemoSeed() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            new DatabaseBootstrapService().initialize(connection, DatabaseBootstrapMode.DEMO);

            assertTrue(DatabaseTestSupport.countRows(connection, "user_account") >= 5);
            assertTrue(DatabaseTestSupport.countRows(connection, "lesson") >= 1);
            assertTrue(DatabaseTestSupport.countRows(connection, "message") >= 1);
        }
    }

    @Test
    void shouldBootstrapFullSeed() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            new DatabaseBootstrapService().initialize(connection, DatabaseBootstrapMode.FULL);

            assertTrue(DatabaseTestSupport.countRows(connection, "user_account") >= 16);
            assertTrue(DatabaseTestSupport.countRows(connection, "class_group") >= 7);
            assertTrue(DatabaseTestSupport.countRows(connection, "grade_record") >= 8);
            assertTrue(DatabaseTestSupport.countRows(connection, "certificate") >= 7);
            assertTrue(DatabaseTestSupport.countRows(connection, "activity_log") >= 25);
            List<String> gradeSheetMismatches = fullSeedGradeSheetStateMismatches(connection);
            assertTrue(
                    gradeSheetMismatches.isEmpty(),
                    "Full seed grade sheets must match grade completeness: " + String.join("; ", gradeSheetMismatches)
            );
        }
    }

    private static List<String> fullSeedGradeSheetStateMismatches(Connection connection) throws SQLException {
        List<String> mismatches = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_grade_sheet, title, state
                FROM grade_sheet
                ORDER BY id_grade_sheet
                """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                long gradeSheetId = resultSet.getLong("id_grade_sheet");
                String databaseState = resultSet.getString("state");
                boolean complete = gradeSheetComplete(connection, gradeSheetId);
                String expectedState = complete
                        ? ("closed".equals(databaseState) ? "closed" : "published")
                        : "draft";
                if (!expectedState.equals(databaseState)) {
                    mismatches.add(gradeSheetId + " " + resultSet.getString("title")
                            + " expected " + expectedState + " but was " + databaseState);
                }
            }
        }
        return mismatches;
    }

    private static boolean gradeSheetComplete(Connection connection, long gradeSheetId) throws SQLException {
        List<Long> studentUserIds = requiredStudentUserIds(connection, gradeSheetId);
        if (studentUserIds.isEmpty()) {
            return false;
        }
        List<Long> assessmentIds = assessmentIds(connection, gradeSheetId);
        for (Long studentUserId : studentUserIds) {
            if (!hasGradeRecord(connection, gradeSheetId, studentUserId)) {
                return false;
            }
            for (Long assessmentId : assessmentIds) {
                if (!hasCorrectedAssessmentScore(connection, studentUserId, assessmentId)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static List<Long> requiredStudentUserIds(Connection connection, long gradeSheetId) throws SQLException {
        List<Long> studentUserIds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT DISTINCT required.id_student_user
                FROM (
                    SELECT ecg.id_student_user
                    FROM associate_grade_sheet_class_group agscg
                    JOIN enroll_class_group ecg ON ecg.id_class_group = agscg.id_class_group
                    WHERE agscg.id_grade_sheet = ?
                      AND ecg.state IN ('active', 'completed')
                    UNION
                    SELECT es.id_student_user
                    FROM grade_sheet gs
                    JOIN enroll_subject es ON es.id_subject = gs.id_subject
                    WHERE gs.id_grade_sheet = ?
                      AND es.state IN ('active', 'completed')
                      AND NOT EXISTS (
                            SELECT 1
                            FROM associate_grade_sheet_class_group agscg
                            WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                      )
                    UNION
                    SELECT gr.id_user_student AS id_student_user
                    FROM grade_record gr
                    WHERE gr.id_grade_sheet = ?
                ) required
                ORDER BY required.id_student_user
                """)) {
            statement.setLong(1, gradeSheetId);
            statement.setLong(2, gradeSheetId);
            statement.setLong(3, gradeSheetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    studentUserIds.add(resultSet.getLong("id_student_user"));
                }
            }
        }
        return studentUserIds;
    }

    private static List<Long> assessmentIds(Connection connection, long gradeSheetId) throws SQLException {
        List<Long> assessmentIds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT DISTINCT a.id_assessment
                FROM grade_sheet gs
                LEFT JOIN associate_grade_sheet_class_group agscg ON agscg.id_grade_sheet = gs.id_grade_sheet
                LEFT JOIN content_block cb ON cb.id_class_group = agscg.id_class_group
                LEFT JOIN assessment_class_group acg ON acg.id_class_group = agscg.id_class_group
                LEFT JOIN assessment a ON (
                        a.id_content_block = cb.id_content_block
                        OR a.id_assessment = acg.id_assessment
                    )
                   AND (a.id_subject IS NULL OR a.id_subject = gs.id_subject)
                WHERE gs.id_grade_sheet = ?
                  AND a.id_assessment IS NOT NULL
                ORDER BY a.id_assessment
                """)) {
            statement.setLong(1, gradeSheetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    assessmentIds.add(resultSet.getLong("id_assessment"));
                }
            }
        }
        if (!assessmentIds.isEmpty()) {
            return assessmentIds;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_assessment
                FROM based_on_assessment
                WHERE id_grade_sheet = ?
                ORDER BY id_assessment
                """)) {
            statement.setLong(1, gradeSheetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    assessmentIds.add(resultSet.getLong("id_assessment"));
                }
            }
        }
        return assessmentIds;
    }

    private static boolean hasGradeRecord(Connection connection, long gradeSheetId, long studentUserId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                FROM grade_record
                WHERE id_grade_sheet = ?
                  AND id_user_student = ?
                LIMIT 1
                """)) {
            statement.setLong(1, gradeSheetId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static boolean hasCorrectedAssessmentScore(Connection connection, long studentUserId, long assessmentId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                FROM attempt
                WHERE id_student_user = ?
                  AND id_assessment = ?
                  AND state = 'corrected'
                  AND score IS NOT NULL
                LIMIT 1
                """)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
