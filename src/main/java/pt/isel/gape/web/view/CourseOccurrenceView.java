package pt.isel.gape.web.view;

import java.time.LocalDate;
import java.util.List;

import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.learning.model.CourseOccurrence;
import pt.isel.gape.learning.model.CourseOccurrencePeriod;
import pt.isel.gape.learning.model.CourseOccurrenceState;

public final class CourseOccurrenceView {

    private final long id;
    private final long courseId;
    private final int referenceYear;
    private final String code;
    private final LocalDate startsAt;
    private final LocalDate endsAt;
    private final CourseOccurrenceState state;
    private final List<CourseOccurrencePeriodView> periods;

    private CourseOccurrenceView(CourseOccurrence occurrence, List<CourseOccurrencePeriod> periods) {
        this.id = occurrence.id();
        this.courseId = occurrence.courseId();
        this.referenceYear = occurrence.referenceYear();
        this.code = occurrence.code();
        this.startsAt = occurrence.startsAt();
        this.endsAt = occurrence.endsAt();
        this.state = occurrence.state();
        this.periods = periods == null
                ? List.of()
                : periods.stream().map(CourseOccurrencePeriodView::from).toList();
    }

    public static CourseOccurrenceView from(CourseOccurrence occurrence, List<CourseOccurrencePeriod> periods) {
        return new CourseOccurrenceView(occurrence, periods);
    }

    public long getId() {
        return id;
    }

    public long getCourseId() {
        return courseId;
    }

    public int getReferenceYear() {
        return referenceYear;
    }

    public String getCode() {
        return getAcademicYearLabel();
    }

    public String getAcademicYearLabel() {
        if (startsAt == null || endsAt == null) {
            return code == null || code.isBlank() ? "Occurrence " + id : code;
        }
        StringBuilder label = new StringBuilder();
        for (int year = startsAt.getYear(); year <= endsAt.getYear(); year++) {
            if (!label.isEmpty()) {
                label.append('-');
            }
            label.append(year);
        }
        return label.toString();
    }

    public String getLabel() {
        return getAcademicYearLabel();
    }

    public String getStartsAt() {
        return startsAt == null ? "" : ApplicationDateTimeFormat.date(startsAt);
    }

    public String getStartsAtValue() {
        return startsAt == null ? "" : startsAt.toString();
    }

    public String getEndsAt() {
        return endsAt == null ? "" : ApplicationDateTimeFormat.date(endsAt);
    }

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
        return stateLabel(state);
    }

    public String getStateBadgeClass() {
        return stateBadgeClass(state);
    }

    public int getPeriodCount() {
        return periods.size();
    }

    public List<CourseOccurrencePeriodView> getPeriods() {
        return periods;
    }

    static String stateLabel(CourseOccurrenceState state) {
        return switch (state) {
            case DRAFT -> "Draft";
            case SCHEDULED -> "Scheduled";
            case ACTIVE -> "Active";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };
    }

    static String stateBadgeClass(CourseOccurrenceState state) {
        return switch (state) {
            case DRAFT -> "bg-warning-50 text-warning-600";
            case SCHEDULED -> "bg-warning-50 text-warning-700";
            case ACTIVE -> "bg-success-50 text-success-600";
            case COMPLETED -> "bg-neutral-20 text-neutral-600";
            case CANCELLED -> "bg-danger-50 text-danger-600";
        };
    }
}
