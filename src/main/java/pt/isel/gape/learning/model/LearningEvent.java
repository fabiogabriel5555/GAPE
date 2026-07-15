package pt.isel.gape.learning.model;

import java.time.LocalDateTime;

public record LearningEvent(
        long id,
        String sourceType,
        String sourceKey,
        String eventType,
        String category,
        String categoryLabel,
        String title,
        String description,
        String contextLabel,
        String contextTitle,
        Long classGroupId,
        Long courseId,
        Long subjectId,
        Long studentUserId,
        String detailHref,
        LocalDateTime occurredAt,
        String stateLabel,
        String stateValue,
        String iconClass,
        String badgeClass,
        String stateBadgeClass,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
