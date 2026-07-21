package pt.isel.gape.transversal.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.Course;
import pt.isel.gape.learning.model.Subject;
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.structure.model.Organization;
import pt.isel.gape.transversal.model.ManagementView;
import pt.isel.gape.transversal.model.ManagementViewScope;
import pt.isel.gape.transversal.model.ManagementViewType;
import pt.isel.gape.transversal.model.ReportAggregation;

/**
 * Read model for the Dashboard management-view experience.
 *
 * <p>The web controller uses this service rather than domain DAOs.  It keeps
 * the list, the selected aggregation and the configuration catalogue tied to
 * the same live access rules that protect the domain services.</p>
 */
public final class ManagementDashboardQueryService {

    private final ManagementViewAccessService accessService;
    private final ManagementViewService managementViewService;
    private final ReportAggregationService reportAggregationService;
    private final ApplicationReadService applicationReadService;

    public ManagementDashboardQueryService(
            ManagementViewAccessService accessService,
            ManagementViewService managementViewService,
            ReportAggregationService reportAggregationService,
            ApplicationReadService applicationReadService
    ) {
        this.accessService = Objects.requireNonNull(accessService, "accessService is required");
        this.managementViewService = Objects.requireNonNull(
                managementViewService,
                "managementViewService is required"
        );
        this.reportAggregationService = Objects.requireNonNull(
                reportAggregationService,
                "reportAggregationService is required"
        );
        this.applicationReadService = Objects.requireNonNull(
                applicationReadService,
                "applicationReadService is required"
        );
    }

    /**
     * Resolves the only panel that is opened on this request.  The aggregation
     * call is intentionally made once: opening a sensitive panel is auditable,
     * whereas listing its title is not an access event.
     */
    public Snapshot load(AccessContext actor, Long requestedManagementViewId) {
        Objects.requireNonNull(actor, "actor is required");
        List<ManagementView> accessibleViews = accessService.listAccessible(actor);
        List<ManagementView> configurableViews = managementViewService.listConfigurable(actor);

        ManagementView selected = selectView(actor, accessibleViews, requestedManagementViewId);
        ReportAggregation aggregation = null;
        if (selected != null) {
            // requireAccess, invoked by aggregate, records the real opening.
            aggregation = reportAggregationService.aggregate(actor, selected.id());
        }
        Set<Long> configurableIds = configurableViews.stream()
                .map(ManagementView::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        List<Long> recipientUserIds = selected != null && configurableIds.contains(selected.id())
                ? managementViewService.explicitRecipientUserIds(actor, selected.id())
                : List.of();

        ScopeCatalogue catalogue = loadScopeCatalogue(actor);
        return new Snapshot(
                accessibleViews,
                selected,
                aggregation,
                configurableViews,
                recipientUserIds,
                catalogue.scopeChoices(),
                catalogue.scopeTargetOptions()
        );
    }

    private ManagementView selectView(
            AccessContext actor,
            List<ManagementView> accessibleViews,
            Long requestedManagementViewId
    ) {
        if (requestedManagementViewId != null) {
            if (requestedManagementViewId <= 0) {
                throw new IllegalArgumentException("Management view id must be positive");
            }
            return accessibleViews.stream()
                    .filter(view -> view.id() == requestedManagementViewId)
                    .findFirst()
                    // Let the access service make the protected request's
                    // final decision and audit a denied attempt.  The normal
                    // path throws here; returning a freshly allowed row only
                    // covers a concurrent scope change between list and open.
                    .orElseGet(() -> accessService.requireAccess(actor, requestedManagementViewId));
        }
        return accessibleViews.stream()
                .min(Comparator
                        .comparing((ManagementView view) -> view.type() != ManagementViewType.DASHBOARD)
                        .thenComparingLong(ManagementView::id))
                .orElse(null);
    }

    private ScopeCatalogue loadScopeCatalogue(AccessContext actor) {
        try {
            List<ManagementViewScope> scopes = new ArrayList<>();
            List<ScopeTargetOption> targets = new ArrayList<>();
            AccessProfileType profile = actor.profileType();

            if (canConfigure(actor, ManagementViewScope.GLOBAL, null)) {
                scopes.add(ManagementViewScope.GLOBAL);
            }

            switch (profile) {
                case ADMINISTRATOR -> appendAdministratorScopes(actor, scopes, targets);
                case COORDINATOR -> appendCoordinatorScopes(actor, scopes, targets);
                case TEACHER -> appendTeacherScopes(actor, scopes, targets);
                case STUDENT -> {
                    if (canConfigure(actor, ManagementViewScope.PERSONAL, actor.userId())) {
                        scopes.add(ManagementViewScope.PERSONAL);
                    }
                }
            }

            return new ScopeCatalogue(List.copyOf(scopes), List.copyOf(targets));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load available management-view scopes", exception);
        }
    }

    private void appendAdministratorScopes(
            AccessContext actor,
            List<ManagementViewScope> scopes,
            List<ScopeTargetOption> targets
    ) throws SQLException {
        List<Organization> organizations = applicationReadService.organizations().findByAdministrator(actor.userId());
        List<Organization> allowedOrganizations = organizations.stream()
                .filter(organization -> canConfigure(actor, ManagementViewScope.ORGANIZATION, organization.id()))
                .toList();
        if (!allowedOrganizations.isEmpty()) {
            scopes.add(ManagementViewScope.ORGANIZATION);
            for (Organization organization : allowedOrganizations) {
                targets.add(new ScopeTargetOption(
                        ManagementViewScope.ORGANIZATION,
                        organization.id(),
                        label(organization.acronym(), organization.name())
                ));
            }
        }

        Set<Long> courseIds = new LinkedHashSet<>();
        for (Organization organization : allowedOrganizations) {
            for (Course course : applicationReadService.courses().findByOrganization(organization.id())) {
                if (canConfigure(actor, ManagementViewScope.COURSE, course.id())) {
                    courseIds.add(course.id());
                    targets.add(new ScopeTargetOption(
                            ManagementViewScope.COURSE,
                            course.id(),
                            label(course.acronym(), course.name())
                    ));
                }
            }
        }
        if (!courseIds.isEmpty()) {
            scopes.add(ManagementViewScope.COURSE);
        }
    }

    private void appendCoordinatorScopes(
            AccessContext actor,
            List<ManagementViewScope> scopes,
            List<ScopeTargetOption> targets
    ) throws SQLException {
        Set<Long> subjectIds = applicationReadService.coordinateSubjects()
                .findActiveSubjectIdsByCoordinator(actor.userId());
        boolean hasTargets = false;
        for (Long subjectId : subjectIds) {
            Subject subject = applicationReadService.subjects().findById(subjectId).orElse(null);
            if (subject != null && canConfigure(actor, ManagementViewScope.SUBJECT, subject.id())) {
                hasTargets = true;
                targets.add(new ScopeTargetOption(
                        ManagementViewScope.SUBJECT,
                        subject.id(),
                        label(subject.acronym(), subject.name())
                ));
            }
        }
        if (hasTargets) {
            scopes.add(ManagementViewScope.SUBJECT);
        }
    }

    private void appendTeacherScopes(
            AccessContext actor,
            List<ManagementViewScope> scopes,
            List<ScopeTargetOption> targets
    ) throws SQLException {
        Set<Long> classGroupIds = applicationReadService.teachClassGroups()
                .findActiveClassGroupIdsByTeacher(actor.userId());
        boolean hasTargets = false;
        for (Long classGroupId : classGroupIds) {
            ClassGroup classGroup = applicationReadService.classGroups().findById(classGroupId).orElse(null);
            if (classGroup != null && canConfigure(actor, ManagementViewScope.CLASS_GROUP, classGroup.id())) {
                hasTargets = true;
                targets.add(new ScopeTargetOption(
                        ManagementViewScope.CLASS_GROUP,
                        classGroup.id(),
                        classGroup.code()
                ));
            }
        }
        if (hasTargets) {
            scopes.add(ManagementViewScope.CLASS_GROUP);
        }
    }

    private boolean canConfigure(AccessContext actor, ManagementViewScope scope, Long contextId) {
        return accessService.canConfigure(
                actor.userId(),
                actor.profileType(),
                scope,
                contextId,
                actor.userId()
        );
    }

    private static String label(String primary, String fallback) {
        if (primary == null || primary.isBlank()) {
            return fallback;
        }
        if (fallback == null || fallback.isBlank() || primary.equalsIgnoreCase(fallback)) {
            return primary;
        }
        return primary + " - " + fallback;
    }

    /** Service-level projection; controller maps it to JSP-specific view data. */
    public record Snapshot(
            List<ManagementView> accessibleViews,
            ManagementView selectedView,
            ReportAggregation selectedAggregation,
            List<ManagementView> configurableViews,
            List<Long> selectedRecipientUserIds,
            List<ManagementViewScope> scopeChoices,
            List<ScopeTargetOption> scopeTargetOptions
    ) {
        public Snapshot {
            accessibleViews = List.copyOf(accessibleViews);
            configurableViews = List.copyOf(configurableViews);
            selectedRecipientUserIds = List.copyOf(selectedRecipientUserIds);
            scopeChoices = List.copyOf(scopeChoices);
            scopeTargetOptions = List.copyOf(scopeTargetOptions);
        }

        public boolean canConfigure() {
            return !scopeChoices.isEmpty();
        }
    }

    private record ScopeCatalogue(
            List<ManagementViewScope> scopeChoices,
            List<ScopeTargetOption> scopeTargetOptions
    ) {
    }

    /** Scope target authorised for a configuration chooser. */
    public record ScopeTargetOption(ManagementViewScope scope, long targetId, String label) {
        public ScopeTargetOption {
            Objects.requireNonNull(scope, "scope is required");
            if (targetId <= 0) {
                throw new IllegalArgumentException("targetId must be positive");
            }
            Objects.requireNonNull(label, "label is required");
        }
    }
}
