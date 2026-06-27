package pt.isel.gape.learning.model;

import java.util.Locale;

public enum AssessmentType {
    FORM("form"),
    TEST("test"),
    EXAM("exam");

    private final String databaseValue;

    AssessmentType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static AssessmentType fromDatabaseValue(String value) {
        if (value != null && "questionnaire".equalsIgnoreCase(value.trim())) {
            return FORM;
        }
        for (AssessmentType type : values()) {
            if (type.databaseValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported assessment type: " + value);
    }

    public static AssessmentType parse(String value) {
        if (value == null || value.isBlank()) {
            return FORM;
        }
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
