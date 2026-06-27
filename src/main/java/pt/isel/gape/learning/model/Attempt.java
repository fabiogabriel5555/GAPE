package pt.isel.gape.learning.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Attempt(
        long id,
        long studentUserId,
        long assessmentId,
        int attemptNumber,
        BigDecimal score,
        AttemptState state,
        LocalDateTime startedAt,
        LocalDateTime submittedAt
) {
}
