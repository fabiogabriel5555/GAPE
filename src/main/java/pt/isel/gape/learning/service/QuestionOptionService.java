package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Comparator;
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
import pt.isel.gape.learning.dao.QuestionOptionDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.Question;
import pt.isel.gape.learning.model.QuestionOption;
import pt.isel.gape.learning.model.QuestionOptionCreateCommand;
import pt.isel.gape.learning.model.QuestionOptionState;
import pt.isel.gape.learning.model.QuestionOptionUpdateCommand;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class QuestionOptionService {

    private static final int OPTION_TEXT_MAX_LENGTH = 300;

    private final ConnectionProvider connectionProvider;
    private final AssessmentDAO assessmentDAO;
    private final QuestionDAO questionDAO;
    private final QuestionOptionDAO optionDAO;
    private final AssessmentAccessPolicy accessPolicy;
    private final AuditService auditService;

    public QuestionOptionService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new AssessmentDAO(connectionProvider),
                new QuestionDAO(connectionProvider),
                new QuestionOptionDAO(connectionProvider),
                new PermissionDAO(connectionProvider),
                clock
        );
    }

    public QuestionOptionService(
            ConnectionProvider connectionProvider,
            AssessmentDAO assessmentDAO,
            QuestionDAO questionDAO,
            QuestionOptionDAO optionDAO,
            PermissionDAO permissionDAO,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.assessmentDAO = Objects.requireNonNull(assessmentDAO, "assessmentDAO is required");
        this.questionDAO = Objects.requireNonNull(questionDAO, "questionDAO is required");
        this.optionDAO = Objects.requireNonNull(optionDAO, "optionDAO is required");
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

    public QuestionOption createOption(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            QuestionOptionCreateCommand command,
            String sourceIp
    ) {
        try {
            validateCreateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Question question = questionDAO.lockById(connection, command.questionId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Question not found: " + command.questionId()));
                    Assessment assessment = assessmentDAO.lockById(connection, question.assessmentId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Assessment not found: " + question.assessmentId()));
                    accessPolicy.requireAssessmentManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            assessment,
                            sourceIp
                    );
                    requireMutableStructure(connection, assessment.id());
                    requireOptionAllowed(question);
                    demoteCurrentCorrectOptionsForSingleChoice(connection, question, command.correct(), command.state());
                    requireCorrectnessRules(connection, assessment, question, command.correct(), command.state(), null);
                    long optionId = optionDAO.create(connection, command);
                    auditService.record(connection, actorUserId, sessionId, "QUESTION_OPTION_CREATE",
                            "question_option", Long.toString(optionId), "success", sourceIp);
                    QuestionOption option = requireOption(connection, optionId);
                    connection.commit();
                    return option;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "QUESTION_OPTION_CREATE", "new", sourceIp);
            throw wrap(exception, "Failed to create question option");
        }
    }

    public List<QuestionOption> listByQuestion(long questionId) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                return optionDAO.findByQuestion(connection, questionId);
            }
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to list question options");
        }
    }

    public QuestionOption updateOption(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long optionId,
            QuestionOptionUpdateCommand command,
            String sourceIp
    ) {
        try {
            validateUpdateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    QuestionOption current = optionDAO.lockById(connection, optionId)
                            .orElseThrow(() -> new IllegalArgumentException("Question option not found: " + optionId));
                    Question question = questionDAO.lockById(connection, current.questionId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Question not found: " + current.questionId()));
                    Assessment assessment = assessmentDAO.lockById(connection, question.assessmentId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Assessment not found: " + question.assessmentId()));
                    accessPolicy.requireAssessmentManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            assessment,
                            sourceIp
                    );
                    requireMutableStructure(connection, assessment.id());
                    requireOptionAllowed(question);
                    demoteCurrentCorrectOptionsForSingleChoice(connection, question, command.correct(), command.state(), optionId);
                    requireCorrectnessRules(connection, assessment, question, command.correct(), command.state(), optionId);
                    optionDAO.update(connection, optionId, command);
                    auditService.record(connection, actorUserId, sessionId, "QUESTION_OPTION_UPDATE",
                            "question_option", Long.toString(optionId), "success", sourceIp);
                    QuestionOption updated = requireOption(connection, optionId);
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
            auditFailure(actorUserId, sessionId, "QUESTION_OPTION_UPDATE", Long.toString(optionId), sourceIp);
            throw wrap(exception, "Failed to update question option");
        }
    }

    public List<QuestionOption> archiveOption(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long optionId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    QuestionOption current = optionDAO.lockById(connection, optionId)
                            .orElseThrow(() -> new IllegalArgumentException("Question option not found: " + optionId));
                    Question question = questionDAO.lockById(connection, current.questionId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Question not found: " + current.questionId()));
                    Assessment assessment = assessmentDAO.lockById(connection, question.assessmentId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Assessment not found: " + question.assessmentId()));
                    accessPolicy.requireAssessmentManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            assessment,
                            sourceIp
                    );
                    requireMutableStructure(connection, assessment.id());
                    requireOptionAllowed(question);
                    if (current.state() != QuestionOptionState.ACTIVE) {
                        throw new IllegalArgumentException("Question option is not active");
                    }
                    List<QuestionOption> currentOptions = optionDAO.lockByQuestion(connection, question.id());
                    List<QuestionOption> remainingActiveOptions = currentOptions.stream()
                            .filter(option -> option.state() == QuestionOptionState.ACTIVE)
                            .filter(option -> option.id() != optionId)
                            .toList();
                    if (remainingActiveOptions.isEmpty()) {
                        throw new IllegalArgumentException("Questions with options require at least one option");
                    }

                    optionDAO.update(connection, optionId, new QuestionOptionUpdateCommand(
                            current.orderNo(),
                            current.text(),
                            current.correct(),
                            QuestionOptionState.INACTIVE
                    ));

                    boolean needsCorrectOption = assessment.correctionMode().allowsAutomaticCorrection()
                            && remainingActiveOptions.stream()
                                    .noneMatch(option -> Boolean.TRUE.equals(option.correct()));
                    if (needsCorrectOption) {
                        QuestionOption promoted = remainingActiveOptions.get(0);
                        optionDAO.update(connection, promoted.id(), new QuestionOptionUpdateCommand(
                                promoted.orderNo(),
                                promoted.text(),
                                true,
                                promoted.state()
                        ));
                    }

                    auditService.record(connection, actorUserId, sessionId, "QUESTION_OPTION_ARCHIVE",
                            "question_option", Long.toString(optionId), "success", sourceIp);
                    List<QuestionOption> activeOptions = optionDAO.findActiveByQuestion(connection, question.id());
                    connection.commit();
                    return activeOptions;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "QUESTION_OPTION_ARCHIVE", Long.toString(optionId), sourceIp);
            throw wrap(exception, "Failed to archive question option");
        }
    }

    public void updateOptions(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long questionId,
            Map<Long, QuestionOptionUpdateCommand> submittedCommands,
            String sourceIp
    ) {
        try {
            Objects.requireNonNull(submittedCommands, "submittedCommands is required");
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Question question = questionDAO.lockById(connection, questionId)
                            .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
                    Assessment assessment = assessmentDAO.lockById(connection, question.assessmentId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Assessment not found: " + question.assessmentId()));
                    accessPolicy.requireAssessmentManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            assessment,
                            sourceIp
                    );
                    requireMutableStructure(connection, assessment.id());
                    requireOptionAllowed(question);
                    List<QuestionOption> currentOptions = optionDAO.lockByQuestion(connection, questionId);
                    Map<Long, QuestionOption> currentById = new LinkedHashMap<>();
                    for (QuestionOption option : currentOptions) {
                        currentById.put(option.id(), option);
                    }
                    for (Map.Entry<Long, QuestionOptionUpdateCommand> entry : submittedCommands.entrySet()) {
                        if (!currentById.containsKey(entry.getKey())) {
                            throw new IllegalArgumentException("Question option not found: " + entry.getKey());
                        }
                        validateUpdateCommand(entry.getValue());
                    }
                    Map<Long, QuestionOptionUpdateCommand> finalCommands = finalOptionCommands(
                            currentOptions,
                            submittedCommands
                    );
                    finalCommands = normalizeOrders(finalCommands);
                    if (assessment.correctionMode().allowsAutomaticCorrection()) {
                        finalCommands = ensureActiveCorrectOption(finalCommands);
                    }
                    requireCorrectnessRules(assessment, question, finalCommands);
                    int orderOffset = currentOptions.stream()
                            .mapToInt(QuestionOption::orderNo)
                            .max()
                            .orElse(0) + currentOptions.size() + 1000;
                    optionDAO.shiftOrders(connection, questionId, orderOffset);
                    for (Map.Entry<Long, QuestionOptionUpdateCommand> entry : finalCommands.entrySet()) {
                        optionDAO.update(connection, entry.getKey(), entry.getValue());
                    }
                    auditService.record(connection, actorUserId, sessionId, "QUESTION_OPTION_BULK_UPDATE",
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
            auditFailure(actorUserId, sessionId, "QUESTION_OPTION_BULK_UPDATE", Long.toString(questionId), sourceIp);
            throw wrap(exception, "Failed to update question options");
        }
    }

    private void demoteCurrentCorrectOptionsForSingleChoice(
            Connection connection,
            Question question,
            Boolean correct,
            QuestionOptionState state
    ) throws SQLException {
        demoteCurrentCorrectOptionsForSingleChoice(connection, question, correct, state, null);
    }

    private void demoteCurrentCorrectOptionsForSingleChoice(
            Connection connection,
            Question question,
            Boolean correct,
            QuestionOptionState state,
            Long excludedOptionId
    ) throws SQLException {
        if (!question.type().allowsSingleSelectedOption()
                || state != QuestionOptionState.ACTIVE
                || !Boolean.TRUE.equals(correct)) {
            return;
        }
        for (QuestionOption option : optionDAO.lockByQuestion(connection, question.id())) {
            if (option.state() == QuestionOptionState.ACTIVE
                    && Boolean.TRUE.equals(option.correct())
                    && (excludedOptionId == null || option.id() != excludedOptionId)) {
                optionDAO.update(connection, option.id(), new QuestionOptionUpdateCommand(
                        option.orderNo(),
                        option.text(),
                        false,
                        option.state()
                ));
            }
        }
    }

    private static Map<Long, QuestionOptionUpdateCommand> finalOptionCommands(
            List<QuestionOption> currentOptions,
            Map<Long, QuestionOptionUpdateCommand> submittedCommands
    ) {
        Map<Long, QuestionOptionUpdateCommand> finalCommands = new LinkedHashMap<>();
        for (QuestionOption option : currentOptions) {
            QuestionOptionUpdateCommand submitted = submittedCommands.get(option.id());
            finalCommands.put(option.id(), submitted == null
                    ? new QuestionOptionUpdateCommand(
                            option.orderNo(),
                            option.text(),
                            option.correct(),
                            option.state()
                    )
                    : submitted);
        }
        return finalCommands;
    }

    private static Map<Long, QuestionOptionUpdateCommand> normalizeOrders(
            Map<Long, QuestionOptionUpdateCommand> commands
    ) {
        List<Map.Entry<Long, QuestionOptionUpdateCommand>> sorted = commands.entrySet().stream()
                .sorted(Comparator
                        .comparingInt((Map.Entry<Long, QuestionOptionUpdateCommand> entry) -> entry.getValue().orderNo())
                        .thenComparingLong(Map.Entry::getKey))
                .toList();
        Map<Long, QuestionOptionUpdateCommand> normalized = new LinkedHashMap<>();
        int order = 1;
        for (Map.Entry<Long, QuestionOptionUpdateCommand> entry : sorted) {
            QuestionOptionUpdateCommand command = entry.getValue();
            normalized.put(entry.getKey(), new QuestionOptionUpdateCommand(
                    order++,
                    command.text(),
                    command.correct(),
                    command.state()
            ));
        }
        return normalized;
    }

    private static Map<Long, QuestionOptionUpdateCommand> ensureActiveCorrectOption(
            Map<Long, QuestionOptionUpdateCommand> commands
    ) {
        boolean hasActiveCorrectOption = commands.values().stream()
                .anyMatch(command -> command.state() == QuestionOptionState.ACTIVE
                        && Boolean.TRUE.equals(command.correct()));
        if (hasActiveCorrectOption) {
            return commands;
        }
        Map<Long, QuestionOptionUpdateCommand> updated = new LinkedHashMap<>();
        boolean promoted = false;
        for (Map.Entry<Long, QuestionOptionUpdateCommand> entry : commands.entrySet()) {
            QuestionOptionUpdateCommand command = entry.getValue();
            if (!promoted && command.state() == QuestionOptionState.ACTIVE) {
                updated.put(entry.getKey(), new QuestionOptionUpdateCommand(
                        command.orderNo(),
                        command.text(),
                        true,
                        command.state()
                ));
                promoted = true;
            } else {
                updated.put(entry.getKey(), command);
            }
        }
        return updated;
    }

    private static void requireCorrectnessRules(
            Assessment assessment,
            Question question,
            Map<Long, QuestionOptionUpdateCommand> finalCommands
    ) {
        long activeCount = finalCommands.values().stream()
                .filter(command -> command.state() == QuestionOptionState.ACTIVE)
                .count();
        if (activeCount == 0) {
            throw new IllegalArgumentException("Questions with options require at least one option");
        }
        long activeCorrectCount = finalCommands.values().stream()
                .filter(command -> command.state() == QuestionOptionState.ACTIVE)
                .filter(command -> Boolean.TRUE.equals(command.correct()))
                .count();
        if (question.type().allowsSingleSelectedOption() && activeCorrectCount > 1) {
            throw new IllegalArgumentException("Single-choice questions allow at most one correct option");
        }
        if (assessment.correctionMode().allowsAutomaticCorrection() && activeCorrectCount == 0) {
            throw new IllegalArgumentException("Questions with options require at least one correct option");
        }
    }

    private void requireCorrectnessRules(
            Connection connection,
            Assessment assessment,
            Question question,
            Boolean correct,
            QuestionOptionState state,
            Long excludedOptionId
    ) throws SQLException {
        long currentActiveCount = optionDAO.countActiveOptions(connection, question.id(), excludedOptionId);
        boolean newActive = state == QuestionOptionState.ACTIVE;
        long effectiveActiveCount = currentActiveCount + (newActive ? 1 : 0);
        if (effectiveActiveCount == 0) {
            throw new IllegalArgumentException("Questions with options require at least one option");
        }
        long currentCorrectCount = optionDAO.countActiveCorrectOptions(connection, question.id(), excludedOptionId);
        boolean newActiveCorrect = state == QuestionOptionState.ACTIVE && Boolean.TRUE.equals(correct);
        long effectiveCorrectCount = currentCorrectCount + (newActiveCorrect ? 1 : 0);
        if (question.type().allowsSingleSelectedOption() && effectiveCorrectCount > 1) {
            throw new IllegalArgumentException("Single-choice questions allow at most one correct option");
        }
        if (assessment.correctionMode().allowsAutomaticCorrection()
                && state == QuestionOptionState.ACTIVE
                && effectiveCorrectCount == 0) {
            throw new IllegalArgumentException("Questions with options require at least one correct option");
        }
    }

    private void requireMutableStructure(Connection connection, long assessmentId) throws SQLException {
        if (assessmentDAO.hasAnyAttempts(connection, assessmentId)) {
            throw new IllegalStateException("Assessment options cannot change after attempts have started");
        }
    }

    private static void requireOptionAllowed(Question question) {
        if (!question.type().allowsOptions()) {
            throw new IllegalArgumentException("Question type does not allow options");
        }
    }

    private QuestionOption requireOption(Connection connection, long optionId) throws SQLException {
        return optionDAO.findById(connection, optionId)
                .orElseThrow(() -> new IllegalArgumentException("Question option not found: " + optionId));
    }

    private static void validateCreateCommand(QuestionOptionCreateCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.questionId() <= 0) {
            throw new IllegalArgumentException("Question option question is required");
        }
        validateCommonCommand(command.orderNo(), command.text(), command.state());
    }

    private static void validateUpdateCommand(QuestionOptionUpdateCommand command) {
        Objects.requireNonNull(command, "command is required");
        validateCommonCommand(command.orderNo(), command.text(), command.state());
    }

    private static void validateCommonCommand(int orderNo, String text, Object state) {
        if (orderNo <= 0) {
            throw new IllegalArgumentException("Question option order must be greater than zero");
        }
        AcademicTextValidator.requireName(text, "Question option text is required");
        requireMaxLength(text.trim(), OPTION_TEXT_MAX_LENGTH, "Question option text is too long");
        Objects.requireNonNull(state, "question option state is required");
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
                "question_option", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
