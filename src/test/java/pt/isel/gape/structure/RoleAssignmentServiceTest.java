package pt.isel.gape.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.structure.service.RoleAssignmentService;
import pt.isel.gape.transversal.DatabaseTestSupport;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

class RoleAssignmentServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private RoleAssignmentService roleAssignmentService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;

        roleAssignmentService = new RoleAssignmentService(
                new PermissionDAO(connectionProvider),
                new ManageOrganizationDAO(connectionProvider),
                new CoordinateSubjectDAO(connectionProvider),
                new TeachClassGroupDAO(connectionProvider),
                new AuditService(new ActivityLogDAO(connectionProvider), FIXED_CLOCK)
        );
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void administratorWithManageUsersCanBeAssignedToActiveOrganization() {
        roleAssignmentService.assignAdministratorToOrganization(
                1L,
                null,
                1L,
                11L,
                LocalDate.of(2026, 2, 1),
                null,
                "127.0.0.1"
        );

        assertTrue(roleAssignmentService.administratorManagesOrganization(1L, 11L));
    }

    @Test
    void administratorWithManageUsersCanAssignCoordinatorToSubject() {
        roleAssignmentService.assignCoordinatorToSubject(
                1L,
                null,
                2L,
                41L,
                LocalDate.of(2026, 2, 1),
                null,
                "127.0.0.1"
        );

        assertTrue(roleAssignmentService.coordinatorCoordinatesSubject(2L, 41L));
    }

    @Test
    void activeTeacherCanBeAssignedToActiveClassGroup() {
        roleAssignmentService.assignTeacherToClassGroup(
                1L,
                null,
                3L,
                52L,
                LocalDate.of(2026, 2, 1),
                null,
                "127.0.0.1"
        );

        assertTrue(roleAssignmentService.teacherTeachesClassGroup(3L, 52L));
    }

    @Test
    void nonAdministratorCannotAssignContextualRolesAndFailureIsAudited() throws Exception {
        assertThrows(
                SecurityException.class,
                () -> roleAssignmentService.assignTeacherToClassGroup(
                        2L,
                        null,
                        3L,
                        52L,
                        LocalDate.of(2026, 2, 1),
                        null,
                        "127.0.0.1"
                )
        );

        assertEquals(1, countAudit("ROLE_ASSIGN_TEACH_CLASS_GROUP", "failure"));
    }

    @Test
    void inactiveUserCannotReceiveActiveContextAssignment() throws Exception {
        insertTeacherProfileForInactiveUser();

        assertThrows(
                IllegalArgumentException.class,
                () -> roleAssignmentService.assignTeacherToClassGroup(
                        1L,
                        null,
                        5L,
                        50L,
                        LocalDate.of(2026, 2, 1),
                        null,
                        "127.0.0.1"
                )
        );
    }

    @Test
    void expiredAssignmentDoesNotAuthorizeContext() {
        roleAssignmentService.assignCoordinatorToSubject(
                1L,
                null,
                2L,
                41L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                "127.0.0.1"
        );

        assertFalse(roleAssignmentService.coordinatorCoordinatesSubject(2L, 41L));
    }

    private void insertTeacherProfileForInactiveUser() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO teacher_profile (id_user, cod_teacher) VALUES (5, 'TCH-INACTIVE')"
             )) {
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
