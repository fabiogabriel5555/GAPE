package pt.isel.gape.web.view;

import java.util.List;

public final class StudentSubjectCourseGroupView {

    private final CourseView course;
    private final List<StudentCurricularSubjectView> subjects;

    public StudentSubjectCourseGroupView(CourseView course, List<StudentCurricularSubjectView> subjects) {
        this.course = course;
        this.subjects = List.copyOf(subjects);
    }

    public CourseView getCourse() {
        return course;
    }

    public List<StudentCurricularSubjectView> getSubjects() {
        return subjects;
    }
}
