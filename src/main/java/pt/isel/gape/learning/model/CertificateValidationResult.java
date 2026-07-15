package pt.isel.gape.learning.model;

import java.time.LocalDateTime;

public record CertificateValidationResult(
        boolean valid,
        Long certificateId,
        String title,
        CertificateType type,
        Long courseId,
        Long courseOccurrenceId,
        LocalDateTime issuedAt
) {

    public CertificateValidationResult(
            boolean valid,
            Long certificateId,
            String title,
            CertificateType type,
            Long courseId,
            LocalDateTime issuedAt
    ) {
        this(valid, certificateId, title, type, courseId, null, issuedAt);
    }

    public static CertificateValidationResult invalid() {
        return new CertificateValidationResult(false, null, null, null, null, null, null);
    }
}
