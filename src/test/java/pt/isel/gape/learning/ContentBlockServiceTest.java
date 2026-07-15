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
    void assignedTeacherCanCreateInactiveContentBlock() {
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
                        ContentBlockState.INACTIVE
                ),
                "127.0.0.1"
        );

        assertEquals(ContentBlockState.INACTIVE, block.state());
        assertEquals(2, block.orderNo());
    }

    @Test
    void contentBlockOrderMustBeUniqueInClassGroup() {
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
                                ContentBlockState.ACTIVE
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
                                ContentBlockState.ACTIVE
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
                        "First block updated",
                        1,
                        ContentBlockState.INACTIVE
                ),
                "127.0.0.1"
        );

        assertEquals("Introducao atualizada", updated.name());
        assertEquals(ContentBlockState.INACTIVE, updated.state());
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
                                "First block",
                                1,
                                ContentBlockState.ACTIVE
                        ),
                        "127.0.0.1"
                )
        );
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
                        "Block without dependencies",
                        null,
                        orderNo,
                        ContentBlockState.ACTIVE
                ),
                "127.0.0.1"
        );
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
