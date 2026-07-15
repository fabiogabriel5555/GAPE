package pt.isel.gape.learning.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.GradeRecord;
import pt.isel.gape.learning.model.GradeRecordResult;
import pt.isel.gape.learning.model.GradeRecordState;

public final class GradeRecordDAO {

    private final ConnectionProvider connectionProvider;

    public GradeRecordDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long upsertAutomatic(
            Connection connection,
            long gradeSheetId,
            long studentUserId,
            BigDecimal value,
            GradeRecordResult result,
            LocalDateTime recordedAt
    ) throws SQLException {
        return upsertAutomatic(
                connection,
                gradeSheetId,
                studentUserId,
                value,
                result,
                recordedAt,
                "Automatically calculated from assessment scores and weights."
        );
    }

    public long upsertAutomatic(
            Connection connection,
            long gradeSheetId,
            long studentUserId,
            BigDecimal value,
            GradeRecordResult result,
            LocalDateTime recordedAt,
            String notes
    ) throws SQLException {
        String code = automaticCode(gradeSheetId, studentUserId);
        String sql = """
                INSERT INTO grade_record (
                    id_grade_sheet, id_user_student, id_attempt, cod_grade_record,
                    value, result, state, recorded_at, notes
                ) VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    id_attempt = NULL,
                    cod_grade_record = VALUES(cod_grade_record),
                    value = VALUES(value),
                    result = VALUES(result),
                    state = VALUES(state),
                    recorded_at = VALUES(recorded_at),
                    notes = VALUES(notes)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, gradeSheetId);
            statement.setLong(2, studentUserId);
            statement.setString(3, code);
            statement.setBigDecimal(4, value);
            statement.setString(5, result.toDatabaseValue());
            statement.setString(6, GradeRecordState.PUBLISHED.toDatabaseValue());
            statement.setTimestamp(7, Timestamp.valueOf(recordedAt));
            statement.setString(8, notes);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        }
        return findActiveBySheetAndStudent(connection, gradeSheetId, studentUserId)
                .map(GradeRecord::id)
                .orElseThrow(() -> new SQLException("Automatic grade record was not persisted"));
    }

    public void deactivateActiveBySheetAndStudent(
            Connection connection,
            long gradeSheetId,
            long studentUserId
    ) throws SQLException {
        deactivateActiveBySheetAndStudent(
                connection,
                gradeSheetId,
                studentUserId,
                "Marked inactive because the automatic final grade is pending assessment scores."
        );
    }

    public void deactivateActiveBySheetAndStudent(
            Connection connection,
            long gradeSheetId,
            long studentUserId,
            String notes
    ) throws SQLException {
        String sql = """
                UPDATE grade_record
                SET state = ?, notes = ?
                WHERE id_grade_sheet = ?
                  AND id_user_student = ?
                  AND state IN ('draft', 'published', 'corrected')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, GradeRecordState.INACTIVE.toDatabaseValue());
            statement.setString(2, notes);
            statement.setLong(3, gradeSheetId);
            statement.setLong(4, studentUserId);
            statement.executeUpdate();
        }
    }

    public Optional<GradeRecord> findById(long gradeRecordId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, gradeRecordId);
        }
    }

    public Optional<GradeRecord> findById(Connection connection, long gradeRecordId) throws SQLException {
        String sql = selectGradeRecordSql() + " WHERE id_grade_record = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gradeRecordId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapGradeRecord(resultSet));
            }
        }
    }

    public Optional<GradeRecord> lockById(Connection connection, long gradeRecordId) throws SQLException {
        String sql = selectGradeRecordSql() + " WHERE id_grade_record = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gradeRecordId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapGradeRecord(resultSet));
            }
        }
    }

    public Optional<GradeRecord> findActiveBySheetAndStudent(
            Connection connection,
            long gradeSheetId,
            long studentUserId
    ) throws SQLException {
        String sql = selectGradeRecordSql() + """
                WHERE id_grade_sheet = ?
                  AND id_user_student = ?
                  AND state IN ('draft', 'published', 'corrected')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gradeSheetId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapGradeRecord(resultSet));
            }
        }
    }

    public Optional<GradeRecord> findActiveBySheetAndStudent(long gradeSheetId, long studentUserId)
            throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findActiveBySheetAndStudent(connection, gradeSheetId, studentUserId);
        }
    }

    public List<GradeRecord> findByStudent(Connection connection, long studentUserId) throws SQLException {
        String sql = selectGradeRecordSql() + """
                WHERE id_user_student = ?
                  AND state IN ('draft', 'published', 'corrected')
                ORDER BY recorded_at DESC, id_grade_record DESC
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapGradeRecords(resultSet);
            }
        }
    }

    public List<GradeRecord> findByGradeSheet(Connection connection, long gradeSheetId) throws SQLException {
        String sql = selectGradeRecordSql() + """
                WHERE id_grade_sheet = ?
                  AND state IN ('draft', 'published', 'corrected')
                ORDER BY recorded_at DESC, id_grade_record DESC
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gradeSheetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapGradeRecords(resultSet);
            }
        }
    }

    public List<GradeRecord> findByGradeSheet(long gradeSheetId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findByGradeSheet(connection, gradeSheetId);
        }
    }

    public List<GradeRecord> findVisiblePublishedByStudent(Connection connection, long studentUserId)
            throws SQLException {
        String sql = """
                SELECT gr.id_grade_record, gr.id_grade_sheet, gr.id_user_student, gr.id_attempt,
                       gr.cod_grade_record, gr.value, gr.result, gr.state, gr.recorded_at, gr.notes
                FROM grade_record gr
                JOIN grade_sheet gs ON gs.id_grade_sheet = gr.id_grade_sheet
                WHERE gr.id_user_student = ?
                  AND gs.state IN ('published', 'closed')
                  AND gr.state IN ('draft', 'published', 'corrected')
                ORDER BY gr.recorded_at DESC, gr.id_grade_record DESC
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapGradeRecords(resultSet);
            }
        }
    }

    public boolean hasActiveRecord(Connection connection, long gradeSheetId, long studentUserId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM grade_record
                WHERE id_grade_sheet = ?
                  AND id_user_student = ?
                  AND state IN ('draft', 'published', 'corrected')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gradeSheetId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean hasApprovedActiveRecord(Connection connection, long gradeSheetId, long studentUserId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM grade_record
                WHERE id_grade_sheet = ?
                  AND id_user_student = ?
                  AND result = 'approved'
                  AND state IN ('draft', 'published', 'corrected')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gradeSheetId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean studentBelongsToGradeSheetContext(
            Connection connection,
            long gradeSheetId,
            long studentUserId
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM grade_sheet gs
                WHERE gs.id_grade_sheet = ?
                  AND (
                        (
                            EXISTS (
                                SELECT 1
                                FROM associate_grade_sheet_class_group agscg
                                WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                            )
                            AND EXISTS (
                                SELECT 1
                                FROM associate_grade_sheet_class_group agscg
                                JOIN enroll_class_group ecg ON ecg.id_class_group = agscg.id_class_group
                                JOIN class_group cg ON cg.id_class_group = agscg.id_class_group
                                JOIN course c ON c.id_course = cg.id_course
                                JOIN subject s ON s.id_subject = cg.id_subject
                                WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                                  AND ecg.id_student_user = ?
                                  AND ecg.state = 'active'
                                  AND cg.state = 'active'
                                  AND c.state = 'active'
                                  AND s.state = 'active'
                                  AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                                  AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                            )
                        )
                        OR (
                            NOT EXISTS (
                                SELECT 1
                                FROM associate_grade_sheet_class_group agscg
                                WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                            )
                            AND EXISTS (
                                SELECT 1
                                FROM class_group cg
                                JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group
                                JOIN course c ON c.id_course = cg.id_course
                                JOIN subject s ON s.id_subject = gs.id_subject
                                WHERE ecg.id_student_user = ?
                                  AND cg.id_subject = gs.id_subject
                                  AND cg.id_course_occurrence = gs.id_course_occurrence
                                  AND ecg.state = 'active'
                                  AND cg.state = 'active'
                                  AND c.state = 'active'
                                  AND s.state = 'active'
                                  AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                                  AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                            )
                        )
                  )
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gradeSheetId);
            statement.setLong(2, studentUserId);
            statement.setLong(3, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public Optional<BigDecimal> findApprovedValue(
            Connection connection,
            long gradeSheetId,
            long studentUserId
    ) throws SQLException {
        String sql = """
                SELECT value
                FROM grade_record
                WHERE id_grade_sheet = ?
                  AND id_user_student = ?
                  AND result = 'approved'
                  AND state IN ('draft', 'published', 'corrected')
                ORDER BY recorded_at DESC, id_grade_record DESC
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gradeSheetId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(resultSet.getBigDecimal("value"));
            }
        }
    }

    private static String selectGradeRecordSql() {
        return """
                SELECT id_grade_record, id_grade_sheet, id_user_student, id_attempt,
                       cod_grade_record, value, result, state, recorded_at, notes
                FROM grade_record
                """;
    }

    private static List<GradeRecord> mapGradeRecords(ResultSet resultSet) throws SQLException {
        List<GradeRecord> records = new ArrayList<>();
        while (resultSet.next()) {
            records.add(mapGradeRecord(resultSet));
        }
        return records;
    }

    private static GradeRecord mapGradeRecord(ResultSet resultSet) throws SQLException {
        return new GradeRecord(
                resultSet.getLong("id_grade_record"),
                resultSet.getLong("id_grade_sheet"),
                resultSet.getLong("id_user_student"),
                nullableLong(resultSet, "id_attempt"),
                resultSet.getString("cod_grade_record"),
                resultSet.getBigDecimal("value"),
                GradeRecordResult.fromDatabaseValue(resultSet.getString("result")),
                GradeRecordState.fromDatabaseValue(resultSet.getString("state")),
                resultSet.getTimestamp("recorded_at").toLocalDateTime(),
                resultSet.getString("notes")
        );
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static String automaticCode(long gradeSheetId, long studentUserId) {
        String code = "AUTO-" + gradeSheetId + "-" + studentUserId;
        if (code.length() <= 30) {
            return code;
        }
        return "A-" + Long.toString(gradeSheetId, 36).toUpperCase()
                + "-" + Long.toString(studentUserId, 36).toUpperCase();
    }
}
