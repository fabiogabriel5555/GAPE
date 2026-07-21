package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Allocates the identifier of a pedagogical item from the same namespace
 * used by the pedagogical items already present in its content block.
 *
 * <p>The block row is locked for the duration of the caller's transaction so
 * two concurrent creations in one block cannot receive the same next ID.</p>
 */
public final class PedagogicalItemIdAllocator {

    public enum ItemType {
        CONTENT("content_item"),
        LESSON("lesson"),
        ASSESSMENT("assessment");

        private final String table;

        ItemType(String table) {
            this.table = table;
        }
    }

    public long nextId(Connection connection, long contentBlockId, ItemType itemType) throws SQLException {
        if (contentBlockId <= 0) {
            throw new IllegalArgumentException("contentBlockId must be positive");
        }
        if (itemType == null) {
            throw new IllegalArgumentException("itemType is required");
        }

        lockContentBlock(connection, contentBlockId);
        long candidate = maxPedagogicalId(connection, contentBlockId) + 1;
        while (idExists(connection, itemType, candidate)) {
            candidate++;
        }
        return candidate;
    }

    private static void lockContentBlock(Connection connection, long contentBlockId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id_content_block FROM content_block WHERE id_content_block = ? FOR UPDATE")) {
            statement.setLong(1, contentBlockId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Content block not found: " + contentBlockId);
                }
            }
        }
    }

    private static long maxPedagogicalId(Connection connection, long contentBlockId) throws SQLException {
        String sql = """
                SELECT GREATEST(
                    COALESCE((SELECT MAX(id_content_item) FROM associate_block_content WHERE id_content_block = ?), 0),
                    COALESCE((SELECT MAX(id_lesson) FROM lesson WHERE id_content_block = ?), 0),
                    COALESCE((SELECT MAX(id_assessment) FROM assessment WHERE id_content_block = ?), 0)
                )
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contentBlockId);
            statement.setLong(2, contentBlockId);
            statement.setLong(3, contentBlockId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static boolean idExists(Connection connection, ItemType itemType, long candidate) throws SQLException {
        String column = switch (itemType) {
            case CONTENT -> "id_content_item";
            case LESSON -> "id_lesson";
            case ASSESSMENT -> "id_assessment";
        };
        String sql = "SELECT 1 FROM " + itemType.table + " WHERE " + column + " = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, candidate);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
