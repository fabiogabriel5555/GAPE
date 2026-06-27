package pt.isel.gape.web.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import pt.isel.gape.learning.model.Response;

public final class ResponseView {

    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-PT"));

    private final Response response;
    private final QuestionView question;
    private final List<QuestionOptionView> selectedOptions;

    private ResponseView(Response response, QuestionView question, List<QuestionOptionView> selectedOptions) {
        this.response = response;
        this.question = question;
        this.selectedOptions = List.copyOf(selectedOptions);
    }

    public static ResponseView from(
            Response response,
            QuestionView question,
            List<QuestionOptionView> selectedOptions
    ) {
        return new ResponseView(response, question, selectedOptions);
    }

    public long getId() {
        return response.id();
    }

    public long getAttemptId() {
        return response.attemptId();
    }

    public long getQuestionId() {
        return response.questionId();
    }

    public String getCode() {
        return response.code();
    }

    public String getAnswer() {
        return response.answer() == null ? "" : response.answer();
    }

    public String getAttachment() {
        return response.attachment() == null ? "" : response.attachment();
    }

    public String getAttachmentFileName() {
        String attachment = getAttachment();
        int slashIndex = Math.max(attachment.lastIndexOf('/'), attachment.lastIndexOf('\\'));
        return slashIndex >= 0 ? attachment.substring(slashIndex + 1) : attachment;
    }

    public BigDecimal getScoreRaw() {
        return response.score();
    }

    public String getScore() {
        return response.score() == null ? "" : response.score().stripTrailingZeros().toPlainString();
    }

    public String getScoreLabel() {
        return response.score() == null ? "Pending" : getScore() + " / " + question.getScore();
    }

    public String getAnsweredAt() {
        return response.answeredAt() == null ? "" : DISPLAY_DATE_TIME.format(response.answeredAt());
    }

    public QuestionView getQuestion() {
        return question;
    }

    public List<QuestionOptionView> getSelectedOptions() {
        return selectedOptions;
    }

    public boolean isHasAnswer() {
        return response.answer() != null && !response.answer().isBlank();
    }

    public boolean isHasAttachment() {
        return response.attachment() != null && !response.attachment().isBlank();
    }

    public boolean isScored() {
        return response.score() != null;
    }

    public String getDisplayAnswer() {
        if (!selectedOptions.isEmpty()) {
            return selectedOptions.stream()
                    .map(QuestionOptionView::getText)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
        }
        if (isHasAttachment()) {
            return getAttachmentFileName();
        }
        return isHasAnswer() ? getAnswer() : "No answer.";
    }
}
