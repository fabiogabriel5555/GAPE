package pt.isel.gape.learning.model;

import java.util.Locale;

public enum CourseSubjectAssociationState {
    ACTIVE,
    HISTORICAL;

    public String toDatabaseValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static CourseSubjectAssociationState fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "active" -> ACTIVE;
            case "historical" -> HISTORICAL;
            default -> throw new IllegalArgumentException("Unsupported course-subject association state: " + value);
        };
    }
}
