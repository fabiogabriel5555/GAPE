package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.ClassGroupEnrollmentDAO;
import pt.isel.gape.learning.dao.CertificateDAO;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseOccurrenceDAO;
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.CourseEnrollmentCommand;
import pt.isel.gape.learning.model.CourseOccurrence;
import pt.isel.gape.learning.model.CourseOccurrenceState;
import pt.isel.gape.learning.model.CourseState;
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

public final class EnrollmentService {

    private final ConnectionProvider connectionProvider;
    private final CourseDAO courseDAO;
    private final CourseOccurrenceDAO courseOccurrenceDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final ClassGroupEnrollmentDAO classGroupEnrollmentDAO;
    private final CertificateDAO certificateDAO;
    private final PermissionChecker permissionChecker;
    private final AuditService auditService;
    private final Clock clock;

    public EnrollmentService(
            ConnectionProvider connectionProvider,
            CourseDAO courseDAO,
            CourseOccurrenceDAO courseOccurrenceDAO,
            EnrollmentDAO enrollmentDAO,
            ClassGroupEnrollmentDAO classGroupEnrollmentDAO,
            PermissionChecker permissionChecker,
            AuditService auditService,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.courseDAO = Objects.requireNonNull(courseDAO, "courseDAO is required");
        this.courseOccurrenceDAO = Objects.requireNonNull(courseOccurrenceDAO, "courseOccurrenceDAO is required");
        this.enrollmentDAO = Objects.requireNonNull(enrollmentDAO, "enrollmentDAO is required");
        this.classGroupEnrollmentDAO = Objects.requireNonNull(
                classGroupEnrollmentDAO,
                "classGroupEnrollmentDAO is required"
        );
        this.certificateDAO = new CertificateDAO(connectionProvider);
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public EnrollmentService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new CourseDAO(connectionProvider),
                new CourseOccurrenceDAO(connectionProvider),
                new EnrollmentDAO(connectionProvider),
                new ClassGroupEnrollmentDAO(connectionProvider),
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

    public CourseEnrollment enrollStudentInCourse(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            CourseEnrollmentCommand command,
            String sourceIp
    ) {
        try {
            validateCourseCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course course = requireCourse(connection, command.courseId());
                    CourseOccurrence occurrence = requireCourseOccurrence(
                            connection,
                            command.courseId(),
                            command.courseOccurrenceId()
                    );
                    requireEnrollmentAccess(actorUserId, sessionId, actorProfileType,
                            command.studentUserId(), course.organizationId(), course.id(), sourceIp);
                    validateStudent(connection, command.studentUserId());
                    requireActiveCourse(course);
                    requireEnrollableOccurrence(occurrence);
                    if (enrollmentDAO.findCourseEnrollment(
                            connection,
                            command.studentUserId(),
                            command.courseId(),
                            occurrence.id()
                    ).isPresent()) {
                        throw new IllegalStateException("Student is already enrolled in this course occurrence");
                    }
                    enrollmentDAO.enrollCourse(connection, new CourseEnrollment(
                            command.studentUserId(),
                            command.courseId(),
                            occurrence.id(),
                            EnrollmentState.ACTIVE,
                            occurrence.startsAt(),
                            occurrence.endsAt()
                    ));
                    CourseEnrollment createdEnrollment = enrollmentDAO
                            .findCourseEnrollment(
                                    connection,
                                    command.studentUserId(),
                                    command.courseId(),
                                    occurrence.id()
                            )
                            .orElseThrow(() -> new IllegalStateException("Created course enrollment was not found"));
                    certificateDAO.createDraftIfAbsent(
                            connection,
                            command.courseId(),
                            createdEnrollment.courseOccurrenceId(),
                            command.studentUserId(),
                            "Certificate - " + course.name()
                    );
                    auditService.record(connection, actorUserId, sessionId, "COURSE_ENROLL",
                            "course_enrollment", courseEnrollmentIdentifier(
                                    command.studentUserId(), command.courseId(), occurrence.id()
                            ),
                            "success", sourceIp);
                    connection.commit();
                    return createdEnrollment;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "COURSE_ENROLL", "course_enrollment",
                    command == null ? "new" : courseEnrollmentIdentifier(
                            command.studentUserId(), command.courseId(), command.courseOccurrenceId()
                    ),
                    sourceIp);
            throw wrap(exception, "Failed to enroll student in course");
        }
    }

    public CourseEnrollment withdrawStudentFromCourse(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long courseId,
            long courseOccurrenceId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course course = requireCourse(connection, courseId);
                    CourseOccurrence occurrence = requireCourseOccurrence(connection, courseId, courseOccurrenceId);
                    requireEnrollmentAccess(actorUserId, sessionId, actorProfileType,
                            studentUserId, course.organizationId(), course.id(), sourceIp);
                    requireActiveCourse(course);
                    enrollmentDAO.findCourseEnrollment(connection, studentUserId, courseId, occurrence.id())
                            .orElseThrow(() -> new IllegalArgumentException("Course enrollment not found"));
                    classGroupEnrollmentDAO.withdrawActiveInCourseOccurrence(
                            connection, studentUserId, courseId, occurrence.id(), occurrence.endsAt()
                    );
                    enrollmentDAO.withdrawCourse(
                            connection, studentUserId, courseId, occurrence.id(), occurrence.endsAt()
                    );
                    auditService.record(connection, actorUserId, sessionId, "COURSE_WITHDRAW",
                            "course_enrollment", courseEnrollmentIdentifier(studentUserId, courseId, occurrence.id()),
                            "success", sourceIp);
                    connection.commit();
                    return enrollmentDAO.findCourseEnrollment(connection, studentUserId, courseId, occurrence.id())
                            .orElseThrow(() -> new IllegalStateException("Updated course enrollment was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "COURSE_WITHDRAW", "course_enrollment",
                    courseEnrollmentIdentifier(studentUserId, courseId, courseOccurrenceId), sourceIp);
            throw wrap(exception, "Failed to withdraw student from course");
        }
    }

    public CourseEnrollment updateCourseEnrollment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long courseId,
            long courseOccurrenceId,
            EnrollmentState state,
            String sourceIp
    ) {
        Objects.requireNonNull(state, "state is required");
        validateCourseEnrollmentState(state);
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course course = requireCourse(connection, courseId);
                    CourseOccurrence occurrence = requireCourseOccurrence(connection, courseId, courseOccurrenceId);
                    requireEnrollmentAccess(actorUserId, sessionId, actorProfileType,
                            studentUserId, course.organizationId(), course.id(), sourceIp);
                    requireActiveCourse(course);
                    enrollmentDAO.findCourseEnrollment(connection, studentUserId, courseId, occurrence.id())
                            .orElseThrow(() -> new IllegalArgumentException("Course enrollment not found"));
                    if (state == EnrollmentState.ACTIVE) {
                        validateStudent(connection, studentUserId);
                        requireActiveCourse(course);
                        requireEnrollableOccurrence(occurrence);
                    }
                    if (state == EnrollmentState.WITHDRAWN) {
                        classGroupEnrollmentDAO.withdrawActiveInCourseOccurrence(
                                connection,
                                studentUserId,
                                courseId,
                                occurrence.id(),
                                occurrence.endsAt()
                        );
                    }
                    enrollmentDAO.updateCourseEnrollment(
                            connection,
                            studentUserId,
                            courseId,
                            occurrence.id(),
                            state,
                            occurrence.endsAt()
                    );
                    if (state == EnrollmentState.ACTIVE) {
                        certificateDAO.createDraftIfAbsent(
                                connection,
                                courseId,
                                occurrence.id(),
                                studentUserId,
                                "Certificate - " + course.name()
                        );
                    }
                    auditService.record(connection, actorUserId, sessionId, "COURSE_ENROLL_UPDATE",
                            "course_enrollment", courseEnrollmentIdentifier(studentUserId, courseId, occurrence.id()),
                            "success", sourceIp);
                    connection.commit();
                    return enrollmentDAO.findCourseEnrollment(connection, studentUserId, courseId, occurrence.id())
                            .orElseThrow(() -> new IllegalStateException("Updated course enrollment was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "COURSE_ENROLL_UPDATE", "course_enrollment",
                    courseEnrollmentIdentifier(studentUserId, courseId, courseOccurrenceId), sourceIp);
            throw wrap(exception, "Failed to update course enrollment");
        }
    }

    public void deleteCourseEnrollment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long courseId,
            long courseOccurrenceId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course course = requireCourse(connection, courseId);
                    CourseOccurrence occurrence = requireCourseOccurrence(connection, courseId, courseOccurrenceId);
                    requireEnrollmentAccess(actorUserId, sessionId, actorProfileType,
                            studentUserId, course.organizationId(), course.id(), sourceIp);
                    requireActiveCourse(course);
                    enrollmentDAO.findCourseEnrollment(connection, studentUserId, courseId, occurrence.id())
                            .orElseThrow(() -> new IllegalArgumentException("Course enrollment not found"));
                    classGroupEnrollmentDAO.deleteInCourseOccurrence(connection, studentUserId, courseId, occurrence.id());
                    enrollmentDAO.deleteCourseEnrollment(connection, studentUserId, courseId, occurrence.id());
                    auditService.record(connection, actorUserId, sessionId, "COURSE_ENROLL_DELETE",
                            "course_enrollment", courseEnrollmentIdentifier(studentUserId, courseId, occurrence.id()),
                            "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "COURSE_ENROLL_DELETE", "course_enrollment",
                    courseEnrollmentIdentifier(studentUserId, courseId, courseOccurrenceId), sourceIp);
            throw wrap(exception, "Failed to delete course enrollment");
        }
    }

    private Course requireCourse(Connection connection, long courseId) throws SQLException {
        return courseDAO.findById(connection, courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
    }

    private void validateStudent(Connection connection, long studentUserId) throws SQLException {
        if (!enrollmentDAO.activeStudentExists(connection, studentUserId)) {
            throw new IllegalArgumentException("Enrollment requires an active student");
        }
    }

    private void requireEnrollmentAccess(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long organizationId,
            long courseId,
            String sourceIp
    ) {
        AuthorizationDecision organizationDecision = AuthorizationDecision.deny("administrator_profile_required");
        AuthorizationDecision courseDecision = AuthorizationDecision.deny("administrator_profile_required");
        if (actorProfileType == AccessProfileType.ADMINISTRATOR) {
            organizationDecision = permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_ENROLLMENTS,
                    AccessEntityType.ORGANIZATION,
                    organizationId,
                    sourceIp
            ));
            if (organizationDecision.allowed()) {
                return;
            }
            courseDecision = permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_ENROLLMENTS,
                    AccessEntityType.COURSE,
                    courseId,
                    sourceIp
            ));
            if (courseDecision.allowed()) {
                return;
            }
        }
        throw new SecurityException("Missing enrollment management context: "
                + organizationDecision.reason() + "/" + courseDecision.reason());
    }

    private static void validateCourseCommand(CourseEnrollmentCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.studentUserId() <= 0) {
            throw new IllegalArgumentException("Enrollment student is required");
        }
        if (command.courseId() <= 0) {
            throw new IllegalArgumentException("Enrollment course is required");
        }
        if (command.courseOccurrenceId() <= 0) {
            throw new IllegalArgumentException("Course occurrence is required");
        }
    }

    private static void validateCourseEnrollmentState(EnrollmentState state) {
        if (state != EnrollmentState.ACTIVE
                && state != EnrollmentState.INACTIVE
                && state != EnrollmentState.WITHDRAWN) {
            throw new IllegalArgumentException("Course enrollment state is managed automatically: " + state);
        }
    }

    private static void requireActiveCourse(Course course) {
        if (course.state() != CourseState.ACTIVE) {
            throw new IllegalStateException("Enrollment requires an active course");
        }
    }

    private CourseOccurrence requireCourseOccurrence(
            Connection connection,
            long courseId,
            long courseOccurrenceId
    ) throws SQLException {
        CourseOccurrence occurrence = courseOccurrenceDAO.findById(connection, courseOccurrenceId)
                .orElseThrow(() -> new IllegalArgumentException("Course occurrence not found: " + courseOccurrenceId));
        if (occurrence.courseId() != courseId) {
            throw new IllegalArgumentException("Course occurrence must belong to the selected course");
        }
        return occurrence;
    }

    private static void requireEnrollableOccurrence(CourseOccurrence occurrence) {
        if (occurrence.state() != CourseOccurrenceState.ACTIVE
                && occurrence.state() != CourseOccurrenceState.SCHEDULED) {
            throw new IllegalStateException("Enrollment requires a scheduled or active course occurrence");
        }
    }

    private static String courseEnrollmentIdentifier(long studentUserId, long courseId, long courseOccurrenceId) {
        return studentUserId + ":" + courseId + ":" + courseOccurrenceId;
    }

    private void auditFailure(
            long actorUserId,
            Long sessionId,
            String operationType,
            String affectedEntityType,
            String affectedIdentifier,
            String sourceIp
    ) {
        auditService.record(actorUserId, sessionId, operationType,
                affectedEntityType, affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
