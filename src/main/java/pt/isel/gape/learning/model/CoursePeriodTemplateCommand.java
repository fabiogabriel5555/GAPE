package pt.isel.gape.learning.model;

public record CoursePeriodTemplateCommand(
        int curricularYear,
        CurricularTerm term,
        int startsMonth,
        int startsDay,
        int endsMonth,
        int endsDay
) {
}
