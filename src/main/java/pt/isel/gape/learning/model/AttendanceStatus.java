package pt.isel.gape.learning.model;

import java.util.Locale;

public enum AttendanceStatus {
    PRESENT("present"),
    ABSENT("absent"),
    JUSTIFIED("justified"),
    LATE("late"),
    PARTIAL("partial");

    private final String databaseValue;

    AttendanceStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public boolean allowsJustification() {
        return this == ABSENT || this == LATE || this == PARTIAL;
    }

    public static AttendanceStatus fromDatabaseValue(String value) {
        for (AttendanceStatus status : values()) {
            if (status.databaseValue.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unsupported attendance status: " + value);
    }

    public static AttendanceStatus parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
