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
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectCreateCommand;
import pt.isel.gape.learning.model.SubjectState;
import pt.isel.gape.learning.model.SubjectUpdateCommand;

public final class SubjectDAO {

    private final ConnectionProvider connectionProvider;

    public SubjectDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, SubjectCreateCommand command) throws SQLException {
        String sql = """
                INSERT INTO subject (id_organization, name, acronym, photo, description, ects, workload_hours, state)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, command.organizationId());
            statement.setString(2, command.name().trim());
            setNullableString(statement, 3, command.acronym());
            setNullableString(statement, 4, command.photo());
            setNullableString(statement, 5, command.description());
            setNullableBigDecimal(statement, 6, command.ects());
            setNullableInteger(statement, 7, command.workloadHours());
            statement.setString(8, command.state().toDatabaseValue());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating subject failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<Subject> findById(long subjectId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, subjectId);
        }
    }

    public Optional<Subject> findById(Connection connection, long subjectId) throws SQLException {
        String sql = """
                SELECT id_subject, id_organization, name, acronym, photo, description, ects, workload_hours, state
                FROM subject
                WHERE id_subject = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapSubject(resultSet));
            }
        }
    }

    public List<Subject> findByOrganization(long organizationId) throws SQLException {
        String sql = """
                SELECT id_subject, id_organization, name, acronym, photo, description, ects, workload_hours, state
                FROM subject
                WHERE id_organization = ?
                ORDER BY name
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organizationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Subject> subjects = new ArrayList<>();
                while (resultSet.next()) {
                    subjects.add(mapSubject(resultSet));
                }
                return subjects;
            }
        }
    }

    public List<Subject> findActiveByOrganization(long organizationId) throws SQLException {
        String sql = """
                SELECT id_subject, id_organization, name, acronym, photo, description, ects, workload_hours, state
                FROM subject
                WHERE id_organization = ?
                  AND state = 'active'
                ORDER BY name
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organizationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Subject> subjects = new ArrayList<>();
                while (resultSet.next()) {
                    subjects.add(mapSubject(resultSet));
                }
                return subjects;
            }
        }
    }

    public List<Subject> findByCoordinator(long coordinatorUserId) throws SQLException {
        String sql = """
                SELECT s.id_subject, s.id_organization, s.name, s.acronym, s.photo, s.description,
                       s.ects, s.workload_hours, s.state
                FROM subject s
                JOIN coordinate_subject cs ON cs.id_subject = s.id_subject
                JOIN user_account u ON u.id_user = cs.id_coordinator_user
                WHERE cs.id_coordinator_user = ?
                  AND cs.state = 'active'
                  AND u.state = 'active'
                  AND s.state <> 'archived'
                  AND (cs.start_date IS NULL OR cs.start_date <= CURRENT_DATE)
                  AND (cs.end_date IS NULL OR cs.end_date >= CURRENT_DATE)
                ORDER BY s.name
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, coordinatorUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Subject> subjects = new ArrayList<>();
                while (resultSet.next()) {
                    subjects.add(mapSubject(resultSet));
                }
                return subjects;
            }
        }
    }

    public void update(Connection connection, long subjectId, SubjectUpdateCommand command) throws SQLException {
        String sql = """
                UPDATE subject
                SET name = ?, acronym = ?, photo = ?, description = ?, ects = ?, workload_hours = ?, state = ?
                WHERE id_subject = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, command.name().trim());
            setNullableString(statement, 2, command.acronym());
            setNullableString(statement, 3, command.photo());
            setNullableString(statement, 4, command.description());
            setNullableBigDecimal(statement, 5, command.ects());
            setNullableInteger(statement, 6, command.workloadHours());
            statement.setString(7, command.state().toDatabaseValue());
            statement.setLong(8, subjectId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Subject not found: " + subjectId);
            }
        }
    }

    public void updatePhoto(Connection connection, long subjectId, String photo) throws SQLException {
        String sql = "UPDATE subject SET photo = ? WHERE id_subject = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableString(statement, 1, photo);
            statement.setLong(2, subjectId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Subject not found: " + subjectId);
            }
        }
    }

    public void updateState(Connection connection, long subjectId, SubjectState state) throws SQLException {
        String sql = "UPDATE subject SET state = ? WHERE id_subject = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            statement.setLong(2, subjectId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Subject not found: " + subjectId);
            }
        }
    }

    public boolean hasDomainDependencies(Connection connection, long subjectId) throws SQLException {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM integrate_subject WHERE id_subject = ?)
                  + (SELECT COUNT(*) FROM coordinate_subject WHERE id_subject = ?)
                  + (SELECT COUNT(*) FROM enroll_subject WHERE id_subject = ?)
                  + (SELECT COUNT(*) FROM class_group WHERE id_subject = ?)
                  + (SELECT COUNT(*) FROM assessment WHERE id_subject = ?)
                  + (SELECT COUNT(*) FROM associate_subject_content WHERE id_subject = ?)
                  + (SELECT COUNT(*) FROM grade_sheet WHERE id_subject = ?)
                  AS dependency_count
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            statement.setLong(2, subjectId);
            statement.setLong(3, subjectId);
            statement.setLong(4, subjectId);
            statement.setLong(5, subjectId);
            statement.setLong(6, subjectId);
            statement.setLong(7, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("dependency_count") > 0;
            }
        }
    }

    public void delete(Connection connection, long subjectId) throws SQLException {
        String sql = "DELETE FROM subject WHERE id_subject = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Subject not found: " + subjectId);
            }
        }
    }

    private static Subject mapSubject(ResultSet resultSet) throws SQLException {
        int workloadHours = resultSet.getInt("workload_hours");
        boolean workloadWasNull = resultSet.wasNull();
        return new Subject(
                resultSet.getLong("id_subject"),
                resultSet.getLong("id_organization"),
                resultSet.getString("name"),
                resultSet.getString("acronym"),
                resultSet.getString("photo"),
                resultSet.getString("description"),
                resultSet.getBigDecimal("ects"),
                workloadWasNull ? null : workloadHours,
                SubjectState.fromDatabaseValue(resultSet.getString("state"))
        );
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
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

    private static void setNullableInteger(PreparedStatement statement, int index, Integer value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }
}
