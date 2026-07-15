package pt.isel.gape.learning.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AssessmentUpdateCommand(
        Long subjectId,
        Long contentBlockId,
        String physicalRoomCode,
        String title,
        String description,
        AssessmentType type,
        AssessmentMode mode,
        AssessmentCorrectionMode correctionMode,
        BigDecimal maxGrade,
        BigDecimal passingGrade,
        BigDecimal finalGradeWeight,
        Integer attemptsLimit,
        EnrollmentApprovalMode enrollmentMode,
        AssessmentState state,
        LocalDateTime availableFrom,
        LocalDateTime availableUntil,
        List<Long> classGroupIds
) {
    private static final BigDecimal DEFAULT_FINAL_GRADE_WEIGHT = new BigDecimal("100.00");

    public AssessmentUpdateCommand {
        physicalRoomCode = physicalRoomCode == null || physicalRoomCode.isBlank()
                ? null
                : physicalRoomCode.trim();
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
            LocalDateTime availableUntil,
            List<Long> classGroupIds
    ) {
        this(
                subjectId,
                contentBlockId,
                null,
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
                classGroupIds
        );
    }

    public AssessmentUpdateCommand(
            Long subjectId,
            Long contentBlockId,
            String physicalRoomCode,
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
        this(
                subjectId,
                contentBlockId,
                physicalRoomCode,
                title,
                description,
                type,
                mode,
                correctionMode,
                maxGrade,
                passingGrade,
                DEFAULT_FINAL_GRADE_WEIGHT,
                attemptsLimit,
                enrollmentMode,
                state,
                availableFrom,
                availableUntil,
                classGroupIds
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
            EnrollmentApprovalMode enrollmentMode,
            AssessmentState state,
            LocalDateTime availableFrom,
            LocalDateTime availableUntil
    ) {
        this(
                subjectId,
                contentBlockId,
                null,
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
                availableUntil
        );
    }

    public AssessmentUpdateCommand(
            Long subjectId,
            Long contentBlockId,
            String physicalRoomCode,
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
                physicalRoomCode,
                title,
                description,
                type,
                mode,
                correctionMode,
                maxGrade,
                passingGrade,
                DEFAULT_FINAL_GRADE_WEIGHT,
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
            BigDecimal finalGradeWeight,
            Integer attemptsLimit,
            EnrollmentApprovalMode enrollmentMode,
            AssessmentState state,
            LocalDateTime availableFrom,
            LocalDateTime availableUntil,
            List<Long> classGroupIds
    ) {
        this(
                subjectId,
                contentBlockId,
                null,
                title,
                description,
                type,
                mode,
                correctionMode,
                maxGrade,
                passingGrade,
                finalGradeWeight,
                attemptsLimit,
                enrollmentMode,
                state,
                availableFrom,
                availableUntil,
                classGroupIds
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
                null,
                title,
                description,
                type,
                mode,
                correctionMode,
                maxGrade,
                passingGrade,
                attemptsLimit,
                state,
                availableFrom,
                availableUntil
        );
    }

    public AssessmentUpdateCommand(
            Long subjectId,
            Long contentBlockId,
            String physicalRoomCode,
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
                physicalRoomCode,
                title,
                description,
                type,
                mode,
                correctionMode,
                maxGrade,
                passingGrade,
                DEFAULT_FINAL_GRADE_WEIGHT,
                attemptsLimit,
                EnrollmentApprovalMode.AUTO_APPROVE,
                state,
                availableFrom,
                availableUntil,
                List.of()
        );
    }
}
