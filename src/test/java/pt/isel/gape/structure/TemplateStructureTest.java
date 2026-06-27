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
    void classGroupDetailAndEditExposeEnrollmentManagement() throws IOException {
        String servlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/ClassGroupManagementServlet.java"));
        String service = Files.readString(JAVA_DIR.resolve("pt/isel/gape/learning/service/ClassGroupEnrollmentService.java"));
        String dao = Files.readString(JAVA_DIR.resolve("pt/isel/gape/learning/dao/ClassGroupEnrollmentDAO.java"));
        String detail = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/class-group-detail-page.jspf"));
        String management = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/class-group-enrollment-management.jspf"));
        String row = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/class-group-enrollment-row.jspf"));

        assertTrue(detail.contains("Class Details &amp; Enrollments")
                        && detail.contains("data-cg-tab=\"info\"")
                        && detail.contains("data-cg-panel=\"info\"")
                        && detail.contains("Setup, teachers and enrollments.")
                        && detail.contains("class-group-enrollment-management.jspf"),
                "Class Group Detail must combine details and enrollments in one tab");
        int contentsIndex = detail.indexOf("<c:out value=\"${blockContentCount}\"/> contents");
        int loadedIndex = detail.indexOf(">Loaded");
        int newBlockIndex = detail.indexOf(">New Block");
        assertTrue(contentsIndex >= 0 && loadedIndex > contentsIndex && newBlockIndex > loadedIndex,
                "Class Group Detail structure actions must be ordered as contents, Loaded, New Block");
        assertFalse(detail.contains("data-cg-tab=\"enrollments\"")
                        || detail.contains("data-cg-panel=\"enrollments\""),
                "Class Group Detail must not keep a separate Enrollments tab");

        for (Path formPath : List.of(
                WEBAPP_DIR.resolve("admin/admin/class-group/admin-class-group-form.jsp"),
                WEBAPP_DIR.resolve("coordinator/coordinator/class-group/coordinator-class-group-form.jsp"),
                WEBAPP_DIR.resolve("instructor/instructor/class-group/instructor-class-group-form.jsp")
        )) {
            String form = Files.readString(formPath);
            assertTrue(form.contains("class-group-enrollment-management.jspf")
                            && form.contains("class-group-teacher-management.jspf"),
                    () -> "Edit Class Group must include enrollment and teacher management: " + formPath);
        }

        assertTrue(management.contains(">Enrollments</h3>")
                        && management.contains("pendingClassGroupEnrollments")
                        && management.contains("activeClassGroupEnrollments")
                        && management.contains("auditClassGroupEnrollments")
                        && management.contains("classGroupEnrollmentPolicy")
                        && management.contains("/learning/class-groups/${classGroup.id}/enrollments"),
                "Class group enrollment management must separate pending, active and audit enrollments");
        assertTrue(row.contains("/approve")
                        && row.contains("/reject")
                        && row.contains("/update")
                        && row.contains("/delete")
                        && row.contains("stateValue")
                        && row.contains("startDateValue")
                        && row.contains("endDateValue"),
                "Class group enrollment rows must allow approving, rejecting, editing and deleting enrollments");
        assertTrue(servlet.contains("updateStudentEnrollment")
                        && servlet.contains("deleteStudentEnrollment")
                        && servlet.contains("prepareClassGroupEnrollmentAttributes")
                        && servlet.contains("redirectToReturnPath(request, response, \"/learning/class-groups/\" + classGroupId + \"#class-group-enrollments\")"),
                "ClassGroupManagementServlet must route class group enrollment management actions and preserve the current section");
        assertTrue(service.contains("updateClassGroupEnrollment")
                        && service.contains("deleteClassGroupEnrollment")
                        && service.contains("CLASS_GROUP_ENROLL_UPDATE")
                        && service.contains("CLASS_GROUP_ENROLL_DELETE"),
                "ClassGroupEnrollmentService must support editing and deleting class group enrollments");
        assertTrue(dao.contains("updateEnrollment(")
                        && dao.contains("deleteEnrollment("),
                "ClassGroupEnrollmentDAO must persist class group enrollment edits and deletions");
    }

    @Test
    void classGroupListingsExposeShowActivitiesAutomation() throws IOException {
        List<Path> listPaths = List.of(
                WEBAPP_DIR.resolve("admin/admin/class-group/admin-class-groups.jsp"),
                WEBAPP_DIR.resolve("coordinator/coordinator/class-group/coordinator-class-groups.jsp"),
                WEBAPP_DIR.resolve("instructor/instructor/class-group/instructor-class-groups.jsp")
        );

        for (Path listPath : listPaths) {
            String page = Files.readString(listPath);
            assertTrue(page.contains("data-gape-tree-toggle=\"classGroupStructure${classGroup.id}\"")
                            && page.contains("Show Activities")
                            && page.contains("class-group-activities-panel.jspf")
                            && page.contains("canModifyClassGroupRow"),
                    () -> "Class group list must reuse the shared Show Activities fragment: " + listPath);
        }

        String servlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/ClassGroupManagementServlet.java"));
        assertTrue(servlet.contains("\"classGroupLessonsByClassGroup\"")
                        && servlet.contains("\"classGroupRoomsByClassGroup\"")
                        && servlet.contains("\"canManagePhysicalRoomByCode\""),
                "ClassGroupManagementServlet must load lessons, rooms and room permissions for Show Activities");

        String activityFragment = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/class-group-activities-panel.jspf"));
        assertTrue(activityFragment.contains("Class Activities")
                        && activityFragment.contains("/learning/lessons/${lesson.id}")
                        && activityFragment.contains("${lesson.compactDateRangeLabel}")
                        && activityFragment.contains("/learning/rooms/${room.encodedCode}"),
                "Class group activity fragment must expose lessons and rooms");

        String courseList = Files.readString(WEBAPP_DIR.resolve("admin/admin/course/admin-courses.jsp"));
        String subjectList = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subjects.jsp"));
        String organizationList = Files.readString(WEBAPP_DIR.resolve("admin/admin/organization/admin-organizations.jsp"));
        assertTrue(courseList.contains("courseClassGroupActivities${course.id}_${subject.id}_${classGroup.id}")
                        && courseList.contains("Show Activities")
                        && courseList.contains("class-group-activities-panel.jspf"),
                "Course list must expose Show Activities inside each class group");
        assertTrue(subjectList.contains("subjectClassGroupActivities${subject.id}_${course.id}_${classGroup.id}")
                        && subjectList.contains("Show Activities")
                        && subjectList.contains("class-group-activities-panel.jspf"),
                "Subject list must expose Show Activities inside each class group");
        assertTrue(organizationList.contains("organizationClassGroupActivities${course.id}_${subject.subjectId}_${classGroup.id}")
                        && organizationList.contains("Show Activities")
                        && organizationList.contains("class-group-activities-panel.jspf"),
                "Organization list must expose Show Activities inside each class group");

        String courseServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/CourseManagementServlet.java"));
        String subjectServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/SubjectManagementServlet.java"));
        String organizationServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/OrganizationManagementServlet.java"));
        assertTrue(courseServlet.contains("ClassGroupActivityViewSupport")
                        && courseServlet.contains("exposeClassGroupActivities"),
                "CourseManagementServlet must load class group activities");
        assertTrue(subjectServlet.contains("ClassGroupActivityViewSupport")
                        && subjectServlet.contains("exposeClassGroupActivities"),
                "SubjectManagementServlet must load class group activities");
        assertTrue(organizationServlet.contains("ClassGroupActivityViewSupport")
                        && organizationServlet.contains("exposeClassGroupActivities"),
                "OrganizationManagementServlet must load class group activities");
    }

    @Test
    void addContentModalSupportsLessons() throws IOException {
        String fragment = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/class-group-detail-page.jspf"));
        assertTrue(fragment.contains(">Class</button>")
                        && fragment.contains("data-content-category=\"class\"")
                        && fragment.contains("data-category=\"class\"")
                        && fragment.contains("data-source-kind=\"lesson\" data-lesson-type=\"online\"")
                        && fragment.contains("data-source-kind=\"lesson\" data-lesson-type=\"hybrid\"")
                        && fragment.contains("data-source-kind=\"lesson\" data-lesson-type=\"onsite\"")
                        && fragment.contains("data-lesson-action=\"${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/blocks/${block.id}/lessons\"")
                        && fragment.contains("data-lesson-starts-at")
                        && fragment.contains("data-lesson-ends-at")
                        && fragment.contains("data-lesson-access-url")
                        && fragment.contains("data-lesson-room"),
                "Add Content modal must create online, hybrid and in-person lessons directly from content blocks");

        String servlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/ClassGroupManagementServlet.java"));
        assertTrue(servlet.contains("@MultipartConfig")
                        && servlet.contains("case \"lessons\" -> createBlockLesson")
                        && servlet.contains("lessonService.createLesson")
                        && servlet.contains("physicalRoomOptions"),
                "ClassGroupManagementServlet must accept lesson creation from the multipart Add Content modal");
    }

    @Test
    void studentEnrollmentPageExposesClassGroupFlow() throws IOException {
        String servlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/StudentEnrollmentServlet.java"));
        String coursesPage = Files.readString(WEBAPP_DIR.resolve("student/student/course/student-courses.jsp"));
        String courseDetailPage = Files.readString(WEBAPP_DIR.resolve("student/student/course/student-course-detail.jsp"));
        String subjectsPage = Files.readString(WEBAPP_DIR.resolve("student/student/subject/student-subjects.jsp"));
        String classGroupsPage = Files.readString(WEBAPP_DIR.resolve("student/student/class-group/student-class-groups.jsp"));
        String classGroupDetailPage = Files.readString(WEBAPP_DIR.resolve("student/student/class-group/student-class-group-detail.jsp"));
        String subjectDetailPage = Files.readString(WEBAPP_DIR.resolve("student/student/subject/student-subject-detail.jsp"));
        String studentLessonsPage = Files.readString(WEBAPP_DIR.resolve("student/student/lesson/student-lessons.jsp"));
        String studentCalendarPage = Files.readString(WEBAPP_DIR.resolve("student/student/calendar/student-calendar.jsp"));
        String studentLessonServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/StudentLessonServlet.java"));
        String studentSidebar = Files.readString(FRAGMENTS_DIR.resolve("student-dashboard-sidebar.jspf"));
        String studentDashboardStart = Files.readString(FRAGMENTS_DIR.resolve("student-dashboard-start.jspf"));
        String studentClassGroupCard = Files.readString(FRAGMENTS_DIR.resolve("student-class-group-card.jspf"));
        String scripts = Files.readString(FRAGMENTS_DIR.resolve("template-base-scripts.jspf"));
        String autoUpdateScript = Files.readString(ASSETS_DIR.resolve("js/gape-student-enrollment-auto-update.js"));

        assertTrue(servlet.contains("ClassGroupEnrollmentService")
                        && servlet.contains("studentClassGroups")
                        && servlet.contains("\"class-groups\"")
                        && servlet.contains("\"/student/class-groups/*\"")
                        && servlet.contains("showClassGroupDetail")
                        && servlet.contains("canEnrollInClassGroupContext(classGroup, activeSubjectContexts)")
                        && servlet.contains("return activeSubjectContexts.contains(key);"),
                "StudentEnrollmentServlet must load and handle student class group enrollments");
        assertTrue(coursesPage.contains("Courses")
                        && classGroupsPage.contains("Class Group Enrollments")
                        && studentClassGroupCard.contains("/student/enrollments/class-groups/${item.classGroup.id}")
                        && studentClassGroupCard.contains("/student/enrollments/class-groups/${item.classGroup.id}/withdraw"),
                "Student enrollment page must expose class group enrollment and withdrawal actions");
        assertTrue(classGroupsPage.contains("studentClassGroupCourseGroups")
                        && classGroupsPage.contains("gape-student-class-course-group")
                        && classGroupsPage.contains("gape-student-class-subject-group")
                        && classGroupsPage.contains("Open course detail")
                        && classGroupsPage.contains("Open subject detail")
                        && classGroupsPage.contains("data-student-show-unenrolled")
                        && classGroupsPage.contains("col-xxl-4 col-xl-6 col-md-6\" data-student-unenrolled-summary")
                        && classGroupsPage.contains("gape-student-class-group-card--summary")
                        && classGroupsPage.contains("gape-student-card-icon-button")
                        && classGroupsPage.contains("<c:if test=\"${item.enrolled}\">")
                        && classGroupsPage.contains("<c:if test=\"${not item.enrolled}\">")
                        && classGroupsPage.contains("col-xxl-4 col-xl-6 col-md-6")
                        && studentClassGroupCard.contains("data-student-unenrolled-card")
                        && studentClassGroupCard.contains("Modality:")
                        && studentClassGroupCard.contains("Shift:")
                        && studentClassGroupCard.contains("Occup:")
                        && studentClassGroupCard.contains("Dates:")
                        && studentClassGroupCard.contains("unavailableActionLabel")
                        && studentClassGroupCard.contains("gape-student-class-group-card__icon")
                        && studentClassGroupCard.contains("/student/class-groups/${item.classGroup.id}")
                        && studentClassGroupCard.contains("aria-label=\"Open class group\"")
                        && studentClassGroupCard.contains("data-gape-enrollment-actions-kind=\"class-group-list\""),
                "Student class groups page must group larger cards by course and subject with compact icon actions");
        int classGroupCardsRowIndex = classGroupsPage.indexOf("<div class=\"row gy-3\">");
        int enrolledClassGroupCardIndex = classGroupsPage.indexOf("<c:if test=\"${item.enrolled}\">", classGroupCardsRowIndex);
        int availableClassGroupCardIndex = classGroupsPage.indexOf("<c:if test=\"${not item.enrolled}\">", enrolledClassGroupCardIndex);
        assertTrue(classGroupCardsRowIndex >= 0
                        && enrolledClassGroupCardIndex > classGroupCardsRowIndex
                        && availableClassGroupCardIndex > enrolledClassGroupCardIndex,
                "Student class groups page must render enrolled class groups before other available class groups");
        assertTrue(classGroupsPage.indexOf("data-student-unenrolled-summary")
                        > classGroupsPage.indexOf("<%@ include file=\"/WEB-INF/fragments/student-class-group-card.jspf\" %>"),
                "Student class groups summary card must render after the normal class group cards");
        assertTrue(studentDashboardStart.contains("min-height: 48px;")
                        && studentDashboardStart.contains("margin-block-end: 22px !important;")
                        && studentDashboardStart.contains("background: var(--main-50);")
                        && studentDashboardStart.contains("border: 1px solid var(--main-600);")
                        && studentDashboardStart.contains("height: 40px;")
                        && studentDashboardStart.contains(".gape-student-card-actions,")
                        && studentDashboardStart.contains("margin-top: auto;")
                        && studentDashboardStart.contains("border-top: 1px solid var(--neutral-30);")
                        && studentDashboardStart.contains(".gape-student-class-group-card--summary"),
                "Student dashboard styles must keep centered title spacing, Eduall icon buttons and subtle summary cards");
        assertFalse(studentDashboardStart.contains("box-shadow: 0 8px 18px rgba(37, 99, 235, .14);")
                        || studentDashboardStart.contains("linear-gradient(180deg, #ffffff 0%, var(--main-50) 100%)"),
                "Student card buttons must not use the rejected custom blue shadow/gradient style");
        assertFalse(classGroupsPage.contains("Teachers:"),
                "Student class groups page cards must not render teacher counts");
        assertFalse(classGroupsPage.contains("gape-student-class-group-summary-card"),
                "Student class groups summary must use the same card shape as class group cards");
        assertFalse(classGroupsPage.contains("Pedagogical Blocks"),
                "Student class groups page must not render pedagogical blocks inside class group cards");
        assertTrue(subjectDetailPage.contains("col-xxl-3 col-xl-4 col-md-6")
                        && subjectDetailPage.contains("px-14 py-14")
                        && subjectDetailPage.contains("<h6 class=\"text-14 fw-semibold text-neutral-800 mb-4\"><c:out value=\"${item.classGroup.code}\"/></h6>")
                        && subjectDetailPage.contains("/student/enrollments/class-groups/${item.classGroup.id}")
                        && subjectDetailPage.contains("/student/class-groups/${item.classGroup.id}")
                        && subjectDetailPage.contains("aria-label=\"Request enrollment\"")
                        && subjectDetailPage.contains("aria-label=\"Open class group\"")
                        && subjectDetailPage.contains("aria-label=\"Leave class group\"")
                        && subjectDetailPage.contains("Modality:")
                        && subjectDetailPage.contains("Shift:")
                        && subjectDetailPage.contains("Occup:")
                        && subjectDetailPage.contains("data-gape-enrollment-target=\"subject-${course.id}-${subject.subjectId}\"")
                        && subjectDetailPage.contains("data-gape-enrollment-actions-kind=\"class-group-detail\""),
                "Student subject detail must expose compact class group cards with labelled information and icon actions");
        assertFalse(classGroupsPage.contains("Capacity:")
                        || classGroupsPage.contains("capacityLabel")
                        || subjectDetailPage.contains("Capacity:")
                        || subjectDetailPage.contains("capacityLabel"),
                "Student class group cards must not render Capacity; Occupancy is the student-facing availability indicator");
        assertFalse(subjectDetailPage.contains("Dates:")
                        || subjectDetailPage.contains("Teachers:"),
                "Student subject detail class group cards must not render dates or teacher counts");
        assertFalse(subjectDetailPage.contains("Pedagogical Blocks"),
                "Student subject detail class group cards must not render pedagogical blocks");
        assertFalse(subjectDetailPage.contains(">Withdraw<"),
                "Student-facing enrollment actions must not use the Withdraw label");
        assertTrue(classGroupDetailPage.contains("Study Path")
                        && classGroupDetailPage.contains("gape-student-structure-board"),
                "Student class group detail page must exist with student-focused content");
        assertTrue(servlet.contains("studentContentBlocks(classGroup.id())")
                        && servlet.contains("contentBlockDAO.findByClassGroup(classGroupId)")
                        && servlet.contains(".filter(ContentBlockView::isActive)")
                        && !servlet.contains("isVisibleToStudent("),
                "Student class group detail must load only active block structure without a pre-JSP visibility helper");
        assertTrue(classGroupDetailPage.contains("data-gape-enrollment-target=\"class-group-${classGroup.id}\"")
                        && classGroupDetailPage.contains("data-gape-enrollment-actions-kind=\"class-group-detail-page\"")
                        && classGroupDetailPage.contains("Open structure")
                        && classGroupDetailPage.contains("gape-student-activity-row")
                        && classGroupDetailPage.contains("lesson.hasMeetingLink")
                        && classGroupDetailPage.contains("/student/lessons/${lesson.id}/access")
                        && classGroupDetailPage.contains("lesson.attendanceLabel")
                        && !classGroupDetailPage.contains("data-bs-toggle=\"collapse\"")
                        && !classGroupDetailPage.contains("href=\"${fn:escapeXml(lesson.accessUrl)}\"")
                        && classGroupDetailPage.contains("studentContentInlineUrl")
                        && classGroupDetailPage.contains("studentContentDownloadUrl")
                        && classGroupDetailPage.contains("blockActivitiesByBlock"),
                "Student class group detail must expose enrollment status, inline lesson details and visible learning materials");
        assertTrue(studentSidebar.contains("studentClassGroupContextId")
                        && studentSidebar.contains("/student/class-groups/${studentClassGroupContextId}")
                        && studentSidebar.contains("class-group-detail"),
                "Student sidebar must mark class group detail pages like other detail pages");
        int studentMessageMenuIndex = studentSidebar.indexOf("data-menu-key=\"message\"");
        int studentCalendarMenuIndex = studentSidebar.indexOf("data-menu-key=\"calendar\"", studentMessageMenuIndex);
        int studentCoursesMenuIndex = studentSidebar.indexOf("data-menu-key=\"courses\"", studentCalendarMenuIndex);
        assertTrue(studentMessageMenuIndex >= 0
                        && studentCalendarMenuIndex > studentMessageMenuIndex
                        && studentCoursesMenuIndex > studentCalendarMenuIndex,
                "Student sidebar must render Calendar immediately after Message and before Courses");
        assertTrue(studentLessonsPage.contains("lessonCourseGroups")
                        && studentLessonsPage.contains("courseGroup.subjectGroups")
                        && studentLessonsPage.contains("subjectGroup.lessons")
                        && studentLessonsPage.contains("col-xxl-4 col-xl-6 col-md-6")
                        && studentLessonsPage.contains("gape-student-card gape-student-card--actionable px-18 py-18")
                        && studentLessonsPage.contains("gape-student-card-actions")
                        && studentLessonsPage.contains("w-40 h-40 rounded-10")
                        && studentLessonsPage.contains("lesson.dateRangeLabel")
                        && studentLessonsPage.contains("(lesson.online or lesson.hybrid) and lesson.hasMeetingLink")
                        && studentCalendarPage.contains("(lesson.online or lesson.hybrid) and lesson.hasMeetingLink")
                        && studentLessonServlet.contains("lessonCourseGroups(lessons, classGroupById)")
                        && studentLessonsPage.contains("/student/lessons/${lesson.id}/access")
                        && studentCalendarPage.contains("/student/lessons/${lesson.id}/access")
                        && studentLessonServlet.contains("openMeetingAccess")
                        && studentLessonServlet.contains("lessonService.getLesson")
                        && !studentLessonsPage.contains("href=\"${fn:escapeXml(lesson.accessUrl)}\"")
                        && !studentCalendarPage.contains("href=\"${fn:escapeXml(lesson.accessUrl)}\"")
                        && !studentLessonServlet.contains("studentLessonContextId")
                        && !studentSidebar.contains("studentLessonContextId")
                        && !studentSidebar.contains("lesson-detail"),
                "Student lesson access must group lessons by course and subject and use the audited meeting access endpoint without exposing a student lesson detail page");
        assertFalse(servlet.contains("openClassGroupContexts")
                        || servlet.contains("hasOpenEnrollmentInContext(")
                        || servlet.contains("hasOverlappingActiveEnrollment(")
                        || classGroupsPage.contains("Already in another group")
                        || subjectDetailPage.contains("Already in another group"),
                "Students must be allowed to request more than one class group in the same course subject context");
        assertTrue(courseDetailPage.contains("data-gape-enrollment-actions-kind=\"subject-open-compact\"")
                        && subjectsPage.contains("data-gape-enrollment-actions-kind=\"subject-open\"")
                        && studentClassGroupCard.contains("data-gape-enrollment-actions-kind=\"class-group-list\"")
                        && coursesPage.contains("gape-student-card-actions")
                        && subjectsPage.contains("gape-student-card-actions")
                        && courseDetailPage.contains("gape-student-card-actions"),
                "Student enrollment cards must expose local update targets for each page context");
        assertFalse(subjectsPage.contains("gape-student-card-icon-button--primary")
                        || servlet.contains("gape-student-card-icon-button--primary")
                        || studentDashboardStart.contains("gape-student-card-icon-button--primary")
                        || courseDetailPage.contains("gape-student-card-icon-button--primary")
                        || classGroupsPage.contains("gape-student-card-icon-button--primary")
                        || studentClassGroupCard.contains("gape-student-card-icon-button--primary")
                        || subjectDetailPage.contains("gape-student-card-icon-button--primary")
                        || studentLessonsPage.contains("gape-student-card-icon-button--primary")
                        || classGroupDetailPage.contains("gape-student-card-icon-button--primary")
                        || studentCalendarPage.contains("gape-student-card-icon-button--primary"),
                "Student action buttons must use the light Eduall icon button style instead of the filled primary variant");
        assertTrue(studentDashboardStart.contains(".gape-student-card-icon-button--request")
                        && studentDashboardStart.contains(".gape-student-card-icon-button--meeting")
                        && studentDashboardStart.contains(".gape-student-card-icon-button--download")
                        && studentDashboardStart.contains("background: var(--success-50);")
                        && studentDashboardStart.contains("background: #f3ecff;")
                        && studentDashboardStart.contains("background: var(--warning-50);")
                        && subjectsPage.contains("gape-student-card-icon-button--request")
                        && courseDetailPage.contains("gape-student-card-icon-button--request")
                        && studentClassGroupCard.contains("gape-student-card-icon-button--request")
                        && subjectDetailPage.contains("gape-student-card-icon-button--request")
                        && classGroupDetailPage.contains("gape-student-card-icon-button--request")
                        && studentLessonsPage.contains("gape-student-card-icon-button--meeting")
                        && classGroupDetailPage.contains("gape-student-card-icon-button--meeting")
                        && studentCalendarPage.contains("gape-student-card-icon-button--meeting")
                        && classGroupDetailPage.contains("gape-student-card-icon-button--download")
                        && servlet.contains("gape-student-card-icon-button gape-student-card-icon-button--request"),
                "Student card action buttons must use distinct Eduall color variants by action type");
        assertTrue(servlet.contains("writeStudentEnrollmentResponse")
                        && servlet.contains("subjectEnrollmentUpdates")
                        && servlet.contains("classGroupEnrollmentUpdates")
                        && servlet.contains("\"updates\"")
                        && servlet.contains("synchronizeEnrollmentTargets"),
                "StudentEnrollmentServlet must return structured Ajax updates after enrollment actions and polling");
        assertTrue(scripts.contains("gape-student-enrollment-auto-update.js")
                        && autoUpdateScript.contains("/student/enrollments/")
                        && autoUpdateScript.contains("/student/enrollments/updates")
                        && autoUpdateScript.contains("fetch(studentEnrollmentActionUrl(form.action)")
                        && autoUpdateScript.contains("syncVisibleEnrollmentTargets")
                        && autoUpdateScript.contains("setInterval(syncVisibleEnrollmentTargets")
                        && autoUpdateScript.contains(".gape-student-dashboard-main")
                        && autoUpdateScript.contains("response.json()")
                        && autoUpdateScript.contains("applyEnrollmentUpdates")
                        && autoUpdateScript.contains("data-gape-enrollment-target")
                        && autoUpdateScript.contains("new URLSearchParams(new FormData(form))")
                        && autoUpdateScript.contains("application/x-www-form-urlencoded")
                        && autoUpdateScript.contains(";jsessionid="),
                "Student enrollment forms must update local enrollment targets after Ajax submission");
        assertFalse(autoUpdateScript.contains("currentMain.innerHTML = nextMain.innerHTML")
                        || autoUpdateScript.contains("window.location.reload()"),
                "Student enrollment Ajax must not refresh or replace the full student page");
    }

    @Test
    void courseDetailOrdersStudentsBeforeSubjectsAndAvoidsDuplicateCourseContext() throws IOException {
        String courseDetail = Files.readString(WEBAPP_DIR.resolve("admin/admin/course/admin-course-detail.jsp"));

        int studentsSection = courseDetail.indexOf("course-enrollment-management.jspf");
        int subjectsSection = courseDetail.indexOf(">Subjects</h3>");
        assertTrue(studentsSection > 0 && subjectsSection > 0,
                "Course detail must render both Students and Subjects sections");
        assertTrue(studentsSection < subjectsSection,
                "Course detail must render Students before Subjects");
        assertTrue(courseDetail.contains("${course.courseManagementContextHtml}"),
                "Course detail subjects must render the course context without duplicating the subject/course chain");
        assertFalse(courseDetail.contains("subjectManagementContextHtml"),
                "Course detail subjects must not use the subject management context because it duplicates the course");
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
                        "instructor-dashbord.jsp",
                        "student-dashboard.jsp"
                );
                List<String> unexpectedNames = stream
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .filter(name -> name.endsWith(".jsp"))
                        .filter(name -> !allowedDashbordNames.contains(name)
                                && (name.contains("dashboard")
                                || name.contains("ashboard")
                                || name.contains("dashbord")))
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

                    if (!hasDashboardSidebar(html)
                            || pageName.endsWith("-alt-home.jsp")
                            || redirectsBeforeRendering(html)) {
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

    private static boolean redirectsBeforeRendering(String html) {
        return html.contains("response.sendRedirect(request.getContextPath()")
                && html.contains("return;");
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
        assertTrue(form.contains("MANAGE_ORGANIZATION_STRUCTURE"),
                "User form must expose the organization structure administrator permission group");
        assertFalse(form.contains("MANAGE_LEARNING") || form.contains("MANAGE_ENROLLMENTS"),
                "User form must not expose Learning or Enrollment as administrator permissions");
    }

    @Test
    void associationFormsExposeDeleteActions() throws IOException {
        String courseSubjectForm = Files.readString(WEBAPP_DIR.resolve("admin/admin/course/admin-course-subject-form.jsp"));
        String subjectCourseAssociations = Files.readString(FRAGMENTS_DIR.resolve("subject-course-associations-panel.jspf"));
        String subjectList = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subjects.jsp"));

        assertTrue(courseSubjectForm.contains("/subjects/${association.subjectId}/delete")
                        && courseSubjectForm.contains("gape-action-delete"),
                "Associate Subject must allow deleting existing subject associations");
        assertTrue(subjectCourseAssociations.contains("/courses/${association.courseId}/delete")
                        && subjectCourseAssociations.contains("gape-action-delete")
                        && subjectCourseAssociations.contains("name=\"approvalMode\""),
                "Course Associations panel must allow deleting existing course associations and editing enrollment approval policy");
        assertTrue(subjectList.contains("canManageSubjectAssociationsById")
                        && subjectList.contains("canManageSubjectAssociationsRow")
                        && subjectList.contains("${subjectBasePath}/${subject.id}/edit#course-associations"),
                "Subjects list must expose course association actions through Edit Subject Course Associations");
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
        String subjectCourseAssociations = Files.readString(FRAGMENTS_DIR.resolve("subject-course-associations-panel.jspf"));

        assertTrue(coordinatorHome.contains("<jsp:include page=\"/admin/admin-dashbord.jsp\"")
                        && coordinatorMessage.contains("<jsp:include page=\"/admin/admin-message.jsp\"")
                        && coordinatorProfile.contains("<jsp:include page=\"/admin/admin-my-profile.jsp\""),
                "Coordinator shared root pages must reuse the administrator templates");
        assertTrue(coordinatorQuizAttempts.contains("response.sendRedirect(request.getContextPath() + \"/learning/assessments\")")
                        && coordinatorReviews.contains("<jsp:include page=\"/admin/admin-reviews.jsp\""),
                "Coordinator shared root pages must route assessments to the real flow and reuse the administrator review template");
        assertTrue(sidebar.contains("/coordinator/subjects")
                        && sidebar.contains("coordinatorSubjectContextId")
                        && sidebar.contains("coordinatorSubjectActiveChild"),
                "Coordinator sidebar must expose the admin-like subject navigation under the coordinator route");
        assertTrue(sidebar.contains("not isCoordinatorDashboard")
                        && sidebar.contains("/coordinator/courses"),
                "Coordinator sidebar must expose coordinator-scoped courses without exposing the generic courses tab");
        assertTrue(sidebar.contains("coordinator/coordinator-reviews.jsp")
                        && sidebar.contains("/learning/assessments")
                        && sidebar.contains("${reviewsHref}")
                        && sidebar.contains("${quizAttemptsHref}"),
                "Coordinator sidebar must route reviews to coordinator pages and assessments to the real assessment flow");
        int coordinatorSection = sidebar.indexOf(">Coordinator<");
        int coordinatorCoursesLink = sidebar.indexOf("/coordinator/courses", coordinatorSection);
        int coordinatorSubjectsLink = sidebar.indexOf("/coordinator/subjects", coordinatorCoursesLink);
        int coordinatorClassGroupsLink = sidebar.indexOf("/learning/class-groups", coordinatorSubjectsLink);
        int coordinatorLessonsLink = sidebar.indexOf("/learning/lessons", coordinatorClassGroupsLink);
        int messageHref = sidebar.indexOf("${messageHref}");
        int calendarHref = sidebar.indexOf("${calendarHref}", messageHref);
        int roomsHref = sidebar.indexOf("/learning/rooms", calendarHref);
        int quizAttemptsHref = sidebar.indexOf("${quizAttemptsHref}", coordinatorLessonsLink);
        int reviewsHref = sidebar.indexOf("${reviewsHref}", quizAttemptsHref);
        int attendanceHref = sidebar.indexOf("${attendanceHref}", reviewsHref);
        assertTrue(messageHref < calendarHref
                        && calendarHref < roomsHref
                        && roomsHref < coordinatorSection
                        && coordinatorSection < coordinatorCoursesLink
                        && coordinatorCoursesLink < coordinatorSubjectsLink
                        && coordinatorSubjectsLink < coordinatorClassGroupsLink
                        && coordinatorClassGroupsLink < coordinatorLessonsLink
                        && coordinatorLessonsLink < quizAttemptsHref
                        && quizAttemptsHref < reviewsHref
                        && reviewsHref < attendanceHref,
                "Coordinator sidebar must keep rooms after calendar and attendance after reviews");
        assertTrue(subjectList.contains("${subjectBasePath}/${subject.id}")
                        && subjectDetail.contains("${subjectBasePath}/${subject.id}/edit")
                        && subjectForm.contains("subject-course-associations-panel.jspf")
                        && subjectDetail.contains("subject-course-associations-panel.jspf")
                        && subjectCourseAssociations.contains("${subjectBasePath}/${subject.id}/courses")
                        && subjectCourseAssociations.contains("${subjectCourseBasePath}/${association.courseId}"),
                "Shared subject views must use the request-scoped base path for admin and coordinator routes");
        assertTrue(subjectForm.contains("not creating and canAssignSubjectCoordinators"),
                "Subject edit form must hide coordinator assignment outside administrator context");
    }

    @Test
    void sidebarShowsRoomsAsManagementMenu() throws IOException {
        String sidebar = Files.readString(FRAGMENTS_DIR.resolve("dashboard-sidebar.jspf"));

        int messageHref = sidebar.indexOf("${messageHref}");
        int calendarHref = sidebar.indexOf("${calendarHref}", messageHref);
        int roomsHref = sidebar.indexOf("/learning/rooms", calendarHref);
        int reviewsHref = sidebar.indexOf("${reviewsHref}", calendarHref);
        int attendanceHref = sidebar.indexOf("${attendanceHref}", reviewsHref);
        assertTrue(sidebar.contains("/learning/calendar")
                        && sidebar.contains("/student/calendar")
                        && sidebar.contains("Calendar")
                        && messageHref < calendarHref
                        && calendarHref < roomsHref
                        && roomsHref < reviewsHref
                        && reviewsHref < attendanceHref,
                "Sidebar must expose Calendar after Message, Rooms after Calendar and Attendance after Reviews");
        assertTrue(sidebar.contains("/learning/rooms"),
                "Sidebar must expose the physical room management route");
        assertTrue(sidebar.contains("Rooms"),
                "Physical room management must be labelled as Rooms in the sidebar");
        assertFalse(sidebar.contains("Physical Rooms"),
                "Physical room management must not remain as a separately named Physical Rooms menu");
        assertTrue(sidebar.contains("or isCoordinatorDashboard or isTeacherDashboard"),
                "Rooms menu visibility must include coordinator and teacher dashboards");

        int coordinatorSection = sidebar.indexOf(">Coordinator<");
        int coordinatorCourses = sidebar.indexOf("/coordinator/courses", coordinatorSection);
        int coordinatorSubjects = sidebar.indexOf("/coordinator/subjects", coordinatorCourses);
        int coordinatorClassGroups = sidebar.indexOf("/learning/class-groups", coordinatorSubjects);
        int coordinatorLessons = sidebar.indexOf("/learning/lessons", coordinatorClassGroups);
        int teacherSection = sidebar.indexOf(">Teacher<");
        assertTrue(coordinatorSection > 0
                        && coordinatorCourses > coordinatorSection
                        && coordinatorSubjects > coordinatorCourses
                        && coordinatorClassGroups > coordinatorSubjects
                        && coordinatorLessons > coordinatorClassGroups
                        && coordinatorLessons < teacherSection,
                "Coordinator sidebar must show Courses, Subjects, Class Groups and Lessons in order");

        int teacherSubjects = sidebar.indexOf("/instructor/subjects", teacherSection);
        int teacherClassGroups = sidebar.indexOf("/learning/class-groups", teacherSubjects);
        int teacherLessons = sidebar.indexOf("/learning/lessons", teacherClassGroups);
        int adminSection = sidebar.indexOf(">Admin<");
        assertTrue(teacherSection > 0
                        && teacherSubjects > teacherSection
                        && teacherClassGroups > teacherSubjects
                        && teacherLessons > teacherClassGroups
                        && teacherLessons < adminSection,
                "Teacher sidebar must show Subjects, Class Groups and Lessons in order");

        int adminUsers = sidebar.indexOf("/admin/users", adminSection);
        int adminOrganizations = sidebar.indexOf("/admin/organizations", adminUsers);
        int adminCourses = sidebar.indexOf("/admin/courses", adminOrganizations);
        int adminSubjects = sidebar.indexOf("/admin/subjects", adminCourses);
        int adminClassGroups = sidebar.indexOf("/learning/class-groups", adminSubjects);
        int adminLessons = sidebar.indexOf("/learning/lessons", adminClassGroups);
        assertTrue(adminSection > 0
                        && adminUsers > adminSection
                        && adminOrganizations > adminUsers
                        && adminCourses > adminOrganizations
                        && adminSubjects > adminCourses
                        && adminClassGroups > adminSubjects
                        && adminLessons > adminClassGroups,
                "Admin sidebar must keep Users, Organizations, Courses, Subjects, Class Groups and Lessons in order");
    }

    @Test
    void scheduleAttendanceFrontendUsesServicesAndMenus() throws IOException {
        String sidebar = Files.readString(FRAGMENTS_DIR.resolve("dashboard-sidebar.jspf"));
        String studentSidebar = Files.readString(FRAGMENTS_DIR.resolve("student-dashboard-sidebar.jspf"));
        String lessonList = Files.readString(WEBAPP_DIR.resolve("WEB-INF/views/learning/lesson-list.jsp"));
        String studentCalendar = Files.readString(WEBAPP_DIR.resolve("student/student/calendar/student-calendar.jsp"));
        String learningAttendance = Files.readString(WEBAPP_DIR.resolve("WEB-INF/views/learning/attendance.jsp"));
        String studentAttendance = Files.readString(WEBAPP_DIR.resolve("student/student/attendance/student-attendance.jsp"));
        String lessonServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/LessonManagementServlet.java"));
        String studentLessonServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/StudentLessonServlet.java"));
        String attendanceServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/AttendanceManagementServlet.java"));
        String attendanceService = Files.readString(JAVA_DIR.resolve("pt/isel/gape/learning/service/AttendanceRecordService.java"));
        String attachmentStorage = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/media/JustificationAttachmentStorage.java"));
        String csrfFilter = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/filter/CsrfFilter.java"));

        assertTrue(sidebar.contains("/learning/attendance")
                        && sidebar.contains("/student/attendance")
                        && sidebar.contains("Attendance")
                        && studentSidebar.contains("data-menu-key=\"attendance\"")
                        && studentSidebar.contains("/student/attendance"),
                "Attendance routes must be exposed in dashboard and student menus");
        assertTrue(lessonList.contains("scheduleEvents")
                        && lessonList.contains("/learning/calendar")
                        && lessonList.contains("classGroupIds")
                        && lessonList.contains("data-class-group-picker")
                        && lessonList.contains("gape-class-group-picker__panel")
                        && lessonList.contains("<ul class=\"list-unstyled")
                        && lessonList.contains("<li>")
                        && lessonList.contains("type=\"checkbox\" name=\"classGroupIds\"")
                        && lessonList.contains("<option value=\"lesson\">Lesson</option>")
                        && lessonList.contains("<option value=\"assessment\">Assessment</option>")
                        && lessonList.contains("name=\"lessonId\"")
                        && lessonList.contains("name=\"assessmentId\"")
                        && lessonServlet.contains("ScheduleEventService")
                        && lessonServlet.contains("createScheduleEvent")
                        && lessonServlet.contains("listVisibleEvents")
                        && !lessonServlet.contains("new SelectOptionView(\"questionnaire\"")
                        && !lessonServlet.contains("new SelectOptionView(\"exam\""),
                "Learning calendar must display and create schedule events through ScheduleEventService");
        assertFalse(lessonList.contains("select id=\"schedule-event-class-groups\"")
                        || lessonList.contains("name=\"classGroupIds\" multiple"),
                "Learning calendar must keep Class Groups inside the compact checkbox list, not a visible multi-select");
        assertTrue(studentCalendar.contains("calendarItems")
                        && studentCalendar.contains("item.eventItem")
                        && studentCalendar.contains("(lesson.online or lesson.hybrid) and lesson.hasMeetingLink")
                        && studentCalendar.contains("/student/lessons/${lesson.id}/access")
                        && studentLessonServlet.contains("ScheduleEventService")
                        && studentLessonServlet.contains("listVisibleEvents")
                        && studentLessonServlet.contains("calendarItems(lessons, scheduleEvents)"),
                "Student calendar must combine schedule events and lessons in one safe ordered timeline");
        assertTrue(attendanceServlet.contains("AttendanceRecordService")
                        && attendanceServlet.contains("AbsenceJustificationService")
                        && attendanceServlet.contains("@MultipartConfig")
                        && attendanceServlet.contains("JustificationAttachmentStorage")
                        && attendanceServlet.contains("attendanceCreationStatusOptions")
                        && attendanceServlet.contains("attendanceCreationSourceOptions")
                        && attendanceServlet.contains("recordAttendance")
                        && attendanceServlet.contains("submitJustification")
                        && attendanceServlet.contains("processJustification")
                        && attendanceService.contains("AttendanceStatus.JUSTIFIED")
                        && attendanceService.contains("AttendanceSource.AUTOMATIC")
                        && learningAttendance.contains("data-default-value")
                        && learningAttendance.contains("attendanceCreateStatusOptions")
                        && learningAttendance.contains("/learning/attendance/justifications/${justification.id}/approve")
                        && learningAttendance.contains("/learning/attendance/justifications/${justification.id}/reject")
                        && studentAttendance.contains("/student/attendance/justifications")
                        && studentAttendance.contains("enctype=\"multipart/form-data\"")
                        && studentAttendance.contains("type=\"file\"")
                        && studentAttendance.contains("name=\"attachmentFile\"")
                        && attachmentStorage.contains("justifications/")
                        && attachmentStorage.contains("ALLOWED_EXTENSIONS")
                        && csrfFilter.contains("\"/student/attendance\""),
                "Attendance, justification submission and processing must be wired to services and CSRF-protected routes");
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
        assertTrue(instructorQuizAttempts.contains("response.sendRedirect(request.getContextPath() + \"/learning/assessments\")")
                        && instructorReviews.contains("<jsp:include page=\"/admin/admin-reviews.jsp\""),
                "Instructor shared root pages must route assessments to the real flow and reuse the administrator review template");
        assertTrue(sidebar.contains("/instructor/instructor-message.jsp")
                        && sidebar.contains("/instructor/subjects")
                        && sidebar.contains("teacherSubjectContextId")
                        && sidebar.contains("/instructor/instructor-reviews.jsp")
                        && sidebar.contains("/learning/assessments")
                        && sidebar.contains("${messageHref}")
                        && sidebar.contains("${reviewsHref}")
                        && sidebar.contains("${quizAttemptsHref}"),
                "Instructor sidebar must route shared pages to instructor wrappers and assessments to the real assessment flow");
        assertTrue(sidebar.contains("not isTeacherDashboard"),
                "Instructor sidebar must not expose the generic courses tab");
        int teacherSection = sidebar.indexOf(">Teacher<");
        assertTrue(teacherSection > 0
                        && sidebar.indexOf("Subjects", teacherSection) > teacherSection
                        && sidebar.indexOf("Class Groups", teacherSection) > sidebar.indexOf("Subjects", teacherSection),
                "Instructor sidebar must show subjects and class groups under the Teacher section");
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
        assertTrue(organizationList.contains("data-gape-tree-toggle=\"organizationUnits${organization.id}\"")
                        && organizationList.contains("data-gape-tree-toggle=\"unitCourses${unit.id}\"")
                        && organizationList.contains("data-gape-tree-toggle=\"courseSubjects${course.id}\"")
                        && organizationList.contains("data-gape-tree-toggle=\"subjectClassGroups${course.id}_${subject.subjectId}\"")
                        && organizationList.contains("items=\"${unit.courses}\"")
                        && organizationList.contains("items=\"${course.subjects}\"")
                        && organizationList.contains("items=\"${subject.classGroups}\""),
                "Organization list must expose show-more toggles for units, courses, subjects and class groups");
        assertFalse(organizationList.contains("gape-organization-units-row"),
                "Organic units must not render as a separate table row");
        String organizationServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/OrganizationManagementServlet.java"));
        assertTrue(organizationServlet.contains("courseTreesByOrganicUnit")
                        && organizationServlet.contains("OrganizationCourseTreeView")
                        && organizationServlet.contains("OrganizationSubjectTreeView")
                        && organizationServlet.contains("OrganizationClassGroupTreeView"),
                "Organization servlet must build the organization tree down to class groups");
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
