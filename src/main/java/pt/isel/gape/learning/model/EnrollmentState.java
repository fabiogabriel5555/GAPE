package pt.isel.gape.learning.model;

import java.util.Locale;

public enum EnrollmentState {
    PENDING("pending"),
    ACTIVE("active"),
    INACTIVE("inactive"),
    REJECTED("rejected"),
    COMPLETED("completed"),
    WITHDRAWN("withdrawn"),
    ARCHIVED("archived");

    private final String databaseValue;

    EnrollmentState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static EnrollmentState fromDatabaseValue(String value) {
        for (EnrollmentState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported enrollment state: " + value);
    }

    public static EnrollmentState parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
