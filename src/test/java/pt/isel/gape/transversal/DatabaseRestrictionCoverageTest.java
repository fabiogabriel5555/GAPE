package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * Cobertura de restricoes da base de dados (Fase 1, prompt 2.8).
 *
 * Cenario positivo: o dataset valido (seed/base.sql) carrega registos nas tabelas centrais
 * das quatro areas do modelo.
 *
 * Cenario negativo: cada statement dos scripts test (PK, FK, UNIQUE, CHECK e regras
 * application rules in triggers) and rejected by an integrity exception.
 *
 * As tabelas do modelo cobertas correspondem a lista do planeamento:
 * user_account, user_session, deletion_request, organization, organic_unit,
 * course, subject, integrate_subject, class_group, enroll_class_group, content_block,
 * content_item, lesson, physical_room, assessment, enroll_assessment, question, question_option, attempt,
 * response, certificate, message.
 */
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class DatabaseRestrictionCoverageTest {

    private static final List<String> CORE_TABLES_WITH_VALID_DATA = List.of(
            "user_account", "user_session", "organization", "organic_unit", "course",
            "course_occurrence", "course_occurrence_period", "subject",
            "integrate_subject", "class_group", "enroll_course", "enroll_class_group",
            "content_block", "content_item",
            "physical_room", "lesson", "assessment", "enroll_assessment", "question", "question_option", "attempt",
            "response", "certificate", "message", "activity_log"
    );

    private static final List<String> NEGATIVE_SCRIPTS = List.of(
            "pk.sql", "fk.sql", "unique.sql", "check.sql", "application.sql"
    );

    @Test
    void validDatasetCoversCoreTablesOfAllAreas() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            DatabaseTestSupport.resetDatabase(connection);
            DatabaseTestSupport.executeScript(connection, DatabaseTestSupport.SQL_SEED_DIR.resolve("base.sql"));

            for (String table : CORE_TABLES_WITH_VALID_DATA) {
                assertTrue(
                        DatabaseTestSupport.countRows(connection, table) > 0,
                        "Expected valid seed rows in table " + table
                );
            }
        }
    }

    @Test
    void everyNegativeScenarioIsRejectedByAnIntegrityRule() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            connection.setAutoCommit(false);
            DatabaseTestSupport.resetDatabase(connection);
            DatabaseTestSupport.executeScript(connection, DatabaseTestSupport.SQL_SEED_DIR.resolve("base.sql"));

            for (String script : NEGATIVE_SCRIPTS) {
                assertEveryStatementFails(connection, DatabaseTestSupport.SQL_TEST_DIR.resolve(script));
            }

            connection.rollback();
        }
    }

    @Test
    void inactiveSubjectsKeepExistingAssociationsButRejectNewActiveAssignments() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             Statement statement = connection.createStatement()) {
            DatabaseTestSupport.resetDatabase(connection);
            DatabaseTestSupport.executeScript(connection, DatabaseTestSupport.SQL_SEED_DIR.resolve("base.sql"));

            assertEquals(1, statement.executeUpdate("UPDATE subject SET state = 'inactive' WHERE id_subject = 40"));

            statement.executeUpdate("""
                    INSERT INTO subject (
                        id_subject, id_organization, name, acronym, description, ects, workload_hours, state
                    ) VALUES (9300, 10, 'Inactive Trigger Subject', 'ITS', NULL, 6.00, 60, 'inactive')
                    """);
            SQLException association = assertThrows(
                    SQLException.class,
                    () -> statement.executeUpdate("""
                            INSERT INTO integrate_subject (
                                id_course, id_subject, curricular_year, term, mandatory, state, ended_at
                            ) VALUES (30, 9300, 1, 'semester_1', 1, 'active', NULL)
                            """)
            );
            DatabaseTestSupport.assertIntegrityException(association);

            SQLException coordinatorAssignment = assertThrows(
                    SQLException.class,
                    () -> statement.executeUpdate("""
                            INSERT INTO coordinate_subject (id_coordinator_user, id_subject, state)
                            VALUES (2, 9300, 'active')
                            """)
            );
            DatabaseTestSupport.assertIntegrityException(coordinatorAssignment);
        }
    }

    private static void assertEveryStatementFails(Connection connection, Path scriptPath) throws Exception {
        List<String> statements = DatabaseTestSupport.parseSqlStatements(scriptPath);
        assertFalse(statements.isEmpty(), "Expected statements in " + scriptPath);

        for (String sql : statements) {
            Savepoint savepoint = connection.setSavepoint();
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
                fail("Expected integrity failure in " + scriptPath.getFileName() + " for statement: " + sql);
            } catch (SQLException exception) {
                DatabaseTestSupport.assertIntegrityException(exception);
            } finally {
                connection.rollback(savepoint);
            }
        }
    }
}
