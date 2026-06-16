package pt.isel.gape.learning.model;

public enum ContentBlockAccessMode {
    OPEN("open"),
    RESTRICTED("restricted"),
    SCHEDULED("scheduled");

    private final String databaseValue;

    ContentBlockAccessMode(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static ContentBlockAccessMode fromDatabaseValue(String value) {
        for (ContentBlockAccessMode mode : values()) {
            if (mode.databaseValue.equals(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown content block access mode: " + value);
    }
}
