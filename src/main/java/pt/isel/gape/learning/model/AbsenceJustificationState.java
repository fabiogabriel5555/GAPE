package pt.isel.gape.learning.model;

import java.util.Locale;

public enum AbsenceJustificationState {
    SUBMITTED("submitted"),
    UNDER_REVIEW("under_review"),
    APPROVED("approved"),
    REJECTED("rejected"),
    CANCELLED("cancelled");

    private final String databaseValue;

    AbsenceJustificationState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public boolean isFinalDecision() {
        return this == APPROVED || this == REJECTED;
    }

    public static AbsenceJustificationState fromDatabaseValue(String value) {
        for (AbsenceJustificationState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported absence justification state: " + value);
    }

    public static AbsenceJustificationState parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
