package pt.isel.gape.access.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import pt.isel.gape.access.model.AccessProfile;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.model.UserCreateCommand;
import pt.isel.gape.access.model.UserState;
import pt.isel.gape.access.model.UserUpdateCommand;
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
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, userId);
        }
    }

    public Optional<User> findById(Connection connection, long userId) throws SQLException {
        String sql = """
                SELECT id_user, name, email, state, language, photo, created_at,
                       credential_hash, credential_salt, document_type, document_number
                FROM user_account
                WHERE id_user = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
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

    public List<User> findAll() throws SQLException {
        String sql = """
                SELECT id_user, name, email, state, language, photo, created_at,
                       credential_hash, credential_salt, document_type, document_number
                FROM user_account
                ORDER BY id_user
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<User> users = new ArrayList<>();
            while (resultSet.next()) {
                long userId = resultSet.getLong("id_user");
                users.add(mapUser(resultSet, loadProfiles(connection, userId)));
            }
            return users;
        }
    }

    public List<User> findActiveTeachers() throws SQLException {
        String sql = """
                SELECT u.id_user, u.name, u.email, u.state, u.language, u.photo, u.created_at,
                       u.credential_hash, u.credential_salt, u.document_type, u.document_number
                FROM user_account u
                JOIN teacher_profile tp ON tp.id_user = u.id_user
                WHERE u.state = 'active'
                ORDER BY u.name, u.email
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<User> users = new ArrayList<>();
            while (resultSet.next()) {
                long userId = resultSet.getLong("id_user");
                users.add(mapUser(resultSet, loadProfiles(connection, userId)));
            }
            return users;
        }
    }

    public List<User> findActiveStudentsEnrolledInSubject(long courseId, long subjectId) throws SQLException {
        String sql = """
                SELECT DISTINCT u.id_user, u.name, u.email, u.state, u.language, u.photo, u.created_at,
                       u.credential_hash, u.credential_salt, u.document_type, u.document_number
                FROM user_account u
                JOIN student_profile sp ON sp.id_user = u.id_user
                JOIN enroll_subject es ON es.id_student_user = u.id_user
                WHERE u.state = 'active'
                  AND es.id_course = ?
                  AND es.id_subject = ?
                  AND es.state = 'active'
                  AND (es.start_date IS NULL OR es.start_date <= CURRENT_DATE)
                  AND (es.end_date IS NULL OR es.end_date >= CURRENT_DATE)
                ORDER BY u.name, u.email
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            statement.setLong(2, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (resultSet.next()) {
                    long userId = resultSet.getLong("id_user");
                    users.add(mapUser(resultSet, loadProfiles(connection, userId)));
                }
                return users;
            }
        }
    }

    public long create(UserCreateCommand command, LocalDateTime createdAt) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return create(connection, command, createdAt);
        }
    }

    public long create(Connection connection, UserCreateCommand command, LocalDateTime createdAt) throws SQLException {
        String sql = """
                INSERT INTO user_account (
                    name, email, state, language, photo, created_at,
                    credential_hash, credential_salt, document_type, document_number
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, command.name());
            statement.setString(2, command.email());
            statement.setString(3, command.state().toDatabaseValue());
            statement.setString(4, command.language());
            statement.setString(5, command.photo());
            statement.setObject(6, createdAt);
            statement.setString(7, command.credentialHash());
            statement.setString(8, command.credentialSalt());
            statement.setString(9, command.documentType());
            statement.setString(10, command.documentNumber());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating user failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public boolean update(long userId, UserUpdateCommand command) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return update(connection, userId, command);
        }
    }

    public boolean update(Connection connection, long userId, UserUpdateCommand command) throws SQLException {
        String sql = """
                UPDATE user_account
                SET name = ?, email = ?, state = ?, language = ?, photo = ?,
                    document_type = ?, document_number = ?
                WHERE id_user = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, command.name());
            statement.setString(2, command.email());
            statement.setString(3, command.state().toDatabaseValue());
            statement.setString(4, command.language());
            statement.setString(5, command.photo());
            statement.setString(6, command.documentType());
            statement.setString(7, command.documentNumber());
            statement.setLong(8, userId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updatePhoto(long userId, String photo) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return updatePhoto(connection, userId, photo);
        }
    }

    public boolean updatePhoto(Connection connection, long userId, String photo) throws SQLException {
        String sql = """
                UPDATE user_account
                SET photo = ?
                WHERE id_user = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableString(statement, 1, photo);
            statement.setLong(2, userId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updatePersonalProfile(
            long userId,
            String name,
            String email,
            String language,
            String photo,
            String documentType,
            String documentNumber
    ) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return updatePersonalProfile(connection, userId, name, email, language, photo, documentType, documentNumber);
        }
    }

    public boolean updatePersonalProfile(
            Connection connection,
            long userId,
            String name,
            String email,
            String language,
            String photo,
            String documentType,
            String documentNumber
    ) throws SQLException {
        String sql = """
                UPDATE user_account
                SET name = ?, email = ?, language = ?, photo = ?,
                    document_type = ?, document_number = ?
                WHERE id_user = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, language);
            statement.setString(4, photo);
            statement.setString(5, documentType);
            statement.setString(6, documentNumber);
            statement.setLong(7, userId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateState(long userId, UserState state) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return updateState(connection, userId, state);
        }
    }

    public boolean updateState(Connection connection, long userId, UserState state) throws SQLException {
        String sql = """
                UPDATE user_account
                SET state = ?
                WHERE id_user = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            statement.setLong(2, userId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateCredentials(long userId, String credentialHash, String credentialSalt) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return updateCredentials(connection, userId, credentialHash, credentialSalt);
        }
    }

    public boolean updateCredentials(
            Connection connection,
            long userId,
            String credentialHash,
            String credentialSalt
    ) throws SQLException {
        String sql = """
                UPDATE user_account
                SET credential_hash = ?, credential_salt = ?
                WHERE id_user = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, credentialHash);
            statement.setString(2, credentialSalt);
            statement.setLong(3, userId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(long userId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return delete(connection, userId);
        }
    }

    public boolean delete(Connection connection, long userId) throws SQLException {
        String sql = """
                DELETE FROM user_account
                WHERE id_user = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean existsEmail(String email, Long excludingUserId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return existsEmail(connection, email, excludingUserId);
        }
    }

    public boolean existsEmail(Connection connection, String email, Long excludingUserId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM user_account
                WHERE LOWER(email) = LOWER(?)
                  AND (? IS NULL OR id_user <> ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            if (excludingUserId == null) {
                statement.setNull(2, java.sql.Types.BIGINT);
                statement.setNull(3, java.sql.Types.BIGINT);
            } else {
                statement.setLong(2, excludingUserId);
                statement.setLong(3, excludingUserId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean existsDocument(String documentType, String documentNumber, Long excludingUserId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return existsDocument(connection, documentType, documentNumber, excludingUserId);
        }
    }

    public boolean existsDocument(
            Connection connection,
            String documentType,
            String documentNumber,
            Long excludingUserId
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM user_account
                WHERE document_type = ?
                  AND document_number = ?
                  AND (? IS NULL OR id_user <> ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, documentType);
            statement.setString(2, documentNumber);
            if (excludingUserId == null) {
                statement.setNull(3, java.sql.Types.BIGINT);
                statement.setNull(4, java.sql.Types.BIGINT);
            } else {
                statement.setLong(3, excludingUserId);
                statement.setLong(4, excludingUserId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
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
        return new User(
                resultSet.getLong("id_user"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                UserState.fromDatabaseValue(resultSet.getString("state")),
                resultSet.getString("language"),
                resultSet.getString("photo"),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getString("credential_hash"),
                resultSet.getString("credential_salt"),
                resultSet.getString("document_type"),
                resultSet.getString("document_number"),
                profiles
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
