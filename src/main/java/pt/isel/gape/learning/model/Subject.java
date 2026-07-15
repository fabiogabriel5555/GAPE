package pt.isel.gape.learning.model;

import java.math.BigDecimal;

public record Subject(
        long id,
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
}
