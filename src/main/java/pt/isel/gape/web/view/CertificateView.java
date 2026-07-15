package pt.isel.gape.web.view;

import java.math.BigDecimal;
import java.util.List;

import pt.isel.gape.learning.model.Certificate;
import pt.isel.gape.learning.model.CertificateState;
import pt.isel.gape.learning.model.CertificateType;

public final class CertificateView {

    private final Certificate certificate;
    private final String courseLabel;
    private final String courseName;
    private final String courseAcronym;
    private final String courseEctsLabel;
    private final String courseCertificateMaxGradeLabel;
    private final String courseDurationLabel;
    private final String studentName;
    private final String studentEmail;
    private final String gradeSheetLabel;
    private final List<CertificateSubjectRowView> subjectRows;

    private CertificateView(
            Certificate certificate,
            String courseLabel,
            String courseName,
            String courseAcronym,
            String courseEctsLabel,
            String courseCertificateMaxGradeLabel,
            String courseDurationLabel,
            String studentName,
            String studentEmail,
            String gradeSheetLabel,
            List<CertificateSubjectRowView> subjectRows
    ) {
        this.certificate = certificate;
        this.courseLabel = courseLabel == null || courseLabel.isBlank() ? "Course " + certificate.courseId() : courseLabel;
        this.courseName = courseName == null || courseName.isBlank() ? this.courseLabel : courseName;
        this.courseAcronym = courseAcronym == null || courseAcronym.isBlank() ? this.courseName : courseAcronym;
        this.courseEctsLabel = courseEctsLabel == null || courseEctsLabel.isBlank() ? "-" : courseEctsLabel;
        this.courseCertificateMaxGradeLabel = courseCertificateMaxGradeLabel == null || courseCertificateMaxGradeLabel.isBlank()
                ? "20"
                : courseCertificateMaxGradeLabel;
        this.courseDurationLabel = durationHoursLabel(courseDurationLabel);
        this.studentName = studentName == null || studentName.isBlank()
                ? "Student " + certificate.studentUserId()
                : studentName;
        this.studentEmail = studentEmail == null ? "" : studentEmail;
        this.gradeSheetLabel = gradeSheetLabel == null || gradeSheetLabel.isBlank() ? "-" : gradeSheetLabel;
        this.subjectRows = subjectRows == null ? List.of() : List.copyOf(subjectRows);
    }

    public static CertificateView from(
            Certificate certificate,
            String courseLabel,
            String studentName,
            String studentEmail,
            String gradeSheetLabel
    ) {
        return new CertificateView(
                certificate,
                courseLabel,
                courseLabel,
                courseLabel,
                null,
                null,
                null,
                studentName,
                studentEmail,
                gradeSheetLabel,
                List.of()
        );
    }

    public static CertificateView from(
            Certificate certificate,
            String courseLabel,
            String courseName,
            String courseAcronym,
            BigDecimal courseEcts,
            BigDecimal courseCertificateMaxGrade,
            String courseDuration,
            String studentName,
            String studentEmail,
            String gradeSheetLabel,
            List<CertificateSubjectRowView> subjectRows
    ) {
        return new CertificateView(
                certificate,
                courseLabel,
                courseName,
                courseAcronym,
                GradeSheetView.gradeLabel(courseEcts),
                GradeSheetView.gradeLabel(courseCertificateMaxGrade),
                courseDuration,
                studentName,
                studentEmail,
                gradeSheetLabel,
                subjectRows
        );
    }

    public long getId() {
        return certificate.id();
    }

    public long getCourseId() {
        return certificate.courseId();
    }

    public long getStudentUserId() {
        return certificate.studentUserId();
    }

    public String getTitle() {
        return certificate.title();
    }

    public String getNotes() {
        return certificate.notes() == null || certificate.notes().isBlank() ? "-" : certificate.notes();
    }

    public String getTypeValue() {
        return certificate.type().toDatabaseValue();
    }

    public String getTypeLabel() {
        return typeLabel(certificate.type());
    }

    public String getTemplate() {
        return certificate.template() == null || certificate.template().isBlank() ? "-" : certificate.template();
    }

    public String getValidationCode() {
        return certificate.validationCode() == null ? "" : certificate.validationCode();
    }

    public String getIssuedAt() {
        return GradeSheetView.format(certificate.issuedAt());
    }

    public String getIssuedAtSort() {
        return certificate.issuedAt() == null ? "" : certificate.issuedAt().toString();
    }

    public String getStateValue() {
        return certificate.state().toDatabaseValue();
    }

    public String getFinalGrade() {
        return GradeSheetView.gradeLabel(certificate.finalGrade());
    }

    public String getCourseLabel() {
        return courseLabel;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getCourseAcronym() {
        return courseAcronym;
    }

    public String getCourseEctsLabel() {
        return courseEctsLabel;
    }

    public String getCourseCertificateMaxGradeLabel() {
        return courseCertificateMaxGradeLabel;
    }

    public String getCourseDurationLabel() {
        return courseDurationLabel;
    }

    private static String durationHoursLabel(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String normalized = value.trim();
        String lower = normalized.toLowerCase();
        if (lower.contains("hora")
                || lower.contains("hour")
                || lower.matches(".*\\d\\s*h$")) {
            return normalized;
        }
        return normalized + " hours";
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public String getGradeSheetLabel() {
        return gradeSheetLabel;
    }

    public List<CertificateSubjectRowView> getSubjectRows() {
        return subjectRows;
    }

    public int getSubjectCount() {
        return subjectRows.size();
    }

    public String getSubjectCountLabel() {
        int count = subjectRows.size();
        if (count == 1) {
            return "1 subject";
        }
        return count + " subjects";
    }

    public boolean isCompleted() {
        return certificate.state() == CertificateState.ISSUED
                && certificate.validationCode() != null
                && !certificate.validationCode().isBlank()
                && certificate.issuedAt() != null
                && certificate.finalGrade() != null;
    }

    public String getStatusLabel() {
        return switch (certificate.state()) {
            case ISSUED -> "Published";
            case ACTIVE -> "Active";
            case DRAFT -> "Draft";
        };
    }

    public String getStatusBadgeClass() {
        return switch (certificate.state()) {
            case ISSUED -> "bg-success-50 text-success-600";
            case ACTIVE -> "bg-info-50 text-info-600";
            case DRAFT -> "bg-warning-50 text-warning-600";
        };
    }

    public static String typeLabel(CertificateType type) {
        return switch (type) {
            case COMPLETION -> "Completion";
            case ATTENDANCE -> "Attendance";
            case QUALIFICATION -> "Qualification";
            case OTHER -> "Other";
        };
    }

    public static final class CertificateSubjectRowView {

        private final String subjectLabel;
        private final String ectsLabel;
        private final String gradeLabel;

        public CertificateSubjectRowView(String subjectLabel, BigDecimal ects, BigDecimal grade) {
            this.subjectLabel = subjectLabel == null || subjectLabel.isBlank() ? "Subject" : subjectLabel;
            this.ectsLabel = GradeSheetView.gradeLabel(ects);
            this.gradeLabel = GradeSheetView.gradeLabel(grade);
        }

        public String getSubjectLabel() {
            return subjectLabel;
        }

        public String getEctsLabel() {
            return ectsLabel;
        }

        public String getGradeLabel() {
            return gradeLabel;
        }
    }
}
