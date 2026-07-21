package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.ClassGroupEnrollmentDAO;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseSubjectDAO;
import pt.isel.gape.learning.dao.EnrollmentApprovalPolicyDAO;
import pt.isel.gape.learning.dao.EnrollmentDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupEnrollment;
import pt.isel.gape.learning.model.ClassGroupEnrollmentCommand;
import pt.isel.gape.learning.model.ClassGroupState;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.EnrollmentApprovalMode;
import pt.isel.gape.learning.model.EnrollmentState;
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

public final class ClassGroupEnrollmentService {

    private final ConnectionProvider connectionProvider;
    private final ClassGroupDAO classGroupDAO;
    private final ClassGroupEnrollmentDAO classGroupEnrollmentDAO;
    private final CourseDAO courseDAO;
    private final SubjectDAO subjectDAO;
    private final CourseSubjectDAO courseSubjectDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final GradeLifecycleService gradeLifecycleService;
    private final EnrollmentApprovalPolicyDAO enrollmentApprovalPolicyDAO;
    private final PermissionChecker permissionChecker;
    private final AuditService auditService;
    private final Clock clock;

    public ClassGroupEnrollmentService(
            ConnectionProvider connectionProvider,
            ClassGroupDAO classGroupDAO,
            ClassGroupEnrollmentDAO classGroupEnrollmentDAO,
            CourseDAO courseDAO,
            SubjectDAO subjectDAO,
            CourseSubjectDAO courseSubjectDAO,
            EnrollmentDAO enrollmentDAO,
            EnrollmentApprovalPolicyDAO enrollmentApprovalPolicyDAO,
            PermissionChecker permissionChecker,
            AuditService auditService,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.classGroupDAO = Objects.requireNonNull(classGroupDAO, "classGroupDAO is required");
        this.classGroupEnrollmentDAO = Objects.requireNonNull(
                classGroupEnrollmentDAO,
                "classGroupEnrollmentDAO is required"
        );
        this.courseDAO = Objects.requireNonNull(courseDAO, "courseDAO is required");
        this.subjectDAO = Objects.requireNonNull(subjectDAO, "subjectDAO is required");
        this.courseSubjectDAO = Objects.requireNonNull(courseSubjectDAO, "courseSubjectDAO is required");
        this.enrollmentDAO = Objects.requireNonNull(enrollmentDAO, "enrollmentDAO is required");
        this.gradeLifecycleService = new GradeLifecycleService(connectionProvider, clock);
        this.enrollmentApprovalPolicyDAO = Objects.requireNonNull(
                enrollmentApprovalPolicyDAO,
                "enrollmentApprovalPolicyDAO is required"
        );
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public ClassGroupEnrollmentService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new ClassGroupDAO(connectionProvider),
                new ClassGroupEnrollmentDAO(connectionProvider),
                new CourseDAO(connectionProvider),
                new SubjectDAO(connectionProvider),
                new CourseSubjectDAO(connectionProvider),
                new EnrollmentDAO(connectionProvider),
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

    public ClassGroupEnrollment enrollStudentInClassGroup(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ClassGroupEnrollmentCommand command,
            String sourceIp
    ) {
        try {
            ClassGroupEnrollmentCommand normalized = normalizeCommand(command);
            long classGroupId = normalized.classGroupId();
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ClassGroup classGroup = classGroupDAO.lockById(connection, classGroupId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Class group not found: " + classGroupId
                            ));
                    normalized = deriveOccurrencePeriod(normalized, classGroup);
                    Course course = requireCourse(connection, classGroup.courseId());
                    Subject subject = requireSubject(connection, classGroup.subjectId());
                    CourseSubjectAssociation association = requireAssociation(
                            connection,
                            classGroup.courseId(),
                            classGroup.subjectId()
                    );
                    requireClassGroupEnrollmentAccess(
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            normalized.studentUserId(),
                            classGroup,
                            sourceIp,
                            false
                    );
                    validateStudent(connection, normalized.studentUserId());
                    requireActiveContext(classGroup, course, subject, association);
                    if (classGroupEnrollmentDAO.findEnrollment(
                            connection,
                            normalized.studentUserId(),
                            normalized.classGroupId()
                    ).isPresent()) {
                        throw new IllegalStateException("Class group enrollment already exists");
                    }
                    if (!classGroupEnrollmentDAO.lockActiveCourseOccurrenceEnrollmentCovering(
                            connection,
                            normalized.studentUserId(),
                            classGroup.id(),
                            normalized.startDate(),
                            normalized.endDate()
                    )) {
                        throw new IllegalStateException(
                                "Student must be actively enrolled in the course occurrence for the full class group period"
                        );
                    }
                    long activeEnrollments = classGroupDAO.countActiveEnrollments(connection, normalized.classGroupId());
                    if (classGroup.maxStudents() != null && activeEnrollments >= classGroup.maxStudents()) {
                        throw new IllegalStateException("Class group maximum capacity exceeded");
                    }
                    classGroupEnrollmentDAO.enroll(connection, normalized);
                    gradeLifecycleService.ensureClassGroupGradeSheetDraft(connection, classGroup, subject);
                    auditService.record(connection, actorUserId, sessionId, "CLASS_GROUP_ENROLL",
                            "class_group_enrollment",
                            enrollmentIdentifier(normalized.studentUserId(), normalized.classGroupId()),
                            "success", sourceIp);
                    connection.commit();
                    return classGroupEnrollmentDAO.findEnrollment(
                                    connection,
                                    normalized.studentUserId(),
                                    normalized.classGroupId()
                            )
                            .orElseThrow(() -> new IllegalStateException(
                                    "Created class group enrollment was not found"
                            ));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CLASS_GROUP_ENROLL",
                    command == null
                            ? "new"
                            : enrollmentIdentifier(command.studentUserId(), command.classGroupId()),
                    sourceIp);
            throw wrap(exception, "Failed to enroll student in class group");
        }
    }

    public ClassGroupEnrollment requestStudentInClassGroup(
            long actorUserId,
            Long sessionId,
            ClassGroupEnrollmentCommand command,
            String sourceIp
    ) {
        try {
            ClassGroupEnrollmentCommand normalized = normalizeCommand(command);
            long classGroupId = normalized.classGroupId();
            if (actorUserId != normalized.studentUserId()) {
                throw new SecurityException("Students can only request their own class group enrollments");
            }
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ClassGroup classGroup = classGroupDAO.lockById(connection, classGroupId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Class group not found: " + classGroupId
                            ));
                    normalized = deriveOccurrencePeriod(normalized, classGroup);
                    Course course = requireCourse(connection, classGroup.courseId());
                    Subject subject = requireSubject(connection, classGroup.subjectId());
                    CourseSubjectAssociation association = requireAssociation(
                            connection,
                            classGroup.courseId(),
                            classGroup.subjectId()
                    );
                    requireStudentSelfAccess(actorUserId, sessionId, normalized.studentUserId(), sourceIp);
                    validateClassGroupRequestContext(connection, classGroup, course, subject, association, normalized);

                    ClassGroupEnrollment current = classGroupEnrollmentDAO
                            .findEnrollment(connection, normalized.studentUserId(), normalized.classGroupId())
                            .orElse(null);
                    if (current != null
                            && (current.state() == EnrollmentState.ACTIVE || current.state() == EnrollmentState.PENDING)) {
                        throw new IllegalStateException("Class group enrollment is already active or pending");
                    }

                    EnrollmentApprovalMode mode = enrollmentApprovalPolicyDAO.classGroupMode(
                            connection,
                            normalized.classGroupId()
                    );
                    EnrollmentState targetState = mode == EnrollmentApprovalMode.AUTO_APPROVE
                            ? EnrollmentState.ACTIVE
                            : EnrollmentState.PENDING;

                    if (targetState == EnrollmentState.ACTIVE) {
                        validateClassGroupApprovalAvailability(connection, classGroup, normalized);
                    }

                    if (current == null) {
                        if (targetState == EnrollmentState.ACTIVE) {
                            classGroupEnrollmentDAO.enroll(connection, normalized);
                        } else {
                            classGroupEnrollmentDAO.request(connection, normalized);
                        }
                    } else {
                        classGroupEnrollmentDAO.reactivateRequest(connection, normalized, targetState);
                    }
                    if (targetState == EnrollmentState.ACTIVE) {
                        gradeLifecycleService.ensureClassGroupGradeSheetDraft(connection, classGroup, subject);
                    }

                    auditService.record(connection, actorUserId, sessionId,
                            targetState == EnrollmentState.ACTIVE ? "CLASS_GROUP_ENROLL_AUTO_APPROVE" : "CLASS_GROUP_ENROLL_REQUEST",
                            "class_group_enrollment",
                            enrollmentIdentifier(normalized.studentUserId(), normalized.classGroupId()),
                            "success", sourceIp);
                    connection.commit();
                    return classGroupEnrollmentDAO.findEnrollment(
                                    connection,
                                    normalized.studentUserId(),
                                    normalized.classGroupId()
                            )
                            .orElseThrow(() -> new IllegalStateException(
                                    "Class group enrollment request was not found"
                            ));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CLASS_GROUP_ENROLL_REQUEST",
                    command == null
                            ? "new"
                            : enrollmentIdentifier(command.studentUserId(), command.classGroupId()),
                    sourceIp);
            throw wrap(exception, "Failed to request class group enrollment");
        }
    }

    public ClassGroupEnrollment approveClassGroupEnrollment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long classGroupId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ClassGroup classGroup = classGroupDAO.lockById(connection, classGroupId)
                            .orElseThrow(() -> new IllegalArgumentException("Class group not found: " + classGroupId));
                    requireClassGroupOpenForEnrollmentChanges(classGroup);
                    Course course = requireCourse(connection, classGroup.courseId());
                    Subject subject = requireSubject(connection, classGroup.subjectId());
                    CourseSubjectAssociation association = requireAssociation(
                            connection,
                            classGroup.courseId(),
                            classGroup.subjectId()
                    );
                    requireClassGroupEnrollmentAccess(
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            studentUserId,
                            classGroup,
                            sourceIp,
                            false
                    );
                    ClassGroupEnrollment current = classGroupEnrollmentDAO.findEnrollment(
                                    connection,
                                    studentUserId,
                                    classGroupId
                            )
                            .orElseThrow(() -> new IllegalArgumentException("Class group enrollment request not found"));
                    if (current.state() != EnrollmentState.PENDING) {
                        throw new IllegalStateException("Only pending class group enrollment requests can be approved");
                    }
                    ClassGroupEnrollmentCommand approval = new ClassGroupEnrollmentCommand(
                            studentUserId,
                            classGroupId,
                            classGroup.startsAt(),
                            classGroup.endsAt()
                    );
                    validateClassGroupRequestContext(connection, classGroup, course, subject, association, approval);
                    validateClassGroupApprovalAvailability(connection, classGroup, approval);
                    classGroupEnrollmentDAO.updateState(
                            connection,
                            studentUserId,
                            classGroupId,
                            EnrollmentState.PENDING,
                            EnrollmentState.ACTIVE,
                            approval.startDate(),
                            approval.endDate()
                    );
                    gradeLifecycleService.ensureClassGroupGradeSheetDraft(connection, classGroup, subject);
                    auditService.record(connection, actorUserId, sessionId, "CLASS_GROUP_ENROLL_APPROVE",
                            "class_group_enrollment",
                            enrollmentIdentifier(studentUserId, classGroupId),
                            "success", sourceIp);
                    connection.commit();
                    return classGroupEnrollmentDAO.findEnrollment(connection, studentUserId, classGroupId)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Approved class group enrollment was not found"
                            ));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CLASS_GROUP_ENROLL_APPROVE",
                    enrollmentIdentifier(studentUserId, classGroupId), sourceIp);
            throw wrap(exception, "Failed to approve class group enrollment");
        }
    }

    public ClassGroupEnrollment rejectClassGroupEnrollment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long classGroupId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ClassGroup classGroup = requireClassGroup(connection, classGroupId);
                    requireClassGroupOpenForEnrollmentChanges(classGroup);
                    requireClassGroupEnrollmentAccess(
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            studentUserId,
                            classGroup,
                            sourceIp,
                            false
                    );
                    ClassGroupEnrollment current = classGroupEnrollmentDAO.findEnrollment(
                                    connection,
                                    studentUserId,
                                    classGroupId
                            )
                            .orElseThrow(() -> new IllegalArgumentException("Class group enrollment request not found"));
                    if (current.state() != EnrollmentState.PENDING) {
                        throw new IllegalStateException("Only pending class group enrollment requests can be rejected");
                    }
                    classGroupEnrollmentDAO.updateState(
                            connection,
                            studentUserId,
                            classGroupId,
                            EnrollmentState.PENDING,
                            EnrollmentState.REJECTED,
                            current.startDate(),
                            current.endDate()
                    );
                    auditService.record(connection, actorUserId, sessionId, "CLASS_GROUP_ENROLL_REJECT",
                            "class_group_enrollment",
                            enrollmentIdentifier(studentUserId, classGroupId),
                            "success", sourceIp);
                    connection.commit();
                    return classGroupEnrollmentDAO.findEnrollment(connection, studentUserId, classGroupId)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Rejected class group enrollment was not found"
                            ));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CLASS_GROUP_ENROLL_REJECT",
                    enrollmentIdentifier(studentUserId, classGroupId), sourceIp);
            throw wrap(exception, "Failed to reject class group enrollment");
        }
    }

    public void updateClassGroupEnrollmentPolicy(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            EnrollmentApprovalMode mode,
            String sourceIp
    ) {
        Objects.requireNonNull(mode, "mode is required");
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ClassGroup classGroup = requireClassGroup(connection, classGroupId);
                    requireClassGroupOpenForEnrollmentChanges(classGroup);
                    requireClassGroupEnrollmentAccess(
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            1L,
                            classGroup,
                            sourceIp,
                            false
                    );
                    enrollmentApprovalPolicyDAO.upsertClassGroupMode(connection, classGroupId, mode);
                    auditService.record(connection, actorUserId, sessionId, "CLASS_GROUP_ENROLL_POLICY_UPDATE",
                            "class_group_enrollment_policy", Long.toString(classGroupId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CLASS_GROUP_ENROLL_POLICY_UPDATE",
                    Long.toString(classGroupId), sourceIp);
            throw wrap(exception, "Failed to update class group enrollment policy");
        }
    }

    public ClassGroupEnrollment withdrawStudentFromClassGroup(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long classGroupId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ClassGroup classGroup = requireClassGroup(connection, classGroupId);
                    requireClassGroupOpenForEnrollmentChanges(classGroup);
                    requireClassGroupEnrollmentAccess(
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            studentUserId,
                            classGroup,
                            sourceIp,
                            true
                    );
                    LocalDate withdrawalDate = boundedWithdrawalDate(classGroup);
                    ClassGroupEnrollment current = classGroupEnrollmentDAO
                            .findEnrollment(connection, studentUserId, classGroupId)
                            .orElseThrow(() -> new IllegalArgumentException("Class group enrollment not found"));
                    if (current.startDate() != null && withdrawalDate.isBefore(current.startDate())) {
                        throw new IllegalArgumentException("Withdrawal date cannot be before enrollment start date");
                    }
                    classGroupEnrollmentDAO.withdraw(connection, studentUserId, classGroupId, withdrawalDate);
                    gradeLifecycleService.synchronizeAfterClassGroupEnrollmentChange(connection, classGroupId);
                    auditService.record(connection, actorUserId, sessionId, "CLASS_GROUP_WITHDRAW",
                            "class_group_enrollment", enrollmentIdentifier(studentUserId, classGroupId),
                            "success", sourceIp);
                    connection.commit();
                    return classGroupEnrollmentDAO.findEnrollment(connection, studentUserId, classGroupId)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Updated class group enrollment was not found"
                            ));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CLASS_GROUP_WITHDRAW",
                    enrollmentIdentifier(studentUserId, classGroupId), sourceIp);
            throw wrap(exception, "Failed to withdraw student from class group");
        }
    }

    public void deleteClassGroupEnrollment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long classGroupId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ClassGroup classGroup = requireClassGroup(connection, classGroupId);
                    requireClassGroupOpenForEnrollmentChanges(classGroup);
                    requireClassGroupEnrollmentAccess(
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            studentUserId,
                            classGroup,
                            sourceIp,
                            false
                    );
                    classGroupEnrollmentDAO.findEnrollment(connection, studentUserId, classGroupId)
                            .orElseThrow(() -> new IllegalArgumentException("Class group enrollment not found"));
                    auditService.record(connection, actorUserId, sessionId, "CLASS_GROUP_ENROLL_DELETE",
                            "class_group_enrollment", enrollmentIdentifier(studentUserId, classGroupId),
                            "success", sourceIp);
                    classGroupEnrollmentDAO.deleteEnrollment(connection, studentUserId, classGroupId);
                    gradeLifecycleService.synchronizeAfterClassGroupEnrollmentChange(connection, classGroupId);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CLASS_GROUP_ENROLL_DELETE",
                    enrollmentIdentifier(studentUserId, classGroupId), sourceIp);
            throw wrap(exception, "Failed to delete class group enrollment");
        }
    }

    private ClassGroup requireClassGroup(Connection connection, long classGroupId) throws SQLException {
        return classGroupDAO.findById(connection, classGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Class group not found: " + classGroupId));
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

    private void validateStudent(Connection connection, long studentUserId) throws SQLException {
        if (!enrollmentDAO.activeStudentExists(connection, studentUserId)) {
            throw new IllegalArgumentException("Class group enrollment requires an active student");
        }
    }

    private void validateClassGroupRequestContext(
            Connection connection,
            ClassGroup classGroup,
            Course course,
            Subject subject,
            CourseSubjectAssociation association,
            ClassGroupEnrollmentCommand command
    ) throws SQLException {
        validateStudent(connection, command.studentUserId());
        requireActiveContext(classGroup, course, subject, association);
        if (!classGroupEnrollmentDAO.lockActiveCourseOccurrenceEnrollmentCovering(
                connection,
                command.studentUserId(),
                classGroup.id(),
                command.startDate(),
                command.endDate()
        )) {
            throw new IllegalStateException(
                    "Student must be actively enrolled in the course occurrence for the full class group period"
            );
        }
    }

    private void validateClassGroupApprovalAvailability(
            Connection connection,
            ClassGroup classGroup,
            ClassGroupEnrollmentCommand command
    ) throws SQLException {
        long activeEnrollments = classGroupDAO.countActiveEnrollments(connection, command.classGroupId());
        if (classGroup.maxStudents() != null && activeEnrollments >= classGroup.maxStudents()) {
            throw new IllegalStateException("Class group maximum capacity exceeded");
        }
    }

    private void requireStudentSelfAccess(
            long actorUserId,
            Long sessionId,
            long studentUserId,
            String sourceIp
    ) {
        if (actorUserId != studentUserId) {
            throw new SecurityException("Students can only manage their own class group enrollment requests");
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

    private void requireActiveContext(
            ClassGroup classGroup,
            Course course,
            Subject subject,
            CourseSubjectAssociation association
    ) {
        if (classGroup.state() != ClassGroupState.ACTIVE) {
            throw new IllegalStateException("Class group enrollment requires an active class group");
        }
        if (course.state() != CourseState.ACTIVE) {
            throw new IllegalStateException("Class group enrollment requires an active course");
        }
        if (subject.state() != SubjectState.ACTIVE) {
            throw new IllegalStateException("Class group enrollment requires an active subject");
        }
    }

    private void requireClassGroupEnrollmentAccess(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            ClassGroup classGroup,
            String sourceIp,
            boolean allowStudentSelf
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
                return;
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
                return;
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
        throw new SecurityException("Missing class group enrollment context: "
                + adminDecision.reason() + "/" + coordinatorDecision.reason() + "/" + teacherDecision.reason());
    }

    private ClassGroupEnrollmentCommand normalizeCommand(ClassGroupEnrollmentCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.studentUserId() <= 0) {
            throw new IllegalArgumentException("Class group enrollment student is required");
        }
        if (command.classGroupId() <= 0) {
            throw new IllegalArgumentException("Class group enrollment class group is required");
        }
        return new ClassGroupEnrollmentCommand(
                command.studentUserId(),
                command.classGroupId(),
                null,
                null
        );
    }

    private static void requireClassGroupOpenForEnrollmentChanges(ClassGroup classGroup) {
        if (classGroup.state() == ClassGroupState.COMPLETED) {
            throw new IllegalStateException("Completed class groups cannot be changed");
        }
    }

    private static void requireValidDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Class group enrollment end date cannot be before start date");
        }
    }

    private static ClassGroupEnrollmentCommand deriveOccurrencePeriod(
            ClassGroupEnrollmentCommand command,
            ClassGroup classGroup
    ) {
        requireValidDates(classGroup.startsAt(), classGroup.endsAt());
        return new ClassGroupEnrollmentCommand(
                command.studentUserId(),
                command.classGroupId(),
                classGroup.startsAt(),
                classGroup.endsAt()
        );
    }

    private LocalDate boundedWithdrawalDate(ClassGroup classGroup) {
        LocalDate today = LocalDate.now(clock);
        if (today.isBefore(classGroup.startsAt())) {
            return classGroup.startsAt();
        }
        if (today.isAfter(classGroup.endsAt())) {
            return classGroup.endsAt();
        }
        return today;
    }

    private static String enrollmentIdentifier(long studentUserId, long classGroupId) {
        return studentUserId + ":" + classGroupId;
    }

    private void auditFailure(
            long actorUserId,
            Long sessionId,
            String operationType,
            String affectedIdentifier,
            String sourceIp
    ) {
        auditService.record(actorUserId, sessionId, operationType,
                "class_group_enrollment", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
