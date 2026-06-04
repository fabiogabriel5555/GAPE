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
                PROPERTIES.getProperty("db.url"),
                PROPERTIES.getProperty("db.user"),
                PROPERTIES.getProperty("db.password")
        );
    }

    public static String getProperty(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
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
            requireProperty(properties, "db.user");
            requireProperty(properties, "db.password");

            Class.forName(driver);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load database properties", e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Database driver class not found", e);
        }
    }

    private static String requireProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (Objects.isNull(value) || value.isBlank()) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return value;
    }
}
