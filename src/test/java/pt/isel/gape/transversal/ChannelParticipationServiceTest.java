package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.transversal.model.Channel;
import pt.isel.gape.transversal.model.ChannelCreateCommand;
import pt.isel.gape.transversal.model.ChannelParticipation;
import pt.isel.gape.transversal.model.ChannelParticipationCommand;
import pt.isel.gape.transversal.model.ChannelState;
import pt.isel.gape.transversal.model.ChannelType;
import pt.isel.gape.transversal.model.ChannelVisibility;
import pt.isel.gape.transversal.model.ParticipationRole;
import pt.isel.gape.transversal.service.ChannelParticipationService;
import pt.isel.gape.transversal.service.ChannelService;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class ChannelParticipationServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-04T17:00:00Z"), ZoneOffset.UTC);
    private static final String IP = "127.0.0.1";

    private ChannelService channelService;
    private ChannelParticipationService participationService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        channelService = new ChannelService(connectionProvider, FIXED_CLOCK);
        participationService = new ChannelParticipationService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void teacherAddsStudentWithClassGroupAccess() {
        Channel channel = createClassGroupChannel();

        ChannelParticipation participation = participationService.addParticipation(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ChannelParticipationCommand(4L, channel.id(), ParticipationRole.STUDENT, false),
                IP
        );

        assertEquals(4L, participation.userId());
        assertEquals(channel.id(), participation.channelId());
        assertEquals(ParticipationRole.STUDENT, participation.role());
    }

    @Test
    void secondActiveParticipationIsRejected() {
        Channel channel = createClassGroupChannel();
        participationService.addParticipation(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ChannelParticipationCommand(4L, channel.id(), ParticipationRole.STUDENT, false),
                IP
        );

        assertThrows(IllegalStateException.class, () -> participationService.addParticipation(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ChannelParticipationCommand(4L, channel.id(), ParticipationRole.MEMBER, false),
                IP
        ));
    }

    @Test
    void studentOutsideContextCannotJoinClassGroupChannel() throws Exception {
        addActiveStudentWithoutEnrollment(6L);
        Channel channel = createClassGroupChannel();

        assertThrows(SecurityException.class, () -> participationService.addParticipation(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ChannelParticipationCommand(6L, channel.id(), ParticipationRole.STUDENT, false),
                IP
        ));
    }

    @Test
    void nonModeratorCannotAddParticipants() {
        assertThrows(SecurityException.class, () -> participationService.addParticipation(
                4L,
                null,
                AccessProfileType.STUDENT,
                new ChannelParticipationCommand(1L, 190L, ParticipationRole.MEMBER, false),
                IP
        ));
    }

    @Test
    void participantListReflectsAddedUsers() {
        Channel channel = createClassGroupChannel();
        participationService.addParticipation(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ChannelParticipationCommand(4L, channel.id(), ParticipationRole.STUDENT, true),
                IP
        );

        List<ChannelParticipation> participants = participationService.listParticipants(channel.id());

        assertTrue(participants.stream().anyMatch(participation -> participation.userId() == 3L));
        assertTrue(participants.stream().anyMatch(participation -> participation.userId() == 4L));
    }

    private Channel createClassGroupChannel() {
        return channelService.createChannel(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ChannelCreateCommand(
                        "Participation Channel",
                        ChannelType.FORUM,
                        ChannelVisibility.PARTICIPANTS,
                        ChannelState.ACTIVE,
                        List.of(50L),
                        List.of(),
                        List.of()
                ),
                IP
        );
    }

    private static void addActiveStudentWithoutEnrollment(long userId) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement user = connection.prepareStatement("""
                    INSERT INTO user_account (
                        id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt
                    ) VALUES (?, ?, ?, 'active', 'pt-PT', NULL, '2026-01-01 10:00:00', 'hash', 'salt')
                    """)) {
                user.setLong(1, userId);
                user.setString(2, "Outside Student");
                user.setString(3, "outside.student@gape.local");
                user.executeUpdate();
            }
            try (PreparedStatement profile = connection.prepareStatement("""
                    INSERT INTO student_profile (id_user, cod_student)
                    VALUES (?, ?)
                    """)) {
                profile.setLong(1, userId);
                profile.setString(2, "STD-OUT-" + userId);
                profile.executeUpdate();
            }
        }
    }
}
