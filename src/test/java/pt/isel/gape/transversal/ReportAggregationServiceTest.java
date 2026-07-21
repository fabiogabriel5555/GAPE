package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
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
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.transversal.model.ManagementViewScope;
import pt.isel.gape.transversal.model.ManagementViewState;
import pt.isel.gape.transversal.model.ManagementViewType;
import pt.isel.gape.transversal.model.ReportAggregation;
import pt.isel.gape.transversal.service.ReportAggregationService;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class ReportAggregationServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-15T10:15:30Z"),
            ZoneOffset.UTC
    );
    private static final String IP = "127.0.0.1";

    private ReportAggregationService reportAggregationService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        reportAggregationService = new ReportAggregationService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void classGroupReportAggregatesCoherentIndicatorsFromExistingDaos() throws Exception {
        AccessContext teacher = actor(3L, AccessProfileType.TEACHER);
        long viewId = insertManagementView(
                "PRJ-T1 indicators", ManagementViewType.REPORT, ManagementViewScope.CLASS_GROUP, 50L, 3L
        );

        ReportAggregation aggregation = reportAggregationService.aggregate(teacher, viewId);

        assertEquals(ManagementViewScope.CLASS_GROUP, aggregation.visibilityScope());
        assertEquals(50L, aggregation.scopeContextId());
        assertEquals(3L, aggregation.ownerUserId());
        assertEquals(1, aggregation.courseCount());
        assertEquals(1, aggregation.subjectCount());
        assertEquals(1, aggregation.classGroupCount());
        assertEquals(1, aggregation.activeEnrollmentCount());
        assertEquals(1, aggregation.lessonCount());
        assertEquals(2, aggregation.assessmentCount());
        assertEquals(0, aggregation.submittedAttemptCount());
        assertEquals(1, aggregation.correctedAttemptCount());
        assertEquals(0, aggregation.pendingCorrectionCount());
        assertEquals(1, aggregation.attemptCount());
        assertEquals(1, aggregation.attendanceRecordCount());
        assertEquals(0, aggregation.issuedCertificateCount());
    }

    @Test
    void personalStudentAggregationExcludesExpiredAndUnenrolledClassGroupData() throws Exception {
        insertCurrentStudentContext();
        insertExpiredOccurrenceCertificate();
        AccessContext student = actor(4L, AccessProfileType.STUDENT);
        long personalViewId = insertManagementView(
                "My current report", ManagementViewType.REPORT, ManagementViewScope.PERSONAL, 4L, 4L
        );

        ReportAggregation aggregation = reportAggregationService.aggregate(student, personalViewId);

        assertEquals(ManagementViewScope.PERSONAL, aggregation.visibilityScope());
        assertEquals(4L, aggregation.scopeContextId());
        assertEquals(4L, aggregation.ownerUserId());
        assertEquals(1, aggregation.courseCount());
        assertEquals(1, aggregation.subjectCount());
        assertEquals(1, aggregation.classGroupCount());
        assertEquals(1, aggregation.activeEnrollmentCount());
        assertEquals(0, aggregation.lessonCount());
        assertEquals(0, aggregation.assessmentCount());
        assertEquals(0, aggregation.submittedAttemptCount());
        assertEquals(0, aggregation.correctedAttemptCount());
        assertEquals(0, aggregation.pendingCorrectionCount());
        assertEquals(0, aggregation.attemptCount());
        assertEquals(0, aggregation.attendanceRecordCount());
        assertEquals(0, aggregation.issuedCertificateCount());
    }

    @Test
    void aggregationUsesTheScopeAccessBoundaryBeforeLoadingIndicators() throws Exception {
        long foreignClassGroupReportId = insertManagementView(
                "Out-of-context report", ManagementViewType.REPORT, ManagementViewScope.CLASS_GROUP, 52L, 3L
        );

        assertThrows(
                SecurityException.class,
                () -> reportAggregationService.aggregate(actor(4L, AccessProfileType.STUDENT), foreignClassGroupReportId)
        );
    }

    private long insertManagementView(
            String title,
            ManagementViewType type,
            ManagementViewScope scope,
            Long scopeContextId,
            long ownerUserId
    ) throws SQLException {
        String sql = """
                INSERT INTO management_view (
                    title, type, description, visibility_scope, scope_target_type,
                    scope_target_id, owner_user_id, state
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseTestSupport.openConnection();
             java.sql.PreparedStatement statement = connection.prepareStatement(
                     sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS
             )) {
            statement.setString(1, title);
            statement.setString(2, type.toDatabaseValue());
            statement.setString(3, "Provisioned aggregation test panel");
            statement.setString(4, scope.toDatabaseValue());
            if (scope.targetType() == null) {
                statement.setNull(5, java.sql.Types.VARCHAR);
            } else {
                statement.setString(5, scope.targetType().toDatabaseValue());
            }
            if (scopeContextId == null) {
                statement.setNull(6, java.sql.Types.BIGINT);
            } else {
                statement.setLong(6, scopeContextId);
            }
            statement.setLong(7, ownerUserId);
            statement.setString(8, ManagementViewState.ACTIVE.toDatabaseValue());
            statement.executeUpdate();
            try (java.sql.ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Provisioned management-view fixture did not return an id");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    private static AccessContext actor(long userId, AccessProfileType profileType) {
        return new AccessContext(
                userId,
                userId == 1L ? 100L : null,
                profileType,
                AuthorizationPolicy.VIEW_REPORTS,
                AccessEntityType.GLOBAL,
                null,
                IP
        );
    }

    private void insertCurrentStudentContext() throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO subject (
                        id_subject, id_organization, id_organic_unit, name, acronym, photo, description,
                        ects, final_grade_max, workload_hours, state
                    ) VALUES (
                        42, 10, 20, 'Current Reporting Subject', 'CUR', NULL, 'Temporal report fixture',
                        6.00, 20.00, 30, 'active'
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO integrate_subject (
                        id_course, id_subject, curricular_year, term, mandatory, state, ended_at
                    ) VALUES (30, 42, 1, 'semester_2', 1, 'active', NULL)
                    """);
            statement.executeUpdate("""
                    INSERT INTO class_group (
                        id_class_group, id_subject, id_course, id_course_occurrence, id_course_occurrence_period,
                        cod_class_group, modality, state, min_students, max_students, starts_at, ends_at, shift
                    ) VALUES (
                        53, 42, 30, 300, 3002,
                        'CUR-T1', 'online', 'active', 5, 25, '2026-07-01', '2026-12-31', 'morning'
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO enroll_class_group (
                        id_student_user, id_class_group, state, start_date, end_date
                    ) VALUES (4, 53, 'active', '2026-07-01', '2026-12-31')
                    """);
        }
    }

    private void insertExpiredOccurrenceCertificate() throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO certificate (
                        id_certificate, id_course, id_course_occurrence, id_user_student,
                        title, notes, type, template, validation_code, issued_at, state, final_grade
                    ) VALUES (
                        193, 30, 299, 4,
                        'Historical certificate', 'Must not appear in the current occurrence report',
                        'completion', 'template-test', 'HISTORICAL-300',
                        '2025-12-31 12:00:00', 'issued', 16.00
                    )
                    """);
        }
    }
}
