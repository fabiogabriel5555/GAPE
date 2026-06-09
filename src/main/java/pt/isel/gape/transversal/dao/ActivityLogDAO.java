package pt.isel.gape.transversal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.transversal.model.ActivityLog;

public final class ActivityLogDAO {

    private final ConnectionProvider connectionProvider;

    public ActivityLogDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long insert(
            Long userId,
            Long sessionId,
            String operationType,
            String affectedEntityType,
            String affectedEntityIdentifier,
            LocalDateTime occurredAt,
            String outcome,
            String sourceIp
    ) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return insert(connection, userId, sessionId, operationType, affectedEntityType,
                    affectedEntityIdentifier, occurredAt, outcome, sourceIp);
        }
    }

    public long insert(
            Connection connection,
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

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            validateSessionUser(connection, userId, sessionId);
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
            statement.setObject(6, occurredAt);
            statement.setString(7, outcome);
            statement.setString(8, sourceIp);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating activity log failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    private static void validateSessionUser(Connection connection, Long userId, Long sessionId) throws SQLException {
        if (userId == null || sessionId == null) {
            return;
        }
        String sql = """
                SELECT id_user
                FROM user_session
                WHERE id_session = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || resultSet.getLong("id_user") != userId) {
                    throw new SQLException("Activity_Log Session must belong to the same User", "45000", 1644);
                }
            }
        }
    }

    public Optional<ActivityLog> findById(long activityLogId) throws SQLException {
        String sql = """
                SELECT id_activity_log, id_user, id_session, operation_type,
                       affected_entity_type, affected_entity_identifier,
                       occurred_at, outcome, source_ip
                FROM activity_log
                WHERE id_activity_log = ?
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, activityLogId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapActivityLog(resultSet));
            }
        }
    }

    public List<ActivityLog> findAll() throws SQLException {
        String sql = """
                SELECT id_activity_log, id_user, id_session, operation_type,
                       affected_entity_type, affected_entity_identifier,
                       occurred_at, outcome, source_ip
                FROM activity_log
                ORDER BY occurred_at DESC, id_activity_log DESC
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<ActivityLog> logs = new ArrayList<>();
            while (resultSet.next()) {
                logs.add(mapActivityLog(resultSet));
            }
            return logs;
        }
    }

    public List<ActivityLog> findByUserId(long userId) throws SQLException {
        String sql = """
                SELECT id_activity_log, id_user, id_session, operation_type,
                       affected_entity_type, affected_entity_identifier,
                       occurred_at, outcome, source_ip
                FROM activity_log
                WHERE id_user = ?
                ORDER BY occurred_at DESC, id_activity_log DESC
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ActivityLog> logs = new ArrayList<>();
                while (resultSet.next()) {
                    logs.add(mapActivityLog(resultSet));
                }
                return logs;
            }
        }
    }

    public List<ActivityLog> findByUserInvolvement(long userId) throws SQLException {
        String sql = """
                SELECT id_activity_log, id_user, id_session, operation_type,
                       affected_entity_type, affected_entity_identifier,
                       occurred_at, outcome, source_ip
                FROM activity_log
                WHERE id_user = ?
                   OR (affected_entity_type = 'user_account' AND affected_entity_identifier = ?)
                ORDER BY occurred_at DESC, id_activity_log DESC
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, Long.toString(userId));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ActivityLog> logs = new ArrayList<>();
                while (resultSet.next()) {
                    logs.add(mapActivityLog(resultSet));
                }
                return logs;
            }
        }
    }

    private static ActivityLog mapActivityLog(ResultSet resultSet) throws SQLException {
        long userId = resultSet.getLong("id_user");
        boolean userWasNull = resultSet.wasNull();
        long sessionId = resultSet.getLong("id_session");
        boolean sessionWasNull = resultSet.wasNull();
        return new ActivityLog(
                resultSet.getLong("id_activity_log"),
                userWasNull ? null : userId,
                sessionWasNull ? null : sessionId,
                resultSet.getString("operation_type"),
                resultSet.getString("affected_entity_type"),
                resultSet.getString("affected_entity_identifier"),
                resultSet.getObject("occurred_at", LocalDateTime.class),
                resultSet.getString("outcome"),
                resultSet.getString("source_ip")
        );
    }
}
