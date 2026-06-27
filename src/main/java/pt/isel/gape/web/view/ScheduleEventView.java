package pt.isel.gape.web.view;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import pt.isel.gape.learning.model.ScheduleEvent;
import pt.isel.gape.learning.model.ScheduleEventState;
import pt.isel.gape.learning.model.ScheduleEventType;

public final class ScheduleEventView {

    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-PT"));

    private final ScheduleEvent event;
    private final List<String> classGroupLabels;

    private ScheduleEventView(ScheduleEvent event, List<String> classGroupLabels) {
        this.event = event;
        this.classGroupLabels = classGroupLabels == null ? List.of() : List.copyOf(classGroupLabels);
    }

    public static ScheduleEventView from(ScheduleEvent event, List<String> classGroupLabels) {
        return new ScheduleEventView(event, classGroupLabels);
    }

    public long getId() {
        return event.id();
    }

    public Long getLessonId() {
        return event.lessonId();
    }

    public Long getAssessmentId() {
        return event.assessmentId();
    }

    public String getTitle() {
        return event.title();
    }

    public String getDescription() {
        return event.description() == null || event.description().isBlank()
                ? "No description."
                : event.description();
    }

    public String getTypeValue() {
        return event.type().toDatabaseValue();
    }

    public String getTypeLabel() {
        return switch (event.type()) {
            case LESSON -> "Lesson";
            case ASSESSMENT -> "Assessment";
            case REMINDER -> "Reminder";
            case MEETING -> "Meeting";
            case OTHER -> "Event";
        };
    }

    public String getTypeIconClass() {
        return switch (event.type()) {
            case LESSON -> "ph ph-chalkboard-teacher";
            case ASSESSMENT -> "ph ph-seal-question";
            case REMINDER -> "ph ph-bell-ringing";
            case MEETING -> "ph ph-users-three";
            case OTHER -> "ph ph-calendar-dots";
        };
    }

    public String getTypeBadgeClass() {
        return switch (event.type()) {
            case LESSON -> "bg-main-50 text-main-600";
            case ASSESSMENT -> "bg-warning-50 text-warning-600";
            case REMINDER -> "bg-info-50 text-info-600";
            case MEETING -> "bg-main-two-50 text-main-two-600";
            case OTHER -> "bg-neutral-30 text-neutral-600";
        };
    }

    public String getStateValue() {
        return event.state().toDatabaseValue();
    }

    public String getStateLabel() {
        return switch (event.state()) {
            case DRAFT -> "Draft";
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case CANCELLED -> "Cancelled";
            case COMPLETED -> "Completed";
        };
    }

    public String getStateBadgeClass() {
        return switch (event.state()) {
            case DRAFT -> "bg-neutral-30 text-neutral-600";
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-warning-50 text-warning-600";
            case CANCELLED -> "bg-danger-50 text-danger-600";
            case COMPLETED -> "bg-info-50 text-info-600";
        };
    }

    public LocalDateTime getStartsAtRaw() {
        return event.startsAt();
    }

    public LocalDateTime getEndsAtRaw() {
        return event.endsAt();
    }

    public String getStartsAt() {
        return DISPLAY_DATE_TIME.format(event.startsAt());
    }

    public String getEndsAt() {
        return DISPLAY_DATE_TIME.format(event.endsAt());
    }

    public String getDateRangeLabel() {
        return getStartsAt() + " to " + getEndsAt();
    }

    public String getDurationLabel() {
        long minutes = Duration.between(event.startsAt(), event.endsAt()).toMinutes();
        if (event.allDay()) {
            return "All day";
        }
        if (minutes < 60) {
            return minutes + " min";
        }
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return remainingMinutes == 0 ? hours + " h" : hours + " h " + remainingMinutes + " min";
    }

    public boolean isAllDay() {
        return event.allDay();
    }

    public boolean isReminderEnabled() {
        return event.reminderEnabled();
    }

    public String getReminderLabel() {
        if (!event.reminderEnabled()) {
            return "No reminder";
        }
        Integer minutes = event.reminderMinutesBefore();
        if (minutes == null) {
            return "Reminder enabled";
        }
        if (minutes < 60) {
            return minutes + " min before";
        }
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        return remainingMinutes == 0
                ? hours + " h before"
                : hours + " h " + remainingMinutes + " min before";
    }

    public List<Long> getClassGroupIds() {
        return event.classGroupIds();
    }

    public List<String> getClassGroupLabels() {
        return classGroupLabels;
    }

    public String getClassGroupLabel() {
        return classGroupLabels.isEmpty() ? "No class group" : String.join(", ", classGroupLabels);
    }

    public boolean isLessonEvent() {
        return event.type() == ScheduleEventType.LESSON;
    }

    public boolean isAssessmentEvent() {
        return event.type() == ScheduleEventType.ASSESSMENT;
    }

    public boolean isActive() {
        return event.state() == ScheduleEventState.ACTIVE;
    }
}
