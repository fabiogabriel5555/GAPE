package pt.isel.gape.structure.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AdministratorPermissionAssignment;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.validation.AcademicTextValidator;
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationDecision;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.OrganicUnitDAO;
import pt.isel.gape.structure.dao.OrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.OrganicUnitCreateCommand;
import pt.isel.gape.structure.model.OrganicUnitState;
import pt.isel.gape.structure.model.OrganicUnitType;
import pt.isel.gape.structure.model.OrganicUnitUpdateCommand;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.structure.model.OrganizationState;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class OrganicUnitService {

    private final ConnectionProvider connectionProvider;
    private final OrganicUnitDAO organicUnitDAO;
    private final OrganizationDAO organizationDAO;
    private final PermissionDAO permissionDAO;
    private final PermissionChecker permissionChecker;
    private final AuditService auditService;

    public OrganicUnitService(
            ConnectionProvider connectionProvider,
            OrganicUnitDAO organicUnitDAO,
            OrganizationDAO organizationDAO,
            PermissionChecker permissionChecker,
            AuditService auditService
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.organicUnitDAO = Objects.requireNonNull(organicUnitDAO, "organicUnitDAO is required");
        this.organizationDAO = Objects.requireNonNull(organizationDAO, "organizationDAO is required");
        this.permissionDAO = new PermissionDAO(connectionProvider);
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
    }

    public OrganicUnitService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new OrganicUnitDAO(connectionProvider),
                new OrganizationDAO(connectionProvider),
                new PermissionChecker(
                        new PermissionDAO(connectionProvider),
                        new ManageOrganizationDAO(connectionProvider),
                        new CoordinateSubjectDAO(connectionProvider),
                        new TeachClassGroupDAO(connectionProvider)
                ),
                new AuditService(new ActivityLogDAO(connectionProvider), clock)
        );
    }

    public OrganicUnit createOrganicUnit(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            OrganicUnitCreateCommand command,
            String sourceIp
    ) {
        try {
            validateCreateCommand(command);
            requireOrganicUnitCreateContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    command.organizationId(),
                    command.parentOrganicUnitId(),
                    sourceIp
            );
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Organization organization = requireOrganization(connection, command.organizationId());
                    requireOrganizationActive(organization);
                    validateParent(connection, null, command.organizationId(), command.parentOrganicUnitId());
                    OrganicUnitCreateCommand commandWithGeneratedCode = new OrganicUnitCreateCommand(
                            command.organizationId(),
                            nextOrganicUnitCode(connection, command.organizationId(), command.type()),
                            command.name(),
                            command.acronym(),
                            command.type(),
                            command.state(),
                            command.parentOrganicUnitId()
                    );
                    long organicUnitId = organicUnitDAO.create(connection, commandWithGeneratedCode);
                    auditService.record(connection, actorUserId, sessionId, "ORGANIC_UNIT_CREATE",
                            "organic_unit", Long.toString(organicUnitId), "success", sourceIp);
                    connection.commit();
                    return organicUnitDAO.findById(organicUnitId)
                            .orElseThrow(() -> new IllegalStateException("Created organic unit was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ORGANIC_UNIT_CREATE", "new", sourceIp);
            throw wrap(exception, "Failed to create organic unit");
        }
    }

    public OrganicUnit getOrganicUnit(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organicUnitId,
            String sourceIp
    ) {
        try {
            OrganicUnit unit = organicUnitDAO.findById(organicUnitId)
                    .orElseThrow(() -> new IllegalArgumentException("Organic unit not found: " + organicUnitId));
            requireOrganicUnitAccess(actorUserId, sessionId, actorProfileType, unit.id(), sourceIp);
            return unit;
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to read organic unit");
        }
    }

    public List<OrganicUnit> listOrganicUnits(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String sourceIp
    ) {
        try {
            if (organizationManagerDecision(actorUserId, sessionId, actorProfileType, organizationId, sourceIp).allowed()) {
                return organicUnitDAO.findByOrganization(organizationId);
            }
            try (Connection connection = connectionProvider.getConnection()) {
                requireOrganization(connection, organizationId);
            }
            return organicUnitDAO.findByOrganization(organizationId)
                    .stream()
                    .filter(unit -> organicUnitAccessDecision(
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            unit.id(),
                            sourceIp
                    ).allowed() || organicUnitMutationDecision(
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            unit.id(),
                            sourceIp
                    ).allowed())
                    .toList();
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list organic units");
        }
    }

    public OrganicUnit updateOrganicUnit(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organicUnitId,
            OrganicUnitUpdateCommand command,
            String sourceIp
    ) {
        try {
            validateUpdateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    OrganicUnit current = requireOrganicUnit(connection, organicUnitId);
                    requireOrganicUnitMutationContext(actorUserId, sessionId, actorProfileType, current.id(), sourceIp);
                    requireOrganicUnitCreateContext(
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            current.organizationId(),
                            command.parentOrganicUnitId(),
                            sourceIp
                    );
                    Organization organization = requireOrganization(connection, current.organizationId());
                    requireOrganizationActive(organization);
                    requireOrganicUnitActive(current);
                    validateParent(connection, organicUnitId, current.organizationId(), command.parentOrganicUnitId());
                    OrganicUnitUpdateCommand commandWithManagedCode = new OrganicUnitUpdateCommand(
                            current.type() == command.type()
                                    ? current.code()
                                    : nextOrganicUnitCode(connection, current.organizationId(), command.type()),
                            command.name(),
                            command.acronym(),
                            command.type(),
                            command.state(),
                            command.parentOrganicUnitId()
                    );
                    organicUnitDAO.update(connection, organicUnitId, commandWithManagedCode);
                    auditService.record(connection, actorUserId, sessionId, "ORGANIC_UNIT_UPDATE",
                            "organic_unit", Long.toString(organicUnitId), "success", sourceIp);
                    connection.commit();
                    return organicUnitDAO.findById(organicUnitId)
                            .orElseThrow(() -> new IllegalStateException("Updated organic unit was not found"));
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ORGANIC_UNIT_UPDATE", Long.toString(organicUnitId), sourceIp);
            throw wrap(exception, "Failed to update organic unit");
        }
    }

    public void archiveOrganicUnit(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organicUnitId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    OrganicUnit current = requireOrganicUnit(connection, organicUnitId);
                    requireOrganicUnitMutationContext(actorUserId, sessionId, actorProfileType, current.id(), sourceIp);
                    Organization organization = requireOrganization(connection, current.organizationId());
                    requireOrganizationActive(organization);
                    organicUnitDAO.updateState(connection, organicUnitId, OrganicUnitState.INACTIVE);
                    auditService.record(connection, actorUserId, sessionId, "ORGANIC_UNIT_ARCHIVE",
                            "organic_unit", Long.toString(organicUnitId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ORGANIC_UNIT_ARCHIVE", Long.toString(organicUnitId), sourceIp);
            throw wrap(exception, "Failed to archive organic unit");
        }
    }

    public void deleteOrganicUnit(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organicUnitId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    OrganicUnit current = requireOrganicUnit(connection, organicUnitId);
                    requireOrganicUnitMutationContext(actorUserId, sessionId, actorProfileType, current.id(), sourceIp);
                    Organization organization = requireOrganization(connection, current.organizationId());
                    requireOrganizationActive(organization);
                    requireOrganicUnitActive(current);
                    if (organicUnitDAO.hasDomainDependencies(connection, organicUnitId)) {
                        throw new IllegalStateException("Organic unit with domain dependencies cannot be deleted");
                    }
                    organicUnitDAO.delete(connection, organicUnitId);
                    auditService.record(connection, actorUserId, sessionId, "ORGANIC_UNIT_DELETE",
                            "organic_unit", Long.toString(organicUnitId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ORGANIC_UNIT_DELETE", Long.toString(organicUnitId), sourceIp);
            throw wrap(exception, "Failed to delete organic unit");
        }
    }

    public boolean canCreateOrganicUnit(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            Long parentOrganicUnitId,
            String sourceIp
    ) {
        return organicUnitCreateDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                organizationId,
                parentOrganicUnitId,
                sourceIp
        ).allowed();
    }

    public boolean canModifyOrganicUnit(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organicUnitId,
            String sourceIp
    ) {
        return organicUnitMutationDecision(actorUserId, sessionId, actorProfileType, organicUnitId, sourceIp).allowed();
    }

    public Set<Long> listDirectOrganicUnitAdministratorIds(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organicUnitId,
            String sourceIp
    ) {
        try {
            requireOrganicUnitAccess(actorUserId, sessionId, actorProfileType, organicUnitId, sourceIp);
            try (Connection connection = connectionProvider.getConnection()) {
                return permissionDAO.findActiveAdministratorUserIdsByExactContext(
                        connection,
                        AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE,
                        AccessEntityType.ORGANIC_UNIT,
                        organicUnitId
                );
            }
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list organic unit administrators");
        }
    }

    public Set<Long> listEligibleOrganicUnitAdministratorIds(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organicUnitId,
            String sourceIp
    ) {
        try {
            OrganicUnit unit = organicUnitDAO.findById(organicUnitId)
                    .orElseThrow(() -> new IllegalArgumentException("Organic unit not found: " + organicUnitId));
            requireOrganicUnitGrantDelegation(actorUserId, sessionId, actorProfileType, organicUnitId, sourceIp);
            try (Connection connection = connectionProvider.getConnection()) {
                Organization organization = requireOrganization(connection, unit.organizationId());
                requireOrganizationActive(organization);
                requireOrganicUnitActive(unit);
                Set<Long> eligible = new LinkedHashSet<>();
                for (Long adminUserId : permissionDAO.findActiveAdministratorUserIds(connection)) {
                    Set<AdministratorPermissionAssignment> assignments =
                            permissionDAO.findActiveAdministratorAssignments(connection, adminUserId);
                    if (permissionDAO.hasExactAdministratorContextGrant(
                            connection,
                            adminUserId,
                            AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE,
                            AccessEntityType.ORGANIC_UNIT,
                            organicUnitId
                    )) {
                        continue;
                    }
                    if (assignments.stream().allMatch(OrganicUnitService::isOrganizationStructureAssignment)) {
                        eligible.add(adminUserId);
                    }
                }
                return java.util.Collections.unmodifiableSet(eligible);
            }
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list eligible organic unit administrators");
        }
    }

    public boolean canRevokeOrganicUnitAdministrator(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organicUnitId,
            long adminUserId,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            requireOrganicUnitGrantDelegation(actorUserId, sessionId, actorProfileType, organicUnitId, sourceIp);
            return permissionDAO.hasExactAdministratorContextGrant(
                    connection,
                    adminUserId,
                    AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE,
                    AccessEntityType.ORGANIC_UNIT,
                    organicUnitId
            ) && permissionDAO.findActiveAdministratorAssignments(connection, adminUserId).size() > 1;
        } catch (RuntimeException | SQLException exception) {
            return false;
        }
    }

    public void assignOrganicUnitAdministrator(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organicUnitId,
            long adminUserId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    OrganicUnit unit = requireOrganicUnit(connection, organicUnitId);
                    requireOrganicUnitGrantDelegation(actorUserId, sessionId, actorProfileType, organicUnitId, sourceIp);
                    Organization organization = requireOrganization(connection, unit.organizationId());
                    requireOrganizationActive(organization);
                    requireOrganicUnitActive(unit);
                    requireAssignableOrganicUnitAdministrator(connection, adminUserId, organicUnitId);
                    permissionDAO.grantAdministratorPermission(
                            connection,
                            adminUserId,
                            organicUnitAdministratorAssignment(organicUnitId)
                    );
                    auditService.record(connection, actorUserId, sessionId, "ORGANIC_UNIT_ADMIN_ASSIGN",
                            "organic_unit", Long.toString(organicUnitId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ORGANIC_UNIT_ADMIN_ASSIGN", Long.toString(organicUnitId), sourceIp);
            throw wrap(exception, "Failed to assign organic unit administrator");
        }
    }

    public void revokeOrganicUnitAdministrator(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organicUnitId,
            long adminUserId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    OrganicUnit unit = requireOrganicUnit(connection, organicUnitId);
                    requireOrganicUnitGrantDelegation(actorUserId, sessionId, actorProfileType, organicUnitId, sourceIp);
                    Organization organization = requireOrganization(connection, unit.organizationId());
                    requireOrganizationActive(organization);
                    requireOrganicUnitActive(unit);
                    AdministratorPermissionAssignment assignment = organicUnitAdministratorAssignment(organicUnitId);
                    if (!permissionDAO.hasExactAdministratorContextGrant(
                            connection,
                            adminUserId,
                            AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE,
                            AccessEntityType.ORGANIC_UNIT,
                            organicUnitId
                    )) {
                        throw new IllegalArgumentException("Administrator does not manage this organic unit");
                    }
                    if (permissionDAO.findActiveAdministratorAssignments(connection, adminUserId).size() <= 1) {
                        throw new IllegalStateException("Administrator profile must keep at least one permission assignment");
                    }
                    permissionDAO.deleteAdministratorAssignment(connection, adminUserId, assignment);
                    auditService.record(connection, actorUserId, sessionId, "ORGANIC_UNIT_ADMIN_REVOKE",
                            "organic_unit", Long.toString(organicUnitId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "ORGANIC_UNIT_ADMIN_REVOKE", Long.toString(organicUnitId), sourceIp);
            throw wrap(exception, "Failed to revoke organic unit administrator");
        }
    }

    private void validateParent(
            Connection connection,
            Long organicUnitId,
            long organizationId,
            Long parentOrganicUnitId
    ) throws SQLException {
        if (parentOrganicUnitId == null) {
            return;
        }
        if (organicUnitId != null && parentOrganicUnitId.equals(organicUnitId)) {
            throw new IllegalArgumentException("Organic unit cannot be subordinated to itself");
        }

        Set<Long> visited = new HashSet<>();
        Long currentParentId = parentOrganicUnitId;
        while (currentParentId != null) {
            if (!visited.add(currentParentId)) {
                throw new IllegalArgumentException("Existing organic unit hierarchy already contains a cycle");
            }
            if (organicUnitId != null && currentParentId.equals(organicUnitId)) {
                throw new IllegalArgumentException("Organic unit hierarchy cannot contain cycles");
            }
            OrganicUnit parent = requireOrganicUnit(connection, currentParentId);
            if (parent.organizationId() != organizationId) {
                throw new IllegalArgumentException("Parent organic unit must belong to the same organization");
            }
            requireOrganicUnitActive(parent);
            currentParentId = parent.parentOrganicUnitId();
        }
    }

    private void requireOrganizationManager(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String sourceIp
    ) {
        AuthorizationDecision decision = organizationManagerDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                organizationId,
                sourceIp
        );
        if (!decision.allowed()) {
            throw new SecurityException("Missing organization management context: " + decision.reason());
        }
    }

    private AuthorizationDecision organizationManagerDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String sourceIp
    ) {
        return permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_ORGANIZATIONS,
                AccessEntityType.ORGANIZATION,
                organizationId,
                sourceIp
        ));
    }

    private void requireOrganicUnitCreateContext(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            Long parentOrganicUnitId,
            String sourceIp
    ) {
        AuthorizationDecision decision = organicUnitCreateDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                organizationId,
                parentOrganicUnitId,
                sourceIp
        );
        if (!decision.allowed()) {
            throw new SecurityException("Missing organic unit creation context: " + decision.reason());
        }
    }

    private void requireOrganicUnitAccess(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organicUnitId,
            String sourceIp
    ) {
        AuthorizationDecision decision = organicUnitAccessDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                organicUnitId,
                sourceIp
        );
        if (!decision.allowed()) {
            throw new SecurityException("Missing organic unit management context: " + decision.reason());
        }
    }

    private AuthorizationDecision organicUnitAccessDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organicUnitId,
            String sourceIp
    ) {
        AuthorizationDecision directDecision = permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_ORGANIZATIONS,
                AccessEntityType.ORGANIC_UNIT,
                organicUnitId,
                sourceIp
        ));
        if (directDecision.allowed()) {
            return directDecision;
        }
        AuthorizationDecision descendantDecision = organicUnitMutationDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                organicUnitId,
                sourceIp
        );
        return descendantDecision.allowed()
                ? descendantDecision
                : AuthorizationDecision.deny(directDecision.reason() + "/" + descendantDecision.reason());
    }

    private AuthorizationDecision organicUnitCreateDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            Long parentOrganicUnitId,
            String sourceIp
    ) {
        AccessEntityType entityType = parentOrganicUnitId == null
                ? AccessEntityType.ORGANIZATION
                : AccessEntityType.ORGANIC_UNIT;
        long entityId = parentOrganicUnitId == null ? organizationId : parentOrganicUnitId;
        return permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_ORGANIZATIONS,
                entityType,
                entityId,
                sourceIp
        ));
    }

    private void requireOrganicUnitMutationContext(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organicUnitId,
            String sourceIp
    ) {
        AuthorizationDecision decision = organicUnitMutationDecision(
                actorUserId,
                sessionId,
                actorProfileType,
                organicUnitId,
                sourceIp
        );
        if (!decision.allowed()) {
            throw new SecurityException("Missing organic unit ancestor context: " + decision.reason());
        }
    }

    private AuthorizationDecision organicUnitMutationDecision(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organicUnitId,
            String sourceIp
    ) {
        return permissionChecker.checkDescendant(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_ORGANIZATIONS,
                AccessEntityType.ORGANIC_UNIT,
                organicUnitId,
                sourceIp
        ));
    }

    private void requireOrganicUnitGrantDelegation(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organicUnitId,
            String sourceIp
    ) {
        AuthorizationDecision decision = permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE,
                AccessEntityType.ORGANIC_UNIT,
                organicUnitId,
                sourceIp
        ));
        if (!decision.allowed()) {
            throw new SecurityException("Administrator cannot manage organic unit administrators outside own context: "
                    + decision.reason());
        }
    }

    private void requireAssignableOrganicUnitAdministrator(
            Connection connection,
            long adminUserId,
            long organicUnitId
    ) throws SQLException {
        if (!permissionDAO.findActiveAdministratorUserIds(connection).contains(adminUserId)) {
            throw new IllegalArgumentException("Organic unit administrator must be an active administrator");
        }
        if (permissionDAO.hasExactAdministratorContextGrant(
                connection,
                adminUserId,
                AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE,
                AccessEntityType.ORGANIC_UNIT,
                organicUnitId
        )) {
            throw new IllegalArgumentException("Administrator already manages this organic unit");
        }
        Set<AdministratorPermissionAssignment> assignments =
                permissionDAO.findActiveAdministratorAssignments(connection, adminUserId);
        if (!assignments.stream().allMatch(OrganicUnitService::isOrganizationStructureAssignment)) {
            throw new IllegalArgumentException("Administrator already has a different administrator permission scope");
        }
    }

    private static boolean isOrganizationStructureAssignment(AdministratorPermissionAssignment assignment) {
        return AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE.equals(assignment.permissionCode());
    }

    private static AdministratorPermissionAssignment organicUnitAdministratorAssignment(long organicUnitId) {
        return new AdministratorPermissionAssignment(
                AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE,
                AccessEntityType.ORGANIC_UNIT,
                organicUnitId
        );
    }

    private Organization requireOrganization(Connection connection, long organizationId) throws SQLException {
        return organizationDAO.findById(connection, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));
    }

    private OrganicUnit requireOrganicUnit(Connection connection, long organicUnitId) throws SQLException {
        return organicUnitDAO.findById(connection, organicUnitId)
                .orElseThrow(() -> new IllegalArgumentException("Organic unit not found: " + organicUnitId));
    }

    private static void validateCreateCommand(OrganicUnitCreateCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.organizationId() <= 0) {
            throw new IllegalArgumentException("Organic unit organization is required");
        }
        AcademicTextValidator.requireName(command.name(), "Organic unit name is required");
        AcademicTextValidator.requireAcronym(command.acronym(), "Organic unit acronym is required");
        Objects.requireNonNull(command.type(), "organic unit type is required");
        Objects.requireNonNull(command.state(), "organic unit state is required");
    }

    private static void validateUpdateCommand(OrganicUnitUpdateCommand command) {
        Objects.requireNonNull(command, "command is required");
        AcademicTextValidator.requireName(command.name(), "Organic unit name is required");
        AcademicTextValidator.requireAcronym(command.acronym(), "Organic unit acronym is required");
        Objects.requireNonNull(command.type(), "organic unit type is required");
        Objects.requireNonNull(command.state(), "organic unit state is required");
    }

    private String nextOrganicUnitCode(Connection connection, long organizationId, OrganicUnitType type) throws SQLException {
        String prefix = codePrefix(type);
        int maxSequence = 0;
        for (String existingCode : organicUnitDAO.findCodesByPrefix(connection, organizationId, prefix)) {
            String suffix = existingCode.substring(prefix.length() + 1);
            try {
                maxSequence = Math.max(maxSequence, Integer.parseInt(suffix));
            } catch (NumberFormatException ignored) {
                // Ignore legacy/manual codes that share the prefix but not the managed sequence format.
            }
        }
        return "%s-%03d".formatted(prefix, maxSequence + 1);
    }

    private static String codePrefix(OrganicUnitType type) {
        return switch (type) {
            case SCHOOL -> "SCH";
            case FACULTY -> "FAC";
            case DEPARTMENT -> "DEP";
            case CENTER -> "CTR";
            case OFFICE -> "OFF";
            case SERVICE -> "SRV";
            case SECTION -> "SEC";
            case DIRECTION -> "DIR";
            case OTHER -> "UNT";
        };
    }

    private static void requireOrganizationActive(Organization organization) {
        if (organization.state() != OrganizationState.ACTIVE) {
            throw new IllegalStateException("Inactive organizations cannot be changed");
        }
    }

    private static void requireOrganicUnitActive(OrganicUnit organicUnit) {
        if (organicUnit.state() != OrganicUnitState.ACTIVE) {
            throw new IllegalStateException("Inactive organic units cannot be changed");
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
                "organic_unit", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
