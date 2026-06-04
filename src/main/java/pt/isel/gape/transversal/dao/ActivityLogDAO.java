package pt.isel.gape.transversal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import pt.isel.gape.common.config.ConnectionProvider;

public final class ActivityLogDAO {

    private final ConnectionProvider connectionProvider;

    public ActivityLogDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public void insert(
            Long userId,
            Long sessionId,
            String operationType,
            String affectedEntityType,
            String affectedEntityIdentifier,
            LocalDateTime occurredAt,
            String outcome,
            String sourceIp
    ) throws SQLException {
        String sql = """
                INSERT INTO activity_log (
                    id_user, id_session, operation_type, affected_entity_type,
                    affected_entity_identifier, occurred_at, outcome, source_ip
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (userId == null) {
                statement.setNull(1, java.sql.Types.BIGINT);
            } else {
                statement.setLong(1, userId);
            }
            if (sessionId == null) {
                statement.setNull(2, java.sql.Types.BIGINT);
            } else {
                statement.setLong(2, sessionId);
            }
            statement.setString(3, operationType);
            statement.setString(4, affectedEntityType);
            statement.setString(5, affectedEntityIdentifier);
            statement.setTimestamp(6, Timestamp.valueOf(occurredAt));
            statement.setString(7, outcome);
            statement.setString(8, sourceIp);
            statement.executeUpdate();
        }
    }
}
