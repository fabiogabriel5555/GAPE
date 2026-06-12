package pt.isel.gape.learning.model;

import java.util.Locale;

public enum CourseType {
    DEGREE("degree"),
    MASTER("master"),
    SHORT_COURSE("short_course"),
    PROFESSIONAL_TRAINING("professional_training"),
    OTHER("other");

    private final String databaseValue;

    CourseType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static CourseType fromDatabaseValue(String value) {
        for (CourseType type : values()) {
            if (type.databaseValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported course type: " + value);
    }

    public static CourseType parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
