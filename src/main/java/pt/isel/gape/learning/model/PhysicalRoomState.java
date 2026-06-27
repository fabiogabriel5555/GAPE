package pt.isel.gape.learning.model;

import java.util.Locale;

public enum PhysicalRoomState {
    ACTIVE("active"),
    INACTIVE("inactive"),
    UNAVAILABLE("unavailable");

    private final String databaseValue;

    PhysicalRoomState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static PhysicalRoomState fromDatabaseValue(String value) {
        for (PhysicalRoomState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported physical room state: " + value);
    }

    public static PhysicalRoomState parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
