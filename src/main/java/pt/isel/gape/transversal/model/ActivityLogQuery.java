package pt.isel.gape.transversal.model;

import java.time.LocalDateTime;

/**
 * Optional, exact-match filters applied to an activity-log consultation.
 *
 * <p>The query deliberately carries no visibility decision.  Visibility is
 * always evaluated by {@code ActivityLogService} against the requesting
 * actor's live profile and contextual assignments.</p>
 */
public record ActivityLogQuery(
        Long userId,
        String operationType,
        String affectedEntityType,
        String outcome,
        LocalDateTime occurredFrom,
        LocalDateTime occurredUntil
) {

    public ActivityLogQuery {
        if (userId != null && userId <= 0) {
            throw new IllegalArgumentException("userId must be positive when provided");
        }
        operationType = normalize(operationType);
        affectedEntityType = normalize(affectedEntityType);
        outcome = normalize(outcome);
        if (occurredFrom != null && occurredUntil != null && occurredUntil.isBefore(occurredFrom)) {
            throw new IllegalArgumentException("occurredUntil cannot be before occurredFrom");
        }
    }

    public static ActivityLogQuery all() {
        return new ActivityLogQuery(null, null, null, null, null, null);
    }

    public static ActivityLogQuery forUser(long userId) {
        return new ActivityLogQuery(userId, null, null, null, null, null);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
