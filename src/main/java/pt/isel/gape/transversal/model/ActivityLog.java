package pt.isel.gape.transversal.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record ActivityLog(
        long id,
        Long userId,
        Long sessionId,
        String operationType,
        String affectedEntityType,
        String affectedEntityIdentifier,
        LocalDateTime occurredAt,
        String outcome,
        String sourceIp
) {

    public ActivityLog {
        Objects.requireNonNull(operationType, "operationType is required");
        Objects.requireNonNull(affectedEntityType, "affectedEntityType is required");
        Objects.requireNonNull(affectedEntityIdentifier, "affectedEntityIdentifier is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        Objects.requireNonNull(outcome, "outcome is required");
    }
}
