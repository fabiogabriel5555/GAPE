package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.transversal.dao.ManagementViewDAO;
import pt.isel.gape.transversal.model.ManagementView;
import pt.isel.gape.transversal.model.ManagementViewScope;
import pt.isel.gape.transversal.model.ManagementViewScopeTargetType;
import pt.isel.gape.transversal.model.ManagementViewState;
import pt.isel.gape.transversal.model.ManagementViewType;
import pt.isel.gape.transversal.model.ManagementViewUpdateCommand;
import pt.isel.gape.transversal.service.ManagementViewService;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class ManagementViewServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-15T10:15:30Z"),
            ZoneOffset.UTC
    );
    private static final String IP = "127.0.0.1";

    private ManagementViewService managementViewService;
    private ManagementViewDAO managementViewDAO;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        managementViewService = new ManagementViewService(connectionProvider, FIXED_CLOCK);
        managementViewDAO = new ManagementViewDAO(connectionProvider);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void authorizedProfilesCanReconfigureProvisionedViewsForEverySupportedScope() throws Exception {
        long globalId = provisionManagementView(
                "Global indicators", ManagementViewType.DASHBOARD, ManagementViewScope.GLOBAL, null, 1L
        );
        long organizationId = provisionManagementView(
                "Organization indicators", ManagementViewType.DASHBOARD, ManagementViewScope.ORGANIZATION, 10L, 1L
        );
        long courseId = provisionManagementView(
                "Course report", ManagementViewType.REPORT, ManagementViewScope.COURSE, 30L, 1L
        );
        long subjectId = provisionManagementView(
                "Subject report", ManagementViewType.REPORT, ManagementViewScope.SUBJECT, 40L, 2L
        );
        long classGroupId = provisionManagementView(
                "Class group report", ManagementViewType.CONTROL_PANEL, ManagementViewScope.CLASS_GROUP, 50L, 3L
        );
        long personalId = provisionManagementView(
                "My private report", ManagementViewType.DASHBOARD, ManagementViewScope.PERSONAL, 4L, 4L
        );

        ManagementView global = update(actor(1L, AccessProfileType.ADMINISTRATOR), globalId,
                "Updated global indicators", ManagementViewType.REPORT, ManagementViewScope.GLOBAL, null);
        ManagementView organization = update(actor(1L, AccessProfileType.ADMINISTRATOR), organizationId,
                "Updated organization indicators", ManagementViewType.DASHBOARD, ManagementViewScope.ORGANIZATION, 10L);
        ManagementView course = update(actor(1L, AccessProfileType.ADMINISTRATOR), courseId,
                "Updated course report", ManagementViewType.REPORT, ManagementViewScope.COURSE, 30L);
        ManagementView subject = update(actor(2L, AccessProfileType.COORDINATOR), subjectId,
                "Updated subject report", ManagementViewType.REPORT, ManagementViewScope.SUBJECT, 40L);
        ManagementView classGroup = update(actor(3L, AccessProfileType.TEACHER), classGroupId,
                "Updated class group report", ManagementViewType.CONTROL_PANEL, ManagementViewScope.CLASS_GROUP, 50L);
        ManagementView personal = update(actor(4L, AccessProfileType.STUDENT), personalId,
                "Updated private report", ManagementViewType.DASHBOARD, ManagementViewScope.PERSONAL, null);

        assertScope(global, ManagementViewScope.GLOBAL, null, null, 1L);
        assertScope(organization, ManagementViewScope.ORGANIZATION,
                ManagementViewScopeTargetType.ORGANIZATION, 10L, 1L);
        assertScope(course, ManagementViewScope.COURSE, ManagementViewScopeTargetType.COURSE, 30L, 1L);
        assertScope(subject, ManagementViewScope.SUBJECT, ManagementViewScopeTargetType.SUBJECT, 40L, 2L);
        assertScope(classGroup, ManagementViewScope.CLASS_GROUP,
                ManagementViewScopeTargetType.CLASS_GROUP, 50L, 3L);
        assertScope(personal, ManagementViewScope.PERSONAL, ManagementViewScopeTargetType.USER, 4L, 4L);
        assertEquals(ManagementViewState.ACTIVE, personal.state());
        assertEquals(6, countAudit(ManagementViewService.CONFIGURE_OPERATION, "success"));
    }

    @Test
    void configurationAndExplicitDistributionOfAProvisionedViewAreAudited() throws Exception {
        AccessContext administrator = actor(1L, AccessProfileType.ADMINISTRATOR);
        long globalId = provisionManagementView(
                "Initial global report", ManagementViewType.DASHBOARD, ManagementViewScope.GLOBAL, null, 1L
        );

        ManagementView updated = update(
                administrator,
                globalId,
                "Updated global report",
                ManagementViewType.REPORT,
                ManagementViewScope.GLOBAL,
                null
        );
        managementViewService.configureExplicitAccess(administrator, globalId, List.of(2L, 3L));

        assertEquals("Updated global report", updated.title());
        assertEquals(ManagementViewType.REPORT, updated.type());
        assertNull(updated.scopeContextId());
        assertTrue(managementViewDAO.hasExplicitAccess(2L, globalId));
        assertTrue(managementViewDAO.hasExplicitAccess(3L, globalId));
        assertFalse(managementViewDAO.hasExplicitAccess(4L, globalId));
        assertTrue(managementViewService.listConfigurable(administrator).stream()
                .anyMatch(view -> view.id() == globalId));
        assertEquals(List.of(2L, 3L), managementViewService.explicitRecipientUserIds(administrator, globalId));
        assertEquals(2, countAudit(ManagementViewService.CONFIGURE_OPERATION, "success"));
    }

    @Test
    void rejectsReconfigurationOutsideTheActorScopeAndAuditsTheDenial() throws Exception {
        long globalId = provisionManagementView(
                "Protected global report", ManagementViewType.REPORT, ManagementViewScope.GLOBAL, null, 1L
        );

        assertThrows(
                SecurityException.class,
                () -> update(
                        actor(4L, AccessProfileType.STUDENT),
                        globalId,
                        "Forbidden global report",
                        ManagementViewType.REPORT,
                        ManagementViewScope.GLOBAL,
                        null
                )
        );

        assertEquals(1, countAudit(ManagementViewService.CONFIGURE_OPERATION, "denied"));
    }

    private ManagementView update(
            AccessContext actor,
            long viewId,
            String title,
            ManagementViewType type,
            ManagementViewScope scope,
            Long scopeContextId
    ) {
        return managementViewService.updateManagementView(
                actor,
                viewId,
                new ManagementViewUpdateCommand(
                        title,
                        type,
                        "Provisioned panel configuration",
                        scope,
                        scopeContextId,
                        ManagementViewState.ACTIVE
                )
        );
    }

    /** Test fixtures provision rows directly; production code has no create-panel API. */
    private long provisionManagementView(
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
            statement.setString(3, "Provisioned test panel");
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

    private void assertScope(
            ManagementView view,
            ManagementViewScope expectedScope,
            ManagementViewScopeTargetType expectedTargetType,
            Long expectedTargetId,
            long expectedOwnerId
    ) {
        assertEquals(expectedScope, view.visibilityScope());
        assertEquals(expectedTargetType, view.scopeTargetType());
        assertEquals(expectedTargetId, view.scopeContextId());
        assertEquals(expectedOwnerId, view.ownerUserId());
    }

    private int countAudit(String operationType, String outcome) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM activity_log
                     WHERE operation_type = ?
                       AND outcome = ?
                     """)) {
            statement.setString(1, operationType);
            statement.setString(2, outcome);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}
