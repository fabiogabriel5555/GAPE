package pt.isel.gape.structure.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.structure.model.OrganizationCreateCommand;
import pt.isel.gape.structure.model.OrganizationState;
import pt.isel.gape.structure.model.OrganizationType;
import pt.isel.gape.structure.model.OrganizationUpdateCommand;

public final class OrganizationDAO {

    private final ConnectionProvider connectionProvider;

    public OrganizationDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public long create(Connection connection, OrganizationCreateCommand command) throws SQLException {
        String sql = """
                INSERT INTO organization (name, acronym, photo, type, state)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, command.name().trim());
            setNullableString(statement, 2, command.acronym());
            setNullableString(statement, 3, command.photo());
            statement.setString(4, command.type().toDatabaseValue());
            statement.setString(5, command.state().toDatabaseValue());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating organization failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public Optional<Organization> findById(long organizationId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(connection, organizationId);
        }
    }

    public Optional<Organization> findById(Connection connection, long organizationId) throws SQLException {
        String sql = """
                SELECT id_organization, name, acronym, photo, type, state
                FROM organization
                WHERE id_organization = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organizationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapOrganization(resultSet));
            }
        }
    }

    public List<Organization> findByAdministrator(long adminUserId) throws SQLException {
        String sql = """
                SELECT o.id_organization, o.name, o.acronym, o.photo, o.type, o.state
                FROM organization o
                JOIN manage_organization mo ON mo.id_organization = o.id_organization
                WHERE mo.id_admin_user = ?
                  AND mo.state = 'active'
                  AND o.state = 'active'
                  AND (mo.start_date IS NULL OR mo.start_date <= CURRENT_DATE)
                  AND (mo.end_date IS NULL OR mo.end_date >= CURRENT_DATE)
                ORDER BY o.id_organization
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adminUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Organization> organizations = new ArrayList<>();
                while (resultSet.next()) {
                    organizations.add(mapOrganization(resultSet));
                }
                return organizations;
            }
        }
    }

    public List<Organization> findByAdministratorPermissionContexts(long adminUserId, String permissionCode)
            throws SQLException {
        return findByAdministratorPermissionContexts(adminUserId, List.of(permissionCode));
    }

    public List<Organization> findByAdministratorPermissionContexts(
            long adminUserId,
            Collection<String> permissionCodes
    )
            throws SQLException {
        List<String> codes = permissionCodes == null
                ? List.of()
                : permissionCodes.stream().filter(code -> code != null && !code.isBlank()).distinct().toList();
        if (codes.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(codes.size(), "?"));
        String sql = """
                SELECT DISTINCT o.id_organization, o.name, o.acronym, o.photo, o.type, o.state
                FROM grant_administrator ga
                JOIN permission p ON p.cod_permission = ga.cod_permission
                JOIN organization o ON (
                    (ga.context_type = 'ORGANIZATION' AND o.id_organization = ga.context_id)
                    OR (
                        ga.context_type = 'ORGANIC_UNIT'
                        AND EXISTS (
                            SELECT 1
                            FROM organic_unit ou
                            WHERE ou.id_organic_unit = ga.context_id
                              AND ou.id_organization = o.id_organization
                        )
                    )
                    OR (
                        ga.context_type = 'COURSE'
                        AND EXISTS (
                            SELECT 1
                            FROM course c
                            WHERE c.id_course = ga.context_id
                              AND c.id_organization = o.id_organization
                        )
                    )
                    OR (
                        ga.context_type = 'SUBJECT'
                        AND EXISTS (
                            SELECT 1
                            FROM subject s
                            WHERE s.id_subject = ga.context_id
                              AND s.id_organization = o.id_organization
                        )
                    )
                    OR (
                        ga.context_type = 'CLASS_GROUP'
                        AND EXISTS (
                            SELECT 1
                            FROM class_group cg
                            JOIN course c ON c.id_course = cg.id_course
                            WHERE cg.id_class_group = ga.context_id
                              AND c.id_organization = o.id_organization
                        )
                    )
                )
                WHERE ga.id_admin_user = ?
                  AND ga.cod_permission IN (%s)
                  AND p.state = 'active'
                  AND o.state = 'active'
                ORDER BY o.id_organization
                """.formatted(placeholders);

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adminUserId);
            int index = 2;
            for (String code : codes) {
                statement.setString(index++, code);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Organization> organizations = new ArrayList<>();
                while (resultSet.next()) {
                    organizations.add(mapOrganization(resultSet));
                }
                return organizations;
            }
        }
    }

    public List<Organization> findActive() throws SQLException {
        String sql = """
                SELECT id_organization, name, acronym, photo, type, state
                FROM organization
                WHERE state = 'active'
                ORDER BY id_organization
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Organization> organizations = new ArrayList<>();
            while (resultSet.next()) {
                organizations.add(mapOrganization(resultSet));
            }
            return organizations;
        }
    }

    public void update(Connection connection, long organizationId, OrganizationUpdateCommand command) throws SQLException {
        String sql = """
                UPDATE organization
                SET name = ?, acronym = ?, photo = ?, type = ?, state = ?
                WHERE id_organization = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, command.name().trim());
            setNullableString(statement, 2, command.acronym());
            setNullableString(statement, 3, command.photo());
            statement.setString(4, command.type().toDatabaseValue());
            statement.setString(5, command.state().toDatabaseValue());
            statement.setLong(6, organizationId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Organization not found: " + organizationId);
            }
        }
    }

    public void updatePhoto(Connection connection, long organizationId, String photo) throws SQLException {
        String sql = """
                UPDATE organization
                SET photo = ?
                WHERE id_organization = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableString(statement, 1, photo);
            statement.setLong(2, organizationId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Organization not found: " + organizationId);
            }
        }
    }

    public void updateState(Connection connection, long organizationId, OrganizationState state) throws SQLException {
        String sql = """
                UPDATE organization
                SET state = ?
                WHERE id_organization = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.toDatabaseValue());
            statement.setLong(2, organizationId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Organization not found: " + organizationId);
            }
        }
    }

    public boolean hasDomainDependencies(Connection connection, long organizationId) throws SQLException {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM organic_unit WHERE id_organization = ?)
                  + (SELECT COUNT(*) FROM course WHERE id_organization = ?)
                  + (SELECT COUNT(*) FROM physical_room WHERE id_organization = ?)
                  + (SELECT COUNT(*) FROM associate_organization_content WHERE id_organization = ?)
                  AS dependency_count
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organizationId);
            statement.setLong(2, organizationId);
            statement.setLong(3, organizationId);
            statement.setLong(4, organizationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("dependency_count") > 0;
            }
        }
    }

    public void delete(Connection connection, long organizationId) throws SQLException {
        String sql = """
                DELETE FROM organization
                WHERE id_organization = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, organizationId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("Organization not found: " + organizationId);
            }
        }
    }

    private static Organization mapOrganization(ResultSet resultSet) throws SQLException {
        return new Organization(
                resultSet.getLong("id_organization"),
                resultSet.getString("name"),
                resultSet.getString("acronym"),
                resultSet.getString("photo"),
                OrganizationType.fromDatabaseValue(resultSet.getString("type")),
                OrganizationState.fromDatabaseValue(resultSet.getString("state"))
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
