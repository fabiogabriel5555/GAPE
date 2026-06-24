package pt.isel.gape.web.view;

import pt.isel.gape.structure.model.OrganicUnit;

public final class OrganicUnitOptionView {

    private final long id;
    private final long organizationId;
    private final String code;
    private final String name;
    private final String acronym;

    private OrganicUnitOptionView(OrganicUnit unit) {
        this.id = unit.id();
        this.organizationId = unit.organizationId();
        this.code = unit.code();
        this.name = unit.name();
        this.acronym = unit.acronym();
    }

    public static OrganicUnitOptionView from(OrganicUnit unit) {
        return new OrganicUnitOptionView(unit);
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
        return acronym == null || acronym.isBlank() ? code : acronym;
    }

    public String getLabel() {
        return getAcronym() + " - " + name;
    }
}
