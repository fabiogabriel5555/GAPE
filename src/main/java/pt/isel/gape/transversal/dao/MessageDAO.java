package pt.isel.gape.transversal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.transversal.model.Message;
import pt.isel.gape.transversal.model.MessagePriority;
import pt.isel.gape.transversal.model.MessageState;
import pt.isel.gape.transversal.model.MessageType;

public final class MessageDAO {

    private final ConnectionProvider connectionProvider;

    public MessageDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(
            Connection connection,
            long channelId,
            Long senderUserId,
            Long parentMessageId,
            Long scheduleEventOriginId,
            String title,
            String body,
            MessageType type,
            MessagePriority priority,
            String attachment,
            LocalDateTime createdAt,
            LocalDateTime scheduledAt,
            LocalDateTime sentAt,
            MessageState state
    ) throws SQLException {
        String sql = """
                INSERT INTO message (
                    id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
                    title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, channelId);
            setNullableLong(statement, 2, senderUserId);
            setNullableLong(statement, 3, parentMessageId);
            setNullableLong(statement, 4, scheduleEventOriginId);
            setNullableString(statement, 5, title);
            setNullableString(statement, 6, body);
            statement.setString(7, type.toDatabaseValue());
            setNullableString(statement, 8, priority == null ? null : priority.toDatabaseValue());
            setNullableString(statement, 9, attachment);
            statement.setTimestamp(10, Timestamp.valueOf(createdAt));
            setNullableTimestamp(statement, 11, scheduledAt);
            setNullableTimestamp(statement, 12, sentAt);
            statement.setString(13, state.toDatabaseValue());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating message failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<Message> findById(long messageId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, messageId);
        }
    }

    public Optional<Message> findById(Connection connection, long messageId) throws SQLException {
        String sql = selectMessageSql() + " WHERE id_message = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, messageId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapMessage(resultSet));
            }
        }
    }

    public Optional<Message> lockById(Connection connection, long messageId) throws SQLException {
        String sql = selectMessageSql() + " WHERE id_message = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, messageId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapMessage(resultSet));
            }
        }
    }

    public List<Message> findDueScheduledMessages(Connection connection, LocalDateTime now) throws SQLException {
        String sql = selectMessageSql() + """
                WHERE state = 'scheduled'
                  AND scheduled_at <= ?
                ORDER BY scheduled_at, id_message
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(now));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Message> messages = new ArrayList<>();
                while (resultSet.next()) {
                    messages.add(mapMessage(resultSet));
                }
                return messages;
            }
        }
    }

    public void markSent(Connection connection, long messageId, LocalDateTime sentAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE message
                SET state = 'sent',
                    sent_at = ?,
                    updated_at = ?
                WHERE id_message = ?
                  AND state = 'scheduled'
                """)) {
            statement.setTimestamp(1, Timestamp.valueOf(sentAt));
            statement.setTimestamp(2, Timestamp.valueOf(sentAt));
            statement.setLong(3, messageId);
            statement.executeUpdate();
        }
    }

    public List<Long> findScheduleEventRecipientIds(Connection connection, long scheduleEventId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_user
                FROM receive_schedule_event
                WHERE id_schedule_event = ?
                ORDER BY id_user
                """)) {
            statement.setLong(1, scheduleEventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (resultSet.next()) {
                    ids.add(resultSet.getLong("id_user"));
                }
                return ids;
            }
        }
    }

    private static String selectMessageSql() {
        return """
                SELECT id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
                       title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
                FROM message
                """;
    }

    private static Message mapMessage(ResultSet resultSet) throws SQLException {
        String priority = resultSet.getString("priority");
        return new Message(
                resultSet.getLong("id_message"),
                resultSet.getLong("id_channel"),
                nullableLong(resultSet, "id_user_sender"),
                nullableLong(resultSet, "id_parent_message"),
                nullableLong(resultSet, "id_schedule_event_origin"),
                resultSet.getString("title"),
                resultSet.getString("body"),
                MessageType.fromDatabaseValue(resultSet.getString("type")),
                priority == null ? null : MessagePriority.fromDatabaseValue(priority),
                resultSet.getString("attachment"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                nullableTimestamp(resultSet, "updated_at"),
                nullableTimestamp(resultSet, "scheduled_at"),
                nullableTimestamp(resultSet, "sent_at"),
                MessageState.fromDatabaseValue(resultSet.getString("state"))
        );
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static LocalDateTime nullableTimestamp(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
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

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }
}
