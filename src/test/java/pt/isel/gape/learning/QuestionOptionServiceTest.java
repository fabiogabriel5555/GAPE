package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import pt.isel.gape.learning.model.QuestionOption;
import pt.isel.gape.learning.model.QuestionOptionCreateCommand;
import pt.isel.gape.learning.model.QuestionOptionUpdateCommand;
import pt.isel.gape.learning.model.QuestionType;
import pt.isel.gape.learning.service.AssessmentService;
import pt.isel.gape.learning.service.QuestionOptionService;
import pt.isel.gape.learning.service.QuestionService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class QuestionOptionServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-11T10:15:30Z"), ZoneOffset.UTC);
    private static final String IP = "127.0.0.1";

    private AssessmentService assessmentService;
    private QuestionService questionService;
    private QuestionOptionService optionService;

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
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void teacherCanCreateValidQuestionOption() {
        Question question = createQuestion("Form Options", QuestionType.SINGLE_CHOICE);

        QuestionOption option = optionService.createOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                option(question.id(), 1, "Option right", true),
                IP
        );

        assertEquals(question.id(), option.questionId());
        assertEquals(Boolean.TRUE, option.correct());
    }

    @Test
    void optionRequiresExistingQuestion() {
        assertThrows(IllegalArgumentException.class, () -> optionService.createOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                option(99999L, 1, "Without question", true),
                IP
        ));
    }

    @Test
    void paragraphQuestionsCannotHaveOptions() {
        Question question = createQuestion("Form Text", QuestionType.PARAGRAPH);

        assertThrows(IllegalArgumentException.class, () -> optionService.createOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                option(question.id(), 1, "Nao permitido", true),
                IP
        ));
    }

    @Test
    void ratingQuestionsCannotHaveOptions() {
        Question question = createQuestion("Form Rating", QuestionType.RATING);

        assertThrows(IllegalArgumentException.class, () -> optionService.createOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                option(question.id(), 1, "Cinco estrelas", true),
                IP
        ));
    }

    @Test
    void singleChoiceCreatingCorrectOptionDemotesPreviousCorrectOption() {
        Question question = createQuestion("Form Single Choice", QuestionType.SINGLE_CHOICE);
        QuestionOption first = optionService.createOption(3L, null, AccessProfileType.TEACHER,
                option(question.id(), 1, "Certa", true), IP);
        QuestionOption second = optionService.createOption(3L, null, AccessProfileType.TEACHER,
                option(question.id(), 2, "Tambem right", true), IP);

        List<QuestionOption> options = optionService.listByQuestion(question.id());
        QuestionOption updatedFirst = options.stream()
                .filter(option -> option.id() == first.id())
                .findFirst()
                .orElseThrow();
        QuestionOption updatedSecond = options.stream()
                .filter(option -> option.id() == second.id())
                .findFirst()
                .orElseThrow();
        assertFalse(Boolean.TRUE.equals(updatedFirst.correct()));
        assertTrue(Boolean.TRUE.equals(updatedSecond.correct()));
    }

    @Test
    void automaticMultipleChoiceRequiresCorrectOption() {
        Question question = createQuestion("Form Multiple", QuestionType.MULTIPLE_CHOICE);

        assertThrows(IllegalArgumentException.class, () -> optionService.createOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                option(question.id(), 1, "Distrator", false),
                IP
        ));
    }

    @Test
    void submittedAttemptsBlockOptionChanges() {
        QuestionOptionUpdateCommand command = new QuestionOptionUpdateCommand(
                1,
                "Option updated",
                true
        );

        assertThrows(IllegalStateException.class, () -> optionService.updateOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                110L,
                command,
                IP
        ));
    }

    @Test
    void deleteOptionPromotesRemainingOptionWhenOnlyCorrectIsRemoved() {
        Question question = createQuestion("Form Promotion", QuestionType.MULTIPLE_CHOICE);
        QuestionOption correct = optionService.createOption(3L, null, AccessProfileType.TEACHER,
                option(question.id(), 1, "Certa", true), IP);
        QuestionOption remaining = optionService.createOption(3L, null, AccessProfileType.TEACHER,
                option(question.id(), 2, "Distrator", false), IP);

        List<QuestionOption> options = optionService.archiveOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                correct.id(),
                IP
        );

        assertEquals(1, options.size());
        assertEquals(remaining.id(), options.getFirst().id());
        assertTrue(Boolean.TRUE.equals(options.getFirst().correct()));
    }

    @Test
    void archiveOptionRejectsDeletingLastOption() {
        Question question = createQuestion("Form Last Option", QuestionType.SINGLE_CHOICE);
        QuestionOption option = optionService.createOption(3L, null, AccessProfileType.TEACHER,
                option(question.id(), 1, "Unica", true), IP);

        assertThrows(IllegalArgumentException.class, () -> optionService.archiveOption(
                3L,
                null,
                AccessProfileType.TEACHER,
                option.id(),
                IP
        ));
    }

    private Question createQuestion(String title, QuestionType type) {
        Assessment assessment = assessmentService.createAssessment(
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
                        type.requiresManualScoring()
                                ? AssessmentCorrectionMode.MIXED
                                : AssessmentCorrectionMode.AUTOMATIC,
                        bd("20.00"),
                        bd("10.00"),
                        2,
                        AssessmentState.SCHEDULED,
                        LocalDateTime.of(2026, 2, 11, 10, 15),
                        LocalDateTime.of(2026, 2, 20, 23, 59)
                ),
                IP
        );
        return questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                new QuestionCreateCommand(
                        assessment.id(),
                        "Q-" + type.name().replace('_', '-'),
                        "Question test",
                        type,
                        1,
                        true,
                        bd("10.00"),
                        type == QuestionType.RATING
                                ? QuestionConfiguration.ratingExpectedAnswer("stars_integer", "5", "5")
                                : null
                ),
                IP
        );
    }

    private static QuestionOptionCreateCommand option(long questionId, int order, String text, boolean correct) {
        return new QuestionOptionCreateCommand(
                questionId,
                order,
                text,
                correct
        );
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
