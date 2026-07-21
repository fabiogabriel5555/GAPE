package pt.isel.gape.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.service.AcademicLifecycleSynchronizationService;
import pt.isel.gape.common.sql.SqlScriptExecutor;

/**
 * Applies ordered schema migrations when destructive database bootstrap is disabled.
 */
public final class DatabaseMigrationService {

    static final String HISTORY_TABLE = "gape_schema_migration";
    static final String MANIFEST_RESOURCE = "sql/migration/migrations.txt";

    private static final String SCHEMA_RESOURCE = "sql/schema.sql";
    private static final String MIGRATION_LOCK = "gape_schema_migration";
    private static final int LOCK_TIMEOUT_SECONDS = 30;
    private static final long BASELINE_VERSION = 1L;

    private static final Set<String> REQUIRED_TABLES = Set.of(
            "user_account",
            "course_period_template",
            "course_occurrence",
            "course_occurrence_period",
            "learning_event",
            "learning_event_read",
            "direct_message_channel"
    );

    private static final Set<SchemaObject> REQUIRED_COLUMNS = Set.of(
            new SchemaObject("user_account", "document_number_fingerprint"),
            new SchemaObject("course", "frequency"),
            new SchemaObject("course_occurrence", "reference_year"),
            new SchemaObject("class_group", "id_course_occurrence"),
            new SchemaObject("class_group", "id_course_occurrence_period"),
            new SchemaObject("content_block", "created_at"),
            new SchemaObject("content_block", "updated_at"),
            new SchemaObject("enroll_course", "id_course_occurrence"),
            new SchemaObject("enroll_assessment", "start_date"),
            new SchemaObject("enroll_assessment", "end_date"),
            new SchemaObject("assessment", "cod_physical_room"),
            new SchemaObject("grade_sheet", "id_course_occurrence"),
            new SchemaObject("grade_sheet", "publication_explanation"),
            new SchemaObject("grade_sheet", "scope"),
            new SchemaObject("grade_sheet", "subject_occurrence_aggregate_id"),
            new SchemaObject("certificate", "id_course_occurrence"),
            new SchemaObject("certificate", "validation_code"),
            new SchemaObject("integrate_subject", "state"),
            new SchemaObject("integrate_subject", "ended_at"),
            new SchemaObject("direct_message_channel", "id_user_low"),
            new SchemaObject("direct_message_channel", "id_user_high"),
            new SchemaObject("direct_message_channel", "id_channel"),
            new SchemaObject("management_view", "scope_target_type"),
            new SchemaObject("management_view", "scope_target_id"),
            new SchemaObject("management_view", "owner_user_id")
    );

    private static final Set<SchemaObject> REQUIRED_NOT_NULL_COLUMNS = Set.of(
            new SchemaObject("course", "duration"),
            new SchemaObject("course", "frequency"),
            new SchemaObject("course_occurrence", "reference_year"),
            new SchemaObject("class_group", "id_course_occurrence"),
            new SchemaObject("class_group", "id_course_occurrence_period"),
            new SchemaObject("class_group", "starts_at"),
            new SchemaObject("class_group", "ends_at"),
            new SchemaObject("enroll_course", "id_course_occurrence"),
            new SchemaObject("enroll_course", "start_date"),
            new SchemaObject("enroll_course", "end_date"),
            new SchemaObject("enroll_class_group", "start_date"),
            new SchemaObject("enroll_class_group", "end_date"),
            new SchemaObject("enroll_assessment", "start_date"),
            new SchemaObject("enroll_assessment", "end_date"),
            new SchemaObject("lesson", "starts_at"),
            new SchemaObject("lesson", "ends_at"),
            new SchemaObject("assessment", "available_from"),
            new SchemaObject("assessment", "available_until"),
            new SchemaObject("grade_sheet", "id_course_occurrence"),
            new SchemaObject("grade_sheet", "scope"),
            new SchemaObject("certificate", "id_course_occurrence")
    );

    private static final Set<SchemaObject> REQUIRED_BASELINE_UNIQUE_INDEXES = Set.of(
            new SchemaObject("certificate", "uq_certificate_validation_code"),
            new SchemaObject("direct_message_channel", "primary"),
            new SchemaObject("direct_message_channel", "uq_direct_message_channel_channel")
    );

    private static final Set<SchemaObject> REQUIRED_TARGET_UNIQUE_INDEXES = Set.of(
            new SchemaObject("user_account", "uq_user_account_document_fingerprint"),
            new SchemaObject("certificate", "uq_certificate_occurrence_student"),
            new SchemaObject("class_group", "uq_class_group_occurrence_subject_code"),
            new SchemaObject("course_occurrence", "uq_course_occurrence_reference_year"),
            new SchemaObject("grade_sheet", "uq_grade_sheet_subject_occurrence_aggregate")
    );

    private static final Set<String> REQUIRED_TARGET_TRIGGERS = Set.of(
            "bi_certificate_validate",
            "bu_certificate_validate",
            "bu_registered_direct_channel_validate",
            "bd_registered_direct_channel_validate",
            "bi_direct_participation_validate",
            "bu_direct_participation_validate",
            "bd_direct_participation_validate",
            "bi_direct_channel_class_group_validate",
            "bu_direct_channel_class_group_validate",
            "bi_direct_channel_content_block_validate",
            "bu_direct_channel_content_block_validate",
            "bi_direct_channel_assessment_validate",
            "bu_direct_channel_assessment_validate",
            "bi_direct_message_channel_validate",
            "bu_direct_message_channel_validate",
            "bi_subject_validate",
            "bu_subject_validate",
            "bi_coordinate_subject_validate",
            "bu_coordinate_subject_validate",
            "bi_integrate_subject_validate",
            "bu_integrate_subject_validate",
            "bi_course_occurrence_validate",
            "bu_course_occurrence_validate",
            "bi_enroll_course_validate",
            "bu_enroll_course_validate",
            "bi_enroll_class_group_validate",
            "bu_enroll_class_group_validate",
            "bi_enroll_assessment_validate",
            "bu_enroll_assessment_validate",
            "bi_grade_sheet_validate",
            "bu_grade_sheet_validate",
            "bi_associate_grade_sheet_class_group_validate",
            "bu_associate_grade_sheet_class_group_validate"
    );

    private static final Set<String> FORBIDDEN_TABLES = Set.of(
            "enroll_subject",
            "subject_enrollment_policy"
    );

    private static final Set<SchemaObject> FORBIDDEN_COLUMNS = Set.of(
            new SchemaObject("certificate", "revoked_at"),
            new SchemaObject("content_block", "access_mode"),
            new SchemaObject("content_block", "available_from"),
            new SchemaObject("content_block", "available_until"),
            new SchemaObject("coordinate_subject", "start_date"),
            new SchemaObject("coordinate_subject", "end_date"),
            new SchemaObject("receive_message", "delivery_mode"),
            new SchemaObject("subject", "initial_course_id")
    );

    public void migrateIfConfigured() {
        DatabaseBootstrapMode bootstrapMode = DatabaseBootstrapMode.fromProperty(
                DatabaseConfig.getProperty("db.bootstrap.mode", "none")
        );
        if (shouldSkipForBootstrap(bootstrapMode)) {
            System.out.println("[GAPE][DB] Migrations skipped while bootstrap mode is active.");
            return;
        }
        if (!migrationsEnabled()) {
            System.out.println("[GAPE][DB] Migrations disabled by db.migrations.enabled=false.");
            return;
        }

        try (Connection connection = DatabaseConfig.getConnection()) {
            migrate(connection);
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Failed to migrate database schema", exception);
        }
    }

    public void migrate(Connection connection) throws SQLException, IOException {
        Objects.requireNonNull(connection, "connection");
        migrate(connection, loadMigrations());
    }

    void migrate(Connection connection, List<MigrationDefinition> migrations) throws SQLException, IOException {
        Objects.requireNonNull(connection, "connection");
        validateMigrationDefinitions(migrations);
        acquireLock(connection);
        try {
            boolean emptySchema = applicationTableCount(connection) == 0;
            if (emptySchema) {
                SqlScriptExecutor.executeResource(connection, SCHEMA_RESOURCE);
            }

            createHistoryTable(connection);
            Map<Long, InstalledMigration> installed = loadInstalledMigrations(connection);

            /*
             * A destructive bootstrap executes schema.sql, which already represents
             * the current schema.  Its migrations are intentionally skipped during
             * that startup, so the next normal startup must establish the matching
             * history instead of attempting obsolete intermediate migrations.
             */
            if (installed.isEmpty() && isCurrentTargetSchema(connection)) {
                replaceMigrationHistory(connection, migrations);
                installed = loadInstalledMigrations(connection);
                System.out.println("[GAPE][DB] Recovered migration history for the current schema.");
            }
            validateInstalledMigrations(installed, migrations);

            MigrationDefinition baseline = migrations.get(0);
            if (!installed.containsKey(BASELINE_VERSION)) {
                validateBaselineStructure(connection);
                recordMigration(connection, baseline, 0L, true);
                installed.put(BASELINE_VERSION, InstalledMigration.successful(baseline));
            }

            for (MigrationDefinition migration : migrations) {
                if (migration.version() == BASELINE_VERSION || installed.containsKey(migration.version())) {
                    continue;
                }
                applyMigration(connection, migration);
            }

            validateTargetStructure(connection);
            synchronizeAcademicLifecycleData(connection);

            System.out.println("[GAPE][DB] Schema migrations are current at version "
                    + migrations.get(migrations.size() - 1).version() + ".");
        } finally {
            releaseLock(connection);
        }
    }

    static List<MigrationDefinition> loadMigrations() throws IOException {
        String manifest = readResourceText(MANIFEST_RESOURCE);
        List<MigrationDefinition> migrations = new ArrayList<>();
        long previousVersion = 0;

        for (String rawLine : manifest.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] fields = line.split("\\|", -1);
            if (fields.length != 3) {
                throw new IOException("Invalid migration manifest line: " + line);
            }

            long version;
            try {
                version = Long.parseLong(fields[0].trim());
            } catch (NumberFormatException exception) {
                throw new IOException("Invalid migration version in line: " + line, exception);
            }
            String description = fields[1].trim();
            String resource = fields[2].trim();
            if (version <= previousVersion || description.isEmpty() || resource.isEmpty()) {
                throw new IOException("Migration versions must be positive, ordered and fully described: " + line);
            }
            if (!resource.matches("sql/migration/V\\d{3,}__[A-Za-z0-9_]+\\.sql")) {
                throw new IOException("Invalid migration resource name: " + resource);
            }

            byte[] script = readResource(resource);
            migrations.add(new MigrationDefinition(version, description, resource, sha256(script)));
            previousVersion = version;
        }

        try {
            validateMigrationDefinitions(migrations);
            return List.copyOf(migrations);
        } catch (IllegalArgumentException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
    }

    static boolean shouldSkipForBootstrap(DatabaseBootstrapMode bootstrapMode) {
        return Objects.requireNonNull(bootstrapMode, "bootstrapMode").shouldBootstrap();
    }

    static MigrationDefinition migrationDefinition(long version, String description, String resource)
            throws IOException {
        return new MigrationDefinition(version, description, resource, sha256(readResource(resource)));
    }

    private static void validateMigrationDefinitions(List<MigrationDefinition> migrations) {
        if (migrations == null || migrations.isEmpty() || migrations.get(0).version() != BASELINE_VERSION) {
            throw new IllegalArgumentException("Migration manifest must start at baseline version " + BASELINE_VERSION);
        }

        long previousVersion = 0;
        for (MigrationDefinition migration : migrations) {
            if (migration == null
                    || migration.version() <= previousVersion
                    || migration.description() == null
                    || migration.description().isBlank()
                    || migration.resource() == null
                    || !migration.resource().matches("sql/migration/V\\d{3,}__[A-Za-z0-9_]+\\.sql")
                    || migration.checksum() == null
                    || !migration.checksum().matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("Invalid or unordered migration definition after version "
                        + previousVersion);
            }
            previousVersion = migration.version();
        }
    }

    private static boolean migrationsEnabled() {
        String configured = DatabaseConfig.getProperty("db.migrations.enabled", "true").trim();
        if (configured.equalsIgnoreCase("true")) {
            return true;
        }
        if (configured.equalsIgnoreCase("false")) {
            return false;
        }
        throw new IllegalStateException("db.migrations.enabled must be true or false");
    }

    private static void applyMigration(Connection connection, MigrationDefinition migration)
            throws SQLException, IOException {
        long startedAt = System.nanoTime();
        try {
            SqlScriptExecutor.executeResource(connection, migration.resource());
            recordMigration(connection, migration, elapsedMilliseconds(startedAt), true);
        } catch (SQLException | IOException exception) {
            try {
                recordMigration(connection, migration, elapsedMilliseconds(startedAt), false);
            } catch (SQLException historyException) {
                exception.addSuppressed(historyException);
            }
            throw exception;
        }
    }

    private static void synchronizeAcademicLifecycleData(Connection connection) throws SQLException {
        new AcademicLifecycleSynchronizationService(DatabaseConfig::getConnection, ApplicationClock.system())
                .synchronize(connection);
    }

    private static void validateBaselineStructure(Connection connection) throws SQLException {
        validateStructure(connection, false);
    }

    private static void validateTargetStructure(Connection connection) throws SQLException {
        validateStructure(connection, true);
    }

    private static boolean isCurrentTargetSchema(Connection connection) {
        try {
            validateTargetStructure(connection);
            return true;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private static void validateStructure(Connection connection, boolean rejectLegacyArtifacts) throws SQLException {
        SchemaSnapshot schema = loadSchemaSnapshot(connection);
        List<String> problems = new ArrayList<>();

        for (String table : REQUIRED_TABLES) {
            if (!schema.tables().contains(table)) {
                problems.add("missing table " + table);
            }
        }
        if (rejectLegacyArtifacts) {
            for (String table : FORBIDDEN_TABLES) {
                if (schema.tables().contains(table)) {
                    problems.add("obsolete table still present: " + table);
                }
            }
        }
        for (SchemaObject column : REQUIRED_COLUMNS) {
            if (!schema.columns().contains(column)) {
                problems.add("missing column " + column.display());
            }
        }
        for (SchemaObject column : REQUIRED_NOT_NULL_COLUMNS) {
            if (!schema.notNullColumns().contains(column)) {
                problems.add("column must be NOT NULL: " + column.display());
            }
        }
        for (SchemaObject index : REQUIRED_BASELINE_UNIQUE_INDEXES) {
            if (!schema.uniqueIndexes().contains(index)) {
                problems.add("missing unique index " + index.display());
            }
        }
        if (rejectLegacyArtifacts) {
            for (SchemaObject index : REQUIRED_TARGET_UNIQUE_INDEXES) {
                if (!schema.uniqueIndexes().contains(index)) {
                    problems.add("missing unique index " + index.display());
                }
            }
            for (String trigger : REQUIRED_TARGET_TRIGGERS) {
                if (!schema.triggers().contains(trigger)) {
                    problems.add("missing trigger " + trigger);
                }
            }
            for (SchemaObject column : FORBIDDEN_COLUMNS) {
                if (schema.columns().contains(column)) {
                    problems.add("obsolete column still present: " + column.display());
                }
            }
        }

        if (!problems.isEmpty()) {
            throw new SQLException(
                    (rejectLegacyArtifacts
                            ? "Database does not match the current schema migration target: "
                            : "Database cannot be baselined safely because it does not match schema migration version 1: ")
                            + String.join("; ", problems)
                            + (rejectLegacyArtifacts
                            ? ". Review the failed migration and the affected legacy data."
                            : ". No migration version was recorded. Apply a reviewed data migration or rebuild only "
                            + "a disposable database from sql/schema.sql.")
            );
        }
    }

    private static SchemaSnapshot loadSchemaSnapshot(Connection connection) throws SQLException {
        Set<String> tables = new LinkedHashSet<>();
        Set<SchemaObject> columns = new LinkedHashSet<>();
        Set<SchemaObject> notNullColumns = new LinkedHashSet<>();
        Set<SchemaObject> uniqueIndexes = new LinkedHashSet<>();
        Set<String> triggers = new LinkedHashSet<>();

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT table_name, column_name, is_nullable
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                """); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String table = normalized(resultSet.getString("table_name"));
                SchemaObject column = new SchemaObject(table, normalized(resultSet.getString("column_name")));
                tables.add(table);
                columns.add(column);
                if ("NO".equalsIgnoreCase(resultSet.getString("is_nullable"))) {
                    notNullColumns.add(column);
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT table_name, index_name
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND non_unique = 0
                """); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                uniqueIndexes.add(new SchemaObject(
                        normalized(resultSet.getString("table_name")),
                        normalized(resultSet.getString("index_name"))
                ));
            }
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT trigger_name
                FROM information_schema.triggers
                WHERE trigger_schema = DATABASE()
                """); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                triggers.add(normalized(resultSet.getString("trigger_name")));
            }
        }

        return new SchemaSnapshot(Set.copyOf(tables), Set.copyOf(columns),
                Set.copyOf(notNullColumns), Set.copyOf(uniqueIndexes), Set.copyOf(triggers));
    }

    private static int applicationTableCount(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_type = 'BASE TABLE'
                  AND table_name <> ?
                """)) {
            statement.setString(1, HISTORY_TABLE);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static void createHistoryTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS gape_schema_migration (
                        version BIGINT UNSIGNED NOT NULL,
                        description VARCHAR(200) NOT NULL,
                        script VARCHAR(300) NOT NULL,
                        checksum CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
                        installed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        execution_time_ms BIGINT UNSIGNED NOT NULL,
                        success BOOLEAN NOT NULL,
                        PRIMARY KEY (version)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                    """);
        }
    }

    private static Map<Long, InstalledMigration> loadInstalledMigrations(Connection connection) throws SQLException {
        Map<Long, InstalledMigration> migrations = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT version, description, script, checksum, success
                FROM gape_schema_migration
                ORDER BY version
                """); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                long version = resultSet.getLong("version");
                migrations.put(version, new InstalledMigration(
                        version,
                        resultSet.getString("description"),
                        resultSet.getString("script"),
                        resultSet.getString("checksum"),
                        resultSet.getBoolean("success")
                ));
            }
        }
        return migrations;
    }

    private static void validateInstalledMigrations(
            Map<Long, InstalledMigration> installed,
            List<MigrationDefinition> available
    ) throws SQLException {
        Map<Long, MigrationDefinition> definitions = new LinkedHashMap<>();
        for (MigrationDefinition migration : available) {
            definitions.put(migration.version(), migration);
        }

        for (InstalledMigration migration : installed.values()) {
            MigrationDefinition definition = definitions.get(migration.version());
            if (definition == null) {
                throw new SQLException("Installed migration version is not present in the manifest: "
                        + migration.version());
            }
            if (!migration.success()) {
                throw new SQLException("Migration version " + migration.version()
                        + " previously failed; repair it before restarting the application");
            }
            if (!MessageDigest.isEqual(
                    migration.checksum().getBytes(StandardCharsets.US_ASCII),
                    definition.checksum().getBytes(StandardCharsets.US_ASCII)
            )) {
                throw new SQLException("Checksum mismatch for installed migration version " + migration.version());
            }
            if (!migration.description().equals(definition.description())
                    || !migration.script().equals(definition.resource())) {
                throw new SQLException("Metadata mismatch for installed migration version " + migration.version());
            }
        }
    }

    private static void recordMigration(
            Connection connection,
            MigrationDefinition migration,
            long executionTimeMs,
            boolean success
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO gape_schema_migration (
                    version, description, script, checksum, execution_time_ms, success
                ) VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, migration.version());
            statement.setString(2, migration.description());
            statement.setString(3, migration.resource());
            statement.setString(4, migration.checksum());
            statement.setLong(5, executionTimeMs);
            statement.setBoolean(6, success);
            statement.executeUpdate();
        }
    }

    private static void replaceMigrationHistory(
            Connection connection,
            List<MigrationDefinition> migrations
    ) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM " + HISTORY_TABLE);
        }
        for (MigrationDefinition migration : migrations) {
            recordMigration(connection, migration, 0L, true);
        }
    }

    private static void acquireLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, ?)");) {
            statement.setString(1, MIGRATION_LOCK);
            statement.setInt(2, LOCK_TIMEOUT_SECONDS);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || resultSet.getInt(1) != 1) {
                    throw new SQLException("Timed out waiting for database migration lock");
                }
            }
        }
    }

    private static void releaseLock(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, MIGRATION_LOCK);
            statement.executeQuery();
        } catch (SQLException exception) {
            System.err.println("[GAPE][DB] Failed to release migration lock: " + exception.getMessage());
        }
    }

    private static long elapsedMilliseconds(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }

    private static byte[] readResource(String resourcePath) throws IOException {
        try (InputStream input = DatabaseMigrationService.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IOException("Missing migration resource: " + resourcePath);
            }
            return input.readAllBytes();
        }
    }

    private static String readResourceText(String resourcePath) throws IOException {
        return new String(readResource(resourcePath), StandardCharsets.UTF_8);
    }

    private static String sha256(byte[] content) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder hexadecimal = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hexadecimal.append(String.format(Locale.ROOT, "%02x", value & 0xff));
            }
            return hexadecimal.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA-256 is unavailable", exception);
        }
    }

    private static String normalized(String identifier) {
        return identifier.toLowerCase(Locale.ROOT);
    }

    record MigrationDefinition(long version, String description, String resource, String checksum) {
    }

    private record InstalledMigration(
            long version,
            String description,
            String script,
            String checksum,
            boolean success
    ) {
        private static InstalledMigration successful(MigrationDefinition migration) {
            return new InstalledMigration(migration.version(), migration.description(), migration.resource(),
                    migration.checksum(), true);
        }
    }

    private record SchemaObject(String parent, String name) {
        private String display() {
            return parent + "." + name;
        }
    }

    private record SchemaSnapshot(
            Set<String> tables,
            Set<SchemaObject> columns,
            Set<SchemaObject> notNullColumns,
            Set<SchemaObject> uniqueIndexes,
            Set<String> triggers
    ) {
    }
}
