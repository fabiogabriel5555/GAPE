package pt.isel.gape.web.view;

import java.time.LocalDate;

import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupModality;
import pt.isel.gape.learning.model.ClassGroupShift;
import pt.isel.gape.learning.model.ClassGroupState;

public final class OrganizationClassGroupTreeView {

    private final long id;
    private final long courseId;
    private final long subjectId;
    private final String code;
    private final ClassGroupModality modality;
    private final ClassGroupState state;
    private final LocalDate startsAt;
    private final LocalDate endsAt;
    private final ClassGroupShift shift;

    private OrganizationClassGroupTreeView(ClassGroup classGroup) {
        this.id = classGroup.id();
        this.courseId = classGroup.courseId();
        this.subjectId = classGroup.subjectId();
        this.code = classGroup.code();
        this.modality = classGroup.modality();
        this.state = classGroup.state();
        this.startsAt = classGroup.startsAt();
        this.endsAt = classGroup.endsAt();
        this.shift = classGroup.shift();
    }

    public static OrganizationClassGroupTreeView from(ClassGroup classGroup) {
        return new OrganizationClassGroupTreeView(classGroup);
    }

    public long getId() {
        return id;
    }

    public long getCourseId() {
        return courseId;
    }

    public long getSubjectId() {
        return subjectId;
    }

    public String getCode() {
        return code == null || code.isBlank() ? "Class group " + id : code;
    }

    public String getModalityLabel() {
        return switch (modality) {
            case ONSITE -> "On-site";
            case ONLINE -> "Online";
            case HYBRID -> "Hybrid";
        };
    }

    public String getStateLabel() {
        return switch (state) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case CLOSED -> "Closed";
            case ARCHIVED -> "Archived";
        };
    }

    public String getStateBadgeClass() {
        return switch (state) {
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-warning-30 text-warning-600";
            case CLOSED -> "bg-info-50 text-info-600";
            case ARCHIVED -> "bg-danger-50 text-danger-600";
        };
    }

    public boolean isArchived() {
        return state == ClassGroupState.ARCHIVED;
    }

    public String getShift() {
        return shift == null ? "-" : shift.getLabel();
    }

    public String getDateRangeLabel() {
        if (startsAt == null && endsAt == null) {
            return "-";
        }
        return (startsAt == null ? "-" : startsAt.toString())
                + " to "
                + (endsAt == null ? "-" : endsAt.toString());
    }
}
