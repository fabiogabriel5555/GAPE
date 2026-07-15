package pt.isel.gape.web.view;

public final class EnrollmentView {

    private final CourseView course;

    private EnrollmentView(CourseView course) {
        this.course = course;
    }

    public static EnrollmentView course(CourseView course) {
        return new EnrollmentView(course);
    }

    public CourseView getCourse() {
        return course;
    }

    public String getStateLabel() {
        return course.getEnrollmentStateLabel();
    }

    public String getBadgeClass() {
        return course.getEnrollmentBadgeClass();
    }
}
