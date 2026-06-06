package pt.isel.gape.structure.model;

public enum RoleAssignmentState {
    ACTIVE,
    INACTIVE,
    ARCHIVED;

    public String toDatabaseValue() {
        return name().toLowerCase();
    }
}
