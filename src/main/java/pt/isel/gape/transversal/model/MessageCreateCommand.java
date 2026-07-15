package pt.isel.gape.transversal.model;

import java.time.LocalDateTime;
import java.util.List;

public record MessageCreateCommand(
        long channelId,
        Long senderUserId,
        Long parentMessageId,
        Long scheduleEventOriginId,
        String title,
        String body,
        MessageType type,
        MessagePriority priority,
        String attachment,
        LocalDateTime scheduledAt,
        List<Long> recipientIds
) {
    public MessageCreateCommand {
        recipientIds = recipientIds == null ? List.of() : List.copyOf(recipientIds);
    }
}
