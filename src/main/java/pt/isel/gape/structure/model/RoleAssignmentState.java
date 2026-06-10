package pt.isel.gape.structure.model;

public enum RoleAssignmentState {
    ACTIVE,
    INACTIVE,
    ARCHIVED;

    public static RoleAssignmentState fromDatabaseValue(String value) {
        return RoleAssignmentState.valueOf(value.toUpperCase());
    }

    public String toDatabaseValue() {
        return name().toLowerCase();
    }
}
