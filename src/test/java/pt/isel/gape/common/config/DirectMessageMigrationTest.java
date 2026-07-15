package pt.isel.gape.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pt.isel.gape.transversal.DatabaseTestSupport;

class DirectMessageMigrationTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = DatabaseTestSupport.openConnection();
        DatabaseTestSupport.resetDatabase(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            DatabaseTestSupport.resetDatabase(connection);
        } finally {
            connection.close();
        }
    }

    @Test
    void invalidLegacyRegistrationStopsBeforeDiscardingConversationData() throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TRIGGER IF EXISTS bi_direct_message_channel_validate");
            statement.execute("DROP TRIGGER IF EXISTS bu_direct_message_channel_validate");
            statement.executeUpdate("""
                    INSERT INTO user_account (
                        id_user, name, email, state, language, credential_hash, credential_salt
                    ) VALUES
                        (1001, 'Legacy A', 'legacy-a@example.test', 'active', 'en', 'hash', 'salt'),
                        (1002, 'Legacy B', 'legacy-b@example.test', 'active', 'en', 'hash', 'salt'),
                        (1003, 'Legacy C', 'legacy-c@example.test', 'active', 'en', 'hash', 'salt')
                    """);
            statement.executeUpdate("""
                    INSERT INTO channel (id_channel, title, type, visibility, created_at, state)
                    VALUES (2001, 'Invalid legacy direct channel', 'message', 'participants', CURRENT_TIMESTAMP, 'active')
                    """);
            statement.executeUpdate("""
                    INSERT INTO participate_channel (id_user, id_channel, role, joined_at, muted, state)
                    VALUES
                        (1001, 2001, 'owner', CURRENT_TIMESTAMP, FALSE, 'active'),
                        (1002, 2001, 'member', CURRENT_TIMESTAMP, FALSE, 'active'),
                        (1003, 2001, 'member', CURRENT_TIMESTAMP, FALSE, 'active')
                    """);
            statement.executeUpdate("""
                    INSERT INTO direct_message_channel (id_user_low, id_user_high, id_channel)
                    VALUES (1001, 1002, 2001)
                    """);
        }

        SQLException exception = assertThrows(
                SQLException.class,
                () -> new DatabaseMigrationService().migrate(connection)
        );

        assertEquals("45000", exception.getSQLState());
        assertEquals(3, activeParticipantCount(2001L));
        assertEquals(1, migrationHistoryCount(5L, false));
    }

    private int activeParticipantCount(long channelId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM participate_channel
                WHERE id_channel = ? AND state = 'active'
                """)) {
            statement.setLong(1, channelId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private int migrationHistoryCount(long version, boolean success) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM gape_schema_migration
                WHERE version = ? AND success = ?
                """)) {
            statement.setLong(1, version);
            statement.setBoolean(2, success);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}
