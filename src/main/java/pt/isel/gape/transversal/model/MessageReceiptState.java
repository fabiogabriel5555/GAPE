package pt.isel.gape.transversal.model;

public enum MessageReceiptState {
    PENDING("pending"),
    DELIVERED("delivered"),
    READ("read");

    private final String databaseValue;

    MessageReceiptState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static MessageReceiptState fromDatabaseValue(String value) {
        for (MessageReceiptState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported message receipt state: " + value);
    }
}
