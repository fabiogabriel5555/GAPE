package pt.isel.gape.web.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import pt.isel.gape.learning.model.AttendanceRecord;
import pt.isel.gape.learning.model.AttendanceSource;
import pt.isel.gape.learning.model.AttendanceState;
import pt.isel.gape.learning.model.AttendanceStatus;

public final class AttendanceRecordView {

    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter INPUT_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final AttendanceRecord record;
    private final LessonView lesson;
    private final String studentName;
    private final String studentEmail;
    private final boolean hasJustification;

    private AttendanceRecordView(
            AttendanceRecord record,
            LessonView lesson,
            String studentName,
            String studentEmail,
            boolean hasJustification
    ) {
        this.record = record;
        this.lesson = lesson;
        this.studentName = studentName == null || studentName.isBlank() ? "Unknown student" : studentName;
        this.studentEmail = studentEmail == null ? "" : studentEmail;
        this.hasJustification = hasJustification;
    }

    public static AttendanceRecordView from(
            AttendanceRecord record,
            LessonView lesson,
            String studentName,
            String studentEmail,
            boolean hasJustification
    ) {
        return new AttendanceRecordView(record, lesson, studentName, studentEmail, hasJustification);
    }

    public long getId() {
        return record.id();
    }

    public long getLessonId() {
        return record.lessonId();
    }

    public long getStudentUserId() {
        return record.studentUserId();
    }

    public LessonView getLesson() {
        return lesson;
    }

    public String getLessonTitle() {
        return lesson == null ? "Lesson " + record.lessonId() : lesson.getTitle();
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public String getStatusValue() {
        return record.status().toDatabaseValue();
    }

    public String getStatusLabel() {
        return switch (record.status()) {
            case PRESENT -> "Present";
            case ABSENT -> "Absent";
            case JUSTIFIED -> "Justified";
            case LATE -> "Late";
            case PARTIAL -> "Partial";
        };
    }

    public String getStatusBadgeClass() {
        return switch (record.status()) {
            case PRESENT -> "bg-success-50 text-success-600";
            case ABSENT -> "bg-danger-50 text-danger-600";
            case JUSTIFIED -> "bg-main-50 text-main-600";
            case LATE -> "bg-warning-50 text-warning-600";
            case PARTIAL -> "bg-info-50 text-info-600";
        };
    }

    public String getSourceValue() {
        return record.source().toDatabaseValue();
    }

    public String getSourceLabel() {
        return switch (record.source()) {
            case MANUAL -> "Manual";
            case AUTOMATIC -> "Automatic";
            case OTHER -> "Other";
        };
    }

    public String getStateValue() {
        return record.state().toDatabaseValue();
    }

    public String getStateLabel() {
        return switch (record.state()) {
            case ACTIVE -> "Active";
            case CORRECTED -> "Corrected";
            case CANCELLED -> "Cancelled";
        };
    }

    public String getStateBadgeClass() {
        return switch (record.state()) {
            case ACTIVE -> "bg-success-50 text-success-600";
            case CORRECTED -> "bg-info-50 text-info-600";
            case CANCELLED -> "bg-danger-50 text-danger-600";
        };
    }

    public String getCheckIn() {
        return format(record.checkIn());
    }

    public String getCheckOut() {
        return format(record.checkOut());
    }

    public String getCheckInValue() {
        return formatInput(record.checkIn());
    }

    public String getCheckOutValue() {
        return formatInput(record.checkOut());
    }

    public String getNotes() {
        return record.notes() == null || record.notes().isBlank() ? "-" : record.notes();
    }

    public String getPermanenceLabel() {
        long minutes = record.permanenceMinutes();
        if (minutes <= 0) {
            return "-";
        }
        if (minutes < 60) {
            return minutes + " min";
        }
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return remainingMinutes == 0 ? hours + " h" : hours + " h " + remainingMinutes + " min";
    }

    public boolean isAllowsJustification() {
        return record.status().allowsJustification() && record.state() != AttendanceState.CANCELLED;
    }

    public boolean isHasJustification() {
        return hasJustification;
    }

    public boolean isCanSubmitJustification() {
        return isAllowsJustification() && !hasJustification;
    }

    public boolean isAbsent() {
        return record.status() == AttendanceStatus.ABSENT;
    }

    public boolean isManual() {
        return record.source() == AttendanceSource.MANUAL;
    }

    private static String format(LocalDateTime value) {
        return value == null ? "-" : DISPLAY_DATE_TIME.format(value);
    }

    private static String formatInput(LocalDateTime value) {
        return value == null ? "" : INPUT_DATE_TIME.format(value);
    }
}
