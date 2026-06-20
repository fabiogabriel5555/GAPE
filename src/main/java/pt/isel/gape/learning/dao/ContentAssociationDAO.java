package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.BlockContentItem;
import pt.isel.gape.learning.model.ContentAssociation;
import pt.isel.gape.learning.model.ContentAssociationCommand;
import pt.isel.gape.learning.model.ContentAssociationType;
import pt.isel.gape.learning.model.ContentContext;
import pt.isel.gape.learning.model.ContentItem;
import pt.isel.gape.learning.model.ContentFormat;
import pt.isel.gape.learning.model.ContentItemState;

public final class ContentAssociationDAO {

    private final ConnectionProvider connectionProvider;

    public ContentAssociationDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public void associate(Connection connection, long contentItemId, ContentAssociationCommand command)
            throws SQLException {
        switch (command.type()) {
            case ORGANIZATION -> executeSimpleAssociation(
                    connection,
                    "associate_organization_content",
                    "id_organization",
                    command.targetId(),
                    contentItemId,
                    command.role()
            );
            case ORGANIC_UNIT -> executeSimpleAssociation(
                    connection,
                    "associate_organic_unit_content",
                    "id_organic_unit",
                    command.targetId(),
                    contentItemId,
                    command.role()
            );
            case COURSE -> executeSimpleAssociation(
                    connection,
                    "associate_course_content",
                    "id_course",
                    command.targetId(),
                    contentItemId,
                    command.role()
            );
            case SUBJECT -> executeSimpleAssociation(
                    connection,
                    "associate_subject_content",
                    "id_subject",
                    command.targetId(),
                    contentItemId,
                    command.role()
            );
            case CLASS_GROUP -> executeSimpleAssociation(
                    connection,
                    "associate_class_group_content",
                    "id_class_group",
                    command.targetId(),
                    contentItemId,
                    command.role()
            );
            case CONTENT_BLOCK -> executeBlockAssociation(connection, contentItemId, command);
            case ASSESSMENT -> executeSimpleAssociation(
                    connection,
                    "associate_assessment_content",
                    "id_assessment",
                    command.targetId(),
                    contentItemId,
                    command.role()
            );
        }
    }

    public void remove(
            Connection connection,
            ContentAssociationType type,
            long targetId,
            long contentItemId
    ) throws SQLException {
        AssociationTable table = AssociationTable.forType(type);
        String sql = """
                DELETE FROM %s
                WHERE %s = ?
                  AND id_content_item = ?
                """.formatted(table.tableName(), table.targetColumnName());

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, targetId);
            statement.setLong(2, contentItemId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Content association not found");
            }
        }
    }

    public Optional<ContentAssociation> findAssociation(
            Connection connection,
            ContentAssociationType type,
            long targetId,
            long contentItemId
    ) throws SQLException {
        AssociationTable table = AssociationTable.forType(type);
        String select = type == ContentAssociationType.CONTENT_BLOCK
                ? "role, order_no, mandatory"
                : "role, NULL AS order_no, FALSE AS mandatory";
        String sql = """
                SELECT %s
                FROM %s
                WHERE %s = ?
                  AND id_content_item = ?
                """.formatted(select, table.tableName(), table.targetColumnName());

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, targetId);
            statement.setLong(2, contentItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                int orderNo = resultSet.getInt("order_no");
                boolean orderWasNull = resultSet.wasNull();
                return Optional.of(new ContentAssociation(
                        type,
                        targetId,
                        contentItemId,
                        resultSet.getString("role"),
                        orderWasNull ? null : orderNo,
                        resultSet.getBoolean("mandatory")
                ));
            }
        }
    }

    public List<ContentItem> findContentItemsByContext(
            Connection connection,
            ContentAssociationType type,
            long targetId
    ) throws SQLException {
        AssociationTable table = AssociationTable.forType(type);
        String sql = """
                SELECT ci.id_content_item, ci.author_user_id, ci.title, ci.description,
                       ci.format, ci.source, ci.state, ci.created_at, ci.updated_at
                FROM %s assoc
                JOIN content_item ci ON ci.id_content_item = assoc.id_content_item
                WHERE assoc.%s = ?
                ORDER BY ci.title, ci.id_content_item
                """.formatted(table.tableName(), table.targetColumnName());

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, targetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ContentItem> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(mapContentItem(resultSet));
                }
                return items;
            }
        }
    }

    public List<BlockContentItem> findBlockContentItems(Connection connection, long contentBlockId)
            throws SQLException {
        String sql = """
                SELECT ci.id_content_item, ci.author_user_id, ci.title, ci.description,
                       ci.format, ci.source, ci.state, ci.created_at, ci.updated_at,
                       assoc.role, assoc.order_no, assoc.mandatory,
                       (
                           SELECT cf.thumbnail_path
                           FROM content_file cf
                           WHERE cf.thumbnail_path IS NOT NULL
                             AND cf.thumbnail_path <> ''
                             AND (cf.id_content_item = ci.id_content_item OR cf.final_path = ci.source)
                           ORDER BY CASE WHEN cf.id_content_item = ci.id_content_item THEN 0 ELSE 1 END,
                                    cf.id_content_file
                           LIMIT 1
                       ) AS thumbnail_path
                FROM associate_block_content assoc
                JOIN content_item ci ON ci.id_content_item = assoc.id_content_item
                WHERE assoc.id_content_block = ?
                ORDER BY ci.created_at ASC,
                         ci.id_content_item ASC
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contentBlockId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<BlockContentItem> items = new ArrayList<>();
                while (resultSet.next()) {
                    int orderNo = resultSet.getInt("order_no");
                    boolean orderWasNull = resultSet.wasNull();
                    items.add(new BlockContentItem(
                            mapContentItem(resultSet),
                            resultSet.getString("role"),
                            orderWasNull ? null : orderNo,
                            resultSet.getBoolean("mandatory"),
                            resultSet.getString("thumbnail_path")
                    ));
                }
                return items;
            }
        }
    }

    public Optional<Boolean> findFirstBlockMandatory(Connection connection, long contentItemId)
            throws SQLException {
        String sql = """
                SELECT mandatory
                FROM associate_block_content
                WHERE id_content_item = ?
                ORDER BY id_content_block
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contentItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(resultSet.getBoolean("mandatory"));
            }
        }
    }

    public Optional<ContentContext> findContext(
            Connection connection,
            ContentAssociationType type,
            long targetId
    ) throws SQLException {
        return switch (type) {
            case ORGANIZATION -> findSingleContext(connection, """
                    SELECT 'organization' AS context_type, o.id_organization AS target_id,
                           o.id_organization, NULL AS id_organic_unit, NULL AS id_course,
                           NULL AS id_subject, NULL AS id_class_group, NULL AS id_content_block,
                           NULL AS id_assessment, o.state
                    FROM organization o
                    WHERE o.id_organization = ?
                    """, type, targetId);
            case ORGANIC_UNIT -> findSingleContext(connection, """
                    SELECT 'organic_unit' AS context_type, ou.id_organic_unit AS target_id,
                           ou.id_organization, ou.id_organic_unit, NULL AS id_course,
                           NULL AS id_subject, NULL AS id_class_group, NULL AS id_content_block,
                           NULL AS id_assessment, ou.state
                    FROM organic_unit ou
                    WHERE ou.id_organic_unit = ?
                    """, type, targetId);
            case COURSE -> findSingleContext(connection, """
                    SELECT 'course' AS context_type, c.id_course AS target_id,
                           c.id_organization, c.id_organic_unit, c.id_course,
                           NULL AS id_subject, NULL AS id_class_group, NULL AS id_content_block,
                           NULL AS id_assessment, c.state
                    FROM course c
                    WHERE c.id_course = ?
                    """, type, targetId);
            case SUBJECT -> findSingleContext(connection, """
                    SELECT 'subject' AS context_type, s.id_subject AS target_id,
                           s.id_organization, NULL AS id_organic_unit, NULL AS id_course,
                           s.id_subject, NULL AS id_class_group, NULL AS id_content_block,
                           NULL AS id_assessment, s.state
                    FROM subject s
                    WHERE s.id_subject = ?
                    """, type, targetId);
            case CLASS_GROUP -> findSingleContext(connection, """
                    SELECT 'class_group' AS context_type, cg.id_class_group AS target_id,
                           c.id_organization, c.id_organic_unit, cg.id_course,
                           cg.id_subject, cg.id_class_group, NULL AS id_content_block,
                           NULL AS id_assessment, cg.state
                    FROM class_group cg
                    JOIN course c ON c.id_course = cg.id_course
                    WHERE cg.id_class_group = ?
                    """, type, targetId);
            case CONTENT_BLOCK -> findSingleContext(connection, """
                    SELECT 'content_block' AS context_type, cb.id_content_block AS target_id,
                           c.id_organization, c.id_organic_unit, cg.id_course,
                           cg.id_subject, cg.id_class_group, cb.id_content_block,
                           NULL AS id_assessment, cb.state
                    FROM content_block cb
                    JOIN class_group cg ON cg.id_class_group = cb.id_class_group
                    JOIN course c ON c.id_course = cg.id_course
                    WHERE cb.id_content_block = ?
                    """, type, targetId);
            case ASSESSMENT -> findSingleContext(connection, """
                    SELECT 'assessment' AS context_type, a.id_assessment AS target_id,
                           COALESCE(c.id_organization, s.id_organization) AS id_organization,
                           c.id_organic_unit,
                           cg.id_course,
                           COALESCE(a.id_subject, cg.id_subject) AS id_subject,
                           cb.id_class_group,
                           a.id_content_block,
                           a.id_assessment,
                           a.state
                    FROM assessment a
                    LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
                    LEFT JOIN class_group cg ON cg.id_class_group = cb.id_class_group
                    LEFT JOIN course c ON c.id_course = cg.id_course
                    LEFT JOIN subject s ON s.id_subject = a.id_subject
                    WHERE a.id_assessment = ?
                    """, type, targetId);
        };
    }

    public List<ContentContext> findContextsForContent(Connection connection, long contentItemId)
            throws SQLException {
        List<ContentContext> contexts = new ArrayList<>();
        for (ContentAssociationType type : ContentAssociationType.values()) {
            AssociationTable table = AssociationTable.forType(type);
            String sql = """
                    SELECT %s AS target_id
                    FROM %s
                    WHERE id_content_item = ?
                    """.formatted(table.targetColumnName(), table.tableName());
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, contentItemId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        findContext(connection, type, resultSet.getLong("target_id")).ifPresent(contexts::add);
                    }
                }
            }
        }
        return List.copyOf(contexts);
    }

    public boolean hasActiveStudentAccess(Connection connection, long userId, ContentContext context)
            throws SQLException {
        if (context.classGroupId() != null) {
            return exists(connection, """
                    SELECT COUNT(*)
                    FROM enroll_class_group
                    WHERE id_student_user = ?
                      AND id_class_group = ?
                      AND state = 'active'
                    """, userId, context.classGroupId());
        }
        if (context.courseId() != null && context.subjectId() != null) {
            return exists(connection, """
                    SELECT COUNT(*)
                    FROM enroll_subject
                    WHERE id_student_user = ?
                      AND id_course = ?
                      AND id_subject = ?
                      AND state = 'active'
                    """, userId, context.courseId(), context.subjectId());
        }
        if (context.courseId() != null) {
            return exists(connection, """
                    SELECT COUNT(*)
                    FROM enroll_course
                    WHERE id_student_user = ?
                      AND id_course = ?
                      AND state = 'active'
                    """, userId, context.courseId());
        }
        if (context.subjectId() != null) {
            return exists(connection, """
                    SELECT COUNT(*)
                    FROM enroll_subject
                    WHERE id_student_user = ?
                      AND id_subject = ?
                      AND state = 'active'
                    """, userId, context.subjectId());
        }
        return false;
    }

    public boolean courseIntegratesSubject(Connection connection, long courseId, long subjectId) throws SQLException {
        return exists(connection, """
                SELECT COUNT(*)
                FROM integrate_subject
                WHERE id_course = ?
                  AND id_subject = ?
                  AND state <> 'archived'
                """, courseId, subjectId);
    }

    private static void executeSimpleAssociation(
            Connection connection,
            String tableName,
            String targetColumnName,
            long targetId,
            long contentItemId,
            String role
    ) throws SQLException {
        String sql = """
                INSERT INTO %s (%s, id_content_item, role)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE role = VALUES(role)
                """.formatted(tableName, targetColumnName);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, targetId);
            statement.setLong(2, contentItemId);
            statement.setString(3, role.trim());
            statement.executeUpdate();
        }
    }

    private static void executeBlockAssociation(
            Connection connection,
            long contentItemId,
            ContentAssociationCommand command
    ) throws SQLException {
        String sql = """
                INSERT INTO associate_block_content (
                    id_content_block, id_content_item, order_no, role, mandatory
                ) VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    order_no = VALUES(order_no),
                    role = VALUES(role),
                    mandatory = VALUES(mandatory)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, command.targetId());
            statement.setLong(2, contentItemId);
            if (command.orderNo() == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, command.orderNo());
            }
            statement.setString(4, command.role().trim());
            statement.setBoolean(5, command.mandatory());
            statement.executeUpdate();
        }
    }

    private static Optional<ContentContext> findSingleContext(
            Connection connection,
            String sql,
            ContentAssociationType expectedType,
            long targetId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, targetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapContext(resultSet, expectedType));
            }
        }
    }

    private static ContentContext mapContext(ResultSet resultSet, ContentAssociationType expectedType)
            throws SQLException {
        return new ContentContext(
                expectedType,
                resultSet.getLong("target_id"),
                nullableLong(resultSet, "id_organization"),
                nullableLong(resultSet, "id_organic_unit"),
                nullableLong(resultSet, "id_course"),
                nullableLong(resultSet, "id_subject"),
                nullableLong(resultSet, "id_class_group"),
                nullableLong(resultSet, "id_content_block"),
                nullableLong(resultSet, "id_assessment"),
                resultSet.getString("state")
        );
    }

    private static ContentItem mapContentItem(ResultSet resultSet) throws SQLException {
        java.sql.Timestamp createdAt = resultSet.getTimestamp("created_at");
        java.sql.Timestamp updatedAt = resultSet.getTimestamp("updated_at");
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

    private static boolean exists(Connection connection, String sql, long firstId, long secondId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, firstId);
            statement.setLong(2, secondId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    private static boolean exists(Connection connection, String sql, long firstId, long secondId, long thirdId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, firstId);
            statement.setLong(2, secondId);
            statement.setLong(3, thirdId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    private static Long nullableLong(ResultSet resultSet, String columnName) throws SQLException {
        long value = resultSet.getLong(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private record AssociationTable(String tableName, String targetColumnName) {
        private static AssociationTable forType(ContentAssociationType type) {
            return switch (type) {
                case ORGANIZATION -> new AssociationTable("associate_organization_content", "id_organization");
                case ORGANIC_UNIT -> new AssociationTable("associate_organic_unit_content", "id_organic_unit");
                case COURSE -> new AssociationTable("associate_course_content", "id_course");
                case SUBJECT -> new AssociationTable("associate_subject_content", "id_subject");
                case CLASS_GROUP -> new AssociationTable("associate_class_group_content", "id_class_group");
                case CONTENT_BLOCK -> new AssociationTable("associate_block_content", "id_content_block");
                case ASSESSMENT -> new AssociationTable("associate_assessment_content", "id_assessment");
            };
        }
    }
}
