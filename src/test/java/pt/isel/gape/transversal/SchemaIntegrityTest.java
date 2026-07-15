package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
                "organization", "organic_unit", "course", "course_occurrence", "course_occurrence_period",
                "subject", "class_group", "content_block",
                "integrate_subject", "manage_organization", "coordinate_subject", "teach_class_group",
                "enroll_course", "enroll_class_group",
                "class_group_enrollment_policy",
                "content_item", "content_file", "physical_room", "lesson", "assessment", "assessment_class_group",
                "question", "question_option",
                "enroll_assessment", "attempt", "response", "response_option",
                "associate_organization_content", "associate_organic_unit_content", "associate_course_content",
                "associate_subject_content", "associate_class_group_content", "associate_block_content",
                "associate_assessment_content",
                "schedule_event", "receive_schedule_event", "associate_schedule_event_class_group", "learning_event",
                "learning_event_read", "attendance_record", "absence_justification", "grade_sheet",
                "associate_grade_sheet_class_group",
                "based_on_assessment", "grade_record", "certificate", "based_on_grade_sheet_certificate",
                "management_view", "access_management_view", "channel", "participate_channel",
                "direct_message_channel", "message",
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
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "learning_event", "PRIMARY", "PRIMARY KEY"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "learning_event_read", "PRIMARY", "PRIMARY KEY"));

            List<String[]> fkChecks = List.of(
                    new String[]{"user_session", "fk_user_session_user"},
                    new String[]{"course", "fk_course_org"},
                    new String[]{"assessment_class_group", "fk_assessment_class_group_assessment"},
                    new String[]{"lesson", "fk_lesson_class_group"},
                    new String[]{"attendance_record", "fk_attendance_student"},
                    new String[]{"learning_event_read", "fk_learning_event_read_event"},
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
            assertTrue(DatabaseTestSupport.isUniqueIndex(connection, "content_block", "uq_content_block_order"));
            assertTrue(DatabaseTestSupport.isUniqueIndex(connection, "grade_record", "uq_grade_record_sheet_active_student"));
            assertTrue(DatabaseTestSupport.isUniqueIndex(connection, "certificate", "uq_certificate_occurrence_student"));
            assertTrue(DatabaseTestSupport.isUniqueIndex(connection, "course_occurrence", "uq_course_occurrence_reference_year"));
            assertTrue(DatabaseTestSupport.isUniqueIndex(connection, "certificate", "uq_certificate_validation_code"));
            assertTrue(DatabaseTestSupport.isUniqueIndex(connection, "learning_event", "uq_learning_event_source"));

            assertTrue(DatabaseTestSupport.existsConstraint(connection, "user_account", "ck_user_account_state", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "user_account", "ck_user_account_document_pair", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "user_session", "ck_user_session_last_activity_end", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "course", "ck_course_duration_years", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "course_occurrence", "ck_course_occurrence_dates", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "course_occurrence", "ck_course_occurrence_reference_year", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "course_occurrence_period", "ck_course_occurrence_period_dates", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "class_group", "ck_class_group_modality", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "class_group", "ck_class_group_shift", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "class_group", "ck_class_group_students_range", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "content_block", "ck_content_block_state", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "assessment", "ck_assessment_mode", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "message", "ck_message_attachment_type", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "learning_event", "ck_learning_event_category", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "grade_sheet", "ck_grade_sheet_scale", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "grade_record", "ck_grade_record_result", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "grade_record", "ck_grade_record_state", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "certificate", "ck_certificate_type", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "certificate", "ck_certificate_state", "CHECK"));
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "certificate", "ck_certificate_issued_fields", "CHECK"));
        }
    }

    @Test
    void shouldExposeValidationTriggersForCrossEntityRules() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_course_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_course_occurrence_period_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bu_course_occurrence_period_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_enroll_class_group_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_enroll_assessment_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_content_block_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_lesson_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_assessment_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bu_assessment_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_assessment_class_group_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bu_assessment_class_group_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_attempt_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bu_attempt_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_response_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_response_option_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_schedule_event_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_associate_grade_sheet_class_group_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bu_associate_grade_sheet_class_group_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_based_on_assessment_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bu_based_on_assessment_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_grade_record_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bu_grade_record_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_certificate_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bu_certificate_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_bgsc_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bu_bgsc_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bu_registered_direct_channel_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bd_registered_direct_channel_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_direct_participation_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bu_direct_participation_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bd_direct_participation_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_direct_message_channel_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bu_direct_message_channel_validate"));
            assertTrue(DatabaseTestSupport.existsTrigger(connection, "bi_message_validate"));
        }
    }

    @Test
    void directMessagePairTriggersRejectUnorderedInsertAndUpdate() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO user_account (
                        id_user, name, email, state, language, credential_hash, credential_salt
                    ) VALUES
                        (1, 'Low User', 'low@example.test', 'active', 'en', 'hash', 'salt'),
                        (2, 'High User', 'high@example.test', 'active', 'en', 'hash', 'salt')
                    """);
            statement.executeUpdate("""
                    INSERT INTO channel (id_channel, title, type, visibility, created_at, state)
                    VALUES (10, 'Direct pair', 'message', 'participants', CURRENT_TIMESTAMP, 'active')
                    """);
            statement.executeUpdate("""
                    INSERT INTO participate_channel (id_user, id_channel, role, joined_at, muted, state)
                    VALUES
                        (1, 10, 'owner', CURRENT_TIMESTAMP, FALSE, 'active'),
                        (2, 10, 'member', CURRENT_TIMESTAMP, FALSE, 'active')
                    """);
            statement.executeUpdate("""
                    INSERT INTO direct_message_channel (id_user_low, id_user_high, id_channel)
                    VALUES (1, 2, 10)
                    """);

            SQLException insertException = assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO direct_message_channel (id_user_low, id_user_high, id_channel)
                    VALUES (2, 1, 10)
                    """));
            SQLException updateException = assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    UPDATE direct_message_channel
                    SET id_user_low = 2, id_user_high = 1
                    WHERE id_user_low = 1 AND id_user_high = 2
                    """));

            DatabaseTestSupport.assertIntegrityException(insertException);
            DatabaseTestSupport.assertIntegrityException(updateException);
        }
    }

    @Test
    void shouldExposeNotNullColumnsMetadata() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "user_account", "email"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "user_account", "credential_hash"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "organization", "type"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "course", "duration"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "course_occurrence", "reference_year"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "course_occurrence", "starts_at"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "course_occurrence_period", "starts_at"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "class_group", "modality"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "class_group", "id_course_occurrence"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "class_group", "min_students"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "class_group", "max_students"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "content_item", "format"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "lesson", "id_content_block"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "assessment", "type"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "certificate", "id_user_student"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "schedule_event", "starts_at"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "learning_event", "occurred_at"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "learning_event_read", "read_at"));
            assertTrue(DatabaseTestSupport.isNotNullColumn(connection, "activity_log", "operation_type"));
        }
    }
}
