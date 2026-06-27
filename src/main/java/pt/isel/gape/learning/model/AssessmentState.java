package pt.isel.gape.learning.model;

import java.util.Locale;

public enum AssessmentState {
    DRAFT("draft"),
    SCHEDULED("scheduled"),
    ACTIVE("active"),
    COMPLETED("completed");

    private final String databaseValue;

    AssessmentState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static AssessmentState fromDatabaseValue(String value) {
        for (AssessmentState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported assessment state: " + value);
    }

    public static AssessmentState parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
