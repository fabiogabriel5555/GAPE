package pt.isel.gape.learning.model;

import java.math.BigDecimal;

public record CourseCreateCommand(
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
    private static final BigDecimal DEFAULT_CERTIFICATE_MAX_GRADE = new BigDecimal("20.00");

    public CourseCreateCommand(
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
        this(
                organizationId,
                organicUnitId,
                name,
                acronym,
                photo,
                description,
                ects,
                DEFAULT_CERTIFICATE_MAX_GRADE,
                duration,
                type,
                state
        );
    }
}
