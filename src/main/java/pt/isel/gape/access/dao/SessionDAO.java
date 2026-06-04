package pt.isel.gape.access.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import pt.isel.gape.access.model.Session;
import pt.isel.gape.access.model.SessionState;
import pt.isel.gape.common.config.ConnectionProvider;

public final class SessionDAO {

    private final ConnectionProvider connectionProvider;

    public SessionDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public Session create(long userId, String token, LocalDateTime startAt, LocalDateTime lastActivity) throws SQLException {
        String sql = """
                INSERT INTO user_session (id_user, token, state, start_at, last_activity, end_at)
                VALUES (?, ?, ?, ?, ?, NULL)
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, userId);
            statement.setString(2, token);
            statement.setString(3, SessionState.ACTIVE.toDatabaseValue());
            statement.setTimestamp(4, Timestamp.valueOf(startAt));
            statement.setTimestamp(5, Timestamp.valueOf(lastActivity));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating session failed: no generated key returned");
                }
                return new Session(
                        keys.getLong(1),
                        userId,
                        token,
                        SessionState.ACTIVE,
                        startAt,
                        lastActivity,
                        null
                );
            }
        }
    }

    public Optional<Session> findById(long sessionId) throws SQLException {
        String sql = """
                SELECT id_session, id_user, token, state, start_at, last_activity, end_at
                FROM user_session
                WHERE id_session = ?
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapSession(resultSet));
            }
        }
    }

    public Optional<Session> findByToken(String token) throws SQLException {
        String sql = """
                SELECT id_session, id_user, token, state, start_at, last_activity, end_at
                FROM user_session
                WHERE token = ?
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, token);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapSession(resultSet));
            }
        }
    }

    public Session updateLastActivity(long sessionId, LocalDateTime lastActivity) throws SQLException {
        String sql = """
                UPDATE user_session
                SET last_activity = ?
                WHERE id_session = ? AND state = ?
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(lastActivity));
            statement.setLong(2, sessionId);
            statement.setString(3, SessionState.ACTIVE.toDatabaseValue());
            statement.executeUpdate();
        }

        return findById(sessionId)
                .orElseThrow(() -> new SQLException("Session not found after last_activity update: " + sessionId));
    }

    public Session updateState(long sessionId, SessionState state, LocalDateTime lastActivity, LocalDateTime endAt)
            throws SQLException {
        String sql = """
                UPDATE user_session
                SET state = ?, last_activity = ?, end_at = ?
                WHERE id_session = ?
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            statement.setTimestamp(2, Timestamp.valueOf(lastActivity));
            if (endAt == null) {
                statement.setNull(3, java.sql.Types.TIMESTAMP);
            } else {
                statement.setTimestamp(3, Timestamp.valueOf(endAt));
            }
            statement.setLong(4, sessionId);
            statement.executeUpdate();
        }

        return findById(sessionId)
                .orElseThrow(() -> new SQLException("Session not found after state update: " + sessionId));
    }

    private Session mapSession(ResultSet resultSet) throws SQLException {
        Timestamp endAt = resultSet.getTimestamp("end_at");
        return new Session(
                resultSet.getLong("id_session"),
                resultSet.getLong("id_user"),
                resultSet.getString("token"),
                SessionState.fromDatabaseValue(resultSet.getString("state")),
                resultSet.getTimestamp("start_at").toLocalDateTime(),
                resultSet.getTimestamp("last_activity").toLocalDateTime(),
                endAt == null ? null : endAt.toLocalDateTime()
        );
    }
}
