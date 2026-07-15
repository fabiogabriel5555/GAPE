package pt.isel.gape.web.view;

import java.util.List;

import pt.isel.gape.learning.model.CourseFrequency;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CurricularTerm;

public final class SubjectCourseView {

    private final CourseView course;
    private final long subjectId;
    private final Integer curricularYear;
    private final CurricularTerm term;
    private final boolean mandatory;

    private SubjectCourseView(
            CourseSubjectAssociation association,
            CourseView course
    ) {
        this.course = course;
        this.subjectId = association.subjectId();
        this.curricularYear = association.curricularYear();
        this.term = association.term();
        this.mandatory = association.mandatory();
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
        return term.labelForCourseYear(curricularYear);
    }

    public List<SelectOptionView> getCourseTermOptions() {
        CourseFrequency frequency = CourseFrequency.parse(course.getFrequency());
        return frequency.terms().stream()
                .map(option -> new SelectOptionView(option.name(), option.label(), option == term))
                .toList();
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public String getMandatoryLabel() {
        return mandatory ? "Mandatory" : "Optional";
    }

}
