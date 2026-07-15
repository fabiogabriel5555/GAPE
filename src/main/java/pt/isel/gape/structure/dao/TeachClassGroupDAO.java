package pt.isel.gape.structure.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.structure.model.ClassGroupContext;
import pt.isel.gape.structure.model.RoleAssignmentState;
import pt.isel.gape.structure.model.TeacherClassGroupAssignment;

public final class TeachClassGroupDAO implements pt.isel.gape.transversal.service.ApplicationReadService.TeachClassGroups {

    private final ConnectionProvider connectionProvider;

    public TeachClassGroupDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public boolean canAssign(long teacherUserId, long classGroupId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return canAssign(connection, teacherUserId, classGroupId);
        }
    }

    public boolean canAssign(Connection connection, long teacherUserId, long classGroupId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM teacher_profile tp
                JOIN user_account u ON u.id_user = tp.id_user
                JOIN class_group cg ON cg.id_class_group = ?
                WHERE tp.id_user = ?
                  AND u.state = 'active'
                  AND cg.state = 'active'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            statement.setLong(2, teacherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public void assign(long teacherUserId, long classGroupId, LocalDate startDate, LocalDate endDate) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            assign(connection, teacherUserId, classGroupId, startDate, endDate);
        }
    }

    public void assign(
            Connection connection,
            long teacherUserId,
            long classGroupId,
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                INSERT INTO teach_class_group (id_teacher_user, id_class_group, state, start_date, end_date)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE state = VALUES(state), start_date = VALUES(start_date), end_date = VALUES(end_date)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            statement.setLong(2, classGroupId);
            statement.setString(3, RoleAssignmentState.ACTIVE.toDatabaseValue());
            setDate(statement, 4, startDate);
            setDate(statement, 5, endDate);
            statement.executeUpdate();
        }
    }

    public boolean hasActiveAssignment(long teacherUserId, long classGroupId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return hasActiveAssignment(connection, teacherUserId, classGroupId);
        }
    }

    public boolean hasActiveAssignment(Connection connection, long teacherUserId, long classGroupId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM teach_class_group tcg
                JOIN teacher_profile tp ON tp.id_user = tcg.id_teacher_user
                JOIN user_account u ON u.id_user = tp.id_user
                JOIN class_group cg ON cg.id_class_group = tcg.id_class_group
                WHERE tcg.id_teacher_user = ?
                  AND tcg.id_class_group = ?
                  AND tcg.state = 'active'
                  AND u.state = 'active'
                  AND cg.state = 'active'
                  AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                  AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            statement.setLong(2, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public Set<Long> findActiveClassGroupIdsByTeacher(long teacherUserId) throws SQLException {
        String sql = """
                SELECT tcg.id_class_group
                FROM teach_class_group tcg
                JOIN class_group cg ON cg.id_class_group = tcg.id_class_group
                WHERE tcg.id_teacher_user = ?
                  AND tcg.state = 'active'
                  AND cg.state = 'active'
                  AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                  AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                ORDER BY tcg.id_class_group
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                Set<Long> classGroupIds = new LinkedHashSet<>();
                while (resultSet.next()) {
                    classGroupIds.add(resultSet.getLong("id_class_group"));
                }
                return Set.copyOf(classGroupIds);
            }
        }
    }

    public long countActiveAssignments(long classGroupId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM teach_class_group tcg
                JOIN teacher_profile tp ON tp.id_user = tcg.id_teacher_user
                JOIN user_account u ON u.id_user = tp.id_user
                WHERE tcg.id_class_group = ?
                  AND tcg.state = 'active'
                  AND u.state = 'active'
                  AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                  AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public Map<Long, Integer> countActiveAssignmentsByClassGroupIds(Collection<Long> classGroupIds)
            throws SQLException {
        if (classGroupIds == null || classGroupIds.isEmpty()) {
            return Map.of();
        }
        String sql = """
                SELECT tcg.id_class_group, COUNT(*) AS teacher_count
                FROM teach_class_group tcg
                JOIN teacher_profile tp ON tp.id_user = tcg.id_teacher_user
                JOIN user_account u ON u.id_user = tp.id_user
                WHERE tcg.id_class_group IN (%s)
                  AND tcg.state = 'active'
                  AND u.state = 'active'
                  AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                  AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                GROUP BY tcg.id_class_group
                """.formatted(placeholders(classGroupIds.size()));

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Long classGroupId : classGroupIds) {
                statement.setLong(index++, classGroupId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Long, Integer> counts = new LinkedHashMap<>();
                while (resultSet.next()) {
                    counts.put(resultSet.getLong("id_class_group"), resultSet.getInt("teacher_count"));
                }
                return counts;
            }
        }
    }

    public List<TeacherClassGroupAssignment> findByClassGroup(long classGroupId) throws SQLException {
        String sql = """
                SELECT tcg.id_teacher_user, tcg.id_class_group, tcg.state, tcg.start_date, tcg.end_date,
                       u.name AS teacher_name, u.email AS teacher_email
                FROM teach_class_group tcg
                JOIN teacher_profile tp ON tp.id_user = tcg.id_teacher_user
                JOIN user_account u ON u.id_user = tp.id_user
                WHERE tcg.id_class_group = ?
                ORDER BY CASE tcg.state WHEN 'active' THEN 0 ELSE 1 END,
                         u.name,
                         tcg.id_teacher_user
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<TeacherClassGroupAssignment> assignments = new ArrayList<>();
                while (resultSet.next()) {
                    assignments.add(new TeacherClassGroupAssignment(
                            resultSet.getLong("id_teacher_user"),
                            resultSet.getLong("id_class_group"),
                            RoleAssignmentState.fromDatabaseValue(resultSet.getString("state")),
                            nullableDate(resultSet, "start_date"),
                            nullableDate(resultSet, "end_date"),
                            resultSet.getString("teacher_name"),
                            resultSet.getString("teacher_email")
                    ));
                }
                return assignments;
            }
        }
    }

    public boolean deactivate(
            Connection connection,
            long teacherUserId,
            long classGroupId,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                UPDATE teach_class_group
                SET state = ?, end_date = ?
                WHERE id_teacher_user = ?
                  AND id_class_group = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, RoleAssignmentState.INACTIVE.toDatabaseValue());
            setDate(statement, 2, endDate);
            statement.setLong(3, teacherUserId);
            statement.setLong(4, classGroupId);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * A teacher assignment follows the same lifecycle as a subject coordinator assignment:
     * its state can change, but it has no assignment-period fields to edit.
     */
    public boolean updateState(
            Connection connection,
            long teacherUserId,
            long classGroupId,
            RoleAssignmentState state
    ) throws SQLException {
        String sql = """
                UPDATE teach_class_group
                SET state = ?
                WHERE id_teacher_user = ?
                  AND id_class_group = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            statement.setLong(2, teacherUserId);
            statement.setLong(3, classGroupId);
            return statement.executeUpdate() > 0;
        }
    }

    public List<ClassGroupContext> findActiveByOrganizations(Collection<Long> organizationIds) throws SQLException {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(organizationIds.size(), "?"));
        String sql = """
                SELECT cg.id_class_group, cg.id_course, cg.id_subject, c.id_organization, c.id_organic_unit,
                       cg.cod_class_group, o.name AS organization_name, o.acronym AS organization_acronym,
                       ou.name AS organic_unit_name, ou.acronym AS organic_unit_acronym,
                       c.name AS course_name, c.acronym AS course_acronym,
                       s.name AS subject_name, s.acronym AS subject_acronym, cg.state
                FROM class_group cg
                JOIN course c ON c.id_course = cg.id_course
                JOIN subject s ON s.id_subject = cg.id_subject
                JOIN organization o ON o.id_organization = c.id_organization
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                WHERE c.id_organization IN (%s)
                  AND cg.state = 'active'
                  AND c.state = 'active'
                  AND s.state = 'active'
                ORDER BY c.name, s.name, cg.cod_class_group
                """.formatted(placeholders);

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (Long organizationId : organizationIds) {
                statement.setLong(index++, organizationId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ClassGroupContext> classGroups = new ArrayList<>();
                while (resultSet.next()) {
                    classGroups.add(mapClassGroupContext(resultSet));
                }
                return classGroups;
            }
        }
    }

    public ClassGroupContext requireContext(Connection connection, long classGroupId) throws SQLException {
        String sql = """
                SELECT cg.id_class_group, cg.id_course, cg.id_subject, c.id_organization, c.id_organic_unit,
                       cg.cod_class_group, o.name AS organization_name, o.acronym AS organization_acronym,
                       ou.name AS organic_unit_name, ou.acronym AS organic_unit_acronym,
                       c.name AS course_name, c.acronym AS course_acronym,
                       s.name AS subject_name, s.acronym AS subject_acronym, cg.state
                FROM class_group cg
                JOIN course c ON c.id_course = cg.id_course
                JOIN subject s ON s.id_subject = cg.id_subject
                JOIN organization o ON o.id_organization = c.id_organization
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                WHERE cg.id_class_group = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Class group not found: " + classGroupId);
                }
                return mapClassGroupContext(resultSet);
            }
        }
    }

    public ClassGroupContext requireContext(long classGroupId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return requireContext(connection, classGroupId);
        }
    }

    public void synchronizeAssignments(
            Connection connection,
            long teacherUserId,
            Set<Long> selectedClassGroupIds,
            LocalDate startDate
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE teach_class_group SET state = 'inactive', end_date = ? WHERE id_teacher_user = ?"
        )) {
            setDate(statement, 1, startDate);
            statement.setLong(2, teacherUserId);
            statement.executeUpdate();
        }
        for (long classGroupId : selectedClassGroupIds) {
            assign(connection, teacherUserId, classGroupId, startDate, null);
        }
    }

    private static ClassGroupContext mapClassGroupContext(ResultSet resultSet) throws SQLException {
        return new ClassGroupContext(
                resultSet.getLong("id_class_group"),
                resultSet.getLong("id_course"),
                resultSet.getLong("id_subject"),
                resultSet.getLong("id_organization"),
                nullableLong(resultSet, "id_organic_unit"),
                resultSet.getString("cod_class_group"),
                resultSet.getString("organization_name"),
                resultSet.getString("organization_acronym"),
                resultSet.getString("organic_unit_name"),
                resultSet.getString("organic_unit_acronym"),
                resultSet.getString("course_name"),
                resultSet.getString("course_acronym"),
                resultSet.getString("subject_name"),
                resultSet.getString("subject_acronym"),
                resultSet.getString("state")
        );
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static LocalDate nullableDate(ResultSet resultSet, String column) throws SQLException {
        Date value = resultSet.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private static void setDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(value));
        }
    }

    private static String placeholders(int count) {
        return String.join(", ", java.util.Collections.nCopies(count, "?"));
    }
}
