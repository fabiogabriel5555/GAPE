package pt.isel.gape.learning.model;

import java.time.MonthDay;

public record CoursePeriodTemplate(
        long id,
        long courseId,
        int curricularYear,
        CurricularTerm term,
        int startsMonth,
        int startsDay,
        int endsMonth,
        int endsDay
) {
    public MonthDay startsMonthDay() {
        return MonthDay.of(startsMonth, startsDay);
    }

    public MonthDay endsMonthDay() {
        return MonthDay.of(endsMonth, endsDay);
    }

}
