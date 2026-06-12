package pt.isel.gape.structure.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.dao.PermissionDAO;
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
                    requireOrganizationNotArchived(organization);
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
            requireOrganizationManager(actorUserId, sessionId, actorProfileType, organizationId, sourceIp);
            return organicUnitDAO.findByOrganization(organizationId);
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
                    requireOrganizationNotArchived(organization);
                    requireOrganicUnitNotArchived(current);
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
                    requireOrganizationNotArchived(organization);
                    requireOrganicUnitNotArchived(current);
                    organicUnitDAO.updateState(connection, organicUnitId, OrganicUnitState.ARCHIVED);
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
                    requireOrganizationNotArchived(organization);
                    requireOrganicUnitNotArchived(current);
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
            requireOrganicUnitNotArchived(parent);
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
        AuthorizationDecision decision = permissionChecker.check(new AccessContext(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.MANAGE_ORGANIZATIONS,
                AccessEntityType.ORGANIC_UNIT,
                organicUnitId,
                sourceIp
        ));
        if (!decision.allowed()) {
            throw new SecurityException("Missing organic unit management context: " + decision.reason());
        }
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
        if (command.state() == OrganicUnitState.ARCHIVED) {
            throw new IllegalArgumentException("Use the archive operation to archive organic units");
        }
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

    private static void requireOrganizationNotArchived(Organization organization) {
        if (organization.state() == OrganizationState.ARCHIVED) {
            throw new IllegalStateException("Archived organizations cannot be changed");
        }
    }

    private static void requireOrganicUnitNotArchived(OrganicUnit organicUnit) {
        if (organicUnit.state() == OrganicUnitState.ARCHIVED) {
            throw new IllegalStateException("Archived organic units cannot be changed");
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
