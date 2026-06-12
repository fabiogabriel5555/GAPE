package pt.isel.gape.learning.model;

import java.math.BigDecimal;

public record CourseUpdateCommand(
        long organizationId,
        Long organicUnitId,
        String name,
        String acronym,
        String photo,
        String description,
        BigDecimal ects,
        String duration,
        CourseType type,
        CourseState state
) {
}
