package pt.isel.gape.learning.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.validation.AcademicTextValidator;
import pt.isel.gape.common.validation.MediaPathValidator;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectCreateCommand;
import pt.isel.gape.learning.model.SubjectState;
import pt.isel.gape.learning.model.SubjectUpdateCommand;
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationDecision;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.OrganicUnitState;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.structure.model.OrganizationState;
import pt.isel.gape.structure.model.RoleAssignmentState;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class SubjectService {

    private final ConnectionProvider connectionProvider;
    private final SubjectDAO subjectDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final OrganizationDAO organizationDAO;
    private final OrganicUnitDAO organicUnitDAO;
    private final CoordinateSubjectDAO coordinateSubjectDAO;
    private final PermissionChecker permissionChecker;
    private final AuditService auditService;
    private final Clock clock;

    public SubjectService(
            ConnectionProvider connectionProvider,
            SubjectDAO subjectDAO,
            OrganizationDAO organizationDAO,
            CoordinateSubjectDAO coordinateSubjectDAO,
            PermissionChecker permissionChecker,
            AuditService auditService,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.subjectDAO = Objects.requireNonNull(subjectDAO, "subjectDAO is required");
        this.courseSubjectDAO = new CourseSubjectDAO(connectionProvider);
        this.organizationDAO = Objects.requireNonNull(organizationDAO, "organizationDAO is required");
        this.organicUnitDAO = new OrganicUnitDAO(connectionProvider);
        this.coordinateSubjectDAO = Objects.requireNonNull(coordinateSubjectDAO, "coordinateSubjectDAO is required");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public SubjectService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new SubjectDAO(connectionProvider),
                new OrganizationDAO(connectionProvider),
                new CoordinateSubjectDAO(connectionProvider),
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

    public Subject createSubject(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            SubjectCreateCommand command,
            String sourceIp
    ) {
        try {
            validateCreateCommand(command);
            requireSubjectAdministrator(actorUserId, sessionId, actorProfileType, command.organizationId(), sourceIp);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    validateOrganizationAndUnit(connection, command.organizationId(), command.organicUnitId());
                    long subjectId = subjectDAO.create(connection, command);
                    auditService.record(connection, actorUserId, sessionId, "SUBJECT_CREATE",
                            "subject", Long.toString(subjectId), "success", sourceIp);
                    connection.commit();
                    return subjectDAO.findById(subjectId)
                            .orElseThrow(() -> new IllegalStateException("Created subject was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "SUBJECT_CREATE", "new", sourceIp);
            throw wrap(exception, "Failed to create subject");
        }
    }

    public Subject getSubject(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long subjectId,
            String sourceIp
    ) {
        try {
            Subject subject = requireSubject(subjectId);
            requireSubjectManager(actorUserId, sessionId, actorProfileType, subject, sourceIp);
            return subject;
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to read subject");
        }
    }

    public List<Subject> listSubjects(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String sourceIp
    ) {
        try {
            if (subjectAdministrationDecision(actorUserId, sessionId, actorProfileType, organizationId, sourceIp).allowed()) {
                return subjectDAO.findByOrganization(organizationId);
            }
            try (Connection connection = connectionProvider.getConnection()) {
                validateOrganizationAndUnit(connection, organizationId, null);
            }
            return subjectDAO.findByOrganization(organizationId)
                    .stream()
                    .filter(subject -> subjectAccessDecision(
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            subject,
                            sourceIp
                    ).allowed() || subjectMutationDecision(
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            subject,
                            sourceIp
                    ).allowed())
                    .toList();
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list subjects");
        }
    }

    public boolean canCreateSubject(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String sourceIp
    ) {
        return subjectAdministrationDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                organizationId,
                sourceIp
        ).allowed();
    }

    public Subject updateSubject(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long subjectId,
            SubjectUpdateCommand command,
            String sourceIp
    ) {
        try {
            validateUpdateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Subject current = requireSubject(connection, subjectId);
                    requireSubjectMutationManager(actorUserId, sessionId, actorProfileType, current, sourceIp);
                    validateOrganizationAndUnit(connection, current.organizationId(), command.organicUnitId());
                    MediaPathValidator.optionalEntityProfilePath(command.photo(), "subjects", subjectId, "Subject photo");
                    subjectDAO.update(connection, subjectId, command);
                    auditService.record(connection, actorUserId, sessionId, "SUBJECT_UPDATE",
                            "subject", Long.toString(subjectId), "success", sourceIp);
                    connection.commit();
                    return subjectDAO.findById(subjectId)
                            .orElseThrow(() -> new IllegalStateException("Updated subject was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "SUBJECT_UPDATE", Long.toString(subjectId), sourceIp);
            throw wrap(exception, "Failed to update subject");
        }
    }

    public Subject attachCreatedSubjectPhoto(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long subjectId,
            String photo,
            String sourceIp
    ) {
        try {
            String normalizedPhoto = MediaPathValidator.optionalEntityProfilePath(
                    photo,
                    "subjects",
                    subjectId,
                    "Subject photo"
            );
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Subject current = requireSubject(connection, subjectId);
                    requireSubjectMutationManager(actorUserId, sessionId, actorProfileType, current, sourceIp);
                    subjectDAO.updatePhoto(connection, subjectId, normalizedPhoto);
                    auditService.record(connection, actorUserId, sessionId, "SUBJECT_UPDATE",
                            "subject", Long.toString(subjectId), "success", sourceIp);
                    connection.commit();
                    return subjectDAO.findById(subjectId)
                            .orElseThrow(() -> new IllegalStateException("Updated subject was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "SUBJECT_UPDATE", Long.toString(subjectId), sourceIp);
            throw wrap(exception, "Failed to attach subject photo");
        }
    }

    public void assignCoordinator(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long subjectId,
            long coordinatorUserId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Subject subject = requireSubject(connection, subjectId);
                    requireSubjectAdministrator(actorUserId, sessionId, actorProfileType, subject.organizationId(), sourceIp);
                    if (subject.state() != SubjectState.ACTIVE) {
                        throw new IllegalStateException("Inactive subjects cannot receive new coordinator assignments");
                    }
                    assignCoordinatorInternal(connection, coordinatorUserId, subjectId);
                    auditService.record(connection, actorUserId, sessionId, "SUBJECT_ASSIGN_COORDINATOR",
                            "subject", subjectId + ":" + coordinatorUserId, "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "SUBJECT_ASSIGN_COORDINATOR",
                    subjectId + ":" + coordinatorUserId, sourceIp);
            throw wrap(exception, "Failed to assign coordinator to subject");
        }
    }

    public void updateCoordinatorAssignment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long subjectId,
            long coordinatorUserId,
            RoleAssignmentState state,
            String sourceIp
    ) {
        Objects.requireNonNull(state, "state is required");
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Subject subject = requireSubject(connection, subjectId);
                    requireSubjectAdministrator(actorUserId, sessionId, actorProfileType, subject.organizationId(), sourceIp);
                    if (state == RoleAssignmentState.ACTIVE && subject.state() != SubjectState.ACTIVE) {
                        throw new IllegalStateException("Inactive subjects cannot receive new coordinator assignments");
                    }
                    if (state == RoleAssignmentState.ACTIVE
                            && !coordinateSubjectDAO.canAssign(connection, coordinatorUserId, subjectId)) {
                        throw new IllegalArgumentException("Subject assignment requires active coordinator and active subject");
                    }
                    coordinateSubjectDAO.updateAssignment(connection, coordinatorUserId, subjectId, state);
                    auditService.record(connection, actorUserId, sessionId, "SUBJECT_COORDINATOR_UPDATE",
                            "subject", subjectId + ":" + coordinatorUserId, "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "SUBJECT_COORDINATOR_UPDATE",
                    subjectId + ":" + coordinatorUserId, sourceIp);
            throw wrap(exception, "Failed to update coordinator assignment");
        }
    }

    public void removeCoordinatorAssignment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long subjectId,
            long coordinatorUserId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Subject subject = requireSubject(connection, subjectId);
                    requireSubjectAdministrator(actorUserId, sessionId, actorProfileType, subject.organizationId(), sourceIp);
                    coordinateSubjectDAO.deleteAssignment(connection, coordinatorUserId, subjectId);
                    auditService.record(connection, actorUserId, sessionId, "SUBJECT_COORDINATOR_REMOVE",
                            "subject", subjectId + ":" + coordinatorUserId, "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "SUBJECT_COORDINATOR_REMOVE",
                    subjectId + ":" + coordinatorUserId, sourceIp);
            throw wrap(exception, "Failed to remove coordinator assignment");
        }
    }

    public void archiveSubject(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long subjectId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Subject subject = requireSubject(connection, subjectId);
                    requireSubjectMutationManager(actorUserId, sessionId, actorProfileType, subject, sourceIp);
                    subjectDAO.updateState(connection, subjectId, SubjectState.INACTIVE);
                    auditService.record(connection, actorUserId, sessionId, "SUBJECT_ARCHIVE",
                            "subject", Long.toString(subjectId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "SUBJECT_ARCHIVE", Long.toString(subjectId), sourceIp);
            throw wrap(exception, "Failed to archive subject");
        }
    }

    public void unarchiveSubject(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long subjectId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Subject subject = requireSubject(connection, subjectId);
                    requireSubjectMutationManager(actorUserId, sessionId, actorProfileType, subject, sourceIp);
                    if (subject.state() != SubjectState.INACTIVE) {
                        throw new IllegalStateException("Only inactive subjects can be activated");
                    }
                    validateOrganizationAndUnit(connection, subject.organizationId(), subject.organicUnitId());
                    subjectDAO.updateState(connection, subjectId, SubjectState.ACTIVE);
                    auditService.record(connection, actorUserId, sessionId, "SUBJECT_UNARCHIVE",
                            "subject", Long.toString(subjectId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "SUBJECT_UNARCHIVE", Long.toString(subjectId), sourceIp);
            throw wrap(exception, "Failed to unarchive subject");
        }
    }

    public void deleteSubject(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long subjectId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Subject subject = requireSubject(connection, subjectId);
                    requireSubjectMutationManager(actorUserId, sessionId, actorProfileType, subject, sourceIp);
                    if (subjectDAO.hasDomainDependencies(connection, subjectId)) {
                        throw new IllegalStateException("Subject with domain dependencies cannot be deleted");
                    }
                    auditService.record(connection, actorUserId, sessionId, "SUBJECT_DELETE",
                            "subject", Long.toString(subjectId), "success", sourceIp);
                    subjectDAO.delete(connection, subjectId);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "SUBJECT_DELETE", Long.toString(subjectId), sourceIp);
            throw wrap(exception, "Failed to delete subject");
        }
    }

    public boolean canModifySubject(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long subjectId,
            String sourceIp
    ) {
        try {
            Subject subject = requireSubject(subjectId);
            return subjectMutationDecision(actorUserId, sessionId, actorProfileType, subject, sourceIp).allowed();
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to check subject modification permission");
        }
    }

    private void assignCoordinatorInternal(
            Connection connection,
            long coordinatorUserId,
            long subjectId
    ) throws SQLException {
        if (!coordinateSubjectDAO.canAssign(connection, coordinatorUserId, subjectId)) {
            throw new IllegalArgumentException("Subject assignment requires active coordinator and active subject");
        }
        coordinateSubjectDAO.assign(connection, coordinatorUserId, subjectId);
    }

    private void validateOrganizationAndUnit(
            Connection connection,
            long organizationId,
            Long organicUnitId
    ) throws SQLException {
        Organization organization = organizationDAO.findById(connection, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Subject organization not found: " + organizationId));
        if (organization.state() != OrganizationState.ACTIVE) {
            throw new IllegalStateException("Inactive organizations cannot receive subjects");
        }
        if (organicUnitId == null) {
            return;
        }
        OrganicUnit unit = organicUnitDAO.findById(connection, organicUnitId)
                .orElseThrow(() -> new IllegalArgumentException("Subject organic unit not found: " + organicUnitId));
        if (unit.organizationId() != organizationId) {
            throw new IllegalArgumentException("Subject organic unit must belong to the same organization");
        }
        if (unit.state() != OrganicUnitState.ACTIVE) {
            throw new IllegalStateException("Inactive organic units cannot receive subjects");
        }
    }

    private void requireSubjectManager(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            Subject subject,
            String sourceIp
    ) {
        AuthorizationDecision decision = subjectAccessDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                subject,
                sourceIp
        );
        if (decision.allowed()) {
            return;
        }
        throw new SecurityException("Missing subject management context: " + decision.reason());
    }

    private AuthorizationDecision subjectAccessDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            Subject subject,
            String sourceIp
    ) {
        AuthorizationDecision adminDecision = permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_SUBJECTS,
                AccessEntityType.SUBJECT,
                subject.id(),
                sourceIp
        ));
        if (adminDecision.allowed()) {
            return adminDecision;
        }
        if (actorProfileType == AccessProfileType.COORDINATOR) {
            AuthorizationDecision coordinatorDecision = permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_SUBJECTS,
                    AccessEntityType.SUBJECT,
                    subject.id(),
                    sourceIp
            ));
            return coordinatorDecision.allowed()
                    ? coordinatorDecision
                    : AuthorizationDecision.deny(adminDecision.reason() + "/" + coordinatorDecision.reason());
        }
        if (actorProfileType == AccessProfileType.TEACHER) {
            try (Connection connection = connectionProvider.getConnection()) {
                return subjectDAO.teacherCanReadSubject(connection, actorUserId, subject.id())
                        ? AuthorizationDecision.allow()
                        : AuthorizationDecision.deny(adminDecision.reason() + "/teacher_subject_context_required");
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to check teacher subject context", exception);
            }
        }
        return adminDecision;
    }

    private void requireSubjectMutationManager(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            Subject subject,
            String sourceIp
    ) {
        AuthorizationDecision decision = subjectMutationDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                subject,
                sourceIp
        );
        if (!decision.allowed()) {
            throw new SecurityException("Missing subject ancestor context: " + decision.reason());
        }
    }

    private AuthorizationDecision subjectMutationDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            Subject subject,
            String sourceIp
    ) {
        if (actorProfileType == AccessProfileType.COORDINATOR) {
            AuthorizationDecision coordinatorDecision = permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_SUBJECTS,
                    AccessEntityType.SUBJECT,
                    subject.id(),
                    sourceIp
            ));
            return coordinatorDecision.allowed()
                    ? coordinatorDecision
                    : AuthorizationDecision.deny(coordinatorDecision.reason());
        }
        if (actorProfileType != AccessProfileType.ADMINISTRATOR) {
            return AuthorizationDecision.deny("administrator_profile_required_for_subject_mutation");
        }
        AuthorizationDecision descendantDecision = permissionChecker.checkDescendant(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_SUBJECTS,
                AccessEntityType.SUBJECT,
                subject.id(),
                sourceIp
        ));
        if (descendantDecision.allowed()) {
            return descendantDecision;
        }
        AuthorizationDecision exactSubjectDecision = permissionChecker.checkExactAdministratorContext(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_SUBJECTS,
                AccessEntityType.SUBJECT,
                subject.id(),
                sourceIp
        ));
        if (exactSubjectDecision.allowed()) {
            return exactSubjectDecision;
        }
        AuthorizationDecision exclusiveCourseDecision = exclusiveCourseSubjectMutationDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                subject.id(),
                sourceIp
        );
        return exclusiveCourseDecision.allowed()
                ? exclusiveCourseDecision
                : AuthorizationDecision.deny(descendantDecision.reason()
                + "/" + exactSubjectDecision.reason()
                + "/" + exclusiveCourseDecision.reason());
    }

    private AuthorizationDecision exclusiveCourseSubjectMutationDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long subjectId,
            String sourceIp
    ) {
        try {
            boolean hasAssociation = false;
            AuthorizationDecision lastDeniedDecision = AuthorizationDecision.deny("no_subject_course_association");
            for (CourseSubjectAssociation association : courseSubjectDAO.findBySubject(subjectId)) {
                hasAssociation = true;
                AuthorizationDecision courseDecision = courseMutationContextDecision(
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        association.courseId(),
                        sourceIp
                );
                if (!courseDecision.allowed()) {
                    return AuthorizationDecision.deny("subject_association_outside_context/" + courseDecision.reason());
                }
                lastDeniedDecision = courseDecision;
            }
            return hasAssociation
                    ? lastDeniedDecision
                    : AuthorizationDecision.deny("no_subject_course_association");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check subject course context", exception);
        }
    }

    private AuthorizationDecision courseMutationContextDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            String sourceIp
    ) {
        AuthorizationDecision descendantDecision = permissionChecker.checkDescendant(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_COURSES,
                AccessEntityType.COURSE,
                courseId,
                sourceIp
        ));
        if (descendantDecision.allowed()) {
            return descendantDecision;
        }
        AuthorizationDecision exactDecision = permissionChecker.checkExactAdministratorContext(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_COURSES,
                AccessEntityType.COURSE,
                courseId,
                sourceIp
        ));
        return exactDecision.allowed()
                ? exactDecision
                : AuthorizationDecision.deny(descendantDecision.reason() + "/" + exactDecision.reason());
    }

    private void requireSubjectAdministrator(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String sourceIp
    ) {
        AuthorizationDecision decision = subjectAdministrationDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                organizationId,
                sourceIp
        );
        if (!decision.allowed()) {
            throw new SecurityException("Missing subject administration context: " + decision.reason());
        }
    }

    private AuthorizationDecision subjectAdministrationDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String sourceIp
    ) {
        return permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_SUBJECTS,
                AccessEntityType.ORGANIZATION,
                organizationId,
                sourceIp
        ));
    }

    private Subject requireSubject(long subjectId) throws SQLException {
        return subjectDAO.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));
    }

    private Subject requireSubject(Connection connection, long subjectId) throws SQLException {
        return subjectDAO.findById(connection, subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));
    }

    private static void validateCreateCommand(SubjectCreateCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.organizationId() <= 0) {
            throw new IllegalArgumentException("Subject organization is required");
        }
        if (command.organicUnitId() != null && command.organicUnitId() <= 0) {
            throw new IllegalArgumentException("Subject organic unit is invalid");
        }
        AcademicTextValidator.requireName(command.name(), "Subject name is required");
        AcademicTextValidator.requireAcronym(command.acronym(), "Subject acronym is required");
        requireNoInitialPhoto(command.photo(), "Subject photo");
        Objects.requireNonNull(command.state(), "subject state is required");
        requirePositiveDecimal(command.ects(), "Subject ECTS is required");
        requirePositiveDecimal(command.finalGradeMax(), "Subject max final grade is required");
    }

    private static void validateUpdateCommand(SubjectUpdateCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.organicUnitId() != null && command.organicUnitId() <= 0) {
            throw new IllegalArgumentException("Subject organic unit is invalid");
        }
        AcademicTextValidator.requireName(command.name(), "Subject name is required");
        AcademicTextValidator.requireAcronym(command.acronym(), "Subject acronym is required");
        MediaPathValidator.optionalSafeRelativePath(command.photo(), "Subject photo");
        Objects.requireNonNull(command.state(), "subject state is required");
        requirePositiveDecimal(command.ects(), "Subject ECTS is required");
        requirePositiveDecimal(command.finalGradeMax(), "Subject max final grade is required");
    }

    private static void requirePositiveDecimal(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNoInitialPhoto(String value, String fieldLabel) {
        if (value != null && !value.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + " must be uploaded after the record is created");
        }
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
                "subject", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
