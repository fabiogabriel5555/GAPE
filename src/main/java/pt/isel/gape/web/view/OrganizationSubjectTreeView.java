package pt.isel.gape.web.view;

import java.util.List;

import pt.isel.gape.common.validation.MediaPathValidator;
import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CurricularTerm;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectState;

public final class OrganizationSubjectTreeView {

    private final long courseId;
    private final long subjectId;
    private final String name;
    private final String acronym;
    private final String photo;
    private final Integer curricularYear;
    private final CurricularTerm term;
    private final boolean mandatory;
    private final SubjectState subjectState;
    private final List<OrganizationClassGroupTreeView> classGroups;

    private OrganizationSubjectTreeView(
            CourseSubjectAssociation association,
            Subject subject,
            List<OrganizationClassGroupTreeView> classGroups
    ) {
        this.courseId = association.courseId();
        this.subjectId = subject.id();
        this.name = subject.name();
        this.acronym = subject.acronym();
        this.photo = MediaPathValidator.safeRelativePath(subject.photo()).orElse(null);
        this.curricularYear = association.curricularYear();
        this.term = association.term();
        this.mandatory = association.mandatory();
        this.subjectState = subject.state();
        this.classGroups = List.copyOf(classGroups);
    }

    public static OrganizationSubjectTreeView from(
            CourseSubjectAssociation association,
            Subject subject,
            List<OrganizationClassGroupTreeView> classGroups
    ) {
        return new OrganizationSubjectTreeView(association, subject, classGroups);
    }

    public long getCourseId() {
        return courseId;
    }

    public long getSubjectId() {
        return subjectId;
    }

    public String getTreeKey() {
        return courseId + "_" + subjectId;
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

    public String getCurricularPositionLabel() {
        if (curricularYear == null || term == null) {
            return "No curricular position";
        }
        return term.labelForCourseYear(curricularYear);
    }

    public String getMandatoryLabel() {
        return mandatory ? "Mandatory" : "Optional";
    }

    public String getStateLabel() {
        return switch (subjectState) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
        };
    }

    public String getStateBadgeClass() {
        return switch (subjectState) {
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-danger-50 text-danger-600";
        };
    }

    public boolean isInactive() {
        return subjectState == SubjectState.INACTIVE;
    }

    public int getClassGroupCount() {
        return classGroups.size();
    }

    public List<OrganizationClassGroupTreeView> getClassGroups() {
        return classGroups;
    }

}
