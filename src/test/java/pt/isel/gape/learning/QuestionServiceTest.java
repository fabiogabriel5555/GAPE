package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import pt.isel.gape.learning.model.Question;
import pt.isel.gape.learning.model.QuestionConfiguration;
import pt.isel.gape.learning.model.QuestionCreateCommand;
import pt.isel.gape.learning.model.QuestionState;
import pt.isel.gape.learning.model.QuestionType;
import pt.isel.gape.learning.model.QuestionUpdateCommand;
import pt.isel.gape.learning.service.AssessmentService;
import pt.isel.gape.learning.service.QuestionService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class QuestionServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-11T10:15:30Z"), ZoneOffset.UTC);
    private static final String IP = "127.0.0.1";

    private AssessmentService assessmentService;
    private QuestionService questionService;

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
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void teacherCanCreateValidQuestion() {
        Assessment assessment = createAssessment("Form Questions");

        Question question = questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                question(assessment.id(), "Q-A", QuestionType.SINGLE_CHOICE, 1, "5.00"),
                IP
        );

        assertEquals(assessment.id(), question.assessmentId());
        assertEquals(QuestionType.SINGLE_CHOICE, question.type());
        assertEquals(1, question.orderNo());
    }

    @Test
    void questionRequiresExistingAssessment() {
        QuestionCreateCommand command = question(99999L, "Q-X", QuestionType.SINGLE_CHOICE, 1, "5.00");

        assertThrows(IllegalArgumentException.class, () -> questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                command,
                IP
        ));
    }

    @Test
    void teacherCannotCreateQuestionForUnmanagedAssessment() {
        assertThrows(SecurityException.class, () -> questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                question(91L, "Q-UNAUTH", QuestionType.SINGLE_CHOICE, 1, "5.00"),
                IP
        ));
    }

    @Test
    void activeQuestionScoresCannotExceedAssessmentMaximum() {
        Assessment assessment = createAssessment("Form With Limit", AssessmentCorrectionMode.MIXED);
        questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                question(assessment.id(), "Q-1", QuestionType.SINGLE_CHOICE, 1, "18.00"),
                IP
        );

        assertThrows(IllegalArgumentException.class, () -> questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                question(assessment.id(), "Q-2", QuestionType.PARAGRAPH, 2, "3.00"),
                IP
        ));
    }

    @Test
    void automaticAssessmentRejectsManualQuestionType() {
        Assessment assessment = createAssessment("Form Automatic");

        assertThrows(IllegalArgumentException.class, () -> questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                question(assessment.id(), "Q-MAN", QuestionType.PARAGRAPH, 1, "5.00"),
                IP
        ));
    }

    @Test
    void automaticAssessmentAcceptsRatingWithExpectedValue() {
        Assessment assessment = createAssessment("Form Rating Automatic");

        Question question = questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                new QuestionCreateCommand(
                        assessment.id(),
                        "Q-RATE",
                        "Question rating",
                        QuestionType.RATING,
                        1,
                        true,
                        bd("5.00"),
                        QuestionConfiguration.ratingExpectedAnswer("stars_half", "5", "3.5"),
                        QuestionState.ACTIVE
                ),
                IP
        );

        assertEquals(QuestionType.RATING, question.type());
    }

    @Test
    void ratingQuestionRequiresExpectedValue() {
        Assessment assessment = createAssessment("Form Rating Without Expected Answer");

        assertThrows(IllegalArgumentException.class, () -> questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                question(assessment.id(), "Q-RATE", QuestionType.RATING, 1, "5.00"),
                IP
        ));
    }

    @Test
    void questionScoreCannotBeNegative() {
        Assessment assessment = createAssessment("Form Score Invalid");

        assertThrows(IllegalArgumentException.class, () -> questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                question(assessment.id(), "Q-NEG", QuestionType.SINGLE_CHOICE, 1, "-1.00"),
                IP
        ));
    }

    @Test
    void questionScoreMustBeAtLeastMinimum() {
        Assessment assessment = createAssessment("Form Minimum Score");

        assertThrows(IllegalArgumentException.class, () -> questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                question(assessment.id(), "Q-MIN", QuestionType.SINGLE_CHOICE, 1, "0.09"),
                IP
        ));
    }

    @Test
    void teacherCanReorderQuestions() {
        Assessment assessment = createAssessment("Form Reordering");
        Question first = questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                question(assessment.id(), "Q-1", QuestionType.SINGLE_CHOICE, 1, "5.00"),
                IP
        );
        Question second = questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                question(assessment.id(), "Q-2", QuestionType.SINGLE_CHOICE, 2, "5.00"),
                IP
        );

        List<Question> reordered = questionService.reorderQuestions(
                3L,
                null,
                AccessProfileType.TEACHER,
                assessment.id(),
                List.of(second.id(), first.id()),
                IP
        );

        assertEquals(second.id(), reordered.get(0).id());
        assertEquals(1, reordered.get(0).orderNo());
        assertEquals(first.id(), reordered.get(1).id());
        assertEquals(2, reordered.get(1).orderNo());
    }

    @Test
    void rebalanceActiveQuestionScoresDistributesAssessmentMaximum() {
        Assessment assessment = createAssessment("Form Rebalance", AssessmentCorrectionMode.MIXED);
        questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                question(assessment.id(), "Q-RB-1", QuestionType.SINGLE_CHOICE, 1, "5.00"),
                IP
        );
        questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                question(assessment.id(), "Q-RB-2", QuestionType.PARAGRAPH, 2, "5.00"),
                IP
        );

        questionService.rebalanceActiveQuestionScores(
                3L,
                null,
                AccessProfileType.TEACHER,
                assessment.id(),
                IP
        );

        BigDecimal total = questionService.listByAssessment(assessment.id()).stream()
                .filter(question -> question.state() == QuestionState.ACTIVE)
                .map(Question::score)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, total.compareTo(bd("20.00")));
    }

    @Test
    void submittedAttemptsBlockQuestionChanges() {
        QuestionUpdateCommand command = new QuestionUpdateCommand(
                "Q1",
                "Question updated",
                QuestionType.SINGLE_CHOICE,
                1,
                true,
                bd("10.00"),
                null,
                QuestionState.ACTIVE
        );

        assertThrows(IllegalStateException.class, () -> questionService.updateQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                100L,
                command,
                IP
        ));
    }

    private Assessment createAssessment(String title) {
        return createAssessment(title, AssessmentCorrectionMode.AUTOMATIC);
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

    private static QuestionCreateCommand question(
            long assessmentId,
            String code,
            QuestionType type,
            int order,
            String score
    ) {
        return new QuestionCreateCommand(
                assessmentId,
                code,
                "Question test " + code,
                type,
                order,
                true,
                bd(score),
                null,
                QuestionState.ACTIVE
        );
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
