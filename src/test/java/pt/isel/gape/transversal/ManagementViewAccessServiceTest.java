package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

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
import pt.isel.gape.transversal.dao.ManagementViewDAO;
import pt.isel.gape.transversal.model.ManagementView;
import pt.isel.gape.transversal.model.ManagementViewScope;
import pt.isel.gape.transversal.model.ManagementViewState;
import pt.isel.gape.transversal.model.ManagementViewType;
import pt.isel.gape.transversal.service.ManagementViewAccessService;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class ManagementViewAccessServiceTest {

    private static final long CURRENT_SUBJECT_ID = 42L;
    private static final long CURRENT_CLASS_GROUP_ID = 53L;
    private static final long UNMANAGED_ORGANIZATION_ID = 19L;
    private static final long OTHER_STUDENT_ID = 6L;
    private static final long MISSCOPED_ADMINISTRATOR_ID = 7L;
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-15T10:15:30Z"),
            ZoneOffset.UTC
    );
    private static final String IP = "127.0.0.1";

    private ManagementViewAccessService accessService;
    private ManagementViewDAO managementViewDAO;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws Exception {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        accessService = new ManagementViewAccessService(connectionProvider, FIXED_CLOCK);
        managementViewDAO = new ManagementViewDAO(connectionProvider);
        insertCurrentStudentContext();
        insertOtherActiveStudent();
        insertMisScopedAdministrator();
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void profilesCanAccessOnlyTheirAssignedScopeAndStudentReportsStayContextual() throws Exception {
        long globalViewId = insertManagementView(
                "Global dashboard", ManagementViewType.DASHBOARD, ManagementViewScope.GLOBAL, null, 1L
        );
        long managedOrganizationViewId = insertManagementView(
                "Managed organization", ManagementViewType.DASHBOARD, ManagementViewScope.ORGANIZATION, 10L, 1L
        );
        long unmanagedOrganizationViewId = insertManagementView(
                "Unmanaged organization", ManagementViewType.DASHBOARD,
                ManagementViewScope.ORGANIZATION, UNMANAGED_ORGANIZATION_ID, 1L
        );
        long coordinatedSubjectViewId = insertManagementView(
                "Coordinated subject", ManagementViewType.REPORT, ManagementViewScope.SUBJECT, 40L, 2L
        );
        long foreignSubjectViewId = insertManagementView(
                "Foreign subject", ManagementViewType.REPORT, ManagementViewScope.SUBJECT, 41L, 2L
        );
        long taughtClassGroupViewId = insertManagementView(
                "Taught class group", ManagementViewType.CONTROL_PANEL, ManagementViewScope.CLASS_GROUP, 50L, 3L
        );
        long foreignClassGroupViewId = insertManagementView(
                "Foreign class group", ManagementViewType.REPORT, ManagementViewScope.CLASS_GROUP, 52L, 3L
        );
        long personalViewId = insertManagementView(
                "Student personal", ManagementViewType.DASHBOARD, ManagementViewScope.PERSONAL, 4L, 4L
        );
        long anotherStudentPersonalViewId = insertManagementView(
                "Other student personal", ManagementViewType.DASHBOARD,
                ManagementViewScope.PERSONAL, OTHER_STUDENT_ID, OTHER_STUDENT_ID
        );
        long currentCourseReportViewId = insertManagementView(
                "Current course report", ManagementViewType.REPORT, ManagementViewScope.COURSE, 30L, 1L
        );
        long currentClassGroupReportViewId = insertManagementView(
                "Current class group report", ManagementViewType.REPORT,
                ManagementViewScope.CLASS_GROUP, CURRENT_CLASS_GROUP_ID, 3L
        );
        long currentClassGroupDashboardViewId = insertManagementView(
                "Current class group dashboard", ManagementViewType.DASHBOARD,
                ManagementViewScope.CLASS_GROUP, CURRENT_CLASS_GROUP_ID, 3L
        );
        grantExplicitAccess(4L, foreignClassGroupViewId);

        AccessContext administrator = actor(1L, AccessProfileType.ADMINISTRATOR);
        assertTrue(accessService.canAccess(administrator, globalViewId));
        assertTrue(accessService.canAccess(administrator, managedOrganizationViewId));
        assertFalse(accessService.canAccess(administrator, unmanagedOrganizationViewId));
        assertFalse(accessService.canAccess(administrator, coordinatedSubjectViewId));
        assertFalse(accessService.canAccess(
                actor(MISSCOPED_ADMINISTRATOR_ID, AccessProfileType.ADMINISTRATOR),
                managedOrganizationViewId
        ));
        assertFalse(accessService.canConfigure(
                MISSCOPED_ADMINISTRATOR_ID,
                AccessProfileType.ADMINISTRATOR,
                ManagementViewScope.ORGANIZATION,
                10L,
                MISSCOPED_ADMINISTRATOR_ID
        ));
        grantExactOrganizationPermission();
        assertTrue(accessService.canAccess(
                actor(MISSCOPED_ADMINISTRATOR_ID, AccessProfileType.ADMINISTRATOR),
                managedOrganizationViewId
        ));
        assertTrue(accessService.canConfigure(
                MISSCOPED_ADMINISTRATOR_ID,
                AccessProfileType.ADMINISTRATOR,
                ManagementViewScope.ORGANIZATION,
                10L,
                MISSCOPED_ADMINISTRATOR_ID
        ));

        AccessContext coordinator = actor(2L, AccessProfileType.COORDINATOR);
        assertTrue(accessService.canAccess(coordinator, coordinatedSubjectViewId));
        assertFalse(accessService.canAccess(coordinator, foreignSubjectViewId));
        assertFalse(accessService.canAccess(coordinator, taughtClassGroupViewId));

        AccessContext teacher = actor(3L, AccessProfileType.TEACHER);
        assertTrue(accessService.canAccess(teacher, taughtClassGroupViewId));
        assertFalse(accessService.canAccess(teacher, foreignClassGroupViewId));
        assertFalse(accessService.canAccess(teacher, coordinatedSubjectViewId));

        AccessContext student = actor(4L, AccessProfileType.STUDENT);
        assertTrue(accessService.canAccess(student, personalViewId));
        assertFalse(accessService.canAccess(student, anotherStudentPersonalViewId));
        assertTrue(accessService.canAccess(student, currentCourseReportViewId));
        assertTrue(accessService.canAccess(student, currentClassGroupReportViewId));
        assertFalse(accessService.canAccess(student, foreignClassGroupViewId));
        assertFalse(accessService.canAccess(student, currentClassGroupDashboardViewId));

        Set<Long> studentAccessibleIds = accessService.listAccessible(student).stream()
                .map(ManagementView::id)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(studentAccessibleIds.contains(personalViewId));
        assertTrue(studentAccessibleIds.contains(currentCourseReportViewId));
        assertTrue(studentAccessibleIds.contains(currentClassGroupReportViewId));
        assertFalse(studentAccessibleIds.contains(foreignClassGroupViewId));
        assertFalse(studentAccessibleIds.contains(anotherStudentPersonalViewId));
    }

    @Test
    void requireAccessAuditsSuccessAndDeniedScopeEvenWhenAnExplicitGrantExists() throws Exception {
        long globalViewId = insertManagementView(
                "Audited global dashboard", ManagementViewType.DASHBOARD, ManagementViewScope.GLOBAL, null, 1L
        );
        long outOfScopeViewId = insertManagementView(
                "Audited foreign class report", ManagementViewType.REPORT, ManagementViewScope.CLASS_GROUP, 52L, 3L
        );
        grantExplicitAccess(4L, outOfScopeViewId);

        ManagementView opened = accessService.requireAccess(
                actor(1L, AccessProfileType.ADMINISTRATOR),
                globalViewId
        );
        assertEquals(globalViewId, opened.id());
        assertThrows(
                SecurityException.class,
                () -> accessService.requireAccess(actor(4L, AccessProfileType.STUDENT), outOfScopeViewId)
        );

        assertEquals(1, countAccessAudit(globalViewId, "success"));
        assertEquals(1, countAccessAudit(outOfScopeViewId, "denied"));
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
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, title);
            statement.setString(2, type.toDatabaseValue());
            statement.setString(3, "Provisioned access-policy test panel");
            statement.setString(4, scope.toDatabaseValue());
            if (scope.targetType() == null) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(5, scope.targetType().toDatabaseValue());
            }
            if (scopeContextId == null) {
                statement.setNull(6, Types.BIGINT);
            } else {
                statement.setLong(6, scopeContextId);
            }
            statement.setLong(7, ownerUserId);
            statement.setString(8, ManagementViewState.ACTIVE.toDatabaseValue());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Provisioned management-view fixture did not return an id");
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    private void grantExplicitAccess(long userId, long managementViewId) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            managementViewDAO.grantExplicitAccess(connection, userId, managementViewId);
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
                    INSERT INTO teach_class_group (
                        id_teacher_user, id_class_group, state, start_date, end_date
                    ) VALUES (3, 53, 'active', '2026-07-01', '2026-12-31')
                    """);
            statement.executeUpdate("""
                    INSERT INTO enroll_class_group (
                        id_student_user, id_class_group, state, start_date, end_date
                    ) VALUES (4, 53, 'active', '2026-07-01', '2026-12-31')
                    """);
        }
    }

    private void insertOtherActiveStudent() throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement user = connection.prepareStatement("""
                     INSERT INTO user_account (
                         id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt
                     ) VALUES (?, 'Other Student', 'other.student@gape.local', 'active', 'pt-PT', NULL,
                               '2026-01-01 10:00:00', 'test-hash', 'test-salt')
                     """);
             PreparedStatement profile = connection.prepareStatement("""
                     INSERT INTO student_profile (id_user, cod_student)
                     VALUES (?, 'STD-OTHER')
                     """)) {
            user.setLong(1, OTHER_STUDENT_ID);
            user.executeUpdate();
            profile.setLong(1, OTHER_STUDENT_ID);
            profile.executeUpdate();
        }
    }

    private void insertMisScopedAdministrator() throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO user_account (
                        id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt
                    ) VALUES (7, 'Mis-scoped Administrator', 'mis-scoped.admin@gape.local', 'active', 'pt-PT', NULL,
                              '2026-01-01 10:05:00', 'test-hash-admin', 'test-salt-admin')
                    """);
            statement.executeUpdate("""
                    INSERT INTO administrator_profile (id_user, cod_administrator)
                    VALUES (7, 'ADM-MISSCOPED')
                    """);
            statement.executeUpdate("""
                    INSERT INTO manage_organization (
                        id_admin_user, id_organization, state, start_date, end_date
                    ) VALUES (7, 10, 'active', '2026-01-01', NULL)
                    """);
            statement.executeUpdate("""
                    INSERT INTO grant_administrator (
                        id_admin_user, cod_permission, context_type, context_id
                    ) VALUES (7, 'MANAGE_ORGANIZATION_STRUCTURE', 'ORGANIZATION', 11)
                    """);
        }
    }

    private void grantExactOrganizationPermission() throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO grant_administrator (
                         id_admin_user, cod_permission, context_type, context_id
                     ) VALUES (?, 'MANAGE_ORGANIZATION_STRUCTURE', 'ORGANIZATION', 10)
                     """)) {
            statement.setLong(1, MISSCOPED_ADMINISTRATOR_ID);
            statement.executeUpdate();
        }
    }

    private int countAccessAudit(long managementViewId, String outcome) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM activity_log
                     WHERE operation_type = ?
                       AND affected_entity_type = ?
                       AND affected_entity_identifier = ?
                       AND outcome = ?
                     """)) {
            statement.setString(1, ManagementViewAccessService.ACCESS_OPERATION);
            statement.setString(2, ManagementViewAccessService.ENTITY_TYPE);
            statement.setString(3, Long.toString(managementViewId));
            statement.setString(4, outcome);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}
