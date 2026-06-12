package pt.isel.gape.learning.model;

import java.util.Locale;

public enum CourseState {
    ACTIVE("active"),
    INACTIVE("inactive"),
    ARCHIVED("archived");

    private final String databaseValue;

    CourseState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static CourseState fromDatabaseValue(String value) {
        for (CourseState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported course state: " + value);
    }

    public static CourseState parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
