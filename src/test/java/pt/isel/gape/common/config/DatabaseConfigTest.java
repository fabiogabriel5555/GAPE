package pt.isel.gape.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DatabaseConfigTest {

    private static final String BOOLEAN_PROPERTY = "gape.test.strictBoolean";

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty(BOOLEAN_PROPERTY);
    }

    @Test
    void environmentNamesDoNotDuplicateTheApplicationPrefix() {
        assertEquals("GAPE_UPLOAD_DIR", DatabaseConfig.propertyEnvName("gape.upload.dir"));
        assertEquals("GAPE_DB_URL", DatabaseConfig.propertyEnvName("db.url"));
    }

    @Test
    void booleanPropertiesAcceptOnlyExplicitBooleanValues() {
        System.setProperty(BOOLEAN_PROPERTY, "true");
        assertTrue(DatabaseConfig.getBooleanProperty(BOOLEAN_PROPERTY, false));

        System.setProperty(BOOLEAN_PROPERTY, "FALSE");
        assertFalse(DatabaseConfig.getBooleanProperty(BOOLEAN_PROPERTY, true));

        System.setProperty(BOOLEAN_PROPERTY, "yes");
        assertThrows(
                IllegalStateException.class,
                () -> DatabaseConfig.getBooleanProperty(BOOLEAN_PROPERTY, false)
        );
    }

    @Test
    void databaseOffsetTracksLisbonDaylightSavingTime() {
        assertEquals("+00:00", DatabaseConfig.databaseTimeZoneOffset(Instant.parse("2026-01-15T12:00:00Z")));
        assertEquals("+01:00", DatabaseConfig.databaseTimeZoneOffset(Instant.parse("2026-07-15T12:00:00Z")));
    }

    @Test
    void eachCheckedOutConnectionUsesTheCurrentLisbonOffset() throws Exception {
        String expectedOffset = DatabaseConfig.databaseTimeZoneOffset(Instant.now());
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT @@session.time_zone")) {
            resultSet.next();
            assertEquals(expectedOffset, resultSet.getString(1));
        }
    }
}
