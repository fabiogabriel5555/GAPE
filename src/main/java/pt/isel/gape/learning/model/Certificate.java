package pt.isel.gape.learning.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Certificate(
        long id,
        long courseId,
        long courseOccurrenceId,
        long studentUserId,
        String title,
        String notes,
        CertificateType type,
        String template,
        String validationCode,
        LocalDateTime issuedAt,
        CertificateState state,
        BigDecimal finalGrade,
        List<Long> gradeSheetIds
) {
    public Certificate(
            long id,
            long courseId,
            long courseOccurrenceId,
            long studentUserId,
            String title,
            String notes,
            CertificateType type,
            String template,
            LocalDateTime issuedAt,
            CertificateState state,
            BigDecimal finalGrade,
            List<Long> gradeSheetIds
    ) {
        this(id, courseId, courseOccurrenceId, studentUserId, title, notes, type, template,
                null, issuedAt, state, finalGrade, gradeSheetIds);
    }

    public Certificate(
            long id,
            long courseId,
            long studentUserId,
            String title,
            String notes,
            CertificateType type,
            String template,
            LocalDateTime issuedAt,
            CertificateState state,
            BigDecimal finalGrade,
            List<Long> gradeSheetIds
    ) {
        this(id, courseId, 0L, studentUserId, title, notes, type, template,
                null, issuedAt, state, finalGrade, gradeSheetIds);
    }

    public Certificate(
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
            BigDecimal finalGrade,
            List<Long> gradeSheetIds
    ) {
        this(id, courseId, 0L, studentUserId, title, notes, type, template,
                validationCode, issuedAt, state, finalGrade, gradeSheetIds);
    }
}
