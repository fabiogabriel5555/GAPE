package pt.isel.gape.web.view;

import java.time.LocalDate;

import pt.isel.gape.access.model.UserState;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.structure.model.OrganizationAdministratorAssignment;
import pt.isel.gape.structure.model.RoleAssignmentState;

public final class OrganizationAdministratorView {

    private final long userId;
    private final String name;
    private final String email;
    private final UserState userState;
    private final RoleAssignmentState assignmentState;
    private final LocalDate startDate;
    private final LocalDate endDate;

    private OrganizationAdministratorView(OrganizationAdministratorAssignment assignment) {
        this.userId = assignment.userId();
        this.name = assignment.name();
        this.email = assignment.email();
        this.userState = assignment.userState();
        this.assignmentState = assignment.assignmentState();
        this.startDate = assignment.startDate();
        this.endDate = assignment.endDate();
    }

    public static OrganizationAdministratorView from(OrganizationAdministratorAssignment assignment) {
        return new OrganizationAdministratorView(assignment);
    }

    public long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getUserStateLabel() {
        return switch (userState) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case BLOCKED -> "Blocked";
        };
    }

    public String getAssignmentStateLabel() {
        return switch (assignmentState) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
        };
    }

    public String getAssignmentBadgeClass() {
        if (userState != UserState.ACTIVE || assignmentState != RoleAssignmentState.ACTIVE) {
            return "bg-danger-50 text-danger-600";
        }
        return "bg-success-50 text-success-600";
    }

    public String getStartDateLabel() {
        return startDate == null ? "-" : ApplicationDateTimeFormat.date(startDate);
    }

    public String getEndDateLabel() {
        return endDate == null ? "-" : ApplicationDateTimeFormat.date(endDate);
    }

    public boolean isCurrentlyActive() {
        LocalDate today = LocalDate.now(ApplicationClock.system());
        return userState == UserState.ACTIVE
                && assignmentState == RoleAssignmentState.ACTIVE
                && (startDate == null || !startDate.isAfter(today))
                && (endDate == null || !endDate.isBefore(today));
    }
}
