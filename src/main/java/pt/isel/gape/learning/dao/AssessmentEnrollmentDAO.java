package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.AssessmentEnrollment;
import pt.isel.gape.learning.model.AssessmentEnrollmentCommand;
import pt.isel.gape.learning.model.EnrollmentState;

public final class AssessmentEnrollmentDAO {

    private final ConnectionProvider connectionProvider;

    public AssessmentEnrollmentDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public void enroll(Connection connection, AssessmentEnrollmentCommand command) throws SQLException {
        save(connection, command, EnrollmentState.ACTIVE);
    }

    public void request(Connection connection, AssessmentEnrollmentCommand command) throws SQLException {
        save(connection, command, EnrollmentState.PENDING);
    }

    private void save(
            Connection connection,
            AssessmentEnrollmentCommand command,
            EnrollmentState state
    ) throws SQLException {
        String sql = """
                INSERT INTO enroll_assessment (id_student_user, id_assessment, state)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, command.studentUserId());
            statement.setLong(2, command.assessmentId());
            statement.setString(3, state.toDatabaseValue());
            statement.executeUpdate();
        }
    }

    public void reactivateRequest(
            Connection connection,
            AssessmentEnrollmentCommand command,
            EnrollmentState newState
    ) throws SQLException {
        String sql = """
                UPDATE enroll_assessment
                SET state = ?
                WHERE id_student_user = ?
                  AND id_assessment = ?
                  AND state IN ('inactive', 'rejected', 'withdrawn', 'completed')
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newState.toDatabaseValue());
            statement.setLong(2, command.studentUserId());
            statement.setLong(3, command.assessmentId());
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Reusable assessment enrollment not found");
            }
        }
    }

    public void updateState(
            Connection connection,
            long studentUserId,
            long assessmentId,
            EnrollmentState expectedState,
            EnrollmentState newState
    ) throws SQLException {
        String sql = """
                UPDATE enroll_assessment
                SET state = ?
                WHERE id_student_user = ?
                  AND id_assessment = ?
                  AND state = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newState.toDatabaseValue());
            statement.setLong(2, studentUserId);
            statement.setLong(3, assessmentId);
            statement.setString(4, expectedState.toDatabaseValue());
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Expected assessment enrollment state not found");
            }
        }
    }

    public void updateEnrollment(
            Connection connection,
            long studentUserId,
            long assessmentId,
            EnrollmentState state
    ) throws SQLException {
        String sql = """
                UPDATE enroll_assessment
                SET state = ?
                WHERE id_student_user = ?
                  AND id_assessment = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            statement.setLong(2, studentUserId);
            statement.setLong(3, assessmentId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Assessment enrollment not found");
            }
        }
    }

    public void withdraw(Connection connection, long studentUserId, long assessmentId)
            throws SQLException {
        String sql = """
                UPDATE enroll_assessment
                SET state = 'withdrawn'
                WHERE id_student_user = ?
                  AND id_assessment = ?
                  AND state = 'active'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, assessmentId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Active assessment enrollment not found");
            }
        }
    }

    public void deleteEnrollment(Connection connection, long studentUserId, long assessmentId)
            throws SQLException {
        String sql = """
                DELETE FROM enroll_assessment
                WHERE id_student_user = ?
                  AND id_assessment = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, assessmentId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Assessment enrollment not found");
            }
        }
    }

    public Optional<AssessmentEnrollment> findEnrollment(
            Connection connection,
            long studentUserId,
            long assessmentId
    ) throws SQLException {
        String sql = """
                SELECT id_student_user, id_assessment, state
                FROM enroll_assessment
                WHERE id_student_user = ?
                  AND id_assessment = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapEnrollment(resultSet));
            }
        }
    }

    public Optional<AssessmentEnrollment> findEnrollment(long studentUserId, long assessmentId)
            throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findEnrollment(connection, studentUserId, assessmentId);
        }
    }

    public List<AssessmentEnrollment> findByAssessment(long assessmentId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findByAssessment(connection, assessmentId);
        }
    }

    public List<AssessmentEnrollment> findByAssessment(Connection connection, long assessmentId)
            throws SQLException {
        String sql = """
                SELECT id_student_user, id_assessment, state
                FROM enroll_assessment
                WHERE id_assessment = ?
                ORDER BY state, id_student_user
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AssessmentEnrollment> enrollments = new ArrayList<>();
                while (resultSet.next()) {
                    enrollments.add(mapEnrollment(resultSet));
                }
                return enrollments;
            }
        }
    }

    public List<Long> findEligibleStudentIds(Connection connection, long assessmentId) throws SQLException {
        String sql = """
                SELECT DISTINCT eligible.id_student_user
                FROM (
                    SELECT ecg.id_student_user
                    FROM assessment a
                    JOIN content_block cb ON cb.id_content_block = a.id_content_block
                    JOIN class_group cg ON cg.id_class_group = cb.id_class_group
                    JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group
                    JOIN user_account u ON u.id_user = ecg.id_student_user
                    WHERE a.id_assessment = ?
                      AND ecg.state = 'active'
                      AND u.state = 'active'
                      AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                      AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                    UNION
                    SELECT ecg.id_student_user
                    FROM assessment a
                    JOIN assessment_class_group acg ON acg.id_assessment = a.id_assessment
                    JOIN class_group cg ON cg.id_class_group = acg.id_class_group
                    JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group
                    JOIN enroll_subject es
                      ON es.id_student_user = ecg.id_student_user
                     AND es.id_subject = cg.id_subject
                     AND es.id_course = cg.id_course
                    JOIN user_account u ON u.id_user = ecg.id_student_user
                    WHERE a.id_assessment = ?
                      AND a.id_content_block IS NULL
                      AND a.id_subject IS NOT NULL
                      AND cg.id_subject = a.id_subject
                      AND cg.state = 'active'
                      AND ecg.state = 'active'
                      AND es.state = 'active'
                      AND u.state = 'active'
                      AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                      AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                      AND (es.start_date IS NULL OR es.start_date <= CURRENT_DATE)
                      AND (es.end_date IS NULL OR es.end_date >= CURRENT_DATE)
                ) eligible
                ORDER BY eligible.id_student_user
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            statement.setLong(2, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Long> studentIds = new ArrayList<>();
                while (resultSet.next()) {
                    studentIds.add(resultSet.getLong("id_student_user"));
                }
                return studentIds;
            }
        }
    }

    public List<Long> findEligibleStudentIds(long assessmentId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findEligibleStudentIds(connection, assessmentId);
        }
    }

    public boolean isStudentEligible(Connection connection, long studentUserId, long assessmentId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM (
                    SELECT ecg.id_student_user
                    FROM assessment a
                    JOIN content_block cb ON cb.id_content_block = a.id_content_block
                    JOIN class_group cg ON cg.id_class_group = cb.id_class_group
                    JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group
                    JOIN user_account u ON u.id_user = ecg.id_student_user
                    WHERE a.id_assessment = ?
                      AND ecg.id_student_user = ?
                      AND ecg.state = 'active'
                      AND u.state = 'active'
                      AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                      AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                    UNION
                    SELECT ecg.id_student_user
                    FROM assessment a
                    JOIN assessment_class_group acg ON acg.id_assessment = a.id_assessment
                    JOIN class_group cg ON cg.id_class_group = acg.id_class_group
                    JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group
                    JOIN enroll_subject es
                      ON es.id_student_user = ecg.id_student_user
                     AND es.id_subject = cg.id_subject
                     AND es.id_course = cg.id_course
                    JOIN user_account u ON u.id_user = ecg.id_student_user
                    WHERE a.id_assessment = ?
                      AND a.id_content_block IS NULL
                      AND a.id_subject IS NOT NULL
                      AND ecg.id_student_user = ?
                      AND cg.id_subject = a.id_subject
                      AND cg.state = 'active'
                      AND ecg.state = 'active'
                      AND es.state = 'active'
                      AND u.state = 'active'
                      AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                      AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                      AND (es.start_date IS NULL OR es.start_date <= CURRENT_DATE)
                      AND (es.end_date IS NULL OR es.end_date >= CURRENT_DATE)
                ) eligible
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            statement.setLong(2, studentUserId);
            statement.setLong(3, assessmentId);
            statement.setLong(4, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public int syncAutomaticEnrollments(Connection connection, long assessmentId)
            throws SQLException {
        String sql = """
                INSERT INTO enroll_assessment (id_student_user, id_assessment, state)
                SELECT eligible.id_student_user, ?, 'active'
                FROM (
                    SELECT ecg.id_student_user
                    FROM assessment a
                    JOIN content_block cb ON cb.id_content_block = a.id_content_block
                    JOIN class_group cg ON cg.id_class_group = cb.id_class_group
                    JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group
                    WHERE a.id_assessment = ?
                      AND a.enrollment_mode = 'auto_approve'
                      AND ecg.state = 'active'
                      AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                      AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                    UNION
                    SELECT ecg.id_student_user
                    FROM assessment a
                    JOIN assessment_class_group acg ON acg.id_assessment = a.id_assessment
                    JOIN class_group cg ON cg.id_class_group = acg.id_class_group
                    JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group
                    JOIN enroll_subject es
                      ON es.id_student_user = ecg.id_student_user
                     AND es.id_subject = cg.id_subject
                     AND es.id_course = cg.id_course
                    WHERE a.id_assessment = ?
                      AND a.id_content_block IS NULL
                      AND a.id_subject IS NOT NULL
                      AND a.enrollment_mode = 'auto_approve'
                      AND cg.id_subject = a.id_subject
                      AND cg.state = 'active'
                      AND ecg.state = 'active'
                      AND es.state = 'active'
                      AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                      AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                      AND (es.start_date IS NULL OR es.start_date <= CURRENT_DATE)
                      AND (es.end_date IS NULL OR es.end_date >= CURRENT_DATE)
                ) eligible
                ON DUPLICATE KEY UPDATE
                    state = CASE
                        WHEN state IN ('pending', 'inactive', 'rejected', 'withdrawn', 'completed') THEN 'active'
                        ELSE state
                    END
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            statement.setLong(2, assessmentId);
            statement.setLong(3, assessmentId);
            return statement.executeUpdate();
        }
    }

    public int syncAutomaticEnrollments(long assessmentId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return syncAutomaticEnrollments(connection, assessmentId);
        }
    }

    public int syncAllAutomaticEnrollments(Connection connection) throws SQLException {
        String sql = """
                INSERT INTO enroll_assessment (id_student_user, id_assessment, state)
                SELECT eligible.id_student_user, eligible.id_assessment, 'active'
                FROM (
                    SELECT ecg.id_student_user, a.id_assessment
                    FROM assessment a
                    JOIN content_block cb ON cb.id_content_block = a.id_content_block
                    JOIN class_group cg ON cg.id_class_group = cb.id_class_group
                    JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group
                    JOIN user_account u ON u.id_user = ecg.id_student_user
                    WHERE a.enrollment_mode = 'auto_approve'
                      AND a.state <> 'completed'
                      AND ecg.state = 'active'
                      AND u.state = 'active'
                      AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                      AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                    UNION
                    SELECT ecg.id_student_user, a.id_assessment
                    FROM assessment a
                    JOIN assessment_class_group acg ON acg.id_assessment = a.id_assessment
                    JOIN class_group cg ON cg.id_class_group = acg.id_class_group
                    JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group
                    JOIN enroll_subject es
                      ON es.id_student_user = ecg.id_student_user
                     AND es.id_subject = cg.id_subject
                     AND es.id_course = cg.id_course
                    JOIN user_account u ON u.id_user = ecg.id_student_user
                    WHERE a.id_content_block IS NULL
                      AND a.id_subject IS NOT NULL
                      AND a.enrollment_mode = 'auto_approve'
                      AND a.state <> 'completed'
                      AND cg.id_subject = a.id_subject
                      AND cg.state = 'active'
                      AND ecg.state = 'active'
                      AND es.state = 'active'
                      AND u.state = 'active'
                      AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                      AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                      AND (es.start_date IS NULL OR es.start_date <= CURRENT_DATE)
                      AND (es.end_date IS NULL OR es.end_date >= CURRENT_DATE)
                ) eligible
                ON DUPLICATE KEY UPDATE
                    state = CASE
                        WHEN state IN ('pending', 'inactive', 'rejected', 'withdrawn', 'completed') THEN 'active'
                        ELSE state
                    END
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        }
    }

    public int syncAllAutomaticEnrollments() throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return syncAllAutomaticEnrollments(connection);
        }
    }

    private static AssessmentEnrollment mapEnrollment(ResultSet resultSet) throws SQLException {
        return new AssessmentEnrollment(
                resultSet.getLong("id_student_user"),
                resultSet.getLong("id_assessment"),
                EnrollmentState.fromDatabaseValue(resultSet.getString("state"))
        );
    }
}
