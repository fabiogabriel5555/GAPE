package pt.isel.gape.structure.model;

public record OrganizationUpdateCommand(
        String name,
        String acronym,
        String photo,
        OrganizationType type,
        OrganizationState state
) {
}
