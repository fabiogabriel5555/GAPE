package pt.isel.gape.learning.model;

public record AssessmentEnrollmentCommand(
        long studentUserId,
        long assessmentId
) {
}
