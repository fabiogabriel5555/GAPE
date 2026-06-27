package pt.isel.gape.learning.model;

import java.util.Locale;

public enum ContentFormat {
    TEXT("text"),
    IMAGE("image"),
    VIDEO("video"),
    AUDIO("audio"),
    PDF("pdf"),
    ARCHIVE("archive"),
    URL("url"),
    SCORM("scorm"),
    XAPI("xapi"),
    PRESENTATION("presentation"),
    EMBED("embed"),
    OTHER("other");

    private final String databaseValue;

    ContentFormat(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public boolean requiresSource() {
        return this != OTHER;
    }

    public boolean isFileBacked() {
        return switch (this) {
            case TEXT, IMAGE, VIDEO, AUDIO, PDF, ARCHIVE, SCORM, XAPI, PRESENTATION -> true;
            case URL, EMBED, OTHER -> false;
        };
    }

    public static ContentFormat fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Content format is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ContentFormat format : values()) {
            if (format.databaseValue.equals(normalized)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unsupported content format: " + value);
    }
}
