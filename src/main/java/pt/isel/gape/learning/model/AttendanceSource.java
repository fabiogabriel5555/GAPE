package pt.isel.gape.learning.model;

import java.util.Locale;

public enum AttendanceSource {
    MANUAL("manual"),
    AUTOMATIC("automatic"),
    OTHER("other");

    private final String databaseValue;

    AttendanceSource(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static AttendanceSource fromDatabaseValue(String value) {
        for (AttendanceSource source : values()) {
            if (source.databaseValue.equalsIgnoreCase(value)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unsupported attendance source: " + value);
    }

    public static AttendanceSource parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
