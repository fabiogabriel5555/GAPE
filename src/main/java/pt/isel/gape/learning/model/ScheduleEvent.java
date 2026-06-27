package pt.isel.gape.learning.model;

import java.time.LocalDateTime;
import java.util.List;

public record ScheduleEvent(
        long id,
        Long lessonId,
        Long assessmentId,
        String title,
        String description,
        ScheduleEventType type,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean allDay,
        boolean reminderEnabled,
        Integer reminderMinutesBefore,
        ScheduleEventState state,
        List<Long> classGroupIds
) {
    public ScheduleEvent {
        classGroupIds = classGroupIds == null ? List.of() : List.copyOf(classGroupIds);
    }
}
