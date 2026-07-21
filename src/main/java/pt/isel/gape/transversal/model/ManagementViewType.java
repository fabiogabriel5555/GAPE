package pt.isel.gape.transversal.model;

import java.util.Locale;

/**
 * The functional shape of a configurable management view.
 */
public enum ManagementViewType {
    DASHBOARD("dashboard"),
    REPORT("report"),
    CONTROL_PANEL("control_panel"),
    OTHER("other");

    private final String databaseValue;

    ManagementViewType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static ManagementViewType fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Management view type is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        // Kept only while databases seeded before phase 15 are upgraded.
        if ("analytics".equals(normalized)) {
            return OTHER;
        }
        for (ManagementViewType type : values()) {
            if (type.databaseValue.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported management view type: " + value);
    }
}
