package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.common.validation.AcademicTextValidator;
import pt.isel.gape.integration.videoconference.StoredVideoConferenceAdapter;
import pt.isel.gape.integration.videoconference.VideoConferenceAccess;
import pt.isel.gape.integration.videoconference.VideoConferenceAdapter;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.ContentBlockDAO;
import pt.isel.gape.learning.dao.LessonDAO;
import pt.isel.gape.learning.dao.PhysicalRoomDAO;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupState;
import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.learning.model.LessonCreateCommand;
import pt.isel.gape.learning.model.LessonState;
import pt.isel.gape.learning.model.LessonType;
import pt.isel.gape.learning.model.LessonUpdateCommand;
import pt.isel.gape.learning.model.PhysicalRoom;
import pt.isel.gape.learning.model.PhysicalRoomState;
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

public final class LessonService {

    private static final int TITLE_MAX_LENGTH = 160;
    private static final int DESCRIPTION_MAX_LENGTH = 500;
    private static final int ROOM_CODE_MAX_LENGTH = 40;

    private final ConnectionProvider connectionProvider;
    private final LessonDAO lessonDAO;
    private final PhysicalRoomDAO physicalRoomDAO;
    private final ClassGroupDAO classGroupDAO;
    private final ContentBlockDAO contentBlockDAO;
    private final PermissionDAO permissionDAO;
    private final PermissionChecker permissionChecker;
    private final VideoConferenceAdapter videoConferenceAdapter;
    private final AuditService auditService;
    private final Clock clock;

    public LessonService(
            ConnectionProvider connectionProvider,
            LessonDAO lessonDAO,
            PhysicalRoomDAO physicalRoomDAO,
            ClassGroupDAO classGroupDAO,
            ContentBlockDAO contentBlockDAO,
            PermissionDAO permissionDAO,
            PermissionChecker permissionChecker,
            VideoConferenceAdapter videoConferenceAdapter,
            AuditService auditService
    ) {
        this(
                connectionProvider,
                lessonDAO,
                physicalRoomDAO,
                classGroupDAO,
                contentBlockDAO,
                permissionDAO,
                permissionChecker,
                videoConferenceAdapter,
                auditService,
                ApplicationClock.system()
        );
    }

    private LessonService(
            ConnectionProvider connectionProvider,
            LessonDAO lessonDAO,
            PhysicalRoomDAO physicalRoomDAO,
            ClassGroupDAO classGroupDAO,
            ContentBlockDAO contentBlockDAO,
            PermissionDAO permissionDAO,
            PermissionChecker permissionChecker,
            VideoConferenceAdapter videoConferenceAdapter,
            AuditService auditService,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.lessonDAO = Objects.requireNonNull(lessonDAO, "lessonDAO is required");
        this.physicalRoomDAO = Objects.requireNonNull(physicalRoomDAO, "physicalRoomDAO is required");
        this.classGroupDAO = Objects.requireNonNull(classGroupDAO, "classGroupDAO is required");
        this.contentBlockDAO = Objects.requireNonNull(contentBlockDAO, "contentBlockDAO is required");
        this.permissionDAO = Objects.requireNonNull(permissionDAO, "permissionDAO is required");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker is required");
        this.videoConferenceAdapter = Objects.requireNonNull(videoConferenceAdapter, "videoConferenceAdapter is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public LessonService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new LessonDAO(connectionProvider),
                new PhysicalRoomDAO(connectionProvider),
                new ClassGroupDAO(connectionProvider),
                new ContentBlockDAO(connectionProvider),
                new PermissionDAO(connectionProvider),
                new StoredVideoConferenceAdapter(),
                new AuditService(new ActivityLogDAO(connectionProvider), clock),
                clock
        );
    }

    public LessonService(
            ConnectionProvider connectionProvider,
            LessonDAO lessonDAO,
            PhysicalRoomDAO physicalRoomDAO,
            ClassGroupDAO classGroupDAO,
            ContentBlockDAO contentBlockDAO,
            PermissionDAO permissionDAO,
            VideoConferenceAdapter videoConferenceAdapter,
            AuditService auditService,
            Clock clock
    ) {
        this(
                connectionProvider,
                lessonDAO,
                physicalRoomDAO,
                classGroupDAO,
                contentBlockDAO,
                permissionDAO,
                new PermissionChecker(
                        permissionDAO,
                        new ManageOrganizationDAO(connectionProvider),
                        new CoordinateSubjectDAO(connectionProvider),
                        new TeachClassGroupDAO(connectionProvider)
                ),
                videoConferenceAdapter,
                auditService,
                clock
        );
    }

    public LessonService(
            ConnectionProvider connectionProvider,
            LessonDAO lessonDAO,
            PhysicalRoomDAO physicalRoomDAO,
            ClassGroupDAO classGroupDAO,
            ContentBlockDAO contentBlockDAO,
            PermissionDAO permissionDAO,
            VideoConferenceAdapter videoConferenceAdapter,
            AuditService auditService
    ) {
        this(
                connectionProvider,
                lessonDAO,
                physicalRoomDAO,
                classGroupDAO,
                contentBlockDAO,
                permissionDAO,
                videoConferenceAdapter,
                auditService,
                ApplicationClock.system()
        );
    }

    public Lesson createLesson(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            LessonCreateCommand command,
            String sourceIp
    ) {
        try {
            LocalDateTime now = currentMinute();
            LessonCreateCommand effectiveCommand = deriveTemporalState(command, now);
            validateCreateCommand(effectiveCommand, now);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    synchronizeTemporalStates(connection, now);
                    ClassGroup classGroup = requireClassGroup(connection, effectiveCommand.classGroupId());
                    ContentBlock contentBlock = requireContentBlock(connection, effectiveCommand.contentBlockId());
                    requireLessonManager(actorUserId, sessionId, actorProfileType, classGroup, sourceIp);
                    requireActiveContext(classGroup, contentBlock);
                    requireLessonWithinClassGroupDates(
                            effectiveCommand.startsAt(),
                            effectiveCommand.endsAt(),
                            classGroup
                    );
                    LessonCreateCommand normalizedCommand = normalizeCreateCommand(
                            connection,
                            effectiveCommand,
                            classGroup,
                            null,
                            now
                    );
                    long lessonId = lessonDAO.create(connection, normalizedCommand);
                    synchronizeTemporalStates(connection, now);
                    auditService.record(connection, actorUserId, sessionId, "LESSON_CREATE",
                            "lesson", Long.toString(lessonId), "success", sourceIp);
                    Lesson lesson = requireLesson(connection, lessonId);
                    connection.commit();
                    return lesson;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "LESSON_CREATE", "new", sourceIp);
            throw wrap(exception, "Failed to create lesson");
        }
    }

    public Lesson getLesson(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long lessonId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                synchronizeTemporalStates(connection, currentMinute());
                Lesson lesson = requireLesson(connection, lessonId);
                ClassGroup classGroup = requireClassGroup(connection, lesson.classGroupId());
                ContentBlock contentBlock = requireContentBlock(connection, lesson.contentBlockId());
                requireLessonReadAccess(connection, actorUserId, sessionId, actorProfileType, classGroup, sourceIp);
                if (actorProfileType == AccessProfileType.STUDENT && contentBlock.isInactive()) {
                    throw new SecurityException("Lesson is not visible to students");
                }
                if (actorProfileType == AccessProfileType.STUDENT && !isStudentVisible(lesson)) {
                    throw new SecurityException("Lesson is not visible to students");
                }
                if (actorProfileType == AccessProfileType.STUDENT
                        && lesson.accessUrl() != null
                        && !lesson.accessUrl().isBlank()) {
                    auditService.record(actorUserId, sessionId, "LESSON_ACCESS_LINK_VIEW",
                            "lesson", Long.toString(lessonId), "success", sourceIp);
                }
                return lesson;
            }
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to read lesson");
        }
    }

    public List<Lesson> listLessonsByClassGroup(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long classGroupId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                synchronizeTemporalStates(connection, currentMinute());
                ClassGroup classGroup = requireClassGroup(connection, classGroupId);
                requireLessonReadAccess(connection, actorUserId, sessionId, actorProfileType, classGroup, sourceIp);
            }
            return lessonDAO.findByClassGroup(classGroupId);
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list lessons by class group");
        }
    }

    public List<Lesson> listLessonsByContentBlock(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long contentBlockId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                synchronizeTemporalStates(connection, currentMinute());
                ContentBlock block = requireContentBlock(connection, contentBlockId);
                ClassGroup classGroup = requireClassGroup(connection, block.classGroupId());
                requireLessonReadAccess(connection, actorUserId, sessionId, actorProfileType, classGroup, sourceIp);
                if (actorProfileType == AccessProfileType.STUDENT && block.isInactive()) {
                    return List.of();
                }
            }
            return lessonDAO.findByContentBlock(contentBlockId);
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list lessons by content block");
        }
    }

    public List<Lesson> listLessonsForStudent(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) {
        try {
            if (actorProfileType != AccessProfileType.STUDENT) {
                throw new SecurityException("Student profile is required to list student lessons");
            }
            requireActiveProfile(actorUserId, sessionId, actorProfileType, sourceIp);
            synchronizeTemporalStates();
            return lessonDAO.findForStudent(actorUserId);
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list student lessons");
        }
    }

    public List<Lesson> listPersonalCalendarLessons(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) {
        try {
            requireActiveProfile(actorUserId, sessionId, actorProfileType, sourceIp);
            synchronizeTemporalStates();
            return switch (actorProfileType) {
                case STUDENT -> lessonDAO.findPersonalCalendarForStudent(actorUserId);
                case TEACHER -> lessonDAO.findPersonalCalendarForTeacher(actorUserId);
                case COORDINATOR -> lessonDAO.findPersonalCalendarForCoordinator(actorUserId);
                case ADMINISTRATOR -> lessonDAO.findPersonalCalendarForStaff(actorUserId);
            };
        } catch (RuntimeException | SQLException exception) {
            throw wrap(exception, "Failed to list personal calendar lessons");
        }
    }

    public Lesson updateLesson(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long lessonId,
            LessonUpdateCommand command,
            String sourceIp
    ) {
        try {
            LocalDateTime now = currentMinute();
            LessonUpdateCommand effectiveCommand = deriveTemporalState(command, now);
            validateUpdateCommand(effectiveCommand, now);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    synchronizeTemporalStates(connection, now);
                    Lesson current = requireLessonLocked(connection, lessonId);
                    if (current.classGroupId() != effectiveCommand.classGroupId()) {
                        throw new IllegalArgumentException("Lesson class group cannot be changed after creation");
                    }
                    ClassGroup classGroup = requireClassGroup(connection, current.classGroupId());
                    ContentBlock contentBlock = requireContentBlock(connection, effectiveCommand.contentBlockId());
                    requireLessonManager(actorUserId, sessionId, actorProfileType, classGroup, sourceIp);
                    requireActiveContext(classGroup, contentBlock);
                    requireLessonWithinClassGroupDates(
                            effectiveCommand.startsAt(),
                            effectiveCommand.endsAt(),
                            classGroup
                    );
                    LessonUpdateCommand normalizedCommand = normalizeUpdateCommand(
                            connection,
                            effectiveCommand,
                            classGroup,
                            lessonId,
                            current.state(),
                            now
                    );
                    lessonDAO.update(connection, lessonId, normalizedCommand);
                    synchronizeTemporalStates(connection, now);
                    auditService.record(connection, actorUserId, sessionId, "LESSON_UPDATE",
                            "lesson", Long.toString(lessonId), "success", sourceIp);
                    Lesson lesson = requireLesson(connection, lessonId);
                    connection.commit();
                    return lesson;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "LESSON_UPDATE", Long.toString(lessonId), sourceIp);
            throw wrap(exception, "Failed to update lesson");
        }
    }

    public void cancelLesson(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long lessonId,
            String sourceIp
    ) {
        changeLessonState(actorUserId, sessionId, actorProfileType, lessonId,
                LessonState.CANCELLED, "LESSON_CANCEL", sourceIp);
    }

    public void completeLesson(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long lessonId,
            String sourceIp
    ) {
        changeLessonState(actorUserId, sessionId, actorProfileType, lessonId,
                LessonState.COMPLETED, "LESSON_COMPLETE", sourceIp);
    }

    public void deleteLesson(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long lessonId,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    synchronizeTemporalStates(connection, currentMinute());
                    Lesson current = requireLessonLocked(connection, lessonId);
                    ClassGroup classGroup = requireClassGroup(connection, current.classGroupId());
                    requireLessonManager(actorUserId, sessionId, actorProfileType, classGroup, sourceIp);
                    if (lessonDAO.hasDomainDependencies(connection, lessonId)) {
                        throw new IllegalStateException("Lesson with schedule or attendance history cannot be deleted");
                    }
                    lessonDAO.delete(connection, lessonId);
                    auditService.record(connection, actorUserId, sessionId, "LESSON_DELETE",
                            "lesson", Long.toString(lessonId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "LESSON_DELETE", Long.toString(lessonId), sourceIp);
            throw wrap(exception, "Failed to delete lesson");
        }
    }

    private void changeLessonState(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long lessonId,
            LessonState state,
            String operationType,
            String sourceIp
    ) {
        try {
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    synchronizeTemporalStates(connection, currentMinute());
                    Lesson current = requireLessonLocked(connection, lessonId);
                    ClassGroup classGroup = requireClassGroup(connection, current.classGroupId());
                    requireLessonManager(actorUserId, sessionId, actorProfileType, classGroup, sourceIp);
                    lessonDAO.updateState(connection, lessonId, state);
                    auditService.record(connection, actorUserId, sessionId, operationType,
                            "lesson", Long.toString(lessonId), "success", sourceIp);
                    connection.commit();
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, operationType, Long.toString(lessonId), sourceIp);
            throw wrap(exception, "Failed to change lesson state");
        }
    }

    private LessonCreateCommand normalizeCreateCommand(
            Connection connection,
            LessonCreateCommand command,
            ClassGroup classGroup,
            Long excludedLessonId,
            LocalDateTime now
    ) throws SQLException {
        LessonState effectiveState = command.state();
        NormalizedLessonAccess access = normalizeLessonAccess(
                connection,
                classGroup,
                command.type(),
                command.physicalRoomCode(),
                command.provider(),
                command.accessUrl(),
                effectiveState,
                command.startsAt(),
                command.endsAt(),
                excludedLessonId
        );
        return new LessonCreateCommand(
                command.classGroupId(),
                command.contentBlockId(),
                access.physicalRoomCode(),
                command.title().trim(),
                normalizeText(command.description()),
                command.type(),
                access.provider(),
                access.accessUrl(),
                command.attendanceRequired(),
                effectiveState,
                command.startsAt(),
                command.endsAt()
        );
    }

    private LessonUpdateCommand normalizeUpdateCommand(
            Connection connection,
            LessonUpdateCommand command,
            ClassGroup classGroup,
            long excludedLessonId,
            LessonState currentState,
            LocalDateTime now
    ) throws SQLException {
        LessonState effectiveState = command.state();
        NormalizedLessonAccess access = normalizeLessonAccess(
                connection,
                classGroup,
                command.type(),
                command.physicalRoomCode(),
                command.provider(),
                command.accessUrl(),
                effectiveState,
                command.startsAt(),
                command.endsAt(),
                excludedLessonId
        );
        return new LessonUpdateCommand(
                command.classGroupId(),
                command.contentBlockId(),
                access.physicalRoomCode(),
                command.title().trim(),
                normalizeText(command.description()),
                command.type(),
                access.provider(),
                access.accessUrl(),
                command.attendanceRequired(),
                effectiveState,
                command.startsAt(),
                command.endsAt()
        );
    }

    public int synchronizeTemporalStates() {
        try (Connection connection = connectionProvider.getConnection()) {
            return synchronizeTemporalStates(connection, currentMinute());
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to synchronize lesson states");
        }
    }

    private int synchronizeTemporalStates(Connection connection, LocalDateTime now) throws SQLException {
        return lessonDAO.synchronizeTemporalStates(connection, now);
    }

    private LocalDateTime currentMinute() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
    }

    private NormalizedLessonAccess normalizeLessonAccess(
            Connection connection,
            ClassGroup classGroup,
            LessonType type,
            String physicalRoomCode,
            String provider,
            String accessUrl,
            LessonState state,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            Long excludedLessonId
    ) throws SQLException {
        String normalizedRoomCode = normalizeText(physicalRoomCode);
        VideoConferenceAccess videoAccess = null;

        if (type == LessonType.ONLINE) {
            if (normalizedRoomCode != null) {
                throw new IllegalArgumentException("Online lessons cannot have a physical room");
            }
            videoAccess = videoConferenceAdapter.validateAccess(provider, accessUrl);
        } else if (type == LessonType.ONSITE) {
            requireText(normalizedRoomCode, "Onsite lessons require a physical room");
            if (accessUrl != null && !accessUrl.isBlank()) {
                throw new IllegalArgumentException("Onsite lessons cannot have a video conference access URL");
            }
            if (provider != null && !provider.isBlank()) {
                throw new IllegalArgumentException("Onsite lessons cannot have a video conference provider");
            }
        } else if (type == LessonType.HYBRID) {
            requireText(normalizedRoomCode, "Hybrid lessons require a physical room");
            videoAccess = videoConferenceAdapter.validateAccess(provider, accessUrl);
        }

        if (normalizedRoomCode != null) {
            requireMaxLength(normalizedRoomCode, ROOM_CODE_MAX_LENGTH, "Physical room code is too long");
            PhysicalRoom room = physicalRoomDAO.lockByCode(connection, normalizedRoomCode)
                    .orElseThrow(() -> new IllegalArgumentException("Physical room not found: " + normalizedRoomCode));
            requirePhysicalRoomAvailable(connection, room, classGroup, state, startsAt, endsAt, excludedLessonId);
        }

        return new NormalizedLessonAccess(
                normalizedRoomCode,
                videoAccess == null ? null : videoAccess.provider(),
                videoAccess == null ? null : videoAccess.accessUrl()
        );
    }

    private void requirePhysicalRoomAvailable(
            Connection connection,
            PhysicalRoom room,
            ClassGroup classGroup,
            LessonState lessonState,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            Long excludedLessonId
    ) throws SQLException {
        if (room.state() != PhysicalRoomState.ACTIVE) {
            throw new IllegalStateException("Lesson physical room must be active");
        }
        long classGroupOrganizationId = lessonDAO.findClassGroupOrganizationId(connection, classGroup.id())
                .orElseThrow(() -> new IllegalArgumentException("Class group organization not found: " + classGroup.id()));
        if (room.organizationId() != classGroupOrganizationId) {
            throw new IllegalArgumentException("Lesson physical room must belong to the same organization");
        }
        long activeEnrollments = classGroupDAO.countActiveEnrollments(connection, classGroup.id());
        if (room.capacity() < activeEnrollments) {
            throw new IllegalStateException("Physical room capacity is below active class group enrollments");
        }
        if (lessonState.reservesPhysicalRoom()
                && lessonDAO.roomHasOverlappingReservedLesson(
                        connection,
                        room.code(),
                        startsAt,
                        endsAt,
                        excludedLessonId
                )) {
            throw new IllegalStateException("Physical room already has an overlapping lesson");
        }
    }

    private Lesson requireLesson(Connection connection, long lessonId) throws SQLException {
        return lessonDAO.findById(connection, lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + lessonId));
    }

    private Lesson requireLessonLocked(Connection connection, long lessonId) throws SQLException {
        return lessonDAO.lockById(connection, lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + lessonId));
    }

    private ClassGroup requireClassGroup(Connection connection, long classGroupId) throws SQLException {
        return classGroupDAO.findById(connection, classGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Class group not found: " + classGroupId));
    }

    private ContentBlock requireContentBlock(Connection connection, long contentBlockId) throws SQLException {
        return contentBlockDAO.findById(connection, contentBlockId)
                .orElseThrow(() -> new IllegalArgumentException("Content block not found: " + contentBlockId));
    }

    private static void requireActiveContext(ClassGroup classGroup, ContentBlock contentBlock) {
        if (classGroup.state() != ClassGroupState.ACTIVE && classGroup.state() != ClassGroupState.SCHEDULED) {
            throw new IllegalStateException("Lessons require an active or scheduled class group");
        }
        if (contentBlock.classGroupId() != classGroup.id()) {
            throw new IllegalArgumentException("Lesson content block must belong to the same class group");
        }
    }

    private static void requireLessonWithinClassGroupDates(
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            ClassGroup classGroup
    ) {
        LocalDate classGroupStart = classGroup.startsAt();
        LocalDate classGroupEnd = classGroup.endsAt();
        if (startsAt != null) {
            requireLessonInstantWithinClassGroup(startsAt, classGroupStart, classGroupEnd, "Lesson start date");
        }
        if (endsAt != null) {
            requireLessonInstantWithinClassGroup(endsAt, classGroupStart, classGroupEnd, "Lesson end date");
        }
    }

    private static void requireLessonInstantWithinClassGroup(
            LocalDateTime value,
            LocalDate classGroupStart,
            LocalDate classGroupEnd,
            String label
    ) {
        LocalDate date = value.toLocalDate();
        if (classGroupStart != null && date.isBefore(classGroupStart)) {
            throw new IllegalArgumentException(label + " cannot be before the class group start date");
        }
        if (classGroupEnd != null && date.isAfter(classGroupEnd)) {
            throw new IllegalArgumentException(label + " cannot be after the class group end date");
        }
    }

    private void requireLessonManager(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ClassGroup classGroup,
            String sourceIp
    ) {
        AuthorizationDecision adminDecision = AuthorizationDecision.deny("administrator_profile_required");
        if (actorProfileType == AccessProfileType.ADMINISTRATOR) {
            adminDecision = permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_LEARNING,
                    AccessEntityType.CLASS_GROUP,
                    classGroup.id(),
                    sourceIp
            ));
            if (adminDecision.allowed()) {
                return;
            }
            if (canAdministratorManageLessonContext(actorUserId, classGroup)) {
                return;
            }
        }

        AuthorizationDecision coordinatorDecision = AuthorizationDecision.deny("coordinator_profile_required");
        if (actorProfileType == AccessProfileType.COORDINATOR) {
            coordinatorDecision = permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_LEARNING,
                    AccessEntityType.SUBJECT,
                    classGroup.subjectId(),
                    sourceIp
            ));
            if (coordinatorDecision.allowed()) {
                return;
            }
        }

        AuthorizationDecision teacherDecision = AuthorizationDecision.deny("teacher_profile_required");
        if (actorProfileType == AccessProfileType.TEACHER) {
            teacherDecision = permissionChecker.check(new AccessContext(
                    actorUserId,
                    sessionId,
                    actorProfileType,
                    AuthorizationPolicy.MANAGE_LEARNING,
                    AccessEntityType.CLASS_GROUP,
                    classGroup.id(),
                    sourceIp
            ));
            if (teacherDecision.allowed()) {
                return;
            }
        }

        throw new SecurityException("Missing lesson management context: "
                + adminDecision.reason()
                + "/" + coordinatorDecision.reason()
                + "/" + teacherDecision.reason());
    }

    private boolean canAdministratorManageLessonContext(long actorUserId, ClassGroup classGroup) {
        try {
            return permissionDAO.activeProfileExists(actorUserId, AccessProfileType.ADMINISTRATOR)
                    && (permissionDAO.hasActiveGlobalAdministratorGrant(actorUserId, AuthorizationPolicy.MANAGE_ALL)
                    || permissionDAO.hasActiveAdministratorContextGrant(
                            actorUserId,
                            AuthorizationPolicy.MANAGE_LEARNING,
                            AccessEntityType.CLASS_GROUP,
                            classGroup.id()
                    ));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check administrator lesson context", exception);
        }
    }

    private void requireLessonReadAccess(
            Connection connection,
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ClassGroup classGroup,
            String sourceIp
    ) throws SQLException {
        if (canManageLesson(actorUserId, sessionId, actorProfileType, classGroup, sourceIp)) {
            return;
        }
        if (actorProfileType == AccessProfileType.STUDENT) {
            requireActiveProfile(actorUserId, sessionId, actorProfileType, sourceIp);
            if (lessonDAO.hasCurrentStudentClassGroupAccess(connection, actorUserId, classGroup.id())) {
                return;
            }
        }
        throw new SecurityException("Missing lesson read access");
    }

    private boolean canManageLesson(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ClassGroup classGroup,
            String sourceIp
    ) {
        try {
            requireLessonManager(actorUserId, sessionId, actorProfileType, classGroup, sourceIp);
            return true;
        } catch (SecurityException exception) {
            return false;
        }
    }

    private void requireActiveProfile(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) {
        AuthorizationDecision decision = permissionChecker.check(AccessContext.global(
                actorUserId,
                sessionId,
                actorProfileType,
                AuthorizationPolicy.VIEW_REPORTS,
                sourceIp
        ));
        if (!decision.allowed()) {
            throw new SecurityException("Missing active profile: " + decision.reason());
        }
    }

    private static void validateCreateCommand(LessonCreateCommand command, LocalDateTime now) {
        Objects.requireNonNull(command, "command is required");
        validateCommonCommand(
                command.classGroupId(),
                command.contentBlockId(),
                command.title(),
                command.description(),
                command.type(),
                command.state(),
                command.startsAt(),
                command.endsAt(),
                now
        );
    }

    private static void validateUpdateCommand(LessonUpdateCommand command, LocalDateTime now) {
        Objects.requireNonNull(command, "command is required");
        validateCommonCommand(
                command.classGroupId(),
                command.contentBlockId(),
                command.title(),
                command.description(),
                command.type(),
                command.state(),
                command.startsAt(),
                command.endsAt(),
                now
        );
    }

    private static LessonCreateCommand deriveTemporalState(LessonCreateCommand command, LocalDateTime now) {
        Objects.requireNonNull(command, "command is required");
        return new LessonCreateCommand(
                command.classGroupId(),
                command.contentBlockId(),
                command.physicalRoomCode(),
                command.title(),
                command.description(),
                command.type(),
                command.provider(),
                command.accessUrl(),
                command.attendanceRequired(),
                deriveTemporalState(command.startsAt(), command.endsAt(), now),
                command.startsAt(),
                command.endsAt()
        );
    }

    private static LessonUpdateCommand deriveTemporalState(LessonUpdateCommand command, LocalDateTime now) {
        Objects.requireNonNull(command, "command is required");
        return new LessonUpdateCommand(
                command.classGroupId(),
                command.contentBlockId(),
                command.physicalRoomCode(),
                command.title(),
                command.description(),
                command.type(),
                command.provider(),
                command.accessUrl(),
                command.attendanceRequired(),
                deriveTemporalState(command.startsAt(), command.endsAt(), now),
                command.startsAt(),
                command.endsAt()
        );
    }

    private static LessonState deriveTemporalState(LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime now) {
        if (startsAt == null) {
            return LessonState.DRAFT;
        }
        if (endsAt != null && !endsAt.isAfter(now)) {
            return LessonState.COMPLETED;
        }
        if (!startsAt.isAfter(now)) {
            return LessonState.ACTIVE;
        }
        return LessonState.SCHEDULED;
    }

    private static void validateCommonCommand(
            long classGroupId,
            long contentBlockId,
            String title,
            String description,
            LessonType type,
            LessonState state,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            LocalDateTime now
    ) {
        if (classGroupId <= 0) {
            throw new IllegalArgumentException("Lesson class group is required");
        }
        if (contentBlockId <= 0) {
            throw new IllegalArgumentException("Lesson content block is required");
        }
        AcademicTextValidator.requireName(title, "Lesson title is required");
        requireMaxLength(title.trim(), TITLE_MAX_LENGTH, "Lesson title is too long");
        requireMaxLength(description, DESCRIPTION_MAX_LENGTH, "Lesson description is too long");
        Objects.requireNonNull(type, "lesson type is required");
        Objects.requireNonNull(state, "lesson state is required");
        requireValidDates(startsAt, endsAt, now);
    }

    private static void requireValidDates(LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime now) {
        Objects.requireNonNull(now, "now is required");
        if (startsAt == null) {
            throw new IllegalArgumentException("Lesson start date is required");
        }
        if (endsAt == null) {
            throw new IllegalArgumentException("Lesson end date is required");
        }
        if (startsAt.isBefore(now)) {
            throw new IllegalArgumentException("Lesson start date cannot be in the past");
        }
        if (endsAt.isBefore(now)) {
            throw new IllegalArgumentException("Lesson end date cannot be in the past");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("Lesson end date must be after start date");
        }
    }

    private static boolean isStudentVisible(Lesson lesson) {
        return lesson.state() == LessonState.SCHEDULED
                || lesson.state() == LessonState.ACTIVE
                || lesson.state() == LessonState.COMPLETED;
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

    private static String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void auditFailure(
            long actorUserId,
            Long sessionId,
            String operationType,
            String affectedIdentifier,
            String sourceIp
    ) {
        auditService.record(actorUserId, sessionId, operationType,
                "lesson", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }

    private record NormalizedLessonAccess(
            String physicalRoomCode,
            String provider,
            String accessUrl
    ) {
    }
}
