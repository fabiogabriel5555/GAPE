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
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.ContentAssociationType;
import pt.isel.gape.learning.model.ContentDeletionResult;
import pt.isel.gape.learning.model.ContentFormat;
import pt.isel.gape.learning.model.ContentItem;
import pt.isel.gape.learning.model.ContentItemCreateCommand;
import pt.isel.gape.learning.model.ContentRemovalResult;
import pt.isel.gape.learning.model.ContentItemState;
import pt.isel.gape.learning.model.ReusableContentFile;
import pt.isel.gape.learning.service.ContentItemService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class ContentItemServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);
    private static final String SOURCE_IP = "127.0.0.1";

    private ContentItemService contentItemService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        contentItemService = new ContentItemService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @ParameterizedTest
    @MethodSource("validContentReferences")
    void activeTeacherCanCreateContentItemForSupportedFormats(ContentFormat format, String source) {
        ContentItem contentItem = contentItemService.createContentItem(
                3L,
                null,
                AccessProfileType.TEACHER,
                command("Content " + format.name(), format, source),
                SOURCE_IP
        );

        assertTrue(contentItem.id() > 0);
        assertEquals(3L, contentItem.authorUserId());
        assertEquals(format, contentItem.format());
        assertEquals(source, contentItem.source());
        assertEquals(ContentItemState.ACTIVE, contentItem.state());
    }

    @ParameterizedTest
    @MethodSource("invalidContentReferences")
    void contentCreationRejectsInvalidReferencesForFormat(ContentFormat format, String source) {
        assertThrows(
                IllegalArgumentException.class,
                () -> contentItemService.createContentItem(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        command("Invalid content " + format.name(), format, source),
                        SOURCE_IP
                )
        );
    }

    @Test
    void contentCreationRequiresResponsibleUser() {
        assertThrows(
                IllegalArgumentException.class,
                () -> contentItemService.createContentItem(
                        0L,
                        null,
                        AccessProfileType.TEACHER,
                        command("Without owner", ContentFormat.PDF, "contents/without-owner.pdf"),
                        SOURCE_IP
                )
        );
    }

    @Test
    void repositoryListsOnlyActiveReusableFileBackedContent() {
        contentItemService.createContentItem(
                3L,
                null,
                AccessProfileType.TEACHER,
                command("Link outside repository", ContentFormat.URL, "https://example.com/link"),
                SOURCE_IP
        );

        List<ReusableContentFile> items = contentItemService.listReusableFileBackedContent(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                SOURCE_IP
        );
        Set<Long> ids = items.stream()
                .map(ReusableContentFile::repositoryContentItemId)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(ids.contains(70L));
        assertTrue(ids.contains(71L));
        assertFalse(ids.contains(72L));
        assertTrue(items.stream().allMatch(item -> item.format() == ContentFormat.PDF
                || item.format() == ContentFormat.TEXT
                || item.format() == ContentFormat.IMAGE
                || item.format() == ContentFormat.VIDEO
                || item.format() == ContentFormat.AUDIO
                || item.format() == ContentFormat.ARCHIVE
                || (item.format() == ContentFormat.OTHER && item.source().startsWith("assessment:"))));
        assertTrue(items.stream().anyMatch(item -> "assessment:90".equals(item.source())));
        ReusableContentFile pdf = items.stream()
                .filter(item -> item.repositoryContentItemId() == 70L)
                .findFirst()
                .orElseThrow();
        assertTrue(pdf.defaultMandatory());
    }

    @Test
    void repositoryForNonStudentActorIgnoresOriginalAndTargetContextAuthorization() throws Exception {
        ContentItem otherContextContent = contentItemService.createContentItem(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                command("File from another class group", ContentFormat.PDF, "contents/other-class/private.pdf"),
                SOURCE_IP
        );
        associateContentToBlock62(otherContextContent.id(), false);

        List<ReusableContentFile> items = contentItemService.listReusableFileBackedContent(
                3L,
                null,
                AccessProfileType.TEACHER,
                ContentAssociationType.CLASS_GROUP,
                999_999L,
                SOURCE_IP
        );

        Set<Long> ids = items.stream()
                .map(ReusableContentFile::repositoryContentItemId)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(ids.contains(70L));
        assertTrue(ids.contains(otherContextContent.id()));
    }

    @Test
    void repositoryFileReadIsFreeForNonStudentActor() throws Exception {
        ContentItem otherContextContent = contentItemService.createContentItem(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                command("Private file", ContentFormat.PDF, "contents/other-class/private-target.pdf"),
                SOURCE_IP
        );
        associateContentToBlock62(otherContextContent.id(), false);

        ReusableContentFile reusableFile = contentItemService.getReusableFileBackedContent(
                3L,
                null,
                AccessProfileType.TEACHER,
                otherContextContent.id(),
                ContentAssociationType.CLASS_GROUP,
                999_999L,
                SOURCE_IP
        );

        assertEquals(otherContextContent.id(), reusableFile.repositoryContentItemId());
        assertEquals(otherContextContent.source(), reusableFile.source());
    }

    @Test
    void reusableFileBackedContentPreviewIsFreeForNonStudentActor() throws Exception {
        ContentItem otherContextContent = contentItemService.createContentItem(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                command("File for public preview", ContentFormat.PDF, "contents/other-class/free-preview.pdf"),
                SOURCE_IP
        );
        associateContentToBlock62(otherContextContent.id(), false);

        ContentItem contentItem = contentItemService.getContentItem(
                3L,
                null,
                AccessProfileType.TEACHER,
                otherContextContent.id(),
                SOURCE_IP
        );

        assertEquals(otherContextContent.id(), contentItem.id());
        assertEquals(otherContextContent.source(), contentItem.source());
    }

    @Test
    void studentCannotUseReusableRepository() {
        assertThrows(SecurityException.class, () -> contentItemService.listReusableFileBackedContent(
                4L,
                null,
                AccessProfileType.STUDENT,
                SOURCE_IP
        ));
        assertThrows(SecurityException.class, () -> contentItemService.getReusableFileBackedContent(
                4L,
                null,
                AccessProfileType.STUDENT,
                70L,
                SOURCE_IP
        ));
    }

    @Test
    void studentReusableFileBackedContentPreviewStillRequiresContentAccessContext() throws Exception {
        ContentItem otherContextContent = contentItemService.createContentItem(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                command("File for private student preview", ContentFormat.PDF, "contents/other-class/student-preview.pdf"),
                SOURCE_IP
        );
        associateContentToBlock62(otherContextContent.id(), false);

        assertThrows(SecurityException.class, () -> contentItemService.getContentItem(
                4L,
                null,
                AccessProfileType.STUDENT,
                otherContextContent.id(),
                SOURCE_IP
        ));
    }

    @Test
    void studentCannotDeleteContentItemWithoutManagementPermission() throws Exception {
        assertThrows(
                SecurityException.class,
                () -> contentItemService.deleteContentItem(
                        4L,
                        null,
                        AccessProfileType.STUDENT,
                        70L,
                        SOURCE_IP
                )
        );

        assertTrue(contentItemExists(70L));
    }

    @Test
    void authorWithoutManagementContextCannotDeleteDetachedContentItem() throws Exception {
        ContentItem contentItem = createTextContent("Content by author");

        assertThrows(
                SecurityException.class,
                () -> contentItemService.deleteContentItem(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        contentItem.id(),
                        SOURCE_IP
                )
        );

        assertTrue(contentItemExists(contentItem.id()));
    }

    @Test
    void uploadCleanupCanDiscardActorOwnedDetachedPendingContentItem() throws Exception {
        ContentItem contentItem = createTextContent("Content pending upload");

        contentItemService.discardPendingDetachedContentItem(
                3L,
                null,
                AccessProfileType.TEACHER,
                contentItem.id(),
                SOURCE_IP
        );

        assertFalse(contentItemExists(contentItem.id()));
    }

    @Test
    void uploadCleanupCannotDiscardAssociatedContentItem() throws Exception {
        ContentItem contentItem = createTextContent("Content ja associado");
        associateContentToBlock60(contentItem.id(), false);

        assertThrows(
                SecurityException.class,
                () -> contentItemService.discardPendingDetachedContentItem(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        contentItem.id(),
                        SOURCE_IP
                )
        );

        assertTrue(contentItemExists(contentItem.id()));
        assertTrue(blockAssociationExists(60L, contentItem.id()));
    }

    @Test
    void administratorCanPhysicallyDeleteDetachedContentItem() throws Exception {
        ContentItem contentItem = createTextContent("Content for administrator");

        ContentDeletionResult result = contentItemService.deleteContentItem(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                contentItem.id(),
                SOURCE_IP
        );

        assertEquals(ContentDeletionResult.PHYSICALLY_DELETED, result);
        assertFalse(contentItemExists(contentItem.id()));
    }

    @Test
    void contextManagerCanDeleteContentItemCreatedByAnotherUser() throws Exception {
        ContentItem contentItem = contentItemService.createContentItem(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                command("Content created by admin", ContentFormat.PDF,
                        "contents/items/context-manager/delete-generic-by-manager.pdf"),
                SOURCE_IP
        );
        associateContentToBlock60(contentItem.id(), false);

        ContentDeletionResult result = contentItemService.deleteContentItem(
                3L,
                null,
                AccessProfileType.TEACHER,
                contentItem.id(),
                SOURCE_IP
        );

        assertEquals(ContentDeletionResult.PHYSICALLY_DELETED, result);
        assertFalse(contentItemExists(contentItem.id()));
        assertFalse(blockAssociationExists(60L, contentItem.id()));
    }

    @Test
    void contentInActiveMandatoryBlockIsArchivedInsteadOfPhysicallyDeleted() throws Exception {
        ContentDeletionResult result = contentItemService.deleteContentItem(
                3L,
                null,
                AccessProfileType.TEACHER,
                70L,
                SOURCE_IP
        );

        assertEquals(ContentDeletionResult.INACTIVATED, result);
        assertTrue(contentItemExists(70L));
        assertEquals("inactive", contentItemState(70L));
    }

    @Test
    void contentLinkedToSubmittedAssessmentAttemptIsArchivedInsteadOfPhysicallyDeleted() throws Exception {
        ContentItem contentItem = createTextContent("Content with attempt");
        associateContentToAssessment90(contentItem.id());

        ContentDeletionResult result = contentItemService.deleteContentItem(
                3L,
                null,
                AccessProfileType.TEACHER,
                contentItem.id(),
                SOURCE_IP
        );

        assertEquals(ContentDeletionResult.INACTIVATED, result);
        assertTrue(contentItemExists(contentItem.id()));
        assertEquals("inactive", contentItemState(contentItem.id()));
    }

    @Test
    void blockManagerCanDeletePedagogicalContentCreatedByAnotherUser() throws Exception {
        ContentItem contentItem = contentItemService.createContentItem(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                command("Content created by admin", ContentFormat.PDF,
                        "contents/items/context-manager/delete-by-manager.pdf"),
                SOURCE_IP
        );
        associateContentToBlock60(contentItem.id(), false);

        ContentRemovalResult result = contentItemService.deleteContentItemFromBlock(
                3L,
                null,
                AccessProfileType.TEACHER,
                60L,
                contentItem.id(),
                SOURCE_IP
        );

        assertEquals(ContentDeletionResult.PHYSICALLY_DELETED, result.deletionResult());
        assertEquals(List.of("contents/items/context-manager/delete-by-manager.pdf"), result.orphanedRelativePaths());
        assertFalse(contentItemExists(contentItem.id()));
        assertFalse(blockAssociationExists(60L, contentItem.id()));
    }

    @Test
    void studentCannotDeletePedagogicalContentFromBlock() throws Exception {
        ContentItem contentItem = createContent("Content protected against student",
                "contents/items/student-delete/protected.pdf");
        associateContentToBlock60(contentItem.id(), false);

        assertThrows(
                SecurityException.class,
                () -> contentItemService.deleteContentItemFromBlock(
                        4L,
                        null,
                        AccessProfileType.STUDENT,
                        60L,
                        contentItem.id(),
                        SOURCE_IP
                )
        );

        assertTrue(contentItemExists(contentItem.id()));
        assertTrue(blockAssociationExists(60L, contentItem.id()));
    }

    @Test
    void deletingMandatoryContentFromActiveBlockArchivesInsteadOfPhysicallyDeleting() throws Exception {
        ContentItem contentItem = createContent("Content active required",
                "contents/items/mandatory-active/protected.pdf");
        associateContentToBlock60(contentItem.id(), true);

        ContentRemovalResult result = contentItemService.deleteContentItemFromBlock(
                3L,
                null,
                AccessProfileType.TEACHER,
                60L,
                contentItem.id(),
                SOURCE_IP
        );

        assertEquals(ContentDeletionResult.INACTIVATED, result.deletionResult());
        assertTrue(result.orphanedRelativePaths().isEmpty());
        assertTrue(contentItemExists(contentItem.id()));
        assertEquals("inactive", contentItemState(contentItem.id()));
        assertTrue(blockAssociationExists(60L, contentItem.id()));
    }

    @Test
    void deletingContentFromBlockDeletesItemAndReturnsUniqueStoredFile() throws Exception {
        ContentItem contentItem = createContent("Content unique in block", "contents/items/test-unique/processed/content.txt");
        associateContentToBlock60(contentItem.id(), false);

        ContentRemovalResult result = contentItemService.deleteContentItemFromBlock(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                60L,
                contentItem.id(),
                SOURCE_IP
        );

        assertEquals(ContentDeletionResult.PHYSICALLY_DELETED, result.deletionResult());
        assertEquals(List.of("contents/items/test-unique/processed/content.txt"), result.orphanedRelativePaths());
        assertFalse(contentItemExists(contentItem.id()));
        assertFalse(blockAssociationExists(60L, contentItem.id()));
    }

    @Test
    void deletingContentFromBlockKeepsStoredFileWhenAnotherItemReusesIt() throws Exception {
        String sharedSource = "contents/items/shared/processed/content.pdf";
        ContentItem contentItem = createContent("Content reused in block", sharedSource);
        ContentItem reusedItem = createContent("Another item with the same file", sharedSource);
        associateContentToBlock60(contentItem.id(), false);

        ContentRemovalResult result = contentItemService.deleteContentItemFromBlock(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                60L,
                contentItem.id(),
                SOURCE_IP
        );

        assertEquals(ContentDeletionResult.PHYSICALLY_DELETED, result.deletionResult());
        assertTrue(result.orphanedRelativePaths().isEmpty());
        assertFalse(contentItemExists(contentItem.id()));
        assertTrue(contentItemExists(reusedItem.id()));
    }

    @Test
    void deletingContentFromBlockReturnsThumbnailWhenOriginalFileBecomesUnused() throws Exception {
        ContentItem contentItem = createContent(
                "Video unico",
                ContentFormat.VIDEO,
                "contents/items/test-video/processed/content.mp4"
        );
        associateContentToBlock60(contentItem.id(), false);
        recordContentFile(contentItem.id(),
                "video.mp4",
                "contents/items/test-video/processed/content.mp4",
                "contents/items/test-video/processed/thumbnail.webp");

        ContentRemovalResult result = contentItemService.deleteContentItemFromBlock(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                60L,
                contentItem.id(),
                SOURCE_IP
        );

        assertEquals(List.of(
                "contents/items/test-video/processed/content.mp4",
                "contents/items/test-video/processed/thumbnail.webp"
        ), result.orphanedRelativePaths());
        assertFalse(contentItemExists(contentItem.id()));
    }

    @Test
    void deletingOriginalContentPreservesStoredFileMetadataForAnotherItemUsingSameFile() throws Exception {
        String sharedSource = "contents/video/3/120.mp4";
        String thumbnailPath = "contents/video/3/120-thumb.webp";
        ContentItem originalItem = createContent("Video original", ContentFormat.VIDEO, sharedSource);
        ContentItem reusedItem = createContent("Video reutilizado", ContentFormat.VIDEO, sharedSource);
        associateContentToBlock60(originalItem.id(), false);
        recordContentFile(originalItem.id(), "video.mkv", sharedSource, thumbnailPath);

        ContentRemovalResult originalRemoval = contentItemService.deleteContentItemFromBlock(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                60L,
                originalItem.id(),
                SOURCE_IP
        );

        assertTrue(originalRemoval.orphanedRelativePaths().isEmpty());
        assertFalse(contentItemExists(originalItem.id()));
        assertEquals(List.of(sharedSource, thumbnailPath), storedPathsForContentItem(reusedItem.id()));

        associateContentToBlock60(reusedItem.id(), false);
        ContentRemovalResult reusedRemoval = contentItemService.deleteContentItemFromBlock(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                60L,
                reusedItem.id(),
                SOURCE_IP
        );

        assertEquals(List.of(sharedSource, thumbnailPath), reusedRemoval.orphanedRelativePaths());
        assertFalse(contentItemExists(reusedItem.id()));
    }

    private ContentItem createTextContent(String title) {
        return contentItemService.createContentItem(
                3L,
                null,
                AccessProfileType.TEACHER,
                command(title, ContentFormat.TEXT, "contents/test-support-text.txt"),
                SOURCE_IP
        );
    }

    private ContentItem createContent(String title, String source) {
        return createContent(title, source.endsWith(".pdf") ? ContentFormat.PDF : ContentFormat.TEXT, source);
    }

    private ContentItem createContent(String title, ContentFormat format, String source) {
        return contentItemService.createContentItem(
                3L,
                null,
                AccessProfileType.TEACHER,
                command(title, format, source),
                SOURCE_IP
        );
    }

    private static ContentItemCreateCommand command(String title, ContentFormat format, String source) {
        return new ContentItemCreateCommand(
                title,
                "Content created by service tests",
                format,
                source,
                ContentItemState.ACTIVE
        );
    }

    private static Stream<Arguments> validContentReferences() {
        return Stream.of(
                Arguments.of(ContentFormat.TEXT, "contents/full-lesson-text.txt"),
                Arguments.of(ContentFormat.IMAGE, "contents/image.png"),
                Arguments.of(ContentFormat.VIDEO, "contents/video.mp4"),
                Arguments.of(ContentFormat.AUDIO, "contents/audio.mp3"),
                Arguments.of(ContentFormat.PDF, "contents/guide.pdf"),
                Arguments.of(ContentFormat.ARCHIVE, "contents/archive/package.zip"),
                Arguments.of(ContentFormat.URL, "https://example.com/guide"),
                Arguments.of(ContentFormat.SCORM, "contents/scorm/package.zip"),
                Arguments.of(ContentFormat.XAPI, "contents/xapi/package.zip"),
                Arguments.of(ContentFormat.PRESENTATION, "contents/slides/lesson.pdf"),
                Arguments.of(ContentFormat.EMBED, "https://player.example.com/embed/lesson"),
                Arguments.of(ContentFormat.OTHER, null)
        );
    }

    private static Stream<Arguments> invalidContentReferences() {
        return Stream.of(
                Arguments.of(ContentFormat.TEXT, " "),
                Arguments.of(ContentFormat.PDF, "../guide.pdf"),
                Arguments.of(ContentFormat.PDF, "contents/guide.txt"),
                Arguments.of(ContentFormat.IMAGE, "/contents/image.png"),
                Arguments.of(ContentFormat.URL, "ftp://example.com/guide"),
                Arguments.of(ContentFormat.EMBED, "ftp://example.com/embed"),
                Arguments.of(ContentFormat.EMBED, null)
        );
    }

    private static void associateContentToAssessment90(long contentItemId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO associate_assessment_content (id_assessment, id_content_item, role)
                     VALUES (90, ?, 'support')
                     """)) {
            statement.setLong(1, contentItemId);
            statement.executeUpdate();
        }
    }

    private static void associateContentToBlock60(long contentItemId, boolean mandatory) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO associate_block_content (id_content_block, id_content_item, order_no, role, mandatory)
                     VALUES (60, ?, NULL, 'support_material', ?)
                     """)) {
            statement.setLong(1, contentItemId);
            statement.setBoolean(2, mandatory);
            statement.executeUpdate();
        }
    }

    private static void associateContentToBlock62(long contentItemId, boolean mandatory) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO associate_block_content (id_content_block, id_content_item, order_no, role, mandatory)
                     VALUES (62, ?, NULL, 'support_material', ?)
                     """)) {
            statement.setLong(1, contentItemId);
            statement.setBoolean(2, mandatory);
            statement.executeUpdate();
        }
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
                     ) VALUES (?, ?, 'video/mp4', 'video/mp4', 10, 8, NULL, NULL, ?, ?, 'ready', NULL,
                               '2026-06-04 10:15:30', '2026-06-04 10:15:30')
                     """)) {
            statement.setLong(1, contentItemId);
            statement.setString(2, originalFileName);
            statement.setString(3, finalPath);
            statement.setString(4, thumbnailPath);
            statement.executeUpdate();
        }
    }

    private static boolean contentItemExists(long contentItemId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM content_item
                     WHERE id_content_item = ?
                     """)) {
            statement.setLong(1, contentItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    private static boolean blockAssociationExists(long contentBlockId, long contentItemId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM associate_block_content
                     WHERE id_content_block = ?
                       AND id_content_item = ?
                     """)) {
            statement.setLong(1, contentBlockId);
            statement.setLong(2, contentItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    private static List<String> storedPathsForContentItem(long contentItemId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT final_path, thumbnail_path
                     FROM content_file
                     WHERE id_content_item = ?
                     ORDER BY id_content_file
                     """)) {
            statement.setLong(1, contentItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                java.util.ArrayList<String> paths = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    paths.add(resultSet.getString("final_path"));
                    String thumbnailPath = resultSet.getString("thumbnail_path");
                    if (thumbnailPath != null && !thumbnailPath.isBlank()) {
                        paths.add(thumbnailPath);
                    }
                }
                return List.copyOf(paths);
            }
        }
    }

    private static String contentItemState(long contentItemId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT state
                     FROM content_item
                     WHERE id_content_item = ?
                     """)) {
            statement.setLong(1, contentItemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getString("state");
            }
        }
    }
}
