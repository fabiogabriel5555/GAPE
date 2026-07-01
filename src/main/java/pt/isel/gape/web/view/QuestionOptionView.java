package pt.isel.gape.web.view;

import pt.isel.gape.learning.model.QuestionOption;

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

    public String getIdToken() {
        return "|" + option.id() + "|";
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

}
