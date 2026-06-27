package pt.isel.gape.learning.model;

import java.math.BigDecimal;

public record CorrectionResult(
        long attemptId,
        BigDecimal score,
        AttemptState state,
        int automaticallyCorrectedResponses,
        int pendingManualResponses
) {
}
