package pt.isel.gape.learning.model;

import java.util.List;

public record ResponseCommand(
        long questionId,
        String answer,
        String attachment,
        List<Long> optionIds,
        boolean uploadedAttachment
) {
    public ResponseCommand(long questionId, String answer, String attachment, List<Long> optionIds) {
        this(questionId, answer, attachment, optionIds, false);
    }

    public ResponseCommand {
        optionIds = optionIds == null ? List.of() : List.copyOf(optionIds);
    }
}
