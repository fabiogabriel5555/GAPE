package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;

class DatabaseConnectionTest {

    @Test
    void shouldConnectAndExecuteProbeQuery() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet resultSet = statement.executeQuery()) {

            assertNotNull(connection, "Connection must not be null");
            assertTrue(resultSet.next(), "SELECT 1 must return one row");
            assertEquals(1, resultSet.getInt(1), "Probe query should return value 1");
        }
    }
}
