package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchemaIntegrityTest {

    @BeforeEach
    void prepareSchema() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            DatabaseTestSupport.resetDatabase(connection);
        }
    }

    @Test
    void shouldCreateExpectedTables() throws Exception {
        Set<String> expectedTables = Set.of(
                "user_account", "administrator_profile", "coordinator_profile", "teacher_profile", "student_profile",
                "user_session", "deletion_request", "permission", "grant_administrator",
                "grant_coordinator", "grant_teacher", "grant_student",
                "organization", "organic_unit", "course", "subject", "class_group", "content_block",
                "integrate_subject", "manage_organization", "coordinate_subject", "teach_class_group",
                "enroll_course", "enroll_subject", "enroll_class_group",
                "subject_enrollment_policy", "class_group_enrollment_policy",
                "content_item", "content_file", "physical_room", "lesson", "assessment", "question", "question_option",
                "attempt", "response", "response_option",
                "associate_organization_content", "associate_organic_unit_content", "associate_course_content",
                "associate_subject_content", "associate_class_group_content", "associate_block_content",
                "associate_assessment_content",
                "schedule_event", "receive_schedule_event", "associate_schedule_event_class_group",
                "attendance_record", "absence_justification", "grade_sheet", "associate_grade_sheet_class_group",
                "based_on_assessment", "grade_record", "certificate", "based_on_grade_sheet_certificate",
                "management_view", "access_management_view", "channel", "participate_channel", "message",
                "receive_message", "associate_channel_class_group", "associate_channel_content_block",
                "associate_channel_assessment", "activity_log"
        );

        Set<String> existing = new HashSet<>();
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            String sql = """
                    SELECT table_name
                    FROM information_schema.tables
                    WHERE table_schema = ?
                      AND table_type = 'BASE TABLE'
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, DatabaseTestSupport.currentSchema(connection));
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        existing.add(resultSet.getString(1));
                    }
                }
            }
        }

        assertTrue(existing.containsAll(expectedTables), "All expected GAPE tables must exist");
    }

    @Test
    void shouldExposePrimaryForeignUniqueAndCheckConstraints() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "user_account", "PRIMARY", "PRIMARY KEY"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "organization", "PRIMARY", "PRIMARY KEY"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "schedule_event", "PRIMARY", "PRIMARY KEY"));

            List<String[]> fkChecks = List.of(
                    new String[]{"user_session", "fk_user_session_user"},
                    new String[]{"course", "fk_course_org"},
                    new String[]{"lesson", "fk_lesson_class_group"},
                    new String[]{"attendance_record", "fk_attendance_student"},
                    new String[]{"message", "fk_message_channel"}
            );
            for (String[] fk : fkChecks) {
                assertTrue(
                        DatabaseTestSupport.existsConstraint(connection, fk[0], fk[1], "FOREIGN KEY"),
                        "Expected FK constraint " + fk[1] + " on table " + fk[0]
                );
            }

            assertTrue(DatabaseTestSupport.isUniqueIndex(connection, "user_account", "uq_user_account_email"));
            assertTrue(DatabaseTestSupport.isUniqueIndex(connection, "user_account", "uq_user_account_document"));
            assertTrue(DatabaseTestSupport.isUniqueIndex(connection, "user_session", "uq_user_session_token"));
            assertTrue(DatabaseTestSupport.isUniqueIndex(connection, "question", "uq_question_assessment_code"));
            assertTrue(DatabaseTestSupport.isUniqueIndex(connection, "content_block", "uq_content_block_active_order"));

            assertTrue(DatabaseTestSupport.existsConstraint(connection, "user_account", "ck_user_account_state", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "user_account", "ck_user_account_document_pair", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "user_session", "ck_user_session_last_activity_end", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "class_group", "ck_class_group_modality", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "class_group", "ck_class_group_shift", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "class_group", "ck_class_group_students_range", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "content_block", "ck_content_block_scheduled_access", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "message", "ck_message_attachment_type", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "certificate", "ck_certificate_issued_context", "CHECK"));
        }
    }

    @Test
    void shouldExposeValidationTriggersForCrossEntityRules() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_course_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_enroll_class_group_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_content_block_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_lesson_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_attempt_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_schedule_event_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_message_validate"));
        }
    }

    @Test
    void shouldExposeNotNullColumnsMetadata() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "user_account", "email"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "user_account", "credential_hash"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "organization", "type"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "class_group", "modality"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "content_item", "format"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "lesson", "id_content_block"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "assessment", "type"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "certificate", "id_user_student"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "schedule_event", "starts_at"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "activity_log", "operation_type"));
        }
    }
}
