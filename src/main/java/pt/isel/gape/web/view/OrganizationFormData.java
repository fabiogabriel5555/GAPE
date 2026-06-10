package pt.isel.gape.web.view;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.structure.model.OrganizationState;
import pt.isel.gape.structure.model.OrganizationType;

public final class OrganizationFormData {

    private final Long id;
    private final String name;
    private final String acronym;
    private final String photo;
    private final String type;
    private final String state;
    private final Set<Long> administratorUserIds;

    public OrganizationFormData(
            Long id,
            String name,
            String acronym,
            String photo,
            String type,
            String state,
            Set<Long> administratorUserIds
    ) {
        this.id = id;
        this.name = name;
        this.acronym = acronym;
        this.photo = photo;
        this.type = type;
        this.state = state;
        this.administratorUserIds = administratorUserIds == null ? Set.of() : Set.copyOf(administratorUserIds);
    }

    public static OrganizationFormData blank(long currentAdminUserId) {
        return new OrganizationFormData(
                null,
                "",
                "",
                "",
                OrganizationType.EDUCATIONAL_INSTITUTION.name(),
                OrganizationState.ACTIVE.name(),
                Set.of(currentAdminUserId)
        );
    }

    public static OrganizationFormData from(Organization organization) {
        return new OrganizationFormData(
                organization.id(),
                organization.name(),
                organization.acronym() == null ? "" : organization.acronym(),
                organization.photo() == null ? "" : organization.photo(),
                organization.type().name(),
                organization.state().name(),
                Set.of()
        );
    }

    public static OrganizationFormData from(HttpServletRequest request, Long id) {
        return new OrganizationFormData(
                id,
                value(request, "name"),
                value(request, "acronym"),
                value(request, "photo"),
                value(request, "type"),
                value(request, "state"),
                selectedAdministratorIds(request)
        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAcronym() {
        return acronym;
    }

    public String getPhoto() {
        return photo;
    }

    public String getType() {
        return type;
    }

    public String getState() {
        return state;
    }

    public Set<Long> getAdministratorUserIds() {
        return administratorUserIds;
    }

    public boolean hasSelectedAdministrator(long userId) {
        return administratorUserIds.contains(userId);
    }

    private static Set<Long> selectedAdministratorIds(HttpServletRequest request) {
        String[] values = request.getParameterValues("administratorUserIds");
        if (values == null) {
            return Set.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                ids.add(Long.parseLong(value));
            }
        }
        return ids;
    }

    private static String value(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }
}
