package pt.isel.gape.security.authorization;

import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.model.AccessProfileType;

public record AuthorizationRule(
        Set<AccessProfileType> profileTypes,
        String permissionCode,
        AccessEntityType entityType
) {

    public AuthorizationRule {
        profileTypes = Set.copyOf(Objects.requireNonNull(profileTypes, "profileTypes are required"));
        Objects.requireNonNull(permissionCode, "permissionCode is required");
        Objects.requireNonNull(entityType, "entityType is required");
    }

    public boolean acceptsProfile(AccessProfileType profileType) {
        return profileTypes.contains(profileType);
    }
}
