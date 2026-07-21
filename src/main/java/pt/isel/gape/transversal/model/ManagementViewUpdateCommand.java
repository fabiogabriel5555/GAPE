package pt.isel.gape.transversal.model;

/** Configuration fields that may be changed without transferring ownership. */
public record ManagementViewUpdateCommand(
        String title,
        ManagementViewType type,
        String description,
        ManagementViewScope visibilityScope,
        Long scopeContextId,
        ManagementViewState state
) {
}
