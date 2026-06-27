package pt.isel.gape.learning.model;

import java.util.Locale;

public enum AttendanceState {
    ACTIVE("active"),
    CORRECTED("corrected"),
    CANCELLED("cancelled");

    private final String databaseValue;

    AttendanceState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static AttendanceState fromDatabaseValue(String value) {
        for (AttendanceState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported attendance state: " + value);
    }

    public static AttendanceState parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
