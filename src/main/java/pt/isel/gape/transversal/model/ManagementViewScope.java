package pt.isel.gape.transversal.model;

import java.util.Locale;

/**
 * Visibility and contextual boundary of a management view.
 */
public enum ManagementViewScope {
    GLOBAL("global", null),
    ORGANIZATION("organization", ManagementViewScopeTargetType.ORGANIZATION),
    COURSE("course", ManagementViewScopeTargetType.COURSE),
    SUBJECT("subject", ManagementViewScopeTargetType.SUBJECT),
    CLASS_GROUP("class_group", ManagementViewScopeTargetType.CLASS_GROUP),
    PERSONAL("personal", ManagementViewScopeTargetType.USER);

    private final String databaseValue;
    private final ManagementViewScopeTargetType targetType;

    ManagementViewScope(String databaseValue, ManagementViewScopeTargetType targetType) {
        this.databaseValue = databaseValue;
        this.targetType = targetType;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public ManagementViewScopeTargetType targetType() {
        return targetType;
    }

    public boolean requiresTarget() {
        return targetType != null;
    }

    public static ManagementViewScope fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Management view visibility scope is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        // USER was the old seed representation of a personal view.
        if ("user".equals(normalized)) {
            return PERSONAL;
        }
        for (ManagementViewScope scope : values()) {
            if (scope.databaseValue.equals(normalized)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unsupported management view visibility scope: " + value);
    }
}
