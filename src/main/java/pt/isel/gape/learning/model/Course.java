package pt.isel.gape.learning.model;

import java.math.BigDecimal;

public record Course(
        long id,
        long organizationId,
        Long organicUnitId,
        String name,
        String acronym,
        String photo,
        String description,
        BigDecimal ects,
        BigDecimal certificateMaxGrade,
        String duration,
        CourseType type,
        CourseState state
) {
}
