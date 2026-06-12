package pt.isel.gape.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.dao.DeletionRequestDAO;
import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.DeletionRequestState;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.transversal.DatabaseTestSupport;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class DeletionRequestServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);
    private static final String SOURCE_IP = "127.0.0.1";

    private ConnectionProvider connectionProvider;
    private Object deletionRequestService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            resetSchema(connection);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        DatabaseTestSupport.beginTestTransaction();
        connectionProvider = DatabaseTestSupport::openConnection;

        deletionRequestService = newDeletionRequestService();
    }

    @AfterEach
    void tearDown() throws Exception {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void userSubmitsDeletionRequestAndAuditRecord() throws Exception {
        submitDeletionRequest(4L, "Right to erasure request");

        long requestId = latestDeletionRequestIdForUser(4L);
        assertEquals("submitted", deletionState(requestId));
        assertNotNull(submittedAt(requestId));
        assertEquals(1, countAudit("DELETION_SUBMIT", "success"));
    }

    @Test
    void nonAdministratorCannotProcessDeletionRequestAndDenialIsAudited() throws Exception {
        submitDeletionRequest(4L, "Student request");
        long requestId = latestDeletionRequestIdForUser(4L);

        assertThrows(
                SecurityException.class,
                () -> processDeletionRequest(
                        3L,
                        AccessProfileType.TEACHER,
                        requestId,
                        DeletionRequestState.APPROVED,
                        submittedAt(requestId).plusMinutes(5)
                )
        );

        assertEquals(1, countAudit("DELETION_PROCESS", "failure"));
    }

    @Test
    void nonAdministratorWithProcessPermissionCannotProcessDeletionRequest() throws Exception {
        grantTeacherProcessDeletionPermission();
        submitDeletionRequest(4L, "Student request");
        long requestId = latestDeletionRequestIdForUser(4L);

        assertThrows(
                SecurityException.class,
                () -> processDeletionRequest(
                        3L,
                        AccessProfileType.TEACHER,
                        requestId,
                        DeletionRequestState.APPROVED,
                        submittedAt(requestId).plusMinutes(5)
                )
        );

        assertEquals("submitted", deletionState(requestId));
        assertEquals(1, countAudit("DELETION_PROCESS", "failure"));
    }

    @Test
    void finalStateWithoutProcessingDateIsRejectedAndAudited() throws Exception {
        submitDeletionRequest(4L, "Student request");
        long requestId = latestDeletionRequestIdForUser(4L);

        assertThrows(
                IllegalArgumentException.class,
                () -> processDeletionRequest(
                        1L,
                        AccessProfileType.ADMINISTRATOR,
                        requestId,
                        DeletionRequestState.APPROVED,
                        null
                )
        );

        assertEquals("submitted", deletionState(requestId));
        assertEquals(1, countAudit("DELETION_PROCESS", "failure"));
    }

    @Test
    void processingDateCannotBeBeforeSubmissionDate() throws Exception {
        submitDeletionRequest(4L, "Student request");
        long requestId = latestDeletionRequestIdForUser(4L);

        assertThrows(
                IllegalArgumentException.class,
                () -> processDeletionRequest(
                        1L,
                        AccessProfileType.ADMINISTRATOR,
                        requestId,
                        DeletionRequestState.REJECTED,
                        submittedAt(requestId).minusMinutes(1)
                )
        );

        assertEquals("submitted", deletionState(requestId));
        assertEquals(1, countAudit("DELETION_PROCESS", "failure"));
    }

    @Test
    void administratorWithPermissionProcessesDeletionRequestAndAuditsSuccess() throws Exception {
        submitDeletionRequest(4L, "Student request");
        long requestId = latestDeletionRequestIdForUser(4L);
        LocalDateTime processedAt = submittedAt(requestId).plusMinutes(10);

        processDeletionRequest(
                1L,
                AccessProfileType.ADMINISTRATOR,
                requestId,
                DeletionRequestState.APPROVED,
                processedAt
        );

        assertEquals("approved", deletionState(requestId));
        assertEquals(1L, processorAdminUserId(requestId));
        assertEquals(processedAt, processedAt(requestId));
        assertEquals(1, countAudit("DELETION_PROCESS", "success"));
    }

    private Object newDeletionRequestService() {
        try {
            Class<?> type = Class.forName("pt.isel.gape.access.service.DeletionRequestService");
            try {
                Constructor<?> constructor = type.getConstructor(
                        DeletionRequestDAO.class,
                        UserDAO.class,
                        PermissionChecker.class,
                        AuditService.class,
                        Clock.class
                );
                return constructor.newInstance(
                        new DeletionRequestDAO(connectionProvider),
                        new UserDAO(connectionProvider),
                        new PermissionChecker(connectionProvider),
                        new AuditService(new ActivityLogDAO(connectionProvider), FIXED_CLOCK),
                        FIXED_CLOCK
                );
            } catch (NoSuchMethodException ignored) {
                try {
                    return type.getConstructor(ConnectionProvider.class, Clock.class)
                            .newInstance(connectionProvider, FIXED_CLOCK);
                } catch (NoSuchMethodException ignoredAgain) {
                    return type.getConstructor(ConnectionProvider.class)
                            .newInstance(connectionProvider);
                }
            }
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("Expected production class pt.isel.gape.access.service.DeletionRequestService", exception);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not create DeletionRequestService with the expected JDBC service constructors", exception);
        }
    }

    private void submitDeletionRequest(long actorUserId, String reason) {
        invoke(
                "submitDeletionRequest",
                new Class<?>[] { long.class, Long.class, String.class, String.class },
                actorUserId, null, reason, SOURCE_IP
        );
    }

    private void processDeletionRequest(
            long actorUserId,
            AccessProfileType actorProfileType,
            long requestId,
            DeletionRequestState state,
            LocalDateTime processedAt
    ) {
        invoke(
                "processDeletionRequest",
                new Class<?>[] {
                        long.class, Long.class, AccessProfileType.class, long.class,
                        DeletionRequestState.class, LocalDateTime.class, String.class
                },
                actorUserId, null, actorProfileType, requestId, state, processedAt, SOURCE_IP
        );
    }

    private Object invoke(String methodName, Class<?>[] parameterTypes, Object... arguments) {
        try {
            return deletionRequestService.getClass().getMethod(methodName, parameterTypes)
                    .invoke(deletionRequestService, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError("DeletionRequestService threw checked exception from " + methodName, cause);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("DeletionRequestService is missing expected method " + methodName, exception);
        }
    }

    private static void resetSchema(Connection connection) throws Exception {
        DatabaseTestSupport.dropCurrentSchemaObjects(connection);
        dropMinimalSchemaTables(connection);
        try (Statement statement = connection.createStatement()) {
            for (String sql : minimalSchemaStatements()) {
                statement.execute(sql);
            }
            for (String sql : minimalSeedStatements()) {
                statement.execute(sql);
            }
        }
    }

    private static void dropMinimalSchemaTables(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                for (String tableName : List.of(
                        "activity_log",
                        "deletion_request",
                        "grant_student",
                        "grant_teacher",
                        "grant_coordinator",
                        "grant_administrator",
                        "permission",
                        "user_session",
                        "student_profile",
                        "teacher_profile",
                        "coordinator_profile",
                        "administrator_profile",
                        "user_account"
                )) {
                    statement.execute("DROP TABLE IF EXISTS " + tableName);
                }
            } finally {
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
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
                CREATE TABLE deletion_request (
                    id_deletion BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                    submitter_user_id BIGINT UNSIGNED NOT NULL,
                    processor_admin_user_id BIGINT UNSIGNED NULL,
                    submitted_at DATETIME NOT NULL,
                    processed_at DATETIME NULL,
                    reason VARCHAR(300) NULL,
                    state VARCHAR(20) NOT NULL,
                    PRIMARY KEY (id_deletion),
                    CONSTRAINT fk_deletion_submitter
                        FOREIGN KEY (submitter_user_id) REFERENCES user_account (id_user)
                        ON UPDATE CASCADE ON DELETE RESTRICT,
                    CONSTRAINT fk_deletion_processor_admin
                        FOREIGN KEY (processor_admin_user_id) REFERENCES administrator_profile (id_user)
                        ON UPDATE CASCADE ON DELETE SET NULL
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
                    (3, 'Teacher User', 'teacher@gape.local', 'active', 'pt-PT', 'users/3/profile.webp', '2026-01-01 09:10:00', 'hash-teacher', 'salt-teacher'),
                    (4, 'Student User', 'student@gape.local', 'active', 'pt-PT', 'users/4/profile.webp', '2026-01-01 09:15:00', 'hash-student', 'salt-student')
                """,
                "INSERT INTO administrator_profile (id_user, cod_administrator) VALUES (1, 'ADM-001')",
                "INSERT INTO teacher_profile (id_user, cod_teacher) VALUES (3, 'TCH-001')",
                "INSERT INTO student_profile (id_user, cod_student) VALUES (4, 'STD-001')",
                """
                INSERT INTO permission (cod_permission, name, state) VALUES
                    ('MANAGE_ALL', 'Manage All', 'active')
                """,
                """
                INSERT INTO grant_administrator (id_admin_user, cod_permission) VALUES
                    (1, 'MANAGE_ALL')
                """
        );
    }

    private long latestDeletionRequestIdForUser(long userId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id_deletion
                     FROM deletion_request
                     WHERE submitter_user_id = ?
                     ORDER BY id_deletion DESC
                     LIMIT 1
                     """)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private String deletionState(long requestId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT state FROM deletion_request WHERE id_deletion = ?"
             )) {
            statement.setLong(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getString(1);
            }
        }
    }

    private LocalDateTime submittedAt(long requestId) throws Exception {
        return timestampValue(requestId, "submitted_at");
    }

    private LocalDateTime processedAt(long requestId) throws Exception {
        return timestampValue(requestId, "processed_at");
    }

    private LocalDateTime timestampValue(long requestId, String columnName) throws Exception {
        String sql = "SELECT " + columnName + " FROM deletion_request WHERE id_deletion = ?";
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getObject(1, LocalDateTime.class);
            }
        }
    }

    private long processorAdminUserId(long requestId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT processor_admin_user_id FROM deletion_request WHERE id_deletion = ?"
             )) {
            statement.setLong(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private int countAudit(String operationType, String outcome) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM activity_log WHERE operation_type = ? AND outcome = ?"
             )) {
            statement.setString(1, operationType);
            statement.setString(2, outcome);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private void grantTeacherProcessDeletionPermission() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO grant_teacher (id_teacher_user, cod_permission) VALUES (3, 'MANAGE_ALL')"
             )) {
            statement.executeUpdate();
        }
    }
}
