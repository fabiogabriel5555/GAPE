package pt.isel.gape.learning.model;

import java.util.Locale;

public enum CertificateState {
    DRAFT("draft"),
    ACTIVE("active"),
    ISSUED("issued");

    private final String databaseValue;

    CertificateState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public boolean isPubliclyValid() {
        return this == ISSUED;
    }

    public static CertificateState fromDatabaseValue(String value) {
        for (CertificateState state : values()) {
            if (state.databaseValue.equals(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown certificate state: " + value);
    }

    public static CertificateState parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("certificate state is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (CertificateState state : values()) {
            if (state.databaseValue.equals(normalized) || state.name().equalsIgnoreCase(normalized)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown certificate state: " + value);
    }
}
