package pt.isel.gape.transversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
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
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.model.ActivityLog;
import pt.isel.gape.transversal.service.ActivityLogService;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class ActivityLogServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);

    private ActivityLogService activityLogService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            resetSchema(connection);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        DatabaseTestSupport.beginTestTransaction();
        ConnectionProvider connectionProvider = DatabaseTestSupport::openConnection;

        activityLogService = new ActivityLogService(
                new ActivityLogDAO(connectionProvider),
                new PermissionChecker(connectionProvider),
                FIXED_CLOCK
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void recordsActivityLogWithMinimumFields() {
        long id = activityLogService.record(
                1L,
                100L,
                "USER_UPDATE",
                "user_account",
                "4",
                "success",
                "127.0.0.1"
        );

        ActivityLog log = activityLogService.findById(id).orElseThrow();

        assertEquals(1L, log.userId());
        assertEquals(100L, log.sessionId());
        assertEquals("USER_UPDATE", log.operationType());
        assertEquals("4", log.affectedEntityIdentifier());
        assertEquals("success", log.outcome());
    }

    @Test
    void sessionMustBelongToSameUser() {
        assertThrows(
                IllegalStateException.class,
                () -> activityLogService.record(
                        4L,
                        100L,
                        "USER_UPDATE",
                        "user_account",
                        "4",
                        "failure",
                        "127.0.0.1"
                )
        );
    }

    @Test
    void administratorCanListGlobalLogAndStudentOnlyOwnLog() {
        activityLogService.record(4L, null, "USER_PROFILE_UPDATE", "user_account", "4", "success", "127.0.0.1");

        List<ActivityLog> adminLogs = activityLogService.listForActor(1L, AccessProfileType.ADMINISTRATOR);
        List<ActivityLog> studentLogs = activityLogService.listForActor(4L, AccessProfileType.STUDENT);

        assertTrue(adminLogs.size() >= 2);
        assertEquals(1, studentLogs.size());
        assertEquals(4L, studentLogs.getFirst().userId());
    }

    @Test
    void requiredFieldsAreValidatedBeforeInsert() {
        assertThrows(
                IllegalArgumentException.class,
                () -> activityLogService.record(1L, null, " ", "user_account", "4", "success", "127.0.0.1")
        );
    }

    @Test
    void criticalAuditIdentifiersDoNotExposeDirectPersonalData() {
        activityLogService.record(1L, null, "DELETION_PROCESS", "deletion_request", "1", "success", "127.0.0.1");
        activityLogService.record(1L, null, "USER_PROFILE_UPDATE", "user_account", "4", "success", "127.0.0.1");
        activityLogService.record(1L, null, "USER_BLOCK", "user_account", "4", "success", "127.0.0.1");

        List<ActivityLog> logs = activityLogService.listForActor(1L, AccessProfileType.ADMINISTRATOR);

        for (ActivityLog log : logs) {
            if (isCriticalOperation(log.operationType())) {
                String identifier = log.affectedEntityIdentifier();
                assertTrue(!identifier.contains("@"));
                assertTrue(!identifier.toLowerCase().contains("reason"));
                assertTrue(!identifier.toLowerCase().contains("doc-"));
            }
        }
    }

    private static boolean isCriticalOperation(String operationType) {
        return operationType.startsWith("USER_")
                || operationType.startsWith("DELETION_");
    }

    private static void resetSchema(Connection connection) throws Exception {
        DatabaseTestSupport.dropCurrentSchemaObjects(connection);
        try (Statement statement = connection.createStatement()) {
            for (String sql : minimalSchemaStatements()) {
                statement.execute(sql);
            }
            for (String sql : minimalSeedStatements()) {
                statement.execute(sql);
            }
        }
    }

    private static List<String> minimalSchemaStatements() {
        return List.of(
                """
                CREATE TABLE user_account (
                    id_user BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                    name VARCHAR(120) NOT NULL,
                    email VARCHAR(160) NOT NULL,
                    state VARCHAR(20) NOT NULL,
                    language VARCHAR(10) NOT NULL,
                    photo VARCHAR(255) NULL,
                    created_at DATETIME NOT NULL,
                    credential_hash VARCHAR(255) NOT NULL,
                    credential_salt VARCHAR(255) NOT NULL,
                    document_type VARCHAR(40) NULL,
                    document_number VARCHAR(40) NULL,
                    PRIMARY KEY (id_user),
                    UNIQUE KEY uq_user_account_email (email),
                    UNIQUE KEY uq_user_account_document (document_type, document_number)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """,
                """
                CREATE TABLE administrator_profile (
                    id_user BIGINT UNSIGNED NOT NULL,
                    cod_administrator VARCHAR(40) NOT NULL,
                    PRIMARY KEY (id_user),
                    UNIQUE KEY uq_administrator_profile_code (cod_administrator),
                    CONSTRAINT fk_administrator_profile_user
                        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
                        ON UPDATE CASCADE ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """,
                """
                CREATE TABLE coordinator_profile (
                    id_user BIGINT UNSIGNED NOT NULL,
                    cod_coordinator VARCHAR(40) NOT NULL,
                    PRIMARY KEY (id_user),
                    UNIQUE KEY uq_coordinator_profile_code (cod_coordinator),
                    CONSTRAINT fk_coordinator_profile_user
                        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
                        ON UPDATE CASCADE ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """,
                """
                CREATE TABLE teacher_profile (
                    id_user BIGINT UNSIGNED NOT NULL,
                    cod_teacher VARCHAR(40) NOT NULL,
                    PRIMARY KEY (id_user),
                    UNIQUE KEY uq_teacher_profile_code (cod_teacher),
                    CONSTRAINT fk_teacher_profile_user
                        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
                        ON UPDATE CASCADE ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """,
                """
                CREATE TABLE student_profile (
                    id_user BIGINT UNSIGNED NOT NULL,
                    cod_student VARCHAR(40) NOT NULL,
                    PRIMARY KEY (id_user),
                    UNIQUE KEY uq_student_profile_code (cod_student),
                    CONSTRAINT fk_student_profile_user
                        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
                        ON UPDATE CASCADE ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """,
                """
                CREATE TABLE user_session (
                    id_session BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                    id_user BIGINT UNSIGNED NOT NULL,
                    token VARCHAR(255) NOT NULL,
                    state VARCHAR(20) NOT NULL,
                    start_at DATETIME NOT NULL,
                    last_activity DATETIME NOT NULL,
                    end_at DATETIME NULL,
                    PRIMARY KEY (id_session),
                    UNIQUE KEY uq_user_session_token (token),
                    CONSTRAINT fk_user_session_user
                        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
                        ON UPDATE CASCADE ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """,
                """
                CREATE TABLE permission (
                    cod_permission VARCHAR(80) NOT NULL,
                    name VARCHAR(120) NOT NULL,
                    state VARCHAR(20) NOT NULL,
                    PRIMARY KEY (cod_permission)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """,
                """
                CREATE TABLE grant_administrator (
                    id_admin_user BIGINT UNSIGNED NOT NULL,
                    cod_permission VARCHAR(80) NOT NULL,
                    context_type VARCHAR(30) NOT NULL DEFAULT 'GLOBAL',
                    context_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
                    PRIMARY KEY (id_admin_user, cod_permission, context_type, context_id),
                    KEY idx_grant_administrator_permission (cod_permission),
                    KEY idx_grant_administrator_context (context_type, context_id),
                    CONSTRAINT fk_grant_administrator_admin
                        FOREIGN KEY (id_admin_user) REFERENCES administrator_profile (id_user)
                        ON UPDATE CASCADE ON DELETE CASCADE,
                    CONSTRAINT fk_grant_administrator_permission
                        FOREIGN KEY (cod_permission) REFERENCES permission (cod_permission)
                        ON UPDATE CASCADE ON DELETE CASCADE,
                    CONSTRAINT ck_grant_administrator_context_type
                        CHECK (context_type IN ('GLOBAL', 'ORGANIZATION', 'ORGANIC_UNIT', 'COURSE', 'SUBJECT', 'CLASS_GROUP'))
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """,
                """
                CREATE TABLE grant_coordinator (
                    id_coordinator_user BIGINT UNSIGNED NOT NULL,
                    cod_permission VARCHAR(80) NOT NULL,
                    PRIMARY KEY (id_coordinator_user, cod_permission),
                    CONSTRAINT fk_grant_coordinator_user
                        FOREIGN KEY (id_coordinator_user) REFERENCES coordinator_profile (id_user)
                        ON UPDATE CASCADE ON DELETE CASCADE,
                    CONSTRAINT fk_grant_coordinator_permission
                        FOREIGN KEY (cod_permission) REFERENCES permission (cod_permission)
                        ON UPDATE CASCADE ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """,
                """
                CREATE TABLE grant_teacher (
                    id_teacher_user BIGINT UNSIGNED NOT NULL,
                    cod_permission VARCHAR(80) NOT NULL,
                    PRIMARY KEY (id_teacher_user, cod_permission),
                    CONSTRAINT fk_grant_teacher_user
                        FOREIGN KEY (id_teacher_user) REFERENCES teacher_profile (id_user)
                        ON UPDATE CASCADE ON DELETE CASCADE,
                    CONSTRAINT fk_grant_teacher_permission
                        FOREIGN KEY (cod_permission) REFERENCES permission (cod_permission)
                        ON UPDATE CASCADE ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """,
                """
                CREATE TABLE grant_student (
                    id_student_user BIGINT UNSIGNED NOT NULL,
                    cod_permission VARCHAR(80) NOT NULL,
                    PRIMARY KEY (id_student_user, cod_permission),
                    CONSTRAINT fk_grant_student_user
                        FOREIGN KEY (id_student_user) REFERENCES student_profile (id_user)
                        ON UPDATE CASCADE ON DELETE CASCADE,
                    CONSTRAINT fk_grant_student_permission
                        FOREIGN KEY (cod_permission) REFERENCES permission (cod_permission)
                        ON UPDATE CASCADE ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """,
                """
                CREATE TABLE activity_log (
                    id_activity_log BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                    id_user BIGINT UNSIGNED NULL,
                    id_session BIGINT UNSIGNED NULL,
                    operation_type VARCHAR(80) NOT NULL,
                    affected_entity_type VARCHAR(80) NOT NULL,
                    affected_entity_identifier VARCHAR(120) NOT NULL,
                    occurred_at DATETIME NOT NULL,
                    outcome VARCHAR(20) NOT NULL,
                    source_ip VARCHAR(45) NULL,
                    PRIMARY KEY (id_activity_log),
                    CONSTRAINT fk_activity_log_user
                        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
                        ON UPDATE CASCADE ON DELETE SET NULL,
                    CONSTRAINT fk_activity_log_session
                        FOREIGN KEY (id_session) REFERENCES user_session (id_session)
                        ON UPDATE CASCADE ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """,
                """
                CREATE TRIGGER bi_activity_log_validate
                BEFORE INSERT ON activity_log
                FOR EACH ROW
                BEGIN
                    IF NEW.id_session IS NOT NULL
                       AND NEW.id_user IS NOT NULL
                       AND NOT EXISTS (
                           SELECT 1
                           FROM user_session
                           WHERE id_session = NEW.id_session
                             AND id_user = NEW.id_user
                       ) THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Activity_Log Session must belong to the same User';
                    END IF;
                END
                """
        );
    }

    private static List<String> minimalSeedStatements() {
        return List.of(
                """
                INSERT INTO user_account (
                    id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt
                ) VALUES
                    (1, 'Admin User', 'admin@gape.local', 'active', 'pt-PT', 'users/1/profile.webp', '2026-01-01 09:00:00', 'hash-admin', 'salt-admin'),
                    (4, 'Student User', 'student@gape.local', 'active', 'pt-PT', 'users/4/profile.webp', '2026-01-01 09:15:00', 'hash-student', 'salt-student')
                """,
                "INSERT INTO administrator_profile (id_user, cod_administrator) VALUES (1, 'ADM-001')",
                "INSERT INTO student_profile (id_user, cod_student) VALUES (4, 'STD-001')",
                "INSERT INTO user_session (id_session, id_user, token, state, start_at, last_activity, end_at) VALUES (100, 1, 'tok-admin-100', 'active', '2026-01-10 10:00:00', '2026-01-10 10:30:00', NULL)",
                "INSERT INTO permission (cod_permission, name, state) VALUES ('MANAGE_ALL', 'Manage All', 'active')",
                "INSERT INTO grant_administrator (id_admin_user, cod_permission) VALUES (1, 'MANAGE_ALL')",
                """
                INSERT INTO activity_log (
                    id_activity_log, id_user, id_session, operation_type, affected_entity_type,
                    affected_entity_identifier, occurred_at, outcome, source_ip
                ) VALUES
                    (232, 1, 100, 'MANAGE_USERS', 'user_account', '4', '2026-02-12 13:00:00', 'success', '127.0.0.1')
                """
        );
    }
}
