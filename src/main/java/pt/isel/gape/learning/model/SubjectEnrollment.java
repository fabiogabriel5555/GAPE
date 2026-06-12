package pt.isel.gape.learning.model;

import java.time.LocalDate;

public record SubjectEnrollment(
        long studentUserId,
        long courseId,
        long subjectId,
        EnrollmentState state,
        LocalDate startDate,
        LocalDate endDate
) {
}
