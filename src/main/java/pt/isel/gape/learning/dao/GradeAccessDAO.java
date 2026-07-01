package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GradeAccessDAO {

    public long findClassGroupSubjectId(Connection connection, long classGroupId) throws SQLException {
        String sql = "SELECT id_subject FROM class_group WHERE id_class_group = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Class group not found: " + classGroupId);
                }
                return resultSet.getLong("id_subject");
            }
        }
    }

    public ClassGroupContext findClassGroupContext(Connection connection, long classGroupId) throws SQLException {
        String sql = """
                SELECT id_subject, id_course
                FROM class_group
                WHERE id_class_group = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Class group not found: " + classGroupId);
                }
                return new ClassGroupContext(
                        resultSet.getLong("id_subject"),
                        resultSet.getLong("id_course")
                );
            }
        }
    }

    public boolean teacherManagesSubject(Connection connection, long teacherUserId, long subjectId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM class_group cg
                JOIN teach_class_group tcg ON tcg.id_class_group = cg.id_class_group
                JOIN grant_teacher gt ON gt.id_teacher_user = tcg.id_teacher_user
                JOIN permission p ON p.cod_permission = gt.cod_permission
                JOIN user_account u ON u.id_user = tcg.id_teacher_user
                WHERE cg.id_subject = ?
                  AND tcg.id_teacher_user = ?
                  AND cg.state = 'active'
                  AND tcg.state = 'active'
                  AND gt.cod_permission = 'MANAGE_LEARNING'
                  AND p.state = 'active'
                  AND u.state = 'active'
                  AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                  AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                """;
        return countExists(connection, sql, subjectId, teacherUserId);
    }

    public boolean coordinatorManagesCourse(Connection connection, long coordinatorUserId, long courseId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM integrate_subject isub
                JOIN coordinate_subject cs ON cs.id_subject = isub.id_subject
                JOIN grant_coordinator gc ON gc.id_coordinator_user = cs.id_coordinator_user
                JOIN permission p ON p.cod_permission = gc.cod_permission
                JOIN user_account u ON u.id_user = cs.id_coordinator_user
                WHERE isub.id_course = ?
                  AND cs.id_coordinator_user = ?
                  AND cs.state = 'active'
                  AND gc.cod_permission = 'MANAGE_LEARNING'
                  AND p.state = 'active'
                  AND u.state = 'active'
                  AND (cs.start_date IS NULL OR cs.start_date <= CURRENT_DATE)
                  AND (cs.end_date IS NULL OR cs.end_date >= CURRENT_DATE)
                """;
        return countExists(connection, sql, courseId, coordinatorUserId);
    }

    public boolean teacherManagesCourse(Connection connection, long teacherUserId, long courseId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM class_group cg
                JOIN teach_class_group tcg ON tcg.id_class_group = cg.id_class_group
                JOIN grant_teacher gt ON gt.id_teacher_user = tcg.id_teacher_user
                JOIN permission p ON p.cod_permission = gt.cod_permission
                JOIN user_account u ON u.id_user = tcg.id_teacher_user
                WHERE cg.id_course = ?
                  AND tcg.id_teacher_user = ?
                  AND cg.state = 'active'
                  AND tcg.state = 'active'
                  AND gt.cod_permission = 'MANAGE_LEARNING'
                  AND p.state = 'active'
                  AND u.state = 'active'
                  AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                  AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                """;
        return countExists(connection, sql, courseId, teacherUserId);
    }

    public List<Long> teacherManagedClassGroupIds(Connection connection, long teacherUserId) throws SQLException {
        String sql = """
                SELECT DISTINCT cg.id_class_group
                FROM class_group cg
                JOIN teach_class_group tcg ON tcg.id_class_group = cg.id_class_group
                JOIN grant_teacher gt ON gt.id_teacher_user = tcg.id_teacher_user
                JOIN permission p ON p.cod_permission = gt.cod_permission
                JOIN user_account u ON u.id_user = tcg.id_teacher_user
                WHERE tcg.id_teacher_user = ?
                  AND cg.state = 'active'
                  AND tcg.state = 'active'
                  AND gt.cod_permission = 'MANAGE_LEARNING'
                  AND p.state = 'active'
                  AND u.state = 'active'
                  AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                  AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                ORDER BY cg.id_class_group
                """;
        return findIds(connection, sql, teacherUserId);
    }

    public List<Long> coordinatorManagedClassGroupIds(Connection connection, long coordinatorUserId) throws SQLException {
        String sql = """
                SELECT DISTINCT cg.id_class_group
                FROM class_group cg
                JOIN coordinate_subject cs ON cs.id_subject = cg.id_subject
                JOIN grant_coordinator gc ON gc.id_coordinator_user = cs.id_coordinator_user
                JOIN permission p ON p.cod_permission = gc.cod_permission
                JOIN user_account u ON u.id_user = cs.id_coordinator_user
                WHERE cs.id_coordinator_user = ?
                  AND cg.state = 'active'
                  AND cs.state = 'active'
                  AND gc.cod_permission = 'MANAGE_LEARNING'
                  AND p.state = 'active'
                  AND u.state = 'active'
                  AND (cs.start_date IS NULL OR cs.start_date <= CURRENT_DATE)
                  AND (cs.end_date IS NULL OR cs.end_date >= CURRENT_DATE)
                ORDER BY cg.id_class_group
                """;
        return findIds(connection, sql, coordinatorUserId);
    }

    public List<Long> activeStudentIdsInClassGroups(Connection connection, List<Long> classGroupIds)
            throws SQLException {
        if (classGroupIds == null || classGroupIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(", ", Collections.nCopies(classGroupIds.size(), "?"));
        String sql = """
                SELECT DISTINCT ecg.id_student_user
                FROM enroll_class_group ecg
                JOIN user_account u ON u.id_user = ecg.id_student_user
                WHERE ecg.id_class_group IN (%s)
                  AND ecg.state = 'active'
                  AND u.state = 'active'
                  AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                  AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                ORDER BY ecg.id_student_user
                """.formatted(placeholders);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < classGroupIds.size(); index++) {
                statement.setLong(index + 1, classGroupIds.get(index));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (resultSet.next()) {
                    ids.add(resultSet.getLong(1));
                }
                return List.copyOf(ids);
            }
        }
    }

    private static boolean countExists(Connection connection, String sql, long firstId, long secondId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, firstId);
            statement.setLong(2, secondId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    private static List<Long> findIds(Connection connection, String sql, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (resultSet.next()) {
                    ids.add(resultSet.getLong(1));
                }
                return List.copyOf(ids);
            }
        }
    }

    public record ClassGroupContext(long subjectId, long courseId) {
    }
}
