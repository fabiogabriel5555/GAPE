package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.LessonDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationDecision;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.authorization.PermissionChecker;

final class ScheduleAccessPolicy {

    private final ClassGroupDAO classGroupDAO;
    private final LessonDAO lessonDAO;
    private final AssessmentDAO assessmentDAO;
    private final PermissionChecker permissionChecker;
    private final PermissionDAO permissionDAO;

    ScheduleAccessPolicy(
            ClassGroupDAO classGroupDAO,
            LessonDAO lessonDAO,
            AssessmentDAO assessmentDAO,
            PermissionChecker permissionChecker,
            PermissionDAO permissionDAO
    ) {
        this.classGroupDAO = Objects.requireNonNull(classGroupDAO, "classGroupDAO is required");
        this.lessonDAO = Objects.requireNonNull(lessonDAO, "lessonDAO is required");
        this.assessmentDAO = Objects.requireNonNull(assessmentDAO, "assessmentDAO is required");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.permissionDAO = Objects.requireNonNull(permissionDAO, "permissionDAO is required");
    }

    void requireClassGroupManager(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            String sourceIp
    ) throws SQLException {
        if (canManageClassGroup(connection, actorUserId, sessionId, actorProfileType, classGroupId, sourceIp)) {
            return;
        }
        throw new SecurityException("Missing class group management context");
    }

    void requireLessonManager(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            Lesson lesson,
            String sourceIp
    ) throws SQLException {
        requireClassGroupManager(connection, actorUserId, sessionId, actorProfileType, lesson.classGroupId(), sourceIp);
    }

    void requireAssessmentManager(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            Assessment assessment,
            String sourceIp
    ) throws SQLException {
        List<Long> classGroupIds = assessmentDAO.findApplicableClassGroupIds(connection, assessment.id());
        if (!classGroupIds.isEmpty()) {
            for (Long classGroupId : classGroupIds) {
                requireClassGroupManager(connection, actorUserId, sessionId, actorProfileType, classGroupId, sourceIp);
            }
            return;
        }
        Long classGroupId = assessmentDAO.findContextClassGroupId(connection, assessment.id());
        if (classGroupId != null) {
            requireClassGroupManager(connection, actorUserId, sessionId, actorProfileType, classGroupId, sourceIp);
            return;
        }
        if (assessment.subjectId() != null
                && canManageSubject(actorUserId, sessionId, actorProfileType, assessment.subjectId(), sourceIp)) {
            return;
        }
        throw new SecurityException("Missing assessment management context");
    }

    void requireStudentProfile(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) {
        if (actorProfileType != AccessProfileType.STUDENT) {
            throw new SecurityException("Operation requires a student profile");
        }
        AuthorizationDecision decision = permissionChecker.check(AccessContext.global(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.VIEW_REPORTS,
                sourceIp
        ));
        if (!decision.allowed()) {
            throw new SecurityException("Missing active student profile: " + decision.reason());
        }
    }

    void requireStudentClassGroupAccess(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            String sourceIp
    ) throws SQLException {
        requireStudentProfile(actorUserId, sessionId, actorProfileType, sourceIp);
        if (!lessonDAO.hasCurrentStudentClassGroupAccess(connection, actorUserId, classGroupId)) {
            throw new SecurityException("Student is not enrolled in the class group context");
        }
    }

    boolean canManageClassGroup(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            String sourceIp
    ) throws SQLException {
        ClassGroup classGroup = classGroupDAO.findById(connection, classGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Class group not found: " + classGroupId));
        return switch (actorProfileType) {
            case ADMINISTRATOR -> canAdministratorManageClassGroup(actorUserId, classGroup, sourceIp);
            case COORDINATOR -> canManageSubject(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    classGroup.subjectId(),
                    sourceIp
            );
            case TEACHER -> permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_LEARNING,
                    AccessEntityType.CLASS_GROUP,
                    classGroupId,
                    sourceIp
            )).allowed();
            case STUDENT -> false;
        };
    }

    private boolean canManageSubject(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long subjectId,
            String sourceIp
    ) {
        if (actorProfileType == AccessProfileType.ADMINISTRATOR) {
            return permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_LEARNING,
                    AccessEntityType.SUBJECT,
                    subjectId,
                    sourceIp
            )).allowed();
        }
        return actorProfileType == AccessProfileType.COORDINATOR
                && permissionChecker.check(new AccessContext(
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        AuthorizationPolicy.MANAGE_LEARNING,
                        AccessEntityType.SUBJECT,
                        subjectId,
                        sourceIp
                )).allowed();
    }

    private boolean canAdministratorManageClassGroup(
            long actorUserId,
            ClassGroup classGroup,
            String sourceIp
    ) throws SQLException {
        if (!permissionDAO.activeProfileExists(actorUserId, AccessProfileType.ADMINISTRATOR)) {
            return false;
        }
        if (permissionDAO.hasActiveGlobalAdministratorGrant(actorUserId, AuthorizationPolicy.MANAGE_ALL)) {
            return true;
        }
        if (permissionDAO.hasActiveAdministratorContextGrant(
                actorUserId,
                AuthorizationPolicy.MANAGE_LEARNING,
                AccessEntityType.CLASS_GROUP,
                classGroup.id()
        )) {
            return true;
        }
        if (permissionDAO.hasActiveAdministratorContextGrant(
                actorUserId,
                AuthorizationPolicy.MANAGE_LEARNING,
                AccessEntityType.SUBJECT,
                classGroup.subjectId()
        )) {
            return true;
        }
        return permissionDAO.hasActiveAdministratorContextGrant(
                actorUserId,
                AuthorizationPolicy.MANAGE_LEARNING,
                AccessEntityType.COURSE,
                classGroup.courseId()
        );
    }
}
