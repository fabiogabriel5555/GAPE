package pt.isel.gape.web.view;

import java.math.BigDecimal;
import java.util.List;

import pt.isel.gape.learning.model.ContentFormat;
import pt.isel.gape.learning.model.Question;
import pt.isel.gape.learning.model.QuestionConfiguration;
import pt.isel.gape.learning.model.QuestionState;
import pt.isel.gape.learning.model.QuestionType;

public final class QuestionView {

    private final Question question;
    private final List<QuestionOptionView> options;

    private QuestionView(Question question, List<QuestionOptionView> options) {
        this.question = question;
        this.options = List.copyOf(options);
    }

    public static QuestionView from(Question question, List<QuestionOptionView> options) {
        return new QuestionView(question, options);
    }

    public long getId() {
        return question.id();
    }

    public long getAssessmentId() {
        return question.assessmentId();
    }

    public String getCode() {
        return question.code();
    }

    public String getStatement() {
        return question.statement();
    }

    public String getType() {
        return question.type().name();
    }

    public String getTypeValue() {
        return question.type().toDatabaseValue();
    }

    public String getTypeLabel() {
        return switch (question.type()) {
            case SINGLE_CHOICE -> "Single choice";
            case MULTIPLE_CHOICE -> "Multiple choice";
            case SHORT_TEXT -> "Short text";
            case PARAGRAPH -> "Paragraph";
            case FILE_UPLOAD -> "File upload";
            case RATING -> "Rating";
        };
    }

    public String getCategoryLabel() {
        return switch (question.type()) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> "Multiple choice";
            case SHORT_TEXT, PARAGRAPH -> "Text";
            case FILE_UPLOAD -> "File upload";
            case RATING -> "Rating & ranking";
        };
    }

    public String getIconClass() {
        return switch (question.type()) {
            case SINGLE_CHOICE -> "ph ph-radio-button";
            case MULTIPLE_CHOICE -> "ph ph-check-square";
            case SHORT_TEXT -> "ph ph-text-aa";
            case PARAGRAPH -> "ph ph-text-align-left";
            case FILE_UPLOAD -> "ph ph-upload-simple";
            case RATING -> "ph ph-star";
        };
    }

    public int getOrderNo() {
        return question.orderNo();
    }

    public boolean isRequired() {
        return question.required();
    }

    public String getRequiredLabel() {
        return question.required() ? "Required" : "Optional";
    }

    public BigDecimal getScoreRaw() {
        return question.score();
    }

    public String getScore() {
        return question.score() == null ? "-" : question.score().stripTrailingZeros().toPlainString();
    }

    public String getExpectedAnswer() {
        return question.expectedAnswer() == null ? "" : question.expectedAnswer();
    }

    public String getState() {
        return question.state().name();
    }

    public String getStateValue() {
        return question.state().toDatabaseValue();
    }

    public String getStateLabel() {
        return switch (question.state()) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
        };
    }

    public String getStateBadgeClass() {
        return switch (question.state()) {
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-warning-30 text-warning-600";
        };
    }

    public boolean isActive() {
        return question.state() == QuestionState.ACTIVE;
    }

    public boolean isAllowsOptions() {
        return question.type().allowsOptions();
    }

    public boolean isSingleSelectedOption() {
        return question.type().allowsSingleSelectedOption();
    }

    public boolean isTextAnswer() {
        return question.type() == QuestionType.SHORT_TEXT
                || question.type() == QuestionType.PARAGRAPH;
    }

    public boolean isParagraph() {
        return question.type() == QuestionType.PARAGRAPH;
    }

    public boolean isFileUpload() {
        return question.type() == QuestionType.FILE_UPLOAD;
    }

    public List<QuestionOptionView> getOptions() {
        return options;
    }

    public List<QuestionOptionView> getActiveOptions() {
        return options.stream()
                .filter(QuestionOptionView::isActive)
                .toList();
    }

    public int getOptionCount() {
        return options.size();
    }

    public String getRatingStyle() {
        return QuestionConfiguration.ratingStyle(question.expectedAnswer());
    }

    public boolean isRatingStyleStars() {
        return QuestionConfiguration.RATING_STYLE_STARS.equals(getRatingStyle());
    }

    public boolean isRatingStyleCircles() {
        return QuestionConfiguration.RATING_STYLE_CIRCLES.equals(getRatingStyle());
    }

    public boolean isRatingStyleHearts() {
        return QuestionConfiguration.RATING_STYLE_HEARTS.equals(getRatingStyle());
    }

    public String getRatingDesignValue() {
        return QuestionConfiguration.ratingDesignValue(question.expectedAnswer());
    }

    public String getRatingStep() {
        return QuestionConfiguration.ratingStep(question.expectedAnswer());
    }

    public boolean isRatingFractional() {
        return QuestionConfiguration.ratingAllowsFractions(question.expectedAnswer());
    }

    public int getRatingMax() {
        return QuestionConfiguration.ratingMax(question.expectedAnswer());
    }

    public String getRatingExpectedValue() {
        return QuestionConfiguration.ratingExpectedValue(question.expectedAnswer())
                .stripTrailingZeros()
                .toPlainString();
    }

    public List<ContentFormat> getAcceptedFileFormats() {
        return QuestionConfiguration.acceptedFileFormats(question.expectedAnswer());
    }

    public String getAcceptedFileAcceptAttribute() {
        return QuestionConfiguration.acceptAttribute(getAcceptedFileFormats());
    }

    public String getAcceptedFileFormatsLabel() {
        return getAcceptedFileFormats().stream()
                .map(QuestionConfiguration::fileFormatLabel)
                .reduce((left, right) -> left + ", " + right)
                .orElse("Configured files");
    }

    public boolean isAcceptsPdf() {
        return QuestionConfiguration.containsFileFormat(question.expectedAnswer(), ContentFormat.PDF);
    }

    public boolean isAcceptsText() {
        return QuestionConfiguration.containsFileFormat(question.expectedAnswer(), ContentFormat.TEXT);
    }

    public boolean isAcceptsImage() {
        return QuestionConfiguration.containsFileFormat(question.expectedAnswer(), ContentFormat.IMAGE);
    }

    public boolean isAcceptsVideo() {
        return QuestionConfiguration.containsFileFormat(question.expectedAnswer(), ContentFormat.VIDEO);
    }

    public boolean isAcceptsAudio() {
        return QuestionConfiguration.containsFileFormat(question.expectedAnswer(), ContentFormat.AUDIO);
    }

    public boolean isAcceptsArchive() {
        return QuestionConfiguration.containsFileFormat(question.expectedAnswer(), ContentFormat.ARCHIVE);
    }
}
