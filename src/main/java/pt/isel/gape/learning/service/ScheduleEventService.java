package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.validation.AcademicTextValidator;
import pt.isel.gape.learning.dao.AssessmentDAO;
import pt.isel.gape.learning.dao.ClassGroupDAO;
import pt.isel.gape.learning.dao.LessonDAO;
import pt.isel.gape.learning.dao.ScheduleEventDAO;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.learning.model.ScheduleEvent;
import pt.isel.gape.learning.model.ScheduleEventCreateCommand;
import pt.isel.gape.learning.model.ScheduleEventType;
import pt.isel.gape.security.authorization.PermissionChecker;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class ScheduleEventService {

    private static final int TITLE_MAX_LENGTH = 160;
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    private final ConnectionProvider connectionProvider;
    private final ScheduleEventDAO scheduleEventDAO;
    private final LessonDAO lessonDAO;
    private final AssessmentDAO assessmentDAO;
    private final ScheduleAccessPolicy accessPolicy;
    private final AuditService auditService;

    public ScheduleEventService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new ScheduleEventDAO(connectionProvider),
                new LessonDAO(connectionProvider),
                new AssessmentDAO(connectionProvider),
                new ClassGroupDAO(connectionProvider),
                new PermissionDAO(connectionProvider),
                clock
        );
    }

    public ScheduleEventService(
            ConnectionProvider connectionProvider,
            ScheduleEventDAO scheduleEventDAO,
            LessonDAO lessonDAO,
            AssessmentDAO assessmentDAO,
            ClassGroupDAO classGroupDAO,
            PermissionDAO permissionDAO,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.scheduleEventDAO = Objects.requireNonNull(scheduleEventDAO, "scheduleEventDAO is required");
        this.lessonDAO = Objects.requireNonNull(lessonDAO, "lessonDAO is required");
        this.assessmentDAO = Objects.requireNonNull(assessmentDAO, "assessmentDAO is required");
        Objects.requireNonNull(classGroupDAO, "classGroupDAO is required");
        Objects.requireNonNull(permissionDAO, "permissionDAO is required");
        Objects.requireNonNull(clock, "clock is required");
        this.accessPolicy = new ScheduleAccessPolicy(
                classGroupDAO,
                lessonDAO,
                assessmentDAO,
                new PermissionChecker(connectionProvider),
                permissionDAO
        );
        this.auditService = new AuditService(new ActivityLogDAO(connectionProvider), clock);
    }

    public ScheduleEvent createEvent(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            ScheduleEventCreateCommand command,
            String sourceIp
    ) {
        try {
            validateCommand(command);
            try (Connection connection = connectionProvider.getConnection()) {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    NormalizedScheduleEvent normalized = normalize(connection, command);
                    for (Long classGroupId : normalized.classGroupIds()) {
                        accessPolicy.requireClassGroupManager(
                                connection,
                                actorUserId,
                                sessionId,
                                actorProfileType,
                                classGroupId,
                                sourceIp
                        );
                    }
                    long eventId = scheduleEventDAO.create(connection, normalized.command());
                    scheduleEventDAO.replaceClassGroups(connection, eventId, normalized.classGroupIds());
                    scheduleEventDAO.replaceRecipients(
                            connection,
                            eventId,
                            scheduleEventDAO.findRecipientIdsForClassGroups(connection, normalized.classGroupIds())
                    );
                    auditService.record(connection, actorUserId, sessionId, "SCHEDULE_EVENT_CREATE",
                            "schedule_event", Long.toString(eventId), "success", sourceIp);
                    ScheduleEvent event = requireEvent(connection, eventId);
                    connection.commit();
                    return event;
                } catch (RuntimeException | SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        } catch (RuntimeException | SQLException exception) {
            auditFailure(actorUserId, sessionId, "SCHEDULE_EVENT_CREATE", "new", sourceIp);
            throw wrap(exception, "Failed to create schedule event");
        }
    }

    public List<ScheduleEvent> listVisibleEvents(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            return switch (actorProfileType) {
                case ADMINISTRATOR -> scheduleEventDAO.findVisibleForAdministrator(connection, actorUserId);
                case COORDINATOR -> scheduleEventDAO.findVisibleForCoordinator(connection, actorUserId);
                case TEACHER -> scheduleEventDAO.findVisibleForTeacher(connection, actorUserId);
                case STUDENT -> {
                    accessPolicy.requireStudentProfile(actorUserId, sessionId, actorProfileType, sourceIp);
                    yield scheduleEventDAO.findVisibleForStudent(connection, actorUserId);
                }
            };
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to list visible schedule events");
        }
    }

    public ScheduleEvent getEvent(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            long eventId,
            String sourceIp
    ) {
        try (Connection connection = connectionProvider.getConnection()) {
            ScheduleEvent event = requireEvent(connection, eventId);
            if (actorProfileType == AccessProfileType.STUDENT) {
                boolean visible = scheduleEventDAO.findVisibleForStudent(connection, actorUserId).stream()
                        .anyMatch(candidate -> candidate.id() == eventId);
                if (!visible) {
                    throw new SecurityException("Student cannot access schedule event");
                }
                return event;
            }
            for (Long classGroupId : event.classGroupIds()) {
                accessPolicy.requireClassGroupManager(
                        connection,
                        actorUserId,
                        sessionId,
                        actorProfileType,
                        classGroupId,
                        sourceIp
                );
            }
            return event;
        } catch (SQLException exception) {
            throw wrap(exception, "Failed to read schedule event");
        }
    }

    private NormalizedScheduleEvent normalize(Connection connection, ScheduleEventCreateCommand command)
            throws SQLException {
        if (command.lessonId() != null && command.assessmentId() != null) {
            throw new IllegalArgumentException("Schedule event cannot be linked to lesson and assessment at once");
        }
        if (command.lessonId() != null) {
            Lesson lesson = lessonDAO.findById(connection, command.lessonId())
                    .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + command.lessonId()));
            if (command.type() != ScheduleEventType.LESSON) {
                throw new IllegalArgumentException("Lesson schedule events require lesson type");
            }
            if (!command.startsAt().equals(lesson.startsAt()) || !command.endsAt().equals(lesson.endsAt())) {
                throw new IllegalArgumentException("Schedule event period must match the referenced lesson");
            }
            return new NormalizedScheduleEvent(command, List.of(lesson.classGroupId()));
        }
        if (command.assessmentId() != null) {
            Assessment assessment = assessmentDAO.findById(connection, command.assessmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + command.assessmentId()));
            if (command.type() != ScheduleEventType.ASSESSMENT) {
                throw new IllegalArgumentException("Assessment schedule events require assessment type");
            }
            if (assessment.availableFrom() != null && command.startsAt().isBefore(assessment.availableFrom())) {
                throw new IllegalArgumentException("Schedule event starts before assessment availability");
            }
            if (assessment.availableUntil() != null && command.endsAt().isAfter(assessment.availableUntil())) {
                throw new IllegalArgumentException("Schedule event ends after assessment availability");
            }
            List<Long> applicableClassGroupIds = assessmentDAO.findApplicableClassGroupIds(connection, assessment.id());
            List<Long> requestedClassGroupIds = orderedUnique(command.classGroupIds());
            List<Long> classGroupIds = requestedClassGroupIds.isEmpty()
                    ? applicableClassGroupIds
                    : requestedClassGroupIds;
            if (!applicableClassGroupIds.isEmpty() && !applicableClassGroupIds.containsAll(classGroupIds)) {
                throw new IllegalArgumentException("Schedule event class groups must match the assessment context");
            }
            if (applicableClassGroupIds.isEmpty() && !requestedClassGroupIds.isEmpty()) {
                if (assessment.subjectId() == null
                        || !assessmentDAO.classGroupsMatchSubject(connection, assessment.subjectId(), classGroupIds)) {
                    throw new IllegalArgumentException("Schedule event class groups must match the assessment subject");
                }
            }
            if (classGroupIds.isEmpty()) {
                throw new IllegalArgumentException("Assessment schedule events require class group context");
            }
            return new NormalizedScheduleEvent(
                    new ScheduleEventCreateCommand(
                            command.lessonId(),
                            command.assessmentId(),
                            command.title(),
                            command.description(),
                            command.type(),
                            command.startsAt(),
                            command.endsAt(),
                            command.allDay(),
                            command.reminderEnabled(),
                            command.reminderMinutesBefore(),
                            command.state(),
                            classGroupIds
                    ),
                    classGroupIds
            );
        }
        if (command.type() == ScheduleEventType.LESSON || command.type() == ScheduleEventType.ASSESSMENT) {
            throw new IllegalArgumentException("Lesson and assessment events require their referenced entity");
        }
        List<Long> classGroupIds = orderedUnique(command.classGroupIds());
        if (classGroupIds.isEmpty()) {
            throw new IllegalArgumentException("Schedule event requires at least one class group");
        }
        for (Long classGroupId : classGroupIds) {
            if (!scheduleEventDAO.classGroupExists(connection, classGroupId)) {
                throw new IllegalArgumentException("Class group not found: " + classGroupId);
            }
        }
        return new NormalizedScheduleEvent(command, classGroupIds);
    }

    private ScheduleEvent requireEvent(Connection connection, long eventId) throws SQLException {
        return scheduleEventDAO.findById(connection, eventId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule event not found: " + eventId));
    }

    private static void validateCommand(ScheduleEventCreateCommand command) {
        Objects.requireNonNull(command, "command is required");
        AcademicTextValidator.requireName(command.title(), "Schedule event title is required");
        requireMaxLength(command.title().trim(), TITLE_MAX_LENGTH, "Schedule event title is too long");
        requireMaxLength(command.description(), DESCRIPTION_MAX_LENGTH, "Schedule event description is too long");
        Objects.requireNonNull(command.type(), "schedule event type is required");
        Objects.requireNonNull(command.startsAt(), "schedule event start is required");
        Objects.requireNonNull(command.endsAt(), "schedule event end is required");
        Objects.requireNonNull(command.state(), "schedule event state is required");
        if (command.endsAt().isBefore(command.startsAt())) {
            throw new IllegalArgumentException("Schedule event end cannot be before start");
        }
        if (Boolean.TRUE.equals(command.reminderEnabled())
                && command.reminderMinutesBefore() == null) {
            throw new IllegalArgumentException("Enabled reminders require minutes before");
        }
        if (command.reminderMinutesBefore() != null && command.reminderMinutesBefore() < 0) {
            throw new IllegalArgumentException("Reminder minutes cannot be negative");
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
                "schedule_event", affectedIdentifier, "failure", sourceIp);
    }

    private static RuntimeException wrap(Exception exception, String message) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(message, exception);
    }

    private static List<Long> orderedUnique(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long value : values) {
            if (value == null || value <= 0) {
                throw new IllegalArgumentException("Class group ids must be positive");
            }
            unique.add(value);
        }
        return List.copyOf(unique);
    }

    private record NormalizedScheduleEvent(ScheduleEventCreateCommand command, List<Long> classGroupIds) {
    }
}
