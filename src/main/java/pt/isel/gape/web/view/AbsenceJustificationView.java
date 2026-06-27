package pt.isel.gape.web.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import pt.isel.gape.learning.model.AbsenceJustification;
import pt.isel.gape.learning.model.AbsenceJustificationState;

public final class AbsenceJustificationView {

    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-PT"));

    private final AbsenceJustification justification;
    private final AttendanceRecordView attendanceRecord;

    private AbsenceJustificationView(
            AbsenceJustification justification,
            AttendanceRecordView attendanceRecord
    ) {
        this.justification = justification;
        this.attendanceRecord = attendanceRecord;
    }

    public static AbsenceJustificationView from(
            AbsenceJustification justification,
            AttendanceRecordView attendanceRecord
    ) {
        return new AbsenceJustificationView(justification, attendanceRecord);
    }

    public long getId() {
        return justification.id();
    }

    public long getAttendanceRecordId() {
        return justification.attendanceRecordId();
    }

    public long getStudentSubmitterUserId() {
        return justification.studentSubmitterUserId();
    }

    public Long getProcessorUserId() {
        return justification.processorUserId();
    }

    public AttendanceRecordView getAttendanceRecord() {
        return attendanceRecord;
    }

    public String getLessonTitle() {
        return attendanceRecord == null
                ? "Attendance record " + justification.attendanceRecordId()
                : attendanceRecord.getLessonTitle();
    }

    public String getStudentName() {
        return attendanceRecord == null ? "Unknown student" : attendanceRecord.getStudentName();
    }

    public String getStudentEmail() {
        return attendanceRecord == null ? "" : attendanceRecord.getStudentEmail();
    }

    public String getStatusLabel() {
        return attendanceRecord == null ? "-" : attendanceRecord.getStatusLabel();
    }

    public String getReason() {
        return justification.reason();
    }

    public String getAttachment() {
        return justification.attachment();
    }

    public String getAttachmentLabel() {
        return justification.attachment() == null || justification.attachment().isBlank()
                ? "-"
                : justification.attachment();
    }

    public String getDecisionNotes() {
        return justification.decisionNotes() == null || justification.decisionNotes().isBlank()
                ? "-"
                : justification.decisionNotes();
    }

    public String getSubmittedAt() {
        return format(justification.submittedAt());
    }

    public String getProcessedAt() {
        return format(justification.processedAt());
    }

    public String getStateValue() {
        return justification.state().toDatabaseValue();
    }

    public String getStateLabel() {
        return switch (justification.state()) {
            case SUBMITTED -> "Submitted";
            case UNDER_REVIEW -> "Under review";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
            case CANCELLED -> "Cancelled";
        };
    }

    public String getStateBadgeClass() {
        return switch (justification.state()) {
            case SUBMITTED -> "bg-warning-50 text-warning-600";
            case UNDER_REVIEW -> "bg-info-50 text-info-600";
            case APPROVED -> "bg-success-50 text-success-600";
            case REJECTED -> "bg-danger-50 text-danger-600";
            case CANCELLED -> "bg-neutral-30 text-neutral-600";
        };
    }

    public boolean isSubmitted() {
        return justification.state() == AbsenceJustificationState.SUBMITTED;
    }

    public boolean isFinalDecision() {
        return justification.state().isFinalDecision();
    }

    private static String format(LocalDateTime value) {
        return value == null ? "-" : DISPLAY_DATE_TIME.format(value);
    }
}
