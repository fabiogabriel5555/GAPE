package pt.isel.gape.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.structure.model.OrganizationCreateCommand;
import pt.isel.gape.structure.model.OrganizationState;
import pt.isel.gape.structure.model.OrganizationType;
import pt.isel.gape.structure.model.OrganizationUpdateCommand;
import pt.isel.gape.structure.service.OrganizationService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class OrganizationServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private OrganizationService organizationService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        organizationService = new OrganizationService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void administratorWithPermissionCanCreateActiveOrganizationWithAdministrator() throws Exception {
        Organization organization = organizationService.createOrganization(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new OrganizationCreateCommand(
                        "Academia Nova",
                        "AN",
                        "organizations/new/profile.webp",
                        OrganizationType.EDUCATIONAL_INSTITUTION,
                        OrganizationState.ACTIVE,
                        Set.of(1L)
                ),
                "127.0.0.1"
        );

        assertTrue(organization.id() > 0);
        assertEquals(OrganizationState.ACTIVE, organization.state());
        assertEquals("organizations/new/profile.webp", organization.photo());
        assertTrue(hasActiveAssignment(1L, organization.id()));
        assertEquals(1, countAudit("ORGANIZATION_CREATE", "success"));
    }

    @Test
    void activeOrganizationRequiresAtLeastOneAdministrator() {
        assertThrows(
                IllegalArgumentException.class,
                () -> organizationService.createOrganization(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new OrganizationCreateCommand(
                                "Academy Without Admin",
                                "ASA",
                                null,
                                OrganizationType.COMPANY,
                                OrganizationState.ACTIVE,
                                Set.of()
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void organizationRequiresName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> organizationService.createOrganization(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new OrganizationCreateCommand(
                                " ",
                                "INV",
                                null,
                                OrganizationType.OTHER,
                                OrganizationState.INACTIVE,
                                Set.of()
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void manageAllAdministratorCannotActivateOrganizationWithoutActiveAdministrator() {
        Organization organization = organizationService.createOrganization(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new OrganizationCreateCommand(
                        "Organization Without Active Admin",
                        "OSAA",
                        null,
                        OrganizationType.TRAINING_COMPANY,
                        OrganizationState.INACTIVE,
                        Set.of()
                ),
                "127.0.0.1"
        );

        assertThrows(
                IllegalStateException.class,
                () -> organizationService.updateOrganization(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        organization.id(),
                        new OrganizationUpdateCommand(
                                organization.name(),
                                organization.acronym(),
                                null,
                                OrganizationType.TRAINING_COMPANY,
                                OrganizationState.ACTIVE
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void administratorWithManageUsersCanBootstrapAdministratorAssignment() {
        organizationService.assignAdministrator(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                11L,
                1L,
                LocalDate.of(2026, 2, 1),
                null,
                "127.0.0.1"
        );

        assertTrue(organizationService.updateOrganization(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                11L,
                new OrganizationUpdateCommand(
                        "Organization Externa Updated",
                        "ORGX",
                        null,
                        OrganizationType.TRAINING_COMPANY,
                        OrganizationState.ACTIVE
                ),
                "127.0.0.1"
        ).name().contains("Updated"));
    }

    @Test
    void inactiveOrganizationBlocksFurtherOperations() {
        organizationService.assignAdministrator(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                11L,
                1L,
                LocalDate.of(2026, 2, 1),
                null,
                "127.0.0.1"
        );
        organizationService.archiveOrganization(1L, null, AccessProfileType.ADMINISTRATOR, 11L, "127.0.0.1");

        assertThrows(
                IllegalStateException.class,
                () -> organizationService.updateOrganization(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        11L,
                        new OrganizationUpdateCommand(
                                "Arquivada",
                                "ARQ",
                                null,
                                OrganizationType.OTHER,
                                OrganizationState.INACTIVE
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void organizationWithDependenciesCannotBeDeleted() {
        assertThrows(
                IllegalStateException.class,
                () -> organizationService.deleteOrganization(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        10L,
                        "127.0.0.1"
                )
        );
    }

    private boolean hasActiveAssignment(long adminUserId, long organizationId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM manage_organization
                     WHERE id_admin_user = ?
                       AND id_organization = ?
                       AND state = 'active'
                     """)) {
            statement.setLong(1, adminUserId);
            statement.setLong(2, organizationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
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
