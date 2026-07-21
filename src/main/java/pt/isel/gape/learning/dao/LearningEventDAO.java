package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.LearningEvent;

public final class LearningEventDAO implements pt.isel.gape.transversal.service.ApplicationReadService.LearningEvents {

    private final ConnectionProvider connectionProvider;

    public LearningEventDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    private static String compactEntitySql(String alias) {
        return "COALESCE(NULLIF(" + alias + ".acronym, ''), " + alias + ".name)";
    }

    private static String entityTitleSql(String alias) {
        return "COALESCE(NULLIF(" + alias + ".name, ''), " + compactEntitySql(alias) + ")";
    }

    private static String organicUnitSuffixSql(String alias) {
        return ", CASE WHEN " + alias + ".id_organic_unit IS NULL THEN '' ELSE CONCAT(' | ', "
                + compactEntitySql(alias) + ") END";
    }

    private static String organicUnitTitleSuffixSql(String alias) {
        return ", CASE WHEN " + alias + ".id_organic_unit IS NULL THEN '' ELSE CONCAT(' | ', "
                + entityTitleSql(alias) + ") END";
    }

    private static String classGroupContextSql(
            String classGroupAlias,
            String subjectAlias,
            String courseAlias,
            String organicUnitAlias,
            String organizationAlias
    ) {
        return "CONCAT(" + classGroupAlias + ".cod_class_group, ' | ', "
                + compactEntitySql(subjectAlias) + ", ' | ', "
                + compactEntitySql(courseAlias)
                + organicUnitSuffixSql(organicUnitAlias)
                + ", ' | ', " + compactEntitySql(organizationAlias) + ")";
    }

    private static String classGroupContextTitleSql(
            String classGroupAlias,
            String subjectAlias,
            String courseAlias,
            String organicUnitAlias,
            String organizationAlias
    ) {
        return "CONCAT(" + classGroupAlias + ".cod_class_group, ' | ', "
                + entityTitleSql(subjectAlias) + ", ' | ', "
                + entityTitleSql(courseAlias)
                + organicUnitTitleSuffixSql(organicUnitAlias)
                + ", ' | ', " + entityTitleSql(organizationAlias) + ")";
    }

    private static String subjectCourseContextSql(
            String subjectAlias,
            String courseAlias,
            String organicUnitAlias,
            String organizationAlias
    ) {
        return "CONCAT(" + compactEntitySql(subjectAlias) + ", ' | ', "
                + compactEntitySql(courseAlias)
                + organicUnitSuffixSql(organicUnitAlias)
                + ", ' | ', " + compactEntitySql(organizationAlias) + ")";
    }

    private static String subjectCourseContextTitleSql(
            String subjectAlias,
            String courseAlias,
            String organicUnitAlias,
            String organizationAlias
    ) {
        return "CONCAT(" + entityTitleSql(subjectAlias) + ", ' | ', "
                + entityTitleSql(courseAlias)
                + organicUnitTitleSuffixSql(organicUnitAlias)
                + ", ' | ', " + entityTitleSql(organizationAlias) + ")";
    }

    private static String courseContextSql(
            String courseAlias,
            String organicUnitAlias,
            String organizationAlias
    ) {
        return "CONCAT(" + compactEntitySql(courseAlias)
                + organicUnitSuffixSql(organicUnitAlias)
                + ", ' | ', " + compactEntitySql(organizationAlias) + ")";
    }

    private static String courseContextTitleSql(
            String courseAlias,
            String organicUnitAlias,
            String organizationAlias
    ) {
        return "CONCAT(" + entityTitleSql(courseAlias)
                + organicUnitTitleSuffixSql(organicUnitAlias)
                + ", ' | ', " + entityTitleSql(organizationAlias) + ")";
    }

    private static String subjectContextSql(String subjectAlias, String organizationAlias) {
        return "CONCAT(" + compactEntitySql(subjectAlias) + ", ' | ', "
                + compactEntitySql(organizationAlias) + ")";
    }

    private static String subjectContextTitleSql(String subjectAlias, String organizationAlias) {
        return "CONCAT(" + entityTitleSql(subjectAlias) + ", ' | ', "
                + entityTitleSql(organizationAlias) + ")";
    }

    public int countVisible(
            Collection<Long> visibleClassGroupIds,
            Collection<Long> visibleCourseIds,
            Collection<Long> visibleSubjectIds,
            Long studentUserId,
            boolean studentProfile,
            boolean administratorProfile,
            String category,
            Long classGroupFilter,
            Long courseFilter,
            Long subjectFilter,
            LocalDateTime now
    ) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            QueryParts parts = visibleQuery(
                    "SELECT COUNT(*) FROM learning_event WHERE visibility_state = 'visible'",
                    visibleClassGroupIds,
                    visibleCourseIds,
                    visibleSubjectIds,
                    studentUserId,
                    studentProfile,
                    administratorProfile,
                    category,
                    classGroupFilter,
                    courseFilter,
                    subjectFilter,
                    now
            );
            try (PreparedStatement statement = connection.prepareStatement(parts.sql())) {
                bind(statement, parts.parameters());
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getInt(1);
                }
            }
        }
    }

    public int countUnreadVisible(
            Collection<Long> visibleClassGroupIds,
            Collection<Long> visibleCourseIds,
            Collection<Long> visibleSubjectIds,
            Long studentUserId,
            boolean studentProfile,
            boolean administratorProfile,
            String category,
            Long classGroupFilter,
            Long courseFilter,
            Long subjectFilter,
            LocalDateTime now,
            long viewerUserId
    ) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            QueryParts parts = visibleQuery(
                    """
                            SELECT COUNT(*)
                            FROM learning_event le
                            LEFT JOIN learning_event_read ler
                              ON ler.id_learning_event = le.id_learning_event
                             AND ler.id_user = ?
                            WHERE visibility_state = 'visible'
                            """,
                    visibleClassGroupIds,
                    visibleCourseIds,
                    visibleSubjectIds,
                    studentUserId,
                    studentProfile,
                    administratorProfile,
                    category,
                    classGroupFilter,
                    courseFilter,
                    subjectFilter,
                    now
            );
            String sql = parts.sql() + " AND ler.id_learning_event IS NULL";
            List<Object> parameters = new ArrayList<>();
            parameters.add(viewerUserId);
            parameters.addAll(parts.parameters());
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getInt(1);
                }
            }
        }
    }

    public List<LearningEvent> findVisible(
            Collection<Long> visibleClassGroupIds,
            Collection<Long> visibleCourseIds,
            Collection<Long> visibleSubjectIds,
            Long studentUserId,
            boolean studentProfile,
            boolean administratorProfile,
            String category,
            Long classGroupFilter,
            Long courseFilter,
            Long subjectFilter,
            LocalDateTime now,
            long viewerUserId,
            int limit,
            int offset
    ) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            QueryParts parts = visibleQuery(
                    selectSqlWithRead() + " WHERE visibility_state = 'visible'",
                    visibleClassGroupIds,
                    visibleCourseIds,
                    visibleSubjectIds,
                    studentUserId,
                    studentProfile,
                    administratorProfile,
                    category,
                    classGroupFilter,
                    courseFilter,
                    subjectFilter,
                    now
            );
            String sql = parts.sql() + " ORDER BY le.occurred_at DESC, le.id_learning_event DESC LIMIT ? OFFSET ?";
            List<Object> parameters = new ArrayList<>();
            parameters.add(viewerUserId);
            parameters.addAll(parts.parameters());
            parameters.add(Math.max(1, limit));
            parameters.add(Math.max(0, offset));
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<LearningEvent> events = new ArrayList<>();
                    while (resultSet.next()) {
                        events.add(mapEvent(resultSet));
                    }
                    return List.copyOf(events);
                }
            }
        }
    }

    public Optional<LearningEvent> findVisibleById(
            Collection<Long> visibleClassGroupIds,
            Collection<Long> visibleCourseIds,
            Collection<Long> visibleSubjectIds,
            Long studentUserId,
            boolean studentProfile,
            boolean administratorProfile,
            LocalDateTime now,
            long viewerUserId,
            long eventId
    ) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            QueryParts parts = visibleQuery(
                    selectSqlWithRead() + " WHERE visibility_state = 'visible'",
                    visibleClassGroupIds,
                    visibleCourseIds,
                    visibleSubjectIds,
                    studentUserId,
                    studentProfile,
                    administratorProfile,
                    null,
                    null,
                    null,
                    null,
                    now
            );
            String sql = parts.sql() + " AND le.id_learning_event = ?";
            List<Object> parameters = new ArrayList<>();
            parameters.add(viewerUserId);
            parameters.addAll(parts.parameters());
            parameters.add(eventId);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(mapEvent(resultSet)) : Optional.empty();
                }
            }
        }
    }

    public void markReadById(long viewerUserId, long eventId, LocalDateTime readAt) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO learning_event_read (id_learning_event, id_user, read_at)
                    SELECT id_learning_event, ?, ?
                    FROM learning_event
                    WHERE id_learning_event = ?
                      AND visibility_state = 'visible'
                      AND occurred_at <= ?
                    ON DUPLICATE KEY UPDATE read_at = read_at
                    """)) {
                statement.setLong(1, viewerUserId);
                statement.setTimestamp(2, Timestamp.valueOf(readAt));
                statement.setLong(3, eventId);
                statement.setTimestamp(4, Timestamp.valueOf(readAt));
                statement.executeUpdate();
            }
        }
    }

    public void markReadByHref(
            long viewerUserId,
            String href,
            boolean includeAnchoredChildren,
            LocalDateTime readAt
    ) throws SQLException {
        if (href == null || href.isBlank()) {
            return;
        }
        String normalizedHref = href.trim();
        try (Connection connection = connectionProvider.getConnection()) {
            String sql = """
                    INSERT INTO learning_event_read (id_learning_event, id_user, read_at)
                    SELECT id_learning_event, ?, ?
                    FROM learning_event
                    WHERE visibility_state = 'visible'
                      AND occurred_at <= ?
                      AND (
                            detail_href = ?
                            %s
                      )
                    ON DUPLICATE KEY UPDATE read_at = read_at
                    """.formatted(includeAnchoredChildren ? "OR detail_href LIKE CONCAT(?, '#%')" : "");
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, viewerUserId);
                statement.setTimestamp(2, Timestamp.valueOf(readAt));
                statement.setTimestamp(3, Timestamp.valueOf(readAt));
                statement.setString(4, normalizedHref);
                if (includeAnchoredChildren) {
                    statement.setString(5, normalizedHref);
                }
                statement.executeUpdate();
            }
        }
    }

    public void rebuildFromCurrentRecords(Connection connection) throws SQLException {
        hideRebuildableEvents(connection);
        upsertLessonEvents(connection);
        upsertAssessmentEvents(connection);
        upsertAttendanceEvents(connection);
        upsertAbsenceJustificationEvents(connection);
        upsertGradeSheetEvents(connection);
        upsertCertificateEvents(connection);
        upsertStudentClassGroupEnrollmentAcceptedEvents(connection);
        upsertStudentRunningLessonEvents(connection);
        upsertStudentAssessmentEnrollmentRequiredEvents(connection);
        upsertStudentAssessmentAttemptRequiredEvents(connection);
        upsertStudentGradeAvailableEvents(connection);
        upsertStudentCertificatePublishedEvents(connection);
        upsertClassGroupEnrollmentEvents(connection);
        upsertAssessmentEnrollmentEvents(connection);
        upsertCourseEnrollmentEvents(connection);
        upsertClassGroupEvents(connection);
        upsertAttemptEvents(connection);
        upsertTeacherAssignmentEvents(connection);
        upsertSubjectCoordinationEvents(connection);
        upsertOrganizationManagementEvents(connection);
        upsertDeletionRequestEvents(connection);
        upsertUserAccountEvents(connection);
    }

    /**
     * Rebuilds the derived event feed in one small transaction.  Mutating
     * controllers call this after a lifecycle action has committed so event
     * badges never keep a stale pending request or correction count.
     */
    public void rebuildFromCurrentRecords() throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                rebuildFromCurrentRecords(connection);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    private static QueryParts visibleQuery(
            String baseSql,
            Collection<Long> visibleClassGroupIds,
            Collection<Long> visibleCourseIds,
            Collection<Long> visibleSubjectIds,
            Long studentUserId,
            boolean studentProfile,
            boolean administratorProfile,
            String category,
            Long classGroupFilter,
            Long courseFilter,
            Long subjectFilter,
            LocalDateTime now
    ) {
        StringBuilder sql = new StringBuilder(baseSql);
        List<Object> parameters = new ArrayList<>();

        // Actionable requests/corrections are immediate even when an imported
        // membership date is in the future (and even when the DB and JVM use
        // different time zones). Historical events still respect the viewer's
        // time boundary.
        sql.append(" AND (occurred_at <= ? OR event_type IN ("
                + "'class_group_enrollment_pending', 'assessment_enrollment_pending', "
                + "'assessment_correction_pending'))");
        parameters.add(now);

        String normalizedCategory = normalizeCategory(category);
        if (normalizedCategory != null) {
            sql.append(" AND category = ?");
            parameters.add(normalizedCategory);
        }

        appendContextFilter(sql, parameters, classGroupFilter, courseFilter, subjectFilter);
        appendVisibilityFilter(
                sql,
                parameters,
                visibleClassGroupIds,
                visibleCourseIds,
                visibleSubjectIds,
                studentUserId,
                studentProfile,
                administratorProfile
        );
        return new QueryParts(sql.toString(), parameters);
    }

    private static void appendContextFilter(
            StringBuilder sql,
            List<Object> parameters,
            Long classGroupFilter,
            Long courseFilter,
            Long subjectFilter
    ) {
        if (classGroupFilter == null) {
            return;
        }
        List<String> conditions = new ArrayList<>();
        conditions.add("id_class_group = ?");
        parameters.add(classGroupFilter);
        if (courseFilter != null) {
            conditions.add("(id_class_group IS NULL AND id_course = ?)");
            parameters.add(courseFilter);
        }
        if (subjectFilter != null) {
            conditions.add("(id_class_group IS NULL AND id_subject = ?)");
            parameters.add(subjectFilter);
        }
        sql.append(" AND (").append(String.join(" OR ", conditions)).append(")");
    }

    private static void appendVisibilityFilter(
            StringBuilder sql,
            List<Object> parameters,
            Collection<Long> visibleClassGroupIds,
            Collection<Long> visibleCourseIds,
            Collection<Long> visibleSubjectIds,
            Long studentUserId,
            boolean studentProfile,
            boolean administratorProfile
    ) {
        List<String> contextConditions = contextConditions(parameters, visibleClassGroupIds, visibleCourseIds, visibleSubjectIds);
        if (studentProfile) {
            List<String> studentConditions = new ArrayList<>();
            if (studentUserId != null) {
                studentConditions.add("id_student_user = ?");
                parameters.add(studentUserId);
            }
            if (!contextConditions.isEmpty()) {
                studentConditions.add("(id_student_user IS NULL AND (" + String.join(" OR ", contextConditions) + "))");
            }
            if (studentConditions.isEmpty()) {
                sql.append(" AND 1 = 0");
            } else {
                sql.append(" AND (").append(String.join(" OR ", studentConditions)).append(")");
            }
            return;
        }
        if (contextConditions.isEmpty()) {
            sql.append(administratorProfile
                    ? " AND id_class_group IS NULL AND id_course IS NULL AND id_subject IS NULL"
                    : " AND 1 = 0");
        } else {
            sql.append(" AND (").append(String.join(" OR ", contextConditions));
            if (administratorProfile) {
                sql.append(" OR (id_class_group IS NULL AND id_course IS NULL AND id_subject IS NULL)");
            }
            sql.append(")");
        }
    }

    private static List<String> contextConditions(
            List<Object> parameters,
            Collection<Long> visibleClassGroupIds,
            Collection<Long> visibleCourseIds,
            Collection<Long> visibleSubjectIds
    ) {
        List<String> conditions = new ArrayList<>();
        appendInCondition(conditions, parameters, "id_class_group", visibleClassGroupIds);
        appendInCondition(conditions, parameters, "id_course", visibleCourseIds);
        appendInCondition(conditions, parameters, "id_subject", visibleSubjectIds);
        return conditions;
    }

    private static void appendInCondition(
            List<String> conditions,
            List<Object> parameters,
            String column,
            Collection<Long> values
    ) {
        if (values == null || values.isEmpty()) {
            return;
        }
        conditions.add(column + " IN (" + placeholders(values.size()) + ")");
        parameters.addAll(values);
    }

    private static void hideRebuildableEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                UPDATE learning_event
                SET visibility_state = 'hidden'
                WHERE source_type IN (
                    'lesson', 'assessment', 'attendance', 'absence_justification',
                    'grade_sheet', 'certificate', 'enroll_class_group',
                    'enroll_course', 'class_group', 'grade_record', 'attempt',
                    'teach_class_group', 'coordinate_subject', 'manage_organization',
                    'deletion_request', 'user_account'
                )
                   OR source_type IN ('enrollment')
                   OR event_type IN (
                       'lesson', 'assessment', 'class_group_enrollment',
                       'attendance', 'grade_sheet', 'certificate_issued'
                   )
                """);
    }

    private static void upsertLessonEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'lesson', CONCAT(l.id_lesson, ':', l.state), CONCAT('lesson_', l.state), 'lessons', 'Lesson',
                       CONCAT('Lesson ', CASE l.state
                           WHEN 'scheduled' THEN 'scheduled'
                           WHEN 'active' THEN 'started'
                           WHEN 'completed' THEN 'completed'
                           WHEN 'cancelled' THEN 'cancelled'
                           ELSE l.state
                       END, ': ', l.title),
                       CONCAT(CASE l.type
                           WHEN 'online' THEN 'Online lesson'
                           WHEN 'onsite' THEN 'Presential lesson'
                           WHEN 'hybrid' THEN 'Hybrid lesson'
                           ELSE 'Lesson'
                       END),
                       %s,
                       %s,
                       cg.id_class_group, cg.id_course, cg.id_subject, NULL,
                       CONCAT('/learning/lessons/', l.id_lesson),
                       CASE l.state
                           WHEN 'completed' THEN l.ends_at
                           WHEN 'cancelled' THEN l.ends_at
                           ELSE l.starts_at
                       END,
                       CASE l.state
                           WHEN 'scheduled' THEN 'Scheduled'
                           WHEN 'active' THEN 'Active'
                           WHEN 'completed' THEN 'Completed'
                           WHEN 'cancelled' THEN 'Cancelled'
                           ELSE CONCAT(UCASE(LEFT(l.state, 1)), SUBSTRING(l.state, 2))
                       END,
                       l.state,
                       CASE l.type
                           WHEN 'online' THEN 'ph ph-video-camera'
                           WHEN 'onsite' THEN 'ph ph-map-pin'
                           ELSE 'ph ph-chalkboard-teacher'
                       END,
                       CASE l.type
                           WHEN 'online' THEN 'bg-info-50 text-info-600'
                           WHEN 'onsite' THEN 'bg-success-50 text-success-600'
                           ELSE 'bg-main-50 text-main-600'
                       END,
                       CASE l.state
                           WHEN 'completed' THEN 'bg-success-50 text-success-600'
                           WHEN 'cancelled' THEN 'bg-danger-50 text-danger-600'
                           WHEN 'scheduled' THEN 'bg-warning-50 text-warning-700'
                           ELSE 'bg-main-50 text-main-600'
                       END
                FROM lesson l
                JOIN class_group cg ON cg.id_class_group = l.id_class_group
                JOIN subject s ON s.id_subject = cg.id_subject
                JOIN course c ON c.id_course = cg.id_course
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                JOIN organization o ON o.id_organization = c.id_organization
                WHERE l.state <> 'draft'
                  AND (
                      (l.state IN ('scheduled', 'active') AND l.starts_at IS NOT NULL)
                      OR (l.state IN ('completed', 'cancelled') AND l.ends_at IS NOT NULL)
                  )
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    description = VALUES(description),
                    context_label = VALUES(context_label),
                    context_title = VALUES(context_title),
                    id_class_group = VALUES(id_class_group),
                    id_course = VALUES(id_course),
                    id_subject = VALUES(id_subject),
                    detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at),
                    state_label = VALUES(state_label),
                    state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class),
                    badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class),
                    visibility_state = 'visible'
                """.formatted(
                        classGroupContextSql("cg", "s", "c", "ou", "o"),
                        classGroupContextTitleSql("cg", "s", "c", "ou", "o")
                ));
    }

    private static void upsertAssessmentEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'assessment', CONCAT(a.id_assessment, ':', ctx.id_class_group, ':', a.state),
                       CONCAT('assessment_', a.state),
                       'assessments', 'Assessment',
                       CONCAT('Assessment ', CASE a.state
                           WHEN 'scheduled' THEN 'scheduled'
                           WHEN 'active' THEN 'opened'
                           WHEN 'completed' THEN 'completed'
                           ELSE a.state
                       END, ': ', a.title),
                       CONCAT(CASE a.type
                           WHEN 'test' THEN 'Test'
                           WHEN 'exam' THEN 'Exam'
                           WHEN 'project' THEN 'Project'
                           WHEN 'assignment' THEN 'Assignment'
                           ELSE 'Assessment'
                       END, ' | ', CASE a.mode
                           WHEN 'online' THEN 'Online'
                           WHEN 'onsite' THEN 'Presential'
                           ELSE 'Hybrid'
                       END),
                       %s,
                       %s,
                       cg.id_class_group, cg.id_course, cg.id_subject, NULL,
                       CONCAT('/learning/assessments/', a.id_assessment),
                       CASE a.state
                           WHEN 'completed' THEN a.available_until
                           ELSE a.available_from
                       END,
                       CASE a.state
                           WHEN 'scheduled' THEN 'Scheduled'
                           WHEN 'active' THEN 'Active'
                           WHEN 'completed' THEN 'Completed'
                           ELSE CONCAT(UCASE(LEFT(a.state, 1)), SUBSTRING(a.state, 2))
                       END,
                       a.state,
                       CASE a.type
                           WHEN 'exam' THEN 'ph ph-seal-question'
                           WHEN 'test' THEN 'ph ph-clipboard-text'
                           ELSE 'ph ph-file-text'
                       END,
                       'bg-info-50 text-info-600',
                       CASE a.state
                           WHEN 'completed' THEN 'bg-success-50 text-success-600'
                           WHEN 'scheduled' THEN 'bg-warning-50 text-warning-700'
                           ELSE 'bg-main-50 text-main-600'
                       END
                FROM assessment a
                JOIN (
                    SELECT a2.id_assessment, cb.id_class_group
                    FROM assessment a2
                    JOIN content_block cb ON cb.id_content_block = a2.id_content_block
                    UNION
                    SELECT id_assessment, id_class_group
                    FROM assessment_class_group
                ) ctx ON ctx.id_assessment = a.id_assessment
                JOIN class_group cg ON cg.id_class_group = ctx.id_class_group
                JOIN subject s ON s.id_subject = cg.id_subject
                JOIN course c ON c.id_course = cg.id_course
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                JOIN organization o ON o.id_organization = c.id_organization
                WHERE a.state <> 'draft'
                  AND (
                      (a.state IN ('scheduled', 'active') AND a.available_from IS NOT NULL)
                      OR (a.state = 'completed' AND a.available_until IS NOT NULL)
                  )
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    description = VALUES(description),
                    context_label = VALUES(context_label),
                    context_title = VALUES(context_title),
                    id_class_group = VALUES(id_class_group),
                    id_course = VALUES(id_course),
                    id_subject = VALUES(id_subject),
                    detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at),
                    state_label = VALUES(state_label),
                    state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class),
                    badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class),
                    visibility_state = 'visible'
                """.formatted(
                        classGroupContextSql("cg", "s", "c", "ou", "o"),
                        classGroupContextTitleSql("cg", "s", "c", "ou", "o")
                ));
    }

    private static void upsertClassGroupEnrollmentEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'enroll_class_group', CONCAT(ecg.id_class_group, ':', ecg.id_student_user),
                       'class_group_enrollment_pending',
                       'class_group_enrollments', 'Class group enrollment',
                       CONCAT('Class group enrollment request: ', ecg.id_student_user, ' - ', u.name),
                       'Pending approval',
                       %s,
                       %s,
                       cg.id_class_group, cg.id_course, cg.id_subject, ecg.id_student_user,
                       CONCAT('/learning/class-groups/', cg.id_class_group, '#class-group-enrollments'),
                       /* A pending request is actionable immediately.  Its automatic
                          membership start may be in a future occurrence, so it must
                          never postpone the event itself. */
                       LEAST(CAST(ecg.start_date AS DATETIME), CURRENT_TIMESTAMP),
                       'Pending approval',
                       'pending',
                       'ph ph-user-circle-plus',
                       'bg-danger-50 text-danger-600',
                       'bg-danger-50 text-danger-600'
                FROM enroll_class_group ecg
                JOIN user_account u ON u.id_user = ecg.id_student_user
                JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
                JOIN subject s ON s.id_subject = cg.id_subject
                JOIN course c ON c.id_course = cg.id_course
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                JOIN organization o ON o.id_organization = c.id_organization
                WHERE ecg.state = 'pending'
                  AND ecg.start_date IS NOT NULL
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    description = VALUES(description),
                    context_label = VALUES(context_label),
                    context_title = VALUES(context_title),
                    id_class_group = VALUES(id_class_group),
                    id_course = VALUES(id_course),
                    id_subject = VALUES(id_subject),
                    id_student_user = VALUES(id_student_user),
                    detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at),
                    state_label = VALUES(state_label),
                    state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class),
                    badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class),
                    visibility_state = 'visible'
                """.formatted(
                        classGroupContextSql("cg", "s", "c", "ou", "o"),
                        classGroupContextTitleSql("cg", "s", "c", "ou", "o")
                ));
    }

    /** Pending assessment enrollment requests are actionable events, just like class-group requests. */
    private static void upsertAssessmentEnrollmentEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'assessment', CONCAT('enrollment:', ea.id_assessment, ':', ea.id_student_user),
                       'assessment_enrollment_pending',
                       'assessments', 'Assessment enrollment request',
                       CONCAT('Assessment enrollment request: ', ea.id_student_user, ' - ', u.name),
                       CONCAT('Pending approval | ', a.title),
                       CASE WHEN cg.id_class_group IS NULL THEN %s ELSE %s END,
                       CASE WHEN cg.id_class_group IS NULL THEN %s ELSE %s END,
                       cg.id_class_group, cg.id_course, a.id_subject, ea.id_student_user,
                       CONCAT('/learning/assessments/', a.id_assessment, '#enrollments'),
                       /* Assessment membership follows the same automatic-date rule
                          as class-group membership: show the pending request now. */
                       LEAST(CAST(ea.start_date AS DATETIME), CURRENT_TIMESTAMP),
                       'Pending approval', 'pending',
                       'ph ph-user-circle-plus',
                       'bg-danger-50 text-danger-600',
                       'bg-danger-50 text-danger-600'
                FROM enroll_assessment ea
                JOIN assessment a ON a.id_assessment = ea.id_assessment
                JOIN user_account u ON u.id_user = ea.id_student_user
                LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
                LEFT JOIN (
                    SELECT id_assessment, MIN(id_class_group) AS id_class_group
                    FROM assessment_class_group
                    GROUP BY id_assessment
                ) acg ON acg.id_assessment = a.id_assessment
                LEFT JOIN class_group cg ON cg.id_class_group = COALESCE(cb.id_class_group, acg.id_class_group)
                JOIN subject s ON s.id_subject = a.id_subject
                JOIN organization so ON so.id_organization = s.id_organization
                LEFT JOIN course c ON c.id_course = cg.id_course
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                LEFT JOIN organization o ON o.id_organization = c.id_organization
                WHERE ea.state = 'pending'
                  AND ea.start_date IS NOT NULL
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    description = VALUES(description),
                    context_label = VALUES(context_label),
                    context_title = VALUES(context_title),
                    id_class_group = VALUES(id_class_group),
                    id_course = VALUES(id_course),
                    id_subject = VALUES(id_subject),
                    id_student_user = VALUES(id_student_user),
                    detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at),
                    state_label = VALUES(state_label),
                    state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class),
                    badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class),
                    visibility_state = 'visible'
                """.formatted(
                        subjectContextSql("s", "so"),
                        classGroupContextSql("cg", "s", "c", "ou", "o"),
                        subjectContextTitleSql("s", "so"),
                        classGroupContextTitleSql("cg", "s", "c", "ou", "o")
                ));
    }

    private static void upsertCourseEnrollmentEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'enroll_course', CONCAT(ec.id_course, ':', ec.id_course_occurrence, ':', ec.id_student_user, ':', ec.state),
                       CONCAT('course_enrollment_', ec.state),
                       'course_enrollments', 'Course enrollment',
                       CONCAT('Course enrollment ', CASE ec.state
                           WHEN 'active' THEN 'active'
                           WHEN 'inactive' THEN 'inactive'
                           WHEN 'completed' THEN 'completed'
                           WHEN 'withdrawn' THEN 'withdrawn'
                           ELSE ec.state
                       END, ': ', ec.id_student_user, ' - ', u.name),
                       CONCAT('Course enrollment in ', c.name),
                       %s,
                       %s,
                       NULL, ec.id_course, NULL, ec.id_student_user,
                       CONCAT('/admin/courses/', ec.id_course, '#enrollments'),
                       CASE ec.state
                           WHEN 'completed' THEN CAST(ec.end_date AS DATETIME)
                           WHEN 'withdrawn' THEN CAST(ec.end_date AS DATETIME)
                           WHEN 'inactive' THEN CAST(ec.end_date AS DATETIME)
                           ELSE CAST(ec.start_date AS DATETIME)
                       END,
                       CASE ec.state
                           WHEN 'active' THEN 'Active'
                           WHEN 'inactive' THEN 'Inactive'
                           WHEN 'completed' THEN 'Completed'
                           WHEN 'withdrawn' THEN 'Withdrawn'
                           ELSE CONCAT(UCASE(LEFT(ec.state, 1)), SUBSTRING(ec.state, 2))
                       END,
                       ec.state,
                       'ph ph-graduation-cap',
                       'bg-main-50 text-main-600',
                       CASE ec.state
                           WHEN 'active' THEN 'bg-success-50 text-success-600'
                           WHEN 'completed' THEN 'bg-info-50 text-info-600'
                           WHEN 'inactive' THEN 'bg-danger-50 text-danger-600'
                           ELSE 'bg-neutral-50 text-neutral-600'
                       END
                FROM enroll_course ec
                JOIN user_account u ON u.id_user = ec.id_student_user
                JOIN course c ON c.id_course = ec.id_course
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                JOIN organization o ON o.id_organization = c.id_organization
                WHERE (
                    ec.state IN ('completed', 'withdrawn', 'inactive') AND ec.end_date IS NOT NULL
                ) OR (
                    ec.state NOT IN ('completed', 'withdrawn', 'inactive') AND ec.start_date IS NOT NULL
                )
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    description = VALUES(description),
                    context_label = VALUES(context_label),
                    context_title = VALUES(context_title),
                    id_course = VALUES(id_course),
                    id_student_user = VALUES(id_student_user),
                    detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at),
                    state_label = VALUES(state_label),
                    state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class),
                    badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class),
                    visibility_state = 'visible'
                """.formatted(
                        courseContextSql("c", "ou", "o"),
                        courseContextTitleSql("c", "ou", "o")
                ));
    }

    private static void upsertAttendanceEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'attendance', CONCAT(ar.id_attendance_record, ':', ar.state, ':', ar.status),
                       CONCAT('attendance_', ar.state, '_', ar.status),
                       'attendance', 'Attendance',
                       CONCAT('Attendance ', CASE ar.state
                           WHEN 'active' THEN
                               CASE ar.status
                                   WHEN 'present' THEN 'present'
                                   WHEN 'absent' THEN 'absent'
                                   WHEN 'justified' THEN 'justified'
                                   WHEN 'late' THEN 'late'
                                   WHEN 'partial' THEN 'partial'
                                   ELSE ar.status
                               END
                           WHEN 'corrected' THEN
                               CASE ar.status
                                   WHEN 'justified' THEN 'justified'
                                   WHEN 'present' THEN 'corrected present'
                                   WHEN 'absent' THEN 'corrected absent'
                                   ELSE 'corrected'
                               END
                           WHEN 'cancelled' THEN 'cancelled'
                           ELSE ar.state
                       END, ': ', ar.id_user_student, ' - ', u.name, ' | ', l.title),
                       'Attendance record',
                       %s,
                       %s,
                       cg.id_class_group, cg.id_course, cg.id_subject, ar.id_user_student,
                       CONCAT('/learning/lessons#attendanceDetail', ar.id_attendance_record),
                       COALESCE(ar.check_in, ar.check_out, l.ends_at, l.starts_at),
                       CASE ar.state
                           WHEN 'active' THEN
                               CASE ar.status
                                   WHEN 'present' THEN 'Present'
                                   WHEN 'absent' THEN 'Absent'
                                   WHEN 'justified' THEN 'Justified'
                                   WHEN 'late' THEN 'Late'
                                   WHEN 'partial' THEN 'Partial'
                                   ELSE CONCAT(UCASE(LEFT(ar.status, 1)), SUBSTRING(ar.status, 2))
                               END
                           WHEN 'corrected' THEN
                               CASE ar.status
                                   WHEN 'justified' THEN 'Justified'
                                   WHEN 'present' THEN 'Corrected present'
                                   WHEN 'absent' THEN 'Corrected absent'
                                   ELSE 'Corrected'
                               END
                           WHEN 'cancelled' THEN 'Cancelled'
                           ELSE CONCAT(UCASE(LEFT(ar.state, 1)), SUBSTRING(ar.state, 2))
                       END,
                       CONCAT(ar.state, ':', ar.status),
                       'ph ph-check-square-offset',
                       'bg-main-two-50 text-main-two-600',
                       CASE ar.state
                           WHEN 'active' THEN
                               CASE ar.status
                                   WHEN 'present' THEN 'bg-success-50 text-success-600'
                                   WHEN 'absent' THEN 'bg-danger-50 text-danger-600'
                                   ELSE 'bg-main-50 text-main-600'
                               END
                           WHEN 'cancelled' THEN 'bg-danger-50 text-danger-600'
                           ELSE 'bg-success-50 text-success-600'
                       END
                FROM attendance_record ar
                JOIN lesson l ON l.id_lesson = ar.id_lesson
                JOIN class_group cg ON cg.id_class_group = l.id_class_group
                JOIN subject s ON s.id_subject = cg.id_subject
                JOIN course c ON c.id_course = cg.id_course
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                JOIN organization o ON o.id_organization = c.id_organization
                JOIN user_account u ON u.id_user = ar.id_user_student
                WHERE (l.ends_at IS NULL OR l.ends_at <= CURRENT_TIMESTAMP)
                  AND COALESCE(ar.check_in, ar.check_out, l.ends_at, l.starts_at) IS NOT NULL
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    description = VALUES(description),
                    context_label = VALUES(context_label),
                    context_title = VALUES(context_title),
                    id_class_group = VALUES(id_class_group),
                    id_course = VALUES(id_course),
                    id_subject = VALUES(id_subject),
                    id_student_user = VALUES(id_student_user),
                    detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at),
                    state_label = VALUES(state_label),
                    state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class),
                    badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class),
                    visibility_state = 'visible'
                """.formatted(
                        classGroupContextSql("cg", "s", "c", "ou", "o"),
                        classGroupContextTitleSql("cg", "s", "c", "ou", "o")
                ));
    }

    private static void upsertAbsenceJustificationEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'absence_justification', CONCAT(aj.id_absence_justification, ':', aj.state),
                       CONCAT('absence_justification_', aj.state),
                       'absence_justifications', 'Absence justification',
                       CONCAT('Absence justification ', CASE aj.state
                           WHEN 'submitted' THEN 'submitted'
                           WHEN 'under_review' THEN 'under review'
                           WHEN 'approved' THEN 'approved'
                           WHEN 'rejected' THEN 'rejected'
                           WHEN 'cancelled' THEN 'cancelled'
                           ELSE aj.state
                       END, ': ', aj.id_user_student_submitter, ' - ', u.name, ' | ', l.title),
                       'Absence justification request and decision',
                       %s,
                       %s,
                       cg.id_class_group, cg.id_course, cg.id_subject, aj.id_user_student_submitter,
                       CONCAT('/learning/lessons#attendanceSettings', aj.id_attendance_record),
                       CASE aj.state
                           WHEN 'approved' THEN aj.processed_at
                           WHEN 'rejected' THEN aj.processed_at
                           ELSE aj.submitted_at
                       END,
                       CASE aj.state
                           WHEN 'submitted' THEN 'Submitted'
                           WHEN 'under_review' THEN 'Under review'
                           WHEN 'approved' THEN 'Approved'
                           WHEN 'rejected' THEN 'Rejected'
                           WHEN 'cancelled' THEN 'Cancelled'
                           ELSE CONCAT(UCASE(LEFT(aj.state, 1)), SUBSTRING(aj.state, 2))
                       END,
                       aj.state,
                       'ph ph-note-pencil',
                       'bg-warning-50 text-warning-700',
                       CASE aj.state
                           WHEN 'approved' THEN 'bg-success-50 text-success-600'
                           WHEN 'rejected' THEN 'bg-danger-50 text-danger-600'
                           WHEN 'cancelled' THEN 'bg-neutral-50 text-neutral-600'
                           ELSE 'bg-warning-50 text-warning-700'
                       END
                FROM absence_justification aj
                JOIN attendance_record ar ON ar.id_attendance_record = aj.id_attendance_record
                JOIN lesson l ON l.id_lesson = ar.id_lesson
                JOIN class_group cg ON cg.id_class_group = l.id_class_group
                JOIN subject s ON s.id_subject = cg.id_subject
                JOIN course c ON c.id_course = cg.id_course
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                JOIN organization o ON o.id_organization = c.id_organization
                JOIN user_account u ON u.id_user = aj.id_user_student_submitter
                WHERE (
                    aj.state IN ('approved', 'rejected') AND aj.processed_at IS NOT NULL
                ) OR (
                    aj.state NOT IN ('approved', 'rejected') AND aj.submitted_at IS NOT NULL
                )
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    description = VALUES(description),
                    context_label = VALUES(context_label),
                    context_title = VALUES(context_title),
                    id_class_group = VALUES(id_class_group),
                    id_course = VALUES(id_course),
                    id_subject = VALUES(id_subject),
                    id_student_user = VALUES(id_student_user),
                    detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at),
                    state_label = VALUES(state_label),
                    state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class),
                    badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class),
                    visibility_state = 'visible'
                """.formatted(
                        classGroupContextSql("cg", "s", "c", "ou", "o"),
                        classGroupContextTitleSql("cg", "s", "c", "ou", "o")
                ));
    }

    private static void upsertGradeSheetEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'grade_sheet', CONCAT(gs.id_grade_sheet, ':', cg.id_class_group, ':', gs.state),
                       CONCAT('grade_sheet_', gs.state),
                       'grade_sheets', 'Grade sheet',
                       CONCAT('Grade sheet ', CASE gs.state
                           WHEN 'published' THEN 'published'
                           WHEN 'closed' THEN 'closed'
                           WHEN 'inactive' THEN 'inactive'
                           ELSE gs.state
                       END, ': ', gs.title),
                       CONCAT('Grade sheet ', gs.type),
                       %s,
                       %s,
                       cg.id_class_group, cg.id_course, gs.id_subject, NULL,
                       CONCAT('/learning/attendance#gradeSheetDetail', gs.id_grade_sheet),
                       gs.released_at,
                       CASE gs.state
                           WHEN 'published' THEN 'Published'
                           WHEN 'closed' THEN 'Closed'
                           WHEN 'inactive' THEN 'Inactive'
                           ELSE CONCAT(UCASE(LEFT(gs.state, 1)), SUBSTRING(gs.state, 2))
                       END,
                       gs.state,
                       'ph ph-table',
                       'bg-info-50 text-info-600',
                       CASE gs.state
                           WHEN 'published' THEN 'bg-success-50 text-success-600'
                           WHEN 'closed' THEN 'bg-neutral-50 text-neutral-600'
                           WHEN 'inactive' THEN 'bg-danger-50 text-danger-600'
                           ELSE 'bg-warning-50 text-warning-700'
                       END
                FROM grade_sheet gs
                JOIN associate_grade_sheet_class_group agscg ON agscg.id_grade_sheet = gs.id_grade_sheet
                JOIN class_group cg ON cg.id_class_group = agscg.id_class_group
                JOIN subject s ON s.id_subject = gs.id_subject
                JOIN course c ON c.id_course = cg.id_course
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                JOIN organization o ON o.id_organization = c.id_organization
                WHERE gs.state = 'published'
                  AND gs.released_at IS NOT NULL
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    description = VALUES(description),
                    context_label = VALUES(context_label),
                    context_title = VALUES(context_title),
                    id_class_group = VALUES(id_class_group),
                    id_course = VALUES(id_course),
                    id_subject = VALUES(id_subject),
                    detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at),
                    state_label = VALUES(state_label),
                    state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class),
                    badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class),
                    visibility_state = 'visible'
                """.formatted(
                        classGroupContextSql("cg", "s", "c", "ou", "o"),
                        classGroupContextTitleSql("cg", "s", "c", "ou", "o")
                ));
    }

    private static void upsertCertificateEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'certificate', CONCAT(c.id_certificate, ':', c.state), CONCAT('certificate_', c.state),
                       'certificates', 'Certificate',
                       CONCAT('Certificate ', CASE c.state
                           WHEN 'active' THEN 'activated'
                           WHEN 'issued' THEN 'issued'
                           ELSE c.state
                       END, ': ', c.title),
                       CONCAT(c.id_user_student, ' - ', u.name),
                       %s,
                       %s,
                       NULL, c.id_course, NULL, c.id_user_student,
                       CONCAT('/learning/attendance#certificateDetail', c.id_certificate),
                       c.issued_at,
                       CASE c.state
                           WHEN 'active' THEN 'Active'
                           WHEN 'issued' THEN 'Issued'
                           ELSE CONCAT(UCASE(LEFT(c.state, 1)), SUBSTRING(c.state, 2))
                       END,
                       c.state,
                       'ph ph-certificate',
                       'bg-success-50 text-success-600',
                       CASE c.state
                           WHEN 'issued' THEN 'bg-success-50 text-success-600'
                           ELSE 'bg-warning-50 text-warning-700'
                       END
                FROM certificate c
                JOIN course ON course.id_course = c.id_course
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = course.id_organic_unit
                JOIN organization o ON o.id_organization = course.id_organization
                JOIN user_account u ON u.id_user = c.id_user_student
                WHERE c.state = 'issued'
                  AND c.issued_at IS NOT NULL
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    description = VALUES(description),
                    context_label = VALUES(context_label),
                    context_title = VALUES(context_title),
                    id_course = VALUES(id_course),
                    id_student_user = VALUES(id_student_user),
                    detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at),
                    state_label = VALUES(state_label),
                    state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class),
                    badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class),
                    visibility_state = 'visible'
                """.formatted(
                        courseContextSql("course", "ou", "o"),
                        courseContextTitleSql("course", "ou", "o")
                ));
    }

    /**
     * Student event spans are deliberately derived from the current academic
     * records.  The stable source key makes the read receipt durable: a span
     * disappears after it is viewed, but an event that ceases to apply is also
     * hidden on the next rebuild.
     */
    private static void upsertStudentClassGroupEnrollmentAcceptedEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'enroll_class_group', CONCAT('student-accepted:', ecg.id_student_user, ':', ecg.id_class_group),
                       'student_class_group_enrollment_accepted', 'class_group_enrollments', 'Class group enrollment',
                       CONCAT('Class group enrollment accepted: ', cg.cod_class_group),
                       'Your class group enrollment is active.',
                       %s, %s,
                       cg.id_class_group, cg.id_course, cg.id_subject, ecg.id_student_user,
                       CONCAT('/student/class-groups/', cg.id_class_group, '#student-class-group-overview'),
                       LEAST(CAST(ecg.start_date AS DATETIME), CURRENT_TIMESTAMP),
                       'Enrollment accepted', 'active',
                       'ph ph-user-check', 'bg-success-50 text-success-600', 'bg-success-50 text-success-600'
                FROM enroll_class_group ecg
                JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
                JOIN subject s ON s.id_subject = cg.id_subject
                JOIN course c ON c.id_course = cg.id_course
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                JOIN organization o ON o.id_organization = c.id_organization
                WHERE ecg.state = 'active'
                  AND ecg.start_date IS NOT NULL
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title), description = VALUES(description),
                    context_label = VALUES(context_label), context_title = VALUES(context_title),
                    id_class_group = VALUES(id_class_group), id_course = VALUES(id_course), id_subject = VALUES(id_subject),
                    id_student_user = VALUES(id_student_user), detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at), state_label = VALUES(state_label), state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class), badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class), visibility_state = 'visible'
                """.formatted(
                        classGroupContextSql("cg", "s", "c", "ou", "o"),
                        classGroupContextTitleSql("cg", "s", "c", "ou", "o")
                ));
    }

    private static void upsertStudentRunningLessonEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'lesson', CONCAT('student-running:', ecg.id_student_user, ':', l.id_lesson),
                       'student_lesson_running', 'lessons', 'Lesson',
                       CONCAT('Lesson running: ', l.title),
                       'This lesson is currently in progress.',
                       %s, %s,
                       cg.id_class_group, cg.id_course, cg.id_subject, ecg.id_student_user,
                       CONCAT('/student/class-groups/', cg.id_class_group, '#student-lesson-', l.id_lesson),
                       LEAST(l.starts_at, CURRENT_TIMESTAMP),
                       'Running now', 'active',
                       'ph ph-play-circle', 'bg-main-50 text-main-600', 'bg-success-50 text-success-600'
                FROM lesson l
                JOIN class_group cg ON cg.id_class_group = l.id_class_group
                JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group AND ecg.state = 'active'
                JOIN subject s ON s.id_subject = cg.id_subject
                JOIN course c ON c.id_course = cg.id_course
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                JOIN organization o ON o.id_organization = c.id_organization
                WHERE l.state = 'active'
                  AND l.starts_at <= CURRENT_TIMESTAMP
                  AND (l.ends_at IS NULL OR l.ends_at >= CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title), description = VALUES(description),
                    context_label = VALUES(context_label), context_title = VALUES(context_title),
                    id_class_group = VALUES(id_class_group), id_course = VALUES(id_course), id_subject = VALUES(id_subject),
                    id_student_user = VALUES(id_student_user), detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at), state_label = VALUES(state_label), state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class), badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class), visibility_state = 'visible'
                """.formatted(
                        classGroupContextSql("cg", "s", "c", "ou", "o"),
                        classGroupContextTitleSql("cg", "s", "c", "ou", "o")
                ));
    }

    private static void upsertStudentAssessmentEnrollmentRequiredEvents(Connection connection) throws SQLException {
        upsertStudentAssessmentActionEvents(connection, false);
    }

    private static void upsertStudentAssessmentAttemptRequiredEvents(Connection connection) throws SQLException {
        upsertStudentAssessmentActionEvents(connection, true);
    }

    private static void upsertStudentAssessmentActionEvents(Connection connection, boolean attemptRequired) throws SQLException {
        String requiredState = attemptRequired ? "attempt" : "enrollment";
        String eventType = attemptRequired
                ? "student_assessment_attempt_required"
                : "student_assessment_enrollment_required";
        String titlePrefix = attemptRequired ? "Assessment ready to attempt: " : "Assessment enrollment required: ";
        String description = attemptRequired
                ? "You are enrolled and can make your first attempt."
                : "Enroll in this assessment to make an attempt.";
        String stateLabel = attemptRequired ? "Attempt required" : "Enrollment required";
        String stateValue = attemptRequired ? "attempt_required" : "enrollment_required";
        String icon = attemptRequired ? "ph ph-pencil-line" : "ph ph-user-circle-plus";
        String badge = attemptRequired ? "bg-warning-50 text-warning-700" : "bg-info-50 text-info-600";
        String enrollmentCondition = attemptRequired
                ? "EXISTS (SELECT 1 FROM enroll_assessment ea WHERE ea.id_assessment = a.id_assessment AND ea.id_student_user = ecg.id_student_user AND ea.state = 'active')"
                : "NOT EXISTS (SELECT 1 FROM enroll_assessment ea WHERE ea.id_assessment = a.id_assessment AND ea.id_student_user = ecg.id_student_user AND ea.state IN ('active', 'pending'))";
        String attemptCondition = attemptRequired
                ? "AND NOT EXISTS (SELECT 1 FROM attempt at WHERE at.id_assessment = a.id_assessment AND at.id_student_user = ecg.id_student_user)"
                : "";
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'assessment', CONCAT('student-%s:', ecg.id_student_user, ':', a.id_assessment, ':', cg.id_class_group),
                       '%s', 'assessments', 'Assessment',
                       CONCAT('%s', a.title), '%s',
                       %s, %s,
                       cg.id_class_group, cg.id_course, cg.id_subject, ecg.id_student_user,
                       CONCAT('/student/class-groups/', cg.id_class_group, '#student-assessment-', a.id_assessment),
                       LEAST(a.available_from, CURRENT_TIMESTAMP),
                       '%s', '%s', '%s', '%s', '%s'
                FROM assessment a
                JOIN (
                    SELECT a2.id_assessment, cb.id_class_group
                    FROM assessment a2
                    JOIN content_block cb ON cb.id_content_block = a2.id_content_block
                    UNION
                    SELECT acg.id_assessment, acg.id_class_group
                    FROM assessment_class_group acg
                ) ctx ON ctx.id_assessment = a.id_assessment
                JOIN class_group cg ON cg.id_class_group = ctx.id_class_group
                JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group AND ecg.state = 'active'
                JOIN subject s ON s.id_subject = cg.id_subject
                JOIN course c ON c.id_course = cg.id_course
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                JOIN organization o ON o.id_organization = c.id_organization
                WHERE a.state = 'active'
                  AND a.available_from <= CURRENT_TIMESTAMP
                  AND (a.available_until IS NULL OR a.available_until >= CURRENT_TIMESTAMP)
                  AND %s
                  %s
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title), description = VALUES(description),
                    context_label = VALUES(context_label), context_title = VALUES(context_title),
                    id_class_group = VALUES(id_class_group), id_course = VALUES(id_course), id_subject = VALUES(id_subject),
                    id_student_user = VALUES(id_student_user), detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at), state_label = VALUES(state_label), state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class), badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class), visibility_state = 'visible'
                """.formatted(
                        requiredState, eventType, titlePrefix, description,
                        classGroupContextSql("cg", "s", "c", "ou", "o"),
                        classGroupContextTitleSql("cg", "s", "c", "ou", "o"),
                        stateLabel, stateValue, icon, badge, badge,
                        enrollmentCondition, attemptCondition
                ));
    }

    private static void upsertStudentGradeAvailableEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'grade_sheet', CONCAT('student-grade:', gr.id_grade_record, ':', cg.id_class_group),
                       'student_grade_sheet_grade_available', 'grade_sheets', 'Grade sheet',
                       CONCAT('Your grade is available: ', gs.title),
                       'Your class group grade can now be viewed.',
                       %s, %s,
                       cg.id_class_group, cg.id_course, cg.id_subject, gr.id_user_student,
                       CONCAT('/student/class-groups/', cg.id_class_group, '#student-class-group-grade-sheet'),
                       LEAST(gr.recorded_at, CURRENT_TIMESTAMP),
                       'Grade available', 'grade_available',
                       'ph ph-table', 'bg-success-50 text-success-600', 'bg-success-50 text-success-600'
                FROM grade_record gr
                JOIN grade_sheet gs ON gs.id_grade_sheet = gr.id_grade_sheet
                JOIN associate_grade_sheet_class_group agscg ON agscg.id_grade_sheet = gs.id_grade_sheet
                JOIN class_group cg ON cg.id_class_group = agscg.id_class_group
                JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group
                    AND ecg.id_student_user = gr.id_user_student AND ecg.state = 'active'
                JOIN subject s ON s.id_subject = cg.id_subject
                JOIN course c ON c.id_course = cg.id_course
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                JOIN organization o ON o.id_organization = c.id_organization
                WHERE gr.state IN ('draft', 'published', 'corrected')
                  AND gs.state IN ('draft', 'published')
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title), description = VALUES(description),
                    context_label = VALUES(context_label), context_title = VALUES(context_title),
                    id_class_group = VALUES(id_class_group), id_course = VALUES(id_course), id_subject = VALUES(id_subject),
                    id_student_user = VALUES(id_student_user), detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at), state_label = VALUES(state_label), state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class), badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class), visibility_state = 'visible'
                """.formatted(
                        classGroupContextSql("cg", "s", "c", "ou", "o"),
                        classGroupContextTitleSql("cg", "s", "c", "ou", "o")
                ));
    }

    private static void upsertStudentCertificatePublishedEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'certificate', CONCAT('student-published:', c.id_certificate),
                       'student_certificate_published', 'certificates', 'Certificate',
                       CONCAT('Certificate published: ', c.title),
                       'Your course certificate is available to view.',
                       %s, %s,
                       NULL, c.id_course, NULL, c.id_user_student,
                       CONCAT('/student/attendance#studentCertificateDetail', c.id_certificate),
                       LEAST(c.issued_at, CURRENT_TIMESTAMP),
                       'Published', 'published',
                       'ph ph-certificate', 'bg-success-50 text-success-600', 'bg-success-50 text-success-600'
                FROM certificate c
                JOIN course ON course.id_course = c.id_course
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = course.id_organic_unit
                JOIN organization o ON o.id_organization = course.id_organization
                WHERE c.state = 'issued' AND c.issued_at IS NOT NULL
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title), description = VALUES(description),
                    context_label = VALUES(context_label), context_title = VALUES(context_title),
                    id_course = VALUES(id_course), id_student_user = VALUES(id_student_user), detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at), state_label = VALUES(state_label), state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class), badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class), visibility_state = 'visible'
                """.formatted(
                        courseContextSql("course", "ou", "o"),
                        courseContextTitleSql("course", "ou", "o")
                ));
    }

    private static void upsertClassGroupEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'class_group', CONCAT(cg.id_class_group, ':', cg.state), CONCAT('class_group_', cg.state),
                       'class_groups', 'Class group',
                       CONCAT('Class group ', CASE cg.state
                           WHEN 'scheduled' THEN 'scheduled'
                           WHEN 'active' THEN 'started'
                           WHEN 'completed' THEN 'completed'
                           ELSE cg.state
                       END, ': ', cg.cod_class_group),
                       CONCAT(s.name, ' | ', c.name),
                       %s,
                       %s,
                       cg.id_class_group, cg.id_course, cg.id_subject, NULL,
                       CONCAT('/learning/class-groups/', cg.id_class_group),
                       CASE cg.state
                           WHEN 'completed' THEN CAST(cg.ends_at AS DATETIME)
                           ELSE CAST(cg.starts_at AS DATETIME)
                       END,
                       CASE cg.state
                           WHEN 'scheduled' THEN 'Scheduled'
                           WHEN 'active' THEN 'Active'
                           WHEN 'completed' THEN 'Completed'
                           ELSE CONCAT(UCASE(LEFT(cg.state, 1)), SUBSTRING(cg.state, 2))
                       END,
                       cg.state,
                       'ph ph-users-three',
                       'bg-main-50 text-main-600',
                       CASE cg.state
                           WHEN 'active' THEN 'bg-success-50 text-success-600'
                           WHEN 'completed' THEN 'bg-info-50 text-info-600'
                           WHEN 'scheduled' THEN 'bg-warning-50 text-warning-700'
                           ELSE 'bg-neutral-50 text-neutral-600'
                       END
                FROM class_group cg
                JOIN course c ON c.id_course = cg.id_course
                JOIN subject s ON s.id_subject = cg.id_subject
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                JOIN organization o ON o.id_organization = c.id_organization
                WHERE cg.state <> 'draft'
                  AND (
                      (cg.state IN ('scheduled', 'active') AND cg.starts_at IS NOT NULL)
                      OR (cg.state = 'completed' AND cg.ends_at IS NOT NULL)
                  )
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    description = VALUES(description),
                    context_label = VALUES(context_label),
                    context_title = VALUES(context_title),
                    id_class_group = VALUES(id_class_group),
                    id_course = VALUES(id_course),
                    id_subject = VALUES(id_subject),
                    detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at),
                    state_label = VALUES(state_label),
                    state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class),
                    badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class),
                    visibility_state = 'visible'
                """.formatted(
                        classGroupContextSql("cg", "s", "c", "ou", "o"),
                        classGroupContextTitleSql("cg", "s", "c", "ou", "o")
                ));
    }

    private static void upsertAttemptEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'attempt', CONCAT(at.id_attempt, ':', COALESCE(cg.id_class_group, 0)),
                       'assessment_correction_pending',
                       'attempts', 'Assessment correction',
                       CONCAT('Assessment correction pending: ', at.id_student_user, ' - ', u.name),
                       CONCAT(a.title, ' | attempt ', at.attempt_number),
                       CASE
                           WHEN cg.id_class_group IS NULL THEN %s
                           ELSE %s
                       END,
                       CASE
                           WHEN cg.id_class_group IS NULL THEN %s
                           ELSE %s
                       END,
                       cg.id_class_group, cg.id_course, a.id_subject, at.id_student_user,
                       CONCAT('/learning/assessments/', a.id_assessment),
                       /* A submitted attempt is actionable as soon as it exists;
                          tolerate imported future timestamps without hiding it. */
                       LEAST(at.submitted_at, CURRENT_TIMESTAMP),
                       'Pending correction',
                       'submitted',
                       'ph ph-pencil-line',
                       'bg-danger-50 text-danger-600',
                       'bg-danger-50 text-danger-600'
                FROM attempt at
                JOIN assessment a ON a.id_assessment = at.id_assessment
                JOIN user_account u ON u.id_user = at.id_student_user
                LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
                LEFT JOIN (
                    SELECT id_assessment, MIN(id_class_group) AS id_class_group
                    FROM assessment_class_group
                    GROUP BY id_assessment
                ) acg ON acg.id_assessment = a.id_assessment
                LEFT JOIN class_group cg ON cg.id_class_group = COALESCE(cb.id_class_group, acg.id_class_group)
                JOIN subject s ON s.id_subject = a.id_subject
                JOIN organization so ON so.id_organization = s.id_organization
                LEFT JOIN course c ON c.id_course = cg.id_course
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                LEFT JOIN organization o ON o.id_organization = c.id_organization
                WHERE at.state = 'submitted'
                  AND at.submitted_at IS NOT NULL
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    description = VALUES(description),
                    context_label = VALUES(context_label),
                    context_title = VALUES(context_title),
                    id_class_group = VALUES(id_class_group),
                    id_course = VALUES(id_course),
                    id_subject = VALUES(id_subject),
                    id_student_user = VALUES(id_student_user),
                    detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at),
                    state_label = VALUES(state_label),
                    state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class),
                    badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class),
                    visibility_state = 'visible'
                """.formatted(
                        subjectContextSql("s", "so"),
                        classGroupContextSql("cg", "s", "c", "ou", "o"),
                        subjectContextTitleSql("s", "so"),
                        classGroupContextTitleSql("cg", "s", "c", "ou", "o")
                ));
    }

    private static void upsertTeacherAssignmentEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'teach_class_group', CONCAT(tcg.id_teacher_user, ':', tcg.id_class_group, ':', tcg.state),
                       CONCAT('teacher_assignment_', tcg.state),
                       'teacher_assignments', 'Teacher assignment',
                       CONCAT('Teacher assignment ', CASE tcg.state
                           WHEN 'active' THEN 'active'
                           WHEN 'inactive' THEN 'inactive'
                           ELSE tcg.state
                       END, ': ', tcg.id_teacher_user, ' - ', u.name),
                       CONCAT('Teacher assigned to ', cg.cod_class_group),
                       %s,
                       %s,
                       cg.id_class_group, cg.id_course, cg.id_subject, NULL,
                       CONCAT('/learning/class-groups/', cg.id_class_group, '#class-group-teachers'),
                       CASE tcg.state
                           WHEN 'inactive' THEN CAST(tcg.end_date AS DATETIME)
                           ELSE CAST(tcg.start_date AS DATETIME)
                       END,
                       CASE tcg.state
                           WHEN 'active' THEN 'Active'
                           WHEN 'inactive' THEN 'Inactive'
                           ELSE CONCAT(UCASE(LEFT(tcg.state, 1)), SUBSTRING(tcg.state, 2))
                       END,
                       tcg.state,
                       'ph ph-chalkboard-teacher',
                       'bg-main-50 text-main-600',
                       CASE tcg.state
                           WHEN 'active' THEN 'bg-success-50 text-success-600'
                           WHEN 'inactive' THEN 'bg-danger-50 text-danger-600'
                           ELSE 'bg-neutral-50 text-neutral-600'
                       END
                FROM teach_class_group tcg
                JOIN user_account u ON u.id_user = tcg.id_teacher_user
                JOIN class_group cg ON cg.id_class_group = tcg.id_class_group
                JOIN subject s ON s.id_subject = cg.id_subject
                JOIN course c ON c.id_course = cg.id_course
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                JOIN organization o ON o.id_organization = c.id_organization
                WHERE (
                    tcg.state = 'active' AND tcg.start_date IS NOT NULL
                ) OR (
                    tcg.state = 'inactive' AND tcg.end_date IS NOT NULL
                )
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    description = VALUES(description),
                    context_label = VALUES(context_label),
                    context_title = VALUES(context_title),
                    id_class_group = VALUES(id_class_group),
                    id_course = VALUES(id_course),
                    id_subject = VALUES(id_subject),
                    detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at),
                    state_label = VALUES(state_label),
                    state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class),
                    badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class),
                    visibility_state = 'visible'
                """.formatted(
                        classGroupContextSql("cg", "s", "c", "ou", "o"),
                        classGroupContextTitleSql("cg", "s", "c", "ou", "o")
                ));
    }

    private static void upsertSubjectCoordinationEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'coordinate_subject', CONCAT(cs.id_coordinator_user, ':', cs.id_subject, ':', cs.state),
                       CONCAT('subject_coordination_', cs.state),
                       'subject_coordination', 'Subject coordination',
                       CONCAT('Subject coordination ', CASE cs.state
                           WHEN 'active' THEN 'active'
                           WHEN 'inactive' THEN 'inactive'
                           ELSE cs.state
                       END, ': ', cs.id_coordinator_user, ' - ', u.name),
                       CONCAT('Coordinator assigned to ', s.name),
                       %s,
                       %s,
                       NULL, NULL, cs.id_subject, NULL,
                       CONCAT('/admin/subjects/', cs.id_subject),
                       CURRENT_TIMESTAMP,
                       CASE cs.state
                           WHEN 'active' THEN 'Active'
                           WHEN 'inactive' THEN 'Inactive'
                           ELSE CONCAT(UCASE(LEFT(cs.state, 1)), SUBSTRING(cs.state, 2))
                       END,
                       cs.state,
                       'ph ph-user-switch',
                       'bg-info-50 text-info-600',
                       CASE cs.state
                           WHEN 'active' THEN 'bg-success-50 text-success-600'
                           WHEN 'inactive' THEN 'bg-danger-50 text-danger-600'
                           ELSE 'bg-neutral-50 text-neutral-600'
                       END
                FROM coordinate_subject cs
                JOIN user_account u ON u.id_user = cs.id_coordinator_user
                JOIN subject s ON s.id_subject = cs.id_subject
                JOIN organization o ON o.id_organization = s.id_organization
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    description = VALUES(description),
                    context_label = VALUES(context_label),
                    context_title = VALUES(context_title),
                    id_subject = VALUES(id_subject),
                    detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at),
                    state_label = VALUES(state_label),
                    state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class),
                    badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class),
                    visibility_state = 'visible'
                """.formatted(
                        subjectContextSql("s", "o"),
                        subjectContextTitleSql("s", "o")
                ));
    }

    private static void upsertOrganizationManagementEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'manage_organization', CONCAT(mo.id_admin_user, ':', mo.id_organization, ':', mo.state),
                       CONCAT('organization_management_', mo.state),
                       'organization_management', 'Organization management',
                       CONCAT('Organization management ', CASE mo.state
                           WHEN 'active' THEN 'active'
                           WHEN 'inactive' THEN 'inactive'
                           ELSE mo.state
                       END, ': ', mo.id_admin_user, ' - ', u.name),
                       CONCAT('Administrator assigned to ', o.name),
                       %s,
                       %s,
                       NULL, NULL, NULL, NULL,
                       CONCAT('/admin/organizations/', mo.id_organization),
                       CASE mo.state
                           WHEN 'inactive' THEN CAST(mo.end_date AS DATETIME)
                           ELSE CAST(mo.start_date AS DATETIME)
                       END,
                       CASE mo.state
                           WHEN 'active' THEN 'Active'
                           WHEN 'inactive' THEN 'Inactive'
                           ELSE CONCAT(UCASE(LEFT(mo.state, 1)), SUBSTRING(mo.state, 2))
                       END,
                       mo.state,
                       'ph ph-buildings',
                       'bg-main-two-50 text-main-two-600',
                       CASE mo.state
                           WHEN 'active' THEN 'bg-success-50 text-success-600'
                           WHEN 'inactive' THEN 'bg-danger-50 text-danger-600'
                           ELSE 'bg-neutral-50 text-neutral-600'
                       END
                FROM manage_organization mo
                JOIN user_account u ON u.id_user = mo.id_admin_user
                JOIN organization o ON o.id_organization = mo.id_organization
                WHERE (
                    mo.state = 'active' AND mo.start_date IS NOT NULL
                ) OR (
                    mo.state = 'inactive' AND mo.end_date IS NOT NULL
                )
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    description = VALUES(description),
                    context_label = VALUES(context_label),
                    context_title = VALUES(context_title),
                    detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at),
                    state_label = VALUES(state_label),
                    state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class),
                    badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class),
                    visibility_state = 'visible'
                """.formatted(compactEntitySql("o"), entityTitleSql("o")));
    }

    private static void upsertDeletionRequestEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'deletion_request', CONCAT(dr.id_deletion, ':', dr.state),
                       CONCAT('deletion_request_', dr.state),
                       'deletion_requests', 'Deletion request',
                       CONCAT('Deletion request ', CASE dr.state
                           WHEN 'submitted' THEN 'submitted'
                           WHEN 'under_review' THEN 'under review'
                           WHEN 'approved' THEN 'approved'
                           WHEN 'rejected' THEN 'rejected'
                           WHEN 'completed' THEN 'completed'
                           ELSE dr.state
                       END, ': ', dr.submitter_user_id, ' - ', u.name),
                       COALESCE(dr.reason, 'Account deletion request'),
                       'Account access',
                       'Account access',
                       NULL, NULL, NULL, dr.submitter_user_id,
                       '/admin/deletion-requests',
                       CASE dr.state
                           WHEN 'approved' THEN dr.processed_at
                           WHEN 'rejected' THEN dr.processed_at
                           WHEN 'completed' THEN dr.processed_at
                           ELSE dr.submitted_at
                       END,
                       CASE dr.state
                           WHEN 'submitted' THEN 'Submitted'
                           WHEN 'under_review' THEN 'Under review'
                           WHEN 'approved' THEN 'Approved'
                           WHEN 'rejected' THEN 'Rejected'
                           WHEN 'completed' THEN 'Completed'
                           ELSE CONCAT(UCASE(LEFT(dr.state, 1)), SUBSTRING(dr.state, 2))
                       END,
                       dr.state,
                       'ph ph-user-minus',
                       'bg-warning-50 text-warning-700',
                       CASE dr.state
                           WHEN 'approved' THEN 'bg-success-50 text-success-600'
                           WHEN 'rejected' THEN 'bg-danger-50 text-danger-600'
                           WHEN 'completed' THEN 'bg-info-50 text-info-600'
                           ELSE 'bg-warning-50 text-warning-700'
                       END
                FROM deletion_request dr
                JOIN user_account u ON u.id_user = dr.submitter_user_id
                WHERE (
                    dr.state IN ('approved', 'rejected', 'completed') AND dr.processed_at IS NOT NULL
                ) OR (
                    dr.state NOT IN ('approved', 'rejected', 'completed') AND dr.submitted_at IS NOT NULL
                )
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    description = VALUES(description),
                    context_label = VALUES(context_label),
                    context_title = VALUES(context_title),
                    id_student_user = VALUES(id_student_user),
                    detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at),
                    state_label = VALUES(state_label),
                    state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class),
                    badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class),
                    visibility_state = 'visible'
                """);
    }

    private static void upsertUserAccountEvents(Connection connection) throws SQLException {
        executeUpdate(connection, """
                INSERT INTO learning_event (
                    source_type, source_key, event_type, category, category_label,
                    title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                    detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                )
                SELECT 'user_account', CONCAT(u.id_user, ':', u.state), CONCAT('user_account_', u.state),
                       'users', 'User account',
                       CONCAT('User account ', CASE u.state
                           WHEN 'active' THEN 'active'
                           WHEN 'inactive' THEN 'inactive'
                           WHEN 'blocked' THEN 'blocked'
                           ELSE u.state
                       END, ': ', u.id_user, ' - ', u.name),
                       'User account state',
                       'Account access',
                       'Account access',
                       NULL, NULL, NULL, u.id_user,
                       CONCAT('/admin/users/', u.id_user),
                       u.created_at,
                       CASE u.state
                           WHEN 'active' THEN 'Active'
                           WHEN 'inactive' THEN 'Inactive'
                           WHEN 'blocked' THEN 'Blocked'
                           ELSE CONCAT(UCASE(LEFT(u.state, 1)), SUBSTRING(u.state, 2))
                       END,
                       u.state,
                       'ph ph-user-circle',
                       'bg-neutral-50 text-neutral-600',
                       CASE u.state
                           WHEN 'active' THEN 'bg-success-50 text-success-600'
                           WHEN 'inactive' THEN 'bg-danger-50 text-danger-600'
                           WHEN 'blocked' THEN 'bg-danger-50 text-danger-600'
                           ELSE 'bg-neutral-50 text-neutral-600'
                       END
                FROM user_account u
                WHERE u.state = 'active'
                  AND u.created_at IS NOT NULL
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    description = VALUES(description),
                    context_label = VALUES(context_label),
                    context_title = VALUES(context_title),
                    id_student_user = VALUES(id_student_user),
                    detail_href = VALUES(detail_href),
                    occurred_at = VALUES(occurred_at),
                    state_label = VALUES(state_label),
                    state_value = VALUES(state_value),
                    icon_class = VALUES(icon_class),
                    badge_class = VALUES(badge_class),
                    state_badge_class = VALUES(state_badge_class),
                    visibility_state = 'visible'
                """);
    }

    private static void executeUpdate(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private static String normalizeCategory(String category) {
        if (category == null || category.isBlank() || "all".equalsIgnoreCase(category)) {
            return null;
        }
        return category.trim().toLowerCase(Locale.ROOT);
    }

    private static String placeholders(int size) {
        return String.join(", ", java.util.Collections.nCopies(size, "?"));
    }

    private static void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            Object value = parameters.get(index);
            int position = index + 1;
            if (value == null) {
                statement.setNull(position, Types.NULL);
            } else if (value instanceof Long longValue) {
                statement.setLong(position, longValue);
            } else if (value instanceof Integer intValue) {
                statement.setInt(position, intValue);
            } else if (value instanceof LocalDateTime dateTime) {
                statement.setTimestamp(position, Timestamp.valueOf(dateTime));
            } else {
                statement.setString(position, value.toString());
            }
        }
    }

    private static String selectSql() {
        return """
                SELECT id_learning_event, source_type, source_key, event_type, category, category_label,
                       title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                       detail_href, occurred_at, state_label, state_value, icon_class, badge_class,
                       state_badge_class, FALSE AS read_by_current_user, NULL AS read_at, created_at, updated_at
                FROM learning_event
                """;
    }

    private static String selectSqlWithRead() {
        return """
                SELECT le.id_learning_event, le.source_type, le.source_key, le.event_type, le.category, le.category_label,
                       le.title, le.description, le.context_label, le.context_title, le.id_class_group, le.id_course, le.id_subject,
                       le.id_student_user, le.detail_href, le.occurred_at, le.state_label, le.state_value,
                       le.icon_class, le.badge_class, le.state_badge_class,
                       CASE WHEN ler.id_user IS NULL THEN FALSE ELSE TRUE END AS read_by_current_user,
                       ler.read_at, le.created_at, le.updated_at
                FROM learning_event le
                LEFT JOIN learning_event_read ler
                  ON ler.id_learning_event = le.id_learning_event
                 AND ler.id_user = ?
                """;
    }

    private static LearningEvent mapEvent(ResultSet resultSet) throws SQLException {
        return new LearningEvent(
                resultSet.getLong("id_learning_event"),
                resultSet.getString("source_type"),
                resultSet.getString("source_key"),
                resultSet.getString("event_type"),
                resultSet.getString("category"),
                resultSet.getString("category_label"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("context_label"),
                resultSet.getString("context_title"),
                nullableLong(resultSet, "id_class_group"),
                nullableLong(resultSet, "id_course"),
                nullableLong(resultSet, "id_subject"),
                nullableLong(resultSet, "id_student_user"),
                resultSet.getString("detail_href"),
                resultSet.getObject("occurred_at", LocalDateTime.class),
                resultSet.getString("state_label"),
                resultSet.getString("state_value"),
                resultSet.getString("icon_class"),
                resultSet.getString("badge_class"),
                resultSet.getString("state_badge_class"),
                resultSet.getBoolean("read_by_current_user"),
                nullableDateTime(resultSet, "read_at"),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class)
        );
    }

    private static LocalDateTime nullableDateTime(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getObject(column, LocalDateTime.class);
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private record QueryParts(String sql, List<Object> parameters) {
    }
}
