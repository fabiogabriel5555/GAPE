package pt.isel.gape.learning.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Response(
        long id,
        long attemptId,
        long questionId,
        String code,
        String answer,
        String attachment,
        BigDecimal score,
        LocalDateTime answeredAt
) {
}
