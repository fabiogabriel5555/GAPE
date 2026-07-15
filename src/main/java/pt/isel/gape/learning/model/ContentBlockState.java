package pt.isel.gape.learning.model;

public enum ContentBlockState {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String databaseValue;

    ContentBlockState(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static ContentBlockState fromDatabaseValue(String value) {
        for (ContentBlockState state : values()) {
            if (state.databaseValue.equals(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown content block state: " + value);
    }
}
