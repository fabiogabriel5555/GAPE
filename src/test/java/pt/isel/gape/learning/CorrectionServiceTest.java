package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AssessmentCorrectionMode;
import pt.isel.gape.learning.model.AssessmentCreateCommand;
import pt.isel.gape.learning.model.AssessmentMode;
import pt.isel.gape.learning.model.AssessmentState;
import pt.isel.gape.learning.model.AssessmentType;
import pt.isel.gape.learning.model.Attempt;
import pt.isel.gape.learning.model.AttemptState;
import pt.isel.gape.learning.model.CorrectionResult;
import pt.isel.gape.learning.model.ManualCorrectionCommand;
import pt.isel.gape.learning.model.Question;
import pt.isel.gape.learning.model.QuestionConfiguration;
import pt.isel.gape.learning.model.QuestionCreateCommand;
import pt.isel.gape.learning.model.QuestionOptionCreateCommand;
import pt.isel.gape.learning.model.QuestionType;
import pt.isel.gape.learning.model.Response;
import pt.isel.gape.learning.model.ResponseCommand;
import pt.isel.gape.learning.service.AssessmentService;
import pt.isel.gape.learning.service.AttemptService;
import pt.isel.gape.learning.service.CorrectionService;
import pt.isel.gape.learning.service.QuestionOptionService;
import pt.isel.gape.learning.service.QuestionService;
import pt.isel.gape.learning.service.ResponseService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class CorrectionServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-11T10:15:30Z"), ZoneOffset.UTC);
    private static final String IP = "127.0.0.1";

    private AssessmentService assessmentService;
    private QuestionService questionService;
    private QuestionOptionService optionService;
    private AttemptService attemptService;
    private ResponseService responseService;
    private CorrectionService correctionService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        assessmentService = new AssessmentService(connectionProvider, FIXED_CLOCK);
        questionService = new QuestionService(connectionProvider, FIXED_CLOCK);
        optionService = new QuestionOptionService(connectionProvider, FIXED_CLOCK);
        attemptService = new AttemptService(connectionProvider, FIXED_CLOCK);
        responseService = new ResponseService(connectionProvider, FIXED_CLOCK);
        correctionService = new CorrectionService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void automaticCorrectionScoresObjectiveResponses() {
        CorrectionResult result = correctionService.autoCorrectAttempt(
                3L,
                null,
                AccessProfileType.TEACHER,
                120L,
                IP
        );

        assertEquals(AttemptState.CORRECTED, result.state());
        assertEquals(0, result.score().compareTo(bd("10.00")));
        assertEquals(1, result.automaticallyCorrectedResponses());
    }

    @Test
    void mixedCorrectionLeavesManualResponsesPending() {
        Assessment assessment = createAssessment("Form Mixed Com Manual", AssessmentCorrectionMode.MIXED);
        Question objective = createQuestion(assessment.id(), "Q-OBJ", QuestionType.SINGLE_CHOICE, 1, "5.00");
        Question text = createQuestion(assessment.id(), "Q-TXT", QuestionType.PARAGRAPH, 2, "5.00");
        long correctOptionId = optionService.createOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                new QuestionOptionCreateCommand(objective.id(), 1, "Certa", true),
                IP
        ).id();
        optionService.createOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                new QuestionOptionCreateCommand(objective.id(), 2, "Errada", false),
                IP
        );
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);
        responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(objective.id(), null, null, List.of(correctOptionId)),
                IP
        );
        Response textResponse = responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(text.id(), "Text para corrigir", null, List.of()),
                IP
        );
        attemptService.submitAttempt(4L, null, AccessProfileType.STUDENT, attempt.id(), IP);

        CorrectionResult result = correctionService.autoCorrectAttempt(
                3L,
                null,
                AccessProfileType.TEACHER,
                attempt.id(),
                IP
        );

        assertEquals(AttemptState.SUBMITTED, result.state());
        assertNull(result.score());
        assertEquals(1, result.pendingManualResponses());
        assertNull(responseService.getResponse(textResponse.id()).score());
    }

    @Test
    void automaticCorrectionScoresRatingExpectedValue() {
        Assessment assessment = createAssessment("Form Rating", AssessmentCorrectionMode.AUTOMATIC);
        Question rating = createQuestion(
                assessment.id(),
                "Q-RATE",
                QuestionType.RATING,
                1,
                "5.00",
                QuestionConfiguration.ratingExpectedAnswer("circles_half", "5", "3.5")
        );
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);
        responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(rating.id(), "3.5", null, List.of()),
                IP
        );
        attemptService.submitAttempt(4L, null, AccessProfileType.STUDENT, attempt.id(), IP);

        CorrectionResult result = correctionService.autoCorrectAttempt(
                3L,
                null,
                AccessProfileType.TEACHER,
                attempt.id(),
                IP
        );

        assertEquals(AttemptState.CORRECTED, result.state());
        assertEquals(0, result.score().compareTo(bd("5.00")));
        assertEquals(1, result.automaticallyCorrectedResponses());
        assertEquals(0, result.pendingManualResponses());
    }

    @Test
    void autoCorrectResponseScoresEligibleQuestion() {
        Assessment assessment = createAssessment("Form Response Auto", AssessmentCorrectionMode.MANUAL);
        Question objective = createQuestion(assessment.id(), "Q-RESP-AUTO", QuestionType.SINGLE_CHOICE, 1, "5.00");
        long correctOptionId = optionService.createOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                new QuestionOptionCreateCommand(objective.id(), 1, "Certa", true),
                IP
        ).id();
        optionService.createOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                new QuestionOptionCreateCommand(objective.id(), 2, "Errada", false),
                IP
        );
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);
        Response response = responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(objective.id(), null, null, List.of(correctOptionId)),
                IP
        );
        attemptService.submitAttempt(4L, null, AccessProfileType.STUDENT, attempt.id(), IP);

        CorrectionResult result = correctionService.autoCorrectResponse(
                3L,
                null,
                AccessProfileType.TEACHER,
                response.id(),
                IP
        );

        assertEquals(AttemptState.CORRECTED, result.state());
        assertEquals(0, result.score().compareTo(bd("5.00")));
        assertEquals(1, result.automaticallyCorrectedResponses());
        assertEquals(0, result.pendingManualResponses());
        assertEquals(0, responseService.getResponse(response.id()).score().compareTo(bd("5.00")));
    }

    @Test
    void autoCorrectEligibleResponsesLeavesManualQuestionsPending() {
        Assessment assessment = createAssessment("Form Eligible Auto", AssessmentCorrectionMode.MANUAL);
        Question objective = createQuestion(assessment.id(), "Q-ELIGIBLE-AUTO", QuestionType.SINGLE_CHOICE, 1, "5.00");
        Question text = createQuestion(assessment.id(), "Q-ELIGIBLE-MANUAL", QuestionType.PARAGRAPH, 2, "5.00");
        long correctOptionId = optionService.createOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                new QuestionOptionCreateCommand(objective.id(), 1, "Correct option", true),
                IP
        ).id();
        optionService.createOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                new QuestionOptionCreateCommand(objective.id(), 2, "Wrong option", false),
                IP
        );
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);
        Response objectiveResponse = responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(objective.id(), null, null, List.of(correctOptionId)),
                IP
        );
        Response textResponse = responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(text.id(), "Manual answer", null, List.of()),
                IP
        );
        attemptService.submitAttempt(4L, null, AccessProfileType.STUDENT, attempt.id(), IP);

        CorrectionResult result = correctionService.autoCorrectEligibleResponses(
                3L,
                null,
                AccessProfileType.TEACHER,
                attempt.id(),
                IP
        );

        assertEquals(AttemptState.SUBMITTED, result.state());
        assertNull(result.score());
        assertEquals(1, result.automaticallyCorrectedResponses());
        assertEquals(1, result.pendingManualResponses());
        Attempt updatedAttempt = attemptService.getAttempt(attempt.id());
        assertEquals(AttemptState.SUBMITTED, updatedAttempt.state());
        assertNull(updatedAttempt.score());
        assertEquals(0, responseService.getResponse(objectiveResponse.id()).score().compareTo(bd("5.00")));
        assertNull(responseService.getResponse(textResponse.id()).score());
    }

    @Test
    void autoCorrectEligibleResponsesSkipsObjectiveWithoutExpectedAnswer() {
        Assessment assessment = createAssessment("Form Objective Without Expected", AssessmentCorrectionMode.MANUAL);
        Question objective = createQuestion(assessment.id(), "Q-NO-EXPECTED", QuestionType.SINGLE_CHOICE, 1, "5.00");
        long selectedOptionId = optionService.createOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                new QuestionOptionCreateCommand(objective.id(), 1, "Neutral option", null),
                IP
        ).id();
        optionService.createOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                new QuestionOptionCreateCommand(objective.id(), 2, "Other neutral option", null),
                IP
        );
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);
        Response response = responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(objective.id(), null, null, List.of(selectedOptionId)),
                IP
        );
        attemptService.submitAttempt(4L, null, AccessProfileType.STUDENT, attempt.id(), IP);

        CorrectionResult result = correctionService.autoCorrectEligibleResponses(
                3L,
                null,
                AccessProfileType.TEACHER,
                attempt.id(),
                IP
        );

        assertEquals(AttemptState.SUBMITTED, result.state());
        assertNull(result.score());
        assertEquals(0, result.automaticallyCorrectedResponses());
        assertEquals(1, result.pendingManualResponses());
        assertNull(responseService.getResponse(response.id()).score());
    }

    @Test
    void manualCorrectionAppliesScoreAndCorrectsAttempt() {
        Assessment assessment = createAssessment("Form Manual", AssessmentCorrectionMode.MANUAL);
        Question text = createQuestion(assessment.id(), "Q-MAN", QuestionType.PARAGRAPH, 1, "10.00");
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);
        Response response = responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(text.id(), "Resposta manual", null, List.of()),
                IP
        );
        attemptService.submitAttempt(4L, null, AccessProfileType.STUDENT, attempt.id(), IP);

        CorrectionResult result = correctionService.correctResponseManually(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ManualCorrectionCommand(response.id(), bd("7.50")),
                IP
        );

        assertEquals(AttemptState.CORRECTED, result.state());
        assertEquals(0, result.score().compareTo(bd("7.50")));
    }

    @Test
    void manualBatchCorrectionAppliesScoresAndCorrectsAttempt() {
        Assessment assessment = createAssessment("Form Manual Batch", AssessmentCorrectionMode.MANUAL);
        Question first = createQuestion(assessment.id(), "Q-BATCH-1", QuestionType.PARAGRAPH, 1, "10.00");
        Question second = createQuestion(assessment.id(), "Q-BATCH-2", QuestionType.SHORT_TEXT, 2, "5.00");
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);
        Response firstResponse = responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(first.id(), "Resposta longa", null, List.of()),
                IP
        );
        Response secondResponse = responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(second.id(), "Resposta curta", null, List.of()),
                IP
        );
        attemptService.submitAttempt(4L, null, AccessProfileType.STUDENT, attempt.id(), IP);

        CorrectionResult result = correctionService.correctAttemptManually(
                3L,
                null,
                AccessProfileType.TEACHER,
                attempt.id(),
                Map.of(firstResponse.id(), bd("8.00"), secondResponse.id(), bd("4.50")),
                IP
        );

        assertEquals(AttemptState.CORRECTED, result.state());
        assertEquals(0, result.score().compareTo(bd("12.50")));
        assertEquals(0, result.pendingManualResponses());
    }

    @Test
    void maximumScoreIsReachableWhenEveryResponseReceivesItsQuestionMaximum() {
        Assessment assessment = createAssessment("Form Full Score", AssessmentCorrectionMode.MANUAL);
        Question first = createQuestion(assessment.id(), "Q-FULL-1", QuestionType.PARAGRAPH, 1, "10.00");
        Question second = createQuestion(assessment.id(), "Q-FULL-2", QuestionType.SHORT_TEXT, 2, "10.00");
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);
        Response firstResponse = responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(first.id(), "Resposta longa", null, List.of()),
                IP
        );
        Response secondResponse = responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(second.id(), "Resposta curta", null, List.of()),
                IP
        );
        attemptService.submitAttempt(4L, null, AccessProfileType.STUDENT, attempt.id(), IP);

        CorrectionResult result = correctionService.correctAttemptManually(
                3L,
                null,
                AccessProfileType.TEACHER,
                attempt.id(),
                Map.of(firstResponse.id(), bd("10.00"), secondResponse.id(), bd("10.00")),
                IP
        );

        assertEquals(AttemptState.CORRECTED, result.state());
        assertEquals(0, result.score().compareTo(assessment.maxGrade()));
    }

    @Test
    void manualCorrectionCannotExceedQuestionScore() {
        Assessment assessment = createAssessment("Form Invalid Manual", AssessmentCorrectionMode.MANUAL);
        Question text = createQuestion(assessment.id(), "Q-MAX", QuestionType.PARAGRAPH, 1, "5.00");
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);
        Response response = responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(text.id(), "Resposta manual", null, List.of()),
                IP
        );
        attemptService.submitAttempt(4L, null, AccessProfileType.STUDENT, attempt.id(), IP);

        assertThrows(IllegalArgumentException.class, () -> correctionService.correctResponseManually(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ManualCorrectionCommand(response.id(), bd("6.00")),
                IP
        ));
    }

    @Test
    void automaticCorrectionRejectsManualAssessmentMode() {
        Assessment assessment = createAssessment("Form Manual Auto Reject", AssessmentCorrectionMode.MANUAL);
        Question text = createQuestion(assessment.id(), "Q-MAN-AUTO", QuestionType.PARAGRAPH, 1, "5.00");
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);
        responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(text.id(), "Resposta manual", null, List.of()),
                IP
        );
        attemptService.submitAttempt(4L, null, AccessProfileType.STUDENT, attempt.id(), IP);

        assertThrows(IllegalStateException.class, () -> correctionService.autoCorrectAttempt(
                3L,
                null,
                AccessProfileType.TEACHER,
                attempt.id(),
                IP
        ));
    }

    @Test
    void manualCorrectionCanOverrideAutomaticAssessmentMode() {
        Assessment assessment = createAssessment("Form Automatic Manual Override", AssessmentCorrectionMode.AUTOMATIC);
        Question objective = createQuestion(assessment.id(), "Q-AUTO-MAN", QuestionType.SINGLE_CHOICE, 1, "5.00");
        long correctOptionId = optionService.createOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                new QuestionOptionCreateCommand(objective.id(), 1, "Certa", true),
                IP
        ).id();
        optionService.createOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                new QuestionOptionCreateCommand(objective.id(), 2, "Errada", false),
                IP
        );
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);
        Response response = responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(objective.id(), null, null, List.of(correctOptionId)),
                IP
        );
        attemptService.submitAttempt(4L, null, AccessProfileType.STUDENT, attempt.id(), IP);

        CorrectionResult result = correctionService.correctResponseManually(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ManualCorrectionCommand(response.id(), bd("5.00")),
                IP
        );

        assertEquals(AttemptState.CORRECTED, result.state());
        assertEquals(0, result.score().compareTo(bd("5.00")));
    }

    @Test
    void correctionBeforeSubmissionIsRejected() {
        Assessment assessment = createAssessment("Form Not Submitted", AssessmentCorrectionMode.AUTOMATIC);
        Question objective = createQuestion(assessment.id(), "Q-NOT-SUB", QuestionType.SINGLE_CHOICE, 1, "5.00");
        optionService.createOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                new QuestionOptionCreateCommand(objective.id(), 1, "Certa", true),
                IP
        );
        optionService.createOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                new QuestionOptionCreateCommand(objective.id(), 2, "Errada", false),
                IP
        );
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);

        assertThrows(IllegalStateException.class, () -> correctionService.autoCorrectAttempt(
                3L,
                null,
                AccessProfileType.TEACHER,
                attempt.id(),
                IP
        ));
    }

    @Test
    void manualCorrectionRejectsNegativeScore() {
        Assessment assessment = createAssessment("Form Negative Manual", AssessmentCorrectionMode.MANUAL);
        Question text = createQuestion(assessment.id(), "Q-NEG-MAN", QuestionType.PARAGRAPH, 1, "5.00");
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);
        Response response = responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(text.id(), "Resposta manual", null, List.of()),
                IP
        );
        attemptService.submitAttempt(4L, null, AccessProfileType.STUDENT, attempt.id(), IP);

        assertThrows(IllegalArgumentException.class, () -> correctionService.correctResponseManually(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ManualCorrectionCommand(response.id(), bd("-1.00")),
                IP
        ));
    }

    @Test
    void studentCannotCorrectAttempt() {
        assertThrows(SecurityException.class, () -> correctionService.autoCorrectAttempt(
                4L,
                null,
                AccessProfileType.STUDENT,
                120L,
                IP
        ));
    }

    private Assessment createAssessment(String title, AssessmentCorrectionMode correctionMode) {
        return assessmentService.createAssessment(
                3L,
                null,
                AccessProfileType.TEACHER,
                new AssessmentCreateCommand(
                        null,
                        60L,
                        title,
                        null,
                        AssessmentType.FORM,
                        AssessmentMode.ONLINE,
                        correctionMode,
                        bd("20.00"),
                        bd("10.00"),
                        2,
                        AssessmentState.SCHEDULED,
                        LocalDateTime.of(2026, 2, 11, 10, 15),
                        LocalDateTime.of(2026, 2, 20, 23, 59)
                ),
                IP
        );
    }

    private Question createQuestion(
            long assessmentId,
            String code,
            QuestionType type,
            int order,
            String score
    ) {
        return createQuestion(assessmentId, code, type, order, score, null);
    }

    private Question createQuestion(
            long assessmentId,
            String code,
            QuestionType type,
            int order,
            String score,
            String expectedAnswer
    ) {
        return questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                new QuestionCreateCommand(
                        assessmentId,
                        code,
                        "Question " + code,
                        type,
                        order,
                        true,
                        bd(score),
                        expectedAnswer
                ),
                IP
        );
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
