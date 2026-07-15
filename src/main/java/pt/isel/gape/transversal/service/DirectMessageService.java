package pt.isel.gape.transversal.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.transversal.dao.CommunicationDAO;
import pt.isel.gape.transversal.model.Channel;
import pt.isel.gape.transversal.model.ChannelCreateCommand;
import pt.isel.gape.transversal.model.ChannelParticipationCommand;
import pt.isel.gape.transversal.model.ChannelState;
import pt.isel.gape.transversal.model.ChannelType;
import pt.isel.gape.transversal.model.ChannelVisibility;
import pt.isel.gape.transversal.model.DirectMessageContent;
import pt.isel.gape.transversal.model.Message;
import pt.isel.gape.transversal.model.MessageCreateCommand;
import pt.isel.gape.transversal.model.MessagePriority;
import pt.isel.gape.transversal.model.MessageType;
import pt.isel.gape.transversal.model.ParticipationRole;

public final class DirectMessageService {

    private static final int TITLE_MAX_LENGTH = 160;

    private final ConnectionProvider connectionProvider;
    private final CommunicationDAO communicationDAO;
    private final ChannelService channelService;
    private final ChannelParticipationService participationService;
    private final MessageService messageService;

    public DirectMessageService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new CommunicationDAO(connectionProvider),
                new ChannelService(connectionProvider, clock),
                new ChannelParticipationService(connectionProvider, clock),
                new MessageService(connectionProvider, clock)
        );
    }

    DirectMessageService(
            ConnectionProvider connectionProvider,
            CommunicationDAO communicationDAO,
            ChannelService channelService,
            ChannelParticipationService participationService,
            MessageService messageService
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.communicationDAO = Objects.requireNonNull(communicationDAO, "communicationDAO is required");
        this.channelService = Objects.requireNonNull(channelService, "channelService is required");
        this.participationService = Objects.requireNonNull(participationService, "participationService is required");
        this.messageService = Objects.requireNonNull(messageService, "messageService is required");
    }

    public Message sendDirectMessage(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long recipientUserId,
            String body,
            String sourceIp
    ) {
        return sendDirectMessages(
                actorUserId,
                sessionId,
                actorProfileType,
                recipientUserId,
                List.of(DirectMessageContent.text(body)),
                sourceIp
        ).get(0);
    }

    public Message sendDirectMessage(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long recipientUserId,
            String body,
            String attachmentPath,
            String attachmentName,
            String sourceIp
    ) {
        return sendDirectMessages(
                actorUserId,
                sessionId,
                actorProfileType,
                recipientUserId,
                List.of(DirectMessageContent.attachment(body, attachmentPath, attachmentName)),
                sourceIp
        ).get(0);
    }

    public List<Message> sendDirectMessages(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long recipientUserId,
            List<DirectMessageContent> contents,
            String sourceIp
    ) {
        if (recipientUserId <= 0) {
            throw new IllegalArgumentException("Recipient is required");
        }
        if (recipientUserId == actorUserId) {
            throw new IllegalArgumentException("Cannot send a direct message to yourself");
        }
        if (contents == null || contents.isEmpty()) {
            throw new IllegalArgumentException("At least one message is required");
        }
        List<DirectMessageContent> normalizedContents = contents.stream()
                .map(DirectMessageService::normalize)
                .toList();

        SQLException concurrentRegistrationFailure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return sendDirectMessagesInTransaction(
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        recipientUserId,
                        normalizedContents,
                        sourceIp
                );
            } catch (SQLException exception) {
                if (attempt == 0 && isDuplicateKey(exception)) {
                    concurrentRegistrationFailure = exception;
                    continue;
                }
                throw new IllegalStateException("Failed to send direct message", exception);
            }
        }
        throw new IllegalStateException("Failed to register direct conversation", concurrentRegistrationFailure);
    }

    private List<Message> sendDirectMessagesInTransaction(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long recipientUserId,
            List<DirectMessageContent> contents,
            String sourceIp
    ) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                long channelId = findOrCreateChannel(
                        connection,
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        recipientUserId,
                        sourceIp
                );
                List<Message> messages = new ArrayList<>(contents.size());
                for (DirectMessageContent content : contents) {
                    MessageType messageType = content.attachmentPath() == null
                            ? MessageType.TEXT
                            : MessageType.ATTACHMENT;
                    String titleSource = content.attachmentPath() == null
                            ? content.body()
                            : (content.attachmentName() == null ? "Attachment" : content.attachmentName());
                    messages.add(messageService.sendMessage(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            new MessageCreateCommand(
                                    channelId,
                                    actorUserId,
                                    null,
                                    null,
                                    titleFrom(titleSource),
                                    content.body(),
                                    messageType,
                                    MessagePriority.NORMAL,
                                    content.attachmentPath(),
                                    null,
                                    List.of(recipientUserId)
                            ),
                            sourceIp
                    ));
                }
                connection.commit();
                return List.copyOf(messages);
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    private long findOrCreateChannel(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long recipientUserId,
            String sourceIp
    ) throws SQLException {
        var registration = communicationDAO.findDirectMessageChannelRegistration(
                connection,
                actorUserId,
                recipientUserId
        );
        if (registration.isPresent()) {
            return communicationDAO.findRegisteredDirectMessageChannel(
                    connection,
                    actorUserId,
                    recipientUserId
            ).orElseThrow(() -> new IllegalStateException(
                    "Registered direct conversation violates its channel or participant invariants"
            ));
        }

        var legacyChannel = communicationDAO.findDirectMessageChannel(connection, actorUserId, recipientUserId);
        if (legacyChannel.isPresent()) {
            communicationDAO.registerDirectMessageChannel(
                    connection,
                    actorUserId,
                    recipientUserId,
                    legacyChannel.get()
            );
            return legacyChannel.get();
        }

        Channel channel = channelService.createChannel(
                connection,
                actorUserId,
                sessionId,
                actorProfileType,
                new ChannelCreateCommand(
                        "Direct message",
                        ChannelType.MESSAGE,
                        ChannelVisibility.PARTICIPANTS,
                        ChannelState.ACTIVE,
                        List.of(),
                        List.of(),
                        List.of()
                ),
                sourceIp
        );
        participationService.addParticipation(
                connection,
                actorUserId,
                sessionId,
                actorProfileType,
                new ChannelParticipationCommand(
                        recipientUserId,
                        channel.id(),
                        ParticipationRole.MEMBER,
                        false
                ),
                sourceIp
        );
        communicationDAO.registerDirectMessageChannel(
                connection,
                actorUserId,
                recipientUserId,
                channel.id()
        );
        return channel.id();
    }

    private static DirectMessageContent normalize(DirectMessageContent content) {
        Objects.requireNonNull(content, "message content is required");
        String body = normalizeOptional(content.body());
        String attachmentPath = normalizeOptional(content.attachmentPath());
        String attachmentName = normalizeOptional(content.attachmentName());
        if (body == null && attachmentPath == null) {
            throw new IllegalArgumentException("Message body or attachment is required");
        }
        return new DirectMessageContent(body, attachmentPath, attachmentName);
    }

    private static boolean isDuplicateKey(SQLException exception) {
        for (SQLException current = exception; current != null; current = current.getNextException()) {
            if (current.getErrorCode() == 1062 || "23000".equals(current.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static String titleFrom(String body) {
        if (body.length() <= TITLE_MAX_LENGTH) {
            return body;
        }
        return body.substring(0, TITLE_MAX_LENGTH - 3) + "...";
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
