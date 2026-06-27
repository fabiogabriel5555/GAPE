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
import pt.isel.gape.learning.model.Question;
import pt.isel.gape.learning.model.QuestionConfiguration;
import pt.isel.gape.learning.model.QuestionCreateCommand;
import pt.isel.gape.learning.model.QuestionState;
import pt.isel.gape.learning.model.QuestionType;
import pt.isel.gape.learning.model.Response;
import pt.isel.gape.learning.model.ResponseCommand;
import pt.isel.gape.learning.service.AssessmentService;
import pt.isel.gape.learning.service.AttemptService;
import pt.isel.gape.learning.service.QuestionService;
import pt.isel.gape.learning.service.ResponseService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class ResponseServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-11T10:15:30Z"), ZoneOffset.UTC);
    private static final String IP = "127.0.0.1";

    private AssessmentService assessmentService;
    private QuestionService questionService;
    private AttemptService attemptService;
    private ResponseService responseService;
    private ConnectionProvider connectionProvider;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        connectionProvider = DatabaseTestSupport::openConnection;
        assessmentService = new AssessmentService(connectionProvider, FIXED_CLOCK);
        questionService = new QuestionService(connectionProvider, FIXED_CLOCK);
        attemptService = new AttemptService(connectionProvider, FIXED_CLOCK);
        responseService = new ResponseService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void studentCanSaveSingleChoiceResponse() {
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, 90L, IP);

        Response response = responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(100L, null, null, List.of(110L)),
                IP
        );

        assertEquals(attempt.id(), response.attemptId());
        assertEquals(100L, response.questionId());
        assertNull(response.score());
    }

    @Test
    void optionQuestionRejectsTextAnswer() {
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, 90L, IP);

        assertThrows(IllegalArgumentException.class, () -> responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(100L, "texto indevido", null, List.of()),
                IP
        ));
    }

    @Test
    void responseForInactiveQuestionIsRejected() {
        Assessment assessment = createAssessment("Form Inactive Question");
        createQuestion(assessment.id(), "Q-ACTIVE", QuestionType.PARAGRAPH, 1, null, QuestionState.ACTIVE);
        Question inactive = createQuestion(
                assessment.id(),
                "Q-INACTIVE",
                QuestionType.PARAGRAPH,
                2,
                null,
                QuestionState.INACTIVE
        );
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);

        assertThrows(IllegalArgumentException.class, () -> responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(inactive.id(), "Resposta indevida", null, List.of()),
                IP
        ));
    }

    @Test
    void responseQuestionMustBelongToAttemptAssessment() {
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, 90L, IP);

        assertThrows(IllegalArgumentException.class, () -> responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(101L, null, null, List.of(112L)),
                IP
        ));
    }

    @Test
    void singleChoiceRejectsMultipleSelectedOptions() {
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, 90L, IP);

        assertThrows(IllegalArgumentException.class, () -> responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(100L, null, null, List.of(110L, 111L)),
                IP
        ));
    }

    @Test
    void studentCanSaveParagraphAndFileResponses() {
        Assessment assessment = createAssessment("Form Text File");
        Question textQuestion = createQuestion(assessment.id(), "Q-TEXT", QuestionType.PARAGRAPH, 1);
        Question fileQuestion = createQuestion(assessment.id(), "Q-FILE", QuestionType.FILE_UPLOAD, 2);
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);

        Response textResponse = responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(textQuestion.id(), "Resposta desenvolvida", null, List.of()),
                IP
        );
        Response fileResponse = responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(fileQuestion.id(), null, "contents/legacy.pdf", List.of(), true),
                IP
        );

        assertEquals("Resposta desenvolvida", textResponse.answer());
        assertEquals("contents/legacy.pdf", fileResponse.attachment());
    }

    @Test
    void fileResponseRejectsForgedHiddenAttachmentWithoutExistingResponse() {
        Assessment assessment = createAssessment("Form Forged File");
        Question fileQuestion = createQuestion(assessment.id(), "Q-FILE", QuestionType.FILE_UPLOAD, 1);
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);

        assertThrows(IllegalArgumentException.class, () -> responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(fileQuestion.id(), null, "contents/legacy.pdf", List.of(), false),
                IP
        ));
    }

    @Test
    void fileResponseRejectsUnsafeAttachmentPath() {
        Assessment assessment = createAssessment("Form Invalid File");
        Question fileQuestion = createQuestion(assessment.id(), "Q-FILE", QuestionType.FILE_UPLOAD, 1);
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);

        assertThrows(IllegalArgumentException.class, () -> responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(fileQuestion.id(), null, "../secret.pdf", List.of()),
                IP
        ));
    }

    @Test
    void studentCanSaveRatingResponseValueWithoutOptions() {
        Assessment assessment = createAssessment("Form Rating");
        Question ratingQuestion = createQuestion(
                assessment.id(),
                "Q-RATE",
                QuestionType.RATING,
                1,
                QuestionConfiguration.ratingExpectedAnswer("stars_half", "5", "3.5")
        );
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);

        Response response = responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(ratingQuestion.id(), "3.5", null, List.of()),
                IP
        );

        assertEquals("3.5", response.answer());
        assertNull(response.score());
    }

    @Test
    void ratingResponseRejectsOptionsAndInvalidStep() {
        Assessment assessment = createAssessment("Form Rating Invalid");
        Question ratingQuestion = createQuestion(
                assessment.id(),
                "Q-RATE-INV",
                QuestionType.RATING,
                1,
                QuestionConfiguration.ratingExpectedAnswer("hearts_half", "5", "4")
        );
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);

        assertThrows(IllegalArgumentException.class, () -> responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(ratingQuestion.id(), "3.5", null, List.of(110L)),
                IP
        ));
        assertThrows(IllegalArgumentException.class, () -> responseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(ratingQuestion.id(), "3.25", null, List.of()),
                IP
        ));
    }

    @Test
    void responseAfterAvailabilityEndIsRejected() {
        Assessment assessment = createAssessment(
                "Form Expiring",
                LocalDateTime.of(2026, 2, 11, 10, 15),
                LocalDateTime.of(2026, 2, 11, 10, 16)
        );
        Question textQuestion = createQuestion(assessment.id(), "Q-EXP", QuestionType.PARAGRAPH, 1);
        Attempt attempt = attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, assessment.id(), IP);
        ResponseService expiredResponseService = new ResponseService(
                connectionProvider,
                Clock.fixed(Instant.parse("2026-02-11T10:16:00Z"), ZoneOffset.UTC)
        );

        assertThrows(IllegalStateException.class, () -> expiredResponseService.saveResponse(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                new ResponseCommand(textQuestion.id(), "Tarde demais", null, List.of()),
                IP
        ));
    }

    private Assessment createAssessment(String title) {
        return createAssessment(
                title,
                LocalDateTime.of(2026, 2, 11, 10, 15),
                LocalDateTime.of(2026, 2, 20, 23, 59)
        );
    }

    private Assessment createAssessment(String title, LocalDateTime availableFrom, LocalDateTime availableUntil) {
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
                        AssessmentCorrectionMode.MIXED,
                        bd("20.00"),
                        bd("10.00"),
                        2,
                        AssessmentState.SCHEDULED,
                        availableFrom,
                        availableUntil
                ),
                IP
        );
    }

    private Question createQuestion(long assessmentId, String code, QuestionType type, int order) {
        return createQuestion(assessmentId, code, type, order, null);
    }

    private Question createQuestion(long assessmentId, String code, QuestionType type, int order, String expectedAnswer) {
        return createQuestion(assessmentId, code, type, order, expectedAnswer, QuestionState.ACTIVE);
    }

    private Question createQuestion(
            long assessmentId,
            String code,
            QuestionType type,
            int order,
            String expectedAnswer,
            QuestionState state
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
                        bd("5.00"),
                        expectedAnswer,
                        state
                ),
                IP
        );
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
