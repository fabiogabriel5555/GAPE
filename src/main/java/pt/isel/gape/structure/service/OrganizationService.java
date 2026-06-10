package pt.isel.gape.structure.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationDecision;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.structure.model.OrganizationAdministratorAssignment;
import pt.isel.gape.structure.model.OrganizationCreateCommand;
import pt.isel.gape.structure.model.OrganizationState;
import pt.isel.gape.structure.model.OrganizationUpdateCommand;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class OrganizationService {

    private final ConnectionProvider connectionProvider;
    private final OrganizationDAO organizationDAO;
    private final ManageOrganizationDAO manageOrganizationDAO;
    private final PermissionDAO permissionDAO;
    private final PermissionChecker permissionChecker;
    private final AuditService auditService;
    private final Clock clock;

    public OrganizationService(
            ConnectionProvider connectionProvider,
            OrganizationDAO organizationDAO,
            ManageOrganizationDAO manageOrganizationDAO,
            PermissionDAO permissionDAO,
            PermissionChecker permissionChecker,
            AuditService auditService,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.organizationDAO = Objects.requireNonNull(organizationDAO, "organizationDAO is required");
        this.manageOrganizationDAO = Objects.requireNonNull(manageOrganizationDAO, "manageOrganizationDAO is required");
        this.permissionDAO = Objects.requireNonNull(permissionDAO, "permissionDAO is required");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public OrganizationService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new OrganizationDAO(connectionProvider),
                new ManageOrganizationDAO(connectionProvider),
                new PermissionDAO(connectionProvider),
                new PermissionChecker(
                        new PermissionDAO(connectionProvider),
                        new ManageOrganizationDAO(connectionProvider),
                        new CoordinateSubjectDAO(connectionProvider),
                        new TeachClassGroupDAO(connectionProvider)
                ),
                new AuditService(new ActivityLogDAO(connectionProvider), clock),
                clock
        );
    }

    public Organization createOrganization(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            OrganizationCreateCommand command,
            String sourceIp
    ) {
        try {
            requireGlobalOrganizationPermission(actorUserId, sessionId, actorProfileType, sourceIp);
            validateCreateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    OrganizationCreateCommand persistedCommand = command.state() == OrganizationState.ACTIVE
                            ? new OrganizationCreateCommand(
                                    command.name(),
                                    command.acronym(),
                                    command.photo(),
                                    command.type(),
                                    OrganizationState.INACTIVE,
                                    command.administratorUserIds()
                            )
                            : command;
                    long organizationId = organizationDAO.create(connection, persistedCommand);
                    for (long adminUserId : safeAdministrators(command.administratorUserIds())) {
                        requireAssignableAdministrator(connection, adminUserId, organizationId);
                        manageOrganizationDAO.assign(connection, adminUserId, organizationId, LocalDate.now(clock), null);
                    }
                    requireActiveOrganizationAdministrator(connection, organizationId, command.state());
                    if (command.state() == OrganizationState.ACTIVE) {
                        organizationDAO.updateState(connection, organizationId, OrganizationState.ACTIVE);
                    }
                    auditService.record(connection, actorUserId, sessionId, "ORGANIZATION_CREATE",
                            "organization", Long.toString(organizationId), "success", sourceIp);
                    connection.commit();
                    return organizationDAO.findById(organizationId)
                            .orElseThrow(() -> new IllegalStateException("Created organization was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ORGANIZATION_CREATE", "new", sourceIp);
            throw wrap(exception, "Failed to create organization");
        }
    }

    public Organization getOrganization(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String sourceIp
    ) {
        try {
            requireOrganizationManager(actorUserId, sessionId, actorProfileType, organizationId, sourceIp);
            return organizationDAO.findById(organizationId)
                    .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to read organization");
        }
    }

    public List<Organization> listManagedOrganizations(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) {
        try {
            requireGlobalOrganizationPermission(actorUserId, sessionId, actorProfileType, sourceIp);
            return organizationDAO.findByAdministrator(actorUserId);
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list managed organizations");
        }
    }

    public List<OrganizationAdministratorAssignment> listAdministrators(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String sourceIp
    ) {
        try {
            requireOrganizationManager(actorUserId, sessionId, actorProfileType, organizationId, sourceIp);
            return manageOrganizationDAO.findAssignmentsByOrganization(organizationId);
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list organization administrators");
        }
    }

    public Organization updateOrganization(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            OrganizationUpdateCommand command,
            String sourceIp
    ) {
        try {
            requireOrganizationManager(actorUserId, sessionId, actorProfileType, organizationId, sourceIp);
            validateUpdateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Organization current = requireOrganization(connection, organizationId);
                    requireNotArchived(current);
                    organizationDAO.update(connection, organizationId, command);
                    requireActiveOrganizationAdministrator(connection, organizationId, command.state());
                    auditService.record(connection, actorUserId, sessionId, "ORGANIZATION_UPDATE",
                            "organization", Long.toString(organizationId), "success", sourceIp);
                    connection.commit();
                    return organizationDAO.findById(organizationId)
                            .orElseThrow(() -> new IllegalStateException("Updated organization was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ORGANIZATION_UPDATE", Long.toString(organizationId), sourceIp);
            throw wrap(exception, "Failed to update organization");
        }
    }

    public Organization attachCreatedOrganizationPhoto(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String photo,
            String sourceIp
    ) {
        try {
            requireGlobalOrganizationPermission(actorUserId, sessionId, actorProfileType, sourceIp);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Organization current = requireOrganization(connection, organizationId);
                    requireNotArchived(current);
                    organizationDAO.updatePhoto(connection, organizationId, photo);
                    auditService.record(connection, actorUserId, sessionId, "ORGANIZATION_UPDATE",
                            "organization", Long.toString(organizationId), "success", sourceIp);
                    connection.commit();
                    return organizationDAO.findById(organizationId)
                            .orElseThrow(() -> new IllegalStateException("Updated organization was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ORGANIZATION_UPDATE", Long.toString(organizationId), sourceIp);
            throw wrap(exception, "Failed to attach organization photo");
        }
    }

    public void assignAdministrator(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            long adminUserId,
            LocalDate startDate,
            LocalDate endDate,
            String sourceIp
    ) {
        try {
            requireAssignmentPermission(actorUserId, sessionId, actorProfileType, organizationId, sourceIp);
            requireValidDates(startDate, endDate);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Organization organization = requireOrganization(connection, organizationId);
                    requireNotArchived(organization);
                    requireAssignableAdministrator(connection, adminUserId, organizationId);
                    manageOrganizationDAO.assign(connection, adminUserId, organizationId, startDate, endDate);
                    requireActiveOrganizationAdministrator(connection, organizationId, organization.state());
                    auditService.record(connection, actorUserId, sessionId, "ORGANIZATION_ASSIGN_ADMIN",
                            "organization", organizationId + ":" + adminUserId, "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ORGANIZATION_ASSIGN_ADMIN",
                    organizationId + ":" + adminUserId, sourceIp);
            throw wrap(exception, "Failed to assign administrator to organization");
        }
    }

    public void archiveOrganization(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String sourceIp
    ) {
        try {
            requireOrganizationManager(actorUserId, sessionId, actorProfileType, organizationId, sourceIp);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Organization current = requireOrganization(connection, organizationId);
                    requireNotArchived(current);
                    organizationDAO.updateState(connection, organizationId, OrganizationState.ARCHIVED);
                    auditService.record(connection, actorUserId, sessionId, "ORGANIZATION_ARCHIVE",
                            "organization", Long.toString(organizationId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ORGANIZATION_ARCHIVE", Long.toString(organizationId), sourceIp);
            throw wrap(exception, "Failed to archive organization");
        }
    }

    public void deleteOrganization(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String sourceIp
    ) {
        try {
            requireOrganizationManager(actorUserId, sessionId, actorProfileType, organizationId, sourceIp);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Organization current = requireOrganization(connection, organizationId);
                    requireNotArchived(current);
                    if (organizationDAO.hasDomainDependencies(connection, organizationId)) {
                        throw new IllegalStateException("Organization with domain dependencies cannot be deleted");
                    }
                    organizationDAO.delete(connection, organizationId);
                    auditService.record(connection, actorUserId, sessionId, "ORGANIZATION_DELETE",
                            "organization", Long.toString(organizationId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ORGANIZATION_DELETE", Long.toString(organizationId), sourceIp);
            throw wrap(exception, "Failed to delete organization");
        }
    }

    private void requireGlobalOrganizationPermission(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) {
        AuthorizationDecision decision = permissionChecker.check(AccessContext.global(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_ORGANIZATIONS,
                sourceIp
        ));
        if (!decision.allowed()) {
            throw new SecurityException("Missing permission to manage organizations: " + decision.reason());
        }
    }

    private void requireOrganizationManager(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String sourceIp
    ) {
        AuthorizationDecision decision = permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_ORGANIZATIONS,
                AccessEntityType.ORGANIZATION,
                organizationId,
                sourceIp
        ));
        if (!decision.allowed()) {
            throw new SecurityException("Missing organization management context: " + decision.reason());
        }
    }

    private void requireAssignmentPermission(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String sourceIp
    ) throws SQLException {
        AuthorizationDecision contextualDecision = permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_ORGANIZATIONS,
                AccessEntityType.ORGANIZATION,
                organizationId,
                sourceIp
        ));
        if (contextualDecision.allowed()) {
            return;
        }
        if (actorProfileType == AccessProfileType.ADMINISTRATOR
                && permissionDAO.activeProfileExists(actorUserId, AccessProfileType.ADMINISTRATOR)
                && permissionDAO.hasActiveGrant(actorUserId, AccessProfileType.ADMINISTRATOR, AuthorizationPolicy.MANAGE_USERS)) {
            return;
        }
        throw new SecurityException("Missing permission to assign organization administrators: "
                + contextualDecision.reason());
    }

    private Organization requireOrganization(Connection connection, long organizationId) throws SQLException {
        return organizationDAO.findById(connection, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));
    }

    private void requireAssignableAdministrator(Connection connection, long adminUserId, long organizationId)
            throws SQLException {
        if (!manageOrganizationDAO.canAssign(connection, adminUserId, organizationId)) {
            throw new IllegalArgumentException("Assignment requires active administrator and non-archived organization");
        }
    }

    private void requireActiveOrganizationAdministrator(
            Connection connection,
            long organizationId,
            OrganizationState state
    ) throws SQLException {
        if (state == OrganizationState.ACTIVE
                && !manageOrganizationDAO.hasAnyActiveAdministrator(connection, organizationId)) {
            throw new IllegalArgumentException("Active organization requires at least one active administrator");
        }
    }

    private static void validateCreateCommand(OrganizationCreateCommand command) {
        Objects.requireNonNull(command, "command is required");
        requireText(command.name(), "Organization name is required");
        Objects.requireNonNull(command.type(), "organization type is required");
        Objects.requireNonNull(command.state(), "organization state is required");
        if (command.state() == OrganizationState.ACTIVE && safeAdministrators(command.administratorUserIds()).isEmpty()) {
            throw new IllegalArgumentException("Active organization requires at least one administrator");
        }
    }

    private static void validateUpdateCommand(OrganizationUpdateCommand command) {
        Objects.requireNonNull(command, "command is required");
        requireText(command.name(), "Organization name is required");
        Objects.requireNonNull(command.type(), "organization type is required");
        Objects.requireNonNull(command.state(), "organization state is required");
        if (command.state() == OrganizationState.ARCHIVED) {
            throw new IllegalArgumentException("Use the archive operation to archive organizations");
        }
    }

    private static Set<Long> safeAdministrators(Set<Long> administratorUserIds) {
        return administratorUserIds == null ? Set.of() : administratorUserIds;
    }

    private static void requireNotArchived(Organization organization) {
        if (organization.state() == OrganizationState.ARCHIVED) {
            throw new IllegalStateException("Archived organizations cannot be changed");
        }
    }

    private static void requireValidDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Assignment end date cannot be before start date");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void auditFailure(
            long actorUserId,
            Long sessionId,
            String operationType,
            String affectedIdentifier,
            String sourceIp
    ) {
        auditService.record(actorUserId, sessionId, operationType,
                "organization", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
