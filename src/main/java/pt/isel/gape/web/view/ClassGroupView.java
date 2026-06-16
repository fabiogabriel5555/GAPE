package pt.isel.gape.web.view;

import java.time.LocalDate;

import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupModality;
import pt.isel.gape.learning.model.ClassGroupState;

public final class ClassGroupView {

    private final long id;
    private final long courseId;
    private final long subjectId;
    private final String code;
    private final ClassGroupModality modality;
    private final ClassGroupState state;
    private final Integer minStudents;
    private final Integer maxStudents;
    private final LocalDate startsAt;
    private final LocalDate endsAt;
    private final String shift;
    private final CourseView course;
    private final SubjectView subject;
    private final int activeEnrollmentCount;
    private final int blockCount;
    private final int teacherCount;

    private ClassGroupView(
            ClassGroup classGroup,
            CourseView course,
            SubjectView subject,
            int activeEnrollmentCount,
            int blockCount,
            int teacherCount
    ) {
        this.id = classGroup.id();
        this.courseId = classGroup.courseId();
        this.subjectId = classGroup.subjectId();
        this.code = classGroup.code();
        this.modality = classGroup.modality();
        this.state = classGroup.state();
        this.minStudents = classGroup.minStudents();
        this.maxStudents = classGroup.maxStudents();
        this.startsAt = classGroup.startsAt();
        this.endsAt = classGroup.endsAt();
        this.shift = classGroup.shift();
        this.course = course;
        this.subject = subject;
        this.activeEnrollmentCount = activeEnrollmentCount;
        this.blockCount = blockCount;
        this.teacherCount = teacherCount;
    }

    public static ClassGroupView from(
            ClassGroup classGroup,
            CourseView course,
            SubjectView subject,
            int activeEnrollmentCount,
            int blockCount,
            int teacherCount
    ) {
        return new ClassGroupView(classGroup, course, subject, activeEnrollmentCount, blockCount, teacherCount);
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

    public String getModality() {
        return modality.name();
    }

    public String getModalityLabel() {
        return switch (modality) {
            case ONSITE -> "On-site";
            case ONLINE -> "Online";
            case HYBRID -> "Hybrid";
        };
    }

    public String getState() {
        return state.name();
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

    public boolean isActive() {
        return state == ClassGroupState.ACTIVE;
    }

    public boolean isArchived() {
        return state == ClassGroupState.ARCHIVED;
    }

    public Integer getMinStudents() {
        return minStudents;
    }

    public Integer getMaxStudents() {
        return maxStudents;
    }

    public String getCapacityLabel() {
        String min = minStudents == null ? "-" : minStudents.toString();
        String max = maxStudents == null ? "-" : maxStudents.toString();
        return min + " / " + max;
    }

    public String getActiveEnrollmentCountLabel() {
        return activeEnrollmentCount == 1 ? "1 active student" : activeEnrollmentCount + " active students";
    }

    public String getStartsAt() {
        return startsAt == null ? "" : startsAt.toString();
    }

    public String getEndsAt() {
        return endsAt == null ? "" : endsAt.toString();
    }

    public String getDateRangeLabel() {
        if (startsAt == null && endsAt == null) {
            return "-";
        }
        return (startsAt == null ? "-" : startsAt.toString()) + " to " + (endsAt == null ? "-" : endsAt.toString());
    }

    public String getShift() {
        return shift == null || shift.isBlank() ? "-" : shift;
    }

    public CourseView getCourse() {
        return course;
    }

    public SubjectView getSubject() {
        return subject;
    }

    public String getCourseName() {
        return course.getName();
    }

    public String getSubjectName() {
        return subject.getName();
    }

    public int getActiveEnrollmentCount() {
        return activeEnrollmentCount;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public int getTeacherCount() {
        return teacherCount;
    }

    public String getTeacherCountLabel() {
        return teacherCount == 1 ? "1 teacher" : teacherCount + " teachers";
    }

    public String getContextLabel() {
        return getCode() + " | " + subject.getAcronym() + " | " + course.getSubjectManagementContextLabel();
    }

    public String getContextHtml() {
        return contextPartHtml(getCode(), getCode())
                + " | "
                + contextPartHtml(subject.getAcronym(), subject.getName())
                + " | "
                + course.getSubjectManagementContextHtml();
    }

    public String getContextTitle() {
        return getCode() + " | " + subject.getName() + " | " + course.getSubjectManagementContextTitle();
    }

    private static String contextPartHtml(String acronym, String name) {
        String compact = acronym == null || acronym.isBlank() ? name : acronym;
        compact = compact == null || compact.isBlank() ? "-" : compact;
        return "<span class=\"gape-acronym-token\" tabindex=\"0\" title=\""
                + escapeHtml(name == null || name.isBlank() ? compact : name)
                + "\">"
                + escapeHtml(compact)
                + "</span>";
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
