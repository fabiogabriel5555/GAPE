package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.AssessmentCorrectionMode;
import pt.isel.gape.learning.model.AssessmentCreateCommand;
import pt.isel.gape.learning.model.AssessmentMode;
import pt.isel.gape.learning.model.AssessmentState;
import pt.isel.gape.learning.model.AssessmentType;
import pt.isel.gape.learning.model.Attempt;
import pt.isel.gape.learning.model.AttemptState;
import pt.isel.gape.learning.model.QuestionCreateCommand;
import pt.isel.gape.learning.model.QuestionType;
import pt.isel.gape.learning.service.AssessmentService;
import pt.isel.gape.learning.service.AttemptService;
import pt.isel.gape.learning.service.QuestionService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class AttemptServiceTest {

    private static final Clock WINDOW_CLOCK = Clock.fixed(Instant.parse("2026-02-11T10:15:30Z"), ZoneOffset.UTC);
    private static final Clock OUTSIDE_WINDOW_CLOCK = Clock.fixed(Instant.parse("2026-03-01T10:15:30Z"), ZoneOffset.UTC);
    private static final String IP = "127.0.0.1";

    private ConnectionProvider connectionProvider;
    private AssessmentService assessmentService;
    private AttemptService attemptService;
    private QuestionService questionService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        connectionProvider = DatabaseTestSupport::openConnection;
        assessmentService = new AssessmentService(connectionProvider, WINDOW_CLOCK);
        attemptService = new AttemptService(connectionProvider, WINDOW_CLOCK);
        questionService = new QuestionService(connectionProvider, WINDOW_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void studentCanStartSecondAttemptInsideWindow() {
        Attempt attempt = attemptService.startAttempt(
                4L,
                null,
                AccessProfileType.STUDENT,
                90L,
                IP
        );

        assertEquals(2, attempt.attemptNumber());
        assertEquals(AttemptState.IN_PROGRESS, attempt.state());
    }

    @Test
    void attemptOutsideAvailabilityWindowIsRejected() {
        AttemptService outsideWindowService = new AttemptService(connectionProvider, OUTSIDE_WINDOW_CLOCK);

        assertThrows(IllegalStateException.class, () -> outsideWindowService.startAttempt(
                4L,
                null,
                AccessProfileType.STUDENT,
                90L,
                IP
        ));
    }

    @Test
    void attemptBeforeAvailabilityStartIsRejected() {
        long assessmentId = assessmentService.createAssessment(
                3L,
                null,
                AccessProfileType.TEACHER,
                assessment(
                        "Future Online Form",
                        AssessmentMode.ONLINE,
                        AssessmentCorrectionMode.AUTOMATIC,
                        java.time.LocalDateTime.of(2026, 2, 11, 10, 16),
                        java.time.LocalDateTime.of(2026, 2, 20, 23, 59)
                ),
                IP
        ).id();
        createQuestion(assessmentId);

        assertThrows(IllegalStateException.class, () -> attemptService.startAttempt(
                4L,
                null,
                AccessProfileType.STUDENT,
                assessmentId,
                IP
        ));
    }

    @Test
    void attemptAtAvailabilityEndIsRejected() {
        long assessmentId = assessmentService.createAssessment(
                3L,
                null,
                AccessProfileType.TEACHER,
                assessment(
                        "Exact End Online Form",
                        AssessmentMode.ONLINE,
                        AssessmentCorrectionMode.AUTOMATIC,
                        java.time.LocalDateTime.of(2026, 2, 11, 10, 15),
                        java.time.LocalDateTime.of(2026, 2, 11, 10, 16)
                ),
                IP
        ).id();
        createQuestion(assessmentId);
        AttemptService exactEndService = new AttemptService(
                connectionProvider,
                Clock.fixed(Instant.parse("2026-02-11T10:16:00Z"), ZoneOffset.UTC)
        );

        assertThrows(IllegalStateException.class, () -> exactEndService.startAttempt(
                4L,
                null,
                AccessProfileType.STUDENT,
                assessmentId,
                IP
        ));
    }

    @Test
    void attemptAboveLimitIsRejected() {
        attemptService.startAttempt(4L, null, AccessProfileType.STUDENT, 90L, IP);

        assertThrows(IllegalStateException.class, () -> attemptService.startAttempt(
                4L,
                null,
                AccessProfileType.STUDENT,
                90L,
                IP
        ));
    }

    @Test
    void inPersonAssessmentCannotBeStartedOnline() {
        long assessmentId = assessmentService.createAssessment(
                3L,
                null,
                AccessProfileType.TEACHER,
                assessment("Onsite Form", AssessmentMode.ONSITE, AssessmentCorrectionMode.MANUAL),
                IP
        ).id();

        assertThrows(IllegalStateException.class, () -> attemptService.startAttempt(
                4L,
                null,
                AccessProfileType.STUDENT,
                assessmentId,
                IP
        ));
    }

    @Test
    void assessmentWithoutQuestionsCannotBeStarted() {
        long assessmentId = assessmentService.createAssessment(
                3L,
                null,
                AccessProfileType.TEACHER,
                assessment("Empty Online Form", AssessmentMode.ONLINE, AssessmentCorrectionMode.AUTOMATIC),
                IP
        ).id();

        assertThrows(IllegalStateException.class, () -> attemptService.startAttempt(
                4L,
                null,
                AccessProfileType.STUDENT,
                assessmentId,
                IP
        ));
    }

    @Test
    void nonStudentCannotStartAttempt() {
        assertThrows(SecurityException.class, () -> attemptService.startAttempt(
                3L,
                null,
                AccessProfileType.TEACHER,
                90L,
                IP
        ));
    }

    @Test
    void inactiveStudentCannotStartAttempt() {
        assertThrows(SecurityException.class, () -> attemptService.startAttempt(
                5L,
                null,
                AccessProfileType.STUDENT,
                90L,
                IP
        ));
    }

    @Test
    void submitRequiresRequiredResponses() {
        Attempt attempt = attemptService.startAttempt(
                4L,
                null,
                AccessProfileType.STUDENT,
                90L,
                IP
        );

        assertThrows(IllegalStateException.class, () -> attemptService.submitAttempt(
                4L,
                null,
                AccessProfileType.STUDENT,
                attempt.id(),
                IP
        ));
    }

    private static AssessmentCreateCommand assessment(
            String title,
            AssessmentMode mode,
            AssessmentCorrectionMode correctionMode
    ) {
        return assessment(
                title,
                mode,
                correctionMode,
                java.time.LocalDateTime.of(2026, 2, 11, 10, 15),
                java.time.LocalDateTime.of(2026, 2, 20, 23, 59)
        );
    }

    private static AssessmentCreateCommand assessment(
            String title,
            AssessmentMode mode,
            AssessmentCorrectionMode correctionMode,
            java.time.LocalDateTime availableFrom,
            java.time.LocalDateTime availableUntil
    ) {
        return new AssessmentCreateCommand(
                null,
                60L,
                mode == AssessmentMode.ONSITE ? "SALA-A1" : null,
                title,
                null,
                AssessmentType.FORM,
                mode,
                correctionMode,
                java.math.BigDecimal.valueOf(20),
                java.math.BigDecimal.TEN,
                1,
                AssessmentState.SCHEDULED,
                availableFrom,
                availableUntil
        );
    }

    private void createQuestion(long assessmentId) {
        questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                new QuestionCreateCommand(
                        assessmentId,
                        "Q-WINDOW-" + assessmentId,
                        "Window validation question",
                        QuestionType.SINGLE_CHOICE,
                        1,
                        true,
                        java.math.BigDecimal.TEN,
                        null
                ),
                IP
        );
    }
}
