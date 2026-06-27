package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.ContentFormat;
import pt.isel.gape.learning.model.ContentItem;
import pt.isel.gape.learning.model.ContentItemCreateCommand;
import pt.isel.gape.learning.model.ContentItemState;
import pt.isel.gape.learning.model.ContentItemUpdateCommand;

public final class ContentItemDAO {

    private final ConnectionProvider connectionProvider;

    public ContentItemDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(
            Connection connection,
            long authorUserId,
            ContentItemCreateCommand command,
            LocalDateTime createdAt
    ) throws SQLException {
        String sql = """
                INSERT INTO content_item (
                    author_user_id, title, description, format, source, state, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, NULL)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, authorUserId);
            statement.setString(2, command.title().trim());
            setNullableString(statement, 3, command.description());
            statement.setString(4, command.format().toDatabaseValue());
            setNullableString(statement, 5, command.source());
            statement.setString(6, command.state().toDatabaseValue());
            statement.setTimestamp(7, Timestamp.valueOf(createdAt));
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating content item failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<ContentItem> findById(long contentItemId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, contentItemId);
        }
    }

    public Optional<ContentItem> findById(Connection connection, long contentItemId) throws SQLException {
        String sql = """
                SELECT id_content_item, author_user_id, title, description, format, source,
                       state, created_at, updated_at
                FROM content_item
                WHERE id_content_item = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contentItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapContentItem(resultSet));
            }
        }
    }

    public Optional<ContentItem> findFirstItemBySource(Connection connection, String source) throws SQLException {
        String sql = """
                SELECT id_content_item, author_user_id, title, description, format, source,
                       state, created_at, updated_at
                FROM content_item
                WHERE TRIM(source) = ?
                ORDER BY id_content_item
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, source == null ? "" : source.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapContentItem(resultSet));
            }
        }
    }

    public List<ContentItem> findReusableFileBackedItems(Connection connection) throws SQLException {
        String sql = """
                SELECT id_content_item, author_user_id, title, description, format, source,
                       state, created_at, updated_at
                FROM content_item
                WHERE state = 'active'
                  AND (
                        format IN ('pdf', 'text', 'image', 'video', 'audio')
                        OR (format = 'other' AND source LIKE 'assessment:%')
                  )
                  AND source IS NOT NULL
                  AND TRIM(source) <> ''
                  AND source NOT LIKE 'contents/pending/%'
                ORDER BY CASE format
                            WHEN 'pdf' THEN 0
                            WHEN 'text' THEN 1
                            WHEN 'image' THEN 2
                            WHEN 'video' THEN 3
                            WHEN 'audio' THEN 4
                            WHEN 'other' THEN 5
                            ELSE 6
                         END,
                         title,
                         id_content_item
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<ContentItem> items = new ArrayList<>();
            while (resultSet.next()) {
                items.add(mapContentItem(resultSet));
            }
            return items;
        }
    }

    public boolean hasActiveAssessmentRepositoryReference(long assessmentId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return hasActiveAssessmentRepositoryReference(connection, assessmentId);
        }
    }

    public boolean hasActiveAssessmentRepositoryReference(Connection connection, long assessmentId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM content_item
                WHERE state = 'active'
                  AND format = 'other'
                  AND TRIM(source) = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "assessment:" + assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public int ensureAssessmentRepositoryReferences(
            Connection connection,
            long authorUserId,
            LocalDateTime createdAt
    ) throws SQLException {
        String sql = """
                INSERT INTO content_item (
                    author_user_id, title, description, format, source, state, created_at, updated_at
                )
                SELECT COALESCE(
                           (SELECT MIN(ua.id_user) FROM user_account ua WHERE ua.id_user = ?),
                           (SELECT MIN(ap.id_user) FROM administrator_profile ap),
                           (SELECT MIN(ua.id_user) FROM user_account ua)
                       ),
                       a.title, a.description, 'other', CONCAT('assessment:', a.id_assessment),
                       CASE WHEN a.state = 'completed' THEN 'inactive' ELSE 'active' END,
                       ?, NULL
                FROM assessment a
                WHERE EXISTS (SELECT 1 FROM user_account)
                  AND NOT EXISTS (
                      SELECT 1
                      FROM content_item ci
                      WHERE TRIM(ci.source) = CONCAT('assessment:', a.id_assessment)
                  )
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, authorUserId);
            statement.setTimestamp(2, Timestamp.valueOf(createdAt));
            return statement.executeUpdate();
        }
    }

    public void update(Connection connection, long contentItemId, ContentItemUpdateCommand command, LocalDateTime updatedAt)
            throws SQLException {
        String sql = """
                UPDATE content_item
                SET title = ?, description = ?, format = ?, source = ?, state = ?, updated_at = ?
                WHERE id_content_item = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, command.title().trim());
            setNullableString(statement, 2, command.description());
            statement.setString(3, command.format().toDatabaseValue());
            setNullableString(statement, 4, command.source());
            statement.setString(5, command.state().toDatabaseValue());
            statement.setTimestamp(6, Timestamp.valueOf(updatedAt));
            statement.setLong(7, contentItemId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Content item not found: " + contentItemId);
            }
        }
    }

    public void updateState(
            Connection connection,
            long contentItemId,
            ContentItemState state,
            LocalDateTime updatedAt
    ) throws SQLException {
        String sql = "UPDATE content_item SET state = ?, updated_at = ? WHERE id_content_item = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            statement.setTimestamp(2, Timestamp.valueOf(updatedAt));
            statement.setLong(3, contentItemId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Content item not found: " + contentItemId);
            }
        }
    }

    public void updateSource(
            Connection connection,
            long contentItemId,
            String source,
            LocalDateTime updatedAt
    ) throws SQLException {
        String sql = "UPDATE content_item SET source = ?, updated_at = ? WHERE id_content_item = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableString(statement, 1, source);
            statement.setTimestamp(2, Timestamp.valueOf(updatedAt));
            statement.setLong(3, contentItemId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Content item not found: " + contentItemId);
            }
        }
    }

    public boolean hasSubmittedAssessmentAssociation(Connection connection, long contentItemId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM associate_assessment_content aac
                JOIN attempt a ON a.id_assessment = aac.id_assessment
                WHERE aac.id_content_item = ?
                  AND a.state IN ('submitted', 'corrected')
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contentItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean hasActiveMandatoryBlockAssociation(Connection connection, long contentItemId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM associate_block_content abc
                JOIN content_block cb ON cb.id_content_block = abc.id_content_block
                WHERE abc.id_content_item = ?
                  AND abc.mandatory = TRUE
                  AND cb.state = 'active'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contentItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public long countItemsBySourceExcluding(Connection connection, String source, long excludedContentItemId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM content_item
                WHERE TRIM(source) = ?
                  AND id_content_item <> ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, source.trim());
            statement.setLong(2, excludedContentItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public Optional<Long> findFirstItemIdBySourceExcluding(
            Connection connection,
            String source,
            long excludedContentItemId
    ) throws SQLException {
        String sql = """
                SELECT id_content_item
                FROM content_item
                WHERE TRIM(source) = ?
                  AND id_content_item <> ?
                ORDER BY id_content_item
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, source.trim());
            statement.setLong(2, excludedContentItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(resultSet.getLong("id_content_item"));
                }
                return Optional.empty();
            }
        }
    }

    public void reassignStoredFiles(Connection connection, long sourceContentItemId, long targetContentItemId)
            throws SQLException {
        String sql = """
                UPDATE content_file
                SET id_content_item = ?
                WHERE id_content_item = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, targetContentItemId);
            statement.setLong(2, sourceContentItemId);
            statement.executeUpdate();
        }
    }

    public List<String> findStoredRelativePathsForContentItem(Connection connection, long contentItemId)
            throws SQLException {
        String sql = """
                SELECT original_path, final_path, thumbnail_path
                FROM content_file
                WHERE id_content_item = ?
                """;

        Set<String> paths = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contentItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    addPath(paths, resultSet.getString("original_path"));
                    addPath(paths, resultSet.getString("final_path"));
                    addPath(paths, resultSet.getString("thumbnail_path"));
                }
            }
        }
        return List.copyOf(paths);
    }

    public Optional<String> findThumbnailPathByContentItemOrSource(
            Connection connection,
            long contentItemId,
            String source
    ) throws SQLException {
        String sql = """
                SELECT thumbnail_path
                FROM content_file
                WHERE thumbnail_path IS NOT NULL
                  AND thumbnail_path <> ''
                  AND (id_content_item = ? OR final_path = ?)
                ORDER BY CASE WHEN id_content_item = ? THEN 0 ELSE 1 END,
                         id_content_file
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contentItemId);
            statement.setString(2, source == null ? "" : source.trim());
            statement.setLong(3, contentItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String thumbnailPath = resultSet.getString("thumbnail_path");
                    if (thumbnailPath != null && !thumbnailPath.isBlank()) {
                        return Optional.of(thumbnailPath.trim());
                    }
                }
                return Optional.empty();
            }
        }
    }

    public void delete(Connection connection, long contentItemId) throws SQLException {
        String sql = "DELETE FROM content_item WHERE id_content_item = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contentItemId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Content item not found: " + contentItemId);
            }
        }
    }

    private static void addPath(Set<String> paths, String value) {
        if (value != null && !value.isBlank()) {
            paths.add(value.trim());
        }
    }

    private static ContentItem mapContentItem(ResultSet resultSet) throws SQLException {
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        return new ContentItem(
                resultSet.getLong("id_content_item"),
                resultSet.getLong("author_user_id"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                ContentFormat.fromDatabaseValue(resultSet.getString("format")),
                resultSet.getString("source"),
                ContentItemState.fromDatabaseValue(resultSet.getString("state")),
                createdAt == null ? null : createdAt.toLocalDateTime(),
                updatedAt == null ? null : updatedAt.toLocalDateTime()
        );
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }
}
