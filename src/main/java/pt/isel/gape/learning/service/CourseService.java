package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.validation.AcademicTextValidator;
import pt.isel.gape.common.validation.MediaPathValidator;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseCreateCommand;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseSubjectState;
import pt.isel.gape.learning.model.CourseUpdateCommand;
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
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class CourseService {

    private final ConnectionProvider connectionProvider;
    private final CourseDAO courseDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final OrganizationDAO organizationDAO;
    private final OrganicUnitDAO organicUnitDAO;
    private final PermissionChecker permissionChecker;
    private final AuditService auditService;

    public CourseService(
            ConnectionProvider connectionProvider,
            CourseDAO courseDAO,
            CourseSubjectDAO courseSubjectDAO,
            OrganizationDAO organizationDAO,
            OrganicUnitDAO organicUnitDAO,
            PermissionChecker permissionChecker,
            AuditService auditService
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.courseDAO = Objects.requireNonNull(courseDAO, "courseDAO is required");
        this.courseSubjectDAO = Objects.requireNonNull(courseSubjectDAO, "courseSubjectDAO is required");
        this.organizationDAO = Objects.requireNonNull(organizationDAO, "organizationDAO is required");
        this.organicUnitDAO = Objects.requireNonNull(organicUnitDAO, "organicUnitDAO is required");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
    }

    public CourseService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new CourseDAO(connectionProvider),
                new CourseSubjectDAO(connectionProvider),
                new OrganizationDAO(connectionProvider),
                new OrganicUnitDAO(connectionProvider),
                new PermissionChecker(
                        new PermissionDAO(connectionProvider),
                        new ManageOrganizationDAO(connectionProvider),
                        new CoordinateSubjectDAO(connectionProvider),
                        new TeachClassGroupDAO(connectionProvider)
                ),
                new AuditService(new ActivityLogDAO(connectionProvider), clock)
        );
    }

    public Course createCourse(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            CourseCreateCommand command,
            String sourceIp
    ) {
        try {
            validateCreateCommand(command);
            requireCourseCreateContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    command.organizationId(),
                    command.organicUnitId(),
                    sourceIp
            );
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    validateOrganizationAndUnit(connection, command.organizationId(), command.organicUnitId());
                    long courseId = courseDAO.create(connection, command);
                    auditService.record(connection, actorUserId, sessionId, "COURSE_CREATE",
                            "course", Long.toString(courseId), "success", sourceIp);
                    connection.commit();
                    return courseDAO.findById(courseId)
                            .orElseThrow(() -> new IllegalStateException("Created course was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "COURSE_CREATE", "new", sourceIp);
            throw wrap(exception, "Failed to create course");
        }
    }

    public Course getCourse(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            String sourceIp
    ) {
        try {
            Course course = requireCourse(courseId);
            requireCourseAccess(actorUserId, sessionId, actorProfileType, course.id(), sourceIp);
            return course;
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to read course");
        }
    }

    public List<Course> listCourses(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String sourceIp
    ) {
        try {
            if (courseManagerDecision(actorUserId, sessionId, actorProfileType, organizationId, sourceIp).allowed()) {
                return courseDAO.findByOrganization(organizationId);
            }
            validateOrganizationExists(organizationId);
            return courseDAO.findByOrganization(organizationId)
                    .stream()
                    .filter(course -> courseAccessDecision(
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            course.id(),
                            sourceIp
                    ).allowed() || courseMutationDecision(
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            course.id(),
                            sourceIp
                    ).allowed())
                    .toList();
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list courses");
        }
    }

    public Course updateCourse(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            CourseUpdateCommand command,
            String sourceIp
    ) {
        try {
            validateUpdateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course current = requireCourse(connection, courseId);
                    requireCourseMutationContext(actorUserId, sessionId, actorProfileType, current.id(), sourceIp);
                    requireCourseCreateContext(
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            command.organizationId(),
                            command.organicUnitId(),
                            sourceIp
                    );
                    requireNotArchived(current);
                    validateOrganizationAndUnit(connection, command.organizationId(), command.organicUnitId());
                    MediaPathValidator.optionalEntityProfilePath(command.photo(), "courses", courseId, "Course photo");
                    courseDAO.update(connection, courseId, command);
                    auditService.record(connection, actorUserId, sessionId, "COURSE_UPDATE",
                            "course", Long.toString(courseId), "success", sourceIp);
                    connection.commit();
                    return courseDAO.findById(courseId)
                            .orElseThrow(() -> new IllegalStateException("Updated course was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "COURSE_UPDATE", Long.toString(courseId), sourceIp);
            throw wrap(exception, "Failed to update course");
        }
    }

    public Course attachCreatedCoursePhoto(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            String photo,
            String sourceIp
    ) {
        try {
            String normalizedPhoto = MediaPathValidator.optionalEntityProfilePath(
                    photo,
                    "courses",
                    courseId,
                    "Course photo"
            );
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course current = requireCourse(connection, courseId);
                    requireCourseMutationContext(actorUserId, sessionId, actorProfileType, current.id(), sourceIp);
                    requireNotArchived(current);
                    courseDAO.updatePhoto(connection, courseId, normalizedPhoto);
                    auditService.record(connection, actorUserId, sessionId, "COURSE_UPDATE",
                            "course", Long.toString(courseId), "success", sourceIp);
                    connection.commit();
                    return courseDAO.findById(courseId)
                            .orElseThrow(() -> new IllegalStateException("Updated course was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "COURSE_UPDATE", Long.toString(courseId), sourceIp);
            throw wrap(exception, "Failed to attach course photo");
        }
    }

    public void archiveCourse(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course current = requireCourse(connection, courseId);
                    requireCourseMutationContext(actorUserId, sessionId, actorProfileType, current.id(), sourceIp);
                    requireNotArchived(current);
                    courseDAO.updateState(connection, courseId, CourseState.ARCHIVED);
                    auditService.record(connection, actorUserId, sessionId, "COURSE_ARCHIVE",
                            "course", Long.toString(courseId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "COURSE_ARCHIVE", Long.toString(courseId), sourceIp);
            throw wrap(exception, "Failed to archive course");
        }
    }

    public void unarchiveCourse(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course current = requireCourse(connection, courseId);
                    requireCourseMutationContext(actorUserId, sessionId, actorProfileType, current.id(), sourceIp);
                    if (current.state() != CourseState.ARCHIVED) {
                        throw new IllegalStateException("Only archived courses can be unarchived");
                    }
                    validateOrganizationAndUnit(connection, current.organizationId(), current.organicUnitId());
                    courseDAO.updateState(connection, courseId, CourseState.ACTIVE);
                    auditService.record(connection, actorUserId, sessionId, "COURSE_UNARCHIVE",
                            "course", Long.toString(courseId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "COURSE_UNARCHIVE", Long.toString(courseId), sourceIp);
            throw wrap(exception, "Failed to unarchive course");
        }
    }

    public void deleteCourse(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course current = requireCourse(connection, courseId);
                    requireCourseMutationContext(actorUserId, sessionId, actorProfileType, current.id(), sourceIp);
                    requireNotArchived(current);
                    if (courseDAO.hasDomainDependencies(connection, courseId)) {
                        throw new IllegalStateException("Course with domain dependencies cannot be deleted");
                    }
                    courseDAO.delete(connection, courseId);
                    auditService.record(connection, actorUserId, sessionId, "COURSE_DELETE",
                            "course", Long.toString(courseId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "COURSE_DELETE", Long.toString(courseId), sourceIp);
            throw wrap(exception, "Failed to delete course");
        }
    }

    public boolean canCreateCourse(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            Long organicUnitId,
            String sourceIp
    ) {
        return courseCreateDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                organizationId,
                organicUnitId,
                sourceIp
        ).allowed();
    }

    public boolean canModifyCourse(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            String sourceIp
    ) {
        return courseMutationDecision(actorUserId, sessionId, actorProfileType, courseId, sourceIp).allowed();
    }

    public boolean canManageCourseChildren(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            String sourceIp
    ) {
        if (actorProfileType != AccessProfileType.ADMINISTRATOR) {
            return false;
        }
        return courseAccessDecision(actorUserId, sessionId, actorProfileType, courseId, sourceIp).allowed();
    }

    private void validateOrganizationAndUnit(
            Connection connection,
            long organizationId,
            Long organicUnitId
    ) throws SQLException {
        Organization organization = organizationDAO.findById(connection, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Course organization not found: " + organizationId));
        if (organization.state() == OrganizationState.ARCHIVED) {
            throw new IllegalStateException("Archived organizations cannot receive courses");
        }
        if (organicUnitId != null) {
            OrganicUnit unit = organicUnitDAO.findById(connection, organicUnitId)
                    .orElseThrow(() -> new IllegalArgumentException("Course organic unit not found: " + organicUnitId));
            if (unit.organizationId() != organizationId) {
                throw new IllegalArgumentException("Course organic unit must belong to the same organization");
            }
            if (unit.state() == OrganicUnitState.ARCHIVED) {
                throw new IllegalStateException("Archived organic units cannot receive courses");
            }
        }
    }

    private void requireCourseManager(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String sourceIp
    ) {
        AuthorizationDecision decision = courseManagerDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                organizationId,
                sourceIp
        );
        if (!decision.allowed()) {
            throw new SecurityException("Missing course management context: " + decision.reason());
        }
    }

    private AuthorizationDecision courseManagerDecision(
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
                AuthorizationPolicy.MANAGE_COURSES,
                AccessEntityType.ORGANIZATION,
                organizationId,
                sourceIp
        ));
    }

    private void requireCourseCreateContext(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            Long organicUnitId,
            String sourceIp
    ) {
        AuthorizationDecision decision = courseCreateDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                organizationId,
                organicUnitId,
                sourceIp
        );
        if (!decision.allowed()) {
            throw new SecurityException("Missing course creation context: " + decision.reason());
        }
    }

    private void requireCourseAccess(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            String sourceIp
    ) {
        AuthorizationDecision decision = courseAccessDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                courseId,
                sourceIp
        );
        if (!decision.allowed()) {
            throw new SecurityException("Missing course management context: " + decision.reason());
        }
    }

    private AuthorizationDecision courseAccessDecision(
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
                AuthorizationPolicy.MANAGE_COURSES,
                AccessEntityType.COURSE,
                courseId,
                sourceIp
        ));
        if (decision.allowed() || actorProfileType != AccessProfileType.COORDINATOR) {
            return decision;
        }
        return coordinatorCourseAccessDecision(actorUserId, sessionId, actorProfileType, courseId, sourceIp);
    }

    private AuthorizationDecision coordinatorCourseAccessDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            String sourceIp
    ) {
        try {
            boolean hasActiveAssociation = false;
            for (CourseSubjectAssociation association : courseSubjectDAO.findActiveByCourse(courseId)) {
                if (association.state() != CourseSubjectState.ACTIVE) {
                    continue;
                }
                hasActiveAssociation = true;
                AuthorizationDecision subjectDecision = permissionChecker.check(new AccessContext(
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        AuthorizationPolicy.MANAGE_SUBJECTS,
                        AccessEntityType.SUBJECT,
                        association.subjectId(),
                        sourceIp
                ));
                if (subjectDecision.allowed()) {
                    return subjectDecision;
                }
            }
            return AuthorizationDecision.deny(hasActiveAssociation
                    ? "missing_coordinated_course_subject"
                    : "course_has_no_active_subjects");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check coordinator course context", exception);
        }
    }

    private AuthorizationDecision courseCreateDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            Long organicUnitId,
            String sourceIp
    ) {
        AccessEntityType entityType = organicUnitId == null
                ? AccessEntityType.ORGANIZATION
                : AccessEntityType.ORGANIC_UNIT;
        long entityId = organicUnitId == null ? organizationId : organicUnitId;
        return permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_COURSES,
                entityType,
                entityId,
                sourceIp
        ));
    }

    private void requireCourseMutationContext(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            String sourceIp
    ) {
        AuthorizationDecision decision = courseMutationDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                courseId,
                sourceIp
        );
        if (!decision.allowed()) {
            throw new SecurityException("Missing course ancestor context: " + decision.reason());
        }
    }

    private AuthorizationDecision courseMutationDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            String sourceIp
    ) {
        return permissionChecker.checkDescendant(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_COURSES,
                AccessEntityType.COURSE,
                courseId,
                sourceIp
        ));
    }

    private Course requireCourse(long courseId) throws SQLException {
        return courseDAO.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
    }

    private void validateOrganizationExists(long organizationId) throws SQLException {
        organizationDAO.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Course organization not found: " + organizationId));
    }

    private Course requireCourse(Connection connection, long courseId) throws SQLException {
        return courseDAO.findById(connection, courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
    }

    private static void validateCreateCommand(CourseCreateCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.organizationId() <= 0) {
            throw new IllegalArgumentException("Course organization is required");
        }
        AcademicTextValidator.requireName(command.name(), "Course name is required");
        AcademicTextValidator.requireAcronym(command.acronym(), "Course acronym is required");
        requireNoInitialPhoto(command.photo(), "Course photo");
        Objects.requireNonNull(command.type(), "course type is required");
        Objects.requireNonNull(command.state(), "course state is required");
        requireDurationInYears(command.duration());
        if (command.state() == CourseState.ARCHIVED) {
            throw new IllegalArgumentException("Use the archive operation to archive courses");
        }
    }

    private static void validateUpdateCommand(CourseUpdateCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.organizationId() <= 0) {
            throw new IllegalArgumentException("Course organization is required");
        }
        AcademicTextValidator.requireName(command.name(), "Course name is required");
        AcademicTextValidator.requireAcronym(command.acronym(), "Course acronym is required");
        MediaPathValidator.optionalSafeRelativePath(command.photo(), "Course photo");
        Objects.requireNonNull(command.type(), "course type is required");
        Objects.requireNonNull(command.state(), "course state is required");
        requireDurationInYears(command.duration());
        if (command.state() == CourseState.ARCHIVED) {
            throw new IllegalArgumentException("Use the archive operation to archive courses");
        }
    }

    private static void requireDurationInYears(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!value.trim().matches("\\d+")) {
            throw new IllegalArgumentException("Course duration must contain only digits");
        }
    }

    private static void requireNotArchived(Course course) {
        if (course.state() == CourseState.ARCHIVED) {
            throw new IllegalStateException("Archived courses cannot be changed");
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
                "course", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
