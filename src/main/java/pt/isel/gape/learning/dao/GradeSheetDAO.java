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

public final class GradeSheetDAO implements pt.isel.gape.transversal.service.ApplicationReadService.GradeSheets {

    private static final String CLASS_GROUP_SCOPE = "class_group";
    private static final String SUBJECT_OCCURRENCE_SCOPE = "subject_occurrence";

    private final ConnectionProvider connectionProvider;

    public GradeSheetDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, GradeSheetCreateCommand command) throws SQLException {
        List<Long> classGroupIds = orderedUnique(command.classGroupIds(), "Class group ids must be positive");
        long courseOccurrenceId = resolveGradeSheetOccurrence(
                connection,
                command.subjectId(),
                command.courseOccurrenceId(),
                classGroupIds
        );
        String sql = """
                INSERT INTO grade_sheet (
                    id_subject, id_course_occurrence, title, type, max_grade, passing_grade, released_at, state,
                    scope, subject_occurrence_aggregate_id
                ) VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, command.subjectId());
            statement.setLong(2, courseOccurrenceId);
            statement.setString(3, command.title().trim());
            statement.setString(4, command.type().toDatabaseValue());
            statement.setBigDecimal(5, command.maxGrade());
            statement.setBigDecimal(6, command.passingGrade());
            statement.setString(7, command.state().toDatabaseValue());
            boolean subjectOccurrenceAggregate = classGroupIds.isEmpty();
            statement.setString(8, subjectOccurrenceAggregate ? SUBJECT_OCCURRENCE_SCOPE : CLASS_GROUP_SCOPE);
            if (subjectOccurrenceAggregate) {
                statement.setLong(9, courseOccurrenceId);
            } else {
                statement.setNull(9, Types.BIGINT);
            }
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
        long courseOccurrenceId = resolveGradeSheetOccurrence(
                connection,
                command.subjectId(),
                command.courseOccurrenceId(),
                command.classGroupIds()
        );
        String sql = """
                UPDATE grade_sheet
                SET id_subject = ?, id_course_occurrence = ?, title = ?, type = ?, max_grade = ?, passing_grade = ?
                WHERE id_grade_sheet = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, command.subjectId());
            statement.setLong(2, courseOccurrenceId);
            statement.setString(3, command.title().trim());
            statement.setString(4, command.type().toDatabaseValue());
            statement.setBigDecimal(5, command.maxGrade());
            statement.setBigDecimal(6, command.passingGrade());
            statement.setLong(7, gradeSheetId);
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
        updateState(connection, gradeSheetId, state, releasedAt, null);
    }

    public void updateState(
            Connection connection,
            long gradeSheetId,
            GradeSheetState state,
            LocalDateTime releasedAt,
            String publicationExplanation
    ) throws SQLException {
        String sql = """
                UPDATE grade_sheet
                SET state = ?, released_at = ?, publication_explanation = ?
                WHERE id_grade_sheet = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            setTimestamp(statement, 2, releasedAt);
            setNullableString(statement, 3, publicationExplanation);
            statement.setLong(4, gradeSheetId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Grade sheet not found: " + gradeSheetId);
            }
        }
    }

    public void updatePublicationExplanation(
            Connection connection,
            long gradeSheetId,
            String publicationExplanation
    ) throws SQLException {
        String sql = """
                UPDATE grade_sheet
                SET publication_explanation = ?
                WHERE id_grade_sheet = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableString(statement, 1, publicationExplanation);
            statement.setLong(2, gradeSheetId);
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

    /**
     * Returns only the sheets that belong to one subject.  This keeps subject
     * details from walking every grade sheet in the platform just to render one
     * subject's summary.
     */
    public List<GradeSheet> findBySubject(long subjectId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            String sql = selectGradeSheetSql() + " WHERE id_subject = ? ORDER BY id_grade_sheet DESC";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, subjectId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<GradeSheet> gradeSheets = new ArrayList<>();
                    while (resultSet.next()) {
                        gradeSheets.add(mapGradeSheet(connection, resultSet));
                    }
                    return List.copyOf(gradeSheets);
                }
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
                  AND grade_sheet.scope = 'class_group'
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

    public Optional<GradeSheet> findFinalByClassGroupId(Connection connection, long classGroupId)
            throws SQLException {
        String sql = selectGradeSheetSql() + """
                JOIN associate_grade_sheet_class_group agscg
                  ON agscg.id_grade_sheet = grade_sheet.id_grade_sheet
                WHERE agscg.id_class_group = ?
                  AND grade_sheet.type = 'final'
                  AND grade_sheet.scope = 'class_group'
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

    /**
     * A class group has one final source grade sheet.  Other sheet types may
     * still coexist for interim assessment work, but they must not become a
     * second source for the subject-occurrence consolidation.
     */
    public boolean hasFinalClassGroupGradeSheet(
            Connection connection,
            List<Long> classGroupIds,
            Long excludedGradeSheetId
    ) throws SQLException {
        List<Long> normalizedClassGroupIds = orderedUnique(classGroupIds, "Class group ids must be positive");
        if (normalizedClassGroupIds.isEmpty()) {
            return false;
        }
        String placeholders = String.join(",", Collections.nCopies(normalizedClassGroupIds.size(), "?"));
        String sql = """
                SELECT 1
                FROM grade_sheet gs
                JOIN associate_grade_sheet_class_group agscg
                  ON agscg.id_grade_sheet = gs.id_grade_sheet
                WHERE gs.scope = 'class_group'
                  AND gs.type = 'final'
                  AND agscg.id_class_group IN (%s)
                  AND (? IS NULL OR gs.id_grade_sheet <> ?)
                LIMIT 1
                """.formatted(placeholders);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Long classGroupId : normalizedClassGroupIds) {
                statement.setLong(index++, classGroupId);
            }
            if (excludedGradeSheetId == null) {
                statement.setNull(index++, Types.BIGINT);
                statement.setNull(index, Types.BIGINT);
            } else {
                statement.setLong(index++, excludedGradeSheetId);
                statement.setLong(index, excludedGradeSheetId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public Optional<GradeSheet> findBySubjectWithoutClassGroups(Connection connection, long subjectId)
            throws SQLException {
        String sql = selectGradeSheetSql() + """
                WHERE grade_sheet.id_subject = ?
                  AND grade_sheet.scope = 'subject_occurrence'
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

    public Optional<GradeSheet> findBySubjectWithoutClassGroups(
            Connection connection,
            long subjectId,
            long courseOccurrenceId
    )
            throws SQLException {
        String sql = selectGradeSheetSql() + """
                WHERE grade_sheet.id_subject = ?
                  AND grade_sheet.id_course_occurrence = ?
                  AND grade_sheet.scope = 'subject_occurrence'
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
            statement.setLong(2, courseOccurrenceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapGradeSheet(connection, resultSet));
            }
        }
    }

    /**
     * Returns the source grade sheets for one subject in one course occurrence.
     * A source grade sheet is associated with at least one class group; the
     * corresponding grade sheet without class groups is the subject-level
     * consolidated grade sheet.
     */
    public List<GradeSheet> findClassGroupGradeSheetsForSubjectOccurrence(
            Connection connection,
            long subjectId,
            long courseOccurrenceId
    ) throws SQLException {
        String sql = selectGradeSheetSql() + """
                WHERE grade_sheet.id_subject = ?
                  AND grade_sheet.id_course_occurrence = ?
                  AND grade_sheet.type = 'final'
                  AND grade_sheet.scope = 'class_group'
                  AND EXISTS (
                        SELECT 1
                        FROM associate_grade_sheet_class_group agscg
                        WHERE agscg.id_grade_sheet = grade_sheet.id_grade_sheet
                  )
                ORDER BY grade_sheet.id_grade_sheet
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            statement.setLong(2, courseOccurrenceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<GradeSheet> gradeSheets = new ArrayList<>();
                while (resultSet.next()) {
                    gradeSheets.add(mapGradeSheet(connection, resultSet));
                }
                return List.copyOf(gradeSheets);
            }
        }
    }

    /**
     * Grade sheets that can still have their automatic lifecycle reconciled.
     * Closed and inactive sheets are deliberately excluded from conformance
     * because their state is terminal.
     */
    public List<Long> findSynchronizableGradeSheetIds(Connection connection) throws SQLException {
        String sql = """
                SELECT id_grade_sheet
                FROM grade_sheet
                WHERE state IN ('draft', 'published')
                ORDER BY id_grade_sheet
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return longList(resultSet, "id_grade_sheet");
        }
    }

    /**
     * Returns grade sheets whose whole academic context belongs to an ended,
     * non-cancelled course-occurrence period.  A sheet associated with more
     * than one class group is eligible only when every one of those periods
     * has ended.
     */
    public List<Long> findGradeSheetIdsForCompletedPeriods(Connection connection, LocalDate today)
            throws SQLException {
        String sql = """
                SELECT DISTINCT eligible.id_grade_sheet
                FROM (
                    SELECT gs.id_grade_sheet
                    FROM grade_sheet gs
                    WHERE gs.state IN ('draft', 'published')
                      AND gs.scope = 'class_group'
                      AND EXISTS (
                            SELECT 1
                            FROM associate_grade_sheet_class_group agscg
                            WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                        )
                      AND NOT EXISTS (
                            SELECT 1
                            FROM associate_grade_sheet_class_group agscg
                            JOIN class_group cg
                              ON cg.id_class_group = agscg.id_class_group
                            JOIN course_occurrence_period cop
                              ON cop.id_course_occurrence_period = cg.id_course_occurrence_period
                            WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                              AND (cop.state = 'cancelled' OR cop.ends_at >= ?)
                        )

                    UNION

                    SELECT gs.id_grade_sheet
                    FROM grade_sheet gs
                    JOIN class_group cg
                      ON cg.id_subject = gs.id_subject
                     AND cg.id_course_occurrence = gs.id_course_occurrence
                    JOIN course_occurrence_period cop
                      ON cop.id_course_occurrence_period = cg.id_course_occurrence_period
                    WHERE gs.state IN ('draft', 'published')
                      AND gs.scope = 'subject_occurrence'
                      AND NOT EXISTS (
                            SELECT 1
                            FROM associate_grade_sheet_class_group agscg
                            WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                        )
                      AND cop.state <> 'cancelled'
                      AND cop.ends_at < ?
                ) eligible
                ORDER BY eligible.id_grade_sheet
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, java.sql.Date.valueOf(today));
            statement.setDate(2, java.sql.Date.valueOf(today));
            try (ResultSet resultSet = statement.executeQuery()) {
                return longList(resultSet, "id_grade_sheet");
            }
        }
    }

    public void replaceClassGroups(
            Connection connection,
            long gradeSheetId,
            List<Long> classGroupIds
    ) throws SQLException {
        List<Long> normalizedClassGroupIds = orderedUnique(classGroupIds, "Class group ids must be positive");
        if (normalizedClassGroupIds.isEmpty() && isClassGroupScoped(connection, gradeSheetId)) {
            throw new IllegalArgumentException("Class-group grade sheets must retain at least one class group");
        }
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM associate_grade_sheet_class_group WHERE id_grade_sheet = ?")) {
            delete.setLong(1, gradeSheetId);
            delete.executeUpdate();
        }
        if (normalizedClassGroupIds.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO associate_grade_sheet_class_group (id_grade_sheet, id_class_group)
                VALUES (?, ?)
                """;
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            for (Long classGroupId : normalizedClassGroupIds) {
                insert.setLong(1, gradeSheetId);
                insert.setLong(2, classGroupId);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    /**
     * A consolidated subject sheet exists only when this exact subject and
     * course-occurrence context has at least one real class group.  The check
     * deliberately ignores the current state of the group: completed groups
     * are academic evidence and must remain part of the aggregation.
     */
    public boolean hasClassGroupsForSubjectOccurrence(
            Connection connection,
            long subjectId,
            long courseOccurrenceId
    ) throws SQLException {
        String sql = """
                SELECT 1
                FROM class_group
                WHERE id_subject = ?
                  AND id_course_occurrence = ?
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            statement.setLong(2, courseOccurrenceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    /**
     * Removes subject-occurrence sheets whose source context no longer exists.
     * Their derived grade records and certificate links cannot remain valid, so
     * they are removed transactionally and affected certificates return to a
     * draft state for normal lifecycle recalculation.
     */
    public void removeOrphanSubjectOccurrenceGradeSheets(Connection connection) throws SQLException {
        String sql = """
                SELECT gs.id_grade_sheet
                FROM grade_sheet gs
                WHERE gs.scope = 'subject_occurrence'
                  AND NOT EXISTS (
                        SELECT 1
                        FROM class_group cg
                        WHERE cg.id_subject = gs.id_subject
                          AND cg.id_course_occurrence = gs.id_course_occurrence
                  )
                ORDER BY gs.id_grade_sheet
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            deleteGradeSheetsWithDerivedData(connection, longList(resultSet, "id_grade_sheet"));
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

    /**
     * Resolves assessments from the actual class-group occurrence context.
     * This overload is required by consolidated subject-occurrence sheets:
     * a subject can be integrated into several courses whose calendars and
     * occurrences are unrelated, so the subject alone is never enough to
     * select assessments.
     */
    public List<Long> findAssessmentIdsForSheetContext(
            long subjectId,
            long courseOccurrenceId,
            List<Long> classGroupIds
    ) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findAssessmentIdsForSheetContext(
                    connection,
                    subjectId,
                    courseOccurrenceId,
                    classGroupIds
            );
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

    public List<Long> findAssessmentIdsForSheetContext(
            Connection connection,
            long subjectId,
            long courseOccurrenceId,
            List<Long> classGroupIds
    ) throws SQLException {
        List<Long> uniqueClassGroupIds = orderedUnique(classGroupIds, "Class group ids must be positive");
        if (!uniqueClassGroupIds.isEmpty()) {
            return findAssessmentIdsForSheetContext(connection, subjectId, uniqueClassGroupIds);
        }
        if (courseOccurrenceId <= 0) {
            return List.of();
        }
        String sql = """
                SELECT DISTINCT context_assessment.id_assessment
                FROM (
                    SELECT a.id_assessment
                    FROM assessment a
                    JOIN content_block cb ON cb.id_content_block = a.id_content_block
                    JOIN class_group cg ON cg.id_class_group = cb.id_class_group
                    WHERE cg.id_subject = ?
                      AND cg.id_course_occurrence = ?
                      AND (a.id_subject IS NULL OR a.id_subject = cg.id_subject)
                    UNION
                    SELECT a.id_assessment
                    FROM assessment a
                    JOIN assessment_class_group acg ON acg.id_assessment = a.id_assessment
                    JOIN class_group cg ON cg.id_class_group = acg.id_class_group
                    WHERE cg.id_subject = ?
                      AND cg.id_course_occurrence = ?
                      AND (a.id_subject IS NULL OR a.id_subject = cg.id_subject)
                ) context_assessment
                ORDER BY context_assessment.id_assessment
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            statement.setLong(2, courseOccurrenceId);
            statement.setLong(3, subjectId);
            statement.setLong(4, courseOccurrenceId);
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

    public List<GradeAssessmentWeight> findDefaultAssessmentWeightsForSheetContext(
            Connection connection,
            long subjectId,
            long courseOccurrenceId,
            List<Long> classGroupIds
    ) throws SQLException {
        List<Long> assessmentIds = findAssessmentIdsForSheetContext(
                connection,
                subjectId,
                courseOccurrenceId,
                classGroupIds
        );
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

    private static long resolveGradeSheetOccurrence(
            Connection connection,
            long subjectId,
            Long requestedCourseOccurrenceId,
            List<Long> classGroupIds
    ) throws SQLException {
        if (requestedCourseOccurrenceId != null && requestedCourseOccurrenceId <= 0) {
            throw new IllegalArgumentException("Course occurrence id must be positive");
        }
        List<Long> uniqueClassGroupIds = orderedUnique(classGroupIds, "Class group ids must be positive");
        if (!uniqueClassGroupIds.isEmpty()) {
            String placeholders = String.join(",", Collections.nCopies(uniqueClassGroupIds.size(), "?"));
            String sql = """
                    SELECT DISTINCT id_course_occurrence
                    FROM class_group
                    WHERE id_subject = ?
                      AND id_class_group IN (%s)
                    """.formatted(placeholders);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, subjectId);
                int index = 2;
                for (Long classGroupId : uniqueClassGroupIds) {
                    statement.setLong(index++, classGroupId);
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<Long> occurrenceIds = longList(resultSet, "id_course_occurrence");
                    if (occurrenceIds.size() != 1) {
                        throw new IllegalArgumentException(
                                "Grade sheet class groups must belong to one subject occurrence context"
                        );
                    }
                    long resolved = occurrenceIds.getFirst();
                    if (requestedCourseOccurrenceId != null && requestedCourseOccurrenceId != resolved) {
                        throw new IllegalArgumentException(
                                "Grade sheet occurrence must match the selected class groups"
                        );
                    }
                    return resolved;
                }
            }
        }
        if (requestedCourseOccurrenceId == null) {
            throw new IllegalArgumentException("Course occurrence is required for a grade sheet without class groups");
        }
        return requestedCourseOccurrenceId;
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
                WHERE gs.id_grade_sheet IN (%s)
                  AND (
                        (
                            gs.scope = 'class_group'
                            AND EXISTS (
                                SELECT 1
                                FROM associate_grade_sheet_class_group agscg
                                JOIN class_group cg ON cg.id_class_group = agscg.id_class_group
                                WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                                  AND cg.id_course = ?
                            )
                        )
                        OR (
                            gs.scope = 'subject_occurrence'
                            AND EXISTS (
                                SELECT 1
                                FROM course_occurrence co
                                WHERE co.id_course_occurrence = gs.id_course_occurrence
                                  AND co.id_course = ?
                            )
                        )
                  )
                """.formatted(placeholders);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Long gradeSheetId : uniqueIds) {
                statement.setLong(index++, gradeSheetId);
            }
            statement.setLong(index++, courseId);
            statement.setLong(index, courseId);
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
            return List.copyOf(studentUserIds);
        }
        String sql = """
                SELECT DISTINCT ecg.id_student_user
                FROM class_group cg
                JOIN enroll_class_group ecg
                  ON ecg.id_class_group = cg.id_class_group
                WHERE cg.id_subject = ?
                  AND cg.id_course_occurrence = ?
                  AND ecg.state IN ('active', 'completed')
                ORDER BY ecg.id_student_user
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gradeSheet.subjectId());
            statement.setLong(2, gradeSheet.courseOccurrenceId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    studentUserIds.add(resultSet.getLong("id_student_user"));
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
                    JOIN enroll_course ec ON ec.id_student_user = ecg.id_student_user
                        AND ec.id_course = cg.id_course
                        AND ec.id_course_occurrence = cg.id_course_occurrence
                    WHERE agscg.id_grade_sheet = ?
                      AND ecg.id_student_user = ?
                      AND ecg.state IN ('active', 'completed')
                      AND ec.state IN ('active', 'completed')
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
            return List.copyOf(courseIds);
        }
        String sql = """
                SELECT DISTINCT cg.id_course
                FROM class_group cg
                JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group
                JOIN enroll_course ec ON ec.id_student_user = ecg.id_student_user
                    AND ec.id_course = cg.id_course
                    AND ec.id_course_occurrence = cg.id_course_occurrence
                WHERE cg.id_subject = ?
                  AND cg.id_course_occurrence = ?
                  AND ecg.id_student_user = ?
                  AND ecg.state IN ('active', 'completed')
                  AND ec.state IN ('active', 'completed')
                ORDER BY cg.id_course
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gradeSheet.subjectId());
            statement.setLong(2, gradeSheet.courseOccurrenceId());
            statement.setLong(3, studentUserId);
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
                ORDER BY matched.id_grade_sheet
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            statement.setLong(2, assessmentId);
            statement.setLong(3, assessmentId);
            statement.setLong(4, assessmentId);
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

    public List<Long> findGradeSheetIdsForCompletedClassGroups(Connection connection) throws SQLException {
        String sql = """
                SELECT DISTINCT gs.id_grade_sheet
                FROM grade_sheet gs
                JOIN associate_grade_sheet_class_group agscg
                  ON agscg.id_grade_sheet = gs.id_grade_sheet
                JOIN class_group cg
                  ON cg.id_class_group = agscg.id_class_group
                WHERE gs.state IN ('draft', 'published')
                  AND gs.scope = 'class_group'
                  AND cg.state = 'completed'
                ORDER BY gs.id_grade_sheet
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return longList(resultSet, "id_grade_sheet");
        }
    }

    public boolean allClassGroupsCompleted(Connection connection, List<Long> classGroupIds)
            throws SQLException {
        List<Long> uniqueClassGroupIds = orderedUnique(classGroupIds, "Class group ids must be positive");
        if (uniqueClassGroupIds.isEmpty()) {
            return false;
        }
        String placeholders = String.join(",", Collections.nCopies(uniqueClassGroupIds.size(), "?"));
        String sql = """
                SELECT COUNT(*)
                FROM class_group
                WHERE id_class_group IN (%s)
                  AND state = 'completed'
                """.formatted(placeholders);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Long classGroupId : uniqueClassGroupIds) {
                statement.setLong(index++, classGroupId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) == uniqueClassGroupIds.size();
            }
        }
    }

    public boolean classGroupsEndedBefore(List<Long> classGroupIds, LocalDate date) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return classGroupsEndedBefore(connection, classGroupIds, date);
        }
    }

    private boolean isClassGroupScoped(Connection connection, long gradeSheetId) throws SQLException {
        String sql = """
                SELECT scope
                FROM grade_sheet
                WHERE id_grade_sheet = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gradeSheetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Grade sheet not found: " + gradeSheetId);
                }
                return CLASS_GROUP_SCOPE.equals(resultSet.getString("scope"));
            }
        }
    }

    private static void deleteGradeSheetsWithDerivedData(Connection connection, List<Long> gradeSheetIds)
            throws SQLException {
        if (gradeSheetIds == null || gradeSheetIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(gradeSheetIds.size(), "?"));
        String affectedCertificateSql = """
                UPDATE certificate c
                JOIN based_on_grade_sheet_certificate bgsc
                  ON bgsc.id_certificate = c.id_certificate
                SET c.state = 'draft',
                    c.validation_code = NULL,
                    c.issued_at = NULL,
                    c.final_grade = NULL
                WHERE bgsc.id_grade_sheet IN (%s)
                """.formatted(placeholders);
        String deleteCertificateLinksSql = "DELETE FROM based_on_grade_sheet_certificate WHERE id_grade_sheet IN (%s)"
                .formatted(placeholders);
        String deleteGradeRecordsSql = "DELETE FROM grade_record WHERE id_grade_sheet IN (%s)"
                .formatted(placeholders);
        String deleteGradeSheetsSql = "DELETE FROM grade_sheet WHERE id_grade_sheet IN (%s)".formatted(placeholders);

        executeForGradeSheetIds(connection, affectedCertificateSql, gradeSheetIds);
        executeForGradeSheetIds(connection, deleteCertificateLinksSql, gradeSheetIds);
        executeForGradeSheetIds(connection, deleteGradeRecordsSql, gradeSheetIds);
        executeForGradeSheetIds(connection, deleteGradeSheetsSql, gradeSheetIds);
    }

    private static void executeForGradeSheetIds(
            Connection connection,
            String sql,
            List<Long> gradeSheetIds
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Long gradeSheetId : gradeSheetIds) {
                statement.setLong(index++, gradeSheetId);
            }
            statement.executeUpdate();
        }
    }

    private static String selectGradeSheetSql() {
        return """
                SELECT grade_sheet.id_grade_sheet, grade_sheet.id_subject, grade_sheet.title, grade_sheet.type,
                       grade_sheet.id_course_occurrence,
                       grade_sheet.max_grade, grade_sheet.passing_grade, grade_sheet.weight_alert,
                       grade_sheet.publication_explanation, grade_sheet.released_at, grade_sheet.state
                FROM grade_sheet
                """;
    }

    private GradeSheet mapGradeSheet(Connection connection, ResultSet resultSet) throws SQLException {
        long id = resultSet.getLong("id_grade_sheet");
        return new GradeSheet(
                id,
                resultSet.getLong("id_subject"),
                resultSet.getLong("id_course_occurrence"),
                resultSet.getString("title"),
                GradeSheetType.fromDatabaseValue(resultSet.getString("type")),
                resultSet.getBigDecimal("max_grade"),
                resultSet.getBigDecimal("passing_grade"),
                resultSet.getString("weight_alert"),
                resultSet.getString("publication_explanation"),
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
