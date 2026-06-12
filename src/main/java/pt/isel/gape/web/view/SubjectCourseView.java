package pt.isel.gape.web.view;

import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseSubjectState;
import pt.isel.gape.learning.model.CurricularTerm;

public final class SubjectCourseView {

    private final CourseView course;
    private final long subjectId;
    private final Integer curricularYear;
    private final CurricularTerm term;
    private final boolean mandatory;
    private final CourseSubjectState state;

    private SubjectCourseView(CourseSubjectAssociation association, CourseView course) {
        this.course = course;
        this.subjectId = association.subjectId();
        this.curricularYear = association.curricularYear();
        this.term = association.term();
        this.mandatory = association.mandatory();
        this.state = association.state();
    }

    public static SubjectCourseView from(CourseSubjectAssociation association, CourseView course) {
        return new SubjectCourseView(association, course);
    }

    public CourseView getCourse() {
        return course;
    }

    public long getCourseId() {
        return course.getId();
    }

    public long getSubjectId() {
        return subjectId;
    }

    public String getCourseName() {
        return course.getName();
    }

    public String getCourseAcronym() {
        return course.getAcronym();
    }

    public String getCourseContextLabel() {
        return course.getSubjectManagementContextLabel();
    }

    public String getCourseContextHtml() {
        return course.getSubjectManagementContextHtml();
    }

    public String getCourseContextTitle() {
        return course.getSubjectManagementContextTitle();
    }

    public Integer getCurricularYear() {
        return curricularYear;
    }

    public String getTerm() {
        return term == null ? "" : term.name();
    }

    public String getCurricularPositionLabel() {
        if (curricularYear == null || term == null) {
            return "-";
        }
        return curricularYear + " - " + termLabel(term);
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public String getMandatoryLabel() {
        return mandatory ? "Mandatory" : "Optional";
    }

    public String getState() {
        return state.name();
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

    public boolean isArchived() {
        return state == CourseSubjectState.ARCHIVED;
    }

    private static String termLabel(CurricularTerm term) {
        return switch (term) {
            case ANNUAL -> "Annual";
            case SEMESTER_1 -> "1st semester";
            case SEMESTER_2 -> "2nd semester";
            case TRIMESTER_1 -> "1st trimester";
            case TRIMESTER_2 -> "2nd trimester";
            case TRIMESTER_3 -> "3rd trimester";
        };
    }
}
