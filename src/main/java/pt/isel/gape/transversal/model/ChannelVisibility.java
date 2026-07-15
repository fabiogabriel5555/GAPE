package pt.isel.gape.transversal.model;

public enum ChannelVisibility {
    PARTICIPANTS("participants"),
    PUBLIC("public"),
    PRIVATE("private"),
    ORGANIZATION("organization"),
    CONTEXT("context"),
    SYSTEM("system");

    private final String databaseValue;

    ChannelVisibility(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static ChannelVisibility fromDatabaseValue(String value) {
        for (ChannelVisibility visibility : values()) {
            if (visibility.databaseValue.equalsIgnoreCase(value)) {
                return visibility;
            }
        }
        throw new IllegalArgumentException("Unsupported channel visibility: " + value);
    }
}
