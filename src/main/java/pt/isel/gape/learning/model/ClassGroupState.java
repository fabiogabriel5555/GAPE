package pt.isel.gape.learning.model;

public enum ClassGroupState {
    ACTIVE("active"),
    INACTIVE("inactive"),
    CLOSED("closed"),
    ARCHIVED("archived");

    private final String databaseValue;

    ClassGroupState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static ClassGroupState fromDatabaseValue(String value) {
        for (ClassGroupState state : values()) {
            if (state.databaseValue.equals(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown class group state: " + value);
    }
}
