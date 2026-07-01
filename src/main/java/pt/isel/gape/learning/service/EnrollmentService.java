package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.ClassGroupEnrollmentDAO;
import pt.isel.gape.learning.dao.CertificateDAO;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.EnrollmentApprovalPolicyDAO;
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.EnrollmentApprovalMode;
import pt.isel.gape.learning.model.EnrollmentState;
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
    private final ClassGroupEnrollmentDAO classGroupEnrollmentDAO;
    private final CertificateDAO certificateDAO;
    private final GradeLifecycleService gradeLifecycleService;
    private final EnrollmentApprovalPolicyDAO enrollmentApprovalPolicyDAO;
    private final PermissionChecker permissionChecker;
    private final AuditService auditService;
    private final Clock clock;

    public EnrollmentService(
            ConnectionProvider connectionProvider,
            CourseDAO courseDAO,
            SubjectDAO subjectDAO,
            CourseSubjectDAO courseSubjectDAO,
            EnrollmentDAO enrollmentDAO,
            ClassGroupEnrollmentDAO classGroupEnrollmentDAO,
            EnrollmentApprovalPolicyDAO enrollmentApprovalPolicyDAO,
            PermissionChecker permissionChecker,
            AuditService auditService,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.courseDAO = Objects.requireNonNull(courseDAO, "courseDAO is required");
        this.subjectDAO = Objects.requireNonNull(subjectDAO, "subjectDAO is required");
        this.courseSubjectDAO = Objects.requireNonNull(courseSubjectDAO, "courseSubjectDAO is required");
        this.enrollmentDAO = Objects.requireNonNull(enrollmentDAO, "enrollmentDAO is required");
        this.classGroupEnrollmentDAO = Objects.requireNonNull(
                classGroupEnrollmentDAO,
                "classGroupEnrollmentDAO is required"
        );
        this.certificateDAO = new CertificateDAO(connectionProvider);
        this.gradeLifecycleService = new GradeLifecycleService(connectionProvider, clock);
        this.enrollmentApprovalPolicyDAO = Objects.requireNonNull(
                enrollmentApprovalPolicyDAO,
                "enrollmentApprovalPolicyDAO is required"
        );
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
                new ClassGroupEnrollmentDAO(connectionProvider),
                new EnrollmentApprovalPolicyDAO(connectionProvider),
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
                            normalized.studentUserId(), course.organizationId(), course.id(), null, sourceIp, false);
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
                    certificateDAO.createDraftIfAbsent(
                            connection,
                            normalized.courseId(),
                            normalized.studentUserId(),
                            "Certificate - " + course.name()
                    );
                    gradeLifecycleService.ensureCourseGradeSheets(connection, normalized.courseId());
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
                            studentUserId, course.organizationId(), course.id(), null, sourceIp, false);
                    requireActiveCourse(course);
                    CourseEnrollment current = enrollmentDAO.findCourseEnrollment(connection, studentUserId, courseId)
                            .orElseThrow(() -> new IllegalArgumentException("Course enrollment not found"));
                    if (current.startDate() != null && withdrawalDate.isBefore(current.startDate())) {
                        throw new IllegalArgumentException("Withdrawal date cannot be before enrollment start date");
                    }
                    classGroupEnrollmentDAO.withdrawActiveInCourse(connection, studentUserId, courseId, withdrawalDate);
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

    public CourseEnrollment updateCourseEnrollment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long courseId,
            EnrollmentState state,
            LocalDate endDate,
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
                    requireEnrollmentAccess(actorUserId, sessionId, actorProfileType,
                            studentUserId, course.organizationId(), course.id(), null, sourceIp, false);
                    requireActiveCourse(course);
                    CourseEnrollment current = enrollmentDAO.findCourseEnrollment(connection, studentUserId, courseId)
                            .orElseThrow(() -> new IllegalArgumentException("Course enrollment not found"));
                    LocalDate effectiveEndDate = effectiveCourseEnrollmentEndDate(state, endDate);
                    if (current.startDate() != null
                            && effectiveEndDate != null
                            && effectiveEndDate.isBefore(current.startDate())) {
                        throw new IllegalArgumentException("Enrollment end date cannot be before start date");
                    }
                    if (state == EnrollmentState.ACTIVE) {
                        validateStudent(connection, studentUserId);
                        requireActiveCourse(course);
                    }
                    if (state == EnrollmentState.WITHDRAWN) {
                        classGroupEnrollmentDAO.withdrawActiveInCourse(
                                connection,
                                studentUserId,
                                courseId,
                                effectiveEndDate
                        );
                        enrollmentDAO.withdrawActiveSubjectsInCourse(connection, studentUserId, courseId, effectiveEndDate);
                    }
                    enrollmentDAO.updateCourseEnrollment(connection, studentUserId, courseId, state, effectiveEndDate);
                    if (state == EnrollmentState.ACTIVE) {
                        certificateDAO.createDraftIfAbsent(
                                connection,
                                courseId,
                                studentUserId,
                                "Certificate - " + course.name()
                        );
                        gradeLifecycleService.ensureCourseGradeSheets(connection, courseId);
                    }
                    auditService.record(connection, actorUserId, sessionId, "COURSE_ENROLL_UPDATE",
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
            auditFailure(actorUserId, sessionId, "COURSE_ENROLL_UPDATE", "course_enrollment",
                    courseEnrollmentIdentifier(studentUserId, courseId), sourceIp);
            throw wrap(exception, "Failed to update course enrollment");
        }
    }

    public void deleteCourseEnrollment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long courseId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course course = requireCourse(connection, courseId);
                    requireEnrollmentAccess(actorUserId, sessionId, actorProfileType,
                            studentUserId, course.organizationId(), course.id(), null, sourceIp, false);
                    requireActiveCourse(course);
                    enrollmentDAO.findCourseEnrollment(connection, studentUserId, courseId)
                            .orElseThrow(() -> new IllegalArgumentException("Course enrollment not found"));
                    classGroupEnrollmentDAO.deleteInCourse(connection, studentUserId, courseId);
                    enrollmentDAO.deleteSubjectEnrollmentsInCourse(connection, studentUserId, courseId);
                    enrollmentDAO.deleteCourseEnrollment(connection, studentUserId, courseId);
                    auditService.record(connection, actorUserId, sessionId, "COURSE_ENROLL_DELETE",
                            "course_enrollment", courseEnrollmentIdentifier(studentUserId, courseId),
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
                    courseEnrollmentIdentifier(studentUserId, courseId), sourceIp);
            throw wrap(exception, "Failed to delete course enrollment");
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
                            course.id(), subject.id(), sourceIp, false);
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
                    enrollmentDAO.enrollSubject(connection, normalized);
                    gradeLifecycleService.ensureSubjectGradeSheetDraft(connection, subject);
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

    public SubjectEnrollment requestStudentInSubject(
            long actorUserId,
            Long sessionId,
            SubjectEnrollmentCommand command,
            String sourceIp
    ) {
        try {
            SubjectEnrollmentCommand normalized = normalizeSubjectCommand(command);
            if (actorUserId != normalized.studentUserId()) {
                throw new SecurityException("Students can only request their own subject enrollments");
            }
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course course = requireCourse(connection, normalized.courseId());
                    Subject subject = requireSubject(connection, normalized.subjectId());
                    requireStudentSelfAccess(actorUserId, sessionId, normalized.studentUserId(), sourceIp);
                    EnrollmentApprovalMode mode = enrollmentApprovalPolicyDAO.subjectMode(
                            connection,
                            normalized.courseId(),
                            normalized.subjectId()
                    );
                    validateSubjectEnrollmentRequestContext(connection, course, subject, normalized);

                    SubjectEnrollment current = enrollmentDAO
                            .findSubjectEnrollment(
                                    connection,
                                    normalized.studentUserId(),
                                    normalized.courseId(),
                                    normalized.subjectId()
                            )
                            .orElse(null);
                    if (current != null
                            && (current.state() == EnrollmentState.ACTIVE || current.state() == EnrollmentState.PENDING)) {
                        throw new IllegalStateException("Subject enrollment is already active or pending");
                    }

                    EnrollmentState targetState = mode == EnrollmentApprovalMode.AUTO_APPROVE
                            ? EnrollmentState.ACTIVE
                            : EnrollmentState.PENDING;

                    if (current == null) {
                        if (targetState == EnrollmentState.ACTIVE) {
                            enrollmentDAO.enrollSubject(connection, normalized);
                        } else {
                            enrollmentDAO.requestSubject(connection, normalized);
                        }
                    } else {
                        enrollmentDAO.reactivateSubjectRequest(connection, normalized, targetState);
                    }
                    if (targetState == EnrollmentState.ACTIVE) {
                        gradeLifecycleService.ensureSubjectGradeSheetDraft(connection, subject);
                    }

                    auditService.record(connection, actorUserId, sessionId,
                            targetState == EnrollmentState.ACTIVE ? "SUBJECT_ENROLL_AUTO_APPROVE" : "SUBJECT_ENROLL_REQUEST",
                            "subject_enrollment",
                            subjectEnrollmentIdentifier(
                                    normalized.studentUserId(),
                                    normalized.courseId(),
                                    normalized.subjectId()
                            ),
                            "success",
                            sourceIp);
                    connection.commit();
                    return enrollmentDAO.findSubjectEnrollment(
                                    connection,
                                    normalized.studentUserId(),
                                    normalized.courseId(),
                                    normalized.subjectId()
                            )
                            .orElseThrow(() -> new IllegalStateException("Subject enrollment request was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "SUBJECT_ENROLL_REQUEST", "subject_enrollment",
                    command == null
                            ? "new"
                            : subjectEnrollmentIdentifier(command.studentUserId(), command.courseId(), command.subjectId()),
                    sourceIp);
            throw wrap(exception, "Failed to request subject enrollment");
        }
    }

    public SubjectEnrollment approveSubjectEnrollment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long courseId,
            long subjectId,
            LocalDate startDate,
            LocalDate endDate,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course course = requireCourse(connection, courseId);
                    Subject subject = requireSubject(connection, subjectId);
                    requireEnrollmentAccess(actorUserId, sessionId, actorProfileType,
                            studentUserId, course.organizationId(), course.id(), subject.id(), sourceIp, false);
                    SubjectEnrollment current = enrollmentDAO.findSubjectEnrollment(
                                    connection,
                                    studentUserId,
                                    courseId,
                                    subjectId
                            )
                            .orElseThrow(() -> new IllegalArgumentException("Subject enrollment request not found"));
                    if (current.state() != EnrollmentState.PENDING) {
                        throw new IllegalStateException("Only pending subject enrollment requests can be approved");
                    }
                    LocalDate approvedStart = startDate != null
                            ? startDate
                            : current.startDate() == null ? LocalDate.now(clock) : current.startDate();
                    LocalDate approvedEnd = endDate != null ? endDate : current.endDate();
                    SubjectEnrollmentCommand approval = new SubjectEnrollmentCommand(
                            studentUserId,
                            subjectId,
                            courseId,
                            approvedStart,
                            approvedEnd
                    );
                    validateSubjectEnrollmentRequestContext(connection, course, subject, approval);
                    enrollmentDAO.updateSubjectState(
                            connection,
                            studentUserId,
                            courseId,
                            subjectId,
                            EnrollmentState.PENDING,
                            EnrollmentState.ACTIVE,
                            approvedStart,
                            approvedEnd
                    );
                    gradeLifecycleService.ensureSubjectGradeSheetDraft(connection, subject);
                    auditService.record(connection, actorUserId, sessionId, "SUBJECT_ENROLL_APPROVE",
                            "subject_enrollment", subjectEnrollmentIdentifier(studentUserId, courseId, subjectId),
                            "success", sourceIp);
                    connection.commit();
                    return enrollmentDAO.findSubjectEnrollment(connection, studentUserId, courseId, subjectId)
                            .orElseThrow(() -> new IllegalStateException("Approved subject enrollment was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "SUBJECT_ENROLL_APPROVE", "subject_enrollment",
                    subjectEnrollmentIdentifier(studentUserId, courseId, subjectId), sourceIp);
            throw wrap(exception, "Failed to approve subject enrollment");
        }
    }

    public SubjectEnrollment rejectSubjectEnrollment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
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
                    Subject subject = requireSubject(connection, subjectId);
                    requireEnrollmentAccess(actorUserId, sessionId, actorProfileType,
                            studentUserId, course.organizationId(), course.id(), subject.id(), sourceIp, false);
                    SubjectEnrollment current = enrollmentDAO.findSubjectEnrollment(
                                    connection,
                                    studentUserId,
                                    courseId,
                                    subjectId
                            )
                            .orElseThrow(() -> new IllegalArgumentException("Subject enrollment request not found"));
                    if (current.state() != EnrollmentState.PENDING) {
                        throw new IllegalStateException("Only pending subject enrollment requests can be rejected");
                    }
                    enrollmentDAO.updateSubjectState(
                            connection,
                            studentUserId,
                            courseId,
                            subjectId,
                            EnrollmentState.PENDING,
                            EnrollmentState.REJECTED,
                            current.startDate(),
                            current.endDate()
                    );
                    auditService.record(connection, actorUserId, sessionId, "SUBJECT_ENROLL_REJECT",
                            "subject_enrollment", subjectEnrollmentIdentifier(studentUserId, courseId, subjectId),
                            "success", sourceIp);
                    connection.commit();
                    return enrollmentDAO.findSubjectEnrollment(connection, studentUserId, courseId, subjectId)
                            .orElseThrow(() -> new IllegalStateException("Rejected subject enrollment was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "SUBJECT_ENROLL_REJECT", "subject_enrollment",
                    subjectEnrollmentIdentifier(studentUserId, courseId, subjectId), sourceIp);
            throw wrap(exception, "Failed to reject subject enrollment");
        }
    }

    public SubjectEnrollment updateSubjectEnrollment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long courseId,
            long subjectId,
            EnrollmentState state,
            LocalDate startDate,
            LocalDate endDate,
            String sourceIp
    ) {
        Objects.requireNonNull(state, "state is required");
        try {
            requireValidDates(startDate, endDate);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course course = requireCourse(connection, courseId);
                    Subject subject = requireSubject(connection, subjectId);
                    requireEnrollmentAccess(actorUserId, sessionId, actorProfileType,
                            studentUserId, course.organizationId(), course.id(), subject.id(), sourceIp, false);
                    SubjectEnrollment current = enrollmentDAO.findSubjectEnrollment(
                                    connection,
                                    studentUserId,
                                    courseId,
                                    subjectId
                            )
                            .orElseThrow(() -> new IllegalArgumentException("Subject enrollment not found"));
                    LocalDate effectiveStartDate = startDate != null ? startDate : current.startDate();
                    LocalDate effectiveEndDate = effectiveSubjectEnrollmentEndDate(state, endDate);
                    requireValidDates(effectiveStartDate, effectiveEndDate);
                    if (state == EnrollmentState.ACTIVE) {
                        validateStudent(connection, studentUserId);
                        requireActiveCourse(course);
                        requireActiveSubject(subject);
                        CourseSubjectAssociation association = courseSubjectDAO
                                .findByCourseAndSubject(connection, courseId, subjectId)
                                .orElseThrow(() -> new IllegalArgumentException("Subject is not integrated in the course"));
                        if (association.state() != CourseSubjectState.ACTIVE) {
                            throw new IllegalStateException("Subject-course association must be active");
                        }
                        if (course.organizationId() != subject.organizationId()) {
                            throw new IllegalArgumentException("Course and subject must belong to the same organization");
                        }
                        if (!enrollmentDAO.hasActiveCourseEnrollmentCovering(
                                connection,
                                studentUserId,
                                courseId,
                                effectiveStartDate,
                                effectiveEndDate
                        )) {
                            throw new IllegalStateException("Student must be actively enrolled in the course for the full subject period");
                        }
                    }
                    if (state == EnrollmentState.WITHDRAWN) {
                        classGroupEnrollmentDAO.withdrawActiveInSubject(
                                connection,
                                studentUserId,
                                courseId,
                                subjectId,
                                effectiveEndDate
                        );
                    }
                    enrollmentDAO.updateSubjectEnrollment(
                            connection,
                            studentUserId,
                            courseId,
                            subjectId,
                            state,
                            effectiveStartDate,
                            effectiveEndDate
                    );
                    if (state == EnrollmentState.ACTIVE) {
                        gradeLifecycleService.ensureSubjectGradeSheetDraft(connection, subject);
                    }
                    auditService.record(connection, actorUserId, sessionId, "SUBJECT_ENROLL_UPDATE",
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
            auditFailure(actorUserId, sessionId, "SUBJECT_ENROLL_UPDATE", "subject_enrollment",
                    subjectEnrollmentIdentifier(studentUserId, courseId, subjectId), sourceIp);
            throw wrap(exception, "Failed to update subject enrollment");
        }
    }

    public void deleteSubjectEnrollment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
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
                    Subject subject = requireSubject(connection, subjectId);
                    requireEnrollmentAccess(actorUserId, sessionId, actorProfileType,
                            studentUserId, course.organizationId(), course.id(), subject.id(), sourceIp, false);
                    enrollmentDAO.findSubjectEnrollment(connection, studentUserId, courseId, subjectId)
                            .orElseThrow(() -> new IllegalArgumentException("Subject enrollment not found"));
                    classGroupEnrollmentDAO.deleteInSubject(connection, studentUserId, courseId, subjectId);
                    enrollmentDAO.deleteSubjectEnrollment(connection, studentUserId, courseId, subjectId);
                    auditService.record(connection, actorUserId, sessionId, "SUBJECT_ENROLL_DELETE",
                            "subject_enrollment", subjectEnrollmentIdentifier(studentUserId, courseId, subjectId),
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
            auditFailure(actorUserId, sessionId, "SUBJECT_ENROLL_DELETE", "subject_enrollment",
                    subjectEnrollmentIdentifier(studentUserId, courseId, subjectId), sourceIp);
            throw wrap(exception, "Failed to delete subject enrollment");
        }
    }

    public void updateSubjectEnrollmentPolicy(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long courseId,
            long subjectId,
            EnrollmentApprovalMode mode,
            String sourceIp
    ) {
        Objects.requireNonNull(mode, "mode is required");
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Course course = requireCourse(connection, courseId);
                    Subject subject = requireSubject(connection, subjectId);
                    requireEnrollmentAccess(actorUserId, sessionId, actorProfileType,
                            1L, course.organizationId(), course.id(), subject.id(), sourceIp, false);
                    courseSubjectDAO.findByCourseAndSubject(connection, courseId, subjectId)
                            .orElseThrow(() -> new IllegalArgumentException("Subject is not integrated in the course"));
                    enrollmentApprovalPolicyDAO.upsertSubjectMode(connection, courseId, subjectId, mode);
                    auditService.record(connection, actorUserId, sessionId, "SUBJECT_ENROLL_POLICY_UPDATE",
                            "subject_enrollment_policy", courseId + ":" + subjectId, "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "SUBJECT_ENROLL_POLICY_UPDATE", "subject_enrollment_policy",
                    courseId + ":" + subjectId, sourceIp);
            throw wrap(exception, "Failed to update subject enrollment policy");
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
                            studentUserId, course.organizationId(), course.id(), subjectId, sourceIp, true);
                    requireActiveCourse(course);
                    Subject subject = requireSubject(connection, subjectId);
                    requireActiveSubject(subject);
                    SubjectEnrollment current = enrollmentDAO
                            .findSubjectEnrollment(connection, studentUserId, courseId, subjectId)
                            .orElseThrow(() -> new IllegalArgumentException("Subject enrollment not found"));
                    if (current.startDate() != null && withdrawalDate.isBefore(current.startDate())) {
                        throw new IllegalArgumentException("Withdrawal date cannot be before enrollment start date");
                    }
                    classGroupEnrollmentDAO.withdrawActiveInSubject(
                            connection,
                            studentUserId,
                            courseId,
                            subjectId,
                            withdrawalDate
                    );
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

    private void validateSubjectEnrollmentRequestContext(
            Connection connection,
            Course course,
            Subject subject,
            SubjectEnrollmentCommand command
    ) throws SQLException {
        validateStudent(connection, command.studentUserId());
        requireActiveCourse(course);
        requireActiveSubject(subject);
        CourseSubjectAssociation association = courseSubjectDAO
                .findByCourseAndSubject(connection, command.courseId(), command.subjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject is not integrated in the course"));
        if (association.state() != CourseSubjectState.ACTIVE) {
            throw new IllegalStateException("Subject-course association must be active");
        }
        if (course.organizationId() != subject.organizationId()) {
            throw new IllegalArgumentException("Course and subject must belong to the same organization");
        }
        if (!enrollmentDAO.hasActiveCourseEnrollmentCovering(
                connection,
                command.studentUserId(),
                command.courseId(),
                command.startDate(),
                command.endDate()
        )) {
            throw new IllegalStateException("Student must be actively enrolled in the course for the full subject period");
        }
    }

    private void requireStudentSelfAccess(
            long actorUserId,
            Long sessionId,
            long studentUserId,
            String sourceIp
    ) {
        if (actorUserId != studentUserId) {
            throw new SecurityException("Students can only manage their own enrollment requests");
        }
        AuthorizationDecision selfDecision = permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                AccessProfileType.STUDENT,
                AuthorizationPolicy.VIEW_REPORTS,
                AccessEntityType.SELF,
                studentUserId,
                sourceIp
        ));
        if (!selfDecision.allowed()) {
            throw new SecurityException("Missing student self-service context: " + selfDecision.reason());
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
            String sourceIp,
            boolean allowStudentSelf
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
        AuthorizationDecision coordinatorDecision = AuthorizationDecision.deny("coordinator_profile_required");
        if (subjectId != null && actorProfileType == AccessProfileType.COORDINATOR) {
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
        if (allowStudentSelf && actorProfileType == AccessProfileType.STUDENT && actorUserId == studentUserId) {
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
                + adminDecision.reason() + "/" + courseDecision.reason() + "/" + subjectDecision.reason()
                + "/" + coordinatorDecision.reason());
    }

    private CourseEnrollmentCommand normalizeCourseCommand(CourseEnrollmentCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.studentUserId() <= 0) {
            throw new IllegalArgumentException("Enrollment student is required");
        }
        if (command.courseId() <= 0) {
            throw new IllegalArgumentException("Enrollment course is required");
        }
        requireValidDates(command.startDate(), command.endDate());
        LocalDate startDate = command.startDate() == null ? LocalDate.now(clock) : command.startDate();
        return new CourseEnrollmentCommand(
                command.studentUserId(),
                command.courseId(),
                startDate,
                command.endDate()
        );
    }

    private static void validateCourseEnrollmentState(EnrollmentState state) {
        if (state == EnrollmentState.PENDING || state == EnrollmentState.REJECTED) {
            throw new IllegalArgumentException("Unsupported course enrollment state: " + state);
        }
    }

    private LocalDate effectiveCourseEnrollmentEndDate(EnrollmentState state, LocalDate endDate) {
        if ((state == EnrollmentState.WITHDRAWN || state == EnrollmentState.COMPLETED) && endDate == null) {
            return LocalDate.now(clock);
        }
        return endDate;
    }

    private LocalDate effectiveSubjectEnrollmentEndDate(EnrollmentState state, LocalDate endDate) {
        if ((state == EnrollmentState.WITHDRAWN || state == EnrollmentState.COMPLETED) && endDate == null) {
            return LocalDate.now(clock);
        }
        return endDate;
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
