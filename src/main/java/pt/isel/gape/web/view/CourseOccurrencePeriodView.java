package pt.isel.gape.web.view;

import java.time.LocalDate;

import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.learning.model.CourseOccurrencePeriod;
import pt.isel.gape.learning.model.CourseOccurrenceState;
import pt.isel.gape.learning.model.CurricularTerm;

public final class CourseOccurrencePeriodView {

    private final long id;
    private final long occurrenceId;
    private final int curricularYear;
    private final CurricularTerm term;
    private final LocalDate startsAt;
    private final LocalDate endsAt;
    private final CourseOccurrenceState state;

    private CourseOccurrencePeriodView(CourseOccurrencePeriod period) {
        this.id = period.id();
        this.occurrenceId = period.courseOccurrenceId();
        this.curricularYear = period.curricularYear();
        this.term = period.term();
        this.startsAt = period.startsAt();
        this.endsAt = period.endsAt();
        this.state = period.state();
    }

    public static CourseOccurrencePeriodView from(CourseOccurrencePeriod period) {
        return new CourseOccurrencePeriodView(period);
    }

    public long getId() {
        return id;
    }

    public long getOccurrenceId() {
        return occurrenceId;
    }

    public int getCurricularYear() {
        return curricularYear;
    }

    public String getCurricularYearLabel() {
        return curricularYear + ordinalSuffix(curricularYear) + " Course Year";
    }

    public String getTermValue() {
        return term.toDatabaseValue();
    }

    public String getTermLabel() {
        return term.labelForCourseYear(curricularYear);
    }

    public String getLabel() {
        return getTermLabel();
    }

    public String getStartsAt() {
        return startsAt == null ? "" : ApplicationDateTimeFormat.date(startsAt);
    }

    /**
     * Machine-readable date used by client-side lifecycle calculations.
     * Display dates intentionally use the application DD-MM-YYYY format.
     */
    public String getStartsAtValue() {
        return startsAt == null ? "" : startsAt.toString();
    }

    public String getEndsAt() {
        return endsAt == null ? "" : ApplicationDateTimeFormat.date(endsAt);
    }

    /**
     * Machine-readable date used by client-side lifecycle calculations.
     * Display dates intentionally use the application DD-MM-YYYY format.
     */
    public String getEndsAtValue() {
        return endsAt == null ? "" : endsAt.toString();
    }

    public String getDateRangeLabel() {
        return (startsAt == null ? "-" : ApplicationDateTimeFormat.date(startsAt))
                + " to "
                + (endsAt == null ? "-" : ApplicationDateTimeFormat.date(endsAt));
    }

    public String getStateValue() {
        return state.toDatabaseValue();
    }

    public String getStateLabel() {
        return CourseOccurrenceView.stateLabel(state);
    }

    public String getStateBadgeClass() {
        return CourseOccurrenceView.stateBadgeClass(state);
    }

    private static String ordinalSuffix(int value) {
        int rem100 = value % 100;
        if (rem100 >= 11 && rem100 <= 13) {
            return "th";
        }
        return switch (value % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}
