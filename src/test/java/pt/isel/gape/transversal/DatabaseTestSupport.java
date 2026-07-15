package pt.isel.gape.transversal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import pt.isel.gape.common.config.DatabaseConfig;
import pt.isel.gape.common.sql.SqlScriptExecutor;

public final class DatabaseTestSupport {

    public static final Path SQL_DIR = Path.of("src/main/resources/sql");
    public static final Path SQL_SEED_DIR = SQL_DIR.resolve("seed");
    public static final Path SQL_TEST_DIR = SQL_DIR.resolve("test");

    private static final Map<Path, List<String>> SQL_STATEMENT_CACHE = new ConcurrentHashMap<>();

    private static Set<String> fullSchemaTables = Set.of();
    private static boolean fullSchemaReady;
    private static boolean truncateFastResetSupported = true;
    private static final ThreadLocal<Connection> TEST_TRANSACTION_CONNECTION = new ThreadLocal<>();

    private DatabaseTestSupport() {
    }

    public static Connection openConnection() throws SQLException {
        Connection testConnection = TEST_TRANSACTION_CONNECTION.get();
        if (testConnection != null) {
            return closeShield(testConnection);
        }
        return openPhysicalConnection();
    }

    public static void beginTestTransaction() throws SQLException {
        if (TEST_TRANSACTION_CONNECTION.get() != null) {
            throw new IllegalStateException("A test transaction is already active in this thread");
        }
        Connection connection = openPhysicalConnection();
        connection.setAutoCommit(false);
        TEST_TRANSACTION_CONNECTION.set(connection);
    }

    public static void rollbackTestTransaction() throws SQLException {
        Connection connection = TEST_TRANSACTION_CONNECTION.get();
        TEST_TRANSACTION_CONNECTION.remove();
        if (connection == null) {
            return;
        }

        try {
            connection.rollback();
        } finally {
            connection.close();
        }
    }

    private static Connection openPhysicalConnection() throws SQLException {
        Connection connection = DatabaseConfig.getConnection();
        setStrictSqlMode(connection);
        return connection;
    }

    public static synchronized void resetDatabase(Connection connection) throws Exception {
        if (canFastReset(connection)) {
            clearCurrentSchemaTables(connection, fullSchemaTables);
            return;
        }

        dropCurrentSchemaObjectsWithoutInvalidting(connection);
        executeScript(connection, SQL_DIR.resolve("schema.sql"));
        fullSchemaTables = currentBaseTableNames(connection);
        fullSchemaReady = true;
    }

    public static void resetDatabaseWithBaseSeed() throws Exception {
        try (Connection connection = openConnection()) {
            resetDatabase(connection);
            executeScript(connection, SQL_SEED_DIR.resolve("base.sql"));
        }
    }

    public static synchronized void dropCurrentSchemaObjects(Connection connection) throws SQLException {
        fullSchemaReady = false;
        dropCurrentSchemaObjectsWithoutInvalidting(connection);
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
        boolean manageTransaction = connection.getAutoCommit();
        if (manageTransaction) {
            connection.setAutoCommit(false);
        }

        try (Statement statement = connection.createStatement()) {
            for (int index = 0; index < statements.size(); index++) {
                String sql = statements.get(index);
                try {
                    statement.execute(sql);
                } catch (SQLException exception) {
                    throw withStatementContext(exception, index, sql);
                }
            }
            if (manageTransaction) {
                connection.commit();
            }
        } catch (SQLException | RuntimeException exception) {
            if (manageTransaction) {
                rollbackQuietly(connection);
            }
            throw exception;
        } finally {
            if (manageTransaction) {
                connection.setAutoCommit(true);
            }
        }
    }

    public static List<String> parseSqlStatements(Path scriptPath) throws IOException {
        Path cacheKey = scriptPath.toAbsolutePath().normalize();
        try {
            return SQL_STATEMENT_CACHE.computeIfAbsent(cacheKey, path -> {
                try {
                    return List.copyOf(SqlScriptExecutor.parseStatements(Files.readString(path, StandardCharsets.UTF_8)));
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
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

    private static boolean canFastReset(Connection connection) throws SQLException {
        return fullSchemaReady
                && !fullSchemaTables.isEmpty()
                && currentBaseTableNames(connection).equals(fullSchemaTables);
    }

    private static void clearCurrentSchemaTables(
            Connection connection,
            Set<String> tableNames
    ) throws SQLException {
        if (truncateFastResetSupported && truncateTables(connection, tableNames)) {
            return;
        }

        deleteTableData(connection, tableNames);
    }

    private static boolean truncateTables(Connection connection, Set<String> tableNames) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                for (String tableName : tableNames) {
                    statement.execute("TRUNCATE TABLE " + quoteIdentifier(tableName));
                }
                return true;
            } catch (SQLException exception) {
                truncateFastResetSupported = false;
                return false;
            } finally {
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
    }

    private static void deleteTableData(Connection connection, Set<String> tableNames) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                for (String tableName : tableNames) {
                    statement.execute("DELETE FROM " + quoteIdentifier(tableName));
                }
            } finally {
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
    }

    private static void dropCurrentSchemaObjectsWithoutInvalidting(Connection connection) throws SQLException {
        dropCurrentSchemaTriggers(connection);
        dropCurrentSchemaRoutines(connection);
        dropCurrentSchemaTables(connection);
    }

    private static void dropCurrentSchemaRoutines(Connection connection) throws SQLException {
        List<String> procedures = new ArrayList<>();
        List<String> functions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT routine_name, routine_type
                FROM information_schema.routines
                WHERE routine_schema = ?
                """)) {
            statement.setString(1, currentSchema(connection));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String routineName = quoteIdentifier(resultSet.getString("routine_name"));
                    if ("FUNCTION".equalsIgnoreCase(resultSet.getString("routine_type"))) {
                        functions.add(routineName);
                    } else {
                        procedures.add(routineName);
                    }
                }
            }
        }

        try (Statement statement = connection.createStatement()) {
            for (String procedure : procedures) {
                statement.execute("DROP PROCEDURE IF EXISTS " + procedure);
            }
            for (String function : functions) {
                statement.execute("DROP FUNCTION IF EXISTS " + function);
            }
        }
    }

    private static Set<String> currentBaseTableNames(Connection connection) throws SQLException {
        Set<String> tableNames = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """)) {
            statement.setString(1, currentSchema(connection));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tableNames.add(resultSet.getString(1));
                }
            }
        }
        return Set.copyOf(tableNames);
    }

    private static SQLException withStatementContext(SQLException exception, int index, String sql) {
        String preview = sql.replaceAll("\\s+", " ").trim();
        if (preview.length() > 180) {
            preview = preview.substring(0, 180) + "...";
        }

        SQLException contextual = new SQLException(
                "Failed SQL statement #" + (index + 1) + ": " + preview,
                exception.getSQLState(),
                exception.getErrorCode(),
                exception
        );
        contextual.setNextException(exception);
        return contextual;
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Preserve the original exception from script execution.
        }
    }

    private static Connection closeShield(Connection connection) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                new TestTransactionConnectionHandler(connection)
        );
    }

    private static final class TestTransactionConnectionHandler implements InvocationHandler {

        private final Connection connection;
        private Savepoint serviceSavepoint;

        private TestTransactionConnectionHandler(Connection connection) {
            this.connection = connection;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (methodName.equals("close") && method.getParameterCount() == 0) {
                return null;
            }
            if (methodName.equals("getAutoCommit") && method.getParameterCount() == 0) {
                return false;
            }
            if (methodName.equals("setAutoCommit") && method.getParameterCount() == 1) {
                boolean autoCommit = (Boolean) args[0];
                if (!autoCommit && serviceSavepoint == null) {
                    serviceSavepoint = connection.setSavepoint();
                }
                return null;
            }
            if (methodName.equals("commit") && method.getParameterCount() == 0) {
                releaseServiceSavepoint();
                return null;
            }
            if (methodName.equals("rollback") && method.getParameterCount() == 0) {
                if (serviceSavepoint != null) {
                    connection.rollback(serviceSavepoint);
                    releaseServiceSavepoint();
                }
                return null;
            }

            try {
                return method.invoke(connection, args);
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
        }

        private void releaseServiceSavepoint() throws SQLException {
            if (serviceSavepoint == null) {
                return;
            }
            try {
                connection.releaseSavepoint(serviceSavepoint);
            } finally {
                serviceSavepoint = null;
            }
        }
    }

    private static String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }
}
