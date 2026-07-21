package pt.isel.gape.web.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.SessionCookieConfig;
import pt.isel.gape.common.config.DatabaseConfig;
import pt.isel.gape.security.crypto.SensitiveDataCipher;
import pt.isel.gape.security.transport.HttpsConfiguration;

public final class SecurityConfigurationListener implements ServletContextListener {

    private static final String SECURE_COOKIE_PROPERTY = "gape.session.cookie.secure";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        DatabaseConfig.configureSimpleLogger();
        SensitiveDataCipher.validateRuntimeConfiguration();
        HttpsConfiguration.validateRuntimeConfiguration();
        SessionCookieConfig cookieConfig = event.getServletContext().getSessionCookieConfig();
        cookieConfig.setHttpOnly(true);
        cookieConfig.setAttribute("SameSite", "Lax");

        String secureCookie = System.getProperty(SECURE_COOKIE_PROPERTY);
        if (secureCookie != null && !secureCookie.isBlank()) {
            boolean secure = parseStrictBoolean(secureCookie);
            if (HttpsConfiguration.fromRuntimeConfiguration().requiresHttps() && !secure) {
                throw new IllegalStateException(SECURE_COOKIE_PROPERTY
                        + " cannot be false when HTTPS enforcement is enabled");
            }
            cookieConfig.setSecure(secure);
        } else if (HttpsConfiguration.fromRuntimeConfiguration().requiresHttps()) {
            cookieConfig.setSecure(true);
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
