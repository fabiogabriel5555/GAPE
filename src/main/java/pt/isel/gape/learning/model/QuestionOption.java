package pt.isel.gape.learning.model;

public record QuestionOption(
        long id,
        long questionId,
        int orderNo,
        String text,
        Boolean correct
) {
}
