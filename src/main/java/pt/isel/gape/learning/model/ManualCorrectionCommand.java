package pt.isel.gape.learning.model;

import java.math.BigDecimal;

public record ManualCorrectionCommand(
        long responseId,
        BigDecimal score
) {
}
