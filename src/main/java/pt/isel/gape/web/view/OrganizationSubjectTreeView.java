package pt.isel.gape.web.view;

import java.util.List;

import pt.isel.gape.learning.model.CourseSubjectAssociation;
import pt.isel.gape.learning.model.CourseSubjectState;
import pt.isel.gape.learning.model.CurricularTerm;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.learning.model.SubjectState;

public final class OrganizationSubjectTreeView {

    private final long courseId;
    private final long subjectId;
    private final String name;
    private final String acronym;
    private final Integer curricularYear;
    private final CurricularTerm term;
    private final boolean mandatory;
    private final CourseSubjectState state;
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
        this.curricularYear = association.curricularYear();
        this.term = association.term();
        this.mandatory = association.mandatory();
        this.state = association.state();
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

    public String getName() {
        return name;
    }

    public String getAcronym() {
        return acronym == null || acronym.isBlank() ? "-" : acronym;
    }

    public String getCurricularPositionLabel() {
        if (curricularYear == null || term == null) {
            return "No curricular position";
        }
        return curricularYear + " year | " + labelFor(term);
    }

    public String getMandatoryLabel() {
        return mandatory ? "Mandatory" : "Optional";
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
            case INACTIVE -> "bg-warning-30 text-warning-600";
        };
    }

    public boolean isArchived() {
        return subjectState == SubjectState.INACTIVE || state == CourseSubjectState.INACTIVE;
    }

    public int getClassGroupCount() {
        return classGroups.size();
    }

    public List<OrganizationClassGroupTreeView> getClassGroups() {
        return classGroups;
    }

    private static String labelFor(CurricularTerm term) {
        return switch (term) {
            case ANNUAL -> "Annual";
            case SEMESTER_1 -> "1st semester";
            case SEMESTER_2 -> "2nd semester";
            case TRIMESTER_1 -> "1st trimester";
            case TRIMESTER_2 -> "2nd trimester";
            case TRIMESTER_3 -> "3rd trimester";
        };
    }
}
