package pt.isel.gape.web.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import pt.isel.gape.learning.model.Attempt;
import pt.isel.gape.learning.model.AttemptState;

public final class AttemptView {

    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-PT"));

    private final Attempt attempt;
    private final String studentName;
    private final String studentEmail;
    private final AssessmentView assessment;

    private AttemptView(Attempt attempt, String studentName, String studentEmail, AssessmentView assessment) {
        this.attempt = attempt;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.assessment = assessment;
    }

    public static AttemptView from(
            Attempt attempt,
            String studentName,
            String studentEmail,
            AssessmentView assessment
    ) {
        return new AttemptView(attempt, studentName, studentEmail, assessment);
    }

    public long getId() {
        return attempt.id();
    }

    public long getStudentUserId() {
        return attempt.studentUserId();
    }

    public long getAssessmentId() {
        return attempt.assessmentId();
    }

    public int getAttemptNumber() {
        return attempt.attemptNumber();
    }

    public String getScore() {
        return gradeLabel(visibleScore());
    }

    public BigDecimal getScoreRaw() {
        return visibleScore();
    }

    public String getScoreOverMaxLabel() {
        BigDecimal score = visibleScore();
        return score == null ? "Not assigned yet" : gradeLabel(score) + " / " + assessment.getMaxGrade();
    }

    public String getState() {
        return attempt.state().name();
    }

    public String getStateValue() {
        return attempt.state().toDatabaseValue();
    }

    public String getStateLabel() {
        return switch (attempt.state()) {
            case IN_PROGRESS -> "In progress";
            case SUBMITTED -> "Submitted";
            case CORRECTED -> "Corrected";
            case EXPIRED -> "Expired";
            case CANCELLED -> "Cancelled";
        };
    }

    public String getStateBadgeClass() {
        return switch (attempt.state()) {
            case IN_PROGRESS -> "bg-main-50 text-main-600";
            case SUBMITTED -> "bg-warning-30 text-warning-600";
            case CORRECTED -> "bg-success-50 text-success-600";
            case EXPIRED -> "bg-neutral-30 text-neutral-600";
            case CANCELLED -> "bg-danger-50 text-danger-600";
        };
    }

    public boolean isInProgress() {
        return attempt.state() == AttemptState.IN_PROGRESS;
    }

    public boolean isSubmitted() {
        return attempt.state() == AttemptState.SUBMITTED;
    }

    public boolean isCorrected() {
        return attempt.state() == AttemptState.CORRECTED;
    }

    public boolean isCorrectionOpen() {
        return attempt.state() == AttemptState.SUBMITTED || attempt.state() == AttemptState.CORRECTED;
    }

    public String getStartedAt() {
        return displayDateTime(attempt.startedAt());
    }

    public String getSubmittedAt() {
        return displayDateTime(attempt.submittedAt());
    }

    public String getStudentName() {
        return studentName == null || studentName.isBlank() ? "Unknown student" : studentName;
    }

    public String getStudentEmail() {
        return studentEmail == null ? "" : studentEmail;
    }

    public AssessmentView getAssessment() {
        return assessment;
    }

    public String getResultLabel() {
        BigDecimal score = visibleScore();
        if (score == null) {
            return "Pending correction";
        }
        return score.compareTo(assessment.getPassingGradeRaw()) >= 0 ? "Passed" : "Failed";
    }

    private BigDecimal visibleScore() {
        return attempt.state() == AttemptState.CORRECTED ? attempt.score() : null;
    }

    private static String displayDateTime(LocalDateTime value) {
        return value == null ? "" : DISPLAY_DATE_TIME.format(value);
    }

    private static String gradeLabel(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }
}
