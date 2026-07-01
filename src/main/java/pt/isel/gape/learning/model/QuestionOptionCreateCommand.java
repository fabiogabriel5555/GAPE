package pt.isel.gape.learning.model;

public record QuestionOptionCreateCommand(
        long questionId,
        int orderNo,
        String text,
        Boolean correct
) {
}
