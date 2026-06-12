package pt.isel.gape.learning.model;

import java.time.LocalDate;

public record SubjectEnrollmentCommand(
        long studentUserId,
        long subjectId,
        Long courseId,
        LocalDate startDate,
        LocalDate endDate
) {
}
