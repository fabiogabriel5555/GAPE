package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
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
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.ContentBlockAccessMode;
import pt.isel.gape.learning.model.ContentBlockCreateCommand;
import pt.isel.gape.learning.model.ContentBlockState;
import pt.isel.gape.learning.model.ContentBlockUpdateCommand;
import pt.isel.gape.learning.service.ContentBlockService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class ContentBlockServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private ContentBlockService contentBlockService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        contentBlockService = new ContentBlockService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void assignedTeacherCanCreateScheduledContentBlock() {
        ContentBlock block = contentBlockService.createContentBlock(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ContentBlockCreateCommand(
                        50L,
                        "BLK-02",
                        "Planeamento",
                        "Planeamento do projeto",
                        2,
                        ContentBlockAccessMode.SCHEDULED,
                        ContentBlockState.ACTIVE,
                        LocalDateTime.of(2026, 3, 1, 0, 0),
                        LocalDateTime.of(2026, 4, 1, 23, 59)
                ),
                "127.0.0.1"
        );

        assertEquals(ContentBlockState.ACTIVE, block.state());
        assertEquals(2, block.orderNo());
    }

    @Test
    void inactiveContentBlockCanReuseOrderOfActiveBlock() {
        ContentBlock block = contentBlockService.createContentBlock(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ContentBlockCreateCommand(
                        50L,
                        "BLK-INACTIVE-ORDER",
                        "Rascunho de ordem",
                        null,
                        1,
                        ContentBlockAccessMode.OPEN,
                        ContentBlockState.INACTIVE,
                        null,
                        null
                ),
                "127.0.0.1"
        );

        assertEquals(ContentBlockState.INACTIVE, block.state());
        assertEquals(1, block.orderNo());
    }

    @Test
    void activeContentBlockOrderMustBeUniqueInClassGroup() {
        assertThrows(
                IllegalStateException.class,
                () -> contentBlockService.createContentBlock(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        new ContentBlockCreateCommand(
                                50L,
                                "BLK-DUP-ORDER",
                                "Ordem duplicada",
                                null,
                                1,
                                ContentBlockAccessMode.OPEN,
                                ContentBlockState.ACTIVE,
                                null,
                                null
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void scheduledContentBlockRequiresAvailabilityStart() {
        assertThrows(
                IllegalArgumentException.class,
                () -> contentBlockService.createContentBlock(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        new ContentBlockCreateCommand(
                                50L,
                                "BLK-NO-START",
                                "Sem inicio",
                                null,
                                2,
                                ContentBlockAccessMode.SCHEDULED,
                                ContentBlockState.ACTIVE,
                                null,
                                null
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void contentBlockAvailabilityEndRequiresStart() {
        assertThrows(
                IllegalArgumentException.class,
                () -> contentBlockService.createContentBlock(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        new ContentBlockCreateCommand(
                                50L,
                                "BLK-END-NO-START",
                                "Fim sem inicio",
                                null,
                                2,
                                ContentBlockAccessMode.OPEN,
                                ContentBlockState.ACTIVE,
                                null,
                                LocalDateTime.of(2026, 4, 1, 23, 59)
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void contentBlockAvailabilityEndCannotBeBeforeStart() {
        assertThrows(
                IllegalArgumentException.class,
                () -> contentBlockService.createContentBlock(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        new ContentBlockCreateCommand(
                                50L,
                                "BLK-BAD-RANGE",
                                "Intervalo invalido",
                                null,
                                2,
                                ContentBlockAccessMode.OPEN,
                                ContentBlockState.ACTIVE,
                                LocalDateTime.of(2026, 4, 1, 23, 59),
                                LocalDateTime.of(2026, 3, 1, 0, 0)
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void duplicateContentBlockCodeInClassGroupIsRejected() {
        assertThrows(
                RuntimeException.class,
                () -> contentBlockService.createContentBlock(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        new ContentBlockCreateCommand(
                                50L,
                                "BLK-01",
                                "Codigo duplicado",
                                null,
                                2,
                                ContentBlockAccessMode.OPEN,
                                ContentBlockState.ACTIVE,
                                null,
                                null
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void assignedTeacherCanUpdateContentBlock() {
        ContentBlock updated = contentBlockService.updateContentBlock(
                3L,
                null,
                AccessProfileType.TEACHER,
                60L,
                new ContentBlockUpdateCommand(
                        50L,
                        "BLK-01",
                        "Introducao atualizada",
                        "Primeiro bloco atualizado",
                        1,
                        ContentBlockAccessMode.OPEN,
                        ContentBlockState.ACTIVE,
                        null,
                        null
                ),
                "127.0.0.1"
        );

        assertEquals("Introducao atualizada", updated.name());
    }

    @Test
    void contentBlockCannotMoveToAnotherClassGroup() {
        assertThrows(
                IllegalArgumentException.class,
                () -> contentBlockService.updateContentBlock(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        60L,
                        new ContentBlockUpdateCommand(
                                52L,
                                "BLK-01",
                                "Introducao atualizada",
                                "Primeiro bloco",
                                1,
                                ContentBlockAccessMode.OPEN,
                                ContentBlockState.ACTIVE,
                                null,
                                null
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void archivedContentBlockCannotBeUpdated() {
        contentBlockService.archiveContentBlock(
                3L,
                null,
                AccessProfileType.TEACHER,
                60L,
                "127.0.0.1"
        );

        assertThrows(
                IllegalStateException.class,
                () -> contentBlockService.updateContentBlock(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        60L,
                        new ContentBlockUpdateCommand(
                                50L,
                                "BLK-01",
                                "Introducao atualizada",
                                "Primeiro bloco",
                                1,
                                ContentBlockAccessMode.OPEN,
                                ContentBlockState.ACTIVE,
                                LocalDateTime.of(2026, 2, 1, 0, 0),
                                LocalDateTime.of(2026, 3, 1, 23, 59)
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void assignedTeacherCanArchiveContentBlock() throws Exception {
        ContentBlock block = createStandaloneTeacherBlock("BLK-ARCH", 2);

        contentBlockService.archiveContentBlock(
                3L,
                null,
                AccessProfileType.TEACHER,
                block.id(),
                "127.0.0.1"
        );

        assertEquals("archived", contentBlockState(block.id()));
    }

    @Test
    void contentBlockWithDependenciesCannotBeDeleted() {
        assertThrows(
                IllegalStateException.class,
                () -> contentBlockService.deleteContentBlock(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        60L,
                        "127.0.0.1"
                )
        );
    }

    @Test
    void assignedTeacherCanDeleteContentBlockWithoutDependencies() throws Exception {
        ContentBlock block = createStandaloneTeacherBlock("BLK-DEL", 2);

        contentBlockService.deleteContentBlock(
                3L,
                null,
                AccessProfileType.TEACHER,
                block.id(),
                "127.0.0.1"
        );

        assertFalse(contentBlockExists(block.id()));
    }

    private ContentBlock createStandaloneTeacherBlock(String code, int orderNo) {
        return contentBlockService.createContentBlock(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ContentBlockCreateCommand(
                        50L,
                        code,
                        "Bloco sem dependencias",
                        null,
                        orderNo,
                        ContentBlockAccessMode.OPEN,
                        ContentBlockState.ACTIVE,
                        null,
                        null
                ),
                "127.0.0.1"
        );
    }

    private static String contentBlockState(long contentBlockId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT state
                     FROM content_block
                     WHERE id_content_block = ?
                     """)) {
            statement.setLong(1, contentBlockId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getString("state");
            }
        }
    }

    private static boolean contentBlockExists(long contentBlockId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM content_block
                     WHERE id_content_block = ?
                     """)) {
            statement.setLong(1, contentBlockId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }
}
