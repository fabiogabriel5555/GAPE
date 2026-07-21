package pt.isel.gape.web.view;

import java.util.Objects;

import pt.isel.gape.transversal.model.ManagementViewScope;

/** A scope option made available by the server for panel configuration. */
public final class ManagementViewScopeChoiceView {

    private final ManagementViewScope scope;

    public ManagementViewScopeChoiceView(ManagementViewScope scope) {
        this.scope = Objects.requireNonNull(scope, "scope is required");
    }

    public String getValue() {
        return scope.toDatabaseValue();
    }

    public String getLabel() {
        return ManagementDashboardView.scopeLabel(scope);
    }

    public boolean isTargetRequired() {
        return scope.requiresTarget() && scope != ManagementViewScope.PERSONAL;
    }
}
