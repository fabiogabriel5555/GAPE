package pt.isel.gape.transversal.model;

public enum MessageState {
    DRAFT("draft"),
    SCHEDULED("scheduled"),
    SENT("sent"),
    ACTIVE("active"),
    EDITED("edited"),
    DELETED("deleted"),
    CANCELLED("cancelled");

    private final String databaseValue;

    MessageState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static MessageState fromDatabaseValue(String value) {
        for (MessageState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported message state: " + value);
    }
}
