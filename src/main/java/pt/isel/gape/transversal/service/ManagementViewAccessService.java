package pt.isel.gape.transversal.service;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pt.isel.gape.access.dao.PermissionDAO;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.learning.dao.CourseDAO;
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.security.authorization.AccessEntityType;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.structure.dao.CoordinateSubjectDAO;
import pt.isel.gape.structure.dao.ManageOrganizationDAO;
import pt.isel.gape.structure.dao.TeachClassGroupDAO;
import pt.isel.gape.transversal.dao.ActivityLogDAO;
import pt.isel.gape.transversal.dao.ManagementViewContextAccessDAO;
import pt.isel.gape.transversal.dao.ManagementViewDAO;
import pt.isel.gape.transversal.model.ManagementView;
import pt.isel.gape.transversal.model.ManagementViewScope;
import pt.isel.gape.transversal.model.ManagementViewState;
import pt.isel.gape.transversal.model.ManagementViewType;

/**
 * Server-side authorization boundary for management views and contextual reports.
 *
 * <p>Visibility is calculated from the current profile and domain assignment;
 * the optional {@code access_management_view} relation only controls
 * distribution/cataloguing and is never a privilege escalation path. A caller
 * with an explicit row still has to satisfy the same scope rule.</p>
 */
public final class ManagementViewAccessService implements ManagementViewConfigurationAuthorizer {

    public static final String ACCESS_OPERATION = "MANAGEMENT_VIEW_ACCESS";
    public static final String ENTITY_TYPE = "management_view";

    private final ManagementViewDAO managementViewDAO;
    private final PermissionDAO permissionDAO;
    private final ManageOrganizationDAO manageOrganizationDAO;
    private final CoordinateSubjectDAO coordinateSubjectDAO;
    private final TeachClassGroupDAO teachClassGroupDAO;
    private final CourseDAO courseDAO;
    private final ManagementViewContextAccessDAO contextAccessDAO;
    private final AuditService auditService;
    private final Clock clock;

    public ManagementViewAccessService(ConnectionProvider connectionProvider) {
        this(connectionProvider, ApplicationClock.system());
    }

    /** Test-friendly constructor whose effective date is derived from {@code clock}. */
    public ManagementViewAccessService(ConnectionProvider connectionProvider, Clock clock) {
        this(
                new ManagementViewDAO(connectionProvider),
                new PermissionDAO(connectionProvider),
                new ManageOrganizationDAO(connectionProvider),
                new CoordinateSubjectDAO(connectionProvider),
                new TeachClassGroupDAO(connectionProvider),
                new CourseDAO(connectionProvider),
                new ManagementViewContextAccessDAO(connectionProvider),
                new AuditService(new ActivityLogDAO(connectionProvider), clock),
                clock
        );
    }

    public ManagementViewAccessService(
            ManagementViewDAO managementViewDAO,
            PermissionDAO permissionDAO,
            ManageOrganizationDAO manageOrganizationDAO,
            CoordinateSubjectDAO coordinateSubjectDAO,
            TeachClassGroupDAO teachClassGroupDAO,
            CourseDAO courseDAO,
            ManagementViewContextAccessDAO contextAccessDAO,
            AuditService auditService,
            Clock clock
    ) {
        this.managementViewDAO = Objects.requireNonNull(managementViewDAO, "managementViewDAO is required");
        this.permissionDAO = Objects.requireNonNull(permissionDAO, "permissionDAO is required");
        this.manageOrganizationDAO = Objects.requireNonNull(manageOrganizationDAO, "manageOrganizationDAO is required");
        this.coordinateSubjectDAO = Objects.requireNonNull(coordinateSubjectDAO, "coordinateSubjectDAO is required");
        this.teachClassGroupDAO = Objects.requireNonNull(teachClassGroupDAO, "teachClassGroupDAO is required");
        this.courseDAO = Objects.requireNonNull(courseDAO, "courseDAO is required");
        this.contextAccessDAO = Objects.requireNonNull(contextAccessDAO, "contextAccessDAO is required");
        this.auditService = Objects.requireNonNull(auditService, "auditService is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    /**
     * Evaluates access without writing an audit record. This is appropriate for
     * collection filtering; actual opening of a view must use {@link #requireAccess}.
     */
    public boolean canAccess(AccessContext actor, long managementViewId) {
        Objects.requireNonNull(actor, "actor is required");
        if (managementViewId <= 0) {
            return false;
        }
        try {
            return managementViewDAO.findById(managementViewId)
                    .map(view -> canAccess(actor, view))
                    .orElse(false);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to evaluate management-view access", exception);
        }
    }

    /** Evaluates scope access without consulting explicit access grants. */
    public boolean canAccess(AccessContext actor, ManagementView view) {
        Objects.requireNonNull(actor, "actor is required");
        Objects.requireNonNull(view, "view is required");
        if (actor.userId() <= 0 || !isActiveAndWellFormed(view)) {
            return false;
        }
        try {
            if (!permissionDAO.activeProfileExists(actor.userId(), actor.profileType())) {
                return false;
            }
            return switch (actor.profileType()) {
                case ADMINISTRATOR -> canAdministratorAccess(actor.userId(), view);
                case COORDINATOR -> canCoordinatorAccess(actor.userId(), view);
                case TEACHER -> canTeacherAccess(actor.userId(), view);
                case STUDENT -> canStudentAccess(actor.userId(), view);
            };
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to evaluate management-view scope access", exception);
        }
    }

    /**
     * Loads and opens a view, auditing both a successful opening and a denied
     * authorization decision. Identifiers in the audit record are numeric IDs,
     * never titles, descriptions, names or emails.
     */
    public ManagementView requireAccess(AccessContext actor, long managementViewId) {
        Objects.requireNonNull(actor, "actor is required");
        if (managementViewId <= 0) {
            throw new IllegalArgumentException("managementViewId must be positive");
        }

        final ManagementView view;
        try {
            view = managementViewDAO.findById(managementViewId)
                    .orElseThrow(() -> new IllegalArgumentException("Management view not found: " + managementViewId));
        } catch (IllegalArgumentException exception) {
            audit(actor, managementViewId, "failure");
            throw exception;
        } catch (SQLException exception) {
            audit(actor, managementViewId, "failure");
            throw new IllegalStateException("Failed to load management view " + managementViewId, exception);
        }

        final boolean allowed;
        try {
            allowed = canAccess(actor, view);
        } catch (RuntimeException exception) {
            audit(actor, managementViewId, "failure");
            throw exception;
        }
        if (!allowed) {
            audit(actor, managementViewId, "denied");
            throw new SecurityException("User cannot access this management view");
        }

        audit(actor, managementViewId, "success");
        return view;
    }

    /** Lists only views whose active scope is currently authorized for the actor. */
    public List<ManagementView> listAccessible(AccessContext actor) {
        Objects.requireNonNull(actor, "actor is required");
        try {
            List<ManagementView> allowed = new ArrayList<>();
            for (ManagementView view : managementViewDAO.findAll()) {
                if (canAccess(actor, view)) {
                    allowed.add(view);
                }
            }
            return List.copyOf(allowed);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list accessible management views", exception);
        }
    }

    /**
     * Non-mutating configuration decision used by the management-view service.
     * A configuration request is never inferred from a supplied browser scope;
     * the target is checked against the actor's live domain assignment.
     */
    public boolean canConfigure(
            long actorUserId,
            AccessProfileType actorProfileType,
            ManagementViewScope scope,
            Long scopeContextId,
            Long ownerUserId
    ) {
        if (actorUserId <= 0 || actorProfileType == null || scope == null) {
            return false;
        }
        try {
            if (!permissionDAO.activeProfileExists(actorUserId, actorProfileType)) {
                return false;
            }
            return switch (scope) {
                case GLOBAL -> actorProfileType == AccessProfileType.ADMINISTRATOR
                        && scopeContextId == null
                        && hasGlobalManagementPermission(actorUserId);
                case ORGANIZATION -> actorProfileType == AccessProfileType.ADMINISTRATOR
                        && isPositive(scopeContextId)
                        && hasOrganizationManagementPermission(actorUserId, scopeContextId)
                        && manageOrganizationDAO.hasActiveAssignment(actorUserId, scopeContextId);
                case COURSE -> canConfigureCourse(actorUserId, actorProfileType, scopeContextId);
                case SUBJECT -> actorProfileType == AccessProfileType.COORDINATOR
                        && isPositive(scopeContextId)
                        && hasLearningManagementPermission(actorUserId, actorProfileType)
                        && coordinateSubjectDAO.hasActiveAssignment(actorUserId, scopeContextId);
                case CLASS_GROUP -> actorProfileType == AccessProfileType.TEACHER
                        && isPositive(scopeContextId)
                        && hasLearningManagementPermission(actorUserId, actorProfileType)
                        && teachClassGroupDAO.hasActiveAssignment(actorUserId, scopeContextId);
                case PERSONAL -> actorProfileType == AccessProfileType.STUDENT
                        && ownerUserId != null
                        && ownerUserId == actorUserId
                        && (scopeContextId == null || ownerUserId.equals(scopeContextId));
            };
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to evaluate management-view configuration access", exception);
        }
    }

    /**
     * Configuration security boundary injected into {@link ManagementViewService}.
     */
    @Override
    public void requireConfigurationAccess(
            long actorUserId,
            AccessProfileType actorProfileType,
            ManagementViewScope scope,
            Long scopeContextId,
            Long ownerUserId
    ) {
        if (!canConfigure(actorUserId, actorProfileType, scope, scopeContextId, ownerUserId)) {
            throw new SecurityException("User cannot configure a management view in this scope");
        }
    }

    private boolean canAdministratorAccess(long userId, ManagementView view) throws SQLException {
        return switch (view.visibilityScope()) {
            case GLOBAL -> hasGlobalManagementPermission(userId);
            case ORGANIZATION -> hasOrganizationManagementPermission(userId, view.scopeTargetId())
                    && manageOrganizationDAO.hasActiveAssignment(userId, view.scopeTargetId());
            case COURSE, SUBJECT, CLASS_GROUP, PERSONAL -> false;
        };
    }

    private boolean canCoordinatorAccess(long userId, ManagementView view) throws SQLException {
        return view.visibilityScope() == ManagementViewScope.SUBJECT
                && hasLearningManagementPermission(userId, AccessProfileType.COORDINATOR)
                && coordinateSubjectDAO.hasActiveAssignment(userId, view.scopeTargetId());
    }

    private boolean canTeacherAccess(long userId, ManagementView view) throws SQLException {
        return view.visibilityScope() == ManagementViewScope.CLASS_GROUP
                && hasLearningManagementPermission(userId, AccessProfileType.TEACHER)
                && teachClassGroupDAO.hasActiveAssignment(userId, view.scopeTargetId());
    }

    private boolean canStudentAccess(long userId, ManagementView view) throws SQLException {
        if (view.visibilityScope() == ManagementViewScope.PERSONAL) {
            return Long.valueOf(userId).equals(view.ownerUserId())
                    && Long.valueOf(userId).equals(view.scopeTargetId());
        }
        if (view.type() != ManagementViewType.REPORT) {
            return false;
        }
        LocalDate effectiveDate = LocalDate.now(clock);
        return switch (view.visibilityScope()) {
            case COURSE -> contextAccessDAO.hasCurrentStudentCourseAccess(userId, view.scopeTargetId(), effectiveDate);
            case SUBJECT -> contextAccessDAO.hasCurrentStudentSubjectAccess(userId, view.scopeTargetId(), effectiveDate);
            case CLASS_GROUP -> contextAccessDAO.hasCurrentStudentClassGroupAccess(userId, view.scopeTargetId(), effectiveDate);
            case GLOBAL, ORGANIZATION, PERSONAL -> false;
        };
    }

    private boolean canConfigureCourse(long actorUserId, AccessProfileType actorProfileType, Long courseId)
            throws SQLException {
        if (actorProfileType != AccessProfileType.ADMINISTRATOR
                || !isPositive(courseId)) {
            return false;
        }
        var course = courseDAO.findActiveById(courseId);
        return course.isPresent()
                && hasOrganizationManagementPermission(actorUserId, course.orElseThrow().organizationId())
                && manageOrganizationDAO.hasActiveAssignment(actorUserId, course.orElseThrow().organizationId());
    }

    private boolean hasGlobalManagementPermission(long actorUserId) throws SQLException {
        return permissionDAO.hasActiveGlobalAdministratorGrant(actorUserId, AuthorizationPolicy.MANAGE_ALL);
    }

    /**
     * Organization panels require a permission that is global or bound to the
     * exact organization.  A grant for a course, subject or unrelated
     * organization must not be widened merely because the administrator also
     * has a {@code manage_organization} assignment.
     */
    private boolean hasOrganizationManagementPermission(long actorUserId, long organizationId) throws SQLException {
        return permissionDAO.hasActiveGlobalAdministratorGrant(actorUserId, AuthorizationPolicy.MANAGE_ALL)
                || permissionDAO.hasExactAdministratorContextGrant(
                        actorUserId,
                        AuthorizationPolicy.MANAGE_ALL,
                        AccessEntityType.ORGANIZATION,
                        organizationId
                )
                || permissionDAO.hasExactAdministratorContextGrant(
                        actorUserId,
                        AuthorizationPolicy.MANAGE_ORGANIZATION_STRUCTURE,
                        AccessEntityType.ORGANIZATION,
                        organizationId
                );
    }

    private boolean hasLearningManagementPermission(long actorUserId, AccessProfileType actorProfileType)
            throws SQLException {
        return permissionDAO.hasActiveGrant(actorUserId, actorProfileType, AuthorizationPolicy.MANAGE_LEARNING);
    }

    private static boolean isActiveAndWellFormed(ManagementView view) {
        if (view.state() != ManagementViewState.ACTIVE || view.ownerUserId() == null || view.ownerUserId() <= 0) {
            return false;
        }
        ManagementViewScope scope = view.visibilityScope();
        if (scope == ManagementViewScope.GLOBAL) {
            return view.scopeTargetType() == null && view.scopeTargetId() == null;
        }
        if (view.scopeTargetType() != scope.targetType() || !isPositive(view.scopeTargetId())) {
            return false;
        }
        return scope != ManagementViewScope.PERSONAL
                || view.ownerUserId().equals(view.scopeTargetId());
    }

    private static boolean isPositive(Long value) {
        return value != null && value > 0;
    }

    private void audit(AccessContext actor, long managementViewId, String outcome) {
        auditService.record(
                actor.userId(),
                actor.sessionId(),
                ACCESS_OPERATION,
                ENTITY_TYPE,
                Long.toString(managementViewId),
                outcome,
                actor.sourceIp()
        );
    }

}
