package pt.isel.gape.structure.model;

import java.util.Locale;

public enum OrganicUnitState {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String databaseValue;

    OrganicUnitState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static OrganicUnitState fromDatabaseValue(String value) {
        for (OrganicUnitState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported organic unit state: " + value);
    }

    public static OrganicUnitState parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
