package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.learning.dao.GradeAccessDAO;
import pt.isel.gape.learning.model.GradeSheet;
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationDecision;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.authorization.PermissionChecker;

final class GradeAccessPolicy {

    private final PermissionChecker permissionChecker;
    private final PermissionDAO permissionDAO;
    private final GradeAccessDAO gradeAccessDAO;

    GradeAccessPolicy(PermissionChecker permissionChecker, PermissionDAO permissionDAO) {
        this(permissionChecker, permissionDAO, new GradeAccessDAO());
    }

    GradeAccessPolicy(PermissionChecker permissionChecker, PermissionDAO permissionDAO, GradeAccessDAO gradeAccessDAO) {
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.permissionDAO = Objects.requireNonNull(permissionDAO, "permissionDAO is required");
        this.gradeAccessDAO = Objects.requireNonNull(gradeAccessDAO, "gradeAccessDAO is required");
    }

    void requireGradeSheetManager(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            GradeSheet gradeSheet,
            String sourceIp
    ) throws SQLException {
        if (gradeSheet.classGroupIds() != null && !gradeSheet.classGroupIds().isEmpty()) {
            for (Long classGroupId : gradeSheet.classGroupIds()) {
                requireClassGroupManager(connection, actorUserId, sessionId, actorProfileType, classGroupId, sourceIp);
            }
            return;
        }
        requireSubjectManager(connection, actorUserId, sessionId, actorProfileType, gradeSheet.subjectId(), sourceIp);
    }

    void requireSubjectManager(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long subjectId,
            String sourceIp
    ) throws SQLException {
        if (canManageSubject(connection, actorUserId, sessionId, actorProfileType, subjectId, sourceIp)) {
            return;
        }
        throw new SecurityException("Missing subject management context");
    }

    void requireCourseManager(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            String sourceIp
    ) throws SQLException {
        if (canManageCourse(connection, actorUserId, sessionId, actorProfileType, courseId, sourceIp)) {
            return;
        }
        throw new SecurityException("Missing course management context");
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

    boolean canManageSubject(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long subjectId,
            String sourceIp
    ) throws SQLException {
        return switch (actorProfileType) {
            case ADMINISTRATOR -> canAdministratorManageSubject(actorUserId, subjectId);
            case COORDINATOR -> permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_LEARNING,
                    AccessEntityType.SUBJECT,
                    subjectId,
                    sourceIp
            )).allowed();
            case TEACHER -> canTeacherManageSubject(connection, actorUserId, subjectId);
            case STUDENT -> false;
        };
    }

    private void requireClassGroupManager(
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

    private boolean canManageClassGroup(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            String sourceIp
    ) throws SQLException {
        return switch (actorProfileType) {
            case ADMINISTRATOR -> canAdministratorManageClassGroup(connection, actorUserId, classGroupId);
            case COORDINATOR -> canCoordinatorManageClassGroup(
                    connection,
                    actorUserId,
                    sessionId,
                    classGroupId,
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

    private boolean canManageCourse(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            String sourceIp
    ) throws SQLException {
        return switch (actorProfileType) {
            case ADMINISTRATOR -> canAdministratorManageCourse(actorUserId, courseId);
            case COORDINATOR -> canCoordinatorManageCourse(connection, actorUserId, courseId);
            case TEACHER -> canTeacherManageCourse(connection, actorUserId, courseId);
            case STUDENT -> false;
        };
    }

    private boolean canCoordinatorManageClassGroup(
            Connection connection,
            long coordinatorUserId,
            Long sessionId,
            long classGroupId,
            String sourceIp
    ) throws SQLException {
        long subjectId = gradeAccessDAO.findClassGroupSubjectId(connection, classGroupId);
        return permissionChecker.check(new AccessContext(
                coordinatorUserId,
                sessionId,
                AccessProfileType.COORDINATOR,
                AuthorizationPolicy.MANAGE_LEARNING,
                AccessEntityType.SUBJECT,
                subjectId,
                sourceIp
        )).allowed();
    }

    private boolean canAdministratorManageSubject(long actorUserId, long subjectId) throws SQLException {
        if (!permissionDAO.activeProfileExists(actorUserId, AccessProfileType.ADMINISTRATOR)) {
            return false;
        }
        if (permissionDAO.hasActiveGlobalAdministratorGrant(actorUserId, AuthorizationPolicy.MANAGE_ALL)) {
            return true;
        }
        return permissionDAO.hasActiveAdministratorContextGrant(
                actorUserId,
                AuthorizationPolicy.MANAGE_LEARNING,
                AccessEntityType.SUBJECT,
                subjectId
        );
    }

    private boolean canAdministratorManageCourse(long actorUserId, long courseId) throws SQLException {
        if (!permissionDAO.activeProfileExists(actorUserId, AccessProfileType.ADMINISTRATOR)) {
            return false;
        }
        if (permissionDAO.hasActiveGlobalAdministratorGrant(actorUserId, AuthorizationPolicy.MANAGE_ALL)) {
            return true;
        }
        return permissionDAO.hasActiveAdministratorContextGrant(
                actorUserId,
                AuthorizationPolicy.MANAGE_LEARNING,
                AccessEntityType.COURSE,
                courseId
        );
    }

    private boolean canAdministratorManageClassGroup(
            Connection connection,
            long actorUserId,
            long classGroupId
    ) throws SQLException {
        if (!permissionDAO.activeProfileExists(actorUserId, AccessProfileType.ADMINISTRATOR)) {
            return false;
        }
        if (permissionDAO.hasActiveGlobalAdministratorGrant(actorUserId, AuthorizationPolicy.MANAGE_ALL)) {
            return true;
        }
        GradeAccessDAO.ClassGroupContext context = gradeAccessDAO.findClassGroupContext(connection, classGroupId);
        return permissionDAO.hasActiveAdministratorContextGrant(
                actorUserId,
                AuthorizationPolicy.MANAGE_LEARNING,
                AccessEntityType.CLASS_GROUP,
                classGroupId
        ) || permissionDAO.hasActiveAdministratorContextGrant(
                actorUserId,
                AuthorizationPolicy.MANAGE_LEARNING,
                AccessEntityType.SUBJECT,
                context.subjectId()
        ) || permissionDAO.hasActiveAdministratorContextGrant(
                actorUserId,
                AuthorizationPolicy.MANAGE_LEARNING,
                AccessEntityType.COURSE,
                context.courseId()
        );
    }

    private boolean canTeacherManageSubject(Connection connection, long teacherUserId, long subjectId)
            throws SQLException {
        return gradeAccessDAO.teacherManagesSubject(connection, teacherUserId, subjectId);
    }

    private boolean canCoordinatorManageCourse(Connection connection, long coordinatorUserId, long courseId)
            throws SQLException {
        return gradeAccessDAO.coordinatorManagesCourse(connection, coordinatorUserId, courseId);
    }

    private boolean canTeacherManageCourse(Connection connection, long teacherUserId, long courseId)
            throws SQLException {
        return gradeAccessDAO.teacherManagesCourse(connection, teacherUserId, courseId);
    }
}
