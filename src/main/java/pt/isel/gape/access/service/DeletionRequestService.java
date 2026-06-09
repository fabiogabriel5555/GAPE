package pt.isel.gape.access.service;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.dao.DeletionRequestDAO;
import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.DeletionRequest;
import pt.isel.gape.access.model.DeletionRequestState;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class DeletionRequestService {

    private final DeletionRequestDAO deletionRequestDAO;
    private final UserDAO userDAO;
    private final PermissionChecker permissionChecker;
    private final AuditService auditService;
    private final Clock clock;

    public DeletionRequestService(
            DeletionRequestDAO deletionRequestDAO,
            UserDAO userDAO,
            PermissionChecker permissionChecker,
            AuditService auditService,
            Clock clock
    ) {
        this.deletionRequestDAO = Objects.requireNonNull(deletionRequestDAO, "deletionRequestDAO is required");
        this.userDAO = Objects.requireNonNull(userDAO, "userDAO is required");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public DeletionRequestService(ConnectionProvider connectionProvider) {
        this(
                new DeletionRequestDAO(connectionProvider),
                new UserDAO(connectionProvider),
                new PermissionChecker(connectionProvider),
                new AuditService(new ActivityLogDAO(connectionProvider), ApplicationClock.system()),
                ApplicationClock.system()
        );
    }

    public DeletionRequest submitDeletionRequest(long actorUserId, Long sessionId, String reason, String sourceIp) {
        try {
            requireUser(actorUserId);
            LocalDateTime submittedAt = LocalDateTime.now(clock);
            long requestId = deletionRequestDAO.create(actorUserId, submittedAt, optional(reason));
            record(actorUserId, sessionId, "DELETION_SUBMIT", requestId, "success", sourceIp);
            return deletionRequestDAO.findById(requestId)
                    .orElseThrow(() -> new IllegalStateException("Created deletion request could not be loaded"));
        } catch (SQLException exception) {
            record(actorUserId, sessionId, "DELETION_SUBMIT", actorUserId, "failure", sourceIp);
            throw new IllegalStateException("Failed to submit deletion request for user " + actorUserId, exception);
        } catch (RuntimeException exception) {
            record(actorUserId, sessionId, "DELETION_SUBMIT", actorUserId, "failure", sourceIp);
            throw exception;
        }
    }

    public DeletionRequest submitDeletionRequest(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String reason,
            String sourceIp
    ) {
        Objects.requireNonNull(actorProfileType, "actorProfileType is required");
        return submitDeletionRequest(actorUserId, sessionId, reason, sourceIp);
    }

    public DeletionRequest processDeletionRequest(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long deletionRequestId,
            DeletionRequestState newState,
            LocalDateTime processedAt,
            String sourceIp
    ) {
        try {
            requireProcessor(actorUserId, actorProfileType);
            DeletionRequest existing = deletionRequestDAO.findById(deletionRequestId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown deletion request: " + deletionRequestId));
            validateProcessing(existing, newState, processedAt);
            if (!deletionRequestDAO.process(deletionRequestId, actorUserId, newState, processedAt)) {
                throw new IllegalArgumentException("Unknown deletion request: " + deletionRequestId);
            }
            record(actorUserId, sessionId, "DELETION_PROCESS", deletionRequestId, "success", sourceIp);
            return deletionRequestDAO.findById(deletionRequestId)
                    .orElseThrow(() -> new IllegalStateException("Processed deletion request could not be loaded"));
        } catch (SQLException exception) {
            record(actorUserId, sessionId, "DELETION_PROCESS", deletionRequestId, "failure", sourceIp);
            throw new IllegalStateException("Failed to process deletion request " + deletionRequestId, exception);
        } catch (RuntimeException exception) {
            record(actorUserId, sessionId, "DELETION_PROCESS", deletionRequestId, "failure", sourceIp);
            throw exception;
        }
    }

    public List<DeletionRequest> listRequests(long actorUserId, AccessProfileType actorProfileType) {
        try {
            if (hasProcessPermission(actorUserId, actorProfileType)) {
                return deletionRequestDAO.findAll();
            }
            return deletionRequestDAO.findBySubmitter(actorUserId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list deletion requests", exception);
        }
    }

    public List<DeletionRequest> listRequestsForUser(long actorUserId, AccessProfileType actorProfileType, long targetUserId) {
        try {
            if (hasProcessPermission(actorUserId, actorProfileType) || actorUserId == targetUserId) {
                requireUser(targetUserId);
                return deletionRequestDAO.findBySubmitter(targetUserId);
            }
            throw new SecurityException("PROCESS_DELETION_REQUESTS permission is required");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list deletion requests for user " + targetUserId, exception);
        }
    }

    private void requireProcessor(long actorUserId, AccessProfileType actorProfileType) {
        if (actorProfileType != AccessProfileType.ADMINISTRATOR) {
            throw new SecurityException("ADMINISTRATOR profile is required");
        }
        if (!hasProcessPermission(actorUserId, actorProfileType)) {
            throw new SecurityException("PROCESS_DELETION_REQUESTS permission is required");
        }
    }

    private boolean hasProcessPermission(long actorUserId, AccessProfileType actorProfileType) {
        Objects.requireNonNull(actorProfileType, "actorProfileType is required");
        return permissionChecker.hasPermission(actorUserId, actorProfileType, AuthorizationPolicy.PROCESS_DELETION_REQUESTS);
    }

    private static void validateProcessing(
            DeletionRequest existing,
            DeletionRequestState newState,
            LocalDateTime processedAt
    ) {
        Objects.requireNonNull(newState, "newState is required");
        if (newState == DeletionRequestState.SUBMITTED) {
            throw new IllegalArgumentException("Processed deletion request cannot return to submitted state");
        }
        if (newState.isFinal() && processedAt == null) {
            throw new IllegalArgumentException("Final deletion request state requires processedAt");
        }
        if (processedAt != null && processedAt.isBefore(existing.submittedAt())) {
            throw new IllegalArgumentException("processedAt cannot be before submittedAt");
        }
    }

    private void requireUser(long userId) throws SQLException {
        if (userDAO.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("Unknown user: " + userId);
        }
    }

    private void record(long actorUserId, Long sessionId, String operationType, long requestId, String outcome, String sourceIp) {
        auditService.record(actorUserId, sessionId, operationType, "deletion_request", Long.toString(requestId), outcome, sourceIp);
    }

    private static String optional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
