package pt.isel.gape.transversal.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.dao.ChannelDAO;
import pt.isel.gape.transversal.dao.ChannelParticipationDAO;
import pt.isel.gape.transversal.model.Channel;
import pt.isel.gape.transversal.model.ChannelParticipation;
import pt.isel.gape.transversal.model.ChannelParticipationCommand;
import pt.isel.gape.transversal.model.ParticipationState;

public final class ChannelParticipationService {

    private final ConnectionProvider connectionProvider;
    private final ChannelDAO channelDAO;
    private final ChannelParticipationDAO participationDAO;
    private final ChannelAccessPolicy accessPolicy;
    private final AuditService auditService;
    private final Clock clock;

    public ChannelParticipationService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new ChannelDAO(connectionProvider),
                new ChannelParticipationDAO(connectionProvider),
                new AuditService(new ActivityLogDAO(connectionProvider), clock),
                clock
        );
    }

    public ChannelParticipationService(
            ConnectionProvider connectionProvider,
            ChannelDAO channelDAO,
            ChannelParticipationDAO participationDAO,
            AuditService auditService,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.channelDAO = Objects.requireNonNull(channelDAO, "channelDAO is required");
        this.participationDAO = Objects.requireNonNull(participationDAO, "participationDAO is required");
        this.accessPolicy = new ChannelAccessPolicy(channelDAO, participationDAO);
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public ChannelParticipation addParticipation(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ChannelParticipationCommand command,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    ChannelParticipation participation = addParticipation(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            command,
                            sourceIp
                    );
                    connection.commit();
                    return participation;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditService.record(actorUserId, sessionId, "CHANNEL_PARTICIPATION_ADD",
                    "channel", Long.toString(command == null ? 0 : command.channelId()), "failure", sourceIp);
            throw wrap(exception, "Failed to add channel participation");
        }
    }

    ChannelParticipation addParticipation(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ChannelParticipationCommand command,
            String sourceIp
    ) throws SQLException {
        validateCommand(command);
        Channel channel = requireChannel(connection, command.channelId());
        accessPolicy.requireModerator(connection, actorUserId, actorProfileType, channel);
        accessPolicy.requireParticipantAccess(connection, command.userId(), channel);
        if (participationDAO.hasActiveParticipation(connection, command.userId(), command.channelId())) {
            throw new IllegalStateException("User already has an active participation in this channel");
        }
        participationDAO.upsert(
                connection,
                command.userId(),
                command.channelId(),
                command.role(),
                LocalDateTime.now(clock),
                command.muted(),
                ParticipationState.ACTIVE
        );
        auditService.record(connection, actorUserId, sessionId, "CHANNEL_PARTICIPATION_ADD",
                "channel", Long.toString(command.channelId()), "success", sourceIp);
        return participationDAO.find(connection, command.userId(), command.channelId())
                .orElseThrow(() -> new IllegalStateException("Participation was not persisted"));
    }

    public List<ChannelParticipation> listParticipants(long channelId) {
        try (Connection connection = connectionProvider.getConnection()) {
            return participationDAO.findByChannel(connection, channelId);
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to list channel participants");
        }
    }

    private Channel requireChannel(Connection connection, long channelId) throws SQLException {
        return channelDAO.findById(connection, channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));
    }

    private static void validateCommand(ChannelParticipationCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.userId() <= 0 || command.channelId() <= 0) {
            throw new IllegalArgumentException("User and channel ids must be positive");
        }
        Objects.requireNonNull(command.role(), "channel participation role is required");
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
