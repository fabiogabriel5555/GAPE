package pt.isel.gape.structure.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.structure.model.RoleAssignmentState;

public final class ManageOrganizationDAO {

    private final ConnectionProvider connectionProvider;

    public ManageOrganizationDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public boolean canAssign(long adminUserId, long organizationId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM administrator_profile ap
                JOIN user_account u ON u.id_user = ap.id_user
                JOIN organization o ON o.id_organization = ?
                WHERE ap.id_user = ?
                  AND u.state = 'active'
                  AND o.state = 'active'
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organizationId);
            statement.setLong(2, adminUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public void assign(long adminUserId, long organizationId, LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = """
                INSERT INTO manage_organization (id_admin_user, id_organization, state, start_date, end_date)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE state = VALUES(state), start_date = VALUES(start_date), end_date = VALUES(end_date)
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adminUserId);
            statement.setLong(2, organizationId);
            statement.setString(3, RoleAssignmentState.ACTIVE.toDatabaseValue());
            setDate(statement, 4, startDate);
            setDate(statement, 5, endDate);
            statement.executeUpdate();
        }
    }

    public boolean hasActiveAssignment(long adminUserId, long organizationId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM manage_organization mo
                JOIN administrator_profile ap ON ap.id_user = mo.id_admin_user
                JOIN user_account u ON u.id_user = ap.id_user
                JOIN organization o ON o.id_organization = mo.id_organization
                WHERE mo.id_admin_user = ?
                  AND mo.id_organization = ?
                  AND mo.state = 'active'
                  AND u.state = 'active'
                  AND o.state = 'active'
                  AND (mo.start_date IS NULL OR mo.start_date <= CURRENT_DATE)
                  AND (mo.end_date IS NULL OR mo.end_date >= CURRENT_DATE)
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adminUserId);
            statement.setLong(2, organizationId);
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
