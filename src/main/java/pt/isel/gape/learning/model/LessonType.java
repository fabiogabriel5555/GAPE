package pt.isel.gape.learning.model;

import java.util.Locale;

public enum LessonType {
    ONLINE("online"),
    ONSITE("onsite"),
    HYBRID("hybrid");

    private final String databaseValue;

    LessonType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static LessonType fromDatabaseValue(String value) {
        for (LessonType type : values()) {
            if (type.databaseValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported lesson type: " + value);
    }

    public static LessonType parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
