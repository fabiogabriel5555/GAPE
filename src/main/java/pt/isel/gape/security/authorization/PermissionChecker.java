package pt.isel.gape.security.authorization;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;

public final class PermissionChecker {

    private static final Set<String> GLOBAL_MANAGEMENT_PERMISSIONS = Set.of(
            AuthorizationPolicy.MANAGE_USERS,
            AuthorizationPolicy.MANAGE_PERMISSIONS,
            AuthorizationPolicy.MANAGE_SETTINGS,
            AuthorizationPolicy.VIEW_PERSONAL_DATA,
            AuthorizationPolicy.MANAGE_PERSONAL_DATA,
            AuthorizationPolicy.PROCESS_DELETION_REQUESTS
    );

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
            if (isGlobalManagementPermission(context.permissionCode())
                    && context.profileType() != AccessProfileType.ADMINISTRATOR) {
                return AuthorizationDecision.deny("global_management_requires_administrator");
            }
            if (!permissionDAO.hasActiveGrant(context.userId(), context.profileType(), context.permissionCode())) {
                return AuthorizationDecision.deny("missing_permission");
            }
            if (!hasRequiredContext(context)) {
                return AuthorizationDecision.deny("missing_context_assignment");
            }
            return AuthorizationDecision.allow();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check permission " + context.permissionCode(), exception);
        }
    }

    public boolean hasPermission(long userId, AccessProfileType profileType, String permissionCode) {
        return check(AccessContext.global(userId, null, profileType, permissionCode, null)).allowed();
    }

    public static boolean isGlobalManagementPermission(String permissionCode) {
        return GLOBAL_MANAGEMENT_PERMISSIONS.contains(permissionCode);
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
            case SUBJECT -> context.profileType() == AccessProfileType.COORDINATOR
                    && coordinateSubjectDAO.hasActiveAssignment(context.userId(), entityId);
            case CLASS_GROUP -> context.profileType() == AccessProfileType.TEACHER
                    && teachClassGroupDAO.hasActiveAssignment(context.userId(), entityId);
            case GLOBAL, SELF -> true;
        };
    }
}
