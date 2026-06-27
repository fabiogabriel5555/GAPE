package pt.isel.gape.learning.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AssessmentUpdateCommand(
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
        List<Long> classGroupIds
) {
    public AssessmentUpdateCommand {
        classGroupIds = classGroupIds == null ? List.of() : List.copyOf(classGroupIds);
    }

    public AssessmentUpdateCommand(
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
            LocalDateTime availableUntil
    ) {
        this(
                subjectId,
                contentBlockId,
                title,
                description,
                type,
                mode,
                correctionMode,
                maxGrade,
                passingGrade,
                attemptsLimit,
                enrollmentMode,
                state,
                availableFrom,
                availableUntil,
                List.of()
        );
    }

    public AssessmentUpdateCommand(
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
            AssessmentState state,
            LocalDateTime availableFrom,
            LocalDateTime availableUntil
    ) {
        this(
                subjectId,
                contentBlockId,
                title,
                description,
                type,
                mode,
                correctionMode,
                maxGrade,
                passingGrade,
                attemptsLimit,
                EnrollmentApprovalMode.AUTO_APPROVE,
                state,
                availableFrom,
                availableUntil,
                List.of()
        );
    }
}
