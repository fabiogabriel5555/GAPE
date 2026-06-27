package pt.isel.gape.learning;

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
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.PhysicalRoom;
import pt.isel.gape.learning.model.PhysicalRoomCreateCommand;
import pt.isel.gape.learning.model.PhysicalRoomState;
import pt.isel.gape.learning.model.PhysicalRoomUpdateCommand;
import pt.isel.gape.learning.service.PhysicalRoomService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class PhysicalRoomServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-20T10:15:30Z"), ZoneOffset.UTC);

    private PhysicalRoomService physicalRoomService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        physicalRoomService = new PhysicalRoomService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void administratorCanCreateValidPhysicalRoom() {
        PhysicalRoom room = physicalRoomService.createPhysicalRoom(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                validRoom("SALA-TST-1"),
                "127.0.0.1"
        );

        assertEquals("SALA-TST-1", room.code());
        assertEquals(30, room.capacity());
        assertEquals(PhysicalRoomState.ACTIVE, room.state());
    }

    @Test
    void coordinatorCanManageRoomsInCoordinatedOrganization() throws Exception {
        List<PhysicalRoom> rooms = physicalRoomService.listPhysicalRoomsByOrganization(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                10L,
                "127.0.0.1"
        );

        assertTrue(rooms.stream().anyMatch(room -> "SALA-A1".equals(room.code())));
        assertTrue(physicalRoomService.canManagePhysicalRoomsByOrganization(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                10L,
                "127.0.0.1"
        ));

        PhysicalRoom created = physicalRoomService.createPhysicalRoom(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                validRoom("SALA-COORD-1"),
                "127.0.0.1"
        );
        assertEquals("SALA-COORD-1", created.code());

        PhysicalRoom updated = physicalRoomService.updatePhysicalRoom(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                "SALA-COORD-1",
                new PhysicalRoomUpdateCommand(
                        10L,
                        20L,
                        "Coordinator Room",
                        "Updated by coordinator",
                        31,
                        "Edificio C",
                        PhysicalRoomState.ACTIVE
                ),
                "127.0.0.1"
        );
        assertEquals("Coordinator Room", updated.name());

        physicalRoomService.deletePhysicalRoom(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                "SALA-COORD-1",
                "127.0.0.1"
        );
        assertFalse(roomExists("SALA-COORD-1"));
    }

    @Test
    void teacherCannotManagePhysicalRooms() {
        assertThrows(
                SecurityException.class,
                () -> physicalRoomService.createPhysicalRoom(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        validRoom("SALA-NO-TEACHER"),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void teacherCanReadRoomsInTaughtOrganizationOnly() {
        List<PhysicalRoom> rooms = physicalRoomService.listPhysicalRoomsByOrganization(
                3L,
                null,
                AccessProfileType.TEACHER,
                10L,
                "127.0.0.1"
        );

        assertTrue(rooms.stream().anyMatch(room -> "SALA-A1".equals(room.code())));
        assertFalse(rooms.stream().anyMatch(room -> room.organizationId() != 10L));
        assertFalse(physicalRoomService.canManagePhysicalRoomsByOrganization(
                3L,
                null,
                AccessProfileType.TEACHER,
                10L,
                "127.0.0.1"
        ));
    }

    @Test
    void teacherCannotReadRoomsOutsideTaughtOrganization() {
        assertThrows(
                SecurityException.class,
                () -> physicalRoomService.listPhysicalRoomsByOrganization(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        11L,
                        "127.0.0.1"
                )
        );
    }

    @Test
    void teacherCanReadButCannotManageRoomInTaughtOrganization() {
        PhysicalRoom room = physicalRoomService.getPhysicalRoom(
                3L,
                null,
                AccessProfileType.TEACHER,
                "SALA-A1",
                "127.0.0.1"
        );

        assertEquals("SALA-A1", room.code());
        assertFalse(physicalRoomService.canManagePhysicalRoom(
                3L,
                null,
                AccessProfileType.TEACHER,
                "SALA-A1",
                "127.0.0.1"
        ));
    }

    @Test
    void teacherCannotReadRoomOutsideTaughtOrganization() {
        assertThrows(
                SecurityException.class,
                () -> physicalRoomService.getPhysicalRoom(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        "SALA-X1",
                        "127.0.0.1"
                )
        );
    }

    @Test
    void physicalRoomCapacityMustBeGreaterThanZero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> physicalRoomService.createPhysicalRoom(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new PhysicalRoomCreateCommand(
                                "SALA-ZERO-SVC",
                                10L,
                                20L,
                                "Sala Zero",
                                null,
                                0,
                                "Edificio A",
                                PhysicalRoomState.ACTIVE
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void physicalRoomOrganicUnitMustBelongToSameOrganization() {
        assertThrows(
                IllegalArgumentException.class,
                () -> physicalRoomService.createPhysicalRoom(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new PhysicalRoomCreateCommand(
                                "SALA-ORG-MISMATCH",
                                10L,
                                21L,
                                "External Room Invalid",
                                null,
                                20,
                                "Edificio X",
                                PhysicalRoomState.ACTIVE
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void physicalRoomCanBeUpdated() {
        physicalRoomService.createPhysicalRoom(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                validRoom("SALA-UPD-1"),
                "127.0.0.1"
        );

        PhysicalRoom updated = physicalRoomService.updatePhysicalRoom(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                "SALA-UPD-1",
                new PhysicalRoomUpdateCommand(
                        10L,
                        20L,
                        "Sala Updated",
                        "Capacidade revista",
                        35,
                        "Edificio B",
                        PhysicalRoomState.UNAVAILABLE
                ),
                "127.0.0.1"
        );

        assertEquals("Sala Updated", updated.name());
        assertEquals(35, updated.capacity());
        assertEquals(PhysicalRoomState.UNAVAILABLE, updated.state());
    }

    @Test
    void physicalRoomWithOnlyPastLessonsCanBeDeactivatedAfterLessonStateSync() {
        PhysicalRoom updated = physicalRoomService.updatePhysicalRoom(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                "SALA-A1",
                new PhysicalRoomUpdateCommand(
                        10L,
                        20L,
                        "Sala A1",
                        "Laboratory principal",
                        25,
                        "Edificio A",
                        PhysicalRoomState.UNAVAILABLE
                ),
                "127.0.0.1"
        );

        assertEquals(PhysicalRoomState.UNAVAILABLE, updated.state());
    }

    @Test
    void physicalRoomWithLessonsCannotBeDeleted() {
        assertThrows(
                IllegalStateException.class,
                () -> physicalRoomService.deletePhysicalRoom(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        "SALA-A1",
                        "127.0.0.1"
                )
        );
    }

    @Test
    void physicalRoomWithoutDependenciesCanBeDeleted() throws Exception {
        physicalRoomService.createPhysicalRoom(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                validRoom("SALA-DEL-1"),
                "127.0.0.1"
        );

        physicalRoomService.deletePhysicalRoom(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                "SALA-DEL-1",
                "127.0.0.1"
        );

        assertFalse(roomExists("SALA-DEL-1"));
    }

    private static PhysicalRoomCreateCommand validRoom(String code) {
        return new PhysicalRoomCreateCommand(
                code,
                10L,
                20L,
                "Sala de Teste",
                "Room created by automatic test",
                30,
                "Edificio A",
                PhysicalRoomState.ACTIVE
        );
    }

    private static boolean roomExists(String code) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM physical_room
                     WHERE cod_physical_room = ?
                     """)) {
            statement.setString(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }
}
