package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.AttemptDAO;
import pt.isel.gape.learning.dao.QuestionDAO;
import pt.isel.gape.learning.dao.QuestionOptionDAO;
import pt.isel.gape.learning.dao.ResponseDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AssessmentMode;
import pt.isel.gape.learning.model.AssessmentState;
import pt.isel.gape.learning.model.Attempt;
import pt.isel.gape.learning.model.AttemptState;
import pt.isel.gape.learning.model.Question;
import pt.isel.gape.learning.model.QuestionConfiguration;
import pt.isel.gape.learning.model.QuestionType;
import pt.isel.gape.learning.model.Response;
import pt.isel.gape.learning.model.ResponseCommand;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class ResponseService {

    private static final int ATTACHMENT_MAX_LENGTH = 255;

    private final ConnectionProvider connectionProvider;
    private final AssessmentDAO assessmentDAO;
    private final AttemptDAO attemptDAO;
    private final QuestionDAO questionDAO;
    private final QuestionOptionDAO optionDAO;
    private final ResponseDAO responseDAO;
    private final PdfUploadService pdfUploadService;
    private final AssessmentAccessPolicy accessPolicy;
    private final AuditService auditService;
    private final Clock clock;

    public ResponseService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new AssessmentDAO(connectionProvider),
                new AttemptDAO(connectionProvider),
                new QuestionDAO(connectionProvider),
                new QuestionOptionDAO(connectionProvider),
                new ResponseDAO(connectionProvider),
                new PermissionDAO(connectionProvider),
                new PdfUploadService(),
                clock
        );
    }

    public ResponseService(
            ConnectionProvider connectionProvider,
            AssessmentDAO assessmentDAO,
            AttemptDAO attemptDAO,
            QuestionDAO questionDAO,
            QuestionOptionDAO optionDAO,
            ResponseDAO responseDAO,
            PermissionDAO permissionDAO,
            Clock clock
    ) {
        this(
                connectionProvider,
                assessmentDAO,
                attemptDAO,
                questionDAO,
                optionDAO,
                responseDAO,
                permissionDAO,
                new PdfUploadService(),
                clock
        );
    }

    public ResponseService(
            ConnectionProvider connectionProvider,
            AssessmentDAO assessmentDAO,
            AttemptDAO attemptDAO,
            QuestionDAO questionDAO,
            QuestionOptionDAO optionDAO,
            ResponseDAO responseDAO,
            PermissionDAO permissionDAO,
            PdfUploadService pdfUploadService,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.assessmentDAO = Objects.requireNonNull(assessmentDAO, "assessmentDAO is required");
        this.attemptDAO = Objects.requireNonNull(attemptDAO, "attemptDAO is required");
        this.questionDAO = Objects.requireNonNull(questionDAO, "questionDAO is required");
        this.optionDAO = Objects.requireNonNull(optionDAO, "optionDAO is required");
        this.responseDAO = Objects.requireNonNull(responseDAO, "responseDAO is required");
        this.pdfUploadService = Objects.requireNonNull(pdfUploadService, "pdfUploadService is required");
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

    public Response saveResponse(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long attemptId,
            ResponseCommand command,
            String sourceIp
    ) {
        try {
            Objects.requireNonNull(command, "command is required");
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
                    assessmentDAO.synchronizeTemporalStates(connection, now);
                    Attempt attempt = attemptDAO.lockById(connection, attemptId)
                            .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));
                    requireAttemptOwner(actorUserId, actorProfileType, attempt);
                    if (attempt.state() != AttemptState.IN_PROGRESS) {
                        throw new IllegalStateException("Responses can only be changed while the attempt is in progress");
                    }
                    Assessment assessment = assessmentDAO.lockById(connection, attempt.assessmentId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Assessment not found: " + attempt.assessmentId()));
                    accessPolicy.requireStudentExecutionAccess(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            assessment,
                            sourceIp
                    );
                    requireInAvailabilityWindow(assessment, now);
                    Question question = questionDAO.findById(connection, command.questionId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Question not found: " + command.questionId()));
                    if (question.assessmentId() != assessment.id()) {
                        throw new IllegalArgumentException("Question does not belong to the attempt assessment");
                    }
                    Set<Long> optionIds = normalizeAndValidatePayload(connection, question, command);
                    Response existing = responseDAO.findByAttemptAndQuestion(connection, attempt.id(), question.id())
                            .orElse(null);
                    validateExistingAttachmentReuse(question, command, existing);
                    long responseId;
                    if (existing == null) {
                        responseId = responseDAO.create(
                                connection,
                                attempt.id(),
                                question.id(),
                                "R" + question.id(),
                                command.answer(),
                                command.attachment(),
                                null,
                                now
                        );
                    } else {
                        responseId = existing.id();
                        responseDAO.update(
                                connection,
                                responseId,
                                command.answer(),
                                command.attachment(),
                                null,
                                now
                        );
                    }
                    responseDAO.deleteSelectedOptions(connection, responseId);
                    responseDAO.insertSelectedOptions(connection, responseId, optionIds);
                    auditService.record(connection, actorUserId, sessionId, "RESPONSE_SAVE",
                            "response", Long.toString(responseId), "success", sourceIp);
                    Response response = requireResponse(connection, responseId);
                    connection.commit();
                    return response;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "RESPONSE_SAVE", Long.toString(attemptId), sourceIp);
            throw wrap(exception, "Failed to save response");
        }
    }

    public Response getResponse(long responseId) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                return requireResponse(connection, responseId);
            }
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to read response");
        }
    }

    private Set<Long> normalizeAndValidatePayload(
            Connection connection,
            Question question,
            ResponseCommand command
    ) throws SQLException {
        List<Long> optionIds = command.optionIds() == null ? List.of() : command.optionIds();
        Set<Long> uniqueOptionIds = new LinkedHashSet<>(optionIds);
        if (uniqueOptionIds.size() != optionIds.size()) {
            throw new IllegalArgumentException("Response selected options cannot be duplicated");
        }
        QuestionType type = question.type();
        if (type.allowsOptions()) {
            if (hasText(command.answer()) || hasText(command.attachment())) {
                throw new IllegalArgumentException("Option questions cannot receive text or file answers");
            }
            if (question.required() && uniqueOptionIds.isEmpty()) {
                throw new IllegalArgumentException("Required option question must select an option");
            }
            if (type.allowsSingleSelectedOption() && uniqueOptionIds.size() > 1) {
                throw new IllegalArgumentException("Single-choice questions allow one selected option");
            }
            if (!optionDAO.allOptionsBelongToQuestion(connection, question.id(), uniqueOptionIds)) {
                throw new IllegalArgumentException("Selected options must belong to the question");
            }
            return uniqueOptionIds;
        }
        if (!uniqueOptionIds.isEmpty()) {
            throw new IllegalArgumentException("This question type does not allow selected options");
        }
        if (type == QuestionType.FILE_UPLOAD) {
            if (!hasText(command.attachment())) {
                throw new IllegalArgumentException("File upload questions require an attachment");
            }
            validateAttachment(command.attachment());
            return Set.of();
        }
        if (type == QuestionType.RATING) {
            if (question.required() && !hasText(command.answer())) {
                throw new IllegalArgumentException("Required rating response is missing");
            }
            if (hasText(command.answer())) {
                if (!QuestionConfiguration.isValidRatingAnswerValue(command.answer(), question.expectedAnswer())) {
                    throw new IllegalArgumentException("Rating response is outside the configured scale");
                }
            }
            return Set.of();
        }
        if (question.required() && !hasText(command.answer())) {
            throw new IllegalArgumentException("Required text response is missing");
        }
        if (hasText(command.attachment())) {
            throw new IllegalArgumentException("Only file upload questions can receive attachments");
        }
        return Set.of();
    }

    private void requireInAvailabilityWindow(Assessment assessment, LocalDateTime now) {
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

    private void validateExistingAttachmentReuse(
            Question question,
            ResponseCommand command,
            Response existing
    ) {
        if (question.type() != QuestionType.FILE_UPLOAD || !hasText(command.attachment())) {
            return;
        }
        String attachment = command.attachment().trim();
        validateAttachment(attachment);
        if (command.uploadedAttachment()) {
            requireStoredAttachmentExists(attachment);
            return;
        }
        if (existing == null || !attachment.equals(existing.attachment())) {
            throw new IllegalArgumentException("File upload questions require a new uploaded attachment");
        }
        requireStoredAttachmentExists(attachment);
    }

    private void requireStoredAttachmentExists(String attachment) {
        Path storedFile = pdfUploadService.resolveStoredContentFile(attachment);
        if (!Files.isRegularFile(storedFile)) {
            throw new IllegalArgumentException("Stored response attachment was not found");
        }
    }

    private static void requireAttemptOwner(long actorUserId, AccessProfileType profileType, Attempt attempt) {
        if (profileType != AccessProfileType.STUDENT || attempt.studentUserId() != actorUserId) {
            throw new SecurityException("Attempt belongs to another student");
        }
    }

    private Response requireResponse(Connection connection, long responseId) throws SQLException {
        return responseDAO.findById(connection, responseId)
                .orElseThrow(() -> new IllegalArgumentException("Response not found: " + responseId));
    }

    private static void validateAttachment(String attachment) {
        String trimmed = attachment.trim();
        if (trimmed.length() > ATTACHMENT_MAX_LENGTH) {
            throw new IllegalArgumentException("Response attachment path is too long");
        }
        if (trimmed.contains("..") || trimmed.startsWith("/") || trimmed.startsWith("\\")) {
            throw new IllegalArgumentException("Response attachment path is not allowed");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void auditFailure(
            long actorUserId,
            Long sessionId,
            String operationType,
            String affectedIdentifier,
            String sourceIp
    ) {
        auditService.record(actorUserId, sessionId, operationType,
                "response", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
