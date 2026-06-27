package pt.isel.gape.learning.model;

import java.util.Locale;

public enum ScheduleEventState {
    DRAFT("draft"),
    ACTIVE("active"),
    INACTIVE("inactive"),
    CANCELLED("cancelled"),
    COMPLETED("completed");

    private final String databaseValue;

    ScheduleEventState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static ScheduleEventState fromDatabaseValue(String value) {
        for (ScheduleEventState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported schedule event state: " + value);
    }

    public static ScheduleEventState parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
