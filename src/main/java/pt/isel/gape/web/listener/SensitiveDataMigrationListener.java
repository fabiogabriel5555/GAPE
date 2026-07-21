package pt.isel.gape.web.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import pt.isel.gape.security.crypto.SensitiveDataMigrationService;

/** Runs only after bootstrap and schema migration listeners have completed. */
public final class SensitiveDataMigrationListener implements ServletContextListener {

    private final SensitiveDataMigrationService sensitiveDataMigrationService = new SensitiveDataMigrationService();

    @Override
    public void contextInitialized(ServletContextEvent event) {
        sensitiveDataMigrationService.migrateIfRequired();
    }
}
