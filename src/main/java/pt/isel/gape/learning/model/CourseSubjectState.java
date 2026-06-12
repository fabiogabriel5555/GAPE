package pt.isel.gape.learning.model;

import java.util.Locale;

public enum CourseSubjectState {
    ACTIVE("active"),
    INACTIVE("inactive"),
    ARCHIVED("archived");

    private final String databaseValue;

    CourseSubjectState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static CourseSubjectState fromDatabaseValue(String value) {
        for (CourseSubjectState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported course-subject state: " + value);
    }

    public static CourseSubjectState parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
