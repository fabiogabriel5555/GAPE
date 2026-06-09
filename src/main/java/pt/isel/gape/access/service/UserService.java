package pt.isel.gape.access.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import pt.isel.gape.access.dao.ProfileDAO;
import pt.isel.gape.access.dao.UserDAO;
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
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class UserService {

    private final UserDAO userDAO;
    private final ProfileDAO profileDAO;
    private final PermissionChecker permissionChecker;
    private final AuditService auditService;
    private final Clock clock;
    private final ConnectionProvider connectionProvider;

    public UserService(UserDAO userDAO) {
        this(userDAO, null, null, null, ApplicationClock.system());
    }

    public UserService(
            UserDAO userDAO,
            ProfileDAO profileDAO,
            PermissionChecker permissionChecker,
            AuditService auditService,
            Clock clock
    ) {
        this(null, userDAO, profileDAO, permissionChecker, auditService, clock);
    }

    private UserService(
            ConnectionProvider connectionProvider,
            UserDAO userDAO,
            ProfileDAO profileDAO,
            PermissionChecker permissionChecker,
            AuditService auditService,
            Clock clock
    ) {
        this.connectionProvider = connectionProvider;
        this.userDAO = Objects.requireNonNull(userDAO, "userDAO is required");
        this.profileDAO = profileDAO;
        this.permissionChecker = permissionChecker;
        this.auditService = auditService;
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public UserService(ConnectionProvider connectionProvider) {
        this(
                connectionProvider,
                new UserDAO(connectionProvider),
                new ProfileDAO(connectionProvider),
                new PermissionChecker(connectionProvider),
                new AuditService(new ActivityLogDAO(connectionProvider), ApplicationClock.system()),
                ApplicationClock.system()
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
        requireCrudDependencies();
        try {
            requireManageUsers(actorUserId, actorProfileType);
            UserCreateCommand normalizedCommand = validateCreateCommand(command);
            if (connectionProvider != null) {
                return inTransaction(connection -> {
                    ensureUniqueEmail(connection, normalizedCommand.email(), null);
                    ensureUniqueDocument(connection, normalizedCommand.documentType(), normalizedCommand.documentNumber(), null);
                    long userId = userDAO.create(connection, normalizedCommand, LocalDateTime.now(clock));
                    profileDAO.replaceProfiles(connection, userId, profileCodesForSave(userId, Set.of(), normalizedCommand.accessProfiles()));
                    record(connection, actorUserId, sessionId, "USER_CREATE", "user_account", Long.toString(userId), "success", sourceIp);
                    return requireUser(connection, userId);
                });
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

    public User updateUser(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long targetUserId,
            UserUpdateCommand command,
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
                    if (!userDAO.update(connection, targetUserId, normalizedCommand)) {
                        throw new IllegalArgumentException("Unknown user: " + targetUserId);
                    }
                    profileDAO.replaceProfiles(
                            connection,
                            targetUserId,
                            profileCodesForSave(targetUserId, existingUser.accessProfiles(), normalizedCommand.accessProfiles())
                    );
                    record(connection, actorUserId, sessionId, "USER_UPDATE", "user_account", Long.toString(targetUserId), "success", sourceIp);
                    return requireUser(connection, targetUserId);
                });
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
        requirePermission(actorUserId, actorProfileType, AuthorizationPolicy.MANAGE_USERS, "MANAGE_USERS permission is required");
    }

    private void requirePermission(long actorUserId, AccessProfileType actorProfileType, String permissionCode, String errorMessage) {
        Objects.requireNonNull(actorProfileType, "actorProfileType is required");
        if (!permissionChecker.hasPermission(actorUserId, actorProfileType, permissionCode)) {
            throw new SecurityException(errorMessage);
        }
    }

    private void requireCrudDependencies() {
        if (profileDAO == null || permissionChecker == null || auditService == null) {
            throw new IllegalStateException("UserService was created without CRUD dependencies");
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
