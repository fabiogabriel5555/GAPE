package pt.isel.gape.web.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ManagementViewsDashboardTemplateTest {

    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Path JAVA = Path.of("src/main/java");

    @Test
    void dashboardUsesTheEduAllAdminShellForScopedPanelsAndReports() throws IOException {
        String jsp = Files.readString(WEBAPP.resolve("WEB-INF/views/transversal/management-dashboard.jsp"));
        String script = Files.readString(WEBAPP.resolve("assets/js/gape-management-views-dashboard.js"));
        String stylesheet = Files.readString(WEBAPP.resolve("assets/css/main.css"));
        String servlet = Files.readString(JAVA.resolve("pt/isel/gape/web/controller/DashboardServlet.java"));
        String queryService = Files.readString(JAVA.resolve(
                "pt/isel/gape/transversal/service/ManagementDashboardQueryService.java"
        ));
        String managementViewService = Files.readString(JAVA.resolve(
                "pt/isel/gape/transversal/service/ManagementViewService.java"
        ));
        String managementViewDao = Files.readString(JAVA.resolve(
                "pt/isel/gape/transversal/dao/ManagementViewDAO.java"
        ));
        String policy = Files.readString(JAVA.resolve("pt/isel/gape/security/authorization/AuthorizationPolicy.java"));
        String activityLogServlet = Files.readString(JAVA.resolve(
                "pt/isel/gape/web/controller/AdminActivityLogServlet.java"
        ));
        String sidebar = Files.readString(WEBAPP.resolve("WEB-INF/fragments/dashboard-sidebar.jspf"));
        String studentSidebar = Files.readString(WEBAPP.resolve("WEB-INF/fragments/student-dashboard-sidebar.jspf"));
        String adminUsers = Files.readString(WEBAPP.resolve("admin/admin/user/admin-users.jsp"));
        String adminUserDetail = Files.readString(WEBAPP.resolve("admin/admin/user/admin-user-detail.jsp"));
        String legacyCreateMethod = "create" + "ManagementView";
        String legacyCreateCommand = "ManagementView" + "CreateCommand";

        assertTrue(jsp.contains("dashboard-sidebar.jspf")
                        && jsp.contains("dashboard-topbar.jspf")
                        && jsp.contains("dashboard-footer.jspf")
                        && jsp.contains("gape-management-dashboard-mode-grid")
                        && jsp.contains("gape-management-dashboard-mode-card")
                        && jsp.contains("data-management-tab-card")
                        && jsp.contains("px-20 py-20 bg-white rounded-10"),
                "Management views must retain the EduAll administrator dashboard shell and KPI cards");
        assertTrue(jsp.contains("management-overview-chart")
                        && jsp.contains("management-distribution-chart")
                        && jsp.contains("Indicator summary")
                        && jsp.contains("Available dashboards and reports")
                        && jsp.contains("<option value=\"report\">Reports</option>")
                        && jsp.contains("managementDashboardUrl"),
                "The unified dashboard must expose charts, indicator cards and both dashboards and reports in one catalogue");
        assertFalse(jsp.contains("managementReportMode")
                        || jsp.contains("managementSection")
                        || jsp.contains("/reports/")
                        || jsp.contains("Create" + " panel"),
                "Reports must not retain a separate page mode and panels cannot be created from the dashboard");
        assertTrue(jsp.contains("name=\"csrfToken\"")
                        && jsp.contains("name=\"visibilityScope\"")
                        && jsp.contains("recipientUserIds")
                        && jsp.contains("Scope rules are revalidated by the server"),
                "Panel and recipient configuration must use protected server-side forms");
        assertFalse(jsp.contains("new ManagementViewDAO") || jsp.contains("java.sql."),
                "JSP must not instantiate persistence code");
        assertTrue(script.contains("ApexCharts")
                        && script.contains("data-management-catalogue-search")
                        && script.contains("syncScopeTarget")
                        && script.contains("data-management-edit")
                        && script.contains("candidate.value && !candidate.hidden"),
                "The client behavior must render real aggregate charts and responsive panel controls");
        assertFalse(script.contains("data-management-create") || script.contains("Create" + " panel"),
                "The client behavior must not expose a create-panel flow");
        assertTrue(script.contains("window.fetch")
                        && script.contains("replaceDashboardContent")
                        && script.contains("data-management-async-form")
                        && script.contains("popstate")
                        && script.contains("markManagementTabLoading")
                        && script.contains("window.location.assign(payload.url || requestUrl)"),
                "Dashboard selection and configuration must update asynchronously without replacing the shell");
        assertFalse(script.contains("The dashboard response was not valid."),
                "An unexpected dashboard payload must fall back to a safe navigation instead of a fatal user-facing error");
        assertTrue(jsp.contains("dashboardTab == 'logs'")
                        && jsp.contains("managementCanViewLogs")
                        && jsp.contains("data-management-tab")
                        && jsp.contains("data-management-audit-form")
                        && jsp.contains("name=\"operationType\"")
                        && jsp.contains("name=\"entityType\"")
                        && jsp.contains("name=\"occurredFrom\"")
                        && jsp.contains("name=\"occurredUntil\"")
                        && jsp.contains("name=\"userId\""),
                "Dashboard must keep Logs as an in-place tab with its authorized filters");
        assertFalse(jsp.contains("class=\"gape-management-dashboard-tab")
                        || jsp.contains("gape-management-card bg-white rounded-10 px-20 py-16 mb-24"),
                "Dashboard and Logs must use the two card tabs instead of the obsolete combined card");
        assertTrue(script.contains("bindDashboardTabs")
                        && script.contains("bindAuditFilters")
                        && script.contains("auditUrlForForm")
                        && script.contains("data-management-audit-filter"),
                "Dashboard Logs filters must refresh the current dashboard asynchronously");
        assertTrue(stylesheet.contains("#managementViewModal .modal-dialog")
                        && stylesheet.contains("height: calc(100dvh - 16px)")
                        && stylesheet.contains("#managementViewModal .modal-content > form"),
                "Management-view modal actions must remain available in short mobile viewports");
        assertTrue(servlet.contains("@WebServlet(name = \"dashboardServlet\", urlPatterns = \"/dashboard\")")
                        && servlet.contains("ManagementDashboardQueryService")
                        && servlet.contains("ManagementViewService")
                        && servlet.contains("ReportAggregationService")
                        && servlet.contains("isValidCsrfToken")
                        && servlet.contains("managementCanViewLogs")
                        && servlet.contains("profile == AccessProfileType.ADMINISTRATOR"),
                "Dashboard routes must depend on Services and protect configuration posts with CSRF");
        assertFalse(servlet.contains("/reports")
                        || servlet.contains(legacyCreateMethod)
                        || servlet.contains(legacyCreateCommand)
                        || servlet.contains("\"create\".equals(operation)"),
                "The servlet must expose only the unified Dashboard and reject panel creation");
        assertFalse(managementViewService.contains(legacyCreateMethod)
                        || managementViewDao.contains("public long create(")
                        || Files.exists(JAVA.resolve("pt/isel/gape/transversal/model/" + legacyCreateCommand + ".java")),
                "Panels must be provisioned by project data, not created through the application layers");
        assertFalse(servlet.contains("new ManagementViewDAO") || servlet.contains("getConnection()"),
                "Dashboard servlet must not bypass the Service layer");
        assertTrue(servlet.contains("ActivityLogService")
                        && servlet.contains("ActivityLogQuery")
                        && servlet.contains("listForUserAudit")
                        && servlet.contains("LocalDate")
                        && servlet.contains("atStartOfDay")
                        && servlet.contains("LocalTime.MAX")
                        && servlet.contains("userLocked"),
                "Dashboard Logs must use the scope-aware activity service, date-day bounds and a locked student filter");
        assertTrue(queryService.contains("selectView(actor, accessibleViews, requestedManagementViewId)"),
                "Dashboard selection must open any authorized panel or report from the same catalogue");
        assertFalse(queryService.contains("ManagementViewType preferredType"),
                "The unified dashboard must not keep a separate reports catalogue mode");
        assertFalse(queryService.contains("pt.isel.gape.web."),
                "The dashboard query service must stay inside the service/model boundary");
        assertFalse(policy.contains("isPathOrChild(path, \"/reports\")"),
                "The removed Reports page must not remain in the authorization policy");
        assertFalse(sidebar.contains("reportsHref")
                        || sidebar.contains("/reports")
                        || studentSidebar.contains("data-menu-key=\"reports\"")
                        || studentSidebar.contains("/dashboard?section=reports"),
                "Both sidebars must point users only to the unified Dashboard");
        assertTrue(activityLogServlet.contains("/dashboard?tab=logs"),
                "Legacy activity-log bookmarks must redirect to the Logs tab of the Dashboard");
        assertFalse(activityLogServlet.contains("admin-audit.jsp")
                        || activityLogServlet.contains("ActivityLogService")
                        || activityLogServlet.contains("forward("),
                "The compatibility endpoint must not render or query an autonomous audit page");
        assertFalse(Files.exists(WEBAPP.resolve("admin/admin/user/admin-audit.jsp")),
                "The old standalone audit JSP must not remain deployable");
        assertFalse(jsp.contains("/admin/activity-log")
                        || sidebar.contains("/admin/activity-log")
                        || adminUsers.contains("/admin/activity-log")
                        || adminUserDetail.contains("/admin/activity-log"),
                "No Dashboard-facing surface may retain the old autonomous audit endpoint");
    }
}
