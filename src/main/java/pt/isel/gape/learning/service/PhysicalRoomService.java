package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.validation.AcademicTextValidator;
import pt.isel.gape.learning.dao.LessonDAO;
import pt.isel.gape.learning.dao.PhysicalRoomDAO;
import pt.isel.gape.learning.model.PhysicalRoom;
import pt.isel.gape.learning.model.PhysicalRoomCreateCommand;
import pt.isel.gape.learning.model.PhysicalRoomState;
import pt.isel.gape.learning.model.PhysicalRoomUpdateCommand;
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationDecision;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class PhysicalRoomService {

    private static final int CODE_MAX_LENGTH = 40;
    private static final int NAME_MAX_LENGTH = 120;
    private static final int DESCRIPTION_MAX_LENGTH = 500;
    private static final int LOCATION_MAX_LENGTH = 255;

    private final ConnectionProvider connectionProvider;
    private final PhysicalRoomDAO physicalRoomDAO;
    private final LessonDAO lessonDAO;
    private final PermissionChecker permissionChecker;
    private final AuditService auditService;
    private final Clock clock;

    public PhysicalRoomService(
            ConnectionProvider connectionProvider,
            PhysicalRoomDAO physicalRoomDAO,
            PermissionChecker permissionChecker,
            AuditService auditService
    ) {
        this(
                connectionProvider,
                physicalRoomDAO,
                new LessonDAO(connectionProvider),
                permissionChecker,
                auditService,
                Clock.systemDefaultZone()
        );
    }

    private PhysicalRoomService(
            ConnectionProvider connectionProvider,
            PhysicalRoomDAO physicalRoomDAO,
            LessonDAO lessonDAO,
            PermissionChecker permissionChecker,
            AuditService auditService,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.physicalRoomDAO = Objects.requireNonNull(physicalRoomDAO, "physicalRoomDAO is required");
        this.lessonDAO = Objects.requireNonNull(lessonDAO, "lessonDAO is required");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public PhysicalRoomService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new PhysicalRoomDAO(connectionProvider),
                new LessonDAO(connectionProvider),
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

    public PhysicalRoom createPhysicalRoom(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            PhysicalRoomCreateCommand command,
            String sourceIp
    ) {
        try {
            validateCreateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    requireOrganization(connection, command.organizationId());
                    requireOrganicUnitContext(connection, command.organicUnitId(), command.organizationId());
                    requireRoomManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            command.organizationId(),
                            command.organicUnitId(),
                            sourceIp
                    );
                    String code = physicalRoomDAO.create(connection, command);
                    auditService.record(connection, actorUserId, sessionId, "PHYSICAL_ROOM_CREATE",
                            "physical_room", code, "success", sourceIp);
                    PhysicalRoom room = requirePhysicalRoom(connection, code);
                    connection.commit();
                    return room;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "PHYSICAL_ROOM_CREATE",
                    command == null ? "new" : safeIdentifier(command.code(), "new"), sourceIp);
            throw wrap(exception, "Failed to create physical room");
        }
    }

    public PhysicalRoom getPhysicalRoom(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String code,
            String sourceIp
    ) {
        try {
            requireText(code, "Physical room code is required");
            try (Connection connection = connectionProvider.getConnection()) {
                PhysicalRoom room = requirePhysicalRoom(connection, code.trim());
                requireRoomReader(
                        connection,
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        room.organizationId(),
                        room.organicUnitId(),
                        sourceIp
                );
                return room;
            }
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to read physical room");
        }
    }

    public List<PhysicalRoom> listPhysicalRoomsByOrganization(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String sourceIp
    ) {
        try {
            if (organizationId <= 0) {
                throw new IllegalArgumentException("Physical room organization is required");
            }
            try (Connection connection = connectionProvider.getConnection()) {
                requireOrganization(connection, organizationId);
                requireRoomReader(connection, actorUserId, sessionId, actorProfileType,
                        organizationId, null, sourceIp);
            }
            return physicalRoomDAO.findByOrganization(organizationId);
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list physical rooms");
        }
    }

    public boolean canManagePhysicalRoomsByOrganization(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            String sourceIp
    ) {
        if (organizationId <= 0) {
            return false;
        }
        try (Connection connection = connectionProvider.getConnection()) {
            if (!physicalRoomDAO.organizationExists(connection, organizationId)) {
                return false;
            }
            return canManageRoom(connection, actorUserId, sessionId, actorProfileType,
                    organizationId, null, sourceIp);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check physical room management permission", exception);
        }
    }

    public boolean canManagePhysicalRoom(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String code,
            String sourceIp
    ) {
        if (code == null || code.isBlank()) {
            return false;
        }
        try (Connection connection = connectionProvider.getConnection()) {
            return physicalRoomDAO.findByCode(connection, code.trim())
                    .map(room -> {
                        try {
                            return canManageRoom(
                                    connection,
                                    actorUserId,
                                    sessionId,
                                    actorProfileType,
                                    room.organizationId(),
                                    room.organicUnitId(),
                                    sourceIp
                            );
                        } catch (SQLException exception) {
                            throw new IllegalStateException(
                                    "Failed to check physical room management permission",
                                    exception
                            );
                        }
                    })
                    .orElse(false);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check physical room management permission", exception);
        }
    }

    public PhysicalRoom updatePhysicalRoom(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String code,
            PhysicalRoomUpdateCommand command,
            String sourceIp
    ) {
        try {
            requireText(code, "Physical room code is required");
            validateUpdateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    PhysicalRoom current = requirePhysicalRoomLocked(connection, code.trim());
                    requireOrganization(connection, command.organizationId());
                    requireOrganicUnitContext(connection, command.organicUnitId(), command.organizationId());
                    requireRoomManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            current.organizationId(),
                            current.organicUnitId(),
                            sourceIp
                    );
                    if (current.organizationId() != command.organizationId()
                            && physicalRoomDAO.hasDomainDependencies(connection, current.code())) {
                        throw new IllegalStateException("Physical room organization cannot change while lessons depend on it");
                    }
                    synchronizeLessonStates(connection);
                    if (physicalRoomDAO.hasReservedLessonExceedingCapacity(
                            connection,
                            current.code(),
                            command.capacity()
                    )) {
                        throw new IllegalStateException("Physical room capacity is below active class group enrollments");
                    }
                    if (command.state() != PhysicalRoomState.ACTIVE
                            && physicalRoomDAO.hasReservedLessons(connection, current.code())) {
                        throw new IllegalStateException("Physical room with scheduled or active lessons cannot be deactivated");
                    }
                    physicalRoomDAO.update(connection, current.code(), command);
                    auditService.record(connection, actorUserId, sessionId, "PHYSICAL_ROOM_UPDATE",
                            "physical_room", current.code(), "success", sourceIp);
                    PhysicalRoom room = requirePhysicalRoom(connection, current.code());
                    connection.commit();
                    return room;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "PHYSICAL_ROOM_UPDATE", safeIdentifier(code, "unknown"), sourceIp);
            throw wrap(exception, "Failed to update physical room");
        }
    }

    public void archivePhysicalRoom(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String code,
            String sourceIp
    ) {
        changePhysicalRoomState(actorUserId, sessionId, actorProfileType, code,
                PhysicalRoomState.ARCHIVED, "PHYSICAL_ROOM_ARCHIVE", sourceIp);
    }

    public void setPhysicalRoomState(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String code,
            PhysicalRoomState state,
            String sourceIp
    ) {
        Objects.requireNonNull(state, "physical room state is required");
        changePhysicalRoomState(actorUserId, sessionId, actorProfileType, code,
                state, "PHYSICAL_ROOM_STATE_UPDATE", sourceIp);
    }

    public void deletePhysicalRoom(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String code,
            String sourceIp
    ) {
        try {
            requireText(code, "Physical room code is required");
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    PhysicalRoom current = requirePhysicalRoomLocked(connection, code.trim());
                    requireRoomManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            current.organizationId(),
                            current.organicUnitId(),
                            sourceIp
                    );
                    if (physicalRoomDAO.hasDomainDependencies(connection, current.code())) {
                        throw new IllegalStateException("Physical room with lessons cannot be deleted");
                    }
                    physicalRoomDAO.delete(connection, current.code());
                    auditService.record(connection, actorUserId, sessionId, "PHYSICAL_ROOM_DELETE",
                            "physical_room", current.code(), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "PHYSICAL_ROOM_DELETE", safeIdentifier(code, "unknown"), sourceIp);
            throw wrap(exception, "Failed to delete physical room");
        }
    }

    private void changePhysicalRoomState(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String code,
            PhysicalRoomState state,
            String operationType,
            String sourceIp
    ) {
        try {
            requireText(code, "Physical room code is required");
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    PhysicalRoom current = requirePhysicalRoomLocked(connection, code.trim());
                    requireRoomManager(
                            connection,
                            actorUserId,
                            sessionId,
                            actorProfileType,
                            current.organizationId(),
                            current.organicUnitId(),
                            sourceIp
                    );
                    synchronizeLessonStates(connection);
                    if (state != PhysicalRoomState.ACTIVE
                            && physicalRoomDAO.hasReservedLessons(connection, current.code())) {
                        throw new IllegalStateException("Physical room with scheduled or active lessons cannot be deactivated");
                    }
                    physicalRoomDAO.updateState(connection, current.code(), state);
                    auditService.record(connection, actorUserId, sessionId, operationType,
                            "physical_room", current.code(), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, operationType, safeIdentifier(code, "unknown"), sourceIp);
            throw wrap(exception, "Failed to change physical room state");
        }
    }

    private PhysicalRoom requirePhysicalRoom(Connection connection, String code) throws SQLException {
        return physicalRoomDAO.findByCode(connection, code)
                .orElseThrow(() -> new IllegalArgumentException("Physical room not found: " + code));
    }

    private PhysicalRoom requirePhysicalRoomLocked(Connection connection, String code) throws SQLException {
        return physicalRoomDAO.lockByCode(connection, code)
                .orElseThrow(() -> new IllegalArgumentException("Physical room not found: " + code));
    }

    private void synchronizeLessonStates(Connection connection) throws SQLException {
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        lessonDAO.synchronizeTemporalStates(connection, now);
    }

    private void requireOrganization(Connection connection, long organizationId) throws SQLException {
        if (organizationId <= 0 || !physicalRoomDAO.organizationExists(connection, organizationId)) {
            throw new IllegalArgumentException("Physical room organization not found: " + organizationId);
        }
    }

    private void requireOrganicUnitContext(
            Connection connection,
            Long organicUnitId,
            long organizationId
    ) throws SQLException {
        if (organicUnitId == null) {
            return;
        }
        if (!physicalRoomDAO.organicUnitBelongsToOrganization(connection, organicUnitId, organizationId)) {
            throw new IllegalArgumentException("Physical room organic unit must belong to the same organization");
        }
    }

    private void requireRoomManager(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            Long organicUnitId,
            String sourceIp
    ) throws SQLException {
        AuthorizationDecision adminDecision = AuthorizationDecision.deny("administrator_profile_required");
        if (actorProfileType == AccessProfileType.ADMINISTRATOR) {
            AccessEntityType entityType = organicUnitId == null
                    ? AccessEntityType.ORGANIZATION
                    : AccessEntityType.ORGANIC_UNIT;
            long entityId = organicUnitId == null ? organizationId : organicUnitId;
            adminDecision = permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE,
                    entityType,
                    entityId,
                    sourceIp
            ));
            if (adminDecision.allowed()) {
                return;
            }
        }

        AuthorizationDecision coordinatorDecision = AuthorizationDecision.deny("coordinator_profile_required");
        if (actorProfileType == AccessProfileType.COORDINATOR) {
            coordinatorDecision = physicalRoomDAO.coordinatorCanManageOrganization(connection, actorUserId, organizationId)
                    ? AuthorizationDecision.allow()
                    : AuthorizationDecision.deny("coordinator_learning_context_required");
            if (coordinatorDecision.allowed()) {
                return;
            }
        }

        throw new SecurityException("Missing physical room management context: "
                + adminDecision.reason()
                + "/"
                + coordinatorDecision.reason());
    }

    private void requireRoomReader(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            Long organicUnitId,
            String sourceIp
    ) throws SQLException {
        if (canManageRoom(connection, actorUserId, sessionId, actorProfileType,
                organizationId, organicUnitId, sourceIp)) {
            return;
        }

        if (actorProfileType == AccessProfileType.COORDINATOR
                && physicalRoomDAO.coordinatorCanManageOrganization(connection, actorUserId, organizationId)) {
            return;
        }

        if (actorProfileType == AccessProfileType.TEACHER
                && physicalRoomDAO.teacherCanReadOrganization(connection, actorUserId, organizationId)) {
            return;
        }

        throw new SecurityException("Missing physical room read context");
    }

    private boolean canManageRoom(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long organizationId,
            Long organicUnitId,
            String sourceIp
    ) throws SQLException {
        try {
            requireRoomManager(connection, actorUserId, sessionId, actorProfileType,
                    organizationId, organicUnitId, sourceIp);
            return true;
        } catch (SecurityException exception) {
            return false;
        }
    }

    private static void validateCreateCommand(PhysicalRoomCreateCommand command) {
        Objects.requireNonNull(command, "command is required");
        validateCommonCommand(
                command.code(),
                command.organizationId(),
                command.name(),
                command.description(),
                command.capacity(),
                command.location(),
                command.state()
        );
        if (command.state() == PhysicalRoomState.ARCHIVED) {
            throw new IllegalArgumentException("Use the archive operation to archive physical rooms");
        }
    }

    private static void validateUpdateCommand(PhysicalRoomUpdateCommand command) {
        Objects.requireNonNull(command, "command is required");
        validateCommonCommand(
                "existing",
                command.organizationId(),
                command.name(),
                command.description(),
                command.capacity(),
                command.location(),
                command.state()
        );
        if (command.state() == PhysicalRoomState.ARCHIVED) {
            throw new IllegalArgumentException("Use the archive operation to archive physical rooms");
        }
    }

    private static void validateCommonCommand(
            String code,
            long organizationId,
            String name,
            String description,
            int capacity,
            String location,
            PhysicalRoomState state
    ) {
        requireText(code, "Physical room code is required");
        AcademicTextValidator.rejectContextSeparator(code, "Physical room code");
        requireMaxLength(code.trim(), CODE_MAX_LENGTH, "Physical room code is too long");
        if (organizationId <= 0) {
            throw new IllegalArgumentException("Physical room organization is required");
        }
        AcademicTextValidator.requireName(name, "Physical room name is required");
        requireMaxLength(name.trim(), NAME_MAX_LENGTH, "Physical room name is too long");
        requireMaxLength(description, DESCRIPTION_MAX_LENGTH, "Physical room description is too long");
        requireMaxLength(location, LOCATION_MAX_LENGTH, "Physical room location is too long");
        if (capacity <= 0) {
            throw new IllegalArgumentException("Physical room capacity must be greater than zero");
        }
        Objects.requireNonNull(state, "physical room state is required");
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireMaxLength(String value, int maxLength, String message) {
        if (value != null && value.trim().length() > maxLength) {
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
                "physical_room", affectedIdentifier, "failure", sourceIp);
    }

    private static String safeIdentifier(String identifier, String fallback) {
        return identifier == null || identifier.isBlank() ? fallback : identifier.trim();
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }
}
