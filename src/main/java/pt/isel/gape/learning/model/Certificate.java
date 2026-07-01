package pt.isel.gape.learning.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Certificate(
        long id,
        long courseId,
        long studentUserId,
        String title,
        String notes,
        CertificateType type,
        String template,
        String validationCode,
        LocalDateTime issuedAt,
        CertificateState state,
        LocalDateTime revokedAt,
        BigDecimal finalGrade,
        List<Long> gradeSheetIds
) {
}
