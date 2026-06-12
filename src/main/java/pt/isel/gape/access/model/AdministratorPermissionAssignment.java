package pt.isel.gape.access.model;

import java.util.Objects;

import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationPolicy;

public record AdministratorPermissionAssignment(
        String permissionCode,
        AccessEntityType contextType,
        long contextId
) {

    public AdministratorPermissionAssignment {
        permissionCode = AuthorizationPolicy.canonicalAdminPermission(
                Objects.requireNonNull(permissionCode, "permissionCode is required")
        );
        contextType = Objects.requireNonNull(contextType, "contextType is required");
        if (contextType == AccessEntityType.SELF) {
            throw new IllegalArgumentException("Administrator permissions cannot use SELF context");
        }
        if (AuthorizationPolicy.MANAGE_ALL.equals(permissionCode)) {
            if (contextType != AccessEntityType.GLOBAL || contextId != 0L) {
                throw new IllegalArgumentException("MANAGE_ALL must use GLOBAL context");
            }
        } else if (contextType == AccessEntityType.GLOBAL || contextId <= 0L) {
            throw new IllegalArgumentException(permissionCode + " requires a concrete context");
        }
    }

    public static AdministratorPermissionAssignment manageAll() {
        return new AdministratorPermissionAssignment(
                AuthorizationPolicy.MANAGE_ALL,
                AccessEntityType.GLOBAL,
                0L
        );
    }

    public boolean isManageAll() {
        return AuthorizationPolicy.MANAGE_ALL.equals(permissionCode);
    }
}
