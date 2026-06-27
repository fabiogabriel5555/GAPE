package pt.isel.gape.learning.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.validation.AcademicTextValidator;
import pt.isel.gape.common.validation.MediaPathValidator;
import pt.isel.gape.learning.dao.ContentAssociationDAO;
import pt.isel.gape.learning.dao.ContentItemDAO;
import pt.isel.gape.learning.model.ContentAssociationType;
import pt.isel.gape.learning.model.ContentContext;
import pt.isel.gape.learning.model.ContentDeletionResult;
import pt.isel.gape.learning.model.ContentFormat;
import pt.isel.gape.learning.model.ContentItem;
import pt.isel.gape.learning.model.ContentItemCreateCommand;
import pt.isel.gape.learning.model.ContentRemovalResult;
import pt.isel.gape.learning.model.ContentItemState;
import pt.isel.gape.learning.model.ReusableContentFile;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class ContentItemService {

    private static final int TITLE_MAX_LENGTH = 160;
    private static final int DESCRIPTION_MAX_LENGTH = 500;
    private static final int SOURCE_MAX_LENGTH = 1000;

    private final ConnectionProvider connectionProvider;
    private final ContentItemDAO contentItemDAO;
    private final ContentAssociationDAO contentAssociationDAO;
    private final ContentAccessPolicy contentAccessPolicy;
    private final AuditService auditService;
    private final Clock clock;

    public ContentItemService(
            ConnectionProvider connectionProvider,
            ContentItemDAO contentItemDAO,
            ContentAssociationDAO contentAssociationDAO,
            PermissionChecker permissionChecker,
            AuditService auditService,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.contentItemDAO = Objects.requireNonNull(contentItemDAO, "contentItemDAO is required");
        this.contentAssociationDAO = Objects.requireNonNull(contentAssociationDAO, "contentAssociationDAO is required");
        this.contentAccessPolicy = new ContentAccessPolicy(
                Objects.requireNonNull(permissionChecker, "permissionChecker is required"),
                contentAssociationDAO
        );
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public ContentItemService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new ContentItemDAO(connectionProvider),
                new ContentAssociationDAO(connectionProvider),
                new PermissionChecker(
                        new PermissionDAO(connectionProvider),
                        new ManageOrganizationDAO(connectionProvider),
                        new CoordinateSubjectDAO(connectionProvider),
                        new TeachClassGroupDAO(connectionProvider)
                ),
                new AuditService(new ActivityLogDAO(connectionProvider), clock),
                clock
        );
    }

    public ContentItem createContentItem(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ContentItemCreateCommand command,
            String sourceIp
    ) {
        try {
            validateActor(actorUserId, actorProfileType);
            validateCreateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    contentAccessPolicy.requireActiveProfile(actorUserId, sessionId, actorProfileType, sourceIp);
                    long contentItemId = contentItemDAO.create(
                            connection,
                            actorUserId,
                            command,
                            LocalDateTime.now(clock)
                    );
                    auditService.record(connection, actorUserId, sessionId, "CONTENT_ITEM_CREATE",
                            "content_item", Long.toString(contentItemId), "success", sourceIp);
                    connection.commit();
                    return contentItemDAO.findById(contentItemId)
                            .orElseThrow(() -> new IllegalStateException("Created content item was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CONTENT_ITEM_CREATE", "new", sourceIp);
            throw wrap(exception, "Failed to create content item");
        }
    }

    public ContentItem getContentItem(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long contentItemId,
            String sourceIp
    ) {
        validateActor(actorUserId, actorProfileType);
        try (Connection connection = connectionProvider.getConnection()) {
            ContentItem contentItem = requireContentItem(connection, contentItemId);
            if (canReadReusableRepositoryContent(connection, actorUserId, sessionId, actorProfileType, contentItem,
                    sourceIp)) {
                return contentItem;
            }
            requireContentAccess(connection, actorUserId, sessionId, actorProfileType, contentItem, sourceIp);
            return contentItem;
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to read content item");
        }
    }

    public Optional<String> getContentThumbnailPath(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long contentItemId,
            String sourceIp
    ) {
        validateActor(actorUserId, actorProfileType);
        try (Connection connection = connectionProvider.getConnection()) {
            ContentItem contentItem = requireContentItem(connection, contentItemId);
            if (!canReadReusableRepositoryContent(connection, actorUserId, sessionId, actorProfileType, contentItem,
                    sourceIp)) {
                requireContentAccess(connection, actorUserId, sessionId, actorProfileType, contentItem, sourceIp);
            }
            if (!isStoredFileBackedContent(contentItem)) {
                return Optional.empty();
            }
            Optional<String> thumbnailPath = contentItemDAO.findThumbnailPathByContentItemOrSource(
                    connection,
                    contentItem.id(),
                    contentItem.source()
            );
            if (thumbnailPath.isPresent()) {
                return thumbnailPath;
            }
            return contentItem.format() == ContentFormat.IMAGE
                    ? Optional.of(contentItem.source().trim())
                    : Optional.empty();
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to read content thumbnail");
        }
    }

    public List<ReusableContentFile> listReusableFileBackedContent(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) {
        try {
            validateActor(actorUserId, actorProfileType);
            try (Connection connection = connectionProvider.getConnection()) {
                requireRepositoryReuseActor(actorUserId, sessionId, actorProfileType, sourceIp);
                contentItemDAO.ensureAssessmentRepositoryReferences(connection, actorUserId, LocalDateTime.now(clock));
                Map<String, ReusableContentFile> filesBySource = new LinkedHashMap<>();
                for (ContentItem contentItem : contentItemDAO.findReusableFileBackedItems(connection)) {
                    String sourceKey = contentItem.source().trim();
                    filesBySource.putIfAbsent(sourceKey, reusableContentFile(connection, contentItem));
                }
                return List.copyOf(filesBySource.values());
            }
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list reusable content items");
        }
    }

    public List<ReusableContentFile> listReusableFileBackedContent(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ContentAssociationType targetType,
            long targetId,
            String sourceIp
    ) {
        try {
            validateActor(actorUserId, actorProfileType);
            try (Connection connection = connectionProvider.getConnection()) {
                requireRepositoryReuseActor(actorUserId, sessionId, actorProfileType, sourceIp);
                contentItemDAO.ensureAssessmentRepositoryReferences(connection, actorUserId, LocalDateTime.now(clock));
                Map<String, ReusableContentFile> filesBySource = new LinkedHashMap<>();
                for (ContentItem contentItem : contentItemDAO.findReusableFileBackedItems(connection)) {
                    String sourceKey = contentItem.source().trim();
                    filesBySource.putIfAbsent(sourceKey, reusableContentFile(connection, contentItem));
                }
                return List.copyOf(filesBySource.values());
            }
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list reusable content files");
        }
    }

    public ReusableContentFile getReusableFileBackedContent(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long contentItemId,
            String sourceIp
    ) {
        try {
            validateActor(actorUserId, actorProfileType);
            try (Connection connection = connectionProvider.getConnection()) {
                requireRepositoryReuseActor(actorUserId, sessionId, actorProfileType, sourceIp);
                ContentItem contentItem = requireContentItem(connection, contentItemId);
                if (!isReusableFileBackedContent(contentItem)) {
                    throw new IllegalArgumentException("Reusable content file not found: " + contentItemId);
                }
                return reusableContentFile(connection, contentItem);
            }
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to read reusable content file");
        }
    }

    public ReusableContentFile getReusableFileBackedContent(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long contentItemId,
            ContentAssociationType targetType,
            long targetId,
            String sourceIp
    ) {
        try {
            validateActor(actorUserId, actorProfileType);
            try (Connection connection = connectionProvider.getConnection()) {
                requireRepositoryReuseActor(actorUserId, sessionId, actorProfileType, sourceIp);
                ContentItem contentItem = requireContentItem(connection, contentItemId);
                if (!isReusableFileBackedContent(contentItem)) {
                    throw new IllegalArgumentException("Reusable content file not found: " + contentItemId);
                }
                return reusableContentFile(connection, contentItem);
            }
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to read reusable content file");
        }
    }

    public ContentItem replaceContentSource(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long contentItemId,
            ContentFormat format,
            String source,
            String sourceIp
    ) {
        try {
            validateActor(actorUserId, actorProfileType);
            Objects.requireNonNull(format, "content format is required");
            validateSource(format, source);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ContentItem contentItem = requireContentItem(connection, contentItemId);
                    if (contentItem.authorUserId() != actorUserId) {
                        throw new SecurityException("Only the content author can replace the content source");
                    }
                    if (contentItem.format() != format) {
                        throw new IllegalArgumentException("Content source format cannot be changed");
                    }
                    contentItemDAO.updateSource(connection, contentItemId, source, LocalDateTime.now(clock));
                    auditService.record(connection, actorUserId, sessionId, "CONTENT_ITEM_SOURCE_UPDATE",
                            "content_item", Long.toString(contentItemId), "success", sourceIp);
                    connection.commit();
                    return contentItemDAO.findById(connection, contentItemId)
                            .orElseThrow(() -> new IllegalStateException("Updated content item was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CONTENT_ITEM_SOURCE_UPDATE", Long.toString(contentItemId), sourceIp);
            throw wrap(exception, "Failed to update content item source");
        }
    }

    public ContentDeletionResult deleteContentItem(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long contentItemId,
            String sourceIp
    ) {
        try {
            validateActor(actorUserId, actorProfileType);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ContentItem contentItem = requireContentItem(connection, contentItemId);
                    requireDeletionPrivilege(connection, actorUserId, sessionId, actorProfileType, contentItem, sourceIp);
                    ContentDeletionResult result;
                    if (isPhysicalDeletionBlocked(connection, contentItemId)) {
                        contentItemDAO.updateState(
                                connection,
                                contentItemId,
                                ContentItemState.INACTIVE,
                                LocalDateTime.now(clock)
                        );
                        result = ContentDeletionResult.INACTIVATED;
                    } else {
                        contentItemDAO.delete(connection, contentItemId);
                        result = ContentDeletionResult.PHYSICALLY_DELETED;
                    }
                    auditService.record(connection, actorUserId, sessionId, "CONTENT_ITEM_DELETE",
                            "content_item", Long.toString(contentItemId), result.name().toLowerCase(), sourceIp);
                    connection.commit();
                    return result;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CONTENT_ITEM_DELETE", Long.toString(contentItemId), sourceIp);
            throw wrap(exception, "Failed to delete content item");
        }
    }

    public void discardPendingDetachedContentItem(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long contentItemId,
            String sourceIp
    ) {
        try {
            validateActor(actorUserId, actorProfileType);
            contentAccessPolicy.requireActiveProfile(actorUserId, sessionId, actorProfileType, sourceIp);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ContentItem contentItem = requireContentItem(connection, contentItemId);
                    if (contentItem.authorUserId() != actorUserId) {
                        throw new SecurityException("Pending content item belongs to another user");
                    }
                    if (!contentAssociationDAO.findContextsForContent(connection, contentItemId).isEmpty()) {
                        throw new SecurityException("Associated content cannot be discarded as pending");
                    }
                    contentItemDAO.delete(connection, contentItemId);
                    auditService.record(connection, actorUserId, sessionId, "CONTENT_ITEM_PENDING_DISCARD",
                            "content_item", Long.toString(contentItemId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CONTENT_ITEM_PENDING_DISCARD", Long.toString(contentItemId), sourceIp);
            throw wrap(exception, "Failed to discard pending content item");
        }
    }

    @Deprecated
    public ContentRemovalResult removeContentItemFromBlock(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long contentBlockId,
            long contentItemId,
            String sourceIp
    ) {
        return deleteContentItemFromBlock(
                actorUserId,
                sessionId,
                actorProfileType,
                contentBlockId,
                contentItemId,
                sourceIp
        );
    }

    public ContentRemovalResult deleteContentItemFromBlock(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long contentBlockId,
            long contentItemId,
            String sourceIp
    ) {
        try {
            validateActor(actorUserId, actorProfileType);
            if (actorProfileType == AccessProfileType.STUDENT) {
                throw new SecurityException("Students cannot delete pedagogical content");
            }
            if (contentBlockId <= 0) {
                throw new IllegalArgumentException("content block is required");
            }
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ContentItem contentItem = requireContentItem(connection, contentItemId);
                    ContentContext targetContext = contentAssociationDAO
                            .findContext(connection, ContentAssociationType.CONTENT_BLOCK, contentBlockId)
                            .orElseThrow(() -> new IllegalArgumentException("Content block not found: " + contentBlockId));
                    contentAccessPolicy.requireContextManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            targetContext,
                            sourceIp
                    );
                    contentAssociationDAO
                            .findAssociation(connection, ContentAssociationType.CONTENT_BLOCK, contentBlockId, contentItemId)
                            .orElseThrow(() -> new IllegalArgumentException("Content association not found"));

                    ContentDeletionResult result;
                    List<String> orphanedPaths = List.of();
                    if (isPhysicalDeletionBlocked(connection, contentItemId)) {
                        contentItemDAO.updateState(
                                connection,
                                contentItemId,
                                ContentItemState.INACTIVE,
                                LocalDateTime.now(clock)
                        );
                        result = ContentDeletionResult.INACTIVATED;
                    } else {
                        orphanedPaths = orphanedStoredPaths(connection, contentItem);
                        if (orphanedPaths.isEmpty()) {
                            preserveStoredFileMetadataForReuse(connection, contentItem);
                        }
                        contentItemDAO.delete(connection, contentItemId);
                        result = ContentDeletionResult.PHYSICALLY_DELETED;
                    }
                    auditService.record(connection, actorUserId, sessionId, "CONTENT_ITEM_DELETE_FROM_BLOCK",
                            "content_block", contentBlockId + ":" + contentItemId, result.name().toLowerCase(), sourceIp);
                    connection.commit();
                    return new ContentRemovalResult(result, orphanedPaths);
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CONTENT_ITEM_DELETE_FROM_BLOCK", Long.toString(contentItemId), sourceIp);
            throw wrap(exception, "Failed to delete pedagogical content from block");
        }
    }

    boolean canAccessContent(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ContentItem contentItem,
            String sourceIp
    ) throws SQLException {
        if (contentItem.authorUserId() == actorUserId) {
            return true;
        }
        List<ContentContext> contexts = contentAssociationDAO.findContextsForContent(connection, contentItem.id());
        return contentAccessPolicy.canAccessAnyContext(
                connection,
                actorUserId,
                sessionId,
                actorProfileType,
                contexts,
                sourceIp
        );
    }

    private void requireContentAccess(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ContentItem contentItem,
            String sourceIp
    ) throws SQLException {
        validateActor(actorUserId, actorProfileType);
        if (canAccessContent(connection, actorUserId, sessionId, actorProfileType, contentItem, sourceIp)) {
            return;
        }
        throw new SecurityException("Missing content access context");
    }

    private boolean canReadReusableRepositoryContent(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ContentItem contentItem,
            String sourceIp
    ) throws SQLException {
        if (!isReusableFileBackedContent(contentItem) || actorProfileType == AccessProfileType.STUDENT) {
            return false;
        }
        requireRepositoryReuseActor(actorUserId, sessionId, actorProfileType, sourceIp);
        return true;
    }

    private void requireRepositoryReuseActor(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) {
        if (actorProfileType == AccessProfileType.STUDENT) {
            throw new SecurityException("Students cannot reuse repository content");
        }
        contentAccessPolicy.requireActiveProfile(actorUserId, sessionId, actorProfileType, sourceIp);
    }

    private void requireDeletionPrivilege(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ContentItem contentItem,
            String sourceIp
    ) throws SQLException {
        if (actorProfileType == AccessProfileType.STUDENT) {
            throw new SecurityException("Students cannot delete pedagogical content");
        }
        List<ContentContext> contexts = contentAssociationDAO.findContextsForContent(connection, contentItem.id());
        if (contentAccessPolicy.canManageAnyContext(
                connection,
                actorUserId,
                sessionId,
                actorProfileType,
                contexts,
                sourceIp
        )) {
            return;
        }
        throw new SecurityException("Missing content deletion context");
    }

    private boolean isPhysicalDeletionBlocked(Connection connection, long contentItemId) throws SQLException {
        return contentItemDAO.hasSubmittedAssessmentAssociation(connection, contentItemId)
                || contentItemDAO.hasActiveMandatoryBlockAssociation(connection, contentItemId);
    }

    private List<String> orphanedStoredPaths(Connection connection, ContentItem contentItem) throws SQLException {
        if (!isStoredFileBackedContent(contentItem)) {
            return List.of();
        }
        String source = contentItem.source().trim();
        if (contentItemDAO.countItemsBySourceExcluding(connection, source, contentItem.id()) > 0L) {
            return List.of();
        }
        Set<String> paths = new LinkedHashSet<>();
        paths.add(source);
        paths.addAll(contentItemDAO.findStoredRelativePathsForContentItem(connection, contentItem.id()));
        return List.copyOf(paths);
    }

    private void preserveStoredFileMetadataForReuse(Connection connection, ContentItem contentItem)
            throws SQLException {
        if (!isStoredFileBackedContent(contentItem)) {
            return;
        }
        var targetContentItemId = contentItemDAO.findFirstItemIdBySourceExcluding(
                connection,
                contentItem.source(),
                contentItem.id()
        );
        if (targetContentItemId.isPresent()) {
            contentItemDAO.reassignStoredFiles(connection, contentItem.id(), targetContentItemId.get());
        }
    }

    private ContentItem requireContentItem(Connection connection, long contentItemId) throws SQLException {
        return contentItemDAO.findById(connection, contentItemId)
                .orElseThrow(() -> new IllegalArgumentException("Content item not found: " + contentItemId));
    }

    private ReusableContentFile reusableContentFile(Connection connection, ContentItem contentItem)
            throws SQLException {
        boolean defaultMandatory = contentAssociationDAO
                .findFirstBlockMandatory(connection, contentItem.id())
                .orElse(false);
        return new ReusableContentFile(
                contentItem.id(),
                contentItem.title(),
                contentItem.description(),
                contentItem.format(),
                contentItem.source(),
                defaultMandatory
        );
    }

    private static boolean isReusableFileBackedContent(ContentItem contentItem) {
        return contentItem.state() == ContentItemState.ACTIVE
                && (isStoredFileBackedContent(contentItem) || isAssessmentReference(contentItem));
    }

    private static boolean isStoredFileBackedContent(ContentItem contentItem) {
        return (contentItem.format() == ContentFormat.PDF
                || contentItem.format() == ContentFormat.TEXT
                || contentItem.format() == ContentFormat.IMAGE
                || contentItem.format() == ContentFormat.VIDEO
                || contentItem.format() == ContentFormat.AUDIO)
                && contentItem.source() != null
                && !contentItem.source().isBlank()
                && !contentItem.source().startsWith("contents/pending/");
    }

    private static boolean isAssessmentReference(ContentItem contentItem) {
        return contentItem.format() == ContentFormat.OTHER
                && contentItem.source() != null
                && contentItem.source().trim().startsWith("assessment:");
    }

    private static void validateCreateCommand(ContentItemCreateCommand command) {
        Objects.requireNonNull(command, "command is required");
        requireText(command.title(), "Content title is required");
        requireMaxLength(command.title(), TITLE_MAX_LENGTH, "Content title is too long");
        AcademicTextValidator.rejectContextSeparator(command.title(), "Content title");
        requireMaxLength(command.description(), DESCRIPTION_MAX_LENGTH, "Content description is too long");
        Objects.requireNonNull(command.format(), "content format is required");
        Objects.requireNonNull(command.state(), "content state is required");
        validateSource(command.format(), command.source());
    }

    private static void validateSource(ContentFormat format, String source) {
        if (format.requiresSource()) {
            requireText(source, "Content source is required for " + format.toDatabaseValue());
        }
        requireMaxLength(source, SOURCE_MAX_LENGTH, "Content source is too long");

        if (source == null || source.isBlank()) {
            return;
        }
        if (format == ContentFormat.URL || format == ContentFormat.EMBED) {
            requireHttpUrl(source);
            return;
        }
        if (format.isFileBacked()) {
            MediaPathValidator.optionalSafeRelativePath(source, "Content source");
            requireExpectedFileExtension(format, source);
        }
    }

    private static void requireExpectedFileExtension(ContentFormat format, String source) {
        String normalized = source.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("contents/pending/")) {
            return;
        }
        boolean valid = switch (format) {
            case PDF -> normalized.endsWith(".pdf");
            case TEXT -> normalized.endsWith(".txt")
                    || normalized.endsWith(".md")
                    || normalized.endsWith(".html")
                    || normalized.endsWith(".htm");
            case IMAGE -> normalized.endsWith(".jpg")
                    || normalized.endsWith(".jpeg")
                    || normalized.endsWith(".png")
                    || normalized.endsWith(".gif")
                    || normalized.endsWith(".webp");
            case VIDEO -> normalized.endsWith(".mp4")
                    || normalized.endsWith(".webm")
                    || normalized.endsWith(".mov")
                    || normalized.endsWith(".mkv");
            case AUDIO -> normalized.endsWith(".mp3")
                    || normalized.endsWith(".wav")
                    || normalized.endsWith(".ogg")
                    || normalized.endsWith(".m4a");
            case ARCHIVE -> isArchiveSource(normalized);
            case SCORM, XAPI -> normalized.endsWith(".zip");
            case PRESENTATION -> normalized.endsWith(".pdf")
                    || normalized.endsWith(".ppt")
                    || normalized.endsWith(".pptx");
            case URL, EMBED, OTHER -> true;
        };
        if (!valid) {
            throw new IllegalArgumentException("Content source extension is not compatible with " + format.toDatabaseValue());
        }
    }

    private static boolean isArchiveSource(String normalized) {
        return normalized.endsWith(".zip")
                || normalized.endsWith(".rar")
                || normalized.endsWith(".7z")
                || normalized.endsWith(".tar")
                || normalized.endsWith(".tar.gz")
                || normalized.endsWith(".tgz")
                || normalized.endsWith(".tar.bz2")
                || normalized.endsWith(".tbz2")
                || normalized.endsWith(".tar.xz")
                || normalized.endsWith(".txz")
                || normalized.endsWith(".gz")
                || normalized.endsWith(".bz2")
                || normalized.endsWith(".xz");
    }

    private static void requireHttpUrl(String source) {
        try {
            URI uri = new URI(source.trim());
            String scheme = uri.getScheme();
            if (scheme == null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null
                    || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("URL content source must be an absolute HTTP(S) URL");
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("URL content source is invalid", exception);
        }
    }

    private static void validateActor(long actorUserId, AccessProfileType actorProfileType) {
        if (actorUserId <= 0) {
            throw new IllegalArgumentException("Content responsible user is required");
        }
        Objects.requireNonNull(actorProfileType, "actor profile type is required");
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireMaxLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
    }

    private void auditFailure(
            long actorUserId,
            Long sessionId,
            String operationType,
            String affectedIdentifier,
            String sourceIp
    ) {
        Long safeActorId = actorUserId <= 0 ? null : actorUserId;
        auditService.record(safeActorId, sessionId, operationType,
                "content_item", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
