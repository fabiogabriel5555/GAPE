package pt.isel.gape.learning.model;

import java.util.Locale;

public enum AssessmentCorrectionMode {
    AUTOMATIC("automatic"),
    MIXED("mixed"),
    MANUAL("manual");

    private final String databaseValue;

    AssessmentCorrectionMode(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public boolean allowsAutomaticCorrection() {
        return this == AUTOMATIC || this == MIXED;
    }

    public boolean allowsManualCorrection() {
        return this == MANUAL || this == MIXED;
    }

    public boolean requiresObjectiveOnly() {
        return this == AUTOMATIC;
    }

    public static AssessmentCorrectionMode fromDatabaseValue(String value) {
        for (AssessmentCorrectionMode mode : values()) {
            if (mode.databaseValue.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unsupported assessment correction mode: " + value);
    }

    public static AssessmentCorrectionMode parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
