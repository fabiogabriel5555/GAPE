package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.transversal.model.CommunicationSnapshot;
import pt.isel.gape.transversal.model.MessageType;
import pt.isel.gape.transversal.service.CommunicationReadService;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class CommunicationReadServiceTest {

    private CommunicationReadService readService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        readService = new CommunicationReadService(connectionProvider);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void loadsDirectConversationsAndMessagesWithoutExposingChannels() throws Exception {
        createDirectConversationForStudent();

        CommunicationSnapshot snapshot = readService.loadSnapshot(4L, null, null);

        assertFalse(snapshot.conversations().isEmpty());
        assertNotNull(snapshot.selectedConversation());
        assertEquals(3L, snapshot.selectedConversation().peerUserId());
        assertEquals(2, snapshot.selectedConversation().messageCount());
        assertEquals(1, snapshot.selectedConversation().unreadCount());
        assertEquals("Hello, I received it.", snapshot.selectedConversation().lastMessageBody());
        assertEquals(4L, snapshot.selectedConversation().lastMessageSenderUserId());
        assertEquals(2, snapshot.messages().size());
        assertEquals(901L, snapshot.messages().get(0).id());
        assertEquals(902L, snapshot.selectedMessage().id());
    }

    @Test
    void requestedActiveRecipientWithoutMessagesOpensEmptyDirectConversation() {
        CommunicationSnapshot snapshot = readService.loadSnapshot(4L, 2L, null);

        assertNotNull(snapshot.selectedConversation());
        assertEquals(2L, snapshot.selectedConversation().peerUserId());
        assertEquals("Coordinator User", snapshot.selectedConversation().peerName());
        assertEquals("coord@gape.local", snapshot.selectedConversation().peerEmail());
        assertEquals(0, snapshot.selectedConversation().messageCount());
        assertEquals(0, snapshot.messages().size());
    }

    @Test
    void threadLoadsLatestMessagePageAndOlderMessagesSeparately() throws Exception {
        createDirectConversationWithMessages(55);

        CommunicationSnapshot firstPage = readService.loadThread(4L, 3L, null);

        assertNotNull(firstPage.selectedConversation());
        assertEquals(55, firstPage.selectedConversation().messageCount());
        assertEquals(50, firstPage.messages().size());
        assertTrue(firstPage.hasOlderMessages());
        assertEquals(9105L, firstPage.messages().get(0).id());
        assertEquals(9154L, firstPage.messages().get(49).id());

        CommunicationSnapshot olderPage = readService.loadOlderThreadMessages(
                4L,
                3L,
                firstPage.messages().get(0).id()
        );

        assertEquals(5, olderPage.messages().size());
        assertFalse(olderPage.hasOlderMessages());
        assertEquals(9100L, olderPage.messages().get(0).id());
        assertEquals(9104L, olderPage.messages().get(4).id());
    }

    @Test
    void topbarSummaryReturnsUnreadDeliveredMessages() throws Exception {
        createDirectConversationForStudent();
        createUnreadNotificationForStudent();

        CommunicationSnapshot snapshot = readService.loadTopbarSummary(4L);

        assertEquals(1, snapshot.unreadCount());
        assertEquals(1, snapshot.notificationUnreadCount());
        assertEquals(1, snapshot.notifications().size());
        assertEquals(223L, snapshot.notifications().get(0).id());
    }

    @Test
    void loadsDirectAttachmentMessagesAndFindsThemForDownload() throws Exception {
        createDirectAttachmentConversation();

        CommunicationSnapshot snapshot = readService.loadSnapshot(4L, 3L, 9201L);

        assertNotNull(snapshot.selectedConversation());
        assertEquals(3L, snapshot.selectedConversation().peerUserId());
        assertEquals(1, snapshot.selectedConversation().messageCount());
        assertEquals(1, snapshot.selectedConversation().unreadCount());
        assertEquals("project-brief.pdf", snapshot.selectedConversation().lastMessageBody());
        assertEquals(1, snapshot.messages().size());
        assertEquals(9201L, snapshot.selectedMessage().id());
        assertEquals(MessageType.ATTACHMENT, snapshot.selectedMessage().type());
        assertEquals(
                "contents/pdf/unassigned/project-brief.pdf",
                readService.findDirectMessageForUser(4L, 9201L).orElseThrow().attachment()
        );
    }

    private static void createDirectConversationForStudent() throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement channelStatement = connection.prepareStatement("""
                     INSERT INTO channel (
                         id_channel, title, type, visibility, created_at, state
                     ) VALUES (
                         900, 'Direct message', 'message', 'participants', '2026-02-04 17:55:00', 'active'
                     )
                     """);
             PreparedStatement participationStatement = connection.prepareStatement("""
                     INSERT INTO participate_channel (
                         id_user, id_channel, role, joined_at, muted, state
                     ) VALUES
                         (3, 900, 'owner', '2026-02-04 17:55:00', 0, 'active'),
                         (4, 900, 'member', '2026-02-04 17:55:00', 0, 'active')
                     """);
             PreparedStatement messageStatement = connection.prepareStatement("""
                     INSERT INTO message (
                         id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
                         title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
                     ) VALUES
                         (901, 900, 3, NULL, NULL, 'Hello', 'Hello from the teacher.', 'text',
                          'normal', NULL, '2026-02-04 18:00:00', NULL, NULL, '2026-02-04 18:00:10', 'sent'),
                         (902, 900, 4, NULL, NULL, 'Reply', 'Hello, I received it.', 'text',
                          'normal', NULL, '2026-02-04 18:01:00', NULL, NULL, '2026-02-04 18:01:10', 'sent')
                     """);
             PreparedStatement receiptStatement = connection.prepareStatement("""
                     INSERT INTO receive_message (
                         id_user, id_message, delivered_at, read_at, state
                     ) VALUES
                         (4, 901, '2026-02-04 18:00:20', NULL, 'delivered'),
                         (3, 902, '2026-02-04 18:01:20', '2026-02-04 18:02:00', 'read')
                     """)) {
            channelStatement.executeUpdate();
            participationStatement.executeUpdate();
            messageStatement.executeUpdate();
            receiptStatement.executeUpdate();
        }
    }

    private static void createDirectConversationWithMessages(int count) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement channelStatement = connection.prepareStatement("""
                     INSERT INTO channel (
                         id_channel, title, type, visibility, created_at, state
                     ) VALUES (
                         910, 'Long direct message', 'message', 'participants', '2026-02-04 18:00:00', 'active'
                     )
                     """);
             PreparedStatement participationStatement = connection.prepareStatement("""
                     INSERT INTO participate_channel (
                         id_user, id_channel, role, joined_at, muted, state
                     ) VALUES
                         (3, 910, 'owner', '2026-02-04 18:00:00', 0, 'active'),
                         (4, 910, 'member', '2026-02-04 18:00:00', 0, 'active')
                     """);
             PreparedStatement messageStatement = connection.prepareStatement("""
                     INSERT INTO message (
                         id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
                         title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
                     ) VALUES (?, 910, ?, NULL, NULL, ?, ?, 'text', 'normal', NULL, ?, NULL, NULL, ?, 'sent')
                     """);
             PreparedStatement receiptStatement = connection.prepareStatement("""
                     INSERT INTO receive_message (
                         id_user, id_message, delivered_at, read_at, state
                     ) VALUES (?, ?, ?, ?, ?)
                     """)) {
            channelStatement.executeUpdate();
            participationStatement.executeUpdate();
            LocalDateTime base = LocalDateTime.of(2026, 2, 4, 18, 0);
            for (int index = 0; index < count; index++) {
                long messageId = 9100L + index;
                long senderUserId = index % 2 == 0 ? 3L : 4L;
                long recipientUserId = senderUserId == 3L ? 4L : 3L;
                LocalDateTime sentAt = base.plusMinutes(index);
                messageStatement.setLong(1, messageId);
                messageStatement.setLong(2, senderUserId);
                messageStatement.setString(3, "Message " + index);
                messageStatement.setString(4, "Message number " + index);
                messageStatement.setTimestamp(5, Timestamp.valueOf(sentAt));
                messageStatement.setTimestamp(6, Timestamp.valueOf(sentAt.plusSeconds(5)));
                messageStatement.addBatch();

                receiptStatement.setLong(1, recipientUserId);
                receiptStatement.setLong(2, messageId);
                receiptStatement.setTimestamp(3, Timestamp.valueOf(sentAt.plusSeconds(15)));
                receiptStatement.setTimestamp(4, Timestamp.valueOf(sentAt.plusSeconds(45)));
                receiptStatement.setString(5, "read");
                receiptStatement.addBatch();
            }
            messageStatement.executeBatch();
            receiptStatement.executeBatch();
        }
    }

    private static void createDirectAttachmentConversation() throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement channelStatement = connection.prepareStatement("""
                     INSERT INTO channel (
                         id_channel, title, type, visibility, created_at, state
                     ) VALUES (
                         920, 'Direct attachment message', 'message', 'participants', '2026-02-04 18:20:00', 'active'
                     )
                     """);
             PreparedStatement participationStatement = connection.prepareStatement("""
                     INSERT INTO participate_channel (
                         id_user, id_channel, role, joined_at, muted, state
                     ) VALUES
                         (3, 920, 'owner', '2026-02-04 18:20:00', 0, 'active'),
                         (4, 920, 'member', '2026-02-04 18:20:00', 0, 'active')
                     """);
             PreparedStatement messageStatement = connection.prepareStatement("""
                     INSERT INTO message (
                         id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
                         title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
                     ) VALUES (
                         9201, 920, 3, NULL, NULL, 'project-brief.pdf', NULL, 'attachment',
                         'normal', 'contents/pdf/unassigned/project-brief.pdf',
                         '2026-02-04 18:21:00', NULL, NULL, '2026-02-04 18:21:10', 'sent'
                     )
                     """);
             PreparedStatement receiptStatement = connection.prepareStatement("""
                     INSERT INTO receive_message (
                         id_user, id_message, delivered_at, read_at, state
                     ) VALUES (
                         4, 9201, '2026-02-04 18:21:20', NULL, 'delivered'
                     )
                     """)) {
            channelStatement.executeUpdate();
            participationStatement.executeUpdate();
            messageStatement.executeUpdate();
            receiptStatement.executeUpdate();
        }
    }

    private static void createUnreadNotificationForStudent() throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement messageStatement = connection.prepareStatement("""
                     INSERT INTO message (
                         id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
                         title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
                     ) VALUES (
                         223, 190, NULL, NULL, 140,
                         'Lesson schedule notification', 'The lesson schedule changed.', 'notification',
                         'normal', NULL, '2026-02-04 18:05:00', NULL, NULL, '2026-02-04 18:05:10', 'sent'
                     )
                     """);
             PreparedStatement receiptStatement = connection.prepareStatement("""
                     INSERT INTO receive_message (
                         id_user, id_message, delivered_at, read_at, state
                     ) VALUES (
                         4, 223, '2026-02-04 18:05:20', NULL, 'delivered'
                     )
                     """)) {
            messageStatement.executeUpdate();
            receiptStatement.executeUpdate();
        }
    }
}
