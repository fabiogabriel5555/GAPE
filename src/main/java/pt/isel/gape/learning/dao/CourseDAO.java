package pt.isel.gape.learning.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseCreateCommand;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.CourseType;
import pt.isel.gape.learning.model.CourseUpdateCommand;

public final class CourseDAO {

    private final ConnectionProvider connectionProvider;

    public CourseDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, CourseCreateCommand command) throws SQLException {
        String sql = """
                INSERT INTO course (
                    id_organization, id_organic_unit, name, acronym, photo, description, ects, duration, type, state
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, command.organizationId());
            setNullableLong(statement, 2, command.organicUnitId());
            statement.setString(3, command.name().trim());
            setNullableString(statement, 4, command.acronym());
            setNullableString(statement, 5, command.photo());
            setNullableString(statement, 6, command.description());
            setNullableBigDecimal(statement, 7, command.ects());
            setNullableString(statement, 8, command.duration());
            statement.setString(9, command.type().toDatabaseValue());
            statement.setString(10, command.state().toDatabaseValue());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating course failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<Course> findById(long courseId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, courseId);
        }
    }

    public Optional<Course> findById(Connection connection, long courseId) throws SQLException {
        String sql = """
                SELECT id_course, id_organization, id_organic_unit, name, acronym, photo, description,
                       ects, duration, type, state
                FROM course
                WHERE id_course = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapCourse(resultSet));
            }
        }
    }

    public List<Course> findByOrganization(long organizationId) throws SQLException {
        String sql = """
                SELECT id_course, id_organization, id_organic_unit, name, acronym, photo, description,
                       ects, duration, type, state
                FROM course
                WHERE id_organization = ?
                ORDER BY name
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organizationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Course> courses = new ArrayList<>();
                while (resultSet.next()) {
                    courses.add(mapCourse(resultSet));
                }
                return courses;
            }
        }
    }

    public List<Course> findCatalogCourses(Long organizationId, CourseType type, String query) throws SQLException {
        String normalizedQuery = query == null || query.isBlank()
                ? null
                : "%" + query.trim().toLowerCase(java.util.Locale.ROOT) + "%";
        String sql = """
                SELECT id_course, id_organization, id_organic_unit, name, acronym, photo, description,
                       ects, duration, type, state
                FROM course
                WHERE state = 'active'
                  AND (? IS NULL OR id_organization = ?)
                  AND (? IS NULL OR type = ?)
                  AND (
                        ? IS NULL
                        OR LOWER(name) LIKE ?
                        OR LOWER(COALESCE(acronym, '')) LIKE ?
                        OR LOWER(COALESCE(description, '')) LIKE ?
                  )
                ORDER BY name
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableLong(statement, 1, organizationId);
            setNullableLong(statement, 2, organizationId);
            setNullableString(statement, 3, type == null ? null : type.toDatabaseValue());
            setNullableString(statement, 4, type == null ? null : type.toDatabaseValue());
            setNullableString(statement, 5, normalizedQuery);
            setNullableString(statement, 6, normalizedQuery);
            setNullableString(statement, 7, normalizedQuery);
            setNullableString(statement, 8, normalizedQuery);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Course> courses = new ArrayList<>();
                while (resultSet.next()) {
                    courses.add(mapCourse(resultSet));
                }
                return courses;
            }
        }
    }

    public Optional<Course> findActiveById(long courseId) throws SQLException {
        String sql = """
                SELECT id_course, id_organization, id_organic_unit, name, acronym, photo, description,
                       ects, duration, type, state
                FROM course
                WHERE id_course = ?
                  AND state = 'active'
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapCourse(resultSet));
            }
        }
    }

    public void update(Connection connection, long courseId, CourseUpdateCommand command) throws SQLException {
        String sql = """
                UPDATE course
                SET id_organization = ?, id_organic_unit = ?, name = ?, acronym = ?, photo = ?, description = ?,
                    ects = ?, duration = ?, type = ?, state = ?
                WHERE id_course = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, command.organizationId());
            setNullableLong(statement, 2, command.organicUnitId());
            statement.setString(3, command.name().trim());
            setNullableString(statement, 4, command.acronym());
            setNullableString(statement, 5, command.photo());
            setNullableString(statement, 6, command.description());
            setNullableBigDecimal(statement, 7, command.ects());
            setNullableString(statement, 8, command.duration());
            statement.setString(9, command.type().toDatabaseValue());
            statement.setString(10, command.state().toDatabaseValue());
            statement.setLong(11, courseId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Course not found: " + courseId);
            }
        }
    }

    public void updatePhoto(Connection connection, long courseId, String photo) throws SQLException {
        String sql = "UPDATE course SET photo = ? WHERE id_course = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableString(statement, 1, photo);
            statement.setLong(2, courseId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Course not found: " + courseId);
            }
        }
    }

    public void updateState(Connection connection, long courseId, CourseState state) throws SQLException {
        String sql = "UPDATE course SET state = ? WHERE id_course = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            statement.setLong(2, courseId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Course not found: " + courseId);
            }
        }
    }

    public boolean hasDomainDependencies(Connection connection, long courseId) throws SQLException {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM integrate_subject WHERE id_course = ?)
                  + (SELECT COUNT(*) FROM class_group WHERE id_course = ?)
                  + (SELECT COUNT(*) FROM enroll_course WHERE id_course = ?)
                  + (SELECT COUNT(*) FROM associate_course_content WHERE id_course = ?)
                  + (SELECT COUNT(*) FROM certificate WHERE id_course = ?)
                  AS dependency_count
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            statement.setLong(2, courseId);
            statement.setLong(3, courseId);
            statement.setLong(4, courseId);
            statement.setLong(5, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("dependency_count") > 0;
            }
        }
    }

    public void delete(Connection connection, long courseId) throws SQLException {
        String sql = "DELETE FROM course WHERE id_course = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Course not found: " + courseId);
            }
        }
    }

    private static Course mapCourse(ResultSet resultSet) throws SQLException {
        long organicUnitId = resultSet.getLong("id_organic_unit");
        boolean organicUnitWasNull = resultSet.wasNull();
        return new Course(
                resultSet.getLong("id_course"),
                resultSet.getLong("id_organization"),
                organicUnitWasNull ? null : organicUnitId,
                resultSet.getString("name"),
                resultSet.getString("acronym"),
                resultSet.getString("photo"),
                resultSet.getString("description"),
                resultSet.getBigDecimal("ects"),
                resultSet.getString("duration"),
                CourseType.fromDatabaseValue(resultSet.getString("type")),
                CourseState.fromDatabaseValue(resultSet.getString("state"))
        );
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static void setNullableBigDecimal(PreparedStatement statement, int index, BigDecimal value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DECIMAL);
        } else {
            statement.setBigDecimal(index, value);
        }
    }
}
