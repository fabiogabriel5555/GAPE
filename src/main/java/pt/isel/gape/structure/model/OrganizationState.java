package pt.isel.gape.structure.model;

import java.util.Locale;

public enum OrganizationState {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String databaseValue;

    OrganizationState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static OrganizationState fromDatabaseValue(String value) {
        for (OrganizationState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported organization state: " + value);
    }

    public static OrganizationState parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
