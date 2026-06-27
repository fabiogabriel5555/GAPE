package pt.isel.gape.learning.model;

import java.util.Locale;

public enum QuestionOptionState {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String databaseValue;

    QuestionOptionState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static QuestionOptionState fromDatabaseValue(String value) {
        for (QuestionOptionState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported question option state: " + value);
    }

    public static QuestionOptionState parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
