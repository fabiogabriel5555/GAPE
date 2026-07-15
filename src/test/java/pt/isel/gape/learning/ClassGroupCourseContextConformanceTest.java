package pt.isel.gape.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import pt.isel.gape.transversal.DatabaseTestSupport;

class ClassGroupCourseContextConformanceTest {

    @BeforeAll
    static void initializeDatabase() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            DatabaseTestSupport.resetDatabase(connection);
            DatabaseTestSupport.executeScript(connection, DatabaseTestSupport.SQL_SEED_DIR.resolve("base.sql"));
            DatabaseTestSupport.executeScript(connection, DatabaseTestSupport.SQL_SEED_DIR.resolve("full.sql"));
        }
    }

    @Test
    void everyClassGroupHasExactlyOneValidDirectCourseOccurrenceContext() throws Exception {
        String sql = """
                SELECT COUNT(*), GROUP_CONCAT(class_group_row.id_class_group ORDER BY class_group_row.id_class_group)
                FROM class_group class_group_row
                LEFT JOIN course_occurrence occurrence_row
                  ON occurrence_row.id_course_occurrence = class_group_row.id_course_occurrence
                LEFT JOIN course_occurrence_period period_row
                  ON period_row.id_course_occurrence_period = class_group_row.id_course_occurrence_period
                LEFT JOIN integrate_subject association_row
                  ON association_row.id_course = class_group_row.id_course
                 AND association_row.id_subject = class_group_row.id_subject
                WHERE occurrence_row.id_course_occurrence IS NULL
                   OR occurrence_row.id_course <> class_group_row.id_course
                   OR period_row.id_course_occurrence <> class_group_row.id_course_occurrence
                   OR period_row.curricular_year <> association_row.curricular_year
                   OR period_row.term <> association_row.term
                   OR class_group_row.starts_at <> period_row.starts_at
                   OR class_group_row.ends_at <> period_row.ends_at
                   OR association_row.id_course IS NULL
                """;
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            resultSet.next();
            assertEquals(0L, resultSet.getLong(1),
                    "Every class group must have one valid direct course occurrence context. Invalid groups: "
                            + resultSet.getString(2));
        }
    }

    @Test
    void computerNetworksRemainsActiveInCoursesWithDifferentCalendars() throws Exception {
        String sql = """
                SELECT COUNT(*)
                FROM integrate_subject first_association
                JOIN integrate_subject second_association
                  ON second_association.id_subject = first_association.id_subject
                 AND second_association.id_course = 34
                 AND second_association.state = 'active'
                JOIN course_period_template first_template
                  ON first_template.id_course = first_association.id_course
                 AND first_template.curricular_year = first_association.curricular_year
                 AND first_template.term = first_association.term
                JOIN course_period_template second_template
                 ON second_template.id_course = second_association.id_course
                 AND second_template.curricular_year = second_association.curricular_year
                 AND second_template.term = second_association.term
                WHERE first_association.id_subject = 43
                  AND first_association.id_course = 32
                  AND first_association.state = 'active'
                  AND (
                      first_template.starts_month <> second_template.starts_month
                      OR first_template.starts_day <> second_template.starts_day
                      OR first_template.ends_month <> second_template.ends_month
                      OR first_template.ends_day <> second_template.ends_day
                  )
                """;
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            resultSet.next();
            assertEquals(1L, resultSet.getLong(1),
                    "Computer Networks must remain active in both course calendars");
        }
    }

    @Test
    void computerNetworksSharedMathematicsPeriodHasOneCourseOccurrencePerCourse() throws Exception {
        String sql = """
                SELECT COUNT(*)
                FROM course_occurrence_period mathematics_one_period
                JOIN course_occurrence mathematics_one_occurrence
                  ON mathematics_one_occurrence.id_course_occurrence = mathematics_one_period.id_course_occurrence
                JOIN course_occurrence_period mathematics_two_period
                  ON mathematics_two_period.curricular_year = mathematics_one_period.curricular_year
                 AND mathematics_two_period.term = mathematics_one_period.term
                 AND mathematics_two_period.starts_at = mathematics_one_period.starts_at
                 AND mathematics_two_period.ends_at = mathematics_one_period.ends_at
                JOIN course_occurrence mathematics_two_occurrence
                  ON mathematics_two_occurrence.id_course_occurrence = mathematics_two_period.id_course_occurrence
                JOIN integrate_subject mathematics_one_association
                  ON mathematics_one_association.id_course = mathematics_one_occurrence.id_course
                 AND mathematics_one_association.id_subject = 43
                 AND mathematics_one_association.curricular_year = mathematics_one_period.curricular_year
                 AND mathematics_one_association.term = mathematics_one_period.term
                 AND mathematics_one_association.state = 'active'
                JOIN integrate_subject mathematics_two_association
                  ON mathematics_two_association.id_course = mathematics_two_occurrence.id_course
                 AND mathematics_two_association.id_subject = 43
                 AND mathematics_two_association.curricular_year = mathematics_two_period.curricular_year
                 AND mathematics_two_association.term = mathematics_two_period.term
                 AND mathematics_two_association.state = 'active'
                WHERE mathematics_one_occurrence.id_course = 34
                  AND mathematics_two_occurrence.id_course = 3008
                  AND mathematics_one_occurrence.label = mathematics_two_occurrence.label
                  AND mathematics_one_period.state = 'active'
                  AND mathematics_two_period.state = 'active'
                """;
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            resultSet.next();
            assertEquals(1L, resultSet.getLong(1),
                    "Mathematics 1 and Mathematics 2 must expose one shared temporal period for Computer Networks");
        }
    }

    @Test
    void subjectsMayExistWithoutCourseAssociations() throws Exception {
        String sql = """
                SELECT COUNT(*), GROUP_CONCAT(subject_row.id_subject ORDER BY subject_row.id_subject)
                FROM subject subject_row
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM integrate_subject association_row
                    WHERE association_row.id_subject = subject_row.id_subject
                )
                """;
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            resultSet.next();
            assertEquals(1L, resultSet.getLong(1),
                    "The full seed must include exactly one subject awaiting its first course association: "
                            + resultSet.getString(2));
            assertEquals("46", resultSet.getString(2));
        }
    }

    @Test
    void inactiveSubjectsHaveNoActiveCourseAssociations() throws Exception {
        String sql = """
                SELECT COUNT(*), GROUP_CONCAT(CONCAT(subject_row.id_subject, ':', association_row.id_course)
                    ORDER BY subject_row.id_subject, association_row.id_course)
                FROM subject subject_row
                JOIN integrate_subject association_row
                  ON association_row.id_subject = subject_row.id_subject
                WHERE subject_row.state = 'inactive'
                  AND association_row.state = 'active'
                """;
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            resultSet.next();
            assertEquals(0L, resultSet.getLong(1),
                    "Inactive subjects cannot retain active course associations: " + resultSet.getString(2));
        }
    }

    @Test
    void courseSubjectAssociationStateIsConsistent() throws Exception {
        String sql = """
                SELECT COUNT(*), GROUP_CONCAT(CONCAT(id_course, ':', id_subject) ORDER BY id_course, id_subject)
                FROM integrate_subject
                WHERE (state = 'active' AND ended_at IS NOT NULL)
                   OR (state = 'historical' AND ended_at IS NULL)
                """;
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            resultSet.next();
            assertEquals(0L, resultSet.getLong(1),
                    "Active associations must have no end date and historical associations must have one. Invalid associations: "
                            + resultSet.getString(2));
        }
    }

    @Test
    void everyStudentEnrollmentHasConcreteStartAndEndDates() throws Exception {
        String sql = """
                SELECT COUNT(*), GROUP_CONCAT(enrollment_type ORDER BY enrollment_type)
                FROM (
                    SELECT 'course' AS enrollment_type
                    FROM enroll_course
                    WHERE start_date IS NULL OR end_date IS NULL
                       OR start_date = '1000-01-01' OR end_date = '1000-01-01'
                    UNION ALL
                    SELECT 'class_group' AS enrollment_type
                    FROM enroll_class_group
                    WHERE start_date IS NULL OR end_date IS NULL
                       OR start_date = '1000-01-01' OR end_date = '1000-01-01'
                    UNION ALL
                    SELECT 'assessment' AS enrollment_type
                    FROM enroll_assessment
                    WHERE start_date IS NULL OR end_date IS NULL
                       OR start_date = '1000-01-01' OR end_date = '1000-01-01'
                ) invalid_enrollment
                """;
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            resultSet.next();
            assertEquals(0L, resultSet.getLong(1),
                    "Every student enrollment must have real start and end dates. Invalid enrollment types: "
                            + resultSet.getString(2));
        }
    }

    @Test
    void everyClassGroupHasAValidRequiredCapacityRange() throws Exception {
        String sql = """
                SELECT COUNT(*), GROUP_CONCAT(id_class_group ORDER BY id_class_group)
                FROM class_group
                WHERE min_students IS NULL
                   OR max_students IS NULL
                   OR min_students <= 0
                   OR max_students <= min_students
                """;
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            resultSet.next();
            assertEquals(0L, resultSet.getLong(1),
                    "Every class group must have a positive minimum and a greater maximum capacity. Invalid groups: "
                            + resultSet.getString(2));
        }
    }

    @Test
    void mathematicsOneKeepsItsHistoricalCohortAndClassGroupDemoStudents() throws Exception {
        String sql = """
                SELECT COUNT(*),
                       SUM(state = 'completed'),
                       SUM(state = 'active')
                FROM enroll_course
                WHERE id_course = 34
                  AND id_course_occurrence = 345
                """;
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             var resultSet = statement.executeQuery()) {
            resultSet.next();
            assertEquals(20L, resultSet.getLong(1));
            assertEquals(5L, resultSet.getLong(2));
            assertEquals(15L, resultSet.getLong(3));
        }
    }
}
