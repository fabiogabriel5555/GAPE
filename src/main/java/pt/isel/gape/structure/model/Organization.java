package pt.isel.gape.structure.model;

public record Organization(
        long id,
        String name,
        String acronym,
        String photo,
        OrganizationType type,
        OrganizationState state
) {
}
