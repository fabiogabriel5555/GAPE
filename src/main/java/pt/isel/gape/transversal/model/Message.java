package pt.isel.gape.transversal.model;

import java.time.LocalDateTime;

public record Message(
        long id,
        long channelId,
        Long senderUserId,
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
        MessageState state
) {
}
