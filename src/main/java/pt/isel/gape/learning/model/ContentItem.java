package pt.isel.gape.learning.model;

import java.time.LocalDateTime;

public record ContentItem(
        long id,
        long authorUserId,
        String title,
        String description,
        ContentFormat format,
        String source,
        ContentItemState state,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
