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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

public final class AssessmentDAO implements pt.isel.gape.transversal.service.ApplicationReadService.Assessments {

    private final ConnectionProvider connectionProvider;

    public record ClassGroupAssessmentWeightSummary(int assessmentCount, BigDecimal totalWeight) {
        public boolean hasAssessments() {
            return assessmentCount > 0;
        }

        public boolean totalIsOneHundred() {
            return totalWeight != null && totalWeight.compareTo(new BigDecimal("100.00")) == 0;
        }
    }

    public AssessmentDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, AssessmentCreateCommand command) throws SQLException {
        String sql = """
                INSERT INTO assessment (
                    id_subject, id_content_block, cod_physical_room, title, description, type, mode, correction_mode,
                    max_grade, passing_grade, final_grade_weight, attempts_limit, enrollment_mode, state,
                    available_from, available_until
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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

    public long create(Connection connection, long assessmentId, AssessmentCreateCommand command) throws SQLException {
        String sql = """
                INSERT INTO assessment (
                    id_assessment, id_subject, id_content_block, cod_physical_room, title, description, type, mode,
                    correction_mode, max_grade, passing_grade, final_grade_weight, attempts_limit, enrollment_mode,
                    state, available_from, available_until
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            setStatementValues(statement, command, 2);
            statement.executeUpdate();
            return assessmentId;
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

    public List<Assessment> findByClassGroup(long classGroupId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findByClassGroup(connection, classGroupId);
        }
    }

    public List<Assessment> findByClassGroup(Connection connection, long classGroupId) throws SQLException {
        String sql = """
                SELECT DISTINCT a.id_assessment, a.id_subject, a.id_content_block, a.cod_physical_room,
                       a.title, a.description, a.type, a.mode, a.correction_mode, a.max_grade,
                       a.passing_grade, a.final_grade_weight, a.attempts_limit, a.enrollment_mode,
                       a.state, a.available_from, a.available_until, a.order_no
                FROM assessment a
                LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
                LEFT JOIN assessment_class_group acg ON acg.id_assessment = a.id_assessment
                WHERE cb.id_class_group = ?
                   OR acg.id_class_group = ?
                ORDER BY COALESCE(a.order_no, 2147483647), a.available_from, a.id_assessment
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            statement.setLong(2, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAssessments(resultSet);
            }
        }
    }

    /**
     * Counts the assessments visible in each requested class-group context in
     * one query.  An assessment may be attached through a content block or a
     * direct class-group association, so the union mirrors findByClassGroup.
     */
    public Map<Long, Integer> countByClassGroupIds(Collection<Long> classGroupIds) throws SQLException {
        if (classGroupIds == null || classGroupIds.isEmpty()) {
            return Map.of();
        }
        List<Long> uniqueIds = classGroupIds.stream()
                .filter(java.util.Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (uniqueIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = placeholders(uniqueIds.size());
        String sql = """
                SELECT class_group_id, COUNT(DISTINCT id_assessment) AS assessment_count
                FROM (
                    SELECT cb.id_class_group AS class_group_id, a.id_assessment
                    FROM assessment a
                    JOIN content_block cb ON cb.id_content_block = a.id_content_block
                    WHERE cb.id_class_group IN (%s)
                    UNION ALL
                    SELECT acg.id_class_group AS class_group_id, acg.id_assessment
                    FROM assessment_class_group acg
                    WHERE acg.id_class_group IN (%s)
                ) applicable_assessments
                GROUP BY class_group_id
                """.formatted(placeholders, placeholders);
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Long classGroupId : uniqueIds) {
                statement.setLong(index++, classGroupId);
            }
            for (Long classGroupId : uniqueIds) {
                statement.setLong(index++, classGroupId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Long, Integer> counts = new LinkedHashMap<>();
                while (resultSet.next()) {
                    counts.put(resultSet.getLong("class_group_id"), resultSet.getInt("assessment_count"));
                }
                return Map.copyOf(counts);
            }
        }
    }

    /**
     * Counts uncorrected submitted attempts in each requested class-group context.  An
     * assessment can belong to a group through its content block or through a
     * direct association; using {@code UNION} keeps an attempt represented once
     * for that group when both associations exist.
     */
    public Map<Long, Integer> countSubmittedAttemptsByClassGroupIds(Collection<Long> classGroupIds)
            throws SQLException {
        if (classGroupIds == null || classGroupIds.isEmpty()) {
            return Map.of();
        }
        List<Long> uniqueIds = classGroupIds.stream()
                .filter(java.util.Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (uniqueIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = placeholders(uniqueIds.size());
        String sql = """
                SELECT applicable_assessments.class_group_id, COUNT(attempt.id_attempt) AS attempt_count
                FROM (
                    SELECT cb.id_class_group AS class_group_id, assessment.id_assessment
                    FROM assessment
                    JOIN content_block cb ON cb.id_content_block = assessment.id_content_block
                    WHERE cb.id_class_group IN (%s)
                    UNION
                    SELECT acg.id_class_group AS class_group_id, acg.id_assessment
                    FROM assessment_class_group acg
                    WHERE acg.id_class_group IN (%s)
                ) applicable_assessments
                JOIN attempt ON attempt.id_assessment = applicable_assessments.id_assessment
                WHERE attempt.state = 'submitted'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM grade_record
                      WHERE grade_record.id_attempt = attempt.id_attempt
                  )
                GROUP BY applicable_assessments.class_group_id
                """.formatted(placeholders, placeholders);
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Long classGroupId : uniqueIds) {
                statement.setLong(index++, classGroupId);
            }
            for (Long classGroupId : uniqueIds) {
                statement.setLong(index++, classGroupId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Long, Integer> counts = new LinkedHashMap<>();
                while (resultSet.next()) {
                    counts.put(resultSet.getLong("class_group_id"), resultSet.getInt("attempt_count"));
                }
                return Map.copyOf(counts);
            }
        }
    }

    /**
     * Counts uncorrected submitted attempts once across a set of class groups.  The global
     * dashboard badge represents actionable attempts, not the number of
     * group-association paths by which an attempt can be reached.
     */
    public int countDistinctSubmittedAttemptsByClassGroupIds(Collection<Long> classGroupIds)
            throws SQLException {
        if (classGroupIds == null || classGroupIds.isEmpty()) {
            return 0;
        }
        List<Long> uniqueIds = classGroupIds.stream()
                .filter(java.util.Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (uniqueIds.isEmpty()) {
            return 0;
        }
        String placeholders = placeholders(uniqueIds.size());
        String sql = """
                SELECT COUNT(DISTINCT attempt.id_attempt)
                FROM (
                    SELECT cb.id_class_group AS class_group_id, assessment.id_assessment
                    FROM assessment
                    JOIN content_block cb ON cb.id_content_block = assessment.id_content_block
                    WHERE cb.id_class_group IN (%s)
                    UNION
                    SELECT acg.id_class_group AS class_group_id, acg.id_assessment
                    FROM assessment_class_group acg
                    WHERE acg.id_class_group IN (%s)
                ) applicable_assessments
                JOIN attempt ON attempt.id_assessment = applicable_assessments.id_assessment
                WHERE attempt.state = 'submitted'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM grade_record
                      WHERE grade_record.id_attempt = attempt.id_attempt
                  )
                """.formatted(placeholders, placeholders);
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Long classGroupId : uniqueIds) {
                statement.setLong(index++, classGroupId);
            }
            for (Long classGroupId : uniqueIds) {
                statement.setLong(index++, classGroupId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
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
                                  AND cb.state = 'active'
                                  AND ecg.id_student_user = ?
                                  AND ecg.state = 'active'
                                  AND cg.state = 'active'
                                  AND c.state = 'active'
                                  AND s.state = 'active'
                                  AND ecg.start_date <= DATE(assessment.available_from)
                                  AND ecg.end_date >= DATE(assessment.available_until)
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
                                JOIN subject s ON s.id_subject = cg.id_subject
                                JOIN course c ON c.id_course = cg.id_course
                                WHERE acg.id_assessment = assessment.id_assessment
                                  AND ecg.id_student_user = ?
                                  AND ecg.state = 'active'
                                  AND cg.state = 'active'
                                  AND cg.id_subject = assessment.id_subject
                                  AND s.state = 'active'
                                  AND c.state = 'active'
                                  AND ecg.start_date <= DATE(assessment.available_from)
                                  AND ecg.end_date >= DATE(assessment.available_until)
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
                SET id_subject = ?, id_content_block = ?, cod_physical_room = ?, title = ?, description = ?,
                    type = ?, mode = ?, correction_mode = ?, max_grade = ?, passing_grade = ?,
                    final_grade_weight = ?, attempts_limit = ?, enrollment_mode = ?, state = ?,
                    available_from = ?, available_until = ?
                WHERE id_assessment = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setStatementValues(statement, command);
            statement.setLong(17, assessmentId);
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
        int scheduled = updateTemporalState(
                connection,
                """
                UPDATE assessment
                SET state = 'scheduled'
                WHERE state = 'active'
                  AND available_from IS NOT NULL
                  AND available_from > ?
                  AND (available_until IS NULL OR available_until > ?)
                """,
                now,
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
        return completed + scheduled + active;
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

    public Map<Long, List<Long>> findApplicableClassGroupIdsByAssessmentIds(Collection<Long> assessmentIds)
            throws SQLException {
        if (assessmentIds == null || assessmentIds.isEmpty()) {
            return Map.of();
        }
        try (Connection connection = connectionProvider.getConnection()) {
            return findApplicableClassGroupIdsByAssessmentIds(connection, assessmentIds);
        }
    }

    public Map<Long, List<Long>> findApplicableClassGroupIdsByAssessmentIds(
            Connection connection,
            Collection<Long> assessmentIds
    ) throws SQLException {
        if (assessmentIds == null || assessmentIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = placeholders(assessmentIds.size());
        String sql = """
                SELECT id_assessment, class_group_id, MIN(order_no) AS order_no
                FROM (
                    SELECT a.id_assessment, cb.id_class_group AS class_group_id, 0 AS order_no
                    FROM assessment a
                    JOIN content_block cb ON cb.id_content_block = a.id_content_block
                    WHERE a.id_assessment IN (%s)
                    UNION ALL
                    SELECT acg.id_assessment, acg.id_class_group AS class_group_id, 1 AS order_no
                    FROM assessment_class_group acg
                    WHERE acg.id_assessment IN (%s)
                ) applicable_groups
                GROUP BY id_assessment, class_group_id
                ORDER BY id_assessment, order_no, class_group_id
                """.formatted(placeholders, placeholders);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Long assessmentId : assessmentIds) {
                statement.setLong(index++, assessmentId);
            }
            for (Long assessmentId : assessmentIds) {
                statement.setLong(index++, assessmentId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Long, List<Long>> result = new LinkedHashMap<>();
                while (resultSet.next()) {
                    result.computeIfAbsent(resultSet.getLong("id_assessment"), ignored -> new ArrayList<>())
                            .add(resultSet.getLong("class_group_id"));
                }
                return result;
            }
        }
    }

    public void upsertWeightForMatchingGradeSheets(
            Connection connection,
            long assessmentId,
            BigDecimal finalGradeWeight
    ) throws SQLException {
        String sql = """
                INSERT INTO based_on_assessment (id_grade_sheet, id_assessment, weight)
                SELECT DISTINCT matched.id_grade_sheet, ?, ?
                FROM (
                    SELECT boa.id_grade_sheet
                    FROM based_on_assessment boa
                    WHERE boa.id_assessment = ?
                    UNION
                    SELECT gs.id_grade_sheet
                    FROM grade_sheet gs
                    JOIN (
                        SELECT cg.id_subject, cg.id_course_occurrence
                        FROM assessment a
                        JOIN content_block cb ON cb.id_content_block = a.id_content_block
                        JOIN class_group cg ON cg.id_class_group = cb.id_class_group
                        WHERE a.id_assessment = ?
                          AND (a.id_subject IS NULL OR a.id_subject = cg.id_subject)
                        UNION
                        SELECT cg.id_subject, cg.id_course_occurrence
                        FROM assessment a
                        JOIN assessment_class_group acg ON acg.id_assessment = a.id_assessment
                        JOIN class_group cg ON cg.id_class_group = acg.id_class_group
                        WHERE a.id_assessment = ?
                          AND (a.id_subject IS NULL OR a.id_subject = cg.id_subject)
                    ) assessment_context
                      ON assessment_context.id_subject = gs.id_subject
                     AND assessment_context.id_course_occurrence = gs.id_course_occurrence
                    WHERE gs.scope = 'subject_occurrence'
                      AND NOT EXISTS (
                            SELECT 1
                            FROM associate_grade_sheet_class_group agscg
                            WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                        )
                    UNION
                    SELECT gs.id_grade_sheet
                    FROM grade_sheet gs
                    JOIN associate_grade_sheet_class_group agscg
                      ON agscg.id_grade_sheet = gs.id_grade_sheet
                    JOIN assessment a ON a.id_assessment = ?
                    LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
                    LEFT JOIN assessment_class_group acg
                      ON acg.id_assessment = a.id_assessment
                    WHERE gs.scope = 'class_group'
                      AND (
                            cb.id_class_group = agscg.id_class_group
                            OR acg.id_class_group = agscg.id_class_group
                        )
                      AND (
                            a.id_subject IS NULL
                            OR a.id_subject = gs.id_subject
                        )
                ) matched
                ON DUPLICATE KEY UPDATE weight = VALUES(weight)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            statement.setBigDecimal(2, finalGradeWeight);
            statement.setLong(3, assessmentId);
            statement.setLong(4, assessmentId);
            statement.setLong(5, assessmentId);
            statement.setLong(6, assessmentId);
            statement.executeUpdate();
        }
    }

    public ClassGroupAssessmentWeightSummary summarizeAssessmentWeightsForClassGroup(
            long classGroupId
    ) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return summarizeAssessmentWeightsForClassGroup(connection, classGroupId);
        }
    }

    public ClassGroupAssessmentWeightSummary summarizeAssessmentWeightsForClassGroup(
            Connection connection,
            long classGroupId
    ) throws SQLException {
        String sql = """
                SELECT COUNT(boa.id_assessment) AS assessment_count,
                       COALESCE(SUM(boa.weight), 0) AS total_weight
                FROM (
                    SELECT gs.id_grade_sheet
                    FROM grade_sheet gs
                    JOIN associate_grade_sheet_class_group agscg
                      ON agscg.id_grade_sheet = gs.id_grade_sheet
                    WHERE agscg.id_class_group = ?
                      AND gs.scope = 'class_group'
                      AND gs.type = 'final'
                    ORDER BY gs.id_grade_sheet
                    LIMIT 1
                ) selected_sheet
                LEFT JOIN based_on_assessment boa
                  ON boa.id_grade_sheet = selected_sheet.id_grade_sheet
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new ClassGroupAssessmentWeightSummary(
                        resultSet.getInt("assessment_count"),
                        resultSet.getBigDecimal("total_weight")
                );
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
                  AND (a.id_content_block IS NULL OR cb.state = 'active')
                  AND EXISTS (
                        SELECT 1
                        FROM enroll_assessment ea
                        WHERE ea.id_student_user = ?
                          AND ea.id_assessment = a.id_assessment
                          AND ea.state = 'active'
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
                                  AND ecg.start_date <= DATE(a.available_from)
                                  AND ecg.end_date >= DATE(a.available_until)
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
                                JOIN subject s ON s.id_subject = cg.id_subject
                                JOIN course c ON c.id_course = cg.id_course
                                WHERE acg.id_assessment = a.id_assessment
                                  AND ecg.id_student_user = ?
                                  AND ecg.state = 'active'
                                  AND cg.state = 'active'
                                  AND cg.id_subject = a.id_subject
                                  AND s.state = 'active'
                                  AND c.state = 'active'
                                  AND ecg.start_date <= DATE(a.available_from)
                                  AND ecg.end_date >= DATE(a.available_until)
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

    public boolean roomHasOverlappingReservedAssessment(
            Connection connection,
            String physicalRoomCode,
            LocalDateTime availableFrom,
            LocalDateTime availableUntil,
            Long excludedAssessmentId
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM assessment
                WHERE cod_physical_room = ?
                  AND mode = 'onsite'
                  AND state IN ('scheduled', 'active')
                  AND available_from IS NOT NULL
                  AND available_until IS NOT NULL
                  AND NOT (? <= available_from OR ? >= available_until)
                  AND (? IS NULL OR id_assessment <> ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, physicalRoomCode);
            statement.setTimestamp(2, Timestamp.valueOf(availableUntil));
            statement.setTimestamp(3, Timestamp.valueOf(availableFrom));
            if (excludedAssessmentId == null) {
                statement.setNull(4, Types.BIGINT);
                statement.setNull(5, Types.BIGINT);
            } else {
                statement.setLong(4, excludedAssessmentId);
                statement.setLong(5, excludedAssessmentId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    private static void setStatementValues(PreparedStatement statement, AssessmentCreateCommand command)
            throws SQLException {
        setStatementValues(statement, command, 1);
    }

    private static void setStatementValues(
            PreparedStatement statement,
            AssessmentCreateCommand command,
            int offset
    ) throws SQLException {
        setNullableLong(statement, offset, command.subjectId());
        setNullableLong(statement, offset + 1, command.contentBlockId());
        setNullableString(statement, offset + 2, command.physicalRoomCode());
        statement.setString(offset + 3, command.title().trim());
        setNullableString(statement, offset + 4, command.description());
        statement.setString(offset + 5, command.type().toDatabaseValue());
        statement.setString(offset + 6, command.mode().toDatabaseValue());
        statement.setString(offset + 7, command.correctionMode().toDatabaseValue());
        statement.setBigDecimal(offset + 8, command.maxGrade());
        statement.setBigDecimal(offset + 9, command.passingGrade());
        statement.setBigDecimal(offset + 10, command.finalGradeWeight());
        setNullableInteger(statement, offset + 11, command.attemptsLimit());
        statement.setString(offset + 12, command.enrollmentMode().toDatabaseValue());
        statement.setString(offset + 13, command.state().toDatabaseValue());
        setTimestamp(statement, offset + 14, command.availableFrom());
        setTimestamp(statement, offset + 15, command.availableUntil());
    }

    private static void setStatementValues(PreparedStatement statement, AssessmentUpdateCommand command)
            throws SQLException {
        setNullableLong(statement, 1, command.subjectId());
        setNullableLong(statement, 2, command.contentBlockId());
        setNullableString(statement, 3, command.physicalRoomCode());
        statement.setString(4, command.title().trim());
        setNullableString(statement, 5, command.description());
        statement.setString(6, command.type().toDatabaseValue());
        statement.setString(7, command.mode().toDatabaseValue());
        statement.setString(8, command.correctionMode().toDatabaseValue());
        statement.setBigDecimal(9, command.maxGrade());
        statement.setBigDecimal(10, command.passingGrade());
        statement.setBigDecimal(11, command.finalGradeWeight());
        setNullableInteger(statement, 12, command.attemptsLimit());
        statement.setString(13, command.enrollmentMode().toDatabaseValue());
        statement.setString(14, command.state().toDatabaseValue());
        setTimestamp(statement, 15, command.availableFrom());
        setTimestamp(statement, 16, command.availableUntil());
    }

    private static String selectAssessmentSql() {
        return """
                SELECT id_assessment, id_subject, id_content_block, cod_physical_room, title, description, type,
                       mode, correction_mode, max_grade, passing_grade, final_grade_weight, attempts_limit,
                       enrollment_mode, state, available_from, available_until, order_no
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
                resultSet.getString("cod_physical_room"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                AssessmentType.fromDatabaseValue(resultSet.getString("type")),
                AssessmentMode.fromDatabaseValue(resultSet.getString("mode")),
                AssessmentCorrectionMode.fromDatabaseValue(resultSet.getString("correction_mode")),
                resultSet.getBigDecimal("max_grade"),
                resultSet.getBigDecimal("passing_grade"),
                resultSet.getBigDecimal("final_grade_weight"),
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

    private static String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }
}
