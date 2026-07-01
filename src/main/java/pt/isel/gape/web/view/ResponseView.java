package pt.isel.gape.web.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import pt.isel.gape.learning.model.QuestionConfiguration;
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
        return response.score() == null ? "Not assigned yet" : getScore() + " / " + question.getScore();
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

    public String getSelectedOptionTokens() {
        if (selectedOptions.isEmpty()) {
            return "";
        }
        return selectedOptions.stream()
                .map(option -> Long.toString(option.getId()))
                .collect(Collectors.joining("|", "|", "|"));
    }

    public List<QuestionOptionView> getCorrectOptions() {
        return question.getActiveOptions().stream()
                .filter(QuestionOptionView::isCorrect)
                .toList();
    }

    public String getExpectedOptionTokens() {
        List<QuestionOptionView> correctOptions = getCorrectOptions();
        if (correctOptions.isEmpty()) {
            return "";
        }
        return correctOptions.stream()
                .map(option -> Long.toString(option.getId()))
                .collect(Collectors.joining("|", "|", "|"));
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

    public boolean isObjectiveQuestion() {
        return question.isAutomaticallyScoredObjective();
    }

    public boolean isHasExpectedAnswer() {
        if (question.isAllowsOptions()) {
            return !getCorrectOptions().isEmpty();
        }
        if (question.getTypeValue().equals("rating")) {
            return !question.getExpectedAnswer().isBlank();
        }
        return !getExpectedDisplayAnswer().isBlank();
    }

    public boolean isObjectiveWithExpectedAnswer() {
        return isObjectiveQuestion() && isHasExpectedAnswer();
    }

    public boolean isAnswerMatchesExpected() {
        if (!isObjectiveWithExpectedAnswer()) {
            return false;
        }
        if (question.isAllowsOptions()) {
            Set<Long> selectedIds = selectedOptions.stream()
                    .map(QuestionOptionView::getId)
                    .collect(Collectors.toUnmodifiableSet());
            Set<Long> correctIds = getCorrectOptions().stream()
                    .map(QuestionOptionView::getId)
                    .collect(Collectors.toUnmodifiableSet());
            return !correctIds.isEmpty() && selectedIds.equals(correctIds);
        }
        if (question.getTypeValue().equals("rating")) {
            try {
                BigDecimal expected = QuestionConfiguration.ratingExpectedValue(question.getExpectedAnswer());
                BigDecimal submitted = QuestionConfiguration.ratingAnswerValue(getAnswer(), question.getExpectedAnswer());
                return submitted.compareTo(expected) == 0;
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
        return false;
    }

    public String getObjectiveAnswerToneClass() {
        if (!isObjectiveWithExpectedAnswer()) {
            return "is-neutral";
        }
        return isAnswerMatchesExpected() ? "is-correct" : "is-incorrect";
    }

    public String getExpectedDisplayAnswer() {
        if (question.isAllowsOptions()) {
            return getCorrectOptions().stream()
                    .map(QuestionOptionView::getText)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
        }
        if (question.getTypeValue().equals("rating")) {
            if (question.getExpectedAnswer().isBlank()) {
                return "";
            }
            return question.getRatingExpectedValue() + " / " + question.getRatingMax();
        }
        if (question.isFileUpload()) {
            return question.getExpectedAnswer().isBlank()
                    ? ""
                    : "Accepted formats: " + question.getAcceptedFileFormatsLabel();
        }
        return question.getExpectedAnswer();
    }

    public int getRatingFilledUnits() {
        if (!isHasAnswer()) {
            return 0;
        }
        try {
            BigDecimal value = QuestionConfiguration.ratingAnswerValue(getAnswer(), question.getExpectedAnswer());
            return Math.max(0, Math.min(question.getRatingDisplayMax(), value.intValue()));
        } catch (IllegalArgumentException exception) {
            return 0;
        }
    }

    public int getExpectedRatingFilledUnits() {
        if (!question.getTypeValue().equals("rating") || question.getExpectedAnswer().isBlank()) {
            return 0;
        }
        try {
            BigDecimal value = QuestionConfiguration.ratingExpectedValue(question.getExpectedAnswer());
            return Math.max(0, Math.min(question.getRatingDisplayMax(), value.intValue()));
        } catch (IllegalArgumentException exception) {
            return 0;
        }
    }

    public String getQuestionTypeCssClass() {
        return switch (question.getTypeValue()) {
            case "single_choice" -> "is-single-choice";
            case "multiple_choice" -> "is-multiple-choice";
            case "short_text" -> "is-short-text";
            case "paragraph" -> "is-paragraph";
            case "file_upload" -> "is-file-upload";
            case "rating" -> "is-rating";
            default -> "";
        };
    }
}
