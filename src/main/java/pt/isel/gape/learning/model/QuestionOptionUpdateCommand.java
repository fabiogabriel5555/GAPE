package pt.isel.gape.learning.model;

public record QuestionOptionUpdateCommand(
        int orderNo,
        String text,
        Boolean correct,
        QuestionOptionState state
) {
}
