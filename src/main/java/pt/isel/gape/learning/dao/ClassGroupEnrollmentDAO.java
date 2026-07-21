package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.ClassGroupEnrollment;
import pt.isel.gape.learning.model.ClassGroupEnrollmentCommand;
import pt.isel.gape.learning.model.EnrollmentState;

public final class ClassGroupEnrollmentDAO implements pt.isel.gape.transversal.service.ApplicationReadService.ClassGroupEnrollments {

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
                        WHEN ? < ecg.start_date THEN ecg.start_date
                        WHEN ? > cg.ends_at THEN cg.ends_at
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

    public void withdrawActiveInCourseOccurrence(
            Connection connection,
            long studentUserId,
            long courseId,
            long courseOccurrenceId,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                UPDATE enroll_class_group ecg
                JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
                SET ecg.state = 'withdrawn',
                    ecg.end_date = CASE
                        WHEN ? < ecg.start_date THEN ecg.start_date
                        WHEN ? > cg.ends_at THEN cg.ends_at
                        ELSE ?
                    END
                WHERE ecg.id_student_user = ?
                  AND cg.id_course = ?
                  AND cg.id_course_occurrence = ?
                  AND ecg.state = 'active'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setDate(statement, 1, endDate);
            setDate(statement, 2, endDate);
            setDate(statement, 3, endDate);
            statement.setLong(4, studentUserId);
            statement.setLong(5, courseId);
            statement.setLong(6, courseOccurrenceId);
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

    public void deleteInCourseOccurrence(
            Connection connection,
            long studentUserId,
            long courseId,
            long courseOccurrenceId
    ) throws SQLException {
        String sql = """
                DELETE ecg
                FROM enroll_class_group ecg
                JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
                WHERE ecg.id_student_user = ?
                  AND cg.id_course = ?
                  AND cg.id_course_occurrence = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            statement.setLong(3, courseOccurrenceId);
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

    public List<ClassGroupEnrollment> findByClassGroups(Collection<Long> classGroupIds) throws SQLException {
        if (classGroupIds == null || classGroupIds.isEmpty()) {
            return List.of();
        }
        String sql = """
                SELECT id_student_user, id_class_group, state, start_date, end_date
                FROM enroll_class_group
                WHERE id_class_group IN (%s)
                ORDER BY id_class_group, state, id_student_user
                """.formatted(placeholders(classGroupIds.size()));

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Long classGroupId : classGroupIds) {
                statement.setLong(index++, classGroupId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ClassGroupEnrollment> enrollments = new ArrayList<>();
                while (resultSet.next()) {
                    enrollments.add(mapEnrollment(resultSet));
                }
                return enrollments;
            }
        }
    }

    public List<Long> findClassGroupIdsByStudentCourseOccurrence(
            Connection connection,
            long studentUserId,
            long courseId,
            long courseOccurrenceId
    ) throws SQLException {
        String sql = """
                SELECT ecg.id_class_group
                FROM enroll_class_group ecg
                JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
                WHERE ecg.id_student_user = ?
                  AND cg.id_course = ?
                  AND cg.id_course_occurrence = ?
                ORDER BY ecg.id_class_group
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            statement.setLong(3, courseOccurrenceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (resultSet.next()) {
                    ids.add(resultSet.getLong("id_class_group"));
                }
                return List.copyOf(ids);
            }
        }
    }

    public Map<Long, Integer> countPendingByClassGroupIds(Collection<Long> classGroupIds) throws SQLException {
        if (classGroupIds == null || classGroupIds.isEmpty()) {
            return Map.of();
        }
        String sql = """
                SELECT id_class_group, COUNT(*) AS pending_count
                FROM enroll_class_group
                WHERE state = 'pending'
                  AND id_class_group IN (%s)
                GROUP BY id_class_group
                """.formatted(placeholders(classGroupIds.size()));

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Long classGroupId : classGroupIds) {
                statement.setLong(index++, classGroupId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Long, Integer> counts = new LinkedHashMap<>();
                while (resultSet.next()) {
                    counts.put(resultSet.getLong("id_class_group"), resultSet.getInt("pending_count"));
                }
                return counts;
            }
        }
    }

    public boolean lockActiveCourseOccurrenceEnrollmentCovering(
            Connection connection,
            long studentUserId,
            long classGroupId,
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                SELECT ec.id_student_user
                FROM class_group cg
                JOIN enroll_course ec
                  ON ec.id_course = cg.id_course
                 AND ec.id_course_occurrence = cg.id_course_occurrence
                WHERE ec.id_student_user = ?
                  AND cg.id_class_group = ?
                  AND ec.state = 'active'
                  AND ec.start_date <= COALESCE(?, cg.starts_at)
                  AND ec.end_date >= COALESCE(?, cg.ends_at)
                LIMIT 1
                FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, classGroupId);
            setDate(statement, 3, startDate);
            setDate(statement, 4, endDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean hasActiveCourseOccurrenceEnrollmentCovering(
            Connection connection,
            long studentUserId,
            long classGroupId,
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM class_group cg
                JOIN enroll_course ec
                  ON ec.id_course = cg.id_course
                 AND ec.id_course_occurrence = cg.id_course_occurrence
                WHERE ec.id_student_user = ?
                  AND cg.id_class_group = ?
                  AND ec.state = 'active'
                  AND ec.start_date <= COALESCE(?, cg.starts_at)
                  AND ec.end_date >= COALESCE(?, cg.ends_at)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, classGroupId);
            setDate(statement, 3, startDate);
            setDate(statement, 4, endDate);
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

    private static String placeholders(int count) {
        return String.join(", ", java.util.Collections.nCopies(count, "?"));
    }
}
