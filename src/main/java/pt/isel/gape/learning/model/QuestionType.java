package pt.isel.gape.learning.model;

import java.util.Locale;

public enum QuestionType {
    SINGLE_CHOICE("single_choice"),
    MULTIPLE_CHOICE("multiple_choice"),
    SHORT_TEXT("short_text"),
    PARAGRAPH("paragraph"),
    FILE_UPLOAD("file_upload"),
    RATING("rating");

    private final String databaseValue;

    QuestionType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public boolean allowsOptions() {
        return this == SINGLE_CHOICE || this == MULTIPLE_CHOICE;
    }

    public boolean allowsSingleSelectedOption() {
        return this == SINGLE_CHOICE;
    }

    public boolean isCreatable() {
        return true;
    }

    public boolean isAutomaticallyScoredObjective() {
        return allowsOptions() || this == RATING;
    }

    public boolean requiresManualScoring() {
        return !isAutomaticallyScoredObjective();
    }

    public static QuestionType fromDatabaseValue(String value) {
        for (QuestionType type : values()) {
            if (type.databaseValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported question type: " + value);
    }

    public static QuestionType parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
