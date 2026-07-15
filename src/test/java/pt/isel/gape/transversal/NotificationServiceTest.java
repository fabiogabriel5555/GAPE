package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
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
import pt.isel.gape.transversal.model.Message;
import pt.isel.gape.transversal.model.MessagePriority;
import pt.isel.gape.transversal.model.MessageReceiptState;
import pt.isel.gape.transversal.model.MessageState;
import pt.isel.gape.transversal.model.MessageType;
import pt.isel.gape.transversal.model.NotificationCommand;
import pt.isel.gape.transversal.service.NotificationService;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class NotificationServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-04T17:00:00Z"), ZoneOffset.UTC);
    private static final String IP = "127.0.0.1";

    private NotificationService notificationService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws Exception {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        notificationService = new NotificationService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void notificationCreatesInternalSystemMessageAndReceipt() throws Exception {
        Message message = notificationService.sendNotification(
                3L,
                null,
                AccessProfileType.TEACHER,
                new NotificationCommand(
                        190L,
                        "Internal notice",
                        "Notification body",
                        MessageType.NOTIFICATION,
                        MessagePriority.NORMAL,
                        null,
                        List.of(4L)
                ),
                IP
        );

        assertEquals(MessageState.SENT, message.state());
        assertNull(message.senderUserId());
        assertEquals(MessageReceiptState.DELIVERED, receiptState(4L, message.id()));
    }

    @Test
    void invalidNotificationTypeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> notificationService.sendNotification(
                3L,
                null,
                AccessProfileType.TEACHER,
                new NotificationCommand(
                        190L,
                        "Invalid",
                        "Text is not a notification type",
                        MessageType.TEXT,
                        MessagePriority.NORMAL,
                        null,
                        List.of(4L)
                ),
                IP
        ));
    }

    @Test
    void scheduledNotificationStaysPendingUntilInternalDispatch() throws Exception {
        Message message = notificationService.sendNotification(
                3L,
                null,
                AccessProfileType.TEACHER,
                new NotificationCommand(
                        190L,
                        "Scheduled notification",
                        "Future notification",
                        MessageType.ALERT,
                        MessagePriority.HIGH,
                        LocalDateTime.of(2026, 2, 4, 18, 0),
                        List.of(4L)
                ),
                IP
        );

        assertEquals(MessageState.SCHEDULED, message.state());
        assertEquals(MessageReceiptState.PENDING, receiptState(4L, message.id()));
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
                assertTrue(resultSet.next());
                return MessageReceiptState.fromDatabaseValue(resultSet.getString("state"));
            }
        }
    }
}
