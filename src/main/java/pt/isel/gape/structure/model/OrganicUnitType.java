package pt.isel.gape.structure.model;

import java.util.Locale;

public enum OrganicUnitType {
    SCHOOL("school"),
    FACULTY("faculty"),
    DEPARTMENT("department"),
    CENTER("center"),
    OFFICE("office"),
    SERVICE("service"),
    SECTION("section"),
    DIRECTION("direction"),
    OTHER("other");

    private final String databaseValue;

    OrganicUnitType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static OrganicUnitType fromDatabaseValue(String value) {
        for (OrganicUnitType type : values()) {
            if (type.databaseValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported organic unit type: " + value);
    }

    public static OrganicUnitType parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
