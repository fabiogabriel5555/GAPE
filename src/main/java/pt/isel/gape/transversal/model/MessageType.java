package pt.isel.gape.transversal.model;

public enum MessageType {
    TEXT("text"),
    COMMENT("comment"),
    ANNOUNCEMENT("announcement"),
    WARNING("warning"),
    ALERT("alert"),
    REMINDER("reminder"),
    NOTIFICATION("notification"),
    SYSTEM("system"),
    ATTACHMENT("attachment"),
    OTHER("other");

    private final String databaseValue;

    MessageType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public boolean isSystemGeneratedType() {
        return this == SYSTEM || this == NOTIFICATION || this == REMINDER
                || this == ALERT || this == WARNING;
    }

    public static MessageType fromDatabaseValue(String value) {
        for (MessageType type : values()) {
            if (type.databaseValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported message type: " + value);
    }
}
