package pt.isel.gape.transversal.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.ScheduleEventDAO;
import pt.isel.gape.learning.model.ScheduleEvent;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.dao.ChannelDAO;
import pt.isel.gape.transversal.dao.ChannelParticipationDAO;
import pt.isel.gape.transversal.dao.MessageDAO;
import pt.isel.gape.transversal.dao.MessageReceiptDAO;
import pt.isel.gape.transversal.model.Channel;
import pt.isel.gape.transversal.model.Message;
import pt.isel.gape.transversal.model.MessageCreateCommand;
import pt.isel.gape.transversal.model.MessagePriority;
import pt.isel.gape.transversal.model.MessageReceipt;
import pt.isel.gape.transversal.model.MessageReceiptState;
import pt.isel.gape.transversal.model.MessageState;
import pt.isel.gape.transversal.model.MessageType;

public final class MessageService {

    private static final int TITLE_MAX_LENGTH = 160;
    private static final int BODY_MAX_LENGTH = 3000;
    private static final int ATTACHMENT_MAX_LENGTH = 255;

    private final ConnectionProvider connectionProvider;
    private final ChannelDAO channelDAO;
    private final ChannelParticipationDAO participationDAO;
    private final MessageDAO messageDAO;
    private final MessageReceiptDAO receiptDAO;
    private final ScheduleEventDAO scheduleEventDAO;
    private final ChannelAccessPolicy accessPolicy;
    private final AuditService auditService;
    private final Clock clock;

    public MessageService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new ChannelDAO(connectionProvider),
                new ChannelParticipationDAO(connectionProvider),
                new MessageDAO(connectionProvider),
                new MessageReceiptDAO(connectionProvider),
                new ScheduleEventDAO(connectionProvider),
                new AuditService(new ActivityLogDAO(connectionProvider), clock),
                clock
        );
    }

    public MessageService(
            ConnectionProvider connectionProvider,
            ChannelDAO channelDAO,
            ChannelParticipationDAO participationDAO,
            MessageDAO messageDAO,
            MessageReceiptDAO receiptDAO,
            ScheduleEventDAO scheduleEventDAO,
            AuditService auditService,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.channelDAO = Objects.requireNonNull(channelDAO, "channelDAO is required");
        this.participationDAO = Objects.requireNonNull(participationDAO, "participationDAO is required");
        this.messageDAO = Objects.requireNonNull(messageDAO, "messageDAO is required");
        this.receiptDAO = Objects.requireNonNull(receiptDAO, "receiptDAO is required");
        this.scheduleEventDAO = Objects.requireNonNull(scheduleEventDAO, "scheduleEventDAO is required");
        this.accessPolicy = new ChannelAccessPolicy(channelDAO, participationDAO);
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public Message sendMessage(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            MessageCreateCommand command,
            String sourceIp
    ) {
        return createMessage(actorUserId, sessionId, actorProfileType, command, false, sourceIp);
    }

    public Message sendSystemMessage(
            Long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            MessageCreateCommand command,
            String sourceIp
    ) {
        return createMessage(actorUserId, sessionId, actorProfileType, command, true, sourceIp);
    }

    Message sendMessage(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            MessageCreateCommand command,
            String sourceIp
    ) throws SQLException {
        return createMessage(connection, actorUserId, sessionId, actorProfileType, command, false, sourceIp);
    }

    public int processScheduledMessages(String sourceIp) {
        LocalDateTime now = LocalDateTime.now(clock);
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                List<Message> dueMessages = messageDAO.findDueScheduledMessages(connection, now);
                for (Message message : dueMessages) {
                    messageDAO.markSent(connection, message.id(), now);
                    receiptDAO.markPendingReceiptsDelivered(connection, message.id(), now);
                    auditService.record(connection, message.senderUserId(), null, "MESSAGE_SCHEDULE_DISPATCH",
                            "message", Long.toString(message.id()), "success", sourceIp);
                }
                connection.commit();
                return dueMessages.size();
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to process scheduled messages");
        }
    }

    public MessageReceipt markRead(
            long actorUserId,
            Long sessionId,
            long messageId,
            LocalDateTime readAt,
            String sourceIp
    ) {
        Objects.requireNonNull(readAt, "readAt is required");
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                MessageReceipt receipt = receiptDAO.find(connection, actorUserId, messageId)
                        .orElseThrow(() -> new SecurityException("Message was not delivered to this user"));
                if (receipt.deliveredAt() == null || readAt.isBefore(receipt.deliveredAt())) {
                    throw new IllegalArgumentException("Read date cannot be before delivery date");
                }
                receiptDAO.markRead(connection, actorUserId, messageId, readAt);
                auditService.record(connection, actorUserId, sessionId, "MESSAGE_READ",
                        "message", Long.toString(messageId), "success", sourceIp);
                MessageReceipt updated = receiptDAO.find(connection, actorUserId, messageId)
                        .orElseThrow(() -> new IllegalStateException("Receipt disappeared after read update"));
                connection.commit();
                return updated;
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException exception) {
            auditService.record(actorUserId, sessionId, "MESSAGE_READ",
                    "message", Long.toString(messageId), "failure", sourceIp);
            throw wrap(exception, "Failed to mark message as read");
        }
    }

    public int markDirectConversationRead(
            long actorUserId,
            Long sessionId,
            long peerUserId,
            LocalDateTime readAt,
            String sourceIp
    ) {
        Objects.requireNonNull(readAt, "readAt is required");
        if (peerUserId <= 0) {
            throw new IllegalArgumentException("Conversation user is required");
        }
        if (peerUserId == actorUserId) {
            throw new IllegalArgumentException("Cannot mark a conversation with yourself as read");
        }
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                int markedRead = receiptDAO.markDirectConversationRead(connection, actorUserId, peerUserId, readAt);
                auditService.record(connection, actorUserId, sessionId, "MESSAGE_CONVERSATION_READ",
                        "user", Long.toString(peerUserId), "success", sourceIp);
                connection.commit();
                return markedRead;
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException exception) {
            auditService.record(actorUserId, sessionId, "MESSAGE_CONVERSATION_READ",
                    "user", Long.toString(peerUserId), "failure", sourceIp);
            throw wrap(exception, "Failed to mark conversation as read");
        }
    }

    private Message createMessage(
            Long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            MessageCreateCommand command,
            boolean systemGenerated,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Message message = createMessage(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            command,
                            systemGenerated,
                            sourceIp
                    );
                    connection.commit();
                    return message;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditService.record(actorUserId, sessionId, "MESSAGE_SEND",
                    "message", "new", "failure", sourceIp);
            throw wrap(exception, "Failed to send message");
        }
    }

    private Message createMessage(
            Connection connection,
            Long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            MessageCreateCommand command,
            boolean systemGenerated,
            String sourceIp
    ) throws SQLException {
        validateCommand(command);
        LocalDateTime now = LocalDateTime.now(clock);
        if (command.scheduledAt() != null && command.scheduledAt().isBefore(now)) {
            throw new IllegalArgumentException("Scheduled message date cannot be before creation date");
        }
        Channel channel = requireChannel(connection, command.channelId());
        validateSender(connection, actorUserId, actorProfileType, command, systemGenerated, channel);
        validateParentMessage(connection, command);
        List<Long> recipients = resolveRecipients(connection, command, channel);
        DispatchTimes dispatchTimes = dispatchTimes(now, command.scheduledAt());
        long messageId = messageDAO.create(
                connection,
                command.channelId(),
                command.senderUserId(),
                command.parentMessageId(),
                command.scheduleEventOriginId(),
                command.title(),
                command.body(),
                command.type(),
                command.priority() == null ? MessagePriority.NORMAL : command.priority(),
                command.attachment(),
                now,
                command.scheduledAt(),
                dispatchTimes.sentAt(),
                dispatchTimes.state()
        );
        for (Long recipientId : recipients) {
            receiptDAO.upsert(
                    connection,
                    recipientId,
                    messageId,
                    dispatchTimes.deliveredAt(),
                    null,
                    dispatchTimes.receiptState()
            );
        }
        auditService.record(connection, actorUserId, sessionId, "MESSAGE_SEND",
                "message", Long.toString(messageId), "success", sourceIp);
        return requireMessage(connection, messageId);
    }

    private void validateSender(
            Connection connection,
            Long actorUserId,
            AccessProfileType actorProfileType,
            MessageCreateCommand command,
            boolean systemGenerated,
            Channel channel
    ) throws SQLException {
        if (systemGenerated) {
            if (command.senderUserId() != null) {
                throw new IllegalArgumentException("System generated messages cannot have a sender");
            }
            if (!command.type().isSystemGeneratedType()) {
                throw new IllegalArgumentException("System generated message type is invalid");
            }
            if (actorUserId != null && actorProfileType != null) {
                accessPolicy.requireModerator(connection, actorUserId, actorProfileType, channel);
            }
            return;
        }
        if (actorUserId == null || command.senderUserId() == null || !command.senderUserId().equals(actorUserId)) {
            throw new SecurityException("Human messages must be sent by the authenticated actor");
        }
        if (!participationDAO.hasActiveParticipation(connection, actorUserId, command.channelId())) {
            throw new SecurityException("Message sender must actively participate in the channel");
        }
    }

    private void validateParentMessage(Connection connection, MessageCreateCommand command) throws SQLException {
        if (command.parentMessageId() == null) {
            return;
        }
        Message parent = messageDAO.findById(connection, command.parentMessageId())
                .orElseThrow(() -> new IllegalArgumentException("Parent message not found: " + command.parentMessageId()));
        if (parent.channelId() != command.channelId()) {
            throw new IllegalArgumentException("Reply message must belong to the same channel as the parent message");
        }
        if (command.senderUserId() != null
                && parent.senderUserId() != null
                && parent.senderUserId().equals(command.senderUserId())) {
            throw new IllegalArgumentException("Message cannot reply to itself");
        }
    }

    private List<Long> resolveRecipients(Connection connection, MessageCreateCommand command, Channel channel)
            throws SQLException {
        if (command.scheduleEventOriginId() != null) {
            return resolveScheduleEventRecipients(connection, command, channel);
        }

        List<Long> activeParticipants = participationDAO.findActiveParticipantIds(connection, command.channelId());
        List<Long> requestedRecipients = ChannelDAO.orderedUnique(command.recipientIds());
        List<Long> recipients = requestedRecipients.isEmpty() ? activeParticipants : requestedRecipients;
        if (!activeParticipants.containsAll(recipients)) {
            throw new SecurityException("Message recipients must actively participate in the channel");
        }
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("Message requires at least one recipient");
        }
        return recipients;
    }

    private List<Long> resolveScheduleEventRecipients(
            Connection connection,
            MessageCreateCommand command,
            Channel channel
    ) throws SQLException {
        ScheduleEvent event = scheduleEventDAO.findById(connection, command.scheduleEventOriginId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Schedule event not found: " + command.scheduleEventOriginId()
                ));
        if (!channel.classGroupIds().containsAll(event.classGroupIds())) {
            throw new IllegalArgumentException("Schedule message channel must match the event class groups");
        }
        LocalDateTime effectiveDate = command.scheduledAt() == null ? LocalDateTime.now(clock) : command.scheduledAt();
        if (effectiveDate.isAfter(event.endsAt())) {
            throw new IllegalArgumentException("Schedule message cannot be sent after the event period");
        }
        List<Long> eventRecipients = messageDAO.findScheduleEventRecipientIds(connection, event.id());
        List<Long> requestedRecipients = ChannelDAO.orderedUnique(command.recipientIds());
        List<Long> recipients = requestedRecipients.isEmpty() ? eventRecipients : requestedRecipients;
        if (!eventRecipients.containsAll(recipients)) {
            throw new SecurityException("Schedule message recipients must match the event recipients");
        }
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("Schedule message requires event recipients");
        }
        return recipients;
    }

    private static DispatchTimes dispatchTimes(LocalDateTime now, LocalDateTime scheduledAt) {
        if (scheduledAt != null && scheduledAt.isAfter(now)) {
            return new DispatchTimes(null, null, MessageState.SCHEDULED, MessageReceiptState.PENDING);
        }
        return new DispatchTimes(now, now, MessageState.SENT, MessageReceiptState.DELIVERED);
    }

    private Channel requireChannel(Connection connection, long channelId) throws SQLException {
        return channelDAO.findById(connection, channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));
    }

    private Message requireMessage(Connection connection, long messageId) throws SQLException {
        return messageDAO.findById(connection, messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
    }

    private static void validateCommand(MessageCreateCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.channelId() <= 0) {
            throw new IllegalArgumentException("Channel id must be positive");
        }
        Objects.requireNonNull(command.type(), "message type is required");
        requireMaxLength(command.title(), TITLE_MAX_LENGTH, "Message title is too long");
        requireMaxLength(command.body(), BODY_MAX_LENGTH, "Message body is too long");
        requireMaxLength(command.attachment(), ATTACHMENT_MAX_LENGTH, "Message attachment path is too long");
        if (command.type() == MessageType.ATTACHMENT
                && (command.attachment() == null || command.attachment().isBlank())) {
            throw new IllegalArgumentException("Attachment messages require an attachment");
        }
    }

    private static void requireMaxLength(String value, int maxLength, String message) {
        if (value != null && value.trim().length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }

    private record DispatchTimes(
            LocalDateTime sentAt,
            LocalDateTime deliveredAt,
            MessageState state,
            MessageReceiptState receiptState
    ) {
    }
}
