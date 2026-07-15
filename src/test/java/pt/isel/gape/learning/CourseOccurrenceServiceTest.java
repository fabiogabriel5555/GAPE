package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.model.CourseOccurrence;
import pt.isel.gape.learning.model.CourseOccurrenceCreateCommand;
import pt.isel.gape.learning.model.CourseOccurrenceState;
import pt.isel.gape.learning.service.CourseOccurrenceService;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class CourseOccurrenceServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-06-04T10:15:30Z"),
            ZoneOffset.UTC
    );

    private CourseOccurrenceService service;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        service = new CourseOccurrenceService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void createsOccurrencePeriodsWithoutInventingSubjectGradeSheets() throws Exception {
        CourseOccurrence occurrence = service.createOccurrence(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new CourseOccurrenceCreateCommand(
                        30L,
                        2027
                ),
                "127.0.0.1"
        );

        assertEquals(30L, occurrence.courseId());
        assertEquals(2027, occurrence.referenceYear());
        assertEquals("2027", occurrence.code());
        assertEquals(CourseOccurrenceState.SCHEDULED, occurrence.state());
        assertEquals(2, count("course_occurrence_period", "id_course_occurrence", occurrence.id()));
        assertEquals(0, count("grade_sheet", "id_course_occurrence", occurrence.id()));
        assertEquals(1, countByText("activity_log", "operation_type", "COURSE_OCCURRENCE_CREATE"));
    }

    @Test
    void rejectsDuplicateReferenceYearWithoutWritingRows() throws Exception {
        int occurrencesBefore = count("course_occurrence", "id_course", 30L);

        assertThrows(IllegalArgumentException.class, () -> service.createOccurrence(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new CourseOccurrenceCreateCommand(
                        30L,
                        2026
                ),
                "127.0.0.1"
        ));

        assertEquals(occurrencesBefore, count("course_occurrence", "id_course", 30L));
    }

    @Test
    void classGroupCodeCanBeReusedInANewOccurrenceButNotWithinTheSameOccurrence() throws Exception {
        CourseOccurrence occurrence = createOccurrence(30L);
        long periodId = firstPeriodId(occurrence.id());

        insertClassGroup(occurrence.id(), periodId, "PRJ-T1");

        assertEquals(1, count("class_group", "id_course_occurrence", occurrence.id()));
        assertThrows(SQLException.class, () -> insertClassGroup(occurrence.id(), periodId, "PRJ-T1"));
    }

    @Test
    void missingCourseTemplatesArePersistedBeforeOccurrenceCreation() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM course_period_template WHERE id_course = 31"
             )) {
            statement.executeUpdate();
        }

        CourseOccurrence occurrence = createOccurrence(31L);

        assertNotNull(occurrence);
        assertEquals(2, count("course_period_template", "id_course", 31L));
        assertEquals(2, count("course_occurrence_period", "id_course_occurrence", occurrence.id()));
    }

    @Test
    void rejectsOccurrenceThatOverlapsThePreviousEditionBeforeWritingRows() throws Exception {
        int occurrencesBefore = count("course_occurrence", "id_course", 30L);

        assertThrows(IllegalArgumentException.class, () -> service.createOccurrence(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new CourseOccurrenceCreateCommand(
                        30L,
                        2026
                ),
                "127.0.0.1"
        ));
        assertEquals(occurrencesBefore, count("course_occurrence", "id_course", 30L));
    }

    @Test
    void studentCannotCreateCourseOccurrence() throws Exception {
        int occurrencesBefore = count("course_occurrence", "id_course", 30L);

        assertThrows(SecurityException.class, () -> service.createOccurrence(
                4L,
                null,
                AccessProfileType.STUDENT,
                new CourseOccurrenceCreateCommand(
                        30L,
                        2027
                ),
                "127.0.0.1"
        ));
        assertEquals(occurrencesBefore, count("course_occurrence", "id_course", 30L));
    }

    @Test
    void derivesScheduledActiveAndCompletedStatesFromOccurrenceDates() {
        assertEquals(
                CourseOccurrenceState.SCHEDULED,
                CourseOccurrenceState.forDates(
                        LocalDate.of(2026, 6, 5),
                        LocalDate.of(2026, 12, 31),
                        LocalDate.now(FIXED_CLOCK)
                )
        );
        assertEquals(
                CourseOccurrenceState.ACTIVE,
                CourseOccurrenceState.forDates(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        LocalDate.now(FIXED_CLOCK)
                )
        );
        assertEquals(
                CourseOccurrenceState.COMPLETED,
                CourseOccurrenceState.forDates(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 12, 31),
                        LocalDate.now(FIXED_CLOCK)
                )
        );
    }

    @Test
    void createsParallelPeriodsFromTheSameReferenceYear() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE course
                     SET duration = '2'
                     WHERE id_course = 30
                     """)) {
            statement.executeUpdate();
        }
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO course_period_template (
                         id_course, curricular_year, term, starts_month, starts_day, ends_month, ends_day
                     ) VALUES
                         (30, 2, 'semester_1', 1, 1, 6, 30),
                         (30, 2, 'semester_2', 7, 1, 12, 31)
                     """)) {
            statement.executeUpdate();
        }

        CourseOccurrence occurrence = createOccurrence(30L);

        assertEquals(LocalDate.of(2027, 1, 1), occurrence.startsAt());
        assertEquals(LocalDate.of(2027, 12, 31), occurrence.endsAt());
        assertEquals(4, count("course_occurrence_period", "id_course_occurrence", occurrence.id()));
        assertEquals(2, countPeriodsStartingOn(occurrence.id(), LocalDate.of(2027, 1, 1)));
        assertEquals(2, countPeriodsStartingOn(occurrence.id(), LocalDate.of(2027, 7, 1)));
    }

    @Test
    void createsAnAcademicYearLabelFromEveryCalendarYearCoveredByThePeriods() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE course_period_template
                     SET starts_month = 9, starts_day = 1, ends_month = 2, ends_day = 28
                     WHERE id_course = 30 AND term = 'semester_1'
                     """)) {
            statement.executeUpdate();
        }

        CourseOccurrence occurrence = createOccurrence(30L);

        assertEquals("2027-2028", occurrence.code());
    }

    @Test
    void databaseRejectsAnOccurrenceLabelThatDoesNotMatchItsConcreteCalendar() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO course_occurrence (
                         id_course_occurrence, id_course, reference_year, label, starts_at, ends_at, state
                     ) VALUES (301, 30, 2027, '2027-2028', '2027-01-01', '2027-12-31', 'scheduled')
                     """)) {
            SQLException exception = assertThrows(SQLException.class, statement::executeUpdate);
            DatabaseTestSupport.assertIntegrityException(exception);
        }
    }

    @Test
    void databaseRejectsOccurrenceDatesThatDoNotFollowTheCourseCalendar() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO course_occurrence (
                         id_course_occurrence, id_course, reference_year, label, starts_at, ends_at, state
                     ) VALUES (301, 30, 2027, '2027', '2027-01-02', '2027-12-31', 'scheduled')
                     """)) {
            SQLException exception = assertThrows(SQLException.class, statement::executeUpdate);
            DatabaseTestSupport.assertIntegrityException(exception);
        }
    }

    @Test
    void databaseRejectsOccurrencePeriodsThatDoNotFollowTheCourseCalendar() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement occurrence = connection.prepareStatement("""
                     INSERT INTO course_occurrence (
                         id_course_occurrence, id_course, reference_year, label, starts_at, ends_at, state
                     ) VALUES (301, 30, 2027, '2027', '2027-01-01', '2027-12-31', 'scheduled')
                     """)) {
            occurrence.executeUpdate();
            try (PreparedStatement period = connection.prepareStatement("""
                    INSERT INTO course_occurrence_period (
                        id_course_occurrence_period, id_course_occurrence, curricular_year, term,
                        starts_at, ends_at, state
                    ) VALUES (3011, 301, 1, 'semester_1', '2027-02-01', '2027-06-30', 'scheduled')
                    """)) {
                SQLException exception = assertThrows(SQLException.class, period::executeUpdate);
                DatabaseTestSupport.assertIntegrityException(exception);
            }
        }
    }

    private CourseOccurrence createOccurrence(long courseId) {
        return service.createOccurrence(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new CourseOccurrenceCreateCommand(
                        courseId,
                        2027
                ),
                "127.0.0.1"
        );
    }

    private static long firstPeriodId(long occurrenceId) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id_course_occurrence_period
                     FROM course_occurrence_period
                     WHERE id_course_occurrence = ?
                     ORDER BY starts_at
                     LIMIT 1
                     """)) {
            statement.setLong(1, occurrenceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private static void insertClassGroup(long occurrenceId, long periodId, String code) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO class_group (
                         id_subject, id_course, id_course_occurrence, id_course_occurrence_period,
                         cod_class_group, modality, state, min_students, max_students,
                         starts_at, ends_at, shift
                    ) VALUES (40, 30, ?, ?, ?, 'onsite', 'draft', 5, 30,
                               '2027-01-01', '2027-06-30', 'morning')
                     """)) {
            statement.setLong(1, occurrenceId);
            statement.setLong(2, periodId);
            statement.setString(3, code);
            statement.executeUpdate();
        }
    }

    private static int count(String table, String column, long value) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?";
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static int countByText(String table, String column, String value) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?";
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private static int countPeriodsStartingOn(long occurrenceId, LocalDate startsAt) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM course_occurrence_period
                     WHERE id_course_occurrence = ? AND starts_at = ?
                     """)) {
            statement.setLong(1, occurrenceId);
            statement.setDate(2, java.sql.Date.valueOf(startsAt));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}
