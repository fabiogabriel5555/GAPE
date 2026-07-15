package pt.isel.gape.access.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import pt.isel.gape.access.model.AdministratorPermissionAssignment;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.Permission;
import pt.isel.gape.access.model.PermissionState;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationPolicy;

public final class PermissionDAO implements pt.isel.gape.transversal.service.ApplicationReadService.Permissions {

    private final ConnectionProvider connectionProvider;

    public PermissionDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public Optional<Permission> findByCode(String permissionCode) throws SQLException {
        String sql = """
                SELECT cod_permission, name, state
                FROM permission
                WHERE cod_permission = ?
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, permissionCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapPermission(resultSet));
            }
        }
    }

    public boolean isActivePermission(String permissionCode) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM permission
                WHERE cod_permission = ? AND state = 'active'
                """;

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, permissionCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean hasActiveGrant(long userId, AccessProfileType profileType, String permissionCode) throws SQLException {
        if (profileType == AccessProfileType.ADMINISTRATOR) {
            return hasAnyActiveAdministratorGrant(userId, Set.of(AuthorizationPolicy.canonicalAdminPermission(permissionCode)));
        }
        GrantTable grantTable = GrantTable.forProfile(profileType);
        String sql = """
                SELECT COUNT(*)
                FROM %s grant_table
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                WHERE grant_table.%s = ?
                  AND grant_table.cod_permission = ?
                  AND p.state = 'active'
                """.formatted(grantTable.tableName(), grantTable.userIdColumn());

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, permissionCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public Set<String> findActivePermissionCodes(long userId, AccessProfileType profileType) throws SQLException {
        GrantTable grantTable = GrantTable.forProfile(profileType);
        if (profileType == AccessProfileType.ADMINISTRATOR) {
            String sql = """
                    SELECT DISTINCT grant_table.cod_permission
                    FROM grant_administrator grant_table
                    JOIN permission p ON p.cod_permission = grant_table.cod_permission
                    WHERE grant_table.id_admin_user = ?
                      AND p.state = 'active'
                    ORDER BY grant_table.cod_permission
                    """;

            Set<String> permissions = new LinkedHashSet<>();
            try (Connection connection = connectionProvider.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        permissions.add(resultSet.getString("cod_permission"));
                    }
                }
            }
            return permissions;
        }
        String sql = """
                SELECT grant_table.cod_permission
                FROM %s grant_table
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                WHERE grant_table.%s = ?
                  AND p.state = 'active'
                ORDER BY grant_table.cod_permission
                """.formatted(grantTable.tableName(), grantTable.userIdColumn());

        Set<String> permissions = new LinkedHashSet<>();
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    permissions.add(resultSet.getString("cod_permission"));
                }
            }
        }
        return permissions;
    }

    public boolean activeProfileExists(long userId, AccessProfileType profileType) throws SQLException {
        GrantTable grantTable = GrantTable.forProfile(profileType);
        String sql = """
                SELECT COUNT(*)
                FROM %s profile_table
                JOIN user_account u ON u.id_user = profile_table.id_user
                WHERE profile_table.id_user = ?
                  AND u.state = 'active'
                """.formatted(grantTable.profileTableName());

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public void grantPermission(long userId, AccessProfileType profileType, String permissionCode) throws SQLException {
        if (profileType == AccessProfileType.ADMINISTRATOR) {
            AdministratorPermissionAssignment assignment = new AdministratorPermissionAssignment(
                    AuthorizationPolicy.canonicalAdminPermission(permissionCode),
                    AccessEntityType.GLOBAL,
                    0L
            );
            try (Connection connection = connectionProvider.getConnection()) {
                grantAdministratorPermission(connection, userId, assignment);
            }
            return;
        }
        GrantTable grantTable = GrantTable.forProfile(profileType);
        String sql = """
                INSERT INTO %s (%s, cod_permission)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE cod_permission = VALUES(cod_permission)
                """.formatted(grantTable.tableName(), grantTable.userIdColumn());

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, permissionCode);
            statement.executeUpdate();
        }
    }

    public void revokePermission(long userId, AccessProfileType profileType, String permissionCode) throws SQLException {
        if (profileType == AccessProfileType.ADMINISTRATOR) {
            String sql = """
                    DELETE FROM grant_administrator
                    WHERE id_admin_user = ? AND cod_permission = ?
                    """;

            try (Connection connection = connectionProvider.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, userId);
                statement.setString(2, AuthorizationPolicy.canonicalAdminPermission(permissionCode));
                statement.executeUpdate();
            }
            return;
        }
        GrantTable grantTable = GrantTable.forProfile(profileType);
        String sql = """
                DELETE FROM %s
                WHERE %s = ? AND cod_permission = ?
                """.formatted(grantTable.tableName(), grantTable.userIdColumn());

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, permissionCode);
            statement.executeUpdate();
        }
    }

    public boolean hasActiveGlobalManageAllAdministrator(Connection connection) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM grant_administrator grant_table
                JOIN administrator_profile ap ON ap.id_user = grant_table.id_admin_user
                JOIN user_account u ON u.id_user = ap.id_user
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                WHERE grant_table.cod_permission = ?
                  AND grant_table.context_type = 'GLOBAL'
                  AND grant_table.context_id = 0
                  AND u.state = 'active'
                  AND p.state = 'active'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, AuthorizationPolicy.MANAGE_ALL);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean isSoleActiveGlobalManageAllAdministrator(long adminUserId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return isSoleActiveGlobalManageAllAdministrator(connection, adminUserId);
        }
    }

    public boolean isSoleActiveGlobalManageAllAdministrator(Connection connection, long adminUserId) throws SQLException {
        String sql = """
                SELECT
                    SUM(CASE WHEN grant_table.id_admin_user = ? THEN 1 ELSE 0 END) AS target_count,
                    COUNT(*) AS total_count
                FROM grant_administrator grant_table
                JOIN administrator_profile ap ON ap.id_user = grant_table.id_admin_user
                JOIN user_account u ON u.id_user = ap.id_user
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                WHERE grant_table.cod_permission = ?
                  AND grant_table.context_type = 'GLOBAL'
                  AND grant_table.context_id = 0
                  AND u.state = 'active'
                  AND p.state = 'active'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adminUserId);
            statement.setString(2, AuthorizationPolicy.MANAGE_ALL);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt("target_count") > 0 && resultSet.getInt("total_count") == 1;
            }
        }
    }

    public Set<AdministratorPermissionAssignment> findActiveAdministratorAssignments(long adminUserId)
            throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findActiveAdministratorAssignments(connection, adminUserId);
        }
    }

    public Set<AdministratorPermissionAssignment> findActiveAdministratorAssignments(
            Connection connection,
            long adminUserId
    ) throws SQLException {
        String sql = """
                SELECT grant_table.cod_permission, grant_table.context_type, grant_table.context_id
                FROM grant_administrator grant_table
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                WHERE grant_table.id_admin_user = ?
                  AND p.state = 'active'
                ORDER BY grant_table.cod_permission, grant_table.context_type, grant_table.context_id
                """;

        Set<AdministratorPermissionAssignment> assignments = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adminUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    assignments.add(new AdministratorPermissionAssignment(
                            resultSet.getString("cod_permission"),
                            AccessEntityType.valueOf(resultSet.getString("context_type")),
                            resultSet.getLong("context_id")
                    ));
                }
            }
        }
        return Set.copyOf(assignments);
    }

    public Set<Long> findActiveAdministratorUserIds(Connection connection) throws SQLException {
        String sql = """
                SELECT ap.id_user
                FROM administrator_profile ap
                JOIN user_account u ON u.id_user = ap.id_user
                WHERE u.state = 'active'
                ORDER BY u.name, u.email, ap.id_user
                """;

        Set<Long> adminUserIds = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                adminUserIds.add(resultSet.getLong("id_user"));
            }
        }
        return java.util.Collections.unmodifiableSet(adminUserIds);
    }

    public Set<Long> findActiveAdministratorUserIdsByExactContext(
            Connection connection,
            String permissionCode,
            AccessEntityType contextType,
            long contextId
    ) throws SQLException {
        String sql = """
                SELECT grant_table.id_admin_user
                FROM grant_administrator grant_table
                JOIN administrator_profile ap ON ap.id_user = grant_table.id_admin_user
                JOIN user_account u ON u.id_user = ap.id_user
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                WHERE grant_table.cod_permission = ?
                  AND grant_table.context_type = ?
                  AND grant_table.context_id = ?
                  AND u.state = 'active'
                  AND p.state = 'active'
                ORDER BY u.name, u.email, grant_table.id_admin_user
                """;

        Set<Long> adminUserIds = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, AuthorizationPolicy.canonicalAdminPermission(permissionCode));
            statement.setString(2, contextType.name());
            statement.setLong(3, contextId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    adminUserIds.add(resultSet.getLong("id_admin_user"));
                }
            }
        }
        return java.util.Collections.unmodifiableSet(adminUserIds);
    }

    public boolean hasAnyActiveAdministratorGrant(long adminUserId, Collection<String> permissionCodes)
            throws SQLException {
        Set<String> canonicalCodes = canonicalAdminCodes(permissionCodes);
        if (canonicalCodes.isEmpty()) {
            return false;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(canonicalCodes.size(), "?"));
        String sql = """
                SELECT COUNT(*)
                FROM grant_administrator grant_table
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                WHERE grant_table.id_admin_user = ?
                  AND grant_table.cod_permission IN (%s)
                  AND p.state = 'active'
                """.formatted(placeholders);

        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adminUserId);
            int index = 2;
            for (String code : canonicalCodes) {
                statement.setString(index++, code);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean hasActiveAdministratorContextGrant(
            long adminUserId,
            String permissionCode,
            AccessEntityType entityType,
            long entityId
    ) throws SQLException {
        String canonicalPermission = AuthorizationPolicy.canonicalAdminPermission(permissionCode);
        return switch (entityType) {
            case ORGANIZATION -> hasExactAdministratorContextGrant(
                    adminUserId,
                    canonicalPermission,
                    AccessEntityType.ORGANIZATION,
                    entityId
            );
            case ORGANIC_UNIT -> hasOrganicUnitContextGrant(adminUserId, canonicalPermission, entityId);
            case COURSE -> hasCourseContextGrant(adminUserId, canonicalPermission, entityId);
            case SUBJECT -> hasSubjectContextGrant(adminUserId, canonicalPermission, entityId);
            case CLASS_GROUP -> hasClassGroupContextGrant(adminUserId, canonicalPermission, entityId);
            case GLOBAL, SELF -> hasAnyActiveAdministratorGrant(adminUserId, Set.of(canonicalPermission));
        };
    }

    public boolean hasActiveGlobalAdministratorGrant(long adminUserId, String permissionCode)
            throws SQLException {
        return hasExactAdministratorContextGrant(
                adminUserId,
                AuthorizationPolicy.canonicalAdminPermission(permissionCode),
                AccessEntityType.GLOBAL,
                0L
        );
    }

    public boolean hasActiveAdministratorDescendantGrant(
            long adminUserId,
            String permissionCode,
            AccessEntityType entityType,
            long entityId
    ) throws SQLException {
        String canonicalPermission = AuthorizationPolicy.canonicalAdminPermission(permissionCode);
        return switch (entityType) {
            case ORGANIZATION -> false;
            case ORGANIC_UNIT -> hasOrganicUnitDescendantGrant(adminUserId, canonicalPermission, entityId);
            case COURSE -> hasCourseDescendantGrant(adminUserId, canonicalPermission, entityId);
            case SUBJECT -> hasSubjectDescendantGrant(adminUserId, canonicalPermission, entityId);
            case CLASS_GROUP -> hasClassGroupDescendantGrant(adminUserId, canonicalPermission, entityId);
            case GLOBAL, SELF -> hasActiveGlobalAdministratorGrant(adminUserId, canonicalPermission);
        };
    }

    public boolean hasExactAdministratorContextGrant(
            long adminUserId,
            String permissionCode,
            AccessEntityType contextType,
            long contextId
    ) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return hasExactAdministratorContextGrant(
                    connection,
                    adminUserId,
                    permissionCode,
                    contextType,
                    contextId
            );
        }
    }

    public boolean hasExactAdministratorContextGrant(
            Connection connection,
            long adminUserId,
            String permissionCode,
            AccessEntityType contextType,
            long contextId
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM grant_administrator grant_table
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                WHERE grant_table.id_admin_user = ?
                  AND grant_table.cod_permission = ?
                  AND grant_table.context_type = ?
                  AND grant_table.context_id = ?
                  AND p.state = 'active'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adminUserId);
            statement.setString(2, AuthorizationPolicy.canonicalAdminPermission(permissionCode));
            statement.setString(3, contextType.name());
            statement.setLong(4, contextId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    public void synchronizeAdministratorAssignments(
            Connection connection,
            long adminUserId,
            Collection<AdministratorPermissionAssignment> assignments
    ) throws SQLException {
        Set<AdministratorPermissionAssignment> current = findActiveAdministratorAssignments(connection, adminUserId);
        Set<AdministratorPermissionAssignment> selected = assignments == null
                ? Set.of()
                : Set.copyOf(assignments);

        for (AdministratorPermissionAssignment assignment : current) {
            if (!selected.contains(assignment)) {
                deleteAdministratorAssignment(connection, adminUserId, assignment);
            }
        }
        for (AdministratorPermissionAssignment assignment : new LinkedHashSet<>(selected)) {
            if (!current.contains(assignment)) {
                grantAdministratorPermission(connection, adminUserId, assignment);
            }
        }
    }

    public void deleteAdministratorAssignments(Connection connection, long adminUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM grant_administrator WHERE id_admin_user = ?"
        )) {
            statement.setLong(1, adminUserId);
            statement.executeUpdate();
        }
    }

    public void deleteAdministratorAssignment(
            Connection connection,
            long adminUserId,
            AdministratorPermissionAssignment assignment
    ) throws SQLException {
        String sql = """
                DELETE FROM grant_administrator
                WHERE id_admin_user = ?
                  AND cod_permission = ?
                  AND context_type = ?
                  AND context_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adminUserId);
            statement.setString(2, assignment.permissionCode());
            statement.setString(3, assignment.contextType().name());
            statement.setLong(4, assignment.contextId());
            statement.executeUpdate();
        }
    }

    public void grantAdministratorPermission(
            Connection connection,
            long adminUserId,
            AdministratorPermissionAssignment assignment
    ) throws SQLException {
        String sql = """
                INSERT INTO grant_administrator (id_admin_user, cod_permission, context_type, context_id)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE cod_permission = VALUES(cod_permission)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adminUserId);
            statement.setString(2, assignment.permissionCode());
            statement.setString(3, assignment.contextType().name());
            statement.setLong(4, assignment.contextId());
            statement.executeUpdate();
        }
    }

    private boolean hasOrganicUnitContextGrant(long adminUserId, String permissionCode, long organicUnitId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM grant_administrator grant_table
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                JOIN organic_unit ou ON ou.id_organic_unit = ?
                WHERE grant_table.id_admin_user = ?
                  AND grant_table.cod_permission = ?
                  AND p.state = 'active'
                  AND (
                        (grant_table.context_type = 'ORGANIC_UNIT' AND grant_table.context_id = ou.id_organic_unit)
                        OR (grant_table.context_type = 'ORGANIZATION' AND grant_table.context_id = ou.id_organization)
                  )
                """;
        return existsByEntity(sql, organicUnitId, adminUserId, permissionCode);
    }

    private boolean hasOrganicUnitDescendantGrant(long adminUserId, String permissionCode, long organicUnitId)
            throws SQLException {
        String sql = """
                WITH RECURSIVE target_unit AS (
                    SELECT id_organic_unit, id_organization, parent_organic_unit_id
                    FROM organic_unit
                    WHERE id_organic_unit = ?
                ),
                ancestors AS (
                    SELECT parent_organic_unit_id AS id_organic_unit
                    FROM target_unit
                    WHERE parent_organic_unit_id IS NOT NULL
                    UNION ALL
                    SELECT ou.parent_organic_unit_id
                    FROM organic_unit ou
                    JOIN ancestors a ON a.id_organic_unit = ou.id_organic_unit
                    WHERE ou.parent_organic_unit_id IS NOT NULL
                )
                SELECT COUNT(*)
                FROM grant_administrator grant_table
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                JOIN target_unit tu
                WHERE grant_table.id_admin_user = ?
                  AND grant_table.cod_permission = ?
                  AND p.state = 'active'
                  AND (
                        (grant_table.context_type = 'ORGANIZATION' AND grant_table.context_id = tu.id_organization)
                        OR (grant_table.context_type = 'ORGANIC_UNIT'
                            AND grant_table.context_id IN (SELECT id_organic_unit FROM ancestors))
                  )
                """;
        return existsByEntity(sql, organicUnitId, adminUserId, permissionCode);
    }

    private boolean hasCourseContextGrant(long adminUserId, String permissionCode, long courseId) throws SQLException {
        String sql = """
                WITH RECURSIVE target_course AS (
                    SELECT id_course, id_organization, id_organic_unit
                    FROM course
                    WHERE id_course = ?
                ),
                unit_ancestors AS (
                    SELECT id_organic_unit
                    FROM target_course
                    WHERE id_organic_unit IS NOT NULL
                    UNION ALL
                    SELECT ou.parent_organic_unit_id
                    FROM organic_unit ou
                    JOIN unit_ancestors a ON a.id_organic_unit = ou.id_organic_unit
                    WHERE ou.parent_organic_unit_id IS NOT NULL
                )
                SELECT COUNT(*)
                FROM grant_administrator grant_table
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                JOIN target_course c
                WHERE grant_table.id_admin_user = ?
                  AND grant_table.cod_permission = ?
                  AND p.state = 'active'
                  AND (
                        (grant_table.context_type = 'COURSE' AND grant_table.context_id = c.id_course)
                        OR (grant_table.context_type = 'SUBJECT'
                            AND grant_table.context_id IN (
                                SELECT isub.id_subject
                                FROM integrate_subject isub
                                WHERE isub.id_course = c.id_course
                            ))
                        OR (grant_table.context_type = 'ORGANIC_UNIT'
                            AND grant_table.context_id IN (SELECT id_organic_unit FROM unit_ancestors))
                        OR (grant_table.context_type = 'ORGANIZATION' AND grant_table.context_id = c.id_organization)
                  )
                """;
        return existsByEntity(sql, courseId, adminUserId, permissionCode);
    }

    private boolean hasCourseDescendantGrant(long adminUserId, String permissionCode, long courseId)
            throws SQLException {
        String sql = """
                WITH RECURSIVE target_course AS (
                    SELECT id_course, id_organization, id_organic_unit
                    FROM course
                    WHERE id_course = ?
                ),
                unit_ancestors AS (
                    SELECT id_organic_unit
                    FROM target_course
                    WHERE id_organic_unit IS NOT NULL
                    UNION ALL
                    SELECT ou.parent_organic_unit_id
                    FROM organic_unit ou
                    JOIN unit_ancestors a ON a.id_organic_unit = ou.id_organic_unit
                    WHERE ou.parent_organic_unit_id IS NOT NULL
                )
                SELECT COUNT(*)
                FROM grant_administrator grant_table
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                JOIN target_course c
                WHERE grant_table.id_admin_user = ?
                  AND grant_table.cod_permission = ?
                  AND p.state = 'active'
                  AND (
                        (grant_table.context_type = 'ORGANIZATION' AND grant_table.context_id = c.id_organization)
                        OR (grant_table.context_type = 'ORGANIC_UNIT'
                            AND grant_table.context_id IN (SELECT id_organic_unit FROM unit_ancestors))
                  )
                """;
        return existsByEntity(sql, courseId, adminUserId, permissionCode);
    }

    private boolean hasSubjectContextGrant(long adminUserId, String permissionCode, long subjectId)
            throws SQLException {
        String sql = """
                WITH RECURSIVE subject_courses AS (
                    SELECT c.id_course, c.id_organization, c.id_organic_unit
                    FROM integrate_subject isub
                    JOIN course c ON c.id_course = isub.id_course
                    WHERE isub.id_subject = ?
                ),
                unit_ancestors AS (
                    SELECT id_organic_unit
                    FROM subject_courses
                    WHERE id_organic_unit IS NOT NULL
                    UNION
                    SELECT ou.parent_organic_unit_id
                    FROM organic_unit ou
                    JOIN unit_ancestors a ON a.id_organic_unit = ou.id_organic_unit
                    WHERE ou.parent_organic_unit_id IS NOT NULL
                )
                SELECT COUNT(*)
                FROM grant_administrator grant_table
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                JOIN subject s ON s.id_subject = ?
                WHERE grant_table.id_admin_user = ?
                  AND grant_table.cod_permission = ?
                  AND p.state = 'active'
                  AND (
                        (grant_table.context_type = 'SUBJECT' AND grant_table.context_id = s.id_subject)
                        OR (grant_table.context_type = 'COURSE'
                            AND grant_table.context_id IN (SELECT id_course FROM subject_courses))
                        OR (grant_table.context_type = 'ORGANIC_UNIT'
                            AND grant_table.context_id IN (SELECT id_organic_unit FROM unit_ancestors))
                        OR (grant_table.context_type = 'ORGANIZATION' AND grant_table.context_id = s.id_organization)
                  )
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            statement.setLong(2, subjectId);
            statement.setLong(3, adminUserId);
            statement.setString(4, permissionCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    private boolean hasSubjectDescendantGrant(long adminUserId, String permissionCode, long subjectId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM grant_administrator grant_table
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                JOIN subject s ON s.id_subject = ?
                WHERE grant_table.id_admin_user = ?
                  AND grant_table.cod_permission = ?
                  AND p.state = 'active'
                  AND grant_table.context_type = 'ORGANIZATION'
                  AND grant_table.context_id = s.id_organization
                """;
        return existsByEntity(sql, subjectId, adminUserId, permissionCode);
    }

    private boolean hasClassGroupContextGrant(long adminUserId, String permissionCode, long classGroupId)
            throws SQLException {
        String sql = """
                WITH RECURSIVE target_class_group AS (
                    SELECT cg.id_class_group, cg.id_course, cg.id_subject,
                           c.id_organization AS course_organization_id,
                           c.id_organic_unit
                    FROM class_group cg
                    JOIN course c ON c.id_course = cg.id_course
                    WHERE cg.id_class_group = ?
                ),
                unit_ancestors AS (
                    SELECT id_organic_unit
                    FROM target_class_group
                    WHERE id_organic_unit IS NOT NULL
                    UNION ALL
                    SELECT ou.parent_organic_unit_id
                    FROM organic_unit ou
                    JOIN unit_ancestors a ON a.id_organic_unit = ou.id_organic_unit
                    WHERE ou.parent_organic_unit_id IS NOT NULL
                )
                SELECT COUNT(*)
                FROM grant_administrator grant_table
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                JOIN target_class_group cg
                JOIN subject s ON s.id_subject = cg.id_subject
                WHERE grant_table.id_admin_user = ?
                  AND grant_table.cod_permission = ?
                  AND p.state = 'active'
                  AND (
                        (grant_table.context_type = 'CLASS_GROUP' AND grant_table.context_id = cg.id_class_group)
                        OR (grant_table.context_type = 'COURSE' AND grant_table.context_id = cg.id_course)
                        OR (grant_table.context_type = 'SUBJECT' AND grant_table.context_id = cg.id_subject)
                        OR (grant_table.context_type = 'ORGANIC_UNIT'
                            AND grant_table.context_id IN (SELECT id_organic_unit FROM unit_ancestors))
                        OR (grant_table.context_type = 'ORGANIZATION'
                            AND grant_table.context_id IN (cg.course_organization_id, s.id_organization))
                  )
                """;
        return existsByEntity(sql, classGroupId, adminUserId, permissionCode);
    }

    private boolean hasClassGroupDescendantGrant(long adminUserId, String permissionCode, long classGroupId)
            throws SQLException {
        String sql = """
                WITH RECURSIVE target_class_group AS (
                    SELECT cg.id_class_group, cg.id_course, cg.id_subject,
                           c.id_organization AS course_organization_id,
                           c.id_organic_unit
                    FROM class_group cg
                    JOIN course c ON c.id_course = cg.id_course
                    WHERE cg.id_class_group = ?
                ),
                unit_ancestors AS (
                    SELECT id_organic_unit
                    FROM target_class_group
                    WHERE id_organic_unit IS NOT NULL
                    UNION ALL
                    SELECT ou.parent_organic_unit_id
                    FROM organic_unit ou
                    JOIN unit_ancestors a ON a.id_organic_unit = ou.id_organic_unit
                    WHERE ou.parent_organic_unit_id IS NOT NULL
                )
                SELECT COUNT(*)
                FROM grant_administrator grant_table
                JOIN permission p ON p.cod_permission = grant_table.cod_permission
                JOIN target_class_group cg
                JOIN subject s ON s.id_subject = cg.id_subject
                WHERE grant_table.id_admin_user = ?
                  AND grant_table.cod_permission = ?
                  AND p.state = 'active'
                  AND (
                        (grant_table.context_type = 'ORGANIZATION'
                            AND grant_table.context_id IN (cg.course_organization_id, s.id_organization))
                        OR (grant_table.context_type = 'ORGANIC_UNIT'
                            AND grant_table.context_id IN (SELECT id_organic_unit FROM unit_ancestors))
                        OR (grant_table.context_type = 'COURSE' AND grant_table.context_id = cg.id_course)
                        OR (grant_table.context_type = 'SUBJECT' AND grant_table.context_id = cg.id_subject)
                  )
                """;
        return existsByEntity(sql, classGroupId, adminUserId, permissionCode);
    }

    private boolean existsByEntity(String sql, long entityId, long adminUserId, String permissionCode)
            throws SQLException {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, entityId);
            statement.setLong(2, adminUserId);
            statement.setString(3, AuthorizationPolicy.canonicalAdminPermission(permissionCode));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private static Set<String> canonicalAdminCodes(Collection<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return Set.of();
        }
        Set<String> canonicalCodes = new LinkedHashSet<>();
        for (String permissionCode : permissionCodes) {
            if (permissionCode != null && !permissionCode.isBlank()) {
                canonicalCodes.add(AuthorizationPolicy.canonicalAdminPermission(permissionCode));
            }
        }
        return Set.copyOf(canonicalCodes);
    }

    private static Permission mapPermission(ResultSet resultSet) throws SQLException {
        return new Permission(
                resultSet.getString("cod_permission"),
                resultSet.getString("name"),
                PermissionState.fromDatabaseValue(resultSet.getString("state"))
        );
    }

    private record GrantTable(String tableName, String userIdColumn, String profileTableName) {

        private static GrantTable forProfile(AccessProfileType profileType) {
            return switch (profileType) {
                case ADMINISTRATOR -> new GrantTable("grant_administrator", "id_admin_user", "administrator_profile");
                case COORDINATOR -> new GrantTable("grant_coordinator", "id_coordinator_user", "coordinator_profile");
                case TEACHER -> new GrantTable("grant_teacher", "id_teacher_user", "teacher_profile");
                case STUDENT -> new GrantTable("grant_student", "id_student_user", "student_profile");
            };
        }
    }
}
