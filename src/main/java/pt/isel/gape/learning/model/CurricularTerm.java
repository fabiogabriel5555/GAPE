package pt.isel.gape.learning.model;

import java.util.Locale;

public enum CurricularTerm {
    ANNUAL("annual", CourseFrequency.ANNUAL, 1, "Annual"),
    SEMESTER_1("semester_1", CourseFrequency.SEMESTER, 1, "1st Semester"),
    SEMESTER_2("semester_2", CourseFrequency.SEMESTER, 2, "2nd Semester"),
    QUADRIMESTER_1("quadrimester_1", CourseFrequency.QUADRIMESTER, 1, "1st Quadrimester"),
    QUADRIMESTER_2("quadrimester_2", CourseFrequency.QUADRIMESTER, 2, "2nd Quadrimester"),
    QUADRIMESTER_3("quadrimester_3", CourseFrequency.QUADRIMESTER, 3, "3rd Quadrimester"),
    TRIMESTER_1("trimester_1", CourseFrequency.TRIMESTER, 1, "1st Trimester"),
    TRIMESTER_2("trimester_2", CourseFrequency.TRIMESTER, 2, "2nd Trimester"),
    TRIMESTER_3("trimester_3", CourseFrequency.TRIMESTER, 3, "3rd Trimester"),
    TRIMESTER_4("trimester_4", CourseFrequency.TRIMESTER, 4, "4th Trimester"),
    BIMESTER_1("bimester_1", CourseFrequency.BIMONTHLY, 1, "1st Bimester"),
    BIMESTER_2("bimester_2", CourseFrequency.BIMONTHLY, 2, "2nd Bimester"),
    BIMESTER_3("bimester_3", CourseFrequency.BIMONTHLY, 3, "3rd Bimester"),
    BIMESTER_4("bimester_4", CourseFrequency.BIMONTHLY, 4, "4th Bimester"),
    BIMESTER_5("bimester_5", CourseFrequency.BIMONTHLY, 5, "5th Bimester"),
    BIMESTER_6("bimester_6", CourseFrequency.BIMONTHLY, 6, "6th Bimester"),
    MONTH_1("month_1", CourseFrequency.MONTHLY, 1, "1st Month"),
    MONTH_2("month_2", CourseFrequency.MONTHLY, 2, "2nd Month"),
    MONTH_3("month_3", CourseFrequency.MONTHLY, 3, "3rd Month"),
    MONTH_4("month_4", CourseFrequency.MONTHLY, 4, "4th Month"),
    MONTH_5("month_5", CourseFrequency.MONTHLY, 5, "5th Month"),
    MONTH_6("month_6", CourseFrequency.MONTHLY, 6, "6th Month"),
    MONTH_7("month_7", CourseFrequency.MONTHLY, 7, "7th Month"),
    MONTH_8("month_8", CourseFrequency.MONTHLY, 8, "8th Month"),
    MONTH_9("month_9", CourseFrequency.MONTHLY, 9, "9th Month"),
    MONTH_10("month_10", CourseFrequency.MONTHLY, 10, "10th Month"),
    MONTH_11("month_11", CourseFrequency.MONTHLY, 11, "11th Month"),
    MONTH_12("month_12", CourseFrequency.MONTHLY, 12, "12th Month");

    private final String databaseValue;
    private final CourseFrequency frequency;
    private final int position;
    private final String label;

    CurricularTerm(String databaseValue, CourseFrequency frequency, int position, String label) {
        this.databaseValue = databaseValue;
        this.frequency = frequency;
        this.position = position;
        this.label = label;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public CourseFrequency frequency() {
        return frequency;
    }

    public int position() {
        return position;
    }

    public String label() {
        return label;
    }

    /**
     * Returns the human-facing position of this period in the whole course,
     * rather than restarting the ordinal for each course year.
     */
    public String labelForCourseYear(int courseYear) {
        if (courseYear <= 0) {
            throw new IllegalArgumentException("Course year must be greater than zero");
        }
        int coursePeriodPosition = ((courseYear - 1) * frequency.periodsPerYear()) + position;
        if (this == ANNUAL) {
            return coursePeriodPosition + ordinalSuffix(coursePeriodPosition) + " Course Year";
        }
        return coursePeriodPosition + ordinalSuffix(coursePeriodPosition) + " " + periodName();
    }

    public boolean belongsTo(CourseFrequency frequency) {
        return this.frequency == frequency;
    }

    public static CurricularTerm fromDatabaseValue(String value) {
        for (CurricularTerm term : values()) {
            if (term.databaseValue.equalsIgnoreCase(value)) {
                return term;
            }
        }
        throw new IllegalArgumentException("Unsupported curricular term: " + value);
    }

    public static CurricularTerm parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }

    private String periodName() {
        return label.replaceFirst("^\\d+(?:st|nd|rd|th)\\s+", "");
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
