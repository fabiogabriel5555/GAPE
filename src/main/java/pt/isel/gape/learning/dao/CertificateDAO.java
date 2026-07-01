package pt.isel.gape.learning.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.Certificate;
import pt.isel.gape.learning.model.CertificateState;
import pt.isel.gape.learning.model.CertificateType;

public final class CertificateDAO {

    private final ConnectionProvider connectionProvider;

    public CertificateDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long createDraftIfAbsent(
            Connection connection,
            long courseId,
            long studentUserId,
            String title
    ) throws SQLException {
        Optional<Certificate> existing = findByCourseAndStudent(connection, courseId, studentUserId);
        if (existing.isPresent()) {
            return existing.get().id();
        }
        String sql = """
                INSERT INTO certificate (
                    id_course, id_user_student, title, notes, type, template,
                    validation_code, issued_at, state, revoked_at, final_grade
                ) VALUES (?, ?, ?, NULL, ?, NULL, NULL, NULL, ?, NULL, NULL)
                ON DUPLICATE KEY UPDATE id_certificate = LAST_INSERT_ID(id_certificate)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, courseId);
            statement.setLong(2, studentUserId);
            statement.setString(3, title.trim());
            statement.setString(4, CertificateType.COMPLETION.toDatabaseValue());
            statement.setString(5, CertificateState.DRAFT.toDatabaseValue());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating certificate failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public void updateIssued(
            Connection connection,
            long certificateId,
            String title,
            String notes,
            CertificateType type,
            String template,
            String validationCode,
            LocalDateTime issuedAt,
            BigDecimal finalGrade
    ) throws SQLException {
        String sql = """
                UPDATE certificate
                SET title = ?, notes = ?, type = ?, template = ?,
                    validation_code = ?, issued_at = ?, state = ?, revoked_at = NULL, final_grade = ?
                WHERE id_certificate = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title.trim());
            setNullableString(statement, 2, notes);
            statement.setString(3, type.toDatabaseValue());
            setNullableString(statement, 4, template);
            statement.setString(5, validationCode);
            setNullableTimestamp(statement, 6, issuedAt);
            statement.setString(7, issuedAt == null || finalGrade == null
                    ? CertificateState.DRAFT.toDatabaseValue()
                    : CertificateState.ISSUED.toDatabaseValue());
            setNullableBigDecimal(statement, 8, finalGrade);
            statement.setLong(9, certificateId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Certificate not found: " + certificateId);
            }
        }
    }

    public void updateDraft(
            Connection connection,
            long certificateId,
            String title,
            String notes,
            CertificateType type,
            String template
    ) throws SQLException {
        String sql = """
                UPDATE certificate
                SET title = ?, notes = ?, type = ?, template = ?,
                    validation_code = NULL, issued_at = NULL, state = ?, revoked_at = NULL, final_grade = NULL
                WHERE id_certificate = ?
                  AND state <> ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title.trim());
            setNullableString(statement, 2, notes);
            statement.setString(3, type.toDatabaseValue());
            setNullableString(statement, 4, template);
            statement.setString(5, CertificateState.DRAFT.toDatabaseValue());
            statement.setLong(6, certificateId);
            statement.setString(7, CertificateState.REVOKED.toDatabaseValue());
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Certificate not found or revoked: " + certificateId);
            }
        }
    }

    public void revoke(
            Connection connection,
            long certificateId,
            LocalDateTime revokedAt
    ) throws SQLException {
        String sql = """
                UPDATE certificate
                SET state = ?, revoked_at = ?
                WHERE id_certificate = ?
                  AND state = ?
                  AND validation_code IS NOT NULL
                  AND issued_at IS NOT NULL
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, CertificateState.REVOKED.toDatabaseValue());
            statement.setTimestamp(2, Timestamp.valueOf(revokedAt));
            statement.setLong(3, certificateId);
            statement.setString(4, CertificateState.ISSUED.toDatabaseValue());
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Issued certificate not found: " + certificateId);
            }
        }
    }

    public Optional<Certificate> findById(long certificateId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, certificateId);
        }
    }

    public Optional<Certificate> findById(Connection connection, long certificateId) throws SQLException {
        String sql = selectCertificateSql() + " WHERE id_certificate = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, certificateId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapCertificate(connection, resultSet));
            }
        }
    }

    public Optional<Certificate> lockById(Connection connection, long certificateId) throws SQLException {
        String sql = selectCertificateSql() + " WHERE id_certificate = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, certificateId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapCertificate(connection, resultSet));
            }
        }
    }

    public Optional<Certificate> findByCourseAndStudent(
            Connection connection,
            long courseId,
            long studentUserId
    ) throws SQLException {
        String sql = selectCertificateSql() + """
                WHERE id_course = ?
                  AND id_user_student = ?
                  AND state <> 'revoked'
                ORDER BY issued_at DESC, id_certificate DESC
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapCertificate(connection, resultSet));
            }
        }
    }

    public Optional<Certificate> findIssuedByValidationCode(Connection connection, String validationCode)
            throws SQLException {
        String sql = selectCertificateSql() + """
                WHERE validation_code = ?
                  AND state = 'issued'
                  AND issued_at IS NOT NULL
                  AND final_grade IS NOT NULL
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, validationCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapCertificate(connection, resultSet));
            }
        }
    }

    public boolean validationCodeExists(Connection connection, String validationCode) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM certificate
                WHERE validation_code = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, validationCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean validationCodeExistsForAnother(
            Connection connection,
            String validationCode,
            long certificateId
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM certificate
                WHERE validation_code = ?
                  AND id_certificate <> ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, validationCode);
            statement.setLong(2, certificateId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public void replaceGradeSheets(
            Connection connection,
            long certificateId,
            List<Long> gradeSheetIds
    ) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM based_on_grade_sheet_certificate WHERE id_certificate = ?")) {
            delete.setLong(1, certificateId);
            delete.executeUpdate();
        }
        if (gradeSheetIds == null || gradeSheetIds.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO based_on_grade_sheet_certificate (id_certificate, id_grade_sheet)
                VALUES (?, ?)
                """;
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            for (Long gradeSheetId : orderedUnique(gradeSheetIds)) {
                insert.setLong(1, certificateId);
                insert.setLong(2, gradeSheetId);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    public List<Long> findGradeSheetIds(Connection connection, long certificateId) throws SQLException {
        String sql = """
                SELECT id_grade_sheet
                FROM based_on_grade_sheet_certificate
                WHERE id_certificate = ?
                ORDER BY id_grade_sheet
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, certificateId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (resultSet.next()) {
                    ids.add(resultSet.getLong("id_grade_sheet"));
                }
                return List.copyOf(ids);
            }
        }
    }

    public List<Certificate> findByStudent(Connection connection, long studentUserId) throws SQLException {
        String sql = selectCertificateSql() + """
                WHERE id_user_student = ?
                ORDER BY issued_at DESC, id_certificate DESC
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Certificate> certificates = new ArrayList<>();
                while (resultSet.next()) {
                    certificates.add(mapCertificate(connection, resultSet));
                }
                return List.copyOf(certificates);
            }
        }
    }

    public List<Certificate> findAll() throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            String sql = selectCertificateSql() + " ORDER BY issued_at DESC, id_certificate DESC";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                List<Certificate> certificates = new ArrayList<>();
                while (resultSet.next()) {
                    certificates.add(mapCertificate(connection, resultSet));
                }
                return List.copyOf(certificates);
            }
        }
    }

    public CourseScale findCourseScale(Connection connection, long courseId) throws SQLException {
        String sql = """
                SELECT ects, certificate_max_grade
                FROM course
                WHERE id_course = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Course not found: " + courseId);
                }
                return new CourseScale(
                        resultSet.getBigDecimal("ects"),
                        resultSet.getBigDecimal("certificate_max_grade")
                );
            }
        }
    }

    public String findCourseName(Connection connection, long courseId) throws SQLException {
        String sql = """
                SELECT name
                FROM course
                WHERE id_course = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Course not found: " + courseId);
                }
                return resultSet.getString("name");
            }
        }
    }

    public List<CourseSubjectScale> findActiveCourseSubjects(Connection connection, long courseId)
            throws SQLException {
        String sql = """
                SELECT s.id_subject, s.ects, s.final_grade_max
                FROM integrate_subject isub
                JOIN subject s ON s.id_subject = isub.id_subject
                WHERE isub.id_course = ?
                  AND isub.state = 'active'
                  AND s.state = 'active'
                ORDER BY isub.curricular_year, isub.term, s.id_subject
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CourseSubjectScale> subjects = new ArrayList<>();
                while (resultSet.next()) {
                    subjects.add(new CourseSubjectScale(
                            resultSet.getLong("id_subject"),
                            resultSet.getBigDecimal("ects"),
                            resultSet.getBigDecimal("final_grade_max")
                    ));
                }
                return List.copyOf(subjects);
            }
        }
    }

    public List<SubjectApprovedGrade> findApprovedSubjectGradeCandidates(
            Connection connection,
            long courseId,
            long subjectId,
            long studentUserId
    ) throws SQLException {
        String sql = """
                SELECT gs.id_grade_sheet, gs.max_grade, gr.value
                FROM grade_record gr
                JOIN grade_sheet gs ON gs.id_grade_sheet = gr.id_grade_sheet
                WHERE gr.id_user_student = ?
                  AND gr.result = 'approved'
                  AND gr.state = 'published'
                  AND gs.id_subject = ?
                  AND gs.state IN ('published', 'closed')
                  AND (
                        EXISTS (
                            SELECT 1
                            FROM associate_grade_sheet_class_group agscg
                            JOIN class_group cg ON cg.id_class_group = agscg.id_class_group
                            WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                              AND cg.id_course = ?
                              AND cg.id_subject = ?
                        )
                        OR NOT EXISTS (
                            SELECT 1
                            FROM associate_grade_sheet_class_group agscg
                            WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                        )
                  )
                ORDER BY gr.recorded_at DESC, gr.id_grade_record DESC
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, subjectId);
            statement.setLong(3, courseId);
            statement.setLong(4, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SubjectApprovedGrade> grades = new ArrayList<>();
                while (resultSet.next()) {
                    grades.add(new SubjectApprovedGrade(
                            resultSet.getLong("id_grade_sheet"),
                            resultSet.getBigDecimal("max_grade"),
                            resultSet.getBigDecimal("value")
                    ));
                }
                return List.copyOf(grades);
            }
        }
    }

    private static String selectCertificateSql() {
        return """
                SELECT id_certificate, id_course, id_user_student, title, notes, type, template,
                       validation_code, issued_at, state, revoked_at, final_grade
                FROM certificate
                """;
    }

    private Certificate mapCertificate(Connection connection, ResultSet resultSet) throws SQLException {
        long id = resultSet.getLong("id_certificate");
        return new Certificate(
                id,
                resultSet.getLong("id_course"),
                resultSet.getLong("id_user_student"),
                resultSet.getString("title"),
                resultSet.getString("notes"),
                CertificateType.fromDatabaseValue(resultSet.getString("type")),
                resultSet.getString("template"),
                resultSet.getString("validation_code"),
                getTimestamp(resultSet, "issued_at"),
                CertificateState.fromDatabaseValue(resultSet.getString("state")),
                getTimestamp(resultSet, "revoked_at"),
                resultSet.getBigDecimal("final_grade"),
                findGradeSheetIds(connection, id)
        );
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

    private static void setNullableBigDecimal(PreparedStatement statement, int index, BigDecimal value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DECIMAL);
        } else {
            statement.setBigDecimal(index, value);
        }
    }

    private static void setNullableTimestamp(PreparedStatement statement, int index, LocalDateTime value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    private static List<Long> orderedUnique(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long value : values) {
            if (value == null || value <= 0) {
                throw new IllegalArgumentException("Grade sheet ids must be positive");
            }
            unique.add(value);
        }
        return List.copyOf(unique);
    }

    public record CourseScale(BigDecimal ects, BigDecimal certificateMaxGrade) {
    }

    public record CourseSubjectScale(long subjectId, BigDecimal ects, BigDecimal finalGradeMax) {
    }

    public record SubjectApprovedGrade(long gradeSheetId, BigDecimal gradeSheetMaxGrade, BigDecimal value) {
    }
}
