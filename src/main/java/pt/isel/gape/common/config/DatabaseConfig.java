package pt.isel.gape.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

public final class DatabaseConfig {

    private static final String PROPERTIES_FILE = "config/db.properties";
    private static final Properties PROPERTIES = loadProperties();

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                requireResolvedProperty("db.url"),
                requireResolvedProperty("db.user"),
                requireResolvedProperty("db.password")
        );
    }

    public static String getProperty(String key, String defaultValue) {
        String value = resolvedProperty(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();

        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                throw new IllegalStateException("Missing resource: " + PROPERTIES_FILE);
            }

            properties.load(input);

            String driver = requireProperty(properties, "db.driver");
            requireProperty(properties, "db.url");

            Class.forName(driver);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load database properties", e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Database driver class not found", e);
        }
    }

    private static String requireProperty(Properties properties, String key) {
        String value = resolveValue(key, properties.getProperty(key));
        if (Objects.isNull(value) || value.isBlank()) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return value;
    }

    private static String requireResolvedProperty(String key) {
        String value = resolvedProperty(key);
        if (Objects.isNull(value) || value.isBlank()) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return value;
    }

    private static String resolvedProperty(String key) {
        return resolveValue(key, PROPERTIES.getProperty(key));
    }

    private static String resolveValue(String key, String configuredValue) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        String envValue = System.getenv(propertyEnvName(key));
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        if (configuredValue == null) {
            return null;
        }
        String trimmed = configuredValue.trim();
        if (trimmed.startsWith("${") && trimmed.endsWith("}")) {
            String envName = trimmed.substring(2, trimmed.length() - 1);
            String placeholderValue = System.getenv(envName);
            if (placeholderValue != null && !placeholderValue.isBlank()) {
                return placeholderValue;
            }
            String placeholderSystemValue = System.getProperty(envName);
            if (placeholderSystemValue != null && !placeholderSystemValue.isBlank()) {
                return placeholderSystemValue;
            }
            return "";
        }
        return configuredValue;
    }

    private static String propertyEnvName(String key) {
        return "GAPE_" + key.toUpperCase().replace('.', '_');
    }
}
