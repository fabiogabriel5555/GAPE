package pt.isel.gape.learning.model;

import java.util.Locale;

public enum GradeRecordState {
    DRAFT("draft"),
    PUBLISHED("published"),
    CORRECTED("corrected"),
    INACTIVE("inactive");

    private final String databaseValue;

    GradeRecordState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public boolean isActive() {
        return this == DRAFT || this == PUBLISHED || this == CORRECTED;
    }

    public static GradeRecordState fromDatabaseValue(String value) {
        for (GradeRecordState state : values()) {
            if (state.databaseValue.equals(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown grade record state: " + value);
    }

    public static GradeRecordState parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("grade record state is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (GradeRecordState state : values()) {
            if (state.databaseValue.equals(normalized) || state.name().equalsIgnoreCase(normalized)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown grade record state: " + value);
    }
}
