package pt.isel.gape.transversal.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.validation.AcademicTextValidator;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.dao.ChannelDAO;
import pt.isel.gape.transversal.dao.ChannelParticipationDAO;
import pt.isel.gape.transversal.model.Channel;
import pt.isel.gape.transversal.model.ChannelCreateCommand;
import pt.isel.gape.transversal.model.ChannelState;
import pt.isel.gape.transversal.model.ChannelType;
import pt.isel.gape.transversal.model.ChannelVisibility;
import pt.isel.gape.transversal.model.ParticipationRole;
import pt.isel.gape.transversal.model.ParticipationState;
import pt.isel.gape.transversal.service.AuditService;

public final class ChannelService {

    private static final int TITLE_MAX_LENGTH = 160;

    private final ConnectionProvider connectionProvider;
    private final ChannelDAO channelDAO;
    private final ChannelParticipationDAO participationDAO;
    private final ChannelAccessPolicy accessPolicy;
    private final AuditService auditService;
    private final Clock clock;

    public ChannelService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new ChannelDAO(connectionProvider),
                new ChannelParticipationDAO(connectionProvider),
                new AuditService(new ActivityLogDAO(connectionProvider), clock),
                clock
        );
    }

    public ChannelService(
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

    public Channel createChannel(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ChannelCreateCommand command,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Channel channel = createChannel(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            command,
                            sourceIp
                    );
                    connection.commit();
                    return channel;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "CHANNEL_CREATE", "new", sourceIp);
            throw wrap(exception, "Failed to create channel");
        }
    }

    Channel createChannel(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ChannelCreateCommand command,
            String sourceIp
    ) throws SQLException {
        validateCommand(command);
        NormalizedChannelContext context = normalizeContext(connection, command);
        if (context.classGroupIds().isEmpty()) {
            if (actorProfileType == AccessProfileType.STUDENT && command.type() != ChannelType.MESSAGE) {
                throw new SecurityException("Students can only create direct message channels");
            }
        } else {
            accessPolicy.requireCanCreateForClassGroups(
                    connection,
                    actorUserId,
                    actorProfileType,
                    context.classGroupIds()
            );
        }

        ChannelCreateCommand normalizedCommand = new ChannelCreateCommand(
                command.title(),
                command.type(),
                command.visibility(),
                command.state(),
                context.classGroupIds(),
                context.contentBlockIds(),
                context.assessmentIds()
        );
        LocalDateTime now = LocalDateTime.now(clock);
        long channelId = channelDAO.create(connection, normalizedCommand, now);
        channelDAO.replaceClassGroups(connection, channelId, context.classGroupIds());
        channelDAO.replaceContentBlocks(connection, channelId, context.contentBlockIds());
        channelDAO.replaceAssessments(connection, channelId, context.assessmentIds());
        participationDAO.upsert(
                connection,
                actorUserId,
                channelId,
                ParticipationRole.OWNER,
                now,
                false,
                ParticipationState.ACTIVE
        );
        auditService.record(connection, actorUserId, sessionId, "CHANNEL_CREATE",
                "channel", Long.toString(channelId), "success", sourceIp);
        return requireChannel(connection, channelId);
    }

    public Channel getChannel(long channelId) {
        try (Connection connection = connectionProvider.getConnection()) {
            return requireChannel(connection, channelId);
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to read channel");
        }
    }

    public boolean canModerate(
            long actorUserId,
            AccessProfileType actorProfileType,
            long channelId
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            return accessPolicy.canModerate(connection, actorUserId, actorProfileType, requireChannel(connection, channelId));
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to evaluate channel moderation");
        }
    }

    private NormalizedChannelContext normalizeContext(Connection connection, ChannelCreateCommand command)
            throws SQLException {
        List<Long> explicitClassGroups = ChannelDAO.orderedUnique(command.classGroupIds());
        List<Long> contentBlocks = ChannelDAO.orderedUnique(command.contentBlockIds());
        List<Long> assessments = ChannelDAO.orderedUnique(command.assessmentIds());
        if (!channelDAO.allClassGroupsExist(connection, explicitClassGroups)) {
            throw new IllegalArgumentException("Channel references a missing class group");
        }
        if (!channelDAO.allContentBlocksExist(connection, contentBlocks)) {
            throw new IllegalArgumentException("Channel references a missing content block");
        }
        if (!channelDAO.allAssessmentsExist(connection, assessments)) {
            throw new IllegalArgumentException("Channel references a missing assessment");
        }

        List<Long> blockClassGroups = channelDAO.findClassGroupIdsForContentBlocks(connection, contentBlocks);
        List<Long> assessmentClassGroups = channelDAO.findClassGroupIdsForAssessments(connection, assessments);
        Set<Long> normalizedClassGroups = new LinkedHashSet<>(explicitClassGroups);
        if (!explicitClassGroups.isEmpty() && !explicitClassGroups.containsAll(blockClassGroups)) {
            throw new IllegalArgumentException("Channel content blocks must belong to the channel class groups");
        }
        if (!explicitClassGroups.isEmpty() && !explicitClassGroups.containsAll(assessmentClassGroups)) {
            throw new IllegalArgumentException("Channel assessments must match the channel class groups");
        }
        normalizedClassGroups.addAll(blockClassGroups);
        normalizedClassGroups.addAll(assessmentClassGroups);
        if (!assessments.isEmpty() && assessmentClassGroups.isEmpty() && explicitClassGroups.isEmpty()) {
            throw new IllegalArgumentException("Assessment channel requires an explicit class group context");
        }
        return new NormalizedChannelContext(
                List.copyOf(normalizedClassGroups),
                contentBlocks,
                assessments
        );
    }

    private Channel requireChannel(Connection connection, long channelId) throws SQLException {
        return channelDAO.findById(connection, channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));
    }

    private static void validateCommand(ChannelCreateCommand command) {
        Objects.requireNonNull(command, "command is required");
        AcademicTextValidator.requireName(command.title(), "Channel title is required");
        if (command.title().trim().length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("Channel title is too long");
        }
        Objects.requireNonNull(command.type(), "channel type is required");
        Objects.requireNonNull(command.visibility(), "channel visibility is required");
        Objects.requireNonNull(command.state(), "channel state is required");
        if (command.state() != ChannelState.ACTIVE && command.visibility() == ChannelVisibility.PUBLIC) {
            throw new IllegalArgumentException("Public channels must be active");
        }
    }

    private void auditFailure(
            long actorUserId,
            Long sessionId,
            String operationType,
            String affectedIdentifier,
            String sourceIp
    ) {
        auditService.record(actorUserId, sessionId, operationType,
                "channel", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }

    private record NormalizedChannelContext(
            List<Long> classGroupIds,
            List<Long> contentBlockIds,
            List<Long> assessmentIds
    ) {
    }
}
