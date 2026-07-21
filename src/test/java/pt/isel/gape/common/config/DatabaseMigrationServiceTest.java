package pt.isel.gape.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pt.isel.gape.transversal.DatabaseTestSupport;

class DatabaseMigrationServiceTest {

    private static final long TEST_MIGRATION_VERSION = 900L;
    private static final String TEST_MIGRATION_RESOURCE = "sql/migration/V900__test_pending.sql";

    private final DatabaseMigrationService migrationService = new DatabaseMigrationService();
    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = DatabaseTestSupport.openConnection();
        DatabaseTestSupport.resetDatabase(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection == null) {
            return;
        }
        try {
            DatabaseTestSupport.resetDatabase(connection);
        } finally {
            connection.close();
        }
    }

    @Test
    void listenerIsRegisteredAndAllDestructiveBootstrapModesSkipMigrations() throws Exception {
        String webXml = Files.readString(Path.of("src/main/webapp/WEB-INF/web.xml"));

        assertTrue(webXml.contains(
                "<listener-class>pt.isel.gape.web.listener.DatabaseMigrationListener</listener-class>"
        ));
        assertFalse(DatabaseMigrationService.shouldSkipForBootstrap(DatabaseBootstrapMode.NONE));
        assertTrue(DatabaseMigrationService.shouldSkipForBootstrap(DatabaseBootstrapMode.SCHEMA));
        assertTrue(DatabaseMigrationService.shouldSkipForBootstrap(DatabaseBootstrapMode.DEMO));
        assertTrue(DatabaseMigrationService.shouldSkipForBootstrap(DatabaseBootstrapMode.FULL));
    }

    @Test
    void emptySchemaIsCreatedAndRecordedAtTheValidatedBaseline() throws Exception {
        DatabaseTestSupport.dropCurrentSchemaObjects(connection);

        migrationService.migrate(connection);

        assertTrue(tableExists("user_account"));
        assertTrue(tableExists(DatabaseMigrationService.HISTORY_TABLE));
        assertEquals(currentMigrationVersion(), installedMigrationCount());
        assertEquals(1, installedMigrationCount(1L, true));
        assertEquals(1, installedMigrationCount(2L, true));
        assertEquals(1, installedMigrationCount(3L, true));
        assertEquals(1, installedMigrationCount(4L, true));
        assertEquals(1, installedMigrationCount(5L, true));
        assertEquals(1, installedMigrationCount(6L, true));
        assertEquals(1, installedMigrationCount(7L, true));
        assertEquals(1, installedMigrationCount(8L, true));
        assertEquals(1, installedMigrationCount(9L, true));
        assertEquals(1, installedMigrationCount(10L, true));
        assertEquals(1, installedMigrationCount(11L, true));
        assertEquals(1, installedMigrationCount(12L, true));
        assertEquals(1, installedMigrationCount(13L, true));
        assertEquals(1, installedMigrationCount(14L, true));
        assertEquals(1, installedMigrationCount(15L, true));
        assertEquals(1, installedMigrationCount(16L, true));
        assertEquals(1, installedMigrationCount(17L, true));
        assertEquals(1, installedMigrationCount(18L, true));
        assertEquals(1, installedMigrationCount(19L, true));
        assertEquals(1, installedMigrationCount(20L, true));
        assertEquals(1, installedMigrationCount(21L, true));
        assertEquals(1, installedMigrationCount(22L, true));
        assertEquals(1, installedMigrationCount(23L, true));
        assertEquals(1, installedMigrationCount(24L, true));
        assertEquals(1, installedMigrationCount(25L, true));
        assertEquals(1, installedMigrationCount(26L, true));
        assertEquals(1, installedMigrationCount(27L, true));
        assertEquals(1, installedMigrationCount(28L, true));
        assertEquals(1, installedMigrationCount(29L, true));
        assertEquals(1, installedMigrationCount(30L, true));
        for (long version = 31L; version <= currentMigrationVersion(); version++) {
            assertEquals(1, installedMigrationCount(version, true));
        }
        assertFalse(columnExists("subject", "initial_course_id"));
        assertTrue(columnExists("subject", "id_organic_unit"));
        assertTrue(columnExists("management_view", "scope_target_id"));
    }

    @Test
    void currentUnversionedSchemaIsValidatedBeforeBaselineIsRecorded() throws Exception {
        migrationService.migrate(connection);

        assertEquals(currentMigrationVersion(), installedMigrationCount());
        assertEquals(1, installedMigrationCount(1L, true));
        assertEquals(1, installedMigrationCount(2L, true));
    }

    @Test
    void fullBootstrapSchemaIsRecordedBeforeTheNextNormalStartup() throws Exception {
        new DatabaseBootstrapService().initialize(connection, DatabaseBootstrapMode.FULL);

        migrationService.migrate(connection);

        assertEquals(currentMigrationVersion(), installedMigrationCount());
        for (long version = 1L; version <= currentMigrationVersion(); version++) {
            assertEquals(1, installedMigrationCount(version, true));
        }
    }

    @Test
    void legacySchemaFailsWithoutRecordingABaselineVersion() throws Exception {
        DatabaseTestSupport.dropCurrentSchemaObjects(connection);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE user_account (id_user BIGINT UNSIGNED NOT NULL PRIMARY KEY)");
        }

        SQLException exception = assertThrows(SQLException.class, () -> migrationService.migrate(connection));

        assertTrue(exception.getMessage().contains("cannot be baselined safely"));
        assertTrue(tableExists(DatabaseMigrationService.HISTORY_TABLE));
        assertEquals(0, installedMigrationCount());
    }

    @Test
    void currentSchemaWithLegacyEnrollmentResidueIsCleanedByVersionTwo() throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE enroll_subject (id_legacy BIGINT NOT NULL PRIMARY KEY)");
            statement.execute("CREATE TABLE subject_enrollment_policy (id_legacy BIGINT NOT NULL PRIMARY KEY)");
            statement.execute("""
                    CREATE TRIGGER bi_enroll_subject_validate
                    BEFORE INSERT ON enroll_subject
                    FOR EACH ROW SET NEW.id_legacy = NEW.id_legacy
                    """);
            statement.execute("""
                    CREATE TRIGGER bu_enroll_subject_validate
                    BEFORE UPDATE ON enroll_subject
                    FOR EACH ROW SET NEW.id_legacy = NEW.id_legacy
                    """);
        }

        migrationService.migrate(connection);

        assertFalse(tableExists("enroll_subject"));
        assertFalse(tableExists("subject_enrollment_policy"));
        assertFalse(triggerExists("bi_enroll_subject_validate"));
        assertFalse(triggerExists("bu_enroll_subject_validate"));
        assertEquals(1, installedMigrationCount(2L, true));
    }

    @Test
    void inactiveSubjectActiveAssociationsAreClosedByVersionTwentyFive() throws Exception {
        DatabaseTestSupport.executeScript(connection, DatabaseTestSupport.SQL_SEED_DIR.resolve("base.sql"));
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO subject (
                        id_subject, id_organization, name, acronym, description, ects, workload_hours, state
                    ) VALUES (9001, 10, 'Migration State Subject', 'MSS', NULL, 6.00, 60, 'active')
                    """);
            statement.executeUpdate("""
                    INSERT INTO integrate_subject (
                        id_course, id_subject, curricular_year, term, mandatory, state, ended_at
                    ) VALUES (30, 9001, 1, 'semester_1', 1, 'active', NULL)
                    """);
            statement.execute("DROP TRIGGER bu_subject_validate");
            statement.executeUpdate("UPDATE subject SET state = 'inactive' WHERE id_subject = 9001");
        }

        migrationService.migrate(connection);

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT state, ended_at
                FROM integrate_subject
                WHERE id_course = 30
                  AND id_subject = 9001
                """)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals("historical", resultSet.getString("state"));
                assertTrue(resultSet.getDate("ended_at") != null);
            }
        }
        assertEquals(1, installedMigrationCount(25L, true));
    }

    @Test
    void academicLifecycleDataIsNormalizedByVersionTwentySix() throws Exception {
        DatabaseTestSupport.executeScript(connection, DatabaseTestSupport.SQL_SEED_DIR.resolve("base.sql"));
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    DELETE FROM receive_schedule_event
                    WHERE id_user = 4
                      AND id_schedule_event = 140
                    """);
            statement.executeUpdate("""
                    INSERT INTO receive_schedule_event (id_user, id_schedule_event)
                    VALUES (1, 140)
                    """);
        }

        migrationService.migrate(connection);

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT occurrence_period.state AS period_state,
                       class_group_row.state AS class_group_state,
                       course_enrollment.state AS course_enrollment_state,
                       class_group_enrollment.state AS class_group_enrollment_state,
                       teaching.state AS teaching_state
                FROM course_occurrence_period occurrence_period
                JOIN class_group class_group_row
                  ON class_group_row.id_course_occurrence_period = occurrence_period.id_course_occurrence_period
                JOIN enroll_course course_enrollment
                  ON course_enrollment.id_student_user = 4
                 AND course_enrollment.id_course = class_group_row.id_course
                 AND course_enrollment.id_course_occurrence = class_group_row.id_course_occurrence
                JOIN enroll_class_group class_group_enrollment
                  ON class_group_enrollment.id_student_user = 4
                 AND class_group_enrollment.id_class_group = class_group_row.id_class_group
                JOIN teach_class_group teaching
                  ON teaching.id_class_group = class_group_row.id_class_group
                WHERE class_group_row.id_class_group = 50
                """)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals("completed", resultSet.getString("period_state"));
                assertEquals("completed", resultSet.getString("class_group_state"));
                assertEquals("active", resultSet.getString("course_enrollment_state"));
                assertEquals("completed", resultSet.getString("class_group_enrollment_state"));
                assertEquals("inactive", resultSet.getString("teaching_state"));
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_user
                FROM receive_schedule_event
                WHERE id_schedule_event = 140
                ORDER BY id_user
                """)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Long> recipientIds = new ArrayList<>();
                while (resultSet.next()) {
                    recipientIds.add(resultSet.getLong("id_user"));
                }
                assertEquals(List.of(2L, 3L, 4L), recipientIds,
                        "Migration V026 must rebuild the historical class-group event roster");
            }
        }
        assertEquals(1, installedMigrationCount(26L, true));
    }

    @Test
    void legacyRevokedCertificateIsConvertedToImmutableIssuedCertificate() throws Exception {
        DatabaseTestSupport.executeScript(connection, DatabaseTestSupport.SQL_SEED_DIR.resolve("base.sql"));
        installLegacyCertificateArtifacts(true);

        migrationService.migrate(connection);

        assertFalse(columnExists("certificate", "revoked_at"));
        assertFalse(columnExists("certificate", "active_student_user_id"));
        assertTrue(uniqueIndexExists("certificate", "uq_certificate_occurrence_student"));
        assertTrue(triggerExists("bi_certificate_validate"));
        assertTrue(triggerExists("bu_certificate_validate"));
        assertEquals("issued", certificateValue(191L, "state"));
        assertEquals("legacy-validation-code", certificateValue(191L, "validation_code"));
    }

    @Test
    void ambiguousRevokedCertificateStopsMigrationWithoutDiscardingData() throws Exception {
        DatabaseTestSupport.executeScript(connection, DatabaseTestSupport.SQL_SEED_DIR.resolve("base.sql"));
        installLegacyCertificateArtifacts(false);

        SQLException exception = assertThrows(SQLException.class, () -> migrationService.migrate(connection));

        assertEquals("45000", exception.getSQLState());
        assertTrue(columnExists("certificate", "revoked_at"));
        assertEquals("revoked", certificateValue(191L, "state"));
        assertEquals(1, installedMigrationCount(3L, false));
    }

    @Test
    void legacyClassGroupIndexIsReplacedByOccurrenceScopedUniqueness() throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE class_group DROP INDEX uq_class_group_occurrence_subject_code");
            statement.execute("ALTER TABLE class_group "
                    + "ADD UNIQUE KEY uq_class_group_subject_code (id_subject, cod_class_group)");
        }

        migrationService.migrate(connection);

        assertFalse(uniqueIndexExists("class_group", "uq_class_group_subject_code"));
        assertTrue(uniqueIndexExists("class_group", "uq_class_group_occurrence_subject_code"));
    }

    @Test
    void legacyExternalDeliveryMetadataIsRemovedWithoutLosingReceipts() throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE receive_message DROP CHECK ck_receive_message_state");
            statement.execute("ALTER TABLE receive_message "
                    + "ADD CONSTRAINT ck_receive_message_state "
                    + "CHECK (state IN ('pending', 'delivered', 'read', 'failed'))");
            statement.execute("ALTER TABLE receive_message "
                    + "ADD COLUMN delivery_mode VARCHAR(40) NOT NULL DEFAULT 'internal'");
            statement.execute("ALTER TABLE receive_message "
                    + "ADD CONSTRAINT ck_receive_message_delivery_mode "
                    + "CHECK (delivery_mode IN ('internal', 'email', 'both'))");
        }
        DatabaseTestSupport.executeScript(connection, DatabaseTestSupport.SQL_SEED_DIR.resolve("base.sql"));
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE receive_message "
                    + "SET delivered_at = NULL, read_at = NULL, state = 'failed'");
        }

        migrationService.migrate(connection);

        assertFalse(columnExists("receive_message", "delivery_mode"));
        assertTrue(triggerExists("bi_receive_message_validate"));
        assertEquals(1, receiptCount());
        assertEquals("pending", receiptState());
        assertEquals(1, installedMigrationCount(6L, true));
    }

    @Test
    void installedMigrationChecksumMustMatchTheImmutableScript() throws Exception {
        migrationService.migrate(connection);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE gape_schema_migration SET checksum = REPEAT('0', 64) WHERE version = 1");
        }

        SQLException exception = assertThrows(SQLException.class, () -> migrationService.migrate(connection));

        assertTrue(exception.getMessage().contains("Checksum mismatch"));
        assertEquals(currentMigrationVersion(), installedMigrationCount());
    }

    @Test
    void versionedSchemaAppliesAndRecordsAPendingMigrationBeforeTargetValidation() throws Exception {
        migrationService.migrate(connection);
        List<DatabaseMigrationService.MigrationDefinition> migrations = new ArrayList<>(
                DatabaseMigrationService.loadMigrations()
        );
        migrations.add(DatabaseMigrationService.migrationDefinition(
                TEST_MIGRATION_VERSION,
                "Test pending migration",
                TEST_MIGRATION_RESOURCE
        ));

        migrationService.migrate(connection, List.copyOf(migrations));
        migrationService.migrate(connection, List.copyOf(migrations));

        assertTrue(tableExists("migration_test_probe"));
        assertEquals(currentMigrationVersion() + 1, installedMigrationCount());
        assertEquals(1, installedMigrationCount(TEST_MIGRATION_VERSION, true));
    }

    private boolean tableExists(String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND table_type = 'BASE TABLE'
                """)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    private static long currentMigrationVersion() throws IOException {
        List<DatabaseMigrationService.MigrationDefinition> migrations = DatabaseMigrationService.loadMigrations();
        return migrations.get(migrations.size() - 1).version();
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    private boolean uniqueIndexExists(String tableName, String indexName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                  AND non_unique = 0
                """)) {
            statement.setString(1, tableName);
            statement.setString(2, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private String certificateValue(long certificateId, String columnName) throws SQLException {
        if (!List.of("state", "validation_code").contains(columnName)) {
            throw new IllegalArgumentException("Unsupported certificate column: " + columnName);
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + columnName + " FROM certificate WHERE id_certificate = ?"
        )) {
            statement.setLong(1, certificateId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getString(1);
            }
        }
    }

    private void installLegacyCertificateArtifacts(boolean completeIssueSnapshot) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TRIGGER IF EXISTS bi_certificate_validate");
            statement.execute("DROP TRIGGER IF EXISTS bu_certificate_validate");
            statement.execute("ALTER TABLE certificate DROP CHECK ck_certificate_issued_fields");
            statement.execute("ALTER TABLE certificate DROP CHECK ck_certificate_state");
            statement.execute("ALTER TABLE certificate DROP INDEX uq_certificate_occurrence_student");
            statement.execute("ALTER TABLE certificate "
                    + "ADD COLUMN revoked_at DATETIME NULL, "
                    + "ADD COLUMN active_student_user_id BIGINT UNSIGNED NULL");
            statement.execute("ALTER TABLE certificate ADD UNIQUE KEY uq_certificate_course_active_student "
                    + "(id_course, active_student_user_id)");
            statement.executeUpdate("UPDATE certificate SET active_student_user_id = id_user_student");
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE certificate
                SET validation_code = 'legacy-validation-code',
                    issued_at = '2026-06-30 12:00:00',
                    state = 'revoked',
                    revoked_at = '2026-07-01 09:00:00',
                    final_grade = ?,
                    active_student_user_id = NULL
                WHERE id_certificate = 191
                """)) {
            if (completeIssueSnapshot) {
                statement.setBigDecimal(1, new java.math.BigDecimal("15.00"));
            } else {
                statement.setNull(1, java.sql.Types.DECIMAL);
            }
            statement.executeUpdate();
        }
    }

    private int installedMigrationCount() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM gape_schema_migration")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private int receiptCount() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM receive_message")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private String receiptState() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT state FROM receive_message LIMIT 1")) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private boolean triggerExists(String triggerName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.triggers
                WHERE trigger_schema = DATABASE()
                  AND trigger_name = ?
                """)) {
            statement.setString(1, triggerName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    private int installedMigrationCount(long version, boolean success) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM gape_schema_migration
                WHERE version = ?
                  AND success = ?
                """)) {
            statement.setLong(1, version);
            statement.setBoolean(2, success);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}
