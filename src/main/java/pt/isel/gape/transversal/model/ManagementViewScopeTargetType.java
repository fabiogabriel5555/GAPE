package pt.isel.gape.transversal.model;

import java.util.Locale;

/**
 * Identifies the entity addressed by a non-global management-view scope.
 */
public enum ManagementViewScopeTargetType {
    ORGANIZATION("ORGANIZATION"),
    COURSE("COURSE"),
    SUBJECT("SUBJECT"),
    CLASS_GROUP("CLASS_GROUP"),
    USER("USER");

    private final String databaseValue;

    ManagementViewScopeTargetType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static ManagementViewScopeTargetType fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Management view scope target type is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (ManagementViewScopeTargetType type : values()) {
            if (type.databaseValue.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported management view scope target type: " + value);
    }
}
