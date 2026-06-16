package pt.isel.gape.learning.model;

public enum ClassGroupModality {
    ONSITE("onsite"),
    ONLINE("online"),
    HYBRID("hybrid");

    private final String databaseValue;

    ClassGroupModality(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static ClassGroupModality fromDatabaseValue(String value) {
        for (ClassGroupModality modality : values()) {
            if (modality.databaseValue.equals(value)) {
                return modality;
            }
        }
        throw new IllegalArgumentException("Unknown class group modality: " + value);
    }
}
