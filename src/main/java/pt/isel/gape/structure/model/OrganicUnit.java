package pt.isel.gape.structure.model;

public record OrganicUnit(
        long id,
        long organizationId,
        String code,
        String name,
        String acronym,
        OrganicUnitType type,
        OrganicUnitState state,
        Long parentOrganicUnitId
) {
}
