package pt.isel.gape.access.model;

public enum UserState {
    ACTIVE,
    INACTIVE,
    BLOCKED;

    public static UserState fromDatabaseValue(String value) {
        return UserState.valueOf(value.toUpperCase());
    }

    public String toDatabaseValue() {
        return name().toLowerCase();
    }
}
