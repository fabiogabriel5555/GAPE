package pt.isel.gape.web.view;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.learning.model.LessonState;
import pt.isel.gape.learning.model.LessonType;

public final class LessonView {

    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter COMPACT_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter INPUT_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final Lesson lesson;

    private LessonView(Lesson lesson) {
        this.lesson = lesson;
    }

    public static LessonView from(Lesson lesson) {
        return new LessonView(lesson);
    }

    public long getId() {
        return lesson.id();
    }

    public long getClassGroupId() {
        return lesson.classGroupId();
    }

    public long getContentBlockId() {
        return lesson.contentBlockId();
    }

    public String getPhysicalRoomCode() {
        return lesson.physicalRoomCode();
    }

    public String getTitle() {
        return lesson.title();
    }

    public String getDescription() {
        return lesson.description() == null || lesson.description().isBlank()
                ? "No description."
                : lesson.description();
    }

    public String getRawDescription() {
        return lesson.description();
    }

    public String getType() {
        return lesson.type().name();
    }

    public String getTypeValue() {
        return lesson.type().toDatabaseValue();
    }

    public String getTypeLabel() {
        return switch (lesson.type()) {
            case ONLINE -> "Online";
            case ONSITE -> "Presential";
            case HYBRID -> "Hybrid";
        };
    }

    public String getTypeBadgeClass() {
        return switch (lesson.type()) {
            case ONLINE -> "bg-main-50 text-main-600";
            case ONSITE -> "bg-main-two-50 text-main-two-600";
            case HYBRID -> "bg-info-50 text-info-600";
        };
    }

    public String getTypeIconClass() {
        return switch (lesson.type()) {
            case ONLINE -> "ph ph-video-camera";
            case ONSITE -> "ph ph-chalkboard-teacher";
            case HYBRID -> "ph ph-broadcast";
        };
    }

    public String getProvider() {
        return lesson.provider();
    }

    public String getProviderLabel() {
        return lesson.provider() == null || lesson.provider().isBlank() ? "-" : lesson.provider();
    }

    public String getAccessUrl() {
        return lesson.accessUrl();
    }

    public boolean isAttendanceRequired() {
        return lesson.attendanceRequired();
    }

    public String getAttendanceLabel() {
        return lesson.attendanceRequired() ? "Required" : "Optional";
    }

    public String getState() {
        return lesson.state().name();
    }

    public String getStateValue() {
        return lesson.state().toDatabaseValue();
    }

    public String getStateLabel() {
        return switch (lesson.state()) {
            case DRAFT -> "Draft";
            case SCHEDULED -> "Scheduled";
            case ACTIVE -> "Active";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };
    }

    public String getStateBadgeClass() {
        return switch (lesson.state()) {
            case DRAFT -> "bg-neutral-30 text-neutral-600";
            case SCHEDULED -> "bg-main-50 text-main-600";
            case ACTIVE -> "bg-success-50 text-success-600";
            case COMPLETED -> "bg-info-50 text-info-600";
            case CANCELLED -> "bg-danger-50 text-danger-600";
        };
    }

    public String getStartsAt() {
        return lesson.startsAt() == null ? "" : DISPLAY_DATE_TIME.format(lesson.startsAt());
    }

    public String getEndsAt() {
        return lesson.endsAt() == null ? "" : DISPLAY_DATE_TIME.format(lesson.endsAt());
    }

    public String getStartsAtInput() {
        return lesson.startsAt() == null ? "" : INPUT_DATE_TIME.format(lesson.startsAt());
    }

    public String getEndsAtInput() {
        return lesson.endsAt() == null ? "" : INPUT_DATE_TIME.format(lesson.endsAt());
    }

    public LocalDateTime getStartsAtRaw() {
        return lesson.startsAt();
    }

    public LocalDateTime getEndsAtRaw() {
        return lesson.endsAt();
    }

    public Integer getOrderNoRaw() {
        return lesson.orderNo();
    }

    public String getDateRangeLabel() {
        if (lesson.startsAt() == null && lesson.endsAt() == null) {
            return "Not scheduled";
        }
        return (lesson.startsAt() == null ? "-" : getStartsAt()) + " to " + (lesson.endsAt() == null ? "-" : getEndsAt());
    }

    public String getCompactDateRangeLabel() {
        if (lesson.startsAt() == null && lesson.endsAt() == null) {
            return "Not scheduled";
        }
        return (lesson.startsAt() == null ? "-" : COMPACT_DATE_TIME.format(lesson.startsAt()))
                + " to "
                + (lesson.endsAt() == null ? "-" : COMPACT_DATE_TIME.format(lesson.endsAt()));
    }

    public String getDurationLabel() {
        if (lesson.startsAt() == null || lesson.endsAt() == null) {
            return "-";
        }
        long minutes = Duration.between(lesson.startsAt(), lesson.endsAt()).toMinutes();
        if (minutes < 60) {
            return minutes + " min";
        }
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return remainingMinutes == 0 ? hours + " h" : hours + " h " + remainingMinutes + " min";
    }

    public boolean isOnline() {
        return lesson.type() == LessonType.ONLINE;
    }

    public boolean isOnsite() {
        return lesson.type() == LessonType.ONSITE;
    }

    public boolean isHybrid() {
        return lesson.type() == LessonType.HYBRID;
    }

    public boolean isHasMeetingLink() {
        return lesson.accessUrl() != null && !lesson.accessUrl().isBlank();
    }

    public boolean isHasRoom() {
        return lesson.physicalRoomCode() != null && !lesson.physicalRoomCode().isBlank();
    }

    public boolean isScheduled() {
        return lesson.state() == LessonState.SCHEDULED;
    }

    public boolean isActive() {
        return lesson.state() == LessonState.ACTIVE;
    }

    public boolean isCompleted() {
        return lesson.state() == LessonState.COMPLETED;
    }

    public boolean isCancelled() {
        return lesson.state() == LessonState.CANCELLED;
    }
}
