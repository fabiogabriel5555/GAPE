package pt.isel.gape.transversal.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.transversal.dao.ActivityLogDAO;

public final class AuditService {

    private final ActivityLogDAO activityLogDAO;
    private final Clock clock;

    public AuditService(ActivityLogDAO activityLogDAO, Clock clock) {
        this.activityLogDAO = Objects.requireNonNull(activityLogDAO, "activityLogDAO is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public AuditService(ConnectionProvider connectionProvider, Clock clock) {
        this(new ActivityLogDAO(connectionProvider), clock);
    }

    public void record(
            Long userId,
            Long sessionId,
            String operationType,
            String affectedEntityType,
            String affectedEntityIdentifier,
            String outcome,
            String sourceIp
    ) {
        try {
            activityLogDAO.insert(
                    userId,
                    sessionId,
                    operationType,
                    affectedEntityType,
                    affectedEntityIdentifier,
                    LocalDateTime.now(clock),
                    outcome,
                    sourceIp
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to record audit event " + operationType, exception);
        }
    }

    public void record(
            Connection connection,
            Long userId,
            Long sessionId,
            String operationType,
            String affectedEntityType,
            String affectedEntityIdentifier,
            String outcome,
            String sourceIp
    ) throws SQLException {
        activityLogDAO.insert(
                connection,
                userId,
                sessionId,
                operationType,
                affectedEntityType,
                affectedEntityIdentifier,
                LocalDateTime.now(clock),
                outcome,
                sourceIp
        );
    }
}
