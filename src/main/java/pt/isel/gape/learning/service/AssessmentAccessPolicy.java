package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationDecision;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.authorization.PermissionChecker;

final class AssessmentAccessPolicy {

    private final AssessmentDAO assessmentDAO;
    private final PermissionDAO permissionDAO;
    private final PermissionChecker permissionChecker;

    AssessmentAccessPolicy(
            AssessmentDAO assessmentDAO,
            PermissionDAO permissionDAO,
            PermissionChecker permissionChecker
    ) {
        this.assessmentDAO = Objects.requireNonNull(assessmentDAO, "assessmentDAO is required");
        this.permissionDAO = Objects.requireNonNull(permissionDAO, "permissionDAO is required");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
    }

    void requireAssessmentManager(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            Assessment assessment,
            String sourceIp
    ) throws SQLException {
        Long classGroupId = assessmentDAO.findContextClassGroupId(connection, assessment.id());
        List<Long> classGroupIds = assessmentDAO.findApplicableClassGroupIds(connection, assessment.id());
        if (!classGroupIds.isEmpty()) {
            for (Long applicableClassGroupId : classGroupIds) {
                requireContextManager(
                        connection,
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        assessment.subjectId(),
                        applicableClassGroupId,
                        assessment.id(),
                        sourceIp
                );
            }
            return;
        }
        requireContextManager(
                connection,
                actorUserId,
                sessionId,
                actorProfileType,
                assessment.subjectId(),
                classGroupId,
                assessment.id(),
                sourceIp
        );
    }

    void requireContextManager(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            Long subjectId,
            Long classGroupId,
            Long assessmentId,
            String sourceIp
    ) throws SQLException {
        AuthorizationDecision adminDecision = AuthorizationDecision.deny("administrator_profile_required");
        if (actorProfileType == AccessProfileType.ADMINISTRATOR) {
            if (canAdministratorManage(actorUserId, subjectId, classGroupId)) {
                return;
            }
            adminDecision = AuthorizationDecision.deny("missing_assessment_context");
        }

        AuthorizationDecision coordinatorDecision = AuthorizationDecision.deny("coordinator_profile_required");
        if (actorProfileType == AccessProfileType.COORDINATOR && subjectId != null) {
            coordinatorDecision = permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_LEARNING,
                    AccessEntityType.SUBJECT,
                    subjectId,
                    sourceIp
            ));
            if (coordinatorDecision.allowed()) {
                return;
            }
        }

        AuthorizationDecision teacherDecision = AuthorizationDecision.deny("teacher_profile_required");
        if (actorProfileType == AccessProfileType.TEACHER) {
            boolean hasTeacherContext = classGroupId != null
                    ? canTeacherManageClassGroup(connection, actorUserId, classGroupId)
                    : assessmentId == null && subjectId != null && canTeacherManageSubject(connection, actorUserId, subjectId);
            if (hasTeacherContext) {
                return;
            }
            if (assessmentId != null && assessmentDAO.hasActiveTeacherManagementContext(
                    connection,
                    actorUserId,
                    assessmentId
            )) {
                return;
            }
            teacherDecision = AuthorizationDecision.deny("missing_class_group_assignment");
        }

        throw new SecurityException("Missing assessment management context: "
                + adminDecision.reason()
                + "/" + coordinatorDecision.reason()
                + "/" + teacherDecision.reason());
    }

    void requireStudentExecutionAccess(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            Assessment assessment,
            String sourceIp
    ) throws SQLException {
        if (actorProfileType != AccessProfileType.STUDENT) {
            throw new SecurityException("Assessment attempts require a student profile");
        }
        requireActiveProfile(actorUserId, sessionId, actorProfileType, sourceIp);
        if (!assessmentDAO.hasCurrentStudentAccess(connection, actorUserId, assessment.id())) {
            throw new SecurityException("Student is not enrolled in the assessment context");
        }
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

    private boolean canAdministratorManage(long actorUserId, Long subjectId, Long classGroupId)
            throws SQLException {
        if (!permissionDAO.activeProfileExists(actorUserId, AccessProfileType.ADMINISTRATOR)) {
            return false;
        }
        if (permissionDAO.hasActiveGlobalAdministratorGrant(actorUserId, AuthorizationPolicy.MANAGE_ALL)) {
            return true;
        }
        if (classGroupId != null && permissionDAO.hasActiveAdministratorContextGrant(
                actorUserId,
                AuthorizationPolicy.MANAGE_LEARNING,
                AccessEntityType.CLASS_GROUP,
                classGroupId
        )) {
            return true;
        }
        return subjectId != null && permissionDAO.hasActiveAdministratorContextGrant(
                actorUserId,
                AuthorizationPolicy.MANAGE_LEARNING,
                AccessEntityType.SUBJECT,
                subjectId
        );
    }

    private static boolean canTeacherManageClassGroup(
            Connection connection,
            long teacherUserId,
            long classGroupId
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM teach_class_group tcg
                JOIN grant_teacher gt ON gt.id_teacher_user = tcg.id_teacher_user
                JOIN permission p ON p.cod_permission = gt.cod_permission
                JOIN user_account u ON u.id_user = tcg.id_teacher_user
                WHERE tcg.id_teacher_user = ?
                  AND tcg.id_class_group = ?
                  AND tcg.state = 'active'
                  AND gt.cod_permission = 'MANAGE_LEARNING'
                  AND p.state = 'active'
                  AND u.state = 'active'
                  AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                  AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, teacherUserId);
            statement.setLong(2, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    private static boolean canTeacherManageSubject(
            Connection connection,
            long teacherUserId,
            long subjectId
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM class_group cg
                JOIN teach_class_group tcg ON tcg.id_class_group = cg.id_class_group
                JOIN grant_teacher gt ON gt.id_teacher_user = tcg.id_teacher_user
                JOIN permission p ON p.cod_permission = gt.cod_permission
                JOIN user_account u ON u.id_user = tcg.id_teacher_user
                WHERE cg.id_subject = ?
                  AND cg.state = 'active'
                  AND tcg.id_teacher_user = ?
                  AND tcg.state = 'active'
                  AND gt.cod_permission = 'MANAGE_LEARNING'
                  AND p.state = 'active'
                  AND u.state = 'active'
                  AND (tcg.start_date IS NULL OR tcg.start_date <= CURRENT_DATE)
                  AND (tcg.end_date IS NULL OR tcg.end_date >= CURRENT_DATE)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, subjectId);
            statement.setLong(2, teacherUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }
}
