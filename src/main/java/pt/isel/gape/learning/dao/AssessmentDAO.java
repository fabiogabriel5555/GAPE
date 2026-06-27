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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AssessmentCorrectionMode;
import pt.isel.gape.learning.model.AssessmentCreateCommand;
import pt.isel.gape.learning.model.AssessmentMode;
import pt.isel.gape.learning.model.AssessmentState;
import pt.isel.gape.learning.model.AssessmentType;
import pt.isel.gape.learning.model.AssessmentUpdateCommand;
import pt.isel.gape.learning.model.EnrollmentApprovalMode;

public final class AssessmentDAO {

    private final ConnectionProvider connectionProvider;

    public AssessmentDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, AssessmentCreateCommand command) throws SQLException {
        String sql = """
                INSERT INTO assessment (
                    id_subject, id_content_block, title, description, type, mode, correction_mode,
                    max_grade, passing_grade, attempts_limit, enrollment_mode, state, available_from, available_until
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setStatementValues(statement, command);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating assessment failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<Assessment> findById(long assessmentId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, assessmentId);
        }
    }

    public List<Assessment> findAll() throws SQLException {
        String sql = selectAssessmentSql() + " ORDER BY id_assessment DESC";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapAssessments(resultSet);
        }
    }

    public Optional<Assessment> findById(Connection connection, long assessmentId) throws SQLException {
        String sql = selectAssessmentSql() + " WHERE id_assessment = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapAssessment(resultSet));
            }
        }
    }

    public Optional<Assessment> lockById(Connection connection, long assessmentId) throws SQLException {
        String sql = selectAssessmentSql() + " WHERE id_assessment = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapAssessment(resultSet));
            }
        }
    }

    public List<Assessment> findByContentBlock(long contentBlockId) throws SQLException {
        String sql = selectAssessmentSql() + " WHERE id_content_block = ? ORDER BY COALESCE(order_no, 2147483647), available_from, id_assessment";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contentBlockId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAssessments(resultSet);
            }
        }
    }

    public List<Assessment> findByContentBlock(Connection connection, long contentBlockId) throws SQLException {
        String sql = selectAssessmentSql() + " WHERE id_content_block = ? ORDER BY COALESCE(order_no, 2147483647), available_from, id_assessment";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contentBlockId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAssessments(resultSet);
            }
        }
    }

    public List<Assessment> findBySubject(long subjectId) throws SQLException {
        String sql = selectAssessmentSql() + " WHERE id_subject = ? ORDER BY available_from, id_assessment";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAssessments(resultSet);
            }
        }
    }

    public List<Assessment> findActiveAccessibleByStudent(long studentUserId) throws SQLException {
        String sql = selectAssessmentSql() + """
                WHERE state IN ('active', 'scheduled')
                  AND mode = 'online'
                  AND available_from IS NOT NULL
                  AND available_from <= CURRENT_TIMESTAMP
                  AND (available_until IS NULL OR available_until > CURRENT_TIMESTAMP)
                  AND (
                        (
                            id_content_block IS NOT NULL
                            AND EXISTS (
                                SELECT 1
                                FROM content_block cb
                                JOIN class_group cg ON cg.id_class_group = cb.id_class_group
                                JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group
                                JOIN course c ON c.id_course = cg.id_course
                                JOIN subject s ON s.id_subject = cg.id_subject
                                WHERE cb.id_content_block = assessment.id_content_block
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
                            id_content_block IS NULL
                            AND id_subject IS NOT NULL
                            AND EXISTS (
                                SELECT 1
                                FROM assessment_class_group acg
                                JOIN class_group cg ON cg.id_class_group = acg.id_class_group
                                JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group
                                JOIN enroll_subject es
                                  ON es.id_student_user = ecg.id_student_user
                                 AND es.id_subject = cg.id_subject
                                 AND es.id_course = cg.id_course
                                JOIN subject s ON s.id_subject = cg.id_subject
                                JOIN course c ON c.id_course = cg.id_course
                                WHERE acg.id_assessment = assessment.id_assessment
                                  AND ecg.id_student_user = ?
                                  AND ecg.state = 'active'
                                  AND cg.state = 'active'
                                  AND es.state = 'active'
                                  AND cg.id_subject = assessment.id_subject
                                  AND s.state = 'active'
                                  AND c.state = 'active'
                                  AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                                  AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                                  AND (es.start_date IS NULL OR es.start_date <= CURRENT_DATE)
                                  AND (es.end_date IS NULL OR es.end_date >= CURRENT_DATE)
                            )
                        )
                  )
                ORDER BY available_from, id_assessment
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAssessments(resultSet);
            }
        }
    }

    public void update(Connection connection, long assessmentId, AssessmentUpdateCommand command)
            throws SQLException {
        String sql = """
                UPDATE assessment
                SET id_subject = ?, id_content_block = ?, title = ?, description = ?, type = ?,
                    mode = ?, correction_mode = ?, max_grade = ?, passing_grade = ?,
                    attempts_limit = ?, enrollment_mode = ?, state = ?, available_from = ?, available_until = ?
                WHERE id_assessment = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setStatementValues(statement, command);
            statement.setLong(15, assessmentId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Assessment not found: " + assessmentId);
            }
        }
    }

    public void updateEnrollmentMode(Connection connection, long assessmentId, EnrollmentApprovalMode enrollmentMode)
            throws SQLException {
        String sql = """
                UPDATE assessment
                SET enrollment_mode = ?
                WHERE id_assessment = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, enrollmentMode.toDatabaseValue());
            statement.setLong(2, assessmentId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Assessment not found: " + assessmentId);
            }
        }
    }

    public void updateBlockPlacement(
            Connection connection,
            long assessmentId,
            long targetContentBlockId,
            int orderNo
    ) throws SQLException {
        String sql = """
                UPDATE assessment a
                JOIN content_block cb ON cb.id_content_block = ?
                JOIN class_group cg ON cg.id_class_group = cb.id_class_group
                SET a.id_content_block = ?, a.id_subject = cg.id_subject, a.order_no = ?
                WHERE a.id_assessment = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, targetContentBlockId);
            statement.setLong(2, targetContentBlockId);
            statement.setInt(3, orderNo);
            statement.setLong(4, assessmentId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Assessment not found: " + assessmentId);
            }
        }
    }

    public void updateState(Connection connection, long assessmentId, AssessmentState state)
            throws SQLException {
        String sql = """
                UPDATE assessment
                SET state = ?
                WHERE id_assessment = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            statement.setLong(2, assessmentId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Assessment not found: " + assessmentId);
            }
        }
    }

    public int synchronizeTemporalStates(Connection connection, LocalDateTime now) throws SQLException {
        int completed = updateTemporalState(
                connection,
                """
                UPDATE assessment
                SET state = 'completed'
                WHERE state IN ('scheduled', 'active')
                  AND available_until IS NOT NULL
                  AND available_until <= ?
                """,
                now
        );
        int active = updateTemporalState(
                connection,
                """
                UPDATE assessment
                SET state = 'active'
                WHERE state = 'scheduled'
                  AND available_from IS NOT NULL
                  AND available_from <= ?
                  AND (available_until IS NULL OR available_until > ?)
                """,
                now,
                now
        );
        return completed + active;
    }

    public boolean hasSubmittedAttempts(Connection connection, long assessmentId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM attempt
                WHERE id_assessment = ?
                  AND state IN ('submitted', 'corrected')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean hasAnyAttempts(Connection connection, long assessmentId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM attempt
                WHERE id_assessment = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean hasGradeSheetDependency(Connection connection, long assessmentId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM based_on_assessment
                WHERE id_assessment = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean hasCertificateDependency(Connection connection, long assessmentId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM based_on_assessment boa
                JOIN based_on_grade_sheet_certificate bgsc
                  ON bgsc.id_grade_sheet = boa.id_grade_sheet
                WHERE boa.id_assessment = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public void delete(Connection connection, long assessmentId) throws SQLException {
        String sql = "DELETE FROM assessment WHERE id_assessment = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Assessment not found: " + assessmentId);
            }
        }
    }

    public boolean hasSubmittedAttempts(long assessmentId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return hasSubmittedAttempts(connection, assessmentId);
        }
    }

    public boolean hasAnyAttempts(long assessmentId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return hasAnyAttempts(connection, assessmentId);
        }
    }

    public void replaceApplicableClassGroups(
            Connection connection,
            long assessmentId,
            List<Long> classGroupIds
    ) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM assessment_class_group WHERE id_assessment = ?")) {
            delete.setLong(1, assessmentId);
            delete.executeUpdate();
        }
        if (classGroupIds == null || classGroupIds.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO assessment_class_group (id_assessment, id_class_group)
                VALUES (?, ?)
                """;
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            for (Long classGroupId : orderedUnique(classGroupIds)) {
                insert.setLong(1, assessmentId);
                insert.setLong(2, classGroupId);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    public List<Long> findApplicableClassGroupIds(Connection connection, long assessmentId) throws SQLException {
        String sql = """
                SELECT class_group_id
                FROM (
                    SELECT cb.id_class_group AS class_group_id, 0 AS order_no
                    FROM assessment a
                    JOIN content_block cb ON cb.id_content_block = a.id_content_block
                    WHERE a.id_assessment = ?
                    UNION
                    SELECT acg.id_class_group AS class_group_id, 1 AS order_no
                    FROM assessment_class_group acg
                    WHERE acg.id_assessment = ?
                ) applicable_groups
                ORDER BY order_no, class_group_id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            statement.setLong(2, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Long> classGroupIds = new ArrayList<>();
                while (resultSet.next()) {
                    classGroupIds.add(resultSet.getLong("class_group_id"));
                }
                return classGroupIds;
            }
        }
    }

    public List<Long> findApplicableClassGroupIds(long assessmentId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findApplicableClassGroupIds(connection, assessmentId);
        }
    }

    public long countAttempts(Connection connection, long studentUserId, long assessmentId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM attempt
                WHERE id_student_user = ?
                  AND id_assessment = ?
                  AND state <> 'cancelled'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public int nextAttemptNumber(Connection connection, long studentUserId, long assessmentId) throws SQLException {
        String sql = """
                SELECT COALESCE(MAX(attempt_number), 0) + 1
                FROM attempt
                WHERE id_student_user = ?
                  AND id_assessment = ?
                FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public boolean hasCurrentStudentAccess(Connection connection, long studentUserId, long assessmentId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM assessment a
                LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
                LEFT JOIN class_group block_cg ON block_cg.id_class_group = cb.id_class_group
                JOIN student_profile sp ON sp.id_user = ?
                JOIN user_account u ON u.id_user = sp.id_user
                WHERE a.id_assessment = ?
                  AND a.mode = 'online'
                  AND u.state = 'active'
                  AND EXISTS (
                        SELECT 1
                        FROM enroll_assessment ea
                        WHERE ea.id_student_user = ?
                          AND ea.id_assessment = a.id_assessment
                          AND ea.state = 'active'
                          AND (ea.start_date IS NULL OR ea.start_date <= CURRENT_DATE)
                          AND (ea.end_date IS NULL OR ea.end_date >= CURRENT_DATE)
                  )
                  AND (
                        (
                            block_cg.id_class_group IS NOT NULL
                            AND EXISTS (
                                SELECT 1
                                FROM enroll_class_group ecg
                                JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
                                JOIN course c ON c.id_course = cg.id_course
                                JOIN subject s ON s.id_subject = cg.id_subject
                                WHERE ecg.id_student_user = ?
                                  AND ecg.id_class_group = block_cg.id_class_group
                                  AND ecg.state = 'active'
                                  AND cg.state = 'active'
                                  AND c.state = 'active'
                                  AND s.state = 'active'
                                  AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                                  AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                            )
                        )
                        OR (
                            block_cg.id_class_group IS NULL
                            AND a.id_subject IS NOT NULL
                            AND EXISTS (
                                SELECT 1
                                FROM assessment_class_group acg
                                JOIN class_group cg ON cg.id_class_group = acg.id_class_group
                                JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group
                                JOIN enroll_subject es
                                  ON es.id_student_user = ecg.id_student_user
                                 AND es.id_subject = cg.id_subject
                                 AND es.id_course = cg.id_course
                                JOIN subject s ON s.id_subject = cg.id_subject
                                JOIN course c ON c.id_course = cg.id_course
                                WHERE acg.id_assessment = a.id_assessment
                                  AND ecg.id_student_user = ?
                                  AND ecg.state = 'active'
                                  AND cg.state = 'active'
                                  AND es.state = 'active'
                                  AND cg.id_subject = a.id_subject
                                  AND s.state = 'active'
                                  AND c.state = 'active'
                                  AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                                  AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                                  AND (es.start_date IS NULL OR es.start_date <= CURRENT_DATE)
                                  AND (es.end_date IS NULL OR es.end_date >= CURRENT_DATE)
                            )
                        )
                  )
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, assessmentId);
            statement.setLong(3, studentUserId);
            statement.setLong(4, studentUserId);
            statement.setLong(5, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean hasCurrentStudentAccess(long studentUserId, long assessmentId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return hasCurrentStudentAccess(connection, studentUserId, assessmentId);
        }
    }

    public boolean hasActiveTeacherManagementContext(Connection connection, long teacherUserId, long assessmentId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM (
                    SELECT cb.id_class_group
                    FROM assessment a
                    JOIN content_block cb ON cb.id_content_block = a.id_content_block
                    WHERE a.id_assessment = ?
                    UNION
                    SELECT acg.id_class_group
                    FROM assessment_class_group acg
                    WHERE acg.id_assessment = ?
                ) applicable_groups
                JOIN class_group cg ON cg.id_class_group = applicable_groups.id_class_group
                JOIN teach_class_group tcg ON tcg.id_class_group = cg.id_class_group
                JOIN grant_teacher gt ON gt.id_teacher_user = tcg.id_teacher_user
                JOIN permission p ON p.cod_permission = gt.cod_permission
                JOIN user_account u ON u.id_user = tcg.id_teacher_user
                WHERE tcg.id_teacher_user = ?
                  AND tcg.state = 'active'
                  AND gt.cod_permission = 'MANAGE_LEARNING'
                  AND p.state = 'active'
                  AND u.state = 'active'
                  AND cg.state = 'active'
                  AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                  AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            statement.setLong(2, assessmentId);
            statement.setLong(3, teacherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public Long findContextClassGroupId(Connection connection, long assessmentId) throws SQLException {
        String sql = """
                SELECT cb.id_class_group
                FROM assessment a
                LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
                WHERE a.id_assessment = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                long value = resultSet.getLong(1);
                return resultSet.wasNull() ? null : value;
            }
        }
    }

    public boolean classGroupsMatchSubject(
            Connection connection,
            long subjectId,
            List<Long> classGroupIds
    ) throws SQLException {
        if (classGroupIds == null || classGroupIds.isEmpty()) {
            return true;
        }
        List<Long> uniqueClassGroupIds = orderedUnique(classGroupIds);
        String placeholders = String.join(",", Collections.nCopies(uniqueClassGroupIds.size(), "?"));
        String sql = """
                SELECT COUNT(*)
                FROM class_group
                WHERE id_subject = ?
                  AND state = 'active'
                  AND id_class_group IN (%s)
                """.formatted(placeholders);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            int index = 2;
            for (Long classGroupId : uniqueClassGroupIds) {
                statement.setLong(index++, classGroupId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) == uniqueClassGroupIds.size();
            }
        }
    }

    private static void setStatementValues(PreparedStatement statement, AssessmentCreateCommand command)
            throws SQLException {
        setNullableLong(statement, 1, command.subjectId());
        setNullableLong(statement, 2, command.contentBlockId());
        statement.setString(3, command.title().trim());
        setNullableString(statement, 4, command.description());
        statement.setString(5, command.type().toDatabaseValue());
        statement.setString(6, command.mode().toDatabaseValue());
        statement.setString(7, command.correctionMode().toDatabaseValue());
        statement.setBigDecimal(8, command.maxGrade());
        statement.setBigDecimal(9, command.passingGrade());
        setNullableInteger(statement, 10, command.attemptsLimit());
        statement.setString(11, command.enrollmentMode().toDatabaseValue());
        statement.setString(12, command.state().toDatabaseValue());
        setTimestamp(statement, 13, command.availableFrom());
        setTimestamp(statement, 14, command.availableUntil());
    }

    private static void setStatementValues(PreparedStatement statement, AssessmentUpdateCommand command)
            throws SQLException {
        setNullableLong(statement, 1, command.subjectId());
        setNullableLong(statement, 2, command.contentBlockId());
        statement.setString(3, command.title().trim());
        setNullableString(statement, 4, command.description());
        statement.setString(5, command.type().toDatabaseValue());
        statement.setString(6, command.mode().toDatabaseValue());
        statement.setString(7, command.correctionMode().toDatabaseValue());
        statement.setBigDecimal(8, command.maxGrade());
        statement.setBigDecimal(9, command.passingGrade());
        setNullableInteger(statement, 10, command.attemptsLimit());
        statement.setString(11, command.enrollmentMode().toDatabaseValue());
        statement.setString(12, command.state().toDatabaseValue());
        setTimestamp(statement, 13, command.availableFrom());
        setTimestamp(statement, 14, command.availableUntil());
    }

    private static String selectAssessmentSql() {
        return """
                SELECT id_assessment, id_subject, id_content_block, title, description, type, mode,
                       correction_mode, max_grade, passing_grade, attempts_limit, enrollment_mode, state,
                       available_from, available_until, order_no
                FROM assessment
                """;
    }

    private static List<Assessment> mapAssessments(ResultSet resultSet) throws SQLException {
        List<Assessment> assessments = new ArrayList<>();
        while (resultSet.next()) {
            assessments.add(mapAssessment(resultSet));
        }
        return assessments;
    }

    private static Assessment mapAssessment(ResultSet resultSet) throws SQLException {
        return new Assessment(
                resultSet.getLong("id_assessment"),
                nullableLong(resultSet, "id_subject"),
                nullableLong(resultSet, "id_content_block"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                AssessmentType.fromDatabaseValue(resultSet.getString("type")),
                AssessmentMode.fromDatabaseValue(resultSet.getString("mode")),
                AssessmentCorrectionMode.fromDatabaseValue(resultSet.getString("correction_mode")),
                resultSet.getBigDecimal("max_grade"),
                resultSet.getBigDecimal("passing_grade"),
                nullableInteger(resultSet, "attempts_limit"),
                EnrollmentApprovalMode.fromDatabaseValue(resultSet.getString("enrollment_mode")),
                AssessmentState.fromDatabaseValue(resultSet.getString("state")),
                getTimestamp(resultSet, "available_from"),
                getTimestamp(resultSet, "available_until"),
                nullableInteger(resultSet, "order_no")
        );
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static Integer nullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private static LocalDateTime getTimestamp(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static void setNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }

    private static void setTimestamp(PreparedStatement statement, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    private static List<Long> orderedUnique(List<Long> values) {
        Set<Long> unique = new LinkedHashSet<>();
        for (Long value : values) {
            if (value == null || value <= 0) {
                throw new IllegalArgumentException("Class group ids must be positive");
            }
            unique.add(value);
        }
        return List.copyOf(unique);
    }

    private static int updateTemporalState(Connection connection, String sql, LocalDateTime... values)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                statement.setTimestamp(index + 1, Timestamp.valueOf(values[index]));
            }
            return statement.executeUpdate();
        }
    }
}
