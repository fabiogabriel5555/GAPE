package pt.isel.gape.transversal.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Collection;
import java.util.Objects;

import pt.isel.gape.access.dao.UserDAO;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.common.validation.AcademicTextValidator;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.SubjectDAO;
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.dao.ManagementViewDAO;
import pt.isel.gape.transversal.model.ManagementView;
import pt.isel.gape.transversal.model.ManagementViewScope;
import pt.isel.gape.transversal.model.ManagementViewScopeTargetType;
import pt.isel.gape.transversal.model.ManagementViewState;
import pt.isel.gape.transversal.model.ManagementViewUpdateCommand;

/**
 * Transactional configuration service for management views.
 *
 * <p>Scope authorization is intentionally delegated to
 * {@link ManagementViewConfigurationAuthorizer}; this service owns only
 * structural validation, persistence and configuration auditing.</p>
 */
public final class ManagementViewService {

    public static final String CONFIGURE_OPERATION = "MANAGEMENT_VIEW_CONFIGURE";
    public static final String ENTITY_TYPE = "management_view";

    private static final int TITLE_MAX_LENGTH = 160;
    private static final int DESCRIPTION_MAX_LENGTH = 500;
    private final ConnectionProvider connectionProvider;
    private final ManagementViewDAO managementViewDAO;
    private final OrganizationDAO organizationDAO;
    private final CourseDAO courseDAO;
    private final SubjectDAO subjectDAO;
    private final ClassGroupDAO classGroupDAO;
    private final UserDAO userDAO;
    private final ManagementViewConfigurationAuthorizer configurationAuthorizer;
    private final AuditService auditService;

    public ManagementViewService(ConnectionProvider connectionProvider) {
        this(connectionProvider, ApplicationClock.system());
    }

    public ManagementViewService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new ManagementViewDAO(connectionProvider),
                new OrganizationDAO(connectionProvider),
                new CourseDAO(connectionProvider),
                new SubjectDAO(connectionProvider),
                new ClassGroupDAO(connectionProvider),
                new UserDAO(connectionProvider),
                new ManagementViewAccessService(connectionProvider, clock),
                new AuditService(new ActivityLogDAO(connectionProvider), clock)
        );
    }

    public ManagementViewService(
            ConnectionProvider connectionProvider,
            ManagementViewDAO managementViewDAO,
            OrganizationDAO organizationDAO,
            CourseDAO courseDAO,
            SubjectDAO subjectDAO,
            ClassGroupDAO classGroupDAO,
            UserDAO userDAO,
            ManagementViewConfigurationAuthorizer configurationAuthorizer,
            AuditService auditService
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.managementViewDAO = Objects.requireNonNull(managementViewDAO, "managementViewDAO is required");
        this.organizationDAO = Objects.requireNonNull(organizationDAO, "organizationDAO is required");
        this.courseDAO = Objects.requireNonNull(courseDAO, "courseDAO is required");
        this.subjectDAO = Objects.requireNonNull(subjectDAO, "subjectDAO is required");
        this.classGroupDAO = Objects.requireNonNull(classGroupDAO, "classGroupDAO is required");
        this.userDAO = Objects.requireNonNull(userDAO, "userDAO is required");
        this.configurationAuthorizer = Objects.requireNonNull(
                configurationAuthorizer,
                "configurationAuthorizer is required"
        );
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
    }

    /**
     * Updates a view only when the actor controls both its current context and
     * its requested context.  Ownership is never supplied by the browser.
     */
    public ManagementView updateManagementView(
            AccessContext actor,
            long managementViewId,
            ManagementViewUpdateCommand command
    ) {
        Objects.requireNonNull(actor, "actor is required");
        if (managementViewId <= 0) {
            throw new IllegalArgumentException("managementViewId must be positive");
        }
        try {
            ManagementView current = managementViewDAO.findById(managementViewId)
                    .orElseThrow(() -> new IllegalArgumentException("Management view not found: " + managementViewId));
            NormalizedConfiguration normalized = normalizeUpdate(actor, current, command);
            requireConfigurationAccess(actor, fromExisting(current));
            requireConfigurationAccess(actor, normalized);
            validateContextExists(normalized);
            return inTransaction(connection -> {
                ManagementView lockedCurrent = managementViewDAO.lockById(connection, managementViewId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Management view not found: " + managementViewId
                        ));
                // Recheck authority against the locked view to avoid changing a
                // view whose scope was concurrently reconfigured.
                requireConfigurationAccess(actor, fromExisting(lockedCurrent));
                requireConfigurationAccess(actor, normalized);
                validateContextExists(connection, normalized);
                managementViewDAO.update(
                        connection,
                        managementViewId,
                        command,
                        normalized.scope().targetType(),
                        normalized.scopeContextId(),
                        normalized.ownerUserId()
                );
                auditService.record(
                        connection,
                        actor.userId(),
                        actor.sessionId(),
                        CONFIGURE_OPERATION,
                        ENTITY_TYPE,
                        Long.toString(managementViewId),
                        "success",
                        actor.sourceIp()
                );
                return managementViewDAO.findById(connection, managementViewId)
                        .orElseThrow(() -> new SQLException(
                                "Updated management view cannot be reloaded: " + managementViewId
                        ));
            });
        } catch (SecurityException exception) {
            audit(actor, Long.toString(managementViewId), "denied");
            throw exception;
        } catch (RuntimeException exception) {
            audit(actor, Long.toString(managementViewId), "failure");
            throw exception;
        } catch (SQLException exception) {
            audit(actor, Long.toString(managementViewId), "failure");
            throw new IllegalStateException("Failed to update management view " + managementViewId, exception);
        }
    }

    /**
     * Configures the optional Access_Management_View relation.  The relation
     * helps distribute a panel but cannot bypass its contextual access rule.
     */
    public ManagementView configureExplicitAccess(
            AccessContext actor,
            long managementViewId,
            Collection<Long> recipientUserIds
    ) {
        Objects.requireNonNull(actor, "actor is required");
        if (managementViewId <= 0) {
            throw new IllegalArgumentException("managementViewId must be positive");
        }
        try {
            ManagementView current = managementViewDAO.findById(managementViewId)
                    .orElseThrow(() -> new IllegalArgumentException("Management view not found: " + managementViewId));
            NormalizedConfiguration normalized = fromExisting(current);
            requireConfigurationAccess(actor, normalized);
            return inTransaction(connection -> {
                ManagementView lockedCurrent = managementViewDAO.lockById(connection, managementViewId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Management view not found: " + managementViewId
                        ));
                requireConfigurationAccess(actor, fromExisting(lockedCurrent));
                validateRecipientUsers(connection, recipientUserIds);
                managementViewDAO.replaceExplicitAccess(connection, managementViewId, recipientUserIds);
                auditService.record(
                        connection,
                        actor.userId(),
                        actor.sessionId(),
                        CONFIGURE_OPERATION,
                        ENTITY_TYPE,
                        Long.toString(managementViewId),
                        "success",
                        actor.sourceIp()
                );
                return managementViewDAO.findById(connection, managementViewId)
                        .orElseThrow(() -> new SQLException(
                                "Configured management view cannot be reloaded: " + managementViewId
                        ));
            });
        } catch (SecurityException exception) {
            audit(actor, Long.toString(managementViewId), "denied");
            throw exception;
        } catch (RuntimeException exception) {
            audit(actor, Long.toString(managementViewId), "failure");
            throw exception;
        } catch (SQLException exception) {
            audit(actor, Long.toString(managementViewId), "failure");
            throw new IllegalStateException("Failed to configure management-view access " + managementViewId, exception);
        }
    }

    /**
     * Lists only panels the actor is currently allowed to configure.
     *
     * <p>This is a catalogue operation for a configuration screen, not an
     * access attempt.  Therefore denied rows are silently omitted and no
     * access/configuration audit records are emitted.</p>
     */
    public java.util.List<ManagementView> listConfigurable(AccessContext actor) {
        Objects.requireNonNull(actor, "actor is required");
        validateActor(actor);
        try {
            java.util.List<ManagementView> configurable = new java.util.ArrayList<>();
            for (ManagementView view : managementViewDAO.findAll()) {
                try {
                    requireConfigurationAccess(actor, fromExisting(view));
                    configurable.add(view);
                } catch (SecurityException ignored) {
                    // A configuration catalogue must not reveal inaccessible
                    // scope targets, and this is not an attempted mutation.
                } catch (IllegalArgumentException ignored) {
                    // Legacy/inconsistent rows cannot safely be configured.
                }
            }
            return java.util.List.copyOf(configurable);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list configurable management views", exception);
        }
    }

    /**
     * Reads a distribution list only after the current scope configuration
     * rule has been enforced.  The browser never talks to the DAO directly.
     */
    public java.util.List<Long> explicitRecipientUserIds(AccessContext actor, long managementViewId) {
        Objects.requireNonNull(actor, "actor is required");
        if (managementViewId <= 0) {
            throw new IllegalArgumentException("managementViewId must be positive");
        }
        validateActor(actor);
        try {
            ManagementView view = managementViewDAO.findById(managementViewId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Management view not found: " + managementViewId
                    ));
            requireConfigurationAccess(actor, fromExisting(view));
            return managementViewDAO.findExplicitRecipientUserIds(managementViewId);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Failed to read management-view recipients for " + managementViewId,
                    exception
            );
        }
    }

    private NormalizedConfiguration normalizeUpdate(
            AccessContext actor,
            ManagementView current,
            ManagementViewUpdateCommand command
    ) {
        validateActor(actor);
        validateUpdateCommand(command);
        long ownerUserId = current.ownerUserId() == null ? actor.userId() : current.ownerUserId();
        Long scopeContextId = normalizeScopeContext(actor.userId(), command.visibilityScope(), command.scopeContextId());
        if (command.visibilityScope() == ManagementViewScope.PERSONAL) {
            ownerUserId = actor.userId();
        }
        return new NormalizedConfiguration(command.visibilityScope(), scopeContextId, ownerUserId);
    }

    private static NormalizedConfiguration fromExisting(ManagementView view) {
        if (view.scopeTargetType() != view.visibilityScope().targetType()) {
            throw new IllegalArgumentException("Management view has an inconsistent scope target");
        }
        if (view.ownerUserId() == null || view.ownerUserId() <= 0) {
            throw new IllegalArgumentException("Management view has no valid owner");
        }
        return new NormalizedConfiguration(view.visibilityScope(), view.scopeTargetId(), view.ownerUserId());
    }

    private static Long normalizeScopeContext(long actorUserId, ManagementViewScope scope, Long suppliedScopeContextId) {
        if (scope == ManagementViewScope.GLOBAL) {
            if (suppliedScopeContextId != null) {
                throw new IllegalArgumentException("Global management views cannot have a scope context");
            }
            return null;
        }
        if (scope == ManagementViewScope.PERSONAL) {
            // A client is never allowed to choose a different personal owner.
            return actorUserId;
        }
        if (suppliedScopeContextId == null || suppliedScopeContextId <= 0) {
            throw new IllegalArgumentException("A positive scope context id is required for " + scope);
        }
        return suppliedScopeContextId;
    }

    private void requireConfigurationAccess(AccessContext actor, NormalizedConfiguration configuration) {
        configurationAuthorizer.requireConfigurationAccess(
                actor.userId(),
                actor.profileType(),
                configuration.scope(),
                configuration.scopeContextId(),
                configuration.ownerUserId()
        );
    }

    private void validateContextExists(NormalizedConfiguration configuration) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            validateContextExists(connection, configuration);
        }
    }

    private void validateContextExists(Connection connection, NormalizedConfiguration configuration) throws SQLException {
        if (configuration.scope() == ManagementViewScope.GLOBAL) {
            return;
        }
        long targetId = requiredPositive(configuration.scopeContextId(), "scopeContextId");
        boolean exists = switch (configuration.scope().targetType()) {
            case ORGANIZATION -> organizationDAO.findById(connection, targetId).isPresent();
            case COURSE -> courseDAO.findById(connection, targetId).isPresent();
            case SUBJECT -> subjectDAO.findById(connection, targetId).isPresent();
            case CLASS_GROUP -> classGroupDAO.findById(connection, targetId).isPresent();
            case USER -> userDAO.findById(connection, targetId).isPresent();
        };
        if (!exists) {
            throw new IllegalArgumentException(
                    "Management view scope target does not exist: " + configuration.scope() + " " + targetId
            );
        }
    }

    private void validateRecipientUsers(Connection connection, Collection<Long> recipientUserIds) throws SQLException {
        if (recipientUserIds == null) {
            return;
        }
        for (Long recipientUserId : recipientUserIds) {
            long userId = requiredPositive(recipientUserId, "recipient user id");
            if (userDAO.findById(connection, userId).isEmpty()) {
                throw new IllegalArgumentException("Management-view recipient user does not exist: " + userId);
            }
        }
    }

    private static void validateUpdateCommand(ManagementViewUpdateCommand command) {
        Objects.requireNonNull(command, "command is required");
        validateCommonFields(command.title(), command.type(), command.description(), command.visibilityScope(), command.state());
    }

    private static void validateCommonFields(
            String title,
            Object type,
            String description,
            ManagementViewScope visibilityScope,
            ManagementViewState state
    ) {
        AcademicTextValidator.requireName(title, "Management view title is required");
        if (title.trim().length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("Management view title is too long");
        }
        if (description != null && description.trim().length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("Management view description is too long");
        }
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(visibilityScope, "visibilityScope is required");
        Objects.requireNonNull(state, "state is required");
    }

    private static void validateActor(AccessContext actor) {
        if (actor.userId() <= 0) {
            throw new IllegalArgumentException("actor user id must be positive");
        }
    }

    private static long requiredPositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private <T> T inTransaction(SqlWork<T> work) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    private void audit(AccessContext actor, String identifier, String outcome) {
        auditService.record(
                actor.userId(),
                actor.sessionId(),
                CONFIGURE_OPERATION,
                ENTITY_TYPE,
                identifier,
                outcome,
                actor.sourceIp()
        );
    }

    @FunctionalInterface
    private interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }

    private record NormalizedConfiguration(
            ManagementViewScope scope,
            Long scopeContextId,
            long ownerUserId
    ) {
    }
}
