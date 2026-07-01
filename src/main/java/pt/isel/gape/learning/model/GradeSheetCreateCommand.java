package pt.isel.gape.learning.model;

import java.math.BigDecimal;
import java.util.List;

public record GradeSheetCreateCommand(
        long subjectId,
        String title,
        GradeSheetType type,
        BigDecimal maxGrade,
        BigDecimal passingGrade,
        GradeSheetState state,
        List<Long> classGroupIds,
        List<GradeAssessmentWeight> assessmentWeights
) {
}
