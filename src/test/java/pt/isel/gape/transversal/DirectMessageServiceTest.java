package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import pt.isel.gape.transversal.model.CommunicationSnapshot;
import pt.isel.gape.transversal.model.DirectMessageContent;
import pt.isel.gape.transversal.model.Message;
import pt.isel.gape.transversal.model.MessageReceiptState;
import pt.isel.gape.transversal.model.MessageState;
import pt.isel.gape.transversal.model.MessageType;
import pt.isel.gape.transversal.service.CommunicationReadService;
import pt.isel.gape.transversal.service.DirectMessageService;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class DirectMessageServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-04T17:00:00Z"), ZoneOffset.UTC);
    private static final String IP = "127.0.0.1";

    private ConnectionProvider connectionProvider;
    private DirectMessageService directMessageService;
    private CommunicationReadService readService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        connectionProvider = DatabaseTestSupport::openConnection;
        directMessageService = new DirectMessageService(connectionProvider, FIXED_CLOCK);
        readService = new CommunicationReadService(connectionProvider);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void sendsDirectMessageAndReusesConversation() throws Exception {
        Message first = directMessageService.sendDirectMessage(
                4L,
                null,
                AccessProfileType.STUDENT,
                3L,
                "Hello, I have a question.",
                IP
        );
        Message second = directMessageService.sendDirectMessage(
                4L,
                null,
                AccessProfileType.STUDENT,
                3L,
                "This should stay in the same conversation.",
                IP
        );

        assertEquals(MessageState.SENT, first.state());
        assertEquals(4L, first.senderUserId());
        assertEquals(MessageReceiptState.DELIVERED, receiptState(3L, first.id()));
        assertEquals(1, countDirectChannels(4L, 3L));

        CommunicationSnapshot snapshot = readService.loadSnapshot(4L, 3L, second.id());

        assertFalse(snapshot.conversations().isEmpty());
        assertEquals(3L, snapshot.selectedConversation().peerUserId());
        assertEquals(2, snapshot.messages().size());
        assertEquals(second.id(), snapshot.selectedMessage().id());
    }

    @Test
    void sendsDirectAttachmentMessage() throws Exception {
        Message message = directMessageService.sendDirectMessage(
                4L,
                null,
                AccessProfileType.STUDENT,
                3L,
                "Please review this attachment.",
                "contents/pdf/unassigned/project-brief.pdf",
                "project-brief.pdf",
                IP
        );

        assertEquals(MessageState.SENT, message.state());
        assertEquals(MessageType.ATTACHMENT, message.type());
        assertEquals("project-brief.pdf", message.title());
        assertEquals("Please review this attachment.", message.body());
        assertEquals("contents/pdf/unassigned/project-brief.pdf", message.attachment());
        assertEquals(MessageReceiptState.DELIVERED, receiptState(3L, message.id()));
    }

    @Test
    void sendsAttachmentBatchInOneConversation() throws Exception {
        List<Message> messages = directMessageService.sendDirectMessages(
                4L,
                null,
                AccessProfileType.STUDENT,
                3L,
                List.of(
                        DirectMessageContent.attachment(null, "contents/pdf/unassigned/one.pdf", "one.pdf"),
                        DirectMessageContent.attachment(null, "contents/pdf/unassigned/two.pdf", "two.pdf"),
                        DirectMessageContent.text("Both files are attached.")
                ),
                IP
        );

        assertEquals(3, messages.size());
        assertEquals(1, countDirectChannels(4L, 3L));
        assertEquals(1, countDirectChannelRegistrations(4L, 3L));
        assertEquals(messages.get(0).channelId(), messages.get(2).channelId());
    }

    @Test
    void rollsBackChannelAndEarlierMessagesWhenBatchFails() throws Exception {
        String oversizedBody = "x".repeat(3001);

        assertThrows(IllegalArgumentException.class, () -> directMessageService.sendDirectMessages(
                4L,
                null,
                AccessProfileType.STUDENT,
                3L,
                List.of(
                        DirectMessageContent.text("This must be rolled back"),
                        DirectMessageContent.text(oversizedBody)
                ),
                IP
        ));

        assertEquals(0, countDirectChannels(4L, 3L));
        assertEquals(0, countDirectChannelRegistrations(4L, 3L));
        assertEquals(0, countMessagesWithTitle("This must be rolled back"));
    }

    @Test
    void registeredConversationCannotBeStructurallyCorrupted() throws Exception {
        Message message = directMessageService.sendDirectMessage(
                4L,
                null,
                AccessProfileType.STUDENT,
                3L,
                "Protected conversation",
                IP
        );

        try (Connection connection = DatabaseTestSupport.openConnection();
             Statement statement = connection.createStatement()) {
            SQLException thirdParticipant = assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO participate_channel (id_user, id_channel, role, joined_at, muted, state)
                    VALUES (5, %d, 'member', CURRENT_TIMESTAMP, FALSE, 'active')
                    """.formatted(message.channelId())));
            SQLException deactivateParticipant = assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    UPDATE participate_channel
                    SET state = 'inactive'
                    WHERE id_user = 4 AND id_channel = %d
                    """.formatted(message.channelId())));
            SQLException deactivateChannel = assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    UPDATE channel SET state = 'inactive' WHERE id_channel = %d
                    """.formatted(message.channelId())));
            SQLException addContext = assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO associate_channel_class_group (id_channel, id_class_group)
                    VALUES (%d, 50)
                    """.formatted(message.channelId())));

            DatabaseTestSupport.assertIntegrityException(thirdParticipant);
            DatabaseTestSupport.assertIntegrityException(deactivateParticipant);
            DatabaseTestSupport.assertIntegrityException(deactivateChannel);
            DatabaseTestSupport.assertIntegrityException(addContext);
        }
    }

    private static MessageReceiptState receiptState(long userId, long messageId) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT state
                     FROM receive_message
                     WHERE id_user = ?
                       AND id_message = ?
                     """)) {
            statement.setLong(1, userId);
            statement.setLong(2, messageId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return MessageReceiptState.fromDatabaseValue(resultSet.getString("state"));
            }
        }
    }

    private static int countDirectChannels(long userId, long peerUserId) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM channel c
                     JOIN participate_channel self_pc ON self_pc.id_channel = c.id_channel
                         AND self_pc.id_user = ?
                         AND self_pc.state = 'active'
                     JOIN participate_channel peer_pc ON peer_pc.id_channel = c.id_channel
                         AND peer_pc.id_user = ?
                         AND peer_pc.state = 'active'
                     WHERE c.type = 'message'
                       AND c.visibility = 'participants'
                       AND c.state = 'active'
                     """)) {
            statement.setLong(1, userId);
            statement.setLong(2, peerUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static int countDirectChannelRegistrations(long userId, long peerUserId) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM direct_message_channel
                     WHERE id_user_low = ?
                       AND id_user_high = ?
                     """)) {
            statement.setLong(1, Math.min(userId, peerUserId));
            statement.setLong(2, Math.max(userId, peerUserId));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static int countMessagesWithTitle(String title) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM message
                     WHERE title = ?
                     """)) {
            statement.setString(1, title);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}
