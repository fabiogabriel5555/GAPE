package pt.isel.gape.learning.model;

import java.time.LocalDateTime;

public record AbsenceJustification(
        long id,
        long attendanceRecordId,
        long studentSubmitterUserId,
        Long processorUserId,
        LocalDateTime submittedAt,
        String reason,
        String attachment,
        LocalDateTime processedAt,
        String decisionNotes,
        AbsenceJustificationState state
) {
}
