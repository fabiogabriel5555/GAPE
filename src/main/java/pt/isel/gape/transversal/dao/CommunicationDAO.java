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
import pt.isel.gape.transversal.model.DirectConversationSummary;
import pt.isel.gape.transversal.model.MessagePriority;
import pt.isel.gape.transversal.model.MessageRecipientSummary;
import pt.isel.gape.transversal.model.MessageReceiptState;
import pt.isel.gape.transversal.model.MessageState;
import pt.isel.gape.transversal.model.MessageSummary;
import pt.isel.gape.transversal.model.MessageType;

public final class CommunicationDAO {

    private final ConnectionProvider connectionProvider;

    public CommunicationDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public List<DirectConversationSummary> findDirectConversationsForUser(Connection connection, long userId)
            throws SQLException {
        String sql = """
                WITH direct_messages AS (
                    SELECT peer.id_user AS peer_user_id,
                           peer.name AS peer_name,
                           peer.email AS peer_email,
                           m.id_message,
                           m.id_user_sender,
                           COALESCE(NULLIF(TRIM(m.body), ''), m.title, 'Attachment') AS body,
                           COALESCE(m.sent_at, m.created_at) AS activity_at,
                           CASE
                               WHEN rm_self.id_user = ?
                                AND rm_self.delivered_at IS NOT NULL
                                AND rm_self.read_at IS NULL
                                AND m.id_user_sender <> ?
                               THEN 1 ELSE 0
                           END AS unread_flag
                    FROM channel c
                    JOIN participate_channel self_pc ON self_pc.id_channel = c.id_channel
                        AND self_pc.id_user = ?
                        AND self_pc.state = 'active'
                    JOIN participate_channel peer_pc ON peer_pc.id_channel = c.id_channel
                        AND peer_pc.id_user <> ?
                        AND peer_pc.state = 'active'
                    JOIN user_account peer ON peer.id_user = peer_pc.id_user
                        AND peer.state = 'active'
                    JOIN message m ON m.id_channel = c.id_channel
                        AND m.type IN ('text', 'attachment')
                        AND m.state = 'sent'
                        AND m.id_user_sender IS NOT NULL
                        AND (
                            m.id_user_sender = ?
                            OR EXISTS (
                                SELECT 1
                                FROM receive_message delivered_self
                                WHERE delivered_self.id_message = m.id_message
                                  AND delivered_self.id_user = ?
                                  AND delivered_self.delivered_at IS NOT NULL
                            )
                        )
                    LEFT JOIN receive_message rm_self ON rm_self.id_message = m.id_message
                        AND rm_self.id_user = ?
                    WHERE c.state = 'active'
                      AND c.type = 'message'
                      AND c.visibility = 'participants'
                      AND NOT EXISTS (
                          SELECT 1
                          FROM participate_channel extra_pc
                          WHERE extra_pc.id_channel = c.id_channel
                            AND extra_pc.state = 'active'
                            AND extra_pc.id_user NOT IN (?, peer.id_user)
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
                ),
                ranked_messages AS (
                    SELECT direct_messages.*,
                           COUNT(*) OVER (PARTITION BY peer_user_id) AS message_count,
                           SUM(unread_flag) OVER (PARTITION BY peer_user_id) AS unread_count,
                           ROW_NUMBER() OVER (
                               PARTITION BY peer_user_id
                               ORDER BY activity_at DESC, id_message DESC
                           ) AS message_rank
                    FROM direct_messages
                )
                SELECT peer_user_id,
                       peer_name,
                       peer_email,
                       message_count,
                       COALESCE(unread_count, 0) AS unread_count,
                       activity_at AS last_message_at,
                       body AS last_message_body,
                       id_user_sender AS last_message_sender_user_id
                FROM ranked_messages
                WHERE message_rank = 1
                ORDER BY activity_at DESC, peer_name, peer_email
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, userId);
            statement.setLong(3, userId);
            statement.setLong(4, userId);
            statement.setLong(5, userId);
            statement.setLong(6, userId);
            statement.setLong(7, userId);
            statement.setLong(8, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<DirectConversationSummary> conversations = new ArrayList<>();
                while (resultSet.next()) {
                    conversations.add(mapConversation(resultSet));
                }
                return conversations;
            }
        }
    }

    public List<MessageSummary> findDirectMessagesForUserAndPeer(
            Connection connection,
            long userId,
            long peerUserId
    ) throws SQLException {
        return findDirectMessagesForUserAndPeer(connection, userId, peerUserId, null, Integer.MAX_VALUE);
    }

    public List<MessageSummary> findDirectMessagesForUserAndPeer(
            Connection connection,
            long userId,
            long peerUserId,
            int limit
    ) throws SQLException {
        return findDirectMessagesForUserAndPeer(connection, userId, peerUserId, null, limit);
    }

    public List<MessageSummary> findDirectMessagesForUserAndPeerBefore(
            Connection connection,
            long userId,
            long peerUserId,
            long beforeMessageId,
            int limit
    ) throws SQLException {
        return findDirectMessagesForUserAndPeer(connection, userId, peerUserId, beforeMessageId, limit);
    }

    private List<MessageSummary> findDirectMessagesForUserAndPeer(
            Connection connection,
            long userId,
            long peerUserId,
            Long beforeMessageId,
            int limit
    ) throws SQLException {
        String beforePredicate = beforeMessageId == null ? "" : """
                  AND (
                        COALESCE(m.sent_at, m.created_at) < (
                            SELECT COALESCE(anchor.sent_at, anchor.created_at)
                            FROM message anchor
                            WHERE anchor.id_message = ?
                        )
                        OR (
                            COALESCE(m.sent_at, m.created_at) = (
                                SELECT COALESCE(anchor.sent_at, anchor.created_at)
                                FROM message anchor
                                WHERE anchor.id_message = ?
                            )
                            AND m.id_message < ?
                        )
                  )
                """;
        String sql = """
                SELECT *
                FROM (
                """ + messageSummarySelectSql("""
                (
                    (m.id_user_sender = ? AND rm.id_user = ?)
                    OR (m.id_user_sender <> ? AND rm.id_user = ?)
                )
                """) + """
                    JOIN participate_channel self_pc ON self_pc.id_channel = m.id_channel
                        AND self_pc.id_user = ?
                        AND self_pc.state = 'active'
                    JOIN participate_channel peer_pc ON peer_pc.id_channel = m.id_channel
                        AND peer_pc.id_user = ?
                        AND peer_pc.state = 'active'
                    WHERE c.state = 'active'
                      AND c.type = 'message'
                      AND c.visibility = 'participants'
                      AND m.type IN ('text', 'attachment')
                      AND m.state = 'sent'
                      AND m.id_user_sender IS NOT NULL
                      AND (
                            m.id_user_sender = ?
                            OR (
                                m.id_user_sender = ?
                                AND EXISTS (
                                    SELECT 1
                                    FROM receive_message delivered_self
                                    WHERE delivered_self.id_message = m.id_message
                                      AND delivered_self.id_user = ?
                                      AND delivered_self.delivered_at IS NOT NULL
                                )
                            )
                      )
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
                """ + beforePredicate + """
                    ORDER BY COALESCE(m.sent_at, m.created_at) DESC, m.id_message DESC
                    LIMIT ?
                ) recent_messages
                ORDER BY COALESCE(sent_at, created_at), id_message
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameter = 1;
            statement.setLong(parameter++, userId);
            statement.setLong(parameter++, peerUserId);
            statement.setLong(parameter++, userId);
            statement.setLong(parameter++, userId);
            statement.setLong(parameter++, userId);
            statement.setLong(parameter++, peerUserId);
            statement.setLong(parameter++, userId);
            statement.setLong(parameter++, peerUserId);
            statement.setLong(parameter++, userId);
            statement.setLong(parameter++, userId);
            statement.setLong(parameter++, peerUserId);
            if (beforeMessageId != null) {
                statement.setLong(parameter++, beforeMessageId);
                statement.setLong(parameter++, beforeMessageId);
                statement.setLong(parameter++, beforeMessageId);
            }
            statement.setInt(parameter, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MessageSummary> messages = new ArrayList<>();
                while (resultSet.next()) {
                    messages.add(mapMessageSummary(resultSet));
                }
                return messages;
            }
        }
    }

    public Optional<DirectConversationSummary> findDirectConversationForUserAndPeer(
            Connection connection,
            long userId,
            long peerUserId
    ) throws SQLException {
        String sql = """
                WITH direct_messages AS (
                    SELECT peer.id_user AS peer_user_id,
                           peer.name AS peer_name,
                           peer.email AS peer_email,
                           m.id_message,
                           m.id_user_sender,
                           COALESCE(NULLIF(TRIM(m.body), ''), m.title, 'Attachment') AS body,
                           COALESCE(m.sent_at, m.created_at) AS activity_at,
                           CASE
                               WHEN rm_self.id_user = ?
                                AND rm_self.delivered_at IS NOT NULL
                                AND rm_self.read_at IS NULL
                                AND m.id_user_sender <> ?
                               THEN 1 ELSE 0
                           END AS unread_flag
                    FROM channel c
                    JOIN participate_channel self_pc ON self_pc.id_channel = c.id_channel
                        AND self_pc.id_user = ?
                        AND self_pc.state = 'active'
                    JOIN participate_channel peer_pc ON peer_pc.id_channel = c.id_channel
                        AND peer_pc.id_user = ?
                        AND peer_pc.state = 'active'
                    JOIN user_account peer ON peer.id_user = peer_pc.id_user
                        AND peer.state = 'active'
                    JOIN message m ON m.id_channel = c.id_channel
                        AND m.type IN ('text', 'attachment')
                        AND m.state = 'sent'
                        AND m.id_user_sender IS NOT NULL
                        AND (
                            m.id_user_sender = ?
                            OR EXISTS (
                                SELECT 1
                                FROM receive_message delivered_self
                                WHERE delivered_self.id_message = m.id_message
                                  AND delivered_self.id_user = ?
                                  AND delivered_self.delivered_at IS NOT NULL
                            )
                        )
                    LEFT JOIN receive_message rm_self ON rm_self.id_message = m.id_message
                        AND rm_self.id_user = ?
                    WHERE c.state = 'active'
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
                ),
                ranked_messages AS (
                    SELECT direct_messages.*,
                           COUNT(*) OVER (PARTITION BY peer_user_id) AS message_count,
                           SUM(unread_flag) OVER (PARTITION BY peer_user_id) AS unread_count,
                           ROW_NUMBER() OVER (
                               PARTITION BY peer_user_id
                               ORDER BY activity_at DESC, id_message DESC
                           ) AS message_rank
                    FROM direct_messages
                )
                SELECT peer_user_id,
                       peer_name,
                       peer_email,
                       message_count,
                       COALESCE(unread_count, 0) AS unread_count,
                       activity_at AS last_message_at,
                       body AS last_message_body,
                       id_user_sender AS last_message_sender_user_id
                FROM ranked_messages
                WHERE message_rank = 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, userId);
            statement.setLong(3, userId);
            statement.setLong(4, peerUserId);
            statement.setLong(5, userId);
            statement.setLong(6, userId);
            statement.setLong(7, userId);
            statement.setLong(8, userId);
            statement.setLong(9, peerUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapConversation(resultSet));
            }
        }
    }

    public List<MessageRecipientSummary> findActiveMessageRecipients(
            Connection connection,
            long userId,
            int limit
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_user, name, email
                FROM user_account
                WHERE state = 'active'
                  AND id_user <> ?
                ORDER BY name, email
                LIMIT ?
                """)) {
            statement.setLong(1, userId);
            statement.setInt(2, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MessageRecipientSummary> recipients = new ArrayList<>();
                while (resultSet.next()) {
                    recipients.add(new MessageRecipientSummary(
                            resultSet.getLong("id_user"),
                            resultSet.getString("name"),
                            resultSet.getString("email")
                    ));
                }
                return recipients;
            }
        }
    }

    public Optional<MessageRecipientSummary> findActiveMessageRecipient(
            Connection connection,
            long userId,
            long recipientUserId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_user, name, email
                FROM user_account
                WHERE state = 'active'
                  AND id_user = ?
                  AND id_user <> ?
                """)) {
            statement.setLong(1, recipientUserId);
            statement.setLong(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new MessageRecipientSummary(
                        resultSet.getLong("id_user"),
                        resultSet.getString("name"),
                        resultSet.getString("email")
                ));
            }
        }
    }

    public Optional<Long> findDirectPeerUserIdForMessage(
            Connection connection,
            long userId,
            long messageId
    ) throws SQLException {
        String sql = """
                SELECT peer_pc.id_user AS peer_user_id
                FROM message m
                JOIN channel c ON c.id_channel = m.id_channel
                JOIN participate_channel self_pc ON self_pc.id_channel = c.id_channel
                    AND self_pc.id_user = ?
                    AND self_pc.state = 'active'
                JOIN participate_channel peer_pc ON peer_pc.id_channel = c.id_channel
                    AND peer_pc.id_user <> ?
                    AND peer_pc.state = 'active'
                LEFT JOIN receive_message rm_self ON rm_self.id_message = m.id_message
                    AND rm_self.id_user = ?
                WHERE m.id_message = ?
                  AND c.state = 'active'
                  AND c.type = 'message'
                  AND c.visibility = 'participants'
                  AND m.type IN ('text', 'attachment')
                  AND m.state = 'sent'
                  AND m.id_user_sender IS NOT NULL
                  AND (
                        m.id_user_sender = ?
                        OR (
                            rm_self.id_user = ?
                            AND rm_self.delivered_at IS NOT NULL
                        )
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM participate_channel extra_pc
                      WHERE extra_pc.id_channel = c.id_channel
                        AND extra_pc.state = 'active'
                        AND extra_pc.id_user NOT IN (?, peer_pc.id_user)
                  )
                ORDER BY peer_pc.id_user
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, userId);
            statement.setLong(3, userId);
            statement.setLong(4, messageId);
            statement.setLong(5, userId);
            statement.setLong(6, userId);
            statement.setLong(7, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(resultSet.getLong("peer_user_id"));
            }
        }
    }

    public Optional<MessageSummary> findDirectMessageForUser(
            Connection connection,
            long userId,
            long messageId
    ) throws SQLException {
        String sql = messageSummarySelectSql("rm.id_user = ?") + """
                JOIN participate_channel self_pc ON self_pc.id_channel = m.id_channel
                    AND self_pc.id_user = ?
                    AND self_pc.state = 'active'
                JOIN participate_channel peer_pc ON peer_pc.id_channel = m.id_channel
                    AND peer_pc.id_user <> ?
                    AND peer_pc.state = 'active'
                LEFT JOIN receive_message rm_self ON rm_self.id_message = m.id_message
                    AND rm_self.id_user = ?
                WHERE m.id_message = ?
                  AND c.state = 'active'
                  AND c.type = 'message'
                  AND c.visibility = 'participants'
                  AND m.type IN ('text', 'attachment')
                  AND m.state = 'sent'
                  AND m.id_user_sender IS NOT NULL
                  AND (
                        m.id_user_sender = ?
                        OR (
                            rm_self.id_user = ?
                            AND rm_self.delivered_at IS NOT NULL
                        )
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM participate_channel extra_pc
                      WHERE extra_pc.id_channel = c.id_channel
                        AND extra_pc.state = 'active'
                        AND extra_pc.id_user NOT IN (?, peer_pc.id_user)
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
                ORDER BY peer_pc.id_user
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, userId);
            statement.setLong(3, userId);
            statement.setLong(4, userId);
            statement.setLong(5, messageId);
            statement.setLong(6, userId);
            statement.setLong(7, userId);
            statement.setLong(8, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapMessageSummary(resultSet));
            }
        }
    }

    public Optional<Long> findDirectMessageChannel(
            Connection connection,
            long userId,
            long peerUserId
    ) throws SQLException {
        String sql = """
                SELECT c.id_channel
                FROM channel c
                JOIN participate_channel self_pc ON self_pc.id_channel = c.id_channel
                    AND self_pc.id_user = ?
                    AND self_pc.state = 'active'
                JOIN participate_channel peer_pc ON peer_pc.id_channel = c.id_channel
                    AND peer_pc.id_user = ?
                    AND peer_pc.state = 'active'
                WHERE c.state = 'active'
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
                ORDER BY c.created_at DESC, c.id_channel DESC
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, peerUserId);
            statement.setLong(3, userId);
            statement.setLong(4, peerUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(resultSet.getLong("id_channel"));
            }
        }
    }

    public Optional<Long> findRegisteredDirectMessageChannel(
            Connection connection,
            long userId,
            long peerUserId
    ) throws SQLException {
        String sql = """
                SELECT dmc.id_channel
                FROM direct_message_channel dmc
                JOIN channel c ON c.id_channel = dmc.id_channel
                JOIN participate_channel self_pc ON self_pc.id_channel = c.id_channel
                    AND self_pc.id_user = ?
                    AND self_pc.state = 'active'
                JOIN participate_channel peer_pc ON peer_pc.id_channel = c.id_channel
                    AND peer_pc.id_user = ?
                    AND peer_pc.state = 'active'
                WHERE dmc.id_user_low = ?
                  AND dmc.id_user_high = ?
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
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, peerUserId);
            statement.setLong(3, Math.min(userId, peerUserId));
            statement.setLong(4, Math.max(userId, peerUserId));
            statement.setLong(5, userId);
            statement.setLong(6, peerUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(resultSet.getLong("id_channel"));
            }
        }
    }

    public Optional<Long> findDirectMessageChannelRegistration(
            Connection connection,
            long userId,
            long peerUserId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_channel
                FROM direct_message_channel
                WHERE id_user_low = ?
                  AND id_user_high = ?
                """)) {
            statement.setLong(1, Math.min(userId, peerUserId));
            statement.setLong(2, Math.max(userId, peerUserId));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(resultSet.getLong("id_channel"));
            }
        }
    }

    public void registerDirectMessageChannel(
            Connection connection,
            long userId,
            long peerUserId,
            long channelId
    ) throws SQLException {
        String sql = """
                INSERT INTO direct_message_channel (id_user_low, id_user_high, id_channel)
                VALUES (?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, Math.min(userId, peerUserId));
            statement.setLong(2, Math.max(userId, peerUserId));
            statement.setLong(3, channelId);
            statement.executeUpdate();
        }
    }

    public List<MessageSummary> findMessagesForUser(Connection connection, long userId) throws SQLException {
        String sql = messageSummarySelectSql("rm.id_user = ?") + """
                JOIN participate_channel pc ON pc.id_channel = m.id_channel
                    AND pc.id_user = ?
                    AND pc.state = 'active'
                WHERE c.state = 'active'
                  AND m.state <> 'deleted'
                  AND (
                        (
                            rm.id_user = ?
                            AND rm.delivered_at IS NOT NULL
                            AND m.state = 'sent'
                        )
                        OR (
                            m.id_user_sender = ?
                            AND m.state IN ('sent', 'scheduled', 'draft')
                        )
                  )
                ORDER BY COALESCE(m.sent_at, m.scheduled_at, m.created_at) DESC, m.id_message DESC
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, userId);
            statement.setLong(3, userId);
            statement.setLong(4, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MessageSummary> messages = new ArrayList<>();
                while (resultSet.next()) {
                    messages.add(mapMessageSummary(resultSet));
                }
                return messages;
            }
        }
    }

    public List<MessageSummary> findUnreadNotifications(Connection connection, long userId, int limit)
            throws SQLException {
        String sql = messageSummarySelectSql("rm.id_user = ?") + """
                JOIN participate_channel pc ON pc.id_channel = m.id_channel
                    AND pc.id_user = ?
                    AND pc.state = 'active'
                WHERE rm.id_user = ?
                  AND rm.delivered_at IS NOT NULL
                  AND rm.read_at IS NULL
                  AND rm.state IN ('delivered', 'pending')
                  AND m.state = 'sent'
                  AND m.type IN ('notification', 'reminder', 'alert', 'warning', 'system')
                  AND c.state = 'active'
                ORDER BY rm.delivered_at DESC, m.id_message DESC
                LIMIT ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, userId);
            statement.setLong(3, userId);
            statement.setInt(4, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MessageSummary> messages = new ArrayList<>();
                while (resultSet.next()) {
                    messages.add(mapMessageSummary(resultSet));
                }
                return messages;
            }
        }
    }

    public int countUnreadDeliveredMessages(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM receive_message rm
                JOIN message m ON m.id_message = rm.id_message
                JOIN channel c ON c.id_channel = m.id_channel
                JOIN participate_channel pc ON pc.id_channel = c.id_channel
                    AND pc.id_user = rm.id_user
                    AND pc.state = 'active'
                WHERE rm.id_user = ?
                  AND rm.delivered_at IS NOT NULL
                  AND rm.read_at IS NULL
                  AND rm.state IN ('delivered', 'pending')
                  AND m.state = 'sent'
                  AND c.state = 'active'
                """)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public int countUnreadDirectMessages(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM receive_message rm
                JOIN message m ON m.id_message = rm.id_message
                JOIN channel c ON c.id_channel = m.id_channel
                JOIN participate_channel self_pc ON self_pc.id_channel = c.id_channel
                    AND self_pc.id_user = rm.id_user
                    AND self_pc.state = 'active'
                JOIN participate_channel peer_pc ON peer_pc.id_channel = c.id_channel
                    AND peer_pc.id_user <> rm.id_user
                    AND peer_pc.state = 'active'
                WHERE rm.id_user = ?
                  AND rm.delivered_at IS NOT NULL
                  AND rm.read_at IS NULL
                  AND rm.state IN ('delivered', 'pending')
                  AND m.state = 'sent'
                  AND m.type IN ('text', 'attachment')
                  AND m.id_user_sender IS NOT NULL
                  AND m.id_user_sender <> ?
                  AND c.state = 'active'
                  AND c.type = 'message'
                  AND c.visibility = 'participants'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM participate_channel extra_pc
                      WHERE extra_pc.id_channel = c.id_channel
                        AND extra_pc.state = 'active'
                        AND extra_pc.id_user NOT IN (rm.id_user, peer_pc.id_user)
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
            statement.setLong(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public int countUnreadDeliveredNotifications(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM receive_message rm
                JOIN message m ON m.id_message = rm.id_message
                JOIN channel c ON c.id_channel = m.id_channel
                JOIN participate_channel pc ON pc.id_channel = c.id_channel
                    AND pc.id_user = rm.id_user
                    AND pc.state = 'active'
                WHERE rm.id_user = ?
                  AND rm.delivered_at IS NOT NULL
                  AND rm.read_at IS NULL
                  AND rm.state IN ('delivered', 'pending')
                  AND m.state = 'sent'
                  AND m.type IN ('notification', 'reminder', 'alert', 'warning', 'system')
                  AND c.state = 'active'
                """)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static String messageSummarySelectSql(String receiptJoinCondition) {
        return """
                SELECT m.id_message, m.id_channel, c.title AS channel_title,
                       m.id_user_sender, sender.name AS sender_name, sender.email AS sender_email,
                       m.id_parent_message, m.id_schedule_event_origin, m.title, m.body, m.type,
                       m.priority, m.attachment, m.created_at, m.updated_at, m.scheduled_at, m.sent_at,
                       m.state, rm.delivered_at, rm.read_at, rm.state AS receipt_state
                FROM message m
                JOIN channel c ON c.id_channel = m.id_channel
                LEFT JOIN user_account sender ON sender.id_user = m.id_user_sender
                LEFT JOIN receive_message rm ON rm.id_message = m.id_message
                    AND %s
                """.formatted(receiptJoinCondition);
    }

    private static DirectConversationSummary mapConversation(ResultSet resultSet) throws SQLException {
        return new DirectConversationSummary(
                resultSet.getLong("peer_user_id"),
                resultSet.getString("peer_name"),
                resultSet.getString("peer_email"),
                resultSet.getInt("message_count"),
                resultSet.getInt("unread_count"),
                nullableTimestamp(resultSet, "last_message_at"),
                resultSet.getString("last_message_body"),
                nullableLong(resultSet, "last_message_sender_user_id")
        );
    }

    private static MessageSummary mapMessageSummary(ResultSet resultSet) throws SQLException {
        String priority = resultSet.getString("priority");
        String receiptState = resultSet.getString("receipt_state");
        return new MessageSummary(
                resultSet.getLong("id_message"),
                resultSet.getLong("id_channel"),
                nullableLong(resultSet, "id_user_sender"),
                resultSet.getString("sender_name"),
                resultSet.getString("sender_email"),
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
                MessageState.fromDatabaseValue(resultSet.getString("state")),
                nullableTimestamp(resultSet, "delivered_at"),
                nullableTimestamp(resultSet, "read_at"),
                receiptState == null ? null : MessageReceiptState.fromDatabaseValue(receiptState),
                resultSet.getString("channel_title")
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
}
