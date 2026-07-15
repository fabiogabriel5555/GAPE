package pt.isel.gape.learning.model;

/**
 * A course enrollment always belongs to one specific occurrence. Its dates
 * are derived from that occurrence and are therefore never user supplied.
 */
public record CourseEnrollmentCommand(
        long studentUserId,
        long courseId,
        long courseOccurrenceId
) {
}
