package pt.isel.gape.learning.model;

import java.time.LocalDateTime;

public record Lesson(
        long id,
        long classGroupId,
        long contentBlockId,
        String physicalRoomCode,
        String title,
        String description,
        LessonType type,
        String provider,
        String accessUrl,
        boolean attendanceRequired,
        LessonState state,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {
}
