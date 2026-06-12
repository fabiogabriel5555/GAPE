package pt.isel.gape.learning.model;

import java.time.LocalDate;

public record CourseEnrollment(
        long studentUserId,
        long courseId,
        EnrollmentState state,
        LocalDate startDate,
        LocalDate endDate
) {
}
