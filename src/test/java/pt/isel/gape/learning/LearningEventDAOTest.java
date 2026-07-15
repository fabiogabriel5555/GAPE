package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.LearningEventDAO;
import pt.isel.gape.learning.model.LearningEvent;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class LearningEventDAOTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 2, 12, 12, 0);

    private LearningEventDAO dao;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws Exception {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        dao = new LearningEventDAO(connectionProvider);
        insertLearningEvent(9001L, "/learning/lessons/80", "lesson:80:active");
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void visibleEventStartsUnreadAndBecomesReadForViewer() throws Exception {
        assertEquals(1, unreadCount(3L));

        List<LearningEvent> events = visibleEvents(3L);
        assertEquals(1, events.size());
        assertFalse(events.get(0).read());

        dao.markReadById(3L, events.get(0).id(), NOW);

        assertEquals(0, unreadCount(3L));
        LearningEvent readEvent = visibleEvents(3L).get(0);
        assertTrue(readEvent.read());
        assertEquals(NOW, readEvent.readAt());
    }

    @Test
    void readStateIsIsolatedPerViewer() throws Exception {
        LearningEvent event = visibleEvents(3L).get(0);

        dao.markReadById(3L, event.id(), NOW);

        assertEquals(0, unreadCount(3L));
        assertEquals(1, unreadCount(2L));
        assertFalse(visibleEvents(2L).get(0).read());
    }

    @Test
    void markByHrefReadsAnchoredChildrenWhenRequested() throws Exception {
        insertLearningEvent(9002L, "/learning/class-groups/50#class-group-enrollments", "class-group:50:active");

        dao.markReadByHref(3L, "/learning/class-groups/50", true, NOW);

        assertEquals(1, unreadCount(3L));
        assertTrue(visibleEvents(3L).stream()
                .filter(event -> event.id() == 9002L)
                .findFirst()
                .orElseThrow()
                .read());
    }

    @Test
    void rebuiltEventsUseStandardEnglishTitlesAndIncreasingContext() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            dao.rebuildFromCurrentRecords(connection);
        }

        List<LearningEvent> events = visibleEvents(3L);

        LearningEvent lessonEvent = eventByTitle(events, "Lesson started: Lesson 1");
        assertEquals("PRJ-T1 | PRJ | LEI | DEI | ISG", lessonEvent.contextLabel());
        assertEquals(
                "PRJ-T1 | Project | Computer Engineering | Computer Engineering Department | GAPE Higher Institute",
                lessonEvent.contextTitle()
        );
        assertEquals(LocalDateTime.of(2026, 2, 5, 18, 0), lessonEvent.occurredAt());

        LearningEvent classGroupEvent = eventByTitle(events, "Class group started: PRJ-T1");
        assertEquals("PRJ-T1 | PRJ | LEI | DEI | ISG", classGroupEvent.contextLabel());
        assertEquals(
                "PRJ-T1 | Project | Computer Engineering | Computer Engineering Department | GAPE Higher Institute",
                classGroupEvent.contextTitle()
        );
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), classGroupEvent.occurredAt());
        assertEquals(0, visibleGradeRecordEventCount());
    }

    @Test
    void pendingAssessmentEnrollmentsAndSubmittedAttemptsAreVisibleImmediatelyAndOnlyUntilResolved() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement assessment = connection.prepareStatement("""
                     UPDATE assessment
                     SET available_from = '2099-01-01 00:00:00',
                         available_until = '2099-01-02 23:59:59'
                     WHERE id_assessment = 90
                     """);
             PreparedStatement enrollment = connection.prepareStatement("""
                     UPDATE enroll_assessment
                     SET state = 'pending', start_date = '2099-01-01', end_date = '2099-01-02'
                     WHERE id_student_user = 4 AND id_assessment = 90
                     """);
             PreparedStatement attempt = connection.prepareStatement("""
                      UPDATE attempt
                      SET state = 'submitted',
                          score = NULL,
                          submitted_at = '2099-01-01 10:10:00'
                      WHERE id_attempt = 120
                      """)) {
            assessment.executeUpdate();
            // The schema correctly requires an active assessment enrollment
            // while an attempt is being written, so stage the imported attempt
            // first and only then turn the enrollment into a pending request.
            attempt.executeUpdate();
            enrollment.executeUpdate();
            dao.rebuildFromCurrentRecords(connection);
        }

        // The enrollment dates are intentionally far in the future. Pending work
        // remains an immediate administrative event rather than waiting for the
        // academic occurrence to start.
        LocalDateTime currentTime = LocalDateTime.now().plusMinutes(1);
        List<LearningEvent> pendingEvents = visibleEvents(3L, currentTime);
        assertTrue(pendingEvents.stream().anyMatch(event ->
                "assessment_enrollment_pending".equals(event.eventType())
                        && "pending".equals(event.stateValue())));
        assertTrue(pendingEvents.stream().anyMatch(event ->
                "assessment_correction_pending".equals(event.eventType())
                        && "submitted".equals(event.stateValue())));
        assertTrue(pendingEvents.stream()
                .filter(event -> "assessment_enrollment_pending".equals(event.eventType())
                        || "assessment_correction_pending".equals(event.eventType()))
                .allMatch(event -> !event.occurredAt().isAfter(currentTime)));

        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement enrollment = connection.prepareStatement("""
                     UPDATE enroll_assessment
                     SET state = 'active'
                     WHERE id_student_user = 4 AND id_assessment = 90
                     """);
             PreparedStatement attempt = connection.prepareStatement("""
                     UPDATE attempt
                     SET state = 'corrected', score = 10.000
                     WHERE id_attempt = 120
                     """)) {
            enrollment.executeUpdate();
            attempt.executeUpdate();
            dao.rebuildFromCurrentRecords(connection);
        }

        List<LearningEvent> resolvedEvents = visibleEvents(3L, currentTime);
        assertFalse(resolvedEvents.stream().anyMatch(event ->
                "assessment_enrollment_pending".equals(event.eventType())
                        || "assessment_correction_pending".equals(event.eventType())));
    }

    private static LearningEvent eventByTitle(List<LearningEvent> events, String title) {
        return events.stream()
                .filter(event -> title.equals(event.title()))
                .findFirst()
                .orElseThrow();
    }

    private static int visibleGradeRecordEventCount() throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM learning_event
                     WHERE source_type = 'grade_record'
                       AND visibility_state = 'visible'
                     """)) {
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private int unreadCount(long viewerUserId) throws SQLException {
        return dao.countUnreadVisible(
                List.of(50L),
                List.of(30L),
                List.of(40L),
                null,
                false,
                false,
                "all",
                null,
                null,
                null,
                NOW,
                viewerUserId
        );
    }

    private List<LearningEvent> visibleEvents(long viewerUserId) throws SQLException {
        return visibleEvents(viewerUserId, NOW);
    }

    private List<LearningEvent> visibleEvents(long viewerUserId, LocalDateTime now) throws SQLException {
        return dao.findVisible(
                List.of(50L),
                List.of(30L),
                List.of(40L),
                null,
                false,
                false,
                "all",
                null,
                null,
                null,
                now,
                viewerUserId,
                20,
                0
        );
    }

    private static void insertLearningEvent(long id, String href, String sourceKey) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO learning_event (
                         id_learning_event, source_type, source_key, event_type, category, category_label,
                         title, description, context_label, context_title, id_class_group, id_course, id_subject, id_student_user,
                         detail_href, occurred_at, state_label, state_value, icon_class, badge_class, state_badge_class
                     ) VALUES (
                         ?, 'lesson', ?, 'lesson_active', 'lessons', 'Lesson',
                         'Event read test', 'Test event', 'PRJ-T1', 'Project class group', 50, 30, 40, NULL,
                         ?, '2026-02-10 10:00:00', 'Active', 'active',
                         'ph ph-chalkboard-teacher', 'bg-main-50 text-main-600', 'bg-main-50 text-main-600'
                     )
                     """)) {
            statement.setLong(1, id);
            statement.setString(2, sourceKey);
            statement.setString(3, href);
            statement.executeUpdate();
        }
    }
}
