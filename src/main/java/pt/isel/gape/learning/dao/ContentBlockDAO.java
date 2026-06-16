package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.ContentBlockAccessMode;
import pt.isel.gape.learning.model.ContentBlockCreateCommand;
import pt.isel.gape.learning.model.ContentBlockState;
import pt.isel.gape.learning.model.ContentBlockUpdateCommand;

public final class ContentBlockDAO {

    private final ConnectionProvider connectionProvider;

    public ContentBlockDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, ContentBlockCreateCommand command) throws SQLException {
        String sql = """
                INSERT INTO content_block (
                    id_class_group, cod_content_block, name, description, order_no,
                    access_mode, state, available_from, available_until
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, command.classGroupId());
            statement.setString(2, command.code().trim());
            statement.setString(3, command.name().trim());
            setNullableString(statement, 4, command.description());
            statement.setInt(5, command.orderNo());
            statement.setString(6, command.accessMode().toDatabaseValue());
            statement.setString(7, command.state().toDatabaseValue());
            setTimestamp(statement, 8, command.availableFrom());
            setTimestamp(statement, 9, command.availableUntil());
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
                       order_no, access_mode, state, available_from, available_until
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
                       order_no, access_mode, state, available_from, available_until
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

    public void update(Connection connection, long contentBlockId, ContentBlockUpdateCommand command)
            throws SQLException {
        String sql = """
                UPDATE content_block
                SET cod_content_block = ?, name = ?, description = ?, order_no = ?,
                    access_mode = ?, state = ?, available_from = ?, available_until = ?
                WHERE id_content_block = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, command.code().trim());
            statement.setString(2, command.name().trim());
            setNullableString(statement, 3, command.description());
            statement.setInt(4, command.orderNo());
            statement.setString(5, command.accessMode().toDatabaseValue());
            statement.setString(6, command.state().toDatabaseValue());
            setTimestamp(statement, 7, command.availableFrom());
            setTimestamp(statement, 8, command.availableUntil());
            statement.setLong(9, contentBlockId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Content block not found: " + contentBlockId);
            }
        }
    }

    public void updateState(Connection connection, long contentBlockId, ContentBlockState state) throws SQLException {
        String sql = "UPDATE content_block SET state = ? WHERE id_content_block = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            statement.setLong(2, contentBlockId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Content block not found: " + contentBlockId);
            }
        }
    }

    public boolean activeOrderExists(
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
                  AND state = 'active'
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
        Timestamp availableFrom = resultSet.getTimestamp("available_from");
        Timestamp availableUntil = resultSet.getTimestamp("available_until");
        return new ContentBlock(
                resultSet.getLong("id_content_block"),
                resultSet.getLong("id_class_group"),
                resultSet.getString("cod_content_block"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getInt("order_no"),
                ContentBlockAccessMode.fromDatabaseValue(resultSet.getString("access_mode")),
                ContentBlockState.fromDatabaseValue(resultSet.getString("state")),
                availableFrom == null ? null : availableFrom.toLocalDateTime(),
                availableUntil == null ? null : availableUntil.toLocalDateTime()
        );
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }

    private static void setTimestamp(PreparedStatement statement, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }
}
