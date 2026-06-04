package pt.isel.gape.access.model;

public enum SessionState {
    ACTIVE,
    EXPIRED,
    CLOSED;

    public static SessionState fromDatabaseValue(String value) {
        return SessionState.valueOf(value.toUpperCase());
    }

    public String toDatabaseValue() {
        return name().toLowerCase();
    }
}
