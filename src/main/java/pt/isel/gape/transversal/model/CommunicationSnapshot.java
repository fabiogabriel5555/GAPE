package pt.isel.gape.transversal.model;

import java.util.List;

public record CommunicationSnapshot(
        List<DirectConversationSummary> conversations,
        DirectConversationSummary selectedConversation,
        List<MessageSummary> messages,
        MessageSummary selectedMessage,
        List<MessageSummary> notifications,
        List<MessageRecipientSummary> recipients,
        int unreadCount,
        int notificationUnreadCount,
        boolean hasOlderMessages
) {
    public CommunicationSnapshot {
        conversations = conversations == null ? List.of() : List.copyOf(conversations);
        messages = messages == null ? List.of() : List.copyOf(messages);
        notifications = notifications == null ? List.of() : List.copyOf(notifications);
        recipients = recipients == null ? List.of() : List.copyOf(recipients);
    }
}
