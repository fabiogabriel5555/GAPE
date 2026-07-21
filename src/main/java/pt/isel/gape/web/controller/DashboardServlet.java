package pt.isel.gape.web.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.isel.gape.access.model.Session;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.service.SessionService;
import pt.isel.gape.common.config.ConnectionProvider;
import pt.isel.gape.common.time.ApplicationClock;
import pt.isel.gape.security.authorization.AccessContext;
import pt.isel.gape.security.authorization.AuthorizationPolicy;
import pt.isel.gape.security.session.SessionManager;
import pt.isel.gape.security.session.SessionUser;
import pt.isel.gape.transversal.model.ActivityLog;
import pt.isel.gape.transversal.model.ActivityLogQuery;
import pt.isel.gape.transversal.model.ManagementView;
import pt.isel.gape.transversal.model.ManagementViewScope;
import pt.isel.gape.transversal.model.ManagementViewState;
import pt.isel.gape.transversal.model.ManagementViewType;
import pt.isel.gape.transversal.model.ManagementViewUpdateCommand;
import pt.isel.gape.transversal.service.ActivityLogService;
import pt.isel.gape.transversal.service.ApplicationReadService;
import pt.isel.gape.transversal.service.ManagementDashboardQueryService;
import pt.isel.gape.transversal.service.ManagementViewAccessService;
import pt.isel.gape.transversal.service.ManagementViewService;
import pt.isel.gape.transversal.service.ReportAggregationService;
import pt.isel.gape.learning.service.StudentDashboardService;
import pt.isel.gape.web.view.ActivityLogView;
import pt.isel.gape.web.view.ManagementDashboardView;
import pt.isel.gape.web.view.ManagementViewScopeChoiceView;
import pt.isel.gape.web.view.ManagementViewScopeTargetOptionView;

/**
 * The authenticated, scope-aware dashboard surface for panels and reports.
 *
 * <p>All reads and mutations go through Services.  JSP receives a small,
 * presentation-safe projection and never sees a DAO or a JDBC connection.</p>
 */
@WebServlet(name = "dashboardServlet", urlPatterns = "/dashboard")
public final class DashboardServlet extends DashboardServletSupport {

    private static final String DASHBOARD_TAB = "dashboard";
    private static final String LOGS_TAB = "logs";
    private static final String STUDENT_DASHBOARD_JSP = "/student/student/dashboard/student-dashboard.jsp";
    private static final String ADMIN_DASHBOARD_JSP = "/admin/admin/dashboard/admin-dashboard.jsp";
    private static final String COORDINATOR_DASHBOARD_JSP = "/coordinator/coordinator/dashboard/coordinator-dashboard.jsp";
    private static final String INSTRUCTOR_DASHBOARD_JSP = "/instructor/instructor/dashboard/instructor-dashboard.jsp";

    private final SessionService sessionService;
    private final ManagementDashboardQueryService dashboardQueryService;
    private final ManagementViewService managementViewService;
    private final ActivityLogService activityLogService;
    private final StudentDashboardService studentDashboardService;

    public DashboardServlet() {
        this(
                new SessionService(ConnectionProvider.defaultProvider(), ApplicationClock.system()),
                new SessionManager(),
                new ManagementViewAccessService(ConnectionProvider.defaultProvider(), ApplicationClock.system()),
                new ManagementViewService(ConnectionProvider.defaultProvider(), ApplicationClock.system()),
                new ReportAggregationService(ConnectionProvider.defaultProvider(), ApplicationClock.system()),
                new ApplicationReadService(ConnectionProvider.defaultProvider()),
                new ActivityLogService(ConnectionProvider.defaultProvider())
        );
    }

    DashboardServlet(
            SessionService sessionService,
            SessionManager sessionManager,
            ManagementViewAccessService managementViewAccessService,
            ManagementViewService managementViewService,
            ReportAggregationService reportAggregationService,
            ApplicationReadService applicationReadService
    ) {
        this(
                sessionService,
                sessionManager,
                managementViewAccessService,
                managementViewService,
                reportAggregationService,
                applicationReadService,
                new ActivityLogService(ConnectionProvider.defaultProvider())
        );
    }

    DashboardServlet(
            SessionService sessionService,
            SessionManager sessionManager,
            ManagementViewAccessService managementViewAccessService,
            ManagementViewService managementViewService,
            ReportAggregationService reportAggregationService,
            ApplicationReadService applicationReadService,
            ActivityLogService activityLogService
    ) {
        super(sessionManager);
        this.sessionService = sessionService;
        this.managementViewService = managementViewService;
        this.activityLogService = activityLogService;
        this.studentDashboardService = new StudentDashboardService(
                ConnectionProvider.defaultProvider(), ApplicationClock.system()
        );
        this.dashboardQueryService = new ManagementDashboardQueryService(
                managementViewAccessService,
                managementViewService,
                reportAggregationService,
                applicationReadService
        );
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        CurrentSession currentSession = requireCurrentSession(request, response);
        if (currentSession == null) {
            return;
        }

        String dashboardTab = dashboardTab(request);
        Long managementViewId;
        try {
            managementViewId = DASHBOARD_TAB.equals(dashboardTab)
                    ? optionalPositiveLongParameter(request, "viewId")
                    : null;
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            renderDashboard(request, response, currentSession, managementViewId, dashboardTab);
        } catch (SecurityException exception) {
            // The underlying report access service has already audited the
            // denied opening.  Do not disclose whether the id exists.
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (IllegalStateException exception) {
            throw new ServletException("Unable to load the management dashboard", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"/dashboard".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        CurrentSession currentSession = requireCurrentSession(request, response);
        if (currentSession == null) {
            return;
        }

        if (!sessionManager.isValidCsrfToken(request, request.getParameter("csrfToken"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        AccessContext actor = accessContext(currentSession, request);
        Long redirectViewId = null;
        try {
            redirectViewId = optionalPositiveLongParameter(request, "managementViewId");
            String operation = text(request, "operation");
            if ("update".equals(operation)) {
                long managementViewId = requiredManagementViewId(redirectViewId);
                var updated = managementViewService.updateManagementView(actor, managementViewId, updateCommand(request));
                flashSuccess(request, "Panel configuration updated.");
                redirectToView(request, response, updated.id());
                return;
            }
            if ("recipients".equals(operation)) {
                long managementViewId = requiredManagementViewId(redirectViewId);
                managementViewService.configureExplicitAccess(
                        actor,
                        managementViewId,
                        recipientUserIds(request.getParameter("recipientUserIds"))
                );
                flashSuccess(request, "Panel recipients updated. Scope rules still apply to every recipient.");
                redirectToView(request, response, managementViewId);
                return;
            }
            throw new IllegalArgumentException("Choose a valid panel operation.");
        } catch (SecurityException exception) {
            flashError(request, "You no longer have permission to configure this panel.");
        } catch (IllegalArgumentException exception) {
            flashError(request, safeConfigurationMessage(exception));
        } catch (IllegalStateException exception) {
            flashError(request, "The panel could not be saved. Please try again.");
        }
        redirectToView(request, response, redirectViewId);
    }

    private void renderDashboard(
            HttpServletRequest request,
            HttpServletResponse response,
            CurrentSession currentSession,
            Long managementViewId,
            String dashboardTab
    ) throws IOException, ServletException {
        AccessProfileType profile = currentSession.user().primaryProfileType().orElseThrow();
        prepareDashboard(request, "dashboard", "Dashboard");
        boolean managementCanViewLogs = profile == AccessProfileType.ADMINISTRATOR;
        // Logs are an administrator-only surface.  Do not rely on the JSP
        // condition alone: an old bookmark or a manually edited query string
        // must never make the coordinator/teacher dashboard enter the logs
        // branch.
        if (LOGS_TAB.equals(dashboardTab) && !managementCanViewLogs) {
            dashboardTab = DASHBOARD_TAB;
        }
        request.setAttribute("dashboardTab", dashboardTab);
        if (DASHBOARD_TAB.equals(dashboardTab) && isStudentOnly(currentSession.user())) {
            request.setAttribute("studentDashboard", studentDashboardService.load(currentSession.user().userId()));
            request.setAttribute("studentPageTitle", "Dashboard");
            request.setAttribute(
                    "studentPageDescription",
                    "A student-first view for courses, class groups, messages and upcoming lessons."
            );
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            forward(request, response, STUDENT_DASHBOARD_JSP);
            return;
        }
        request.setAttribute("managementProfileLabel", profileLabel(profile));
        request.setAttribute("managementCanViewLogs", managementCanViewLogs);
        request.setAttribute("managementDashboardCsrfToken", sessionManager.ensureCsrfToken(request));
        if (LOGS_TAB.equals(dashboardTab)) {
            prepareAuditLogs(request, currentSession, profile);
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            forward(request, response, managementDashboardJsp(profile));
            return;
        }

        AccessContext actor = accessContext(currentSession, request);
        ManagementDashboardQueryService.Snapshot snapshot = dashboardQueryService.load(actor, managementViewId);
        Set<Long> configurableIds = snapshot.configurableViews().stream()
                .map(ManagementView::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        ManagementView selected = snapshot.selectedView();
        List<ManagementDashboardView> views = snapshot.accessibleViews().stream()
                .map(view -> ManagementDashboardView.from(
                        view,
                        selected != null && selected.id() == view.id() ? snapshot.selectedAggregation() : null,
                        configurableIds.contains(view.id())
                ))
                .toList();
        ManagementDashboardView selectedView = selected == null
                ? null
                : ManagementDashboardView.from(
                        selected,
                        snapshot.selectedAggregation(),
                        configurableIds.contains(selected.id())
                );
        List<ManagementDashboardView> configurableViews = snapshot.configurableViews().stream()
                .map(view -> ManagementDashboardView.from(view, null, true))
                .toList();
        List<ManagementViewScopeChoiceView> scopeChoices = snapshot.scopeChoices().stream()
                .map(ManagementViewScopeChoiceView::new)
                .toList();
        List<ManagementViewScopeTargetOptionView> scopeTargetOptions = snapshot.scopeTargetOptions().stream()
                .map(option -> new ManagementViewScopeTargetOptionView(
                        option.scope(), option.targetId(), option.label()
                ))
                .toList();

        request.setAttribute("managementViews", views);
        request.setAttribute("selectedManagementView", selectedView);
        request.setAttribute("managementConfigurableViews", configurableViews);
        request.setAttribute("managementSelectedRecipientIds", snapshot.selectedRecipientUserIds());
        request.setAttribute("managementSelectedRecipientCsv", snapshot.selectedRecipientUserIds().stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(", ")));
        request.setAttribute("managementScopeChoices", scopeChoices);
        request.setAttribute("managementScopeTargetOptions", scopeTargetOptions);
        request.setAttribute("managementCanConfigure", snapshot.canConfigure());
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        forward(request, response, managementDashboardJsp(profile));
    }

    private static String managementDashboardJsp(AccessProfileType profile) {
        return switch (profile) {
            case COORDINATOR -> COORDINATOR_DASHBOARD_JSP;
            case TEACHER -> INSTRUCTOR_DASHBOARD_JSP;
            default -> ADMIN_DASHBOARD_JSP;
        };
    }

    /**
     * Prepares the audit tab without exposing persistence details to the JSP.
     * A requested user is treated as a target/involvement narrowing operation,
     * matching the former user-audit action while preserving the service's
     * live scope checks. Students are always locked to their own history.
     */
    private void prepareAuditLogs(
            HttpServletRequest request,
            CurrentSession currentSession,
            AccessProfileType profile
    ) {
        AuditLogFilters filters = auditLogFilters(request, currentSession.user().userId(), profile);
        List<ActivityLog> logs = List.of();
        if (filters.error() == null) {
            if (filters.userId() == null) {
                logs = activityLogService.queryForActor(
                        currentSession.user().userId(),
                        profile,
                        filters.toActivityLogQuery()
                );
            } else {
                logs = activityLogService.listForUserAudit(
                        currentSession.user().userId(),
                        profile,
                        filters.userId()
                ).stream().filter(filters::matches).toList();
            }
        }

        request.setAttribute("auditLogs", logs.stream().map(ActivityLogView::from).toList());
        request.setAttribute("auditLogCount", logs.size());
        request.setAttribute("auditUserId", filters.userId());
        request.setAttribute("auditOperationType", filters.operationType());
        request.setAttribute("auditEntityType", filters.affectedEntityType());
        request.setAttribute("auditOutcome", filters.outcome());
        request.setAttribute("auditOccurredFrom", filters.occurredFromDate());
        request.setAttribute("auditOccurredUntil", filters.occurredUntilDate());
        request.setAttribute("auditUserLocked", filters.userLocked());
        request.setAttribute("auditFilterError", filters.error());
    }

    private CurrentSession requireCurrentSession(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Optional<SessionUser> sessionUser = sessionManager.getSessionUser(request);
        OptionalLong sessionId = sessionManager.getDatabaseSessionId(request);
        Optional<String> sessionToken = sessionManager.getDatabaseSessionToken(request);
        if (sessionUser.isEmpty() || sessionId.isEmpty() || sessionToken.isEmpty()) {
            redirectToLogin(request, response, "required");
            return null;
        }

        Optional<Session> persistedSession = sessionService.findById(sessionId.getAsLong());
        if (persistedSession.isEmpty()) {
            sessionManager.clearSession(request);
            redirectToLogin(request, response, "missing");
            return null;
        }

        Session session = persistedSession.get();
        if (!sessionService.matchesToken(session, sessionToken.get()) || session.userId() != sessionUser.get().userId()) {
            sessionManager.clearSession(request);
            redirectToLogin(request, response, "invalid");
            return null;
        }
        if (sessionService.isExpired(session)) {
            sessionService.expire(session, request.getRemoteAddr());
            sessionManager.clearSession(request);
            redirectToLogin(request, response, "expired");
            return null;
        }
        return new CurrentSession(sessionUser.get(), sessionId.getAsLong());
    }

    private static AccessContext accessContext(CurrentSession currentSession, HttpServletRequest request) {
        AccessProfileType profile = currentSession.user().primaryProfileType()
                .orElseThrow(() -> new SecurityException("Authenticated user has no profile"));
        return AccessContext.global(
                currentSession.user().userId(),
                currentSession.sessionId(),
                profile,
                AuthorizationPolicy.VIEW_REPORTS,
                request.getRemoteAddr()
        );
    }

    private static String dashboardTab(HttpServletRequest request) {
        return LOGS_TAB.equals(text(request, "tab")) ? LOGS_TAB : DASHBOARD_TAB;
    }

    private static boolean isStudentOnly(SessionUser user) {
        return user.profileTypes().size() == 1
                && user.profileTypes().contains(AccessProfileType.STUDENT);
    }

    private static AuditLogFilters auditLogFilters(
            HttpServletRequest request,
            long actorUserId,
            AccessProfileType profile
    ) {
        String operationType = text(request, "operationType");
        String affectedEntityType = text(request, "entityType");
        String outcome = text(request, "outcome");
        String occurredFromDate = text(request, "occurredFrom");
        String occurredUntilDate = text(request, "occurredUntil");
        boolean userLocked = profile == AccessProfileType.STUDENT;
        try {
            Long userId = userLocked
                    ? Long.valueOf(actorUserId)
                    : optionalPositiveLongParameter(request, "userId");
            LocalDate fromDate = parseDate(occurredFromDate, "Start date");
            LocalDate untilDate = parseDate(occurredUntilDate, "End date");
            if (fromDate != null && untilDate != null && untilDate.isBefore(fromDate)) {
                throw new IllegalArgumentException("End date cannot be before start date.");
            }
            return new AuditLogFilters(
                    userId,
                    operationType,
                    affectedEntityType,
                    outcome,
                    occurredFromDate,
                    occurredUntilDate,
                    fromDate == null ? null : fromDate.atStartOfDay(),
                    untilDate == null ? null : untilDate.atTime(LocalTime.MAX),
                    userLocked,
                    null
            );
        } catch (IllegalArgumentException exception) {
            return new AuditLogFilters(
                    userLocked ? actorUserId : null,
                    operationType,
                    affectedEntityType,
                    outcome,
                    occurredFromDate,
                    occurredUntilDate,
                    null,
                    null,
                    userLocked,
                    auditFilterMessage(exception)
            );
        }
    }

    private static LocalDate parseDate(String value, String label) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(label + " must use a valid date.");
        }
    }

    private static String auditFilterMessage(IllegalArgumentException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "The log filters are not valid.";
        }
        return message;
    }

    private static Long optionalPositiveLongParameter(HttpServletRequest request, String parameterName) {
        String raw = text(request, parameterName);
        return raw == null ? null : positiveLong(raw, parameterName + " must be positive");
    }

    private static long positiveLong(String raw, String message) {
        try {
            long value = Long.parseLong(raw);
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(message);
        }
    }

    private static long requiredManagementViewId(Long managementViewId) {
        if (managementViewId == null || managementViewId <= 0) {
            throw new IllegalArgumentException("Choose the panel to configure.");
        }
        return managementViewId;
    }

    private static ManagementViewUpdateCommand updateCommand(HttpServletRequest request) {
        return new ManagementViewUpdateCommand(
                requiredText(request, "title", "A panel title is required."),
                ManagementViewType.fromDatabaseValue(requiredText(request, "type", "Choose a panel type.")),
                optionalText(request, "description"),
                ManagementViewScope.fromDatabaseValue(requiredText(request, "visibilityScope", "Choose a visibility scope.")),
                optionalPositiveLongParameter(request, "scopeContextId"),
                ManagementViewState.fromDatabaseValue(requiredText(request, "state", "Choose a panel state."))
        );
    }

    private static List<Long> recipientUserIds(String rawRecipientIds) {
        if (rawRecipientIds == null || rawRecipientIds.isBlank()) {
            return List.of();
        }
        LinkedHashSet<Long> recipientIds = new LinkedHashSet<>();
        for (String token : rawRecipientIds.trim().split("[\\s,;]+")) {
            recipientIds.add(positiveLong(token, "Each recipient id must be a positive number."));
        }
        return List.copyOf(new ArrayList<>(recipientIds));
    }

    private static String requiredText(HttpServletRequest request, String parameterName, String message) {
        String value = text(request, parameterName);
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String optionalText(HttpServletRequest request, String parameterName) {
        String value = request.getParameter(parameterName);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String safeConfigurationMessage(IllegalArgumentException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "The panel configuration is not valid.";
        }
        return switch (message) {
            case "Management view scope target does not exist: GLOBAL 0" -> "Choose a valid scope target.";
            default -> message;
        };
    }

    private static String profileLabel(AccessProfileType profile) {
        return switch (profile) {
            case ADMINISTRATOR -> "Administrator";
            case COORDINATOR -> "Coordinator";
            case TEACHER -> "Teacher";
            case STUDENT -> "Student";
        };
    }

    private static void redirectToLogin(HttpServletRequest request, HttpServletResponse response, String reason)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/login.jsp?auth=" + reason);
    }

    private static void redirectToView(
            HttpServletRequest request,
            HttpServletResponse response,
            Long managementViewId
    ) throws IOException {
        String suffix = managementViewId == null || managementViewId <= 0
                ? ""
                : "?viewId=" + managementViewId;
        response.sendRedirect(request.getContextPath() + "/dashboard" + suffix);
    }

    private record CurrentSession(SessionUser user, long sessionId) {
    }

    private record AuditLogFilters(
            Long userId,
            String operationType,
            String affectedEntityType,
            String outcome,
            String occurredFromDate,
            String occurredUntilDate,
            LocalDateTime occurredFrom,
            LocalDateTime occurredUntil,
            boolean userLocked,
            String error
    ) {
        ActivityLogQuery toActivityLogQuery() {
            return new ActivityLogQuery(
                    null,
                    operationType,
                    affectedEntityType,
                    outcome,
                    occurredFrom,
                    occurredUntil
            );
        }

        boolean matches(ActivityLog log) {
            return (operationType == null || Objects.equals(operationType, log.operationType()))
                    && (affectedEntityType == null || Objects.equals(affectedEntityType, log.affectedEntityType()))
                    && (outcome == null || Objects.equals(outcome, log.outcome()))
                    && (occurredFrom == null || !log.occurredAt().isBefore(occurredFrom))
                    && (occurredUntil == null || !log.occurredAt().isAfter(occurredUntil));
        }
    }
}
