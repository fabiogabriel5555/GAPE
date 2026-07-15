package pt.isel.gape.web.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import pt.isel.gape.common.config.DatabaseConfig;

@WebListener
public final class DatabasePoolListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        DatabaseConfig.close();
    }
}
