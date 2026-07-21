package pt.isel.gape.web.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import pt.isel.gape.common.config.DatabaseBootstrapService;

public final class DatabaseBootstrapListener implements ServletContextListener {

    private final DatabaseBootstrapService bootstrapService = new DatabaseBootstrapService();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        bootstrapService.initializeIfConfigured();
    }
}
