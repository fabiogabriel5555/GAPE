package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.validation.AcademicTextValidator;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.ContentBlockDAO;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.ContentBlockCreateCommand;
import pt.isel.gape.learning.model.ContentBlockState;
import pt.isel.gape.learning.model.ContentBlockUpdateCommand;
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationDecision;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class ContentBlockService {

    private final ConnectionProvider connectionProvider;
    private final ContentBlockDAO contentBlockDAO;
    private final ClassGroupDAO classGroupDAO;
    private final PermissionChecker permissionChecker;
    private final AuditService auditService;

    public ContentBlockService(
            ConnectionProvider connectionProvider,
            ContentBlockDAO contentBlockDAO,
            ClassGroupDAO classGroupDAO,
            PermissionChecker permissionChecker,
            AuditService auditService
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.contentBlockDAO = Objects.requireNonNull(contentBlockDAO, "contentBlockDAO is required");
        this.classGroupDAO = Objects.requireNonNull(classGroupDAO, "classGroupDAO is required");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
    }

    public ContentBlockService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new ContentBlockDAO(connectionProvider),
                new ClassGroupDAO(connectionProvider),
                new PermissionChecker(
                        new PermissionDAO(connectionProvider),
                        new ManageOrganizationDAO(connectionProvider),
                        new CoordinateSubjectDAO(connectionProvider),
                        new TeachClassGroupDAO(connectionProvider)
                ),
                new AuditService(new ActivityLogDAO(connectionProvider), clock)
        );
    }

    public ContentBlock createContentBlock(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ContentBlockCreateCommand command,
            String sourceIp
    ) {
        try {
            validateCreateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ClassGroup classGroup = requireClassGroup(connection, command.classGroupId());
                    requireContentBlockManager(actorUserId, sessionId, actorProfileType, classGroup, sourceIp);
                    if (contentBlockDAO.orderExists(connection, command.classGroupId(), command.orderNo(), null)) {
                        throw new IllegalStateException("Content block order already exists in this class group");
                    }
                    long contentBlockId = contentBlockDAO.create(connection, command);
                    auditService.record(connection, actorUserId, sessionId, "CONTENT_BLOCK_CREATE",
                            "content_block", Long.toString(contentBlockId), "success", sourceIp);
                    connection.commit();
                    return contentBlockDAO.findById(contentBlockId)
                            .orElseThrow(() -> new IllegalStateException("Created content block was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CONTENT_BLOCK_CREATE", "new", sourceIp);
            throw wrap(exception, "Failed to create content block");
        }
    }

    public ContentBlock getContentBlock(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long contentBlockId,
            String sourceIp
    ) {
        try {
            ContentBlock contentBlock = requireContentBlock(contentBlockId);
            ClassGroup classGroup = requireClassGroup(contentBlock.classGroupId());
            requireContentBlockManager(actorUserId, sessionId, actorProfileType, classGroup, sourceIp);
            return contentBlock;
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to read content block");
        }
    }

    public List<ContentBlock> listContentBlocksByClassGroup(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            String sourceIp
    ) {
        try {
            ClassGroup classGroup = requireClassGroup(classGroupId);
            requireContentBlockManager(actorUserId, sessionId, actorProfileType, classGroup, sourceIp);
            return contentBlockDAO.findByClassGroup(classGroupId);
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list content blocks");
        }
    }

    public void reorderContentBlocks(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            List<Long> orderedBlockIds,
            String sourceIp
    ) {
        try {
            if (orderedBlockIds == null || orderedBlockIds.isEmpty()) {
                throw new IllegalArgumentException("Block order is required");
            }
            if (new HashSet<>(orderedBlockIds).size() != orderedBlockIds.size()) {
                throw new IllegalArgumentException("Block order contains duplicates");
            }
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ClassGroup classGroup = requireClassGroup(connection, classGroupId);
                    requireContentBlockManager(actorUserId, sessionId, actorProfileType, classGroup, sourceIp);
                    List<ContentBlock> blocks = contentBlockDAO.findByClassGroup(connection, classGroupId, true);
                    Set<Long> expectedIds = new HashSet<>();
                    for (ContentBlock block : blocks) {
                        expectedIds.add(block.id());
                    }
                    if (!expectedIds.equals(new HashSet<>(orderedBlockIds))) {
                        throw new IllegalArgumentException("Block order must include every block in the class group");
                    }
                    contentBlockDAO.reorderWithinClassGroup(connection, classGroupId, orderedBlockIds);
                    auditService.record(connection, actorUserId, sessionId, "CONTENT_BLOCK_REORDER",
                            "class_group", Long.toString(classGroupId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CONTENT_BLOCK_REORDER", Long.toString(classGroupId), sourceIp);
            throw wrap(exception, "Failed to reorder content blocks");
        }
    }

    public ContentBlock updateContentBlock(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long contentBlockId,
            ContentBlockUpdateCommand command,
            String sourceIp
    ) {
        try {
            validateUpdateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ContentBlock current = requireContentBlock(connection, contentBlockId);
                    requireSameContentBlockContext(current, command);
                    ClassGroup classGroup = requireClassGroup(connection, current.classGroupId());
                    requireContentBlockManager(actorUserId, sessionId, actorProfileType, classGroup, sourceIp);
                    if (contentBlockDAO.orderExists(
                            connection,
                            current.classGroupId(),
                            command.orderNo(),
                            contentBlockId
                    )) {
                        throw new IllegalStateException("Content block order already exists in this class group");
                    }
                    contentBlockDAO.update(connection, contentBlockId, command);
                    auditService.record(connection, actorUserId, sessionId, "CONTENT_BLOCK_UPDATE",
                            "content_block", Long.toString(contentBlockId), "success", sourceIp);
                    connection.commit();
                    return contentBlockDAO.findById(contentBlockId)
                            .orElseThrow(() -> new IllegalStateException("Updated content block was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CONTENT_BLOCK_UPDATE", Long.toString(contentBlockId), sourceIp);
            throw wrap(exception, "Failed to update content block");
        }
    }

    public void deleteContentBlock(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long contentBlockId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ContentBlock current = requireContentBlock(connection, contentBlockId);
                    ClassGroup classGroup = requireClassGroup(connection, current.classGroupId());
                    requireContentBlockManager(actorUserId, sessionId, actorProfileType, classGroup, sourceIp);
                    if (contentBlockDAO.hasDomainDependencies(connection, contentBlockId)) {
                        throw new IllegalStateException("Content block with domain dependencies cannot be deleted");
                    }
                    contentBlockDAO.delete(connection, contentBlockId);
                    auditService.record(connection, actorUserId, sessionId, "CONTENT_BLOCK_DELETE",
                            "content_block", Long.toString(contentBlockId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CONTENT_BLOCK_DELETE", Long.toString(contentBlockId), sourceIp);
            throw wrap(exception, "Failed to delete content block");
        }
    }

    private ContentBlock requireContentBlock(long contentBlockId) throws SQLException {
        return contentBlockDAO.findById(contentBlockId)
                .orElseThrow(() -> new IllegalArgumentException("Content block not found: " + contentBlockId));
    }

    private ContentBlock requireContentBlock(Connection connection, long contentBlockId) throws SQLException {
        return contentBlockDAO.findById(connection, contentBlockId)
                .orElseThrow(() -> new IllegalArgumentException("Content block not found: " + contentBlockId));
    }

    private ClassGroup requireClassGroup(long classGroupId) throws SQLException {
        return classGroupDAO.findById(classGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Class group not found: " + classGroupId));
    }

    private ClassGroup requireClassGroup(Connection connection, long classGroupId) throws SQLException {
        return classGroupDAO.findById(connection, classGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Class group not found: " + classGroupId));
    }

    private void requireContentBlockManager(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ClassGroup classGroup,
            String sourceIp
    ) {
        AuthorizationDecision classGroupDecision = permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_LEARNING,
                AccessEntityType.CLASS_GROUP,
                classGroup.id(),
                sourceIp
        ));
        if (classGroupDecision.allowed()) {
            return;
        }
        AuthorizationDecision coordinatorDecision = AuthorizationDecision.deny("coordinator_profile_required");
        if (actorProfileType == AccessProfileType.COORDINATOR) {
            coordinatorDecision = permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_LEARNING,
                    AccessEntityType.SUBJECT,
                    classGroup.subjectId(),
                    sourceIp
            ));
            if (coordinatorDecision.allowed()) {
                return;
            }
        }
        throw new SecurityException("Missing content block management context: "
                + classGroupDecision.reason() + "/" + coordinatorDecision.reason());
    }

    private static void validateCreateCommand(ContentBlockCreateCommand command) {
        Objects.requireNonNull(command, "command is required");
        validateCommonCommand(
                command.classGroupId(),
                command.code(),
                command.name(),
                command.orderNo(),
                command.state()
        );
    }

    private static void validateUpdateCommand(ContentBlockUpdateCommand command) {
        Objects.requireNonNull(command, "command is required");
        validateCommonCommand(
                command.classGroupId(),
                command.code(),
                command.name(),
                command.orderNo(),
                command.state()
        );
    }

    private static void requireSameContentBlockContext(ContentBlock current, ContentBlockUpdateCommand command) {
        if (current.classGroupId() != command.classGroupId()) {
            throw new IllegalArgumentException("Content block class group cannot be changed after creation");
        }
    }

    private static void validateCommonCommand(
            long classGroupId,
            String code,
            String name,
            int orderNo,
            ContentBlockState state
    ) {
        if (classGroupId <= 0) {
            throw new IllegalArgumentException("Content block class group is required");
        }
        requireText(code, "Content block code is required");
        AcademicTextValidator.rejectContextSeparator(code, "Content block code");
        AcademicTextValidator.requireName(name, "Content block name is required");
        if (orderNo <= 0) {
            throw new IllegalArgumentException("Content block order must be positive");
        }
        Objects.requireNonNull(state, "content block state is required");
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
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
        auditService.record(actorUserId, sessionId, operationType,
                "content_block", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
