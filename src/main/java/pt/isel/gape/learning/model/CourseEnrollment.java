package pt.isel.gape.learning.model;

import java.time.LocalDate;

public record CourseEnrollment(
        long studentUserId,
        long courseId,
        long courseOccurrenceId,
        EnrollmentState state,
        LocalDate startDate,
        LocalDate endDate
) {
    public CourseEnrollment(
            long studentUserId,
            long courseId,
            EnrollmentState state,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this(studentUserId, courseId, 0L, state, startDate, endDate);
    }
}
