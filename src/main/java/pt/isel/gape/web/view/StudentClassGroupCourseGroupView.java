package pt.isel.gape.web.view;

import java.util.List;

public final class StudentClassGroupCourseGroupView {

    private final long courseId;
    private final String courseName;
    private final List<StudentClassGroupCourseSubjectGroupView> subjectGroups;

    public StudentClassGroupCourseGroupView(
            long courseId,
            String courseName,
            List<StudentClassGroupCourseSubjectGroupView> subjectGroups
    ) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.subjectGroups = List.copyOf(subjectGroups);
    }

    public long getCourseId() {
        return courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public List<StudentClassGroupCourseSubjectGroupView> getSubjectGroups() {
        return subjectGroups;
    }

    public int getClassGroupCount() {
        return subjectGroups.stream()
                .mapToInt(group -> group.getClassGroups().size())
                .sum();
    }
}
