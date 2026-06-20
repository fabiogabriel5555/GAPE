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
import pt.isel.gape.learning.model.BlockContentItem;
import pt.isel.gape.learning.model.ContentAssociationCommand;
import pt.isel.gape.learning.model.ContentAssociationType;
import pt.isel.gape.learning.model.ContentFormat;
import pt.isel.gape.learning.model.ContentItem;
import pt.isel.gape.learning.model.ContentItemCreateCommand;
import pt.isel.gape.learning.model.ContentItemState;
import pt.isel.gape.learning.service.ContentAssociationService;
import pt.isel.gape.learning.service.ContentItemService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class ContentAssociationServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);
    private static final String SOURCE_IP = "127.0.0.1";

    private ContentItemService contentItemService;
    private ContentAssociationService contentAssociationService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        contentItemService = new ContentItemService(connectionProvider, FIXED_CLOCK);
        contentAssociationService = new ContentAssociationService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void assignedTeacherCanAssociateContentToActiveBlock() throws Exception {
        ContentItem contentItem = createTextContent("Conteudo para bloco");

        contentAssociationService.associateContent(
                3L,
                null,
                AccessProfileType.TEACHER,
                contentItem.id(),
                new ContentAssociationCommand(
                        ContentAssociationType.CONTENT_BLOCK,
                        60L,
                        "main",
                        2,
                        true
                ),
                SOURCE_IP
        );

        assertTrue(blockAssociationExists(60L, contentItem.id()));
    }

    @Test
    void administratorListsBlockContentsByOldestInsertionDateWithAssociationMetadata() throws Exception {
        ContentItem contentItem = createTextContent("Conteudo textual mais recente");

        contentAssociationService.associateContent(
                3L,
                null,
                AccessProfileType.TEACHER,
                contentItem.id(),
                new ContentAssociationCommand(
                        ContentAssociationType.CONTENT_BLOCK,
                        60L,
                        "support",
                        1,
                        false
                ),
                SOURCE_IP
        );

        List<BlockContentItem> items = contentAssociationService.listBlockContentItems(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                60L,
                SOURCE_IP
        );

        assertTrue(items.size() >= 2);
        assertEquals(contentItem.id(), items.get(items.size() - 1).contentItem().id());
        BlockContentItem createdItem = items.stream()
                .filter(item -> item.contentItem().id() == contentItem.id())
                .findFirst()
                .orElseThrow();
        assertEquals("support", createdItem.role());
        assertEquals(1, createdItem.orderNo());
        assertFalse(createdItem.mandatory());
    }

    @Test
    void blockContentsReuseThumbnailFromSharedStoredFile() throws Exception {
        String sharedSource = "contents/video/3/120.mp4";
        String thumbnailPath = "contents/video/3/120-thumb.webp";
        ContentItem originalItem = createContent("Video original", ContentFormat.VIDEO, sharedSource);
        ContentItem reusedItem = createContent("Video reutilizado", ContentFormat.VIDEO, sharedSource);
        recordContentFile(originalItem.id(), "video.mkv", sharedSource, thumbnailPath);

        contentAssociationService.associateContent(
                3L,
                null,
                AccessProfileType.TEACHER,
                reusedItem.id(),
                new ContentAssociationCommand(
                        ContentAssociationType.CONTENT_BLOCK,
                        60L,
                        "support",
                        2,
                        false
                ),
                SOURCE_IP
        );

        List<BlockContentItem> items = contentAssociationService.listBlockContentItems(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                60L,
                SOURCE_IP
        );

        BlockContentItem loadedItem = items.stream()
                .filter(item -> item.contentItem().id() == reusedItem.id())
                .findFirst()
                .orElseThrow();
        assertEquals(thumbnailPath, loadedItem.thumbnailPath());
    }

    @Test
    void associationViolatingStructuralChainIsRejected() throws Exception {
        ContentItem contentItem = createTextContent("Conteudo com cadeia invalida");

        contentAssociationService.associateContent(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                contentItem.id(),
                new ContentAssociationCommand(
                        ContentAssociationType.COURSE,
                        31L,
                        "support",
                        null,
                        false
                ),
                SOURCE_IP
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> contentAssociationService.associateContent(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        contentItem.id(),
                        new ContentAssociationCommand(
                                ContentAssociationType.SUBJECT,
                                40L,
                                "support",
                                null,
                                false
                        ),
                        SOURCE_IP
                )
        );

        assertTrue(courseAssociationExists(31L, contentItem.id()));
        assertFalse(subjectAssociationExists(40L, contentItem.id()));
    }

    @Test
    void operationWithoutPermissionIsRejected() throws Exception {
        ContentItem contentItem = createTextContent("Conteudo sem permissao");

        assertThrows(
                SecurityException.class,
                () -> contentAssociationService.associateContent(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        contentItem.id(),
                        new ContentAssociationCommand(
                                ContentAssociationType.CLASS_GROUP,
                                52L,
                                "support",
                                null,
                                false
                        ),
                        SOURCE_IP
                )
        );

        assertFalse(classGroupAssociationExists(52L, contentItem.id()));
    }

    private ContentItem createTextContent(String title) {
        return createContent(title, ContentFormat.TEXT, "contents/texto-apoio-associacao.txt");
    }

    private ContentItem createContent(String title, ContentFormat format, String source) {
        return contentItemService.createContentItem(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ContentItemCreateCommand(
                        title,
                        "Conteudo criado pelos testes de associacao",
                        format,
                        source,
                        ContentItemState.ACTIVE
                ),
                SOURCE_IP
        );
    }

    private static void recordContentFile(
            long contentItemId,
            String originalFileName,
            String finalPath,
            String thumbnailPath
    ) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO content_file (
                         id_content_item, original_filename, original_mime_type, final_mime_type,
                         original_bytes, final_bytes, sha256, original_path, final_path,
                         thumbnail_path, processing_state, processing_error, created_at, processed_at
                     ) VALUES (?, ?, 'video/x-matroska', 'video/mp4', 10, 8, NULL, NULL, ?, ?, 'ready', NULL,
                               '2026-06-04 10:15:30', '2026-06-04 10:15:30')
                     """)) {
            statement.setLong(1, contentItemId);
            statement.setString(2, originalFileName);
            statement.setString(3, finalPath);
            statement.setString(4, thumbnailPath);
            statement.executeUpdate();
        }
    }

    private static boolean blockAssociationExists(long contentBlockId, long contentItemId) throws Exception {
        return associationExists(
                "associate_block_content",
                "id_content_block",
                contentBlockId,
                contentItemId
        );
    }

    private static boolean courseAssociationExists(long courseId, long contentItemId) throws Exception {
        return associationExists(
                "associate_course_content",
                "id_course",
                courseId,
                contentItemId
        );
    }

    private static boolean subjectAssociationExists(long subjectId, long contentItemId) throws Exception {
        return associationExists(
                "associate_subject_content",
                "id_subject",
                subjectId,
                contentItemId
        );
    }

    private static boolean classGroupAssociationExists(long classGroupId, long contentItemId) throws Exception {
        return associationExists(
                "associate_class_group_content",
                "id_class_group",
                classGroupId,
                contentItemId
        );
    }

    private static boolean associationExists(
            String tableName,
            String targetColumn,
            long targetId,
            long contentItemId
    ) throws Exception {
        String sql = """
                SELECT COUNT(*)
                FROM %s
                WHERE %s = ?
                  AND id_content_item = ?
                """.formatted(tableName, targetColumn);
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, targetId);
            statement.setLong(2, contentItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }
}
