package pt.isel.gape.structure;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class TemplateStructureTest {

    private static final Path WEBAPP_DIR = Path.of("src/main/webapp");
    private static final Path JAVA_DIR = Path.of("src/main/java");
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
            "admin/admin/user/admin-users.jsp",
            "admin/admin/user/admin-user-form.jsp",
            "admin/admin/user/admin-user-detail.jsp",
            "admin/admin/user/admin-deletion-requests.jsp",
            "admin/admin/user/admin-audit.jsp",
            "admin/admin/organization/admin-organizations.jsp",
            "admin/admin/organization/admin-organization-form.jsp",
            "admin/admin/organization/admin-organization-detail.jsp",
            "admin/admin/organization/admin-organic-unit-detail.jsp",
            "admin/admin/organization/admin-organic-unit-form.jsp",
            "admin/admin-message.jsp",
            "admin/admin-my-profile.jsp",
            "admin/admin-quiz-attempts.jsp",
            "admin/admin-reviews.jsp",
            "coordinator/coordinator-dashbord.jsp",
            "coordinator/coordinator-message.jsp",
            "coordinator/coordinator-my-profile.jsp",
            "coordinator/coordinator-quiz-attempts.jsp",
            "coordinator/coordinator-reviews.jsp",
            "coordinator/coordinator/subject/coordinator-subjects.jsp",
            "coordinator/coordinator/subject/coordinator-subject-form.jsp",
            "coordinator/coordinator/subject/coordinator-subject-detail.jsp",
            "coordinator/coordinator/subject/coordinator-subject-course-form.jsp",
            "instructor/instructor-dashbord.jsp",
            "instructor/instructor-message.jsp",
            "instructor/instructor-my-profile.jsp",
            "instructor/instructor-quiz-attempts.jsp",
            "instructor/instructor-reviews.jsp",
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
    void coordinatorPagesFollowAdministratorStructure() throws Exception {
        Path coordinatorDir = WEBAPP_DIR.resolve("coordinator");
        List<String> sharedRootPages = List.of(
                "coordinator-dashbord.jsp",
                "coordinator-message.jsp",
                "coordinator-my-profile.jsp",
                "coordinator-quiz-attempts.jsp",
                "coordinator-reviews.jsp"
        );

        for (String sharedRootPage : sharedRootPages) {
            Path page = coordinatorDir.resolve(sharedRootPage);
            assertTrue(Files.isRegularFile(page), () -> "Expected coordinator shared root page: " + page);
        }

        assertFalse(Files.exists(coordinatorDir.resolve("coordinator-home.jsp")),
                "Coordinator dashboard must use the admin-like dashbord page name, not home");
        assertTrue(Files.isDirectory(coordinatorDir.resolve("coordinator")),
                "Coordinator-specific pages must live under coordinator/coordinator, matching admin/admin");

        try (Stream<Path> stream = Files.list(coordinatorDir)) {
            List<String> rootSpecificPages = stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".jsp"))
                    .filter(name -> !sharedRootPages.contains(name))
                    .collect(Collectors.toList());

            assertTrue(rootSpecificPages.isEmpty(),
                    () -> "Coordinator-specific JSPs must not stay in the coordinator root: " + rootSpecificPages);
        }

        try (Stream<Path> stream = Files.walk(coordinatorDir.resolve("coordinator"))) {
            List<Path> unexpectedSpecificPages = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".jsp"))
                    .filter(path -> !path.startsWith(coordinatorDir.resolve("coordinator/subject")))
                    .filter(path -> !path.startsWith(coordinatorDir.resolve("coordinator/class-group")))
                    .collect(Collectors.toList());

            assertTrue(unexpectedSpecificPages.isEmpty(),
                    () -> "Unused coordinator JSPs must not remain under coordinator/coordinator: "
                            + unexpectedSpecificPages);
        }
    }

    @Test
    void instructorPagesFollowAdministratorStructure() throws Exception {
        Path instructorDir = WEBAPP_DIR.resolve("instructor");
        List<String> sharedRootPages = List.of(
                "instructor-dashbord.jsp",
                "instructor-message.jsp",
                "instructor-my-profile.jsp",
                "instructor-quiz-attempts.jsp",
                "instructor-reviews.jsp"
        );

        for (String sharedRootPage : sharedRootPages) {
            Path page = instructorDir.resolve(sharedRootPage);
            assertTrue(Files.isRegularFile(page), () -> "Expected instructor shared root page: " + page);
        }

        assertFalse(Files.exists(instructorDir.resolve("instructor-home.jsp")),
                "Instructor dashboard must use the admin-like dashbord page name, not home");
        assertFalse(Files.exists(instructorDir.resolve("instructor.jsp")),
                "Unused instructor listing JSP must not remain under instructor");
        assertFalse(Files.exists(instructorDir.resolve("instructor-details.jsp")),
                "Unused instructor detail JSP must not remain under instructor");

        try (Stream<Path> stream = Files.list(instructorDir)) {
            List<String> rootSpecificPages = stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".jsp"))
                    .filter(name -> !sharedRootPages.contains(name))
                    .collect(Collectors.toList());

            assertTrue(rootSpecificPages.isEmpty(),
                    () -> "Unused instructor JSPs must not stay in the instructor root: " + rootSpecificPages);
        }

        assertTrue(Files.isDirectory(instructorDir.resolve("instructor/class-group")),
                "Instructor-specific class group pages must live under instructor/instructor/class-group");
    }

    @Test
    void classGroupPagesAreProfileSpecific() throws IOException {
        Map<Path, List<String>> expectedPagesByDirectory = Map.of(
                WEBAPP_DIR.resolve("admin/admin/class-group"),
                List.of(
                        "admin-class-groups.jsp",
                        "admin-class-group-detail.jsp",
                        "admin-class-group-form.jsp",
                        "admin-content-block-form.jsp"
                ),
                WEBAPP_DIR.resolve("coordinator/coordinator/class-group"),
                List.of(
                        "coordinator-class-groups.jsp",
                        "coordinator-class-group-detail.jsp",
                        "coordinator-class-group-form.jsp",
                        "coordinator-content-block-form.jsp"
                ),
                WEBAPP_DIR.resolve("instructor/instructor/class-group"),
                List.of(
                        "instructor-class-groups.jsp",
                        "instructor-class-group-detail.jsp",
                        "instructor-class-group-form.jsp",
                        "instructor-content-block-form.jsp"
                )
        );

        for (Map.Entry<Path, List<String>> entry : expectedPagesByDirectory.entrySet()) {
            Path directory = entry.getKey();
            assertTrue(Files.isDirectory(directory), () -> "Expected class group directory: " + directory);
            for (String pageName : entry.getValue()) {
                assertTrue(Files.isRegularFile(directory.resolve(pageName)),
                        () -> "Expected profile-specific class group page: " + directory.resolve(pageName));
            }
        }

        try (Stream<Path> stream = Files.list(WEBAPP_DIR.resolve("admin/admin/course"))) {
            List<Path> misplacedClassGroupPages = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().contains("class-group")
                            || path.getFileName().toString().contains("class-groups")
                            || path.getFileName().toString().contains("content-block"))
                    .collect(Collectors.toList());

            assertTrue(misplacedClassGroupPages.isEmpty(),
                    () -> "Class group JSPs must not remain under admin/admin/course: " + misplacedClassGroupPages);
        }

        String servlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/ClassGroupManagementServlet.java"));
        assertTrue(servlet.contains("/admin/admin/class-group/admin-class-groups.jsp")
                        && servlet.contains("/coordinator/coordinator/class-group/coordinator-class-groups.jsp")
                        && servlet.contains("/instructor/instructor/class-group/instructor-class-groups.jsp"),
                "ClassGroupManagementServlet must route class groups to profile-specific JSPs");
        assertFalse(servlet.contains("/admin/admin/course/admin-class-groups.jsp"),
                "ClassGroupManagementServlet must not route class groups through the admin course JSP folder");
    }

    @Test
    void classGroupFormsKeepCourseAndSubjectImmutableDuringEdit() throws IOException {
        List<Path> formPaths = List.of(
                WEBAPP_DIR.resolve("admin/admin/class-group/admin-class-group-form.jsp"),
                WEBAPP_DIR.resolve("coordinator/coordinator/class-group/coordinator-class-group-form.jsp"),
                WEBAPP_DIR.resolve("instructor/instructor/class-group/instructor-class-group-form.jsp")
        );

        for (Path formPath : formPaths) {
            String form = Files.readString(formPath);
            assertTrue(form.contains("<c:when test=\"${creating}\">"),
                    () -> "Class group form must only show course/subject selectors while creating: " + formPath);
            assertTrue(form.contains("id=\"courseContext\"")
                            && form.contains("id=\"subjectContext\"")
                            && form.contains("name=\"courseId\" value=\"${form.courseId}\"")
                            && form.contains("name=\"subjectId\" value=\"${form.subjectId}\""),
                    () -> "Class group edit form must preserve immutable course/subject as readonly context: " + formPath);
        }
    }

    @Test
    void studentEnrollmentPageExposesClassGroupFlow() throws IOException {
        String servlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/StudentEnrollmentServlet.java"));
        String page = Files.readString(WEBAPP_DIR.resolve("student/student-enrolled-courses.jsp"));

        assertTrue(servlet.contains("ClassGroupEnrollmentService")
                        && servlet.contains("studentClassGroups")
                        && servlet.contains("\"class-groups\""),
                "StudentEnrollmentServlet must load and handle student class group enrollments");
        assertTrue(page.contains("Class Groups")
                        && page.contains("/student/enrollments/class-groups/${item.classGroup.id}")
                        && page.contains("/student/enrollments/class-groups/${item.classGroup.id}/withdraw")
                        && page.contains("Pedagogical Blocks"),
                "Student enrollment page must expose class group enrollment, withdrawal and visible blocks");
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
                Set<String> allowedDashbordNames = Set.of(
                        "admin-dashbord.jsp",
                        "coordinator-dashbord.jsp",
                        "instructor-dashbord.jsp"
                );
                List<String> unexpectedNames = stream
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .filter(name -> name.endsWith(".jsp"))
                        .filter(name -> name.contains("dashboard")
                                || name.contains("ashboard")
                                || (name.contains("dashbord") && !allowedDashbordNames.contains(name)))
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
                    assertEquals(expectedSidebarHref(dashboardDirectory, page),
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
        assertFalse(mainScript.contains("dynamicActiveSidebarClass($('.dashboard-sidebar"),
                "Main script must not recalculate active state for the shared dashboard sidebar");
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
        String users = Files.readString(WEBAPP_DIR.resolve("admin/admin/user/admin-users.jsp"));

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
        String detail = Files.readString(WEBAPP_DIR.resolve("admin/admin/user/admin-user-detail.jsp"));

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
    void adminUserFormCanAssignAdministratorPermissionsWithContext() throws IOException {
        String form = Files.readString(WEBAPP_DIR.resolve("admin/admin/user/admin-user-form.jsp"));

        assertTrue(form.contains("Administrator Permissions"),
                "User form must render administrator permission assignments");
        assertTrue(form.contains("name=\"adminPermissionAssignments\""),
                "User form must submit selected administrator permissions and contexts");
        assertTrue(form.contains("data-admin-permission-input"),
                "Administrator permission inputs must be tied to the Administrator profile state");
        assertTrue(form.contains("MANAGE_ORGANIZATION_STRUCTURE")
                        && form.contains("MANAGE_LEARNING")
                        && form.contains("MANAGE_ENROLLMENTS"),
                "User form must expose the contextual administrator permission groups");
    }

    @Test
    void associationFormsExposeDeleteActions() throws IOException {
        String courseSubjectForm = Files.readString(WEBAPP_DIR.resolve("admin/admin/course/admin-course-subject-form.jsp"));
        String subjectCourseForm = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subject-course-form.jsp"));
        String subjectList = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subjects.jsp"));

        assertTrue(courseSubjectForm.contains("/subjects/${association.subjectId}/delete")
                        && courseSubjectForm.contains("gape-action-delete"),
                "Associate Subject must allow deleting existing subject associations");
        assertTrue(subjectCourseForm.contains("/courses/${association.courseId}/delete")
                        && subjectCourseForm.contains("gape-action-delete"),
                "Associate Courses must allow deleting existing course associations");
        assertTrue(subjectList.contains("canManageSubjectAssociationsById")
                        && subjectList.contains("canManageSubjectAssociationsRow")
                        && subjectList.contains("${subjectBasePath}/${subject.id}/courses"),
                "Subjects list must expose association actions independently from subject mutation");
    }

    @Test
    void coordinatorSidebarLinksToManagedSubjects() throws IOException {
        String sidebar = Files.readString(FRAGMENTS_DIR.resolve("dashboard-sidebar.jspf"));
        String coordinatorHome = Files.readString(WEBAPP_DIR.resolve("coordinator/coordinator-dashbord.jsp"));
        String coordinatorMessage = Files.readString(WEBAPP_DIR.resolve("coordinator/coordinator-message.jsp"));
        String coordinatorProfile = Files.readString(WEBAPP_DIR.resolve("coordinator/coordinator-my-profile.jsp"));
        String coordinatorQuizAttempts = Files.readString(WEBAPP_DIR.resolve("coordinator/coordinator-quiz-attempts.jsp"));
        String coordinatorReviews = Files.readString(WEBAPP_DIR.resolve("coordinator/coordinator-reviews.jsp"));
        String subjectList = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subjects.jsp"));
        String subjectDetail = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subject-detail.jsp"));
        String subjectForm = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subject-form.jsp"));
        String subjectCourseForm = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subject-course-form.jsp"));

        assertTrue(coordinatorHome.contains("<jsp:include page=\"/admin/admin-dashbord.jsp\"")
                        && coordinatorMessage.contains("<jsp:include page=\"/admin/admin-message.jsp\"")
                        && coordinatorProfile.contains("<jsp:include page=\"/admin/admin-my-profile.jsp\""),
                "Coordinator shared root pages must reuse the administrator templates");
        assertTrue(coordinatorQuizAttempts.contains("<jsp:include page=\"/admin/admin-quiz-attempts.jsp\"")
                        && coordinatorReviews.contains("<jsp:include page=\"/admin/admin-reviews.jsp\""),
                "Coordinator shared root pages must reuse the administrator quiz/review templates");
        assertTrue(sidebar.contains("/coordinator/subjects")
                        && sidebar.contains("coordinatorSubjectContextId")
                        && sidebar.contains("coordinatorSubjectActiveChild"),
                "Coordinator sidebar must expose the admin-like subject navigation under the coordinator route");
        assertTrue(sidebar.contains("not sessionScope['gape.auth.hasCoordinatorProfile']"),
                "Coordinator sidebar must not expose the generic courses tab");
        assertTrue(sidebar.contains("coordinator/coordinator-reviews.jsp")
                        && sidebar.contains("coordinator/coordinator-quiz-attempts.jsp")
                        && sidebar.contains("${reviewsHref}")
                        && sidebar.contains("${quizAttemptsHref}"),
                "Coordinator sidebar must route reviews and quiz attempts to coordinator pages");
        assertTrue(sidebar.indexOf("Message") < sidebar.indexOf("Reviews")
                        && sidebar.indexOf("Reviews") < sidebar.indexOf("Quiz Attempts")
                        && sidebar.indexOf("Quiz Attempts") < sidebar.indexOf(">Coordinator<")
                        && sidebar.indexOf(">Coordinator<") < sidebar.indexOf("Class Groups"),
                "Coordinator sidebar must keep reviews and quiz attempts directly below messages");
        assertTrue(subjectList.contains("${subjectBasePath}/${subject.id}")
                        && subjectDetail.contains("${subjectBasePath}/${subject.id}/edit")
                        && subjectForm.contains("${subjectBasePath}/${form.id}/courses")
                        && subjectCourseForm.contains("${subjectBasePath}/${subject.id}/courses")
                        && subjectDetail.contains("${subjectCourseBasePath}/${association.courseId}")
                        && subjectForm.contains("${subjectCourseBasePath}/${association.courseId}")
                        && subjectCourseForm.contains("${subjectCourseBasePath}/${association.courseId}"),
                "Shared subject views must use the request-scoped base path for admin and coordinator routes");
        assertTrue(subjectForm.contains("not creating and canAssignSubjectCoordinators"),
                "Subject edit form must hide coordinator assignment outside administrator context");
    }

    @Test
    void instructorSharedPagesReuseAdministratorTemplates() throws IOException {
        String sidebar = Files.readString(FRAGMENTS_DIR.resolve("dashboard-sidebar.jspf"));
        String instructorDashboard = Files.readString(WEBAPP_DIR.resolve("instructor/instructor-dashbord.jsp"));
        String instructorMessage = Files.readString(WEBAPP_DIR.resolve("instructor/instructor-message.jsp"));
        String instructorProfile = Files.readString(WEBAPP_DIR.resolve("instructor/instructor-my-profile.jsp"));
        String instructorQuizAttempts = Files.readString(WEBAPP_DIR.resolve("instructor/instructor-quiz-attempts.jsp"));
        String instructorReviews = Files.readString(WEBAPP_DIR.resolve("instructor/instructor-reviews.jsp"));

        assertTrue(instructorDashboard.contains("<jsp:include page=\"/admin/admin-dashbord.jsp\"")
                        && instructorMessage.contains("<jsp:include page=\"/admin/admin-message.jsp\"")
                        && instructorProfile.contains("<jsp:include page=\"/admin/admin-my-profile.jsp\""),
                "Instructor shared root pages must reuse the administrator templates");
        assertTrue(instructorQuizAttempts.contains("<jsp:include page=\"/admin/admin-quiz-attempts.jsp\"")
                        && instructorReviews.contains("<jsp:include page=\"/admin/admin-reviews.jsp\""),
                "Instructor shared root pages must reuse the administrator quiz/review templates");
        assertTrue(sidebar.contains("/instructor/instructor-message.jsp")
                        && sidebar.contains("/instructor/instructor-reviews.jsp")
                        && sidebar.contains("/instructor/instructor-quiz-attempts.jsp")
                        && sidebar.contains("${messageHref}")
                        && sidebar.contains("${reviewsHref}")
                        && sidebar.contains("${quizAttemptsHref}"),
                "Instructor sidebar must route shared pages to the instructor wrappers");
        assertTrue(sidebar.contains("not isTeacherDashboard"),
                "Instructor sidebar must not expose the generic courses tab");
        int teacherSection = sidebar.indexOf(">Teacher<");
        assertTrue(teacherSection > 0 && sidebar.indexOf("Class Groups", teacherSection) > teacherSection,
                "Instructor sidebar must show class groups under the Teacher section");
    }

    @Test
    void organizationPagesRenderPhotoAndManagedUnitCode() throws IOException {
        String organizationForm = Files.readString(WEBAPP_DIR.resolve("admin/admin/organization/admin-organization-form.jsp"));
        String organizationList = Files.readString(WEBAPP_DIR.resolve("admin/admin/organization/admin-organizations.jsp"));
        String organizationDetail = Files.readString(WEBAPP_DIR.resolve("admin/admin/organization/admin-organization-detail.jsp"));
        String unitDetail = Files.readString(WEBAPP_DIR.resolve("admin/admin/organization/admin-organic-unit-detail.jsp"));
        String unitForm = Files.readString(WEBAPP_DIR.resolve("admin/admin/organization/admin-organic-unit-form.jsp"));
        String unitAdministrators = Files.readString(FRAGMENTS_DIR.resolve("organic-unit-administrators.jspf"));

        assertTrue(organizationForm.contains("enctype=\"multipart/form-data\""),
                "Organization form must support photo uploads");
        assertTrue(organizationForm.contains("name=\"organizationImage\""),
                "Organization form must submit the uploaded organization photo");
        assertTrue(organizationForm.contains("accept=\"image/jpeg,image/png,image/gif,image/bmp,image/webp\""),
                "Organization form must limit the chooser to server-supported image formats");
        int organizationPhotoHeading = organizationForm.indexOf("Organization Photo");
        assertTrue(organizationPhotoHeading > 0,
                "Organization form must render the organization photo upload section");
        String beforeOrganizationPhoto = organizationForm.substring(Math.max(0, organizationPhotoHeading - 160), organizationPhotoHeading);
        assertFalse(beforeOrganizationPhoto.contains("not creating"),
                "Organization photo upload must be visible when creating a new organization");
        assertTrue(organizationList.contains("gape-organization-table-photo"),
                "Organization list must render organization photos");
        assertTrue(organizationList.contains("gape-organization-list")
                        && organizationList.contains("gape-organization-card"),
                "Organization list must visually separate each organization card");
        assertTrue(organizationList.contains("gape-organization-units-panel")
                        && organizationList.contains("<section class=\"gape-organization-card\">"),
                "Organic units must render inside the owning organization card");
        assertFalse(organizationList.contains("gape-organization-units-row"),
                "Organic units must not render as a separate table row");
        assertTrue(organizationDetail.contains("gape-organization-detail-photo"),
                "Organization detail must render organization photos");
        assertTrue(organizationDetail.contains("Current Administrators"),
                "Organization detail must show assigned administrators and assignment metadata");
        assertTrue(organizationDetail.contains("not unit.archived"),
                "Organization detail must hide unit edit/archive/delete actions for archived units");
        assertTrue(organizationList.contains("/admin/organizations/${organization.id}/units/${unit.id}"),
                "Organization list must link each organic unit to its detail page");
        assertTrue(organizationDetail.contains("/admin/organizations/${organization.id}/units/${unit.id}"),
                "Organization detail must link each organic unit to its detail page");
        assertFalse(organizationForm.contains("value=\"ARCHIVED\""),
                "Organization edit form must not archive through the normal update flow");
        assertFalse(unitForm.contains("value=\"ARCHIVED\""),
                "Organic unit edit form must not archive through the normal update flow");
        assertFalse(unitForm.contains("name=\"code\""),
                "Organic unit forms must not ask users to manage codes");
        assertTrue(unitForm.contains("Generated Code"),
                "Organic unit edit form must show the application-managed code as read-only information");
        assertTrue(unitForm.contains("value=\"FACULTY\"") && unitForm.contains("value=\"CENTER\"")
                        && unitForm.contains("value=\"OFFICE\"") && unitForm.contains("value=\"SERVICE\""),
                "Organic unit form must expose the expanded project unit types");
        assertTrue(unitDetail.contains("organic-unit-administrators.jspf"),
                "Organic unit detail must render the direct administrator panel");
        assertTrue(unitForm.contains("organic-unit-administrators.jspf"),
                "Organic unit edit form must render the direct administrator panel");
        assertTrue(unitAdministrators.contains("organicUnitAdministratorOptions")
                        && unitAdministrators.contains("assignedOrganicUnitAdministrators")
                        && unitAdministrators.contains("unitAdminAssignAction")
                        && unitAdministrators.contains("unitAdminRevokeAction"),
                "Organic unit administrator panel must expose assign and revoke controls");
        assertTrue(unitAdministrators.indexOf("assignedOrganicUnitAdministrators")
                        < unitAdministrators.indexOf("unitAdminAssignAction"),
                "Organic unit administrator panel must render assigned administrators before the assign form");
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

    private static String expectedSidebarHref(Path dashboardDirectory, Path page) {
        return dashboardDirectory.getParent().relativize(page).toString().replace('\\', '/');
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
