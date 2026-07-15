package pt.isel.gape.web.view;

import java.time.LocalDate;

import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.EnrollmentState;

public final class EnrollmentManagementView {

    private final long studentUserId;
    private final long courseId;
    private final long courseOccurrenceId;
    private final Long subjectId;
    private final EnrollmentState state;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String studentName;
    private final String studentEmail;
    private final String contextName;
    private final String courseOccurrenceCode;

    private EnrollmentManagementView(
            long studentUserId,
            long courseId,
            long courseOccurrenceId,
            Long subjectId,
            EnrollmentState state,
            LocalDate startDate,
            LocalDate endDate,
            String studentName,
            String studentEmail,
            String contextName,
            String courseOccurrenceCode
    ) {
        this.studentUserId = studentUserId;
        this.courseId = courseId;
        this.courseOccurrenceId = courseOccurrenceId;
        this.subjectId = subjectId;
        this.state = state;
        this.startDate = startDate;
        this.endDate = endDate;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.contextName = contextName;
        this.courseOccurrenceCode = courseOccurrenceCode;
    }

    public static EnrollmentManagementView course(
            CourseEnrollment enrollment,
            String studentName,
            String studentEmail,
            String courseOccurrenceCode
    ) {
        return new EnrollmentManagementView(
                enrollment.studentUserId(),
                enrollment.courseId(),
                enrollment.courseOccurrenceId(),
                null,
                enrollment.state(),
                enrollment.startDate(),
                enrollment.endDate(),
                studentName,
                studentEmail,
                null,
                courseOccurrenceCode
        );
    }

    public long getStudentUserId() {
        return studentUserId;
    }

    public long getCourseId() {
        return courseId;
    }

    public long getCourseOccurrenceId() {
        return courseOccurrenceId;
    }

    public String getCourseOccurrenceCode() {
        return courseOccurrenceCode == null || courseOccurrenceCode.isBlank()
                ? "Occurrence " + courseOccurrenceId
                : courseOccurrenceCode;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public String getStateLabel() {
        return switch (state) {
            case PENDING -> "Pending approval";
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case REJECTED -> "Rejected";
            case COMPLETED -> "Completed";
            case WITHDRAWN -> "Left";
        };
    }

    public String getStateValue() {
        return state.toDatabaseValue();
    }

    public String getStateBadgeClass() {
        return switch (state) {
            case PENDING -> "bg-warning-30 text-warning-600";
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-danger-50 text-danger-600";
            case REJECTED -> "bg-danger-50 text-danger-600";
            case COMPLETED -> "bg-neutral-20 text-neutral-600";
            case WITHDRAWN -> "bg-warning-30 text-warning-600";
        };
    }

    public boolean isActive() {
        return state == EnrollmentState.ACTIVE;
    }

    public boolean isCompleted() {
        return state == EnrollmentState.COMPLETED;
    }

    public boolean isPending() {
        return state == EnrollmentState.PENDING;
    }

    public String getStartDate() {
        return startDate == null ? "-" : ApplicationDateTimeFormat.date(startDate);
    }

    public String getStartDateValue() {
        return startDate == null ? "" : startDate.toString();
    }

    public String getEndDate() {
        return endDate == null ? "-" : ApplicationDateTimeFormat.date(endDate);
    }

    public String getEndDateValue() {
        return endDate == null ? "" : endDate.toString();
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentShortLabel() {
        String normalized = studentName == null ? "" : studentName.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return studentUserId + " - Student";
        }
        String[] parts = normalized.split(" ");
        if (parts.length == 1) {
            return studentUserId + " - " + parts[0];
        }
        return studentUserId + " - " + parts[0] + " " + parts[parts.length - 1];
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public String getContextName() {
        return contextName == null || contextName.isBlank() ? "-" : contextName;
    }
}
