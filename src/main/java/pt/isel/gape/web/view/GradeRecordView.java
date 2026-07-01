package pt.isel.gape.web.view;

import pt.isel.gape.learning.model.GradeRecord;

public final class GradeRecordView {

    private final GradeRecord record;
    private final String gradeSheetTitle;
    private final String subjectLabel;
    private final String studentName;
    private final String studentEmail;

    private GradeRecordView(
            GradeRecord record,
            String gradeSheetTitle,
            String subjectLabel,
            String studentName,
            String studentEmail
    ) {
        this.record = record;
        this.gradeSheetTitle = gradeSheetTitle == null || gradeSheetTitle.isBlank()
                ? "Grade sheet " + record.gradeSheetId()
                : gradeSheetTitle;
        this.subjectLabel = subjectLabel == null || subjectLabel.isBlank() ? "-" : subjectLabel;
        this.studentName = studentName == null || studentName.isBlank() ? "Student " + record.studentUserId() : studentName;
        this.studentEmail = studentEmail == null ? "" : studentEmail;
    }

    public static GradeRecordView from(
            GradeRecord record,
            String gradeSheetTitle,
            String subjectLabel,
            String studentName,
            String studentEmail
    ) {
        return new GradeRecordView(record, gradeSheetTitle, subjectLabel, studentName, studentEmail);
    }

    public long getId() {
        return record.id();
    }

    public long getGradeSheetId() {
        return record.gradeSheetId();
    }

    public long getStudentUserId() {
        return record.studentUserId();
    }

    public String getCode() {
        return record.code();
    }

    public String getValue() {
        return GradeSheetView.gradeLabel(record.value());
    }

    public String getResultValue() {
        return record.result().toDatabaseValue();
    }

    public String getResultLabel() {
        return switch (record.result()) {
            case APPROVED -> "Approved";
            case FAILED -> "Failed";
            case PENDING -> "Pending";
            case ABSENT -> "Absent";
        };
    }

    public String getResultBadgeClass() {
        return switch (record.result()) {
            case APPROVED -> "bg-success-50 text-success-600";
            case FAILED -> "bg-danger-50 text-danger-600";
            case PENDING -> "bg-warning-50 text-warning-600";
            case ABSENT -> "bg-neutral-20 text-neutral-600";
        };
    }

    public String getRecordedAt() {
        return GradeSheetView.format(record.recordedAt());
    }

    public String getNotes() {
        return record.notes() == null || record.notes().isBlank() ? "-" : record.notes();
    }

    public String getGradeSheetTitle() {
        return gradeSheetTitle;
    }

    public String getSubjectLabel() {
        return subjectLabel;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }
}
