package pt.isel.gape.web.view;

import pt.isel.gape.learning.model.QuestionOption;
import pt.isel.gape.learning.model.QuestionOptionState;

public final class QuestionOptionView {

    private final QuestionOption option;

    private QuestionOptionView(QuestionOption option) {
        this.option = option;
    }

    public static QuestionOptionView from(QuestionOption option) {
        return new QuestionOptionView(option);
    }

    public long getId() {
        return option.id();
    }

    public long getQuestionId() {
        return option.questionId();
    }

    public int getOrderNo() {
        return option.orderNo();
    }

    public String getText() {
        return option.text();
    }

    public Boolean getCorrect() {
        return option.correct();
    }

    public boolean isCorrect() {
        return Boolean.TRUE.equals(option.correct());
    }

    public String getCorrectLabel() {
        if (option.correct() == null) {
            return "-";
        }
        return option.correct() ? "Correct" : "Incorrect";
    }

    public String getState() {
        return option.state().name();
    }

    public String getStateValue() {
        return option.state().toDatabaseValue();
    }

    public String getStateLabel() {
        return switch (option.state()) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
        };
    }

    public String getStateBadgeClass() {
        return switch (option.state()) {
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-warning-30 text-warning-600";
        };
    }

    public boolean isActive() {
        return option.state() == QuestionOptionState.ACTIVE;
    }
}
