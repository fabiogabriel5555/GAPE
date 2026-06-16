package pt.isel.gape.learning.model;

import java.time.LocalDate;

public record ClassGroupEnrollment(
        long studentUserId,
        long classGroupId,
        EnrollmentState state,
        LocalDate startDate,
        LocalDate endDate
) {
}
