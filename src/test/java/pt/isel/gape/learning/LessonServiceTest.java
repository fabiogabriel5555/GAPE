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
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.learning.model.LessonCreateCommand;
import pt.isel.gape.learning.model.LessonState;
import pt.isel.gape.learning.model.LessonType;
import pt.isel.gape.learning.model.LessonUpdateCommand;
import pt.isel.gape.learning.service.LessonService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class LessonServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-20T10:15:30Z"), ZoneOffset.UTC);
    private static final LocalDateTime FUTURE_START = LocalDateTime.of(2026, 6, 23, 18, 0);
    private static final LocalDateTime OTHER_CLASS_GROUP_START = LocalDateTime.of(2026, 6, 24, 18, 0);

    private LessonService lessonService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        lessonService = new LessonService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void assignedTeacherCanCreateValidOnlineLesson() {
        Lesson lesson = lessonService.createLesson(
                3L,
                null,
                AccessProfileType.TEACHER,
                onlineLesson("Lesson Online Valid", "https://meet.google.com/abc-defg-hij"),
                "127.0.0.1"
        );

        assertEquals(LessonType.ONLINE, lesson.type());
        assertEquals("https://meet.google.com/abc-defg-hij", lesson.accessUrl());
        assertEquals(LessonState.SCHEDULED, lesson.state());
    }

    @Test
    void newLessonUsesNextPedagogicalIdInItsContentBlock() throws SQLException {
        long blockMaximum;
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT GREATEST(
                         COALESCE((SELECT MAX(id_content_item) FROM associate_block_content WHERE id_content_block = 60), 0),
                         COALESCE((SELECT MAX(id_lesson) FROM lesson WHERE id_content_block = 60), 0),
                         COALESCE((SELECT MAX(id_assessment) FROM assessment WHERE id_content_block = 60), 0)
                     )
                     """)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                blockMaximum = resultSet.getLong(1);
            }
        }

        Lesson lesson = lessonService.createLesson(
                3L,
                null,
                AccessProfileType.TEACHER,
                onlineLesson("Lesson Pedagogical ID", "https://meet.google.com/pedagogical-id"),
                "127.0.0.1"
        );

        assertTrue(lesson.id() > blockMaximum);
    }

    @Test
    void coordinatorCanCreateLessonForCoordinatedSubject() {
        Lesson lesson = lessonService.createLesson(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                onlineLesson("Coordinator Lesson", "Teams", "https://teams.microsoft.com/l/meetup-join/abc"),
                "127.0.0.1"
        );

        assertEquals(50L, lesson.classGroupId());
    }

    @Test
    void scopedLearningAdministratorCanCreateLessonForClassGroup() throws Exception {
        addAdministrator(101L, "ADM-LESSON-CLASS", "MANAGE_LEARNING", "CLASS_GROUP", 50L);

        Lesson lesson = lessonService.createLesson(
                101L,
                null,
                AccessProfileType.ADMINISTRATOR,
                onlineLesson("Lesson Admin Scoped", "https://meet.google.com/scoped-admin"),
                "127.0.0.1"
        );

        assertEquals(50L, lesson.classGroupId());
    }

    @Test
    void onlineLessonRequiresAccessUrl() {
        assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.createLesson(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        onlineLesson("Lesson Without Link", null),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void onlineLessonRejectsInvalidAccessUrl() {
        assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.createLesson(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        onlineLesson("Lesson Link Invalid", "http://meet.google.com/abc-defg-hij"),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void onlineLessonRejectsLocalhostAccessUrl() {
        assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.createLesson(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        onlineLesson("Lesson Localhost", "https://localhost/meeting"),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void onlineLessonRequiresProvider() {
        assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.createLesson(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        new LessonCreateCommand(
                                50L,
                                60L,
                                null,
                                "Lesson Without Provider",
                                null,
                                LessonType.ONLINE,
                                null,
                                "https://meet.google.com/abc-defg-hij",
                                true,
                                LessonState.SCHEDULED,
                                FUTURE_START,
                                FUTURE_START.plusHours(1)
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void onlineLessonRejectsProviderLinkMismatch() {
        assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.createLesson(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        onlineLesson("Lesson Wrong Provider", "Zoom", "https://meet.google.com/abc-defg-hij"),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void scheduledLessonRequiresStartDate() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.createLesson(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        new LessonCreateCommand(
                                50L,
                                60L,
                                null,
                                "Lesson In The Past",
                                null,
                                LessonType.ONLINE,
                                "Meet",
                                "https://meet.google.com/past-date",
                                true,
                                LessonState.SCHEDULED,
                                null,
                                LocalDateTime.of(2026, 6, 20, 11, 0)
                        ),
                        "127.0.0.1"
                )
        );

        assertEquals("Lesson start date is required", exception.getMessage());
    }

    @Test
    void scheduledLessonRequiresEndDate() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.createLesson(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        new LessonCreateCommand(
                                50L,
                                60L,
                                null,
                                "Lesson Without End",
                                null,
                                LessonType.ONLINE,
                                "Meet",
                                "https://meet.google.com/no-end-date",
                                true,
                                LessonState.SCHEDULED,
                                FUTURE_START,
                                null
                        ),
                        "127.0.0.1"
                )
        );

        assertEquals("Lesson end date is required", exception.getMessage());
    }

    @Test
    void lessonCreateRejectsPastAndReversedDateRanges() {
        LessonCreateCommand pastStart = new LessonCreateCommand(
                50L,
                60L,
                null,
                "Lesson Past Start",
                null,
                LessonType.ONLINE,
                "Meet",
                "https://meet.google.com/past-start",
                true,
                LessonState.SCHEDULED,
                LocalDateTime.of(2026, 6, 19, 10, 15),
                LocalDateTime.of(2026, 6, 19, 11, 15)
        );
        IllegalArgumentException pastStartException = assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.createLesson(3L, null, AccessProfileType.TEACHER, pastStart, "127.0.0.1")
        );
        assertEquals("Lesson start date cannot be in the past", pastStartException.getMessage());

        LessonCreateCommand reversed = new LessonCreateCommand(
                50L,
                60L,
                null,
                "Lesson Reversed Dates",
                null,
                LessonType.ONLINE,
                "Meet",
                "https://meet.google.com/reversed-dates",
                true,
                LessonState.SCHEDULED,
                FUTURE_START,
                FUTURE_START.minusMinutes(1)
        );
        IllegalArgumentException reversedException = assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.createLesson(3L, null, AccessProfileType.TEACHER, reversed, "127.0.0.1")
        );
        assertEquals("Lesson end date must be after start date", reversedException.getMessage());
    }

    @Test
    void lessonCreatedForCurrentSlotIsActive() {
        LocalDateTime currentMinute = LocalDateTime.of(2026, 6, 20, 10, 15);

        Lesson lesson = lessonService.createLesson(
                3L,
                null,
                AccessProfileType.TEACHER,
                new LessonCreateCommand(
                        50L,
                        60L,
                        null,
                        "Lesson Starting Now",
                        null,
                        LessonType.ONLINE,
                        "Meet",
                        "https://meet.google.com/current-slot",
                        true,
                        LessonState.SCHEDULED,
                        currentMinute,
                        currentMinute.plusHours(1)
                ),
                "127.0.0.1"
        );

        assertEquals(LessonState.ACTIVE, lesson.state());
    }

    @Test
    void futureLessonSubmittedAsScheduledStaysScheduled() {
        Lesson lesson = lessonService.createLesson(
                3L,
                null,
                AccessProfileType.TEACHER,
                new LessonCreateCommand(
                        50L,
                        60L,
                        null,
                        "Lesson Future With Manual State",
                        null,
                        LessonType.ONLINE,
                        "Meet",
                        "https://meet.google.com/future-manual-state",
                        true,
                        LessonState.SCHEDULED,
                        FUTURE_START.plusDays(5),
                        FUTURE_START.plusDays(5).plusHours(1)
                ),
                "127.0.0.1"
        );

        assertEquals(LessonState.SCHEDULED, lesson.state());
    }

    @Test
    void scheduledLessonBecomesActiveWhenCurrentTimeIsInsideLessonWindow() throws Exception {
        long lessonId = insertStoredOnlineLesson(
                "Lesson Scheduled To Active",
                LessonState.SCHEDULED,
                LocalDateTime.of(2026, 6, 20, 10, 0),
                LocalDateTime.of(2026, 6, 20, 11, 0)
        );

        Lesson lesson = lessonService.getLesson(
                3L,
                null,
                AccessProfileType.TEACHER,
                lessonId,
                "127.0.0.1"
        );

        assertEquals(LessonState.ACTIVE, lesson.state());
    }

    @Test
    void activeLessonBecomesCompletedWhenEndTimeIsReached() throws Exception {
        long lessonId = insertStoredOnlineLesson(
                "Lesson Active To Completed",
                LessonState.ACTIVE,
                LocalDateTime.of(2026, 6, 20, 9, 0),
                LocalDateTime.of(2026, 6, 20, 10, 15)
        );

        Lesson lesson = lessonService.getLesson(
                3L,
                null,
                AccessProfileType.TEACHER,
                lessonId,
                "127.0.0.1"
        );

        assertEquals(LessonState.COMPLETED, lesson.state());
    }

    @Test
    void onsiteLessonRequiresPhysicalRoom() {
        assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.createLesson(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        onsiteLesson("Lesson Without Room", null, FUTURE_START),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void onsiteLessonRequiresExistingPhysicalRoom() {
        assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.createLesson(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        onsiteLesson("Lesson Missing Room", "SALA-NONE", FUTURE_START),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void onsiteLessonRejectsRoomFromAnotherOrganization() {
        assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.createLesson(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        onsiteLesson("Lesson External Room", "SALA-X1", FUTURE_START),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void onsiteLessonRejectsOverlappingRoomReservation() {
        lessonService.createLesson(
                3L,
                null,
                AccessProfileType.TEACHER,
                onsiteLesson("Lesson Original", "SALA-A1", FUTURE_START),
                "127.0.0.1"
        );

        assertThrows(
                IllegalStateException.class,
                () -> lessonService.createLesson(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        onsiteLesson("Lesson Sobreposta", "SALA-A1", FUTURE_START.plusMinutes(30)),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void onsiteLessonsMayOverlapWhenTheyUseDifferentPhysicalRooms() throws SQLException {
        insertPhysicalRoom("SALA-B2", 35);
        lessonService.createLesson(
                3L,
                null,
                AccessProfileType.TEACHER,
                onsiteLesson("Lesson Room A", "SALA-A1", FUTURE_START),
                "127.0.0.1"
        );

        Lesson secondLesson = lessonService.createLesson(
                3L,
                null,
                AccessProfileType.TEACHER,
                onsiteLesson("Lesson Room B", "SALA-B2", FUTURE_START.plusMinutes(30)),
                "127.0.0.1"
        );

        assertEquals("SALA-B2", secondLesson.physicalRoomCode());
    }

    @Test
    void onsiteLessonCanUseAvailableActiveRoom() {
        Lesson lesson = lessonService.createLesson(
                3L,
                null,
                AccessProfileType.TEACHER,
                onsiteLesson("Lesson In-Person Valid", "SALA-A1", FUTURE_START.plusHours(2)),
                "127.0.0.1"
        );

        assertEquals(LessonType.ONSITE, lesson.type());
        assertEquals("SALA-A1", lesson.physicalRoomCode());
    }

    @Test
    void onsiteLessonRejectsRoomBelowActiveEnrollmentCount() throws Exception {
        insertPhysicalRoom("SALA-SMALL", 1);
        addActiveStudentEnrollment(6L, 50L);

        assertThrows(
                IllegalStateException.class,
                () -> lessonService.createLesson(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        onsiteLesson("Lesson Small Room", "SALA-SMALL", FUTURE_START.plusHours(4)),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void hybridLessonRequiresPhysicalRoom() {
        assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.createLesson(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        hybridLesson(
                                "Lesson Hybrid Without Room",
                                null,
                                "https://meet.google.com/hybrid-no-room",
                                FUTURE_START.plusHours(2)
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void hybridLessonRequiresAccessUrl() {
        assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.createLesson(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        hybridLesson(
                                "Lesson Hybrid Without Link",
                                "SALA-A1",
                                null,
                                FUTURE_START.plusHours(2)
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void hybridLessonCanUseMeetingLinkAndPhysicalRoom() {
        Lesson lesson = lessonService.createLesson(
                3L,
                null,
                AccessProfileType.TEACHER,
                hybridLesson(
                        "Lesson Hybrid Valid",
                        "SALA-A1",
                        "https://meet.google.com/hybrid-valid",
                        FUTURE_START.plusHours(2)
                ),
                "127.0.0.1"
        );

        assertEquals(LessonType.HYBRID, lesson.type());
        assertEquals("SALA-A1", lesson.physicalRoomCode());
        assertEquals("https://meet.google.com/hybrid-valid", lesson.accessUrl());
    }

    @Test
    void lessonEndMustBeAfterStart() {
        assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.createLesson(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        new LessonCreateCommand(
                                50L,
                                60L,
                                null,
                                "Lesson Datas Invalids",
                                null,
                                LessonType.ONLINE,
                                "meet",
                                "https://meet.google.com/abc-defg-hij",
                                true,
                                LessonState.SCHEDULED,
                                FUTURE_START.plusDays(1),
                                FUTURE_START.plusDays(1)
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void lessonContentBlockMustBelongToSameClassGroup() {
        assertThrows(
                IllegalArgumentException.class,
                () -> lessonService.createLesson(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        new LessonCreateCommand(
                                50L,
                                62L,
                                null,
                                "Lesson Wrong Block",
                                null,
                                LessonType.ONLINE,
                                "meet",
                                "https://meet.google.com/abc-defg-hij",
                                true,
                                LessonState.SCHEDULED,
                                FUTURE_START.plusDays(1),
                                FUTURE_START.plusDays(1).plusHours(1)
                        ),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void studentCanListLessonsForActiveEnrollment() {
        List<Lesson> lessons = lessonService.listLessonsForStudent(
                4L,
                null,
                AccessProfileType.STUDENT,
                "127.0.0.1"
        );

        assertTrue(lessons.stream().anyMatch(lesson -> lesson.id() == 80L));
        assertTrue(lessons.stream().allMatch(lesson -> lesson.classGroupId() == 50L));
    }

    @Test
    void studentCannotReadLessonWithoutClassGroupEnrollment() {
        Lesson otherClassGroupLesson = lessonService.createLesson(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new LessonCreateCommand(
                        52L,
                        62L,
                        null,
                        "Lesson Class Group Without Enrollment",
                        null,
                        LessonType.ONLINE,
                        "meet",
                        "https://meet.google.com/no-enrollment",
                        true,
                        LessonState.SCHEDULED,
                        OTHER_CLASS_GROUP_START,
                        OTHER_CLASS_GROUP_START.plusHours(1)
                ),
                "127.0.0.1"
        );

        assertThrows(
                SecurityException.class,
                () -> lessonService.getLesson(
                        4L,
                        null,
                        AccessProfileType.STUDENT,
                        otherClassGroupLesson.id(),
                        "127.0.0.1"
                )
        );
    }

    @Test
    void lessonCanBeUpdated() {
        Lesson created = lessonService.createLesson(
                3L,
                null,
                AccessProfileType.TEACHER,
                onlineLesson("Lesson Para Update", "https://meet.google.com/abc-defg-hij"),
                "127.0.0.1"
        );

        Lesson updated = lessonService.updateLesson(
                3L,
                null,
                AccessProfileType.TEACHER,
                created.id(),
                new LessonUpdateCommand(
                        50L,
                        60L,
                        null,
                        "Lesson Updated",
                        "Descricao atualizada",
                        LessonType.ONLINE,
                        "meet",
                        "https://meet.google.com/xyz-abcd-efg",
                        true,
                        LessonState.SCHEDULED,
                        FUTURE_START,
                        FUTURE_START.plusHours(1)
                ),
                "127.0.0.1"
        );

        assertEquals("Lesson Updated", updated.title());
        assertEquals(LessonState.SCHEDULED, updated.state());
    }

    @Test
    void lessonWithoutDependenciesCanBeDeleted() throws Exception {
        Lesson created = lessonService.createLesson(
                3L,
                null,
                AccessProfileType.TEACHER,
                onlineLesson("Lesson Para Delete", "https://meet.google.com/abc-defg-hij"),
                "127.0.0.1"
        );

        lessonService.deleteLesson(
                3L,
                null,
                AccessProfileType.TEACHER,
                created.id(),
                "127.0.0.1"
        );

        assertFalse(lessonExists(created.id()));
    }

    @Test
    void lessonWithScheduleOrAttendanceCannotBeDeleted() {
        assertThrows(
                IllegalStateException.class,
                () -> lessonService.deleteLesson(
                        3L,
                        null,
                        AccessProfileType.TEACHER,
                        80L,
                        "127.0.0.1"
                )
        );
    }

    @Test
    void personalCalendarDoesNotUseAdministratorManagementScope() {
        Lesson otherClassGroupLesson = createOtherClassGroupLesson();

        List<Lesson> adminCalendar = lessonService.listPersonalCalendarLessons(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                "127.0.0.1"
        );

        assertTrue(adminCalendar.isEmpty());
        assertFalse(adminCalendar.stream().anyMatch(lesson -> lesson.id() == 80L));
        assertFalse(adminCalendar.stream().anyMatch(lesson -> lesson.id() == otherClassGroupLesson.id()));
    }

    @Test
    void personalCalendarUsesTeacherCoordinatorAndStudentContext() {
        Lesson otherClassGroupLesson = createOtherClassGroupLesson();

        List<Lesson> teacherCalendar = lessonService.listPersonalCalendarLessons(
                3L,
                null,
                AccessProfileType.TEACHER,
                "127.0.0.1"
        );
        List<Lesson> coordinatorCalendar = lessonService.listPersonalCalendarLessons(
                2L,
                null,
                AccessProfileType.COORDINATOR,
                "127.0.0.1"
        );
        List<Lesson> studentCalendar = lessonService.listPersonalCalendarLessons(
                4L,
                null,
                AccessProfileType.STUDENT,
                "127.0.0.1"
        );

        assertTrue(teacherCalendar.stream().anyMatch(lesson -> lesson.id() == 80L));
        assertFalse(teacherCalendar.stream().anyMatch(lesson -> lesson.id() == otherClassGroupLesson.id()));
        assertTrue(coordinatorCalendar.stream().anyMatch(lesson -> lesson.id() == 80L));
        assertFalse(coordinatorCalendar.stream().anyMatch(lesson -> lesson.id() == otherClassGroupLesson.id()));
        assertTrue(studentCalendar.stream().anyMatch(lesson -> lesson.id() == 80L));
        assertFalse(studentCalendar.stream().anyMatch(lesson -> lesson.id() == otherClassGroupLesson.id()));
    }

    private static LessonCreateCommand onlineLesson(String title, String accessUrl) {
        return onlineLesson(title, "Meet", accessUrl);
    }

    private static LessonCreateCommand onlineLesson(String title, String provider, String accessUrl) {
        return new LessonCreateCommand(
                50L,
                60L,
                null,
                title,
                "Online session created by test",
                LessonType.ONLINE,
                provider,
                accessUrl,
                true,
                LessonState.SCHEDULED,
                FUTURE_START,
                FUTURE_START.plusHours(1)
        );
    }

    private static LessonCreateCommand onsiteLesson(String title, String roomCode, LocalDateTime startsAt) {
        return new LessonCreateCommand(
                50L,
                60L,
                roomCode,
                title,
                "In-person session created by test",
                LessonType.ONSITE,
                null,
                null,
                true,
                LessonState.SCHEDULED,
                startsAt,
                startsAt.plusHours(1)
        );
    }

    private static LessonCreateCommand hybridLesson(
            String title,
            String roomCode,
            String accessUrl,
            LocalDateTime startsAt
    ) {
        return new LessonCreateCommand(
                50L,
                60L,
                roomCode,
                title,
                "Hybrid session created by test",
                LessonType.HYBRID,
                "meet",
                accessUrl,
                true,
                LessonState.SCHEDULED,
                startsAt,
                startsAt.plusHours(1)
        );
    }

    private Lesson createOtherClassGroupLesson() {
        return lessonService.createLesson(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new LessonCreateCommand(
                        52L,
                        62L,
                        null,
                        "Lesson Outside Personal Context",
                        null,
                        LessonType.ONLINE,
                        "meet",
                        "https://meet.google.com/out-context",
                        true,
                        LessonState.SCHEDULED,
                        OTHER_CLASS_GROUP_START,
                        OTHER_CLASS_GROUP_START.plusHours(1)
                ),
                "127.0.0.1"
        );
    }

    private static boolean lessonExists(long lessonId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM lesson
                     WHERE id_lesson = ?
                     """)) {
            statement.setLong(1, lessonId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    private static long insertStoredOnlineLesson(
            String title,
            LessonState state,
            LocalDateTime startsAt,
            LocalDateTime endsAt
    ) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO lesson (
                         id_class_group, id_content_block, cod_physical_room, title, description,
                         type, provider, access_url, attendance_required, state, starts_at, ends_at
                     ) VALUES (50, 60, NULL, ?, ?, 'online', 'Meet', ?, 1, ?, ?, ?)
                     """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, title);
            statement.setString(2, "Online session inserted by test");
            statement.setString(3, "https://meet.google.com/" + title.toLowerCase().replace(' ', '-'));
            statement.setString(4, state.toDatabaseValue());
            statement.setTimestamp(5, java.sql.Timestamp.valueOf(startsAt));
            statement.setTimestamp(6, java.sql.Timestamp.valueOf(endsAt));
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Creating test lesson failed, no id generated");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    private static void insertPhysicalRoom(String code, int capacity) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO physical_room (
                         cod_physical_room, id_organization, id_organic_unit, name,
                         description, capacity, location, state
                     ) VALUES (?, 10, 20, ?, 'Sala pequena test', ?, 'Edificio T', 'active')
                     """)) {
            statement.setString(1, code);
            statement.setString(2, "Sala " + code);
            statement.setInt(3, capacity);
            statement.executeUpdate();
        }
    }

    private static void addActiveStudentEnrollment(long userId, long classGroupId) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement user = connection.prepareStatement("""
                    INSERT INTO user_account (
                        id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt
                    ) VALUES (?, ?, ?, 'active', 'pt-PT', NULL, '2026-01-01 10:00:00', 'hash', 'salt')
                    """)) {
                user.setLong(1, userId);
                user.setString(2, "Capacity Student");
                user.setString(3, "capacity.student" + userId + "@gape.local");
                user.executeUpdate();
            }
            try (PreparedStatement profile = connection.prepareStatement("""
                    INSERT INTO student_profile (id_user, cod_student)
                    VALUES (?, ?)
                    """)) {
                profile.setLong(1, userId);
                profile.setString(2, "STD-CAP-" + userId);
                profile.executeUpdate();
            }
            try (PreparedStatement course = connection.prepareStatement("""
                    INSERT INTO enroll_course (id_student_user, id_course, id_course_occurrence, state, start_date, end_date)
                    VALUES (?, 30, 300, 'active', '2026-01-01', '2026-06-30')
                    """)) {
                course.setLong(1, userId);
                course.executeUpdate();
            }
            try (PreparedStatement classGroup = connection.prepareStatement("""
                    INSERT INTO enroll_class_group (id_student_user, id_class_group, state, start_date, end_date)
                    VALUES (?, ?, 'active', '2026-01-01', '2026-06-30')
                    """)) {
                classGroup.setLong(1, userId);
                classGroup.setLong(2, classGroupId);
                classGroup.executeUpdate();
            }
        }
    }

    private static void addAdministrator(
            long userId,
            String administratorCode,
            String permissionCode,
            String contextType,
            long contextId
    ) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement user = connection.prepareStatement("""
                    INSERT INTO user_account (
                        id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt
                    ) VALUES (?, ?, ?, 'active', 'pt-PT', NULL, '2026-01-01 10:00:00', 'hash', 'salt')
                    """)) {
                user.setLong(1, userId);
                user.setString(2, "Scoped Lesson Admin");
                user.setString(3, "scoped.lesson.admin@gape.local");
                user.executeUpdate();
            }
            try (PreparedStatement profile = connection.prepareStatement("""
                    INSERT INTO administrator_profile (id_user, cod_administrator)
                    VALUES (?, ?)
                    """)) {
                profile.setLong(1, userId);
                profile.setString(2, administratorCode);
                profile.executeUpdate();
            }
            try (PreparedStatement grant = connection.prepareStatement("""
                    INSERT INTO grant_administrator (id_admin_user, cod_permission, context_type, context_id)
                    VALUES (?, ?, ?, ?)
                    """)) {
                grant.setLong(1, userId);
                grant.setString(2, permissionCode);
                grant.setString(3, contextType);
                grant.setLong(4, contextId);
                grant.executeUpdate();
            }
        }
    }
}
