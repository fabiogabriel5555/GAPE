package pt.isel.gape.security.authorization;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;

public final class PermissionChecker {

    private final PermissionDAO permissionDAO;
    private final ManageOrganizationDAO manageOrganizationDAO;
    private final CoordinateSubjectDAO coordinateSubjectDAO;
    private final TeachClassGroupDAO teachClassGroupDAO;

    public PermissionChecker(
            PermissionDAO permissionDAO,
            ManageOrganizationDAO manageOrganizationDAO,
            CoordinateSubjectDAO coordinateSubjectDAO,
            TeachClassGroupDAO teachClassGroupDAO
    ) {
        this.permissionDAO = Objects.requireNonNull(permissionDAO, "permissionDAO is required");
        this.manageOrganizationDAO = Objects.requireNonNull(manageOrganizationDAO, "manageOrganizationDAO is required");
        this.coordinateSubjectDAO = Objects.requireNonNull(coordinateSubjectDAO, "coordinateSubjectDAO is required");
        this.teachClassGroupDAO = Objects.requireNonNull(teachClassGroupDAO, "teachClassGroupDAO is required");
    }

    public PermissionChecker(ConnectionProvider connectionProvider) {
        this(
                new PermissionDAO(connectionProvider),
                new ManageOrganizationDAO(connectionProvider),
                new CoordinateSubjectDAO(connectionProvider),
                new TeachClassGroupDAO(connectionProvider)
        );
    }

    public AuthorizationDecision check(AccessContext context) {
        Objects.requireNonNull(context, "context is required");
        try {
            if (!permissionDAO.activeProfileExists(context.userId(), context.profileType())) {
                return AuthorizationDecision.deny("inactive_or_missing_profile");
            }
            if (AuthorizationPolicy.VIEW_REPORTS.equals(context.permissionCode())) {
                return AuthorizationDecision.allow();
            }
            if (context.profileType() != AccessProfileType.ADMINISTRATOR) {
                if (context.entityType() == AccessEntityType.GLOBAL || context.entityType() == AccessEntityType.SELF) {
                    return AuthorizationDecision.deny("missing_permission");
                }
                if (!hasActiveProfileGrant(context)) {
                    return AuthorizationDecision.deny("missing_permission");
                }
                return hasRequiredContext(context)
                        ? AuthorizationDecision.allow()
                        : AuthorizationDecision.deny("missing_context_assignment");
            }
            if (permissionDAO.hasActiveGlobalAdministratorGrant(context.userId(), AuthorizationPolicy.MANAGE_ALL)) {
                return AuthorizationDecision.allow();
            }
            String canonicalPermission = AuthorizationPolicy.canonicalAdminPermission(context.permissionCode());
            if (!AuthorizationPolicy.isAdminPermission(canonicalPermission)) {
                return AuthorizationDecision.deny("missing_permission");
            }
            if (context.entityType() == AccessEntityType.GLOBAL || context.entityType() == AccessEntityType.SELF) {
                return hasAdministratorGlobalAccess(context.userId(), canonicalPermission)
                        ? AuthorizationDecision.allow()
                        : AuthorizationDecision.deny("missing_permission");
            }
            Long entityId = context.entityId();
            if (entityId == null) {
                return AuthorizationDecision.deny("missing_context_assignment");
            }
            return hasAdministratorScopedPermission(context.userId(), canonicalPermission, context.entityType(), entityId)
                    ? AuthorizationDecision.allow()
                    : AuthorizationDecision.deny("missing_context_assignment");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check permission " + context.permissionCode(), exception);
        }
    }

    public AuthorizationDecision checkDescendant(AccessContext context) {
        Objects.requireNonNull(context, "context is required");
        try {
            if (!permissionDAO.activeProfileExists(context.userId(), context.profileType())) {
                return AuthorizationDecision.deny("inactive_or_missing_profile");
            }
            if (context.profileType() != AccessProfileType.ADMINISTRATOR) {
                return check(context);
            }
            if (permissionDAO.hasActiveGlobalAdministratorGrant(context.userId(), AuthorizationPolicy.MANAGE_ALL)) {
                return AuthorizationDecision.allow();
            }
            String canonicalPermission = AuthorizationPolicy.canonicalAdminPermission(context.permissionCode());
            if (!AuthorizationPolicy.isAdminPermission(canonicalPermission)) {
                return AuthorizationDecision.deny("missing_permission");
            }
            if (context.entityType() == AccessEntityType.GLOBAL || context.entityType() == AccessEntityType.SELF) {
                return AuthorizationDecision.deny("missing_context_assignment");
            }
            Long entityId = context.entityId();
            if (entityId == null) {
                return AuthorizationDecision.deny("missing_context_assignment");
            }
            return hasAdministratorDescendantPermission(
                    context.userId(),
                    canonicalPermission,
                    context.entityType(),
                    entityId
            )
                    ? AuthorizationDecision.allow()
                    : AuthorizationDecision.deny("missing_descendant_context");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check descendant permission " + context.permissionCode(), exception);
        }
    }

    public AuthorizationDecision checkExactAdministratorContext(AccessContext context) {
        Objects.requireNonNull(context, "context is required");
        try {
            if (!permissionDAO.activeProfileExists(context.userId(), context.profileType())) {
                return AuthorizationDecision.deny("inactive_or_missing_profile");
            }
            if (context.profileType() != AccessProfileType.ADMINISTRATOR) {
                return AuthorizationDecision.deny("administrator_profile_required");
            }
            if (permissionDAO.hasActiveGlobalAdministratorGrant(context.userId(), AuthorizationPolicy.MANAGE_ALL)) {
                return AuthorizationDecision.allow();
            }
            String canonicalPermission = AuthorizationPolicy.canonicalAdminPermission(context.permissionCode());
            if (!AuthorizationPolicy.isAdminPermission(canonicalPermission)) {
                return AuthorizationDecision.deny("missing_permission");
            }
            if (context.entityType() == AccessEntityType.GLOBAL || context.entityType() == AccessEntityType.SELF) {
                return AuthorizationDecision.deny("missing_context_assignment");
            }
            Long entityId = context.entityId();
            if (entityId == null) {
                return AuthorizationDecision.deny("missing_context_assignment");
            }
            for (String permissionCode : acceptablePermissionCodes(canonicalPermission)) {
                if (permissionDAO.hasExactAdministratorContextGrant(
                        context.userId(),
                        permissionCode,
                        context.entityType(),
                        entityId
                )) {
                    return AuthorizationDecision.allow();
                }
            }
            return AuthorizationDecision.deny("missing_exact_context_assignment");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check exact administrator context " + context.permissionCode(), exception);
        }
    }

    public boolean hasPermission(long userId, AccessProfileType profileType, String permissionCode) {
        return check(AccessContext.global(userId, null, profileType, permissionCode, null)).allowed();
    }

    public static boolean isGlobalManagementPermission(String permissionCode) {
        return AuthorizationPolicy.MANAGE_ALL.equals(AuthorizationPolicy.canonicalAdminPermission(permissionCode));
    }

    private boolean hasRequiredContext(AccessContext context) throws SQLException {
        if (context.entityType() == AccessEntityType.GLOBAL || context.entityType() == AccessEntityType.SELF) {
            return true;
        }
        Long entityId = context.entityId();
        if (entityId == null) {
            return false;
        }
        return switch (context.entityType()) {
            case ORGANIZATION -> context.profileType() == AccessProfileType.ADMINISTRATOR
                    && manageOrganizationDAO.hasActiveAssignment(context.userId(), entityId);
            case ORGANIC_UNIT, COURSE -> false;
            case SUBJECT -> context.profileType() == AccessProfileType.COORDINATOR
                    && coordinateSubjectDAO.hasActiveAssignment(context.userId(), entityId);
            case CLASS_GROUP -> context.profileType() == AccessProfileType.TEACHER
                    && teachClassGroupDAO.hasActiveAssignment(context.userId(), entityId);
            case GLOBAL, SELF -> true;
        };
    }

    private boolean hasActiveProfileGrant(AccessContext context) throws SQLException {
        String requestedPermission = context.permissionCode();
        if (permissionDAO.hasActiveGrant(context.userId(), context.profileType(), requestedPermission)) {
            return true;
        }
        String canonicalPermission = AuthorizationPolicy.canonicalAdminPermission(requestedPermission);
        return !canonicalPermission.equals(requestedPermission)
                && permissionDAO.hasActiveGrant(context.userId(), context.profileType(), canonicalPermission);
    }

    private boolean hasAdministratorScopedPermission(
            long adminUserId,
            String requestedPermissionCode,
            AccessEntityType entityType,
            long entityId
    ) throws SQLException {
        for (String permissionCode : acceptablePermissionCodes(requestedPermissionCode)) {
            if (permissionDAO.hasActiveAdministratorContextGrant(adminUserId, permissionCode, entityType, entityId)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAdministratorGlobalAccess(long adminUserId, String requestedPermissionCode)
            throws SQLException {
        if (AuthorizationPolicy.MANAGE_ALL.equals(requestedPermissionCode)) {
            return permissionDAO.hasActiveGlobalAdministratorGrant(adminUserId, requestedPermissionCode);
        }
        return permissionDAO.hasAnyActiveAdministratorGrant(adminUserId, acceptablePermissionCodes(requestedPermissionCode));
    }

    private boolean hasAdministratorDescendantPermission(
            long adminUserId,
            String requestedPermissionCode,
            AccessEntityType entityType,
            long entityId
    ) throws SQLException {
        for (String permissionCode : acceptablePermissionCodes(requestedPermissionCode)) {
            if (permissionDAO.hasActiveAdministratorDescendantGrant(adminUserId, permissionCode, entityType, entityId)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> acceptablePermissionCodes(String requestedPermissionCode) {
        return switch (requestedPermissionCode) {
            case AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE ->
                    List.of(AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE);
            case AuthorizationPolicy.MANAGE_ALL -> List.of(AuthorizationPolicy.MANAGE_ALL);
            default -> List.of(requestedPermissionCode);
        };
    }
}
