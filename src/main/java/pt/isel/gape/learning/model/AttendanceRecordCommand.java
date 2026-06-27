package pt.isel.gape.learning.model;

import java.time.LocalDateTime;

public record AttendanceRecordCommand(
        long lessonId,
        long studentUserId,
        AttendanceStatus status,
        AttendanceSource source,
        LocalDateTime checkIn,
        LocalDateTime checkOut,
        String notes,
        AttendanceState state
) {
}
