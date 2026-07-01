package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.GradeAssessmentWeight;
import pt.isel.gape.learning.model.GradeSheet;
import pt.isel.gape.learning.model.GradeSheetCreateCommand;
import pt.isel.gape.learning.model.GradeSheetState;
import pt.isel.gape.learning.model.GradeSheetType;
import pt.isel.gape.learning.model.GradeSheetUpdateCommand;

public final class GradeSheetDAO {

    private final ConnectionProvider connectionProvider;

    public GradeSheetDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, GradeSheetCreateCommand command) throws SQLException {
        String sql = """
                INSERT INTO grade_sheet (
                    id_subject, title, type, max_grade, passing_grade, released_at, state
                ) VALUES (?, ?, ?, ?, ?, NULL, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, command.subjectId());
            statement.setString(2, command.title().trim());
            statement.setString(3, command.type().toDatabaseValue());
            statement.setBigDecimal(4, command.maxGrade());
            statement.setBigDecimal(5, command.passingGrade());
            statement.setString(6, command.state().toDatabaseValue());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating grade sheet failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public void update(Connection connection, long gradeSheetId, GradeSheetUpdateCommand command)
            throws SQLException {
        String sql = """
                UPDATE grade_sheet
                SET id_subject = ?, title = ?, type = ?, max_grade = ?, passing_grade = ?
                WHERE id_grade_sheet = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, command.subjectId());
            statement.setString(2, command.title().trim());
            statement.setString(3, command.type().toDatabaseValue());
            statement.setBigDecimal(4, command.maxGrade());
            statement.setBigDecimal(5, command.passingGrade());
            statement.setLong(6, gradeSheetId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Grade sheet not found: " + gradeSheetId);
            }
        }
    }

    public void updateState(
            Connection connection,
            long gradeSheetId,
            GradeSheetState state,
            LocalDateTime releasedAt
    ) throws SQLException {
        String sql = """
                UPDATE grade_sheet
                SET state = ?, released_at = ?
                WHERE id_grade_sheet = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            setTimestamp(statement, 2, releasedAt);
            statement.setLong(3, gradeSheetId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Grade sheet not found: " + gradeSheetId);
            }
        }
    }

    public void updateWeightAlert(Connection connection, long gradeSheetId, String weightAlert)
            throws SQLException {
        String sql = """
                UPDATE grade_sheet
                SET weight_alert = ?
                WHERE id_grade_sheet = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableString(statement, 1, weightAlert);
            statement.setLong(2, gradeSheetId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Grade sheet not found: " + gradeSheetId);
            }
        }
    }

    public Optional<GradeSheet> findById(long gradeSheetId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, gradeSheetId);
        }
    }

    public Optional<GradeSheet> findById(Connection connection, long gradeSheetId) throws SQLException {
        String sql = selectGradeSheetSql() + " WHERE id_grade_sheet = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gradeSheetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapGradeSheet(connection, resultSet));
            }
        }
    }

    public List<GradeSheet> findAll() throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            String sql = selectGradeSheetSql() + " ORDER BY id_grade_sheet DESC";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                List<GradeSheet> gradeSheets = new ArrayList<>();
                while (resultSet.next()) {
                    gradeSheets.add(mapGradeSheet(connection, resultSet));
                }
                return List.copyOf(gradeSheets);
            }
        }
    }

    public Optional<GradeSheet> lockById(Connection connection, long gradeSheetId) throws SQLException {
        String sql = selectGradeSheetSql() + " WHERE id_grade_sheet = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gradeSheetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapGradeSheet(connection, resultSet));
            }
        }
    }

    public Optional<GradeSheet> findByClassGroupId(Connection connection, long classGroupId) throws SQLException {
        String sql = selectGradeSheetSql() + """
                JOIN associate_grade_sheet_class_group agscg
                  ON agscg.id_grade_sheet = grade_sheet.id_grade_sheet
                WHERE agscg.id_class_group = ?
                ORDER BY grade_sheet.id_grade_sheet
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapGradeSheet(connection, resultSet));
            }
        }
    }

    public Optional<GradeSheet> findBySubjectWithoutClassGroups(Connection connection, long subjectId)
            throws SQLException {
        String sql = selectGradeSheetSql() + """
                WHERE grade_sheet.id_subject = ?
                  AND NOT EXISTS (
                        SELECT 1
                        FROM associate_grade_sheet_class_group agscg
                        WHERE agscg.id_grade_sheet = grade_sheet.id_grade_sheet
                  )
                ORDER BY grade_sheet.id_grade_sheet
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapGradeSheet(connection, resultSet));
            }
        }
    }

    public void replaceClassGroups(
            Connection connection,
            long gradeSheetId,
            List<Long> classGroupIds
    ) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM associate_grade_sheet_class_group WHERE id_grade_sheet = ?")) {
            delete.setLong(1, gradeSheetId);
            delete.executeUpdate();
        }
        if (classGroupIds == null || classGroupIds.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO associate_grade_sheet_class_group (id_grade_sheet, id_class_group)
                VALUES (?, ?)
                """;
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            for (Long classGroupId : orderedUnique(classGroupIds, "Class group ids must be positive")) {
                insert.setLong(1, gradeSheetId);
                insert.setLong(2, classGroupId);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    public void replaceAssessmentWeights(
            Connection connection,
            long gradeSheetId,
            List<GradeAssessmentWeight> assessmentWeights
    ) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM based_on_assessment WHERE id_grade_sheet = ?")) {
            delete.setLong(1, gradeSheetId);
            delete.executeUpdate();
        }
        if (assessmentWeights == null || assessmentWeights.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO based_on_assessment (id_grade_sheet, id_assessment, weight)
                VALUES (?, ?, ?)
                """;
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            for (GradeAssessmentWeight assessmentWeight : orderedUniqueWeights(assessmentWeights)) {
                insert.setLong(1, gradeSheetId);
                insert.setLong(2, assessmentWeight.assessmentId());
                insert.setBigDecimal(3, assessmentWeight.weight());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    public List<Long> findClassGroupIds(Connection connection, long gradeSheetId) throws SQLException {
        String sql = """
                SELECT id_class_group
                FROM associate_grade_sheet_class_group
                WHERE id_grade_sheet = ?
                ORDER BY id_class_group
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gradeSheetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (resultSet.next()) {
                    ids.add(resultSet.getLong("id_class_group"));
                }
                return List.copyOf(ids);
            }
        }
    }

    public List<GradeAssessmentWeight> findAssessmentWeights(Connection connection, long gradeSheetId)
            throws SQLException {
        String sql = """
                SELECT id_assessment, weight
                FROM based_on_assessment
                WHERE id_grade_sheet = ?
                ORDER BY id_assessment
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gradeSheetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<GradeAssessmentWeight> weights = new ArrayList<>();
                while (resultSet.next()) {
                    weights.add(new GradeAssessmentWeight(
                            resultSet.getLong("id_assessment"),
                            resultSet.getBigDecimal("weight")
                    ));
                }
                return List.copyOf(weights);
            }
        }
    }

    public List<Long> findAssessmentIdsForSheetContext(
            long subjectId,
            List<Long> classGroupIds
    ) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findAssessmentIdsForSheetContext(connection, subjectId, classGroupIds);
        }
    }

    public List<Long> findAssessmentIdsForSheetContext(
            Connection connection,
            long subjectId,
            List<Long> classGroupIds
    ) throws SQLException {
        List<Long> uniqueClassGroupIds = orderedUnique(classGroupIds, "Class group ids must be positive");
        if (uniqueClassGroupIds.isEmpty()) {
            String sql = """
                    SELECT DISTINCT a.id_assessment
                    FROM assessment a
                    LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
                    LEFT JOIN class_group cg ON cg.id_class_group = cb.id_class_group
                    WHERE a.id_subject = ?
                       OR cg.id_subject = ?
                    ORDER BY a.id_assessment
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, subjectId);
                statement.setLong(2, subjectId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return longList(resultSet, "id_assessment");
                }
            }
        }
        String placeholders = String.join(",", Collections.nCopies(uniqueClassGroupIds.size(), "?"));
        String sql = """
                SELECT DISTINCT a.id_assessment
                FROM assessment a
                LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
                LEFT JOIN assessment_class_group acg ON acg.id_assessment = a.id_assessment
                WHERE (
                        cb.id_class_group IN (%1$s)
                        OR acg.id_class_group IN (%1$s)
                      )
                  AND (
                        a.id_subject IS NULL
                        OR a.id_subject = ?
                      )
                ORDER BY a.id_assessment
                """.formatted(placeholders);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Long classGroupId : uniqueClassGroupIds) {
                statement.setLong(index++, classGroupId);
            }
            for (Long classGroupId : uniqueClassGroupIds) {
                statement.setLong(index++, classGroupId);
            }
            statement.setLong(index, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return longList(resultSet, "id_assessment");
            }
        }
    }

    public List<GradeAssessmentWeight> findDefaultAssessmentWeightsForSheetContext(
            Connection connection,
            long subjectId,
            List<Long> classGroupIds
    ) throws SQLException {
        List<Long> assessmentIds = findAssessmentIdsForSheetContext(connection, subjectId, classGroupIds);
        if (assessmentIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(assessmentIds.size(), "?"));
        String sql = """
                SELECT id_assessment, final_grade_weight
                FROM assessment
                WHERE id_assessment IN (%s)
                """.formatted(placeholders);
        Map<Long, GradeAssessmentWeight> byAssessmentId = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Long assessmentId : assessmentIds) {
                statement.setLong(index++, assessmentId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long assessmentId = resultSet.getLong("id_assessment");
                    byAssessmentId.put(
                            assessmentId,
                            new GradeAssessmentWeight(assessmentId, resultSet.getBigDecimal("final_grade_weight"))
                    );
                }
            }
        }
        List<GradeAssessmentWeight> weights = new ArrayList<>();
        for (Long assessmentId : assessmentIds) {
            GradeAssessmentWeight weight = byAssessmentId.get(assessmentId);
            if (weight != null) {
                weights.add(weight);
            }
        }
        return List.copyOf(weights);
    }

    public boolean subjectExists(Connection connection, long subjectId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM subject
                WHERE id_subject = ?
                  AND state = 'active'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean classGroupsMatchSubject(
            Connection connection,
            long subjectId,
            List<Long> classGroupIds
    ) throws SQLException {
        List<Long> uniqueIds = orderedUnique(classGroupIds, "Class group ids must be positive");
        if (uniqueIds.isEmpty()) {
            return true;
        }
        String placeholders = String.join(",", Collections.nCopies(uniqueIds.size(), "?"));
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
            for (Long classGroupId : uniqueIds) {
                statement.setLong(index++, classGroupId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) == uniqueIds.size();
            }
        }
    }

    public boolean assessmentsMatchSubject(
            Connection connection,
            long subjectId,
            List<GradeAssessmentWeight> assessmentWeights
    ) throws SQLException {
        List<GradeAssessmentWeight> uniqueWeights = orderedUniqueWeights(assessmentWeights);
        if (uniqueWeights.isEmpty()) {
            return true;
        }
        String placeholders = String.join(",", Collections.nCopies(uniqueWeights.size(), "?"));
        String sql = """
                SELECT COUNT(*)
                FROM assessment a
                LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
                LEFT JOIN class_group cg ON cg.id_class_group = cb.id_class_group
                WHERE a.id_assessment IN (%s)
                  AND (a.id_subject IS NULL OR a.id_subject = ?)
                  AND (cg.id_subject IS NULL OR cg.id_subject = ?)
                """.formatted(placeholders);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (GradeAssessmentWeight weight : uniqueWeights) {
                statement.setLong(index++, weight.assessmentId());
            }
            statement.setLong(index++, subjectId);
            statement.setLong(index, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) == uniqueWeights.size();
            }
        }
    }

    public boolean allGradeSheetsBelongToCourse(
            Connection connection,
            long courseId,
            List<Long> gradeSheetIds
    ) throws SQLException {
        List<Long> uniqueIds = orderedUnique(gradeSheetIds, "Grade sheet ids must be positive");
        if (uniqueIds.isEmpty()) {
            return true;
        }
        String placeholders = String.join(",", Collections.nCopies(uniqueIds.size(), "?"));
        String sql = """
                SELECT COUNT(DISTINCT gs.id_grade_sheet)
                FROM grade_sheet gs
                JOIN integrate_subject isub ON isub.id_subject = gs.id_subject
                WHERE isub.id_course = ?
                  AND gs.id_grade_sheet IN (%s)
                """.formatted(placeholders);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, courseId);
            int index = 2;
            for (Long gradeSheetId : uniqueIds) {
                statement.setLong(index++, gradeSheetId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) == uniqueIds.size();
            }
        }
    }

    public List<Long> findStudentUserIdsForSheetContext(Connection connection, GradeSheet gradeSheet)
            throws SQLException {
        Set<Long> studentUserIds = new LinkedHashSet<>();
        if (!gradeSheet.classGroupIds().isEmpty()) {
            String sql = """
                    SELECT DISTINCT id_student_user
                    FROM enroll_class_group
                    WHERE id_class_group = ?
                      AND state IN ('active', 'completed')
                    ORDER BY id_student_user
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Long classGroupId : gradeSheet.classGroupIds()) {
                    statement.setLong(1, classGroupId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            studentUserIds.add(resultSet.getLong("id_student_user"));
                        }
                    }
                }
            }
        } else {
            String sql = """
                    SELECT DISTINCT id_student_user
                    FROM enroll_subject
                    WHERE id_subject = ?
                      AND state IN ('active', 'completed')
                    ORDER BY id_student_user
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, gradeSheet.subjectId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        studentUserIds.add(resultSet.getLong("id_student_user"));
                    }
                }
            }
        }
        String sql = """
                SELECT DISTINCT id_user_student
                FROM grade_record
                WHERE id_grade_sheet = ?
                  AND state IN ('draft', 'published', 'corrected')
                ORDER BY id_user_student
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gradeSheet.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    studentUserIds.add(resultSet.getLong("id_user_student"));
                }
            }
        }
        return List.copyOf(studentUserIds);
    }

    public List<Long> findCourseIdsForSheetContext(
            Connection connection,
            GradeSheet gradeSheet,
            long studentUserId
    ) throws SQLException {
        Set<Long> courseIds = new LinkedHashSet<>();
        if (!gradeSheet.classGroupIds().isEmpty()) {
            String sql = """
                    SELECT DISTINCT cg.id_course
                    FROM associate_grade_sheet_class_group agscg
                    JOIN class_group cg ON cg.id_class_group = agscg.id_class_group
                    JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group
                    WHERE agscg.id_grade_sheet = ?
                      AND ecg.id_student_user = ?
                      AND ecg.state IN ('active', 'completed')
                    ORDER BY cg.id_course
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, gradeSheet.id());
                statement.setLong(2, studentUserId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        courseIds.add(resultSet.getLong("id_course"));
                    }
                }
            }
        }
        String sql = """
                SELECT DISTINCT es.id_course
                FROM enroll_subject es
                JOIN integrate_subject isub
                  ON isub.id_course = es.id_course
                 AND isub.id_subject = es.id_subject
                 AND isub.state = 'active'
                WHERE es.id_student_user = ?
                  AND es.id_subject = ?
                  AND es.state IN ('active', 'completed')
                ORDER BY es.id_course
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, gradeSheet.subjectId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courseIds.add(resultSet.getLong("id_course"));
                }
            }
        }
        return List.copyOf(courseIds);
    }

    public List<Long> findStudentUserIdsForSheetContext(GradeSheet gradeSheet) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findStudentUserIdsForSheetContext(connection, gradeSheet);
        }
    }

    public boolean hasActiveGradeRecord(Connection connection, long gradeSheetId, long studentUserId)
            throws SQLException {
        String sql = """
                SELECT 1
                FROM grade_record
                WHERE id_grade_sheet = ?
                  AND id_user_student = ?
                  AND state = 'published'
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gradeSheetId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean hasCorrectedAssessmentScore(Connection connection, long studentUserId, long assessmentId)
            throws SQLException {
        String sql = """
                SELECT 1
                FROM attempt
                WHERE id_student_user = ?
                  AND id_assessment = ?
                  AND state = 'corrected'
                  AND score IS NOT NULL
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public Optional<java.math.BigDecimal> findLatestCorrectedAssessmentScore(
            Connection connection,
            long studentUserId,
            long assessmentId
    ) throws SQLException {
        String sql = """
                SELECT score
                FROM attempt
                WHERE id_student_user = ?
                  AND id_assessment = ?
                  AND state = 'corrected'
                  AND score IS NOT NULL
                ORDER BY submitted_at DESC, attempt_number DESC, id_attempt DESC
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(resultSet.getBigDecimal("score"));
            }
        }
    }

    public Optional<java.math.BigDecimal> findLatestCorrectedAssessmentScore(long studentUserId, long assessmentId)
            throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findLatestCorrectedAssessmentScore(connection, studentUserId, assessmentId);
        }
    }

    public List<Long> findGradeSheetIdsForAssessmentContext(Connection connection, long assessmentId)
            throws SQLException {
        String sql = """
                SELECT DISTINCT matched.id_grade_sheet
                FROM (
                    SELECT boa.id_grade_sheet
                    FROM based_on_assessment boa
                    WHERE boa.id_assessment = ?
                    UNION
                    SELECT gs.id_grade_sheet
                    FROM grade_sheet gs
                    JOIN assessment a ON a.id_assessment = ?
                    LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
                    LEFT JOIN class_group block_cg ON block_cg.id_class_group = cb.id_class_group
                    WHERE NOT EXISTS (
                            SELECT 1
                            FROM associate_grade_sheet_class_group agscg
                            WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                        )
                      AND (
                            a.id_subject = gs.id_subject
                            OR block_cg.id_subject = gs.id_subject
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
                    WHERE (
                            cb.id_class_group = agscg.id_class_group
                            OR acg.id_class_group = agscg.id_class_group
                        )
                      AND (
                            a.id_subject IS NULL
                            OR a.id_subject = gs.id_subject
                        )
                ) matched
                ORDER BY matched.id_grade_sheet
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            statement.setLong(2, assessmentId);
            statement.setLong(3, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return longList(resultSet, "id_grade_sheet");
            }
        }
    }

    public boolean classGroupsEndedBefore(Connection connection, List<Long> classGroupIds, LocalDate date)
            throws SQLException {
        List<Long> uniqueClassGroupIds = orderedUnique(classGroupIds, "Class group ids must be positive");
        if (uniqueClassGroupIds.isEmpty()) {
            return false;
        }
        String sql = """
                SELECT ends_at
                FROM class_group
                WHERE id_class_group = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Long classGroupId : uniqueClassGroupIds) {
                statement.setLong(1, classGroupId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return false;
                    }
                    java.sql.Date endsAt = resultSet.getDate("ends_at");
                    if (endsAt == null || !endsAt.toLocalDate().isBefore(date)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean classGroupsEndedBefore(List<Long> classGroupIds, LocalDate date) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return classGroupsEndedBefore(connection, classGroupIds, date);
        }
    }

    private static String selectGradeSheetSql() {
        return """
                SELECT grade_sheet.id_grade_sheet, grade_sheet.id_subject, grade_sheet.title, grade_sheet.type,
                       grade_sheet.max_grade, grade_sheet.passing_grade, grade_sheet.weight_alert,
                       grade_sheet.released_at, grade_sheet.state
                FROM grade_sheet
                """;
    }

    private GradeSheet mapGradeSheet(Connection connection, ResultSet resultSet) throws SQLException {
        long id = resultSet.getLong("id_grade_sheet");
        return new GradeSheet(
                id,
                resultSet.getLong("id_subject"),
                resultSet.getString("title"),
                GradeSheetType.fromDatabaseValue(resultSet.getString("type")),
                resultSet.getBigDecimal("max_grade"),
                resultSet.getBigDecimal("passing_grade"),
                resultSet.getString("weight_alert"),
                getTimestamp(resultSet, "released_at"),
                GradeSheetState.fromDatabaseValue(resultSet.getString("state")),
                findClassGroupIds(connection, id),
                findAssessmentWeights(connection, id)
        );
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

    private static List<Long> longList(ResultSet resultSet, String column) throws SQLException {
        List<Long> ids = new ArrayList<>();
        while (resultSet.next()) {
            ids.add(resultSet.getLong(column));
        }
        return List.copyOf(ids);
    }

    private static List<Long> orderedUnique(List<Long> values, String errorMessage) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long value : values) {
            if (value == null || value <= 0) {
                throw new IllegalArgumentException(errorMessage);
            }
            unique.add(value);
        }
        return List.copyOf(unique);
    }

    private static List<GradeAssessmentWeight> orderedUniqueWeights(List<GradeAssessmentWeight> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<Long> seenAssessmentIds = new LinkedHashSet<>();
        List<GradeAssessmentWeight> unique = new ArrayList<>();
        for (GradeAssessmentWeight value : values) {
            if (value == null || value.assessmentId() <= 0) {
                throw new IllegalArgumentException("Assessment ids must be positive");
            }
            if (seenAssessmentIds.add(value.assessmentId())) {
                unique.add(value);
            }
        }
        return List.copyOf(unique);
    }
}
