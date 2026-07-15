package pt.isel.gape.learning.model;

import java.math.BigDecimal;
public record SubjectCreateCommand(
        long organizationId,
        Long organicUnitId,
        String name,
        String acronym,
        String photo,
        String description,
        BigDecimal ects,
        BigDecimal finalGradeMax,
        Integer workloadHours,
        SubjectState state
) {
    private static final BigDecimal DEFAULT_FINAL_GRADE_MAX = new BigDecimal("20.00");

    public SubjectCreateCommand(
            long organizationId,
            String name,
            String acronym,
            String photo,
            String description,
            BigDecimal ects,
            BigDecimal finalGradeMax,
            Integer workloadHours,
            SubjectState state
    ) {
        this(
                organizationId,
                null,
                name,
                acronym,
                photo,
                description,
                ects,
                finalGradeMax,
                workloadHours,
                state
        );
    }

    public SubjectCreateCommand(
            long organizationId,
            String name,
            String acronym,
            String photo,
            String description,
            BigDecimal ects,
            Integer workloadHours,
            SubjectState state
    ) {
        this(
                organizationId,
                null,
                name,
                acronym,
                photo,
                description,
                ects,
                DEFAULT_FINAL_GRADE_MAX,
                workloadHours,
                state
        );
    }
}
