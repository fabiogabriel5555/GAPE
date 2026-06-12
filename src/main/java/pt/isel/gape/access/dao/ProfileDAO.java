package pt.isel.gape.access.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.model.AccessProfile;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;

public final class ProfileDAO {

    private final ConnectionProvider connectionProvider;

    public ProfileDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
    }

    public Set<AccessProfile> findByUserId(long userId) throws SQLException {
        String sql = """
                SELECT 'ADMINISTRATOR' AS profile_type, cod_administrator AS profile_code
                FROM administrator_profile
                WHERE id_user = ?
                UNION ALL
                SELECT 'COORDINATOR' AS profile_type, cod_coordinator AS profile_code
                FROM coordinator_profile
                WHERE id_user = ?
                UNION ALL
                SELECT 'TEACHER' AS profile_type, cod_teacher AS profile_code
                FROM teacher_profile
                WHERE id_user = ?
                UNION ALL
                SELECT 'STUDENT' AS profile_type, cod_student AS profile_code
                FROM student_profile
                WHERE id_user = ?
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 1; index <= 4; index++) {
                statement.setLong(index, userId);
            }

            Set<AccessProfile> profiles = new LinkedHashSet<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    profiles.add(new AccessProfile(
                            AccessProfileType.fromDatabaseValue(resultSet.getString("profile_type")),
                            resultSet.getString("profile_code")
                    ));
                }
            }
            return profiles;
        }
    }

    public boolean exists(long userId, AccessProfileType profileType) throws SQLException {
        ProfileTable table = ProfileTable.forType(profileType);
        String sql = "SELECT COUNT(*) FROM " + table.tableName() + " WHERE id_user = ?";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public void addProfile(long userId, AccessProfile profile) throws SQLException {
        ProfileTable table = ProfileTable.forType(profile.type());
        String sql = """
                INSERT INTO %s (id_user, %s)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE %s = VALUES(%s)
                """.formatted(table.tableName(), table.codeColumn(), table.codeColumn(), table.codeColumn());

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, profile.code());
            statement.executeUpdate();
        }
    }

    public boolean removeProfile(long userId, AccessProfileType profileType) throws SQLException {
        ProfileTable table = ProfileTable.forType(profileType);
        String sql = "DELETE FROM " + table.tableName() + " WHERE id_user = ?";

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            return statement.executeUpdate() > 0;
        }
    }

    public void replaceProfiles(long userId, Set<AccessProfile> profiles) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                replaceProfiles(connection, userId, profiles);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public void replaceProfiles(Connection connection, long userId, Set<AccessProfile> profiles) throws SQLException {
        Set<AccessProfileType> newTypes = profiles.stream()
                .map(AccessProfile::type)
                .collect(java.util.stream.Collectors.toSet());
        for (AccessProfile profile : profiles) {
            upsertProfile(connection, userId, profile);
        }
        for (AccessProfileType type : AccessProfileType.values()) {
            if (!newTypes.contains(type)) {
                deleteProfile(connection, userId, type);
            }
        }
    }

    private static void deleteProfile(Connection connection, long userId, AccessProfileType profileType) throws SQLException {
        ProfileTable table = ProfileTable.forType(profileType);
        String sql = "DELETE FROM " + table.tableName() + " WHERE id_user = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }

    private static void insertProfile(Connection connection, long userId, AccessProfile profile) throws SQLException {
        ProfileTable table = ProfileTable.forType(profile.type());
        String sql = "INSERT INTO " + table.tableName() + " (id_user, " + table.codeColumn() + ") VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, profile.code());
            statement.executeUpdate();
        }
    }

    private static void upsertProfile(Connection connection, long userId, AccessProfile profile) throws SQLException {
        ProfileTable table = ProfileTable.forType(profile.type());
        String sql = """
                INSERT INTO %s (id_user, %s)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE %s = VALUES(%s)
                """.formatted(table.tableName(), table.codeColumn(), table.codeColumn(), table.codeColumn());
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, profile.code());
            statement.executeUpdate();
        }
    }

    private record ProfileTable(String tableName, String codeColumn) {

        private static ProfileTable forType(AccessProfileType profileType) {
            return switch (profileType) {
                case ADMINISTRATOR -> new ProfileTable(
                        "administrator_profile",
                        "cod_administrator"
                );
                case COORDINATOR -> new ProfileTable(
                        "coordinator_profile",
                        "cod_coordinator"
                );
                case TEACHER -> new ProfileTable(
                        "teacher_profile",
                        "cod_teacher"
                );
                case STUDENT -> new ProfileTable(
                        "student_profile",
                        "cod_student"
                );
            };
        }
    }
}
