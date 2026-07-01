package pt.isel.gape.web.view;

import pt.isel.gape.learning.model.CertificateValidationResult;

public final class CertificateValidationView {

    private final CertificateValidationResult result;
    private final String courseLabel;

    private CertificateValidationView(CertificateValidationResult result, String courseLabel) {
        this.result = result;
        this.courseLabel = courseLabel == null || courseLabel.isBlank() ? "-" : courseLabel;
    }

    public static CertificateValidationView from(CertificateValidationResult result, String courseLabel) {
        return new CertificateValidationView(result, courseLabel);
    }

    public boolean isValid() {
        return result.valid();
    }

    public Long getCertificateId() {
        return result.certificateId();
    }

    public String getTitle() {
        return result.title() == null ? "-" : result.title();
    }

    public String getTypeLabel() {
        return result.type() == null ? "-" : CertificateView.typeLabel(result.type());
    }

    public String getCourseLabel() {
        return courseLabel;
    }

    public String getIssuedAt() {
        return GradeSheetView.format(result.issuedAt());
    }
}
