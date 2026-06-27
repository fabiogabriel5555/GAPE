package pt.isel.gape.web.controller;

import java.sql.SQLException;
import java.util.List;

import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.access.model.User;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.AttemptDAO;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.ContentBlockDAO;
import pt.isel.gape.learning.dao.QuestionDAO;
import pt.isel.gape.learning.dao.QuestionOptionDAO;
import pt.isel.gape.learning.dao.ResponseDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.Attempt;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.Question;
import pt.isel.gape.learning.model.QuestionOption;
import pt.isel.gape.learning.model.Response;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.web.view.AssessmentView;
import pt.isel.gape.web.view.AttemptView;
import pt.isel.gape.web.view.QuestionOptionView;
import pt.isel.gape.web.view.QuestionView;
import pt.isel.gape.web.view.ResponseView;

final class AssessmentViewFactory {

    private final AssessmentDAO assessmentDAO;
    private final QuestionDAO questionDAO;
    private final QuestionOptionDAO optionDAO;
    private final AttemptDAO attemptDAO;
    private final ResponseDAO responseDAO;
    private final SubjectDAO subjectDAO;
    private final ContentBlockDAO contentBlockDAO;
    private final ClassGroupDAO classGroupDAO;
    private final UserDAO userDAO;

    AssessmentViewFactory(
            AssessmentDAO assessmentDAO,
            QuestionDAO questionDAO,
            QuestionOptionDAO optionDAO,
            AttemptDAO attemptDAO,
            ResponseDAO responseDAO,
            SubjectDAO subjectDAO,
            ContentBlockDAO contentBlockDAO,
            ClassGroupDAO classGroupDAO,
            UserDAO userDAO
    ) {
        this.assessmentDAO = assessmentDAO;
        this.questionDAO = questionDAO;
        this.optionDAO = optionDAO;
        this.attemptDAO = attemptDAO;
        this.responseDAO = responseDAO;
        this.subjectDAO = subjectDAO;
        this.contentBlockDAO = contentBlockDAO;
        this.classGroupDAO = classGroupDAO;
        this.userDAO = userDAO;
    }

    AssessmentView assessmentView(Assessment assessment) {
        try {
            Subject subject = assessment.subjectId() == null
                    ? null
                    : subjectDAO.findById(assessment.subjectId()).orElse(null);
            ContentBlock contentBlock = assessment.contentBlockId() == null
                    ? null
                    : contentBlockDAO.findById(assessment.contentBlockId()).orElse(null);
            ClassGroup classGroup = contentBlock == null
                    ? null
                    : classGroupDAO.findById(contentBlock.classGroupId()).orElse(null);
            return AssessmentView.from(
                    assessment,
                    subject == null ? null : subject.name(),
                    subject == null ? null : subject.acronym(),
                    contentBlock == null ? null : contentBlock.name(),
                    contentBlock == null ? null : contentBlock.orderNo(),
                    classGroup == null ? null : classGroup.id(),
                    classGroup == null ? null : classGroup.code(),
                    questionDAO.countActiveByAssessment(assessment.id()),
                    attemptDAO.countByAssessment(assessment.id())
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build assessment view", exception);
        }
    }

    List<AssessmentView> assessmentViews(List<Assessment> assessments) {
        return assessments.stream()
                .map(this::assessmentView)
                .toList();
    }

    QuestionView questionView(Question question) {
        try {
            List<QuestionOptionView> options = optionDAO.findActiveByQuestion(question.id()).stream()
                    .map(QuestionOptionView::from)
                    .toList();
            return QuestionView.from(question, options);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build question view", exception);
        }
    }

    List<QuestionView> questionViews(long assessmentId) {
        try {
            return questionDAO.findByAssessment(assessmentId).stream()
                    .map(this::questionView)
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build question views", exception);
        }
    }

    AttemptView attemptView(Attempt attempt) {
        try {
            Assessment assessment = assessmentDAO.findById(attempt.assessmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + attempt.assessmentId()));
            User user = userDAO.findById(attempt.studentUserId()).orElse(null);
            return AttemptView.from(
                    attempt,
                    user == null ? null : user.name(),
                    user == null ? null : user.email(),
                    assessmentView(assessment)
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build attempt view", exception);
        }
    }

    List<AttemptView> attemptViewsByAssessment(long assessmentId) {
        try {
            return attemptDAO.findByAssessment(assessmentId).stream()
                    .map(this::attemptView)
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build attempt views", exception);
        }
    }

    List<AttemptView> attemptViewsByStudent(long studentUserId) {
        try {
            return attemptDAO.findByStudent(studentUserId).stream()
                    .map(this::attemptView)
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build student attempt views", exception);
        }
    }

    List<ResponseView> responseViews(long attemptId) {
        try {
            return responseDAO.findByAttempt(attemptId).stream()
                    .map(this::responseView)
                    .toList();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build response views", exception);
        }
    }

    private ResponseView responseView(Response response) {
        try {
            Question question = questionDAO.findById(response.questionId())
                    .orElseThrow(() -> new IllegalArgumentException("Question not found: " + response.questionId()));
            List<QuestionOptionView> selectedOptions = optionDAO.findSelectedOptions(response.id()).stream()
                    .map(QuestionOptionView::from)
                    .toList();
            return ResponseView.from(response, questionView(question), selectedOptions);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to build response view", exception);
        }
    }

    QuestionOptionView optionView(QuestionOption option) {
        return QuestionOptionView.from(option);
    }
}
