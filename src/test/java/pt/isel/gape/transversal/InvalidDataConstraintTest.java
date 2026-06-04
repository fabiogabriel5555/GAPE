package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.Test;

class InvalidDataConstraintTest {

    @Test
    void shouldRejectPkFkUniqueAndCheckViolations() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            connection.setAutoCommit(false);
            DatabaseTestSupport.resetDatabase(connection);
            DatabaseTestSupport.executeScript(connection, DatabaseTestSupport.SQL_SEED_DIR.resolve("base.sql"));

            List<String> scripts = List.of(
                    "pk.sql",
                    "fk.sql",
                    "unique.sql",
                    "check.sql"
            );

            for (String script : scripts) {
                assertScriptStatementsFail(connection, DatabaseTestSupport.SQL_TEST_DIR.resolve(script));
            }

            connection.rollback();
        }
    }

    private static void assertScriptStatementsFail(Connection connection, java.nio.file.Path scriptPath) throws Exception {
        List<String> statements = DatabaseTestSupport.parseSqlStatements(scriptPath);
        assertFalse(statements.isEmpty(), "Expected statements in " + scriptPath);

        for (String sql : statements) {
            Savepoint savepoint = connection.setSavepoint();
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
                fail("Expected integrity failure for statement: " + sql);
            } catch (SQLException exception) {
                DatabaseTestSupport.assertIntegrityException(exception);
            } finally {
                connection.rollback(savepoint);
            }
        }
    }
}
