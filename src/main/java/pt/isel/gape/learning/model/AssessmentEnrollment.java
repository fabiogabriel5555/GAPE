package pt.isel.gape.learning.model;

public record AssessmentEnrollment(
        long studentUserId,
        long assessmentId,
        EnrollmentState state
) {
}
