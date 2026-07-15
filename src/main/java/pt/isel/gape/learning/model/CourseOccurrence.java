package pt.isel.gape.learning.model;

import java.time.LocalDate;

public record CourseOccurrence(
        long id,
        long courseId,
        int referenceYear,
        String code,
        LocalDate startsAt,
        LocalDate endsAt,
        CourseOccurrenceState state
) {

    /**
     * Compatibility alias for existing views that use the occurrence text as a label.
     * New code should use {@link #code()}.
     */
    public String label() {
        return code;
    }
}
