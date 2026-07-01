package pt.isel.gape.web.view;

import pt.isel.gape.learning.model.AssessmentEnrollment;
import pt.isel.gape.learning.model.EnrollmentState;

public final class AssessmentEnrollmentView {

    private final long studentUserId;
    private final long assessmentId;
    private final EnrollmentState state;
    private final String studentName;
    private final String studentEmail;

    private AssessmentEnrollmentView(
            AssessmentEnrollment enrollment,
            String studentName,
            String studentEmail
    ) {
        this.studentUserId = enrollment.studentUserId();
        this.assessmentId = enrollment.assessmentId();
        this.state = enrollment.state();
        this.studentName = studentName;
        this.studentEmail = studentEmail;
    }

    public static AssessmentEnrollmentView from(
            AssessmentEnrollment enrollment,
            String studentName,
            String studentEmail
    ) {
        return new AssessmentEnrollmentView(enrollment, studentName, studentEmail);
    }

    public long getStudentUserId() {
        return studentUserId;
    }

    public long getAssessmentId() {
        return assessmentId;
    }

    public String getState() {
        return state.name();
    }

    public String getStateValue() {
        return state.toDatabaseValue();
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

    public boolean isReactivateAvailable() {
        return state != EnrollmentState.ACTIVE && state != EnrollmentState.PENDING;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }
}
