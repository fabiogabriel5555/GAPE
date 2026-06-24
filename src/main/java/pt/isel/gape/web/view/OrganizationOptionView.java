package pt.isel.gape.web.view;

import pt.isel.gape.structure.model.Organization;

public final class OrganizationOptionView {

    private final long id;
    private final String name;
    private final String acronym;

    private OrganizationOptionView(Organization organization) {
        this.id = organization.id();
        this.name = organization.name();
        this.acronym = organization.acronym();
    }

    public static OrganizationOptionView from(Organization organization) {
        return new OrganizationOptionView(organization);
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAcronym() {
        return acronym == null || acronym.isBlank() ? name : acronym;
    }

    public String getLabel() {
        return getAcronym().equals(name) ? name : getAcronym() + " - " + name;
    }
}
