package pt.isel.gape.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Objects;
import java.util.Properties;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import pt.isel.gape.common.time.ApplicationClock;

public final class DatabaseConfig {

    private static final String PROPERTIES_FILE = "config/db.properties";
    private static final String[] SIMPLE_LOGGER_PROPERTY_KEYS = {
            "org.slf4j.simpleLogger.defaultLogLevel",
            "org.slf4j.simpleLogger.showDateTime",
            "org.slf4j.simpleLogger.dateTimeFormat",
            "org.slf4j.simpleLogger.showThreadName",
            "org.slf4j.simpleLogger.showLogName",
            "org.slf4j.simpleLogger.levelInBrackets"
    };
    private static final Properties PROPERTIES = loadProperties();

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        Connection connection = DataSourceHolder.DATA_SOURCE.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET time_zone = '" + databaseTimeZoneOffset(Instant.now()) + "'");
            return connection;
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    public static void close() {
        if (!DataSourceHolder.DATA_SOURCE.isClosed()) {
            DataSourceHolder.DATA_SOURCE.close();
        }
    }

    public static String getProperty(String key, String defaultValue) {
        String value = resolvedProperty(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key, Boolean.toString(defaultValue)).trim();
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalStateException("Property must be 'true' or 'false': " + key);
    }

    /**
     * Makes the SLF4J Simple settings stored in {@code config/db.properties}
     * available before the first application logger is created. Explicit JVM
     * properties keep precedence over the project configuration.
     */
    public static void configureSimpleLogger() {
        for (String key : SIMPLE_LOGGER_PROPERTY_KEYS) {
            String systemValue = System.getProperty(key);
            if (systemValue != null && !systemValue.isBlank()) {
                continue;
            }
            String configuredValue = resolvedProperty(key);
            if (configuredValue != null && !configuredValue.isBlank()) {
                System.setProperty(key, configuredValue);
            }
        }
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

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("gape-database");
        config.setDriverClassName(requireResolvedProperty("db.driver"));
        config.setJdbcUrl(requireResolvedProperty("db.url"));
        config.setUsername(requireResolvedProperty("db.user"));
        config.setPassword(requireResolvedProperty("db.password"));
        config.setMaximumPoolSize(positiveIntProperty("db.pool.maximumSize", 12));
        config.setMinimumIdle(nonNegativeIntProperty("db.pool.minimumIdle", 1));
        config.setConnectionTimeout(positiveLongProperty("db.pool.connectionTimeoutMs", 10_000L));
        config.setValidationTimeout(positiveLongProperty("db.pool.validationTimeoutMs", 5_000L));
        config.setIdleTimeout(positiveLongProperty("db.pool.idleTimeoutMs", 600_000L));
        config.setMaxLifetime(positiveLongProperty("db.pool.maxLifetimeMs", 1_800_000L));
        config.setAutoCommit(true);
        return new HikariDataSource(config);
    }

    private static int positiveIntProperty(String key, int defaultValue) {
        long value = positiveLongProperty(key, defaultValue);
        if (value > Integer.MAX_VALUE) {
            throw new IllegalStateException("Property is too large: " + key);
        }
        return (int) value;
    }

    private static int nonNegativeIntProperty(String key, int defaultValue) {
        String configured = getProperty(key, Integer.toString(defaultValue));
        try {
            int value = Integer.parseInt(configured);
            if (value < 0) {
                throw new IllegalStateException("Property must not be negative: " + key);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Property must be an integer: " + key, exception);
        }
    }

    private static long positiveLongProperty(String key, long defaultValue) {
        String configured = getProperty(key, Long.toString(defaultValue));
        try {
            long value = Long.parseLong(configured);
            if (value <= 0) {
                throw new IllegalStateException("Property must be positive: " + key);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Property must be an integer: " + key, exception);
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

    static String propertyEnvName(String key) {
        String normalized = key.toUpperCase().replace('.', '_');
        return normalized.startsWith("GAPE_") ? normalized : "GAPE_" + normalized;
    }

    static String databaseTimeZoneOffset(Instant instant) {
        String offset = ApplicationClock.ZONE.getRules().getOffset(Objects.requireNonNull(instant)).getId();
        return "Z".equals(offset) ? "+00:00" : offset;
    }

    private static final class DataSourceHolder {
        private static final HikariDataSource DATA_SOURCE = createDataSource();

        private DataSourceHolder() {
        }
    }
}
