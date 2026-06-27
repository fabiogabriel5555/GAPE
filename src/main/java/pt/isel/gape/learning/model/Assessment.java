package pt.isel.gape.learning.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Assessment(
        long id,
        Long subjectId,
        Long contentBlockId,
        String title,
        String description,
        AssessmentType type,
        AssessmentMode mode,
        AssessmentCorrectionMode correctionMode,
        BigDecimal maxGrade,
        BigDecimal passingGrade,
        Integer attemptsLimit,
        EnrollmentApprovalMode enrollmentMode,
        AssessmentState state,
        LocalDateTime availableFrom,
        LocalDateTime availableUntil,
        Integer orderNo
) {
}
