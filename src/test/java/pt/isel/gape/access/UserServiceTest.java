package pt.isel.gape.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import pt.isel.gape.access.model.AccessProfile;
import pt.isel.gape.access.model.AccessProfileContextAssignment;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.AdministratorPermissionAssignment;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.model.UserCreateCommand;
import pt.isel.gape.access.model.UserPersonalProfileUpdate;
import pt.isel.gape.access.model.UserState;
import pt.isel.gape.access.model.UserUpdateCommand;
import pt.isel.gape.access.service.UserService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.transversal.DatabaseTestSupport;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("gape-db")
class UserServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:15:30Z"), ZoneOffset.UTC);
    private static final String SOURCE_IP = "127.0.0.1";

    private ConnectionProvider connectionProvider;
    private UserService userService;

    @BeforeAll
    static void initializeDatabase() throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            resetSchema(connection);
            DatabaseTestSupport.executeScript(connection, DatabaseTestSupport.SQL_SEED_DIR.resolve("base.sql"));
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        DatabaseTestSupport.beginTestTransaction();
        connectionProvider = DatabaseTestSupport::openConnection;

        userService = new UserService(connectionProvider, FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() throws Exception {
        DatabaseTestSupport.rollbackTestTransaction();
    }

    @Test
    void administratorCreatesValidUserAndAuditRecord() throws Exception {
        User created = userService.createUser(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                newUserCommand("Valid User", "valid.user@gape.local", "CITIZEN_CARD", "000000000ZZ4", "STD-F5-001"),
                SOURCE_IP
        );

        assertNotNull(created);
        assertTrue(created.id() > 5L);
        assertEquals("Valid User", created.name());
        assertEquals("valid.user@gape.local", created.email());
        assertEquals(UserState.ACTIVE, created.state());
        assertEquals("CITIZEN_CARD", created.documentType());
        assertEquals("000000000ZZ4", created.documentNumber());
        assertTrue(created.accessProfiles().contains(new AccessProfile(AccessProfileType.STUDENT, "STD-F5-001")));
        assertEquals(1, countAudit("USER_CREATE", "success"));
    }

    @Test
    void administratorCreatesUserWithGeneratedProfileCodeWhenCodeIsNotSubmitted() throws Exception {
        User created = userService.createUser(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new UserCreateCommand(
                        "Generated Profile Code",
                        "generated.profile@gape.local",
                        UserState.ACTIVE,
                        "pt-PT",
                        null,
                        "test-hash",
                        "test-salt",
                        null,
                        null,
                        Set.of(new AccessProfile(AccessProfileType.STUDENT, ""))
                ),
                SOURCE_IP
        );

        assertTrue(created.accessProfiles().contains(new AccessProfile(AccessProfileType.STUDENT, "STD-%06d".formatted(created.id()))));
    }

    @Test
    void administratorProfileDoesNotGetAdminGrantsUnlessExplicitlyAssigned() throws Exception {
        User created = userService.createUser(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new UserCreateCommand(
                        "New Administrator",
                        "new.admin@gape.local",
                        UserState.ACTIVE,
                        "pt-PT",
                        null,
                        "test-hash",
                        "test-salt",
                        null,
                        null,
                        Set.of(new AccessProfile(AccessProfileType.ADMINISTRATOR, "ADM-FULL-001"))
                ),
                SOURCE_IP
        );

        assertTrue(created.accessProfiles().contains(new AccessProfile(AccessProfileType.ADMINISTRATOR, "ADM-FULL-001")));
        assertEquals(0, countGrant("grant_administrator", "id_admin_user", created.id(), "MANAGE_ALL"));
        assertEquals(0, countGrant("grant_administrator", "id_admin_user", created.id(), "MANAGE_ORGANIZATION_STRUCTURE"));
    }

    @Test
    void administratorCanCreateAnotherAdministratorAssignedToManagedOrganization() throws Exception {
        User created = userService.createUser(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new UserCreateCommand(
                        "Managed Administrator",
                        "managed.admin@gape.local",
                        UserState.ACTIVE,
                        "pt-PT",
                        null,
                        "test-hash",
                        "test-salt",
                        null,
                        null,
                        Set.of(new AccessProfile(AccessProfileType.ADMINISTRATOR, "ADM-MANAGED-001"))
                ),
                Set.of(10L),
                SOURCE_IP
        );

        assertTrue(created.accessProfiles().contains(new AccessProfile(AccessProfileType.ADMINISTRATOR, "ADM-MANAGED-001")));
        assertTrue(hasActiveOrganizationAssignment(created.id(), 10L));
    }

    @Test
    void manageAllAdministratorCanAssignAnotherAdministratorToAnyNonInactiveOrganization() throws Exception {
        User created = userService.createUser(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new UserCreateCommand(
                        "Unmanaged Administrator",
                        "unmanaged.admin@gape.local",
                        UserState.ACTIVE,
                        "pt-PT",
                        null,
                        "test-hash",
                        "test-salt",
                        null,
                        null,
                        Set.of(new AccessProfile(AccessProfileType.ADMINISTRATOR, "ADM-UNMANAGED-001"))
                ),
                Set.of(11L),
                SOURCE_IP
        );

        assertTrue(hasActiveOrganizationAssignment(created.id(), 11L));
    }

    @Test
    void administratorCanUpdateAnotherAdministratorManagedOrganizationAssignments() throws Exception {
        User created = userService.createUser(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                new UserCreateCommand(
                        "Assignment Edit Admin",
                        "assignment.edit.admin@gape.local",
                        UserState.ACTIVE,
                        "pt-PT",
                        null,
                        "test-hash",
                        "test-salt",
                        null,
                        null,
                        Set.of(new AccessProfile(AccessProfileType.ADMINISTRATOR, "ADM-ASSIGN-001"))
                ),
                SOURCE_IP
        );

        userService.updateUser(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                created.id(),
                new UserUpdateCommand(
                        "Assignment Edit Admin",
                        "assignment.edit.admin@gape.local",
                        UserState.ACTIVE,
                        "pt-PT",
                        null,
                        null,
                        null,
                        Set.of(new AccessProfile(AccessProfileType.ADMINISTRATOR, "ADM-ASSIGN-001"))
                ),
                Set.of(10L),
                SOURCE_IP
        );

        assertTrue(hasActiveOrganizationAssignment(created.id(), 10L));
    }

    @Test
    void administratorProfileRequiresExactlyOneExplicitAdministratorPermission() {
        UserCreateCommand noPermissionCommand = adminCommand(
                "Admin Missing Permission",
                "admin.missing.permission@gape.local",
                "ADM-MISSING-PERMISSION"
        );
        IllegalArgumentException missing = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        noPermissionCommand,
                        List.of(),
                        List.of(),
                        SOURCE_IP
                )
        );
        assertTrue(missing.getMessage().contains("exactly one administrator permission"));

        UserCreateCommand multiplePermissionCommand = adminCommand(
                "Admin Multiple Permissions",
                "admin.multiple.permissions@gape.local",
                "ADM-MULTIPLE-PERMISSIONS"
        );
        IllegalArgumentException multiple = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        multiplePermissionCommand,
                        List.of(
                                AdministratorPermissionAssignment.manageAll(),
                                new AdministratorPermissionAssignment(
                                        AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE,
                                        AccessEntityType.ORGANIZATION,
                                        10L
                                )
                        ),
                        List.of(),
                        SOURCE_IP
                )
        );
        assertTrue(multiple.getMessage().contains("exactly one administrator permission"));
    }

    @Test
    void subjectCoordinatorAssignmentsCanOnlyBeManagedFromSubjectDetails() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new UserCreateCommand(
                                "Conflicting Profile Context",
                                "conflicting.profile.context@gape.local",
                                UserState.ACTIVE,
                                "pt-PT",
                                null,
                                "test-hash",
                                "test-salt",
                                null,
                                null,
                                Set.of(
                                        new AccessProfile(AccessProfileType.COORDINATOR, "COO-CONFLICT-001"),
                                        new AccessProfile(AccessProfileType.TEACHER, "TCH-CONFLICT-001")
                                )
                        ),
                        List.of(),
                        List.of(
                                new AccessProfileContextAssignment(
                                        AccessProfileType.COORDINATOR,
                                        AccessEntityType.SUBJECT,
                                        40L,
                                        null
                                ),
                                new AccessProfileContextAssignment(
                                        AccessProfileType.TEACHER,
                                        AccessEntityType.CLASS_GROUP,
                                        50L,
                                        null
                                )
                        ),
                        SOURCE_IP
                )
        );

        assertTrue(exception.getMessage().contains("managed exclusively from Subject Details"));
    }

    @Test
    void studentProfileContextOnlyAcceptsCourses() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new AccessProfileContextAssignment(
                        AccessProfileType.STUDENT,
                        AccessEntityType.SUBJECT,
                        40L,
                        30L
                )
        );

        assertEquals("Student context must be a course", exception.getMessage());
        AccessProfileContextAssignment courseContext = new AccessProfileContextAssignment(
                AccessProfileType.STUDENT,
                AccessEntityType.COURSE,
                30L,
                99L
        );
        assertNull(courseContext.parentContextId());
    }

    @Test
    void administratorOrganizationContextConflictsWithOtherProfileInsideSameOrganization() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new UserCreateCommand(
                                "Admin Student Conflict",
                                "admin.student.conflict@gape.local",
                                UserState.ACTIVE,
                                "pt-PT",
                                null,
                                "test-hash",
                                "test-salt",
                                null,
                                null,
                                Set.of(
                                        new AccessProfile(AccessProfileType.ADMINISTRATOR, "ADM-STUDENT-CONFLICT"),
                                        new AccessProfile(AccessProfileType.STUDENT, "STD-ADMIN-CONFLICT")
                                )
                        ),
                        List.of(new AdministratorPermissionAssignment(
                                AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE,
                                AccessEntityType.ORGANIZATION,
                                10L
                        )),
                        List.of(new AccessProfileContextAssignment(
                                AccessProfileType.STUDENT,
                                AccessEntityType.COURSE,
                                30L,
                                null
                        )),
                        SOURCE_IP
                )
        );

        assertTrue(exception.getMessage().contains("multiple access profiles"));
    }

    @Test
    void duplicateEmailIsRejectedBeforeInsertAndAudited() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        newUserCommand("Duplicate Email", "admin@gape.local", "PASSPORT", "AA123456", "STD-F5-002"),
                        SOURCE_IP
                )
        );

        assertEquals(1, countAudit("USER_CREATE", "failure"));
        assertEquals(1, countUsersByEmail("admin@gape.local"));
    }

    @Test
    void duplicateDocumentIsRejectedBeforeInsertAndAudited() throws Exception {
        userService.createUser(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                newUserCommand("Document Owner", "document.owner@gape.local", "PASSPORT", "AB123456", "STD-F5-003"),
                SOURCE_IP
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        newUserCommand("Document Duplicate", "document.duplicate@gape.local", "PASSPORT", "AB123456", "STD-F5-004"),
                        SOURCE_IP
                )
        );

        assertEquals(1, countAudit("USER_CREATE", "failure"));
        assertEquals(1, countUsersByDocument("PASSPORT", "AB123456"));
    }

    @Test
    void missingRequiredFieldsAndPartialDocumentAreRejectedAndAudited() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new UserCreateCommand(
                                " ",
                                "missing-name@gape.local",
                                UserState.ACTIVE,
                                "pt-PT",
                                null,
                                "hash",
                                "salt",
                                "PASSPORT",
                                "AC123456",
                                Set.of(new AccessProfile(AccessProfileType.STUDENT, "STD-F5-005"))
                        ),
                        SOURCE_IP
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        newUserCommand("Partial Document", "partial.document@gape.local", "CITIZEN_CARD", null, "STD-F5-006"),
                        SOURCE_IP
                )
        );

        assertEquals(2, countAudit("USER_CREATE", "failure"));
    }

    @Test
    void unsupportedLanguageIsRejectedAndAudited() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        new UserCreateCommand(
                                "Unsupported Language",
                                "unsupported.language@gape.local",
                                UserState.ACTIVE,
                                "en-GB",
                                null,
                                "hash",
                                "salt",
                                null,
                                null,
                                Set.of(new AccessProfile(AccessProfileType.STUDENT, "STD-F5-009"))
                        ),
                        SOURCE_IP
                )
        );

        assertEquals(1, countAudit("USER_CREATE", "failure"));
    }

    @Test
    void unsupportedDocumentTypeIsRejectedAndAudited() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        newUserCommand("Unsupported Document", "unsupported.document@gape.local", "CUSTOM_DOC", "DOC-9010", "STD-F5-010"),
                        SOURCE_IP
                )
        );

        assertEquals(1, countAudit("USER_CREATE", "failure"));
    }

    @Test
    void invalidDocumentNumberIsRejectedAndAudited() throws Exception {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        newUserCommand("Invalid Document", "invalid.document@gape.local", "PASSPORT", "P12345!", "STD-F5-011"),
                        SOURCE_IP
                )
        );

        assertTrue(exception.getMessage().contains("Invalid document number"));
        assertEquals(1, countAudit("USER_CREATE", "failure"));
    }

    @Test
    void administratorBlocksAndUnblocksUserWithAuditRecords() throws Exception {
        User created = userService.createUser(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                newUserCommand("Block Target", "block.target@gape.local", null, null, "STD-F5-007"),
                SOURCE_IP
        );

        User blocked = userService.blockUser(1L, null, AccessProfileType.ADMINISTRATOR, created.id(), SOURCE_IP);
        User unblocked = userService.unblockUser(1L, null, AccessProfileType.ADMINISTRATOR, created.id(), SOURCE_IP);

        assertEquals(UserState.BLOCKED, blocked.state());
        assertEquals(UserState.ACTIVE, unblocked.state());
        assertEquals(1, countAudit("USER_BLOCK", "success"));
        assertEquals(1, countAudit("USER_UNBLOCK", "success"));
    }

    @Test
    void replacingProfileDoesNotCreateProfilePermissionGrants() throws Exception {
        addSpareAdministratorForOrganizations(6L, 10L, 11L);

        User updated = userService.updateUser(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                1L,
                new UserUpdateCommand(
                        "Admin As Student",
                        "admin@gape.local",
                        UserState.ACTIVE,
                        "pt-PT",
                        "users/1/profile.webp",
                        null,
                        null,
                        Set.of(new AccessProfile(AccessProfileType.STUDENT, "STD-ADM-SWITCH"))
                ),
                SOURCE_IP
        );

        assertTrue(updated.accessProfiles().contains(new AccessProfile(AccessProfileType.STUDENT, "STD-ADM-SWITCH")));
        assertEquals(0, countGrant("grant_student", "id_student_user", 1L, "VIEW_REPORTS"));
        assertEquals(0, countGrant("grant_administrator", "id_admin_user", 1L, "MANAGE_ALL"));
    }

    @Test
    void deletingOnlyManageAllAdministratorIsRejected() throws Exception {
        addSpareAdministratorForOrganizations(6L, false, 10L);

        assertThrows(
                RuntimeException.class,
                () -> userService.deleteUser(1L, null, AccessProfileType.ADMINISTRATOR, 1L, SOURCE_IP)
        );
    }

    @Test
    void inactivatingOnlyManageAllAdministratorIsRejected() throws Exception {
        addSpareAdministratorForOrganizations(6L, false, 10L);

        assertThrows(
                RuntimeException.class,
                () -> userService.inactivateUser(1L, null, AccessProfileType.ADMINISTRATOR, 1L, SOURCE_IP)
        );
    }

    @Test
    void removingOnlyManageAllAssignmentIsRejected() throws Exception {
        addSpareAdministratorForOrganizations(6L, false, 10L);

        assertThrows(
                RuntimeException.class,
                () -> userService.updateUser(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        1L,
                        new UserUpdateCommand(
                                "Admin User",
                                "admin@gape.local",
                                UserState.ACTIVE,
                                "pt-PT",
                                "users/1/profile.webp",
                                null,
                                null,
                                Set.of(new AccessProfile(AccessProfileType.ADMINISTRATOR, "ADM-001"))
                        ),
                        List.of(new AdministratorPermissionAssignment(
                                AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE,
                                AccessEntityType.ORGANIZATION,
                                10L
                        )),
                        SOURCE_IP
                )
        );
    }

    @Test
    void removingLastActiveOrganizationAdministratorIsRejected() {
        assertThrows(
                RuntimeException.class,
                () -> userService.updateUser(
                        1L,
                        null,
                        AccessProfileType.ADMINISTRATOR,
                        1L,
                        new UserUpdateCommand(
                                "Admin As Student",
                                "admin@gape.local",
                                UserState.ACTIVE,
                                "pt-PT",
                                "users/1/profile.webp",
                                null,
                                null,
                                Set.of(new AccessProfile(AccessProfileType.STUDENT, "STD-ADM-SWITCH"))
                        ),
                        SOURCE_IP
                )
        );
    }

    @Test
    void userEditsOwnPersonalProfileWithoutGlobalPersonalDataPermission() throws Exception {
        User created = userService.createUser(
                1L,
                null,
                AccessProfileType.ADMINISTRATOR,
                newUserCommand("Profile Owner", "profile.owner@gape.local", "TAX_IDENTIFICATION_NUMBER", "503504564", "STD-F5-008"),
                SOURCE_IP
        );

        User updated = userService.editPersonalProfile(
                created.id(),
                null,
                AccessProfileType.STUDENT,
                created.id(),
                new UserPersonalProfileUpdate(
                        "Profile Owner Updated",
                        "profile.owner.updated@gape.local",
                        "en-US",
                        "users/test/profile.webp",
                        "TAX_IDENTIFICATION_NUMBER",
                        "503504564"
                ),
                SOURCE_IP
        );

        assertEquals("Profile Owner Updated", updated.name());
        assertEquals("profile.owner.updated@gape.local", updated.email());
        assertEquals("en-US", updated.language());
        assertEquals(1, countAudit("USER_PROFILE_UPDATE", "success"));
    }

    @Test
    void unauthorizedProfileCannotReadAnotherUsersPersonalDataAndDenialIsAudited() throws Exception {
        assertThrows(
                SecurityException.class,
                () -> userService.readPersonalData(3L, null, AccessProfileType.TEACHER, 4L, SOURCE_IP)
        );

        assertEquals(1, countAudit("USER_PERSONAL_READ", "failure"));
    }

    private static UserCreateCommand newUserCommand(
            String name,
            String email,
            String documentType,
            String documentNumber,
            String studentCode
    ) {
        return new UserCreateCommand(
                name,
                email,
                UserState.ACTIVE,
                "pt-PT",
                null,
                "test-hash",
                "test-salt",
                documentType,
                documentNumber,
                Set.of(new AccessProfile(AccessProfileType.STUDENT, studentCode))
        );
    }

    private static UserCreateCommand adminCommand(String name, String email, String administratorCode) {
        return new UserCreateCommand(
                name,
                email,
                UserState.ACTIVE,
                "pt-PT",
                null,
                "test-hash",
                "test-salt",
                null,
                null,
                Set.of(new AccessProfile(AccessProfileType.ADMINISTRATOR, administratorCode))
        );
    }

    private static void resetSchema(Connection connection) throws Exception {
        DatabaseTestSupport.dropCurrentSchemaObjects(connection);
        executeServiceTestSchema(connection);
    }

    private static void executeServiceTestSchema(Connection connection) throws Exception {
        List<String> statements = DatabaseTestSupport.parseSqlStatements(DatabaseTestSupport.SQL_DIR.resolve("schema.sql"));
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                if (!isSkippedTrigger(sql)) {
                    statement.execute(sql);
                }
            }
        }
    }

    private static boolean isSkippedTrigger(String sql) {
        String normalized = sql.stripLeading().toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("CREATE TRIGGER ")) {
            return false;
        }
        return !(normalized.startsWith("CREATE TRIGGER BI_ACTIVITY_LOG_VALIDATE ")
                || normalized.startsWith("CREATE TRIGGER BI_DELETION_REQUEST_VALIDATE ")
                || normalized.startsWith("CREATE TRIGGER BU_DELETION_REQUEST_VALIDATE "));
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

    private int countUsersByEmail(String email) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM user_account WHERE email = ?"
             )) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private int countUsersByDocument(String documentType, String documentNumber) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM user_account
                     WHERE document_type = ? AND document_number = ?
                     """)) {
            statement.setString(1, documentType);
            statement.setString(2, documentNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private int countGrant(String tableName, String userColumn, long userId, String permission) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + tableName + " WHERE " + userColumn + " = ? AND cod_permission = ?"
             )) {
            statement.setLong(1, userId);
            statement.setString(2, permission);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private boolean hasActiveOrganizationAssignment(long adminUserId, long organizationId) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM manage_organization
                     WHERE id_admin_user = ?
                       AND id_organization = ?
                       AND state = 'active'
                     """)) {
            statement.setLong(1, adminUserId);
            statement.setLong(2, organizationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private void addSpareAdministratorForOrganizations(long userId, long... organizationIds) throws Exception {
        addSpareAdministratorForOrganizations(userId, true, organizationIds);
    }

    private void addSpareAdministratorForOrganizations(
            long userId,
            boolean grantManageAll,
            long... organizationIds
    ) throws Exception {
        try (Connection connection = DatabaseTestSupport.openConnection()) {
            try (PreparedStatement user = connection.prepareStatement("""
                    INSERT INTO user_account (
                        id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt
                    ) VALUES (?, 'Spare Admin', 'spare.admin@gape.local', 'active', 'pt-PT', NULL,
                              '2026-01-01 10:00:00', 'hash', 'salt')
                    """)) {
                user.setLong(1, userId);
                user.executeUpdate();
            }
            try (PreparedStatement profile = connection.prepareStatement("""
                    INSERT INTO administrator_profile (id_user, cod_administrator)
                    VALUES (?, ?)
                    """)) {
                profile.setLong(1, userId);
                profile.setString(2, "ADM-SPARE-" + userId);
                profile.executeUpdate();
            }
            try (PreparedStatement assignment = connection.prepareStatement("""
                    INSERT INTO manage_organization (id_admin_user, id_organization, state, start_date, end_date)
                    VALUES (?, ?, 'active', '2026-01-01', NULL)
                    """)) {
                for (long organizationId : organizationIds) {
                    assignment.setLong(1, userId);
                    assignment.setLong(2, organizationId);
                    assignment.executeUpdate();
                }
            }
            if (grantManageAll) {
                try (PreparedStatement grant = connection.prepareStatement("""
                        INSERT INTO grant_administrator (id_admin_user, cod_permission, context_type, context_id)
                        VALUES (?, 'MANAGE_ALL', 'GLOBAL', 0)
                        """)) {
                    grant.setLong(1, userId);
                    grant.executeUpdate();
                }
            }
        }
    }
}
