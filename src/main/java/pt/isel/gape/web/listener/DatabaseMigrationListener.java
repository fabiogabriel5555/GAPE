package pt.isel.gape.web.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import pt.isel.gape.common.config.DatabaseMigrationService;

public final class DatabaseMigrationListener implements ServletContextListener {

    private final DatabaseMigrationService migrationService = new DatabaseMigrationService();

    @Override
    public void contextInitialized(ServletContextEvent event) {
        migrationService.migrateIfConfigured();
    }
}
