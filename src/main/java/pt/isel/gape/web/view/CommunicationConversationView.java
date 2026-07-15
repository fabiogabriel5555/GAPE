package pt.isel.gape.web.view;

import java.time.LocalDateTime;
import java.util.Locale;

import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.transversal.model.DirectConversationSummary;

public final class CommunicationConversationView {

    private static final int PREVIEW_LIMIT = 76;

    private final DirectConversationSummary conversation;
    private final long currentUserId;

    public CommunicationConversationView(DirectConversationSummary conversation, long currentUserId) {
        this.conversation = conversation;
        this.currentUserId = currentUserId;
    }

    public long getPeerUserId() {
        return conversation.peerUserId();
    }

    public String getPeerName() {
        return isBlank(conversation.peerName()) ? "Unknown user" : conversation.peerName();
    }

    public String getPeerEmail() {
        return conversation.peerEmail();
    }

    public int getMessageCount() {
        return conversation.messageCount();
    }

    public int getUnreadCount() {
        return conversation.unreadCount();
    }

    public boolean isUnread() {
        return conversation.unreadCount() > 0;
    }

    public String getLastActivityLabel() {
        LocalDateTime lastMessageAt = conversation.lastMessageAt();
        return lastMessageAt == null ? "" : ApplicationDateTimeFormat.dateTime(lastMessageAt);
    }

    public boolean isLastMessageFromCurrentUser() {
        return conversation.lastMessageSenderUserId() != null
                && conversation.lastMessageSenderUserId() == currentUserId;
    }

    public String getLastMessagePreview() {
        String body = conversation.lastMessageBody();
        if (isBlank(body)) {
            return "No messages yet";
        }
        String normalized = body.trim().replaceAll("\\s+", " ");
        return normalized.length() <= PREVIEW_LIMIT
                ? normalized
                : normalized.substring(0, PREVIEW_LIMIT - 3) + "...";
    }

    public String getPreviewLabel() {
        return isLastMessageFromCurrentUser() ? "You: " + getLastMessagePreview() : getLastMessagePreview();
    }

    public String getInitials() {
        String name = getPeerName().trim();
        if (name.isEmpty()) {
            return "?";
        }
        String[] parts = name.split("\\s+");
        String first = parts[0].substring(0, 1);
        if (parts.length == 1) {
            return first.toUpperCase(Locale.ROOT);
        }
        String second = parts[parts.length - 1].substring(0, 1);
        return (first + second).toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
