package pt.isel.gape.transversal.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.transversal.dao.CommunicationDAO;
import pt.isel.gape.transversal.model.CommunicationSnapshot;
import pt.isel.gape.transversal.model.DirectConversationSummary;
import pt.isel.gape.transversal.model.MessageRecipientSummary;
import pt.isel.gape.transversal.model.MessageSummary;

public final class CommunicationReadService {

    private static final int TOPBAR_NOTIFICATION_LIMIT = 5;
    private static final int RECIPIENT_LIMIT = 250;
    private static final int MESSAGE_PAGE_SIZE = 50;

    private final ConnectionProvider connectionProvider;
    private final CommunicationDAO communicationDAO;

    public CommunicationReadService(ConnectionProvider connectionProvider) {
        this(connectionProvider, new CommunicationDAO(connectionProvider));
    }

    CommunicationReadService(ConnectionProvider connectionProvider, CommunicationDAO communicationDAO) {
        this.connectionProvider = connectionProvider;
        this.communicationDAO = communicationDAO;
    }

    public CommunicationSnapshot loadSnapshot(
            long userId,
            Long requestedPeerUserId,
            Long requestedMessageId
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            List<DirectConversationSummary> conversations =
                    communicationDAO.findDirectConversationsForUser(connection, userId);
            Long peerUserId = requestedPeerUserId;
            if (peerUserId == null && requestedMessageId != null) {
                peerUserId = communicationDAO.findDirectPeerUserIdForMessage(connection, userId, requestedMessageId)
                        .orElse(null);
            }
            List<MessageRecipientSummary> recipients =
                    communicationDAO.findActiveMessageRecipients(connection, userId, RECIPIENT_LIMIT);
            DirectConversationSummary selectedConversation = selectExistingConversation(conversations, peerUserId);
            if (selectedConversation == null && peerUserId != null) {
                selectedConversation = selectedRecipientConversation(connection, userId, peerUserId, recipients);
            }
            if (selectedConversation == null && !conversations.isEmpty()) {
                selectedConversation = conversations.get(0);
            }
            MessagePage page = selectedConversation == null
                    ? new MessagePage(List.of(), false)
                    : messagePage(connection, userId, selectedConversation.peerUserId(), null);
            List<MessageSummary> messages = page.messages();
            boolean hasOlderMessages = page.hasOlderMessages();
            MessageSummary selectedMessage = selectMessage(messages, requestedMessageId);
            List<MessageSummary> notifications = communicationDAO.findUnreadNotifications(
                    connection,
                    userId,
                    TOPBAR_NOTIFICATION_LIMIT
            );
            int unreadCount = communicationDAO.countUnreadDirectMessages(connection, userId);
            int notificationUnreadCount = communicationDAO.countUnreadDeliveredNotifications(connection, userId);
            return new CommunicationSnapshot(
                    conversations,
                    selectedConversation,
                    messages,
                    selectedMessage,
                    notifications,
                    recipients,
                    unreadCount,
                    notificationUnreadCount,
                    hasOlderMessages
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load communication data", exception);
        }
    }

    public CommunicationSnapshot loadThread(
            long userId,
            Long requestedPeerUserId,
            Long requestedMessageId
    ) {
        return loadThread(userId, requestedPeerUserId, requestedMessageId, null);
    }

    public CommunicationSnapshot loadOlderThreadMessages(
            long userId,
            long requestedPeerUserId,
            long beforeMessageId
    ) {
        return loadThread(userId, requestedPeerUserId, null, beforeMessageId);
    }

    public CommunicationSnapshot loadTopbarSummary(long userId) {
        try (Connection connection = connectionProvider.getConnection()) {
            List<MessageSummary> notifications = communicationDAO.findUnreadNotifications(
                    connection,
                    userId,
                    TOPBAR_NOTIFICATION_LIMIT
            );
            int unreadCount = communicationDAO.countUnreadDirectMessages(connection, userId);
            int notificationUnreadCount = communicationDAO.countUnreadDeliveredNotifications(connection, userId);
            return new CommunicationSnapshot(
                    List.of(),
                    null,
                    List.of(),
                    null,
                    notifications,
                    List.of(),
                    unreadCount,
                    notificationUnreadCount,
                    false
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load notification summary", exception);
        }
    }

    public int countUnreadDirectMessages(long userId) {
        try (Connection connection = connectionProvider.getConnection()) {
            return communicationDAO.countUnreadDirectMessages(connection, userId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to count unread direct messages", exception);
        }
    }

    public Optional<MessageSummary> findDirectMessageForUser(long userId, long messageId) {
        try (Connection connection = connectionProvider.getConnection()) {
            return communicationDAO.findDirectMessageForUser(connection, userId, messageId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load direct message", exception);
        }
    }

    private CommunicationSnapshot loadThread(
            long userId,
            Long requestedPeerUserId,
            Long requestedMessageId,
            Long beforeMessageId
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            Long peerUserId = requestedPeerUserId;
            if (peerUserId == null && requestedMessageId != null) {
                peerUserId = communicationDAO.findDirectPeerUserIdForMessage(connection, userId, requestedMessageId)
                        .orElse(null);
            }
            DirectConversationSummary selectedConversation = null;
            if (peerUserId != null) {
                Optional<DirectConversationSummary> conversation =
                        communicationDAO.findDirectConversationForUserAndPeer(connection, userId, peerUserId);
                selectedConversation = conversation.isPresent()
                        ? conversation.get()
                        : selectedRecipientConversation(connection, userId, peerUserId, List.of());
            }
            MessagePage page = selectedConversation == null
                    ? new MessagePage(List.of(), false)
                    : messagePage(connection, userId, selectedConversation.peerUserId(), beforeMessageId);
            int unreadCount = communicationDAO.countUnreadDirectMessages(connection, userId);
            return new CommunicationSnapshot(
                    List.of(),
                    selectedConversation,
                    page.messages(),
                    selectMessage(page.messages(), requestedMessageId),
                    List.of(),
                    List.of(),
                    unreadCount,
                    0,
                    page.hasOlderMessages()
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load communication thread", exception);
        }
    }

    private MessagePage messagePage(
            Connection connection,
            long userId,
            long peerUserId,
            Long beforeMessageId
    ) throws SQLException {
        List<MessageSummary> messages = beforeMessageId == null
                ? communicationDAO.findDirectMessagesForUserAndPeer(
                        connection,
                        userId,
                        peerUserId,
                        MESSAGE_PAGE_SIZE + 1
                )
                : communicationDAO.findDirectMessagesForUserAndPeerBefore(
                        connection,
                        userId,
                        peerUserId,
                        beforeMessageId,
                        MESSAGE_PAGE_SIZE + 1
                );
        boolean hasOlderMessages = messages.size() > MESSAGE_PAGE_SIZE;
        if (hasOlderMessages) {
            messages = messages.subList(1, messages.size());
        }
        return new MessagePage(messages, hasOlderMessages);
    }

    private static DirectConversationSummary selectExistingConversation(
            List<DirectConversationSummary> conversations,
            Long requestedPeerUserId
    ) {
        if (requestedPeerUserId == null) {
            return conversations.isEmpty() ? null : conversations.get(0);
        }
        for (DirectConversationSummary conversation : conversations) {
            if (conversation.peerUserId() == requestedPeerUserId) {
                return conversation;
            }
        }
        return null;
    }

    private DirectConversationSummary selectedRecipientConversation(
            Connection connection,
            long userId,
            long peerUserId,
            List<MessageRecipientSummary> recipients
    ) throws SQLException {
        Optional<MessageRecipientSummary> recipient = recipients.stream()
                .filter(candidate -> candidate.userId() == peerUserId)
                .findFirst();
        if (recipient.isEmpty()) {
            recipient = communicationDAO.findActiveMessageRecipient(connection, userId, peerUserId);
        }
        return recipient.map(CommunicationReadService::emptyConversation).orElse(null);
    }

    private static DirectConversationSummary emptyConversation(MessageRecipientSummary recipient) {
        return new DirectConversationSummary(
                recipient.userId(),
                recipient.name(),
                recipient.email(),
                0,
                0,
                null,
                null,
                null
        );
    }

    private static MessageSummary selectMessage(List<MessageSummary> messages, Long requestedMessageId) {
        if (messages.isEmpty()) {
            return null;
        }
        if (requestedMessageId != null) {
            for (MessageSummary message : messages) {
                if (message.id() == requestedMessageId) {
                    return message;
                }
            }
        }
        return messages.get(messages.size() - 1);
    }

    private record MessagePage(List<MessageSummary> messages, boolean hasOlderMessages) {
        private MessagePage {
            messages = messages == null ? List.of() : List.copyOf(messages);
        }
    }
}
