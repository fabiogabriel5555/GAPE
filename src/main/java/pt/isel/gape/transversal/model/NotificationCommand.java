package pt.isel.gape.transversal.model;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationCommand(
        long channelId,
        String title,
        String body,
        MessageType type,
        MessagePriority priority,
        LocalDateTime scheduledAt,
        List<Long> recipientIds
) {
    public NotificationCommand {
        recipientIds = recipientIds == null ? List.of() : List.copyOf(recipientIds);
    }
}
