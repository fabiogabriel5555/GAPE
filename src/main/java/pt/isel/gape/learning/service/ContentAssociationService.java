package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.ContentAssociationDAO;
import pt.isel.gape.learning.dao.ContentItemDAO;
import pt.isel.gape.learning.model.BlockContentItem;
import pt.isel.gape.learning.model.ContentAssociation;
import pt.isel.gape.learning.model.ContentAssociationCommand;
import pt.isel.gape.learning.model.ContentAssociationType;
import pt.isel.gape.learning.model.ContentContext;
import pt.isel.gape.learning.model.ContentItem;
import pt.isel.gape.learning.model.ContentItemState;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class ContentAssociationService {

    private static final int ROLE_MAX_LENGTH = 40;

    private final ConnectionProvider connectionProvider;
    private final ContentItemDAO contentItemDAO;
    private final ContentAssociationDAO contentAssociationDAO;
    private final ContentAccessPolicy contentAccessPolicy;
    private final AuditService auditService;

    public ContentAssociationService(
            ConnectionProvider connectionProvider,
            ContentItemDAO contentItemDAO,
            ContentAssociationDAO contentAssociationDAO,
            PermissionChecker permissionChecker,
            AuditService auditService
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.contentItemDAO = Objects.requireNonNull(contentItemDAO, "contentItemDAO is required");
        this.contentAssociationDAO = Objects.requireNonNull(contentAssociationDAO, "contentAssociationDAO is required");
        this.contentAccessPolicy = new ContentAccessPolicy(
                Objects.requireNonNull(permissionChecker, "permissionChecker is required"),
                contentAssociationDAO
        );
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
    }

    public ContentAssociationService(ConnectionProvider connectionProvider, Clock clock) {
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
                new AuditService(new ActivityLogDAO(connectionProvider), clock)
        );
    }

    public void associateContent(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long contentItemId,
            ContentAssociationCommand command,
            String sourceIp
    ) {
        try {
            validateActor(actorUserId, actorProfileType);
            validateAssociationCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ContentItem contentItem = requireContentItem(connection, contentItemId);
                    if (contentItem.state() == ContentItemState.ARCHIVED) {
                        throw new IllegalStateException("Archived content cannot be associated to new contexts");
                    }
                    ContentContext targetContext = requireContext(connection, command.type(), command.targetId());
                    requireAssociationManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            contentItem,
                            targetContext,
                            sourceIp
                    );
                    validateStructuralChain(connection, contentItemId, targetContext);
                    contentAssociationDAO.associate(connection, contentItemId, command);
                    auditService.record(connection, actorUserId, sessionId, "CONTENT_ASSOCIATE",
                            command.type().toDatabaseValue(), command.targetId() + ":" + contentItemId,
                            "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CONTENT_ASSOCIATE", contentItemId, sourceIp);
            throw wrap(exception, "Failed to associate content item");
        }
    }

    public void removeContentAssociation(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long contentItemId,
            ContentAssociationType type,
            long targetId,
            String sourceIp
    ) {
        try {
            validateActor(actorUserId, actorProfileType);
            Objects.requireNonNull(type, "association type is required");
            if (targetId <= 0) {
                throw new IllegalArgumentException("association target is required");
            }
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ContentItem contentItem = requireContentItem(connection, contentItemId);
                    ContentContext targetContext = requireContext(connection, type, targetId);
                    requireAssociationManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            contentItem,
                            targetContext,
                            sourceIp
                    );
                    ContentAssociation association = contentAssociationDAO
                            .findAssociation(connection, type, targetId, contentItemId)
                            .orElseThrow(() -> new IllegalArgumentException("Content association not found"));
                    if (type == ContentAssociationType.CONTENT_BLOCK
                            && association.mandatory()
                            && targetContext.isActive()
                            && actorProfileType != AccessProfileType.ADMINISTRATOR) {
                        throw new IllegalStateException("Mandatory content cannot be removed from an active block");
                    }
                    contentAssociationDAO.remove(connection, type, targetId, contentItemId);
                    auditService.record(connection, actorUserId, sessionId, "CONTENT_ASSOCIATION_REMOVE",
                            type.toDatabaseValue(), targetId + ":" + contentItemId, "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CONTENT_ASSOCIATION_REMOVE", contentItemId, sourceIp);
            throw wrap(exception, "Failed to remove content association");
        }
    }

    public List<ContentItem> listContentItems(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ContentAssociationType type,
            long targetId,
            String sourceIp
    ) {
        try {
            validateActor(actorUserId, actorProfileType);
            Objects.requireNonNull(type, "association type is required");
            try (Connection connection = connectionProvider.getConnection()) {
                ContentContext targetContext = requireContext(connection, type, targetId);
                if (!contentAccessPolicy.canAccessContext(
                        connection,
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        targetContext,
                        sourceIp
                )) {
                    throw new SecurityException("Missing content listing context");
                }
                return contentAssociationDAO.findContentItemsByContext(connection, type, targetId);
            }
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list content items");
        }
    }

    public List<BlockContentItem> listBlockContentItems(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long contentBlockId,
            String sourceIp
    ) {
        try {
            validateActor(actorUserId, actorProfileType);
            try (Connection connection = connectionProvider.getConnection()) {
                ContentContext targetContext = requireContext(
                        connection,
                        ContentAssociationType.CONTENT_BLOCK,
                        contentBlockId
                );
                if (!contentAccessPolicy.canAccessContext(
                        connection,
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        targetContext,
                        sourceIp
                )) {
                    throw new SecurityException("Missing content listing context");
                }
                return contentAssociationDAO.findBlockContentItems(connection, contentBlockId);
            }
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list block content items");
        }
    }

    private void requireAssociationManager(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ContentItem contentItem,
            ContentContext targetContext,
            String sourceIp
    ) throws SQLException {
        if (actorProfileType == AccessProfileType.STUDENT && contentItem.authorUserId() != actorUserId) {
            throw new SecurityException("Students can only manage their own content");
        }
        contentAccessPolicy.requireContextManager(
                connection,
                actorUserId,
                sessionId,
                actorProfileType,
                targetContext,
                sourceIp
        );
    }

    private void validateStructuralChain(
            Connection connection,
            long contentItemId,
            ContentContext targetContext
    ) throws SQLException {
        if (targetContext.courseId() != null
                && targetContext.subjectId() != null
                && !contentAssociationDAO.courseIntegratesSubject(
                        connection,
                        targetContext.courseId(),
                        targetContext.subjectId()
                )) {
            throw new IllegalArgumentException("Target course does not integrate target subject");
        }

        for (ContentContext existingContext : contentAssociationDAO.findContextsForContent(connection, contentItemId)) {
            if (existingContext.organizationId() != null
                    && targetContext.organizationId() != null
                    && !existingContext.organizationId().equals(targetContext.organizationId())) {
                throw new IllegalArgumentException("Content context belongs to a different organization");
            }
            validateCourseSubjectCompatibility(connection, existingContext, targetContext);
            validateSpecificSubjectCompatibility(existingContext, targetContext);
        }
    }

    private void validateCourseSubjectCompatibility(
            Connection connection,
            ContentContext existingContext,
            ContentContext targetContext
    ) throws SQLException {
        if (existingContext.courseId() != null && targetContext.subjectId() != null
                && targetContext.courseId() == null
                && !contentAssociationDAO.courseIntegratesSubject(
                        connection,
                        existingContext.courseId(),
                        targetContext.subjectId()
                )) {
            throw new IllegalArgumentException("Content subject is not integrated in the existing course context");
        }
        if (existingContext.subjectId() != null && targetContext.courseId() != null
                && existingContext.courseId() == null
                && !contentAssociationDAO.courseIntegratesSubject(
                        connection,
                        targetContext.courseId(),
                        existingContext.subjectId()
                )) {
            throw new IllegalArgumentException("Content course does not integrate the existing subject context");
        }
    }

    private static void validateSpecificSubjectCompatibility(
            ContentContext existingContext,
            ContentContext targetContext
    ) {
        boolean existingSpecific = existingContext.type() == ContentAssociationType.SUBJECT
                || existingContext.type() == ContentAssociationType.CLASS_GROUP
                || existingContext.type() == ContentAssociationType.CONTENT_BLOCK
                || existingContext.type() == ContentAssociationType.ASSESSMENT;
        boolean targetSpecific = targetContext.type() == ContentAssociationType.SUBJECT
                || targetContext.type() == ContentAssociationType.CLASS_GROUP
                || targetContext.type() == ContentAssociationType.CONTENT_BLOCK
                || targetContext.type() == ContentAssociationType.ASSESSMENT;

        if (existingSpecific
                && targetSpecific
                && existingContext.subjectId() != null
                && targetContext.subjectId() != null
                && !existingContext.subjectId().equals(targetContext.subjectId())) {
            throw new IllegalArgumentException("Content subject context is incompatible with the target context");
        }
    }

    private ContentItem requireContentItem(Connection connection, long contentItemId) throws SQLException {
        return contentItemDAO.findById(connection, contentItemId)
                .orElseThrow(() -> new IllegalArgumentException("Content item not found: " + contentItemId));
    }

    private ContentContext requireContext(
            Connection connection,
            ContentAssociationType type,
            long targetId
    ) throws SQLException {
        if (targetId <= 0) {
            throw new IllegalArgumentException("association target is required");
        }
        return contentAssociationDAO.findContext(connection, type, targetId)
                .orElseThrow(() -> new IllegalArgumentException("Association target not found: " + type));
    }

    private static void validateAssociationCommand(ContentAssociationCommand command) {
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(command.type(), "association type is required");
        if (command.targetId() <= 0) {
            throw new IllegalArgumentException("association target is required");
        }
        if (command.role() == null || command.role().isBlank()) {
            throw new IllegalArgumentException("association role is required");
        }
        if (command.role().length() > ROLE_MAX_LENGTH) {
            throw new IllegalArgumentException("association role is too long");
        }
        if (command.type() == ContentAssociationType.CONTENT_BLOCK
                && command.orderNo() != null
                && command.orderNo() <= 0) {
            throw new IllegalArgumentException("block content order must be positive");
        }
        if (command.type() != ContentAssociationType.CONTENT_BLOCK
                && (command.orderNo() != null || command.mandatory())) {
            throw new IllegalArgumentException("order and mandatory flag are only valid for block content");
        }
    }

    private static void validateActor(long actorUserId, AccessProfileType actorProfileType) {
        if (actorUserId <= 0) {
            throw new IllegalArgumentException("actor user is required");
        }
        Objects.requireNonNull(actorProfileType, "actor profile type is required");
    }

    private void auditFailure(
            long actorUserId,
            Long sessionId,
            String operationType,
            long contentItemId,
            String sourceIp
    ) {
        Long safeActorId = actorUserId <= 0 ? null : actorUserId;
        auditService.record(safeActorId, sessionId, operationType,
                "content_item", Long.toString(contentItemId), "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
