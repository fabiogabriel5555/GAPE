package pt.isel.gape.learning.model;

import java.math.BigDecimal;

public record Question(
        long id,
        long assessmentId,
        String code,
        String statement,
        QuestionType type,
        int orderNo,
        boolean required,
        BigDecimal score,
        String expectedAnswer,
        QuestionState state
) {
}
