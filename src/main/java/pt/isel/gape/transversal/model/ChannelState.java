package pt.isel.gape.transversal.model;

public enum ChannelState {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String databaseValue;

    ChannelState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static ChannelState fromDatabaseValue(String value) {
        for (ChannelState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported channel state: " + value);
    }
}
