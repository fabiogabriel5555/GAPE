package pt.isel.gape.web.view;

import java.time.LocalDate;

import pt.isel.gape.common.time.ApplicationDateTimeFormat;
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
            case DRAFT -> "Draft";
            case SCHEDULED -> "Scheduled";
            case ACTIVE -> "Active";
            case COMPLETED -> "Completed";
        };
    }

    public String getStateBadgeClass() {
        return switch (state) {
            case DRAFT -> "bg-neutral-30 text-neutral-600";
            case SCHEDULED -> "bg-info-50 text-info-600";
            case ACTIVE -> "bg-success-50 text-success-600";
            case COMPLETED -> "bg-danger-50 text-danger-600";
        };
    }

    public boolean isCompleted() {
        return state == ClassGroupState.COMPLETED;
    }

    public String getShift() {
        return shift == null ? "-" : shift.getLabel();
    }

    public String getDateRangeLabel() {
        if (startsAt == null && endsAt == null) {
            return "-";
        }
        return (startsAt == null ? "-" : ApplicationDateTimeFormat.date(startsAt))
                + " to "
                + (endsAt == null ? "-" : ApplicationDateTimeFormat.date(endsAt));
    }
}
