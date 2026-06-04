package pt.isel.gape.access.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import pt.isel.gape.access.model.AccessProfile;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.model.UserState;
import pt.isel.gape.common.config.ConnectionProvider;

public final class UserDAO {

    private final ConnectionProvider connectionProvider;

    public UserDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = """
                SELECT id_user, name, email, state, language, photo, created_at,
                       credential_hash, credential_salt, document_type, document_number
                FROM user_account
                WHERE email = ?
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                long userId = resultSet.getLong("id_user");
                Set<AccessProfile> profiles = loadProfiles(connection, userId);
                return Optional.of(mapUser(resultSet, profiles));
            }
        }
    }

    public Optional<User> findById(long userId) throws SQLException {
        String sql = """
                SELECT id_user, name, email, state, language, photo, created_at,
                       credential_hash, credential_salt, document_type, document_number
                FROM user_account
                WHERE id_user = ?
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                Set<AccessProfile> profiles = loadProfiles(connection, userId);
                return Optional.of(mapUser(resultSet, profiles));
            }
        }
    }

    private Set<AccessProfile> loadProfiles(Connection connection, long userId) throws SQLException {
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

        Set<AccessProfile> profiles = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 1; index <= 4; index++) {
                statement.setLong(index, userId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    profiles.add(new AccessProfile(
                            AccessProfileType.fromDatabaseValue(resultSet.getString("profile_type")),
                            resultSet.getString("profile_code")
                    ));
                }
            }
        }

        return profiles;
    }

    private User mapUser(ResultSet resultSet, Set<AccessProfile> profiles) throws SQLException {
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        return new User(
                resultSet.getLong("id_user"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                UserState.fromDatabaseValue(resultSet.getString("state")),
                resultSet.getString("language"),
                resultSet.getString("photo"),
                createdAt.toLocalDateTime(),
                resultSet.getString("credential_hash"),
                resultSet.getString("credential_salt"),
                resultSet.getString("document_type"),
                resultSet.getString("document_number"),
                profiles
        );
    }
}
