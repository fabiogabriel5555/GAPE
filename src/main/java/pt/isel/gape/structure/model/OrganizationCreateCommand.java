package pt.isel.gape.structure.model;

import java.util.Set;

public record OrganizationCreateCommand(
        String name,
        String acronym,
        String photo,
        OrganizationType type,
        OrganizationState state,
        Set<Long> administratorUserIds
) {
}
