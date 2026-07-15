package pt.isel.gape.common.config;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.common.sql.SqlScriptExecutor;
import pt.isel.gape.learning.dao.LearningEventDAO;
import pt.isel.gape.learning.service.AcademicLifecycleSynchronizationService;

public final class DatabaseBootstrapService {

    private static final String DROP_SCRIPT = "sql/drop.sql";
    private static final String SCHEMA_SCRIPT = "sql/schema.sql";

    public void initializeIfConfigured() {
        DatabaseBootstrapMode mode = DatabaseBootstrapMode.fromProperty(
                DatabaseConfig.getProperty("db.bootstrap.mode", "none")
        );

        if (!mode.shouldBootstrap()) {
            return;
        }

        try (Connection connection = DatabaseConfig.getConnection()) {
            initialize(connection, mode);
            System.out.println("[GAPE][DB] Bootstrap completed with mode: " + mode.name().toLowerCase());
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("Failed to bootstrap database with mode " + mode.name().toLowerCase(), exception);
        }
    }

    public void initialize(Connection connection, DatabaseBootstrapMode mode) throws SQLException, IOException {
        SqlScriptExecutor.executeResource(connection, DROP_SCRIPT);
        SqlScriptExecutor.executeResource(connection, SCHEMA_SCRIPT);

        for (String resource : mode.seedResources()) {
            SqlScriptExecutor.executeResource(connection, resource);
        }
        new AcademicLifecycleSynchronizationService(DatabaseConfig::getConnection, ApplicationClock.system())
                .synchronize(connection);
        new LearningEventDAO(DatabaseConfig::getConnection).rebuildFromCurrentRecords(connection);
    }
}
