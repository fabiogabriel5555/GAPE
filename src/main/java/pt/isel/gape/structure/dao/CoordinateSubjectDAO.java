package pt.isel.gape.structure.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.structure.model.CoordinateSubjectAssignment;
import pt.isel.gape.structure.model.RoleAssignmentState;

public final class CoordinateSubjectDAO implements pt.isel.gape.transversal.service.ApplicationReadService.CoordinateSubjects {

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
                  AND s.state = 'active'
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

    public void assign(long coordinatorUserId, long subjectId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            assign(connection, coordinatorUserId, subjectId);
        }
    }

    public void assign(
            Connection connection,
            long coordinatorUserId,
            long subjectId
    ) throws SQLException {
        String sql = """
                INSERT INTO coordinate_subject (id_coordinator_user, id_subject, state)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE state = VALUES(state)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, coordinatorUserId);
            statement.setLong(2, subjectId);
            statement.setString(3, RoleAssignmentState.ACTIVE.toDatabaseValue());
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
                  AND s.state = 'active'
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

    public List<CoordinateSubjectAssignment> findBySubject(long subjectId) throws SQLException {
        String sql = """
                SELECT cs.id_coordinator_user,
                       cs.id_subject,
                       cs.state,
                       u.name AS coordinator_name,
                       u.email AS coordinator_email
                FROM coordinate_subject cs
                JOIN user_account u ON u.id_user = cs.id_coordinator_user
                WHERE cs.id_subject = ?
                ORDER BY
                    CASE cs.state
                        WHEN 'active' THEN 0
                        WHEN 'inactive' THEN 1
                        ELSE 2
                    END,
                    u.name
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CoordinateSubjectAssignment> assignments = new ArrayList<>();
                while (resultSet.next()) {
                    assignments.add(mapAssignment(resultSet));
                }
                return assignments;
            }
        }
    }

    public void updateAssignment(
            Connection connection,
            long coordinatorUserId,
            long subjectId,
            RoleAssignmentState state
    ) throws SQLException {
        String sql = """
                UPDATE coordinate_subject
                SET state = ?
                WHERE id_coordinator_user = ?
                  AND id_subject = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            statement.setLong(2, coordinatorUserId);
            statement.setLong(3, subjectId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Coordinator assignment not found");
            }
        }
    }

    public void deleteAssignment(
            Connection connection,
            long coordinatorUserId,
            long subjectId
    ) throws SQLException {
        String sql = """
                DELETE FROM coordinate_subject
                WHERE id_coordinator_user = ?
                  AND id_subject = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, coordinatorUserId);
            statement.setLong(2, subjectId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Coordinator assignment not found");
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
                  AND s.state = 'active'
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
            Set<Long> selectedSubjectIds
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE coordinate_subject SET state = 'inactive' WHERE id_coordinator_user = ?"
        )) {
            statement.setLong(1, coordinatorUserId);
            statement.executeUpdate();
        }
        for (long subjectId : selectedSubjectIds) {
            assign(connection, coordinatorUserId, subjectId);
        }
    }

    private static CoordinateSubjectAssignment mapAssignment(ResultSet resultSet) throws SQLException {
        return new CoordinateSubjectAssignment(
                resultSet.getLong("id_coordinator_user"),
                resultSet.getLong("id_subject"),
                RoleAssignmentState.fromDatabaseValue(resultSet.getString("state")),
                resultSet.getString("coordinator_name"),
                resultSet.getString("coordinator_email")
        );
    }
}
