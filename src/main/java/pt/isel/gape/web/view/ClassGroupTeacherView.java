package pt.isel.gape.web.view;

import java.time.LocalDate;

import pt.isel.gape.structure.model.RoleAssignmentState;
import pt.isel.gape.structure.model.TeacherClassGroupAssignment;

public final class ClassGroupTeacherView {

    private final long teacherUserId;
    private final long classGroupId;
    private final RoleAssignmentState state;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String teacherName;
    private final String teacherEmail;

    private ClassGroupTeacherView(TeacherClassGroupAssignment assignment) {
        this.teacherUserId = assignment.teacherUserId();
        this.classGroupId = assignment.classGroupId();
        this.state = assignment.state();
        this.startDate = assignment.startDate();
        this.endDate = assignment.endDate();
        this.teacherName = assignment.teacherName();
        this.teacherEmail = assignment.teacherEmail();
    }

    public static ClassGroupTeacherView from(TeacherClassGroupAssignment assignment) {
        return new ClassGroupTeacherView(assignment);
    }

    public long getTeacherUserId() {
        return teacherUserId;
    }

    public long getClassGroupId() {
        return classGroupId;
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

    public boolean isActive() {
        return state == RoleAssignmentState.ACTIVE;
    }

    public String getStartDate() {
        return startDate == null ? "-" : startDate.toString();
    }

    public String getEndDate() {
        return endDate == null ? "-" : endDate.toString();
    }

    public String getTeacherName() {
        return teacherName == null || teacherName.isBlank() ? "Unknown teacher" : teacherName;
    }

    public String getTeacherEmail() {
        return teacherEmail == null ? "" : teacherEmail;
    }
}
