package pt.isel.gape.learning.model;

import java.util.Locale;

public enum CertificateType {
    COMPLETION("completion"),
    ATTENDANCE("attendance"),
    QUALIFICATION("qualification"),
    OTHER("other");

    private final String databaseValue;

    CertificateType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static CertificateType fromDatabaseValue(String value) {
        for (CertificateType type : values()) {
            if (type.databaseValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown certificate type: " + value);
    }

    public static CertificateType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("certificate type is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (CertificateType type : values()) {
            if (type.databaseValue.equals(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown certificate type: " + value);
    }
}
