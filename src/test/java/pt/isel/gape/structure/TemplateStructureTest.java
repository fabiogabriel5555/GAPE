package pt.isel.gape.structure;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class TemplateStructureTest {

    private static final Path WEBAPP_DIR = Path.of("src/main/webapp");
    private static final Path ASSETS_DIR = WEBAPP_DIR.resolve("assets");
    private static final Path FRAGMENTS_DIR = WEBAPP_DIR.resolve("WEB-INF/fragments");
    private static final Path ANALYSIS_DOC = Path.of("docs/docs/analysis/eduall-template-analysis.md");
    private static final Pattern ACTIVE_SIDEBAR_ITEM_PATTERN = Pattern.compile(
            "<li class=\"mb-8 activePage\">\\s*<a href=\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE
    );

    private static final List<String> EXPECTED_FRAGMENT_FILES = List.of(
            "template-base-head.jspf",
            "template-base-scripts.jspf"
    );

    private static final List<String> EXPECTED_BASE_PAGES = List.of(
            "index.jsp",
            "login.jsp",
            "sign-in.jsp",
            "sign-up.jsp",
            "admin/admin-dashbord.jsp",
            "admin/admin-courses.jsp",
            "admin/admin/admin-users.jsp",
            "admin/admin/admin-user-form.jsp",
            "admin/admin/admin-user-detail.jsp",
            "admin/admin/admin-deletion-requests.jsp",
            "admin/admin/admin-audit.jsp",
            "admin/admin-message.jsp",
            "admin/admin-my-profile.jsp",
            "admin/admin-quiz-attempts.jsp",
            "admin/admin-reviews.jsp",
            "coordinator/coordinator-home.jsp",
            "coordinator/coordinator-message.jsp",
            "coordinator/coordinator-my-profile.jsp",
            "coordinator/coordinator-account-settings.jsp",
            "courses.jsp",
            "course.jsp",
            "course-details.jsp",
            "content.jsp",
            "lesson-details.jsp",
            "messages.jsp",
            "forms.jsp",
            "tables.jsp",
            "profile.jsp",
            "contact.jsp",
            "about-four.jsp",
            "instructor/instructor.jsp",
            "instructor/instructor-details.jsp",
            "tutor.jsp",
            "tutor-details.jsp",
            "events.jsp",
            "event-details.jsp",
            "apply-admission.jsp",
            "error-404.jsp",
            "error-500.jsp"
    );

    @Test
    void assetsDirectoryExistsWithExpectedSubdirectories() {
        assertTrue(Files.isDirectory(ASSETS_DIR), () -> "Expected assets directory: " + ASSETS_DIR);
        assertTrue(Files.isDirectory(ASSETS_DIR.resolve("css")), "Expected assets/css directory");
        assertTrue(Files.isDirectory(ASSETS_DIR.resolve("js")), "Expected assets/js directory");
        assertTrue(Files.isDirectory(ASSETS_DIR.resolve("images")), "Expected assets/images directory");
        assertTrue(Files.isDirectory(ASSETS_DIR.resolve("css/images")), "Expected assets/css/images directory");
    }

    @Test
    void reusableFragmentsExist() {
        assertTrue(Files.isDirectory(FRAGMENTS_DIR), () -> "Expected fragments directory: " + FRAGMENTS_DIR);

        for (String fragment : EXPECTED_FRAGMENT_FILES) {
            Path fragmentPath = FRAGMENTS_DIR.resolve(fragment);
            assertTrue(Files.isRegularFile(fragmentPath), () -> "Expected fragment file: " + fragmentPath);
        }
    }

    @Test
    void baseJspPagesExist() {
        for (String page : EXPECTED_BASE_PAGES) {
            Path pagePath = WEBAPP_DIR.resolve(page);
            assertTrue(Files.isRegularFile(pagePath), () -> "Expected JSP page: " + pagePath);
        }
    }

    @Test
    void coordinatorPagesMirrorInstructorPages() throws Exception {
        Path instructorDir = WEBAPP_DIR.resolve("instructor");
        Path coordinatorDir = WEBAPP_DIR.resolve("coordinator");

        try (Stream<Path> stream = Files.list(instructorDir)) {
            for (Path instructorPage : stream.filter(Files::isRegularFile).toList()) {
                String expectedCoordinatorName = instructorPage.getFileName().toString()
                        .replaceFirst("^instructor", "coordinator");
                Path coordinatorPage = coordinatorDir.resolve(expectedCoordinatorName);
                assertTrue(Files.isRegularFile(coordinatorPage), () -> "Expected coordinator mirror page: " + coordinatorPage);
            }
        }
    }

    @Test
    void dashboardPageFilenamesUseCanonicalNames() throws IOException {
        List<Path> profileDirectories = List.of(
                WEBAPP_DIR.resolve("admin"),
                WEBAPP_DIR.resolve("student"),
                WEBAPP_DIR.resolve("instructor"),
                WEBAPP_DIR.resolve("coordinator")
        );

        for (Path profileDirectory : profileDirectories) {
            try (Stream<Path> stream = Files.walk(profileDirectory)) {
                List<String> unexpectedNames = stream
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .filter(name -> name.endsWith(".jsp"))
                        .filter(name -> name.contains("dashboard")
                                || name.contains("ashboard")
                                || (name.contains("dashbord") && !"admin-dashbord.jsp".equals(name)))
                        .collect(Collectors.toList());

                assertTrue(unexpectedNames.isEmpty(),
                        () -> "Unexpected dashboard/dashbord JSP names in " + profileDirectory + ": " + unexpectedNames);
            }
        }

        try (Stream<Path> stream = Files.walk(WEBAPP_DIR.resolve("admin"))) {
            List<String> nonAdminNames = stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".jsp"))
                    .filter(name -> !name.startsWith("admin"))
                    .collect(Collectors.toList());

            assertTrue(nonAdminNames.isEmpty(), () -> "Admin JSP files must start with admin: " + nonAdminNames);
        }

        try (Stream<Path> stream = Files.list(WEBAPP_DIR.resolve("WEB-INF/views/access"))) {
            List<String> adminViewsOutsideAdmin = stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.equals("users-list.jsp")
                            || name.equals("user-form.jsp")
                            || name.equals("user-detail.jsp")
                            || name.equals("admin-deletion-requests.jsp")
                            || name.equals("activity-log.jsp"))
                    .collect(Collectors.toList());

            assertTrue(adminViewsOutsideAdmin.isEmpty(),
                    () -> "Admin views must live under webapp/admin: " + adminViewsOutsideAdmin);
        }
    }

    @Test
    void dashboardSidebarsMarkExactlyOneActiveItem() throws IOException {
        List<Path> dashboardDirectories = List.of(
                WEBAPP_DIR.resolve("admin"),
                WEBAPP_DIR.resolve("student"),
                WEBAPP_DIR.resolve("instructor"),
                WEBAPP_DIR.resolve("coordinator")
        );

        for (Path dashboardDirectory : dashboardDirectories) {
            try (Stream<Path> stream = Files.walk(dashboardDirectory)) {
                for (Path page : stream.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".jsp")).toList()) {
                    String pageName = page.getFileName().toString();
                    String html = Files.readString(page);

                    if (!hasDashboardSidebar(html) || pageName.endsWith("-alt-home.jsp")) {
                        continue;
                    }

                    if (usesCommonDashboardSidebar(html)) {
                        assertTrue(html.contains("request.setAttribute(\"activeMenu\""),
                                () -> "Expected activeMenu request attribute in " + page);
                        continue;
                    }

                    Matcher matcher = ACTIVE_SIDEBAR_ITEM_PATTERN.matcher(html);
                    int count = 0;
                    String activeHref = "";
                    while (matcher.find()) {
                        count++;
                        activeHref = matcher.group(1);
                    }

                    assertEquals(1, count, () -> "Expected exactly one active sidebar item in " + page);
                    assertEquals(expectedSidebarHref(dashboardDirectory.getFileName().toString(), pageName),
                            normalizeDashboardHref(activeHref),
                            () -> "Unexpected active sidebar href in " + page);
                }
            }
        }
    }

    @Test
    void mainScriptDoesNotClearEveryListActiveState() throws IOException {
        String mainScript = Files.readString(WEBAPP_DIR.resolve("assets/js/main.js"));

        assertFalse(mainScript.contains("dynamicActiveMenuClass($('ul'))"),
                "Main script must not run active menu detection over every ul, because it clears dashboard sidebars");
        assertTrue(mainScript.contains("dynamicActiveMenuClass($('.nav-menu'))"),
                "Main script should scope top navigation active state to .nav-menu");
    }

    @Test
    void dashboardSidebarLogoNavigatesToPublicHome() throws IOException {
        String sidebar = Files.readString(FRAGMENTS_DIR.resolve("dashboard-sidebar.jspf"));
        Pattern logoLinkPattern = Pattern.compile(
                "<a href=\"\\$\\{pageContext\\.request\\.contextPath}\\$\\{siteHomeHref}\" class=\"dashboard-sidebar__site-logo\">\\s*<img",
                Pattern.CASE_INSENSITIVE
        );

        assertTrue(sidebar.contains("siteHomeHref"),
                "Dashboard sidebar should keep the site logo target separate from the dashboard target");
        assertTrue(sidebar.contains("value=\"/index.jsp\""),
                "Dashboard sidebar logo must navigate to the public home page");
        assertTrue(logoLinkPattern.matcher(sidebar).find(),
                "Dashboard sidebar logo must use siteHomeHref instead of dashboardHref");
    }

    @Test
    void adminSidebarShowsUserContextChildrenOnly() throws IOException {
        String sidebar = Files.readString(FRAGMENTS_DIR.resolve("dashboard-sidebar.jspf"));

        assertTrue(sidebar.contains("adminUserContextId"),
                "Admin user child navigation must be guarded by a selected user context");
        assertTrue(sidebar.contains("adminUserActiveChild == 'detail'"),
                "User Detail must be represented as a Users child item");
        assertTrue(sidebar.contains("adminUserActiveChild == 'edit'"),
                "Edit User must be represented as a Users child item");
        assertTrue(sidebar.contains("/admin/deletion-requests?userId=${adminUserContextId}"),
                "Deletion must be a user-scoped Users child item");
        assertTrue(sidebar.contains("/admin/activity-log?userId=${adminUserContextId}"),
                "Audit must be a user-scoped Users child item");
        assertFalse(sidebar.contains("href=\"${pageContext.request.contextPath}/admin/deletion-requests\""),
                "Deletion must not remain an independent admin sidebar item");
        assertFalse(sidebar.contains("href=\"${pageContext.request.contextPath}/admin/activity-log\""),
                "Audit must not remain an independent admin sidebar item");
    }

    @Test
    void adminUsersPageIncludesInactiveCardAndUserScopedActions() throws IOException {
        String users = Files.readString(WEBAPP_DIR.resolve("admin/admin/admin-users.jsp"));

        assertTrue(users.contains("Inactive"),
                "Users page must expose the inactive count card");
        assertTrue(users.contains("${inactiveUsers}"),
                "Users page must render the inactive user count");
        assertTrue(users.contains("/admin/users/${user.id}/inactivate"),
                "Users page state modal must offer inactivation");
        assertTrue(users.contains("/admin/users/${user.id}/activate"),
                "Users page state modal must offer activation");
        assertTrue(users.contains("gape-state-action-button") && users.contains(".gape-state-action-button:hover")
                        && users.contains(".gape-state-action-button:active"),
                "State action buttons must expose Eduall-style hover and active states");
        assertFalse(users.contains("style=\"background-color: #f97316;\""),
                "State action buttons must not depend on inline styles for interaction colors");
        assertTrue(users.contains("/admin/deletion-requests?userId=${user.id}"),
                "Users actions must include user-scoped deletion requests");
        assertTrue(users.contains("/admin/activity-log?userId=${user.id}"),
                "Users actions must include user-scoped audit");
    }

    @Test
    void adminUserDetailRendersPhotoAndCriticalActions() throws IOException {
        String detail = Files.readString(WEBAPP_DIR.resolve("admin/admin/admin-user-detail.jsp"));

        assertTrue(detail.contains("gape-user-detail-photo"),
                "User Detail must render the user photo as an image");
        assertFalse(detail.contains("${empty user.photo ? 'No photo' : user.photo}"),
                "User Detail must not render the stored photo path as visible text");
        assertTrue(detail.contains("gape-action-delete") && detail.contains(">Delete</button>"),
                "Delete critical action must be red");
        assertTrue(detail.contains(".gape-action-button:hover") && detail.contains(".gape-action-button:active"),
                "Critical action buttons must expose hover and active interaction states");
        assertTrue(detail.contains("gape-action-block") && detail.contains(">Block</button>"),
                "Block critical action must be orange");
        assertTrue(detail.contains("gape-action-inactivate") && detail.contains(">Inactivate</button>"),
                "Inactivate critical action must be yellow");
        assertTrue(detail.contains("gape-action-edit") && detail.contains(">Edit</a>"),
                "Edit critical action must be green");
        assertTrue(detail.contains("gape-action-audit") && detail.contains(">View Audit</a>"),
                "View Audit critical action must be blue");
    }

    @Test
    void profileDocumentFieldsDoNotUseSelectOffset() throws IOException {
        String profile = Files.readString(FRAGMENTS_DIR.resolve("dashboard-my-profile-content.jspf"));

        assertFalse(profile.contains("padding-block-start: 16px"),
                "Profile select fields must not be pushed below adjacent text inputs");
        assertFalse(profile.contains("row align-items-center gy-4 mb-24"),
                "Profile form field row must align labels by the top, not by centered column height");
    }

    @Test
    void profileNoLongerRendersRemovedAccountPreferences() throws IOException {
        String profile = Files.readString(FRAGMENTS_DIR.resolve("dashboard-my-profile-content.jspf"));
        String removedLabel = "Priv" + "acy Preferences";
        String removedFieldPrefix = "priv" + "acy_";
        String removedEndpoint = "/account/" + "priv" + "acy";
        String removedCssHook = "gape-readonly-" + "priv" + "acy";

        assertFalse(profile.contains(removedLabel),
                "Profile page must not render removed account preferences");
        assertFalse(profile.contains(removedFieldPrefix),
                "Profile page must not submit removed account preference fields");
        assertFalse(profile.contains(removedEndpoint),
                "Profile page must not post to removed account preference endpoint");
        assertFalse(profile.contains(removedCssHook),
                "Profile page must not keep removed account preference styling hooks");
    }

    @Test
    void accountDeletionRequestStandalonePageIsRemoved() throws IOException {
        assertFalse(Files.exists(WEBAPP_DIR.resolve("WEB-INF/views/access/account-deletion-requests.jsp")),
                "Standalone account deletion request JSP must not exist");

        String servlet = Files.readString(Path.of("src/main/java/pt/isel/gape/web/controller/AccountDeletionRequestServlet.java"));
        assertFalse(servlet.contains("account-deletion-requests.jsp"),
                "Deletion request servlet must not forward to the removed standalone JSP");
        assertTrue(servlet.contains("profileFallback"),
                "Deletion request servlet must redirect back to the profile flow");
    }

    @Test
    void dashboardFooterAlwaysShowsLegalLinks() throws IOException {
        String footer = Files.readString(FRAGMENTS_DIR.resolve("dashboard-footer.jspf"));

        assertTrue(footer.contains("Copyright &copy; 2026"),
                "Dashboard footer must use the 2026 copyright");
        assertFalse(footer.contains("Priv" + "acy Policy"),
                "Dashboard footer must not keep removed policy links");
        assertTrue(footer.contains("Terms & Conditions"),
                "Dashboard footer must keep the Terms & Conditions link");
        assertFalse(footer.contains("Account Deletion"),
                "Dashboard footer must not swap legal links for account deletion");

        String sidebar = Files.readString(FRAGMENTS_DIR.resolve("dashboard-sidebar.jspf"));
        assertFalse(sidebar.contains("/account/deletion-requests"),
                "Sidebar must not point to the removed standalone deletion request page");
    }

    @Test
    void footerKeepsOnlyBottomFooterContent() throws IOException {
        try (Stream<Path> stream = Files.walk(WEBAPP_DIR)) {
            for (Path page : stream.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".jsp")).toList()) {
                String html = Files.readString(page);
                assertFullFooterKeepsOnlyBottomFooter(page, html);
                assertDashboardFooterKeepsLegalLinks(page, html);
            }
        }
    }

    @Test
    void eduallAnalysisDocumentationExists() {
        assertTrue(Files.isRegularFile(ANALYSIS_DOC), () -> "Expected analysis documentation: " + ANALYSIS_DOC);
    }

    private static boolean hasDashboardSidebar(String html) {
        return html.contains("dashboard-sidebar") || html.contains("student-dashboard-sidebar");
    }

    private static boolean usesCommonDashboardSidebar(String html) {
        return html.contains("/WEB-INF/fragments/dashboard-sidebar.jspf");
    }

    private static String expectedSidebarHref(String role, String pageName) {
        return role + "/" + pageName;
    }

    private static String normalizeDashboardHref(String href) {
        String contextPrefix = "${pageContext.request.contextPath}/";
        if (href.startsWith(contextPrefix)) {
            return href.substring(contextPrefix.length());
        }
        return href;
    }

    private static void assertFullFooterKeepsOnlyBottomFooter(Path page, String html) {
        int footerStart = html.indexOf("<footer class=\"footer");
        if (footerStart < 0) {
            return;
        }

        int bottomFooter = html.indexOf("<!-- bottom Footer -->", footerStart);
        assertTrue(bottomFooter > footerStart, () -> "Expected bottom footer in " + page);

        String beforeBottom = html.substring(footerStart, bottomFooter);
        assertFalse(beforeBottom.contains("py-120"), () -> "Main footer spacing still exists in " + page);
        assertFalse(beforeBottom.contains("footer-item__title"), () -> "Main footer title still exists in " + page);
        assertFalse(beforeBottom.contains("Subscribe Here"), () -> "Subscribe footer block still exists in " + page);
        assertFalse(beforeBottom.contains("Quick Link"), () -> "Quick link footer block still exists in " + page);

        int footerEnd = html.indexOf("</footer>", bottomFooter);
        assertTrue(footerEnd > bottomFooter, () -> "Expected closing footer in " + page);

        String footer = html.substring(bottomFooter, footerEnd);
        assertTrue(footer.contains("Copyright"), () -> "Expected footer copyright in " + page);
        assertFalse(footer.contains("Priv" + "acy Policy"), () -> "Unexpected removed policy link in " + page);
        assertTrue(footer.contains("Terms & Conditions"), () -> "Expected footer Terms & Conditions link in " + page);
    }

    private static void assertDashboardFooterKeepsLegalLinks(Path page, String html) {
        int footerStart = html.indexOf("<!-- =========message profile footer start============== -->");
        if (footerStart < 0) {
            return;
        }

        int footerEnd = html.indexOf("<!-- =========message profile footer end============== -->", footerStart);
        assertTrue(footerEnd > footerStart, () -> "Expected dashboard footer end in " + page);

        String footer = html.substring(footerStart, footerEnd);
        assertTrue(footer.contains("Copyright"), () -> "Expected dashboard footer copyright in " + page);
        assertFalse(footer.contains("Priv" + "acy Policy"), () -> "Unexpected dashboard removed policy link in " + page);
        assertTrue(footer.contains("Terms & Conditions"), () -> "Expected dashboard footer Terms & Conditions link in " + page);
    }
}
