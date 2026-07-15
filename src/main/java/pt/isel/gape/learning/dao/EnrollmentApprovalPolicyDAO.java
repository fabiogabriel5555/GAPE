package pt.isel.gape.learning.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.EnrollmentApprovalMode;

public final class EnrollmentApprovalPolicyDAO implements pt.isel.gape.transversal.service.ApplicationReadService.EnrollmentApprovalPolicies {

    private final ConnectionProvider connectionProvider;

    public EnrollmentApprovalPolicyDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public EnrollmentApprovalMode classGroupMode(Connection connection, long classGroupId) throws SQLException {
        String sql = """
                SELECT approval_mode
                FROM class_group_enrollment_policy
                WHERE id_class_group = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? EnrollmentApprovalMode.fromDatabaseValue(resultSet.getString("approval_mode"))
                        : EnrollmentApprovalMode.MANUAL;
            }
        }
    }

    public EnrollmentApprovalMode classGroupMode(long classGroupId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return classGroupMode(connection, classGroupId);
        }
    }

    public void upsertClassGroupMode(Connection connection, long classGroupId, EnrollmentApprovalMode mode)
            throws SQLException {
        String sql = """
                INSERT INTO class_group_enrollment_policy (id_class_group, approval_mode)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE approval_mode = VALUES(approval_mode)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classGroupId);
            statement.setString(2, mode.toDatabaseValue());
            statement.executeUpdate();
        }
    }
}
