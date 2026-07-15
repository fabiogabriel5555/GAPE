package pt.isel.gape.transversal.model;

import java.time.LocalDateTime;
import java.util.List;

public record Channel(
        long id,
        String title,
        ChannelType type,
        ChannelVisibility visibility,
        LocalDateTime createdAt,
        ChannelState state,
        List<Long> classGroupIds,
        List<Long> contentBlockIds,
        List<Long> assessmentIds
) {
    public Channel {
        classGroupIds = classGroupIds == null ? List.of() : List.copyOf(classGroupIds);
        contentBlockIds = contentBlockIds == null ? List.of() : List.copyOf(contentBlockIds);
        assessmentIds = assessmentIds == null ? List.of() : List.copyOf(assessmentIds);
    }
}
