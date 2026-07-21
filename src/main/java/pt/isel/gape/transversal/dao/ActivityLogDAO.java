package pt.isel.gape.transversal.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.transversal.model.ActivityLog;
import pt.isel.gape.transversal.model.ActivityLogQuery;
import pt.isel.gape.transversal.model.ActivityLogScope;

/** JDBC persistence and contextual snapshots for the immutable activity log. */
public final class ActivityLogDAO {

    private static final Pattern POSITIVE_ID_PATTERN = Pattern.compile("(?<!\\d)([1-9]\\d*)");

    private final ConnectionProvider connectionProvider;

    public ActivityLogDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long insert(
            Long userId,
            Long sessionId,
            String operationType,
            String affectedEntityType,
            String affectedEntityIdentifier,
            LocalDateTime occurredAt,
            String outcome,
            String sourceIp
    ) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                long id = insert(connection, userId, sessionId, operationType, affectedEntityType,
                        affectedEntityIdentifier, occurredAt, outcome, sourceIp);
                connection.commit();
                return id;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    /**
     * Inserts the log and its contextual snapshot in the caller's transaction.
     * The snapshot is deliberately resolved before the activity row is written,
     * allowing callers to record a successful deletion before removing the
     * source entity in that same transaction.
     */
    public long insert(
            Connection connection,
            Long userId,
            Long sessionId,
            String operationType,
            String affectedEntityType,
            String affectedEntityIdentifier,
            LocalDateTime occurredAt,
            String outcome,
            String sourceIp
    ) throws SQLException {
        ActivityLogScope scope = resolveLiveScope(connection, userId, affectedEntityType, affectedEntityIdentifier);
        String sql = """
                INSERT INTO activity_log (
                    id_user, id_session, operation_type, affected_entity_type,
                    affected_entity_identifier, occurred_at, outcome, source_ip
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            validateSessionUser(connection, userId, sessionId);
            setNullableLong(statement, 1, userId);
            setNullableLong(statement, 2, sessionId);
            statement.setString(3, operationType);
            statement.setString(4, affectedEntityType);
            statement.setString(5, affectedEntityIdentifier);
            statement.setObject(6, occurredAt);
            statement.setString(7, outcome);
            statement.setString(8, sourceIp);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating activity log failed, no id generated");
                }
                long activityLogId = generatedKeys.getLong(1);
                insertScope(connection, activityLogId, scope);
                return activityLogId;
            }
        }
    }

    private static void validateSessionUser(Connection connection, Long userId, Long sessionId) throws SQLException {
        if (userId == null || sessionId == null) {
            return;
        }
        String sql = """
                SELECT id_user
                FROM user_session
                WHERE id_session = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || resultSet.getLong("id_user") != userId) {
                    throw new SQLException("Activity_Log Session must belong to the same User", "45000", 1644);
                }
            }
        }
    }

    public Optional<ActivityLog> findById(long activityLogId) throws SQLException {
        String sql = selectColumns() + " WHERE id_activity_log = ?";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, activityLogId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapActivityLog(resultSet)) : Optional.empty();
            }
        }
    }

    public List<ActivityLog> findAll() throws SQLException {
        return find(ActivityLogQuery.all());
    }

    /** Applies exact filters only; authorization remains the service's responsibility. */
    public List<ActivityLog> find(ActivityLogQuery query) throws SQLException {
        ActivityLogQuery effectiveQuery = query == null ? ActivityLogQuery.all() : query;
        StringBuilder sql = new StringBuilder(selectColumns());
        List<Object> parameters = new ArrayList<>();
        appendFilter(sql, parameters, "id_user", effectiveQuery.userId());
        appendFilter(sql, parameters, "operation_type", effectiveQuery.operationType());
        appendFilter(sql, parameters, "affected_entity_type", effectiveQuery.affectedEntityType());
        appendFilter(sql, parameters, "outcome", effectiveQuery.outcome());
        if (effectiveQuery.occurredFrom() != null) {
            appendWhere(sql);
            sql.append(" occurred_at >= ?");
            parameters.add(effectiveQuery.occurredFrom());
        }
        if (effectiveQuery.occurredUntil() != null) {
            appendWhere(sql);
            sql.append(" occurred_at <= ?");
            parameters.add(effectiveQuery.occurredUntil());
        }
        sql.append(" ORDER BY occurred_at DESC, id_activity_log DESC");

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ActivityLog> logs = new ArrayList<>();
                while (resultSet.next()) {
                    logs.add(mapActivityLog(resultSet));
                }
                return List.copyOf(logs);
            }
        }
    }

    public List<ActivityLog> findByUserId(long userId) throws SQLException {
        return find(ActivityLogQuery.forUser(userId));
    }

    /**
     * Compatibility query for a target user's own or directly affected history.
     * New scope-aware callers must use {@code ActivityLogService} so this raw
     * DAO method cannot become an authorization bypass.
     */
    public List<ActivityLog> findByUserInvolvement(long userId) throws SQLException {
        String sql = selectColumns() + """
                 WHERE id_user = ?
                    OR (affected_entity_type IN ('user_account', 'user') AND affected_entity_identifier = ?)
                 ORDER BY occurred_at DESC, id_activity_log DESC
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, Long.toString(userId));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ActivityLog> logs = new ArrayList<>();
                while (resultSet.next()) {
                    logs.add(mapActivityLog(resultSet));
                }
                return List.copyOf(logs);
            }
        }
    }

    /**
     * Returns persisted scope snapshots keyed by activity-log id.  A missing
     * key identifies a legacy row created before scope snapshots existed.
     */
    public Map<Long, ActivityLogScope> findScopesByActivityLogIds(Collection<Long> activityLogIds)
            throws SQLException {
        if (activityLogIds == null || activityLogIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = activityLogIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        String sql = """
                SELECT id_activity_log, scope_type, scope_id
                FROM activity_log_scope
                WHERE id_activity_log IN (%s)
                ORDER BY id_activity_log, scope_type, scope_id
                """.formatted(placeholders(ids.size()));
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < ids.size(); index++) {
                statement.setLong(index + 1, ids.get(index));
            }
            Map<Long, ScopeBuilder> scopes = new LinkedHashMap<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ScopeBuilder scope = scopes.computeIfAbsent(
                            resultSet.getLong("id_activity_log"), ignored -> new ScopeBuilder()
                    );
                    scope.add(resultSet.getString("scope_type"), resultSet.getLong("scope_id"));
                }
            }
            Map<Long, ActivityLogScope> result = new LinkedHashMap<>();
            for (Map.Entry<Long, ScopeBuilder> entry : scopes.entrySet()) {
                result.put(entry.getKey(), entry.getValue().build());
            }
            return Map.copyOf(result);
        }
    }

    /** Resolves a safe, live fallback only for legacy rows without a snapshot. */
    public ActivityLogScope resolveLiveScope(ActivityLog log) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return resolveLiveScope(
                    connection,
                    log.userId(),
                    log.affectedEntityType(),
                    log.affectedEntityIdentifier()
            );
        }
    }

    public Set<Long> findActiveManagedOrganizationIds(long administratorUserId) throws SQLException {
        return selectIds("""
                SELECT mo.id_organization
                FROM manage_organization mo
                JOIN administrator_profile ap ON ap.id_user = mo.id_admin_user
                JOIN user_account user_row ON user_row.id_user = ap.id_user
                JOIN organization organization_row ON organization_row.id_organization = mo.id_organization
                WHERE mo.id_admin_user = ?
                  AND mo.state = 'active'
                  AND user_row.state = 'active'
                  AND organization_row.state = 'active'
                  AND (mo.start_date IS NULL OR mo.start_date <= CURRENT_DATE)
                  AND (mo.end_date IS NULL OR mo.end_date >= CURRENT_DATE)
                ORDER BY mo.id_organization
                """, administratorUserId);
    }

    public Set<Long> findActiveCoordinatedSubjectIds(long coordinatorUserId) throws SQLException {
        return selectIds("""
                SELECT cs.id_subject
                FROM coordinate_subject cs
                JOIN coordinator_profile cp ON cp.id_user = cs.id_coordinator_user
                JOIN user_account user_row ON user_row.id_user = cp.id_user
                JOIN subject subject_row ON subject_row.id_subject = cs.id_subject
                WHERE cs.id_coordinator_user = ?
                  AND cs.state = 'active'
                  AND user_row.state = 'active'
                  AND subject_row.state = 'active'
                ORDER BY cs.id_subject
                """, coordinatorUserId);
    }

    public Set<Long> findActiveTaughtClassGroupIds(long teacherUserId) throws SQLException {
        return selectIds("""
                SELECT tcg.id_class_group
                FROM teach_class_group tcg
                JOIN teacher_profile tp ON tp.id_user = tcg.id_teacher_user
                JOIN user_account user_row ON user_row.id_user = tp.id_user
                JOIN class_group class_group_row ON class_group_row.id_class_group = tcg.id_class_group
                WHERE tcg.id_teacher_user = ?
                  AND tcg.state = 'active'
                  AND user_row.state = 'active'
                  AND class_group_row.state = 'active'
                  AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                  AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                ORDER BY tcg.id_class_group
                """, teacherUserId);
    }

    private ActivityLogScope resolveLiveScope(
            Connection connection,
            Long actorUserId,
            String affectedEntityType,
            String affectedEntityIdentifier
    ) throws SQLException {
        ScopeBuilder scope = new ScopeBuilder();
        if (actorUserId != null && actorUserId > 0) {
            scope.userIds.add(actorUserId);
        }

        String type = affectedEntityType == null ? "" : affectedEntityType.trim().toLowerCase(Locale.ROOT);
        List<Long> ids = positiveIdentifiers(affectedEntityIdentifier);
        Long firstId = ids.isEmpty() ? null : ids.getFirst();

        switch (type) {
            case "organization" -> addOrganization(scope, firstId);
            case "organic_unit" -> addScopeRows(connection, scope, """
                    SELECT id_organization AS organization_id, NULL AS subject_id,
                           NULL AS class_group_id, NULL AS user_id
                    FROM organic_unit WHERE id_organic_unit = ?
                    """, firstId);
            case "course" -> addCourseScope(connection, scope, firstId);
            case "course_occurrence" -> addScopeRows(connection, scope, """
                    SELECT course_row.id_organization AS organization_id, NULL AS subject_id,
                           NULL AS class_group_id, NULL AS user_id
                    FROM course_occurrence occurrence
                    JOIN course course_row ON course_row.id_course = occurrence.id_course
                    WHERE occurrence.id_course_occurrence = ?
                    """, firstId);
            case "course_occurrence_period" -> addScopeRows(connection, scope, """
                    SELECT course_row.id_organization AS organization_id, NULL AS subject_id,
                           NULL AS class_group_id, NULL AS user_id
                    FROM course_occurrence_period period
                    JOIN course_occurrence occurrence ON occurrence.id_course_occurrence = period.id_course_occurrence
                    JOIN course course_row ON course_row.id_course = occurrence.id_course
                    WHERE period.id_course_occurrence_period = ?
                    """, firstId);
            case "subject" -> addSubjectScope(connection, scope, firstId);
            case "class_group" -> addClassGroupScope(connection, scope, firstId);
            case "content_block" -> addClassGroupScopeRows(connection, scope, """
                    FROM content_block block
                    JOIN class_group class_group_row ON class_group_row.id_class_group = block.id_class_group
                    WHERE block.id_content_block = ?
                    """, firstId);
            case "lesson" -> addClassGroupScopeRows(connection, scope, """
                    FROM lesson lesson_row
                    JOIN class_group class_group_row ON class_group_row.id_class_group = lesson_row.id_class_group
                    WHERE lesson_row.id_lesson = ?
                    """, firstId);
            case "assessment" -> addAssessmentScope(connection, scope, firstId);
            case "question" -> addAssessmentScope(connection, scope,
                    selectOptionalId(connection, "SELECT id_assessment FROM question WHERE id_question = ?", firstId));
            case "question_option" -> addAssessmentScope(connection, scope,
                    selectOptionalId(connection, """
                            SELECT question_row.id_assessment
                            FROM question_option option_row
                            JOIN question question_row ON question_row.id_question = option_row.id_question
                            WHERE option_row.id_option = ?
                            """, firstId));
            case "attempt" -> addAttemptScope(connection, scope, firstId);
            case "response" -> addAttemptScope(connection, scope,
                    selectOptionalId(connection, "SELECT id_attempt FROM response WHERE id_response = ?", firstId));
            case "attendance_record" -> addAttendanceScope(connection, scope, firstId);
            case "absence_justification" -> addAbsenceJustificationScope(connection, scope, firstId);
            case "grade_sheet" -> addGradeSheetScope(connection, scope, firstId);
            case "grade_record" -> addGradeRecordScope(connection, scope, firstId);
            case "certificate" -> addCertificateScope(connection, scope, firstId);
            case "content_item" -> addContentItemScope(connection, scope, firstId);
            case "channel" -> addChannelScope(connection, scope, firstId);
            case "message" -> addMessageScope(connection, scope, firstId);
            case "management_view" -> addManagementViewScope(connection, scope, firstId);
            case "schedule_event" -> addScheduleEventScope(connection, scope, firstId);
            case "physical_room" -> addScopeRows(connection, scope, """
                    SELECT id_organization AS organization_id, NULL AS subject_id,
                           NULL AS class_group_id, NULL AS user_id
                    FROM physical_room WHERE cod_physical_room = ?
                    """, affectedEntityIdentifier);
            case "deletion_request" -> addDeletionRequestScope(connection, scope, firstId);
            case "user", "user_account", "administrator", "coordinator", "teacher", "student" ->
                    addUserScope(connection, scope, firstId);
            case "course_subject" -> {
                addCourseScope(connection, scope, idAt(ids, 0));
                addSubjectScope(connection, scope, idAt(ids, 1));
            }
            case "class_group_enrollment" -> {
                scope.addUser(idAt(ids, 0));
                addClassGroupScope(connection, scope, idAt(ids, 1));
            }
            case "assessment_enrollment" -> {
                scope.addUser(idAt(ids, 0));
                addAssessmentScope(connection, scope, idAt(ids, 1));
            }
            case "course_enrollment" -> {
                scope.addUser(idAt(ids, 0));
                addCourseScope(connection, scope, idAt(ids, 1));
            }
            case "class_group_teacher" -> {
                addClassGroupScope(connection, scope, idAt(ids, 0));
                scope.addUser(idAt(ids, 1));
            }
            default -> {
                // Unknown historic types deliberately retain only the actor
                // scope. They are never widened to a guessed organization.
            }
        }
        return scope.build();
    }

    private static void addOrganization(ScopeBuilder scope, Long organizationId) {
        scope.addOrganization(organizationId);
    }

    private static void addCourseScope(Connection connection, ScopeBuilder scope, Long courseId) throws SQLException {
        addScopeRows(connection, scope, """
                SELECT id_organization AS organization_id, NULL AS subject_id,
                       NULL AS class_group_id, NULL AS user_id
                FROM course WHERE id_course = ?
                """, courseId);
    }

    private static void addSubjectScope(Connection connection, ScopeBuilder scope, Long subjectId) throws SQLException {
        addScopeRows(connection, scope, """
                SELECT id_organization AS organization_id, id_subject AS subject_id,
                       NULL AS class_group_id, NULL AS user_id
                FROM subject WHERE id_subject = ?
                """, subjectId);
    }

    private static void addClassGroupScope(Connection connection, ScopeBuilder scope, Long classGroupId)
            throws SQLException {
        addClassGroupScopeRows(connection, scope, """
                FROM class_group class_group_row
                WHERE class_group_row.id_class_group = ?
                """, classGroupId);
    }

    /**
     * The class-group scope query contains the supplied predicate twice: once
     * for the course organization and once for the subject organization.  Keep
     * parameter duplication here instead of asking every caller to remember
     * it; otherwise a single class-group id leaves the second placeholder
     * unbound at runtime.
     */
    private static void addClassGroupScopeRows(
            Connection connection,
            ScopeBuilder scope,
            String fromAndWhere,
            Object... predicateParameters
    ) throws SQLException {
        Object[] duplicatedParameters = new Object[predicateParameters.length * 2];
        System.arraycopy(predicateParameters, 0, duplicatedParameters, 0, predicateParameters.length);
        System.arraycopy(predicateParameters, 0, duplicatedParameters,
                predicateParameters.length, predicateParameters.length);
        addScopeRows(connection, scope, classGroupScopeSql(fromAndWhere), duplicatedParameters);
    }

    private static String classGroupScopeSql(String fromAndWhere) {
        String source = fromAndWhere == null ? "" : fromAndWhere.trim();
        int whereIndex = source.toUpperCase(Locale.ROOT).indexOf("WHERE");
        if (whereIndex < 0) {
            throw new IllegalArgumentException("Class-group scope SQL requires a WHERE predicate");
        }
        String fromAndJoins = source.substring(0, whereIndex);
        String predicate = source.substring(whereIndex);
        return """
                SELECT course_row.id_organization AS organization_id,
                       class_group_row.id_subject AS subject_id,
                       class_group_row.id_class_group AS class_group_id,
                       NULL AS user_id
                %s
                JOIN course course_row ON course_row.id_course = class_group_row.id_course
                %s
                UNION
                SELECT subject_row.id_organization AS organization_id,
                       class_group_row.id_subject AS subject_id,
                       class_group_row.id_class_group AS class_group_id,
                       NULL AS user_id
                %s
                JOIN subject subject_row ON subject_row.id_subject = class_group_row.id_subject
                %s
                """.formatted(fromAndJoins, predicate, fromAndJoins, predicate);
    }

    private static void addAssessmentScope(Connection connection, ScopeBuilder scope, Long assessmentId)
            throws SQLException {
        if (assessmentId == null) {
            return;
        }
        addScopeRows(connection, scope, """
                SELECT subject_row.id_organization AS organization_id,
                       assessment_row.id_subject AS subject_id,
                       NULL AS class_group_id,
                       NULL AS user_id
                FROM assessment assessment_row
                JOIN subject subject_row ON subject_row.id_subject = assessment_row.id_subject
                WHERE assessment_row.id_assessment = ?
                UNION
                SELECT course_row.id_organization AS organization_id,
                       class_group_row.id_subject AS subject_id,
                       class_group_row.id_class_group AS class_group_id,
                       NULL AS user_id
                FROM assessment_class_group association
                JOIN class_group class_group_row ON class_group_row.id_class_group = association.id_class_group
                JOIN course course_row ON course_row.id_course = class_group_row.id_course
                WHERE association.id_assessment = ?
                UNION
                SELECT subject_row.id_organization AS organization_id,
                       class_group_row.id_subject AS subject_id,
                       class_group_row.id_class_group AS class_group_id,
                       NULL AS user_id
                FROM assessment_class_group association
                JOIN class_group class_group_row ON class_group_row.id_class_group = association.id_class_group
                JOIN subject subject_row ON subject_row.id_subject = class_group_row.id_subject
                WHERE association.id_assessment = ?
                UNION
                SELECT course_row.id_organization AS organization_id,
                       class_group_row.id_subject AS subject_id,
                       class_group_row.id_class_group AS class_group_id,
                       NULL AS user_id
                FROM assessment assessment_row
                JOIN content_block block ON block.id_content_block = assessment_row.id_content_block
                JOIN class_group class_group_row ON class_group_row.id_class_group = block.id_class_group
                JOIN course course_row ON course_row.id_course = class_group_row.id_course
                WHERE assessment_row.id_assessment = ?
                UNION
                SELECT subject_row.id_organization AS organization_id,
                       class_group_row.id_subject AS subject_id,
                       class_group_row.id_class_group AS class_group_id,
                       NULL AS user_id
                FROM assessment assessment_row
                JOIN content_block block ON block.id_content_block = assessment_row.id_content_block
                JOIN class_group class_group_row ON class_group_row.id_class_group = block.id_class_group
                JOIN subject subject_row ON subject_row.id_subject = class_group_row.id_subject
                WHERE assessment_row.id_assessment = ?
                """, assessmentId, assessmentId, assessmentId, assessmentId, assessmentId);
    }

    private static void addAttemptScope(Connection connection, ScopeBuilder scope, Long attemptId) throws SQLException {
        if (attemptId == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_assessment, id_student_user
                FROM attempt WHERE id_attempt = ?
                """)) {
            statement.setLong(1, attemptId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    scope.addUser(nullableLong(resultSet, "id_student_user"));
                    addAssessmentScope(connection, scope, nullableLong(resultSet, "id_assessment"));
                }
            }
        }
    }

    private static void addAttendanceScope(Connection connection, ScopeBuilder scope, Long attendanceRecordId)
            throws SQLException {
        if (attendanceRecordId == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT attendance.id_lesson, attendance.id_user_student
                FROM attendance_record attendance
                WHERE attendance.id_attendance_record = ?
                """)) {
            statement.setLong(1, attendanceRecordId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    scope.addUser(nullableLong(resultSet, "id_user_student"));
                    Long lessonId = nullableLong(resultSet, "id_lesson");
                    addClassGroupScopeRows(connection, scope, """
                            FROM lesson lesson_row
                            JOIN class_group class_group_row ON class_group_row.id_class_group = lesson_row.id_class_group
                            WHERE lesson_row.id_lesson = ?
                            """, lessonId);
                }
            }
        }
    }

    private static void addAbsenceJustificationScope(Connection connection, ScopeBuilder scope, Long justificationId)
            throws SQLException {
        if (justificationId == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_attendance_record, id_user_student_submitter, id_user_processor
                FROM absence_justification
                WHERE id_absence_justification = ?
                """)) {
            statement.setLong(1, justificationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    scope.addUser(nullableLong(resultSet, "id_user_student_submitter"));
                    scope.addUser(nullableLong(resultSet, "id_user_processor"));
                    addAttendanceScope(connection, scope, nullableLong(resultSet, "id_attendance_record"));
                }
            }
        }
    }

    private static void addGradeSheetScope(Connection connection, ScopeBuilder scope, Long gradeSheetId)
            throws SQLException {
        if (gradeSheetId == null) {
            return;
        }
        addScopeRows(connection, scope, """
                SELECT subject_row.id_organization AS organization_id,
                       grade_sheet.id_subject AS subject_id,
                       NULL AS class_group_id,
                       NULL AS user_id
                FROM grade_sheet
                JOIN subject subject_row ON subject_row.id_subject = grade_sheet.id_subject
                WHERE grade_sheet.id_grade_sheet = ?
                UNION
                SELECT course_row.id_organization AS organization_id,
                       grade_sheet.id_subject AS subject_id,
                       NULL AS class_group_id,
                       NULL AS user_id
                FROM grade_sheet
                JOIN course_occurrence occurrence ON occurrence.id_course_occurrence = grade_sheet.id_course_occurrence
                JOIN course course_row ON course_row.id_course = occurrence.id_course
                WHERE grade_sheet.id_grade_sheet = ?
                """, gradeSheetId, gradeSheetId);
        for (Long classGroupId : selectIds(connection, """
                SELECT id_class_group
                FROM associate_grade_sheet_class_group
                WHERE id_grade_sheet = ?
                """, gradeSheetId)) {
            addClassGroupScope(connection, scope, classGroupId);
        }
    }

    private static void addGradeRecordScope(Connection connection, ScopeBuilder scope, Long gradeRecordId)
            throws SQLException {
        if (gradeRecordId == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_grade_sheet, id_user_student
                FROM grade_record WHERE id_grade_record = ?
                """)) {
            statement.setLong(1, gradeRecordId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    scope.addUser(nullableLong(resultSet, "id_user_student"));
                    addGradeSheetScope(connection, scope, nullableLong(resultSet, "id_grade_sheet"));
                }
            }
        }
    }

    private static void addCertificateScope(Connection connection, ScopeBuilder scope, Long certificateId)
            throws SQLException {
        if (certificateId == null) {
            return;
        }
        addScopeRows(connection, scope, """
                SELECT course_row.id_organization AS organization_id,
                       NULL AS subject_id, NULL AS class_group_id,
                       certificate.id_user_student AS user_id
                FROM certificate
                JOIN course course_row ON course_row.id_course = certificate.id_course
                WHERE certificate.id_certificate = ?
                """, certificateId);
        for (Long gradeSheetId : selectIds(connection, """
                SELECT id_grade_sheet
                FROM based_on_grade_sheet_certificate
                WHERE id_certificate = ?
                """, certificateId)) {
            addGradeSheetScope(connection, scope, gradeSheetId);
        }
    }

    private static void addContentItemScope(Connection connection, ScopeBuilder scope, Long contentItemId)
            throws SQLException {
        if (contentItemId == null) {
            return;
        }
        addScopeRows(connection, scope, """
                SELECT NULL AS organization_id, NULL AS subject_id,
                       NULL AS class_group_id, author_user_id AS user_id
                FROM content_item WHERE id_content_item = ?
                UNION
                SELECT association.id_organization, NULL, NULL, NULL
                FROM associate_organization_content association WHERE association.id_content_item = ?
                UNION
                SELECT unit.id_organization, NULL, NULL, NULL
                FROM associate_organic_unit_content association
                JOIN organic_unit unit ON unit.id_organic_unit = association.id_organic_unit
                WHERE association.id_content_item = ?
                UNION
                SELECT course_row.id_organization, NULL, NULL, NULL
                FROM associate_course_content association
                JOIN course course_row ON course_row.id_course = association.id_course
                WHERE association.id_content_item = ?
                UNION
                SELECT subject_row.id_organization, subject_row.id_subject, NULL, NULL
                FROM associate_subject_content association
                JOIN subject subject_row ON subject_row.id_subject = association.id_subject
                WHERE association.id_content_item = ?
                UNION
                SELECT course_row.id_organization, class_group_row.id_subject,
                       class_group_row.id_class_group, NULL
                FROM associate_class_group_content association
                JOIN class_group class_group_row ON class_group_row.id_class_group = association.id_class_group
                JOIN course course_row ON course_row.id_course = class_group_row.id_course
                WHERE association.id_content_item = ?
                UNION
                SELECT subject_row.id_organization, class_group_row.id_subject,
                       class_group_row.id_class_group, NULL
                FROM associate_class_group_content association
                JOIN class_group class_group_row ON class_group_row.id_class_group = association.id_class_group
                JOIN subject subject_row ON subject_row.id_subject = class_group_row.id_subject
                WHERE association.id_content_item = ?
                UNION
                SELECT course_row.id_organization, class_group_row.id_subject,
                       class_group_row.id_class_group, NULL
                FROM associate_block_content association
                JOIN content_block block ON block.id_content_block = association.id_content_block
                JOIN class_group class_group_row ON class_group_row.id_class_group = block.id_class_group
                JOIN course course_row ON course_row.id_course = class_group_row.id_course
                WHERE association.id_content_item = ?
                UNION
                SELECT subject_row.id_organization, class_group_row.id_subject,
                       class_group_row.id_class_group, NULL
                FROM associate_block_content association
                JOIN content_block block ON block.id_content_block = association.id_content_block
                JOIN class_group class_group_row ON class_group_row.id_class_group = block.id_class_group
                JOIN subject subject_row ON subject_row.id_subject = class_group_row.id_subject
                WHERE association.id_content_item = ?
                """, contentItemId, contentItemId, contentItemId, contentItemId, contentItemId,
                contentItemId, contentItemId, contentItemId, contentItemId);
        for (Long assessmentId : selectIds(connection, """
                SELECT id_assessment FROM associate_assessment_content WHERE id_content_item = ?
                """, contentItemId)) {
            addAssessmentScope(connection, scope, assessmentId);
        }
    }

    private static void addChannelScope(Connection connection, ScopeBuilder scope, Long channelId) throws SQLException {
        if (channelId == null) {
            return;
        }
        for (Long classGroupId : selectIds(connection, """
                SELECT id_class_group FROM associate_channel_class_group WHERE id_channel = ?
                """, channelId)) {
            addClassGroupScope(connection, scope, classGroupId);
        }
        for (Long contentBlockId : selectIds(connection, """
                SELECT id_content_block FROM associate_channel_content_block WHERE id_channel = ?
                """, channelId)) {
            addClassGroupScopeRows(connection, scope, """
                    FROM content_block block
                    JOIN class_group class_group_row ON class_group_row.id_class_group = block.id_class_group
                    WHERE block.id_content_block = ?
                    """, contentBlockId);
        }
        for (Long assessmentId : selectIds(connection, """
                SELECT id_assessment FROM associate_channel_assessment WHERE id_channel = ?
                """, channelId)) {
            addAssessmentScope(connection, scope, assessmentId);
        }
        for (Long userId : selectIds(connection, """
                SELECT id_user FROM participate_channel WHERE id_channel = ?
                """, channelId)) {
            scope.addUser(userId);
        }
    }

    private static void addMessageScope(Connection connection, ScopeBuilder scope, Long messageId) throws SQLException {
        if (messageId == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_channel, id_user_sender FROM message WHERE id_message = ?
                """)) {
            statement.setLong(1, messageId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    scope.addUser(nullableLong(resultSet, "id_user_sender"));
                    addChannelScope(connection, scope, nullableLong(resultSet, "id_channel"));
                }
            }
        }
        for (Long userId : selectIds(connection, """
                SELECT id_user FROM receive_message WHERE id_message = ?
                """, messageId)) {
            scope.addUser(userId);
        }
    }

    private static void addManagementViewScope(Connection connection, ScopeBuilder scope, Long managementViewId)
            throws SQLException {
        if (managementViewId == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT visibility_scope, scope_target_type, scope_target_id, owner_user_id
                FROM management_view WHERE id_management_view = ?
                """)) {
            statement.setLong(1, managementViewId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return;
                }
                scope.addUser(nullableLong(resultSet, "owner_user_id"));
                Long targetId = nullableLong(resultSet, "scope_target_id");
                String targetType = resultSet.getString("scope_target_type");
                if (targetType == null) {
                    return;
                }
                switch (targetType.trim().toUpperCase(Locale.ROOT)) {
                    case "ORGANIZATION" -> scope.addOrganization(targetId);
                    case "COURSE" -> addCourseScope(connection, scope, targetId);
                    case "SUBJECT" -> addSubjectScope(connection, scope, targetId);
                    case "CLASS_GROUP" -> addClassGroupScope(connection, scope, targetId);
                    case "USER" -> scope.addUser(targetId);
                    default -> {
                        // Unknown legacy scope types are intentionally not inferred.
                    }
                }
            }
        }
    }

    private static void addScheduleEventScope(Connection connection, ScopeBuilder scope, Long scheduleEventId)
            throws SQLException {
        if (scheduleEventId == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_lesson, id_assessment FROM schedule_event WHERE id_schedule_event = ?
                """)) {
            statement.setLong(1, scheduleEventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Long lessonId = nullableLong(resultSet, "id_lesson");
                    if (lessonId != null) {
                        addClassGroupScopeRows(connection, scope, """
                                FROM lesson lesson_row
                                JOIN class_group class_group_row ON class_group_row.id_class_group = lesson_row.id_class_group
                                WHERE lesson_row.id_lesson = ?
                                """, lessonId);
                    }
                    addAssessmentScope(connection, scope, nullableLong(resultSet, "id_assessment"));
                }
            }
        }
    }

    private static void addDeletionRequestScope(Connection connection, ScopeBuilder scope, Long deletionRequestId)
            throws SQLException {
        if (deletionRequestId == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT submitter_user_id, processor_admin_user_id
                FROM deletion_request WHERE id_deletion = ?
                """)) {
            statement.setLong(1, deletionRequestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    // A deletion request is personal data: preserve precisely
                    // its submitter and processor, rather than inferring a
                    // wider organization from a mutable enrollment.
                    scope.addUser(nullableLong(resultSet, "submitter_user_id"));
                    scope.addUser(nullableLong(resultSet, "processor_admin_user_id"));
                }
            }
        }
    }

    private static void addUserScope(Connection connection, ScopeBuilder scope, Long userId) throws SQLException {
        if (userId == null) {
            return;
        }
        scope.addUser(userId);
        addScopeRows(connection, scope, """
                SELECT DISTINCT organization_id, NULL AS subject_id, NULL AS class_group_id, NULL AS user_id
                FROM (
                    SELECT course_row.id_organization AS organization_id
                    FROM enroll_course enrollment
                    JOIN course course_row ON course_row.id_course = enrollment.id_course
                    WHERE enrollment.id_student_user = ?
                    UNION
                    SELECT course_row.id_organization
                    FROM enroll_class_group enrollment
                    JOIN class_group class_group_row ON class_group_row.id_class_group = enrollment.id_class_group
                    JOIN course course_row ON course_row.id_course = class_group_row.id_course
                    WHERE enrollment.id_student_user = ?
                    UNION
                    SELECT subject_row.id_organization
                    FROM coordinate_subject assignment
                    JOIN subject subject_row ON subject_row.id_subject = assignment.id_subject
                    WHERE assignment.id_coordinator_user = ?
                    UNION
                    SELECT course_row.id_organization
                    FROM teach_class_group assignment
                    JOIN class_group class_group_row ON class_group_row.id_class_group = assignment.id_class_group
                    JOIN course course_row ON course_row.id_course = class_group_row.id_course
                    WHERE assignment.id_teacher_user = ?
                    UNION
                    SELECT id_organization FROM manage_organization WHERE id_admin_user = ?
                ) user_organizations
                """, userId, userId, userId, userId, userId);
    }

    private static void addScopeRows(Connection connection, ScopeBuilder scope, String sql, Object... parameters)
            throws SQLException {
        if (parameters == null || parameters.length == 0 || containsNull(parameters)) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, List.of(parameters));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    scope.addOrganization(nullableLong(resultSet, "organization_id"));
                    scope.addSubject(nullableLong(resultSet, "subject_id"));
                    scope.addClassGroup(nullableLong(resultSet, "class_group_id"));
                    scope.addUser(nullableLong(resultSet, "user_id"));
                }
            }
        }
    }

    private static boolean containsNull(Object[] parameters) {
        for (Object parameter : parameters) {
            if (parameter == null) {
                return true;
            }
        }
        return false;
    }

    private static Long selectOptionalId(Connection connection, String sql, Long parameter) throws SQLException {
        if (parameter == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, parameter);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? nullableLong(resultSet, 1) : null;
            }
        }
    }

    private Set<Long> selectIds(String sql, long parameter) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return Set.copyOf(selectIds(connection, sql, parameter));
        }
    }

    private static List<Long> selectIds(Connection connection, String sql, Long parameter) throws SQLException {
        if (parameter == null) {
            return List.of();
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, parameter);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (resultSet.next()) {
                    long id = resultSet.getLong(1);
                    if (!resultSet.wasNull() && id > 0) {
                        ids.add(id);
                    }
                }
                return List.copyOf(ids);
            }
        }
    }

    private static void insertScope(Connection connection, long activityLogId, ActivityLogScope scope) throws SQLException {
        String sql = """
                INSERT IGNORE INTO activity_log_scope (id_activity_log, scope_type, scope_id)
                VALUES (?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            insertScopeRows(statement, activityLogId, "ORGANIZATION", scope.organizationIds());
            insertScopeRows(statement, activityLogId, "SUBJECT", scope.subjectIds());
            insertScopeRows(statement, activityLogId, "CLASS_GROUP", scope.classGroupIds());
            insertScopeRows(statement, activityLogId, "USER", scope.userIds());
        }
    }

    private static void insertScopeRows(
            PreparedStatement statement,
            long activityLogId,
            String scopeType,
            Set<Long> scopeIds
    ) throws SQLException {
        for (Long scopeId : scopeIds) {
            statement.setLong(1, activityLogId);
            statement.setString(2, scopeType);
            statement.setLong(3, scopeId);
            statement.addBatch();
        }
        if (!scopeIds.isEmpty()) {
            statement.executeBatch();
        }
    }

    private static void appendFilter(StringBuilder sql, List<Object> parameters, String column, Object value) {
        if (value == null) {
            return;
        }
        appendWhere(sql);
        sql.append(' ').append(column).append(" = ?");
        parameters.add(value);
    }

    private static void appendWhere(StringBuilder sql) {
        sql.append(sql.indexOf(" WHERE ") >= 0 ? " AND" : " WHERE");
    }

    private static String selectColumns() {
        return """
                SELECT id_activity_log, id_user, id_session, operation_type,
                       affected_entity_type, affected_entity_identifier,
                       occurred_at, outcome, source_ip
                FROM activity_log
                """;
    }

    private static void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            Object parameter = parameters.get(index);
            if (parameter instanceof Long value) {
                statement.setLong(index + 1, value);
            } else if (parameter instanceof Integer value) {
                statement.setInt(index + 1, value);
            } else {
                statement.setObject(index + 1, parameter);
            }
        }
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static List<Long> positiveIdentifiers(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        Matcher matcher = POSITIVE_ID_PATTERN.matcher(value);
        while (matcher.find()) {
            try {
                ids.add(Long.parseLong(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                // An oversized historical identifier is not a valid scope id.
            }
        }
        return List.copyOf(ids);
    }

    private static Long idAt(List<Long> ids, int index) {
        return ids != null && index >= 0 && index < ids.size() ? ids.get(index) : null;
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static Long nullableLong(ResultSet resultSet, int column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }

    private static ActivityLog mapActivityLog(ResultSet resultSet) throws SQLException {
        return new ActivityLog(
                resultSet.getLong("id_activity_log"),
                nullableLong(resultSet, "id_user"),
                nullableLong(resultSet, "id_session"),
                resultSet.getString("operation_type"),
                resultSet.getString("affected_entity_type"),
                resultSet.getString("affected_entity_identifier"),
                resultSet.getObject("occurred_at", LocalDateTime.class),
                resultSet.getString("outcome"),
                resultSet.getString("source_ip")
        );
    }

    private static final class ScopeBuilder {
        private final Set<Long> organizationIds = new LinkedHashSet<>();
        private final Set<Long> subjectIds = new LinkedHashSet<>();
        private final Set<Long> classGroupIds = new LinkedHashSet<>();
        private final Set<Long> userIds = new LinkedHashSet<>();

        private void add(String type, long id) {
            if (id <= 0 || type == null) {
                return;
            }
            switch (type.trim().toUpperCase(Locale.ROOT)) {
                case "ORGANIZATION" -> organizationIds.add(id);
                case "SUBJECT" -> subjectIds.add(id);
                case "CLASS_GROUP" -> classGroupIds.add(id);
                case "USER" -> userIds.add(id);
                default -> {
                    // Unknown snapshot types are not authorization sources.
                }
            }
        }

        private void addOrganization(Long id) {
            if (id != null && id > 0) {
                organizationIds.add(id);
            }
        }

        private void addSubject(Long id) {
            if (id != null && id > 0) {
                subjectIds.add(id);
            }
        }

        private void addClassGroup(Long id) {
            if (id != null && id > 0) {
                classGroupIds.add(id);
            }
        }

        private void addUser(Long id) {
            if (id != null && id > 0) {
                userIds.add(id);
            }
        }

        private ActivityLogScope build() {
            return new ActivityLogScope(organizationIds, subjectIds, classGroupIds, userIds);
        }
    }
}
