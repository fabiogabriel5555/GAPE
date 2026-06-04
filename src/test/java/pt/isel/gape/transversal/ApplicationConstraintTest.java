package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.Test;

class ApplicationConstraintTest {

    @Test
    void shouldRejectApplicationRulesAlreadyEncodedInSql() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            connection.setAutoCommit(false);
            DatabaseTestSupport.resetDatabase(connection);
            DatabaseTestSupport.executeScript(connection, DatabaseTestSupport.SQL_SEED_DIR.resolve("base.sql"));

            Path script = DatabaseTestSupport.SQL_TEST_DIR.resolve("application.sql");
            List<String> statements = DatabaseTestSupport.parseSqlStatements(script);

            assertFalse(statements.isEmpty(), "Expected statements in " + script);

            for (String sql : statements) {
                Savepoint savepoint = connection.setSavepoint();
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                    fail("Expected application-level SQL constraint failure: " + sql);
                } catch (SQLException exception) {
                    DatabaseTestSupport.assertIntegrityException(exception);
                } finally {
                    connection.rollback(savepoint);
                }
            }

            connection.rollback();
        }
    }
}
