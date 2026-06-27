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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.ScheduleEvent;
import pt.isel.gape.learning.model.ScheduleEventCreateCommand;
import pt.isel.gape.learning.model.ScheduleEventState;
import pt.isel.gape.learning.model.ScheduleEventType;

public final class ScheduleEventDAO {

    private final ConnectionProvider connectionProvider;

    public ScheduleEventDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, ScheduleEventCreateCommand command) throws SQLException {
        String sql = """
                INSERT INTO schedule_event (
                    id_lesson, id_assessment, title, description, type, starts_at, ends_at,
                    all_day, reminder_enabled, reminder_minutes_before, state
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setNullableLong(statement, 1, command.lessonId());
            setNullableLong(statement, 2, command.assessmentId());
            statement.setString(3, command.title().trim());
            setNullableString(statement, 4, command.description());
            statement.setString(5, command.type().toDatabaseValue());
            statement.setTimestamp(6, Timestamp.valueOf(command.startsAt()));
            statement.setTimestamp(7, Timestamp.valueOf(command.endsAt()));
            statement.setBoolean(8, command.allDay());
            statement.setBoolean(9, Boolean.TRUE.equals(command.reminderEnabled()));
            setNullableInteger(statement, 10, command.reminderMinutesBefore());
            statement.setString(11, command.state().toDatabaseValue());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating schedule event failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<ScheduleEvent> findById(long eventId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, eventId);
        }
    }

    public Optional<ScheduleEvent> findById(Connection connection, long eventId) throws SQLException {
        String sql = selectEventSql() + " WHERE se.id_schedule_event = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapEvent(connection, resultSet));
            }
        }
    }

    public Optional<ScheduleEvent> lockById(Connection connection, long eventId) throws SQLException {
        String sql = selectEventSql() + " WHERE se.id_schedule_event = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapEvent(connection, resultSet));
            }
        }
    }

    public Optional<ScheduleEvent> findByLesson(Connection connection, long lessonId) throws SQLException {
        String sql = selectEventSql() + " WHERE se.id_lesson = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, lessonId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapEvent(connection, resultSet));
            }
        }
    }

    public Optional<ScheduleEvent> findByAssessment(Connection connection, long assessmentId) throws SQLException {
        String sql = selectEventSql() + " WHERE se.id_assessment = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapEvent(connection, resultSet));
            }
        }
    }

    public void replaceClassGroups(Connection connection, long eventId, Collection<Long> classGroupIds)
            throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("""
                DELETE FROM associate_schedule_event_class_group
                WHERE id_schedule_event = ?
                """)) {
            delete.setLong(1, eventId);
            delete.executeUpdate();
        }
        List<Long> normalized = orderedUnique(classGroupIds);
        if (normalized.isEmpty()) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO associate_schedule_event_class_group (id_schedule_event, id_class_group)
                VALUES (?, ?)
                """)) {
            for (Long classGroupId : normalized) {
                insert.setLong(1, eventId);
                insert.setLong(2, classGroupId);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    public void replaceRecipients(Connection connection, long eventId, Collection<Long> userIds) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("""
                DELETE FROM receive_schedule_event
                WHERE id_schedule_event = ?
                """)) {
            delete.setLong(1, eventId);
            delete.executeUpdate();
        }
        List<Long> normalized = orderedUnique(userIds);
        if (normalized.isEmpty()) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO receive_schedule_event (id_user, id_schedule_event)
                VALUES (?, ?)
                """)) {
            for (Long userId : normalized) {
                insert.setLong(1, userId);
                insert.setLong(2, eventId);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    public List<Long> findClassGroupIds(Connection connection, long eventId) throws SQLException {
        String sql = """
                SELECT id_class_group
                FROM associate_schedule_event_class_group
                WHERE id_schedule_event = ?
                ORDER BY id_class_group
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (resultSet.next()) {
                    ids.add(resultSet.getLong("id_class_group"));
                }
                return ids;
            }
        }
    }

    public List<Long> findRecipientIdsForClassGroups(Connection connection, Collection<Long> classGroupIds)
            throws SQLException {
        List<Long> normalized = orderedUnique(classGroupIds);
        if (normalized.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(normalized.size(), "?"));
        String sql = """
                SELECT DISTINCT id_user
                FROM (
                    SELECT ecg.id_student_user AS id_user
                    FROM enroll_class_group ecg
                    JOIN user_account u ON u.id_user = ecg.id_student_user
                    WHERE ecg.id_class_group IN (%s)
                      AND ecg.state = 'active'
                      AND u.state = 'active'
                      AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                      AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                    UNION
                    SELECT tcg.id_teacher_user AS id_user
                    FROM teach_class_group tcg
                    JOIN user_account u ON u.id_user = tcg.id_teacher_user
                    WHERE tcg.id_class_group IN (%s)
                      AND tcg.state = 'active'
                      AND u.state = 'active'
                      AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                      AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                    UNION
                    SELECT cs.id_coordinator_user AS id_user
                    FROM class_group cg
                    JOIN coordinate_subject cs ON cs.id_subject = cg.id_subject
                    JOIN user_account u ON u.id_user = cs.id_coordinator_user
                    WHERE cg.id_class_group IN (%s)
                      AND cs.state = 'active'
                      AND u.state = 'active'
                      AND (cs.start_date IS NULL OR cs.start_date <= CURRENT_DATE)
                      AND (cs.end_date IS NULL OR cs.end_date >= CURRENT_DATE)
                ) recipients
                ORDER BY id_user
                """.formatted(placeholders, placeholders, placeholders);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (int round = 0; round < 3; round++) {
                for (Long classGroupId : normalized) {
                    statement.setLong(index++, classGroupId);
                }
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (resultSet.next()) {
                    ids.add(resultSet.getLong("id_user"));
                }
                return ids;
            }
        }
    }

    public List<ScheduleEvent> findVisibleForStudent(Connection connection, long studentUserId) throws SQLException {
        String sql = selectEventSql() + """
                WHERE se.state IN ('active', 'completed')
                  AND EXISTS (
                      SELECT 1
                      FROM associate_schedule_event_class_group secg
                      JOIN enroll_class_group ecg ON ecg.id_class_group = secg.id_class_group
                      JOIN user_account u ON u.id_user = ecg.id_student_user
                      JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
                      JOIN course c ON c.id_course = cg.id_course
                      JOIN subject s ON s.id_subject = cg.id_subject
                      WHERE secg.id_schedule_event = se.id_schedule_event
                        AND ecg.id_student_user = ?
                        AND ecg.state = 'active'
                        AND u.state = 'active'
                        AND cg.state = 'active'
                        AND c.state = 'active'
                        AND s.state = 'active'
                        AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                        AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                  )
                ORDER BY se.starts_at, se.id_schedule_event
                """;
        return findVisible(connection, sql, studentUserId);
    }

    public List<ScheduleEvent> findVisibleForTeacher(Connection connection, long teacherUserId) throws SQLException {
        String sql = selectEventSql() + """
                WHERE se.state IN ('active', 'completed')
                  AND EXISTS (
                      SELECT 1
                      FROM associate_schedule_event_class_group secg
                      JOIN teach_class_group tcg ON tcg.id_class_group = secg.id_class_group
                      JOIN grant_teacher gt ON gt.id_teacher_user = tcg.id_teacher_user
                      JOIN permission p ON p.cod_permission = gt.cod_permission
                      JOIN user_account u ON u.id_user = tcg.id_teacher_user
                      JOIN class_group cg ON cg.id_class_group = secg.id_class_group
                      WHERE secg.id_schedule_event = se.id_schedule_event
                        AND tcg.id_teacher_user = ?
                        AND tcg.state = 'active'
                        AND gt.cod_permission = 'MANAGE_LEARNING'
                        AND p.state = 'active'
                        AND u.state = 'active'
                        AND cg.state = 'active'
                        AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                        AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                  )
                ORDER BY se.starts_at, se.id_schedule_event
                """;
        return findVisible(connection, sql, teacherUserId);
    }

    public List<ScheduleEvent> findVisibleForCoordinator(Connection connection, long coordinatorUserId)
            throws SQLException {
        String sql = selectEventSql() + """
                WHERE se.state IN ('active', 'completed')
                  AND EXISTS (
                      SELECT 1
                      FROM associate_schedule_event_class_group secg
                      JOIN class_group cg ON cg.id_class_group = secg.id_class_group
                      JOIN coordinate_subject cs ON cs.id_subject = cg.id_subject
                      JOIN grant_coordinator gc ON gc.id_coordinator_user = cs.id_coordinator_user
                      JOIN permission p ON p.cod_permission = gc.cod_permission
                      JOIN user_account u ON u.id_user = cs.id_coordinator_user
                      WHERE secg.id_schedule_event = se.id_schedule_event
                        AND cs.id_coordinator_user = ?
                        AND cs.state = 'active'
                        AND gc.cod_permission = 'MANAGE_LEARNING'
                        AND p.state = 'active'
                        AND u.state = 'active'
                        AND cg.state = 'active'
                        AND (cs.start_date IS NULL OR cs.start_date <= CURRENT_DATE)
                        AND (cs.end_date IS NULL OR cs.end_date >= CURRENT_DATE)
                  )
                ORDER BY se.starts_at, se.id_schedule_event
                """;
        return findVisible(connection, sql, coordinatorUserId);
    }

    public List<ScheduleEvent> findVisibleForAdministrator(Connection connection, long administratorUserId)
            throws SQLException {
        String sql = selectEventSql() + """
                WHERE se.state IN ('active', 'completed')
                  AND EXISTS (
                      SELECT 1
                      FROM associate_schedule_event_class_group secg
                      JOIN class_group cg ON cg.id_class_group = secg.id_class_group
                      JOIN course c ON c.id_course = cg.id_course
                      JOIN subject s ON s.id_subject = cg.id_subject
                      LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                      JOIN grant_administrator ga ON ga.id_admin_user = ?
                      JOIN permission p ON p.cod_permission = ga.cod_permission
                      JOIN user_account u ON u.id_user = ga.id_admin_user
                      WHERE secg.id_schedule_event = se.id_schedule_event
                        AND p.state = 'active'
                        AND u.state = 'active'
                        AND (
                            (ga.cod_permission = 'MANAGE_ALL' AND ga.context_type = 'GLOBAL')
                            OR (
                                ga.cod_permission = 'MANAGE_LEARNING'
                                AND (
                                    (ga.context_type = 'CLASS_GROUP' AND ga.context_id = secg.id_class_group)
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
                ORDER BY se.starts_at, se.id_schedule_event
                """;
        return findVisible(connection, sql, administratorUserId);
    }

    public boolean classGroupExists(Connection connection, long classGroupId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM class_group
                WHERE id_class_group = ?
                  AND state <> 'draft'
                """)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    private List<ScheduleEvent> findVisible(Connection connection, String sql, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapEvents(connection, resultSet);
            }
        }
    }

    private static String selectEventSql() {
        return """
                SELECT se.id_schedule_event, se.id_lesson, se.id_assessment, se.title, se.description,
                       se.type, se.starts_at, se.ends_at, se.all_day, se.reminder_enabled,
                       se.reminder_minutes_before, se.state
                FROM schedule_event se
                """;
    }

    private List<ScheduleEvent> mapEvents(Connection connection, ResultSet resultSet) throws SQLException {
        List<ScheduleEvent> events = new ArrayList<>();
        while (resultSet.next()) {
            events.add(mapEvent(connection, resultSet));
        }
        return events;
    }

    private ScheduleEvent mapEvent(Connection connection, ResultSet resultSet) throws SQLException {
        long eventId = resultSet.getLong("id_schedule_event");
        return new ScheduleEvent(
                eventId,
                nullableLong(resultSet, "id_lesson"),
                nullableLong(resultSet, "id_assessment"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                ScheduleEventType.fromDatabaseValue(resultSet.getString("type")),
                resultSet.getTimestamp("starts_at").toLocalDateTime(),
                resultSet.getTimestamp("ends_at").toLocalDateTime(),
                resultSet.getBoolean("all_day"),
                resultSet.getBoolean("reminder_enabled"),
                nullableInteger(resultSet, "reminder_minutes_before"),
                ScheduleEventState.fromDatabaseValue(resultSet.getString("state")),
                findClassGroupIds(connection, eventId)
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

    private static List<Long> orderedUnique(Collection<Long> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long value : values) {
            if (value == null || value <= 0) {
                throw new IllegalArgumentException("Ids must be positive");
            }
            unique.add(value);
        }
        return List.copyOf(unique);
    }
}
