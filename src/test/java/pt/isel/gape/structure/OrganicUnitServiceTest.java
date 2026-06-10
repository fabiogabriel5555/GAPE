package pt.isel.gape.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

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

    @BeforeEach
    void setUp() throws Exception {
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            DatabaseTestSupport.resetDatabase(connection);
            DatabaseTestSupport.executeScript(connection, DatabaseTestSupport.SQL_SEED_DIR.resolve("base.sql"));
        }
        organicUnitService = new OrganicUnitService(connectionProvider, FIXED_CLOCK);
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
                        "Laboratorio de Projetos",
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
                                "Invalida",
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
                                "Unidade Estudante",
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
    void archivedOrganicUnitBlocksFurtherOperations() {
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
}
