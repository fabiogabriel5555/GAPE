package pt.isel.gape.web.view;

import java.util.List;

import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.structure.model.OrganizationState;
import pt.isel.gape.structure.model.OrganizationType;

public final class OrganizationView {

    private final long id;
    private final String name;
    private final String acronym;
    private final String photo;
    private final OrganizationType type;
    private final OrganizationState state;
    private final int organicUnitCount;
    private final List<OrganicUnitView> organicUnits;

    private OrganizationView(Organization organization, int organicUnitCount, List<OrganicUnitView> organicUnits) {
        this.id = organization.id();
        this.name = organization.name();
        this.acronym = organization.acronym();
        this.photo = organization.photo();
        this.type = organization.type();
        this.state = organization.state();
        this.organicUnits = List.copyOf(organicUnits);
        this.organicUnitCount = organicUnitCount;
    }

    public static OrganizationView from(Organization organization, int organicUnitCount) {
        return new OrganizationView(organization, organicUnitCount, List.of());
    }

    public static OrganizationView from(Organization organization, List<OrganicUnitView> organicUnits) {
        return new OrganizationView(organization, organicUnits.size(), organicUnits);
    }

    public long getId() {
        return id;
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

    public boolean getHasPhoto() {
        return isHasPhoto();
    }

    public String getType() {
        return type.name();
    }

    public String getTypeLabel() {
        return switch (type) {
            case EDUCATIONAL_INSTITUTION -> "Educational institution";
            case TRAINING_COMPANY -> "Training company";
            case COMPANY -> "Company";
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
            case ARCHIVED -> "Archived";
        };
    }

    public String getStateBadgeClass() {
        return switch (state) {
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-warning-30 text-warning-600";
            case ARCHIVED -> "bg-danger-50 text-danger-600";
        };
    }

    public boolean isActive() {
        return state == OrganizationState.ACTIVE;
    }

    public boolean isInactive() {
        return state == OrganizationState.INACTIVE;
    }

    public boolean isArchived() {
        return state == OrganizationState.ARCHIVED;
    }

    public int getOrganicUnitCount() {
        return organicUnitCount;
    }

    public List<OrganicUnitView> getOrganicUnits() {
        return organicUnits;
    }
}
