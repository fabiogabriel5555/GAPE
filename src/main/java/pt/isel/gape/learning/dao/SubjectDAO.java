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
import pt.isel.gape.security.authorization.AuthorizationPolicy;

public final class SubjectDAO implements pt.isel.gape.transversal.service.ApplicationReadService.Subjects {

    private final ConnectionProvider connectionProvider;

    public SubjectDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, SubjectCreateCommand command) throws SQLException {
        String sql = """
                INSERT INTO subject (
                    id_organization, id_organic_unit, name, acronym, photo, description,
                    ects, final_grade_max, workload_hours, state
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, command.organizationId());
            setNullableLong(statement, 2, command.organicUnitId());
            statement.setString(3, command.name().trim());
            setNullableString(statement, 4, command.acronym());
            setNullableString(statement, 5, command.photo());
            setNullableString(statement, 6, command.description());
            statement.setBigDecimal(7, command.ects());
            statement.setBigDecimal(8, command.finalGradeMax());
            setNullableInteger(statement, 9, command.workloadHours());
            statement.setString(10, command.state().toDatabaseValue());
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
                SELECT id_subject, id_organization, id_organic_unit, name, acronym, photo, description,
                       ects, final_grade_max, workload_hours, state
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

    public List<Subject> findAll() throws SQLException {
        String sql = """
                SELECT id_subject, id_organization, id_organic_unit, name, acronym, photo, description,
                       ects, final_grade_max, workload_hours, state
                FROM subject
                ORDER BY name, id_subject
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Subject> subjects = new ArrayList<>();
            while (resultSet.next()) {
                subjects.add(mapSubject(resultSet));
            }
            return subjects;
        }
    }

    public List<Subject> findByOrganization(long organizationId) throws SQLException {
        String sql = """
                SELECT id_subject, id_organization, id_organic_unit, name, acronym, photo, description,
                       ects, final_grade_max, workload_hours, state
                FROM subject
                WHERE id_organization = ?
                ORDER BY id_subject
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
                SELECT id_subject, id_organization, id_organic_unit, name, acronym, photo, description,
                       ects, final_grade_max, workload_hours, state
                FROM subject
                WHERE id_organization = ?
                  AND state = 'active'
                ORDER BY id_subject
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
                SELECT s.id_subject, s.id_organization, s.id_organic_unit, s.name, s.acronym, s.photo, s.description,
                       s.ects, s.final_grade_max, s.workload_hours, s.state
                FROM subject s
                JOIN coordinate_subject cs ON cs.id_subject = s.id_subject
                JOIN user_account u ON u.id_user = cs.id_coordinator_user
                WHERE cs.id_coordinator_user = ?
                  AND cs.state = 'active'
                  AND u.state = 'active'
                  AND s.state = 'active'
                ORDER BY s.id_subject
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

    public List<Subject> findByTeacher(long teacherUserId) throws SQLException {
        String sql = """
                SELECT DISTINCT s.id_subject, s.id_organization, s.id_organic_unit, s.name, s.acronym, s.photo, s.description,
                       s.ects, s.final_grade_max, s.workload_hours, s.state
                FROM subject s
                JOIN class_group cg ON cg.id_subject = s.id_subject
                JOIN course c ON c.id_course = cg.id_course
                JOIN teach_class_group tcg ON tcg.id_class_group = cg.id_class_group
                JOIN teacher_profile tp ON tp.id_user = tcg.id_teacher_user
                JOIN user_account u ON u.id_user = tp.id_user
                JOIN grant_teacher gt ON gt.id_teacher_user = tcg.id_teacher_user
                JOIN permission p ON p.cod_permission = gt.cod_permission
                WHERE tcg.id_teacher_user = ?
                  AND tcg.state = 'active'
                  AND cg.state = 'active'
                  AND c.state = 'active'
                  AND s.state = 'active'
                  AND u.state = 'active'
                  AND gt.cod_permission = ?
                  AND p.state = 'active'
                  AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                  AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                ORDER BY s.id_subject
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            statement.setString(2, AuthorizationPolicy.MANAGE_LEARNING);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Subject> subjects = new ArrayList<>();
                while (resultSet.next()) {
                    subjects.add(mapSubject(resultSet));
                }
                return subjects;
            }
        }
    }

    public boolean teacherCanReadSubject(
            Connection connection,
            long teacherUserId,
            long subjectId
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM subject s
                JOIN class_group cg ON cg.id_subject = s.id_subject
                JOIN course c ON c.id_course = cg.id_course
                JOIN teach_class_group tcg ON tcg.id_class_group = cg.id_class_group
                JOIN teacher_profile tp ON tp.id_user = tcg.id_teacher_user
                JOIN user_account u ON u.id_user = tp.id_user
                JOIN grant_teacher gt ON gt.id_teacher_user = tcg.id_teacher_user
                JOIN permission p ON p.cod_permission = gt.cod_permission
                WHERE tcg.id_teacher_user = ?
                  AND s.id_subject = ?
                  AND tcg.state = 'active'
                  AND cg.state = 'active'
                  AND c.state = 'active'
                  AND s.state = 'active'
                  AND u.state = 'active'
                  AND gt.cod_permission = ?
                  AND p.state = 'active'
                  AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                  AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            statement.setLong(2, subjectId);
            statement.setString(3, AuthorizationPolicy.MANAGE_LEARNING);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public void update(Connection connection, long subjectId, SubjectUpdateCommand command) throws SQLException {
        String sql = """
                UPDATE subject
                SET id_organic_unit = ?, name = ?, acronym = ?, photo = ?, description = ?,
                    ects = ?, final_grade_max = ?, workload_hours = ?, state = ?
                WHERE id_subject = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableLong(statement, 1, command.organicUnitId());
            statement.setString(2, command.name().trim());
            setNullableString(statement, 3, command.acronym());
            setNullableString(statement, 4, command.photo());
            setNullableString(statement, 5, command.description());
            statement.setBigDecimal(6, command.ects());
            statement.setBigDecimal(7, command.finalGradeMax());
            setNullableInteger(statement, 8, command.workloadHours());
            statement.setString(9, command.state().toDatabaseValue());
            statement.setLong(10, subjectId);
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
        long organicUnitId = resultSet.getLong("id_organic_unit");
        boolean organicUnitWasNull = resultSet.wasNull();
        return new Subject(
                resultSet.getLong("id_subject"),
                resultSet.getLong("id_organization"),
                organicUnitWasNull ? null : organicUnitId,
                resultSet.getString("name"),
                resultSet.getString("acronym"),
                resultSet.getString("photo"),
                resultSet.getString("description"),
                resultSet.getBigDecimal("ects"),
                resultSet.getBigDecimal("final_grade_max"),
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

    private static void setNullableInteger(PreparedStatement statement, int index, Integer value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }
}
