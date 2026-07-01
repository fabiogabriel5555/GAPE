package pt.isel.gape.learning.model;

import java.math.BigDecimal;

public record GradeAssessmentWeight(
        long assessmentId,
        BigDecimal weight
) {
}
