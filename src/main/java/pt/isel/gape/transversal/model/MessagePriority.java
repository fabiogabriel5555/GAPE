package pt.isel.gape.transversal.model;

public enum MessagePriority {
    LOW("low"),
    NORMAL("normal"),
    HIGH("high"),
    URGENT("urgent");

    private final String databaseValue;

    MessagePriority(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static MessagePriority fromDatabaseValue(String value) {
        for (MessagePriority priority : values()) {
            if (priority.databaseValue.equalsIgnoreCase(value)) {
                return priority;
            }
        }
        throw new IllegalArgumentException("Unsupported message priority: " + value);
    }
}
