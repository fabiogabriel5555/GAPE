package pt.isel.gape.transversal.service;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.transversal.model.ManagementViewScope;

/**
 * Security boundary used by management-view mutations.  Persistence and
 * aggregation do not decide who controls a given academic context.
 */
@FunctionalInterface
public interface ManagementViewConfigurationAuthorizer {

    void requireConfigurationAccess(
            long actorUserId,
            AccessProfileType actorProfileType,
            ManagementViewScope scope,
            Long scopeContextId,
            Long ownerUserId
    );
}
