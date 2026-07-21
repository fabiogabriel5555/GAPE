package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.model.ActivityLog;
import pt.isel.gape.transversal.model.ActivityLogQuery;
import pt.isel.gape.transversal.model.ActivityLogScope;
import pt.isel.gape.transversal.service.ActivityLogService;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class ActivityLogQueryServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-15T10:15:30Z"),
            ZoneOffset.UTC
    );
    private static final LocalDateTime FIXED_OCCURRED_AT = LocalDateTime.of(2026, 7, 15, 10, 15, 30);
    private static final String SOURCE_IP = "127.0.0.1";

    private ActivityLogDAO activityLogDAO;
    private ActivityLogService activityLogService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        activityLogDAO = new ActivityLogDAO(connectionProvider);
        activityLogService = new ActivityLogService(
                activityLogDAO,
                new PermissionChecker(connectionProvider),
                FIXED_CLOCK
        );
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void recordsTheRequiredFieldsValidatesTheSessionAndPersistsTheScopeSnapshot() throws Exception {
        long activityLogId = activityLogService.record(
                1L,
                100L,
                "AUDIT_SESSION_SCOPE_TEST",
                "class_group",
                "50",
                "success",
                SOURCE_IP
        );

        ActivityLog log = activityLogDAO.findById(activityLogId).orElseThrow();
        assertEquals(1L, log.userId());
        assertEquals(100L, log.sessionId());
        assertEquals("AUDIT_SESSION_SCOPE_TEST", log.operationType());
        assertEquals("class_group", log.affectedEntityType());
        assertEquals("50", log.affectedEntityIdentifier());
        assertEquals(FIXED_OCCURRED_AT, log.occurredAt());
        assertEquals("success", log.outcome());
        assertEquals(SOURCE_IP, log.sourceIp());

        Map<Long, ActivityLogScope> persistedScopes = activityLogDAO.findScopesByActivityLogIds(List.of(activityLogId));
        ActivityLogScope scope = persistedScopes.get(activityLogId);
        assertEquals(Set.of(10L), scope.organizationIds());
        assertEquals(Set.of(40L), scope.subjectIds());
        assertEquals(Set.of(50L), scope.classGroupIds());
        assertEquals(Set.of(1L), scope.userIds());

        assertThrows(
                IllegalStateException.class,
                () -> activityLogService.record(
                        4L,
                        100L,
                        "AUDIT_INVALID_SESSION_TEST",
                        "user_account",
                        "4",
                        "denied",
                        SOURCE_IP
                )
        );
    }

    @Test
    void eachProfileSeesOnlyItsOwnOrAssignedAuditScope() {
        long managedOrganizationLog = recordForFixtureUser("AUDIT_ORGANIZATION_10", "organization", "10");
        long unmanagedOrganizationLog = recordForFixtureUser("AUDIT_ORGANIZATION_19", "organization", "19");
        long coordinatedSubjectLog = recordForFixtureUser("AUDIT_SUBJECT_40", "subject", "40");
        long foreignSubjectLog = recordForFixtureUser("AUDIT_SUBJECT_41", "subject", "41");
        long taughtClassGroupLog = recordForFixtureUser("AUDIT_CLASS_GROUP_50", "class_group", "50");
        long foreignClassGroupLog = recordForFixtureUser("AUDIT_CLASS_GROUP_52", "class_group", "52");
        long personalStudentLog = recordForFixtureUser("AUDIT_USER_4", "user_account", "4");
        long foreignStudentLog = recordForFixtureUser("AUDIT_USER_5", "user_account", "5");

        Set<Long> administratorLogIds = idsOf(activityLogService.queryForActor(
                1L,
                AccessProfileType.ADMINISTRATOR,
                ActivityLogQuery.all()
        ));
        assertTrue(administratorLogIds.contains(managedOrganizationLog));
        assertTrue(administratorLogIds.contains(coordinatedSubjectLog));
        assertTrue(administratorLogIds.contains(taughtClassGroupLog));
        assertFalse(administratorLogIds.contains(unmanagedOrganizationLog));

        Set<Long> coordinatorLogIds = idsOf(activityLogService.queryForActor(
                2L,
                AccessProfileType.COORDINATOR,
                ActivityLogQuery.all()
        ));
        assertTrue(coordinatorLogIds.contains(coordinatedSubjectLog));
        assertTrue(coordinatorLogIds.contains(taughtClassGroupLog));
        assertFalse(coordinatorLogIds.contains(foreignSubjectLog));
        assertFalse(coordinatorLogIds.contains(foreignClassGroupLog));

        Set<Long> teacherLogIds = idsOf(activityLogService.queryForActor(
                3L,
                AccessProfileType.TEACHER,
                ActivityLogQuery.all()
        ));
        assertTrue(teacherLogIds.contains(taughtClassGroupLog));
        assertFalse(teacherLogIds.contains(coordinatedSubjectLog));
        assertFalse(teacherLogIds.contains(foreignClassGroupLog));

        Set<Long> studentLogIds = idsOf(activityLogService.queryForActor(
                4L,
                AccessProfileType.STUDENT,
                ActivityLogQuery.all()
        ));
        assertTrue(studentLogIds.contains(personalStudentLog));
        assertFalse(studentLogIds.contains(foreignStudentLog));
        assertFalse(studentLogIds.contains(managedOrganizationLog));
        assertTrue(activityLogService.findVisibleById(
                3L,
                AccessProfileType.TEACHER,
                taughtClassGroupLog
        ).isPresent());
        assertTrue(activityLogService.findVisibleById(
                3L,
                AccessProfileType.TEACHER,
                foreignClassGroupLog
        ).isEmpty());
        assertThrows(
                SecurityException.class,
                () -> activityLogService.listForUserAudit(4L, AccessProfileType.STUDENT, 5L)
        );
    }

    @Test
    void exactFiltersCanNarrowResultsButNeverBroadenTheAuthorizedScope() {
        long coordinatedSubjectLog = recordForFixtureUser("FILTER_SUBJECT_40", "subject", "40");
        long foreignSubjectLog = recordForFixtureUser("FILTER_SUBJECT_41", "subject", "41");
        ActivityLogQuery visibleFilter = new ActivityLogQuery(
                5L,
                "FILTER_SUBJECT_40",
                "subject",
                "success",
                FIXED_OCCURRED_AT.minusSeconds(1),
                FIXED_OCCURRED_AT.plusSeconds(1)
        );
        ActivityLogQuery foreignFilter = new ActivityLogQuery(
                5L,
                "FILTER_SUBJECT_41",
                "subject",
                "success",
                FIXED_OCCURRED_AT.minusSeconds(1),
                FIXED_OCCURRED_AT.plusSeconds(1)
        );

        assertEquals(
                Set.of(coordinatedSubjectLog),
                idsOf(activityLogService.queryForActor(2L, AccessProfileType.COORDINATOR, visibleFilter))
        );
        assertTrue(activityLogService.queryForActor(
                2L,
                AccessProfileType.COORDINATOR,
                foreignFilter
        ).isEmpty());
        assertTrue(activityLogService.queryForActor(
                4L,
                AccessProfileType.STUDENT,
                new ActivityLogQuery(5L, null, null, null, null, null)
        ).isEmpty());
        assertFalse(activityLogService.queryForActor(
                2L,
                AccessProfileType.COORDINATOR,
                ActivityLogQuery.all()
        ).stream().map(ActivityLog::id).anyMatch(id -> id == foreignSubjectLog));
    }

    private long recordForFixtureUser(String operationType, String entityType, String entityIdentifier) {
        return activityLogService.record(
                5L,
                null,
                operationType,
                entityType,
                entityIdentifier,
                "success",
                SOURCE_IP
        );
    }

    private static Set<Long> idsOf(List<ActivityLog> logs) {
        return logs.stream().map(ActivityLog::id).collect(Collectors.toSet());
    }
}
