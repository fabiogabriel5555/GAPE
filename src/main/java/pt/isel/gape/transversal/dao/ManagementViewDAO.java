package pt.isel.gape.transversal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.transversal.model.ManagementView;
import pt.isel.gape.transversal.model.ManagementViewScopeTargetType;
import pt.isel.gape.transversal.model.ManagementViewState;
import pt.isel.gape.transversal.model.ManagementViewType;
import pt.isel.gape.transversal.model.ManagementViewUpdateCommand;
import pt.isel.gape.transversal.model.ManagementViewScope;

/** JDBC persistence for configurable management views and their explicit grants. */
public final class ManagementViewDAO {

    private final ConnectionProvider connectionProvider;

    public ManagementViewDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public void update(
            Connection connection,
            long viewId,
            ManagementViewUpdateCommand command,
            ManagementViewScopeTargetType scopeTargetType,
            Long scopeTargetId,
            long ownerUserId
    ) throws SQLException {
        String sql = """
                UPDATE management_view
                SET title = ?, type = ?, description = ?, visibility_scope = ?,
                    scope_target_type = ?, scope_target_id = ?, owner_user_id = ?, state = ?
                WHERE id_management_view = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, command.title().trim());
            statement.setString(2, command.type().toDatabaseValue());
            setNullableString(statement, 3, normalizeOptional(command.description()));
            statement.setString(4, command.visibilityScope().toDatabaseValue());
            setNullableString(statement, 5,
                    scopeTargetType == null ? null : scopeTargetType.toDatabaseValue());
            setNullableLong(statement, 6, scopeTargetId);
            statement.setLong(7, ownerUserId);
            statement.setString(8, command.state().toDatabaseValue());
            statement.setLong(9, viewId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Management view not found: " + viewId);
            }
        }
    }

    public Optional<ManagementView> findById(long viewId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, viewId);
        }
    }

    public Optional<ManagementView> findById(Connection connection, long viewId) throws SQLException {
        String sql = selectSql() + " WHERE id_management_view = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, viewId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapManagementView(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<ManagementView> lockById(Connection connection, long viewId) throws SQLException {
        String sql = selectSql() + " WHERE id_management_view = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, viewId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapManagementView(resultSet)) : Optional.empty();
            }
        }
    }

    public List<ManagementView> findAll() throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findAll(connection);
        }
    }

    public List<ManagementView> findAll(Connection connection) throws SQLException {
        String sql = selectSql() + " ORDER BY id_management_view";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapManagementViews(resultSet);
        }
    }

    public List<ManagementView> findByOwnerUserId(long ownerUserId) throws SQLException {
        String sql = selectSql() + " WHERE owner_user_id = ? ORDER BY id_management_view";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ownerUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapManagementViews(resultSet);
            }
        }
    }

    public List<ManagementView> findActiveExplicitlyGrantedTo(long userId) throws SQLException {
        String sql = """
                SELECT mv.id_management_view, mv.title, mv.type, mv.description, mv.visibility_scope,
                       mv.scope_target_type, mv.scope_target_id, mv.owner_user_id, mv.state
                FROM management_view mv
                JOIN access_management_view amv ON amv.id_management_view = mv.id_management_view
                WHERE amv.id_user = ?
                  AND mv.state = ?
                ORDER BY mv.id_management_view
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, ManagementViewState.ACTIVE.toDatabaseValue());
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapManagementViews(resultSet);
            }
        }
    }

    /** Explicit access is additive and must never be treated as scope authorization. */
    public boolean hasExplicitAccess(long userId, long viewId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return hasExplicitAccess(connection, userId, viewId);
        }
    }

    public boolean hasExplicitAccess(Connection connection, long userId, long viewId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM access_management_view
                WHERE id_user = ?
                  AND id_management_view = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, viewId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    /**
     * Returns the current distribution list for a panel.  Authorization is
     * deliberately enforced by {@code ManagementViewService}; this DAO only
     * exposes the persisted relation.
     */
    public List<Long> findExplicitRecipientUserIds(long viewId) throws SQLException {
        String sql = """
                SELECT id_user
                FROM access_management_view
                WHERE id_management_view = ?
                ORDER BY id_user
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, viewId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Long> userIds = new ArrayList<>();
                while (resultSet.next()) {
                    userIds.add(resultSet.getLong("id_user"));
                }
                return List.copyOf(userIds);
            }
        }
    }

    public void grantExplicitAccess(Connection connection, long userId, long viewId) throws SQLException {
        String sql = """
                INSERT INTO access_management_view (id_user, id_management_view)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE id_user = VALUES(id_user)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, viewId);
            statement.executeUpdate();
        }
    }

    public boolean revokeExplicitAccess(Connection connection, long userId, long viewId) throws SQLException {
        String sql = """
                DELETE FROM access_management_view
                WHERE id_user = ?
                  AND id_management_view = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, viewId);
            return statement.executeUpdate() > 0;
        }
    }

    public void replaceExplicitAccess(Connection connection, long viewId, Collection<Long> userIds) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("""
                DELETE FROM access_management_view
                WHERE id_management_view = ?
                """)) {
            delete.setLong(1, viewId);
            delete.executeUpdate();
        }
        Set<Long> normalizedIds = normalizeIds(userIds);
        if (normalizedIds.isEmpty()) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO access_management_view (id_user, id_management_view)
                VALUES (?, ?)
                """)) {
            for (Long userId : normalizedIds) {
                insert.setLong(1, userId);
                insert.setLong(2, viewId);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private static String selectSql() {
        return """
                SELECT id_management_view, title, type, description, visibility_scope,
                       scope_target_type, scope_target_id, owner_user_id, state
                FROM management_view
                """;
    }

    private static List<ManagementView> mapManagementViews(ResultSet resultSet) throws SQLException {
        List<ManagementView> views = new ArrayList<>();
        while (resultSet.next()) {
            views.add(mapManagementView(resultSet));
        }
        return List.copyOf(views);
    }

    private static ManagementView mapManagementView(ResultSet resultSet) throws SQLException {
        Long scopeTargetId = nullableLong(resultSet, "scope_target_id");
        Long ownerUserId = nullableLong(resultSet, "owner_user_id");
        String targetType = resultSet.getString("scope_target_type");
        return new ManagementView(
                resultSet.getLong("id_management_view"),
                resultSet.getString("title"),
                ManagementViewType.fromDatabaseValue(resultSet.getString("type")),
                resultSet.getString("description"),
                ManagementViewScope.fromDatabaseValue(resultSet.getString("visibility_scope")),
                targetType == null || targetType.isBlank()
                        ? null
                        : ManagementViewScopeTargetType.fromDatabaseValue(targetType),
                scopeTargetId,
                ownerUserId,
                ManagementViewState.fromDatabaseValue(resultSet.getString("state"))
        );
    }

    private static Set<Long> normalizeIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> normalized = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException("Explicit management-view access requires positive user ids");
            }
            normalized.add(userId);
        }
        return Set.copyOf(normalized);
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }
}
