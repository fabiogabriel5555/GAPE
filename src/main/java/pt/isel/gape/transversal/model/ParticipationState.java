package pt.isel.gape.transversal.model;

public enum ParticipationState {
    ACTIVE("active"),
    INACTIVE("inactive"),
    BLOCKED("blocked");

    private final String databaseValue;

    ParticipationState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static ParticipationState fromDatabaseValue(String value) {
        for (ParticipationState state : values()) {
            if (state.databaseValue.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported channel participation state: " + value);
    }
}
