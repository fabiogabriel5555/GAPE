package pt.isel.gape.access;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.service.GrantService;
import pt.isel.gape.access.service.PermissionService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.transversal.DatabaseTestSupport;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

class PermissionServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private ConnectionProvider connectionProvider;
    private PermissionService permissionService;
    private GrantService grantService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        connectionProvider = DatabaseTestSupport::openConnection;

        PermissionDAO permissionDAO = new PermissionDAO(connectionProvider);
        AuditService auditService = new AuditService(new ActivityLogDAO(connectionProvider), FIXED_CLOCK);
        permissionService = new PermissionService(permissionDAO);
        grantService = new GrantService(permissionDAO, auditService);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void activePermissionCanBeRequired() {
        assertEquals("MANAGE_ALL", permissionService.requireActive("MANAGE_ALL").code());
    }

    @Test
    void inactivePermissionCannotBeGrantedAndIsAudited() throws Exception {
        insertPermission("ARCHIVED_PERMISSION", "Archived Permission", "inactive");

        assertThrows(
                IllegalArgumentException.class,
                () -> grantService.grantPermission(
                        1L,
                        null,
                        AccessProfileType.STUDENT,
                        4L,
                        "ARCHIVED_PERMISSION",
                        "127.0.0.1"
                )
        );

        assertEquals(1, countAudit("PERMISSION_GRANT", "failure"));
    }

    @Test
    void globalManagementPermissionCannotBeGrantedToNonAdministrator() {
        assertThrows(
                SecurityException.class,
                () -> grantService.grantPermission(
                        1L,
                        null,
                        AccessProfileType.TEACHER,
                        3L,
                        "MANAGE_ALL",
                        "127.0.0.1"
                )
        );
    }

    @Test
    void administratorWithManagePermissionsCanGrantActivePermission() {
        assertDoesNotThrow(() -> grantService.grantPermission(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                1L,
                "MANAGE_ALL",
                "127.0.0.1"
        ));
    }

    @Test
    void revokeFailureByUnauthorizedActorIsAudited() throws Exception {
        assertThrows(
                SecurityException.class,
                () -> grantService.revokePermission(
                        2L,
                        null,
                        AccessProfileType.STUDENT,
                        4L,
                        "MANAGE_ALL",
                        "127.0.0.1"
                )
        );

        assertEquals(1, countAudit("PERMISSION_REVOKE", "failure"));
    }

    private void insertPermission(String code, String name, String state) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO permission (cod_permission, name, state) VALUES (?, ?, ?)"
             )) {
            statement.setString(1, code);
            statement.setString(2, name);
            statement.setString(3, state);
            statement.executeUpdate();
        }
    }

    private int countAudit(String operationType, String outcome) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM activity_log WHERE operation_type = ? AND outcome = ?"
             )) {
            statement.setString(1, operationType);
            statement.setString(2, outcome);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}
