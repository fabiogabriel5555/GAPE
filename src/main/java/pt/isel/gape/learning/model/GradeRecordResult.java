package pt.isel.gape.learning.model;

import java.util.Locale;

public enum GradeRecordResult {
    APPROVED("approved"),
    FAILED("failed"),
    PENDING("pending"),
    ABSENT("absent");

    private final String databaseValue;

    GradeRecordResult(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static GradeRecordResult fromDatabaseValue(String value) {
        for (GradeRecordResult result : values()) {
            if (result.databaseValue.equals(value)) {
                return result;
            }
        }
        throw new IllegalArgumentException("Unknown grade record result: " + value);
    }

    public static GradeRecordResult parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("grade record result is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (GradeRecordResult result : values()) {
            if (result.databaseValue.equals(normalized) || result.name().equalsIgnoreCase(normalized)) {
                return result;
            }
        }
        throw new IllegalArgumentException("Unknown grade record result: " + value);
    }
}
