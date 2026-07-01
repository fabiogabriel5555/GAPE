package pt.isel.gape.learning.model;

import java.util.Locale;

public enum GradeSheetType {
    FINAL("final"),
    CONTINUOUS_ASSESSMENT("continuous_assessment"),
    EXAM("exam"),
    PARTIAL("partial"),
    OTHER("other");

    private final String databaseValue;

    GradeSheetType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static GradeSheetType fromDatabaseValue(String value) {
        for (GradeSheetType type : values()) {
            if (type.databaseValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown grade sheet type: " + value);
    }

    public static GradeSheetType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("grade sheet type is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (GradeSheetType type : values()) {
            if (type.databaseValue.equals(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown grade sheet type: " + value);
    }
}
