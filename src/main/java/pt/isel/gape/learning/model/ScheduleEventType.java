package pt.isel.gape.learning.model;

import java.util.Locale;

public enum ScheduleEventType {
    LESSON("lesson"),
    ASSESSMENT("assessment"),
    REMINDER("reminder"),
    MEETING("meeting"),
    OTHER("other");

    private final String databaseValue;

    ScheduleEventType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static ScheduleEventType fromDatabaseValue(String value) {
        for (ScheduleEventType type : values()) {
            if (type.databaseValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported schedule event type: " + value);
    }

    public static ScheduleEventType parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
