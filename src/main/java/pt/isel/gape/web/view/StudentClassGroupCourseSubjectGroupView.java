package pt.isel.gape.web.view;

import java.util.List;

public final class StudentClassGroupCourseSubjectGroupView {

    private final ClassGroupView context;
    private final List<StudentClassGroupView> classGroups;

    public StudentClassGroupCourseSubjectGroupView(
            ClassGroupView context,
            List<StudentClassGroupView> classGroups
    ) {
        this.context = context;
        this.classGroups = List.copyOf(classGroups);
    }

    public long getCourseId() {
        return context.getCourseId();
    }

    public long getSubjectId() {
        return context.getSubjectId();
    }

    public String getCourseName() {
        return context.getCourseName();
    }

    public String getSubjectName() {
        return context.getSubjectName();
    }

    public String getContextTitle() {
        return context.getContextGroupTitle();
    }

    public String getContextHtml() {
        return context.getContextGroupHtml();
    }

    public List<StudentClassGroupView> getClassGroups() {
        return classGroups;
    }
}
