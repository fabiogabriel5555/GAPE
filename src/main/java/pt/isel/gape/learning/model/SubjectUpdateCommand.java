package pt.isel.gape.learning.model;

import java.math.BigDecimal;

public record SubjectUpdateCommand(
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

    public SubjectUpdateCommand(
            String name,
            String acronym,
            String photo,
            String description,
            BigDecimal ects,
            Integer workloadHours,
            SubjectState state
    ) {
        this(name, acronym, photo, description, ects, DEFAULT_FINAL_GRADE_MAX, workloadHours, state);
    }
}
