package pt.isel.gape.learning.model;

import java.util.Locale;

public enum ContentAssociationType {
    ORGANIZATION("organization"),
    ORGANIC_UNIT("organic_unit"),
    COURSE("course"),
    SUBJECT("subject"),
    CLASS_GROUP("class_group"),
    CONTENT_BLOCK("content_block"),
    ASSESSMENT("assessment");

    private final String databaseValue;

    ContentAssociationType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static ContentAssociationType fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Content association type is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (ContentAssociationType type : values()) {
            if (type.databaseValue.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported content association type: " + value);
    }
}
