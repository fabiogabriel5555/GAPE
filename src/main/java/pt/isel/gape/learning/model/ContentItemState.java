package pt.isel.gape.learning.model;

import java.util.Locale;

public enum ContentItemState {
    DRAFT("draft"),
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String databaseValue;

    ContentItemState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static ContentItemState fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Content item state is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ContentItemState state : values()) {
            if (state.databaseValue.equals(normalized)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported content item state: " + value);
    }
}
