package pt.isel.gape.structure.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.structure.model.RoleAssignmentState;

public final class TeachClassGroupDAO {

    private final ConnectionProvider connectionProvider;

    public TeachClassGroupDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public boolean canAssign(long teacherUserId, long classGroupId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM teacher_profile tp
                JOIN user_account u ON u.id_user = tp.id_user
                JOIN class_group cg ON cg.id_class_group = ?
                WHERE tp.id_user = ?
                  AND u.state = 'active'
                  AND cg.state = 'active'
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            statement.setLong(2, teacherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public void assign(long teacherUserId, long classGroupId, LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = """
                INSERT INTO teach_class_group (id_teacher_user, id_class_group, state, start_date, end_date)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE state = VALUES(state), start_date = VALUES(start_date), end_date = VALUES(end_date)
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            statement.setLong(2, classGroupId);
            statement.setString(3, RoleAssignmentState.ACTIVE.toDatabaseValue());
            setDate(statement, 4, startDate);
            setDate(statement, 5, endDate);
            statement.executeUpdate();
        }
    }

    public boolean hasActiveAssignment(long teacherUserId, long classGroupId) throws SQLException {
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

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            statement.setLong(2, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private static void setDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(value));
        }
    }
}
