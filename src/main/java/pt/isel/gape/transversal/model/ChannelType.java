package pt.isel.gape.transversal.model;

public enum ChannelType {
    MESSAGE("message"),
    FORUM("forum"),
    COMMENTS("comments"),
    ANNOUNCEMENT("announcement"),
    SYSTEM("system"),
    ORGANIZATION("organization"),
    CLASS_GROUP("class_group"),
    CONTENT_BLOCK("content_block"),
    ASSESSMENT("assessment"),
    OTHER("other");

    private final String databaseValue;

    ChannelType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static ChannelType fromDatabaseValue(String value) {
        for (ChannelType type : values()) {
            if (type.databaseValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported channel type: " + value);
    }
}
