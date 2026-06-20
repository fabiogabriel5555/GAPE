package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.validation.AcademicTextValidator;
import pt.isel.gape.common.validation.MediaPathValidator;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseSubjectAssociationCommand;
import pt.isel.gape.learning.model.CourseSubjectState;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectCreateCommand;
import pt.isel.gape.learning.model.SubjectInitialCourseAssignment;
import pt.isel.gape.learning.model.SubjectState;
import pt.isel.gape.learning.model.SubjectUpdateCommand;
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationDecision;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.structure.model.OrganizationState;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class SubjectService {

    private final ConnectionProvider connectionProvider;
    private final SubjectDAO subjectDAO;
    private final CourseDAO courseDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final OrganizationDAO organizationDAO;
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
        this(
                connectionProvider,
                subjectDAO,
                new CourseDAO(connectionProvider),
                new CourseSubjectDAO(connectionProvider),
                organizationDAO,
                coordinateSubjectDAO,
                permissionChecker,
                auditService,
                clock
        );
    }

    public SubjectService(
            ConnectionProvider connectionProvider,
            SubjectDAO subjectDAO,
            CourseDAO courseDAO,
            CourseSubjectDAO courseSubjectDAO,
            OrganizationDAO organizationDAO,
            CoordinateSubjectDAO coordinateSubjectDAO,
            PermissionChecker permissionChecker,
            AuditService auditService,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.subjectDAO = Objects.requireNonNull(subjectDAO, "subjectDAO is required");
        this.courseDAO = Objects.requireNonNull(courseDAO, "courseDAO is required");
        this.courseSubjectDAO = Objects.requireNonNull(courseSubjectDAO, "courseSubjectDAO is required");
        this.organizationDAO = Objects.requireNonNull(organizationDAO, "organizationDAO is required");
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
                    validateOrganization(connection, command.organizationId());
                    validateInitialCourses(connection, command);
                    long subjectId = subjectDAO.create(connection, command);
                    for (CourseSubjectAssociationCommand association : initialAssociationCommands(command, subjectId)) {
                        courseSubjectDAO.create(connection, association);
                    }
                    for (long coordinatorUserId : safeCoordinators(command.coordinatorUserIds())) {
                        assignCoordinatorInternal(connection, coordinatorUserId, subjectId, LocalDate.now(clock), null);
                    }
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
                validateOrganization(connection, organizationId);
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
                    validateOrganization(connection, current.organizationId());
                    requireNotArchived(current);
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
                    requireNotArchived(current);
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
            LocalDate startDate,
            LocalDate endDate,
            String sourceIp
    ) {
        try {
            requireValidDates(startDate, endDate);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Subject subject = requireSubject(connection, subjectId);
                    requireSubjectAdministrator(actorUserId, sessionId, actorProfileType, subject.organizationId(), sourceIp);
                    requireNotArchived(subject);
                    assignCoordinatorInternal(connection, coordinatorUserId, subjectId, startDate, endDate);
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
                    requireNotArchived(subject);
                    subjectDAO.updateState(connection, subjectId, SubjectState.ARCHIVED);
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
                    requireNotArchived(subject);
                    if (subjectDAO.hasDomainDependencies(connection, subjectId)) {
                        throw new IllegalStateException("Subject with domain dependencies cannot be deleted");
                    }
                    subjectDAO.delete(connection, subjectId);
                    auditService.record(connection, actorUserId, sessionId, "SUBJECT_DELETE",
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
            long subjectId,
            LocalDate startDate,
            LocalDate endDate
    ) throws SQLException {
        requireValidDates(startDate, endDate);
        if (!coordinateSubjectDAO.canAssign(connection, coordinatorUserId, subjectId)) {
            throw new IllegalArgumentException("Subject assignment requires active coordinator and non-archived subject");
        }
        coordinateSubjectDAO.assign(connection, coordinatorUserId, subjectId, startDate, endDate);
    }

    private void validateOrganization(Connection connection, long organizationId) throws SQLException {
        Organization organization = organizationDAO.findById(connection, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Subject organization not found: " + organizationId));
        if (organization.state() == OrganizationState.ARCHIVED) {
            throw new IllegalStateException("Archived organizations cannot receive subjects");
        }
    }

    private void validateInitialCourses(Connection connection, SubjectCreateCommand command) throws SQLException {
        for (SubjectInitialCourseAssignment assignment : initialCourseAssignments(command)) {
            Course course = courseDAO.findById(connection, assignment.courseId())
                    .orElseThrow(() -> new IllegalArgumentException("Initial course not found: " + assignment.courseId()));
            if (course.organizationId() != command.organizationId()) {
                throw new IllegalArgumentException("Initial course must belong to the subject organization");
            }
            if (course.state() == CourseState.ARCHIVED) {
                throw new IllegalStateException("Archived courses cannot receive subject associations");
            }
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
        if (actorProfileType != AccessProfileType.COORDINATOR) {
            return adminDecision;
        }
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
            boolean hasActiveAssociation = false;
            AuthorizationDecision lastDeniedDecision = AuthorizationDecision.deny("no_active_subject_course_association");
            for (CourseSubjectAssociation association : courseSubjectDAO.findBySubject(subjectId)) {
                if (association.state() == CourseSubjectState.ARCHIVED) {
                    continue;
                }
                hasActiveAssociation = true;
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
            return hasActiveAssociation
                    ? lastDeniedDecision
                    : AuthorizationDecision.deny("no_active_subject_course_association");
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
        AcademicTextValidator.requireName(command.name(), "Subject name is required");
        AcademicTextValidator.requireAcronym(command.acronym(), "Subject acronym is required");
        requireNoInitialPhoto(command.photo(), "Subject photo");
        Objects.requireNonNull(command.state(), "subject state is required");
        if (command.state() == SubjectState.ARCHIVED) {
            throw new IllegalArgumentException("Use the archive operation to archive subjects");
        }
        List<SubjectInitialCourseAssignment> assignments = initialCourseAssignments(command);
        if (assignments.isEmpty()) {
            throw new IllegalArgumentException("At least one initial course is required");
        }
        for (SubjectInitialCourseAssignment assignment : assignments) {
            if (assignment.courseId() <= 0) {
                throw new IllegalArgumentException("Initial course is required");
            }
            if ((assignment.curricularYear() == null) != (assignment.term() == null)) {
                throw new IllegalArgumentException("Curricular year and term must be provided together");
            }
            if (assignment.curricularYear() != null && assignment.curricularYear() <= 0) {
                throw new IllegalArgumentException("Curricular year must be positive");
            }
        }
    }

    private static void validateUpdateCommand(SubjectUpdateCommand command) {
        Objects.requireNonNull(command, "command is required");
        AcademicTextValidator.requireName(command.name(), "Subject name is required");
        AcademicTextValidator.requireAcronym(command.acronym(), "Subject acronym is required");
        MediaPathValidator.optionalSafeRelativePath(command.photo(), "Subject photo");
        Objects.requireNonNull(command.state(), "subject state is required");
        if (command.state() == SubjectState.ARCHIVED) {
            throw new IllegalArgumentException("Use the archive operation to archive subjects");
        }
    }

    private static Set<Long> safeCoordinators(Set<Long> coordinatorUserIds) {
        return coordinatorUserIds == null ? Set.of() : coordinatorUserIds;
    }

    private static List<CourseSubjectAssociationCommand> initialAssociationCommands(
            SubjectCreateCommand command,
            long subjectId
    ) {
        return initialCourseAssignments(command).stream()
                .map(assignment -> new CourseSubjectAssociationCommand(
                        assignment.courseId(),
                        subjectId,
                        assignment.curricularYear(),
                        assignment.term(),
                        assignment.mandatory(),
                        CourseSubjectState.ACTIVE
                ))
                .toList();
    }

    private static List<SubjectInitialCourseAssignment> initialCourseAssignments(SubjectCreateCommand command) {
        if (command.initialCourseAssignments() == null || command.initialCourseAssignments().isEmpty()) {
            return List.of(new SubjectInitialCourseAssignment(
                    command.initialCourseId(),
                    command.initialCurricularYear(),
                    command.initialTerm(),
                    command.initialMandatory()
            ));
        }
        return command.initialCourseAssignments().stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private static void requireNotArchived(Subject subject) {
        if (subject.state() == SubjectState.ARCHIVED) {
            throw new IllegalStateException("Archived subjects cannot be changed");
        }
    }

    private static void requireNoInitialPhoto(String value, String fieldLabel) {
        if (value != null && !value.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + " must be uploaded after the record is created");
        }
    }

    private static void requireValidDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Assignment end date cannot be before start date");
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
