package pt.isel.gape.structure.model;

public record OrganicUnitCreateCommand(
        long organizationId,
        String code,
        String name,
        String acronym,
        OrganicUnitType type,
        OrganicUnitState state,
        Long parentOrganicUnitId
) {
}
