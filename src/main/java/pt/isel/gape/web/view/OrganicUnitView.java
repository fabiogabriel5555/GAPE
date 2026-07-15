package pt.isel.gape.web.view;

import java.util.List;

import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.OrganicUnitState;
import pt.isel.gape.structure.model.OrganicUnitType;

public final class OrganicUnitView {

    private final long id;
    private final long organizationId;
    private final String code;
    private final String name;
    private final String acronym;
    private final OrganicUnitType type;
    private final OrganicUnitState state;
    private final Long parentOrganicUnitId;
    private final String parentLabel;
    private final int hierarchyDepth;
    private final List<OrganizationCourseTreeView> courses;

    private OrganicUnitView(
            OrganicUnit unit,
            String parentLabel,
            int hierarchyDepth,
            List<OrganizationCourseTreeView> courses
    ) {
        this.id = unit.id();
        this.organizationId = unit.organizationId();
        this.code = unit.code();
        this.name = unit.name();
        this.acronym = unit.acronym();
        this.type = unit.type();
        this.state = unit.state();
        this.parentOrganicUnitId = unit.parentOrganicUnitId();
        this.parentLabel = parentLabel;
        this.hierarchyDepth = hierarchyDepth;
        this.courses = List.copyOf(courses);
    }

    public static OrganicUnitView from(OrganicUnit unit, String parentLabel, int hierarchyDepth) {
        return new OrganicUnitView(unit, parentLabel, hierarchyDepth, List.of());
    }

    public static OrganicUnitView from(
            OrganicUnit unit,
            String parentLabel,
            int hierarchyDepth,
            List<OrganizationCourseTreeView> courses
    ) {
        return new OrganicUnitView(unit, parentLabel, hierarchyDepth, courses);
    }

    public long getId() {
        return id;
    }

    public long getOrganizationId() {
        return organizationId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getAcronym() {
        return acronym == null || acronym.isBlank() ? "-" : acronym;
    }

    public String getType() {
        return type.name();
    }

    public String getTypeLabel() {
        return switch (type) {
            case SCHOOL -> "School";
            case FACULTY -> "Faculty";
            case DEPARTMENT -> "Department";
            case CENTER -> "Center";
            case OFFICE -> "Office";
            case SERVICE -> "Service";
            case SECTION -> "Section";
            case DIRECTION -> "Direction";
            case OTHER -> "Other";
        };
    }

    public String getState() {
        return state.name();
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
        return state == OrganicUnitState.INACTIVE;
    }

    public Long getParentOrganicUnitId() {
        return parentOrganicUnitId;
    }

    public String getParentLabel() {
        return parentLabel == null || parentLabel.isBlank() ? "Root" : parentLabel;
    }

    public int getHierarchyDepth() {
        return hierarchyDepth;
    }

    public int getHierarchyIndent() {
        return hierarchyDepth * 24;
    }

    public int getCourseCount() {
        return courses.size();
    }

    public List<OrganizationCourseTreeView> getCourses() {
        return courses;
    }
}
