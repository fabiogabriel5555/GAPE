package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseSubjectAssociationCommand;
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

public final class CourseSubjectService {

    private final ConnectionProvider connectionProvider;
    private final CourseDAO courseDAO;
    private final SubjectDAO subjectDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final PermissionChecker permissionChecker;
    private final AuditService auditService;

    public CourseSubjectService(
            ConnectionProvider connectionProvider,
            CourseDAO courseDAO,
            SubjectDAO subjectDAO,
            CourseSubjectDAO courseSubjectDAO,
            PermissionChecker permissionChecker,
            AuditService auditService
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.courseDAO = Objects.requireNonNull(courseDAO, "courseDAO is required");
        this.subjectDAO = Objects.requireNonNull(subjectDAO, "subjectDAO is required");
        this.courseSubjectDAO = Objects.requireNonNull(courseSubjectDAO, "courseSubjectDAO is required");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
    }

    public CourseSubjectService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new CourseDAO(connectionProvider),
                new SubjectDAO(connectionProvider),
                new CourseSubjectDAO(connectionProvider),
                new PermissionChecker(
                        new PermissionDAO(connectionProvider),
                        new ManageOrganizationDAO(connectionProvider),
                        new CoordinateSubjectDAO(connectionProvider),
                        new TeachClassGroupDAO(connectionProvider)
                ),
                new AuditService(new ActivityLogDAO(connectionProvider), clock)
        );
    }

    public CourseSubjectAssociation associateSubject(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            CourseSubjectAssociationCommand command,
            String sourceIp
    ) {
        try {
            validateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course course = requireCourse(connection, command.courseId());
                    Subject subject = requireSubject(connection, command.subjectId());
                    requireCourseSubjectManager(actorUserId, sessionId, actorProfileType,
                            course.id(), subject.id(), sourceIp);
                    validateActiveContext(course, subject);
                    validateCurricularYearWithinCourse(command, course);
                    CourseSubjectAssociation existing = courseSubjectDAO
                            .findAnyByCourseAndSubject(connection, command.courseId(), command.subjectId())
                            .orElse(null);
                    if (existing != null && existing.isActive()) {
                        throw new IllegalArgumentException("Course-subject association already exists");
                    }
                    if (existing != null) {
                        courseSubjectDAO.reactivate(connection, command);
                    } else {
                        courseSubjectDAO.create(connection, command);
                    }
                    auditService.record(connection, actorUserId, sessionId, "COURSE_SUBJECT_ASSOCIATE",
                            "course_subject", identifier(command.courseId(), command.subjectId()), "success", sourceIp);
                    connection.commit();
                    return courseSubjectDAO.findByCourseAndSubject(command.courseId(), command.subjectId())
                            .orElseThrow(() -> new IllegalStateException("Created course-subject association not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "COURSE_SUBJECT_ASSOCIATE",
                    identifier(command), sourceIp);
            throw wrap(exception, "Failed to associate subject with course");
        }
    }

    public CourseSubjectAssociation updateAssociation(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            CourseSubjectAssociationCommand command,
            String sourceIp
    ) {
        try {
            validateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course course = requireCourse(connection, command.courseId());
                    Subject subject = requireSubject(connection, command.subjectId());
                    requireAssociation(connection, command.courseId(), command.subjectId());
                    requireCourseSubjectManager(actorUserId, sessionId, actorProfileType,
                            course.id(), subject.id(), sourceIp);
                    validateActiveContext(course, subject);
                    validateCurricularYearWithinCourse(command, course);
                    courseSubjectDAO.update(connection, command);
                    auditService.record(connection, actorUserId, sessionId, "COURSE_SUBJECT_UPDATE",
                            "course_subject", identifier(command.courseId(), command.subjectId()), "success", sourceIp);
                    connection.commit();
                    return courseSubjectDAO.findByCourseAndSubject(command.courseId(), command.subjectId())
                            .orElseThrow(() -> new IllegalStateException("Updated course-subject association not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "COURSE_SUBJECT_UPDATE",
                    identifier(command), sourceIp);
            throw wrap(exception, "Failed to update course-subject association");
        }
    }

    public void deleteAssociation(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            long subjectId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course course = requireCourse(connection, courseId);
                    requireSubject(connection, subjectId);
                    requireAssociation(connection, courseId, subjectId);
                    requireCourseSubjectManager(actorUserId, sessionId, actorProfileType,
                            course.id(), subjectId, sourceIp);
                    courseSubjectDAO.close(connection, courseId, subjectId, LocalDate.now());
                    auditService.record(connection, actorUserId, sessionId, "COURSE_SUBJECT_DELETE",
                            "course_subject", identifier(courseId, subjectId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "COURSE_SUBJECT_DELETE", identifier(courseId, subjectId), sourceIp);
            throw wrap(exception, "Failed to delete course-subject association");
        }
    }

    public boolean canManageAssociation(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            long subjectId,
            String sourceIp
    ) {
        return courseSubjectManagementDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                courseId,
                subjectId,
                sourceIp
        ).allowed();
    }

    public boolean canManageSubjectAssociations(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long subjectId,
            String sourceIp
    ) {
        return subjectAssociationManagementDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                subjectId,
                sourceIp
        ).allowed();
    }

    private void validateActiveContext(Course course, Subject subject) {
        if (course.state() != CourseState.ACTIVE) {
            throw new IllegalStateException("Inactive courses cannot receive subject associations");
        }
        if (subject.state() != SubjectState.ACTIVE) {
            throw new IllegalStateException("Inactive subjects cannot be associated with courses");
        }
    }

    private void requireCourseSubjectManager(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            long subjectId,
            String sourceIp
    ) {
        AuthorizationDecision decision = courseSubjectManagementDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                courseId,
                subjectId,
                sourceIp
        );
        if (!decision.allowed()) {
            throw new SecurityException("Missing course-subject management context: " + decision.reason());
        }
    }

    private AuthorizationDecision courseSubjectManagementDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            long subjectId,
            String sourceIp
    ) {
        AuthorizationDecision courseDecision = courseAssociationManagementDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                courseId,
                sourceIp
        );
        if (courseDecision.allowed()) {
            return courseDecision;
        }
        AuthorizationDecision subjectDecision = subjectAssociationManagementDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                subjectId,
                sourceIp
        );
        return subjectDecision.allowed()
                ? subjectDecision
                : AuthorizationDecision.deny(courseDecision.reason() + "/" + subjectDecision.reason());
    }

    private AuthorizationDecision courseAssociationManagementDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            String sourceIp
    ) {
        if (actorProfileType != AccessProfileType.ADMINISTRATOR) {
            return AuthorizationDecision.deny("administrator_profile_required");
        }
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

    private AuthorizationDecision subjectAssociationManagementDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long subjectId,
            String sourceIp
    ) {
        if (actorProfileType == AccessProfileType.ADMINISTRATOR) {
            AuthorizationDecision descendantDecision = permissionChecker.checkDescendant(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_SUBJECTS,
                    AccessEntityType.SUBJECT,
                    subjectId,
                    sourceIp
            ));
            if (descendantDecision.allowed()) {
                return descendantDecision;
            }
            AuthorizationDecision exactDecision = permissionChecker.checkExactAdministratorContext(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_SUBJECTS,
                    AccessEntityType.SUBJECT,
                    subjectId,
                    sourceIp
            ));
            return exactDecision.allowed()
                    ? exactDecision
                    : AuthorizationDecision.deny(descendantDecision.reason() + "/" + exactDecision.reason());
        }
        AuthorizationDecision coordinatorDecision = permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_SUBJECTS,
                AccessEntityType.SUBJECT,
                subjectId,
                sourceIp
        ));
        return coordinatorDecision.allowed()
                ? coordinatorDecision
                : AuthorizationDecision.deny(coordinatorDecision.reason());
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
                .orElseThrow(() -> new IllegalArgumentException("Course-subject association not found"));
    }

    private static void validateCommand(CourseSubjectAssociationCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.courseId() <= 0) {
            throw new IllegalArgumentException("Association course is required");
        }
        if (command.subjectId() <= 0) {
            throw new IllegalArgumentException("Association subject is required");
        }
        if (command.curricularYear() == null) {
            throw new IllegalArgumentException("Curricular year is required");
        }
        if (command.curricularYear() <= 0) {
            throw new IllegalArgumentException("Curricular year must be positive");
        }
        if (command.term() == null) {
            throw new IllegalArgumentException("Curricular term is required");
        }
    }

    private static void validateCurricularYearWithinCourse(
            CourseSubjectAssociationCommand command,
            Course course
    ) {
        int durationYears = durationYears(course);
        if (durationYears <= 0) {
            throw new IllegalArgumentException("Course duration must be configured before associating subjects");
        }
        if (command.curricularYear() > durationYears) {
            throw new IllegalArgumentException("Curricular year cannot be greater than course duration");
        }
        if (!command.term().belongsTo(course.frequency())) {
            throw new IllegalArgumentException("Curricular term must match the course frequency");
        }
    }

    private static int durationYears(Course course) {
        String duration = course.duration();
        if (duration == null || duration.isBlank() || !duration.trim().matches("\\d+")) {
            return 0;
        }
        try {
            return Integer.parseInt(duration.trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String identifier(long courseId, long subjectId) {
        return courseId + ":" + subjectId;
    }

    private static String identifier(CourseSubjectAssociationCommand command) {
        return command == null ? "new" : identifier(command.courseId(), command.subjectId());
    }

    private void auditFailure(
            long actorUserId,
            Long sessionId,
            String operationType,
            String affectedIdentifier,
            String sourceIp
    ) {
        auditService.record(actorUserId, sessionId, operationType,
                "course_subject", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
