package pt.isel.gape.web.view;

import java.util.List;

import pt.isel.gape.common.validation.MediaPathValidator;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.CourseState;
import pt.isel.gape.learning.model.CourseType;

public final class OrganizationCourseTreeView {

    private final long id;
    private final long organizationId;
    private final Long organicUnitId;
    private final String name;
    private final String acronym;
    private final String photo;
    private final CourseType type;
    private final CourseState state;
    private final List<OrganizationSubjectTreeView> subjects;

    private OrganizationCourseTreeView(Course course, List<OrganizationSubjectTreeView> subjects) {
        this.id = course.id();
        this.organizationId = course.organizationId();
        this.organicUnitId = course.organicUnitId();
        this.name = course.name();
        this.acronym = course.acronym();
        this.photo = MediaPathValidator.safeRelativePath(course.photo()).orElse(null);
        this.type = course.type();
        this.state = course.state();
        this.subjects = List.copyOf(subjects);
    }

    public static OrganizationCourseTreeView from(Course course, List<OrganizationSubjectTreeView> subjects) {
        return new OrganizationCourseTreeView(course, subjects);
    }

    public long getId() {
        return id;
    }

    public long getOrganizationId() {
        return organizationId;
    }

    public Long getOrganicUnitId() {
        return organicUnitId;
    }

    public String getName() {
        return name;
    }

    public String getAcronym() {
        return acronym == null || acronym.isBlank() ? "-" : acronym;
    }

    public String getPhoto() {
        return photo;
    }

    public boolean isHasPhoto() {
        return photo != null && !photo.isBlank();
    }

    public String getTypeLabel() {
        return switch (type) {
            case DEGREE -> "Degree";
            case MASTER -> "Master";
            case SHORT_COURSE -> "Short course";
            case PROFESSIONAL_TRAINING -> "Professional training";
            case OTHER -> "Other";
        };
    }

    public String getStateLabel() {
        return switch (state) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
        };
    }

    public String getStateBadgeClass() {
        return switch (state) {
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-danger-50 text-danger-600";
        };
    }

    public boolean isInactive() {
        return state == CourseState.INACTIVE;
    }

    public int getSubjectCount() {
        return subjects.size();
    }

    public List<OrganizationSubjectTreeView> getSubjects() {
        return subjects;
    }
}
