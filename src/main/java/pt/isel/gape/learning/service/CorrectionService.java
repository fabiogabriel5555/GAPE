package pt.isel.gape.learning.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.AttemptDAO;
import pt.isel.gape.learning.dao.QuestionDAO;
import pt.isel.gape.learning.dao.QuestionOptionDAO;
import pt.isel.gape.learning.dao.ResponseDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.Attempt;
import pt.isel.gape.learning.model.AttemptState;
import pt.isel.gape.learning.model.CorrectionResult;
import pt.isel.gape.learning.model.ManualCorrectionCommand;
import pt.isel.gape.learning.model.Question;
import pt.isel.gape.learning.model.QuestionConfiguration;
import pt.isel.gape.learning.model.QuestionOption;
import pt.isel.gape.learning.model.QuestionState;
import pt.isel.gape.learning.model.QuestionType;
import pt.isel.gape.learning.model.Response;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class CorrectionService {

    private final ConnectionProvider connectionProvider;
    private final AssessmentDAO assessmentDAO;
    private final AttemptDAO attemptDAO;
    private final QuestionDAO questionDAO;
    private final QuestionOptionDAO optionDAO;
    private final ResponseDAO responseDAO;
    private final AssessmentAccessPolicy accessPolicy;
    private final AuditService auditService;

    public CorrectionService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new AssessmentDAO(connectionProvider),
                new AttemptDAO(connectionProvider),
                new QuestionDAO(connectionProvider),
                new QuestionOptionDAO(connectionProvider),
                new ResponseDAO(connectionProvider),
                new PermissionDAO(connectionProvider),
                clock
        );
    }

    public CorrectionService(
            ConnectionProvider connectionProvider,
            AssessmentDAO assessmentDAO,
            AttemptDAO attemptDAO,
            QuestionDAO questionDAO,
            QuestionOptionDAO optionDAO,
            ResponseDAO responseDAO,
            PermissionDAO permissionDAO,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.assessmentDAO = Objects.requireNonNull(assessmentDAO, "assessmentDAO is required");
        this.attemptDAO = Objects.requireNonNull(attemptDAO, "attemptDAO is required");
        this.questionDAO = Objects.requireNonNull(questionDAO, "questionDAO is required");
        this.optionDAO = Objects.requireNonNull(optionDAO, "optionDAO is required");
        this.responseDAO = Objects.requireNonNull(responseDAO, "responseDAO is required");
        Objects.requireNonNull(permissionDAO, "permissionDAO is required");
        Objects.requireNonNull(clock, "clock is required");
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

    public CorrectionResult autoCorrectAttempt(
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
                    Attempt attempt = attemptDAO.lockById(connection, attemptId)
                            .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));
                    requireSubmittedAttempt(attempt);
                    Assessment assessment = assessmentDAO.lockById(connection, attempt.assessmentId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Assessment not found: " + attempt.assessmentId()));
                    accessPolicy.requireAssessmentManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            assessment,
                            sourceIp
                    );
                    if (!assessment.correctionMode().allowsAutomaticCorrection()) {
                        throw new IllegalStateException("Assessment does not allow automatic correction");
                    }

                    int autoCorrected = 0;
                    int pendingManual = 0;
                    List<Response> responses = responseDAO.findByAttempt(connection, attempt.id());
                    for (Response response : responses) {
                        Question question = questionDAO.findById(connection, response.questionId())
                                .orElseThrow(() -> new IllegalStateException(
                                        "Response question not found: " + response.questionId()));
                        if (question.state() != QuestionState.ACTIVE) {
                            continue;
                        }
                        if (question.type().isAutomaticallyScoredObjective()) {
                            BigDecimal score = objectiveScore(connection, question, response);
                            responseDAO.updateScore(connection, response.id(), score);
                            autoCorrected++;
                        } else {
                            if (assessment.correctionMode().requiresObjectiveOnly()) {
                                throw new IllegalStateException(
                                        "Automatic assessments cannot contain manual responses");
                            }
                            if (response.score() == null) {
                                pendingManual++;
                            }
                        }
                    }

                    BigDecimal total = responseDAO.sumScoresByActiveQuestionsByAttempt(connection, attempt.id());
                    requireWithinAssessmentMaximum(total, assessment);
                    AttemptState state = pendingManual == 0 ? AttemptState.CORRECTED : AttemptState.SUBMITTED;
                    attemptDAO.updateScoreAndState(connection, attempt.id(), total, state);
                    auditService.record(connection, actorUserId, sessionId, "ATTEMPT_AUTO_CORRECT",
                            "attempt", Long.toString(attempt.id()), "success", sourceIp);
                    connection.commit();
                    return new CorrectionResult(attempt.id(), total, state, autoCorrected, pendingManual);
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ATTEMPT_AUTO_CORRECT", Long.toString(attemptId), sourceIp);
            throw wrap(exception, "Failed to auto-correct attempt");
        }
    }

    public CorrectionResult correctResponseManually(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ManualCorrectionCommand command,
            String sourceIp
    ) {
        try {
            validateManualCorrection(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Response response = responseDAO.findById(connection, command.responseId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Response not found: " + command.responseId()));
                    Attempt attempt = attemptDAO.lockById(connection, response.attemptId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Attempt not found: " + response.attemptId()));
                    requireSubmittedAttempt(attempt);
                    Assessment assessment = assessmentDAO.lockById(connection, attempt.assessmentId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Assessment not found: " + attempt.assessmentId()));
                    accessPolicy.requireAssessmentManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            assessment,
                            sourceIp
                    );
                    if (!assessment.correctionMode().allowsManualCorrection()) {
                        throw new IllegalStateException("Assessment does not allow manual correction");
                    }
                    Question question = questionDAO.findById(connection, response.questionId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Question not found: " + response.questionId()));
                    if (question.state() != QuestionState.ACTIVE) {
                        throw new IllegalArgumentException("Only active question responses can be corrected");
                    }
                    if (command.score().compareTo(question.score()) > 0) {
                        throw new IllegalArgumentException("Response score cannot exceed question score");
                    }
                    responseDAO.updateScore(connection, response.id(), command.score());
                    BigDecimal total = responseDAO.sumScoresByActiveQuestionsByAttempt(connection, attempt.id());
                    requireWithinAssessmentMaximum(total, assessment);
                    boolean hasUnscoredResponses = hasUnscoredActiveResponses(connection, attempt.id());
                    AttemptState state = hasUnscoredResponses ? AttemptState.SUBMITTED : AttemptState.CORRECTED;
                    attemptDAO.updateScoreAndState(connection, attempt.id(), total, state);
                    auditService.record(connection, actorUserId, sessionId, "RESPONSE_MANUAL_CORRECT",
                            "response", Long.toString(response.id()), "success", sourceIp);
                    connection.commit();
                    return new CorrectionResult(attempt.id(), total, state, 0, hasUnscoredResponses ? 1 : 0);
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            String affected = command == null ? "unknown" : Long.toString(command.responseId());
            auditFailure(actorUserId, sessionId, "RESPONSE_MANUAL_CORRECT", affected, sourceIp);
            throw wrap(exception, "Failed to correct response manually");
        }
    }

    private BigDecimal objectiveScore(Connection connection, Question question, Response response)
            throws SQLException {
        if (question.type() == QuestionType.RATING) {
            if (response.answer() == null || response.answer().isBlank()) {
                return BigDecimal.ZERO;
            }
            BigDecimal expected = QuestionConfiguration.ratingExpectedValue(question.expectedAnswer());
            BigDecimal submitted = QuestionConfiguration.ratingAnswerValue(response.answer(), question.expectedAnswer());
            return submitted.compareTo(expected) == 0 ? question.score() : BigDecimal.ZERO;
        }
        Set<Long> correctOptions = optionDAO.findActiveByQuestion(connection, question.id())
                .stream()
                .filter(option -> Boolean.TRUE.equals(option.correct()))
                .map(QuestionOption::id)
                .collect(Collectors.toUnmodifiableSet());
        Set<Long> selectedOptions = optionDAO.findSelectedOptions(connection, response.id())
                .stream()
                .map(QuestionOption::id)
                .collect(Collectors.toUnmodifiableSet());
        if (!correctOptions.isEmpty() && selectedOptions.equals(correctOptions)) {
            return question.score();
        }
        return BigDecimal.ZERO;
    }

    private boolean hasUnscoredActiveResponses(Connection connection, long attemptId) throws SQLException {
        for (Response response : responseDAO.findByAttempt(connection, attemptId)) {
            Question question = questionDAO.findById(connection, response.questionId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Response question not found: " + response.questionId()));
            if (question.state() == QuestionState.ACTIVE && response.score() == null) {
                return true;
            }
        }
        return false;
    }

    private static void requireSubmittedAttempt(Attempt attempt) {
        if (attempt.state() != AttemptState.SUBMITTED && attempt.state() != AttemptState.CORRECTED) {
            throw new IllegalStateException("Attempt must be submitted before correction");
        }
    }

    private static void validateManualCorrection(ManualCorrectionCommand command) {
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(command.score(), "manual correction score is required");
        if (command.score().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Manual correction score cannot be negative");
        }
    }

    private static void requireWithinAssessmentMaximum(BigDecimal total, Assessment assessment) {
        if (total.compareTo(assessment.maxGrade()) > 0) {
            throw new IllegalArgumentException("Attempt score cannot exceed assessment maximum grade");
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
                "correction", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
