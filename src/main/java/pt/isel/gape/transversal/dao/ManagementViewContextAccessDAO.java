package pt.isel.gape.transversal.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.common.config.ConnectionProvider;

/**
 * Read-only authorization queries for contextual management-view reports.
 *
 * <p>The checks deliberately receive the effective date as an argument rather
 * than depending on {@code CURRENT_DATE}. This keeps access decisions
 * deterministic in tests and prevents a stale active enrollment from exposing
 * reports after its configured period has ended.</p>
 */
public final class ManagementViewContextAccessDAO {

    private final ConnectionProvider connectionProvider;

    public ManagementViewContextAccessDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
    }

    public boolean hasCurrentStudentCourseAccess(long studentUserId, long courseId, LocalDate effectiveDate)
            throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return hasCurrentStudentCourseAccess(connection, studentUserId, courseId, effectiveDate);
        }
    }

    public boolean hasCurrentStudentCourseAccess(
            Connection connection,
            long studentUserId,
            long courseId,
            LocalDate effectiveDate
    ) throws SQLException {
        Objects.requireNonNull(connection, "connection is required");
        Objects.requireNonNull(effectiveDate, "effectiveDate is required");
        String sql = """
                SELECT COUNT(*)
                FROM enroll_course enrollment
                JOIN student_profile student ON student.id_user = enrollment.id_student_user
                JOIN user_account user_row ON user_row.id_user = student.id_user
                JOIN course course_row ON course_row.id_course = enrollment.id_course
                WHERE enrollment.id_student_user = ?
                  AND enrollment.id_course = ?
                  AND enrollment.state = 'active'
                  AND enrollment.start_date <= ?
                  AND enrollment.end_date >= ?
                  AND user_row.state = 'active'
                  AND course_row.state = 'active'
                """;
        return exists(connection, sql, studentUserId, courseId, effectiveDate);
    }

    public boolean hasCurrentStudentSubjectAccess(long studentUserId, long subjectId, LocalDate effectiveDate)
            throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return hasCurrentStudentSubjectAccess(connection, studentUserId, subjectId, effectiveDate);
        }
    }

    public boolean hasCurrentStudentSubjectAccess(
            Connection connection,
            long studentUserId,
            long subjectId,
            LocalDate effectiveDate
    ) throws SQLException {
        Objects.requireNonNull(connection, "connection is required");
        Objects.requireNonNull(effectiveDate, "effectiveDate is required");
        String sql = """
                SELECT COUNT(*)
                FROM enroll_class_group enrollment
                JOIN student_profile student ON student.id_user = enrollment.id_student_user
                JOIN user_account user_row ON user_row.id_user = student.id_user
                JOIN class_group class_group_row ON class_group_row.id_class_group = enrollment.id_class_group
                JOIN enroll_course course_enrollment
                  ON course_enrollment.id_student_user = enrollment.id_student_user
                 AND course_enrollment.id_course = class_group_row.id_course
                 AND course_enrollment.id_course_occurrence = class_group_row.id_course_occurrence
                JOIN course course_row ON course_row.id_course = class_group_row.id_course
                JOIN subject subject_row ON subject_row.id_subject = class_group_row.id_subject
                WHERE enrollment.id_student_user = ?
                  AND class_group_row.id_subject = ?
                  AND enrollment.state = 'active'
                  AND enrollment.start_date <= ?
                  AND enrollment.end_date >= ?
                  AND course_enrollment.state = 'active'
                  AND course_enrollment.start_date <= ?
                  AND course_enrollment.end_date >= ?
                  AND class_group_row.state = 'active'
                  AND course_row.state = 'active'
                  AND subject_row.state = 'active'
                  AND user_row.state = 'active'
                """;
        return existsWithCourseEnrollment(connection, sql, studentUserId, subjectId, effectiveDate);
    }

    public boolean hasCurrentStudentClassGroupAccess(
            long studentUserId,
            long classGroupId,
            LocalDate effectiveDate
    ) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return hasCurrentStudentClassGroupAccess(connection, studentUserId, classGroupId, effectiveDate);
        }
    }

    public boolean hasCurrentStudentClassGroupAccess(
            Connection connection,
            long studentUserId,
            long classGroupId,
            LocalDate effectiveDate
    ) throws SQLException {
        Objects.requireNonNull(connection, "connection is required");
        Objects.requireNonNull(effectiveDate, "effectiveDate is required");
        String sql = """
                SELECT COUNT(*)
                FROM enroll_class_group enrollment
                JOIN student_profile student ON student.id_user = enrollment.id_student_user
                JOIN user_account user_row ON user_row.id_user = student.id_user
                JOIN class_group class_group_row ON class_group_row.id_class_group = enrollment.id_class_group
                JOIN enroll_course course_enrollment
                  ON course_enrollment.id_student_user = enrollment.id_student_user
                 AND course_enrollment.id_course = class_group_row.id_course
                 AND course_enrollment.id_course_occurrence = class_group_row.id_course_occurrence
                JOIN course course_row ON course_row.id_course = class_group_row.id_course
                JOIN subject subject_row ON subject_row.id_subject = class_group_row.id_subject
                WHERE enrollment.id_student_user = ?
                  AND enrollment.id_class_group = ?
                  AND enrollment.state = 'active'
                  AND enrollment.start_date <= ?
                  AND enrollment.end_date >= ?
                  AND course_enrollment.state = 'active'
                  AND course_enrollment.start_date <= ?
                  AND course_enrollment.end_date >= ?
                  AND class_group_row.state = 'active'
                  AND course_row.state = 'active'
                  AND subject_row.state = 'active'
                  AND user_row.state = 'active'
                """;
        return existsWithCourseEnrollment(connection, sql, studentUserId, classGroupId, effectiveDate);
    }

    /**
     * Returns the class groups that remain in the student's current report
     * context.  The course enrollment is joined deliberately: a stale class
     * group enrollment cannot survive as an authorization source after the
     * encompassing course occurrence enrollment has ended or been withdrawn.
     */
    public Set<Long> findCurrentStudentClassGroupIds(long studentUserId, LocalDate effectiveDate)
            throws SQLException {
        Objects.requireNonNull(effectiveDate, "effectiveDate is required");
        String sql = """
                SELECT DISTINCT class_group_row.id_class_group
                FROM enroll_class_group enrollment
                JOIN class_group class_group_row ON class_group_row.id_class_group = enrollment.id_class_group
                JOIN enroll_course course_enrollment
                  ON course_enrollment.id_student_user = enrollment.id_student_user
                 AND course_enrollment.id_course = class_group_row.id_course
                 AND course_enrollment.id_course_occurrence = class_group_row.id_course_occurrence
                JOIN student_profile student ON student.id_user = enrollment.id_student_user
                JOIN user_account user_row ON user_row.id_user = student.id_user
                JOIN course course_row ON course_row.id_course = class_group_row.id_course
                JOIN subject subject_row ON subject_row.id_subject = class_group_row.id_subject
                WHERE enrollment.id_student_user = ?
                  AND enrollment.state = 'active'
                  AND enrollment.start_date <= ?
                  AND enrollment.end_date >= ?
                  AND course_enrollment.state = 'active'
                  AND course_enrollment.start_date <= ?
                  AND course_enrollment.end_date >= ?
                  AND class_group_row.state = 'active'
                  AND course_row.state = 'active'
                  AND subject_row.state = 'active'
                  AND user_row.state = 'active'
                ORDER BY class_group_row.id_class_group
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setDate(2, Date.valueOf(effectiveDate));
            statement.setDate(3, Date.valueOf(effectiveDate));
            statement.setDate(4, Date.valueOf(effectiveDate));
            statement.setDate(5, Date.valueOf(effectiveDate));
            try (ResultSet resultSet = statement.executeQuery()) {
                Set<Long> classGroupIds = new LinkedHashSet<>();
                while (resultSet.next()) {
                    classGroupIds.add(resultSet.getLong("id_class_group"));
                }
                return Set.copyOf(classGroupIds);
            }
        }
    }

    private static boolean exists(
            Connection connection,
            String sql,
            long studentUserId,
            long contextId,
            LocalDate effectiveDate
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, contextId);
            statement.setDate(3, Date.valueOf(effectiveDate));
            statement.setDate(4, Date.valueOf(effectiveDate));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    private static boolean existsWithCourseEnrollment(
            Connection connection,
            String sql,
            long studentUserId,
            long contextId,
            LocalDate effectiveDate
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, contextId);
            statement.setDate(3, Date.valueOf(effectiveDate));
            statement.setDate(4, Date.valueOf(effectiveDate));
            statement.setDate(5, Date.valueOf(effectiveDate));
            statement.setDate(6, Date.valueOf(effectiveDate));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }
}
