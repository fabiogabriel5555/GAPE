package pt.isel.gape.learning.model;

import java.time.LocalDate;

public record AssessmentEnrollmentCommand(
        long studentUserId,
        long assessmentId,
        LocalDate startDate,
        LocalDate endDate
) {
}
