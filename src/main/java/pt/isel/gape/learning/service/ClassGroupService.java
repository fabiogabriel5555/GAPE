package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.validation.AcademicTextValidator;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupCreateCommand;
import pt.isel.gape.learning.model.ClassGroupShift;
import pt.isel.gape.learning.model.ClassGroupState;
import pt.isel.gape.learning.model.ClassGroupUpdateCommand;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseSubjectState;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectState;
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

public final class ClassGroupService {

    private final ConnectionProvider connectionProvider;
    private final ClassGroupDAO classGroupDAO;
    private final CourseDAO courseDAO;
    private final SubjectDAO subjectDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final TeachClassGroupDAO teachClassGroupDAO;
    private final PermissionChecker permissionChecker;
    private final AuditService auditService;
    private final Clock clock;

    public ClassGroupService(
            ConnectionProvider connectionProvider,
            ClassGroupDAO classGroupDAO,
            CourseDAO courseDAO,
            SubjectDAO subjectDAO,
            CourseSubjectDAO courseSubjectDAO,
            TeachClassGroupDAO teachClassGroupDAO,
            PermissionChecker permissionChecker,
            AuditService auditService
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.classGroupDAO = Objects.requireNonNull(classGroupDAO, "classGroupDAO is required");
        this.courseDAO = Objects.requireNonNull(courseDAO, "courseDAO is required");
        this.subjectDAO = Objects.requireNonNull(subjectDAO, "subjectDAO is required");
        this.courseSubjectDAO = Objects.requireNonNull(courseSubjectDAO, "courseSubjectDAO is required");
        this.teachClassGroupDAO = Objects.requireNonNull(teachClassGroupDAO, "teachClassGroupDAO is required");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.clock = Clock.systemDefaultZone();
    }

    public ClassGroupService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new ClassGroupDAO(connectionProvider),
                new CourseDAO(connectionProvider),
                new SubjectDAO(connectionProvider),
                new CourseSubjectDAO(connectionProvider),
                new TeachClassGroupDAO(connectionProvider),
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

    private ClassGroupService(
            ConnectionProvider connectionProvider,
            ClassGroupDAO classGroupDAO,
            CourseDAO courseDAO,
            SubjectDAO subjectDAO,
            CourseSubjectDAO courseSubjectDAO,
            TeachClassGroupDAO teachClassGroupDAO,
            PermissionChecker permissionChecker,
            AuditService auditService,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.classGroupDAO = Objects.requireNonNull(classGroupDAO, "classGroupDAO is required");
        this.courseDAO = Objects.requireNonNull(courseDAO, "courseDAO is required");
        this.subjectDAO = Objects.requireNonNull(subjectDAO, "subjectDAO is required");
        this.courseSubjectDAO = Objects.requireNonNull(courseSubjectDAO, "courseSubjectDAO is required");
        this.teachClassGroupDAO = Objects.requireNonNull(teachClassGroupDAO, "teachClassGroupDAO is required");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public ClassGroup createClassGroup(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ClassGroupCreateCommand command,
            String sourceIp
    ) {
        try {
            LocalDate today = LocalDate.now(clock);
            ClassGroupCreateCommand effectiveCommand = deriveTemporalState(command, today);
            validateCreateCommand(effectiveCommand, today);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    synchronizeTemporalStates(connection);
                    Course course = requireCourse(connection, effectiveCommand.courseId());
                    Subject subject = requireSubject(connection, effectiveCommand.subjectId());
                    CourseSubjectAssociation association = requireAssociation(
                            connection,
                            effectiveCommand.courseId(),
                            effectiveCommand.subjectId()
                    );
                    requireClassGroupCreationManager(
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            course.id(),
                            subject.id(),
                            sourceIp
                    );
                    validateActiveContext(course, subject, association);
                    long classGroupId = classGroupDAO.create(connection, effectiveCommand);
                    synchronizeTemporalStates(connection);
                    auditService.record(connection, actorUserId, sessionId, "CLASS_GROUP_CREATE",
                            "class_group", Long.toString(classGroupId), "success", sourceIp);
                    connection.commit();
                    return classGroupDAO.findById(classGroupId)
                            .orElseThrow(() -> new IllegalStateException("Created class group was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CLASS_GROUP_CREATE", "new", sourceIp);
            throw wrap(exception, "Failed to create class group");
        }
    }

    public ClassGroup getClassGroup(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                synchronizeTemporalStates(connection);
                ClassGroup classGroup = requireClassGroup(connection, classGroupId);
                requireClassGroupOperationalManager(actorUserId, sessionId, actorProfileType, classGroup, sourceIp);
                return classGroup;
            }
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to read class group");
        }
    }

    public List<ClassGroup> listClassGroupsByCourse(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                synchronizeTemporalStates(connection);
            }
            requireCourse(courseId);
            return classGroupDAO.findByCourse(courseId)
                    .stream()
                    .filter(classGroup -> classGroupReadDecision(
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            classGroup,
                            sourceIp
                    ).allowed())
                    .toList();
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list class groups");
        }
    }

    public boolean canReadClassGroup(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                synchronizeTemporalStates(connection);
            }
            ClassGroup classGroup = requireClassGroup(classGroupId);
            return classGroupReadDecision(actorUserId, sessionId, actorProfileType, classGroup, sourceIp).allowed();
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to check class group access");
        }
    }

    public boolean canModifyClassGroup(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                synchronizeTemporalStates(connection);
            }
            ClassGroup classGroup = requireClassGroup(classGroupId);
            return classGroupOperationalDecision(actorUserId, sessionId, actorProfileType, classGroup, sourceIp)
                    .allowed();
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to check class group modification permission");
        }
    }

    public boolean canManageClassGroupStructure(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                synchronizeTemporalStates(connection);
            }
            ClassGroup classGroup = requireClassGroup(classGroupId);
            return classGroupStructuralDecision(actorUserId, sessionId, actorProfileType, classGroup, sourceIp)
                    .allowed();
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to check class group structure permission");
        }
    }

    public boolean canManageClassGroupEnrollments(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                synchronizeTemporalStates(connection);
            }
            ClassGroup classGroup = requireClassGroup(classGroupId);
            return classGroupEnrollmentDecision(actorUserId, sessionId, actorProfileType, classGroup, sourceIp)
                    .allowed();
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to check class group enrollment permission");
        }
    }

    public boolean canCreateClassGroup(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            long subjectId,
            String sourceIp
    ) {
        try {
            return classGroupCreationDecision(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    courseId,
                    subjectId,
                    sourceIp
            ).allowed();
        } catch (RuntimeException exception) {
            throw wrap(exception, "Failed to check class group creation permission");
        }
    }

    public ClassGroup updateClassGroup(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            ClassGroupUpdateCommand command,
            String sourceIp
    ) {
        try {
            LocalDate today = LocalDate.now(clock);
            ClassGroupUpdateCommand effectiveCommand = deriveTemporalState(command, today);
            validateUpdateCommand(effectiveCommand, today);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    synchronizeTemporalStates(connection);
                    ClassGroup current = requireClassGroup(connection, classGroupId);
                    ensureClassGroupCanBeUpdated(current);
                    requireClassGroupOperationalManager(actorUserId, sessionId, actorProfileType, current, sourceIp);
                    requireClassGroupContextChangeAllowed(actorProfileType, current, effectiveCommand);
                    Course course = requireCourse(connection, effectiveCommand.courseId());
                    Subject subject = requireSubject(connection, effectiveCommand.subjectId());
                    CourseSubjectAssociation association = requireAssociation(
                            connection,
                            effectiveCommand.courseId(),
                            effectiveCommand.subjectId()
                    );
                    validateActiveContext(course, subject, association);
                    long activeEnrollments = classGroupDAO.countActiveEnrollments(connection, classGroupId);
                    if (effectiveCommand.maxStudents() != null && activeEnrollments > effectiveCommand.maxStudents()) {
                        throw new IllegalStateException("Class group max students cannot be below active enrollments");
                    }
                    classGroupDAO.update(connection, classGroupId, effectiveCommand);
                    synchronizeTemporalStates(connection);
                    auditService.record(connection, actorUserId, sessionId, "CLASS_GROUP_UPDATE",
                            "class_group", Long.toString(classGroupId), "success", sourceIp);
                    connection.commit();
                    return classGroupDAO.findById(classGroupId)
                            .orElseThrow(() -> new IllegalStateException("Updated class group was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CLASS_GROUP_UPDATE", Long.toString(classGroupId), sourceIp);
            throw wrap(exception, "Failed to update class group");
        }
    }

    public void archiveClassGroup(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ClassGroup current = requireClassGroup(connection, classGroupId);
                    requireClassGroupStructuralManager(actorUserId, sessionId, actorProfileType, current, sourceIp);
                    classGroupDAO.updateState(connection, classGroupId, ClassGroupState.COMPLETED);
                    auditService.record(connection, actorUserId, sessionId, "CLASS_GROUP_ARCHIVE",
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
            auditFailure(actorUserId, sessionId, "CLASS_GROUP_ARCHIVE", Long.toString(classGroupId), sourceIp);
            throw wrap(exception, "Failed to archive class group");
        }
    }

    public void deleteClassGroup(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ClassGroup current = requireClassGroup(connection, classGroupId);
                    requireClassGroupStructuralManager(actorUserId, sessionId, actorProfileType, current, sourceIp);
                    if (classGroupDAO.hasDomainDependencies(connection, classGroupId)) {
                        throw new IllegalStateException("Class group with domain dependencies cannot be deleted");
                    }
                    classGroupDAO.delete(connection, classGroupId);
                    auditService.record(connection, actorUserId, sessionId, "CLASS_GROUP_DELETE",
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
            auditFailure(actorUserId, sessionId, "CLASS_GROUP_DELETE", Long.toString(classGroupId), sourceIp);
            throw wrap(exception, "Failed to delete class group");
        }
    }

    public void assignTeacherToClassGroup(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            long teacherUserId,
            LocalDate startDate,
            LocalDate endDate,
            String sourceIp
    ) {
        try {
            requireValidDates(startDate, endDate, "Teacher assignment end date cannot be before start date");
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ClassGroup current = requireClassGroup(connection, classGroupId);
                    requireClassGroupStructuralManager(actorUserId, sessionId, actorProfileType, current, sourceIp);
                    if (!teachClassGroupDAO.canAssign(connection, teacherUserId, classGroupId)) {
                        throw new IllegalArgumentException("Teacher assignment requires active teacher and active class group");
                    }
                    teachClassGroupDAO.assign(connection, teacherUserId, classGroupId, startDate, endDate);
                    auditService.record(connection, actorUserId, sessionId, "CLASS_GROUP_ASSIGN_TEACHER",
                            "class_group_teacher", classGroupId + ":" + teacherUserId, "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CLASS_GROUP_ASSIGN_TEACHER",
                    classGroupId + ":" + teacherUserId, sourceIp);
            throw wrap(exception, "Failed to assign teacher to class group");
        }
    }

    public void removeTeacherFromClassGroup(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            long teacherUserId,
            LocalDate endDate,
            String sourceIp
    ) {
        try {
            LocalDate removalDate = endDate == null ? LocalDate.now() : endDate;
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ClassGroup current = requireClassGroup(connection, classGroupId);
                    requireClassGroupStructuralManager(actorUserId, sessionId, actorProfileType, current, sourceIp);
                    if (!teachClassGroupDAO.deactivate(connection, teacherUserId, classGroupId, removalDate)) {
                        throw new IllegalArgumentException("Teacher assignment not found");
                    }
                    auditService.record(connection, actorUserId, sessionId, "CLASS_GROUP_REMOVE_TEACHER",
                            "class_group_teacher", classGroupId + ":" + teacherUserId, "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CLASS_GROUP_REMOVE_TEACHER",
                    classGroupId + ":" + teacherUserId, sourceIp);
            throw wrap(exception, "Failed to remove teacher from class group");
        }
    }

    private Course requireCourse(long courseId) throws SQLException {
        return courseDAO.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
    }

    private Course requireCourse(Connection connection, long courseId) throws SQLException {
        return courseDAO.findById(connection, courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
    }

    private Subject requireSubject(Connection connection, long subjectId) throws SQLException {
        return subjectDAO.findById(connection, subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));
    }

    private CourseSubjectAssociation requireAssociation(
            Connection connection,
            long courseId,
            long subjectId
    ) throws SQLException {
        return courseSubjectDAO.findByCourseAndSubject(connection, courseId, subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject is not integrated in the course"));
    }

    private ClassGroup requireClassGroup(long classGroupId) throws SQLException {
        return classGroupDAO.findById(classGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Class group not found: " + classGroupId));
    }

    private ClassGroup requireClassGroup(Connection connection, long classGroupId) throws SQLException {
        return classGroupDAO.findById(connection, classGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Class group not found: " + classGroupId));
    }

    private void validateActiveContext(
            Course course,
            Subject subject,
            CourseSubjectAssociation association
    ) {
        if (course.organizationId() != subject.organizationId()) {
            throw new IllegalArgumentException("Class group course and subject must belong to the same organization");
        }
        if (course.state() != CourseState.ACTIVE) {
            throw new IllegalStateException("Class groups require an active course");
        }
        if (subject.state() != SubjectState.ACTIVE) {
            throw new IllegalStateException("Class groups require an active subject");
        }
        if (association.state() != CourseSubjectState.ACTIVE) {
            throw new IllegalStateException("Class group requires an active course-subject association");
        }
    }

    private void requireCourseManager(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            String sourceIp
    ) {
        AuthorizationDecision decision = permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_LEARNING,
                AccessEntityType.COURSE,
                courseId,
                sourceIp
        ));
        if (!decision.allowed()) {
            throw new SecurityException("Missing class group course context: " + decision.reason());
        }
    }

    private void requireClassGroupCreationManager(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            long subjectId,
            String sourceIp
    ) {
        AuthorizationDecision decision = classGroupCreationDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                courseId,
                subjectId,
                sourceIp
        );
        if (!decision.allowed()) {
            throw new SecurityException("Missing class group creation context: " + decision.reason());
        }
    }

    private AuthorizationDecision classGroupCreationDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            long subjectId,
            String sourceIp
    ) {
        AuthorizationDecision courseDecision = permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_LEARNING,
                AccessEntityType.COURSE,
                courseId,
                sourceIp
        ));
        if (courseDecision.allowed()) {
            return courseDecision;
        }
        AuthorizationDecision subjectDecision = permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_LEARNING,
                AccessEntityType.SUBJECT,
                subjectId,
                sourceIp
        ));
        if (subjectDecision.allowed()) {
            return subjectDecision;
        }
        return AuthorizationDecision.deny(courseDecision.reason() + "/" + subjectDecision.reason());
    }

    private void requireClassGroupStructuralManager(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ClassGroup classGroup,
            String sourceIp
    ) {
        AuthorizationDecision decision = classGroupStructuralDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                classGroup,
                sourceIp
        );
        if (!decision.allowed()) {
            throw new SecurityException("Missing class group structural context: " + decision.reason());
        }
    }

    private AuthorizationDecision classGroupStructuralDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ClassGroup classGroup,
            String sourceIp
    ) {
        AuthorizationDecision adminDecision = permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_LEARNING,
                AccessEntityType.CLASS_GROUP,
                classGroup.id(),
                sourceIp
        ));
        if (adminDecision.allowed() && actorProfileType == AccessProfileType.ADMINISTRATOR) {
            return adminDecision;
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
                return coordinatorDecision;
            }
        }
        return AuthorizationDecision.deny(adminDecision.reason() + "/" + coordinatorDecision.reason());
    }

    private void requireClassGroupOperationalManager(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ClassGroup classGroup,
            String sourceIp
    ) {
        AuthorizationDecision decision = classGroupOperationalDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                classGroup,
                sourceIp
        );
        if (!decision.allowed()) {
            throw new SecurityException("Missing class group management context: " + decision.reason());
        }
    }

    private AuthorizationDecision classGroupOperationalDecision(
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
            return classGroupDecision;
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
                return coordinatorDecision;
            }
        }
        return AuthorizationDecision.deny(classGroupDecision.reason() + "/" + coordinatorDecision.reason());
    }

    private AuthorizationDecision classGroupEnrollmentDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ClassGroup classGroup,
            String sourceIp
    ) {
        AuthorizationDecision adminDecision = AuthorizationDecision.deny("administrator_profile_required");
        if (actorProfileType == AccessProfileType.ADMINISTRATOR) {
            adminDecision = permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_ENROLLMENTS,
                    AccessEntityType.CLASS_GROUP,
                    classGroup.id(),
                    sourceIp
            ));
            if (adminDecision.allowed()) {
                return adminDecision;
            }
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
                return coordinatorDecision;
            }
        }
        AuthorizationDecision teacherDecision = AuthorizationDecision.deny("teacher_profile_required");
        if (actorProfileType == AccessProfileType.TEACHER) {
            teacherDecision = permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_LEARNING,
                    AccessEntityType.CLASS_GROUP,
                    classGroup.id(),
                    sourceIp
            ));
            if (teacherDecision.allowed()) {
                return teacherDecision;
            }
        }
        return AuthorizationDecision.deny(adminDecision.reason()
                + "/" + coordinatorDecision.reason()
                + "/" + teacherDecision.reason());
    }

    private AuthorizationDecision classGroupReadDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ClassGroup classGroup,
            String sourceIp
    ) {
        AuthorizationDecision learningDecision = classGroupOperationalDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                classGroup,
                sourceIp
        );
        if (learningDecision.allowed()) {
            return learningDecision;
        }
        AuthorizationDecision enrollmentDecision = classGroupEnrollmentDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                classGroup,
                sourceIp
        );
        return enrollmentDecision.allowed()
                ? enrollmentDecision
                : AuthorizationDecision.deny(learningDecision.reason() + "/" + enrollmentDecision.reason());
    }

    private static void validateCreateCommand(ClassGroupCreateCommand command, LocalDate today) {
        Objects.requireNonNull(command, "command is required");
        validateCommonCommand(
                command.courseId(),
                command.subjectId(),
                command.code(),
                command.modality(),
                command.state(),
                command.minStudents(),
                command.maxStudents(),
                command.startsAt(),
                command.endsAt(),
                command.shift(),
                today
        );
    }

    private static void validateUpdateCommand(ClassGroupUpdateCommand command, LocalDate today) {
        Objects.requireNonNull(command, "command is required");
        validateCommonCommand(
                command.courseId(),
                command.subjectId(),
                command.code(),
                command.modality(),
                command.state(),
                command.minStudents(),
                command.maxStudents(),
                command.startsAt(),
                command.endsAt(),
                command.shift(),
                today
        );
    }

    private static ClassGroupCreateCommand deriveTemporalState(
            ClassGroupCreateCommand command,
            LocalDate today
    ) {
        Objects.requireNonNull(command, "command is required");
        return new ClassGroupCreateCommand(
                command.subjectId(),
                command.courseId(),
                command.code(),
                command.modality(),
                deriveTemporalState(command.startsAt(), command.endsAt(), today),
                command.minStudents(),
                command.maxStudents(),
                command.startsAt(),
                command.endsAt(),
                command.shift(),
                command.showContentThumbnails()
        );
    }

    private static ClassGroupUpdateCommand deriveTemporalState(
            ClassGroupUpdateCommand command,
            LocalDate today
    ) {
        Objects.requireNonNull(command, "command is required");
        return new ClassGroupUpdateCommand(
                command.subjectId(),
                command.courseId(),
                command.code(),
                command.modality(),
                deriveTemporalState(command.startsAt(), command.endsAt(), today),
                command.minStudents(),
                command.maxStudents(),
                command.startsAt(),
                command.endsAt(),
                command.shift(),
                command.showContentThumbnails()
        );
    }

    private static ClassGroupState deriveTemporalState(
            LocalDate startsAt,
            LocalDate endsAt,
            LocalDate today
    ) {
        if (startsAt == null) {
            return ClassGroupState.DRAFT;
        }
        if (endsAt != null && !endsAt.isAfter(today)) {
            return ClassGroupState.COMPLETED;
        }
        if (!startsAt.isAfter(today)) {
            return ClassGroupState.ACTIVE;
        }
        return ClassGroupState.SCHEDULED;
    }

    private static void ensureClassGroupCanBeUpdated(ClassGroup classGroup) {
        if (classGroup.state() == ClassGroupState.COMPLETED) {
            throw new IllegalStateException("Completed class groups cannot be changed");
        }
    }

    private static void requireClassGroupContextChangeAllowed(
            AccessProfileType actorProfileType,
            ClassGroup current,
            ClassGroupUpdateCommand command
    ) {
        if (current.courseId() != command.courseId() || current.subjectId() != command.subjectId()) {
            if (actorProfileType != AccessProfileType.ADMINISTRATOR) {
                throw new SecurityException("Only administrators can change class group course and subject");
            }
        }
    }

    private static void validateCommonCommand(
            long courseId,
            long subjectId,
            String code,
            Object modality,
            ClassGroupState state,
            Integer minStudents,
            Integer maxStudents,
            LocalDate startsAt,
            LocalDate endsAt,
            ClassGroupShift shift,
            LocalDate today
    ) {
        if (courseId <= 0) {
            throw new IllegalArgumentException("Class group course is required");
        }
        if (subjectId <= 0) {
            throw new IllegalArgumentException("Class group subject is required");
        }
        requireText(code, "Class group code is required");
        AcademicTextValidator.rejectContextSeparator(code, "Class group code");
        Objects.requireNonNull(modality, "class group modality is required");
        Objects.requireNonNull(state, "class group state is required");
        Objects.requireNonNull(shift, "class group shift is required");
        requireStudentRange(minStudents, maxStudents);
        requireClassGroupTemporalDates(startsAt, endsAt, today);
    }

    private static void requireStudentRange(Integer minStudents, Integer maxStudents) {
        if (minStudents != null && minStudents < 0) {
            throw new IllegalArgumentException("Class group min students cannot be negative");
        }
        if (maxStudents != null && maxStudents < 0) {
            throw new IllegalArgumentException("Class group max students cannot be negative");
        }
        if (minStudents != null && maxStudents != null && minStudents > maxStudents) {
            throw new IllegalArgumentException("Class group min students cannot exceed max students");
        }
    }

    private static void requireClassGroupTemporalDates(LocalDate startDate, LocalDate endDate, LocalDate today) {
        Objects.requireNonNull(today, "today is required");
        if (endDate != null && startDate == null) {
            throw new IllegalArgumentException("Class group end date requires a start date");
        }
        if (startDate != null && startDate.isBefore(today)) {
            throw new IllegalArgumentException("Class group start date cannot be in the past");
        }
        if (endDate != null && endDate.isBefore(today)) {
            throw new IllegalArgumentException("Class group end date cannot be in the past");
        }
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Class group end date cannot be before start date");
        }
    }

    private static void requireValidDates(LocalDate startDate, LocalDate endDate, String message) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(message);
        }
    }

    public int synchronizeTemporalStates() {
        try (Connection connection = connectionProvider.getConnection()) {
            return synchronizeTemporalStates(connection);
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to synchronize class group states");
        }
    }

    private int synchronizeTemporalStates(Connection connection) throws SQLException {
        return classGroupDAO.synchronizeTemporalStates(connection, LocalDate.now(clock));
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
                "class_group", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
