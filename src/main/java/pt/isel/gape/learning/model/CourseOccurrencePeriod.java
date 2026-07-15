package pt.isel.gape.learning.model;

import java.time.LocalDate;

public record CourseOccurrencePeriod(
        long id,
        long courseOccurrenceId,
        int curricularYear,
        CurricularTerm term,
        LocalDate startsAt,
        LocalDate endsAt,
        CourseOccurrenceState state
) {
}
