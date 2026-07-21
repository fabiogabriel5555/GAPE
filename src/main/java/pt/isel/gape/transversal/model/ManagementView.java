package pt.isel.gape.transversal.model;

import java.util.Objects;

/**
 * A configurable dashboard, report or control panel.
 *
 * <p>The owner is always retained so a personal view can never be inferred
 * from an arbitrary explicit access grant.</p>
 */
public record ManagementView(
        long id,
        String title,
        ManagementViewType type,
        String description,
        ManagementViewScope visibilityScope,
        ManagementViewScopeTargetType scopeTargetType,
        Long scopeTargetId,
        Long ownerUserId,
        ManagementViewState state
) {

    public ManagementView {
        Objects.requireNonNull(title, "title is required");
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(visibilityScope, "visibilityScope is required");
        Objects.requireNonNull(state, "state is required");
    }

    /** Compatibility-oriented, clearer name for clients that treat the target as a context. */
    public Long scopeContextId() {
        return scopeTargetId;
    }

    public boolean isPersonal() {
        return visibilityScope == ManagementViewScope.PERSONAL;
    }
}
