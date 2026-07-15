package pt.isel.gape.learning.model;

import java.math.BigDecimal;
import java.util.List;

public record GradeSheetCreateCommand(
        long subjectId,
        Long courseOccurrenceId,
        String title,
        GradeSheetType type,
        BigDecimal maxGrade,
        BigDecimal passingGrade,
        GradeSheetState state,
        List<Long> classGroupIds,
        List<GradeAssessmentWeight> assessmentWeights
) {
    public GradeSheetCreateCommand(
            long subjectId,
            String title,
            GradeSheetType type,
            BigDecimal maxGrade,
            BigDecimal passingGrade,
            GradeSheetState state,
            List<Long> classGroupIds,
            List<GradeAssessmentWeight> assessmentWeights
    ) {
        this(subjectId, null, title, type, maxGrade, passingGrade, state, classGroupIds, assessmentWeights);
    }
}
