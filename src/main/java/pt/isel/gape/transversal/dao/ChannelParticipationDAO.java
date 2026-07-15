package pt.isel.gape.transversal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.transversal.model.ChannelParticipation;
import pt.isel.gape.transversal.model.ParticipationRole;
import pt.isel.gape.transversal.model.ParticipationState;

public final class ChannelParticipationDAO {

    private final ConnectionProvider connectionProvider;

    public ChannelParticipationDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public void upsert(
            Connection connection,
            long userId,
            long channelId,
            ParticipationRole role,
            LocalDateTime joinedAt,
            boolean muted,
            ParticipationState state
    ) throws SQLException {
        String sql = """
                INSERT INTO participate_channel (id_user, id_channel, role, joined_at, muted, state)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    role = VALUES(role),
                    muted = VALUES(muted),
                    state = VALUES(state),
                    joined_at = IF(state = 'active', joined_at, VALUES(joined_at))
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, channelId);
            statement.setString(3, role.toDatabaseValue());
            statement.setTimestamp(4, Timestamp.valueOf(joinedAt));
            statement.setBoolean(5, muted);
            statement.setString(6, state.toDatabaseValue());
            statement.executeUpdate();
        }
    }

    public Optional<ChannelParticipation> find(Connection connection, long userId, long channelId)
            throws SQLException {
        String sql = selectParticipationSql() + """
                WHERE id_user = ?
                  AND id_channel = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, channelId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapParticipation(resultSet));
            }
        }
    }

    public Optional<ChannelParticipation> find(long userId, long channelId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return find(connection, userId, channelId);
        }
    }

    public boolean hasActiveParticipation(Connection connection, long userId, long channelId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM participate_channel
                WHERE id_user = ?
                  AND id_channel = ?
                  AND state = 'active'
                """)) {
            statement.setLong(1, userId);
            statement.setLong(2, channelId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean hasModeratorParticipation(Connection connection, long userId, long channelId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT role
                FROM participate_channel
                WHERE id_user = ?
                  AND id_channel = ?
                  AND state = 'active'
                """)) {
            statement.setLong(1, userId);
            statement.setLong(2, channelId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return false;
                }
                return ParticipationRole.fromDatabaseValue(resultSet.getString("role")).canModerate();
            }
        }
    }

    public List<Long> findActiveParticipantIds(Connection connection, long channelId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_user
                FROM participate_channel
                WHERE id_channel = ?
                  AND state = 'active'
                ORDER BY id_user
                """)) {
            statement.setLong(1, channelId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (resultSet.next()) {
                    ids.add(resultSet.getLong("id_user"));
                }
                return ids;
            }
        }
    }

    public List<ChannelParticipation> findByChannel(Connection connection, long channelId) throws SQLException {
        String sql = selectParticipationSql() + """
                WHERE id_channel = ?
                ORDER BY id_user
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, channelId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ChannelParticipation> participations = new ArrayList<>();
                while (resultSet.next()) {
                    participations.add(mapParticipation(resultSet));
                }
                return participations;
            }
        }
    }

    private static String selectParticipationSql() {
        return """
                SELECT id_user, id_channel, role, joined_at, muted, state
                FROM participate_channel
                """;
    }

    private static ChannelParticipation mapParticipation(ResultSet resultSet) throws SQLException {
        return new ChannelParticipation(
                resultSet.getLong("id_user"),
                resultSet.getLong("id_channel"),
                ParticipationRole.fromDatabaseValue(resultSet.getString("role")),
                resultSet.getTimestamp("joined_at").toLocalDateTime(),
                resultSet.getBoolean("muted"),
                ParticipationState.fromDatabaseValue(resultSet.getString("state"))
        );
    }
}
