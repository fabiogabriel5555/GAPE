package pt.isel.gape.security.authorization;

import java.util.Objects;
import java.util.Optional;

import pt.isel.gape.access.model.AccessProfileType;

public record AccessContext(
        long userId,
        Long sessionId,
        AccessProfileType profileType,
        String permissionCode,
        AccessEntityType entityType,
        Long entityId,
        String sourceIp
) {

    public AccessContext {
        Objects.requireNonNull(profileType, "profileType is required");
        Objects.requireNonNull(permissionCode, "permissionCode is required");
        Objects.requireNonNull(entityType, "entityType is required");
    }

    public static AccessContext global(
            long userId,
            Long sessionId,
            AccessProfileType profileType,
            String permissionCode,
            String sourceIp
    ) {
        return new AccessContext(userId, sessionId, profileType, permissionCode, AccessEntityType.GLOBAL, null, sourceIp);
    }

    public Optional<Long> sessionIdValue() {
        return Optional.ofNullable(sessionId);
    }

    public Optional<Long> entityIdValue() {
        return Optional.ofNullable(entityId);
    }
}
