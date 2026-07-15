package pt.isel.gape.transversal.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.transversal.dao.ChannelDAO;
import pt.isel.gape.transversal.dao.ChannelParticipationDAO;
import pt.isel.gape.transversal.model.Channel;

final class ChannelAccessPolicy {

    private final ChannelDAO channelDAO;
    private final ChannelParticipationDAO participationDAO;

    ChannelAccessPolicy(ChannelDAO channelDAO, ChannelParticipationDAO participationDAO) {
        this.channelDAO = Objects.requireNonNull(channelDAO, "channelDAO is required");
        this.participationDAO = Objects.requireNonNull(participationDAO, "participationDAO is required");
    }

    void requireModerator(
            Connection connection,
            long actorUserId,
            AccessProfileType actorProfileType,
            Channel channel
    ) throws SQLException {
        if (!canModerate(connection, actorUserId, actorProfileType, channel)) {
            throw new SecurityException("User cannot moderate this channel");
        }
    }

    boolean canModerate(
            Connection connection,
            long actorUserId,
            AccessProfileType actorProfileType,
            Channel channel
    ) throws SQLException {
        if (participationDAO.hasModeratorParticipation(connection, actorUserId, channel.id())) {
            return true;
        }
        return canModerateClassGroups(connection, actorUserId, actorProfileType, channel.classGroupIds());
    }

    void requireCanCreateForClassGroups(
            Connection connection,
            long actorUserId,
            AccessProfileType actorProfileType,
            List<Long> classGroupIds
    ) throws SQLException {
        if (!canModerateClassGroups(connection, actorUserId, actorProfileType, classGroupIds)) {
            throw new SecurityException("User cannot create a channel in this context");
        }
    }

    void requireParticipantAccess(Connection connection, long userId, Channel channel) throws SQLException {
        if (!channelDAO.userExistsAndActive(connection, userId)) {
            throw new IllegalArgumentException("Channel participant must be an active user");
        }
        if (!isStudentOnly(connection, userId)) {
            return;
        }
        for (Long classGroupId : channel.classGroupIds()) {
            if (!channelDAO.hasCurrentStudentClassGroupAccess(connection, userId, classGroupId)) {
                throw new SecurityException("Student cannot participate in a channel outside their context");
            }
        }
    }

    private boolean canModerateClassGroups(
            Connection connection,
            long actorUserId,
            AccessProfileType actorProfileType,
            List<Long> classGroupIds
    ) throws SQLException {
        if (classGroupIds == null || classGroupIds.isEmpty()) {
            return actorProfileType != AccessProfileType.STUDENT;
        }
        for (Long classGroupId : classGroupIds) {
            boolean allowed = switch (actorProfileType) {
                case ADMINISTRATOR -> channelDAO.canAdministratorModerateClassGroup(connection, actorUserId, classGroupId);
                case COORDINATOR -> channelDAO.canCoordinatorModerateClassGroup(connection, actorUserId, classGroupId);
                case TEACHER -> channelDAO.canTeacherModerateClassGroup(connection, actorUserId, classGroupId);
                case STUDENT -> false;
            };
            if (!allowed) {
                return false;
            }
        }
        return true;
    }

    private boolean isStudentOnly(Connection connection, long userId) throws SQLException {
        return channelDAO.userHasStudentProfile(connection, userId)
                && !channelDAO.userHasTeacherProfile(connection, userId)
                && !channelDAO.userHasCoordinatorProfile(connection, userId)
                && !channelDAO.userHasAdministratorProfile(connection, userId);
    }
}
