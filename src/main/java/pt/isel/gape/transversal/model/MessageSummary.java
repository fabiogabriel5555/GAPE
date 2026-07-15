package pt.isel.gape.transversal.model;

import java.time.LocalDateTime;

public record MessageSummary(
        long id,
        long channelId,
        Long senderUserId,
        String senderName,
        String senderEmail,
        Long parentMessageId,
        Long scheduleEventOriginId,
        String title,
        String body,
        MessageType type,
        MessagePriority priority,
        String attachment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime scheduledAt,
        LocalDateTime sentAt,
        MessageState state,
        LocalDateTime deliveredAt,
        LocalDateTime readAt,
        MessageReceiptState receiptState,
        String channelTitle
) {
}
