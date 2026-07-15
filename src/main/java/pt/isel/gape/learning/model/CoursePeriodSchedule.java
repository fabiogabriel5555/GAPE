package pt.isel.gape.learning.model;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Resolves the month/day curriculum calendar into the concrete dates of one
 * course occurrence. The first configured period is the anchor of the cycle.
 */
public final class CoursePeriodSchedule {

    private CoursePeriodSchedule() {
    }

    public static List<PeriodDates> resolve(int referenceYear, List<CoursePeriodTemplate> templates) {
        if (templates == null || templates.isEmpty()) {
            throw new IllegalArgumentException("Course occurrence requires configured course periods");
        }
        CoursePeriodTemplate anchor = templates.stream()
                .min(Comparator.comparingInt(CoursePeriodTemplate::curricularYear)
                        .thenComparingInt(template -> template.term().position()))
                .orElseThrow();
        MonthDay anchorDay = anchor.startsMonthDay();
        return templates.stream()
                .map(template -> new PeriodDates(template, datesFor(referenceYear, anchorDay, template)))
                .toList();
    }

    private static DateRange datesFor(int referenceYear, MonthDay anchorDay, CoursePeriodTemplate template) {
        LocalDate startsAt = template.startsMonthDay().atYear(referenceYear);
        if (template.startsMonthDay().isBefore(anchorDay)) {
            startsAt = startsAt.plusYears(1);
        }
        LocalDate endsAt = template.endsMonthDay().atYear(startsAt.getYear());
        if (endsAt.isBefore(startsAt)) {
            endsAt = endsAt.plusYears(1);
        }
        return new DateRange(startsAt, endsAt);
    }

    public record PeriodDates(CoursePeriodTemplate template, DateRange dates) {
        public PeriodDates {
            Objects.requireNonNull(template, "template is required");
            Objects.requireNonNull(dates, "dates are required");
        }
    }

    public record DateRange(LocalDate startsAt, LocalDate endsAt) {
        public DateRange {
            Objects.requireNonNull(startsAt, "start date is required");
            Objects.requireNonNull(endsAt, "end date is required");
        }
    }
}
