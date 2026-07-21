package pt.isel.gape.web.view;

import java.util.Objects;

import pt.isel.gape.transversal.model.ManagementViewScope;

/** A server-authorised target option for a context-bound panel scope. */
public final class ManagementViewScopeTargetOptionView {

    private final ManagementViewScope scope;
    private final long targetId;
    private final String label;

    public ManagementViewScopeTargetOptionView(ManagementViewScope scope, long targetId, String label) {
        this.scope = Objects.requireNonNull(scope, "scope is required");
        if (targetId <= 0) {
            throw new IllegalArgumentException("targetId must be positive");
        }
        this.targetId = targetId;
        this.label = Objects.requireNonNull(label, "label is required");
    }

    public String getScopeValue() {
        return scope.toDatabaseValue();
    }

    public long getTargetId() {
        return targetId;
    }

    public String getLabel() {
        return label;
    }
}
