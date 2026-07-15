package pt.isel.gape.learning.model;

public record CourseOccurrenceCreateCommand(
        long courseId,
        int referenceYear
) {
}
