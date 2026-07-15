package pt.isel.gape.web.view;

import java.util.Locale;

import pt.isel.gape.learning.model.CoursePeriodTemplate;
import pt.isel.gape.learning.model.CoursePeriodTemplateCommand;
import pt.isel.gape.learning.model.CurricularTerm;

public final class CoursePeriodTemplateView {

    private final long id;
    private final long courseId;
    private final int curricularYear;
    private final String term;
    private final CurricularTerm curricularTerm;
    private final int startsMonth;
    private final int startsDay;
    private final int endsMonth;
    private final int endsDay;

    private CoursePeriodTemplateView(CoursePeriodTemplate template) {
        this.id = template.id();
        this.courseId = template.courseId();
        this.curricularYear = template.curricularYear();
        this.term = template.term().name();
        this.curricularTerm = template.term();
        this.startsMonth = template.startsMonth();
        this.startsDay = template.startsDay();
        this.endsMonth = template.endsMonth();
        this.endsDay = template.endsDay();
    }

    private CoursePeriodTemplateView(CoursePeriodTemplateCommand command) {
        this.id = 0;
        this.courseId = 0;
        this.curricularYear = command.curricularYear();
        this.term = command.term().name();
        this.curricularTerm = command.term();
        this.startsMonth = command.startsMonth();
        this.startsDay = command.startsDay();
        this.endsMonth = command.endsMonth();
        this.endsDay = command.endsDay();
    }

    public static CoursePeriodTemplateView from(CoursePeriodTemplate template) {
        return new CoursePeriodTemplateView(template);
    }

    public static CoursePeriodTemplateView from(CoursePeriodTemplateCommand command) {
        return new CoursePeriodTemplateView(command);
    }

    public long getId() {
        return id;
    }

    public long getCourseId() {
        return courseId;
    }

    public int getCurricularYear() {
        return curricularYear;
    }

    public String getCurricularYearLabel() {
        return curricularYear + ordinalSuffix(curricularYear) + " Course Year";
    }

    public String getTerm() {
        return term;
    }

    public String getTermLabel() {
        return curricularTerm.labelForCourseYear(curricularYear);
    }

    public String getPeriodLabel() {
        return getTermLabel();
    }

    public int getStartsMonth() {
        return startsMonth;
    }

    public int getStartsDay() {
        return startsDay;
    }

    public int getEndsMonth() {
        return endsMonth;
    }

    public int getEndsDay() {
        return endsDay;
    }

    public String getStartsAt() {
        return monthDay(startsMonth, startsDay);
    }

    public String getEndsAt() {
        return monthDay(endsMonth, endsDay);
    }

    public String getDateRangeLabel() {
        return getStartsAt() + " to " + getEndsAt();
    }

    private static String monthDay(int month, int day) {
        return String.format(Locale.ROOT, "%02d-%02d", day, month);
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
