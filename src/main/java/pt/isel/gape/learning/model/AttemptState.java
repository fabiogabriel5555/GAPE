package pt.isel.gape.learning.model;

import java.util.Locale;

public enum AttemptState {
    IN_PROGRESS("in_progress"),
    SUBMITTED("submitted"),
    CORRECTED("corrected"),
    EXPIRED("expired"),
    CANCELLED("cancelled");

    private final String databaseValue;

    AttemptState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public boolean hasSubmission() {
        return this == SUBMITTED || this == CORRECTED;
    }

    public static AttemptState fromDatabaseValue(String value) {
        for (AttemptState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported attempt state: " + value);
    }

    public static AttemptState parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
