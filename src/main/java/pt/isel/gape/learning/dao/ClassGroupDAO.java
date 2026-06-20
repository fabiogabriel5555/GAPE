package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupCreateCommand;
import pt.isel.gape.learning.model.ClassGroupModality;
import pt.isel.gape.learning.model.ClassGroupShift;
import pt.isel.gape.learning.model.ClassGroupState;
import pt.isel.gape.learning.model.ClassGroupUpdateCommand;

public final class ClassGroupDAO {

    private final ConnectionProvider connectionProvider;

    public ClassGroupDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, ClassGroupCreateCommand command) throws SQLException {
        String sql = """
                INSERT INTO class_group (
                    id_subject, id_course, cod_class_group, modality, state,
                    min_students, max_students, starts_at, ends_at, shift
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, command.subjectId());
            statement.setLong(2, command.courseId());
            statement.setString(3, command.code().trim());
            statement.setString(4, command.modality().toDatabaseValue());
            statement.setString(5, command.state().toDatabaseValue());
            setNullableInteger(statement, 6, command.minStudents());
            setNullableInteger(statement, 7, command.maxStudents());
            setDate(statement, 8, command.startsAt());
            setDate(statement, 9, command.endsAt());
            statement.setString(10, command.shift().toDatabaseValue());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating class group failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<ClassGroup> findById(long classGroupId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, classGroupId);
        }
    }

    public Optional<ClassGroup> findById(Connection connection, long classGroupId) throws SQLException {
        String sql = """
                SELECT id_class_group, id_subject, id_course, cod_class_group, modality, state,
                       min_students, max_students, starts_at, ends_at, shift, show_content_thumbnails
                FROM class_group
                WHERE id_class_group = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapClassGroup(resultSet));
            }
        }
    }

    public List<ClassGroup> findAll() throws SQLException {
        String sql = """
                SELECT id_class_group, id_subject, id_course, cod_class_group, modality, state,
                       min_students, max_students, starts_at, ends_at, shift, show_content_thumbnails
                FROM class_group
                ORDER BY id_class_group
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapClassGroups(resultSet);
        }
    }

    public Optional<ClassGroup> lockById(Connection connection, long classGroupId) throws SQLException {
        String sql = """
                SELECT id_class_group, id_subject, id_course, cod_class_group, modality, state,
                       min_students, max_students, starts_at, ends_at, shift, show_content_thumbnails
                FROM class_group
                WHERE id_class_group = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapClassGroup(resultSet));
            }
        }
    }

    public List<ClassGroup> findByCourse(long courseId) throws SQLException {
        String sql = """
                SELECT id_class_group, id_subject, id_course, cod_class_group, modality, state,
                       min_students, max_students, starts_at, ends_at, shift, show_content_thumbnails
                FROM class_group
                WHERE id_course = ?
                ORDER BY id_class_group
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapClassGroups(resultSet);
            }
        }
    }

    public List<ClassGroup> findBySubject(long subjectId) throws SQLException {
        String sql = """
                SELECT id_class_group, id_subject, id_course, cod_class_group, modality, state,
                       min_students, max_students, starts_at, ends_at, shift, show_content_thumbnails
                FROM class_group
                WHERE id_subject = ?
                ORDER BY id_class_group
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapClassGroups(resultSet);
            }
        }
    }

    public List<ClassGroup> findByCourseAndSubject(long courseId, long subjectId) throws SQLException {
        String sql = """
                SELECT id_class_group, id_subject, id_course, cod_class_group, modality, state,
                       min_students, max_students, starts_at, ends_at, shift, show_content_thumbnails
                FROM class_group
                WHERE id_course = ?
                  AND id_subject = ?
                ORDER BY id_class_group
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            statement.setLong(2, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapClassGroups(resultSet);
            }
        }
    }

    public void update(Connection connection, long classGroupId, ClassGroupUpdateCommand command) throws SQLException {
        String sql = """
                UPDATE class_group
                SET id_subject = ?, id_course = ?, cod_class_group = ?, modality = ?, state = ?,
                    min_students = ?, max_students = ?, starts_at = ?, ends_at = ?, shift = ?,
                    show_content_thumbnails = ?
                WHERE id_class_group = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, command.subjectId());
            statement.setLong(2, command.courseId());
            statement.setString(3, command.code().trim());
            statement.setString(4, command.modality().toDatabaseValue());
            statement.setString(5, command.state().toDatabaseValue());
            setNullableInteger(statement, 6, command.minStudents());
            setNullableInteger(statement, 7, command.maxStudents());
            setDate(statement, 8, command.startsAt());
            setDate(statement, 9, command.endsAt());
            statement.setString(10, command.shift().toDatabaseValue());
            statement.setBoolean(11, command.showContentThumbnails());
            statement.setLong(12, classGroupId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Class group not found: " + classGroupId);
            }
        }
    }

    public void updateState(Connection connection, long classGroupId, ClassGroupState state) throws SQLException {
        String sql = "UPDATE class_group SET state = ? WHERE id_class_group = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            statement.setLong(2, classGroupId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Class group not found: " + classGroupId);
            }
        }
    }

    public long countActiveEnrollments(Connection connection, long classGroupId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM enroll_class_group
                WHERE id_class_group = ?
                  AND state = 'active'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public long countActiveEnrollments(long classGroupId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return countActiveEnrollments(connection, classGroupId);
        }
    }

    public boolean hasDomainDependencies(Connection connection, long classGroupId) throws SQLException {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM content_block WHERE id_class_group = ?)
                  + (SELECT COUNT(*) FROM enroll_class_group WHERE id_class_group = ?)
                  + (SELECT COUNT(*) FROM teach_class_group WHERE id_class_group = ?)
                  + (SELECT COUNT(*) FROM associate_class_group_content WHERE id_class_group = ?)
                  + (SELECT COUNT(*) FROM lesson WHERE id_class_group = ?)
                  + (SELECT COUNT(*) FROM associate_grade_sheet_class_group WHERE id_class_group = ?)
                  + (SELECT COUNT(*) FROM associate_schedule_event_class_group WHERE id_class_group = ?)
                  AS dependency_count
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 1; index <= 7; index++) {
                statement.setLong(index, classGroupId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("dependency_count") > 0;
            }
        }
    }

    public void delete(Connection connection, long classGroupId) throws SQLException {
        String sql = "DELETE FROM class_group WHERE id_class_group = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Class group not found: " + classGroupId);
            }
        }
    }

    private static List<ClassGroup> mapClassGroups(ResultSet resultSet) throws SQLException {
        List<ClassGroup> classGroups = new ArrayList<>();
        while (resultSet.next()) {
            classGroups.add(mapClassGroup(resultSet));
        }
        return classGroups;
    }

    private static ClassGroup mapClassGroup(ResultSet resultSet) throws SQLException {
        Date startsAt = resultSet.getDate("starts_at");
        Date endsAt = resultSet.getDate("ends_at");
        int minStudents = resultSet.getInt("min_students");
        boolean minWasNull = resultSet.wasNull();
        int maxStudents = resultSet.getInt("max_students");
        boolean maxWasNull = resultSet.wasNull();
        return new ClassGroup(
                resultSet.getLong("id_class_group"),
                resultSet.getLong("id_subject"),
                resultSet.getLong("id_course"),
                resultSet.getString("cod_class_group"),
                ClassGroupModality.fromDatabaseValue(resultSet.getString("modality")),
                ClassGroupState.fromDatabaseValue(resultSet.getString("state")),
                minWasNull ? null : minStudents,
                maxWasNull ? null : maxStudents,
                startsAt == null ? null : startsAt.toLocalDate(),
                endsAt == null ? null : endsAt.toLocalDate(),
                ClassGroupShift.fromDatabaseValue(resultSet.getString("shift")),
                resultSet.getBoolean("show_content_thumbnails")
        );
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }

    private static void setNullableInteger(PreparedStatement statement, int index, Integer value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private static void setDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(value));
        }
    }
}
