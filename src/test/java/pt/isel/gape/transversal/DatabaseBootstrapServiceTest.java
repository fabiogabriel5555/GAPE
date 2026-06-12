package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;

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
        }
    }
}
