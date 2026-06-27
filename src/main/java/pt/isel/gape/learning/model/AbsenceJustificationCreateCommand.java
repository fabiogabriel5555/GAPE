package pt.isel.gape.learning.model;

import java.time.LocalDateTime;

public record AbsenceJustificationCreateCommand(
        long attendanceRecordId,
        String reason,
        String attachment,
        LocalDateTime submittedAt
) {
}
