package pt.isel.gape.web.view;

import java.time.LocalDate;

import pt.isel.gape.learning.model.ClassGroupEnrollment;
import pt.isel.gape.learning.model.EnrollmentState;

public final class ClassGroupEnrollmentView {

    private final long studentUserId;
    private final long classGroupId;
    private final EnrollmentState state;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String studentName;
    private final String studentEmail;

    private ClassGroupEnrollmentView(
            ClassGroupEnrollment enrollment,
            String studentName,
            String studentEmail
    ) {
        this.studentUserId = enrollment.studentUserId();
        this.classGroupId = enrollment.classGroupId();
        this.state = enrollment.state();
        this.startDate = enrollment.startDate();
        this.endDate = enrollment.endDate();
        this.studentName = studentName;
        this.studentEmail = studentEmail;
    }

    public static ClassGroupEnrollmentView from(
            ClassGroupEnrollment enrollment,
            String studentName,
            String studentEmail
    ) {
        return new ClassGroupEnrollmentView(enrollment, studentName, studentEmail);
    }

    public long getStudentUserId() {
        return studentUserId;
    }

    public long getClassGroupId() {
        return classGroupId;
    }

    public String getState() {
        return state.name();
    }

    public String getStateLabel() {
        return switch (state) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case COMPLETED -> "Completed";
            case WITHDRAWN -> "Withdrawn";
            case ARCHIVED -> "Archived";
        };
    }

    public String getStateBadgeClass() {
        return switch (state) {
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-warning-30 text-warning-600";
            case COMPLETED -> "bg-info-50 text-info-600";
            case WITHDRAWN -> "bg-warning-30 text-warning-600";
            case ARCHIVED -> "bg-danger-50 text-danger-600";
        };
    }

    public boolean isActive() {
        return state == EnrollmentState.ACTIVE;
    }

    public String getStartDate() {
        return startDate == null ? "-" : startDate.toString();
    }

    public String getEndDate() {
        return endDate == null ? "-" : endDate.toString();
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }
}
