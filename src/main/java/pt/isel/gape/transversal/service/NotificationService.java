package pt.isel.gape.transversal.service;

import java.time.Clock;
import java.util.Objects;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.ScheduleEventDAO;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.dao.ChannelDAO;
import pt.isel.gape.transversal.dao.ChannelParticipationDAO;
import pt.isel.gape.transversal.dao.MessageDAO;
import pt.isel.gape.transversal.dao.MessageReceiptDAO;
import pt.isel.gape.transversal.model.Message;
import pt.isel.gape.transversal.model.MessageCreateCommand;
import pt.isel.gape.transversal.model.MessageType;
import pt.isel.gape.transversal.model.NotificationCommand;

public final class NotificationService {

    private final MessageService messageService;

    public NotificationService(ConnectionProvider connectionProvider, Clock clock) {
        Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        Objects.requireNonNull(clock, "clock is required");
        ChannelDAO channelDAO = new ChannelDAO(connectionProvider);
        ChannelParticipationDAO participationDAO = new ChannelParticipationDAO(connectionProvider);
        MessageDAO messageDAO = new MessageDAO(connectionProvider);
        MessageReceiptDAO receiptDAO = new MessageReceiptDAO(connectionProvider);
        AuditService auditService = new AuditService(new ActivityLogDAO(connectionProvider), clock);
        this.messageService = new MessageService(
                connectionProvider,
                channelDAO,
                participationDAO,
                messageDAO,
                receiptDAO,
                new ScheduleEventDAO(connectionProvider),
                auditService,
                clock
        );
    }

    public Message sendNotification(
            Long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            NotificationCommand command,
            String sourceIp
    ) {
        validateCommand(command);
        return messageService.sendSystemMessage(
                actorUserId,
                sessionId,
                actorProfileType,
                new MessageCreateCommand(
                        command.channelId(),
                        null,
                        null,
                        null,
                        command.title(),
                        command.body(),
                        command.type(),
                        command.priority(),
                        null,
                        command.scheduledAt(),
                        command.recipientIds()
                ),
                sourceIp
        );
    }

    private static void validateCommand(NotificationCommand command) {
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(command.type(), "notification type is required");
        if (!command.type().isSystemGeneratedType()) {
            throw new IllegalArgumentException("Notification service only sends system notification types");
        }
        if (command.type() == MessageType.SYSTEM && command.title() == null) {
            throw new IllegalArgumentException("System notifications require a title");
        }
    }

}
