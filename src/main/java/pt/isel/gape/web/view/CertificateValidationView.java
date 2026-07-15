package pt.isel.gape.web.view;

import pt.isel.gape.learning.model.CertificateValidationResult;

public final class CertificateValidationView {

    private final CertificateValidationResult result;
    private final String courseLabel;
    private final String occurrenceLabel;

    private CertificateValidationView(
            CertificateValidationResult result,
            String courseLabel,
            String occurrenceLabel
    ) {
        this.result = result;
        this.courseLabel = fallback(courseLabel);
        this.occurrenceLabel = fallback(occurrenceLabel);
    }

    public static CertificateValidationView from(
            CertificateValidationResult result,
            String courseLabel,
            String occurrenceLabel
    ) {
        return new CertificateValidationView(result, courseLabel, occurrenceLabel);
    }

    public boolean isValid() {
        return result.valid();
    }

    public Long getCertificateId() {
        return result.certificateId();
    }

    public String getTitle() {
        return fallback(result.title());
    }

    public String getTypeLabel() {
        return result.type() == null ? "-" : CertificateView.typeLabel(result.type());
    }

    public String getCourseLabel() {
        return courseLabel;
    }

    public String getOccurrenceLabel() {
        return occurrenceLabel;
    }

    public String getIssuedAt() {
        return GradeSheetView.format(result.issuedAt());
    }

    private static String fallback(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
