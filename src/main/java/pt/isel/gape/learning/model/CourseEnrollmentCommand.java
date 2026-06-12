package pt.isel.gape.learning.model;

import java.time.LocalDate;

public record CourseEnrollmentCommand(
        long studentUserId,
        long courseId,
        LocalDate startDate,
        LocalDate endDate
) {
}
