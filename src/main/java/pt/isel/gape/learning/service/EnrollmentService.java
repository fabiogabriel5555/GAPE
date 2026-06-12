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
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.CourseEnrollmentCommand;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseSubjectState;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectEnrollment;
import pt.isel.gape.learning.model.SubjectEnrollmentCommand;
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

public final class EnrollmentService {

    private final ConnectionProvider connectionProvider;
    private final CourseDAO courseDAO;
    private final SubjectDAO subjectDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final PermissionChecker permissionChecker;
    private final AuditService auditService;
    private final Clock clock;

    public EnrollmentService(
            ConnectionProvider connectionProvider,
            CourseDAO courseDAO,
            SubjectDAO subjectDAO,
            CourseSubjectDAO courseSubjectDAO,
            EnrollmentDAO enrollmentDAO,
            PermissionChecker permissionChecker,
            AuditService auditService,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.courseDAO = Objects.requireNonNull(courseDAO, "courseDAO is required");
        this.subjectDAO = Objects.requireNonNull(subjectDAO, "subjectDAO is required");
        this.courseSubjectDAO = Objects.requireNonNull(courseSubjectDAO, "courseSubjectDAO is required");
        this.enrollmentDAO = Objects.requireNonNull(enrollmentDAO, "enrollmentDAO is required");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public EnrollmentService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new CourseDAO(connectionProvider),
                new SubjectDAO(connectionProvider),
                new CourseSubjectDAO(connectionProvider),
                new EnrollmentDAO(connectionProvider),
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
            CourseEnrollmentCommand normalized = normalizeCourseCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course course = requireCourse(connection, normalized.courseId());
                    requireEnrollmentAccess(actorUserId, sessionId, actorProfileType,
                            normalized.studentUserId(), course.organizationId(), course.id(), null, sourceIp);
                    validateStudent(connection, normalized.studentUserId());
                    requireActiveCourse(course);
                    if (enrollmentDAO.findCourseEnrollment(
                            connection,
                            normalized.studentUserId(),
                            normalized.courseId()
                    ).isPresent()) {
                        throw new IllegalStateException("Course enrollment already exists");
                    }
                    if (enrollmentDAO.hasOverlappingActiveCourseEnrollment(
                            connection,
                            normalized.studentUserId(),
                            normalized.courseId(),
                            normalized.startDate(),
                            normalized.endDate()
                    )) {
                        throw new IllegalStateException("Active course enrollment overlaps the requested period");
                    }
                    enrollmentDAO.enrollCourse(connection, normalized);
                    auditService.record(connection, actorUserId, sessionId, "COURSE_ENROLL",
                            "course_enrollment", courseEnrollmentIdentifier(normalized.studentUserId(), normalized.courseId()),
                            "success", sourceIp);
                    connection.commit();
                    return enrollmentDAO.findCourseEnrollment(connection, normalized.studentUserId(), normalized.courseId())
                            .orElseThrow(() -> new IllegalStateException("Created course enrollment was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "COURSE_ENROLL", "course_enrollment",
                    command == null ? "new" : courseEnrollmentIdentifier(command.studentUserId(), command.courseId()),
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
            LocalDate endDate,
            String sourceIp
    ) {
        try {
            LocalDate withdrawalDate = endDate == null ? LocalDate.now(clock) : endDate;
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course course = requireCourse(connection, courseId);
                    requireEnrollmentAccess(actorUserId, sessionId, actorProfileType,
                            studentUserId, course.organizationId(), course.id(), null, sourceIp);
                    requireNotArchived(course);
                    CourseEnrollment current = enrollmentDAO.findCourseEnrollment(connection, studentUserId, courseId)
                            .orElseThrow(() -> new IllegalArgumentException("Course enrollment not found"));
                    if (current.startDate() != null && withdrawalDate.isBefore(current.startDate())) {
                        throw new IllegalArgumentException("Withdrawal date cannot be before enrollment start date");
                    }
                    enrollmentDAO.withdrawActiveSubjectsInCourse(connection, studentUserId, courseId, withdrawalDate);
                    enrollmentDAO.withdrawCourse(connection, studentUserId, courseId, withdrawalDate);
                    auditService.record(connection, actorUserId, sessionId, "COURSE_WITHDRAW",
                            "course_enrollment", courseEnrollmentIdentifier(studentUserId, courseId),
                            "success", sourceIp);
                    connection.commit();
                    return enrollmentDAO.findCourseEnrollment(connection, studentUserId, courseId)
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
                    courseEnrollmentIdentifier(studentUserId, courseId), sourceIp);
            throw wrap(exception, "Failed to withdraw student from course");
        }
    }

    public SubjectEnrollment enrollStudentInSubject(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            SubjectEnrollmentCommand command,
            String sourceIp
    ) {
        try {
            SubjectEnrollmentCommand normalized = normalizeSubjectCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course course = requireCourse(connection, normalized.courseId());
                    Subject subject = requireSubject(connection, normalized.subjectId());
                    requireEnrollmentAccess(actorUserId, sessionId, actorProfileType,
                            normalized.studentUserId(), course.organizationId(),
                            course.id(), subject.id(), sourceIp);
                    validateStudent(connection, normalized.studentUserId());
                    requireActiveCourse(course);
                    requireActiveSubject(subject);
                    CourseSubjectAssociation association = courseSubjectDAO
                            .findByCourseAndSubject(connection, normalized.courseId(), normalized.subjectId())
                            .orElseThrow(() -> new IllegalArgumentException("Subject is not integrated in the course"));
                    if (association.state() != CourseSubjectState.ACTIVE) {
                        throw new IllegalStateException("Subject-course association must be active");
                    }
                    if (course.organizationId() != subject.organizationId()) {
                        throw new IllegalArgumentException("Course and subject must belong to the same organization");
                    }
                    if (enrollmentDAO.findSubjectEnrollment(
                            connection,
                            normalized.studentUserId(),
                            normalized.courseId(),
                            normalized.subjectId()
                    ).isPresent()) {
                        throw new IllegalStateException("Subject enrollment already exists");
                    }
                    if (!enrollmentDAO.hasActiveCourseEnrollmentCovering(
                            connection,
                            normalized.studentUserId(),
                            normalized.courseId(),
                            normalized.startDate(),
                            normalized.endDate()
                    )) {
                        throw new IllegalStateException("Student must be actively enrolled in the course for the full subject period");
                    }
                    if (enrollmentDAO.hasOverlappingActiveSubjectEnrollment(
                            connection,
                            normalized.studentUserId(),
                            normalized.courseId(),
                            normalized.subjectId(),
                            normalized.startDate(),
                            normalized.endDate()
                    )) {
                        throw new IllegalStateException("Active subject enrollment overlaps the requested period");
                    }
                    enrollmentDAO.enrollSubject(connection, normalized);
                    auditService.record(connection, actorUserId, sessionId, "SUBJECT_ENROLL",
                            "subject_enrollment", subjectEnrollmentIdentifier(
                                    normalized.studentUserId(), normalized.courseId(), normalized.subjectId()),
                            "success", sourceIp);
                    connection.commit();
                    return enrollmentDAO.findSubjectEnrollment(
                                    connection,
                                    normalized.studentUserId(),
                                    normalized.courseId(),
                                    normalized.subjectId()
                            )
                            .orElseThrow(() -> new IllegalStateException("Created subject enrollment was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "SUBJECT_ENROLL", "subject_enrollment",
                    command == null
                            ? "new"
                            : subjectEnrollmentIdentifier(command.studentUserId(), command.courseId(), command.subjectId()),
                    sourceIp);
            throw wrap(exception, "Failed to enroll student in subject");
        }
    }

    public SubjectEnrollment withdrawStudentFromSubject(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long courseId,
            long subjectId,
            LocalDate endDate,
            String sourceIp
    ) {
        try {
            LocalDate withdrawalDate = endDate == null ? LocalDate.now(clock) : endDate;
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course course = requireCourse(connection, courseId);
                    requireEnrollmentAccess(actorUserId, sessionId, actorProfileType,
                            studentUserId, course.organizationId(), course.id(), subjectId, sourceIp);
                    requireNotArchived(course);
                    Subject subject = requireSubject(connection, subjectId);
                    requireNotArchived(subject);
                    SubjectEnrollment current = enrollmentDAO
                            .findSubjectEnrollment(connection, studentUserId, courseId, subjectId)
                            .orElseThrow(() -> new IllegalArgumentException("Subject enrollment not found"));
                    if (current.startDate() != null && withdrawalDate.isBefore(current.startDate())) {
                        throw new IllegalArgumentException("Withdrawal date cannot be before enrollment start date");
                    }
                    enrollmentDAO.withdrawSubject(connection, studentUserId, courseId, subjectId, withdrawalDate);
                    auditService.record(connection, actorUserId, sessionId, "SUBJECT_WITHDRAW",
                            "subject_enrollment", subjectEnrollmentIdentifier(studentUserId, courseId, subjectId),
                            "success", sourceIp);
                    connection.commit();
                    return enrollmentDAO.findSubjectEnrollment(connection, studentUserId, courseId, subjectId)
                            .orElseThrow(() -> new IllegalStateException("Updated subject enrollment was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "SUBJECT_WITHDRAW", "subject_enrollment",
                    subjectEnrollmentIdentifier(studentUserId, courseId, subjectId), sourceIp);
            throw wrap(exception, "Failed to withdraw student from subject");
        }
    }

    private Course requireCourse(Connection connection, long courseId) throws SQLException {
        return courseDAO.findById(connection, courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
    }

    private Subject requireSubject(Connection connection, long subjectId) throws SQLException {
        return subjectDAO.findById(connection, subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));
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
            Long subjectId,
            String sourceIp
    ) {
        AuthorizationDecision adminDecision = AuthorizationDecision.deny("administrator_profile_required");
        AuthorizationDecision courseDecision = AuthorizationDecision.deny("administrator_profile_required");
        AuthorizationDecision subjectDecision = AuthorizationDecision.deny("not_applicable");
        if (actorProfileType == AccessProfileType.ADMINISTRATOR) {
            adminDecision = permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_ENROLLMENTS,
                    AccessEntityType.ORGANIZATION,
                    organizationId,
                    sourceIp
            ));
            if (adminDecision.allowed()) {
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
            if (subjectId != null) {
                subjectDecision = permissionChecker.check(new AccessContext(
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        AuthorizationPolicy.MANAGE_ENROLLMENTS,
                        AccessEntityType.SUBJECT,
                        subjectId,
                        sourceIp
                ));
                if (subjectDecision.allowed()) {
                    return;
                }
            }
        }
        if (actorProfileType == AccessProfileType.STUDENT && actorUserId == studentUserId) {
            AuthorizationDecision selfDecision = permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.VIEW_REPORTS,
                    AccessEntityType.SELF,
                    studentUserId,
                    sourceIp
            ));
            if (selfDecision.allowed()) {
                return;
            }
            throw new SecurityException("Missing student self-service context: " + selfDecision.reason());
        }
        throw new SecurityException("Missing enrollment management context: "
                + adminDecision.reason() + "/" + courseDecision.reason() + "/" + subjectDecision.reason());
    }

    private static CourseEnrollmentCommand normalizeCourseCommand(CourseEnrollmentCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.studentUserId() <= 0) {
            throw new IllegalArgumentException("Enrollment student is required");
        }
        if (command.courseId() <= 0) {
            throw new IllegalArgumentException("Enrollment course is required");
        }
        requireValidDates(command.startDate(), command.endDate());
        return command;
    }

    private SubjectEnrollmentCommand normalizeSubjectCommand(SubjectEnrollmentCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.studentUserId() <= 0) {
            throw new IllegalArgumentException("Enrollment student is required");
        }
        if (command.courseId() == null || command.courseId() <= 0) {
            throw new IllegalArgumentException("Subject enrollment course is required");
        }
        if (command.subjectId() <= 0) {
            throw new IllegalArgumentException("Subject enrollment subject is required");
        }
        requireValidDates(command.startDate(), command.endDate());
        LocalDate startDate = command.startDate() == null ? LocalDate.now(clock) : command.startDate();
        return new SubjectEnrollmentCommand(
                command.studentUserId(),
                command.subjectId(),
                command.courseId(),
                startDate,
                command.endDate()
        );
    }

    private static void requireActiveCourse(Course course) {
        if (course.state() != CourseState.ACTIVE) {
            throw new IllegalStateException("Enrollment requires an active course");
        }
    }

    private static void requireActiveSubject(Subject subject) {
        if (subject.state() != SubjectState.ACTIVE) {
            throw new IllegalStateException("Enrollment requires an active subject");
        }
    }

    private static void requireNotArchived(Course course) {
        if (course.state() == CourseState.ARCHIVED) {
            throw new IllegalStateException("Archived courses cannot be changed");
        }
    }

    private static void requireNotArchived(Subject subject) {
        if (subject.state() == SubjectState.ARCHIVED) {
            throw new IllegalStateException("Archived subjects cannot be changed");
        }
    }

    private static void requireValidDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Enrollment end date cannot be before start date");
        }
    }

    private static String courseEnrollmentIdentifier(long studentUserId, long courseId) {
        return studentUserId + ":" + courseId;
    }

    private static String subjectEnrollmentIdentifier(long studentUserId, Long courseId, long subjectId) {
        return studentUserId + ":" + courseId + ":" + subjectId;
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
