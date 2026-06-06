package pt.isel.gape.access.model;

public enum PermissionState {
    ACTIVE,
    INACTIVE;

    public static PermissionState fromDatabaseValue(String value) {
        return PermissionState.valueOf(value.toUpperCase());
    }

    public String toDatabaseValue() {
        return name().toLowerCase();
    }
}
