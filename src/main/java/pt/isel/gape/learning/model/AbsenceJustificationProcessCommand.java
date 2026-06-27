package pt.isel.gape.learning.model;

import java.time.LocalDateTime;

public record AbsenceJustificationProcessCommand(
        AbsenceJustificationState decision,
        LocalDateTime processedAt,
        String decisionNotes
) {
}
