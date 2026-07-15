package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseSubjectAssociationCommand;
import pt.isel.gape.learning.model.CourseSubjectAssociationState;
import pt.isel.gape.learning.model.CurricularTerm;

public final class CourseSubjectDAO implements pt.isel.gape.transversal.service.ApplicationReadService.CourseSubjects {

    private final ConnectionProvider connectionProvider;

    public CourseSubjectDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public void create(Connection connection, CourseSubjectAssociationCommand command) throws SQLException {
        String sql = """
                INSERT INTO integrate_subject (
                    id_course, id_subject, curricular_year, term, mandatory, state, ended_at
                ) VALUES (?, ?, ?, ?, ?, 'active', NULL)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, command.courseId());
            statement.setLong(2, command.subjectId());
            setNullableInteger(statement, 3, command.curricularYear());
            setNullableTerm(statement, 4, command.term());
            statement.setBoolean(5, command.mandatory());
            statement.executeUpdate();
        }
    }

    public Optional<CourseSubjectAssociation> findByCourseAndSubject(
            long courseId,
            long subjectId
    ) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findByCourseAndSubject(connection, courseId, subjectId);
        }
    }

    public Optional<CourseSubjectAssociation> findByCourseAndSubject(
            Connection connection,
            long courseId,
            long subjectId
    ) throws SQLException {
        String sql = """
                SELECT id_course, id_subject, curricular_year, term, mandatory, state, ended_at
                FROM integrate_subject
                WHERE id_course = ?
                  AND id_subject = ?
                  AND state = 'active'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            statement.setLong(2, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapAssociation(resultSet));
            }
        }
    }

    public Optional<CourseSubjectAssociation> findAnyByCourseAndSubject(
            Connection connection,
            long courseId,
            long subjectId
    ) throws SQLException {
        String sql = """
                SELECT id_course, id_subject, curricular_year, term, mandatory, state, ended_at
                FROM integrate_subject
                WHERE id_course = ?
                  AND id_subject = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            statement.setLong(2, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapAssociation(resultSet)) : Optional.empty();
            }
        }
    }

    public List<CourseSubjectAssociation> findByCourse(long courseId) throws SQLException {
        String sql = """
                SELECT id_course, id_subject, curricular_year, term, mandatory, state, ended_at
                FROM integrate_subject
                WHERE id_course = ?
                  AND state = 'active'
                ORDER BY curricular_year, term, id_subject
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CourseSubjectAssociation> associations = new ArrayList<>();
                while (resultSet.next()) {
                    associations.add(mapAssociation(resultSet));
                }
                return associations;
            }
        }
    }

    public List<CourseSubjectAssociation> findBySubject(long subjectId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findBySubject(connection, subjectId);
        }
    }

    public List<CourseSubjectAssociation> findBySubject(Connection connection, long subjectId) throws SQLException {
        String sql = """
                SELECT id_course, id_subject, curricular_year, term, mandatory, state, ended_at
                FROM integrate_subject
                WHERE id_subject = ?
                  AND state = 'active'
                ORDER BY curricular_year, term, id_course
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CourseSubjectAssociation> associations = new ArrayList<>();
                while (resultSet.next()) {
                    associations.add(mapAssociation(resultSet));
                }
                return associations;
            }
        }
    }

    public long countBySubject(Connection connection, long subjectId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM integrate_subject
                WHERE id_subject = ?
                  AND state = 'active'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public boolean exists(Connection connection, long courseId, long subjectId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM integrate_subject
                WHERE id_course = ?
                  AND id_subject = ?
                  AND state = 'active'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            statement.setLong(2, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public void update(Connection connection, CourseSubjectAssociationCommand command) throws SQLException {
        String sql = """
                UPDATE integrate_subject
                SET curricular_year = ?, term = ?, mandatory = ?
                WHERE id_course = ?
                  AND id_subject = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableInteger(statement, 1, command.curricularYear());
            setNullableTerm(statement, 2, command.term());
            statement.setBoolean(3, command.mandatory());
            statement.setLong(4, command.courseId());
            statement.setLong(5, command.subjectId());
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Course-subject association not found");
            }
        }
    }

    public void reactivate(Connection connection, CourseSubjectAssociationCommand command) throws SQLException {
        String sql = """
                UPDATE integrate_subject
                SET curricular_year = ?, term = ?, mandatory = ?, state = 'active', ended_at = NULL
                WHERE id_course = ?
                  AND id_subject = ?
                  AND state = 'historical'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableInteger(statement, 1, command.curricularYear());
            setNullableTerm(statement, 2, command.term());
            statement.setBoolean(3, command.mandatory());
            statement.setLong(4, command.courseId());
            statement.setLong(5, command.subjectId());
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Historical course-subject association not found");
            }
        }
    }

    public boolean hasDomainDependencies(Connection connection, long courseId, long subjectId) throws SQLException {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM class_group WHERE id_course = ? AND id_subject = ?)
                  AS dependency_count
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            statement.setLong(2, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("dependency_count") > 0;
            }
        }
    }

    public void delete(Connection connection, long courseId, long subjectId) throws SQLException {
        String sql = """
                DELETE FROM integrate_subject
                WHERE id_course = ?
                  AND id_subject = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            statement.setLong(2, subjectId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Course-subject association not found");
            }
        }
    }

    public void close(Connection connection, long courseId, long subjectId, LocalDate endedAt) throws SQLException {
        String sql = """
                UPDATE integrate_subject
                SET state = 'historical', ended_at = ?
                WHERE id_course = ?
                  AND id_subject = ?
                  AND state = 'active'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(endedAt));
            statement.setLong(2, courseId);
            statement.setLong(3, subjectId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Active course-subject association not found");
            }
        }
    }

    private static CourseSubjectAssociation mapAssociation(ResultSet resultSet) throws SQLException {
        int year = resultSet.getInt("curricular_year");
        boolean yearWasNull = resultSet.wasNull();
        String term = resultSet.getString("term");
        Date endedAt = resultSet.getDate("ended_at");
        return new CourseSubjectAssociation(
                resultSet.getLong("id_course"),
                resultSet.getLong("id_subject"),
                yearWasNull ? null : year,
                term == null ? null : CurricularTerm.fromDatabaseValue(term),
                resultSet.getBoolean("mandatory"),
                CourseSubjectAssociationState.fromDatabaseValue(resultSet.getString("state")),
                endedAt == null ? null : endedAt.toLocalDate()
        );
    }

    private static void setNullableInteger(PreparedStatement statement, int index, Integer value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private static void setNullableTerm(PreparedStatement statement, int index, CurricularTerm value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value.toDatabaseValue());
        }
    }
}
