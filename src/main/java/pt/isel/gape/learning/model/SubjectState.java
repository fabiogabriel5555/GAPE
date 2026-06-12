package pt.isel.gape.learning.model;

import java.util.Locale;

public enum SubjectState {
    ACTIVE("active"),
    INACTIVE("inactive"),
    ARCHIVED("archived");

    private final String databaseValue;

    SubjectState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static SubjectState fromDatabaseValue(String value) {
        for (SubjectState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported subject state: " + value);
    }

    public static SubjectState parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
