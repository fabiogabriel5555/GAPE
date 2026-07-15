package pt.isel.gape.transversal.model;

public enum ParticipationRole {
    OWNER("owner"),
    MODERATOR("moderator"),
    MEMBER("member"),
    VIEWER("viewer"),
    ADMINISTRATOR("administrator"),
    COORDINATOR("coordinator"),
    TEACHER("teacher"),
    STUDENT("student");

    private final String databaseValue;

    ParticipationRole(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public boolean canModerate() {
        return this == OWNER || this == MODERATOR || this == ADMINISTRATOR
                || this == COORDINATOR || this == TEACHER;
    }

    public static ParticipationRole fromDatabaseValue(String value) {
        for (ParticipationRole role : values()) {
            if (role.databaseValue.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unsupported channel participation role: " + value);
    }
}
