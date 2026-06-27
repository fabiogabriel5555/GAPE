package pt.isel.gape.learning.model;

import java.util.Locale;

public enum AssessmentMode {
    ONLINE("online"),
    ONSITE("onsite");

    private final String databaseValue;

    AssessmentMode(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static AssessmentMode fromDatabaseValue(String value) {
        for (AssessmentMode mode : values()) {
            if (mode.databaseValue.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unsupported assessment mode: " + value);
    }

    public static AssessmentMode parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
