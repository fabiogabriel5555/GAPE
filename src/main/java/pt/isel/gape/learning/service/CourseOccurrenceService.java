package pt.isel.gape.learning.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.learning.dao.CourseOccurrenceDAO;
import pt.isel.gape.learning.dao.CoursePeriodTemplateDAO;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseOccurrence;
import pt.isel.gape.learning.model.CourseOccurrenceCreateCommand;
import pt.isel.gape.learning.model.CourseOccurrenceState;
import pt.isel.gape.learning.model.CoursePeriodTemplate;
import pt.isel.gape.learning.model.CoursePeriodSchedule;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.service.AuditService;

public final class CourseOccurrenceService {

    private final ConnectionProvider connectionProvider;
    private final CourseDAO courseDAO;
    private final CourseOccurrenceDAO courseOccurrenceDAO;
    private final CoursePeriodTemplateDAO coursePeriodTemplateDAO;
    private final CourseService courseService;
    private final AuditService auditService;
    private final Clock clock;

    public CourseOccurrenceService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                connectionProvider,
                new CourseDAO(connectionProvider),
                new CourseOccurrenceDAO(connectionProvider),
                new CoursePeriodTemplateDAO(connectionProvider),
                new CourseService(connectionProvider, clock),
                new AuditService(new ActivityLogDAO(connectionProvider), clock),
                clock
        );
    }

    CourseOccurrenceService(
            ConnectionProvider connectionProvider,
            CourseDAO courseDAO,
            CourseOccurrenceDAO courseOccurrenceDAO,
            CoursePeriodTemplateDAO coursePeriodTemplateDAO,
            CourseService courseService,
            AuditService auditService,
            Clock clock
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider is required");
        this.courseDAO = Objects.requireNonNull(courseDAO, "courseDAO is required");
        this.courseOccurrenceDAO = Objects.requireNonNull(courseOccurrenceDAO, "courseOccurrenceDAO is required");
        this.coursePeriodTemplateDAO = Objects.requireNonNull(coursePeriodTemplateDAO, "coursePeriodTemplateDAO is required");
        this.courseService = Objects.requireNonNull(courseService, "courseService is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public CourseOccurrence createOccurrence(
            long actorUserId,
            Long sessionId,
            AccessProfileType actorProfileType,
            CourseOccurrenceCreateCommand command,
            String sourceIp
    ) {
        validateCommand(command);
        if (!courseService.canManageCourseChildren(
                actorUserId,
                sessionId,
                actorProfileType,
                command.courseId(),
                sourceIp
        )) {
            throw new SecurityException("Missing permission to manage course occurrences");
        }
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Course course = courseDAO.findById(connection, command.courseId())
                        .orElseThrow(() -> new IllegalArgumentException("Course not found: " + command.courseId()));
                int durationYears = durationYears(course);
                List<CoursePeriodTemplate> periodTemplates = coursePeriodTemplateDAO.findByCourse(
                        connection,
                        command.courseId()
                );
                if (periodTemplates.isEmpty()) {
                    coursePeriodTemplateDAO.replaceForCourse(
                            connection,
                            command.courseId(),
                            CourseService.normalizedPeriodTemplates(durationYears, course.frequency(), List.of())
                    );
                    periodTemplates = coursePeriodTemplateDAO.findByCourse(connection, command.courseId());
                }
                List<CoursePeriodSchedule.PeriodDates> scheduledPeriods = CoursePeriodSchedule.resolve(
                        command.referenceYear(),
                        periodTemplates
                );
                CoursePeriodSchedule.DateRange occurrenceDates = occurrenceDates(scheduledPeriods);
                requireReferenceYearIsAvailable(connection, command);
                requireOccurrenceStartsAfterPrevious(connection, command.courseId(), occurrenceDates.startsAt());
                LocalDate today = LocalDate.now(clock);
                CourseOccurrenceState occurrenceState = CourseOccurrenceState.forDates(
                        occurrenceDates.startsAt(),
                        occurrenceDates.endsAt(),
                        today
                );
                long occurrenceId = courseOccurrenceDAO.create(
                        connection,
                        command.courseId(),
                        command.referenceYear(),
                        occurrenceCode(occurrenceDates),
                        occurrenceDates.startsAt(),
                        occurrenceDates.endsAt(),
                        occurrenceState
                );
                createRequiredPeriods(connection, occurrenceId, scheduledPeriods, today);
                auditService.record(
                        connection,
                        actorUserId,
                        sessionId,
                        "COURSE_OCCURRENCE_CREATE",
                        "course_occurrence",
                        Long.toString(occurrenceId),
                        "success",
                        sourceIp
                );
                CourseOccurrence created = courseOccurrenceDAO.findById(connection, occurrenceId)
                        .orElseThrow(() -> new IllegalStateException("Created course occurrence was not found"));
                connection.commit();
                return created;
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (RuntimeException | SQLException exception) {
            try {
                auditService.record(
                        actorUserId,
                        sessionId,
                        "COURSE_OCCURRENCE_CREATE",
                        "course",
                        Long.toString(command.courseId()),
                        "failure",
                        sourceIp
                );
            } catch (RuntimeException auditException) {
                exception.addSuppressed(auditException);
            }
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Failed to create course occurrence", exception);
        }
    }

    private void createRequiredPeriods(
            Connection connection,
            long occurrenceId,
            List<CoursePeriodSchedule.PeriodDates> scheduledPeriods,
            LocalDate today
    ) throws SQLException {
        for (CoursePeriodSchedule.PeriodDates scheduledPeriod : scheduledPeriods) {
            CoursePeriodTemplate template = scheduledPeriod.template();
            LocalDate periodStart = scheduledPeriod.dates().startsAt();
            LocalDate periodEnd = scheduledPeriod.dates().endsAt();
            courseOccurrenceDAO.createPeriod(
                    connection,
                    occurrenceId,
                    template.curricularYear(),
                    template.term(),
                    periodStart,
                    periodEnd,
                    CourseOccurrenceState.forDates(periodStart, periodEnd, today)
            );
        }
    }

    private static void validateCommand(CourseOccurrenceCreateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Course occurrence is required");
        }
        if (command.courseId() <= 0) {
            throw new IllegalArgumentException("Course is required");
        }
        if (command.referenceYear() < 1900 || command.referenceYear() > 9998) {
            throw new IllegalArgumentException("Occurrence reference year must be between 1900 and 9998");
        }
    }

    private static int durationYears(Course course) {
        String duration = course.duration();
        if (duration == null || duration.isBlank() || !duration.trim().matches("\\d+")) {
            throw new IllegalArgumentException("Course duration in years is required");
        }
        int years = Integer.parseInt(duration.trim());
        if (years <= 0) {
            throw new IllegalArgumentException("Course duration must be greater than zero");
        }
        return years;
    }

    private static CoursePeriodSchedule.DateRange occurrenceDates(
            List<CoursePeriodSchedule.PeriodDates> scheduledPeriods
    ) {
        LocalDate startsAt = scheduledPeriods.stream()
                .map(period -> period.dates().startsAt())
                .min(LocalDate::compareTo)
                .orElseThrow();
        LocalDate endsAt = scheduledPeriods.stream()
                .map(period -> period.dates().endsAt())
                .max(LocalDate::compareTo)
                .orElseThrow();
        return new CoursePeriodSchedule.DateRange(startsAt, endsAt);
    }

    private void requireOccurrenceStartsAfterPrevious(
            Connection connection,
            long courseId,
            LocalDate startsAt
    ) throws SQLException {
        LocalDate latestEndDate = courseOccurrenceDAO.findLatestEndDate(connection, courseId).orElse(null);
        if (latestEndDate != null && !startsAt.isAfter(latestEndDate)) {
            throw new IllegalArgumentException(
                    "Occurrence start date must be after the previous course occurrence ends on " + latestEndDate
            );
        }
    }

    private void requireReferenceYearIsAvailable(
            Connection connection,
            CourseOccurrenceCreateCommand command
    ) throws SQLException {
        if (courseOccurrenceDAO.existsForReferenceYear(connection, command.courseId(), command.referenceYear())) {
            throw new IllegalArgumentException("A course occurrence already exists for reference year " + command.referenceYear());
        }
    }

    private static String occurrenceCode(CoursePeriodSchedule.DateRange dates) {
        int startsYear = dates.startsAt().getYear();
        int endsYear = dates.endsAt().getYear();
        StringBuilder code = new StringBuilder();
        for (int year = startsYear; year <= endsYear; year++) {
            if (!code.isEmpty()) {
                code.append('-');
            }
            code.append(year);
        }
        return code.toString();
    }
}
