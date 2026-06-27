package pt.isel.gape.learning.model;

import java.util.Locale;

public enum LessonState {
    DRAFT("draft"),
    SCHEDULED("scheduled"),
    ACTIVE("active"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    private final String databaseValue;

    LessonState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public boolean reservesPhysicalRoom() {
        return this == SCHEDULED || this == ACTIVE;
    }

    public static LessonState fromDatabaseValue(String value) {
        for (LessonState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported lesson state: " + value);
    }

    public static LessonState parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
