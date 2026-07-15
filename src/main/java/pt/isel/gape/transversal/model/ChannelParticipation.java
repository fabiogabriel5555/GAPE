package pt.isel.gape.transversal.model;

import java.time.LocalDateTime;

public record ChannelParticipation(
        long userId,
        long channelId,
        ParticipationRole role,
        LocalDateTime joinedAt,
        boolean muted,
        ParticipationState state
) {
}
