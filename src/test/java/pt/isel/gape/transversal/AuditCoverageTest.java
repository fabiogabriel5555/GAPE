package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.model.ActivityLog;
import pt.isel.gape.transversal.model.ActivityLogQuery;
import pt.isel.gape.transversal.model.ActivityLogScope;
import pt.isel.gape.transversal.service.AuditService;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class AuditCoverageTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-15T10:15:30Z"),
            ZoneOffset.UTC
    );
    private static final String SOURCE_IP = "127.0.0.1";

    private ActivityLogDAO activityLogDAO;
    private AuditService auditService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        DatabaseTestSupport.resetDatabaseWithBaseSeed();
    }

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;
        activityLogDAO = new ActivityLogDAO(connectionProvider);
        auditService = new AuditService(activityLogDAO, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws SQLException {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void canonicalAuditRecordAndTheNineCriticalAreasRemainCovered() throws Exception {
        auditService.record(
                1L,
                100L,
                "AUDIT_COVERAGE_CANONICAL",
                "class_group",
                "50",
                "success",
                SOURCE_IP
        );

        ActivityLog canonicalLog = activityLogDAO.find(new ActivityLogQuery(
                1L,
                "AUDIT_COVERAGE_CANONICAL",
                "class_group",
                "success",
                null,
                null
        )).stream().findFirst().orElseThrow();
        assertEquals(1L, canonicalLog.userId());
        assertEquals(100L, canonicalLog.sessionId());
        assertEquals(SOURCE_IP, canonicalLog.sourceIp());

        Map<Long, ActivityLogScope> scopes = activityLogDAO.findScopesByActivityLogIds(List.of(canonicalLog.id()));
        ActivityLogScope scope = scopes.get(canonicalLog.id());
        assertNotNull(scope);
        assertTrue(scope.organizationIds().contains(10L));
        assertTrue(scope.subjectIds().contains(40L));
        assertTrue(scope.classGroupIds().contains(50L));

        assertAuditHook("personal data", "src/main/java/pt/isel/gape/access/service/UserService.java", "USER_PERSONAL_READ");
        assertAuditHook("permissions", "src/main/java/pt/isel/gape/access/service/GrantService.java", "PERMISSION_GRANT");
        assertAuditHook("content", "src/main/java/pt/isel/gape/learning/service/ContentItemService.java", "CONTENT_ITEM_CREATE");
        assertAuditHook("attendance", "src/main/java/pt/isel/gape/learning/service/AttendanceRecordService.java", "ATTENDANCE_RECORD_CREATE");
        assertAuditHook("grades", "src/main/java/pt/isel/gape/learning/service/GradeSheetService.java", "GRADE_SHEET_CREATE");
        assertAuditHook("certificates", "src/main/java/pt/isel/gape/learning/service/CertificateService.java", "CERTIFICATE_SYNC");
        assertAuditHook("messages", "src/main/java/pt/isel/gape/transversal/service/MessageService.java", "MESSAGE_SEND");
        assertAuditHook("justifications", "src/main/java/pt/isel/gape/learning/service/AbsenceJustificationService.java", "ABSENCE_JUSTIFICATION_SUBMIT");
        assertAuditHook("deletion requests", "src/main/java/pt/isel/gape/access/service/DeletionRequestService.java", "DELETION_SUBMIT");
    }

    @Test
    void activityLogsAndTheirScopeSnapshotsCannotBeUpdatedOrDeleted() throws SQLException {
        long activityLogId = insertAuditLogForImmutabilityTest();

        assertImmutable(() -> executeUpdate(
                "UPDATE activity_log SET outcome = 'failure' WHERE id_activity_log = " + activityLogId
        ));
        assertImmutable(() -> executeUpdate(
                "DELETE FROM activity_log WHERE id_activity_log = " + activityLogId
        ));
        assertImmutable(() -> executeUpdate(
                "UPDATE activity_log_scope SET scope_id = 11 "
                        + "WHERE id_activity_log = " + activityLogId + " AND scope_type = 'ORGANIZATION'"
        ));
        assertImmutable(() -> executeUpdate(
                "DELETE FROM activity_log_scope WHERE id_activity_log = " + activityLogId
                        + " AND scope_type = 'ORGANIZATION'"
        ));
    }

    @Test
    void operationalSecurityDocumentationKeepsTheHttpsCipherAndBackupRequirements() throws Exception {
        String httpsDocumentation = readProjectFile("docs/security/https.md");
        assertTrue(httpsDocumentation.contains("GAPE_REQUIRE_HTTPS"));
        assertTrue(httpsDocumentation.contains("HttpsEnforcementFilter"));
        assertTrue(httpsDocumentation.contains("308"));
        assertTrue(httpsDocumentation.contains("Strict-Transport-Security"));
        assertTrue(httpsDocumentation.contains("sslMode=VERIFY_IDENTITY"));

        String sensitiveDataDocumentation = readProjectFile("docs/security/sensitive-data.md");
        assertTrue(sensitiveDataDocumentation.contains("GAPE_SENSITIVE_DATA_KEY"));
        assertTrue(sensitiveDataDocumentation.contains("AES-256-GCM"));
        assertTrue(sensitiveDataDocumentation.contains("gape:v1:"));
        assertTrue(sensitiveDataDocumentation.contains("HMAC-SHA-256"));
        assertTrue(sensitiveDataDocumentation.contains("user_account.document_number"));

        String backupDocumentation = readProjectFile("docs/security/backups.md");
        assertTrue(backupDocumentation.contains("backup-database.ps1"));
        assertTrue(backupDocumentation.contains("inferior a 7"));
        assertTrue(backupDocumentation.contains("-Daily"));
        assertTrue(backupDocumentation.contains("--single-transaction"));

        String backupScript = readProjectFile("docs/dev/scripts/backup-database.ps1");
        assertTrue(backupScript.contains("[ValidateRange(7, 3650)]"));
        assertTrue(backupScript.contains("--single-transaction"));
        assertTrue(backupScript.contains("--routines"));
        assertTrue(backupScript.contains("--events"));
        assertTrue(backupScript.contains("--triggers"));
        assertTrue(backupScript.contains("Get-FileHash"));
        assertTrue(backupScript.contains("Select-Object -First 7"));
    }

    private long insertAuditLogForImmutabilityTest() throws SQLException {
        auditService.record(
                1L,
                100L,
                "AUDIT_IMMUTABILITY_TEST",
                "class_group",
                "50",
                "success",
                SOURCE_IP
        );
        return activityLogDAO.find(new ActivityLogQuery(
                1L,
                "AUDIT_IMMUTABILITY_TEST",
                "class_group",
                "success",
                null,
                null
        )).stream().findFirst().orElseThrow().id();
    }

    private static void assertAuditHook(String area, String sourcePath, String operationType) throws Exception {
        String source = readProjectFile(sourcePath);
        assertTrue(source.contains("auditService.record"), () -> area + " must invoke AuditService");
        assertTrue(source.contains("\"" + operationType + "\""), () -> area + " must retain " + operationType);
    }

    private static String readProjectFile(String relativePath) throws Exception {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }

    private static void assertImmutable(SqlOperation operation) {
        SQLException exception = assertThrows(SQLException.class, operation::execute);
        DatabaseTestSupport.assertIntegrityException(exception);
        assertTrue(exception.getMessage().toLowerCase().contains("immutable"));
    }

    private static void executeUpdate(String sql) throws SQLException {
        try (Connection connection = DatabaseTestSupport.openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @FunctionalInterface
    private interface SqlOperation {
        void execute() throws SQLException;
    }
}
