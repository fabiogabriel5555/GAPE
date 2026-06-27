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
import pt.isel.gape.learning.model.ClassGroupEnrollment;
import pt.isel.gape.learning.model.ClassGroupEnrollmentCommand;
import pt.isel.gape.learning.model.EnrollmentState;

public final class ClassGroupEnrollmentDAO {

    private final ConnectionProvider connectionProvider;

    public ClassGroupEnrollmentDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public void enroll(Connection connection, ClassGroupEnrollmentCommand command) throws SQLException {
        save(connection, command, EnrollmentState.ACTIVE);
    }

    public void request(Connection connection, ClassGroupEnrollmentCommand command) throws SQLException {
        save(connection, command, EnrollmentState.PENDING);
    }

    private void save(
            Connection connection,
            ClassGroupEnrollmentCommand command,
            EnrollmentState state
    ) throws SQLException {
        String sql = """
                INSERT INTO enroll_class_group (id_student_user, id_class_group, state, start_date, end_date)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, command.studentUserId());
            statement.setLong(2, command.classGroupId());
            statement.setString(3, state.toDatabaseValue());
            setDate(statement, 4, command.startDate());
            setDate(statement, 5, command.endDate());
            statement.executeUpdate();
        }
    }

    public void updateState(
            Connection connection,
            long studentUserId,
            long classGroupId,
            EnrollmentState expectedState,
            EnrollmentState newState,
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                UPDATE enroll_class_group
                SET state = ?, start_date = ?, end_date = ?
                WHERE id_student_user = ?
                  AND id_class_group = ?
                  AND state = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newState.toDatabaseValue());
            setDate(statement, 2, startDate);
            setDate(statement, 3, endDate);
            statement.setLong(4, studentUserId);
            statement.setLong(5, classGroupId);
            statement.setString(6, expectedState.toDatabaseValue());
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Expected class group enrollment state not found");
            }
        }
    }

    public void reactivateRequest(
            Connection connection,
            ClassGroupEnrollmentCommand command,
            EnrollmentState newState
    ) throws SQLException {
        String sql = """
                UPDATE enroll_class_group
                SET state = ?, start_date = ?, end_date = ?
                WHERE id_student_user = ?
                  AND id_class_group = ?
                  AND state IN ('inactive', 'rejected', 'withdrawn')
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newState.toDatabaseValue());
            setDate(statement, 2, command.startDate());
            setDate(statement, 3, command.endDate());
            statement.setLong(4, command.studentUserId());
            statement.setLong(5, command.classGroupId());
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Reusable class group enrollment not found");
            }
        }
    }

    public void withdraw(
            Connection connection,
            long studentUserId,
            long classGroupId,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                UPDATE enroll_class_group
                SET state = 'withdrawn', end_date = ?
                WHERE id_student_user = ?
                  AND id_class_group = ?
                  AND state = 'active'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setDate(statement, 1, endDate);
            statement.setLong(2, studentUserId);
            statement.setLong(3, classGroupId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Active class group enrollment not found");
            }
        }
    }

    public void updateEnrollment(
            Connection connection,
            long studentUserId,
            long classGroupId,
            EnrollmentState state,
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                UPDATE enroll_class_group
                SET state = ?, start_date = ?, end_date = ?
                WHERE id_student_user = ?
                  AND id_class_group = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            setDate(statement, 2, startDate);
            setDate(statement, 3, endDate);
            statement.setLong(4, studentUserId);
            statement.setLong(5, classGroupId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Class group enrollment not found");
            }
        }
    }

    public void deleteEnrollment(
            Connection connection,
            long studentUserId,
            long classGroupId
    ) throws SQLException {
        String sql = """
                DELETE FROM enroll_class_group
                WHERE id_student_user = ?
                  AND id_class_group = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, classGroupId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Class group enrollment not found");
            }
        }
    }

    public void withdrawActiveInCourse(
            Connection connection,
            long studentUserId,
            long courseId,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                UPDATE enroll_class_group ecg
                JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
                SET ecg.state = 'withdrawn',
                    ecg.end_date = CASE
                        WHEN ecg.start_date IS NOT NULL AND ? IS NOT NULL AND ? < ecg.start_date THEN ecg.start_date
                        ELSE ?
                    END
                WHERE ecg.id_student_user = ?
                  AND cg.id_course = ?
                  AND ecg.state = 'active'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setDate(statement, 1, endDate);
            setDate(statement, 2, endDate);
            setDate(statement, 3, endDate);
            statement.setLong(4, studentUserId);
            statement.setLong(5, courseId);
            statement.executeUpdate();
        }
    }

    public void deleteInCourse(
            Connection connection,
            long studentUserId,
            long courseId
    ) throws SQLException {
        String sql = """
                DELETE ecg
                FROM enroll_class_group ecg
                JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
                WHERE ecg.id_student_user = ?
                  AND cg.id_course = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            statement.executeUpdate();
        }
    }

    public void withdrawActiveInSubject(
            Connection connection,
            long studentUserId,
            long courseId,
            long subjectId,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                UPDATE enroll_class_group ecg
                JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
                SET ecg.state = 'withdrawn',
                    ecg.end_date = CASE
                        WHEN ecg.start_date IS NOT NULL AND ? IS NOT NULL AND ? < ecg.start_date THEN ecg.start_date
                        ELSE ?
                    END
                WHERE ecg.id_student_user = ?
                  AND cg.id_course = ?
                  AND cg.id_subject = ?
                  AND ecg.state = 'active'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setDate(statement, 1, endDate);
            setDate(statement, 2, endDate);
            setDate(statement, 3, endDate);
            statement.setLong(4, studentUserId);
            statement.setLong(5, courseId);
            statement.setLong(6, subjectId);
            statement.executeUpdate();
        }
    }

    public void deleteInSubject(
            Connection connection,
            long studentUserId,
            long courseId,
            long subjectId
    ) throws SQLException {
        String sql = """
                DELETE ecg
                FROM enroll_class_group ecg
                JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
                WHERE ecg.id_student_user = ?
                  AND cg.id_course = ?
                  AND cg.id_subject = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            statement.setLong(3, subjectId);
            statement.executeUpdate();
        }
    }

    public Optional<ClassGroupEnrollment> findEnrollment(
            Connection connection,
            long studentUserId,
            long classGroupId
    ) throws SQLException {
        String sql = """
                SELECT id_student_user, id_class_group, state, start_date, end_date
                FROM enroll_class_group
                WHERE id_student_user = ?
                  AND id_class_group = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapEnrollment(resultSet));
            }
        }
    }

    public Optional<ClassGroupEnrollment> findEnrollment(long studentUserId, long classGroupId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findEnrollment(connection, studentUserId, classGroupId);
        }
    }

    public List<ClassGroupEnrollment> findByStudent(long studentUserId) throws SQLException {
        String sql = """
                SELECT id_student_user, id_class_group, state, start_date, end_date
                FROM enroll_class_group
                WHERE id_student_user = ?
                ORDER BY start_date DESC, id_class_group
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ClassGroupEnrollment> enrollments = new ArrayList<>();
                while (resultSet.next()) {
                    enrollments.add(mapEnrollment(resultSet));
                }
                return enrollments;
            }
        }
    }

    public List<ClassGroupEnrollment> findByClassGroup(long classGroupId) throws SQLException {
        String sql = """
                SELECT id_student_user, id_class_group, state, start_date, end_date
                FROM enroll_class_group
                WHERE id_class_group = ?
                ORDER BY state, id_student_user
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ClassGroupEnrollment> enrollments = new ArrayList<>();
                while (resultSet.next()) {
                    enrollments.add(mapEnrollment(resultSet));
                }
                return enrollments;
            }
        }
    }

    public boolean hasOverlappingActiveEnrollment(
            Connection connection,
            long studentUserId,
            long courseId,
            long subjectId,
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM enroll_class_group ecg
                JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
                WHERE ecg.id_student_user = ?
                  AND cg.id_course = ?
                  AND cg.id_subject = ?
                  AND ecg.state = 'active'
                  AND (ecg.start_date IS NULL OR ? IS NULL OR ecg.start_date <= ?)
                  AND (ecg.end_date IS NULL OR ? IS NULL OR ecg.end_date >= ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            statement.setLong(3, subjectId);
            setDate(statement, 4, endDate);
            setDate(statement, 5, endDate);
            setDate(statement, 6, startDate);
            setDate(statement, 7, startDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean hasOpenEnrollmentInContext(
            Connection connection,
            long studentUserId,
            long courseId,
            long subjectId,
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM enroll_class_group ecg
                JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
                WHERE ecg.id_student_user = ?
                  AND cg.id_course = ?
                  AND cg.id_subject = ?
                  AND ecg.state IN ('active', 'pending')
                  AND (ecg.start_date IS NULL OR ? IS NULL OR ecg.start_date <= ?)
                  AND (ecg.end_date IS NULL OR ? IS NULL OR ecg.end_date >= ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            statement.setLong(3, subjectId);
            setDate(statement, 4, endDate);
            setDate(statement, 5, endDate);
            setDate(statement, 6, startDate);
            setDate(statement, 7, startDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean lockActiveSubjectEnrollmentCovering(
            Connection connection,
            long studentUserId,
            long courseId,
            long subjectId,
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                SELECT id_student_user
                FROM enroll_subject
                WHERE id_student_user = ?
                  AND id_course = ?
                  AND id_subject = ?
                  AND state = 'active'
                  AND (start_date IS NULL OR ? IS NULL OR start_date <= ?)
                  AND (? IS NOT NULL OR end_date IS NULL)
                  AND (? IS NULL OR end_date IS NULL OR end_date >= ?)
                LIMIT 1
                FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            statement.setLong(3, subjectId);
            setDate(statement, 4, startDate);
            setDate(statement, 5, startDate);
            setDate(statement, 6, endDate);
            setDate(statement, 7, endDate);
            setDate(statement, 8, endDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean hasActiveSubjectEnrollmentCovering(
            Connection connection,
            long studentUserId,
            long courseId,
            long subjectId,
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM enroll_subject
                WHERE id_student_user = ?
                  AND id_course = ?
                  AND id_subject = ?
                  AND state = 'active'
                  AND (start_date IS NULL OR ? IS NULL OR start_date <= ?)
                  AND (? IS NOT NULL OR end_date IS NULL)
                  AND (? IS NULL OR end_date IS NULL OR end_date >= ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            statement.setLong(3, subjectId);
            setDate(statement, 4, startDate);
            setDate(statement, 5, startDate);
            setDate(statement, 6, endDate);
            setDate(statement, 7, endDate);
            setDate(statement, 8, endDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private static ClassGroupEnrollment mapEnrollment(ResultSet resultSet) throws SQLException {
        Date start = resultSet.getDate("start_date");
        Date end = resultSet.getDate("end_date");
        return new ClassGroupEnrollment(
                resultSet.getLong("id_student_user"),
                resultSet.getLong("id_class_group"),
                EnrollmentState.fromDatabaseValue(resultSet.getString("state")),
                start == null ? null : start.toLocalDate(),
                end == null ? null : end.toLocalDate()
        );
    }

    private static void setDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(value));
        }
    }
}
