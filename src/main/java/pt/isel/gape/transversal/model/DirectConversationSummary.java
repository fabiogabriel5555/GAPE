package pt.isel.gape.transversal.model;

import java.time.LocalDateTime;

public record DirectConversationSummary(
        long peerUserId,
        String peerName,
        String peerEmail,
        int messageCount,
        int unreadCount,
        LocalDateTime lastMessageAt,
        String lastMessageBody,
        Long lastMessageSenderUserId
) {
}
