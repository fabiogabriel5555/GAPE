package pt.isel.gape.learning.model;

import java.math.BigDecimal;
import java.util.List;

public record GradeSheetUpdateCommand(
        long subjectId,
        String title,
        GradeSheetType type,
        BigDecimal maxGrade,
        BigDecimal passingGrade,
        List<Long> classGroupIds,
        List<GradeAssessmentWeight> assessmentWeights
) {
}
