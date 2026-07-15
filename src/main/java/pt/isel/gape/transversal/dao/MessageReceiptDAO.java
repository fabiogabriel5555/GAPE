package pt.isel.gape.transversal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.transversal.model.MessageReceipt;
import pt.isel.gape.transversal.model.MessageReceiptState;

public final class MessageReceiptDAO {

    private final ConnectionProvider connectionProvider;

    public MessageReceiptDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public void upsert(
            Connection connection,
            long userId,
            long messageId,
            LocalDateTime deliveredAt,
            LocalDateTime readAt,
            MessageReceiptState state
    ) throws SQLException {
        String sql = """
                INSERT INTO receive_message (id_user, id_message, delivered_at, read_at, state)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    delivered_at = VALUES(delivered_at),
                    read_at = VALUES(read_at),
                    state = VALUES(state)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, messageId);
            setNullableTimestamp(statement, 3, deliveredAt);
            setNullableTimestamp(statement, 4, readAt);
            statement.setString(5, state.toDatabaseValue());
            statement.executeUpdate();
        }
    }

    public Optional<MessageReceipt> find(Connection connection, long userId, long messageId) throws SQLException {
        String sql = selectReceiptSql() + """
                WHERE id_user = ?
                  AND id_message = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, messageId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapReceipt(resultSet));
            }
        }
    }

    public Optional<MessageReceipt> find(long userId, long messageId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return find(connection, userId, messageId);
        }
    }

    public List<MessageReceipt> findByMessage(Connection connection, long messageId) throws SQLException {
        String sql = selectReceiptSql() + """
                WHERE id_message = ?
                ORDER BY id_user
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, messageId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MessageReceipt> receipts = new ArrayList<>();
                while (resultSet.next()) {
                    receipts.add(mapReceipt(resultSet));
                }
                return receipts;
            }
        }
    }

    public void markRead(Connection connection, long userId, long messageId, LocalDateTime readAt)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE receive_message
                SET read_at = ?,
                    state = 'read'
                WHERE id_user = ?
                  AND id_message = ?
                  AND delivered_at IS NOT NULL
                  AND delivered_at <= ?
                """)) {
            statement.setTimestamp(1, Timestamp.valueOf(readAt));
            statement.setLong(2, userId);
            statement.setLong(3, messageId);
            statement.setTimestamp(4, Timestamp.valueOf(readAt));
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Message receipt cannot be marked as read before delivery");
            }
        }
    }

    public int markDirectConversationRead(
            Connection connection,
            long userId,
            long peerUserId,
            LocalDateTime readAt
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE receive_message rm
                JOIN message m ON m.id_message = rm.id_message
                JOIN channel c ON c.id_channel = m.id_channel
                JOIN participate_channel self_pc ON self_pc.id_channel = c.id_channel
                    AND self_pc.id_user = ?
                    AND self_pc.state = 'active'
                JOIN participate_channel peer_pc ON peer_pc.id_channel = c.id_channel
                    AND peer_pc.id_user = ?
                    AND peer_pc.state = 'active'
                SET rm.read_at = ?,
                    rm.state = 'read'
                WHERE rm.id_user = ?
                  AND m.id_user_sender = ?
                  AND rm.delivered_at IS NOT NULL
                  AND rm.delivered_at <= ?
                  AND rm.read_at IS NULL
                  AND m.state = 'sent'
                  AND m.type IN ('text', 'attachment')
                  AND c.state = 'active'
                  AND c.type = 'message'
                  AND c.visibility = 'participants'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM participate_channel extra_pc
                      WHERE extra_pc.id_channel = c.id_channel
                        AND extra_pc.state = 'active'
                        AND extra_pc.id_user NOT IN (?, ?)
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM associate_channel_class_group acg WHERE acg.id_channel = c.id_channel
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM associate_channel_content_block acb WHERE acb.id_channel = c.id_channel
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM associate_channel_assessment aa WHERE aa.id_channel = c.id_channel
                  )
                """)) {
            statement.setLong(1, userId);
            statement.setLong(2, peerUserId);
            statement.setTimestamp(3, Timestamp.valueOf(readAt));
            statement.setLong(4, userId);
            statement.setLong(5, peerUserId);
            statement.setTimestamp(6, Timestamp.valueOf(readAt));
            statement.setLong(7, userId);
            statement.setLong(8, peerUserId);
            return statement.executeUpdate();
        }
    }

    public void markPendingReceiptsDelivered(Connection connection, long messageId, LocalDateTime deliveredAt)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE receive_message
                SET delivered_at = ?,
                    state = 'delivered'
                WHERE id_message = ?
                  AND state = 'pending'
                """)) {
            statement.setTimestamp(1, Timestamp.valueOf(deliveredAt));
            statement.setLong(2, messageId);
            statement.executeUpdate();
        }
    }

    private static String selectReceiptSql() {
        return """
                SELECT id_user, id_message, delivered_at, read_at, state
                FROM receive_message
                """;
    }

    private static MessageReceipt mapReceipt(ResultSet resultSet) throws SQLException {
        return new MessageReceipt(
                resultSet.getLong("id_user"),
                resultSet.getLong("id_message"),
                nullableTimestamp(resultSet, "delivered_at"),
                nullableTimestamp(resultSet, "read_at"),
                MessageReceiptState.fromDatabaseValue(resultSet.getString("state"))
        );
    }

    private static LocalDateTime nullableTimestamp(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static void setNullableTimestamp(
            PreparedStatement statement,
            int index,
            LocalDateTime value
    ) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }
}
