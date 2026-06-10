package pt.isel.gape.structure.model;

import java.util.Locale;

public enum OrganizationType {
    EDUCATIONAL_INSTITUTION("educational_institution"),
    TRAINING_COMPANY("training_company"),
    COMPANY("company"),
    OTHER("other");

    private final String databaseValue;

    OrganizationType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static OrganizationType fromDatabaseValue(String value) {
        for (OrganizationType type : values()) {
            if (type.databaseValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported organization type: " + value);
    }

    public static OrganizationType parse(String value) {
        return fromDatabaseValue(value.toLowerCase(Locale.ROOT));
    }
}
