package pt.isel.gape.structure.model;

public record OrganicUnitUpdateCommand(
        String code,
        String name,
        String acronym,
        OrganicUnitType type,
        OrganicUnitState state,
        Long parentOrganicUnitId
) {
}
