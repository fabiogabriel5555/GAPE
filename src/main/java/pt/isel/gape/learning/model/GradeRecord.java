package pt.isel.gape.learning.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GradeRecord(
        long id,
        long gradeSheetId,
        long studentUserId,
        Long attemptId,
        String code,
        BigDecimal value,
        GradeRecordResult result,
        GradeRecordState state,
        LocalDateTime recordedAt,
        String notes
) {
}
