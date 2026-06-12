package pt.isel.gape.structure.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.structure.model.RoleAssignmentState;

public final class CoordinateSubjectDAO {

    private final ConnectionProvider connectionProvider;

    public CoordinateSubjectDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public boolean canAssign(long coordinatorUserId, long subjectId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return canAssign(connection, coordinatorUserId, subjectId);
        }
    }

    public boolean canAssign(Connection connection, long coordinatorUserId, long subjectId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM coordinator_profile cp
                JOIN user_account u ON u.id_user = cp.id_user
                JOIN subject s ON s.id_subject = ?
                WHERE cp.id_user = ?
                  AND u.state = 'active'
                  AND s.state <> 'archived'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            statement.setLong(2, coordinatorUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public void assign(long coordinatorUserId, long subjectId, LocalDate startDate, LocalDate endDate) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            assign(connection, coordinatorUserId, subjectId, startDate, endDate);
        }
    }

    public void assign(
            Connection connection,
            long coordinatorUserId,
            long subjectId,
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                INSERT INTO coordinate_subject (id_coordinator_user, id_subject, state, start_date, end_date)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE state = VALUES(state), start_date = VALUES(start_date), end_date = VALUES(end_date)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, coordinatorUserId);
            statement.setLong(2, subjectId);
            statement.setString(3, RoleAssignmentState.ACTIVE.toDatabaseValue());
            setDate(statement, 4, startDate);
            setDate(statement, 5, endDate);
            statement.executeUpdate();
        }
    }

    public boolean hasActiveAssignment(long coordinatorUserId, long subjectId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return hasActiveAssignment(connection, coordinatorUserId, subjectId);
        }
    }

    public boolean hasActiveAssignment(Connection connection, long coordinatorUserId, long subjectId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM coordinate_subject cs
                JOIN coordinator_profile cp ON cp.id_user = cs.id_coordinator_user
                JOIN user_account u ON u.id_user = cp.id_user
                JOIN subject s ON s.id_subject = cs.id_subject
                WHERE cs.id_coordinator_user = ?
                  AND cs.id_subject = ?
                  AND cs.state = 'active'
                  AND u.state = 'active'
                  AND s.state <> 'archived'
                  AND (cs.start_date IS NULL OR cs.start_date <= CURRENT_DATE)
                  AND (cs.end_date IS NULL OR cs.end_date >= CURRENT_DATE)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, coordinatorUserId);
            statement.setLong(2, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public Set<Long> findActiveSubjectIdsByCoordinator(long coordinatorUserId) throws SQLException {
        String sql = """
                SELECT cs.id_subject
                FROM coordinate_subject cs
                JOIN subject s ON s.id_subject = cs.id_subject
                WHERE cs.id_coordinator_user = ?
                  AND cs.state = 'active'
                  AND s.state <> 'archived'
                  AND (cs.start_date IS NULL OR cs.start_date <= CURRENT_DATE)
                  AND (cs.end_date IS NULL OR cs.end_date >= CURRENT_DATE)
                ORDER BY cs.id_subject
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, coordinatorUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                Set<Long> subjectIds = new LinkedHashSet<>();
                while (resultSet.next()) {
                    subjectIds.add(resultSet.getLong("id_subject"));
                }
                return Set.copyOf(subjectIds);
            }
        }
    }

    public void synchronizeAssignments(
            Connection connection,
            long coordinatorUserId,
            Set<Long> selectedSubjectIds,
            LocalDate startDate
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE coordinate_subject SET state = 'inactive', end_date = ? WHERE id_coordinator_user = ?"
        )) {
            setDate(statement, 1, startDate);
            statement.setLong(2, coordinatorUserId);
            statement.executeUpdate();
        }
        for (long subjectId : selectedSubjectIds) {
            assign(connection, coordinatorUserId, subjectId, startDate, null);
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
