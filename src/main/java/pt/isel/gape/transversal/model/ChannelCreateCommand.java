package pt.isel.gape.transversal.model;

import java.util.List;

public record ChannelCreateCommand(
        String title,
        ChannelType type,
        ChannelVisibility visibility,
        ChannelState state,
        List<Long> classGroupIds,
        List<Long> contentBlockIds,
        List<Long> assessmentIds
) {
    public ChannelCreateCommand {
        classGroupIds = classGroupIds == null ? List.of() : List.copyOf(classGroupIds);
        contentBlockIds = contentBlockIds == null ? List.of() : List.copyOf(contentBlockIds);
        assessmentIds = assessmentIds == null ? List.of() : List.copyOf(assessmentIds);
    }
}
