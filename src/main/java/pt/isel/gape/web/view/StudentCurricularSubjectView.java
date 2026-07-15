package pt.isel.gape.web.view;

import java.util.Objects;

public final class StudentCurricularSubjectView {

    private final CourseView course;
    private final CourseSubjectView subject;

    public StudentCurricularSubjectView(CourseView course, CourseSubjectView subject) {
        this.course = Objects.requireNonNull(course, "course is required");
        this.subject = Objects.requireNonNull(subject, "subject is required");
    }

    public CourseView getCourse() {
        return course;
    }

    public CourseSubjectView getSubject() {
        return subject;
    }
}
