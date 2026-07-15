package pt.isel.gape.web.view;

import java.util.List;

public final class CommunicationPageData {

    private final List<CommunicationConversationView> conversations;
    private final CommunicationConversationView selectedConversation;
    private final List<CommunicationMessageView> messages;
    private final CommunicationMessageView selectedMessage;
    private final List<CommunicationMessageView> notifications;
    private final List<CommunicationRecipientView> recipients;
    private final int unreadCount;
    private final boolean hasOlderMessages;

    public CommunicationPageData(
            List<CommunicationConversationView> conversations,
            CommunicationConversationView selectedConversation,
            List<CommunicationMessageView> messages,
            CommunicationMessageView selectedMessage,
            List<CommunicationMessageView> notifications,
            List<CommunicationRecipientView> recipients,
            int unreadCount,
            boolean hasOlderMessages
    ) {
        this.conversations = List.copyOf(conversations);
        this.selectedConversation = selectedConversation;
        this.messages = List.copyOf(messages);
        this.selectedMessage = selectedMessage;
        this.notifications = List.copyOf(notifications);
        this.recipients = List.copyOf(recipients);
        this.unreadCount = unreadCount;
        this.hasOlderMessages = hasOlderMessages;
    }

    public List<CommunicationConversationView> getConversations() {
        return conversations;
    }

    public CommunicationConversationView getSelectedConversation() {
        return selectedConversation;
    }

    public List<CommunicationMessageView> getMessages() {
        return messages;
    }

    public CommunicationMessageView getSelectedMessage() {
        return selectedMessage;
    }

    public List<CommunicationMessageView> getNotifications() {
        return notifications;
    }

    public List<CommunicationRecipientView> getRecipients() {
        return recipients;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public int getMessageCount() {
        return selectedConversation == null ? messages.size() : selectedConversation.getMessageCount();
    }

    public int getLoadedMessageCount() {
        return messages.size();
    }

    public boolean isHasOlderMessages() {
        return hasOlderMessages;
    }

    public Long getOldestMessageId() {
        return messages.isEmpty() ? null : messages.get(0).getId();
    }

    public int getSentCount() {
        int count = 0;
        for (CommunicationMessageView message : messages) {
            if (message.isFromCurrentUser()) {
                count++;
            }
        }
        return count;
    }

    public int getReceivedCount() {
        return messages.size() - getSentCount();
    }

    public boolean isHasSelectedMessage() {
        return selectedMessage != null;
    }

    public int getConversationCount() {
        return conversations.size();
    }

    public boolean isHasConversations() {
        return !conversations.isEmpty();
    }

    public boolean isHasSelectedConversation() {
        return selectedConversation != null;
    }
}
