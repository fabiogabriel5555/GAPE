package pt.isel.gape.learning.model;

import java.util.Locale;

public enum GradeSheetState {
    DRAFT("draft"),
    PUBLISHED("published"),
    CLOSED("closed"),
    INACTIVE("inactive");

    private final String databaseValue;

    GradeSheetState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public boolean blocksDirectChanges() {
        return this == PUBLISHED || this == CLOSED || this == INACTIVE;
    }

    public static GradeSheetState fromDatabaseValue(String value) {
        for (GradeSheetState state : values()) {
            if (state.databaseValue.equals(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown grade sheet state: " + value);
    }

    public static GradeSheetState parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("grade sheet state is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (GradeSheetState state : values()) {
            if (state.databaseValue.equals(normalized) || state.name().equalsIgnoreCase(normalized)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown grade sheet state: " + value);
    }
}
