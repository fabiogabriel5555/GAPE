package pt.isel.gape.learning.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GradeSheet(
        long id,
        long subjectId,
        String title,
        GradeSheetType type,
        BigDecimal maxGrade,
        BigDecimal passingGrade,
        String weightAlert,
        LocalDateTime releasedAt,
        GradeSheetState state,
        List<Long> classGroupIds,
        List<GradeAssessmentWeight> assessmentWeights
) {
}
