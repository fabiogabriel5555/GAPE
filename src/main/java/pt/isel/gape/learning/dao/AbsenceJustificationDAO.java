package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.AbsenceJustification;
import pt.isel.gape.learning.model.AbsenceJustificationCreateCommand;
import pt.isel.gape.learning.model.AbsenceJustificationState;

public final class AbsenceJustificationDAO {

    private final ConnectionProvider connectionProvider;

    public AbsenceJustificationDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(
            Connection connection,
            long studentSubmitterUserId,
            AbsenceJustificationCreateCommand command
    ) throws SQLException {
        String sql = """
                INSERT INTO absence_justification (
                    id_attendance_record, id_user_student_submitter, id_user_processor,
                    submitted_at, reason, attachment, processed_at, decision_notes, state
                ) VALUES (?, ?, NULL, ?, ?, ?, NULL, NULL, 'submitted')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, command.attendanceRecordId());
            statement.setLong(2, studentSubmitterUserId);
            statement.setTimestamp(3, Timestamp.valueOf(command.submittedAt()));
            statement.setString(4, command.reason().trim());
            setNullableString(statement, 5, command.attachment());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating absence justification failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<AbsenceJustification> findById(long justificationId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, justificationId);
        }
    }

    public Optional<AbsenceJustification> findById(Connection connection, long justificationId) throws SQLException {
        String sql = selectJustificationSql() + " WHERE id_absence_justification = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, justificationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapJustification(resultSet));
            }
        }
    }

    public Optional<AbsenceJustification> lockById(Connection connection, long justificationId) throws SQLException {
        String sql = selectJustificationSql() + " WHERE id_absence_justification = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, justificationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapJustification(resultSet));
            }
        }
    }

    public Optional<AbsenceJustification> findByAttendanceRecord(
            Connection connection,
            long attendanceRecordId
    ) throws SQLException {
        String sql = selectJustificationSql() + " WHERE id_attendance_record = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, attendanceRecordId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapJustification(resultSet));
            }
        }
    }

    public List<AbsenceJustification> findByStudent(Connection connection, long studentUserId) throws SQLException {
        String sql = selectJustificationSql() + " WHERE id_user_student_submitter = ? ORDER BY submitted_at DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapJustifications(resultSet);
            }
        }
    }

    public List<AbsenceJustification> findVisibleForTeacher(Connection connection, long teacherUserId)
            throws SQLException {
        String sql = """
                SELECT aj.id_absence_justification, aj.id_attendance_record, aj.id_user_student_submitter,
                       aj.id_user_processor, aj.submitted_at, aj.reason, aj.attachment, aj.processed_at,
                       aj.decision_notes, aj.state
                FROM absence_justification aj
                JOIN attendance_record ar ON ar.id_attendance_record = aj.id_attendance_record
                JOIN lesson l ON l.id_lesson = ar.id_lesson
                WHERE EXISTS (
                    SELECT 1
                    FROM teach_class_group tcg
                    JOIN grant_teacher gt ON gt.id_teacher_user = tcg.id_teacher_user
                    JOIN permission p ON p.cod_permission = gt.cod_permission
                    JOIN user_account u ON u.id_user = tcg.id_teacher_user
                    JOIN class_group cg ON cg.id_class_group = tcg.id_class_group
                    WHERE tcg.id_class_group = l.id_class_group
                      AND tcg.id_teacher_user = ?
                      AND tcg.state = 'active'
                      AND gt.cod_permission = 'MANAGE_LEARNING'
                      AND p.state = 'active'
                      AND u.state = 'active'
                      AND cg.state = 'active'
                      AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                      AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                )
                ORDER BY aj.submitted_at DESC, aj.id_absence_justification DESC
                """;
        return findVisible(connection, sql, teacherUserId);
    }

    public List<AbsenceJustification> findVisibleForCoordinator(Connection connection, long coordinatorUserId)
            throws SQLException {
        String sql = """
                SELECT aj.id_absence_justification, aj.id_attendance_record, aj.id_user_student_submitter,
                       aj.id_user_processor, aj.submitted_at, aj.reason, aj.attachment, aj.processed_at,
                       aj.decision_notes, aj.state
                FROM absence_justification aj
                JOIN attendance_record ar ON ar.id_attendance_record = aj.id_attendance_record
                JOIN lesson l ON l.id_lesson = ar.id_lesson
                WHERE EXISTS (
                    SELECT 1
                    FROM class_group cg
                    JOIN coordinate_subject cs ON cs.id_subject = cg.id_subject
                    JOIN grant_coordinator gc ON gc.id_coordinator_user = cs.id_coordinator_user
                    JOIN permission p ON p.cod_permission = gc.cod_permission
                    JOIN user_account u ON u.id_user = cs.id_coordinator_user
                    WHERE cg.id_class_group = l.id_class_group
                      AND cs.id_coordinator_user = ?
                      AND cs.state = 'active'
                      AND gc.cod_permission = 'MANAGE_LEARNING'
                      AND p.state = 'active'
                      AND u.state = 'active'
                      AND cg.state = 'active'
                )
                ORDER BY aj.submitted_at DESC, aj.id_absence_justification DESC
                """;
        return findVisible(connection, sql, coordinatorUserId);
    }

    public List<AbsenceJustification> findVisibleForAdministrator(Connection connection, long administratorUserId)
            throws SQLException {
        String sql = """
                SELECT aj.id_absence_justification, aj.id_attendance_record, aj.id_user_student_submitter,
                       aj.id_user_processor, aj.submitted_at, aj.reason, aj.attachment, aj.processed_at,
                       aj.decision_notes, aj.state
                FROM absence_justification aj
                JOIN attendance_record ar ON ar.id_attendance_record = aj.id_attendance_record
                JOIN lesson l ON l.id_lesson = ar.id_lesson
                JOIN class_group cg ON cg.id_class_group = l.id_class_group
                JOIN course c ON c.id_course = cg.id_course
                JOIN subject s ON s.id_subject = cg.id_subject
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                WHERE EXISTS (
                    SELECT 1
                    FROM grant_administrator ga
                    JOIN permission p ON p.cod_permission = ga.cod_permission
                    JOIN user_account u ON u.id_user = ga.id_admin_user
                    WHERE ga.id_admin_user = ?
                      AND p.state = 'active'
                      AND u.state = 'active'
                      AND (
                          (ga.cod_permission = 'MANAGE_ALL' AND ga.context_type = 'GLOBAL')
                          OR (
                              ga.cod_permission = 'MANAGE_LEARNING'
                              AND (
                                  (ga.context_type = 'CLASS_GROUP' AND ga.context_id = cg.id_class_group)
                                  OR (ga.context_type = 'SUBJECT' AND ga.context_id = cg.id_subject)
                                  OR (ga.context_type = 'COURSE' AND ga.context_id = cg.id_course)
                                  OR (ga.context_type = 'ORGANIZATION'
                                      AND ga.context_id IN (c.id_organization, s.id_organization))
                                  OR (ga.context_type = 'ORGANIC_UNIT'
                                      AND (ga.context_id = c.id_organic_unit
                                           OR ga.context_id = ou.parent_organic_unit_id))
                              )
                          )
                      )
                )
                ORDER BY aj.submitted_at DESC, aj.id_absence_justification DESC
                """;
        return findVisible(connection, sql, administratorUserId);
    }

    public void process(
            Connection connection,
            long justificationId,
            long processorUserId,
            AbsenceJustificationState decision,
            LocalDateTime processedAt,
            String decisionNotes
    ) throws SQLException {
        String sql = """
                UPDATE absence_justification
                SET id_user_processor = ?, processed_at = ?, decision_notes = ?, state = ?
                WHERE id_absence_justification = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, processorUserId);
            statement.setTimestamp(2, Timestamp.valueOf(processedAt));
            setNullableString(statement, 3, decisionNotes);
            statement.setString(4, decision.toDatabaseValue());
            statement.setLong(5, justificationId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Absence justification not found: " + justificationId);
            }
        }
    }

    private static String selectJustificationSql() {
        return """
                SELECT id_absence_justification, id_attendance_record, id_user_student_submitter,
                       id_user_processor, submitted_at, reason, attachment, processed_at,
                       decision_notes, state
                FROM absence_justification
                """;
    }

    private static List<AbsenceJustification> findVisible(Connection connection, String sql, long userId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapJustifications(resultSet);
            }
        }
    }

    private static List<AbsenceJustification> mapJustifications(ResultSet resultSet) throws SQLException {
        List<AbsenceJustification> justifications = new ArrayList<>();
        while (resultSet.next()) {
            justifications.add(mapJustification(resultSet));
        }
        return justifications;
    }

    private static AbsenceJustification mapJustification(ResultSet resultSet) throws SQLException {
        return new AbsenceJustification(
                resultSet.getLong("id_absence_justification"),
                resultSet.getLong("id_attendance_record"),
                resultSet.getLong("id_user_student_submitter"),
                nullableLong(resultSet, "id_user_processor"),
                resultSet.getTimestamp("submitted_at").toLocalDateTime(),
                resultSet.getString("reason"),
                resultSet.getString("attachment"),
                getTimestamp(resultSet, "processed_at"),
                resultSet.getString("decision_notes"),
                AbsenceJustificationState.fromDatabaseValue(resultSet.getString("state"))
        );
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static LocalDateTime getTimestamp(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }
}
