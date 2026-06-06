package pt.isel.gape.access.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.Permission;
import pt.isel.gape.access.model.PermissionState;
import pt.isel.gape.common.config.ConnectionProvider;

public final class PermissionDAO {

    private final ConnectionProvider connectionProvider;

    public PermissionDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public Optional<Permission> findByCode(String permissionCode) throws SQLException {
        String sql = """
                SELECT cod_permission, name, state
                FROM permission
                WHERE cod_permission = ?
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, permissionCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapPermission(resultSet));
            }
        }
    }

    public boolean isActivePermission(String permissionCode) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM permission
                WHERE cod_permission = ? AND state = 'active'
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, permissionCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean hasActiveGrant(long userId, AccessProfileType profileType, String permissionCode) throws SQLException {
        GrantTable grantTable = GrantTable.forProfile(profileType);
        String sql = """
                SELECT COUNT(*)
                FROM %s grant_table
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                WHERE grant_table.%s = ?
                  AND grant_table.cod_permission = ?
                  AND p.state = 'active'
                """.formatted(grantTable.tableName(), grantTable.userIdColumn());

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, permissionCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public Set<String> findActivePermissionCodes(long userId, AccessProfileType profileType) throws SQLException {
        GrantTable grantTable = GrantTable.forProfile(profileType);
        String sql = """
                SELECT grant_table.cod_permission
                FROM %s grant_table
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                WHERE grant_table.%s = ?
                  AND p.state = 'active'
                ORDER BY grant_table.cod_permission
                """.formatted(grantTable.tableName(), grantTable.userIdColumn());

        Set<String> permissions = new LinkedHashSet<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    permissions.add(resultSet.getString("cod_permission"));
                }
            }
        }
        return permissions;
    }

    public boolean activeProfileExists(long userId, AccessProfileType profileType) throws SQLException {
        GrantTable grantTable = GrantTable.forProfile(profileType);
        String sql = """
                SELECT COUNT(*)
                FROM %s profile_table
                JOIN user_account u ON u.id_user = profile_table.id_user
                WHERE profile_table.id_user = ?
                  AND u.state = 'active'
                """.formatted(grantTable.profileTableName());

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public void grantPermission(long userId, AccessProfileType profileType, String permissionCode) throws SQLException {
        GrantTable grantTable = GrantTable.forProfile(profileType);
        String sql = """
                INSERT INTO %s (%s, cod_permission)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE cod_permission = VALUES(cod_permission)
                """.formatted(grantTable.tableName(), grantTable.userIdColumn());

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, permissionCode);
            statement.executeUpdate();
        }
    }

    public void revokePermission(long userId, AccessProfileType profileType, String permissionCode) throws SQLException {
        GrantTable grantTable = GrantTable.forProfile(profileType);
        String sql = """
                DELETE FROM %s
                WHERE %s = ? AND cod_permission = ?
                """.formatted(grantTable.tableName(), grantTable.userIdColumn());

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, permissionCode);
            statement.executeUpdate();
        }
    }

    private static Permission mapPermission(ResultSet resultSet) throws SQLException {
        return new Permission(
                resultSet.getString("cod_permission"),
                resultSet.getString("name"),
                PermissionState.fromDatabaseValue(resultSet.getString("state"))
        );
    }

    private record GrantTable(String tableName, String userIdColumn, String profileTableName) {

        private static GrantTable forProfile(AccessProfileType profileType) {
            return switch (profileType) {
                case ADMINISTRATOR -> new GrantTable("grant_administrator", "id_admin_user", "administrator_profile");
                case COORDINATOR -> new GrantTable("grant_coordinator", "id_coordinator_user", "coordinator_profile");
                case TEACHER -> new GrantTable("grant_teacher", "id_teacher_user", "teacher_profile");
                case STUDENT -> new GrantTable("grant_student", "id_student_user", "student_profile");
            };
        }
    }
}
