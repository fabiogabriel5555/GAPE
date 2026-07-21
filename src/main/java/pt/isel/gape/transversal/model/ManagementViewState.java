package pt.isel.gape.transversal.model;

import java.util.Locale;

public enum ManagementViewState {
    ACTIVE("active"),
    INACTIVE("inactive"),
    ARCHIVED("archived");

    private final String databaseValue;

    ManagementViewState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static ManagementViewState fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Management view state is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ManagementViewState state : values()) {
            if (state.databaseValue.equals(normalized)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported management view state: " + value);
    }
}
