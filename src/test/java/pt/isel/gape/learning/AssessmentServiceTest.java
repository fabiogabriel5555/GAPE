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
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AssessmentCorrectionMode;
import pt.isel.gape.learning.model.AssessmentCreateCommand;
import pt.isel.gape.learning.model.AssessmentMode;
import pt.isel.gape.learning.model.AssessmentState;
import pt.isel.gape.learning.model.AssessmentType;
import pt.isel.gape.learning.model.AssessmentUpdateCommand;
import pt.isel.gape.learning.model.QuestionCreateCommand;
import pt.isel.gape.learning.model.QuestionState;
import pt.isel.gape.learning.model.QuestionType;
import pt.isel.gape.learning.service.AssessmentService;
import pt.isel.gape.learning.service.QuestionService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class AssessmentServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-11T10:15:30Z"), ZoneOffset.UTC);
    private static final String IP = "127.0.0.1";

    private AssessmentService assessmentService;
    private QuestionService questionService;
    private AssessmentDAO assessmentDAO;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        assessmentDAO = new AssessmentDAO(connectionProvider);
        assessmentService = new AssessmentService(connectionProvider, FIXED_CLOCK);
        questionService = new QuestionService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void teacherCanCreateValidOnlineQuestionnaire() {
        Assessment assessment = assessmentService.createAssessment(
                3L,
                null,
                AccessProfileType.TEACHER,
                questionnaire("Form Online Valid", AssessmentMode.ONLINE),
                IP
        );

        assertEquals(AssessmentType.FORM, assessment.type());
        assertEquals(AssessmentMode.ONLINE, assessment.mode());
        assertEquals(40L, assessment.subjectId());
        assertEquals(60L, assessment.contentBlockId());
    }

    @Test
    void teacherCanCreateValidOnsiteQuestionnaire() {
        Assessment assessment = assessmentService.createAssessment(
                3L,
                null,
                AccessProfileType.TEACHER,
                questionnaire("Form In-Person Valid", AssessmentMode.ONSITE),
                IP
        );

        assertEquals(AssessmentType.FORM, assessment.type());
        assertEquals(AssessmentMode.ONSITE, assessment.mode());
    }

    @Test
    void coordinatorCanCreateValidOnsiteExam() {
        Assessment assessment = assessmentService.createAssessment(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                exam("Exam In-Person Valid", AssessmentMode.ONSITE),
                IP
        );

        assertEquals(AssessmentType.EXAM, assessment.type());
        assertEquals(AssessmentMode.ONSITE, assessment.mode());
        assertEquals(40L, assessment.subjectId());
    }

    @Test
    void coordinatorCanCreateValidOnlineExam() {
        Assessment assessment = assessmentService.createAssessment(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                exam("Exam Online Valid", AssessmentMode.ONLINE),
                IP
        );

        assertEquals(AssessmentType.EXAM, assessment.type());
        assertEquals(AssessmentMode.ONLINE, assessment.mode());
    }

    @Test
    void subjectLevelExamPersistsApplicableClassGroups() throws SQLException {
        Assessment assessment = assessmentService.createAssessment(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                exam("Exam With Groups", AssessmentMode.ONLINE),
                IP
        );

        assertEquals(List.of(50L), assessmentDAO.findApplicableClassGroupIds(assessment.id()));
    }

    @Test
    void teacherCanCreateValidBlockExam() {
        Assessment assessment = assessmentService.createAssessment(
                3L,
                null,
                AccessProfileType.TEACHER,
                blockExam("Exam De Bloco Valid", AssessmentMode.ONLINE),
                IP
        );

        assertEquals(AssessmentType.EXAM, assessment.type());
        assertEquals(AssessmentMode.ONLINE, assessment.mode());
        assertEquals(40L, assessment.subjectId());
        assertEquals(60L, assessment.contentBlockId());
    }

    @Test
    void passingGradeCannotExceedMaximumGrade() {
        AssessmentCreateCommand command = new AssessmentCreateCommand(
                null,
                60L,
                "Form Invalid",
                null,
                AssessmentType.FORM,
                AssessmentMode.ONLINE,
                AssessmentCorrectionMode.AUTOMATIC,
                bd("10.00"),
                bd("12.00"),
                1,
                AssessmentState.SCHEDULED,
                start(),
                end()
        );

        assertThrows(IllegalArgumentException.class, () -> assessmentService.createAssessment(
                3L,
                null,
                AccessProfileType.TEACHER,
                command,
                IP
        ));
    }

    @Test
    void attemptsLimitMustBeGreaterThanZeroWhenDefined() {
        AssessmentCreateCommand command = new AssessmentCreateCommand(
                null,
                60L,
                "Form Without Attempts",
                null,
                AssessmentType.FORM,
                AssessmentMode.ONLINE,
                AssessmentCorrectionMode.AUTOMATIC,
                bd("20.00"),
                bd("10.00"),
                0,
                AssessmentState.SCHEDULED,
                start(),
                end()
        );

        assertThrows(IllegalArgumentException.class, () -> assessmentService.createAssessment(
                3L,
                null,
                AccessProfileType.TEACHER,
                command,
                IP
        ));
    }

    @Test
    void inPersonAssessmentMustUseManualCorrection() {
        AssessmentCreateCommand command = new AssessmentCreateCommand(
                null,
                60L,
                "In-Person Automatic Assessment",
                null,
                AssessmentType.FORM,
                AssessmentMode.ONSITE,
                AssessmentCorrectionMode.AUTOMATIC,
                bd("20.00"),
                bd("10.00"),
                1,
                AssessmentState.SCHEDULED,
                start(),
                end()
        );

        assertThrows(IllegalArgumentException.class, () -> assessmentService.createAssessment(
                3L,
                null,
                AccessProfileType.TEACHER,
                command,
                IP
        ));
    }

    @Test
    void questionnaireRequiresContentBlock() {
        AssessmentCreateCommand command = new AssessmentCreateCommand(
                40L,
                null,
                "Form Without Block",
                null,
                AssessmentType.FORM,
                AssessmentMode.ONLINE,
                AssessmentCorrectionMode.AUTOMATIC,
                bd("20.00"),
                bd("10.00"),
                1,
                AssessmentState.SCHEDULED,
                start(),
                end()
        );

        assertThrows(IllegalArgumentException.class, () -> assessmentService.createAssessment(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                command,
                IP
        ));
    }

    @Test
    void subjectLevelExamRequiresApplicableClassGroups() {
        AssessmentCreateCommand command = new AssessmentCreateCommand(
                40L,
                null,
                "Exam Without Groups",
                null,
                AssessmentType.EXAM,
                AssessmentMode.ONLINE,
                AssessmentCorrectionMode.MANUAL,
                bd("20.00"),
                bd("10.00"),
                1,
                AssessmentState.SCHEDULED,
                start(),
                end()
        );

        assertThrows(IllegalArgumentException.class, () -> assessmentService.createAssessment(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                command,
                IP
        ));
    }

    @Test
    void teacherCannotCreateSubjectLevelExamForUnmanagedClassGroup() {
        AssessmentCreateCommand command = new AssessmentCreateCommand(
                41L,
                null,
                "Exam Unauthorized Group",
                null,
                AssessmentType.EXAM,
                AssessmentMode.ONLINE,
                AssessmentCorrectionMode.MANUAL,
                bd("20.00"),
                bd("10.00"),
                1,
                AssessmentState.SCHEDULED,
                start(),
                end(),
                List.of(52L)
        );

        assertThrows(SecurityException.class, () -> assessmentService.createAssessment(
                3L,
                null,
                AccessProfileType.TEACHER,
                command,
                IP
        ));
    }

    @Test
    void submittedAttemptsBlockStructuralAssessmentChanges() {
        Assessment assessment = assessmentService.getAssessment(90L);

        AssessmentUpdateCommand command = new AssessmentUpdateCommand(
                assessment.subjectId(),
                assessment.contentBlockId(),
                assessment.title(),
                assessment.description(),
                assessment.type(),
                assessment.mode(),
                assessment.correctionMode(),
                bd("30.00"),
                assessment.passingGrade(),
                assessment.attemptsLimit(),
                AssessmentState.SCHEDULED,
                start(),
                end()
        );

        assertThrows(IllegalStateException.class, () -> assessmentService.updateAssessment(
                3L,
                null,
                AccessProfileType.TEACHER,
                90L,
                command,
                IP
        ));
    }

    @Test
    void submittedAttemptsBlockAttemptLimitChanges() {
        Assessment assessment = assessmentService.getAssessment(90L);

        AssessmentUpdateCommand command = new AssessmentUpdateCommand(
                assessment.subjectId(),
                assessment.contentBlockId(),
                assessment.title(),
                assessment.description(),
                assessment.type(),
                assessment.mode(),
                assessment.correctionMode(),
                assessment.maxGrade(),
                assessment.passingGrade(),
                assessment.attemptsLimit() + 1,
                assessment.state(),
                assessment.availableFrom(),
                assessment.availableUntil()
        );

        assertThrows(IllegalStateException.class, () -> assessmentService.updateAssessment(
                3L,
                null,
                AccessProfileType.TEACHER,
                assessment.id(),
                command,
                IP
        ));
    }

    @Test
    void automaticCorrectionModeRejectsExistingManualQuestions() {
        Assessment assessment = assessmentService.createAssessment(
                3L,
                null,
                AccessProfileType.TEACHER,
                manualBlockExam("Exam Manual To Automatic Mode", AssessmentMode.ONLINE),
                IP
        );
        questionService.createQuestion(
                3L,
                null,
                AccessProfileType.TEACHER,
                new QuestionCreateCommand(
                        assessment.id(),
                        "Q-MAN",
                        "Question manual",
                        QuestionType.PARAGRAPH,
                        1,
                        true,
                        bd("5.00"),
                        null,
                        QuestionState.ACTIVE
                ),
                IP
        );
        AssessmentUpdateCommand command = new AssessmentUpdateCommand(
                assessment.subjectId(),
                assessment.contentBlockId(),
                assessment.title(),
                assessment.description(),
                assessment.type(),
                assessment.mode(),
                AssessmentCorrectionMode.AUTOMATIC,
                assessment.maxGrade(),
                assessment.passingGrade(),
                assessment.attemptsLimit(),
                AssessmentState.SCHEDULED,
                assessment.availableFrom(),
                assessment.availableUntil()
        );

        assertThrows(IllegalArgumentException.class, () -> assessmentService.updateAssessment(
                3L,
                null,
                AccessProfileType.TEACHER,
                assessment.id(),
                command,
                IP
        ));
    }

    private static AssessmentCreateCommand questionnaire(String title, AssessmentMode mode) {
        return new AssessmentCreateCommand(
                null,
                60L,
                title,
                "Block assessment",
                AssessmentType.FORM,
                mode,
                mode == AssessmentMode.ONSITE ? AssessmentCorrectionMode.MANUAL : AssessmentCorrectionMode.AUTOMATIC,
                bd("20.00"),
                bd("10.00"),
                2,
                AssessmentState.SCHEDULED,
                start(),
                end()
        );
    }

    private static AssessmentCreateCommand exam(String title, AssessmentMode mode) {
        return new AssessmentCreateCommand(
                40L,
                null,
                title,
                "Subject assessment",
                AssessmentType.EXAM,
                mode,
                AssessmentCorrectionMode.MANUAL,
                bd("20.00"),
                bd("10.00"),
                1,
                AssessmentState.SCHEDULED,
                start(),
                end(),
                List.of(50L)
        );
    }

    private static AssessmentCreateCommand blockExam(String title, AssessmentMode mode) {
        return new AssessmentCreateCommand(
                null,
                60L,
                title,
                "Block exam assessment",
                AssessmentType.EXAM,
                mode,
                AssessmentCorrectionMode.AUTOMATIC,
                bd("20.00"),
                bd("10.00"),
                1,
                AssessmentState.SCHEDULED,
                start(),
                end()
        );
    }

    private static AssessmentCreateCommand manualBlockExam(String title, AssessmentMode mode) {
        return new AssessmentCreateCommand(
                null,
                60L,
                title,
                "Manual block exam assessment",
                AssessmentType.EXAM,
                mode,
                AssessmentCorrectionMode.MANUAL,
                bd("20.00"),
                bd("10.00"),
                1,
                AssessmentState.SCHEDULED,
                start(),
                end()
        );
    }

    private static LocalDateTime start() {
        return LocalDateTime.of(2026, 2, 11, 10, 15);
    }

    private static LocalDateTime end() {
        return LocalDateTime.of(2026, 2, 20, 23, 59);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
