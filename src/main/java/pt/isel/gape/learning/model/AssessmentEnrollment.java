package pt.isel.gape.learning.model;

import java.time.LocalDate;

public record AssessmentEnrollment(
        long studentUserId,
        long assessmentId,
        EnrollmentState state,
        LocalDate startDate,
        LocalDate endDate
) {
}
