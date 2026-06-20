package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.learning.dao.ContentAssociationDAO;
import pt.isel.gape.learning.model.ContentContext;
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationDecision;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.authorization.PermissionChecker;

final class ContentAccessPolicy {

    private final PermissionChecker permissionChecker;
    private final ContentAssociationDAO contentAssociationDAO;

    ContentAccessPolicy(PermissionChecker permissionChecker, ContentAssociationDAO contentAssociationDAO) {
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.contentAssociationDAO = Objects.requireNonNull(contentAssociationDAO, "contentAssociationDAO is required");
    }

    void requireActiveProfile(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) {
        AuthorizationDecision decision = permissionChecker.check(AccessContext.global(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.VIEW_REPORTS,
                sourceIp
        ));
        if (!decision.allowed()) {
            throw new SecurityException("Missing active profile: " + decision.reason());
        }
    }

    void requireContextManager(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ContentContext context,
            String sourceIp
    ) throws SQLException {
        if (canManageContext(connection, actorUserId, sessionId, actorProfileType, context, sourceIp)) {
            return;
        }
        throw new SecurityException("Missing content management context");
    }

    boolean canManageAnyContext(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            List<ContentContext> contexts,
            String sourceIp
    ) throws SQLException {
        if (contexts.isEmpty()) {
            return canManageDetachedContent(actorUserId, sessionId, actorProfileType, sourceIp);
        }
        for (ContentContext context : contexts) {
            if (canManageContext(connection, actorUserId, sessionId, actorProfileType, context, sourceIp)) {
                return true;
            }
        }
        return false;
    }

    boolean canAccessAnyContext(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            List<ContentContext> contexts,
            String sourceIp
    ) throws SQLException {
        if (contexts.isEmpty()) {
            return canManageDetachedContent(actorUserId, sessionId, actorProfileType, sourceIp);
        }
        for (ContentContext context : contexts) {
            if (canAccessContext(connection, actorUserId, sessionId, actorProfileType, context, sourceIp)) {
                return true;
            }
        }
        return false;
    }

    boolean canAccessContext(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ContentContext context,
            String sourceIp
    ) throws SQLException {
        if (canManageContext(connection, actorUserId, sessionId, actorProfileType, context, sourceIp)) {
            return true;
        }
        return actorProfileType == AccessProfileType.STUDENT
                && contentAssociationDAO.hasActiveStudentAccess(connection, actorUserId, context);
    }

    boolean canManageContext(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ContentContext context,
            String sourceIp
    ) throws SQLException {
        return switch (actorProfileType) {
            case ADMINISTRATOR -> canAdministratorManageContext(actorUserId, sessionId, context, sourceIp);
            case COORDINATOR -> context.subjectId() != null && permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_LEARNING,
                    AccessEntityType.SUBJECT,
                    context.subjectId(),
                    sourceIp
            )).allowed();
            case TEACHER -> context.classGroupId() != null && permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_LEARNING,
                    AccessEntityType.CLASS_GROUP,
                    context.classGroupId(),
                    sourceIp
            )).allowed();
            case STUDENT -> contentAssociationDAO.hasActiveStudentAccess(connection, actorUserId, context);
        };
    }

    private boolean canAdministratorManageContext(
            long actorUserId,
            Long sessionId,
            ContentContext context,
            String sourceIp
    ) {
        AccessEntity entity = mostSpecificEntity(context);
        if (entity == null) {
            return permissionChecker.check(AccessContext.global(
                    actorUserId,
                    sessionId,
                    AccessProfileType.ADMINISTRATOR,
                    AuthorizationPolicy.MANAGE_ALL,
                    sourceIp
            )).allowed();
        }
        return permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                AccessProfileType.ADMINISTRATOR,
                AuthorizationPolicy.MANAGE_LEARNING,
                entity.type(),
                entity.id(),
                sourceIp
        )).allowed();
    }

    private boolean canManageDetachedContent(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) {
        return actorProfileType == AccessProfileType.ADMINISTRATOR
                && permissionChecker.check(AccessContext.global(
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        AuthorizationPolicy.MANAGE_ALL,
                        sourceIp
                )).allowed();
    }

    private static AccessEntity mostSpecificEntity(ContentContext context) {
        if (context.classGroupId() != null) {
            return new AccessEntity(AccessEntityType.CLASS_GROUP, context.classGroupId());
        }
        if (context.subjectId() != null) {
            return new AccessEntity(AccessEntityType.SUBJECT, context.subjectId());
        }
        if (context.courseId() != null) {
            return new AccessEntity(AccessEntityType.COURSE, context.courseId());
        }
        if (context.organicUnitId() != null) {
            return new AccessEntity(AccessEntityType.ORGANIC_UNIT, context.organicUnitId());
        }
        if (context.organizationId() != null) {
            return new AccessEntity(AccessEntityType.ORGANIZATION, context.organizationId());
        }
        return null;
    }

    private record AccessEntity(AccessEntityType type, long id) {
    }
}
