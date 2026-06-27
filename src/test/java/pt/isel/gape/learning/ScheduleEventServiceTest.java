package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
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
import pt.isel.gape.learning.model.ScheduleEvent;
import pt.isel.gape.learning.model.ScheduleEventCreateCommand;
import pt.isel.gape.learning.model.ScheduleEventState;
import pt.isel.gape.learning.model.ScheduleEventType;
import pt.isel.gape.learning.service.ScheduleEventService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class ScheduleEventServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-11T10:15:30Z"), ZoneOffset.UTC);
    private static final String IP = "127.0.0.1";

    private ScheduleEventService scheduleEventService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        scheduleEventService = new ScheduleEventService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void teacherCreatesValidLessonScheduleEvent() throws Exception {
        LocalDateTime startsAt = LocalDateTime.of(2026, 2, 12, 18, 0);
        LocalDateTime endsAt = startsAt.plusHours(2);
        long lessonId = insertOnlineLesson("Schedule Event Lesson", 50L, 60L, startsAt, endsAt);

        ScheduleEvent event = scheduleEventService.createEvent(
                3L,
                null,
                AccessProfileType.TEACHER,
                lessonEvent(lessonId, startsAt, endsAt),
                IP
        );

        assertEquals(ScheduleEventType.LESSON, event.type());
        assertEquals(List.of(50L), event.classGroupIds());
        assertTrue(hasEventClassGroup(event.id(), 50L));
        assertTrue(hasEventRecipient(event.id(), 3L));
        assertTrue(hasEventRecipient(event.id(), 4L));
    }

    @Test
    void invalidEventDatesAreRejected() {
        LocalDateTime startsAt = LocalDateTime.of(2026, 2, 12, 10, 0);

        assertThrows(IllegalArgumentException.class, () -> scheduleEventService.createEvent(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ScheduleEventCreateCommand(
                        null,
                        null,
                        "Invalid Dates",
                        null,
                        ScheduleEventType.MEETING,
                        startsAt,
                        startsAt.minusMinutes(1),
                        false,
                        false,
                        null,
                        ScheduleEventState.ACTIVE,
                        List.of(50L)
                ),
                IP
        ));
    }

    @Test
    void reminderEnabledRequiresMinutesBefore() {
        LocalDateTime startsAt = LocalDateTime.of(2026, 2, 12, 10, 0);

        assertThrows(IllegalArgumentException.class, () -> scheduleEventService.createEvent(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ScheduleEventCreateCommand(
                        null,
                        null,
                        "Reminder Without Minutes",
                        null,
                        ScheduleEventType.MEETING,
                        startsAt,
                        startsAt.plusHours(1),
                        false,
                        true,
                        null,
                        ScheduleEventState.ACTIVE,
                        List.of(50L)
                ),
                IP
        ));
    }

    @Test
    void lessonEventMustUseLessonTypeAndMatchingPeriod() throws Exception {
        LocalDateTime startsAt = LocalDateTime.of(2026, 2, 12, 18, 0);
        LocalDateTime endsAt = startsAt.plusHours(2);
        long lessonId = insertOnlineLesson("Schedule Event Mismatch", 50L, 60L, startsAt, endsAt);

        assertThrows(IllegalArgumentException.class, () -> scheduleEventService.createEvent(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ScheduleEventCreateCommand(
                        lessonId,
                        null,
                        "Wrong Type",
                        null,
                        ScheduleEventType.MEETING,
                        startsAt,
                        endsAt,
                        false,
                        false,
                        null,
                        ScheduleEventState.ACTIVE,
                        List.of()
                ),
                IP
        ));

        assertThrows(IllegalArgumentException.class, () -> scheduleEventService.createEvent(
                3L,
                null,
                AccessProfileType.TEACHER,
                lessonEvent(lessonId, startsAt.plusMinutes(5), endsAt),
                IP
        ));
    }

    @Test
    void assessmentEventOutsideAvailabilityWindowIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> scheduleEventService.createEvent(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ScheduleEventCreateCommand(
                        null,
                        90L,
                        "Assessment Outside Window",
                        null,
                        ScheduleEventType.ASSESSMENT,
                        LocalDateTime.of(2026, 2, 9, 10, 0),
                        LocalDateTime.of(2026, 2, 10, 11, 0),
                        false,
                        false,
                        null,
                        ScheduleEventState.ACTIVE,
                        List.of(50L)
                ),
                IP
        ));
    }

    @Test
    void assessmentEventRejectsClassGroupOutsideSubjectWhenNoExplicitGroupsExist() throws Exception {
        long assessmentId = insertSubjectAssessmentWithoutGroups("Subject Only Assessment", 40L);

        assertThrows(IllegalArgumentException.class, () -> scheduleEventService.createEvent(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new ScheduleEventCreateCommand(
                        null,
                        assessmentId,
                        "Wrong Subject Event",
                        null,
                        ScheduleEventType.ASSESSMENT,
                        LocalDateTime.of(2026, 2, 12, 10, 0),
                        LocalDateTime.of(2026, 2, 12, 11, 0),
                        false,
                        false,
                        null,
                        ScheduleEventState.ACTIVE,
                        List.of(52L)
                ),
                IP
        ));
    }

    @Test
    void visibleEventsAreLimitedByActorContext() throws Exception {
        addActiveStudent(6L);

        List<ScheduleEvent> studentEvents = scheduleEventService.listVisibleEvents(
                4L,
                null,
                AccessProfileType.STUDENT,
                IP
        );
        List<ScheduleEvent> teacherEvents = scheduleEventService.listVisibleEvents(
                3L,
                null,
                AccessProfileType.TEACHER,
                IP
        );
        List<ScheduleEvent> otherStudentEvents = scheduleEventService.listVisibleEvents(
                6L,
                null,
                AccessProfileType.STUDENT,
                IP
        );

        assertTrue(studentEvents.stream().anyMatch(event -> event.id() == 140L));
        assertTrue(teacherEvents.stream().anyMatch(event -> event.id() == 140L));
        assertFalse(otherStudentEvents.stream().anyMatch(event -> event.id() == 140L));
    }

    @Test
    void administratorOrganizationGrantCanSeeScheduleEvents() throws Exception {
        addScopedAdministrator(7L, "ORGANIZATION", 10L);

        List<ScheduleEvent> events = scheduleEventService.listVisibleEvents(
                7L,
                null,
                AccessProfileType.ADMINISTRATOR,
                IP
        );

        assertTrue(events.stream().anyMatch(event -> event.id() == 140L));
    }

    @Test
    void unauthorizedActorCannotCreateScheduleEvent() {
        LocalDateTime startsAt = LocalDateTime.of(2026, 2, 12, 10, 0);

        assertThrows(SecurityException.class, () -> scheduleEventService.createEvent(
                4L,
                null,
                AccessProfileType.STUDENT,
                new ScheduleEventCreateCommand(
                        null,
                        null,
                        "Student Meeting",
                        null,
                        ScheduleEventType.MEETING,
                        startsAt,
                        startsAt.plusHours(1),
                        false,
                        false,
                        null,
                        ScheduleEventState.ACTIVE,
                        List.of(50L)
                ),
                IP
        ));
    }

    @Test
    void teacherCannotCreateScheduleEventForUnmanagedClassGroup() {
        LocalDateTime startsAt = LocalDateTime.of(2026, 2, 12, 10, 0);

        assertThrows(SecurityException.class, () -> scheduleEventService.createEvent(
                3L,
                null,
                AccessProfileType.TEACHER,
                new ScheduleEventCreateCommand(
                        null,
                        null,
                        "Other Group Meeting",
                        null,
                        ScheduleEventType.MEETING,
                        startsAt,
                        startsAt.plusHours(1),
                        false,
                        false,
                        null,
                        ScheduleEventState.ACTIVE,
                        List.of(52L)
                ),
                IP
        ));
    }

    private static ScheduleEventCreateCommand lessonEvent(long lessonId, LocalDateTime startsAt, LocalDateTime endsAt) {
        return new ScheduleEventCreateCommand(
                lessonId,
                null,
                "Lesson Calendar Event",
                "Lesson event created by test",
                ScheduleEventType.LESSON,
                startsAt,
                endsAt,
                false,
                true,
                30,
                ScheduleEventState.ACTIVE,
                List.of()
        );
    }

    private static long insertOnlineLesson(
            String title,
            long classGroupId,
            long contentBlockId,
            LocalDateTime startsAt,
            LocalDateTime endsAt
    ) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO lesson (
                         id_class_group, id_content_block, cod_physical_room, title, description,
                         type, provider, access_url, attendance_required, state, starts_at, ends_at
                     ) VALUES (?, ?, NULL, ?, 'Lesson created by schedule test', 'online',
                         'Meet', ?, 1, 'active', ?, ?)
                     """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, classGroupId);
            statement.setLong(2, contentBlockId);
            statement.setString(3, title);
            statement.setString(4, "https://meet.google.com/" + title.toLowerCase().replace(' ', '-'));
            statement.setTimestamp(5, Timestamp.valueOf(startsAt));
            statement.setTimestamp(6, Timestamp.valueOf(endsAt));
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                generatedKeys.next();
                return generatedKeys.getLong(1);
            }
        }
    }

    private static boolean hasEventClassGroup(long eventId, long classGroupId) throws SQLException {
        return exists("""
                SELECT COUNT(*)
                FROM associate_schedule_event_class_group
                WHERE id_schedule_event = ?
                  AND id_class_group = ?
                """, eventId, classGroupId);
    }

    private static boolean hasEventRecipient(long eventId, long userId) throws SQLException {
        return exists("""
                SELECT COUNT(*)
                FROM receive_schedule_event
                WHERE id_schedule_event = ?
                  AND id_user = ?
                """, eventId, userId);
    }

    private static boolean exists(String sql, long first, long second) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, first);
            statement.setLong(2, second);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    private static long insertSubjectAssessmentWithoutGroups(String title, long subjectId) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO assessment (
                         id_subject, id_content_block, title, description, type, mode, correction_mode,
                         max_grade, passing_grade, attempts_limit, state, available_from, available_until
                     ) VALUES (?, NULL, ?, 'Subject assessment without explicit groups', 'exam', 'onsite',
                         'manual', 20.00, 10.00, 1, 'active', '2026-02-10 00:00:00',
                         '2026-02-20 23:59:59')
                     """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, subjectId);
            statement.setString(2, title);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                generatedKeys.next();
                return generatedKeys.getLong(1);
            }
        }
    }

    private static void addActiveStudent(long userId) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement user = connection.prepareStatement("""
                    INSERT INTO user_account (
                        id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt
                    ) VALUES (?, ?, ?, 'active', 'pt-PT', NULL, '2026-01-01 10:00:00', 'hash', 'salt')
                    """)) {
                user.setLong(1, userId);
                user.setString(2, "Schedule Student " + userId);
                user.setString(3, "schedule.student" + userId + "@gape.local");
                user.executeUpdate();
            }
            try (PreparedStatement profile = connection.prepareStatement("""
                    INSERT INTO student_profile (id_user, cod_student)
                    VALUES (?, ?)
                    """)) {
                profile.setLong(1, userId);
                profile.setString(2, "STD-SCH-" + userId);
                profile.executeUpdate();
            }
        }
    }

    private static void addScopedAdministrator(long userId, String contextType, long contextId) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement user = connection.prepareStatement("""
                    INSERT INTO user_account (
                        id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt
                    ) VALUES (?, ?, ?, 'active', 'pt-PT', NULL, '2026-01-01 10:00:00', 'hash', 'salt')
                    """)) {
                user.setLong(1, userId);
                user.setString(2, "Schedule Administrator " + userId);
                user.setString(3, "schedule.admin" + userId + "@gape.local");
                user.executeUpdate();
            }
            try (PreparedStatement profile = connection.prepareStatement("""
                    INSERT INTO administrator_profile (id_user, cod_administrator)
                    VALUES (?, ?)
                    """)) {
                profile.setLong(1, userId);
                profile.setString(2, "ADM-SCH-" + userId);
                profile.executeUpdate();
            }
            try (PreparedStatement grant = connection.prepareStatement("""
                    INSERT INTO grant_administrator (id_admin_user, cod_permission, context_type, context_id)
                    VALUES (?, 'MANAGE_LEARNING', ?, ?)
                    """)) {
                grant.setLong(1, userId);
                grant.setString(2, contextType);
                grant.setLong(3, contextId);
                grant.executeUpdate();
            }
        }
    }
}
