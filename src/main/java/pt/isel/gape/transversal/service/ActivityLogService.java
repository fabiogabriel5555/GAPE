package pt.isel.gape.transversal.service;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.model.ActivityLog;
import pt.isel.gape.transversal.model.ActivityLogQuery;
import pt.isel.gape.transversal.model.ActivityLogScope;

/**
 * Application boundary for immutable audit consultation.
 *
 * <p>A DAO may filter rows, but it deliberately has no requesting actor.  All
 * scope decisions therefore happen here, after the filters and before a row is
 * returned to a servlet.  This makes an arbitrary user-id or date filter a
 * narrowing operation, never an authorization bypass.</p>
 */
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

    /**
     * Consults only records visible to the supplied, active profile.
     *
     * <ul>
     *   <li>administrators: their managed organizations, plus their own history;</li>
     *   <li>coordinators: their assigned subjects, plus their own history;</li>
     *   <li>teachers: their assigned class groups, plus their own history;</li>
     *   <li>students: their own history only.</li>
     * </ul>
     *
     * <p>{@link ActivityLogQuery#userId()} denotes the actor stored in a log;
     * it is not a way to query an affected user's history.</p>
     */
    public List<ActivityLog> queryForActor(
            long actorUserId,
            AccessProfileType actorProfileType,
            ActivityLogQuery query
    ) {
        return visibleLogs(actorUserId, actorProfileType, query).stream()
                .map(ScopedActivityLog::activityLog)
                .toList();
    }

    public Optional<ActivityLog> findVisibleById(
            long actorUserId,
            AccessProfileType actorProfileType,
            long activityLogId
    ) {
        if (activityLogId <= 0) {
            throw new IllegalArgumentException("activityLogId must be positive");
        }
        return visibleLogs(actorUserId, actorProfileType, ActivityLogQuery.all()).stream()
                .map(ScopedActivityLog::activityLog)
                .filter(log -> log.id() == activityLogId)
                .findFirst();
    }

    public List<ActivityLog> listForActor(long actorUserId, AccessProfileType actorProfileType) {
        return queryForActor(actorUserId, actorProfileType, ActivityLogQuery.all());
    }

    /**
     * Returns only an already-authorized subset involving {@code targetUserId}.
     * A student's target is necessarily themself.  Other profiles may consult
     * a target only where the enclosing audit record is in their live context.
     */
    public List<ActivityLog> listForUserAudit(long actorUserId, AccessProfileType actorProfileType, long targetUserId) {
        if (targetUserId <= 0) {
            throw new IllegalArgumentException("targetUserId must be positive");
        }
        if (actorProfileType == AccessProfileType.STUDENT && actorUserId != targetUserId) {
            throw new SecurityException("Students can consult only their own audit history");
        }
        return visibleLogs(actorUserId, actorProfileType, ActivityLogQuery.all()).stream()
                .filter(entry -> involvesUser(entry, targetUserId))
                .map(ScopedActivityLog::activityLog)
                .toList();
    }

    private List<ScopedActivityLog> visibleLogs(
            long actorUserId,
            AccessProfileType actorProfileType,
            ActivityLogQuery query
    ) {
        validateActor(actorUserId, actorProfileType);
        requireConsultationPermission(actorUserId, actorProfileType);
        try {
            List<ActivityLog> logs = activityLogDAO.find(query == null ? ActivityLogQuery.all() : query);
            Map<Long, ActivityLogScope> persistedScopes = activityLogDAO.findScopesByActivityLogIds(
                    logs.stream().map(ActivityLog::id).toList()
            );
            Set<Long> contextualIds = contextualIdsFor(actorUserId, actorProfileType);

            return logs.stream()
                    .map(log -> new ScopedActivityLog(log, scopeFor(log, persistedScopes)))
                    .filter(entry -> isVisible(entry.scope(), actorUserId, actorProfileType, contextualIds))
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to consult activity logs", exception);
        }
    }

    private ActivityLogScope scopeFor(ActivityLog log, Map<Long, ActivityLogScope> persistedScopes) {
        ActivityLogScope persisted = persistedScopes.get(log.id());
        if (persisted != null && hasContextualScope(persisted)) {
            return persisted;
        }
        try {
            ActivityLogScope resolved = activityLogDAO.resolveLiveScope(log);
            return persisted == null ? resolved : mergeScopes(persisted, resolved);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to resolve legacy activity-log scope " + log.id(), exception);
        }
    }

    private static boolean hasContextualScope(ActivityLogScope scope) {
        return !scope.organizationIds().isEmpty()
                || !scope.subjectIds().isEmpty()
                || !scope.classGroupIds().isEmpty();
    }

    private static ActivityLogScope mergeScopes(ActivityLogScope first, ActivityLogScope second) {
        java.util.Set<Long> organizationIds = new java.util.LinkedHashSet<>(first.organizationIds());
        organizationIds.addAll(second.organizationIds());
        java.util.Set<Long> subjectIds = new java.util.LinkedHashSet<>(first.subjectIds());
        subjectIds.addAll(second.subjectIds());
        java.util.Set<Long> classGroupIds = new java.util.LinkedHashSet<>(first.classGroupIds());
        classGroupIds.addAll(second.classGroupIds());
        java.util.Set<Long> userIds = new java.util.LinkedHashSet<>(first.userIds());
        userIds.addAll(second.userIds());
        return new ActivityLogScope(organizationIds, subjectIds, classGroupIds, userIds);
    }

    private Set<Long> contextualIdsFor(long actorUserId, AccessProfileType actorProfileType) throws SQLException {
        return switch (actorProfileType) {
            case ADMINISTRATOR -> activityLogDAO.findActiveManagedOrganizationIds(actorUserId);
            case COORDINATOR -> activityLogDAO.findActiveCoordinatedSubjectIds(actorUserId);
            case TEACHER -> activityLogDAO.findActiveTaughtClassGroupIds(actorUserId);
            case STUDENT -> Set.of();
        };
    }

    private static boolean isVisible(
            ActivityLogScope scope,
            long actorUserId,
            AccessProfileType actorProfileType,
            Set<Long> contextualIds
    ) {
        if (scope.userIds().contains(actorUserId)) {
            return true;
        }
        return switch (actorProfileType) {
            case ADMINISTRATOR -> intersects(scope.organizationIds(), contextualIds);
            case COORDINATOR -> intersects(scope.subjectIds(), contextualIds);
            case TEACHER -> intersects(scope.classGroupIds(), contextualIds);
            case STUDENT -> false;
        };
    }

    private static boolean intersects(Collection<Long> left, Collection<Long> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return false;
        }
        for (Long value : left) {
            if (right.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean involvesUser(ScopedActivityLog entry, long targetUserId) {
        return Objects.equals(entry.activityLog().userId(), targetUserId)
                || entry.scope().userIds().contains(targetUserId);
    }

    private void requireConsultationPermission(long actorUserId, AccessProfileType actorProfileType) {
        if (permissionChecker != null
                && !permissionChecker.hasPermission(actorUserId, actorProfileType, AuthorizationPolicy.VIEW_REPORTS)) {
            throw new SecurityException("An active profile with report access is required");
        }
    }

    private static void validateActor(long actorUserId, AccessProfileType actorProfileType) {
        if (actorUserId <= 0) {
            throw new IllegalArgumentException("actorUserId must be positive");
        }
        Objects.requireNonNull(actorProfileType, "actorProfileType is required");
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

    private record ScopedActivityLog(ActivityLog activityLog, ActivityLogScope scope) {
    }
}
