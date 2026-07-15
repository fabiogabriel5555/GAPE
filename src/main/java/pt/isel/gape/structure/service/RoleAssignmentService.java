package pt.isel.gape.structure.service;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class RoleAssignmentService {

    private final PermissionDAO permissionDAO;
    private final ManageOrganizationDAO manageOrganizationDAO;
    private final CoordinateSubjectDAO coordinateSubjectDAO;
    private final TeachClassGroupDAO teachClassGroupDAO;
    private final AuditService auditService;

    public RoleAssignmentService(
            PermissionDAO permissionDAO,
            ManageOrganizationDAO manageOrganizationDAO,
            CoordinateSubjectDAO coordinateSubjectDAO,
            TeachClassGroupDAO teachClassGroupDAO,
            AuditService auditService
    ) {
        this.permissionDAO = Objects.requireNonNull(permissionDAO, "permissionDAO is required");
        this.manageOrganizationDAO = Objects.requireNonNull(manageOrganizationDAO, "manageOrganizationDAO is required");
        this.coordinateSubjectDAO = Objects.requireNonNull(coordinateSubjectDAO, "coordinateSubjectDAO is required");
        this.teachClassGroupDAO = Objects.requireNonNull(teachClassGroupDAO, "teachClassGroupDAO is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
    }

    public RoleAssignmentService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new PermissionDAO(connectionProvider),
                new ManageOrganizationDAO(connectionProvider),
                new CoordinateSubjectDAO(connectionProvider),
                new TeachClassGroupDAO(connectionProvider),
                new AuditService(connectionProvider, clock)
        );
    }

    public void assignAdministratorToOrganization(
            long actorUserId,
            Long sessionId,
            long adminUserId,
            long organizationId,
            LocalDate startDate,
            LocalDate endDate,
            String sourceIp
    ) {
        try {
            requireRoleAssignmentManager(actorUserId);
            requireValidDates(startDate, endDate);
            if (!manageOrganizationDAO.canAssign(adminUserId, organizationId)) {
                throw new IllegalArgumentException("Administrator assignment requires active administrator and active organization");
            }
            manageOrganizationDAO.assign(adminUserId, organizationId, startDate, endDate);
            audit(actorUserId, sessionId, "ROLE_ASSIGN_MANAGE_ORGANIZATION", adminUserId, organizationId, "success", sourceIp);
        } catch (RuntimeException | SQLException exception) {
            audit(actorUserId, sessionId, "ROLE_ASSIGN_MANAGE_ORGANIZATION", adminUserId, organizationId, "failure", sourceIp);
            throw wrap(exception, "Failed to assign administrator to organization");
        }
    }

    public void assignCoordinatorToSubject(
            long actorUserId,
            Long sessionId,
            long coordinatorUserId,
            long subjectId,
            String sourceIp
    ) {
        try {
            requireRoleAssignmentManager(actorUserId);
            if (!coordinateSubjectDAO.canAssign(coordinatorUserId, subjectId)) {
                throw new IllegalArgumentException("Subject assignment requires active coordinator and existing subject");
            }
            coordinateSubjectDAO.assign(coordinatorUserId, subjectId);
            audit(actorUserId, sessionId, "ROLE_ASSIGN_COORDINATE_SUBJECT", coordinatorUserId, subjectId, "success", sourceIp);
        } catch (RuntimeException | SQLException exception) {
            audit(actorUserId, sessionId, "ROLE_ASSIGN_COORDINATE_SUBJECT", coordinatorUserId, subjectId, "failure", sourceIp);
            throw wrap(exception, "Failed to assign coordinator to subject");
        }
    }

    public void assignTeacherToClassGroup(
            long actorUserId,
            Long sessionId,
            long teacherUserId,
            long classGroupId,
            LocalDate startDate,
            LocalDate endDate,
            String sourceIp
    ) {
        try {
            requireRoleAssignmentManager(actorUserId);
            requireValidDates(startDate, endDate);
            if (!teachClassGroupDAO.canAssign(teacherUserId, classGroupId)) {
                throw new IllegalArgumentException("Class group assignment requires active teacher and active class group");
            }
            teachClassGroupDAO.assign(teacherUserId, classGroupId, startDate, endDate);
            audit(actorUserId, sessionId, "ROLE_ASSIGN_TEACH_CLASS_GROUP", teacherUserId, classGroupId, "success", sourceIp);
        } catch (RuntimeException | SQLException exception) {
            audit(actorUserId, sessionId, "ROLE_ASSIGN_TEACH_CLASS_GROUP", teacherUserId, classGroupId, "failure", sourceIp);
            throw wrap(exception, "Failed to assign teacher to class group");
        }
    }

    public boolean administratorManagesOrganization(long adminUserId, long organizationId) {
        try {
            return manageOrganizationDAO.hasActiveAssignment(adminUserId, organizationId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check organization assignment", exception);
        }
    }

    public boolean coordinatorCoordinatesSubject(long coordinatorUserId, long subjectId) {
        try {
            return coordinateSubjectDAO.hasActiveAssignment(coordinatorUserId, subjectId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check subject assignment", exception);
        }
    }

    public boolean teacherTeachesClassGroup(long teacherUserId, long classGroupId) {
        try {
            return teachClassGroupDAO.hasActiveAssignment(teacherUserId, classGroupId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check class group assignment", exception);
        }
    }

    private void requireRoleAssignmentManager(long actorUserId) throws SQLException {
        if (!permissionDAO.activeProfileExists(actorUserId, AccessProfileType.ADMINISTRATOR)
                || !permissionDAO.hasActiveGrant(actorUserId, AccessProfileType.ADMINISTRATOR, AuthorizationPolicy.MANAGE_USERS)) {
            throw new SecurityException("Only administrators with MANAGE_USERS can assign contextual roles");
        }
    }

    private void audit(
            long actorUserId,
            Long sessionId,
            String operationType,
            long targetUserId,
            long entityId,
            String outcome,
            String sourceIp
    ) {
        auditService.record(
                actorUserId,
                sessionId,
                operationType,
                "role_assignment",
                targetUserId + ":" + entityId,
                outcome,
                sourceIp
        );
    }

    private static void requireValidDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Assignment end date cannot be before start date");
        }
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
