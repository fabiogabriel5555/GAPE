package pt.isel.gape.transversal.service;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.model.ActivityLog;

public final class ActivityLogService {

    private final ActivityLogDAO activityLogDAO;
    private final PermissionChecker permissionChecker;
    private final Clock clock;

    public ActivityLogService(ActivityLogDAO activityLogDAO, PermissionChecker permissionChecker, Clock clock) {
        this.activityLogDAO = Objects.requireNonNull(activityLogDAO, "activityLogDAO is required");
        this.permissionChecker = permissionChecker;
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public ActivityLogService(ActivityLogDAO activityLogDAO) {
        this(activityLogDAO, null, ApplicationClock.system());
    }

    public ActivityLogService(ConnectionProvider connectionProvider) {
        this(new ActivityLogDAO(connectionProvider), new PermissionChecker(connectionProvider), ApplicationClock.system());
    }

    public long record(
            Long userId,
            Long sessionId,
            String operationType,
            String affectedEntityType,
            String affectedEntityIdentifier,
            String outcome,
            String sourceIp
    ) {
        validateLog(operationType, affectedEntityType, affectedEntityIdentifier, outcome);
        try {
            return activityLogDAO.insert(
                    userId,
                    sessionId,
                    operationType.trim(),
                    affectedEntityType.trim(),
                    affectedEntityIdentifier.trim(),
                    LocalDateTime.now(clock),
                    outcome.trim(),
                    sourceIp
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to record activity log " + operationType, exception);
        }
    }

    public Optional<ActivityLog> findById(long activityLogId) {
        try {
            return activityLogDAO.findById(activityLogId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load activity log " + activityLogId, exception);
        }
    }

    public List<ActivityLog> listForActor(long actorUserId, AccessProfileType actorProfileType) {
        try {
            if (permissionChecker != null
                    && actorProfileType == AccessProfileType.ADMINISTRATOR
                    && permissionChecker.hasPermission(actorUserId, actorProfileType, AuthorizationPolicy.MANAGE_ALL)) {
                return activityLogDAO.findAll();
            }
            return activityLogDAO.findByUserId(actorUserId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list activity logs", exception);
        }
    }

    public List<ActivityLog> findByUserId(long userId) {
        try {
            return activityLogDAO.findByUserId(userId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load activity logs for user " + userId, exception);
        }
    }

    public List<ActivityLog> listForUserAudit(long actorUserId, AccessProfileType actorProfileType, long targetUserId) {
        try {
            if (actorUserId == targetUserId) {
                return activityLogDAO.findByUserId(targetUserId);
            }
            if (permissionChecker != null
                    && actorProfileType == AccessProfileType.ADMINISTRATOR
                    && permissionChecker.hasPermission(actorUserId, actorProfileType, AuthorizationPolicy.MANAGE_ALL)) {
                return activityLogDAO.findByUserInvolvement(targetUserId);
            }
            throw new SecurityException("MANAGE_ALL permission is required");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load audit records for user " + targetUserId, exception);
        }
    }

    private static void validateLog(
            String operationType,
            String affectedEntityType,
            String affectedEntityIdentifier,
            String outcome
    ) {
        require(operationType, "operationType");
        require(affectedEntityType, "affectedEntityType");
        require(affectedEntityIdentifier, "affectedEntityIdentifier");
        require(outcome, "outcome");
    }

    private static void require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
