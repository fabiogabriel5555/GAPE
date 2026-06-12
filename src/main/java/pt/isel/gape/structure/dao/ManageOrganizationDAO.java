package pt.isel.gape.structure.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import pt.isel.gape.access.model.UserState;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.structure.model.OrganizationAdministratorAssignment;
import pt.isel.gape.structure.model.RoleAssignmentState;

public final class ManageOrganizationDAO {

    private final ConnectionProvider connectionProvider;

    public ManageOrganizationDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public boolean canAssign(long adminUserId, long organizationId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return canAssign(connection, adminUserId, organizationId);
        }
    }

    public boolean canAssign(Connection connection, long adminUserId, long organizationId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM administrator_profile ap
                JOIN user_account u ON u.id_user = ap.id_user
                JOIN organization o ON o.id_organization = ?
                WHERE ap.id_user = ?
                  AND u.state = 'active'
                  AND o.state <> 'archived'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organizationId);
            statement.setLong(2, adminUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public void assign(long adminUserId, long organizationId, LocalDate startDate, LocalDate endDate) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            assign(connection, adminUserId, organizationId, startDate, endDate);
        }
    }

    public void assign(
            Connection connection,
            long adminUserId,
            long organizationId,
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        String sql = """
                INSERT INTO manage_organization (id_admin_user, id_organization, state, start_date, end_date)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE state = VALUES(state), start_date = VALUES(start_date), end_date = VALUES(end_date)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adminUserId);
            statement.setLong(2, organizationId);
            statement.setString(3, RoleAssignmentState.ACTIVE.toDatabaseValue());
            setDate(statement, 4, startDate);
            setDate(statement, 5, endDate);
            statement.executeUpdate();
        }
    }

    public boolean hasActiveAssignment(long adminUserId, long organizationId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return hasActiveAssignment(connection, adminUserId, organizationId);
        }
    }

    public boolean hasActiveAssignment(Connection connection, long adminUserId, long organizationId) throws SQLException {
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
                  AND o.state <> 'archived'
                  AND (mo.start_date IS NULL OR mo.start_date <= CURRENT_DATE)
                  AND (mo.end_date IS NULL OR mo.end_date >= CURRENT_DATE)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adminUserId);
            statement.setLong(2, organizationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public Set<Long> findActiveOrganizationIdsByAdministrator(long adminUserId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findActiveOrganizationIdsByAdministrator(connection, adminUserId);
        }
    }

    public Set<Long> findActiveOrganizationIdsByAdministrator(Connection connection, long adminUserId)
            throws SQLException {
        String sql = """
                SELECT mo.id_organization
                FROM manage_organization mo
                JOIN administrator_profile ap ON ap.id_user = mo.id_admin_user
                JOIN user_account u ON u.id_user = ap.id_user
                JOIN organization o ON o.id_organization = mo.id_organization
                WHERE mo.id_admin_user = ?
                  AND mo.state = 'active'
                  AND u.state = 'active'
                  AND o.state <> 'archived'
                  AND (mo.start_date IS NULL OR mo.start_date <= CURRENT_DATE)
                  AND (mo.end_date IS NULL OR mo.end_date >= CURRENT_DATE)
                ORDER BY o.name, mo.id_organization
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adminUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                Set<Long> organizationIds = new LinkedHashSet<>();
                while (resultSet.next()) {
                    organizationIds.add(resultSet.getLong("id_organization"));
                }
                return organizationIds;
            }
        }
    }

    public void synchronizeAssignments(
            Connection connection,
            long adminUserId,
            Set<Long> organizationIdsInScope,
            Set<Long> selectedOrganizationIds,
            LocalDate startDate
    ) throws SQLException {
        Set<Long> scope = organizationIdsInScope == null ? Set.of() : new LinkedHashSet<>(organizationIdsInScope);
        Set<Long> selected = selectedOrganizationIds == null ? Set.of() : new LinkedHashSet<>(selectedOrganizationIds);

        for (long organizationId : selected) {
            if (scope.contains(organizationId)) {
                assign(connection, adminUserId, organizationId, startDate, null);
            }
        }
        for (long organizationId : scope) {
            if (!selected.contains(organizationId)) {
                archiveAssignment(connection, adminUserId, organizationId);
            }
        }
    }

    public boolean hasAnyActiveAdministrator(Connection connection, long organizationId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM manage_organization mo
                JOIN administrator_profile ap ON ap.id_user = mo.id_admin_user
                JOIN user_account u ON u.id_user = ap.id_user
                WHERE mo.id_organization = ?
                  AND mo.state = 'active'
                  AND u.state = 'active'
                  AND (mo.start_date IS NULL OR mo.start_date <= CURRENT_DATE)
                  AND (mo.end_date IS NULL OR mo.end_date >= CURRENT_DATE)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organizationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private static void archiveAssignment(Connection connection, long adminUserId, long organizationId)
            throws SQLException {
        String sql = """
                UPDATE manage_organization
                SET state = ?
                WHERE id_admin_user = ?
                  AND id_organization = ?
                  AND state <> ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, RoleAssignmentState.ARCHIVED.toDatabaseValue());
            statement.setLong(2, adminUserId);
            statement.setLong(3, organizationId);
            statement.setString(4, RoleAssignmentState.ARCHIVED.toDatabaseValue());
            statement.executeUpdate();
        }
    }

    public List<OrganizationAdministratorAssignment> findAssignmentsByOrganization(long organizationId)
            throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findAssignmentsByOrganization(connection, organizationId);
        }
    }

    public List<OrganizationAdministratorAssignment> findAssignmentsByOrganization(
            Connection connection,
            long organizationId
    ) throws SQLException {
        String sql = """
                SELECT u.id_user, u.name, u.email, u.state AS user_state,
                       mo.state AS assignment_state, mo.start_date, mo.end_date
                FROM manage_organization mo
                JOIN user_account u ON u.id_user = mo.id_admin_user
                WHERE mo.id_organization = ?
                ORDER BY mo.state, u.name, u.email
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organizationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<OrganizationAdministratorAssignment> assignments = new ArrayList<>();
                while (resultSet.next()) {
                    assignments.add(mapAssignment(resultSet));
                }
                return assignments;
            }
        }
    }

    public List<Long> findActiveOrganizationsWithoutActiveAdministrator(Connection connection) throws SQLException {
        String sql = """
                SELECT o.id_organization
                FROM organization o
                WHERE o.state = 'active'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM manage_organization mo
                      JOIN administrator_profile ap ON ap.id_user = mo.id_admin_user
                      JOIN user_account u ON u.id_user = ap.id_user
                      WHERE mo.id_organization = o.id_organization
                        AND mo.state = 'active'
                        AND u.state = 'active'
                        AND (mo.start_date IS NULL OR mo.start_date <= CURRENT_DATE)
                        AND (mo.end_date IS NULL OR mo.end_date >= CURRENT_DATE)
                  )
                ORDER BY o.id_organization
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Long> organizationIds = new ArrayList<>();
            while (resultSet.next()) {
                organizationIds.add(resultSet.getLong("id_organization"));
            }
            return organizationIds;
        }
    }

    private static void setDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(value));
        }
    }

    private static OrganizationAdministratorAssignment mapAssignment(ResultSet resultSet) throws SQLException {
        Date startDate = resultSet.getDate("start_date");
        Date endDate = resultSet.getDate("end_date");
        return new OrganizationAdministratorAssignment(
                resultSet.getLong("id_user"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                UserState.fromDatabaseValue(resultSet.getString("user_state")),
                RoleAssignmentState.fromDatabaseValue(resultSet.getString("assignment_state")),
                startDate == null ? null : startDate.toLocalDate(),
                endDate == null ? null : endDate.toLocalDate()
        );
    }
}
