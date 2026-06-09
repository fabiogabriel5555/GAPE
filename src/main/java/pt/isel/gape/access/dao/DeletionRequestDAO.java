package pt.isel.gape.access.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.access.model.DeletionRequest;
import pt.isel.gape.access.model.DeletionRequestState;
import pt.isel.gape.common.config.ConnectionProvider;

public final class DeletionRequestDAO {

    private final ConnectionProvider connectionProvider;

    public DeletionRequestDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public Optional<DeletionRequest> findById(long deletionRequestId) throws SQLException {
        String sql = """
                SELECT id_deletion, submitter_user_id, processor_admin_user_id,
                       submitted_at, processed_at, reason, state
                FROM deletion_request
                WHERE id_deletion = ?
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, deletionRequestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapDeletionRequest(resultSet));
            }
        }
    }

    public List<DeletionRequest> findBySubmitter(long submitterUserId) throws SQLException {
        String sql = """
                SELECT id_deletion, submitter_user_id, processor_admin_user_id,
                       submitted_at, processed_at, reason, state
                FROM deletion_request
                WHERE submitter_user_id = ?
                ORDER BY submitted_at DESC, id_deletion DESC
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, submitterUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<DeletionRequest> requests = new ArrayList<>();
                while (resultSet.next()) {
                    requests.add(mapDeletionRequest(resultSet));
                }
                return requests;
            }
        }
    }

    public List<DeletionRequest> findAll() throws SQLException {
        String sql = """
                SELECT id_deletion, submitter_user_id, processor_admin_user_id,
                       submitted_at, processed_at, reason, state
                FROM deletion_request
                ORDER BY submitted_at DESC, id_deletion DESC
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<DeletionRequest> requests = new ArrayList<>();
            while (resultSet.next()) {
                requests.add(mapDeletionRequest(resultSet));
            }
            return requests;
        }
    }

    public long create(long submitterUserId, LocalDateTime submittedAt, String reason) throws SQLException {
        String sql = """
                INSERT INTO deletion_request (
                    submitter_user_id, submitted_at, reason, state
                ) VALUES (?, ?, ?, 'submitted')
                """;

        try (Connection connection = connectionProvider.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, submitterUserId);
            statement.setObject(2, submittedAt);
            statement.setString(3, reason);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating deletion request failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public boolean process(
            long deletionRequestId,
            long processorAdminUserId,
            DeletionRequestState state,
            LocalDateTime processedAt
    ) throws SQLException {
        String sql = """
                UPDATE deletion_request
                SET processor_admin_user_id = ?, state = ?, processed_at = ?
                WHERE id_deletion = ?
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, processorAdminUserId);
            statement.setString(2, state.toDatabaseValue());
            if (processedAt == null) {
                statement.setNull(3, Types.TIMESTAMP);
            } else {
                statement.setObject(3, processedAt);
            }
            statement.setLong(4, deletionRequestId);
            return statement.executeUpdate() > 0;
        }
    }

    private static DeletionRequest mapDeletionRequest(ResultSet resultSet) throws SQLException {
        long processorId = resultSet.getLong("processor_admin_user_id");
        boolean processorWasNull = resultSet.wasNull();
        LocalDateTime processedAt = resultSet.getObject("processed_at", LocalDateTime.class);
        return new DeletionRequest(
                resultSet.getLong("id_deletion"),
                resultSet.getLong("submitter_user_id"),
                processorWasNull ? null : processorId,
                resultSet.getObject("submitted_at", LocalDateTime.class),
                processedAt,
                resultSet.getString("reason"),
                DeletionRequestState.fromDatabaseValue(resultSet.getString("state"))
        );
    }
}
