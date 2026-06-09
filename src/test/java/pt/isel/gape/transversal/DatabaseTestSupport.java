package pt.isel.gape.transversal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pt.isel.gape.common.config.DatabaseConfig;
import pt.isel.gape.common.sql.SqlScriptExecutor;

public final class DatabaseTestSupport {

    public static final Path SQL_DIR = Path.of("src/main/resources/sql");
    public static final Path SQL_SEED_DIR = SQL_DIR.resolve("seed");
    public static final Path SQL_TEST_DIR = SQL_DIR.resolve("test");

    private DatabaseTestSupport() {
    }

    public static Connection openConnection() throws SQLException {
        Connection connection = DatabaseConfig.getConnection();
        setStrictSqlMode(connection);
        return connection;
    }

    public static void resetDatabase(Connection connection) throws Exception {
        dropCurrentSchemaObjects(connection);
        executeScript(connection, SQL_DIR.resolve("schema.sql"));
    }

    public static void dropCurrentSchemaObjects(Connection connection) throws SQLException {
        dropCurrentSchemaTriggers(connection);
        dropCurrentSchemaTables(connection);
    }

    public static void dropCurrentSchemaTriggers(Connection connection) throws SQLException {
        List<String> triggerNames = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT trigger_name
                FROM information_schema.triggers
                WHERE trigger_schema = ?
                """)) {
            statement.setString(1, currentSchema(connection));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    triggerNames.add(resultSet.getString(1));
                }
            }
        }

        try (Statement statement = connection.createStatement()) {
            for (String triggerName : triggerNames) {
                statement.execute("DROP TRIGGER IF EXISTS " + quoteIdentifier(triggerName));
            }
        }
    }

    public static void dropCurrentSchemaTables(Connection connection) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_type = 'BASE TABLE'
                """)) {
            statement.setString(1, currentSchema(connection));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tableNames.add(resultSet.getString(1));
                }
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                for (String tableName : tableNames) {
                    statement.execute("DROP TABLE IF EXISTS " + quoteIdentifier(tableName));
                }
            } finally {
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
    }

    public static void executeScript(Connection connection, Path scriptPath) throws Exception {
        List<String> statements = parseSqlStatements(scriptPath);
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    public static List<String> parseSqlStatements(Path scriptPath) throws IOException {
        return SqlScriptExecutor.parseStatements(Files.readString(scriptPath, StandardCharsets.UTF_8));
    }

    public static String currentSchema(Connection connection) throws SQLException {
        String catalog = connection.getCatalog();
        if (catalog != null && !catalog.isBlank()) {
            return catalog;
        }
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT DATABASE()")) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        throw new SQLException("Could not resolve current schema/database");
    }

    public static int countRows(Connection connection, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    public static boolean existsConstraint(Connection connection, String tableName, String constraintName, String type)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = ?
                  AND table_name = ?
                  AND LOWER(constraint_name) = LOWER(?)
                  AND constraint_type = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currentSchema(connection));
            statement.setString(2, tableName);
            statement.setString(3, constraintName.toLowerCase(Locale.ROOT));
            statement.setString(4, type);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public static boolean isNotNullColumn(Connection connection, String tableName, String columnName) throws SQLException {
        String sql = """
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = ?
                  AND table_name = ?
                  AND column_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currentSchema(connection));
            statement.setString(2, tableName);
            statement.setString(3, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return false;
                }
                return "NO".equalsIgnoreCase(resultSet.getString(1));
            }
        }
    }

    public static boolean isUniqueIndex(Connection connection, String tableName, String indexName) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = ?
                  AND table_name = ?
                  AND LOWER(index_name) = LOWER(?)
                  AND non_unique = 0
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currentSchema(connection));
            statement.setString(2, tableName);
            statement.setString(3, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public static boolean existsTrigger(Connection connection, String triggerName) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.triggers
                WHERE trigger_schema = ?
                  AND LOWER(trigger_name) = LOWER(?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currentSchema(connection));
            statement.setString(2, triggerName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public static void assertIntegrityException(SQLException exception) {
        String sqlState = exception.getSQLState();
        int errorCode = exception.getErrorCode();

        if (sqlState != null && sqlState.startsWith("23")) {
            return;
        }

        if (errorCode == 1062 || errorCode == 1452 || errorCode == 1048 || errorCode == 3819) {
            return;
        }

        if ("45000".equals(sqlState) && errorCode == 1644) {
            return;
        }

        throw new AssertionError(
                "Expected integrity constraint exception, got SQLState=" + sqlState + " errorCode=" + errorCode,
                exception
        );
    }

    private static void setStrictSqlMode(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    SET SESSION sql_mode =
                    'STRICT_TRANS_TABLES,ONLY_FULL_GROUP_BY,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION'
                    """);
        }
    }

    private static String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }
}
