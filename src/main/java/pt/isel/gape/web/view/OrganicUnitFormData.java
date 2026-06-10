package pt.isel.gape.web.view;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.structure.model.OrganicUnit;
import pt.isel.gape.structure.model.OrganicUnitState;
import pt.isel.gape.structure.model.OrganicUnitType;

public final class OrganicUnitFormData {

    private final Long id;
    private final long organizationId;
    private final String code;
    private final String name;
    private final String acronym;
    private final String type;
    private final String state;
    private final Long parentOrganicUnitId;

    public OrganicUnitFormData(
            Long id,
            long organizationId,
            String code,
            String name,
            String acronym,
            String type,
            String state,
            Long parentOrganicUnitId
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.code = code;
        this.name = name;
        this.acronym = acronym;
        this.type = type;
        this.state = state;
        this.parentOrganicUnitId = parentOrganicUnitId;
    }

    public static OrganicUnitFormData blank(long organizationId) {
        return new OrganicUnitFormData(
                null,
                organizationId,
                "",
                "",
                "",
                OrganicUnitType.DEPARTMENT.name(),
                OrganicUnitState.ACTIVE.name(),
                null
        );
    }

    public static OrganicUnitFormData from(OrganicUnit unit) {
        return new OrganicUnitFormData(
                unit.id(),
                unit.organizationId(),
                unit.code(),
                unit.name(),
                unit.acronym() == null ? "" : unit.acronym(),
                unit.type().name(),
                unit.state().name(),
                unit.parentOrganicUnitId()
        );
    }

    public static OrganicUnitFormData from(HttpServletRequest request, Long id, long organizationId) {
        return from(request, id, organizationId, "");
    }

    public static OrganicUnitFormData from(HttpServletRequest request, Long id, long organizationId, String code) {
        return new OrganicUnitFormData(
                id,
                organizationId,
                code == null ? "" : code,
                value(request, "name"),
                value(request, "acronym"),
                value(request, "type"),
                value(request, "state"),
                optionalLong(request, "parentOrganicUnitId")
        );
    }

    public Long getId() {
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
        return acronym;
    }

    public String getType() {
        return type;
    }

    public String getState() {
        return state;
    }

    public Long getParentOrganicUnitId() {
        return parentOrganicUnitId;
    }

    private static Long optionalLong(HttpServletRequest request, String name) {
        String value = value(request, name);
        return value.isBlank() ? null : Long.parseLong(value);
    }

    private static String value(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }
}
