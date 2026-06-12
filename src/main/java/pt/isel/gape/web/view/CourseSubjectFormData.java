package pt.isel.gape.web.view;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseSubjectState;

public final class CourseSubjectFormData {

    private final String courseId;
    private final String subjectId;
    private final String curricularYear;
    private final String term;
    private final boolean mandatory;
    private final String state;

    public CourseSubjectFormData(
            String courseId,
            String subjectId,
            String curricularYear,
            String term,
            boolean mandatory,
            String state
    ) {
        this.courseId = courseId;
        this.subjectId = subjectId;
        this.curricularYear = curricularYear;
        this.term = term;
        this.mandatory = mandatory;
        this.state = state;
    }

    public static CourseSubjectFormData blank(long courseId) {
        return new CourseSubjectFormData(
                Long.toString(courseId),
                "",
                "",
                "",
                true,
                CourseSubjectState.ACTIVE.name()
        );
    }

    public static CourseSubjectFormData from(CourseSubjectAssociation association) {
        return new CourseSubjectFormData(
                Long.toString(association.courseId()),
                Long.toString(association.subjectId()),
                association.curricularYear() == null ? "" : Integer.toString(association.curricularYear()),
                association.term() == null ? "" : association.term().name(),
                association.mandatory(),
                association.state().name()
        );
    }

    public static CourseSubjectFormData from(HttpServletRequest request, long courseId) {
        return new CourseSubjectFormData(
                Long.toString(courseId),
                value(request, "subjectId"),
                value(request, "curricularYear"),
                value(request, "term"),
                request.getParameter("mandatory") != null,
                value(request, "state")
        );
    }

    public String getCourseId() {
        return courseId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getCurricularYear() {
        return curricularYear;
    }

    public String getTerm() {
        return term;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public String getState() {
        return state;
    }

    private static String value(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }
}
