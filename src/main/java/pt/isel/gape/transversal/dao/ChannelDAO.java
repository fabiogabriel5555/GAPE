package pt.isel.gape.transversal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.transversal.model.Channel;
import pt.isel.gape.transversal.model.ChannelCreateCommand;
import pt.isel.gape.transversal.model.ChannelState;
import pt.isel.gape.transversal.model.ChannelType;
import pt.isel.gape.transversal.model.ChannelVisibility;

public final class ChannelDAO {

    private final ConnectionProvider connectionProvider;

    public ChannelDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, ChannelCreateCommand command, LocalDateTime createdAt)
            throws SQLException {
        String sql = """
                INSERT INTO channel (title, type, visibility, created_at, state)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, command.title().trim());
            statement.setString(2, command.type().toDatabaseValue());
            statement.setString(3, command.visibility().toDatabaseValue());
            statement.setTimestamp(4, Timestamp.valueOf(createdAt));
            statement.setString(5, command.state().toDatabaseValue());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating channel failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<Channel> findById(long channelId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, channelId);
        }
    }

    public Optional<Channel> findById(Connection connection, long channelId) throws SQLException {
        String sql = selectChannelSql() + " WHERE c.id_channel = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, channelId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapChannel(connection, resultSet));
            }
        }
    }

    public Optional<Channel> lockById(Connection connection, long channelId) throws SQLException {
        String sql = selectChannelSql() + " WHERE c.id_channel = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, channelId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapChannel(connection, resultSet));
            }
        }
    }

    public void replaceClassGroups(Connection connection, long channelId, Collection<Long> classGroupIds)
            throws SQLException {
        replaceAssociations(
                connection,
                "associate_channel_class_group",
                "id_channel",
                "id_class_group",
                channelId,
                classGroupIds
        );
    }

    public void replaceContentBlocks(Connection connection, long channelId, Collection<Long> contentBlockIds)
            throws SQLException {
        replaceAssociations(
                connection,
                "associate_channel_content_block",
                "id_channel",
                "id_content_block",
                channelId,
                contentBlockIds
        );
    }

    public void replaceAssessments(Connection connection, long channelId, Collection<Long> assessmentIds)
            throws SQLException {
        replaceAssociations(
                connection,
                "associate_channel_assessment",
                "id_channel",
                "id_assessment",
                channelId,
                assessmentIds
        );
    }

    public List<Long> findClassGroupIds(Connection connection, long channelId) throws SQLException {
        return findAssociationIds(
                connection,
                "associate_channel_class_group",
                "id_channel",
                "id_class_group",
                channelId
        );
    }

    public List<Long> findContentBlockIds(Connection connection, long channelId) throws SQLException {
        return findAssociationIds(
                connection,
                "associate_channel_content_block",
                "id_channel",
                "id_content_block",
                channelId
        );
    }

    public List<Long> findAssessmentIds(Connection connection, long channelId) throws SQLException {
        return findAssociationIds(
                connection,
                "associate_channel_assessment",
                "id_channel",
                "id_assessment",
                channelId
        );
    }

    public List<Long> findClassGroupIdsForContentBlocks(Connection connection, Collection<Long> contentBlockIds)
            throws SQLException {
        List<Long> normalized = orderedUnique(contentBlockIds);
        if (normalized.isEmpty()) {
            return List.of();
        }
        String placeholders = placeholders(normalized.size());
        String sql = """
                SELECT DISTINCT id_class_group
                FROM content_block
                WHERE id_content_block IN (%s)
                ORDER BY id_class_group
                """.formatted(placeholders);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindLongs(statement, normalized, 1);
            try (ResultSet resultSet = statement.executeQuery()) {
                return readLongList(resultSet, "id_class_group");
            }
        }
    }

    public List<Long> findClassGroupIdsForAssessments(Connection connection, Collection<Long> assessmentIds)
            throws SQLException {
        List<Long> normalized = orderedUnique(assessmentIds);
        if (normalized.isEmpty()) {
            return List.of();
        }
        String placeholders = placeholders(normalized.size());
        String sql = """
                SELECT DISTINCT class_group_id
                FROM (
                    SELECT cb.id_class_group AS class_group_id
                    FROM assessment a
                    JOIN content_block cb ON cb.id_content_block = a.id_content_block
                    WHERE a.id_assessment IN (%s)
                    UNION
                    SELECT acg.id_class_group AS class_group_id
                    FROM assessment_class_group acg
                    WHERE acg.id_assessment IN (%s)
                ) assessment_groups
                ORDER BY class_group_id
                """.formatted(placeholders, placeholders);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bindLongs(statement, normalized, 1);
            bindLongs(statement, normalized, index);
            try (ResultSet resultSet = statement.executeQuery()) {
                return readLongList(resultSet, "class_group_id");
            }
        }
    }

    public boolean allClassGroupsExist(Connection connection, Collection<Long> classGroupIds) throws SQLException {
        return countExisting(connection, "class_group", "id_class_group", classGroupIds) == orderedUnique(classGroupIds).size();
    }

    public boolean allContentBlocksExist(Connection connection, Collection<Long> contentBlockIds) throws SQLException {
        return countExisting(connection, "content_block", "id_content_block", contentBlockIds) == orderedUnique(contentBlockIds).size();
    }

    public boolean allAssessmentsExist(Connection connection, Collection<Long> assessmentIds) throws SQLException {
        return countExisting(connection, "assessment", "id_assessment", assessmentIds) == orderedUnique(assessmentIds).size();
    }

    public boolean userExistsAndActive(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM user_account
                WHERE id_user = ?
                  AND state = 'active'
                """)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean userHasStudentProfile(Connection connection, long userId) throws SQLException {
        return userHasProfile(connection, "student_profile", userId);
    }

    public boolean userHasTeacherProfile(Connection connection, long userId) throws SQLException {
        return userHasProfile(connection, "teacher_profile", userId);
    }

    public boolean userHasCoordinatorProfile(Connection connection, long userId) throws SQLException {
        return userHasProfile(connection, "coordinator_profile", userId);
    }

    public boolean userHasAdministratorProfile(Connection connection, long userId) throws SQLException {
        return userHasProfile(connection, "administrator_profile", userId);
    }

    public boolean hasCurrentStudentClassGroupAccess(Connection connection, long studentUserId, long classGroupId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM enroll_class_group ecg
                JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
                JOIN course c ON c.id_course = cg.id_course
                JOIN subject s ON s.id_subject = cg.id_subject
                JOIN user_account u ON u.id_user = ecg.id_student_user
                WHERE ecg.id_student_user = ?
                  AND ecg.id_class_group = ?
                  AND ecg.state = 'active'
                  AND cg.state = 'active'
                  AND c.state = 'active'
                  AND s.state = 'active'
                  AND u.state = 'active'
                """)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean canTeacherModerateClassGroup(Connection connection, long teacherUserId, long classGroupId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM teach_class_group tcg
                JOIN grant_teacher gt ON gt.id_teacher_user = tcg.id_teacher_user
                JOIN permission p ON p.cod_permission = gt.cod_permission
                JOIN user_account u ON u.id_user = tcg.id_teacher_user
                WHERE tcg.id_teacher_user = ?
                  AND tcg.id_class_group = ?
                  AND tcg.state = 'active'
                  AND gt.cod_permission = 'MANAGE_LEARNING'
                  AND p.state = 'active'
                  AND u.state = 'active'
                  AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                  AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                """)) {
            statement.setLong(1, teacherUserId);
            statement.setLong(2, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean canCoordinatorModerateClassGroup(Connection connection, long coordinatorUserId, long classGroupId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM class_group cg
                JOIN coordinate_subject cs ON cs.id_subject = cg.id_subject
                JOIN grant_coordinator gc ON gc.id_coordinator_user = cs.id_coordinator_user
                JOIN permission p ON p.cod_permission = gc.cod_permission
                JOIN user_account u ON u.id_user = cs.id_coordinator_user
                WHERE cs.id_coordinator_user = ?
                  AND cg.id_class_group = ?
                  AND cs.state = 'active'
                  AND gc.cod_permission = 'MANAGE_LEARNING'
                  AND p.state = 'active'
                  AND u.state = 'active'
                """)) {
            statement.setLong(1, coordinatorUserId);
            statement.setLong(2, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean canAdministratorModerateClassGroup(Connection connection, long administratorUserId, long classGroupId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM class_group cg
                JOIN course c ON c.id_course = cg.id_course
                JOIN subject s ON s.id_subject = cg.id_subject
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                JOIN grant_administrator ga ON ga.id_admin_user = ?
                JOIN permission p ON p.cod_permission = ga.cod_permission
                JOIN user_account u ON u.id_user = ga.id_admin_user
                WHERE cg.id_class_group = ?
                  AND p.state = 'active'
                  AND u.state = 'active'
                  AND (
                        (ga.cod_permission = 'MANAGE_ALL' AND ga.context_type = 'GLOBAL')
                        OR (
                            ga.cod_permission = 'MANAGE_LEARNING'
                            AND (
                                (ga.context_type = 'CLASS_GROUP' AND ga.context_id = cg.id_class_group)
                                OR (ga.context_type = 'SUBJECT' AND ga.context_id = cg.id_subject)
                                OR (ga.context_type = 'COURSE' AND ga.context_id = cg.id_course)
                                OR (ga.context_type = 'ORGANIZATION'
                                    AND ga.context_id IN (c.id_organization, s.id_organization))
                                OR (ga.context_type = 'ORGANIC_UNIT'
                                    AND (ga.context_id = c.id_organic_unit
                                         OR ga.context_id = ou.parent_organic_unit_id))
                            )
                        )
                  )
                """)) {
            statement.setLong(1, administratorUserId);
            statement.setLong(2, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    private static String selectChannelSql() {
        return """
                SELECT c.id_channel, c.title, c.type, c.visibility, c.created_at, c.state
                FROM channel c
                """;
    }

    private Channel mapChannel(Connection connection, ResultSet resultSet) throws SQLException {
        long channelId = resultSet.getLong("id_channel");
        return new Channel(
                channelId,
                resultSet.getString("title"),
                ChannelType.fromDatabaseValue(resultSet.getString("type")),
                ChannelVisibility.fromDatabaseValue(resultSet.getString("visibility")),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                ChannelState.fromDatabaseValue(resultSet.getString("state")),
                findClassGroupIds(connection, channelId),
                findContentBlockIds(connection, channelId),
                findAssessmentIds(connection, channelId)
        );
    }

    private static void replaceAssociations(
            Connection connection,
            String table,
            String ownerColumn,
            String childColumn,
            long ownerId,
            Collection<Long> childIds
    ) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("""
                DELETE FROM %s
                WHERE %s = ?
                """.formatted(table, ownerColumn))) {
            delete.setLong(1, ownerId);
            delete.executeUpdate();
        }
        List<Long> normalized = orderedUnique(childIds);
        if (normalized.isEmpty()) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO %s (%s, %s)
                VALUES (?, ?)
                """.formatted(table, ownerColumn, childColumn))) {
            for (Long childId : normalized) {
                insert.setLong(1, ownerId);
                insert.setLong(2, childId);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private static List<Long> findAssociationIds(
            Connection connection,
            String table,
            String ownerColumn,
            String childColumn,
            long ownerId
    ) throws SQLException {
        String sql = """
                SELECT %s
                FROM %s
                WHERE %s = ?
                ORDER BY %s
                """.formatted(childColumn, table, ownerColumn, childColumn);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ownerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return readLongList(resultSet, childColumn);
            }
        }
    }

    private static long countExisting(
            Connection connection,
            String table,
            String idColumn,
            Collection<Long> ids
    ) throws SQLException {
        List<Long> normalized = orderedUnique(ids);
        if (normalized.isEmpty()) {
            return 0;
        }
        String sql = """
                SELECT COUNT(*)
                FROM %s
                WHERE %s IN (%s)
                """.formatted(table, idColumn, placeholders(normalized.size()));
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindLongs(statement, normalized, 1);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static boolean userHasProfile(Connection connection, String table, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM %s
                WHERE id_user = ?
                """.formatted(table))) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    private static List<Long> readLongList(ResultSet resultSet, String column) throws SQLException {
        List<Long> ids = new ArrayList<>();
        while (resultSet.next()) {
            ids.add(resultSet.getLong(column));
        }
        return ids;
    }

    private static int bindLongs(PreparedStatement statement, List<Long> values, int startIndex)
            throws SQLException {
        int index = startIndex;
        for (Long value : values) {
            statement.setLong(index++, value);
        }
        return index;
    }

    private static String placeholders(int count) {
        return String.join(",", Collections.nCopies(count, "?"));
    }

    public static List<Long> orderedUnique(Collection<Long> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long value : values) {
            if (value == null || value <= 0) {
                throw new IllegalArgumentException("Ids must be positive");
            }
            unique.add(value);
        }
        return List.copyOf(unique);
    }
}
