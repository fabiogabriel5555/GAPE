package pt.isel.gape.learning.model;

import java.time.LocalDateTime;

public record ContentBlock(
        long id,
        long classGroupId,
        String code,
        String name,
        String description,
        int orderNo,
        ContentBlockState state,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public boolean isActive() {
        return state == ContentBlockState.ACTIVE;
    }

    public boolean isInactive() {
        return state == ContentBlockState.INACTIVE;
    }
}
