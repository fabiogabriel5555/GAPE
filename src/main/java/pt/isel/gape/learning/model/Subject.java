package pt.isel.gape.learning.model;

import java.math.BigDecimal;

public record Subject(
        long id,
        long organizationId,
        String name,
        String acronym,
        String photo,
        String description,
        BigDecimal ects,
        Integer workloadHours,
        SubjectState state
) {
}
