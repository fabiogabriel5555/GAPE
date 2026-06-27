package pt.isel.gape.web.view;

import java.time.LocalDate;

import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.EnrollmentState;
import pt.isel.gape.learning.model.SubjectEnrollment;

public final class EnrollmentManagementView {

    private final long studentUserId;
    private final long courseId;
    private final Long subjectId;
    private final EnrollmentState state;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String studentName;
    private final String studentEmail;
    private final String contextName;

    private EnrollmentManagementView(
            long studentUserId,
            long courseId,
            Long subjectId,
            EnrollmentState state,
            LocalDate startDate,
            LocalDate endDate,
            String studentName,
            String studentEmail,
            String contextName
    ) {
        this.studentUserId = studentUserId;
        this.courseId = courseId;
        this.subjectId = subjectId;
        this.state = state;
        this.startDate = startDate;
        this.endDate = endDate;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.contextName = contextName;
    }

    public static EnrollmentManagementView course(
            CourseEnrollment enrollment,
            String studentName,
            String studentEmail
    ) {
        return new EnrollmentManagementView(
                enrollment.studentUserId(),
                enrollment.courseId(),
                null,
                enrollment.state(),
                enrollment.startDate(),
                enrollment.endDate(),
                studentName,
                studentEmail,
                null
        );
    }

    public static EnrollmentManagementView subject(
            SubjectEnrollment enrollment,
            String studentName,
            String studentEmail,
            String contextName
    ) {
        return new EnrollmentManagementView(
                enrollment.studentUserId(),
                enrollment.courseId(),
                enrollment.subjectId(),
                enrollment.state(),
                enrollment.startDate(),
                enrollment.endDate(),
                studentName,
                studentEmail,
                contextName
        );
    }

    public long getStudentUserId() {
        return studentUserId;
    }

    public long getCourseId() {
        return courseId;
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
            case INACTIVE -> "bg-warning-30 text-warning-600";
            case REJECTED -> "bg-danger-50 text-danger-600";
            case COMPLETED -> "bg-info-50 text-info-600";
            case WITHDRAWN -> "bg-warning-30 text-warning-600";
        };
    }

    public boolean isActive() {
        return state == EnrollmentState.ACTIVE;
    }

    public boolean isPending() {
        return state == EnrollmentState.PENDING;
    }

    public String getStartDate() {
        return startDate == null ? "-" : startDate.toString();
    }

    public String getStartDateValue() {
        return startDate == null ? "" : startDate.toString();
    }

    public String getEndDate() {
        return endDate == null ? "-" : endDate.toString();
    }

    public String getEndDateValue() {
        return endDate == null ? "" : endDate.toString();
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public String getContextName() {
        return contextName == null || contextName.isBlank() ? "-" : contextName;
    }
}
