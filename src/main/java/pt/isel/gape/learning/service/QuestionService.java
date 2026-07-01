package pt.isel.gape.learning.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.validation.AcademicTextValidator;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.QuestionDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.Question;
import pt.isel.gape.learning.model.QuestionConfiguration;
import pt.isel.gape.learning.model.QuestionCreateCommand;
import pt.isel.gape.learning.model.QuestionType;
import pt.isel.gape.learning.model.QuestionUpdateCommand;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class QuestionService {

    private static final int CODE_MAX_LENGTH = 30;
    private static final int STATEMENT_MAX_LENGTH = 2000;
    private static final BigDecimal MINIMUM_SCORE = new BigDecimal("0.10");

    private final ConnectionProvider connectionProvider;
    private final AssessmentDAO assessmentDAO;
    private final QuestionDAO questionDAO;
    private final AssessmentAccessPolicy accessPolicy;
    private final AuditService auditService;

    public QuestionService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new AssessmentDAO(connectionProvider),
                new QuestionDAO(connectionProvider),
                new PermissionDAO(connectionProvider),
                clock
        );
    }

    public QuestionService(
            ConnectionProvider connectionProvider,
            AssessmentDAO assessmentDAO,
            QuestionDAO questionDAO,
            PermissionDAO permissionDAO,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.assessmentDAO = Objects.requireNonNull(assessmentDAO, "assessmentDAO is required");
        this.questionDAO = Objects.requireNonNull(questionDAO, "questionDAO is required");
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

    public Question createQuestion(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            QuestionCreateCommand command,
            String sourceIp
    ) {
        try {
            validateCreateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Assessment assessment = assessmentDAO.lockById(connection, command.assessmentId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Assessment not found: " + command.assessmentId()));
                    accessPolicy.requireAssessmentManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            assessment,
                            sourceIp
                    );
                    requireMutableStructure(connection, assessment.id());
                    requireCorrectionModeSupportsQuestion(assessment, command.type());
                    requireScoreWithinAssessmentMaximum(connection, assessment, null, command.score());
                    long questionId = questionDAO.create(connection, command);
                    auditService.record(connection, actorUserId, sessionId, "QUESTION_CREATE",
                            "question", Long.toString(questionId), "success", sourceIp);
                    Question question = requireQuestion(connection, questionId);
                    connection.commit();
                    return question;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "QUESTION_CREATE", "new", sourceIp);
            throw wrap(exception, "Failed to create question");
        }
    }

    public Question getQuestion(long questionId) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                return requireQuestion(connection, questionId);
            }
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to read question");
        }
    }

    public List<Question> listByAssessment(long assessmentId) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                return questionDAO.findByAssessment(connection, assessmentId);
            }
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to list questions");
        }
    }

    public Question updateQuestion(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long questionId,
            QuestionUpdateCommand command,
            String sourceIp
    ) {
        try {
            validateUpdateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Question current = questionDAO.lockById(connection, questionId)
                            .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
                    Assessment assessment = assessmentDAO.lockById(connection, current.assessmentId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Assessment not found: " + current.assessmentId()));
                    accessPolicy.requireAssessmentManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            assessment,
                            sourceIp
                    );
                    requireMutableStructure(connection, assessment.id());
                    requireCorrectionModeSupportsQuestion(assessment, command.type());
                    requireScoreWithinAssessmentMaximum(connection, assessment, questionId, command.score());
                    questionDAO.update(connection, questionId, command);
                    auditService.record(connection, actorUserId, sessionId, "QUESTION_UPDATE",
                            "question", Long.toString(questionId), "success", sourceIp);
                    Question updated = requireQuestion(connection, questionId);
                    connection.commit();
                    return updated;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "QUESTION_UPDATE", Long.toString(questionId), sourceIp);
            throw wrap(exception, "Failed to update question");
        }
    }

    public void deleteQuestion(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long questionId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Question current = questionDAO.lockById(connection, questionId)
                            .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
                    Assessment assessment = assessmentDAO.lockById(connection, current.assessmentId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Assessment not found: " + current.assessmentId()));
                    accessPolicy.requireAssessmentManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            assessment,
                            sourceIp
                    );
                    requireMutableStructure(connection, assessment.id());
                    questionDAO.delete(connection, questionId);
                    auditService.record(connection, actorUserId, sessionId, "QUESTION_DELETE",
                            "question", Long.toString(questionId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "QUESTION_DELETE", Long.toString(questionId), sourceIp);
            throw wrap(exception, "Failed to delete question");
        }
    }

    public List<Question> reorderQuestions(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long assessmentId,
            List<Long> orderedQuestionIds,
            String sourceIp
    ) {
        try {
            Objects.requireNonNull(orderedQuestionIds, "question order is required");
            if (orderedQuestionIds.isEmpty()) {
                throw new IllegalArgumentException("Question order is required");
            }
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Assessment assessment = assessmentDAO.lockById(connection, assessmentId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Assessment not found: " + assessmentId));
                    accessPolicy.requireAssessmentManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            assessment,
                            sourceIp
                    );
                    requireMutableStructure(connection, assessment.id());
                    List<Question> currentQuestions = questionDAO.lockByAssessment(connection, assessment.id());
                    Map<Long, Question> currentById = new LinkedHashMap<>();
                    for (Question question : currentQuestions) {
                        currentById.put(question.id(), question);
                    }
                    Map<Long, Question> reordered = new LinkedHashMap<>();
                    for (Long questionId : orderedQuestionIds) {
                        if (questionId == null) {
                            throw new IllegalArgumentException("Question order is invalid");
                        }
                        Question question = currentById.get(questionId);
                        if (question == null) {
                            throw new IllegalArgumentException("Question not found: " + questionId);
                        }
                        if (reordered.containsKey(questionId)) {
                            throw new IllegalArgumentException("Question order contains duplicate questions");
                        }
                        reordered.put(questionId, question);
                    }
                    for (Question question : currentQuestions) {
                        reordered.putIfAbsent(question.id(), question);
                    }

                    int orderOffset = currentQuestions.stream()
                            .mapToInt(Question::orderNo)
                            .max()
                            .orElse(0) + currentQuestions.size() + 1000;
                    questionDAO.shiftOrders(connection, assessment.id(), orderOffset);

                    int orderNo = 1;
                    for (Question question : reordered.values()) {
                        questionDAO.update(
                                connection,
                                question.id(),
                                new QuestionUpdateCommand(
                                        question.code(),
                                        question.statement(),
                                        question.type(),
                                        orderNo++,
                                        question.required(),
                                        question.score(),
                                        question.expectedAnswer()
                                )
                        );
                    }
                    auditService.record(connection, actorUserId, sessionId, "QUESTION_REORDER",
                            "assessment", Long.toString(assessment.id()), "success", sourceIp);
                    List<Question> updated = questionDAO.findByAssessment(connection, assessment.id());
                    connection.commit();
                    return updated;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "QUESTION_REORDER", Long.toString(assessmentId), sourceIp);
            throw wrap(exception, "Failed to reorder questions");
        }
    }

    public void rebalanceActiveQuestionScores(
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
                    Assessment assessment = assessmentDAO.lockById(connection, assessmentId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Assessment not found: " + assessmentId));
                    accessPolicy.requireAssessmentManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            assessment,
                            sourceIp
                    );
                    requireMutableStructure(connection, assessment.id());
                    List<Question> questions = questionDAO.lockByAssessment(connection, assessment.id());
                    if (questions.isEmpty()) {
                        connection.commit();
                        return;
                    }
                    BigDecimal minimumTotal = MINIMUM_SCORE.multiply(BigDecimal.valueOf(questions.size()));
                    if (assessment.maxGrade().compareTo(minimumTotal) < 0) {
                        throw new IllegalArgumentException("Maximum Grade is too low for the number of questions");
                    }

                    BigDecimal total = questions.stream()
                            .map(Question::score)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal difference = assessment.maxGrade().subtract(total);
                    if (difference.compareTo(BigDecimal.ZERO) == 0) {
                        connection.commit();
                        return;
                    }

                    List<QuestionScoreAdjustment> adjustments = adjustedScores(questions, difference);
                    for (QuestionScoreAdjustment adjustment : adjustments) {
                        Question question = adjustment.question();
                        questionDAO.update(
                                connection,
                                question.id(),
                                new QuestionUpdateCommand(
                                        question.code(),
                                        question.statement(),
                                        question.type(),
                                        question.orderNo(),
                                        question.required(),
                                        adjustment.score(),
                                        question.expectedAnswer()
                                )
                        );
                    }
                    auditService.record(connection, actorUserId, sessionId, "QUESTION_SCORE_REBALANCE",
                            "assessment", Long.toString(assessment.id()), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "QUESTION_SCORE_REBALANCE", Long.toString(assessmentId), sourceIp);
            throw wrap(exception, "Failed to rebalance question scores");
        }
    }

    private static List<QuestionScoreAdjustment> adjustedScores(List<Question> questions, BigDecimal difference) {
        List<QuestionScoreAdjustment> adjustments = new ArrayList<>();
        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            Question last = questions.get(questions.size() - 1);
            adjustments.add(new QuestionScoreAdjustment(last, last.score().add(difference)));
            return adjustments;
        }

        BigDecimal remainingExcess = difference.abs();
        for (int index = questions.size() - 1; index >= 0 && remainingExcess.compareTo(BigDecimal.ZERO) > 0; index--) {
            Question question = questions.get(index);
            BigDecimal reducibleScore = question.score().subtract(MINIMUM_SCORE).max(BigDecimal.ZERO);
            BigDecimal reduction = reducibleScore.min(remainingExcess);
            if (reduction.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            adjustments.add(new QuestionScoreAdjustment(question, question.score().subtract(reduction)));
            remainingExcess = remainingExcess.subtract(reduction);
        }
        if (remainingExcess.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Maximum Grade is too low for the number of questions");
        }
        return adjustments;
    }

    private void requireMutableStructure(Connection connection, long assessmentId) throws SQLException {
        if (assessmentDAO.hasAnyAttempts(connection, assessmentId)) {
            throw new IllegalStateException("Assessment questions cannot change after attempts have started");
        }
    }

    private void requireScoreWithinAssessmentMaximum(
            Connection connection,
            Assessment assessment,
            Long excludedQuestionId,
            BigDecimal score
    ) throws SQLException {
        BigDecimal total = questionDAO.sumActiveScores(connection, assessment.id(), excludedQuestionId).add(score);
        if (total.compareTo(assessment.maxGrade()) > 0) {
            throw new IllegalArgumentException("Question scores cannot exceed assessment maximum grade");
        }
    }

    private static void requireCorrectionModeSupportsQuestion(
            Assessment assessment,
            QuestionType type
    ) {
        if (assessment.correctionMode().requiresObjectiveOnly()
                && type.requiresManualScoring()) {
            throw new IllegalArgumentException("Automatic assessments can only contain objective questions");
        }
    }

    private Question requireQuestion(Connection connection, long questionId) throws SQLException {
        return questionDAO.findById(connection, questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
    }

    private static void validateCreateCommand(QuestionCreateCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.assessmentId() <= 0) {
            throw new IllegalArgumentException("Question assessment is required");
        }
        validateCommonCommand(
                command.code(),
                command.statement(),
                command.type(),
                command.orderNo(),
                command.score(),
                command.expectedAnswer()
        );
    }

    private static void validateUpdateCommand(QuestionUpdateCommand command) {
        Objects.requireNonNull(command, "command is required");
        validateCommonCommand(
                command.code(),
                command.statement(),
                command.type(),
                command.orderNo(),
                command.score(),
                command.expectedAnswer()
        );
    }

    private static void validateCommonCommand(
            String code,
            String statement,
            Object type,
            int orderNo,
            BigDecimal score,
            String expectedAnswer
    ) {
        AcademicTextValidator.requireAcronym(code, "Question code is required");
        requireMaxLength(code.trim(), CODE_MAX_LENGTH, "Question code is too long");
        AcademicTextValidator.requireName(statement, "Question statement is required");
        requireMaxLength(statement.trim(), STATEMENT_MAX_LENGTH, "Question statement is too long");
        Objects.requireNonNull(type, "question type is required");
        if (orderNo <= 0) {
            throw new IllegalArgumentException("Question order must be greater than zero");
        }
        Objects.requireNonNull(score, "question score is required");
        if (score.compareTo(MINIMUM_SCORE) < 0) {
            throw new IllegalArgumentException("Question score must be at least 0.10");
        }
        if (type == QuestionType.RATING) {
            QuestionConfiguration.ratingExpectedValue(expectedAnswer);
        }
    }

    private static void requireMaxLength(String value, int maxLength, String message) {
        if (value != null && value.trim().length() > maxLength) {
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
                "question", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }

    private record QuestionScoreAdjustment(Question question, BigDecimal score) {
    }
}
