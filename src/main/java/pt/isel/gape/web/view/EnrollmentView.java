package pt.isel.gape.web.view;

import pt.isel.gape.learning.model.EnrollmentState;

public final class EnrollmentView {

    private final CourseView course;
    private final CourseSubjectView subject;

    private EnrollmentView(CourseView course, CourseSubjectView subject) {
        this.course = course;
        this.subject = subject;
    }

    public static EnrollmentView course(CourseView course) {
        return new EnrollmentView(course, null);
    }

    public static EnrollmentView subject(CourseView course, CourseSubjectView subject) {
        return new EnrollmentView(course, subject);
    }

    public CourseView getCourse() {
        return course;
    }

    public CourseSubjectView getSubject() {
        return subject;
    }

    public boolean isSubjectEnrollment() {
        return subject != null;
    }

    public String getTitle() {
        return subject == null ? course.getName() : subject.getSubjectName();
    }

    public String getContextLabel() {
        return subject == null ? course.getOrganizationName() : course.getName();
    }

    public String getStateLabel() {
        if (subject != null) {
            return subject.getEnrollmentStateLabel();
        }
        return course.getEnrollmentStateLabel();
    }

    public String getBadgeClass() {
        if (subject != null) {
            return subject.getEnrollmentBadgeClass();
        }
        return course.getEnrollmentBadgeClass();
    }

    public boolean isActiveEnrollment() {
        if (subject != null) {
            return subject.isActiveEnrollment();
        }
        return course.isActiveEnrollment();
    }

    public EnrollmentState getState() {
        if (subject != null) {
            EnrollmentState state = subject.getEnrollmentState();
            return state == null ? EnrollmentState.WITHDRAWN : state;
        }
        EnrollmentState state = course.getEnrollmentState();
        return state == null ? EnrollmentState.WITHDRAWN : state;
    }
}
