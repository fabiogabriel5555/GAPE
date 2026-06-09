package pt.isel.gape.access.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record DeletionRequest(
        long id,
        long submitterUserId,
        Long processorAdminUserId,
        LocalDateTime submittedAt,
        LocalDateTime processedAt,
        String reason,
        DeletionRequestState state
) {

    public DeletionRequest {
        Objects.requireNonNull(submittedAt, "submittedAt is required");
        Objects.requireNonNull(state, "state is required");
    }
}
