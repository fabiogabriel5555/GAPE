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
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.OrganicUnitCreateCommand;
import pt.isel.gape.structure.model.OrganicUnitState;
import pt.isel.gape.structure.model.OrganicUnitType;
import pt.isel.gape.structure.model.OrganicUnitUpdateCommand;
import pt.isel.gape.structure.service.OrganicUnitService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class OrganicUnitServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private OrganicUnitService organicUnitService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        organicUnitService = new OrganicUnitService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void administratorCanCreateOrganicUnitInManagedOrganization() {
        OrganicUnit unit = organicUnitService.createOrganicUnit(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new OrganicUnitCreateCommand(
                        10L,
                        "LAB",
                        "Laboratory of Projects",
                        "LAB",
                        OrganicUnitType.SECTION,
                        OrganicUnitState.ACTIVE,
                        20L
                ),
                "127.0.0.1"
        );

        assertTrue(unit.id() > 0);
        assertEquals("SEC-001", unit.code());
        assertEquals(20L, unit.parentOrganicUnitId());
    }

    @Test
    void generatedCodeUsesOrganicUnitTypePrefix() {
        OrganicUnit unit = organicUnitService.createOrganicUnit(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new OrganicUnitCreateCommand(
                        10L,
                        null,
                        "Faculdade de Ciencias",
                        "FC",
                        OrganicUnitType.FACULTY,
                        OrganicUnitState.ACTIVE,
                        null
                ),
                "127.0.0.1"
        );

        assertEquals("FAC-001", unit.code());
    }

    @Test
    void organicUnitRequiresOrganization() {
        assertThrows(
                IllegalArgumentException.class,
                () -> organicUnitService.createOrganicUnit(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new OrganicUnitCreateCommand(
                                0L,
                                "INV",
                                "Invalid",
                                "INV",
                                OrganicUnitType.OTHER,
                                OrganicUnitState.ACTIVE,
                                null
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void parentMustBelongToSameOrganization() {
        assertThrows(
                IllegalArgumentException.class,
                () -> organicUnitService.createOrganicUnit(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new OrganicUnitCreateCommand(
                                10L,
                                "MIX",
                                "Unidade Mista",
                                "MIX",
                                OrganicUnitType.SECTION,
                                OrganicUnitState.ACTIVE,
                                21L
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void selfSubordinationIsBlocked() {
        assertThrows(
                IllegalArgumentException.class,
                () -> organicUnitService.updateOrganicUnit(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        22L,
                        new OrganicUnitUpdateCommand(
                                "NPRJ",
                                "Nucleo de Projeto",
                                "NPRJ",
                                OrganicUnitType.SECTION,
                                OrganicUnitState.ACTIVE,
                                22L
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void hierarchyCyclesAreBlockedByWalkingParentChain() {
        OrganicUnit child = organicUnitService.createOrganicUnit(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new OrganicUnitCreateCommand(
                        10L,
                        "SUB",
                        "Subunidade",
                        "SUB",
                        OrganicUnitType.SECTION,
                        OrganicUnitState.ACTIVE,
                        22L
                ),
                "127.0.0.1"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> organicUnitService.updateOrganicUnit(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        20L,
                        new OrganicUnitUpdateCommand(
                                "DEI",
                                "Departamento de Engenharia Informatica",
                                "DEI",
                                OrganicUnitType.DEPARTMENT,
                                OrganicUnitState.ACTIVE,
                                child.id()
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void nonAdministratorCannotCreateOrganicUnit() {
        assertThrows(
                SecurityException.class,
                () -> organicUnitService.createOrganicUnit(
                        4L,
                        null,
                        AccessProfileType.STUDENT,
                        new OrganicUnitCreateCommand(
                                10L,
                                "STU",
                                "Unidade Student",
                                "STU",
                                OrganicUnitType.OTHER,
                                OrganicUnitState.ACTIVE,
                                null
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void inactiveOrganicUnitBlocksFurtherOperations() {
        organicUnitService.archiveOrganicUnit(1L, null, AccessProfileType.ADMINISTRATOR, 22L, "127.0.0.1");

        assertThrows(
                IllegalStateException.class,
                () -> organicUnitService.updateOrganicUnit(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        22L,
                        new OrganicUnitUpdateCommand(
                                "NPRJ",
                                "Nucleo de Projeto",
                                "NPRJ",
                                OrganicUnitType.SECTION,
                                OrganicUnitState.ACTIVE,
                                20L
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void organicUnitWithDependenciesCannotBeDeleted() {
        assertThrows(
                IllegalStateException.class,
                () -> organicUnitService.deleteOrganicUnit(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        20L,
                        "127.0.0.1"
                )
        );
    }

    @Test
    void assignOrganicUnitAdministratorCreatesDirectUnitGrantForEligibleAdministrator() throws Exception {
        addAdministrator(100L, "ADM-UNIT-100", "MANAGE_ORGANIZATION_STRUCTURE", "ORGANIZATION", 10L);

        organicUnitService.assignOrganicUnitAdministrator(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                20L,
                100L,
                "127.0.0.1"
        );

        assertTrue(organicUnitService.listDirectOrganicUnitAdministratorIds(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                20L,
                "127.0.0.1"
        ).contains(100L));
        assertTrue(hasAdministratorGrant(100L, "MANAGE_ORGANIZATION_STRUCTURE", "ORGANIC_UNIT", 20L));
    }

    @Test
    void assignOrganicUnitAdministratorRejectsNonAdministratorUsers() {
        assertThrows(
                IllegalArgumentException.class,
                () -> organicUnitService.assignOrganicUnitAdministrator(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        20L,
                        4L,
                        "127.0.0.1"
                )
        );
    }

    @Test
    void assignOrganicUnitAdministratorRejectsAdministratorWithDifferentPermissionScope() throws Exception {
        addAdministrator(101L, "ADM-UNIT-101", "MANAGE_LEARNING", "COURSE", 30L);

        assertThrows(
                IllegalArgumentException.class,
                () -> organicUnitService.assignOrganicUnitAdministrator(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        20L,
                        101L,
                        "127.0.0.1"
                )
        );
    }

    @Test
    void revokeOrganicUnitAdministratorKeepsAtLeastOneAdministratorAssignment() throws Exception {
        addAdministrator(102L, "ADM-UNIT-102", "MANAGE_ORGANIZATION_STRUCTURE", "ORGANIC_UNIT", 20L);

        assertThrows(
                IllegalStateException.class,
                () -> organicUnitService.revokeOrganicUnitAdministrator(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        20L,
                        102L,
                        "127.0.0.1"
                )
        );
        assertTrue(hasAdministratorGrant(102L, "MANAGE_ORGANIZATION_STRUCTURE", "ORGANIC_UNIT", 20L));
    }

    @Test
    void revokeOrganicUnitAdministratorRemovesOnlyDirectUnitGrant() throws Exception {
        addAdministrator(103L, "ADM-UNIT-103", "MANAGE_ORGANIZATION_STRUCTURE", "ORGANIZATION", 10L);
        addAdministratorGrant(103L, "MANAGE_ORGANIZATION_STRUCTURE", "ORGANIC_UNIT", 20L);

        organicUnitService.revokeOrganicUnitAdministrator(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                20L,
                103L,
                "127.0.0.1"
        );

        assertTrue(hasAdministratorGrant(103L, "MANAGE_ORGANIZATION_STRUCTURE", "ORGANIZATION", 10L));
        assertFalse(hasAdministratorGrant(103L, "MANAGE_ORGANIZATION_STRUCTURE", "ORGANIC_UNIT", 20L));
    }

    @Test
    void organicUnitScopedAdministratorListsOwnUnitAndDescendants() throws Exception {
        addAdministrator(104L, "ADM-UNIT-104", "MANAGE_ORGANIZATION_STRUCTURE", "ORGANIC_UNIT", 20L);

        java.util.Set<Long> unitIds = organicUnitService.listOrganicUnits(
                        104L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        10L,
                        "127.0.0.1"
                )
                .stream()
                .map(OrganicUnit::id)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(java.util.Set.of(20L, 22L), unitIds);
    }

    private void addAdministrator(
            long userId,
            String administratorCode,
            String permissionCode,
            String contextType,
            long contextId
    ) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement user = connection.prepareStatement("""
                    INSERT INTO user_account (
                        id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt
                    ) VALUES (?, ?, ?, 'active', 'pt-PT', NULL, '2026-01-01 10:00:00', 'hash', 'salt')
                    """)) {
                user.setLong(1, userId);
                user.setString(2, "Unit Admin " + userId);
                user.setString(3, "unit.admin." + userId + "@gape.local");
                user.executeUpdate();
            }
            try (PreparedStatement profile = connection.prepareStatement("""
                    INSERT INTO administrator_profile (id_user, cod_administrator)
                    VALUES (?, ?)
                    """)) {
                profile.setLong(1, userId);
                profile.setString(2, administratorCode);
                profile.executeUpdate();
            }
        }
        addAdministratorGrant(userId, permissionCode, contextType, contextId);
    }

    private void addAdministratorGrant(
            long userId,
            String permissionCode,
            String contextType,
            long contextId
    ) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement grant = connection.prepareStatement("""
                     INSERT INTO grant_administrator (id_admin_user, cod_permission, context_type, context_id)
                     VALUES (?, ?, ?, ?)
                     """)) {
            grant.setLong(1, userId);
            grant.setString(2, permissionCode);
            grant.setString(3, contextType);
            grant.setLong(4, contextId);
            grant.executeUpdate();
        }
    }

    private boolean hasAdministratorGrant(
            long userId,
            String permissionCode,
            String contextType,
            long contextId
    ) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM grant_administrator
                     WHERE id_admin_user = ?
                       AND cod_permission = ?
                       AND context_type = ?
                       AND context_id = ?
                     """)) {
            statement.setLong(1, userId);
            statement.setString(2, permissionCode);
            statement.setString(3, contextType);
            statement.setLong(4, contextId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }
}
