package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.transversal.model.Channel;
import pt.isel.gape.transversal.model.ChannelCreateCommand;
import pt.isel.gape.transversal.model.ChannelState;
import pt.isel.gape.transversal.model.ChannelType;
import pt.isel.gape.transversal.model.ChannelVisibility;
import pt.isel.gape.transversal.service.ChannelService;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class ChannelServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-04T17:00:00Z"), ZoneOffset.UTC);
    private static final String IP = "127.0.0.1";

    private ChannelService channelService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        channelService = new ChannelService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void createsCommunicationChannelTypes() throws Exception {
        for (ChannelType type : List.of(
                ChannelType.MESSAGE,
                ChannelType.FORUM,
                ChannelType.COMMENTS,
                ChannelType.ANNOUNCEMENT,
                ChannelType.SYSTEM
        )) {
            Channel channel = channelService.createChannel(
                    1L,
                    100L,
                    AccessProfileType.ADMINISTRATOR,
                    new ChannelCreateCommand(
                            "Channel " + type.name(),
                            type,
                            ChannelVisibility.PARTICIPANTS,
                            ChannelState.ACTIVE,
                            List.of(),
                            List.of(),
                            List.of()
                    ),
                    IP
            );

            assertEquals(type, channel.type());
            assertTrue(hasActiveParticipation(1L, channel.id()));
        }
    }

    @Test
    void teacherCreatesClassGroupForumChannelWithContext() throws Exception {
        Channel channel = channelService.createChannel(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ChannelCreateCommand(
                        "PRJ Forum",
                        ChannelType.FORUM,
                        ChannelVisibility.PARTICIPANTS,
                        ChannelState.ACTIVE,
                        List.of(50L),
                        List.of(),
                        List.of()
                ),
                IP
        );

        assertEquals(List.of(50L), channel.classGroupIds());
        assertTrue(hasChannelClassGroup(channel.id(), 50L));
        assertTrue(hasActiveParticipation(3L, channel.id()));
    }

    @Test
    void channelRejectsIncoherentContentBlockContext() {
        assertThrows(IllegalArgumentException.class, () -> channelService.createChannel(
                1L,
                100L,
                AccessProfileType.ADMINISTRATOR,
                new ChannelCreateCommand(
                        "Wrong Context",
                        ChannelType.COMMENTS,
                        ChannelVisibility.PARTICIPANTS,
                        ChannelState.ACTIVE,
                        List.of(52L),
                        List.of(60L),
                        List.of()
                ),
                IP
        ));
    }

    @Test
    void moderationWithoutPermissionIsRejected() {
        assertThrows(SecurityException.class, () -> channelService.createChannel(
                4L,
                null,
                AccessProfileType.STUDENT,
                new ChannelCreateCommand(
                        "Student Class Channel",
                        ChannelType.FORUM,
                        ChannelVisibility.PARTICIPANTS,
                        ChannelState.ACTIVE,
                        List.of(50L),
                        List.of(),
                        List.of()
                ),
                IP
        ));
    }

    private static boolean hasChannelClassGroup(long channelId, long classGroupId) throws SQLException {
        return exists("""
                SELECT COUNT(*)
                FROM associate_channel_class_group
                WHERE id_channel = ?
                  AND id_class_group = ?
                """, channelId, classGroupId);
    }

    private static boolean hasActiveParticipation(long userId, long channelId) throws SQLException {
        return exists("""
                SELECT COUNT(*)
                FROM participate_channel
                WHERE id_user = ?
                  AND id_channel = ?
                  AND state = 'active'
                """, userId, channelId);
    }

    private static boolean exists(String sql, long first, long second) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, first);
            statement.setLong(2, second);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }
}
