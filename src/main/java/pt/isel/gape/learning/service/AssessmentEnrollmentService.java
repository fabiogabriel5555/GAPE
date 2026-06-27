package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.AssessmentEnrollmentDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AssessmentEnrollment;
import pt.isel.gape.learning.model.AssessmentEnrollmentCommand;
import pt.isel.gape.learning.model.EnrollmentApprovalMode;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class AssessmentEnrollmentService {

    private final ConnectionProvider connectionProvider;
    private final AssessmentDAO assessmentDAO;
    private final AssessmentEnrollmentDAO assessmentEnrollmentDAO;
    private final AssessmentAccessPolicy accessPolicy;
    private final AuditService auditService;
    private final Clock clock;

    public AssessmentEnrollmentService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new AssessmentDAO(connectionProvider),
                new AssessmentEnrollmentDAO(connectionProvider),
                new PermissionDAO(connectionProvider),
                clock
        );
    }

    public AssessmentEnrollmentService(
            ConnectionProvider connectionProvider,
            AssessmentDAO assessmentDAO,
            AssessmentEnrollmentDAO assessmentEnrollmentDAO,
            PermissionDAO permissionDAO,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.assessmentDAO = Objects.requireNonNull(assessmentDAO, "assessmentDAO is required");
        this.assessmentEnrollmentDAO = Objects.requireNonNull(
                assessmentEnrollmentDAO,
                "assessmentEnrollmentDAO is required"
        );
        Objects.requireNonNull(permissionDAO, "permissionDAO is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
        this.accessPolicy = new AssessmentAccessPolicy(
                assessmentDAO,
                permissionDAO,
                new PermissionChecker(
                        permissionDAO,
                        new ManageOrganizationDAO(connectionProvider),
                        new CoordinateSubjectDAO(connectionProvider),
                        new TeachClassGroupDAO(connectionProvider)
                )
        );
        this.auditService = new AuditService(new ActivityLogDAO(connectionProvider), clock);
    }

    public AssessmentEnrollment enrollStudentInAssessment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            AssessmentEnrollmentCommand command,
            String sourceIp
    ) {
        return manageEnrollment(actorUserId, sessionId, actorProfileType, command, sourceIp, true);
    }

    public AssessmentEnrollment requestStudentInAssessment(
            long actorUserId,
            Long sessionId,
            AssessmentEnrollmentCommand command,
            String sourceIp
    ) {
        if (command == null || actorUserId != command.studentUserId()) {
            throw new SecurityException("Students can only request their own assessment enrollments");
        }
        return manageEnrollment(actorUserId, sessionId, AccessProfileType.STUDENT, command, sourceIp, false);
    }

    private AssessmentEnrollment manageEnrollment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            AssessmentEnrollmentCommand command,
            String sourceIp,
            boolean managerAction
    ) {
        try {
            AssessmentEnrollmentCommand normalized = normalizeCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Assessment assessment = assessmentDAO.lockById(connection, normalized.assessmentId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Assessment not found: " + normalized.assessmentId()
                            ));
                    if (managerAction) {
                        accessPolicy.requireAssessmentManager(
                                connection,
                                actorUserId,
                                sessionId,
                                actorProfileType,
                                assessment,
                                sourceIp
                        );
                    } else {
                        accessPolicy.requireActiveProfile(actorUserId, sessionId, actorProfileType, sourceIp);
                    }
                    requireEligibleStudent(connection, normalized.studentUserId(), assessment.id());
                    AssessmentEnrollment current = assessmentEnrollmentDAO
                            .findEnrollment(connection, normalized.studentUserId(), assessment.id())
                            .orElse(null);
                    if (current != null
                            && (current.state() == EnrollmentState.ACTIVE || current.state() == EnrollmentState.PENDING)) {
                        throw new IllegalStateException("Assessment enrollment is already active or pending");
                    }

                    EnrollmentState targetState = managerAction
                            || assessment.enrollmentMode() == EnrollmentApprovalMode.AUTO_APPROVE
                            ? EnrollmentState.ACTIVE
                            : EnrollmentState.PENDING;
                    if (current == null) {
                        if (targetState == EnrollmentState.ACTIVE) {
                            assessmentEnrollmentDAO.enroll(connection, normalized);
                        } else {
                            assessmentEnrollmentDAO.request(connection, normalized);
                        }
                    } else {
                        assessmentEnrollmentDAO.reactivateRequest(connection, normalized, targetState);
                    }
                    auditService.record(connection, actorUserId, sessionId,
                            targetState == EnrollmentState.ACTIVE
                                    ? "ASSESSMENT_ENROLL"
                                    : "ASSESSMENT_ENROLL_REQUEST",
                            "assessment_enrollment",
                            identifier(normalized.studentUserId(), assessment.id()),
                            "success", sourceIp);
                    connection.commit();
                    return assessmentEnrollmentDAO.findEnrollment(
                                    connection,
                                    normalized.studentUserId(),
                                    assessment.id()
                            )
                            .orElseThrow(() -> new IllegalStateException("Assessment enrollment was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ASSESSMENT_ENROLL", command == null
                    ? "new"
                    : identifier(command.studentUserId(), command.assessmentId()), sourceIp);
            throw wrap(exception, "Failed to enroll student in assessment");
        }
    }

    public AssessmentEnrollment approveAssessmentEnrollment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long assessmentId,
            LocalDate startDate,
            LocalDate endDate,
            String sourceIp
    ) {
        return transitionPending(
                actorUserId,
                sessionId,
                actorProfileType,
                studentUserId,
                assessmentId,
                EnrollmentState.ACTIVE,
                startDate,
                endDate,
                sourceIp,
                "ASSESSMENT_ENROLL_APPROVE"
        );
    }

    public AssessmentEnrollment rejectAssessmentEnrollment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long assessmentId,
            String sourceIp
    ) {
        return transitionPending(
                actorUserId,
                sessionId,
                actorProfileType,
                studentUserId,
                assessmentId,
                EnrollmentState.REJECTED,
                null,
                null,
                sourceIp,
                "ASSESSMENT_ENROLL_REJECT"
        );
    }

    private AssessmentEnrollment transitionPending(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long assessmentId,
            EnrollmentState targetState,
            LocalDate startDate,
            LocalDate endDate,
            String sourceIp,
            String auditType
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Assessment assessment = requireManagedAssessment(
                        connection,
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        assessmentId,
                        sourceIp
                );
                AssessmentEnrollment current = assessmentEnrollmentDAO.findEnrollment(
                                connection,
                                studentUserId,
                                assessment.id()
                        )
                        .orElseThrow(() -> new IllegalArgumentException("Assessment enrollment request not found"));
                if (current.state() != EnrollmentState.PENDING) {
                    throw new IllegalStateException("Only pending assessment enrollment requests can be changed here");
                }
                LocalDate effectiveStart = targetState == EnrollmentState.ACTIVE
                        ? startDate != null ? startDate : current.startDate() == null ? LocalDate.now(clock) : current.startDate()
                        : current.startDate();
                LocalDate effectiveEnd = endDate != null ? endDate : current.endDate();
                assessmentEnrollmentDAO.updateState(
                        connection,
                        studentUserId,
                        assessment.id(),
                        EnrollmentState.PENDING,
                        targetState,
                        effectiveStart,
                        effectiveEnd
                );
                auditService.record(connection, actorUserId, sessionId, auditType, "assessment_enrollment",
                        identifier(studentUserId, assessment.id()), "success", sourceIp);
                connection.commit();
                return assessmentEnrollmentDAO.findEnrollment(connection, studentUserId, assessment.id())
                        .orElseThrow(() -> new IllegalStateException("Assessment enrollment was not found"));
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, auditType, identifier(studentUserId, assessmentId), sourceIp);
            throw wrap(exception, "Failed to update assessment enrollment request");
        }
    }

    public AssessmentEnrollment updateAssessmentEnrollment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long assessmentId,
            EnrollmentState state,
            LocalDate startDate,
            LocalDate endDate,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Assessment assessment = requireManagedAssessment(
                        connection,
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        assessmentId,
                        sourceIp
                );
                requireEligibleStudent(connection, studentUserId, assessment.id());
                assessmentEnrollmentDAO.updateEnrollment(
                        connection,
                        studentUserId,
                        assessment.id(),
                        state,
                        startDate,
                        endDate
                );
                auditService.record(connection, actorUserId, sessionId, "ASSESSMENT_ENROLL_UPDATE",
                        "assessment_enrollment", identifier(studentUserId, assessment.id()), "success", sourceIp);
                connection.commit();
                return assessmentEnrollmentDAO.findEnrollment(connection, studentUserId, assessment.id())
                        .orElseThrow(() -> new IllegalStateException("Assessment enrollment was not found"));
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ASSESSMENT_ENROLL_UPDATE",
                    identifier(studentUserId, assessmentId), sourceIp);
            throw wrap(exception, "Failed to update assessment enrollment");
        }
    }

    public void withdrawAssessmentEnrollment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long assessmentId,
            LocalDate endDate,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Assessment assessment = requireManagedAssessment(
                        connection,
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        assessmentId,
                        sourceIp
                );
                assessmentEnrollmentDAO.withdraw(
                        connection,
                        studentUserId,
                        assessment.id(),
                        endDate == null ? LocalDate.now(clock) : endDate
                );
                auditService.record(connection, actorUserId, sessionId, "ASSESSMENT_ENROLL_WITHDRAW",
                        "assessment_enrollment", identifier(studentUserId, assessment.id()), "success", sourceIp);
                connection.commit();
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ASSESSMENT_ENROLL_WITHDRAW",
                    identifier(studentUserId, assessmentId), sourceIp);
            throw wrap(exception, "Failed to withdraw assessment enrollment");
        }
    }

    public void deleteAssessmentEnrollment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long studentUserId,
            long assessmentId,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Assessment assessment = requireManagedAssessment(
                        connection,
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        assessmentId,
                        sourceIp
                );
                assessmentEnrollmentDAO.deleteEnrollment(connection, studentUserId, assessment.id());
                auditService.record(connection, actorUserId, sessionId, "ASSESSMENT_ENROLL_DELETE",
                        "assessment_enrollment", identifier(studentUserId, assessment.id()), "success", sourceIp);
                connection.commit();
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ASSESSMENT_ENROLL_DELETE",
                    identifier(studentUserId, assessmentId), sourceIp);
            throw wrap(exception, "Failed to delete assessment enrollment");
        }
    }

    public void updateEnrollmentMode(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long assessmentId,
            EnrollmentApprovalMode enrollmentMode,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Assessment assessment = requireManagedAssessment(
                        connection,
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        assessmentId,
                        sourceIp
                );
                assessmentDAO.updateEnrollmentMode(connection, assessment.id(), enrollmentMode);
                if (enrollmentMode == EnrollmentApprovalMode.AUTO_APPROVE) {
                    assessmentEnrollmentDAO.syncAutomaticEnrollments(
                            connection,
                            assessment.id(),
                            LocalDate.now(clock)
                    );
                }
                auditService.record(connection, actorUserId, sessionId, "ASSESSMENT_ENROLL_POLICY",
                        "assessment", Long.toString(assessment.id()), "success", sourceIp);
                connection.commit();
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ASSESSMENT_ENROLL_POLICY", Long.toString(assessmentId), sourceIp);
            throw wrap(exception, "Failed to update assessment enrollment policy");
        }
    }

    private Assessment requireManagedAssessment(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long assessmentId,
            String sourceIp
    ) throws SQLException {
        Assessment assessment = assessmentDAO.lockById(connection, assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));
        accessPolicy.requireAssessmentManager(
                connection,
                actorUserId,
                sessionId,
                actorProfileType,
                assessment,
                sourceIp
        );
        return assessment;
    }

    private void requireEligibleStudent(Connection connection, long studentUserId, long assessmentId)
            throws SQLException {
        if (!assessmentEnrollmentDAO.isStudentEligible(connection, studentUserId, assessmentId)) {
            throw new IllegalStateException("Student must be actively enrolled in the assessment class group or subject");
        }
    }

    private AssessmentEnrollmentCommand normalizeCommand(AssessmentEnrollmentCommand command) {
        Objects.requireNonNull(command, "assessment enrollment command is required");
        if (command.studentUserId() <= 0) {
            throw new IllegalArgumentException("student is required");
        }
        if (command.assessmentId() <= 0) {
            throw new IllegalArgumentException("assessment is required");
        }
        if (command.startDate() != null && command.endDate() != null && command.endDate().isBefore(command.startDate())) {
            throw new IllegalArgumentException("Assessment enrollment end date cannot be before start date");
        }
        return new AssessmentEnrollmentCommand(
                command.studentUserId(),
                command.assessmentId(),
                command.startDate() == null ? LocalDate.now(clock) : command.startDate(),
                command.endDate()
        );
    }

    private void auditFailure(
            long actorUserId,
            Long sessionId,
            String operationType,
            String affectedIdentifier,
            String sourceIp
    ) {
        auditService.record(actorUserId, sessionId, operationType,
                "assessment_enrollment", affectedIdentifier, "failure", sourceIp);
    }

    private static String identifier(long studentUserId, long assessmentId) {
        return studentUserId + ":" + assessmentId;
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
