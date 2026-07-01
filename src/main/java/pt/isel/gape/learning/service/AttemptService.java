package pt.isel.gape.learning.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.AssessmentEnrollmentDAO;
import pt.isel.gape.learning.dao.AttemptDAO;
import pt.isel.gape.learning.dao.QuestionDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AssessmentMode;
import pt.isel.gape.learning.model.AssessmentState;
import pt.isel.gape.learning.model.Attempt;
import pt.isel.gape.learning.model.AttemptState;
import pt.isel.gape.learning.model.EnrollmentApprovalMode;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class AttemptService {

    private final ConnectionProvider connectionProvider;
    private final AssessmentDAO assessmentDAO;
    private final AssessmentEnrollmentDAO assessmentEnrollmentDAO;
    private final AttemptDAO attemptDAO;
    private final QuestionDAO questionDAO;
    private final AssessmentAccessPolicy accessPolicy;
    private final AuditService auditService;
    private final Clock clock;

    public AttemptService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new AssessmentDAO(connectionProvider),
                new AssessmentEnrollmentDAO(connectionProvider),
                new AttemptDAO(connectionProvider),
                new QuestionDAO(connectionProvider),
                new PermissionDAO(connectionProvider),
                clock
        );
    }

    public AttemptService(
            ConnectionProvider connectionProvider,
            AssessmentDAO assessmentDAO,
            AssessmentEnrollmentDAO assessmentEnrollmentDAO,
            AttemptDAO attemptDAO,
            QuestionDAO questionDAO,
            PermissionDAO permissionDAO,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.assessmentDAO = Objects.requireNonNull(assessmentDAO, "assessmentDAO is required");
        this.assessmentEnrollmentDAO = Objects.requireNonNull(
                assessmentEnrollmentDAO,
                "assessmentEnrollmentDAO is required"
        );
        this.attemptDAO = Objects.requireNonNull(attemptDAO, "attemptDAO is required");
        this.questionDAO = Objects.requireNonNull(questionDAO, "questionDAO is required");
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

    public Attempt startAttempt(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long assessmentId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    LocalDateTime now = currentMinute();
                    assessmentDAO.synchronizeTemporalStates(connection, now);
                    Assessment assessment = assessmentDAO.lockById(connection, assessmentId)
                            .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + assessmentId));
                    synchronizeAutomaticEnrollment(connection, assessment);
                    requireAvailableForAttempt(assessment, now);
                    accessPolicy.requireStudentExecutionAccess(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            assessment,
                            sourceIp
                    );
                    requireQuestionsAvailable(connection, assessment.id());
                    long attempts = assessmentDAO.countAttempts(connection, actorUserId, assessmentId);
                    if (assessment.attemptsLimit() != null && attempts >= assessment.attemptsLimit()) {
                        throw new IllegalStateException("Assessment attempts limit has been reached");
                    }
                    int attemptNumber = assessmentDAO.nextAttemptNumber(connection, actorUserId, assessmentId);
                    long attemptId = attemptDAO.create(
                            connection,
                            actorUserId,
                            assessmentId,
                            attemptNumber,
                            AttemptState.IN_PROGRESS,
                            now
                    );
                    auditService.record(connection, actorUserId, sessionId, "ATTEMPT_START",
                            "attempt", Long.toString(attemptId), "success", sourceIp);
                    Attempt attempt = requireAttempt(connection, attemptId);
                    connection.commit();
                    return attempt;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ATTEMPT_START", Long.toString(assessmentId), sourceIp);
            throw wrap(exception, "Failed to start attempt");
        }
    }

    public Attempt submitAttempt(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long attemptId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    LocalDateTime now = currentMinute();
                    assessmentDAO.synchronizeTemporalStates(connection, now);
                    Attempt attempt = attemptDAO.lockById(connection, attemptId)
                            .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));
                    requireAttemptOwner(actorUserId, actorProfileType, attempt);
                    Assessment assessment = assessmentDAO.lockById(connection, attempt.assessmentId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Assessment not found: " + attempt.assessmentId()));
                    synchronizeAutomaticEnrollment(connection, assessment);
                    requireAvailableForAttempt(assessment, now);
                    accessPolicy.requireStudentExecutionAccess(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            assessment,
                            sourceIp
                    );
                    if (attempt.state() != AttemptState.IN_PROGRESS) {
                        throw new IllegalStateException("Only in-progress attempts can be submitted");
                    }
                    if (questionDAO.countMissingRequiredResponses(connection, attempt.id(), assessment.id()) > 0) {
                        throw new IllegalStateException("Required assessment responses are missing");
                    }
                    attemptDAO.submit(connection, attempt.id(), null, AttemptState.SUBMITTED, now);
                    auditService.record(connection, actorUserId, sessionId, "ATTEMPT_SUBMIT",
                            "attempt", Long.toString(attempt.id()), "success", sourceIp);
                    Attempt submitted = requireAttempt(connection, attempt.id());
                    connection.commit();
                    return submitted;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ATTEMPT_SUBMIT", Long.toString(attemptId), sourceIp);
            throw wrap(exception, "Failed to submit attempt");
        }
    }

    public Attempt getAttempt(long attemptId) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                return requireAttempt(connection, attemptId);
            }
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to read attempt");
        }
    }

    public Attempt validateInProgressAttemptAccess(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long attemptId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                LocalDateTime now = currentMinute();
                assessmentDAO.synchronizeTemporalStates(connection, now);
                Attempt attempt = attemptDAO.findById(connection, attemptId)
                        .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));
                requireAttemptOwner(actorUserId, actorProfileType, attempt);
                if (attempt.state() != AttemptState.IN_PROGRESS) {
                    throw new IllegalStateException("Attempt is no longer in progress");
                }
                Assessment assessment = assessmentDAO.findById(connection, attempt.assessmentId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Assessment not found: " + attempt.assessmentId()));
                synchronizeAutomaticEnrollment(connection, assessment);
                requireAvailableForAttempt(assessment, now);
                accessPolicy.requireStudentExecutionAccess(
                        connection,
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        assessment,
                        sourceIp
                );
                return attempt;
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ATTEMPT_ACCESS", Long.toString(attemptId), sourceIp);
            throw wrap(exception, "Failed to validate attempt access");
        }
    }

    private void requireAvailableForAttempt(Assessment assessment, LocalDateTime now) {
        if (assessment.mode() != AssessmentMode.ONLINE) {
            throw new IllegalStateException("In-person assessments cannot be answered online");
        }
        if (assessment.state() != AssessmentState.ACTIVE) {
            throw new IllegalStateException("Assessment is not active");
        }
        if (assessment.availableFrom() != null && now.isBefore(assessment.availableFrom())) {
            throw new IllegalStateException("Assessment is not available yet");
        }
        if (assessment.availableUntil() != null && !now.isBefore(assessment.availableUntil())) {
            throw new IllegalStateException("Assessment is no longer available");
        }
    }

    private void requireQuestionsAvailable(Connection connection, long assessmentId) throws SQLException {
        if (questionDAO.countActiveByAssessment(connection, assessmentId) == 0) {
            throw new IllegalStateException("Assessment has no questions");
        }
    }

    private void synchronizeAutomaticEnrollment(Connection connection, Assessment assessment) throws SQLException {
        if (assessment.enrollmentMode() == EnrollmentApprovalMode.AUTO_APPROVE) {
            assessmentEnrollmentDAO.syncAutomaticEnrollments(
                    connection,
                    assessment.id()
            );
        }
    }

    private static void requireAttemptOwner(long actorUserId, AccessProfileType profileType, Attempt attempt) {
        if (profileType != AccessProfileType.STUDENT || attempt.studentUserId() != actorUserId) {
            throw new SecurityException("Attempt belongs to another student");
        }
    }

    private Attempt requireAttempt(Connection connection, long attemptId) throws SQLException {
        return attemptDAO.findById(connection, attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));
    }

    private LocalDateTime currentMinute() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
    }

    private void auditFailure(
            long actorUserId,
            Long sessionId,
            String operationType,
            String affectedIdentifier,
            String sourceIp
    ) {
        auditService.record(actorUserId, sessionId, operationType,
                "attempt", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
