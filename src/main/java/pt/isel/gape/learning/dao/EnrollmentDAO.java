package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.CourseEnrollmentCommand;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.SubjectEnrollment;
import pt.isel.gape.learning.model.SubjectEnrollmentCommand;

public final class EnrollmentDAO {

    private final ConnectionProvider connectionProvider;

    public EnrollmentDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public void enrollCourse(Connection connection, CourseEnrollmentCommand command) throws SQLException {
        String sql = """
                INSERT INTO enroll_course (id_student_user, id_course, state, start_date, end_date)
                VALUES (?, ?, 'active', ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, command.studentUserId());
            statement.setLong(2, command.courseId());
            setDate(statement, 3, command.startDate());
            setDate(statement, 4, command.endDate());
            statement.executeUpdate();
        }
    }

    public void withdrawCourse(
            Connection connection,
            long studentUserId,
            long courseId,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                UPDATE enroll_course
                SET state = 'withdrawn', end_date = ?
                WHERE id_student_user = ?
                  AND id_course = ?
                  AND state = 'active'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setDate(statement, 1, endDate);
            statement.setLong(2, studentUserId);
            statement.setLong(3, courseId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Active course enrollment not found");
            }
        }
    }

    public void withdrawActiveSubjectsInCourse(
            Connection connection,
            long studentUserId,
            long courseId,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                UPDATE enroll_subject
                SET state = 'withdrawn',
                    end_date = CASE
                        WHEN start_date IS NOT NULL AND ? IS NOT NULL AND ? < start_date THEN start_date
                        ELSE ?
                    END
                WHERE id_student_user = ?
                  AND id_course = ?
                  AND state = 'active'
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

    public Optional<CourseEnrollment> findCourseEnrollment(
            Connection connection,
            long studentUserId,
            long courseId
    ) throws SQLException {
        String sql = """
                SELECT id_student_user, id_course, state, start_date, end_date
                FROM enroll_course
                WHERE id_student_user = ?
                  AND id_course = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapCourseEnrollment(resultSet));
            }
        }
    }

    public Optional<CourseEnrollment> findCourseEnrollment(long studentUserId, long courseId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findCourseEnrollment(connection, studentUserId, courseId);
        }
    }

    public List<CourseEnrollment> findCourseEnrollmentsByStudent(long studentUserId) throws SQLException {
        String sql = """
                SELECT id_student_user, id_course, state, start_date, end_date
                FROM enroll_course
                WHERE id_student_user = ?
                ORDER BY start_date DESC, id_course
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CourseEnrollment> enrollments = new ArrayList<>();
                while (resultSet.next()) {
                    enrollments.add(mapCourseEnrollment(resultSet));
                }
                return enrollments;
            }
        }
    }

    public Set<Long> findActiveCourseIdsByStudent(long studentUserId) throws SQLException {
        String sql = """
                SELECT id_course
                FROM enroll_course
                WHERE id_student_user = ?
                  AND state = 'active'
                  AND (start_date IS NULL OR start_date <= CURRENT_DATE)
                  AND (end_date IS NULL OR end_date >= CURRENT_DATE)
                ORDER BY id_course
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                Set<Long> courseIds = new LinkedHashSet<>();
                while (resultSet.next()) {
                    courseIds.add(resultSet.getLong("id_course"));
                }
                return Set.copyOf(courseIds);
            }
        }
    }

    public boolean hasOverlappingActiveCourseEnrollment(
            Connection connection,
            long studentUserId,
            long courseId,
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM enroll_course
                WHERE id_student_user = ?
                  AND id_course = ?
                  AND state = 'active'
                  AND (start_date IS NULL OR ? IS NULL OR start_date <= ?)
                  AND (end_date IS NULL OR ? IS NULL OR end_date >= ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            setDate(statement, 3, endDate);
            setDate(statement, 4, endDate);
            setDate(statement, 5, startDate);
            setDate(statement, 6, startDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean hasActiveCourseEnrollment(
            Connection connection,
            long studentUserId,
            long courseId,
            LocalDate onDate
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM enroll_course
                WHERE id_student_user = ?
                  AND id_course = ?
                  AND state = 'active'
                  AND (start_date IS NULL OR start_date <= ?)
                  AND (end_date IS NULL OR end_date >= ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            setDate(statement, 3, onDate);
            setDate(statement, 4, onDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean hasActiveCourseEnrollmentCovering(
            Connection connection,
            long studentUserId,
            long courseId,
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM enroll_course
                WHERE id_student_user = ?
                  AND id_course = ?
                  AND state = 'active'
                  AND (start_date IS NULL OR ? IS NULL OR start_date <= ?)
                  AND (? IS NOT NULL OR end_date IS NULL)
                  AND (? IS NULL OR end_date IS NULL OR end_date >= ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            setDate(statement, 3, startDate);
            setDate(statement, 4, startDate);
            setDate(statement, 5, endDate);
            setDate(statement, 6, endDate);
            setDate(statement, 7, endDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public void enrollSubject(Connection connection, SubjectEnrollmentCommand command) throws SQLException {
        String sql = """
                INSERT INTO enroll_subject (id_student_user, id_course, id_subject, state, start_date, end_date)
                VALUES (?, ?, ?, 'active', ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, command.studentUserId());
            statement.setLong(2, requireCourseId(command.courseId()));
            statement.setLong(3, command.subjectId());
            setDate(statement, 4, command.startDate());
            setDate(statement, 5, command.endDate());
            statement.executeUpdate();
        }
    }

    public void withdrawSubject(
            Connection connection,
            long studentUserId,
            long courseId,
            long subjectId,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                UPDATE enroll_subject
                SET state = 'withdrawn', end_date = ?
                WHERE id_student_user = ?
                  AND id_course = ?
                  AND id_subject = ?
                  AND state = 'active'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setDate(statement, 1, endDate);
            statement.setLong(2, studentUserId);
            statement.setLong(3, courseId);
            statement.setLong(4, subjectId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Active subject enrollment not found");
            }
        }
    }

    public Optional<SubjectEnrollment> findSubjectEnrollment(
            Connection connection,
            long studentUserId,
            long courseId,
            long subjectId
    ) throws SQLException {
        String sql = """
                SELECT id_student_user, id_course, id_subject, state, start_date, end_date
                FROM enroll_subject
                WHERE id_student_user = ?
                  AND id_course = ?
                  AND id_subject = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            statement.setLong(3, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapSubjectEnrollment(resultSet));
            }
        }
    }

    public Optional<SubjectEnrollment> findSubjectEnrollment(
            long studentUserId,
            long courseId,
            long subjectId
    ) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findSubjectEnrollment(connection, studentUserId, courseId, subjectId);
        }
    }

    public List<SubjectEnrollment> findSubjectEnrollmentsByStudent(long studentUserId) throws SQLException {
        String sql = """
                SELECT id_student_user, id_course, id_subject, state, start_date, end_date
                FROM enroll_subject
                WHERE id_student_user = ?
                ORDER BY start_date DESC, id_course, id_subject
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SubjectEnrollment> enrollments = new ArrayList<>();
                while (resultSet.next()) {
                    enrollments.add(mapSubjectEnrollment(resultSet));
                }
                return enrollments;
            }
        }
    }

    public List<SubjectEnrollment> findActiveSubjectEnrollmentsByStudent(long studentUserId) throws SQLException {
        String sql = """
                SELECT id_student_user, id_course, id_subject, state, start_date, end_date
                FROM enroll_subject
                WHERE id_student_user = ?
                  AND state = 'active'
                  AND (start_date IS NULL OR start_date <= CURRENT_DATE)
                  AND (end_date IS NULL OR end_date >= CURRENT_DATE)
                ORDER BY id_course, id_subject
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SubjectEnrollment> enrollments = new ArrayList<>();
                while (resultSet.next()) {
                    enrollments.add(mapSubjectEnrollment(resultSet));
                }
                return enrollments;
            }
        }
    }

    public boolean hasOverlappingActiveSubjectEnrollment(
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
                  AND id_subject = ?
                  AND state = 'active'
                  AND (start_date IS NULL OR ? IS NULL OR start_date <= ?)
                  AND (end_date IS NULL OR ? IS NULL OR end_date >= ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, subjectId);
            setDate(statement, 3, endDate);
            setDate(statement, 4, endDate);
            setDate(statement, 5, startDate);
            setDate(statement, 6, startDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean activeStudentExists(Connection connection, long studentUserId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM student_profile sp
                JOIN user_account u ON u.id_user = sp.id_user
                WHERE sp.id_user = ?
                  AND u.state = 'active'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private static CourseEnrollment mapCourseEnrollment(ResultSet resultSet) throws SQLException {
        Date start = resultSet.getDate("start_date");
        Date end = resultSet.getDate("end_date");
        return new CourseEnrollment(
                resultSet.getLong("id_student_user"),
                resultSet.getLong("id_course"),
                EnrollmentState.fromDatabaseValue(resultSet.getString("state")),
                start == null ? null : start.toLocalDate(),
                end == null ? null : end.toLocalDate()
        );
    }

    private static SubjectEnrollment mapSubjectEnrollment(ResultSet resultSet) throws SQLException {
        Date start = resultSet.getDate("start_date");
        Date end = resultSet.getDate("end_date");
        return new SubjectEnrollment(
                resultSet.getLong("id_student_user"),
                resultSet.getLong("id_course"),
                resultSet.getLong("id_subject"),
                EnrollmentState.fromDatabaseValue(resultSet.getString("state")),
                start == null ? null : start.toLocalDate(),
                end == null ? null : end.toLocalDate()
        );
    }

    private static long requireCourseId(Long courseId) {
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("Subject enrollment course is required");
        }
        return courseId;
    }

    private static void setDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(value));
        }
    }
}
