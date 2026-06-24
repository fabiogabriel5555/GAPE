package pt.isel.gape.learning.dao;

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
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.learning.model.LessonCreateCommand;
import pt.isel.gape.learning.model.LessonState;
import pt.isel.gape.learning.model.LessonType;
import pt.isel.gape.learning.model.LessonUpdateCommand;
import pt.isel.gape.security.authorization.AuthorizationPolicy;

public final class LessonDAO {

    private final ConnectionProvider connectionProvider;

    public LessonDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, LessonCreateCommand command) throws SQLException {
        String sql = """
                INSERT INTO lesson (
                    id_class_group, id_content_block, cod_physical_room, title, description,
                    type, provider, access_url, attendance_required, state, starts_at, ends_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setStatementValues(statement, command);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating lesson failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<Lesson> findById(long lessonId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, lessonId);
        }
    }

    public Optional<Lesson> findById(Connection connection, long lessonId) throws SQLException {
        String sql = selectLessonSql() + " WHERE id_lesson = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, lessonId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapLesson(resultSet));
            }
        }
    }

    public Optional<Lesson> lockById(Connection connection, long lessonId) throws SQLException {
        String sql = selectLessonSql() + " WHERE id_lesson = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, lessonId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapLesson(resultSet));
            }
        }
    }

    public List<Lesson> findByClassGroup(long classGroupId) throws SQLException {
        String sql = selectLessonSql() + " WHERE id_class_group = ? ORDER BY starts_at, id_lesson";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapLessons(resultSet);
            }
        }
    }

    public List<Lesson> findByContentBlock(long contentBlockId) throws SQLException {
        String sql = selectLessonSql() + " WHERE id_content_block = ? ORDER BY starts_at, id_lesson";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contentBlockId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapLessons(resultSet);
            }
        }
    }

    public List<Lesson> findByRoom(String physicalRoomCode) throws SQLException {
        String sql = selectLessonSql() + " WHERE cod_physical_room = ? ORDER BY starts_at, id_lesson";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, physicalRoomCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapLessons(resultSet);
            }
        }
    }

    public List<Lesson> findForStudent(long studentUserId) throws SQLException {
        return findPersonalCalendarForStudent(studentUserId);
    }

    public List<Lesson> findPersonalCalendarForStudent(long studentUserId) throws SQLException {
        String sql = selectLessonSql() + """
                WHERE id_class_group IN (
                    SELECT ecg.id_class_group
                    FROM enroll_class_group ecg
                    JOIN student_profile sp ON sp.id_user = ecg.id_student_user
                    JOIN user_account u ON u.id_user = sp.id_user
                    JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
                    JOIN course c ON c.id_course = cg.id_course
                    JOIN subject s ON s.id_subject = cg.id_subject
                    WHERE ecg.id_student_user = ?
                      AND ecg.state = 'active'
                      AND cg.state = 'active'
                      AND c.state = 'active'
                      AND s.state <> 'archived'
                      AND u.state = 'active'
                      AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                      AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                )
                ORDER BY starts_at, id_lesson
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapLessons(resultSet);
            }
        }
    }

    public List<Lesson> findPersonalCalendarForTeacher(long teacherUserId) throws SQLException {
        String sql = selectLessonSql() + """
                WHERE id_class_group IN (
                    SELECT tcg.id_class_group
                    FROM teach_class_group tcg
                    JOIN teacher_profile tp ON tp.id_user = tcg.id_teacher_user
                    JOIN user_account u ON u.id_user = tp.id_user
                    JOIN grant_teacher gt ON gt.id_teacher_user = tcg.id_teacher_user
                    JOIN permission p ON p.cod_permission = gt.cod_permission
                    JOIN class_group cg ON cg.id_class_group = tcg.id_class_group
                    JOIN course c ON c.id_course = cg.id_course
                    JOIN subject s ON s.id_subject = cg.id_subject
                    WHERE tcg.id_teacher_user = ?
                      AND tcg.state = 'active'
                      AND cg.state = 'active'
                      AND c.state = 'active'
                      AND s.state <> 'archived'
                      AND u.state = 'active'
                      AND gt.cod_permission = ?
                      AND p.state = 'active'
                      AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                      AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                )
                ORDER BY starts_at, id_lesson
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            statement.setString(2, AuthorizationPolicy.MANAGE_LEARNING);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapLessons(resultSet);
            }
        }
    }

    public List<Lesson> findPersonalCalendarForCoordinator(long coordinatorUserId) throws SQLException {
        String sql = selectLessonSql() + """
                WHERE id_class_group IN (
                    SELECT cg.id_class_group
                    FROM coordinate_subject cs
                    JOIN coordinator_profile cp ON cp.id_user = cs.id_coordinator_user
                    JOIN user_account u ON u.id_user = cp.id_user
                    JOIN grant_coordinator gc ON gc.id_coordinator_user = cs.id_coordinator_user
                    JOIN permission p ON p.cod_permission = gc.cod_permission
                    JOIN subject s ON s.id_subject = cs.id_subject
                    JOIN class_group cg ON cg.id_subject = s.id_subject
                    JOIN course c ON c.id_course = cg.id_course
                    WHERE cs.id_coordinator_user = ?
                      AND cs.state = 'active'
                      AND cg.state = 'active'
                      AND c.state = 'active'
                      AND s.state <> 'archived'
                      AND u.state = 'active'
                      AND gc.cod_permission = ?
                      AND p.state = 'active'
                      AND (cs.start_date IS NULL OR cs.start_date <= CURRENT_DATE)
                      AND (cs.end_date IS NULL OR cs.end_date >= CURRENT_DATE)
                )
                ORDER BY starts_at, id_lesson
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, coordinatorUserId);
            statement.setString(2, AuthorizationPolicy.MANAGE_LEARNING);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapLessons(resultSet);
            }
        }
    }

    public List<Lesson> findPersonalCalendarForStaff(long userId) throws SQLException {
        String sql = selectLessonSql() + """
                WHERE id_class_group IN (
                    SELECT tcg.id_class_group
                    FROM teach_class_group tcg
                    JOIN teacher_profile tp ON tp.id_user = tcg.id_teacher_user
                    JOIN user_account u ON u.id_user = tp.id_user
                    JOIN grant_teacher gt ON gt.id_teacher_user = tcg.id_teacher_user
                    JOIN permission p ON p.cod_permission = gt.cod_permission
                    JOIN class_group cg ON cg.id_class_group = tcg.id_class_group
                    JOIN course c ON c.id_course = cg.id_course
                    JOIN subject s ON s.id_subject = cg.id_subject
                    WHERE tcg.id_teacher_user = ?
                      AND tcg.state = 'active'
                      AND cg.state = 'active'
                      AND c.state = 'active'
                      AND s.state <> 'archived'
                      AND u.state = 'active'
                      AND gt.cod_permission = ?
                      AND p.state = 'active'
                      AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                      AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                )
                   OR id_class_group IN (
                    SELECT cg.id_class_group
                    FROM coordinate_subject cs
                    JOIN coordinator_profile cp ON cp.id_user = cs.id_coordinator_user
                    JOIN user_account u ON u.id_user = cp.id_user
                    JOIN grant_coordinator gc ON gc.id_coordinator_user = cs.id_coordinator_user
                    JOIN permission p ON p.cod_permission = gc.cod_permission
                    JOIN subject s ON s.id_subject = cs.id_subject
                    JOIN class_group cg ON cg.id_subject = s.id_subject
                    JOIN course c ON c.id_course = cg.id_course
                    WHERE cs.id_coordinator_user = ?
                      AND cs.state = 'active'
                      AND cg.state = 'active'
                      AND c.state = 'active'
                      AND s.state <> 'archived'
                      AND u.state = 'active'
                      AND gc.cod_permission = ?
                      AND p.state = 'active'
                      AND (cs.start_date IS NULL OR cs.start_date <= CURRENT_DATE)
                      AND (cs.end_date IS NULL OR cs.end_date >= CURRENT_DATE)
                )
                ORDER BY starts_at, id_lesson
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, AuthorizationPolicy.MANAGE_LEARNING);
            statement.setLong(3, userId);
            statement.setString(4, AuthorizationPolicy.MANAGE_LEARNING);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapLessons(resultSet);
            }
        }
    }

    public void update(Connection connection, long lessonId, LessonUpdateCommand command)
            throws SQLException {
        String sql = """
                UPDATE lesson
                SET id_class_group = ?, id_content_block = ?, cod_physical_room = ?,
                    title = ?, description = ?, type = ?, provider = ?, access_url = ?,
                    attendance_required = ?, state = ?, starts_at = ?, ends_at = ?
                WHERE id_lesson = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setStatementValues(statement, command);
            statement.setLong(13, lessonId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Lesson not found: " + lessonId);
            }
        }
    }

    public void updateState(Connection connection, long lessonId, LessonState state) throws SQLException {
        String sql = "UPDATE lesson SET state = ? WHERE id_lesson = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            statement.setLong(2, lessonId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Lesson not found: " + lessonId);
            }
        }
    }

    public int synchronizeTemporalStates(Connection connection, LocalDateTime now) throws SQLException {
        int completed = updateTemporalState(
                connection,
                """
                UPDATE lesson
                SET state = 'completed'
                WHERE state IN ('scheduled', 'active')
                  AND ends_at <= ?
                """,
                now
        );
        int active = updateTemporalState(
                connection,
                """
                UPDATE lesson
                SET state = 'active'
                WHERE state = 'scheduled'
                  AND starts_at <= ?
                  AND ends_at > ?
                """,
                now,
                now
        );
        return completed + active;
    }

    public boolean roomHasOverlappingReservedLesson(
            Connection connection,
            String physicalRoomCode,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            Long excludedLessonId
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM lesson
                WHERE cod_physical_room = ?
                  AND state IN ('scheduled', 'active')
                  AND type IN ('onsite', 'hybrid')
                  AND (? IS NULL OR id_lesson <> ?)
                  AND NOT (? <= starts_at OR ? >= ends_at)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, physicalRoomCode);
            if (excludedLessonId == null) {
                statement.setNull(2, java.sql.Types.BIGINT);
                statement.setNull(3, java.sql.Types.BIGINT);
            } else {
                statement.setLong(2, excludedLessonId);
                statement.setLong(3, excludedLessonId);
            }
            statement.setTimestamp(4, Timestamp.valueOf(endsAt));
            statement.setTimestamp(5, Timestamp.valueOf(startsAt));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public Optional<Long> findClassGroupOrganizationId(Connection connection, long classGroupId)
            throws SQLException {
        String sql = """
                SELECT c.id_organization
                FROM class_group cg
                JOIN course c ON c.id_course = cg.id_course
                WHERE cg.id_class_group = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(resultSet.getLong("id_organization"));
            }
        }
    }

    public boolean hasActiveStudentClassGroupEnrollment(
            Connection connection,
            long studentUserId,
            long classGroupId
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM enroll_class_group
                WHERE id_student_user = ?
                  AND id_class_group = ?
                  AND state = 'active'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean hasCurrentStudentClassGroupAccess(
            Connection connection,
            long studentUserId,
            long classGroupId
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM enroll_class_group ecg
                JOIN student_profile sp ON sp.id_user = ecg.id_student_user
                JOIN user_account u ON u.id_user = sp.id_user
                JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
                JOIN course c ON c.id_course = cg.id_course
                JOIN subject s ON s.id_subject = cg.id_subject
                WHERE ecg.id_student_user = ?
                  AND ecg.id_class_group = ?
                  AND ecg.state = 'active'
                  AND cg.state = 'active'
                  AND c.state = 'active'
                  AND s.state <> 'archived'
                  AND u.state = 'active'
                  AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                  AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    public boolean hasDomainDependencies(Connection connection, long lessonId) throws SQLException {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM schedule_event WHERE id_lesson = ?)
                  + (SELECT COUNT(*) FROM attendance_record WHERE id_lesson = ?)
                  AS dependency_count
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, lessonId);
            statement.setLong(2, lessonId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("dependency_count") > 0;
            }
        }
    }

    public void delete(Connection connection, long lessonId) throws SQLException {
        String sql = "DELETE FROM lesson WHERE id_lesson = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, lessonId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Lesson not found: " + lessonId);
            }
        }
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

    private static String selectLessonSql() {
        return """
                SELECT id_lesson, id_class_group, id_content_block, cod_physical_room,
                       title, description, type, provider, access_url, attendance_required,
                       state, starts_at, ends_at
                FROM lesson
                """;
    }

    private static void setStatementValues(PreparedStatement statement, LessonCreateCommand command)
            throws SQLException {
        statement.setLong(1, command.classGroupId());
        statement.setLong(2, command.contentBlockId());
        setNullableString(statement, 3, command.physicalRoomCode());
        statement.setString(4, command.title().trim());
        setNullableString(statement, 5, command.description());
        statement.setString(6, command.type().toDatabaseValue());
        setNullableString(statement, 7, command.provider());
        setNullableString(statement, 8, command.accessUrl());
        statement.setBoolean(9, command.attendanceRequired());
        statement.setString(10, command.state().toDatabaseValue());
        statement.setTimestamp(11, Timestamp.valueOf(command.startsAt()));
        statement.setTimestamp(12, Timestamp.valueOf(command.endsAt()));
    }

    private static void setStatementValues(PreparedStatement statement, LessonUpdateCommand command)
            throws SQLException {
        statement.setLong(1, command.classGroupId());
        statement.setLong(2, command.contentBlockId());
        setNullableString(statement, 3, command.physicalRoomCode());
        statement.setString(4, command.title().trim());
        setNullableString(statement, 5, command.description());
        statement.setString(6, command.type().toDatabaseValue());
        setNullableString(statement, 7, command.provider());
        setNullableString(statement, 8, command.accessUrl());
        statement.setBoolean(9, command.attendanceRequired());
        statement.setString(10, command.state().toDatabaseValue());
        statement.setTimestamp(11, Timestamp.valueOf(command.startsAt()));
        statement.setTimestamp(12, Timestamp.valueOf(command.endsAt()));
    }

    private static List<Lesson> mapLessons(ResultSet resultSet) throws SQLException {
        List<Lesson> lessons = new ArrayList<>();
        while (resultSet.next()) {
            lessons.add(mapLesson(resultSet));
        }
        return lessons;
    }

    private static Lesson mapLesson(ResultSet resultSet) throws SQLException {
        return new Lesson(
                resultSet.getLong("id_lesson"),
                resultSet.getLong("id_class_group"),
                resultSet.getLong("id_content_block"),
                resultSet.getString("cod_physical_room"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                LessonType.fromDatabaseValue(resultSet.getString("type")),
                resultSet.getString("provider"),
                resultSet.getString("access_url"),
                resultSet.getBoolean("attendance_required"),
                LessonState.fromDatabaseValue(resultSet.getString("state")),
                resultSet.getTimestamp("starts_at").toLocalDateTime(),
                resultSet.getTimestamp("ends_at").toLocalDateTime()
        );
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }
}
