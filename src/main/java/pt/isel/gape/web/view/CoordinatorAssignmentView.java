package pt.isel.gape.web.view;

import java.time.LocalDate;

import pt.isel.gape.structure.model.CoordinateSubjectAssignment;
import pt.isel.gape.structure.model.RoleAssignmentState;

public final class CoordinatorAssignmentView {

    private final long coordinatorUserId;
    private final long subjectId;
    private final RoleAssignmentState state;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String coordinatorName;
    private final String coordinatorEmail;

    private CoordinatorAssignmentView(CoordinateSubjectAssignment assignment) {
        this.coordinatorUserId = assignment.coordinatorUserId();
        this.subjectId = assignment.subjectId();
        this.state = assignment.state();
        this.startDate = assignment.startDate();
        this.endDate = assignment.endDate();
        this.coordinatorName = assignment.coordinatorName();
        this.coordinatorEmail = assignment.coordinatorEmail();
    }

    public static CoordinatorAssignmentView from(CoordinateSubjectAssignment assignment) {
        return new CoordinatorAssignmentView(assignment);
    }

    public long getCoordinatorUserId() {
        return coordinatorUserId;
    }

    public long getSubjectId() {
        return subjectId;
    }

    public String getCoordinatorName() {
        return coordinatorName == null || coordinatorName.isBlank() ? "Unknown coordinator" : coordinatorName;
    }

    public String getCoordinatorEmail() {
        return coordinatorEmail == null ? "" : coordinatorEmail;
    }

    public String getStateValue() {
        return state.toDatabaseValue();
    }

    public String getStateLabel() {
        return switch (state) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case ARCHIVED -> "Archived";
        };
    }

    public String getStateBadgeClass() {
        return switch (state) {
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-warning-30 text-warning-600";
            case ARCHIVED -> "bg-danger-50 text-danger-600";
        };
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
}
