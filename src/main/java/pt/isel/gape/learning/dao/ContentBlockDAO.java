package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.ContentBlockCreateCommand;
import pt.isel.gape.learning.model.ContentBlockState;
import pt.isel.gape.learning.model.ContentBlockUpdateCommand;

public final class ContentBlockDAO implements pt.isel.gape.transversal.service.ApplicationReadService.ContentBlocks {

    private final ConnectionProvider connectionProvider;

    public ContentBlockDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, ContentBlockCreateCommand command) throws SQLException {
        String sql = """
                INSERT INTO content_block (
                    id_class_group, cod_content_block, name, description, order_no,
                    state
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, command.classGroupId());
            statement.setString(2, command.code().trim());
            statement.setString(3, command.name().trim());
            setNullableString(statement, 4, command.description());
            statement.setInt(5, command.orderNo());
            statement.setString(6, command.state().toDatabaseValue());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating content block failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<ContentBlock> findById(long contentBlockId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, contentBlockId);
        }
    }

    public Optional<ContentBlock> findById(Connection connection, long contentBlockId) throws SQLException {
        String sql = """
                SELECT id_content_block, id_class_group, cod_content_block, name, description,
                       order_no, state, created_at, updated_at
                FROM content_block
                WHERE id_content_block = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contentBlockId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapContentBlock(resultSet));
            }
        }
    }

    public List<ContentBlock> findByClassGroup(long classGroupId) throws SQLException {
        String sql = """
                SELECT id_content_block, id_class_group, cod_content_block, name, description,
                       order_no, state, created_at, updated_at
                FROM content_block
                WHERE id_class_group = ?
                ORDER BY order_no, name
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ContentBlock> blocks = new ArrayList<>();
                while (resultSet.next()) {
                    blocks.add(mapContentBlock(resultSet));
                }
                return blocks;
            }
        }
    }

    public List<ContentBlock> findByClassGroup(Connection connection, long classGroupId, boolean lock)
            throws SQLException {
        String sql = """
                SELECT id_content_block, id_class_group, cod_content_block, name, description,
                       order_no, state, created_at, updated_at
                FROM content_block
                WHERE id_class_group = ?
                ORDER BY order_no, name
                """ + (lock ? " FOR UPDATE" : "");

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ContentBlock> blocks = new ArrayList<>();
                while (resultSet.next()) {
                    blocks.add(mapContentBlock(resultSet));
                }
                return blocks;
            }
        }
    }

    public Map<Long, Integer> countByClassGroupIds(Collection<Long> classGroupIds) throws SQLException {
        if (classGroupIds == null || classGroupIds.isEmpty()) {
            return Map.of();
        }
        String sql = """
                SELECT id_class_group, COUNT(*) AS block_count
                FROM content_block
                WHERE id_class_group IN (%s)
                GROUP BY id_class_group
                """.formatted(placeholders(classGroupIds.size()));

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Long classGroupId : classGroupIds) {
                statement.setLong(index++, classGroupId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Long, Integer> counts = new LinkedHashMap<>();
                while (resultSet.next()) {
                    counts.put(resultSet.getLong("id_class_group"), resultSet.getInt("block_count"));
                }
                return counts;
            }
        }
    }

    public void update(Connection connection, long contentBlockId, ContentBlockUpdateCommand command)
            throws SQLException {
        String sql = """
                UPDATE content_block
                SET cod_content_block = ?, name = ?, description = ?, order_no = ?,
                    state = ?
                WHERE id_content_block = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, command.code().trim());
            statement.setString(2, command.name().trim());
            setNullableString(statement, 3, command.description());
            statement.setInt(4, command.orderNo());
            statement.setString(5, command.state().toDatabaseValue());
            statement.setLong(6, contentBlockId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Content block not found: " + contentBlockId);
            }
        }
    }

    public void reorderWithinClassGroup(Connection connection, long classGroupId, List<Long> orderedBlockIds)
            throws SQLException {
        String offsetSql = """
                UPDATE content_block
                SET order_no = order_no + 10000
                WHERE id_class_group = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(offsetSql)) {
            statement.setLong(1, classGroupId);
            statement.executeUpdate();
        }

        String updateSql = """
                UPDATE content_block
                SET order_no = ?
                WHERE id_class_group = ?
                  AND id_content_block = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
            int order = 1;
            for (Long blockId : orderedBlockIds) {
                statement.setInt(1, order++);
                statement.setLong(2, classGroupId);
                statement.setLong(3, blockId);
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("Content block not found in class group: " + blockId);
                }
            }
        }
    }

    public boolean orderExists(
            Connection connection,
            long classGroupId,
            int orderNo,
            Long excludedContentBlockId
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM content_block
                WHERE id_class_group = ?
                  AND order_no = ?
                  AND (? IS NULL OR id_content_block <> ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            statement.setInt(2, orderNo);
            if (excludedContentBlockId == null) {
                statement.setNull(3, java.sql.Types.BIGINT);
                statement.setNull(4, java.sql.Types.BIGINT);
            } else {
                statement.setLong(3, excludedContentBlockId);
                statement.setLong(4, excludedContentBlockId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean hasDomainDependencies(Connection connection, long contentBlockId) throws SQLException {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM lesson WHERE id_content_block = ?)
                  + (SELECT COUNT(*) FROM assessment WHERE id_content_block = ?)
                  + (SELECT COUNT(*) FROM associate_block_content WHERE id_content_block = ?)
                  + (SELECT COUNT(*) FROM associate_channel_content_block WHERE id_content_block = ?)
                  AS dependency_count
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contentBlockId);
            statement.setLong(2, contentBlockId);
            statement.setLong(3, contentBlockId);
            statement.setLong(4, contentBlockId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("dependency_count") > 0;
            }
        }
    }

    public void delete(Connection connection, long contentBlockId) throws SQLException {
        String sql = "DELETE FROM content_block WHERE id_content_block = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contentBlockId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Content block not found: " + contentBlockId);
            }
        }
    }

    private static ContentBlock mapContentBlock(ResultSet resultSet) throws SQLException {
        return new ContentBlock(
                resultSet.getLong("id_content_block"),
                resultSet.getLong("id_class_group"),
                resultSet.getString("cod_content_block"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getInt("order_no"),
                ContentBlockState.fromDatabaseValue(resultSet.getString("state")),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at") == null
                        ? null
                        : resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }

    private static String placeholders(int count) {
        return String.join(", ", java.util.Collections.nCopies(count, "?"));
    }
}
