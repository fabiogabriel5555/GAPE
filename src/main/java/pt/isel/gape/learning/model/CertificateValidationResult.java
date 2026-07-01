package pt.isel.gape.learning.model;

import java.time.LocalDateTime;

public record CertificateValidationResult(
        boolean valid,
        Long certificateId,
        String title,
        CertificateType type,
        Long courseId,
        LocalDateTime issuedAt
) {

    public static CertificateValidationResult invalid() {
        return new CertificateValidationResult(false, null, null, null, null, null);
    }
}
