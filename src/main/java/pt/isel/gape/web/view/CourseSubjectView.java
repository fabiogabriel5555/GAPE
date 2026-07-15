package pt.isel.gape.web.view;

import java.math.BigDecimal;

import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CurricularTerm;

public final class CourseSubjectView {

    private final long courseId;
    private final long subjectId;
    private final SubjectView subject;
    private final Integer curricularYear;
    private final CurricularTerm term;
    private final boolean mandatory;

    private CourseSubjectView(CourseSubjectAssociation association, SubjectView subject) {
        this.courseId = association.courseId();
        this.subjectId = association.subjectId();
        this.subject = subject;
        this.curricularYear = association.curricularYear();
        this.term = association.term();
        this.mandatory = association.mandatory();
    }

    public static CourseSubjectView from(CourseSubjectAssociation association, SubjectView subject) {
        return new CourseSubjectView(association, subject);
    }

    public long getCourseId() {
        return courseId;
    }

    public long getSubjectId() {
        return subjectId;
    }

    public SubjectView getSubject() {
        return subject;
    }

    public String getSubjectName() {
        return subject.getName();
    }

    public String getSubjectAcronym() {
        return subject.getAcronym();
    }

    public String getSubjectEctsLabel() {
        return subject.getEctsLabel();
    }

    public BigDecimal getSubjectEcts() {
        return subject.getEcts();
    }

    public String getSubjectFinalGradeMaxLabel() {
        return subject.getFinalGradeMaxLabel();
    }

    public String getSubjectWorkloadHoursLabel() {
        return subject.getWorkloadHoursLabel();
    }

    public Integer getCurricularYear() {
        return curricularYear;
    }

    public String getTerm() {
        return term == null ? "" : term.name();
    }

    public String getCurricularPositionLabel() {
        if (curricularYear == null || term == null) {
            return "No curricular position";
        }
        return term.labelForCourseYear(curricularYear);
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public String getMandatoryLabel() {
        return mandatory ? "Mandatory" : "Optional";
    }

    public static String labelFor(CurricularTerm term) {
        return term.label();
    }
}
