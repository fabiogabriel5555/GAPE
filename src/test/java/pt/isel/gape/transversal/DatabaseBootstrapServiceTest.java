package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.DatabaseBootstrapMode;
import pt.isel.gape.common.config.DatabaseBootstrapService;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.web.support.DashboardClassGroupPendingEnrollmentCounter;

class DatabaseBootstrapServiceTest {

    private static final Pattern BASIC_EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern SHA256_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");

    @Test
    void shouldBootstrapSchemaOnly() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            new DatabaseBootstrapService().initialize(connection, DatabaseBootstrapMode.SCHEMA);

            assertTrue(DatabaseTestSupport.countRows(connection, "user_account") == 0);
            assertTrue(DatabaseTestSupport.existsConstraint(connection, "user_account", "uq_user_account_email", "UNIQUE"));
        }
    }

    @Test
    void shouldBootstrapDemoSeed() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            new DatabaseBootstrapService().initialize(connection, DatabaseBootstrapMode.DEMO);

            assertTrue(DatabaseTestSupport.countRows(connection, "user_account") >= 5);
            assertTrue(DatabaseTestSupport.countRows(connection, "lesson") >= 1);
            assertTrue(DatabaseTestSupport.countRows(connection, "learning_event") >= 1);
            assertTrue(DatabaseTestSupport.countRows(connection, "message") >= 1);
            assertTemporalAssessmentStatesMatchAvailability(connection);
        }
    }

    @Test
    void shouldBootstrapFullSeed() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            new DatabaseBootstrapService().initialize(connection, DatabaseBootstrapMode.FULL);

            assertTrue(DatabaseTestSupport.countRows(connection, "user_account") >= 16);
            assertTrue(DatabaseTestSupport.countRows(connection, "class_group") >= 7);
            assertTrue(DatabaseTestSupport.countRows(connection, "grade_record") >= 8);
            assertTrue(DatabaseTestSupport.countRows(connection, "learning_event") >= 10);
            assertTrue(DatabaseTestSupport.countRows(connection, "certificate") >= 7);
            assertTrue(DatabaseTestSupport.countRows(connection, "activity_log") >= 25);
            assertFullSeedValueCoverage(connection);
            assertStudent6510FullCoverage(connection);
            assertFullSeedAcademicLifecycleConformance(connection);
            assertTemporalAssessmentStatesMatchAvailability(connection);
            assertFullSeedEmailsAreValid(connection);
            assertFullSeedContentFileHashesAreValid(connection);
            assertFullSeedLongDirectMessageChat(connection);
            assertFullSeedCertificateLifecycle(connection);
            assertFullSeedServiceDomainRules(connection);
            assertFullSeedAssessmentQuestionAttemptConformance(connection);
            assertFullSeedRuntimeCoherence(connection);
            assertFullSeedGradeSheetTopology(connection);
            assertComputerNetworksUsesIndependentCourseCalendars(connection);
            assertDashboardPendingEnrollmentCount(connection);
            List<String> gradeSheetMismatches = fullSeedGradeSheetStateMismatches(connection);
            assertTrue(
                    gradeSheetMismatches.isEmpty(),
                    "Full seed grade sheets must comply with publication and completeness rules: "
                            + String.join("; ", gradeSheetMismatches)
            );
        }
    }

    private static void assertDashboardPendingEnrollmentCount(Connection connection) throws SQLException {
        int persistedEventCount;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT
                    (SELECT COUNT(*)
                     FROM enroll_class_group
                     WHERE state = 'pending')
                    +
                    (SELECT COUNT(DISTINCT attempt.id_attempt)
                     FROM (
                         SELECT cb.id_class_group AS class_group_id, assessment.id_assessment
                         FROM assessment
                         JOIN content_block cb ON cb.id_content_block = assessment.id_content_block
                         UNION
                         SELECT acg.id_class_group AS class_group_id, acg.id_assessment
                         FROM assessment_class_group acg
                     ) applicable
                     JOIN attempt ON attempt.id_assessment = applicable.id_assessment
                     WHERE attempt.state = 'submitted'
                       AND NOT EXISTS (
                           SELECT 1
                           FROM grade_record
                           WHERE grade_record.id_attempt = attempt.id_attempt
                       ))
                """);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            persistedEventCount = resultSet.getInt(1);
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM attempt
                JOIN grade_record ON grade_record.id_attempt = attempt.id_attempt
                WHERE attempt.state = 'submitted'
                """);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            assertEquals(
                    0,
                    resultSet.getInt(1),
                    "A graded attempt must be marked corrected rather than submitted"
            );
        }

        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class },
                (proxy, method, arguments) -> "getRemoteAddr".equals(method.getName()) ? "127.0.0.1" : null
        );
        SessionUser administrator = new SessionUser(
                1L,
                "Administrator",
                "admin@gape.local",
                null,
                Set.of(AccessProfileType.ADMINISTRATOR)
        );
        int dashboardCount = new DashboardClassGroupPendingEnrollmentCounter(DatabaseTestSupport::openConnection)
                .countPendingWork(request, administrator, null);

        assertEquals(
                persistedEventCount,
                dashboardCount,
                "The Class Groups dashboard badge must include pending enrollments and uncorrected attempts"
        );
    }

    private static void assertTemporalAssessmentStatesMatchAvailability(Connection connection) throws SQLException {
        List<String> mismatches = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_assessment, title, state,
                       CASE
                           WHEN available_until IS NOT NULL AND available_until <= CURRENT_TIMESTAMP THEN 'completed'
                           WHEN available_from > CURRENT_TIMESTAMP THEN 'scheduled'
                           ELSE 'active'
                       END AS expected_state
                FROM assessment
                WHERE state <> CASE
                           WHEN available_until IS NOT NULL AND available_until <= CURRENT_TIMESTAMP THEN 'completed'
                           WHEN available_from > CURRENT_TIMESTAMP THEN 'scheduled'
                           ELSE 'active'
                       END
                  AND state IN ('scheduled', 'active')
                ORDER BY id_assessment
                """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                mismatches.add(resultSet.getLong("id_assessment") + " " + resultSet.getString("title")
                        + " expected " + resultSet.getString("expected_state")
                        + " but was " + resultSet.getString("state"));
            }
        }
        assertTrue(
                mismatches.isEmpty(),
                "Assessment seed states must match availability dates: " + String.join("; ", mismatches)
        );
    }

    private static void assertFullSeedAcademicLifecycleConformance(Connection connection) throws SQLException {
        assertNoRows(connection, "Full seed course occurrences must persist their date-derived state", """
                SELECT id_course_occurrence, id_course, state, starts_at, ends_at
                FROM course_occurrence
                WHERE state <> CASE
                    WHEN state = 'cancelled' THEN 'cancelled'
                    WHEN starts_at > CURRENT_DATE THEN 'scheduled'
                    WHEN ends_at < CURRENT_DATE THEN 'completed'
                    ELSE 'active'
                END
                ORDER BY id_course_occurrence
                """);
        assertNoRows(connection, "Full seed occurrence periods must persist their date-derived state", """
                SELECT id_course_occurrence_period, id_course_occurrence, state, starts_at, ends_at
                FROM course_occurrence_period
                WHERE state <> CASE
                    WHEN state = 'cancelled' THEN 'cancelled'
                    WHEN starts_at > CURRENT_DATE THEN 'scheduled'
                    WHEN ends_at < CURRENT_DATE THEN 'completed'
                    ELSE 'active'
                END
                ORDER BY id_course_occurrence_period
                """);
        assertNoRows(connection, "Full seed class groups must persist the state of their concrete period", """
                SELECT id_class_group, cod_class_group, state, starts_at, ends_at
                FROM class_group
                WHERE state <> CASE
                    WHEN starts_at > CURRENT_DATE THEN 'scheduled'
                    WHEN ends_at < CURRENT_DATE THEN 'completed'
                    ELSE 'active'
                END
                ORDER BY id_class_group
                """);
        assertNoRows(connection, "Full seed class groups must retain one exact course occurrence context", """
                SELECT cg.id_class_group, cg.cod_class_group, cg.id_course, cg.id_course_occurrence,
                       cg.id_course_occurrence_period
                FROM class_group cg
                LEFT JOIN course_occurrence occurrence
                  ON occurrence.id_course_occurrence = cg.id_course_occurrence
                LEFT JOIN course_occurrence_period period_row
                  ON period_row.id_course_occurrence_period = cg.id_course_occurrence_period
                LEFT JOIN integrate_subject association_row
                  ON association_row.id_course = cg.id_course
                 AND association_row.id_subject = cg.id_subject
                WHERE occurrence.id_course_occurrence IS NULL
                   OR occurrence.id_course <> cg.id_course
                   OR period_row.id_course_occurrence <> cg.id_course_occurrence
                   OR association_row.id_course IS NULL
                   OR period_row.curricular_year <> association_row.curricular_year
                   OR period_row.term <> association_row.term
                   OR cg.starts_at <> period_row.starts_at
                   OR cg.ends_at <> period_row.ends_at
                ORDER BY cg.id_class_group
                """);
        assertNoRows(connection, "Full seed open class groups must have an open active academic context", """
                SELECT cg.id_class_group, cg.cod_class_group, cg.state,
                       c.state AS course_state, s.state AS subject_state,
                       association_row.state AS association_state,
                       occurrence.state AS occurrence_state, period_row.state AS period_state
                FROM class_group cg
                JOIN course c ON c.id_course = cg.id_course
                JOIN subject s ON s.id_subject = cg.id_subject
                JOIN integrate_subject association_row
                  ON association_row.id_course = cg.id_course
                 AND association_row.id_subject = cg.id_subject
                JOIN course_occurrence occurrence
                  ON occurrence.id_course_occurrence = cg.id_course_occurrence
                JOIN course_occurrence_period period_row
                  ON period_row.id_course_occurrence_period = cg.id_course_occurrence_period
                WHERE (cg.state = 'active'
                       AND (c.state <> 'active'
                            OR s.state <> 'active'
                            OR association_row.state <> 'active'
                            OR occurrence.state <> 'active'
                            OR period_row.state <> 'active'))
                   OR (cg.state = 'scheduled'
                       AND (c.state <> 'active'
                            OR s.state <> 'active'
                            OR association_row.state <> 'active'
                            OR occurrence.state NOT IN ('scheduled', 'active')
                            OR period_row.state NOT IN ('scheduled', 'active')))
                ORDER BY cg.id_class_group
                """);
        assertNoRows(connection, "Full seed active course enrollments cannot outlive their occurrence", """
                SELECT enrollment.id_student_user, enrollment.id_course, enrollment.id_course_occurrence,
                       enrollment.state, occurrence.state AS occurrence_state
                FROM enroll_course enrollment
                JOIN course_occurrence occurrence
                  ON occurrence.id_course_occurrence = enrollment.id_course_occurrence
                WHERE enrollment.state = 'active'
                  AND occurrence.state NOT IN ('scheduled', 'active')
                ORDER BY enrollment.id_student_user, enrollment.id_course, enrollment.id_course_occurrence
                """);
        assertNoRows(connection, "Full seed active class-group enrollments cannot outlive their class group", """
                SELECT enrollment.id_student_user, enrollment.id_class_group,
                       enrollment.state, class_group_row.state AS class_group_state
                FROM enroll_class_group enrollment
                JOIN class_group class_group_row
                  ON class_group_row.id_class_group = enrollment.id_class_group
                WHERE enrollment.state = 'active'
                  AND class_group_row.state <> 'active'
                ORDER BY enrollment.id_student_user, enrollment.id_class_group
                """);
        assertNoRows(connection, "Completed class groups cannot retain unresolvable pending enrollment requests", """
                SELECT enrollment.id_student_user, enrollment.id_class_group,
                       enrollment.state, class_group_row.state AS class_group_state
                FROM enroll_class_group enrollment
                JOIN class_group class_group_row
                  ON class_group_row.id_class_group = enrollment.id_class_group
                WHERE enrollment.state = 'pending'
                  AND class_group_row.state = 'completed'
                ORDER BY enrollment.id_student_user, enrollment.id_class_group
                """);
        assertNoRows(connection, "Completed assessments cannot retain actionable pending enrollment requests", """
                SELECT enrollment.id_student_user, enrollment.id_assessment,
                       enrollment.state, assessment_row.state AS assessment_state
                FROM enroll_assessment enrollment
                JOIN assessment assessment_row
                  ON assessment_row.id_assessment = enrollment.id_assessment
                WHERE enrollment.state = 'pending'
                  AND assessment_row.state = 'completed'
                ORDER BY enrollment.id_student_user, enrollment.id_assessment
                """);
        assertNoRows(connection, "Full seed active teaching assignments cannot outlive their class group", """
                SELECT teaching.id_teacher_user, teaching.id_class_group,
                       teaching.state, class_group_row.state AS class_group_state
                FROM teach_class_group teaching
                JOIN class_group class_group_row
                  ON class_group_row.id_class_group = teaching.id_class_group
                WHERE teaching.state = 'active'
                  AND class_group_row.state <> 'active'
                ORDER BY teaching.id_teacher_user, teaching.id_class_group
                """);
        assertNoRows(connection, "Full seed enrollment dates must always be concrete and ordered", """
                SELECT 'course' AS enrollment_type, id_student_user,
                       CONCAT(id_course, ':', id_course_occurrence) AS context, start_date, end_date
                FROM enroll_course
                WHERE start_date IS NULL OR end_date IS NULL OR end_date < start_date
                UNION ALL
                SELECT 'class_group', id_student_user, CAST(id_class_group AS CHAR), start_date, end_date
                FROM enroll_class_group
                WHERE start_date IS NULL OR end_date IS NULL OR end_date < start_date
                UNION ALL
                SELECT 'assessment', id_student_user, CAST(id_assessment AS CHAR), start_date, end_date
                FROM enroll_assessment
                WHERE start_date IS NULL OR end_date IS NULL OR end_date < start_date
                ORDER BY enrollment_type, id_student_user, context
                """);
        assertNoRows(connection, "Full seed class-group enrollments must derive their period from the class-group occurrence", """
                SELECT enrollment.id_student_user,
                       enrollment.id_class_group,
                       enrollment.state,
                       enrollment.start_date,
                       enrollment.end_date,
                       class_group_row.starts_at,
                       class_group_row.ends_at
                FROM enroll_class_group enrollment
                JOIN class_group class_group_row
                  ON class_group_row.id_class_group = enrollment.id_class_group
                WHERE enrollment.start_date <> class_group_row.starts_at
                   OR (enrollment.state <> 'withdrawn' AND enrollment.end_date <> class_group_row.ends_at)
                   OR (enrollment.state = 'withdrawn'
                       AND (enrollment.end_date < class_group_row.starts_at
                            OR enrollment.end_date > class_group_row.ends_at))
                ORDER BY enrollment.id_student_user, enrollment.id_class_group
                """);
    }

    private static void assertFullSeedRuntimeCoherence(Connection connection) throws SQLException {
        assertNoRows(connection, "Full seed course durations must match CourseService numeric format", """
                SELECT id_course, name, duration
                FROM course
                WHERE duration IS NULL
                   OR duration NOT REGEXP '^[0-9]+$'
                   OR CAST(duration AS UNSIGNED) <= 0
                ORDER BY id_course
                """);
        assertNoRows(connection, "Full seed subject-course years must fit course duration", """
                SELECT isub.id_course, c.name, c.duration, isub.id_subject, isub.curricular_year, isub.term
                FROM integrate_subject isub
                JOIN course c ON c.id_course = isub.id_course
                WHERE isub.curricular_year IS NULL
                   OR isub.curricular_year <= 0
                   OR isub.term IS NULL
                   OR c.duration IS NULL
                   OR c.duration NOT REGEXP '^[0-9]+$'
                   OR isub.curricular_year > CAST(c.duration AS UNSIGNED)
                ORDER BY isub.id_course, isub.id_subject
                """);
        assertNoRows(connection, "Full seed lessons must have concrete valid dates", """
                SELECT id_lesson, title, state, starts_at, ends_at
                FROM lesson
                WHERE starts_at IS NULL
                   OR ends_at IS NULL
                   OR starts_at >= ends_at
                ORDER BY id_lesson
                """);
        assertNoRows(connection, "Full seed lesson states must match lesson dates after runtime sync", """
                SELECT id_lesson, title, state, starts_at, ends_at
                FROM lesson
                WHERE state IN ('scheduled', 'active')
                  AND state <> CASE
                        WHEN ends_at IS NOT NULL AND ends_at <= CURRENT_TIMESTAMP THEN 'completed'
                        WHEN starts_at > CURRENT_TIMESTAMP THEN 'scheduled'
                        ELSE 'active'
                  END
                ORDER BY id_lesson
                """);
        assertNoRows(connection, "Full seed class groups must integrate their subjects", """
                SELECT cg.id_class_group, cg.cod_class_group, cg.id_course, cg.id_subject
                FROM class_group cg
                LEFT JOIN integrate_subject isub
                  ON isub.id_course = cg.id_course
                 AND isub.id_subject = cg.id_subject
                WHERE isub.id_course IS NULL
                ORDER BY cg.id_class_group
                """);
        assertNoRows(connection, "Full seed class groups with scheduled lesson activity must have pedagogical blocks", """
                SELECT cg.id_class_group, cg.cod_class_group
                FROM class_group cg
                WHERE EXISTS (
                        SELECT 1
                        FROM lesson l
                        WHERE l.id_class_group = cg.id_class_group
                  )
                  AND NOT EXISTS (
                        SELECT 1
                        FROM content_block cb
                        WHERE cb.id_class_group = cg.id_class_group
                  )
                ORDER BY cg.id_class_group
                """);
        assertNoRows(connection, "Full seed active class groups must have active context", """
                SELECT cg.id_class_group, cg.cod_class_group, cg.state,
                       c.state AS course_state, s.state AS subject_state
                FROM class_group cg
                JOIN course c ON c.id_course = cg.id_course
                JOIN subject s ON s.id_subject = cg.id_subject
                JOIN integrate_subject isub
                  ON isub.id_course = cg.id_course
                 AND isub.id_subject = cg.id_subject
                WHERE cg.state IN ('scheduled', 'active')
                  AND (c.state <> 'active' OR s.state <> 'active')
                ORDER BY cg.id_class_group
                """);
        assertNoRows(connection, "Full seed lesson type requirements must be satisfied", """
                SELECT id_lesson, title, type, cod_physical_room, access_url
                FROM lesson
                WHERE (type = 'online' AND COALESCE(TRIM(access_url), '') = '')
                   OR (type = 'hybrid' AND COALESCE(TRIM(access_url), '') = '')
                   OR (type = 'onsite' AND cod_physical_room IS NULL)
                ORDER BY id_lesson
                """);
        assertNoRows(connection, "Full seed lessons must use content blocks from the same class group", """
                SELECT l.id_lesson, l.title, l.id_class_group, l.id_content_block, cb.id_class_group AS block_class_group
                FROM lesson l
                JOIN content_block cb ON cb.id_content_block = l.id_content_block
                WHERE cb.id_class_group <> l.id_class_group
                ORDER BY l.id_lesson
                """);
        assertNoRows(connection, "Full seed lesson rooms must be active and in the same organization", """
                SELECT l.id_lesson, l.title, l.cod_physical_room, pr.state,
                       pr.id_organization AS room_organization, c.id_organization AS course_organization
                FROM lesson l
                JOIN physical_room pr ON pr.cod_physical_room = l.cod_physical_room
                JOIN class_group cg ON cg.id_class_group = l.id_class_group
                JOIN course c ON c.id_course = cg.id_course
                WHERE pr.state <> 'active'
                   OR pr.id_organization <> c.id_organization
                ORDER BY l.id_lesson
                """);
        assertNoRows(connection, "Full seed assessments must have valid availability dates", """
                SELECT id_assessment, title, state, available_from, available_until
                FROM assessment
                WHERE available_from IS NULL
                   OR available_until IS NULL
                   OR available_from >= available_until
                ORDER BY id_assessment
                """);
        assertNoRows(connection, "Full seed assessment room requirements must match assessment mode", """
                SELECT id_assessment, title, mode, cod_physical_room
                FROM assessment
                WHERE (mode = 'onsite' AND cod_physical_room IS NULL)
                   OR (mode = 'online' AND cod_physical_room IS NOT NULL)
                ORDER BY id_assessment
                """);
        assertNoRows(connection, "Full seed onsite assessment rooms must be active and in the same organization", """
                SELECT DISTINCT a.id_assessment, a.title, a.cod_physical_room, pr.state,
                       pr.id_organization AS room_organization, c.id_organization AS course_organization
                FROM assessment a
                JOIN physical_room pr ON pr.cod_physical_room = a.cod_physical_room
                LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
                LEFT JOIN assessment_class_group acg ON acg.id_assessment = a.id_assessment
                JOIN class_group cg ON cg.id_class_group = COALESCE(cb.id_class_group, acg.id_class_group)
                JOIN course c ON c.id_course = cg.id_course
                WHERE a.mode = 'onsite'
                  AND (pr.state <> 'active' OR pr.id_organization <> c.id_organization)
                ORDER BY a.id_assessment
                """);
        assertNoRows(connection, "Full seed onsite assessments cannot overlap reserved lessons in the same room", """
                SELECT a.id_assessment, a.title, l.id_lesson, l.title AS lesson_title, a.cod_physical_room
                FROM assessment a
                JOIN lesson l ON l.cod_physical_room = a.cod_physical_room
                WHERE a.mode = 'onsite'
                  AND a.state IN ('scheduled', 'active')
                  AND l.state IN ('scheduled', 'active')
                  AND l.type IN ('onsite', 'hybrid')
                  AND NOT (a.available_until <= l.starts_at OR a.available_from >= l.ends_at)
                ORDER BY a.id_assessment, l.id_lesson
                """);
        assertNoRows(connection, "Full seed onsite assessments cannot overlap each other in the same room", """
                SELECT a1.id_assessment, a1.title, a2.id_assessment AS conflicting_assessment,
                       a2.title AS conflicting_title, a1.cod_physical_room
                FROM assessment a1
                JOIN assessment a2
                  ON a2.cod_physical_room = a1.cod_physical_room
                 AND a2.id_assessment > a1.id_assessment
                WHERE a1.mode = 'onsite'
                  AND a2.mode = 'onsite'
                  AND a1.state IN ('scheduled', 'active')
                  AND a2.state IN ('scheduled', 'active')
                  AND NOT (a1.available_until <= a2.available_from OR a1.available_from >= a2.available_until)
                ORDER BY a1.id_assessment, a2.id_assessment
                """);
        assertNoRows(connection, "Full seed assessment class group links must match subject context", """
                SELECT acg.id_assessment, acg.id_class_group, a.id_subject, cg.id_subject AS class_subject
                FROM assessment_class_group acg
                JOIN assessment a ON a.id_assessment = acg.id_assessment
                JOIN class_group cg ON cg.id_class_group = acg.id_class_group
                WHERE a.id_subject IS NOT NULL
                  AND a.id_subject <> cg.id_subject
                ORDER BY acg.id_assessment, acg.id_class_group
                """);
        assertNoRows(connection, "Full seed attempts must match score and submission state rules", """
                SELECT id_attempt, id_assessment, id_student_user, state, score, submitted_at
                FROM attempt
                WHERE (state IN ('submitted', 'corrected') AND submitted_at IS NULL)
                   OR (state <> 'corrected' AND score IS NOT NULL)
                ORDER BY id_attempt
                """);
        assertNoRows(connection, "Full seed attempts must have an actionable assessment enrollment", """
                SELECT attempt.id_attempt, attempt.id_student_user, attempt.id_assessment,
                       enrollment.state AS enrollment_state
                FROM attempt
                LEFT JOIN enroll_assessment enrollment
                  ON enrollment.id_student_user = attempt.id_student_user
                 AND enrollment.id_assessment = attempt.id_assessment
                WHERE enrollment.id_student_user IS NULL
                   OR enrollment.state NOT IN ('active', 'completed')
                ORDER BY attempt.id_attempt
                """);
        assertNoRows(connection, "Full seed scheduled assessments must not already contain attempts", """
                SELECT attempt.id_attempt, attempt.id_assessment, assessment.title,
                       assessment.available_from, attempt.started_at
                FROM attempt
                JOIN assessment ON assessment.id_assessment = attempt.id_assessment
                WHERE assessment.state = 'scheduled'
                ORDER BY attempt.id_attempt
                """);
        assertNoRows(connection, "Full seed attachment messages must have an attachment", """
                SELECT id_message, title, type, attachment
                FROM message
                WHERE type = 'attachment'
                  AND COALESCE(TRIM(attachment), '') = ''
                ORDER BY id_message
                """);
        assertNoRows(connection, "Full seed message replies must stay in the same channel", """
                SELECT child.id_message, child.title, child.id_channel, parent.id_channel AS parent_channel
                FROM message child
                JOIN message parent ON parent.id_message = child.id_parent_message
                WHERE child.id_channel <> parent.id_channel
                ORDER BY child.id_message
                """);
        assertNoRows(connection, "Full seed message dates must be coherent", """
                SELECT id_message, title, state, created_at, updated_at, scheduled_at, sent_at
                FROM message
                WHERE (updated_at IS NOT NULL AND updated_at < created_at)
                   OR (scheduled_at IS NOT NULL AND scheduled_at < created_at)
                   OR (sent_at IS NOT NULL AND sent_at < created_at)
                   OR (state = 'scheduled' AND scheduled_at IS NULL)
                ORDER BY id_message
                """);
        assertNoRows(connection, "Full seed read receipts must be delivered before read", """
                SELECT id_user, id_message, delivered_at, read_at, state
                FROM receive_message
                WHERE read_at IS NOT NULL
                  AND (delivered_at IS NULL OR read_at < delivered_at)
                ORDER BY id_user, id_message
                """);
    }

    private static void assertFullSeedGradeSheetTopology(Connection connection) throws SQLException {
        assertNoRows(connection, "Full seed consolidated grade sheets must have a real exact class-group context", """
                SELECT gs.id_grade_sheet, gs.id_subject, gs.id_course_occurrence, gs.type,
                       gs.subject_occurrence_aggregate_id
                FROM grade_sheet gs
                WHERE gs.scope = 'subject_occurrence'
                  AND (
                        gs.type <> 'final'
                        OR NOT (gs.subject_occurrence_aggregate_id <=> gs.id_course_occurrence)
                        OR EXISTS (
                            SELECT 1
                            FROM associate_grade_sheet_class_group agscg
                            WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                        )
                        OR NOT EXISTS (
                            SELECT 1
                            FROM class_group cg
                            WHERE cg.id_subject = gs.id_subject
                              AND cg.id_course_occurrence = gs.id_course_occurrence
                        )
                  )
                ORDER BY gs.id_grade_sheet
                """);
        assertNoRows(connection, "Full seed class-group grade sheets must keep one aligned class group", """
                SELECT gs.id_grade_sheet, gs.id_subject, gs.id_course_occurrence,
                       COUNT(agscg.id_class_group) AS class_group_count
                FROM grade_sheet gs
                LEFT JOIN associate_grade_sheet_class_group agscg
                  ON agscg.id_grade_sheet = gs.id_grade_sheet
                LEFT JOIN class_group cg ON cg.id_class_group = agscg.id_class_group
                WHERE gs.scope = 'class_group'
                GROUP BY gs.id_grade_sheet, gs.id_subject, gs.id_course_occurrence
                HAVING class_group_count <> 1
                    OR SUM(cg.id_subject <> gs.id_subject OR cg.id_course_occurrence <> gs.id_course_occurrence) <> 0
                ORDER BY gs.id_grade_sheet
                """);
        assertNoRows(connection, "Every class group must have exactly one final source grade sheet", """
                SELECT cg.id_class_group, cg.cod_class_group, COUNT(gs.id_grade_sheet) AS final_source_count
                FROM class_group cg
                LEFT JOIN associate_grade_sheet_class_group agscg
                  ON agscg.id_class_group = cg.id_class_group
                LEFT JOIN grade_sheet gs
                  ON gs.id_grade_sheet = agscg.id_grade_sheet
                 AND gs.scope = 'class_group'
                 AND gs.type = 'final'
                GROUP BY cg.id_class_group, cg.cod_class_group
                HAVING final_source_count <> 1
                ORDER BY cg.id_class_group
                """);
        assertNoRows(connection, "Every real subject occurrence must have exactly one consolidated grade sheet", """
                SELECT context.id_subject, context.id_course_occurrence,
                       COUNT(gs.id_grade_sheet) AS aggregate_count
                FROM (
                    SELECT DISTINCT id_subject, id_course_occurrence
                    FROM class_group
                ) context
                LEFT JOIN grade_sheet gs
                  ON gs.id_subject = context.id_subject
                 AND gs.id_course_occurrence = context.id_course_occurrence
                 AND gs.scope = 'subject_occurrence'
                GROUP BY context.id_subject, context.id_course_occurrence
                HAVING aggregate_count <> 1
                ORDER BY context.id_subject, context.id_course_occurrence
                """);
    }

    private static void assertComputerNetworksUsesIndependentCourseCalendars(Connection connection)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(DISTINCT isub.id_course) AS active_course_count,
                       COUNT(DISTINCT CONCAT(cop.starts_at, ':', cop.ends_at)) AS occurrence_period_count
                FROM integrate_subject isub
                JOIN class_group cg
                  ON cg.id_course = isub.id_course
                 AND cg.id_subject = isub.id_subject
                JOIN course_occurrence_period cop
                  ON cop.id_course_occurrence_period = cg.id_course_occurrence_period
                WHERE isub.id_subject = 43
                  AND isub.state = 'active'
                  AND isub.id_course IN (32, 34, 3008)
                """)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), "Computer Networks calendar fixture must be queryable");
                assertTrue(
                        resultSet.getLong("active_course_count") == 3,
                        "Computer Networks must be actively associated with Information Systems Master, Mathematics 1 and Mathematics 2"
                );
                assertTrue(
                        resultSet.getLong("occurrence_period_count") >= 2,
                        "Computer Networks course associations must use different occurrence periods"
                );
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*) AS total_groups,
                       SUM(cg.state = 'completed') AS completed_groups,
                       SUM(cg.state = 'active') AS active_groups,
                       SUM(cg.state = 'completed'
                           AND cg.id_course = 32
                           AND cg.id_course_occurrence = 321
                           AND cg.id_course_occurrence_period = 3201) AS completed_information_systems_groups,
                       SUM(cg.state = 'active'
                           AND cg.id_course = 34
                           AND cg.id_course_occurrence = 345
                           AND cg.id_course_occurrence_period = 34502) AS active_mathematics_one_groups,
                       SUM(cg.state = 'active'
                           AND cg.id_course = 3008
                           AND cg.id_course_occurrence = 30071
                           AND cg.id_course_occurrence_period = 300075) AS active_mathematics_two_groups
                FROM class_group cg
                WHERE cg.id_subject = 43
                """)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), "Computer Networks class group fixture must be queryable");
                assertTrue(resultSet.getLong("total_groups") == 11,
                        "Computer Networks must contain the ten original groups plus the Mathematics 2 group");
                assertTrue(resultSet.getLong("completed_groups") == 5,
                        "Computer Networks must contain exactly five completed class groups");
                assertTrue(resultSet.getLong("active_groups") == 6,
                        "Computer Networks must contain five active Mathematics 1 groups and one active Mathematics 2 group");
                assertTrue(resultSet.getLong("completed_information_systems_groups") == 5,
                        "Completed Computer Networks groups must belong to the completed Information Systems period");
                assertTrue(resultSet.getLong("active_mathematics_one_groups") == 5,
                        "Five active Computer Networks groups must belong to the Mathematics 1 period");
                assertTrue(resultSet.getLong("active_mathematics_two_groups") == 1,
                        "One active Computer Networks group must belong to the equivalent Mathematics 2 period");
            }
        }
    }

    private static void assertFullSeedServiceDomainRules(Connection connection) throws SQLException {
        assertNoRows(connection, "Full seed active organizations must have a current active administrator", """
                SELECT o.id_organization, o.name
                FROM organization o
                WHERE o.state = 'active'
                  AND NOT EXISTS (
                        SELECT 1
                        FROM manage_organization mo
                        JOIN administrator_profile ap ON ap.id_user = mo.id_admin_user
                        JOIN user_account u ON u.id_user = ap.id_user
                        WHERE mo.id_organization = o.id_organization
                          AND mo.state = 'active'
                          AND u.state = 'active'
                          AND (mo.start_date IS NULL OR mo.start_date <= CURRENT_DATE)
                          AND (mo.end_date IS NULL OR mo.end_date >= CURRENT_DATE)
                  )
                ORDER BY o.id_organization
                """);
        assertNoRows(connection, "Full seed active organic units must use active same-organization context", """
                SELECT ou.id_organic_unit, ou.name, o.state AS organization_state,
                       parent.id_organization AS parent_organization, parent.state AS parent_state
                FROM organic_unit ou
                JOIN organization o ON o.id_organization = ou.id_organization
                LEFT JOIN organic_unit parent ON parent.id_organic_unit = ou.parent_organic_unit_id
                WHERE ou.state = 'active'
                  AND (o.state <> 'active'
                       OR (parent.id_organic_unit IS NOT NULL
                           AND (parent.id_organization <> ou.id_organization OR parent.state <> 'active')))
                ORDER BY ou.id_organic_unit
                """);
        assertNoRows(connection, "Full seed active role assignments must use active users", """
                SELECT 'manage_organization' AS assignment_type, mo.id_admin_user AS id_user, mo.id_organization AS context_id,
                       u.state AS user_state, o.state AS context_state
                FROM manage_organization mo
                JOIN user_account u ON u.id_user = mo.id_admin_user
                JOIN organization o ON o.id_organization = mo.id_organization
                WHERE mo.state = 'active'
                  AND u.state <> 'active'
                UNION ALL
                SELECT 'coordinate_subject' AS assignment_type, cs.id_coordinator_user AS id_user, cs.id_subject AS context_id,
                       u.state AS user_state, s.state AS context_state
                FROM coordinate_subject cs
                JOIN user_account u ON u.id_user = cs.id_coordinator_user
                JOIN subject s ON s.id_subject = cs.id_subject
                WHERE cs.state = 'active'
                  AND u.state <> 'active'
                UNION ALL
                SELECT 'teach_class_group' AS assignment_type, tcg.id_teacher_user AS id_user, tcg.id_class_group AS context_id,
                       u.state AS user_state, cg.state AS context_state
                FROM teach_class_group tcg
                JOIN user_account u ON u.id_user = tcg.id_teacher_user
                JOIN class_group cg ON cg.id_class_group = tcg.id_class_group
                WHERE tcg.state = 'active'
                  AND u.state <> 'active'
                ORDER BY assignment_type, id_user, context_id
                """);
        assertNoRows(connection, "Full seed active courses must use active organization context", """
                SELECT c.id_course, c.name, c.state, o.state AS organization_state, ou.state AS organic_unit_state
                FROM course c
                JOIN organization o ON o.id_organization = c.id_organization
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = c.id_organic_unit
                WHERE c.state = 'active'
                  AND (o.state <> 'active'
                       OR (ou.id_organic_unit IS NOT NULL
                           AND (ou.state <> 'active' OR ou.id_organization <> c.id_organization)))
                ORDER BY c.id_course
                """);
        assertNoRows(connection, "Full seed active subjects must use active organizations", """
                SELECT s.id_subject, s.name, s.state, o.state AS organization_state
                FROM subject s
                JOIN organization o ON o.id_organization = s.id_organization
                WHERE s.state = 'active'
                  AND o.state <> 'active'
                ORDER BY s.id_subject
                """);
        assertNoRows(connection, "Full seed course-subject associations must keep matching organizations", """
                SELECT isub.id_course, isub.id_subject, c.state AS course_state, s.state AS subject_state,
                       c.id_organization AS course_organization, s.id_organization AS subject_organization
                FROM integrate_subject isub
                JOIN course c ON c.id_course = isub.id_course
                JOIN subject s ON s.id_subject = isub.id_subject
                WHERE c.id_organization <> s.id_organization
                ORDER BY isub.id_course, isub.id_subject
                """);
        assertNoRows(connection, "Full seed physical rooms must use a valid active organization context", """
                SELECT pr.cod_physical_room, pr.state, o.state AS organization_state,
                       ou.state AS organic_unit_state, ou.id_organization AS unit_organization
                FROM physical_room pr
                JOIN organization o ON o.id_organization = pr.id_organization
                LEFT JOIN organic_unit ou ON ou.id_organic_unit = pr.id_organic_unit
                WHERE pr.state = 'active'
                  AND (o.state <> 'active'
                       OR (ou.id_organic_unit IS NOT NULL
                           AND (ou.state <> 'active' OR ou.id_organization <> pr.id_organization)))
                ORDER BY pr.cod_physical_room
                """);
        assertNoRows(connection, "Full seed active course enrollments must be service-eligible", """
                SELECT ec.id_student_user, ec.id_course, ec.state, u.state AS user_state, c.state AS course_state,
                       ec.start_date, ec.end_date
                FROM enroll_course ec
                JOIN user_account u ON u.id_user = ec.id_student_user
                JOIN student_profile sp ON sp.id_user = ec.id_student_user
                JOIN course c ON c.id_course = ec.id_course
                WHERE ec.state = 'active'
                  AND (u.state <> 'active'
                       OR c.state <> 'active'
                       OR (ec.end_date IS NOT NULL AND ec.start_date IS NOT NULL AND ec.end_date < ec.start_date))
                ORDER BY ec.id_student_user, ec.id_course
                """);
        assertNoRows(connection, "Full seed active class group enrollments must be covered by an active course occurrence enrollment", """
                SELECT ecg.id_student_user, ecg.id_class_group, ecg.state, ecg.start_date, ecg.end_date
                FROM enroll_class_group ecg
                JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
                WHERE ecg.state = 'active'
                  AND NOT EXISTS (
                        SELECT 1
                        FROM enroll_course ec
                        WHERE ec.id_student_user = ecg.id_student_user
                          AND ec.id_course = cg.id_course
                          AND ec.id_course_occurrence = cg.id_course_occurrence
                          AND ec.state = 'active'
                          AND (ec.start_date IS NULL OR ecg.start_date IS NULL OR ec.start_date <= ecg.start_date)
                          AND (ecg.end_date IS NULL OR ec.end_date IS NULL OR ec.end_date >= ecg.end_date)
                  )
                ORDER BY ecg.id_student_user, ecg.id_class_group
                """);
        assertNoRows(connection, "Full seed active assessment enrollments must be covered by the assessment context", """
                SELECT ea.id_student_user, ea.id_assessment, ea.state
                FROM enroll_assessment ea
                JOIN assessment a ON a.id_assessment = ea.id_assessment
                WHERE ea.state = 'active'
                  AND NOT EXISTS (
                        SELECT 1
                        FROM assessment_class_group acg
                        JOIN class_group cg ON cg.id_class_group = acg.id_class_group
                        JOIN enroll_class_group ecg
                          ON ecg.id_class_group = cg.id_class_group
                         AND ecg.id_student_user = ea.id_student_user
                         AND ecg.state IN ('active', 'completed')
                        WHERE acg.id_assessment = ea.id_assessment
                  )
                  AND NOT EXISTS (
                        SELECT 1
                        FROM content_block cb
                        JOIN class_group cg ON cg.id_class_group = cb.id_class_group
                        JOIN enroll_class_group ecg
                          ON ecg.id_class_group = cg.id_class_group
                         AND ecg.id_student_user = ea.id_student_user
                         AND ecg.state IN ('active', 'completed')
                        WHERE cb.id_content_block = a.id_content_block
                  )
                ORDER BY ea.id_student_user, ea.id_assessment
                """);
        assertNoRows(connection, "Full seed pending assessment enrollments must be eligible for the assessment context", """
                SELECT ea.id_student_user, ea.id_assessment, ea.state
                FROM enroll_assessment ea
                JOIN assessment a ON a.id_assessment = ea.id_assessment
                WHERE ea.state = 'pending'
                  AND NOT EXISTS (
                        SELECT 1
                        FROM content_block cb
                        JOIN class_group cg ON cg.id_class_group = cb.id_class_group
                        JOIN enroll_class_group ecg
                          ON ecg.id_class_group = cg.id_class_group
                         AND ecg.id_student_user = ea.id_student_user
                         AND ecg.state = 'active'
                        WHERE cb.id_content_block = a.id_content_block
                          AND cg.state = 'active'
                          AND ecg.start_date <= DATE(a.available_from)
                          AND ecg.end_date >= DATE(a.available_until)
                  )
                  AND NOT EXISTS (
                        SELECT 1
                        FROM assessment_class_group acg
                        JOIN class_group cg ON cg.id_class_group = acg.id_class_group
                        JOIN enroll_class_group ecg
                          ON ecg.id_class_group = cg.id_class_group
                         AND ecg.id_student_user = ea.id_student_user
                         AND ecg.state = 'active'
                        WHERE acg.id_assessment = ea.id_assessment
                          AND cg.state = 'active'
                          AND ecg.start_date <= DATE(a.available_from)
                          AND ecg.end_date >= DATE(a.available_until)
                  )
                ORDER BY ea.id_student_user, ea.id_assessment
                """);
        assertNoRows(connection, "Full seed question scores must not exceed assessment maximums", """
                SELECT a.id_assessment, a.title, a.max_grade, SUM(q.score) AS question_total
                FROM assessment a
                JOIN question q ON q.id_assessment = a.id_assessment
                GROUP BY a.id_assessment, a.title, a.max_grade
                HAVING question_total > a.max_grade
                ORDER BY a.id_assessment
                """);
        assertNoRows(connection, "Full seed assessments must match AssessmentService structural requirements", """
                SELECT a.id_assessment, a.title, a.type, a.mode, a.correction_mode, a.id_subject, a.id_content_block,
                       a.max_grade, a.passing_grade
                FROM assessment a
                WHERE a.max_grade <= 0
                   OR a.passing_grade > a.max_grade
                   OR (a.mode = 'onsite' AND a.correction_mode <> 'manual')
                   OR (a.type IN ('form', 'test') AND a.id_content_block IS NULL)
                   OR (a.type = 'exam' AND a.id_subject IS NULL AND a.id_content_block IS NULL)
                ORDER BY a.id_assessment
                """);
        assertNoRows(connection, "Functions Applied Checkpoint question scores must reach its assessment maximum", """
                SELECT a.id_assessment, a.title, a.max_grade, COALESCE(SUM(q.score), 0) AS question_total
                FROM assessment a
                LEFT JOIN question q ON q.id_assessment = a.id_assessment
                WHERE a.id_assessment = 6500
                GROUP BY a.id_assessment, a.title, a.max_grade
                HAVING ABS(question_total - a.max_grade) > 0.001
                """);
        assertNoRows(connection, "Functions Applied Checkpoint must keep its rich correction fixture", """
                SELECT a.id_assessment, a.title,
                       COUNT(DISTINCT q.id_question) AS question_count,
                       COUNT(DISTINCT CASE WHEN ea.state = 'active' THEN ea.id_student_user END) AS active_enrollments,
                       COUNT(DISTINCT CASE WHEN ea.state = 'pending' THEN ea.id_student_user END) AS pending_enrollments,
                       COUNT(DISTINCT CASE WHEN at.state = 'submitted' THEN at.id_attempt END) AS pending_corrections,
                       COUNT(DISTINCT CASE WHEN at.state = 'corrected' THEN at.id_attempt END) AS corrected_attempts
                FROM assessment a
                LEFT JOIN question q ON q.id_assessment = a.id_assessment
                LEFT JOIN enroll_assessment ea ON ea.id_assessment = a.id_assessment
                LEFT JOIN attempt at ON at.id_assessment = a.id_assessment
                WHERE a.id_assessment = 6500
                GROUP BY a.id_assessment, a.title
                HAVING question_count <> 10
                    OR active_enrollments <> 5
                    OR pending_enrollments <> 5
                    OR pending_corrections <> 3
                    OR corrected_attempts <> 2
                    OR MAX(a.state) <> 'active'
                    OR MAX(a.enrollment_mode) <> 'manual'
                """);
        assertNoRows(connection, "Full seed active or scheduled assessments cannot use inactive contexts", """
                SELECT a.id_assessment, a.title, a.state, s.state AS subject_state,
                       cg.state AS class_group_state
                FROM assessment a
                LEFT JOIN subject s ON s.id_subject = a.id_subject
                LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
                LEFT JOIN class_group cg ON cg.id_class_group = cb.id_class_group
                WHERE a.state IN ('scheduled', 'active')
                  AND ((s.id_subject IS NOT NULL AND s.state <> 'active')
                       OR (cg.id_class_group IS NOT NULL AND cg.state = 'draft'))
                ORDER BY a.id_assessment
                """);
        assertNoRows(connection, "Full seed active or scheduled onsite assessments must fit room capacity", """
                SELECT a.id_assessment, a.title, a.cod_physical_room, pr.capacity,
                       COUNT(ecg.id_student_user) AS active_enrollments
                FROM assessment a
                JOIN physical_room pr ON pr.cod_physical_room = a.cod_physical_room
                JOIN (
                    SELECT a2.id_assessment, cb.id_class_group
                    FROM assessment a2
                    JOIN content_block cb ON cb.id_content_block = a2.id_content_block
                    UNION
                    SELECT id_assessment, id_class_group
                    FROM assessment_class_group
                ) ctx ON ctx.id_assessment = a.id_assessment
                LEFT JOIN enroll_class_group ecg
                  ON ecg.id_class_group = ctx.id_class_group
                 AND ecg.state = 'active'
                 AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                 AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                LEFT JOIN user_account u
                  ON u.id_user = ecg.id_student_user
                 AND u.state = 'active'
                WHERE a.mode = 'onsite'
                  AND a.state IN ('scheduled', 'active')
                GROUP BY a.id_assessment, a.title, a.cod_physical_room, pr.capacity
                HAVING active_enrollments > pr.capacity
                ORDER BY a.id_assessment
                """);
        assertNoRows(connection, "Full seed selected response options must match their response question", """
                SELECT ro.id_response, ro.id_option, r.id_question AS response_question, qo.id_question AS option_question
                FROM response_option ro
                JOIN response r ON r.id_response = ro.id_response
                JOIN question_option qo ON qo.id_option = ro.id_option
                WHERE qo.id_question <> r.id_question
                ORDER BY ro.id_response, ro.id_option
                """);
        assertNoRows(connection, "Full seed lesson access details must match LessonService type rules", """
                SELECT id_lesson, title, type, cod_physical_room, provider, access_url
                FROM lesson
                WHERE (type = 'online' AND cod_physical_room IS NOT NULL)
                   OR (type = 'onsite' AND (provider IS NOT NULL OR access_url IS NOT NULL))
                   OR (type = 'hybrid' AND cod_physical_room IS NULL)
                ORDER BY id_lesson
                """);
        assertNoRows(connection, "Full seed active or scheduled room lessons must fit room capacity", """
                SELECT l.id_lesson, l.title, l.cod_physical_room, pr.capacity,
                       COUNT(ecg.id_student_user) AS active_enrollments
                FROM lesson l
                JOIN physical_room pr ON pr.cod_physical_room = l.cod_physical_room
                LEFT JOIN enroll_class_group ecg
                  ON ecg.id_class_group = l.id_class_group
                 AND ecg.state = 'active'
                 AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
                 AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
                LEFT JOIN user_account u
                  ON u.id_user = ecg.id_student_user
                 AND u.state = 'active'
                WHERE l.type IN ('onsite', 'hybrid')
                  AND l.state IN ('scheduled', 'active')
                GROUP BY l.id_lesson, l.title, l.cod_physical_room, pr.capacity
                HAVING active_enrollments > pr.capacity
                ORDER BY l.id_lesson
                """);
        assertNoRows(connection, "Full seed attendance records must match attendance service rules", """
                SELECT ar.id_attendance_record, ar.id_lesson, ar.id_user_student, ar.status,
                       ar.check_in, ar.check_out, ar.state
                FROM attendance_record ar
                JOIN lesson l ON l.id_lesson = ar.id_lesson
                WHERE (ar.check_out IS NOT NULL AND ar.check_in IS NULL)
                   OR (ar.status = 'absent' AND (ar.check_in IS NOT NULL OR ar.check_out IS NOT NULL))
                   OR (ar.status = 'justified' AND NOT EXISTS (
                        SELECT 1
                        FROM absence_justification aj
                        WHERE aj.id_attendance_record = ar.id_attendance_record
                          AND aj.state = 'approved'
                   ))
                   OR NOT EXISTS (
                        SELECT 1
                        FROM enroll_class_group ecg
                        WHERE ecg.id_student_user = ar.id_user_student
                          AND ecg.id_class_group = l.id_class_group
                          AND ecg.state IN ('active', 'completed')
                          AND (ecg.start_date IS NULL OR l.starts_at IS NULL OR ecg.start_date <= CAST(l.starts_at AS DATE))
                          AND (ecg.end_date IS NULL OR l.ends_at IS NULL OR ecg.end_date >= CAST(l.ends_at AS DATE))
                   )
                ORDER BY ar.id_attendance_record
                """);
        assertNoRows(connection, "Full seed absence justifications must be coherent with attendance records", """
                SELECT aj.id_absence_justification, aj.id_attendance_record, aj.id_user_student_submitter,
                       aj.id_user_processor, aj.state, ar.id_user_student, ar.status, ar.state AS attendance_state
                FROM absence_justification aj
                JOIN attendance_record ar ON ar.id_attendance_record = aj.id_attendance_record
                WHERE aj.id_user_student_submitter <> ar.id_user_student
                   OR (aj.state IN ('approved', 'rejected') AND aj.id_user_processor IS NULL)
                   OR (aj.state = 'approved' AND (ar.status <> 'justified' OR ar.state <> 'corrected'))
                   OR (aj.state = 'rejected' AND ar.status = 'justified')
                ORDER BY aj.id_absence_justification
                """);
        assertNoRows(connection, "Full seed schedule events must match referenced lesson periods", """
                SELECT se.id_schedule_event, se.title, se.type, se.starts_at, se.ends_at,
                       l.id_lesson, l.starts_at AS lesson_start, l.ends_at AS lesson_end
                FROM schedule_event se
                LEFT JOIN lesson l ON l.id_lesson = se.id_lesson
                WHERE se.type = 'lesson'
                  AND (se.id_lesson IS NULL
                       OR se.id_assessment IS NOT NULL
                       OR l.id_lesson IS NULL
                       OR se.starts_at <> l.starts_at
                       OR se.ends_at <> l.ends_at)
                ORDER BY se.id_schedule_event
                """);
        assertNoRows(connection, "Full seed schedule events must fit referenced assessment availability", """
                SELECT se.id_schedule_event, se.title, se.type, se.starts_at, se.ends_at,
                       a.id_assessment, a.available_from, a.available_until
                FROM schedule_event se
                LEFT JOIN assessment a ON a.id_assessment = se.id_assessment
                WHERE se.type = 'assessment'
                  AND (se.id_assessment IS NULL
                       OR se.id_lesson IS NOT NULL
                       OR a.id_assessment IS NULL
                       OR (a.available_from IS NOT NULL AND se.starts_at < a.available_from)
                       OR (a.available_until IS NOT NULL AND se.ends_at > a.available_until))
                ORDER BY se.id_schedule_event
                """);
        assertNoRows(connection, "Full seed non-linked schedule events must have class group context", """
                SELECT se.id_schedule_event, se.title, se.type
                FROM schedule_event se
                WHERE se.id_lesson IS NULL
                  AND se.id_assessment IS NULL
                  AND se.type NOT IN ('lesson', 'assessment')
                  AND NOT EXISTS (
                        SELECT 1
                        FROM associate_schedule_event_class_group aseg
                        WHERE aseg.id_schedule_event = se.id_schedule_event
                )
                ORDER BY se.id_schedule_event
                """);
        assertNoRows(connection, "Full seed lesson schedule events must expose exactly the lesson class group", """
                SELECT se.id_schedule_event, se.title, l.id_class_group AS lesson_class_group,
                       aseg.id_class_group AS associated_class_group
                FROM schedule_event se
                JOIN lesson l ON l.id_lesson = se.id_lesson
                LEFT JOIN associate_schedule_event_class_group aseg
                  ON aseg.id_schedule_event = se.id_schedule_event
                WHERE se.type = 'lesson'
                  AND (aseg.id_class_group IS NULL OR aseg.id_class_group <> l.id_class_group)
                ORDER BY se.id_schedule_event
                """);
        assertNoRows(connection, "Full seed assessment schedule events must match assessment class group context", """
                SELECT se.id_schedule_event, se.title, se.id_assessment, aseg.id_class_group
                FROM schedule_event se
                JOIN assessment a ON a.id_assessment = se.id_assessment
                JOIN associate_schedule_event_class_group aseg
                  ON aseg.id_schedule_event = se.id_schedule_event
                WHERE se.type = 'assessment'
                  AND NOT EXISTS (
                        SELECT 1
                        FROM (
                            SELECT a2.id_assessment, cb.id_class_group
                            FROM assessment a2
                            JOIN content_block cb ON cb.id_content_block = a2.id_content_block
                            UNION
                            SELECT id_assessment, id_class_group
                            FROM assessment_class_group
                        ) ctx
                        WHERE ctx.id_assessment = se.id_assessment
                          AND ctx.id_class_group = aseg.id_class_group
                  )
                  AND NOT (
                        NOT EXISTS (
                            SELECT 1
                            FROM content_block cb
                            WHERE cb.id_content_block = a.id_content_block
                        )
                        AND NOT EXISTS (
                            SELECT 1
                            FROM assessment_class_group acg
                            WHERE acg.id_assessment = a.id_assessment
                        )
                        AND a.id_subject IS NOT NULL
                        AND EXISTS (
                            SELECT 1
                            FROM class_group cg
                            WHERE cg.id_class_group = aseg.id_class_group
                              AND cg.id_subject = a.id_subject
                              AND cg.state = 'active'
                        )
                  )
                ORDER BY se.id_schedule_event, aseg.id_class_group
                """);
        assertNoRows(connection, "Full seed schedule event recipients must be generated from associated class groups", """
                SELECT rse.id_schedule_event, rse.id_user
                FROM receive_schedule_event rse
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM associate_schedule_event_class_group aseg
                    JOIN schedule_event se ON se.id_schedule_event = aseg.id_schedule_event
                    JOIN enroll_class_group ecg ON ecg.id_class_group = aseg.id_class_group
                    JOIN user_account u ON u.id_user = ecg.id_student_user
                    WHERE aseg.id_schedule_event = rse.id_schedule_event
                      AND ecg.id_student_user = rse.id_user
                      AND ecg.state IN ('active', 'completed')
                      AND u.state = 'active'
                      AND ecg.start_date <= DATE(se.starts_at)
                      AND ecg.end_date >= DATE(se.starts_at)
                    UNION ALL
                    SELECT 1
                    FROM associate_schedule_event_class_group aseg
                    JOIN schedule_event se ON se.id_schedule_event = aseg.id_schedule_event
                    JOIN teach_class_group tcg ON tcg.id_class_group = aseg.id_class_group
                    JOIN user_account u ON u.id_user = tcg.id_teacher_user
                    WHERE aseg.id_schedule_event = rse.id_schedule_event
                      AND tcg.id_teacher_user = rse.id_user
                      AND tcg.state IN ('active', 'inactive')
                      AND u.state = 'active'
                      AND (tcg.start_date IS NULL OR tcg.start_date <= DATE(se.starts_at))
                      AND (tcg.end_date IS NULL OR tcg.end_date >= DATE(se.starts_at))
                    UNION ALL
                    SELECT 1
                    FROM associate_schedule_event_class_group aseg
                    JOIN class_group cg ON cg.id_class_group = aseg.id_class_group
                    JOIN coordinate_subject cs ON cs.id_subject = cg.id_subject
                    JOIN user_account u ON u.id_user = cs.id_coordinator_user
                    WHERE aseg.id_schedule_event = rse.id_schedule_event
                      AND cs.id_coordinator_user = rse.id_user
                      AND cs.state = 'active'
                      AND u.state = 'active'
                )
                ORDER BY rse.id_schedule_event, rse.id_user
                """);
        assertNoRows(connection, "Full seed schedule event generated recipients must not be missing", """
                SELECT expected.id_schedule_event, expected.id_user
                FROM (
                    SELECT DISTINCT aseg.id_schedule_event, ecg.id_student_user AS id_user
                    FROM associate_schedule_event_class_group aseg
                    JOIN schedule_event se ON se.id_schedule_event = aseg.id_schedule_event
                    JOIN enroll_class_group ecg ON ecg.id_class_group = aseg.id_class_group
                    JOIN user_account u ON u.id_user = ecg.id_student_user
                    WHERE ecg.state IN ('active', 'completed')
                      AND u.state = 'active'
                      AND (ecg.start_date IS NULL OR ecg.start_date <= DATE(se.starts_at))
                      AND (ecg.end_date IS NULL OR ecg.end_date >= DATE(se.starts_at))
                    UNION
                    SELECT DISTINCT aseg.id_schedule_event, tcg.id_teacher_user AS id_user
                    FROM associate_schedule_event_class_group aseg
                    JOIN schedule_event se ON se.id_schedule_event = aseg.id_schedule_event
                    JOIN teach_class_group tcg ON tcg.id_class_group = aseg.id_class_group
                    JOIN user_account u ON u.id_user = tcg.id_teacher_user
                    WHERE tcg.state IN ('active', 'inactive')
                      AND u.state = 'active'
                      AND (tcg.start_date IS NULL OR tcg.start_date <= DATE(se.starts_at))
                      AND (tcg.end_date IS NULL OR tcg.end_date >= DATE(se.starts_at))
                    UNION
                    SELECT DISTINCT aseg.id_schedule_event, cs.id_coordinator_user AS id_user
                    FROM associate_schedule_event_class_group aseg
                    JOIN class_group cg ON cg.id_class_group = aseg.id_class_group
                    JOIN coordinate_subject cs ON cs.id_subject = cg.id_subject
                    JOIN user_account u ON u.id_user = cs.id_coordinator_user
                    WHERE cs.state = 'active'
                      AND u.state = 'active'
                ) expected
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM receive_schedule_event rse
                    WHERE rse.id_schedule_event = expected.id_schedule_event
                      AND rse.id_user = expected.id_user
                )
                ORDER BY expected.id_schedule_event, expected.id_user
                """);
        assertNoRows(connection, "Full seed active channels must have participants and public channels must be active", """
                SELECT c.id_channel, c.title, c.state, c.visibility
                FROM channel c
                WHERE (c.state = 'active' AND NOT EXISTS (
                        SELECT 1
                        FROM participate_channel pc
                        JOIN user_account u ON u.id_user = pc.id_user
                        WHERE pc.id_channel = c.id_channel
                          AND pc.state = 'active'
                          AND u.state = 'active'
                  ))
                   OR (c.visibility = 'public' AND c.state <> 'active')
                ORDER BY c.id_channel
                """);
        assertNoRows(connection, "Full seed channel context associations must be structurally coherent", """
                SELECT 'content_block' AS context_type, accb.id_channel, accb.id_content_block AS context_id
                FROM associate_channel_content_block accb
                JOIN content_block cb ON cb.id_content_block = accb.id_content_block
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM associate_channel_class_group accg
                    WHERE accg.id_channel = accb.id_channel
                      AND accg.id_class_group = cb.id_class_group
                )
                UNION ALL
                SELECT 'assessment' AS context_type, aca.id_channel, aca.id_assessment AS context_id
                FROM associate_channel_assessment aca
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM associate_channel_class_group accg
                    WHERE accg.id_channel = aca.id_channel
                )
                UNION ALL
                SELECT 'assessment_group' AS context_type, aca.id_channel, aca.id_assessment AS context_id
                FROM associate_channel_assessment aca
                JOIN (
                    SELECT a.id_assessment, cb.id_class_group
                    FROM assessment a
                    JOIN content_block cb ON cb.id_content_block = a.id_content_block
                    UNION
                    SELECT id_assessment, id_class_group
                    FROM assessment_class_group
                ) ctx ON ctx.id_assessment = aca.id_assessment
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM associate_channel_class_group accg
                    WHERE accg.id_channel = aca.id_channel
                      AND accg.id_class_group = ctx.id_class_group
                )
                ORDER BY context_type, id_channel, context_id
                """);
        assertNoRows(connection, "Full seed human messages must have a sender and system messages must use system types", """
                SELECT id_message, title, type, id_user_sender
                FROM message
                WHERE (id_user_sender IS NULL AND type NOT IN ('system', 'notification', 'reminder', 'alert', 'warning'))
                   OR (id_user_sender IS NOT NULL AND type = 'system')
                ORDER BY id_message
                """);
        assertNoRows(connection, "Full seed message senders and recipients must actively participate in the channel", """
                SELECT 'sender' AS participant_type, m.id_message, m.id_channel, m.id_user_sender AS id_user
                FROM message m
                WHERE m.id_user_sender IS NOT NULL
                  AND NOT EXISTS (
                        SELECT 1
                        FROM participate_channel pc
                        JOIN user_account u ON u.id_user = pc.id_user
                        WHERE pc.id_channel = m.id_channel
                          AND pc.id_user = m.id_user_sender
                          AND pc.state = 'active'
                          AND u.state = 'active'
                  )
                UNION ALL
                SELECT 'recipient' AS participant_type, m.id_message, m.id_channel, rm.id_user
                FROM receive_message rm
                JOIN message m ON m.id_message = rm.id_message
                WHERE NOT EXISTS (
                        SELECT 1
                        FROM participate_channel pc
                        JOIN user_account u ON u.id_user = pc.id_user
                        WHERE pc.id_channel = m.id_channel
                          AND pc.id_user = rm.id_user
                          AND pc.state = 'active'
                          AND u.state = 'active'
                  )
                ORDER BY participant_type, id_message, id_user
                """);
        assertNoRows(connection, "Full seed schedule-origin messages must respect event period and recipients", """
                SELECT m.id_message, m.title, m.id_schedule_event_origin, m.scheduled_at, m.sent_at, se.ends_at
                FROM message m
                JOIN schedule_event se ON se.id_schedule_event = m.id_schedule_event_origin
                WHERE COALESCE(m.scheduled_at, m.sent_at, m.created_at) > se.ends_at
                   OR EXISTS (
                        SELECT 1
                        FROM associate_schedule_event_class_group aseg
                        WHERE aseg.id_schedule_event = se.id_schedule_event
                          AND NOT EXISTS (
                                SELECT 1
                                FROM associate_channel_class_group accg
                                WHERE accg.id_channel = m.id_channel
                                  AND accg.id_class_group = aseg.id_class_group
                          )
                   )
                   OR EXISTS (
                        SELECT 1
                        FROM receive_message rm
                        WHERE rm.id_message = m.id_message
                          AND NOT EXISTS (
                                SELECT 1
                                FROM receive_schedule_event rse
                                WHERE rse.id_schedule_event = se.id_schedule_event
                                  AND rse.id_user = rm.id_user
                          )
                   )
                ORDER BY m.id_message
                """);
        assertNoRows(connection, "Full seed message replies cannot be sent by the same user as their parent", """
                SELECT child.id_message, child.title, child.id_user_sender, child.id_parent_message
                FROM message child
                JOIN message parent ON parent.id_message = child.id_parent_message
                WHERE child.id_user_sender IS NOT NULL
                  AND parent.id_user_sender IS NOT NULL
                  AND child.id_user_sender = parent.id_user_sender
                ORDER BY child.id_message
                """);
        assertNoRows(connection, "Full seed sent messages must have delivery timestamps and scheduled messages must wait", """
                SELECT id_message, title, state, scheduled_at, sent_at
                FROM message
                WHERE (state IN ('sent', 'active', 'edited') AND sent_at IS NULL)
                   OR (state = 'scheduled' AND sent_at IS NOT NULL)
                   OR (state = 'scheduled' AND scheduled_at IS NULL)
                ORDER BY id_message
                """);
        assertFullSeedIssuedCertificatesAreReproducible(connection);
    }

    private static List<String> fullSeedGradeSheetStateMismatches(Connection connection) throws SQLException {
        List<String> mismatches = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_grade_sheet, title, state, publication_explanation
                FROM grade_sheet
                ORDER BY id_grade_sheet
                """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                long gradeSheetId = resultSet.getLong("id_grade_sheet");
                String databaseState = resultSet.getString("state");
                if ("inactive".equals(databaseState) || "closed".equals(databaseState)) {
                    continue;
                }
                boolean complete = gradeSheetComplete(connection, gradeSheetId);
                boolean completedPeriod = gradeSheetBelongsToCompletedPeriod(connection, gradeSheetId);
                String expectedState = complete || completedPeriod ? "published" : "draft";
                if (!expectedState.equals(databaseState)) {
                    mismatches.add(gradeSheetId + " " + resultSet.getString("title")
                            + " expected " + expectedState + " but was " + databaseState);
                    continue;
                }
                if (!complete && completedPeriod
                        && (resultSet.getString("publication_explanation") == null
                        || resultSet.getString("publication_explanation").isBlank())) {
                    mismatches.add(gradeSheetId + " " + resultSet.getString("title")
                            + " is published after a completed period without a pending-grade explanation");
                }
            }
        }
        return mismatches;
    }

    private static boolean gradeSheetBelongsToCompletedPeriod(Connection connection, long gradeSheetId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                FROM grade_sheet gs
                WHERE gs.id_grade_sheet = ?
                  AND (
                        (
                            EXISTS (
                                SELECT 1
                                FROM associate_grade_sheet_class_group agscg
                                WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                            )
                            AND NOT EXISTS (
                                SELECT 1
                                FROM associate_grade_sheet_class_group agscg
                                JOIN class_group cg ON cg.id_class_group = agscg.id_class_group
                                JOIN course_occurrence_period cop
                                  ON cop.id_course_occurrence_period = cg.id_course_occurrence_period
                                WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                                  AND (cop.state = 'cancelled' OR cop.ends_at >= CURRENT_DATE)
                            )
                        )
                        OR (
                            gs.scope = 'subject_occurrence'
                            AND NOT EXISTS (
                                SELECT 1
                                FROM class_group cg
                                JOIN course_occurrence_period cop
                                  ON cop.id_course_occurrence_period = cg.id_course_occurrence_period
                                WHERE cg.id_subject = gs.id_subject
                                  AND cg.id_course_occurrence = gs.id_course_occurrence
                                  AND (cop.state = 'cancelled' OR cop.ends_at >= CURRENT_DATE)
                            )
                        )
                    )
                LIMIT 1
                """)) {
            statement.setLong(1, gradeSheetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static void assertFullSeedValueCoverage(Connection connection) throws SQLException {
        assertColumnCovers(connection, "user_account", "state", "active", "inactive", "blocked");
        assertColumnCovers(connection, "user_session", "state", "active", "expired", "closed");
        assertColumnCovers(connection, "deletion_request", "state",
                "submitted", "under_review", "approved", "rejected", "completed");
        assertColumnCovers(connection, "organization", "type",
                "educational_institution", "training_company", "company", "other");
        assertColumnCovers(connection, "organic_unit", "type",
                "school", "faculty", "department", "center", "office", "service", "section", "direction", "other");
        assertColumnCovers(connection, "course", "type",
                "degree", "master", "short_course", "professional_training", "other");
        assertColumnCovers(connection, "integrate_subject", "term",
                "annual", "semester_1", "semester_2", "trimester_1", "trimester_2", "trimester_3");
        assertColumnCovers(connection, "class_group", "modality", "onsite", "online", "hybrid");
        assertColumnCovers(connection, "class_group", "state", "scheduled", "active", "completed");
        assertColumnCovers(connection, "class_group", "shift", "morning", "afternoon", "evening", "mixed");
        assertColumnCovers(connection, "content_block", "state", "active", "inactive");
        assertColumnCovers(connection, "content_item", "format",
                "text", "image", "video", "audio", "pdf", "archive", "url", "scorm", "xapi",
                "presentation", "embed", "other");
        assertColumnCovers(connection, "content_item", "state", "draft", "active", "inactive");
        assertColumnCovers(connection, "content_file", "processing_state", "processing", "ready", "failed");
        assertColumnCovers(connection, "physical_room", "state", "active", "inactive", "unavailable");
        assertColumnCovers(connection, "lesson", "type", "online", "onsite", "hybrid");
        assertColumnCovers(connection, "lesson", "state", "draft", "scheduled", "active", "completed", "cancelled");
        assertColumnCovers(connection, "assessment", "type", "form", "test", "exam");
        assertColumnCovers(connection, "assessment", "mode", "online", "onsite");
        assertColumnCovers(connection, "assessment", "correction_mode", "automatic", "mixed", "manual");
        assertColumnCovers(connection, "assessment", "enrollment_mode", "manual", "auto_approve");
        assertColumnCovers(connection, "assessment", "state", "draft", "scheduled", "active", "completed");
        assertColumnCovers(connection, "question", "type",
                "single_choice", "multiple_choice", "short_text", "paragraph", "file_upload", "rating");
        assertColumnCovers(connection, "enroll_course", "state", "active", "inactive", "completed", "withdrawn");
        assertColumnCovers(connection, "enroll_class_group", "state",
                "pending", "active", "inactive", "rejected", "completed", "withdrawn");
        assertColumnCovers(connection, "enroll_assessment", "state",
                "pending", "active", "inactive", "rejected", "completed", "withdrawn");
        assertColumnCovers(connection, "attempt", "state",
                "in_progress", "submitted", "corrected", "expired", "cancelled");
        assertColumnCovers(connection, "schedule_event", "type",
                "lesson", "assessment", "reminder", "meeting", "other");
        assertColumnCovers(connection, "schedule_event", "state",
                "draft", "active", "inactive", "cancelled", "completed");
        assertColumnCovers(connection, "attendance_record", "status",
                "present", "absent", "justified", "late", "partial");
        assertColumnCovers(connection, "attendance_record", "source", "manual", "automatic", "other");
        assertColumnCovers(connection, "attendance_record", "state", "active", "corrected", "cancelled");
        assertColumnCovers(connection, "absence_justification", "state",
                "submitted", "under_review", "approved", "rejected", "cancelled");
        assertColumnCovers(connection, "grade_sheet", "type",
                "final", "continuous_assessment", "exam", "partial", "other");
        assertColumnCovers(connection, "grade_sheet", "state", "draft", "published", "closed", "inactive");
        assertColumnCovers(connection, "grade_record", "result", "approved", "failed", "pending", "absent");
        assertColumnCovers(connection, "grade_record", "state", "draft", "published", "corrected", "inactive");
        assertColumnCovers(connection, "certificate", "type", "completion", "attendance", "qualification", "other");
        assertColumnCovers(connection, "certificate", "state", "draft", "issued");
        assertColumnCovers(connection, "channel", "type",
                "message", "forum", "comments", "announcement", "system", "organization",
                "class_group", "content_block", "assessment", "other");
        assertColumnCovers(connection, "channel", "visibility",
                "participants", "public", "private", "organization", "context", "system");
        assertColumnCovers(connection, "channel", "state", "active", "inactive");
        assertColumnCovers(connection, "participate_channel", "role",
                "owner", "moderator", "member", "viewer", "administrator", "coordinator", "teacher", "student");
        assertColumnCovers(connection, "participate_channel", "state", "active", "inactive", "blocked");
        assertColumnCovers(connection, "message", "type",
                "text", "comment", "announcement", "warning", "alert", "reminder",
                "notification", "system", "attachment", "other");
        assertColumnCovers(connection, "message", "priority", "low", "normal", "high", "urgent");
        assertColumnCovers(connection, "message", "state",
                "draft", "scheduled", "sent", "active", "edited", "deleted", "cancelled");
        assertColumnCovers(connection, "receive_message", "state", "pending", "delivered", "read");
    }

    private static void assertFullSeedAssessmentQuestionAttemptConformance(Connection connection)
            throws SQLException {
        assertNoRows(connection, "Full seed available online assessments must have active questions", """
                SELECT a.id_assessment, a.title, a.state
                FROM assessment a
                LEFT JOIN question q ON q.id_assessment = a.id_assessment
                WHERE a.mode = 'online'
                  AND a.state IN ('active', 'scheduled')
                GROUP BY a.id_assessment, a.title, a.state
                HAVING COUNT(q.id_question) = 0
                ORDER BY a.id_assessment
                """);
        assertNoRows(connection, "Full seed attempts must have assessment questions", """
                SELECT at.id_attempt, at.id_assessment, at.state
                FROM attempt at
                WHERE NOT EXISTS (
                        SELECT 1
                        FROM question q
                        WHERE q.id_assessment = at.id_assessment
                  )
                ORDER BY at.id_attempt
                """);
        assertNoRows(connection, "Full seed submitted attempts must include every required response", """
                SELECT at.id_attempt, at.id_assessment,
                       COUNT(DISTINCT q.id_question) AS required_questions,
                       COUNT(DISTINCT r.id_question) AS answered_required_questions
                FROM attempt at
                JOIN question q
                  ON q.id_assessment = at.id_assessment
                 AND q.required_flag = 1
                LEFT JOIN response r
                  ON r.id_attempt = at.id_attempt
                 AND r.id_question = q.id_question
                WHERE at.state = 'submitted'
                GROUP BY at.id_attempt, at.id_assessment
                HAVING answered_required_questions <> required_questions
                ORDER BY at.id_attempt
                """);
        assertNoRows(connection, "Full seed submitted attempts must contain an answer payload", """
                SELECT at.id_attempt, at.id_assessment
                FROM attempt at
                LEFT JOIN response r ON r.id_attempt = at.id_attempt
                WHERE at.state = 'submitted'
                GROUP BY at.id_attempt, at.id_assessment
                HAVING COUNT(r.id_response) = 0
                ORDER BY at.id_attempt
                """);
        assertNoRows(connection, "Full seed automatic assessments cannot contain manual questions", """
                SELECT a.id_assessment, a.title, q.id_question, q.type
                FROM assessment a
                JOIN question q ON q.id_assessment = a.id_assessment
                WHERE a.correction_mode = 'automatic'
                  AND q.type IN ('short_text', 'paragraph', 'file_upload')
                ORDER BY a.id_assessment, q.id_question
                """);
        assertNoRows(connection, "Full seed option questions must expose their options", """
                SELECT q.id_question, q.id_assessment, q.type
                FROM question q
                LEFT JOIN question_option qo ON qo.id_question = q.id_question
                WHERE q.type IN ('single_choice', 'multiple_choice')
                GROUP BY q.id_question, q.id_assessment, q.type
                HAVING COUNT(qo.id_option) = 0
                ORDER BY q.id_assessment, q.id_question
                """);
        assertNoRows(connection, "Functions Applied Checkpoint questions must all be required", """
                SELECT id_question, required_flag
                FROM question
                WHERE id_assessment = 6500
                  AND required_flag <> 1
                ORDER BY id_question
                """);
        assertNoRows(connection, "Functions Applied Checkpoint attempts must answer all 10 questions", """
                SELECT at.id_attempt,
                       COUNT(DISTINCT q.id_question) AS question_count,
                       COUNT(DISTINCT r.id_question) AS response_question_count,
                       COUNT(DISTINCT r.id_response) AS response_count
                FROM attempt at
                JOIN question q ON q.id_assessment = at.id_assessment
                LEFT JOIN response r
                  ON r.id_attempt = at.id_attempt
                 AND r.id_question = q.id_question
                WHERE at.id_assessment = 6500
                GROUP BY at.id_attempt
                HAVING question_count <> 10
                    OR response_question_count <> 10
                    OR response_count <> 10
                ORDER BY at.id_attempt
                """);
        assertNoRows(connection, "Functions Applied Checkpoint attempts must contain an answer payload for every question", """
                SELECT at.id_attempt, q.id_question
                FROM attempt at
                JOIN question q ON q.id_assessment = at.id_assessment
                LEFT JOIN response r
                  ON r.id_attempt = at.id_attempt
                 AND r.id_question = q.id_question
                WHERE at.id_assessment = 6500
                  AND NOT (
                      (r.answer IS NOT NULL AND TRIM(r.answer) <> '')
                      OR (r.attachment IS NOT NULL AND TRIM(r.attachment) <> '')
                      OR EXISTS (
                          SELECT 1
                          FROM response_option ro
                          WHERE ro.id_response = r.id_response
                      )
                  )
                ORDER BY at.id_attempt, q.id_question
                """);
        assertNoRows(connection, "Functions Applied Checkpoint corrected totals must equal response scores", """
                SELECT at.id_attempt,
                       at.score AS attempt_score,
                       COALESCE(SUM(r.score), 0) AS response_score,
                       COUNT(r.id_response) AS response_count,
                       COUNT(r.score) AS scored_response_count
                FROM attempt at
                JOIN response r ON r.id_attempt = at.id_attempt
                WHERE at.id_assessment = 6500
                  AND at.state = 'corrected'
                GROUP BY at.id_attempt, at.score
                HAVING response_count <> 10
                    OR scored_response_count <> 10
                    OR ABS(attempt_score - response_score) > 0.001
                ORDER BY at.id_attempt
                """);
        assertNoRows(connection, "Functions Applied Checkpoint must retain a full-score corrected attempt", """
                SELECT a.id_assessment, a.max_grade
                FROM assessment a
                LEFT JOIN attempt at
                  ON at.id_assessment = a.id_assessment
                 AND at.state = 'corrected'
                 AND ABS(at.score - a.max_grade) <= 0.001
                WHERE a.id_assessment = 6500
                GROUP BY a.id_assessment, a.max_grade
                HAVING COUNT(at.id_attempt) = 0
                """);
        assertNoRows(connection, "Functions Applied Checkpoint submitted attempts must remain unscored until correction", """
                SELECT at.id_attempt, at.score, COUNT(r.score) AS scored_response_count
                FROM attempt at
                JOIN response r ON r.id_attempt = at.id_attempt
                WHERE at.id_assessment = 6500
                  AND at.state = 'submitted'
                GROUP BY at.id_attempt, at.score
                HAVING at.score IS NOT NULL OR scored_response_count <> 0
                ORDER BY at.id_attempt
                """);
    }

    private static void assertStudent6510FullCoverage(Connection connection) throws SQLException {
        assertStudentColumnCovers(connection, "enroll_course", "id_student_user", "state", 6510,
                "active", "inactive", "completed", "withdrawn");
        assertStudentColumnCovers(connection, "enroll_class_group", "id_student_user", "state", 6510,
                "pending", "active", "inactive", "rejected", "completed", "withdrawn");
        assertStudentColumnCovers(connection, "enroll_assessment", "id_student_user", "state", 6510,
                "pending", "active", "inactive", "rejected", "completed", "withdrawn");
        // The scheduled student assessment is intentionally attempt-free until
        // its availability window opens.  Attempt-state variety is covered by
        // the independent full-seed assessment fixtures; this student retains
        // historical corrected results without violating the schedule.
        assertStudentColumnCovers(connection, "attempt", "id_student_user", "state", 6510,
                "corrected");
        assertStudentColumnCovers(connection, "attendance_record", "id_user_student", "status", 6510,
                "present", "absent", "justified", "late", "partial");
        assertStudentColumnCovers(connection, "attendance_record", "id_user_student", "state", 6510,
                "active", "corrected", "cancelled");
        assertStudentColumnCovers(connection, "absence_justification", "id_user_student_submitter", "state", 6510,
                "submitted", "under_review", "approved", "rejected", "cancelled");
        assertStudentColumnCovers(connection, "grade_record", "id_user_student", "state", 6510, "published");
        assertStudentColumnCovers(connection, "grade_record", "id_user_student", "result", 6510, "approved");
        assertStudentColumnCovers(connection, "certificate", "id_user_student", "state", 6510,
                "draft", "issued");
        // RC-MATH-05 is a published student-facing class-group fixture: its
        // pedagogical items must never remain Draft, otherwise they are
        // correctly hidden from the student.  Keep Draft state coverage in
        // the independent full-seed coverage context (class group 3002 and
        // subject 3002) instead of weakening this rule for the real class.
        assertStudentColumnCovers(connection, "lesson", "id_class_group", "state", 65,
                "scheduled", "active", "completed", "cancelled");
        assertStudentColumnCovers(connection, "assessment", "id_subject", "state", 43,
                "scheduled", "active", "completed");
    }

    private static void assertFullSeedEmailsAreValid(Connection connection) throws SQLException {
        List<String> invalidEmails = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_user, email
                FROM user_account
                ORDER BY id_user
                """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String email = resultSet.getString("email");
                if (!BASIC_EMAIL_PATTERN.matcher(email).matches()) {
                    invalidEmails.add(resultSet.getLong("id_user") + " " + email);
                }
            }
        }
        assertTrue(
                invalidEmails.isEmpty(),
                "Full seed user emails must be valid: " + String.join("; ", invalidEmails)
        );
    }

    private static void assertFullSeedContentFileHashesAreValid(Connection connection) throws SQLException {
        List<String> invalidHashes = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_content_file, sha256
                FROM content_file
                WHERE sha256 IS NOT NULL
                ORDER BY id_content_file
                """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String sha256 = resultSet.getString("sha256");
                if (!SHA256_PATTERN.matcher(sha256).matches()) {
                    invalidHashes.add(resultSet.getLong("id_content_file") + " " + sha256);
                }
            }
        }
        assertTrue(
                invalidHashes.isEmpty(),
                "Full seed content file hashes must be SHA-256 hex values: " + String.join("; ", invalidHashes)
        );
    }

    private static void assertFullSeedLongDirectMessageChat(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT
                    COUNT(*) AS message_count,
                    SUM(CASE WHEN m.type = 'attachment' THEN 1 ELSE 0 END) AS attachment_count,
                    SUM(CASE WHEN m.id_user_sender = 1 THEN 1 ELSE 0 END) AS admin_sent_count,
                    SUM(CASE WHEN m.id_user_sender = 6 THEN 1 ELSE 0 END) AS teacher_sent_count,
                    SUM(CASE WHEN rm.id_user = 1 AND rm.delivered_at IS NOT NULL AND rm.read_at IS NULL THEN 1 ELSE 0 END)
                        AS admin_unread_count,
                    SUM(CASE WHEN rm.id_user = 6 AND rm.delivered_at IS NOT NULL AND rm.read_at IS NULL THEN 1 ELSE 0 END)
                        AS teacher_unread_count,
                    SUM(CASE WHEN m.type = 'attachment' AND m.attachment LIKE 'contents/%' THEN 1 ELSE 0 END)
                        AS downloadable_attachment_count
                FROM message m
                JOIN receive_message rm ON rm.id_message = m.id_message
                WHERE m.id_channel = 3205
                """);
             ResultSet resultSet = statement.executeQuery()) {
            assertTrue(resultSet.next(), "Long direct message chat aggregate must be available");
            assertTrue(resultSet.getInt("message_count") == 100, "Admin/teacher2 chat must have 100 messages");
            assertTrue(resultSet.getInt("attachment_count") == 10, "Admin/teacher2 chat must have 10 attachments");
            assertTrue(resultSet.getInt("admin_sent_count") == 50, "Admin must send 50 messages in the long chat");
            assertTrue(resultSet.getInt("teacher_sent_count") == 50, "Teacher2 must send 50 messages in the long chat");
            assertTrue(resultSet.getInt("admin_unread_count") == 4, "Admin must have 4 unread demo messages");
            assertTrue(resultSet.getInt("teacher_unread_count") == 3, "Teacher2 must have 3 unread demo messages");
            assertTrue(
                    resultSet.getInt("downloadable_attachment_count") == 10,
                    "Long chat attachments must use private content download paths"
            );
        }

        assertNoRows(connection, "Long direct message chat must only use the expected real demo attachments", """
                SELECT id_message, attachment
                FROM message
                WHERE id_channel = 3205
                  AND type = 'attachment'
                  AND attachment NOT IN (
                        'contents/images/er.webp',
                        'contents/guide-prj.pdf',
                        'contents/videos/normalization.mp4',
                        'contents/packages/scorm-quality.zip',
                        'contents/presentations/industrial-safety-slides.pdf',
                        'contents/videos/planning.mp4',
                        'contents/packages/xapi-audit.zip'
                  )
                ORDER BY id_message
                """);
    }

    private static void assertFullSeedCertificateLifecycle(Connection connection) throws SQLException {
        List<String> mismatches = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_certificate, title, state, validation_code, issued_at, final_grade
                FROM certificate
                ORDER BY id_certificate
                """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                long certificateId = resultSet.getLong("id_certificate");
                String title = resultSet.getString("title");
                String state = resultSet.getString("state");
                boolean hasValidationCode = resultSet.getString("validation_code") != null;
                boolean hasIssuedAt = resultSet.getTimestamp("issued_at") != null;
                boolean hasFinalGrade = resultSet.getBigDecimal("final_grade") != null;
                if ("active".equals(state)) {
                    mismatches.add(certificateId + " " + title + " uses unsupported active state");
                } else if ("draft".equals(state) && (hasValidationCode || hasIssuedAt || hasFinalGrade)) {
                    mismatches.add(certificateId + " " + title + " draft has published fields");
                } else if ("issued".equals(state) && (!hasValidationCode || !hasIssuedAt || !hasFinalGrade)) {
                    mismatches.add(certificateId + " " + title + " issued certificate misses published fields");
                }
            }
        }
        assertTrue(
                mismatches.isEmpty(),
                "Full seed certificates must match service lifecycle: " + String.join("; ", mismatches)
        );
    }

    private static void assertFullSeedIssuedCertificatesAreReproducible(Connection connection) throws SQLException {
        List<String> mismatches = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_certificate, id_course, id_user_student, title, final_grade
                FROM certificate
                WHERE state = 'issued'
                ORDER BY id_certificate
                """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                long certificateId = resultSet.getLong("id_certificate");
                long courseId = resultSet.getLong("id_course");
                long studentUserId = resultSet.getLong("id_user_student");
                String title = resultSet.getString("title");
                BigDecimal persistedFinalGrade = resultSet.getBigDecimal("final_grade");

                CertificateCalculationAudit calculation = calculateIssuedCertificate(
                        connection,
                        certificateId,
                        courseId,
                        studentUserId,
                        title,
                        mismatches
                );
                if (calculation == null) {
                    continue;
                }
                if (persistedFinalGrade == null || persistedFinalGrade.compareTo(calculation.finalGrade()) != 0) {
                    mismatches.add(certificateId + " " + title + " final grade expected "
                            + calculation.finalGrade() + " but was " + persistedFinalGrade);
                }
                List<Long> linkedGradeSheetIds = certificateGradeSheetIds(connection, certificateId);
                if (!gradeSheetSubjectIds(connection, linkedGradeSheetIds)
                        .equals(gradeSheetSubjectIds(connection, calculation.gradeSheetIds()))) {
                    mismatches.add(certificateId + " " + title + " grade sheet subjects expected "
                            + gradeSheetSubjectIds(connection, calculation.gradeSheetIds())
                            + " but were " + gradeSheetSubjectIds(connection, linkedGradeSheetIds));
                }
            }
        }
        assertTrue(
                mismatches.isEmpty(),
                "Full seed issued certificates must be reproducible by CertificateService rules: "
                        + String.join("; ", mismatches)
        );
    }

    private static CertificateCalculationAudit calculateIssuedCertificate(
            Connection connection,
            long certificateId,
            long courseId,
            long studentUserId,
            String title,
            List<String> mismatches
    ) throws SQLException {
        if (!activeStudentExists(connection, studentUserId)) {
            mismatches.add(certificateId + " " + title + " student is not active: " + studentUserId);
            return null;
        }
        String enrollmentState = courseEnrollmentState(connection, studentUserId, courseId);
        if (enrollmentState == null || "withdrawn".equals(enrollmentState) || "rejected".equals(enrollmentState)) {
            mismatches.add(certificateId + " " + title + " student is not eligible for course "
                    + courseId + ": " + enrollmentState);
            return null;
        }

        CourseScaleAudit course = courseScale(connection, courseId);
        List<CourseSubjectScaleAudit> subjects = activeCourseSubjects(connection, courseId);
        if (subjects.isEmpty()) {
            mismatches.add(certificateId + " " + title + " course has no active subjects");
            return null;
        }
        BigDecimal subjectEctsTotal = BigDecimal.ZERO;
        for (CourseSubjectScaleAudit subject : subjects) {
            if (subject.mandatory()) {
                subjectEctsTotal = subjectEctsTotal.add(subject.ects());
            }
        }
        if (subjectEctsTotal.compareTo(BigDecimal.ZERO) <= 0) {
            mismatches.add(certificateId + " " + title + " course has no positive mandatory subject ECTS");
            return null;
        }

        BigDecimal weightedTotal = BigDecimal.ZERO;
        List<Long> gradeSheetIds = new ArrayList<>();
        for (CourseSubjectScaleAudit subject : subjects) {
            SubjectApprovedGradeAudit approvedGrade = approvedSubjectGrade(
                    connection,
                    courseId,
                    subject.subjectId(),
                    studentUserId
            );
            if (approvedGrade == null) {
                if (subject.mandatory()) {
                    mismatches.add(certificateId + " " + title
                            + " has no approved published complete grade for mandatory subject " + subject.subjectId());
                    return null;
                }
                continue;
            }
            BigDecimal subjectFinalGrade = approvedGrade.value()
                    .divide(approvedGrade.gradeSheetMaxGrade(), 8, RoundingMode.HALF_UP)
                    .multiply(subject.finalGradeMax());
            BigDecimal courseScaleGrade = subjectFinalGrade
                    .divide(subject.finalGradeMax(), 8, RoundingMode.HALF_UP)
                    .multiply(course.certificateMaxGrade());
            if (subject.mandatory()) {
                weightedTotal = weightedTotal.add(courseScaleGrade.multiply(subject.ects()));
                if (approvedGrade.value().divide(approvedGrade.gradeSheetMaxGrade(), 8, RoundingMode.HALF_UP)
                        .compareTo(new BigDecimal("0.50")) <= 0) {
                    mismatches.add(certificateId + " " + title
                            + " mandatory subject " + subject.subjectId() + " is not positive");
                    return null;
                }
            }
            gradeSheetIds.add(approvedGrade.gradeSheetId());
        }
        BigDecimal finalGrade = weightedTotal.divide(subjectEctsTotal, 2, RoundingMode.HALF_UP);
        if (finalGrade.compareTo(BigDecimal.ZERO) < 0 || finalGrade.compareTo(course.certificateMaxGrade()) > 0) {
            mismatches.add(certificateId + " " + title + " final grade is outside course scale: " + finalGrade);
            return null;
        }
        return new CertificateCalculationAudit(finalGrade, List.copyOf(gradeSheetIds));
    }

    private static boolean activeStudentExists(Connection connection, long studentUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                FROM student_profile sp
                JOIN user_account u ON u.id_user = sp.id_user
                WHERE sp.id_user = ?
                  AND u.state = 'active'
                LIMIT 1
                """)) {
            statement.setLong(1, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static String courseEnrollmentState(Connection connection, long studentUserId, long courseId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT state
                FROM enroll_course
                WHERE id_student_user = ?
                  AND id_course = ?
                """)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("state") : null;
            }
        }
    }

    private static CourseScaleAudit courseScale(Connection connection, long courseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT ects, certificate_max_grade
                FROM course
                WHERE id_course = ?
                """)) {
            statement.setLong(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Course not found: " + courseId);
                }
                return new CourseScaleAudit(
                        resultSet.getBigDecimal("ects"),
                        resultSet.getBigDecimal("certificate_max_grade")
                );
            }
        }
    }

    private static List<CourseSubjectScaleAudit> activeCourseSubjects(Connection connection, long courseId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT s.id_subject, s.ects, s.final_grade_max, isub.mandatory
                FROM integrate_subject isub
                JOIN subject s ON s.id_subject = isub.id_subject
                WHERE isub.id_course = ?
                  AND isub.state = 'active'
                  AND s.state = 'active'
                ORDER BY isub.curricular_year, isub.term, s.id_subject
                """)) {
            statement.setLong(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CourseSubjectScaleAudit> subjects = new ArrayList<>();
                while (resultSet.next()) {
                    subjects.add(new CourseSubjectScaleAudit(
                            resultSet.getLong("id_subject"),
                            resultSet.getBigDecimal("ects"),
                            resultSet.getBigDecimal("final_grade_max"),
                            resultSet.getBoolean("mandatory")
                    ));
                }
                return List.copyOf(subjects);
            }
        }
    }

    private static SubjectApprovedGradeAudit approvedSubjectGrade(
            Connection connection,
            long courseId,
            long subjectId,
            long studentUserId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT gs.id_grade_sheet, gs.max_grade, gr.value
                FROM grade_record gr
                JOIN grade_sheet gs ON gs.id_grade_sheet = gr.id_grade_sheet
                WHERE gr.id_user_student = ?
                  AND gr.result = 'approved'
                  AND gr.state = 'published'
                  AND gs.id_subject = ?
                  AND gs.state IN ('published', 'closed')
                  AND (
                        EXISTS (
                            SELECT 1
                            FROM associate_grade_sheet_class_group agscg
                            JOIN class_group cg ON cg.id_class_group = agscg.id_class_group
                            WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                              AND cg.id_course = ?
                              AND cg.id_subject = ?
                        )
                        OR (
                            gs.scope = 'subject_occurrence'
                            AND EXISTS (
                                SELECT 1
                                FROM course_occurrence co
                                WHERE co.id_course_occurrence = gs.id_course_occurrence
                                  AND co.id_course = ?
                            )
                        )
                  )
                ORDER BY (gr.value / NULLIF(gs.max_grade, 0)) DESC,
                         gr.recorded_at DESC,
                         gr.id_grade_record DESC
                """)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, subjectId);
            statement.setLong(3, courseId);
            statement.setLong(4, subjectId);
            statement.setLong(5, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long gradeSheetId = resultSet.getLong("id_grade_sheet");
                    if (certificateGradeSheetCompleteForStudent(connection, gradeSheetId, studentUserId)) {
                        return new SubjectApprovedGradeAudit(
                                gradeSheetId,
                                resultSet.getBigDecimal("max_grade"),
                                resultSet.getBigDecimal("value")
                        );
                    }
                }
            }
        }
        return null;
    }

    private static boolean certificateGradeSheetCompleteForStudent(
            Connection connection,
            long gradeSheetId,
            long studentUserId
    ) throws SQLException {
        long subjectId;
        long courseOccurrenceId;
        String state;
        boolean hasClassGroups;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT gs.id_subject, gs.id_course_occurrence, gs.state,
                       EXISTS (
                           SELECT 1
                           FROM associate_grade_sheet_class_group agscg
                           WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                       ) AS has_class_groups
                FROM grade_sheet gs
                WHERE gs.id_grade_sheet = ?
                """)) {
            statement.setLong(1, gradeSheetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return false;
                }
                subjectId = resultSet.getLong("id_subject");
                courseOccurrenceId = resultSet.getLong("id_course_occurrence");
                state = resultSet.getString("state");
                hasClassGroups = resultSet.getBoolean("has_class_groups");
            }
        }
        if (!("published".equals(state) || "closed".equals(state))) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                FROM grade_record
                WHERE id_grade_sheet = ?
                  AND id_user_student = ?
                  AND state = 'published'
                LIMIT 1
                """)) {
            statement.setLong(1, gradeSheetId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static boolean weightsAreConfigured(
            Connection connection,
            long gradeSheetId,
            List<Long> assessmentIds
    ) throws SQLException {
        if (assessmentIds == null || assessmentIds.isEmpty()) {
            return false;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Long assessmentId : assessmentIds) {
            BigDecimal weight = assessmentWeight(connection, gradeSheetId, assessmentId);
            if (weight == null
                    || weight.compareTo(BigDecimal.ZERO) < 0
                    || weight.compareTo(new BigDecimal("100.00")) > 0) {
                return false;
            }
            total = total.add(weight);
        }
        return total.compareTo(new BigDecimal("100.00")) == 0;
    }

    private static BigDecimal assessmentWeight(Connection connection, long gradeSheetId, long assessmentId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT weight
                FROM based_on_assessment
                WHERE id_grade_sheet = ?
                  AND id_assessment = ?
                """)) {
            statement.setLong(1, gradeSheetId);
            statement.setLong(2, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getBigDecimal("weight") : null;
            }
        }
    }

    private static List<Long> certificateGradeSheetIds(Connection connection, long certificateId)
            throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_grade_sheet
                FROM based_on_grade_sheet_certificate
                WHERE id_certificate = ?
                ORDER BY id_grade_sheet
                """)) {
            statement.setLong(1, certificateId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getLong("id_grade_sheet"));
                }
            }
        }
        return List.copyOf(ids);
    }

    private record CourseScaleAudit(BigDecimal ects, BigDecimal certificateMaxGrade) {
    }

    private record CourseSubjectScaleAudit(
            long subjectId,
            BigDecimal ects,
            BigDecimal finalGradeMax,
            boolean mandatory
    ) {
    }

    private record SubjectApprovedGradeAudit(long gradeSheetId, BigDecimal gradeSheetMaxGrade, BigDecimal value) {
    }

    private record CertificateCalculationAudit(BigDecimal finalGrade, List<Long> gradeSheetIds) {
    }

    private static void assertNoRows(Connection connection, String message, String sql) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            ResultSetMetaData metadata = resultSet.getMetaData();
            int columnCount = metadata.getColumnCount();
            while (resultSet.next()) {
                List<String> values = new ArrayList<>();
                for (int index = 1; index <= columnCount; index++) {
                    values.add(metadata.getColumnLabel(index) + "=" + resultSet.getString(index));
                }
                rows.add(String.join(", ", values));
                if (rows.size() >= 20) {
                    break;
                }
            }
        }
        assertTrue(rows.isEmpty(), message + ": " + String.join("; ", rows));
    }

    private static void assertColumnCovers(
            Connection connection,
            String tableName,
            String columnName,
            String... expectedValues
    ) throws SQLException {
        List<String> missing = new ArrayList<>();
        for (String expectedValue : expectedValues) {
            if (!existsValue(connection, tableName, columnName, expectedValue)) {
                missing.add(expectedValue);
            }
        }
        assertTrue(
                missing.isEmpty(),
                tableName + "." + columnName + " missing full seed values: " + String.join(", ", missing)
        );
    }

    private static void assertStudentColumnCovers(
            Connection connection,
            String tableName,
            String studentColumn,
            String stateColumn,
            long studentId,
            String... expectedValues
    ) throws SQLException {
        List<String> missing = new ArrayList<>();
        String sql = "SELECT 1 FROM " + tableName + " WHERE " + studentColumn + " = ? AND " + stateColumn + " = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String expectedValue : expectedValues) {
                statement.setLong(1, studentId);
                statement.setString(2, expectedValue);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        missing.add(expectedValue);
                    }
                }
            }
        }
        assertTrue(
                missing.isEmpty(),
                "Student #6510 " + tableName + "." + stateColumn + " missing states: " + String.join(", ", missing)
        );
    }

    private static boolean existsValue(
            Connection connection,
            String tableName,
            String columnName,
            String expectedValue
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM " + tableName + " WHERE " + columnName + " = ? LIMIT 1")) {
            statement.setString(1, expectedValue);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static Set<Long> gradeSheetSubjectIds(Connection connection, List<Long> gradeSheetIds)
            throws SQLException {
        Set<Long> subjectIds = new java.util.LinkedHashSet<>();
        if (gradeSheetIds == null || gradeSheetIds.isEmpty()) {
            return subjectIds;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(gradeSheetIds.size(), "?"));
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT DISTINCT id_subject FROM grade_sheet WHERE id_grade_sheet IN (" + placeholders + ")")) {
            for (int index = 0; index < gradeSheetIds.size(); index++) {
                statement.setLong(index + 1, gradeSheetIds.get(index));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    subjectIds.add(resultSet.getLong("id_subject"));
                }
            }
        }
        return subjectIds;
    }

    private static boolean gradeSheetComplete(Connection connection, long gradeSheetId) throws SQLException {
        if (!hasClassGroupScope(connection, gradeSheetId)) {
            return subjectGradeSheetComplete(connection, gradeSheetId);
        }
        List<Long> studentUserIds = requiredStudentUserIds(connection, gradeSheetId);
        if (studentUserIds.isEmpty()) {
            return false;
        }
        List<Long> assessmentIds = assessmentIds(connection, gradeSheetId);
        if (!weightsAreConfigured(connection, gradeSheetId, assessmentIds)) {
            return false;
        }
        for (Long studentUserId : studentUserIds) {
            if (!hasGradeRecord(connection, gradeSheetId, studentUserId)) {
                return false;
            }
            for (Long assessmentId : assessmentIds) {
                if (!hasCorrectedAssessmentScore(connection, studentUserId, assessmentId)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasClassGroupScope(Connection connection, long gradeSheetId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                FROM associate_grade_sheet_class_group
                WHERE id_grade_sheet = ?
                LIMIT 1
                """)) {
            statement.setLong(1, gradeSheetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static boolean subjectGradeSheetComplete(Connection connection, long gradeSheetId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT source.id_grade_sheet, source.state
                FROM grade_sheet aggregate_sheet
                JOIN grade_sheet source
                  ON source.id_subject = aggregate_sheet.id_subject
                 AND source.id_course_occurrence = aggregate_sheet.id_course_occurrence
                WHERE aggregate_sheet.id_grade_sheet = ?
                  AND EXISTS (
                        SELECT 1
                        FROM associate_grade_sheet_class_group source_class_group
                        WHERE source_class_group.id_grade_sheet = source.id_grade_sheet
                    )
                ORDER BY source.id_grade_sheet
                """)) {
            statement.setLong(1, gradeSheetId);
            boolean foundSource = false;
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    foundSource = true;
                    String state = resultSet.getString("state");
                    // A subject-occurrence sheet is an aggregator: its
                    // publication depends on every source class-group sheet
                    // being Published. Missing source values are represented
                    // as '-' and do not make the aggregate Draft.
                    if (!"published".equals(state)) {
                        return false;
                    }
                }
            }
            return foundSource;
        }
    }

    private static List<Long> requiredStudentUserIds(Connection connection, long gradeSheetId) throws SQLException {
        List<Long> studentUserIds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT DISTINCT required.id_student_user
                FROM (
                    SELECT ecg.id_student_user
                    FROM associate_grade_sheet_class_group agscg
                    JOIN enroll_class_group ecg ON ecg.id_class_group = agscg.id_class_group
                    WHERE agscg.id_grade_sheet = ?
                      AND ecg.state IN ('active', 'completed')
                    UNION
                    SELECT ecg.id_student_user
                    FROM grade_sheet gs
                    JOIN class_group cg
                      ON cg.id_subject = gs.id_subject
                     AND cg.id_course_occurrence = gs.id_course_occurrence
                    JOIN enroll_class_group ecg ON ecg.id_class_group = cg.id_class_group
                    WHERE gs.id_grade_sheet = ?
                      AND ecg.state IN ('active', 'completed')
                      AND NOT EXISTS (
                            SELECT 1
                            FROM associate_grade_sheet_class_group agscg
                            WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                      )
                    UNION
                    SELECT gr.id_user_student AS id_student_user
                    FROM grade_record gr
                    WHERE gr.id_grade_sheet = ?
                ) required
                ORDER BY required.id_student_user
                """)) {
            statement.setLong(1, gradeSheetId);
            statement.setLong(2, gradeSheetId);
            statement.setLong(3, gradeSheetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    studentUserIds.add(resultSet.getLong("id_student_user"));
                }
            }
        }
        return studentUserIds;
    }

    private static List<Long> assessmentIds(Connection connection, long gradeSheetId) throws SQLException {
        List<Long> assessmentIds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT DISTINCT a.id_assessment
                FROM grade_sheet gs
                LEFT JOIN associate_grade_sheet_class_group agscg ON agscg.id_grade_sheet = gs.id_grade_sheet
                LEFT JOIN content_block cb ON cb.id_class_group = agscg.id_class_group
                LEFT JOIN assessment_class_group acg ON acg.id_class_group = agscg.id_class_group
                LEFT JOIN assessment a ON (
                        a.id_content_block = cb.id_content_block
                        OR a.id_assessment = acg.id_assessment
                    )
                   AND (a.id_subject IS NULL OR a.id_subject = gs.id_subject)
                WHERE gs.id_grade_sheet = ?
                  AND a.id_assessment IS NOT NULL
                ORDER BY a.id_assessment
                """)) {
            statement.setLong(1, gradeSheetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    assessmentIds.add(resultSet.getLong("id_assessment"));
                }
            }
        }
        if (!assessmentIds.isEmpty()) {
            return assessmentIds;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_assessment
                FROM based_on_assessment
                WHERE id_grade_sheet = ?
                ORDER BY id_assessment
                """)) {
            statement.setLong(1, gradeSheetId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    assessmentIds.add(resultSet.getLong("id_assessment"));
                }
            }
        }
        return assessmentIds;
    }

    private static boolean hasGradeRecord(Connection connection, long gradeSheetId, long studentUserId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                FROM grade_record
                WHERE id_grade_sheet = ?
                  AND id_user_student = ?
                  AND state = 'published'
                LIMIT 1
                """)) {
            statement.setLong(1, gradeSheetId);
            statement.setLong(2, studentUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static boolean hasCorrectedAssessmentScore(Connection connection, long studentUserId, long assessmentId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                FROM attempt
                WHERE id_student_user = ?
                  AND id_assessment = ?
                  AND state = 'corrected'
                  AND score IS NOT NULL
                LIMIT 1
                """)) {
            statement.setLong(1, studentUserId);
            statement.setLong(2, assessmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
