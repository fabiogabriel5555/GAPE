package pt.isel.gape.access.service;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.transversal.service.AuditService;

public final class GrantService {

    public static final String MANAGE_PERMISSIONS = "MANAGE_PERMISSIONS";
    public static final Set<String> GLOBAL_MANAGEMENT_PERMISSIONS = Set.of(
            "MANAGE_USERS",
            MANAGE_PERMISSIONS,
            "MANAGE_SETTINGS"
    );

    private final PermissionDAO permissionDAO;
    private final AuditService auditService;

    public GrantService(PermissionDAO permissionDAO, AuditService auditService) {
        this.permissionDAO = Objects.requireNonNull(permissionDAO, "permissionDAO is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
    }

    public GrantService(ConnectionProvider connectionProvider, Clock clock) {
        this(new PermissionDAO(connectionProvider), new AuditService(connectionProvider, clock));
    }

    public void grantPermission(
            long actorUserId,
            Long sessionId,
            AccessProfileType targetProfileType,
            long targetUserId,
            String permissionCode,
            String sourceIp
    ) {
        try {
            requireAdministratorWithPermission(actorUserId);
            validateGrantTarget(targetProfileType, targetUserId, permissionCode);
            permissionDAO.grantPermission(targetUserId, targetProfileType, permissionCode);
            auditGrant(actorUserId, sessionId, targetProfileType, targetUserId, permissionCode, "success", sourceIp);
        } catch (RuntimeException | SQLException exception) {
            auditGrant(actorUserId, sessionId, targetProfileType, targetUserId, permissionCode, "failure", sourceIp);
            if (exception instanceof SQLException sqlException) {
                throw new IllegalStateException("Failed to grant permission " + permissionCode, sqlException);
            }
            throw (RuntimeException) exception;
        }
    }

    public void revokePermission(
            long actorUserId,
            Long sessionId,
            AccessProfileType targetProfileType,
            long targetUserId,
            String permissionCode,
            String sourceIp
    ) {
        try {
            requireAdministratorWithPermission(actorUserId);
            permissionDAO.revokePermission(targetUserId, targetProfileType, permissionCode);
            auditRevoke(actorUserId, sessionId, targetProfileType, targetUserId, permissionCode, "success", sourceIp);
        } catch (RuntimeException | SQLException exception) {
            auditRevoke(actorUserId, sessionId, targetProfileType, targetUserId, permissionCode, "failure", sourceIp);
            if (exception instanceof SQLException sqlException) {
                throw new IllegalStateException("Failed to revoke permission " + permissionCode, sqlException);
            }
            throw (RuntimeException) exception;
        }
    }

    private void requireAdministratorWithPermission(long actorUserId) throws SQLException {
        if (!permissionDAO.activeProfileExists(actorUserId, AccessProfileType.ADMINISTRATOR)
                || !permissionDAO.hasActiveGrant(actorUserId, AccessProfileType.ADMINISTRATOR, MANAGE_PERMISSIONS)) {
            throw new SecurityException("Only administrators with MANAGE_PERMISSIONS can manage grants");
        }
    }

    private void validateGrantTarget(AccessProfileType targetProfileType, long targetUserId, String permissionCode)
            throws SQLException {
        if (!permissionDAO.isActivePermission(permissionCode)) {
            throw new IllegalArgumentException("Inactive permission cannot be assigned: " + permissionCode);
        }
        if (!permissionDAO.activeProfileExists(targetUserId, targetProfileType)) {
            throw new IllegalArgumentException("Permission can only be assigned to an active user profile");
        }
        if (PermissionChecker.isGlobalManagementPermission(permissionCode)
                && targetProfileType != AccessProfileType.ADMINISTRATOR) {
            throw new SecurityException("Global management permissions can only be assigned to administrators");
        }
    }

    private void auditGrant(
            long actorUserId,
            Long sessionId,
            AccessProfileType targetProfileType,
            long targetUserId,
            String permissionCode,
            String outcome,
            String sourceIp
    ) {
        auditService.record(
                actorUserId,
                sessionId,
                "PERMISSION_GRANT",
                targetProfileType.name(),
                targetUserId + ":" + permissionCode,
                outcome,
                sourceIp
        );
    }

    private void auditRevoke(
            long actorUserId,
            Long sessionId,
            AccessProfileType targetProfileType,
            long targetUserId,
            String permissionCode,
            String outcome,
            String sourceIp
    ) {
        auditService.record(
                actorUserId,
                sessionId,
                "PERMISSION_REVOKE",
                targetProfileType.name(),
                targetUserId + ":" + permissionCode,
                outcome,
                sourceIp
        );
    }
}
