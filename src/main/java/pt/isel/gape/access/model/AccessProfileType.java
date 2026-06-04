package pt.isel.gape.access.model;

public enum AccessProfileType {
    ADMINISTRATOR,
    COORDINATOR,
    TEACHER,
    STUDENT;

    public static AccessProfileType fromDatabaseValue(String value) {
        return AccessProfileType.valueOf(value.toUpperCase());
    }
}
