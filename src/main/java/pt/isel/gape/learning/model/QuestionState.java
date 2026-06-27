package pt.isel.gape.learning.model;

import java.util.Locale;

public enum QuestionState {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String databaseValue;

    QuestionState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static QuestionState fromDatabaseValue(String value) {
        for (QuestionState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported question state: " + value);
    }

    public static QuestionState parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
