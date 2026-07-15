package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.AttendanceRecord;
import pt.isel.gape.learning.model.AttendanceRecordCommand;
import pt.isel.gape.learning.model.AttendanceSource;
import pt.isel.gape.learning.model.AttendanceState;
import pt.isel.gape.learning.model.AttendanceStatus;

public final class AttendanceRecordDAO implements pt.isel.gape.transversal.service.ApplicationReadService.AttendanceRecords {

    private final ConnectionProvider connectionProvider;

    public AttendanceRecordDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, AttendanceRecordCommand command) throws SQLException {
        String sql = """
                INSERT INTO attendance_record (
                    id_lesson, id_user_student, status, source, check_in, check_out, notes, state
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, command.lessonId());
            statement.setLong(2, command.studentUserId());
            statement.setString(3, command.status().toDatabaseValue());
            statement.setString(4, command.source().toDatabaseValue());
            setTimestamp(statement, 5, command.checkIn());
            setTimestamp(statement, 6, command.checkOut());
            setNullableString(statement, 7, command.notes());
            statement.setString(8, command.state().toDatabaseValue());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating attendance record failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<AttendanceRecord> findById(long recordId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, recordId);
        }
    }

    public Optional<AttendanceRecord> findById(Connection connection, long recordId) throws SQLException {
        String sql = selectAttendanceSql() + " WHERE id_attendance_record = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, recordId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapAttendance(resultSet));
            }
        }
    }

    public Optional<AttendanceRecord> lockById(Connection connection, long recordId) throws SQLException {
        String sql = selectAttendanceSql() + " WHERE id_attendance_record = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, recordId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapAttendance(resultSet));
            }
        }
    }

    public Optional<AttendanceRecord> findByLessonAndStudent(
            Connection connection,
            long lessonId,
            long studentUserId
    ) throws SQLException {
        String sql = selectAttendanceSql() + " WHERE id_lesson = ? AND id_user_student = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, lessonId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapAttendance(resultSet));
            }
        }
    }

    public List<AttendanceRecord> findByLesson(Connection connection, long lessonId) throws SQLException {
        String sql = selectAttendanceSql() + " WHERE id_lesson = ? ORDER BY id_user_student";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, lessonId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAttendanceRecords(resultSet);
            }
        }
    }

    public List<AttendanceRecord> findByStudent(Connection connection, long studentUserId) throws SQLException {
        String sql = selectAttendanceSql() + " WHERE id_user_student = ? ORDER BY id_lesson";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAttendanceRecords(resultSet);
            }
        }
    }

    public List<AttendanceRecord> findVisibleForTeacher(Connection connection, long teacherUserId)
            throws SQLException {
        String sql = """
                SELECT ar.id_attendance_record, ar.id_lesson, ar.id_user_student, ar.status, ar.source,
                       ar.check_in, ar.check_out, ar.notes, ar.state
                FROM attendance_record ar
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
                ORDER BY l.starts_at DESC, ar.id_lesson, ar.id_user_student
                """;
        return findVisible(connection, sql, teacherUserId);
    }

    public List<AttendanceRecord> findVisibleForCoordinator(Connection connection, long coordinatorUserId)
            throws SQLException {
        String sql = """
                SELECT ar.id_attendance_record, ar.id_lesson, ar.id_user_student, ar.status, ar.source,
                       ar.check_in, ar.check_out, ar.notes, ar.state
                FROM attendance_record ar
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
                ORDER BY l.starts_at DESC, ar.id_lesson, ar.id_user_student
                """;
        return findVisible(connection, sql, coordinatorUserId);
    }

    public List<AttendanceRecord> findVisibleForAdministrator(Connection connection, long administratorUserId)
            throws SQLException {
        String sql = """
                SELECT ar.id_attendance_record, ar.id_lesson, ar.id_user_student, ar.status, ar.source,
                       ar.check_in, ar.check_out, ar.notes, ar.state
                FROM attendance_record ar
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
                ORDER BY l.starts_at DESC, ar.id_lesson, ar.id_user_student
                """;
        return findVisible(connection, sql, administratorUserId);
    }

    public void updateStatus(
            Connection connection,
            long recordId,
            AttendanceStatus status,
            AttendanceState state
    ) throws SQLException {
        String sql = """
                UPDATE attendance_record
                SET status = ?, state = ?
                WHERE id_attendance_record = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.toDatabaseValue());
            statement.setString(2, state.toDatabaseValue());
            statement.setLong(3, recordId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Attendance record not found: " + recordId);
            }
        }
    }

    public boolean hasActiveRecord(Connection connection, long lessonId, long studentUserId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM attendance_record
                WHERE id_lesson = ?
                  AND id_user_student = ?
                  AND state = 'active'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, lessonId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    private static String selectAttendanceSql() {
        return """
                SELECT id_attendance_record, id_lesson, id_user_student, status, source,
                       check_in, check_out, notes, state
                FROM attendance_record
                """;
    }

    private static List<AttendanceRecord> mapAttendanceRecords(ResultSet resultSet) throws SQLException {
        List<AttendanceRecord> records = new ArrayList<>();
        while (resultSet.next()) {
            records.add(mapAttendance(resultSet));
        }
        return records;
    }

    private static List<AttendanceRecord> findVisible(Connection connection, String sql, long userId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAttendanceRecords(resultSet);
            }
        }
    }

    private static AttendanceRecord mapAttendance(ResultSet resultSet) throws SQLException {
        LocalDateTime checkIn = getTimestamp(resultSet, "check_in");
        LocalDateTime checkOut = getTimestamp(resultSet, "check_out");
        return new AttendanceRecord(
                resultSet.getLong("id_attendance_record"),
                resultSet.getLong("id_lesson"),
                resultSet.getLong("id_user_student"),
                AttendanceStatus.fromDatabaseValue(resultSet.getString("status")),
                AttendanceSource.fromDatabaseValue(resultSet.getString("source")),
                checkIn,
                checkOut,
                resultSet.getString("notes"),
                AttendanceState.fromDatabaseValue(resultSet.getString("state")),
                permanenceMinutes(checkIn, checkOut)
        );
    }

    public static long permanenceMinutes(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkIn == null || checkOut == null) {
            return 0L;
        }
        return Duration.between(checkIn, checkOut).toMinutes();
    }

    private static LocalDateTime getTimestamp(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static void setTimestamp(PreparedStatement statement, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }
}
