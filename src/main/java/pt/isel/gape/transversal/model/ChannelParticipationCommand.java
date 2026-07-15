package pt.isel.gape.transversal.model;

public record ChannelParticipationCommand(
        long userId,
        long channelId,
        ParticipationRole role,
        boolean muted
) {
}
