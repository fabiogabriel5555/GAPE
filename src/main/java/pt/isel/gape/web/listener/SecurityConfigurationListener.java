package pt.isel.gape.web.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.SessionCookieConfig;
import pt.isel.gape.common.config.DatabaseConfig;

public final class SecurityConfigurationListener implements ServletContextListener {

    private static final String SECURE_COOKIE_PROPERTY = "gape.session.cookie.secure";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        DatabaseConfig.configureSimpleLogger();
        SessionCookieConfig cookieConfig = event.getServletContext().getSessionCookieConfig();
        cookieConfig.setHttpOnly(true);
        cookieConfig.setAttribute("SameSite", "Lax");

        String secureCookie = System.getProperty(SECURE_COOKIE_PROPERTY);
        if (secureCookie != null && !secureCookie.isBlank()) {
            cookieConfig.setSecure(parseStrictBoolean(secureCookie));
        }
    }

    private boolean parseStrictBoolean(String value) {
        if ("true".equalsIgnoreCase(value.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(value.trim())) {
            return false;
        }
        throw new IllegalStateException(SECURE_COOKIE_PROPERTY + " must be 'true' or 'false'");
    }
}
