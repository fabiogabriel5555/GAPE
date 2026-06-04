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
            "admin/dashboard.jsp",
            "admin/admin-dashbord.jsp",
            "admin/admin-dashbord-courses.jsp",
            "admin/admin-dashbord-message.jsp",
            "admin/admin-dashbord-my-profile.jsp",
            "admin/admin-dashbord-quiz-attempts.jsp",
            "admin/admin-dashbord-reviews.jsp",
            "admin/admin-dashbord-settings.jsp",
            "admin/admin-dashbord-wishlist.jsp",
            "coordinator/coordinator-dashboard.jsp",
            "coordinator/coordinator-dashboard-message.jsp",
            "coordinator/coordinator-dashboard-my-profile.jsp",
            "coordinator/coordinator-dashboard-account-settings.jsp",
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
            "privacy-policy.jsp",
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
    void dashboardSidebarsMarkExactlyOneActiveItem() throws IOException {
        List<Path> dashboardDirectories = List.of(
                WEBAPP_DIR.resolve("admin"),
                WEBAPP_DIR.resolve("student"),
                WEBAPP_DIR.resolve("instructor"),
                WEBAPP_DIR.resolve("coordinator")
        );

        for (Path dashboardDirectory : dashboardDirectories) {
            try (Stream<Path> stream = Files.list(dashboardDirectory)) {
                for (Path page : stream.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".jsp")).toList()) {
                    String pageName = page.getFileName().toString();
                    String html = Files.readString(page);

                    if (!hasDashboardSidebar(html) || pageName.endsWith("-ashboard.jsp")) {
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
                    assertEquals(expectedSidebarHref(dashboardDirectory.getFileName().toString(), pageName), activeHref,
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

    private static String expectedSidebarHref(String role, String pageName) {
        if ("admin".equals(role) && "dashboard.jsp".equals(pageName)) {
            return "admin/admin-dashbord.jsp";
        }

        return role + "/" + pageName;
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
        assertTrue(footer.contains("Privacy Policy"), () -> "Expected footer Privacy Policy link in " + page);
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
        assertTrue(footer.contains("Privacy Policy"), () -> "Expected dashboard footer Privacy Policy link in " + page);
        assertTrue(footer.contains("Terms & Conditions"), () -> "Expected dashboard footer Terms & Conditions link in " + page);
    }
}
