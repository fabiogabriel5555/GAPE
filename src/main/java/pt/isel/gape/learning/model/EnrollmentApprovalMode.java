package pt.isel.gape.learning.model;

import java.util.Locale;

public enum EnrollmentApprovalMode {
    MANUAL("manual"),
    AUTO_APPROVE("auto_approve");

    private final String databaseValue;

    EnrollmentApprovalMode(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static EnrollmentApprovalMode fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return MANUAL;
        }
        for (EnrollmentApprovalMode mode : values()) {
            if (mode.databaseValue.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unsupported enrollment approval mode: " + value);
    }

    public static EnrollmentApprovalMode parse(String value) {
        return fromDatabaseValue(value == null ? null : value.toLowerCase(Locale.ROOT));
    }
}
