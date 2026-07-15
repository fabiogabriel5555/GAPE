package pt.isel.gape.learning.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GradeSheet(
        long id,
        long subjectId,
        long courseOccurrenceId,
        String title,
        GradeSheetType type,
        BigDecimal maxGrade,
        BigDecimal passingGrade,
        String weightAlert,
        String publicationExplanation,
        LocalDateTime releasedAt,
        GradeSheetState state,
        List<Long> classGroupIds,
        List<GradeAssessmentWeight> assessmentWeights
) {
    public GradeSheet(
            long id,
            long subjectId,
            long courseOccurrenceId,
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
        this(
                id,
                subjectId,
                courseOccurrenceId,
                title,
                type,
                maxGrade,
                passingGrade,
                weightAlert,
                null,
                releasedAt,
                state,
                classGroupIds,
                assessmentWeights
        );
    }

    public GradeSheet(
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
        this(
                id,
                subjectId,
                0,
                title,
                type,
                maxGrade,
                passingGrade,
                weightAlert,
                null,
                releasedAt,
                state,
                classGroupIds,
                assessmentWeights
        );
    }
}
