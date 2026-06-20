package pt.isel.gape.access.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.dao.ProfileDAO;
import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.access.model.AccessProfileContextAssignment;
import pt.isel.gape.access.model.AdministratorPermissionAssignment;
import pt.isel.gape.access.model.AccessProfile;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.access.model.UserCreateCommand;
import pt.isel.gape.access.model.UserPersonalProfileUpdate;
import pt.isel.gape.access.model.UserState;
import pt.isel.gape.access.model.UserUpdateCommand;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.config.SupportedDocumentTypeCatalog;
import pt.isel.gape.common.config.SupportedLanguageCatalog;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.common.validation.PortugueseDocumentNumberValidator;
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationDecision;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class UserService {

    private final UserDAO userDAO;
    private final ProfileDAO profileDAO;
    private final PermissionDAO permissionDAO;
    private final PermissionChecker permissionChecker;
    private final AuditService auditService;
    private final ManageOrganizationDAO manageOrganizationDAO;
    private final Clock clock;
    private final ConnectionProvider connectionProvider;

    public UserService(UserDAO userDAO) {
        this(userDAO, null, null, null, null, ApplicationClock.system());
    }

    public UserService(
            UserDAO userDAO,
            ProfileDAO profileDAO,
            PermissionDAO permissionDAO,
            PermissionChecker permissionChecker,
            AuditService auditService,
            Clock clock
    ) {
        this(null, userDAO, profileDAO, permissionDAO, permissionChecker, auditService, null, clock);
    }

    private UserService(
            ConnectionProvider connectionProvider,
            UserDAO userDAO,
            ProfileDAO profileDAO,
            PermissionDAO permissionDAO,
            PermissionChecker permissionChecker,
            AuditService auditService,
            ManageOrganizationDAO manageOrganizationDAO,
            Clock clock
    ) {
        this.connectionProvider = connectionProvider;
        this.userDAO = Objects.requireNonNull(userDAO, "userDAO is required");
        this.profileDAO = profileDAO;
        this.permissionDAO = permissionDAO;
        this.permissionChecker = permissionChecker;
        this.auditService = auditService;
        this.manageOrganizationDAO = manageOrganizationDAO;
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public UserService(ConnectionProvider connectionProvider) {
        this(connectionProvider, ApplicationClock.system());
    }

    public UserService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new UserDAO(connectionProvider),
                new ProfileDAO(connectionProvider),
                new PermissionDAO(connectionProvider),
                new PermissionChecker(connectionProvider),
                new AuditService(new ActivityLogDAO(connectionProvider), clock),
                new ManageOrganizationDAO(connectionProvider),
                clock
        );
    }

    public Optional<User> findByEmail(String email) {
        try {
            return userDAO.findByEmail(email);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load user by email", exception);
        }
    }

    public Optional<User> findById(long userId) {
        try {
            return userDAO.findById(userId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load user by id", exception);
        }
    }

    public List<User> listUsers(long actorUserId, Long sessionId, AccessProfileType actorProfileType, String sourceIp) {
        requireCrudDependencies();
        try {
            requireManageUsers(actorUserId, actorProfileType);
            List<User> users = userDAO.findAll();
            record(actorUserId, sessionId, "USER_LIST", "user_account", "all", "success", sourceIp);
            return users;
        } catch (SQLException exception) {
            record(actorUserId, sessionId, "USER_LIST", "user_account", "all", "failure", sourceIp);
            throw new IllegalStateException("Failed to list users", exception);
        } catch (RuntimeException exception) {
            record(actorUserId, sessionId, "USER_LIST", "user_account", "all", "failure", sourceIp);
            throw exception;
        }
    }

    public User readPersonalData(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long targetUserId,
            String sourceIp
    ) {
        requireCrudDependencies();
        try {
            requireReadPersonalData(actorUserId, actorProfileType, targetUserId);
            User user = requireUser(targetUserId);
            record(actorUserId, sessionId, "USER_PERSONAL_READ", "user_account", Long.toString(targetUserId), "success", sourceIp);
            return user;
        } catch (RuntimeException exception) {
            record(actorUserId, sessionId, "USER_PERSONAL_READ", "user_account", Long.toString(targetUserId), "failure", sourceIp);
            throw exception;
        }
    }

    public User createUser(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            UserCreateCommand command,
            String sourceIp
    ) {
        return createUser(actorUserId, sessionId, actorProfileType, command, null, null, sourceIp);
    }

    public User createUser(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            UserCreateCommand command,
            Set<Long> managedOrganizationIds,
            String sourceIp
    ) {
        return createUser(
                actorUserId,
                sessionId,
                actorProfileType,
                command,
                organizationScopeAssignments(managedOrganizationIds),
                null,
                sourceIp
        );
    }

    public User createUser(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            UserCreateCommand command,
            List<AdministratorPermissionAssignment> adminPermissionAssignments,
            String sourceIp
    ) {
        return createUser(
                actorUserId,
                sessionId,
                actorProfileType,
                command,
                adminPermissionAssignments,
                null,
                sourceIp
        );
    }

    public User createUser(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            UserCreateCommand command,
            List<AdministratorPermissionAssignment> adminPermissionAssignments,
            List<AccessProfileContextAssignment> profileContextAssignments,
            String sourceIp
    ) {
        requireCrudDependencies();
        try {
            requireManageUsers(actorUserId, actorProfileType);
            UserCreateCommand normalizedCommand = validateCreateCommand(command);
            if (connectionProvider != null) {
                return inTransaction(connection -> {
                    ensureUniqueEmail(connection, normalizedCommand.email(), null);
                    ensureUniqueDocument(connection, normalizedCommand.documentType(), normalizedCommand.documentNumber(), null);
                    Set<AdministratorPermissionAssignment> selectedAdminAssignments = adminPermissionAssignments == null
                            ? null
                            : normalizeAdminPermissionAssignments(adminPermissionAssignments);
                    Set<AccessProfileContextAssignment> selectedProfileContexts = profileContextAssignments == null
                            ? null
                            : normalizeProfileContextAssignments(profileContextAssignments);
                    validateNoOverlappingProfileScopes(
                            connection,
                            normalizedCommand.accessProfiles(),
                            selectedAdminAssignments,
                            selectedProfileContexts
                    );
                    long userId = userDAO.create(connection, normalizedCommand, LocalDateTime.now(clock));
                    profileDAO.replaceProfiles(connection, userId, profileCodesForSave(userId, Set.of(), normalizedCommand.accessProfiles()));
                    synchronizeAdministratorPermissionAssignments(
                            connection,
                            actorUserId,
                            userId,
                            normalizedCommand.state(),
                            normalizedCommand.accessProfiles(),
                            selectedAdminAssignments,
                            sessionId,
                            actorProfileType,
                            sourceIp
                    );
                    synchronizeAccessProfileContextAssignments(
                            connection,
                            userId,
                            normalizedCommand.state(),
                            normalizedCommand.accessProfiles(),
                            selectedProfileContexts
                    );
                    record(connection, actorUserId, sessionId, "USER_CREATE", "user_account", Long.toString(userId), "success", sourceIp);
                    return requireUser(connection, userId);
                });
            }
            if (adminPermissionAssignments != null && !adminPermissionAssignments.isEmpty()) {
                throw new IllegalStateException("Administrator permission assignments require transactional user service");
            }
            if (profileContextAssignments != null && !profileContextAssignments.isEmpty()) {
                throw new IllegalStateException("Access profile context assignments require transactional user service");
            }
            ensureUniqueEmail(normalizedCommand.email(), null);
            ensureUniqueDocument(normalizedCommand.documentType(), normalizedCommand.documentNumber(), null);
            long userId = userDAO.create(normalizedCommand, LocalDateTime.now(clock));
            profileDAO.replaceProfiles(userId, profileCodesForSave(userId, Set.of(), normalizedCommand.accessProfiles()));
            record(actorUserId, sessionId, "USER_CREATE", "user_account", Long.toString(userId), "success", sourceIp);
            return requireUser(userId);
        } catch (SQLException exception) {
            record(actorUserId, sessionId, "USER_CREATE", "user_account", affectedIdentifier(command), "failure", sourceIp);
            throw new IllegalStateException("Failed to create user", exception);
        } catch (RuntimeException exception) {
            record(actorUserId, sessionId, "USER_CREATE", "user_account", affectedIdentifier(command), "failure", sourceIp);
            throw exception;
        }
    }

    public User attachCreatedUserPhoto(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long targetUserId,
            String photo,
            String sourceIp
    ) {
        requireCrudDependencies();
        try {
            requireManageUsers(actorUserId, actorProfileType);
            String normalizedPhoto = optional(photo);
            if (connectionProvider != null) {
                return inTransaction(connection -> {
                    requireUser(connection, targetUserId);
                    if (!userDAO.updatePhoto(connection, targetUserId, normalizedPhoto)) {
                        throw new IllegalArgumentException("Unknown user: " + targetUserId);
                    }
                    record(connection, actorUserId, sessionId, "USER_UPDATE", "user_account", Long.toString(targetUserId), "success", sourceIp);
                    return requireUser(connection, targetUserId);
                });
            }
            requireUser(targetUserId);
            if (!userDAO.updatePhoto(targetUserId, normalizedPhoto)) {
                throw new IllegalArgumentException("Unknown user: " + targetUserId);
            }
            record(actorUserId, sessionId, "USER_UPDATE", "user_account", Long.toString(targetUserId), "success", sourceIp);
            return requireUser(targetUserId);
        } catch (SQLException exception) {
            record(actorUserId, sessionId, "USER_UPDATE", "user_account", Long.toString(targetUserId), "failure", sourceIp);
            throw new IllegalStateException("Failed to attach user photo " + targetUserId, exception);
        } catch (RuntimeException exception) {
            record(actorUserId, sessionId, "USER_UPDATE", "user_account", Long.toString(targetUserId), "failure", sourceIp);
            throw exception;
        }
    }

    public User updateUser(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long targetUserId,
            UserUpdateCommand command,
            String sourceIp
    ) {
        return updateUser(actorUserId, sessionId, actorProfileType, targetUserId, command, null, null, sourceIp);
    }

    public User updateUser(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long targetUserId,
            UserUpdateCommand command,
            Set<Long> managedOrganizationIds,
            String sourceIp
    ) {
        return updateUser(
                actorUserId,
                sessionId,
                actorProfileType,
                targetUserId,
                command,
                organizationScopeAssignments(managedOrganizationIds),
                null,
                sourceIp
        );
    }

    public User updateUser(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long targetUserId,
            UserUpdateCommand command,
            List<AdministratorPermissionAssignment> adminPermissionAssignments,
            String sourceIp
    ) {
        return updateUser(
                actorUserId,
                sessionId,
                actorProfileType,
                targetUserId,
                command,
                adminPermissionAssignments,
                null,
                sourceIp
        );
    }

    public User updateUser(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long targetUserId,
            UserUpdateCommand command,
            List<AdministratorPermissionAssignment> adminPermissionAssignments,
            List<AccessProfileContextAssignment> profileContextAssignments,
            String sourceIp
    ) {
        requireCrudDependencies();
        try {
            requireManageUsers(actorUserId, actorProfileType);
            UserUpdateCommand normalizedCommand = validateUpdateCommand(command);
            if (connectionProvider != null) {
                return inTransaction(connection -> {
                    User existingUser = requireUser(connection, targetUserId);
                    ensureUniqueEmail(connection, normalizedCommand.email(), targetUserId);
                    ensureUniqueDocument(connection, normalizedCommand.documentType(), normalizedCommand.documentNumber(), targetUserId);
                    Set<AdministratorPermissionAssignment> selectedAdminAssignments = adminPermissionAssignments == null
                            ? null
                            : normalizeAdminPermissionAssignments(adminPermissionAssignments);
                    Set<AccessProfileContextAssignment> selectedProfileContexts = profileContextAssignments == null
                            ? null
                            : normalizeProfileContextAssignments(profileContextAssignments);
                    validateNoOverlappingProfileScopes(
                            connection,
                            normalizedCommand.accessProfiles(),
                            selectedAdminAssignments,
                            selectedProfileContexts
                    );
                    if (!userDAO.update(connection, targetUserId, normalizedCommand)) {
                        throw new IllegalArgumentException("Unknown user: " + targetUserId);
                    }
                    profileDAO.replaceProfiles(
                            connection,
                            targetUserId,
                            profileCodesForSave(targetUserId, existingUser.accessProfiles(), normalizedCommand.accessProfiles())
                    );
                    synchronizeAdministratorPermissionAssignments(
                            connection,
                            actorUserId,
                            targetUserId,
                            normalizedCommand.state(),
                            normalizedCommand.accessProfiles(),
                            selectedAdminAssignments,
                            sessionId,
                            actorProfileType,
                            sourceIp
                    );
                    synchronizeAccessProfileContextAssignments(
                            connection,
                            targetUserId,
                            normalizedCommand.state(),
                            normalizedCommand.accessProfiles(),
                            selectedProfileContexts
                    );
                    ensureAllActiveOrganizationsHaveAdministrators(connection);
                    ensureActiveManageAllAdministratorExists(connection);
                    record(connection, actorUserId, sessionId, "USER_UPDATE", "user_account", Long.toString(targetUserId), "success", sourceIp);
                    return requireUser(connection, targetUserId);
                });
            }
            if (adminPermissionAssignments != null && !adminPermissionAssignments.isEmpty()) {
                throw new IllegalStateException("Administrator permission assignments require transactional user service");
            }
            if (profileContextAssignments != null && !profileContextAssignments.isEmpty()) {
                throw new IllegalStateException("Access profile context assignments require transactional user service");
            }
            User existingUser = requireUser(targetUserId);
            ensureUniqueEmail(normalizedCommand.email(), targetUserId);
            ensureUniqueDocument(normalizedCommand.documentType(), normalizedCommand.documentNumber(), targetUserId);
            if (!userDAO.update(targetUserId, normalizedCommand)) {
                throw new IllegalArgumentException("Unknown user: " + targetUserId);
            }
            profileDAO.replaceProfiles(
                    targetUserId,
                    profileCodesForSave(targetUserId, existingUser.accessProfiles(), normalizedCommand.accessProfiles())
            );
            record(actorUserId, sessionId, "USER_UPDATE", "user_account", Long.toString(targetUserId), "success", sourceIp);
            return requireUser(targetUserId);
        } catch (SQLException exception) {
            record(actorUserId, sessionId, "USER_UPDATE", "user_account", Long.toString(targetUserId), "failure", sourceIp);
            throw new IllegalStateException("Failed to update user " + targetUserId, exception);
        } catch (RuntimeException exception) {
            record(actorUserId, sessionId, "USER_UPDATE", "user_account", Long.toString(targetUserId), "failure", sourceIp);
            throw exception;
        }
    }

    public User editPersonalProfile(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long targetUserId,
            UserPersonalProfileUpdate update,
            String sourceIp
    ) {
        requireCrudDependencies();
        try {
            requireManagePersonalData(actorUserId, actorProfileType, targetUserId);
            UserPersonalProfileUpdate normalizedUpdate = validatePersonalProfileUpdate(update);
            if (connectionProvider != null) {
                return inTransaction(connection -> {
                    requireUser(connection, targetUserId);
                    ensureUniqueEmail(connection, normalizedUpdate.email(), targetUserId);
                    ensureUniqueDocument(connection, normalizedUpdate.documentType(), normalizedUpdate.documentNumber(), targetUserId);
                    boolean updated = userDAO.updatePersonalProfile(
                            connection,
                            targetUserId,
                            normalizedUpdate.name(),
                            normalizedUpdate.email(),
                            normalizedUpdate.language(),
                            normalizedUpdate.photo(),
                            normalizedUpdate.documentType(),
                            normalizedUpdate.documentNumber()
                    );
                    if (!updated) {
                        throw new IllegalArgumentException("Unknown user: " + targetUserId);
                    }
                    record(connection, actorUserId, sessionId, "USER_PROFILE_UPDATE", "user_account", Long.toString(targetUserId), "success", sourceIp);
                    return requireUser(connection, targetUserId);
                });
            }
            requireUser(targetUserId);
            ensureUniqueEmail(normalizedUpdate.email(), targetUserId);
            ensureUniqueDocument(normalizedUpdate.documentType(), normalizedUpdate.documentNumber(), targetUserId);
            boolean updated = userDAO.updatePersonalProfile(
                    targetUserId,
                    normalizedUpdate.name(),
                    normalizedUpdate.email(),
                    normalizedUpdate.language(),
                    normalizedUpdate.photo(),
                    normalizedUpdate.documentType(),
                    normalizedUpdate.documentNumber()
            );
            if (!updated) {
                throw new IllegalArgumentException("Unknown user: " + targetUserId);
            }
            record(actorUserId, sessionId, "USER_PROFILE_UPDATE", "user_account", Long.toString(targetUserId), "success", sourceIp);
            return requireUser(targetUserId);
        } catch (SQLException exception) {
            record(actorUserId, sessionId, "USER_PROFILE_UPDATE", "user_account", Long.toString(targetUserId), "failure", sourceIp);
            throw new IllegalStateException("Failed to update personal profile " + targetUserId, exception);
        } catch (RuntimeException exception) {
            record(actorUserId, sessionId, "USER_PROFILE_UPDATE", "user_account", Long.toString(targetUserId), "failure", sourceIp);
            throw exception;
        }
    }

    public User changePassword(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long targetUserId,
            String credentialHash,
            String credentialSalt,
            String sourceIp
    ) {
        requireCrudDependencies();
        try {
            requireManagePersonalData(actorUserId, actorProfileType, targetUserId);
            String normalizedHash = required(credentialHash, "credentialHash");
            String normalizedSalt = required(credentialSalt, "credentialSalt");
            if (connectionProvider != null) {
                return inTransaction(connection -> {
                    requireUser(connection, targetUserId);
                    if (!userDAO.updateCredentials(connection, targetUserId, normalizedHash, normalizedSalt)) {
                        throw new IllegalArgumentException("Unknown user: " + targetUserId);
                    }
                    record(connection, actorUserId, sessionId, "USER_PASSWORD_UPDATE", "user_account", Long.toString(targetUserId), "success", sourceIp);
                    return requireUser(connection, targetUserId);
                });
            }
            requireUser(targetUserId);
            if (!userDAO.updateCredentials(targetUserId, normalizedHash, normalizedSalt)) {
                throw new IllegalArgumentException("Unknown user: " + targetUserId);
            }
            record(actorUserId, sessionId, "USER_PASSWORD_UPDATE", "user_account", Long.toString(targetUserId), "success", sourceIp);
            return requireUser(targetUserId);
        } catch (SQLException exception) {
            record(actorUserId, sessionId, "USER_PASSWORD_UPDATE", "user_account", Long.toString(targetUserId), "failure", sourceIp);
            throw new IllegalStateException("Failed to update password " + targetUserId, exception);
        } catch (RuntimeException exception) {
            record(actorUserId, sessionId, "USER_PASSWORD_UPDATE", "user_account", Long.toString(targetUserId), "failure", sourceIp);
            throw exception;
        }
    }

    public User blockUser(long actorUserId, Long sessionId, AccessProfileType actorProfileType, long targetUserId, String sourceIp) {
        return changeState(actorUserId, sessionId, actorProfileType, targetUserId, UserState.BLOCKED, "USER_BLOCK", sourceIp);
    }

    public User unblockUser(long actorUserId, Long sessionId, AccessProfileType actorProfileType, long targetUserId, String sourceIp) {
        return changeState(actorUserId, sessionId, actorProfileType, targetUserId, UserState.ACTIVE, "USER_UNBLOCK", sourceIp);
    }

    public User activateUser(long actorUserId, Long sessionId, AccessProfileType actorProfileType, long targetUserId, String sourceIp) {
        return changeState(actorUserId, sessionId, actorProfileType, targetUserId, UserState.ACTIVE, "USER_ACTIVATE", sourceIp);
    }

    public User inactivateUser(long actorUserId, Long sessionId, AccessProfileType actorProfileType, long targetUserId, String sourceIp) {
        return changeState(actorUserId, sessionId, actorProfileType, targetUserId, UserState.INACTIVE, "USER_INACTIVATE", sourceIp);
    }

    public void deleteUser(long actorUserId, Long sessionId, AccessProfileType actorProfileType, long targetUserId, String sourceIp) {
        requireCrudDependencies();
        try {
            requireManageUsers(actorUserId, actorProfileType);
            if (connectionProvider != null) {
                inTransaction(connection -> {
                    requireUser(connection, targetUserId);
                    if (!userDAO.delete(connection, targetUserId)) {
                        throw new IllegalArgumentException("Unknown user: " + targetUserId);
                    }
                    ensureAllActiveOrganizationsHaveAdministrators(connection);
                    ensureActiveManageAllAdministratorExists(connection);
                    record(connection, actorUserId, sessionId, "USER_DELETE", "user_account", Long.toString(targetUserId), "success", sourceIp);
                    return null;
                });
                return;
            }
            requireUser(targetUserId);
            if (!userDAO.delete(targetUserId)) {
                throw new IllegalArgumentException("Unknown user: " + targetUserId);
            }
            record(actorUserId, sessionId, "USER_DELETE", "user_account", Long.toString(targetUserId), "success", sourceIp);
        } catch (SQLException exception) {
            record(actorUserId, sessionId, "USER_DELETE", "user_account", Long.toString(targetUserId), "failure", sourceIp);
            throw new IllegalStateException("Failed to delete user " + targetUserId, exception);
        } catch (RuntimeException exception) {
            record(actorUserId, sessionId, "USER_DELETE", "user_account", Long.toString(targetUserId), "failure", sourceIp);
            throw exception;
        }
    }

    private User changeState(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long targetUserId,
            UserState state,
            String operationType,
            String sourceIp
    ) {
        requireCrudDependencies();
        try {
            requireManageUsers(actorUserId, actorProfileType);
            if (connectionProvider != null) {
                return inTransaction(connection -> {
                    requireUser(connection, targetUserId);
                    if (!userDAO.updateState(connection, targetUserId, state)) {
                        throw new IllegalArgumentException("Unknown user: " + targetUserId);
                    }
                    ensureAllActiveOrganizationsHaveAdministrators(connection);
                    ensureActiveManageAllAdministratorExists(connection);
                    record(connection, actorUserId, sessionId, operationType, "user_account", Long.toString(targetUserId), "success", sourceIp);
                    return requireUser(connection, targetUserId);
                });
            }
            requireUser(targetUserId);
            if (!userDAO.updateState(targetUserId, state)) {
                throw new IllegalArgumentException("Unknown user: " + targetUserId);
            }
            record(actorUserId, sessionId, operationType, "user_account", Long.toString(targetUserId), "success", sourceIp);
            return requireUser(targetUserId);
        } catch (SQLException exception) {
            record(actorUserId, sessionId, operationType, "user_account", Long.toString(targetUserId), "failure", sourceIp);
            throw new IllegalStateException("Failed to change user state " + targetUserId, exception);
        } catch (RuntimeException exception) {
            record(actorUserId, sessionId, operationType, "user_account", Long.toString(targetUserId), "failure", sourceIp);
            throw exception;
        }
    }

    private User requireUser(long userId) {
        return findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + userId));
    }

    private User requireUser(Connection connection, long userId) throws SQLException {
        return userDAO.findById(connection, userId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + userId));
    }

    private void ensureUniqueEmail(String email, Long excludingUserId) throws SQLException {
        if (userDAO.existsEmail(email, excludingUserId)) {
            throw new IllegalArgumentException("Duplicate email: " + email);
        }
    }

    private void ensureUniqueEmail(Connection connection, String email, Long excludingUserId) throws SQLException {
        if (userDAO.existsEmail(connection, email, excludingUserId)) {
            throw new IllegalArgumentException("Duplicate email: " + email);
        }
    }

    private void ensureUniqueDocument(String documentType, String documentNumber, Long excludingUserId) throws SQLException {
        if (documentType != null && userDAO.existsDocument(documentType, documentNumber, excludingUserId)) {
            throw new IllegalArgumentException("Duplicate document: " + documentType + "/" + documentNumber);
        }
    }

    private void ensureUniqueDocument(
            Connection connection,
            String documentType,
            String documentNumber,
            Long excludingUserId
    ) throws SQLException {
        if (documentType != null && userDAO.existsDocument(connection, documentType, documentNumber, excludingUserId)) {
            throw new IllegalArgumentException("Duplicate document: " + documentType + "/" + documentNumber);
        }
    }

    private UserCreateCommand validateCreateCommand(UserCreateCommand command) {
        Objects.requireNonNull(command, "command is required");
        String[] document = normalizeDocumentType(command.documentType(), command.documentNumber());
        return new UserCreateCommand(
                required(command.name(), "name"),
                required(command.email(), "email"),
                Objects.requireNonNull(command.state(), "state is required"),
                supportedLanguage(command.language()),
                optional(command.photo()),
                required(command.credentialHash(), "credentialHash"),
                required(command.credentialSalt(), "credentialSalt"),
                document[0],
                document[1],
                validateProfiles(command.accessProfiles())
        );
    }

    private UserUpdateCommand validateUpdateCommand(UserUpdateCommand command) {
        Objects.requireNonNull(command, "command is required");
        String[] document = normalizeDocumentType(command.documentType(), command.documentNumber());
        return new UserUpdateCommand(
                required(command.name(), "name"),
                required(command.email(), "email"),
                Objects.requireNonNull(command.state(), "state is required"),
                supportedLanguage(command.language()),
                optional(command.photo()),
                document[0],
                document[1],
                validateProfiles(command.accessProfiles())
        );
    }

    private UserPersonalProfileUpdate validatePersonalProfileUpdate(UserPersonalProfileUpdate update) {
        Objects.requireNonNull(update, "update is required");
        String[] document = normalizeDocumentType(update.documentType(), update.documentNumber());
        return new UserPersonalProfileUpdate(
                required(update.name(), "name"),
                required(update.email(), "email"),
                supportedLanguage(update.language()),
                optional(update.photo()),
                document[0],
                document[1]
        );
    }

    private static Set<AccessProfile> validateProfiles(Set<AccessProfile> accessProfiles) {
        Objects.requireNonNull(accessProfiles, "accessProfiles are required");
        for (AccessProfile profile : accessProfiles) {
            if (profile.code() == null) {
                throw new IllegalArgumentException("profile code is required");
            }
        }
        Set<AccessProfileType> types = accessProfiles.stream()
                .map(AccessProfile::type)
                .collect(Collectors.toSet());
        if (types.size() != accessProfiles.size()) {
            throw new IllegalArgumentException("Duplicate profile type for user");
        }
        return Set.copyOf(accessProfiles);
    }

    private static Set<AccessProfile> profileCodesForSave(
            long userId,
            Set<AccessProfile> existingProfiles,
            Set<AccessProfile> selectedProfiles
    ) {
        return selectedProfiles.stream()
                .map(profile -> new AccessProfile(
                        profile.type(),
                        profileCodeForSave(userId, existingProfiles, profile)
                ))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static String profileCodeForSave(long userId, Set<AccessProfile> existingProfiles, AccessProfile selectedProfile) {
        String submittedCode = optional(selectedProfile.code());
        if (submittedCode != null) {
            return submittedCode;
        }
        return existingProfiles.stream()
                .filter(profile -> profile.type() == selectedProfile.type())
                .map(AccessProfile::code)
                .findFirst()
                .orElseGet(() -> generatedProfileCode(userId, selectedProfile.type()));
    }

    private static String generatedProfileCode(long userId, AccessProfileType profileType) {
        String prefix = switch (profileType) {
            case ADMINISTRATOR -> "ADM";
            case COORDINATOR -> "COO";
            case TEACHER -> "TCH";
            case STUDENT -> "STD";
        };
        return "%s-%06d".formatted(prefix, userId);
    }

    private static String[] normalizeDocumentType(String documentType, String documentNumber) {
        String normalizedType = optional(documentType);
        String normalizedNumber = optional(documentNumber);
        if ((normalizedType == null) != (normalizedNumber == null)) {
            throw new IllegalArgumentException("Document type and number must be both filled or both empty");
        }
        if (normalizedType != null && !SupportedDocumentTypeCatalog.contains(normalizedType)) {
            throw new IllegalArgumentException("Unsupported document type: " + normalizedType);
        }
        if (normalizedType != null) {
            normalizedNumber = PortugueseDocumentNumberValidator.normalize(normalizedType, normalizedNumber);
        }
        return new String[] { normalizedType, normalizedNumber };
    }

    private void requireReadPersonalData(long actorUserId, AccessProfileType actorProfileType, long targetUserId) {
        if (actorUserId == targetUserId) {
            return;
        }
        requirePermission(actorUserId, actorProfileType, AuthorizationPolicy.VIEW_PERSONAL_DATA, "VIEW_PERSONAL_DATA permission is required");
    }

    private void requireManagePersonalData(long actorUserId, AccessProfileType actorProfileType, long targetUserId) {
        if (actorUserId == targetUserId) {
            return;
        }
        requirePermission(actorUserId, actorProfileType, AuthorizationPolicy.MANAGE_PERSONAL_DATA, "MANAGE_PERSONAL_DATA permission is required");
    }

    private void requireManageUsers(long actorUserId, AccessProfileType actorProfileType) {
        Objects.requireNonNull(actorProfileType, "actorProfileType is required");
        if (actorProfileType != AccessProfileType.ADMINISTRATOR) {
            throw new SecurityException("Administrator profile is required to manage users");
        }
        if (permissionChecker.hasPermission(actorUserId, actorProfileType, AuthorizationPolicy.MANAGE_ALL)) {
            return;
        }
        throw new SecurityException("MANAGE_ALL permission is required");
    }

    private void requirePermission(long actorUserId, AccessProfileType actorProfileType, String permissionCode, String errorMessage) {
        Objects.requireNonNull(actorProfileType, "actorProfileType is required");
        if (!permissionChecker.hasPermission(actorUserId, actorProfileType, permissionCode)) {
            throw new SecurityException(errorMessage);
        }
    }

    private void requireCrudDependencies() {
        if (profileDAO == null || permissionDAO == null || permissionChecker == null || auditService == null) {
            throw new IllegalStateException("UserService was created without CRUD dependencies");
        }
    }

    private void ensureAllActiveOrganizationsHaveAdministrators(Connection connection) throws SQLException {
        if (manageOrganizationDAO == null) {
            return;
        }
        List<Long> organizationIds = manageOrganizationDAO.findActiveOrganizationsWithoutActiveAdministrator(connection);
        if (!organizationIds.isEmpty()) {
            throw new IllegalStateException("Active organizations require at least one active administrator: "
                    + organizationIds);
        }
    }

    private void ensureActiveManageAllAdministratorExists(Connection connection) throws SQLException {
        if (!permissionDAO.hasActiveGlobalManageAllAdministrator(connection)) {
            throw new IllegalStateException("The system requires at least one active administrator with MANAGE_ALL");
        }
    }

    private void synchronizeAdministratorPermissionAssignments(
            Connection connection,
            long actorUserId,
            long targetUserId,
            UserState targetState,
            Set<AccessProfile> targetProfiles,
            Collection<AdministratorPermissionAssignment> selectedAssignments,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) throws SQLException {
        if (selectedAssignments == null) {
            return;
        }
        if (manageOrganizationDAO == null) {
            throw new IllegalStateException("Administrator permission assignments require role assignment dependencies");
        }

        Set<AdministratorPermissionAssignment> selected = normalizeAdminPermissionAssignments(selectedAssignments);
        boolean targetIsAdministrator = hasProfile(targetProfiles, AccessProfileType.ADMINISTRATOR);
        if (!targetIsAdministrator) {
            if (!selected.isEmpty()) {
                throw new IllegalArgumentException("Administrator permission assignments require an administrator profile");
            }
            permissionDAO.deleteAdministratorAssignments(connection, targetUserId);
            return;
        }
        requireExactlyOneAdministratorPermission(selected);
        if (!selected.isEmpty() && targetState != UserState.ACTIVE) {
            throw new IllegalArgumentException("Administrator permission assignments require an active administrator");
        }

        for (AdministratorPermissionAssignment assignment : selected) {
            requireDelegableAssignment(actorUserId, sessionId, actorProfileType, assignment, sourceIp);
        }

        permissionDAO.synchronizeAdministratorAssignments(connection, targetUserId, selected);
        synchronizeOrganizationResponsibilityAssignments(connection, actorUserId, targetUserId, selected);
    }

    private void synchronizeAccessProfileContextAssignments(
            Connection connection,
            long targetUserId,
            UserState targetState,
            Set<AccessProfile> targetProfiles,
            Collection<AccessProfileContextAssignment> selectedAssignments
    ) throws SQLException {
        if (selectedAssignments == null) {
            return;
        }
        Set<AccessProfileContextAssignment> selected = normalizeProfileContextAssignments(selectedAssignments);
        boolean coordinatorProfile = hasProfile(targetProfiles, AccessProfileType.COORDINATOR);
        boolean teacherProfile = hasProfile(targetProfiles, AccessProfileType.TEACHER);
        boolean studentProfile = hasProfile(targetProfiles, AccessProfileType.STUDENT);

        Set<Long> coordinatorSubjectIds = selectedContextIds(selected, AccessProfileType.COORDINATOR, AccessEntityType.SUBJECT);
        Set<Long> teacherClassGroupIds = selectedContextIds(selected, AccessProfileType.TEACHER, AccessEntityType.CLASS_GROUP);
        Set<Long> studentCourseIds = selectedContextIds(selected, AccessProfileType.STUDENT, AccessEntityType.COURSE);
        Map<Long, Set<Long>> studentSubjectIdsByCourse = selectedStudentSubjects(selected);
        studentCourseIds = new LinkedHashSet<>(studentCourseIds);
        studentCourseIds.addAll(studentSubjectIdsByCourse.keySet());

        requireProfileContextState(AccessProfileType.COORDINATOR, coordinatorProfile, coordinatorSubjectIds, targetState);
        requireProfileContextState(AccessProfileType.TEACHER, teacherProfile, teacherClassGroupIds, targetState);
        requireProfileContextState(AccessProfileType.STUDENT, studentProfile, studentCourseIds, targetState);

        Set<Long> teacherSubjectIds = new LinkedHashSet<>();
        Set<Long> teacherCourseIds = new LinkedHashSet<>();
        for (long classGroupId : teacherClassGroupIds) {
            ClassGroupProfileContext context = requireActiveClassGroupContext(connection, classGroupId);
            teacherCourseIds.add(context.courseId());
            teacherSubjectIds.add(context.subjectId());
        }
        for (long subjectId : coordinatorSubjectIds) {
            requireActiveSubject(connection, subjectId);
        }
        for (long courseId : studentCourseIds) {
            requireActiveCourse(connection, courseId);
        }
        Set<Long> studentSubjectIds = new LinkedHashSet<>();
        for (Map.Entry<Long, Set<Long>> entry : studentSubjectIdsByCourse.entrySet()) {
            for (long subjectId : entry.getValue()) {
                requireActiveCourseSubject(connection, entry.getKey(), subjectId);
                studentSubjectIds.add(subjectId);
            }
        }

        if (!disjoint(coordinatorSubjectIds, teacherSubjectIds)) {
            throw new IllegalArgumentException("The same user cannot coordinate and teach the same subject context");
        }
        if (!disjoint(coordinatorSubjectIds, studentSubjectIds)) {
            throw new IllegalArgumentException("The same user cannot coordinate and study the same subject context");
        }
        if (!disjoint(teacherCourseIds, studentCourseIds) || !disjoint(teacherSubjectIds, studentSubjectIds)) {
            throw new IllegalArgumentException("The same user cannot teach and study the same learning context");
        }

        LocalDate startDate = LocalDate.now(clock);
        synchronizeCoordinatorSubjects(connection, targetUserId, coordinatorSubjectIds, startDate);
        synchronizeTeacherClassGroups(connection, targetUserId, teacherClassGroupIds, startDate);
        synchronizeStudentEnrollments(connection, targetUserId, studentCourseIds, studentSubjectIdsByCourse, startDate);
    }

    private static void validateNoOverlappingProfileScopes(
            Connection connection,
            Set<AccessProfile> targetProfiles,
            Set<AdministratorPermissionAssignment> selectedAdminAssignments,
            Set<AccessProfileContextAssignment> selectedProfileAssignments
    ) throws SQLException {
        if (selectedAdminAssignments == null && selectedProfileAssignments == null) {
            return;
        }
        Set<ProfileScope> scopes = new LinkedHashSet<>();
        if (selectedAdminAssignments != null && hasProfile(targetProfiles, AccessProfileType.ADMINISTRATOR)) {
            for (AdministratorPermissionAssignment assignment : selectedAdminAssignments) {
                registerScopes(scopes, administratorScopes(connection, assignment));
            }
        }
        if (selectedProfileAssignments != null) {
            for (AccessProfileContextAssignment assignment : selectedProfileAssignments) {
                registerScopes(scopes, profileScopes(connection, assignment));
            }
        }
    }

    private static void registerScopes(Set<ProfileScope> existing, Set<ProfileScope> candidates) {
        for (ProfileScope candidate : candidates) {
            for (ProfileScope current : existing) {
                if (candidate.conflictsWith(current)) {
                    throw new IllegalArgumentException(
                            "The same user cannot have multiple access profiles in the same organization or organic unit"
                    );
                }
            }
            existing.add(candidate);
        }
    }

    private static Set<ProfileScope> administratorScopes(
            Connection connection,
            AdministratorPermissionAssignment assignment
    ) throws SQLException {
        if (assignment.isManageAll()) {
            return Set.of(ProfileScope.global(AccessProfileType.ADMINISTRATOR));
        }
        return switch (assignment.contextType()) {
            case ORGANIZATION -> Set.of(ProfileScope.organization(
                    AccessProfileType.ADMINISTRATOR,
                    assignment.contextId()
            ));
            case ORGANIC_UNIT -> Set.of(organicUnitScope(
                    connection,
                    AccessProfileType.ADMINISTRATOR,
                    assignment.contextId()
            ));
            case COURSE -> Set.of(courseScope(
                    connection,
                    AccessProfileType.ADMINISTRATOR,
                    assignment.contextId()
            ));
            case SUBJECT -> subjectScopes(
                    connection,
                    AccessProfileType.ADMINISTRATOR,
                    assignment.contextId()
            );
            case CLASS_GROUP -> Set.of(classGroupScope(
                    connection,
                    AccessProfileType.ADMINISTRATOR,
                    assignment.contextId()
            ));
            case GLOBAL -> Set.of(ProfileScope.global(AccessProfileType.ADMINISTRATOR));
            case SELF -> Set.of();
        };
    }

    private static Set<ProfileScope> profileScopes(
            Connection connection,
            AccessProfileContextAssignment assignment
    ) throws SQLException {
        return switch (assignment.profileType()) {
            case ADMINISTRATOR -> Set.of();
            case COORDINATOR -> subjectScopes(connection, AccessProfileType.COORDINATOR, assignment.contextId());
            case TEACHER -> Set.of(classGroupScope(connection, AccessProfileType.TEACHER, assignment.contextId()));
            case STUDENT -> {
                if (assignment.contextType() == AccessEntityType.COURSE) {
                    yield Set.of(courseScope(connection, AccessProfileType.STUDENT, assignment.contextId()));
                }
                yield Set.of(courseScope(connection, AccessProfileType.STUDENT, assignment.parentContextId()));
            }
        };
    }

    private static ProfileScope organicUnitScope(
            Connection connection,
            AccessProfileType profileType,
            long organicUnitId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_organization
                FROM organic_unit
                WHERE id_organic_unit = ?
                  AND state = 'active'
                """)) {
            statement.setLong(1, organicUnitId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Active organic unit context not found: " + organicUnitId);
                }
                return ProfileScope.organicUnit(
                        profileType,
                        resultSet.getLong("id_organization"),
                        organicUnitId
                );
            }
        }
    }

    private static ProfileScope courseScope(
            Connection connection,
            AccessProfileType profileType,
            long courseId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id_organization, id_organic_unit
                FROM course
                WHERE id_course = ?
                  AND state = 'active'
                """)) {
            statement.setLong(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Active course context not found: " + courseId);
                }
                Long organicUnitId = nullableLong(resultSet, "id_organic_unit");
                if (organicUnitId == null) {
                    return ProfileScope.organization(profileType, resultSet.getLong("id_organization"));
                }
                return ProfileScope.organicUnit(profileType, resultSet.getLong("id_organization"), organicUnitId);
            }
        }
    }

    private static Set<ProfileScope> subjectScopes(
            Connection connection,
            AccessProfileType profileType,
            long subjectId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT s.id_organization, c.id_organic_unit
                FROM subject s
                LEFT JOIN integrate_subject isub
                       ON isub.id_subject = s.id_subject
                      AND isub.state = 'active'
                LEFT JOIN course c
                       ON c.id_course = isub.id_course
                      AND c.state = 'active'
                WHERE s.id_subject = ?
                  AND s.state = 'active'
                """)) {
            statement.setLong(1, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                Set<ProfileScope> scopes = new LinkedHashSet<>();
                boolean found = false;
                while (resultSet.next()) {
                    found = true;
                    long organizationId = resultSet.getLong("id_organization");
                    Long organicUnitId = nullableLong(resultSet, "id_organic_unit");
                    if (organicUnitId == null) {
                        scopes.add(ProfileScope.organization(profileType, organizationId));
                    } else {
                        scopes.add(ProfileScope.organicUnit(profileType, organizationId, organicUnitId));
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException("Active subject context not found: " + subjectId);
                }
                return scopes;
            }
        }
    }

    private static ProfileScope classGroupScope(
            Connection connection,
            AccessProfileType profileType,
            long classGroupId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT c.id_organization, c.id_organic_unit
                FROM class_group cg
                JOIN course c ON c.id_course = cg.id_course
                JOIN subject s ON s.id_subject = cg.id_subject
                WHERE cg.id_class_group = ?
                  AND cg.state = 'active'
                  AND c.state = 'active'
                  AND s.state = 'active'
                """)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Active class group context not found: " + classGroupId);
                }
                Long organicUnitId = nullableLong(resultSet, "id_organic_unit");
                if (organicUnitId == null) {
                    return ProfileScope.organization(profileType, resultSet.getLong("id_organization"));
                }
                return ProfileScope.organicUnit(profileType, resultSet.getLong("id_organization"), organicUnitId);
            }
        }
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static void requireProfileContextState(
            AccessProfileType profileType,
            boolean profileSelected,
            Set<Long> contextIds,
            UserState targetState
    ) {
        if (!profileSelected) {
            if (!contextIds.isEmpty()) {
                throw new IllegalArgumentException(profileType + " context requires the matching access profile");
            }
            return;
        }
        if (contextIds.isEmpty()) {
            throw new IllegalArgumentException(profileType + " profile requires at least one context");
        }
        if (targetState != UserState.ACTIVE) {
            throw new IllegalArgumentException(profileType + " context requires an active user");
        }
    }

    private static Set<Long> selectedContextIds(
            Set<AccessProfileContextAssignment> selected,
            AccessProfileType profileType,
            AccessEntityType contextType
    ) {
        return selected.stream()
                .filter(assignment -> assignment.profileType() == profileType)
                .filter(assignment -> assignment.contextType() == contextType)
                .map(AccessProfileContextAssignment::contextId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Map<Long, Set<Long>> selectedStudentSubjects(Set<AccessProfileContextAssignment> selected) {
        Map<Long, Set<Long>> subjectsByCourse = new HashMap<>();
        for (AccessProfileContextAssignment assignment : selected) {
            if (assignment.profileType() == AccessProfileType.STUDENT
                    && assignment.contextType() == AccessEntityType.SUBJECT) {
                subjectsByCourse
                        .computeIfAbsent(assignment.parentContextId(), ignored -> new LinkedHashSet<>())
                        .add(assignment.contextId());
            }
        }
        return subjectsByCourse;
    }

    private static boolean disjoint(Set<Long> left, Set<Long> right) {
        Set<Long> copy = new HashSet<>(left);
        copy.retainAll(right);
        return copy.isEmpty();
    }

    private static Set<AccessProfileContextAssignment> normalizeProfileContextAssignments(
            Collection<AccessProfileContextAssignment> assignments
    ) {
        Set<AccessProfileContextAssignment> normalized = new LinkedHashSet<>();
        for (AccessProfileContextAssignment assignment : assignments) {
            if (assignment != null) {
                normalized.add(assignment);
            }
        }
        return Set.copyOf(normalized);
    }

    private static void synchronizeCoordinatorSubjects(
            Connection connection,
            long userId,
            Set<Long> subjectIds,
            LocalDate startDate
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE coordinate_subject SET state = 'inactive', end_date = ? WHERE id_coordinator_user = ?"
        )) {
            setDate(statement, 1, startDate);
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
        for (long subjectId : subjectIds) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO coordinate_subject (id_coordinator_user, id_subject, state, start_date, end_date)
                    VALUES (?, ?, 'active', ?, NULL)
                    ON DUPLICATE KEY UPDATE state = 'active', start_date = VALUES(start_date), end_date = NULL
                    """)) {
                statement.setLong(1, userId);
                statement.setLong(2, subjectId);
                setDate(statement, 3, startDate);
                statement.executeUpdate();
            }
        }
    }

    private static void synchronizeTeacherClassGroups(
            Connection connection,
            long userId,
            Set<Long> classGroupIds,
            LocalDate startDate
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE teach_class_group SET state = 'inactive', end_date = ? WHERE id_teacher_user = ?"
        )) {
            setDate(statement, 1, startDate);
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
        for (long classGroupId : classGroupIds) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO teach_class_group (id_teacher_user, id_class_group, state, start_date, end_date)
                    VALUES (?, ?, 'active', ?, NULL)
                    ON DUPLICATE KEY UPDATE state = 'active', start_date = VALUES(start_date), end_date = NULL
                    """)) {
                statement.setLong(1, userId);
                statement.setLong(2, classGroupId);
                setDate(statement, 3, startDate);
                statement.executeUpdate();
            }
        }
    }

    private static void synchronizeStudentEnrollments(
            Connection connection,
            long userId,
            Set<Long> courseIds,
            Map<Long, Set<Long>> subjectIdsByCourse,
            LocalDate startDate
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE enroll_subject SET state = 'withdrawn', end_date = ? WHERE id_student_user = ?"
        )) {
            setDate(statement, 1, startDate);
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE enroll_course SET state = 'withdrawn', end_date = ? WHERE id_student_user = ?"
        )) {
            setDate(statement, 1, startDate);
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
        for (long courseId : courseIds) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO enroll_course (id_student_user, id_course, state, start_date, end_date)
                    VALUES (?, ?, 'active', ?, NULL)
                    ON DUPLICATE KEY UPDATE state = 'active', start_date = VALUES(start_date), end_date = NULL
                    """)) {
                statement.setLong(1, userId);
                statement.setLong(2, courseId);
                setDate(statement, 3, startDate);
                statement.executeUpdate();
            }
        }
        for (Map.Entry<Long, Set<Long>> entry : subjectIdsByCourse.entrySet()) {
            for (long subjectId : entry.getValue()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO enroll_subject (id_student_user, id_course, id_subject, state, start_date, end_date)
                        VALUES (?, ?, ?, 'active', ?, NULL)
                        ON DUPLICATE KEY UPDATE state = 'active', start_date = VALUES(start_date), end_date = NULL
                        """)) {
                    statement.setLong(1, userId);
                    statement.setLong(2, entry.getKey());
                    statement.setLong(3, subjectId);
                    setDate(statement, 4, startDate);
                    statement.executeUpdate();
                }
            }
        }
    }

    private static void requireActiveSubject(Connection connection, long subjectId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM subject WHERE id_subject = ? AND state = 'active'"
        )) {
            statement.setLong(1, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                if (resultSet.getInt(1) == 0) {
                    throw new IllegalArgumentException("Active subject context not found: " + subjectId);
                }
            }
        }
    }

    private static void requireActiveCourse(Connection connection, long courseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM course WHERE id_course = ? AND state = 'active'"
        )) {
            statement.setLong(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                if (resultSet.getInt(1) == 0) {
                    throw new IllegalArgumentException("Active course context not found: " + courseId);
                }
            }
        }
    }

    private static void requireActiveCourseSubject(Connection connection, long courseId, long subjectId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM integrate_subject isub
                JOIN course c ON c.id_course = isub.id_course
                JOIN subject s ON s.id_subject = isub.id_subject
                WHERE isub.id_course = ?
                  AND isub.id_subject = ?
                  AND isub.state = 'active'
                  AND c.state = 'active'
                  AND s.state = 'active'
                """)) {
            statement.setLong(1, courseId);
            statement.setLong(2, subjectId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                if (resultSet.getInt(1) == 0) {
                    throw new IllegalArgumentException("Active course subject context not found: "
                            + courseId + ":" + subjectId);
                }
            }
        }
    }

    private static ClassGroupProfileContext requireActiveClassGroupContext(Connection connection, long classGroupId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT cg.id_course, cg.id_subject, c.id_organization, c.id_organic_unit
                FROM class_group cg
                JOIN course c ON c.id_course = cg.id_course
                JOIN subject s ON s.id_subject = cg.id_subject
                WHERE cg.id_class_group = ?
                  AND cg.state = 'active'
                  AND c.state = 'active'
                  AND s.state = 'active'
                """)) {
            statement.setLong(1, classGroupId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Active class group context not found: " + classGroupId);
                }
                return new ClassGroupProfileContext(
                        resultSet.getLong("id_course"),
                        resultSet.getLong("id_subject"),
                        resultSet.getLong("id_organization"),
                        nullableLong(resultSet, "id_organic_unit")
                );
            }
        }
    }

    private void synchronizeOrganizationResponsibilityAssignments(
            Connection connection,
            long actorUserId,
            long targetUserId,
            Set<AdministratorPermissionAssignment> selectedAssignments
    ) throws SQLException {
        Set<Long> selectedOrganizationIds = selectedAssignments.stream()
                .filter(assignment -> AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE.equals(assignment.permissionCode()))
                .filter(assignment -> assignment.contextType() == AccessEntityType.ORGANIZATION)
                .map(AdministratorPermissionAssignment::contextId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> organizationScope = permissionChecker.hasPermission(
                actorUserId,
                AccessProfileType.ADMINISTRATOR,
                AuthorizationPolicy.MANAGE_ALL
        )
                ? managedOrganizationsTouchedByTargetOrSelection(connection, targetUserId, selectedOrganizationIds)
                : manageOrganizationDAO.findActiveOrganizationIdsByAdministrator(connection, actorUserId);

        for (long organizationId : selectedOrganizationIds) {
            if (!manageOrganizationDAO.canAssign(connection, targetUserId, organizationId)) {
                throw new IllegalArgumentException("Organization assignment requires active administrator and non-archived organization");
            }
        }
        manageOrganizationDAO.synchronizeAssignments(
                connection,
                targetUserId,
                organizationScope,
                selectedOrganizationIds,
                LocalDate.now(clock)
        );
    }

    private static boolean hasProfile(Set<AccessProfile> profiles, AccessProfileType profileType) {
        return profiles.stream().anyMatch(profile -> profile.type() == profileType);
    }

    private static void setDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DATE);
        } else {
            statement.setDate(index, java.sql.Date.valueOf(value));
        }
    }

    private static void requireExactlyOneAdministratorPermission(Set<AdministratorPermissionAssignment> selected) {
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Administrator profile requires exactly one administrator permission");
        }
        long permissionCount = selected.stream()
                .map(AdministratorPermissionAssignment::permissionCode)
                .distinct()
                .count();
        if (permissionCount != 1L) {
            throw new IllegalArgumentException("Administrator profile requires exactly one administrator permission");
        }
    }

    private void requireDelegableAssignment(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            AdministratorPermissionAssignment assignment,
            String sourceIp
    ) {
        if (assignment.isManageAll()) {
            requirePermission(
                    actorUserId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_ALL,
                    "Only MANAGE_ALL administrators can delegate MANAGE_ALL"
            );
            return;
        }
        AuthorizationDecision decision = permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                assignment.permissionCode(),
                assignment.contextType(),
                assignment.contextId(),
                sourceIp
        ));
        if (!decision.allowed()) {
            throw new SecurityException("Administrator cannot delegate permission outside own context: "
                    + decision.reason());
        }
    }

    private Set<Long> managedOrganizationsTouchedByTargetOrSelection(
            Connection connection,
            long targetUserId,
            Set<Long> selectedOrganizationIds
    ) throws SQLException {
        Set<Long> organizationIds = new LinkedHashSet<>(manageOrganizationDAO.findActiveOrganizationIdsByAdministrator(
                connection,
                targetUserId
        ));
        organizationIds.addAll(selectedOrganizationIds);
        return Set.copyOf(organizationIds);
    }

    private static Set<Long> normalizeOrganizationIds(Set<Long> organizationIds) {
        if (organizationIds == null || organizationIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> normalized = new LinkedHashSet<>();
        for (Long organizationId : organizationIds) {
            if (organizationId == null || organizationId <= 0) {
                throw new IllegalArgumentException("Invalid organization assignment");
            }
            normalized.add(organizationId);
        }
        return Set.copyOf(normalized);
    }

    private static Set<AdministratorPermissionAssignment> normalizeAdminPermissionAssignments(
            Collection<AdministratorPermissionAssignment> assignments
    ) {
        Set<AdministratorPermissionAssignment> normalized = new LinkedHashSet<>();
        for (AdministratorPermissionAssignment assignment : assignments) {
            if (assignment != null) {
                if (!AuthorizationPolicy.isAdminPermission(assignment.permissionCode())) {
                    throw new IllegalArgumentException("Unsupported administrator permission: "
                            + assignment.permissionCode());
                }
                normalized.add(assignment);
            }
        }
        return Set.copyOf(normalized);
    }

    private static List<AdministratorPermissionAssignment> organizationScopeAssignments(Set<Long> organizationIds) {
        Set<Long> normalizedOrganizationIds = normalizeOrganizationIds(organizationIds);
        return normalizedOrganizationIds.stream()
                .map(organizationId -> new AdministratorPermissionAssignment(
                        AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE,
                        AccessEntityType.ORGANIZATION,
                        organizationId
                ))
                .toList();
    }

    private record ClassGroupProfileContext(long courseId, long subjectId, long organizationId, Long organicUnitId) {
    }

    private record ProfileScope(
            AccessProfileType profileType,
            boolean global,
            long organizationId,
            Long organicUnitId
    ) {

        static ProfileScope global(AccessProfileType profileType) {
            return new ProfileScope(profileType, true, 0L, null);
        }

        static ProfileScope organization(AccessProfileType profileType, long organizationId) {
            return new ProfileScope(profileType, false, organizationId, null);
        }

        static ProfileScope organicUnit(AccessProfileType profileType, long organizationId, long organicUnitId) {
            return new ProfileScope(profileType, false, organizationId, organicUnitId);
        }

        boolean conflictsWith(ProfileScope other) {
            if (profileType == other.profileType) {
                return false;
            }
            if (global || other.global) {
                return true;
            }
            if (organizationId != other.organizationId) {
                return false;
            }
            return organicUnitId == null
                    || other.organicUnitId == null
                    || Objects.equals(organicUnitId, other.organicUnitId);
        }
    }

    private void record(
            long actorUserId,
            Long sessionId,
            String operationType,
            String affectedEntityType,
            String affectedEntityIdentifier,
            String outcome,
            String sourceIp
    ) {
        if (auditService != null) {
            auditService.record(actorUserId, sessionId, operationType, affectedEntityType, affectedEntityIdentifier, outcome, sourceIp);
        }
    }

    private void record(
            Connection connection,
            long actorUserId,
            Long sessionId,
            String operationType,
            String affectedEntityType,
            String affectedEntityIdentifier,
            String outcome,
            String sourceIp
    ) throws SQLException {
        if (auditService != null) {
            auditService.record(connection, actorUserId, sessionId, operationType, affectedEntityType, affectedEntityIdentifier, outcome, sourceIp);
        }
    }

    private <T> T inTransaction(TransactionCallback<T> callback) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = callback.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    @FunctionalInterface
    private interface TransactionCallback<T> {
        T execute(Connection connection) throws SQLException;
    }

    private static String affectedIdentifier(UserCreateCommand command) {
        return "new";
    }

    private static String required(String value, String fieldName) {
        String normalized = optional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private static String supportedLanguage(String value) {
        String language = required(value, "language");
        if (!SupportedLanguageCatalog.contains(language)) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        return language;
    }

    private static String optional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
