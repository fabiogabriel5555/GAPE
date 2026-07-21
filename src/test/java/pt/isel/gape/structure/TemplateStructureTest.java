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
            "login.jsp",
            "admin/admin-dashbord.jsp",
            "admin/admin/user/admin-users.jsp",
            "admin/admin/user/admin-user-form.jsp",
            "admin/admin/user/admin-user-detail.jsp",
            "admin/admin/user/admin-deletion-requests.jsp",
            "admin/admin/organization/admin-organizations.jsp",
            "admin/admin/organization/admin-organization-form.jsp",
            "admin/admin/organization/admin-organization-detail.jsp",
            "admin/admin/organization/admin-organic-unit-detail.jsp",
            "admin/admin/organization/admin-organic-unit-form.jsp",
            "admin/admin-my-profile.jsp",
            "coordinator/coordinator-my-profile.jsp",
            "coordinator/coordinator/subject/coordinator-subjects.jsp",
            "coordinator/coordinator/subject/coordinator-subject-form.jsp",
            "coordinator/coordinator/subject/coordinator-subject-detail.jsp",
            "instructor/instructor-my-profile.jsp",
            "WEB-INF/views/public/course-catalog.jsp",
            "WEB-INF/views/public/course-detail.jsp",
            "WEB-INF/views/transversal/messages.jsp",
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
    void legacyRootJspAliasesAreRemovedAfterViewMigration() {
        for (String page : List.of(
                "courses.jsp",
                "course-details.jsp",
                "course.jsp",
                "course-list-view.jsp",
                "messages.jsp",
                "dashbord.jsp"
        )) {
            assertFalse(Files.exists(WEBAPP_DIR.resolve(page)),
                    () -> "Legacy root JSP must not remain after migration: " + page);
        }
    }

    @Test
    void coordinatorPagesFollowAdministratorStructure() throws Exception {
        Path coordinatorDir = WEBAPP_DIR.resolve("coordinator");
        List<String> sharedRootPages = List.of(
                "coordinator-my-profile.jsp"
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

        for (String page : List.of(
                "course/coordinator-courses.jsp",
                "assessment/coordinator-assessments.jsp",
                "attendance/coordinator-attendance.jsp",
                "lesson/coordinator-lessons.jsp",
                "room/coordinator-rooms.jsp",
                "grade/coordinator-grades-certificates.jsp",
                "message/coordinator-messages.jsp",
                "dashboard/coordinator-dashboard.jsp"
        )) {
            assertTrue(Files.isRegularFile(coordinatorDir.resolve("coordinator").resolve(page)),
                    () -> "Coordinator must own its management page: " + page);
        }
    }

    @Test
    void instructorPagesFollowAdministratorStructure() throws Exception {
        Path instructorDir = WEBAPP_DIR.resolve("instructor");
        List<String> sharedRootPages = List.of(
                "instructor-my-profile.jsp"
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
    void everyActorOwnsItsManagementPages() throws IOException {
        Map<Path, List<String>> actorPages = Map.of(
                WEBAPP_DIR.resolve("coordinator/coordinator"), List.of(
                        "course/coordinator-courses.jsp",
                        "subject/coordinator-subjects.jsp",
                        "class-group/coordinator-class-groups.jsp",
                        "lesson/coordinator-lessons.jsp",
                        "assessment/coordinator-assessments.jsp",
                        "attendance/coordinator-attendance.jsp",
                        "room/coordinator-rooms.jsp",
                        "grade/coordinator-grades-certificates.jsp",
                        "message/coordinator-messages.jsp",
                        "dashboard/coordinator-dashboard.jsp"
                ),
                WEBAPP_DIR.resolve("instructor/instructor"), List.of(
                        "subject/instructor-subjects.jsp",
                        "class-group/instructor-class-groups.jsp",
                        "lesson/instructor-lessons.jsp",
                        "assessment/instructor-assessments.jsp",
                        "attendance/instructor-attendance.jsp",
                        "room/instructor-rooms.jsp",
                        "grade/instructor-grades-certificates.jsp",
                        "message/instructor-messages.jsp",
                        "dashboard/instructor-dashboard.jsp"
                ),
                WEBAPP_DIR.resolve("student/student"), List.of(
                        "dashboard/student-dashboard.jsp",
                        "lesson/student-lessons.jsp",
                        "attendance/student-attendance.jsp",
                        "assessment/student-assessment-attempt.jsp",
                        "assessment/student-assessment-result.jsp",
                        "grade/student-grades-certificates.jsp",
                        "message/student-messages.jsp"
                )
        );
        for (Map.Entry<Path, List<String>> entry : actorPages.entrySet()) {
            for (String relativePage : entry.getValue()) {
                Path page = entry.getKey().resolve(relativePage);
                assertTrue(Files.isRegularFile(page), () -> "Expected actor-owned page: " + page);
                assertFalse(Files.readString(page).contains("/admin/"),
                        () -> "Actor page must not depend on an administrator page: " + page);
            }
        }

        String courseServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/CourseManagementServlet.java"));
        String subjectServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/SubjectManagementServlet.java"));
        String classGroupServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/ClassGroupManagementServlet.java"));
        String lessonServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/LessonManagementServlet.java"));
        String assessmentServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/AssessmentManagementServlet.java"));
        String attendanceServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/AttendanceManagementServlet.java"));
        String roomServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/PhysicalRoomManagementServlet.java"));
        String messageServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/CommunicationServlet.java"));
        assertTrue(courseServlet.contains("coordinator/coordinator/course/coordinator-courses.jsp")
                        && subjectServlet.contains("instructor/instructor/subject/instructor-subjects.jsp")
                        && classGroupServlet.contains("COORDINATOR_CLASS_GROUP_FORM_JSP")
                        && lessonServlet.contains("INSTRUCTOR_LESSON_LIST_JSP")
                        && assessmentServlet.contains("COORDINATOR_ASSESSMENT_LIST_JSP")
                        && attendanceServlet.contains("INSTRUCTOR_ATTENDANCE_JSP")
                        && roomServlet.contains("COORDINATOR_ROOM_LIST_JSP")
                        && messageServlet.contains("STUDENT_MESSAGES_JSP"),
                "Each actor must be routed to its own named page namespace");
    }

    @Test
    void classGroupFormsKeepCourseAndSubjectImmutableDuringEdit() throws IOException {
        Path adminFormPath = WEBAPP_DIR.resolve("admin/admin/class-group/admin-class-group-form.jsp");
        List<Path> formPaths = List.of(adminFormPath);
        List<Path> profileFormWrappers = List.of(
                WEBAPP_DIR.resolve("coordinator/coordinator/class-group/coordinator-class-group-form.jsp"),
                WEBAPP_DIR.resolve("instructor/instructor/class-group/instructor-class-group-form.jsp")
        );
        String contextScript = Files.readString(WEBAPP_DIR.resolve("assets/js/gape-class-group-context.js"));
        String mainCss = Files.readString(WEBAPP_DIR.resolve("assets/css/main.css"));

        assertTrue(contextScript.contains("data-class-group-context-form")
                        && contextScript.contains("data-class-group-course")
                        && contextScript.contains("data-class-group-subject")
                        && contextScript.contains("courseHasCreationContext")
                        && contextScript.contains("selectedCourseUnavailable")
                        && contextScript.contains("subject.disabled = !selectedCourseReady || count === 0")
                        && contextScript.contains("gape-class-group-course-option-main")
                        && contextScript.contains("gape-class-group-course-option-context")
                        && contextScript.contains("gape-class-group-context-option-muted")
                        && contextScript.contains("matcher: contextMatcher")
                        && contextScript.contains("data.element.hidden)")
                        && contextScript.contains("option.disabled = !available")
                        && contextScript.contains("data-class-group-locked-subject")
                        && contextScript.contains("select2:opening.gapeClassGroupSelectGuidance")
                        && contextScript.contains("positionDropdownBelow")
                        && contextScript.contains("gape-class-group-unavailable-option"),
                "Class Group context script must enforce Course-first dependent context, keep unavailable choices visible, explain them on hover, open menus below their controls, and support a locked subject context");
        assertTrue(mainCss.contains(".gape-class-group-context-field.is-disabled")
                        && mainCss.contains(".gape-class-group-course-option")
                        && mainCss.contains(".gape-class-group-course-option-context")
                        && mainCss.contains(".gape-class-group-readonly-value")
                        && mainCss.contains(".gape-class-group-readonly-value--locked-subject")
                        && mainCss.contains(".gape-class-group-unavailable-option")
                        && mainCss.contains(".select2-container--default .select2-selection--single .select2-selection__rendered .gape-class-group-context-option:not(.gape-class-group-course-option)")
                        && mainCss.contains(".gape-class-group-context-field .form-control")
                        && mainCss.contains("flex: 1 1 0")
                        && mainCss.contains("grid-template-columns: minmax(0, 1fr) auto")
                        && mainCss.contains("text-overflow: ellipsis")
                        && mainCss.contains("select2-results__option--selected.select2-results__option--selectable:hover .gape-class-group-course-option")
                        && mainCss.contains(".gape-eduall-select-dropdown.gape-class-group-period-dropdown .select2-results__option--disabled")
                        && mainCss.contains(".gape-class-group-period-group-label")
                        && mainCss.contains(".gape-class-group-period-selection")
                        && mainCss.contains("cursor: not-allowed"),
                "Class Group forms must share the same context select visual language used by Lesson and Assessment");
        assertFalse(mainCss.contains(".gape-class-group-period-option"),
                "Course occurrence period child entries must retain the standard simple Select2 row design");

        for (Path formPath : formPaths) {
            String form = Files.readString(formPath);
            assertTrue(form.contains("<c:when test=\"${creating}\">"),
                    () -> "Class group form must only show course/subject selectors while creating: " + formPath);
            assertTrue(form.contains("data-class-group-context-form")
                            && form.contains("data-class-group-course-context")
                            && form.contains("data-class-group-subject-context")
                            && form.contains("data-class-group-course")
                            && form.contains("data-class-group-subject disabled")
                            && form.contains("gape-class-group-context-field opacity-75 is-disabled")
                            && form.contains("data-course-acronym=\"<c:out value='${course.acronym}'/>\"")
                            && form.contains("course.courseManagementContextLabel")
                            && form.contains("data-subject-acronym=\"<c:out value='${association.subjectAcronym}'/>\"")
                            && form.contains("association.subjectAcronym")
                            && form.contains("gape-class-group-context.js"),
                    () -> "Class group create form must use Course-first context selects with course context only: " + formPath);
            assertTrue(form.contains("id=\"courseContext\"")
                            && form.contains("id=\"subjectContext\"")
                            && form.contains("name=\"courseId\" value=\"${form.courseId}\"")
                            && form.contains("name=\"subjectId\" value=\"${form.subjectId}\"")
                            && form.contains("gape-class-group-readonly-value")
                            && form.contains("gape-class-group-readonly-value-context")
                            && form.contains("${classGroup.course.courseManagementContextLabel}"),
                    () -> "Class group edit form must preserve immutable course/subject as readonly context: " + formPath);
            assertFalse(form.contains("data-dependent-select data-parent-select=\"#courseId\""),
                    () -> "Class group context must not use the old loose dependent-select behavior: " + formPath);
            assertTrue(form.contains("courseOccurrencePeriodId")
                            && form.contains("data-class-group-period")
                            && form.contains("data-review-period")
                            && form.contains("data-preview-period"),
                    () -> "Class group form must use the selected occurrence period as its temporal context: " + formPath);
            assertTrue(form.contains("data-start=\"${period.startsAtValue}\"")
                            && form.contains("data-end=\"${period.endsAtValue}\"")
                            && form.contains("<optgroup label=\"<c:out value='${occurrence.label}'/> - <c:out value='${period.label}'/>\"")
                            && form.contains("data-class-group-period-group")
                            && form.contains("data-occurrence-period-label")
                            && form.contains("dropdownCssClass: 'gape-eduall-select-dropdown gape-class-group-period-dropdown'")
                            && form.contains("matcher: periodMatcher")
                            && form.contains("templateResult: periodTemplate")
                            && form.contains("templateSelection: periodSelectionTemplate")
                            && form.contains("syncPeriodGroups()")
                            && form.contains("opacity-75 is-disabled\" data-class-group-period-context")
                            && form.contains("required disabled class=\"form-select")
                            && form.contains("var hasMatchingPeriod = false;")
                            && form.contains("setPeriodFieldState(hasMatchingPeriod)")
                            && form.contains("option.hidden = !matches;")
                            && form.contains("option.disabled = !!unavailableReason;")
                            && form.contains("This occurrence period ended on ")
                            && form.contains("setPeriodUnavailableGuidance")
                            && form.contains("data-gape-period-unavailable")
                            && form.contains("The selected course and subject have no current or future occurrence period.")
                            && form.contains("placement: 'top'")
                            && form.contains("min=\"1\"")
                            && form.contains("maxStudents\" name=\"maxStudents\" type=\"number\" min=\"2\"")
                            && form.contains("Maximum students must be greater than minimum students."),
                    () -> "Class group form must group exact course occurrence periods, keep past compatible periods visible but unavailable, and enforce its required capacity range: " + formPath);
            assertFalse(form.contains("name=\"startsAt\"")
                            || form.contains("name=\"endsAt\"")
                            || form.contains(">Dates<")
                            || form.contains("Start Date")
                            || form.contains("End Date"),
                    () -> "Class group form must not expose free start/end dates: " + formPath);
        }

        for (Path wrapperPath : profileFormWrappers) {
            String wrapper = Files.readString(wrapperPath);
            assertTrue(wrapper.contains("data-class-group-context-form")
                            && !wrapper.contains("/admin/admin/class-group/admin-class-group-form.jsp"),
                    () -> "Profile class group form must be an actor-owned copy of the unified occurrence-period form: " + wrapperPath);
            assertFalse(wrapper.contains("Start Date") || wrapper.contains("End Date"),
                    () -> "Profile class group wrapper must not reintroduce free date fields: " + wrapperPath);
        }
    }

    @Test
    void subjectDetailsNewGroupPreservesItsSubjectContext() throws IOException {
        String subjectDetail = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subject-detail.jsp"));
        String classGroupForm = Files.readString(WEBAPP_DIR.resolve("admin/admin/class-group/admin-class-group-form.jsp"));
        String mainCss = Files.readString(WEBAPP_DIR.resolve("assets/css/main.css"));
        String servlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/ClassGroupManagementServlet.java"));

        assertTrue(subjectDetail.contains("subjectContextId=${subject.id}"),
                "New Group from Subject Details must identify its originating subject");
        assertTrue(classGroupForm.contains("data-class-group-locked-subject")
                        && classGroupForm.contains("data-subject-context-selectable")
                        && classGroupForm.contains("data-subject-curricular-year")
                        && classGroupForm.contains("gape-class-group-readonly-value--locked-subject")
                        && classGroupForm.contains("${subjectCreationContext.acronym}")
                        && classGroupForm.contains("${subjectCreationContext.name}")
                        && !classGroupForm.contains("gape-class-group-readonly-value-caret")
                        && !classGroupForm.contains("gape-class-group-readonly-value--locked-subject form-control fw-normal text-14")
                        && !classGroupForm.contains("Selected from Subject Details"),
                "Subject-context creation must keep the subject fixed, match the selected-subject label, and expose only its associated courses as selectable");
        assertTrue(mainCss.contains(".gape-class-group-readonly-value--locked-subject")
                        && mainCss.contains("font-size: 1rem;"),
                "The fixed subject label must keep the same 16px text size as the Select2 subject field");
        assertTrue(servlet.contains("SubjectCreationContext")
                        && servlet.contains("courseOptionsForSubjectCreation")
                        && servlet.contains("requireSubjectCreationContext")
                        && servlet.contains("Create Class Group on ")
                        && servlet.contains("classGroupPageTitle"),
                "The class-group controller must prepare and enforce the subject-origin context");
    }

    @Test
    void courseFormsUseStrictOrganizationOrganicUnitContext() throws IOException {
        String courseForm = Files.readString(WEBAPP_DIR.resolve("admin/admin/course/admin-course-form.jsp"));
        String courseEnrollmentManagement = Files.readString(FRAGMENTS_DIR.resolve("course-enrollment-management.jspf"));
        String courseEnrollmentCard = Files.readString(FRAGMENTS_DIR.resolve("course-enrollment-card.jspf"));
        String contextScript = Files.readString(WEBAPP_DIR.resolve("assets/js/gape-course-context.js"));
        String mainCss = Files.readString(WEBAPP_DIR.resolve("assets/css/main.css"));
        String courseServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/CourseManagementServlet.java"));

        assertTrue(courseForm.contains("data-course-context-form")
                        && courseForm.contains("data-course-organization-context")
                        && courseForm.contains("data-course-organic-unit-context")
                        && courseForm.contains("data-course-organization")
                        && courseForm.contains("data-course-organic-unit")
                        && courseForm.contains("data-organization-id=\"${unit.organizationId}\"")
                        && courseForm.contains("${selectedOrganizationId != unit.organizationId ? 'hidden disabled' : ''}")
                        && courseForm.contains("${selectedOrganizationId == 0 ? ' opacity-75 is-disabled' : ''}")
                        && courseForm.contains("gape-course-context.js")
                        && courseForm.contains("pattern=\"[^\\|]*\""),
                "Course create/edit form must use strict Organization -> Organic Unit context selects");
        assertFalse(courseForm.contains("data-dependent-select data-parent-select=\"#organizationId\""),
                "Course form must not use the old loose dependent-select behavior for Organic Unit");
        assertFalse(courseForm.contains("js-example-basic-single gape-eduall-select\" data-course-organization")
                        || courseForm.contains("js-example-basic-single gape-eduall-select\" data-course-organic-unit"),
                "Course context selects must not be reinitialized by the generic main.js Select2 setup");

        assertTrue(contextScript.contains("data-course-context-form")
                        && contextScript.contains("data-subject-context-form")
                        && contextScript.contains("var contextType =")
                        && contextScript.contains("form.querySelector('[data-' + contextType")
                        && contextScript.contains("rebuildOrganicUnitOptions")
                        && contextScript.contains("No organic units in selected organization")
                        && contextScript.contains("placeholder.disabled = hasSelectedOrganization && !hasAvailableOptions;")
                        && contextScript.contains("window.setTimeout(function ()")
                        && contextScript.contains("organicUnit.disabled = !selectedOrganizationReady;")
                        && contextScript.contains("syncCourseContext")
                        && contextScript.contains("matcher: contextMatcher")
                        && contextScript.contains("data.element.hidden || data.element.disabled")
                        && contextScript.contains("gape-course-context-option")
                        && contextScript.contains("window.jQuery(option)")
                        && contextScript.contains("Select an organization before selecting an organic unit."),
                "Course context script must enforce Organization-first Organic Unit selection");
        assertTrue(courseServlet.contains("CourseFormData.blank(null)")
                        && courseServlet.contains("private long selectedOrganizationId(HttpServletRequest request, CourseFormData form)")
                        && courseServlet.contains("return 0;")
                        && !courseServlet.contains("firstManagedOrganizationId"),
                "A standalone Course form must start without an implicit Organization selection");
        assertTrue(mainCss.contains(".gape-course-context-field.is-disabled")
                        && mainCss.contains(".gape-course-context-field .form-control")
                        && mainCss.contains(".gape-course-context-option")
                        && mainCss.contains("select2-results__option--selected.select2-results__option--selectable:hover .gape-course-context-option")
                        && mainCss.contains("text-overflow: ellipsis"),
                "Course context selects must share the same disabled and ellipsis visual treatment");
        assertTrue(courseForm.contains("gape-course-frequency-field${empty form.duration ? ' is-disabled' : ''}")
                        && courseForm.contains("frequencyField.classList.toggle('is-disabled', !enabled);"),
                "Only the frequency select must receive the disabled treatment when duration is absent");
        assertFalse(courseForm.contains("gape-course-frequency-field${empty form.duration ? ' opacity-75 is-disabled' : ''}")
                        || courseForm.contains("frequencyField.classList.toggle('opacity-75', !enabled);")
                        || courseForm.contains(".gape-course-frequency-field.is-disabled label"),
                "The Frequency label must remain visually enabled when its select is unavailable");
        assertTrue(courseEnrollmentManagement.contains("gape-course-structure-panel")
                        && courseEnrollmentManagement.contains("gape-enrollment-list-header")
                        && courseEnrollmentManagement.contains("course-enrollment-card.jspf")
                        && courseEnrollmentManagement.contains("newCourseEnrollmentModal")
                        && courseEnrollmentManagement.contains("gape-completed-occurrences-divider")
                        && courseEnrollmentManagement.contains("data-gape-tree-toggle=\"completedCourseEnrollments\"")
                        && courseEnrollmentManagement.contains("completedCourseEnrollments")
                        && courseEnrollmentManagement.contains("Completed enrollments")
                        && courseEnrollmentManagement.contains("#<c:out value=\"${student.id}\"/>")
                        && courseEnrollmentManagement.contains("<c:out value=\"${student.name}\"/>")
                        && courseEnrollmentManagement.contains("<c:out value=\"${student.email}\"/>")
                        && courseEnrollmentManagement.contains("Select occurrence")
                        && courseEnrollmentManagement.contains("occurrence.stateValue != 'scheduled' and occurrence.stateValue != 'active'")
                        && courseEnrollmentManagement.contains("cd-surface px-22 py-22 mb-20")
                        && courseEnrollmentCard.contains("gape-enrollment-row")
                        && courseEnrollmentCard.contains("enrollment.courseOccurrenceCode")
                        && courseEnrollmentCard.contains("enrollment.studentEmail")
                        && courseEnrollmentCard.contains("View enrollment details")
                        && courseEnrollmentCard.contains("courseEnrollmentDetail")
                        && courseEnrollmentCard.contains("Edit enrollment")
                        && !courseEnrollmentCard.contains("option value=\"completed\"")
                        && mainCss.contains(".gape-course-enrollment-table"),
                "Course enrollment management must use the same responsive card structure as Course occurrences");
    }

    @Test
    void subjectFormsUseStrictOrganizationOrganicUnitContext() throws IOException {
        String subjectForm = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subject-form.jsp"));
        String contextScript = Files.readString(WEBAPP_DIR.resolve("assets/js/gape-course-context.js"));
        String subjectServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/SubjectManagementServlet.java"));

        assertTrue(subjectForm.contains("data-subject-context-form")
                        && subjectForm.contains("data-subject-organization-context")
                        && subjectForm.contains("data-subject-organic-unit-context")
                        && subjectForm.contains("data-subject-organization")
                        && subjectForm.contains("data-subject-organic-unit")
                        && subjectForm.contains("name=\"organicUnitId\"")
                        && subjectForm.contains("data-organization-id=\"${unit.organizationId}\"")
                        && subjectForm.contains("${selectedOrganizationId != unit.organizationId ? 'hidden disabled' : ''}")
                        && subjectForm.contains("organicUnitOptions")
                        && subjectForm.contains("gape-course-context.js"),
                "Subject create/edit form must use the Organization -> Organic Unit context selector");
        assertTrue(contextScript.contains("data-subject-context-form")
                        && contextScript.contains("contextType = form.hasAttribute('data-subject-context-form')")
                        && contextScript.contains("rebuildOrganicUnitOptions")
                        && contextScript.contains("No organic units in selected organization")
                        && contextScript.contains("placeholder.disabled = hasSelectedOrganization && !hasAvailableOptions;")
                        && contextScript.contains("organicUnit.disabled = !selectedOrganizationReady;"),
                "The shared context script must synchronize Subject organic units with their organization");
        assertTrue(subjectServlet.contains("return SubjectFormData.blank(null);")
                        && subjectServlet.contains("private static long selectedOrganizationId(List<Organization> organizations, SubjectFormData form)")
                        && subjectServlet.contains("return 0L;")
                        && !subjectServlet.contains("firstManagedOrganizationId"),
                "A standalone Subject form must start without an implicit Organization selection");
    }

    @Test
    void courseAndSubjectListsUseSubtlePaginationAndSimpleSummaryCards() throws IOException {
        String courseList = Files.readString(WEBAPP_DIR.resolve("admin/admin/course/admin-courses.jsp"));
        String subjectList = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subjects.jsp"));
        String classGroupList = Files.readString(WEBAPP_DIR.resolve("admin/admin/class-group/admin-class-groups.jsp"));
        String mainCss = Files.readString(WEBAPP_DIR.resolve("assets/css/main.css"));

        assertTrue(courseList.contains("<span class=\"text-14 text-neutral-500\">Total</span>")
                        && subjectList.contains("<span class=\"text-14 text-neutral-500\">Total</span>")
                        && classGroupList.contains("<span class=\"text-14 text-neutral-500\">Total</span>")
                        && courseList.contains("${courseCount}</h2>")
                        && subjectList.contains("${subjectCount}</h2>")
                        && classGroupList.contains("${classGroupCount}</h2>")
                        && courseList.contains("bg-white rounded-10 px-24 py-24 border border-neutral-30")
                        && subjectList.contains("bg-white rounded-10 px-24 py-24 border border-neutral-30")
                        && classGroupList.contains("bg-white rounded-10 px-24 py-24 border border-neutral-30"),
                "Course, Subject and Class Group summaries must use the original simple metric-card treatment");
        assertFalse(courseList.contains("gape-summary-card")
                        || subjectList.contains("gape-summary-card")
                        || classGroupList.contains("gape-summary-card"),
                "List summaries must not use the Lessons & Assessments card treatment");
        assertTrue(courseList.contains("justify-content-end mt-20\" data-course-pagination")
                        && subjectList.contains("justify-content-end mt-20\" data-subject-pagination")
                        && courseList.contains("data-course-pagination")
                        && subjectList.contains("data-subject-pagination")
                        && mainCss.contains(".gape-list-pagination {")
                        && mainCss.contains("gap: 8px;")
                        && mainCss.contains("height: 38px;")
                        && mainCss.contains(".gape-list-load-all {")
                        && mainCss.contains("border: 1px solid var(--main-600);")
                        && mainCss.contains("justify-content: center;")
                        && mainCss.contains("tbody[data-course-list] > tr:last-child > td")
                        && mainCss.contains("tbody[data-subject-list] > tr:last-child > td"),
                "Course and Subject pagination must be compact, right-aligned and expose a Load All button");
        assertFalse(courseList.contains("justify-content-end border-top border-neutral-30 mt-20 pt-20\" data-course-pagination")
                        || subjectList.contains("justify-content-end border-top border-neutral-30 mt-20 pt-20\" data-subject-pagination"),
                "Pagination controls must not add a horizontal separator below the list");
    }

    @Test
    void classGroupDetailAndEditExposeEnrollmentManagement() throws IOException {
        String servlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/ClassGroupManagementServlet.java"));
        String service = Files.readString(JAVA_DIR.resolve("pt/isel/gape/learning/service/ClassGroupEnrollmentService.java"));
        String dao = Files.readString(JAVA_DIR.resolve("pt/isel/gape/learning/dao/ClassGroupEnrollmentDAO.java"));
        String detail = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/class-group-detail-page.jspf"));
        String management = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/class-group-enrollment-management.jspf"));
        String row = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/class-group-enrollment-row.jspf"));
        String listScript = Files.readString(WEBAPP_DIR.resolve("assets/js/gape-class-group-list.js"));
        String teachers = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/class-group-teacher-management.jspf"));
        String gradeSheets = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/class-group-grade-sheet-panel.jsp"));

        assertTrue(detail.contains(">Enrollments</span>")
                        && detail.contains("data-cg-tab=\"info\"")
                        && detail.contains("data-cg-panel=\"info\"")
                        && detail.contains("data-cg-lazy-panel=\"info\"")
                        && detail.contains("data-cg-lazy-panel=\"teachers\"")
                        && detail.contains("data-cg-lazy-panel=\"grade-sheet\"")
                        && detail.contains("data-cg-lazy-panel=\"structure\"")
                        && detail.contains("data-cg-lazy-url=\"${pageContext.request.contextPath}/learning/class-groups/${classGroup.id}/structure\"")
                        && detail.contains("<c:if test=\"${not empty block.description}\">")
                        && detail.contains("<p class=\"text-14 text-neutral-500 mb-0\"><c:out value=\"${block.description}\"/></p>")
                        && detail.contains(".cg-content-title {")
                        && detail.contains("grid-template-columns: minmax(0, 1fr) 182px;")
                        && detail.contains(".cg-content-row > :first-child > .min-w-0 {")
                        && detail.contains("flex: 1 1 auto;")
                        && detail.contains("min-width: 0;")
                        && detail.contains(".cg-content-meta-row {")
                        && detail.contains("grid-template-columns: minmax(0, 1fr) 96px 82px;")
                        && detail.contains("cg-content-meta-state")
                        && detail.contains("cg-content-meta-type")
                        && detail.contains("text-overflow: ellipsis;")
                        && detail.contains("white-space: nowrap;")
                        && detail.contains("<h5 class=\"cg-content-title text-15 fw-semibold text-neutral-800 mb-0\">")
                        && !detail.contains("<h5 class=\"cg-content-title text-15 fw-normal text-neutral-800 mb-0\">")
                        && detail.contains("data-cg-tab=\"teachers\"")
                        && detail.contains("data-cg-panel=\"teachers\"")
                        && detail.contains("id=\"classGroupSetupModal\"")
                        && detail.contains("GAPE - Class Group Details")
                        && detail.contains("gape-class-group-modal-root")
                        && detail.contains(".modal-dialog.gape-class-group-modal-dialog {")
                        && detail.contains(".cg-enrollment-modal-dialog .modal-header")
                        && detail.contains(".cg-enrollment-modal-dialog .modal-footer")
                        && detail.contains("lockClassGroupModalGeometry")
                        && detail.contains("classGroupAssessmentWeightWarning")
                        && detail.contains("data-assessment-field=\"finalGradeWeight\"")
                        && detail.contains("class-group-enrollment-management.jspf")
                        && detail.contains("/learning/assessments/${content.assessmentReferenceId}/edit")
                        && detail.contains("/learning/assessments/${assessmentItem.id}/edit"),
                "Class Group Detail must expose enrollments, teachers and setup from the primary cards");
        assertFalse(detail.contains("Pedagogical blocks, contents and critical actions.")
                        || detail.contains("Student access and enrollment state.")
                        || detail.contains("Teaching assignments for this class group.")
                        || detail.contains("Class group grade table."),
                "Class Group Details cards must follow the concise Subject Details card pattern");
        assertTrue(detail.contains("--cg-primary: #00a991;")
                        && detail.contains("--cg-primary-dark: #008a78;")
                        && !detail.contains("--cg-primary: #2563eb;"),
                "Class Group Details must retain its green secondary visual identity");
        assertTrue(servlet.contains("showEnrollmentsFragment")
                        && servlet.contains("showTeachersFragment")
                        && servlet.contains("showGradeSheetFragment")
                        && servlet.contains("showStructureFragment")
                        && servlet.contains("prepareClassGroupStructureAttributes")
                        && servlet.contains("classGroupDetailStructureLoaded"),
                "Class Group Details must load secondary panels on demand");
        assertTrue(detail.contains("var loaded = await loadClassGroupLazyPanel(target);")
                        && detail.indexOf("var loaded = await loadClassGroupLazyPanel(target);")
                                < detail.indexOf("syncClassGroupTab(tab);")
                        && detail.contains("cg-lazy-loading-spinner")
                        && !detail.contains("gapeClassGroupActivatePanel"),
                "Class Group Details must reveal a card only after its progressive request completes");
        int contentsIndex = detail.indexOf("<c:out value=\"${blockContentCount}\"/> contents");
        int loadedIndex = detail.indexOf(">Loaded");
        int newBlockIndex = detail.indexOf(">New Block");
        assertTrue(contentsIndex >= 0 && loadedIndex > contentsIndex && newBlockIndex > loadedIndex,
                "Class Group Detail structure actions must be ordered as contents, Loaded, New Block");
        assertFalse(detail.contains("Class Details &amp; Enrollments")
                        || detail.contains("data-cg-tab=\"enrollments\"")
                        || detail.contains("data-cg-panel=\"enrollments\""),
                "Class Group Detail must not keep the old combined Class Details & Enrollments tab");

        for (Path formPath : List.of(
                WEBAPP_DIR.resolve("admin/admin/class-group/admin-class-group-form.jsp"),
                WEBAPP_DIR.resolve("coordinator/coordinator/class-group/coordinator-class-group-form.jsp"),
                WEBAPP_DIR.resolve("instructor/instructor/class-group/instructor-class-group-form.jsp")
        )) {
            String form = Files.readString(formPath);
            assertFalse(form.contains("class-group-enrollment-management.jspf")
                            || form.contains("class-group-teacher-management.jspf"),
                    () -> "Edit Class Group must not include removed enrollment or teacher tabs: " + formPath);
        }

        assertTrue(management.contains(">Enrollments</h3>")
                        && management.contains("pendingClassGroupEnrollments")
                        && management.contains("activeClassGroupEnrollments")
                        && management.contains("auditClassGroupEnrollments")
                        && management.contains("classGroupEnrollmentPolicy")
                        && management.contains("/learning/class-groups/${classGroup.id}/enrollments")
                        && management.contains("data-cg-enrollment-tab=\"enrollments\"")
                        && management.contains("data-cg-enrollment-tab=\"requests\"")
                        && management.contains("data-cg-enrollment-panel=\"enrollments\"")
                        && management.contains("data-cg-enrollment-panel=\"requests\"")
                        && management.contains("The enrollment period is managed automatically")
                        && management.contains("data-class-group-enrollment-requests-pending-badge"),
                "Class group enrollment management must separate enrollment records from enrollment requests");
        assertTrue(row.contains("cg-detail-action--decision")
                        && row.contains("Decide Enrollment Request")
                        && row.contains("cg-enrollment-modal-dialog")
                        && row.contains("data-class-group-rigid-modal")
                        && row.contains("/approve")
                        && row.contains("/reject")
                        && row.contains("/delete")
                        && row.contains("startDateValue")
                        && row.contains("endDateValue")
                        && row.contains("data-class-group-live-enrollment-request-action")
                        && row.contains("data-class-group-live-submit")
                        && !row.contains("cg-primary-button px-14 py-8 text-13"),
                "Class group enrollment requests must use one highlighted decision modal before the stable detail and delete actions");
        assertTrue(detail.contains("fetch(withCurrentClassGroupSession(form.action)")
                        && detail.contains("fetch(withCurrentClassGroupSession(url)"),
                "Class group live mutations must preserve URL-rewritten sessions instead of falling back to a page navigation");
        assertTrue(listScript.contains("function withCurrentSessionUrl(rawUrl)")
                        && listScript.contains("fetch(withCurrentSessionUrl(url)"),
                "Class group pagination must preserve URL-rewritten sessions while loading a page or all rows");
        assertFalse(row.contains("/update")
                        || row.contains("Edit Enrollment")
                        || row.contains("name=\"state\"")
                        || row.contains("name=\"startDate\"")
                        || row.contains("name=\"endDate\""),
                "Class group enrollment rows must not expose manual state or date editing");
        assertFalse(detail.contains("enrollmentEndDate")
                        || detail.contains("name=\"endDate\" type=\"date\"")
                        || detail.contains("name=\"startDate\" type=\"date\""),
                "Class Group Details must not retain legacy manual enrollment or teaching date controls");
        assertTrue(servlet.contains("deleteStudentEnrollment")
                        && servlet.contains("prepareClassGroupEnrollmentAttributes")
                        && servlet.contains("redirectToReturnPath(request, response, \"/learning/class-groups/\" + classGroupId + \"#class-group-enrollments\")"),
                "ClassGroupManagementServlet must route class group enrollment management actions and preserve the current section");
        assertFalse(servlet.contains("updateStudentEnrollment"),
                "ClassGroupManagementServlet must not retain the manual enrollment update route");
        assertTrue(service.contains("deleteClassGroupEnrollment")
                        && service.contains("deriveOccurrencePeriod")
                        && service.contains("CLASS_GROUP_ENROLL_DELETE"),
                "ClassGroupEnrollmentService must derive the occurrence period and support deleting enrollments");
        assertFalse(service.contains("public ClassGroupEnrollment updateClassGroupEnrollment(")
                        || service.contains("CLASS_GROUP_ENROLL_UPDATE"),
                "ClassGroupEnrollmentService must not expose a free-form enrollment editor");
        assertTrue(dao.contains("countPendingByClassGroupIds")
                        && dao.contains("deleteEnrollment("),
                "ClassGroupEnrollmentDAO must persist deletions and provide pending-request counts");
        assertFalse(dao.contains("updateEnrollment("),
                "ClassGroupEnrollmentDAO must not retain a generic state/date update operation");
        assertTrue(teachers.contains("Teacher Assignments")
                        && teachers.contains("data-cg-detail-sort-root")
                        && teachers.contains("newClassGroupTeacherAssignmentModal")
                        && teachers.contains("Assign Teacher")
                        && teachers.contains("data-class-group-live-submit")
                        && teachers.contains("/teachers/${teacherAssignment.teacherUserId}/update")
                        && teachers.contains("data-class-group-live-form"),
                "Class group teachers must use the Subject Details assignment panel interactions");
        assertTrue(gradeSheets.contains("Grade Sheets")
                        && gradeSheets.contains("data-cg-detail-sort-root")
                        && gradeSheets.contains("classGroupGradeSheetDetail")
                        && gradeSheets.contains("/learning/grades/sheets/${classGroupGradeSheet.id}/download?format=pdf")
                        && gradeSheets.contains("cg-grade-sheet-list-header")
                        && gradeSheets.contains("cg-grade-sheet-row")
                        && detail.contains(".cg-grade-sheet-list-header,")
                        && detail.contains(".cg-grade-sheet-row {")
                        && detail.contains("minmax(260px, 1.15fr) minmax(220px, 1fr) minmax(130px, .62fr) minmax(140px, .72fr)"),
                "Class group grade sheets must expose the same workflow with a dedicated compact column grammar");
        assertTrue(detail.contains(".cg-detail-action--decision")
                        && detail.contains(".cg-enrollment-modal-dialog")
                        && detail.contains(".cg-enrollment-decision-summary")
                        && detail.contains("height: 34px;")
                        && detail.contains("width: 34px;"),
                "Class Group enrollment decisions and all inline actions must keep fixed geometry without hover layout shifts");
        assertTrue(detail.contains("cg-critical-grid")
                        && detail.contains("Restricted lifecycle operations for this class group.")
                        && detail.contains("Requires confirmation")
                        && detail.contains(".cg-danger-button:hover")
                        && detail.contains("background: #b91c1c;")
                        && detail.contains("data-class-group-enrollment-card-pending-badge")
                        && detail.contains("data-class-group-learning-plan-pending-badge")
                        && detail.contains("data-class-group-assessment-pending-badge")
                        && detail.contains("syncClassGroupSidebarPendingBadges")
                        && servlet.contains("pendingAssessmentCorrectionCounts(blockAssessmentsByBlock)")
                        && servlet.contains("classGroupDetailPendingCorrectionCount")
                        && servlet.contains("countByAssessmentIdsAndState(assessmentIds, AttemptState.SUBMITTED)")
                        && !detail.contains("data-cg-panel=\"critical-actions\""),
                "Class group critical actions must use the Subject Details lifecycle-card design outside the learning plan");
        assertTrue(servlet.contains("updateTeacherAssignment")
                        && servlet.contains("RoleAssignmentState.fromDatabaseValue"),
                "ClassGroupManagementServlet must persist the editable teacher assignment controls");
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
            assertTrue(page.contains(">Sort by")
                            && page.contains("data-class-group-sort-toggle")
                            && page.contains("gape-filter-toggle")
                            && page.contains("data-class-group-sort-option")
                            && page.contains("data-sort-field=\"code\"")
                            && page.contains("data-sort-field=\"date\"")
                            && page.contains("data-sort-field=\"status\"")
                            && page.contains("gape-sort-arrow--normal")
                            && page.contains("gape-sort-arrow--reverse")
                            && page.contains("data-sort-state=\"none\""),
                    () -> "Class group list must expose client-side Sort by options for code, date and status: " + listPath);
            assertFalse(page.contains("data-sort-field=\"modality\"")
                            || page.contains("data-sort-modality=\"${fn:escapeXml(classGroup.modalityLabel)}\"")
                            || page.contains("<span>Modality</span>"),
                    () -> "Class group Sort by must not expose Modality: " + listPath);
            assertTrue(page.contains("data-class-group-list")
                            && page.contains("data-class-group-row")
                            && page.contains("data-class-group-detail-id=\"classGroupStructure${classGroup.id}\"")
                            && page.contains("data-class-group-detail-row")
                            && page.contains("data-sort-index=\"${classGroupLoop.index}\"")
                            && page.contains("data-sort-code=\"${fn:escapeXml(classGroup.code)}\"")
                            && page.contains("data-sort-date=\"${classGroup.id}\"")
                            && page.contains("data-sort-status=\"${fn:escapeXml(classGroup.stateLabel)}\"")
                            && page.contains("data-class-group-context-column")
                            && page.contains("renderRows()")
                            && page.contains("compareGroups(activeField, activeDirection, first, second)")
                            && page.contains("list.appendChild(group.detail)")
                            && page.contains("activeField = null;")
                            && page.contains("activeDirection = null;")
                            && page.contains("sortToggle.classList.toggle('is-active', activeField !== null)")
                            && page.contains("closeDropdown(sortToggle)"),
                    () -> "Class group list must sort rows in-place and keep each details row paired with its class group: " + listPath);
            assertTrue(page.contains(">Group by")
                            && page.contains("data-class-group-group-toggle")
                            && page.contains("data-class-group-group-option")
                            && page.contains("data-group-field=\"organization\"")
                            && page.contains("data-group-field=\"course\"")
                            && page.contains("data-group-field=\"subject\"")
                            && page.contains("data-group-normal=\"asc\"")
                            && page.contains("data-group-state=\"none\"")
                            && page.contains("gape-group-arrow--normal")
                            && page.contains("gape-group-arrow--reverse")
                            && page.contains("data-group-organization-id=\"${classGroup.course.organizationId}\"")
                            && page.contains("data-group-organization=\"${fn:escapeXml(classGroup.course.organizationName)}\"")
                            && page.contains("data-group-organic-unit-id=\"${classGroup.course.organicUnitId}\"")
                            && page.contains("data-group-organic-unit=\"${fn:escapeXml(classGroup.course.organicUnitLabel)}\"")
                            && page.contains("data-group-course-id=\"${classGroup.courseId}\"")
                            && page.contains("data-group-course=\"${fn:escapeXml(classGroup.courseName)}\"")
                            && page.contains("data-group-subject-id=\"${classGroup.subjectId}\"")
                            && page.contains("data-group-subject=\"${fn:escapeXml(classGroup.subjectName)}\"")
                            && page.contains("gape-class-group-organization-group-row")
                            && page.contains("gape-class-group-unit-group-row")
                            && page.contains("gape-class-group-course-group-row")
                            && page.contains("gape-class-group-subject-group-row")
                            && page.contains("gape-class-group-table.is-class-group-grouped")
                            && page.contains("gape-class-group-table.is-class-group-grouped-by-organization")
                            && page.contains("gape-class-group-table.is-class-group-grouped-by-course")
                            && page.contains("gape-class-group-table.is-class-group-grouped-by-subject")
                            && page.contains("table.classList.toggle('is-class-group-grouped', grouped)")
                            && page.contains("table.classList.toggle('is-class-group-grouped-by-organization', activeGroupField === 'organization')")
                            && page.contains("table.classList.toggle('is-class-group-grouped-by-course', activeGroupField === 'course')")
                            && page.contains("table.classList.toggle('is-class-group-grouped-by-subject', activeGroupField === 'subject')")
                            && page.contains("column.toggleAttribute('hidden', grouped)")
                            && page.contains("cell.colSpan = visibleColumnCount()")
                            && page.contains("updateDetailColumnCount(group)")
                            && page.contains("createClassGroupOrganizationHeader(organizationBucket)")
                            && page.contains("createClassGroupUnitHeader(unitBucket)")
                            && page.contains("createClassGroupCourseHeader(courseBucket, 'organization')")
                            && page.contains("createClassGroupCourseHeader(courseBucket, 'course')")
                            && page.contains("createClassGroupSubjectHeader(subjectBucket, 'organization')")
                            && page.contains("createClassGroupSubjectHeader(subjectBucket, 'course')")
                            && page.contains("createClassGroupSubjectHeader(subjectBucket, 'subject')")
                            && page.contains("data-class-group-organization-visibility-toggle")
                            && page.contains("data-class-group-unit-visibility-toggle")
                            && page.contains("data-class-group-course-visibility-toggle")
                            && page.contains("data-class-group-subject-visibility-toggle")
                            && page.contains("collapsedOrganizationGroups")
                            && page.contains("collapsedUnitGroups")
                            && page.contains("collapsedCourseGroups")
                            && page.contains("collapsedSubjectGroups")
                            && page.contains("Hide organization class groups")
                            && page.contains("Show subject class groups")
                            && page.contains("clearCollapsedGroups()")
                            && page.contains("activeGroupField === 'organization'")
                            && page.contains("activeGroupField === 'course'")
                            && page.contains("activeGroupField === 'subject'")
                            && page.contains("groupToggle.classList.toggle('is-active', activeGroupField !== null)")
                            && page.contains("closeDropdown(groupToggle)"),
                    () -> "Class group list must group by organization, course or subject with cyclic active states and hidden context: " + listPath);
        }

        String servlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/ClassGroupManagementServlet.java"));
        assertTrue(servlet.contains("\"classGroupLessonsByClassGroup\"")
                        && servlet.contains("\"classGroupRoomsByClassGroup\"")
                        && servlet.contains("\"canManagePhysicalRoomByCode\""),
                "ClassGroupManagementServlet must load lessons, rooms and room permissions for Show Activities");
        String structurePermissionMethod = servlet.substring(
                servlet.indexOf("private boolean canManageClassGroupStructure"),
                servlet.indexOf("private boolean canModifyClassGroup")
        );
        String modifyPermissionMethod = servlet.substring(
                servlet.indexOf("private boolean canModifyClassGroup"),
                servlet.indexOf("private boolean canManageClassGroupEnrollments")
        );
        assertFalse(structurePermissionMethod.contains("isCompleted()")
                        || modifyPermissionMethod.contains("isCompleted()"),
                "Completed class groups must still expose edit and delete actions in listings");
        assertTrue(servlet.contains(".sorted(Comparator.comparingLong(ClassGroup::id).reversed())"),
                "Class group list must default to newest class groups first");

        String activityFragment = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/class-group-activities-panel.jspf"));
        assertTrue(activityFragment.contains("Class Activities")
                        && activityFragment.contains("/learning/lessons/${lesson.id}")
                        && activityFragment.contains("${lesson.compactDateRangeLabel}")
                        && activityFragment.contains("/learning/rooms/${room.encodedCode}"),
                "Class group activity fragment must expose lessons and rooms");

        String courseList = Files.readString(WEBAPP_DIR.resolve("admin/admin/course/admin-courses.jsp"));
        String courseListRows = Files.readString(FRAGMENTS_DIR.resolve("course-list-rows.jsp"));
        String subjectList = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subjects.jsp"));
        String subjectListRows = Files.readString(FRAGMENTS_DIR.resolve("subject-list-rows.jsp"));
        String organizationList = Files.readString(WEBAPP_DIR.resolve("admin/admin/organization/admin-organizations.jsp"));
        String organizationDetail = Files.readString(WEBAPP_DIR.resolve("admin/admin/organization/admin-organization-detail.jsp"));
        String organizationStructureTree = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/organization-structure-tree.jspf"));
        String organizationActivities = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/organization-class-group-activities-panel.jspf"));
        assertTrue(courseList.contains("${courseBasePath}/${course.id}/edit")
                        && courseList.contains("<c:if test=\"${canModifyCourseRow}\">")
                        && courseList.contains("data-bs-target=\"#deleteCourse${course.id}\""),
                "Course list actions must expose Detail, Edit and Delete whenever the row can be modified");
        assertFalse(courseList.contains("course.inactive")
                        || courseList.contains("/${course.id}/unarchive")
                        || courseList.contains("title=\"Activate\""),
                "Course list actions must not vary by state or expose a separate Activate action");
        assertTrue(courseList.contains(">Sort by")
                        && courseList.contains("data-course-sort-toggle")
                        && courseList.contains("gape-filter-toggle")
                        && courseList.contains("data-course-sort-option")
                        && courseList.contains("data-sort-field=\"name\"")
                        && courseList.contains("data-sort-field=\"date\"")
                        && courseList.contains("data-sort-field=\"status\"")
                        && courseList.contains("gape-sort-arrow--normal")
                        && courseList.contains("gape-sort-arrow--reverse")
                        && courseList.contains("data-sort-state=\"none\""),
                "Course list must expose client-side Sort by options for name, date and status");
        assertFalse(courseList.contains("data-sort-field=\"type\"")
                        || courseList.contains("data-sort-type=\"${fn:escapeXml(course.typeLabel)}\"")
                        || courseList.contains("<span>Type</span>"),
                "Course Sort by must not expose Type");
        assertFalse(courseList.contains("data-sort-direction-label")
                        || courseList.contains("gape-sort-direction-label")
                        || organizationList.contains("data-sort-direction-label")
                        || organizationList.contains("gape-sort-direction-label")
                        || subjectList.contains("data-sort-direction-label")
                        || subjectList.contains("gape-sort-direction-label"),
                "Sort by options must show only option names and arrows, without direction text labels");
        assertTrue(courseList.contains("data-course-list")
                        && courseList.contains("course-list-rows.jsp")
                        && courseList.contains("data-course-pagination")
                        && courseList.contains("data-course-load-all")
                        && courseList.contains("courseRowsUrl")
                        && courseListRows.contains("data-course-row")
                        && courseListRows.contains("data-sort-index=\"${courseListOffset + courseLoop.index}\"")
                        && courseListRows.contains("data-course-load-more-meta")
                        && courseListRows.contains("data-sort-name=\"${fn:escapeXml(course.name)}\"")
                        && courseListRows.contains("data-sort-date=\"${course.id}\"")
                        && courseListRows.contains("data-sort-status=\"${fn:escapeXml(course.stateLabel)}\"")
                        && courseList.contains("data-course-context-column")
                        && courseList.contains("renderRows()")
                        && courseList.contains("compareGroups(activeField, activeDirection, first, second)")
                        && courseList.contains("list.appendChild(group.row)")
                        && courseList.contains("activeField = null;")
                        && courseList.contains("activeDirection = null;")
                        && courseList.contains("sortToggle.classList.toggle('is-active', activeField !== null)")
                        && courseList.contains("closeDropdown(sortToggle)"),
                "Course list must sort table rows in-place without the removed details rows");
        assertFalse(courseList.contains("data-course-detail-id=\"courseSubjects${course.id}\"")
                        || courseList.contains("data-course-detail-row")
                        || courseList.contains("list.appendChild(group.detail)"),
                "Course list must not keep hidden detail rows after removing Show");
        assertTrue(courseList.contains(">Group by")
                        && courseList.contains("data-course-group-toggle")
                        && courseList.contains("data-course-group-option")
                        && courseList.contains("data-group-field=\"organization\"")
                        && courseList.contains("data-group-normal=\"asc\"")
                        && courseList.contains("data-group-state=\"none\"")
                        && !courseList.contains("data-group-field=\"organicUnit\"")
                        && courseList.contains("gape-course-actions")
                        && courseList.contains("gape-course-management-panel")
                        && courseList.contains("gape-course-table-scroll")
                        && courseList.contains("gape-course-table")
                        && courseList.contains("gape-group-arrow--normal")
                        && courseList.contains("gape-group-arrow--reverse")
                        && courseList.contains("data-group-organization-id=\"${course.organizationId}\"")
                        && courseList.contains("data-group-organization=\"${fn:escapeXml(course.organizationName)}\"")
                        && courseList.contains("data-group-organic-unit-id=\"${course.organicUnitId}\"")
                        && courseList.contains("data-group-organic-unit=\"${fn:escapeXml(course.organicUnitLabel)}\"")
                        && courseList.contains("gape-course-organization-group-row")
                        && courseList.contains("gape-course-unit-group-row")
                        && courseList.contains("gape-course-table.is-course-grouped")
                        && courseList.contains("gape-course-table.is-course-grouped-by-organization")
                        && courseList.contains("table.classList.toggle('is-course-grouped', grouped)")
                        && courseList.contains("table.classList.toggle('is-course-grouped-by-organization', activeGroupField === 'organization')")
                        && courseList.contains("column.toggleAttribute('hidden', grouped)")
                        && courseList.contains("cell.colSpan = visibleColumnCount()")
                        && courseList.contains("createCourseOrganizationGroupHeader(organizationBucket)")
                        && courseList.contains("createCourseUnitGroupHeader(unitBucket)")
                        && courseList.contains("data-course-organization-visibility-toggle")
                        && courseList.contains("data-course-unit-visibility-toggle")
                        && courseList.contains("collapsedOrganizationGroups")
                        && courseList.contains("collapsedUnitGroups")
                        && courseList.contains("Hide organization courses")
                        && courseList.contains("Show organic unit courses")
                        && courseList.contains("detachRowGroups()")
                        && courseList.contains("collapsedOrganizationGroups.has(organizationBucket.key)")
                        && courseList.contains("collapsedUnitGroups.has(unitBucket.key)")
                        && courseList.contains("activeGroupField === 'organization'")
                        && courseList.contains("activeGroupDirection = oppositeDirection(normalDirection)")
                        && courseList.contains("activeGroupDirection = null;")
                        && courseList.contains("groupToggle.classList.toggle('is-active', activeGroupField !== null)")
                        && courseList.contains("closeDropdown(groupToggle)")
                        && courseList.contains("unitBucket.items.forEach(appendGroup)")
                        && courseList.contains("countLabel(bucket.units.length, 'department', 'departments')"),
                "Course list must expose a single Organization group option that cycles normal, reverse and off while separating organizations from departments");
        assertTrue(subjectList.contains(">Sort by")
                        && subjectList.contains("data-subject-sort-toggle")
                        && subjectList.contains("data-subject-sort-option")
                        && subjectList.contains("data-sort-field=\"name\"")
                        && subjectList.contains("data-sort-field=\"date\"")
                        && subjectList.contains("data-sort-field=\"status\"")
                        && subjectList.contains("gape-sort-arrow--normal")
                        && subjectList.contains("gape-sort-arrow--reverse")
                        && subjectList.contains("data-sort-state=\"none\""),
                "Subject list must expose client-side Sort by options for name, date and status");
        assertFalse(subjectList.contains("data-sort-field=\"ects\"")
                        || subjectList.contains("data-sort-ects=\"${empty subject.ects ? '0' : subject.ects}\"")
                        || subjectList.contains("<span>ECTS</span>"),
                "Subject Sort by must not expose ECTS");
        assertTrue(subjectList.contains("data-subject-list")
                        && subjectList.contains("subject-list-rows.jsp")
                        && subjectList.contains("data-subject-pagination")
                        && subjectList.contains("data-subject-load-all")
                        && subjectList.contains("subjectRowsUrl")
                        && subjectListRows.contains("data-subject-row")
                        && subjectListRows.contains("data-sort-index=\"${subjectListOffset + subjectLoop.index}\"")
                        && subjectListRows.contains("data-subject-load-more-meta")
                        && subjectListRows.contains("data-sort-name=\"${fn:escapeXml(subject.name)}\"")
                        && subjectListRows.contains("data-sort-date=\"${subject.id}\"")
                        && subjectListRows.contains("data-sort-status=\"${fn:escapeXml(subject.stateLabel)}\"")
                        && subjectList.contains("data-subject-context-column")
                        && subjectList.contains("data-subject-association-column")
                        && subjectListRows.contains("data-subject-association-count")
                        && subjectListRows.contains("subject.organizationContextHtml")
                        && !subjectListRows.contains("association.courseContextHtml")
                        && subjectList.contains("white-space: nowrap;")
                        && subjectList.contains("renderRows()")
                        && subjectList.contains("compareGroups(activeField, activeDirection, first, second)")
                        && subjectList.contains("sortToggle.classList.toggle('is-active', activeField !== null)")
                        && subjectList.contains("closeDropdown(sortToggle)"),
                "Subject list must sort table rows in-place without the removed details rows");
        assertFalse(subjectList.contains("data-subject-detail-id=\"subjectClassGroups${subject.id}\"")
                        || subjectList.contains("data-subject-detail-row")
                        || subjectList.contains("list.appendChild(group.detail)"),
                "Subject list must not keep hidden detail rows after removing Show");
        assertTrue(subjectList.contains(">Group by")
                        && subjectList.contains("data-subject-group-toggle")
                        && subjectList.contains("data-subject-group-option")
                        && subjectList.contains("data-group-field=\"organization\"")
                        && subjectList.contains("data-group-field=\"course\"")
                        && subjectList.contains("data-group-normal=\"asc\"")
                        && subjectList.contains("data-group-state=\"none\"")
                        && subjectList.contains("data-group-organization-id=\"${subject.organizationId}\"")
                        && subjectList.contains("data-group-organization=\"${fn:escapeXml(subject.organizationName)}\"")
                        && subjectList.contains("data-group-organic-unit-id=\"${subjectGroupOrganicUnitId}\"")
                        && subjectList.contains("data-group-organic-unit=\"${fn:escapeXml(subjectGroupOrganicUnitName)}\"")
                        && subjectList.contains("data-group-course-id=\"${subjectGroupCourseId}\"")
                        && subjectList.contains("data-group-course=\"${fn:escapeXml(subjectGroupCourseName)}\"")
                        && subjectList.contains("gape-subject-organization-group-row")
                        && subjectList.contains("gape-subject-unit-group-row")
                        && subjectList.contains("gape-subject-course-group-row")
                        && subjectList.contains("createSubjectOrganizationGroupHeader(organizationBucket)")
                        && subjectList.contains("createSubjectUnitGroupHeader(unitBucket)")
                        && subjectList.contains("createSubjectCourseGroupHeader(courseBucket, true)")
                        && subjectList.contains("createSubjectCourseGroupHeader(courseBucket, false)")
                        && subjectList.contains("data-subject-organization-visibility-toggle")
                        && subjectList.contains("data-subject-unit-visibility-toggle")
                        && subjectList.contains("data-subject-course-visibility-toggle")
                        && subjectList.contains("collapsedOrganizationGroups")
                        && subjectList.contains("collapsedUnitGroups")
                        && subjectList.contains("collapsedCourseGroups")
                        && subjectList.contains("gape-subject-table.is-subject-grouped")
                        && subjectList.contains("gape-subject-table.is-subject-grouped-by-organization")
                        && subjectList.contains("gape-subject-table.is-subject-grouped-by-course")
                        && subjectList.contains("table.classList.toggle('is-subject-grouped', grouped)")
                        && subjectList.contains("table.classList.toggle('is-subject-grouped-by-organization', activeGroupField === 'organization')")
                        && subjectList.contains("table.classList.toggle('is-subject-grouped-by-course', activeGroupField === 'course')")
                        && subjectList.contains("column.toggleAttribute('hidden', grouped)")
                        && subjectList.contains("cell.colSpan = visibleColumnCount()")
                        && subjectList.contains("updateDetailColumnCount(group)")
                        && subjectList.contains("activeGroupField === 'organization'")
                        && subjectList.contains("activeGroupField === 'course'")
                        && subjectList.contains("activeGroupDirection = oppositeDirection(normalDirection)")
                        && subjectList.contains("clearCollapsedGroups()")
                        && subjectList.contains("groupToggle.classList.toggle('is-active', activeGroupField !== null)"),
                "Subject list must group subjects by organization, organic unit and course, or directly by course, with cyclic active states");
        assertFalse(courseList.contains("data-gape-tree-toggle")
                        || courseList.contains("data-course-detail-row")
                        || courseList.contains("courseSubjectsByCourse")
                        || courseList.contains("courseSubjectClassGroupsByCourseAndSubject")
                        || courseList.contains("courseClassGroupActivities${course.id}_${subject.id}_${classGroup.id}")
                        || courseList.contains("class-group-activities-panel.jspf"),
                "Course list must not expose the removed Show hierarchy or activity mechanics");
        assertFalse(subjectList.contains("subjectClassGroupActivities${subject.id}_${course.id}_${classGroup.id}")
                        || subjectList.contains("Show Activities")
                        || subjectList.contains("class-group-activities-panel.jspf"),
                "Subject list must not expose the removed Show hierarchy or activity mechanics");
        assertTrue(organizationDetail.contains("organization-structure-tree.jspf")
                        && organizationStructureTree.contains("organizationClassGroupActivities${course.id}_${subject.subjectId}_${classGroup.id}")
                        && organizationStructureTree.contains("Show Activities")
                        && organizationStructureTree.contains("organization-class-group-activities-panel.jspf")
                        && organizationActivities.contains("og-activity-grid")
                        && organizationActivities.contains("Lessons")
                        && organizationActivities.contains("Assessments")
                        && organizationActivities.contains("classGroupLessonTimelineRankByClassGroup")
                        && !organizationActivities.contains("classGroupRooms"),
                "Organization detail must expose Show Activities inside each class group");
        assertTrue(organizationList.contains("data-gape-tree-toggle=\"organizationUnits${organization.id}\"")
                        && organizationList.contains("gape-organization-units-panel")
                        && organizationList.contains("gape-hierarchy-node")
                        && organizationList.contains("items=\"${organization.organicUnits}\""),
                "Organization list must expose Show only at the organization level using the legacy organic-unit tree design");
        assertFalse(organizationList.contains("data-gape-tree-toggle=\"unitCourses${unit.id}\"")
                        || organizationList.contains("items=\"${unit.courses}\"")
                        || organizationList.contains("class-group-activities-panel.jspf")
                        || organizationList.contains("organization-class-group-activities-panel.jspf"),
                "Organization list must not expose deeper hierarchy or activity panels");

        String courseServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/CourseManagementServlet.java"));
        String subjectServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/SubjectManagementServlet.java"));
        String organizationServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/OrganizationManagementServlet.java"));
        assertTrue(courseServlet.contains("ClassGroupActivityViewSupport")
                        && courseServlet.contains("exposeClassGroupActivities"),
                "CourseManagementServlet must load class group activities");
        assertTrue(courseServlet.contains(".sorted(Comparator.comparingLong(Course::id).reversed())"),
                "Course list must default to newest courses first");
        assertTrue(subjectServlet.contains(".sorted(Comparator.comparingLong(Subject::id).reversed())"),
                "Subject list must default to newest subjects first");
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
        String studentSubjectClassGroupCard = Files.readString(FRAGMENTS_DIR.resolve("student-subject-class-group-card.jspf"));
        String scripts = Files.readString(FRAGMENTS_DIR.resolve("template-base-scripts.jspf"));
        String autoUpdateScript = Files.readString(ASSETS_DIR.resolve("js/gape-student-enrollment-auto-update.js"));

        assertTrue(servlet.contains("ClassGroupEnrollmentService")
                        && servlet.contains("studentClassGroups")
                        && servlet.contains("\"class-groups\"")
                        && servlet.contains("\"/student/class-groups/*\"")
                        && servlet.contains("showClassGroupDetail")
                        && servlet.contains("canEnrollInClassGroupContext(classGroup, activeCourseContexts)")
                        && servlet.contains("courseOccurrenceContextKey(")
                        && servlet.contains("classGroup.courseOccurrenceId() == enrollment.courseOccurrenceId()"),
                "StudentEnrollmentServlet must load class groups from the student's active course occurrence");
        assertFalse(servlet.contains("optionalDate(request, \"startDate\")")
                        || servlet.contains("optionalDate(request, \"endDate\")"),
                "Student class group enrollment requests must not accept manually selected occurrence dates");
        assertTrue(coursesPage.contains("Courses")
                        && classGroupsPage.contains("Class Groups")
                        && classGroupDetailPage.contains("/student/enrollments/class-groups/${classGroup.id}/withdraw")
                        && !studentClassGroupCard.contains("/student/enrollments/class-groups/${item.classGroup.id}/withdraw"),
                "Student enrollment page must keep withdrawal exclusively on class-group details");
        int listActionsStart = servlet.indexOf("private String classGroupListActionsHtml");
        int listActionsEnd = servlet.indexOf("private String classGroupRequestFormHtml", listActionsStart);
        assertTrue(listActionsStart >= 0 && listActionsEnd > listActionsStart
                        && !servlet.substring(listActionsStart, listActionsEnd).contains("classGroupLeaveFormHtml"),
                "Student class-group polling updates must not reintroduce withdrawal on list cards");
        assertTrue(classGroupsPage.contains("studentClassGroupCourseGroups")
                        && classGroupsPage.contains("gape-student-class-course-group")
                        && classGroupsPage.contains("gape-student-class-subject-group")
                        && classGroupsPage.contains("Open course detail")
                        && classGroupsPage.contains("Open subject detail")
                        && classGroupsPage.contains("Review the class groups in which you are currently enrolled.")
                        && classGroupsPage.contains("gape-student-card-icon-button")
                        && classGroupsPage.contains("<%@ include file=\"/WEB-INF/fragments/student-class-group-card.jspf\" %>")
                        && studentClassGroupCard.contains("col-xxl-3 col-xl-4 col-md-6")
                        && studentClassGroupCard.contains("Modality:")
                        && studentClassGroupCard.contains("Shift:")
                        && studentClassGroupCard.contains("Occup:")
                        && !studentClassGroupCard.contains("Dates:")
                        && studentClassGroupCard.contains("px-14 py-14")
                        && studentClassGroupCard.contains("gape-student-class-group-card__icon--compact")
                        && studentClassGroupCard.contains("gape-student-class-group-card__icon")
                        && studentClassGroupCard.contains("/student/class-groups/${item.classGroup.id}")
                        && studentClassGroupCard.contains("aria-label=\"Open class group\"")
                        && studentClassGroupCard.contains("data-gape-enrollment-actions-kind=\"class-group-list\""),
                "Student class groups page must render only enrolled groups with compact icon actions");
        assertTrue(servlet.contains("enrolledStudentClassGroups")
                        && servlet.contains(".filter(StudentClassGroupView::isActiveEnrollment)"),
                "StudentEnrollmentServlet must constrain My Class Groups to active student enrollments");
        assertFalse(classGroupsPage.contains("data-student-show-unenrolled")
                        || studentClassGroupCard.contains("data-student-unenrolled-card")
                        || studentClassGroupCard.contains("Request enrollment"),
                "My Class Groups must not render un-enrolled groups or an enrollment request action");
        assertTrue(studentDashboardStart.contains("min-height: 48px;")
                        && studentDashboardStart.contains("min-height: 97px;")
                        && studentDashboardStart.contains("align-items: flex-start;")
                        && studentDashboardStart.contains("margin-block-end: 5px !important;")
                        && studentDashboardStart.contains("padding-block-start: 3px;")
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
        assertTrue(subjectDetailPage.contains("student-subject-class-group-card.jspf")
                        && subjectDetailPage.contains("hideUnenrolledSubjectClassGroups")
                        && subjectDetailPage.contains("data-student-show-subject-unenrolled")
                        && subjectDetailPage.contains("other class group")
                        && subjectDetailPage.contains("item.currentOrPendingEnrollment")
                        && studentSubjectClassGroupCard.contains("col-xxl-3 col-xl-4 col-md-6")
                        && studentSubjectClassGroupCard.contains("px-14 py-14")
                        && studentSubjectClassGroupCard.contains("<h6 class=\"text-14 fw-semibold text-neutral-800 mb-4\"><c:out value=\"${item.classGroup.code}\"/></h6>")
                        && studentSubjectClassGroupCard.contains("/student/enrollments/class-groups/${item.classGroup.id}")
                        && studentSubjectClassGroupCard.contains("/student/class-groups/${item.classGroup.id}")
                        && studentSubjectClassGroupCard.contains("aria-label=\"Request enrollment\"")
                        && studentSubjectClassGroupCard.contains("aria-label=\"Open class group\"")
                        && studentSubjectClassGroupCard.contains("aria-label=\"Leave class group\"")
                        && studentSubjectClassGroupCard.contains("Modality:")
                        && studentSubjectClassGroupCard.contains("Shift:")
                        && studentSubjectClassGroupCard.contains("Occup:")
                        && studentSubjectClassGroupCard.contains("data-gape-enrollment-actions-kind=\"class-group-detail\""),
                "Student subject detail must aggregate withdrawn class groups while keeping active and pending enrollments visible as individual cards");
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
                        && classGroupDetailPage.contains("<section id=\"student-class-group-structure\" class=\"gape-student-structure-board\">")
                        && !classGroupDetailPage.contains("gape-student-panel bg-white rounded-10 px-24 py-24 border border-neutral-30\">\n    <section id=\"student-class-group-structure\""),
                "Student class group detail must let the Study Path board use the full available width");
        assertTrue(servlet.contains("studentContentBlocks(classGroup.id())")
                        && servlet.contains("contentBlockDAO.findByClassGroup(classGroupId)")
                        && !servlet.contains(".filter(ContentBlockView::isActive)")
                        && !servlet.contains("isVisibleToStudent("),
                "Student class group detail must load block structure without a pre-JSP visibility helper");
        assertTrue(classGroupDetailPage.contains("data-gape-enrollment-target=\"class-group-${classGroup.id}\"")
                        && classGroupDetailPage.contains("data-gape-enrollment-actions-kind=\"class-group-detail-page\"")
                        && classGroupDetailPage.contains("View class group grade sheet")
                        && classGroupDetailPage.contains("studentClassGroupGradeSheetModal")
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
                "Student sidebar must render Events immediately after Message and before Courses");
        assertTrue(studentLessonsPage.contains("lessonCourseGroups")
                        && studentLessonsPage.contains("courseGroup.subjectGroups")
                        && studentLessonsPage.contains("subjectGroup.lessons")
                        && studentLessonsPage.contains("col-xxl-3 col-xl-4 col-md-6")
                        && studentLessonsPage.contains("gape-student-card gape-student-class-group-card px-14 py-14")
                        && studentLessonsPage.contains("data-student-tabs")
                        && studentLessonsPage.contains("data-student-tab-panel=\"attendance\"")
                        && studentLessonsPage.contains("gape-student-card-actions")
                        && studentLessonsPage.contains("w-40 h-40 rounded-10")
                        && studentLessonsPage.contains("lesson.dateRangeLabel")
                        && studentLessonsPage.contains("(lesson.online or lesson.hybrid) and lesson.hasMeetingLink")
                        && (studentLessonServlet.contains("lessonCourseGroups(activeLessons, classGroupById)")
                                || studentLessonServlet.contains("lessonCourseGroups(lessons, classGroupById)"))
                        && studentLessonsPage.contains("/student/lessons/${lesson.id}/access")
                        && studentCalendarPage.contains("gape-calendar-grid")
                        && studentCalendarPage.contains("data-event-calendar-source-item")
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
        assertTrue(courseDetailPage.contains("/student/subjects/${course.id}/${subject.subjectId}")
                        && subjectsPage.contains("/student/subjects/${course.id}/${subject.subjectId}")
                        && studentClassGroupCard.contains("data-gape-enrollment-actions-kind=\"class-group-list\"")
                        && coursesPage.contains("gape-student-card-actions")
                        && subjectsPage.contains("gape-student-card-actions")
                        && courseDetailPage.contains("gape-student-card-actions"),
                "Student curricular subject cards must remain navigable while class group cards expose enrollment targets");
        assertFalse(servlet.contains("SubjectEnrollment")
                        || servlet.contains("subjectEnrollmentUpdates")
                        || courseDetailPage.contains("data-gape-enrollment-target=\"subject-")
                        || subjectsPage.contains("data-gape-enrollment-target=\"subject-")
                        || subjectDetailPage.contains("data-gape-enrollment-target=\"subject-")
                        || courseDetailPage.contains("/student/enrollments/courses/${course.id}/subjects/")
                        || subjectsPage.contains("/student/enrollments/courses/${course.id}/subjects/")
                        || subjectDetailPage.contains("/student/enrollments/courses/${course.id}/subjects/"),
                "Subjects must be curricular context only, without a separate enrollment lifecycle");
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
                        && studentSubjectClassGroupCard.contains("gape-student-card-icon-button--request")
                        && classGroupDetailPage.contains("gape-student-card-icon-button--request")
                        && studentLessonsPage.contains("gape-student-card-icon-button--meeting")
                        && classGroupDetailPage.contains("gape-student-card-icon-button--meeting")
                        && classGroupDetailPage.contains("gape-student-card-icon-button--download")
                        && servlet.contains("gape-student-card-icon-button gape-student-card-icon-button--request"),
                "Student card action buttons must use distinct Eduall color variants by action type");
        assertTrue(servlet.contains("writeStudentEnrollmentResponse")
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
    void courseDetailUsesOrganizationDetailsCardsAndAvoidsDuplicateCourseContext() throws IOException {
        String courseDetail = Files.readString(WEBAPP_DIR.resolve("admin/admin/course/admin-course-detail.jsp"));
        String associateSubjectModal = Files.readString(FRAGMENTS_DIR.resolve("course-associate-subject-modal.jsp"));
        String courseEnrollmentManagement = Files.readString(FRAGMENTS_DIR.resolve("course-enrollment-management.jspf"));
        String courseEnrollmentCard = Files.readString(FRAGMENTS_DIR.resolve("course-enrollment-card.jspf"));
        String courseOccurrencesPanel = Files.readString(FRAGMENTS_DIR.resolve("course-occurrences-panel.jsp"));
        String courseServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/CourseManagementServlet.java"));

        int structureCard = courseDetail.indexOf("data-course-tab=\"structure\"");
        int enrollmentsCard = courseDetail.indexOf("data-course-tab=\"enrollments\"");
        int occurrencesCard = courseDetail.indexOf("data-course-tab=\"occurrences\"");
        int structurePanel = courseDetail.indexOf("data-course-panel=\"structure\"");
        int enrollmentsPanel = courseDetail.indexOf("data-course-panel=\"enrollments\"");
        int occurrencesPanel = courseDetail.indexOf("data-course-panel=\"occurrences\"");
        int enrollmentInclude = courseDetail.indexOf("course-enrollment-management.jspf");
        assertTrue(structureCard > 0 && enrollmentsCard > 0 && occurrencesCard > 0
                        && structurePanel > 0 && enrollmentsPanel > 0 && occurrencesPanel > 0,
                "Course detail must render Subjects, Occurrence and Enrollments primary cards and panels");
        assertTrue(structureCard < occurrencesCard && occurrencesCard < enrollmentsCard
                        && structurePanel < enrollmentsPanel && enrollmentsPanel < enrollmentInclude
                        && enrollmentsPanel < occurrencesPanel,
                "Course detail must render its cards as Subjects, Occurrence and Enrollments");
        assertTrue(courseDetail.contains("cd-surface cd-hero")
                        && courseDetail.contains("GAPE - Course Details")
                        && courseServlet.contains("prepareDashboard(request, \"courses\", \"Course Details\")")
                        && courseDetail.contains("courseSetupModal")
                        && courseDetail.contains("data-bs-target=\"#courseSetupModal\"")
                        && courseDetail.contains("Critical Actions")
                        && courseDetail.contains("cd-critical-grid")
                        && courseDetail.contains(">Subjects</span>")
                        && courseDetail.contains("<h3 class=\"text-18 fw-semibold text-neutral-800 mb-4\">Structure</h3>")
                        && courseDetail.contains("Subjects associated with this course, their curricular position and class groups.")
                        && courseDetail.contains("associateCourseSubjectModal")
                        && courseDetail.contains("data-course-detail-sort-root")
                        && courseDetail.contains("data-course-detail-sort-option")
                        && courseDetail.contains("data-course-detail-sort-list")
                        && courseDetail.contains("data-sort-field=\"date\"")
                        && courseEnrollmentManagement.contains("data-course-detail-sort-root")
                        && courseEnrollmentManagement.contains("data-course-detail-sort-option")
                        && courseEnrollmentManagement.contains("data-course-detail-sort-list")
                        && courseServlet.contains("availableSubjectOptions")
                        && courseServlet.contains("redirectToReturnPath(request, response, courseBasePath(request) + \"/\" + courseId + \"#course-structure\")")
                        && courseDetail.contains("gape-course-structure-panel")
                        && courseDetail.contains("gape-structure-list-header")
                        && courseDetail.contains("gape-structure-row")
                        && courseDetail.contains("Active Class Groups")
                        && courseDetail.contains("activeClassGroupCountBySubject")
                        && courseDetail.contains("association.subject.hasPhoto")
                        && courseDetail.contains("gape-learning-table-photo")
                        && courseDetail.contains("gape-photo-placeholder--table")
                        && courseDetail.contains("data-course-class-group-activities")
                        && courseDetail.contains("data-course-lazy-panel=\"occurrences\"")
                        && courseDetail.contains("data-course-lazy-panel=\"enrollments\"")
                        && courseOccurrencesPanel.contains("courseOccurrenceReferenceYear")
                        && courseServlet.contains("showClassGroupActivitiesFragment")
                        && courseServlet.contains("ClassGroupState.ACTIVE")
                        && courseDetail.contains("courseEnrollmentEmbedded")
                        && courseDetail.contains("data-course-panel=\"enrollments\" hidden")
                        && courseDetail.contains("not courseEctsConsistent")
                        && courseDetail.contains("The sum of the subject ECTS in this course"),
                "Course detail must follow the Organization Details card, setup, ECTS alert, occurrence grade-sheet list and critical-actions structure");
        assertFalse(courseDetail.contains("Max grade")
                        || courseDetail.contains("Course Associations")
                        || courseDetail.contains("edit#course-associations")
                        || courseDetail.contains("courseGradeSheet")
                        || courseDetail.contains("data-course-tab=\"grade-sheet\"")
                        || courseServlet.contains("exposeCourseGradeSheet"),
                "Course detail must not render or load course-level grade sheets");
        assertTrue(courseDetail.contains("${course.courseManagementContextHtml}"),
                "Course detail subjects must render the course context without duplicating the subject/course chain");
        assertTrue(courseDetail.contains("courseOccurrenceReferenceYear")
                        && courseDetail.contains("courseOccurrenceAcademicYearPicker")
                        && courseDetail.contains("data-academic-year-grid")
                        && courseDetail.contains("data-existing-course-occurrence")
                        && courseDetail.contains("availabilityFor(referenceYear)")
                        && courseDetail.contains("novalidate")
                        && courseDetail.contains("validateOccurrence(true)")
                        && courseOccurrencesPanel.contains("data-course-occurrence-submit")
                        && courseDetail.contains("[data-course-occurrence-submit]:disabled")
                        && courseDetail.contains("submitButton.disabled = !valid"),
                "Course occurrence creation must use an academic-year picker that derives dates and blocks duplicate or overlapping occurrences");
        assertTrue(courseDetail.contains("data-gape-tree-toggle=\"occurrencePeriods")
                        && courseDetail.contains("data-gape-tree-toggle=\"completedCourseOccurrences\"")
                        && courseDetail.contains("gape-completed-occurrences-divider")
                        && courseDetail.contains("occurrence.stateValue eq 'completed'")
                        && courseDetail.contains("ph-caret-down"),
                "Course occurrences must use the Structure-style show/hide control and group completed entries at the end");
        assertFalse(courseDetail.contains("Draft — temporary state while the occurrence is being created."),
                "Course occurrence creation must not show an unnecessary Draft message");
        assertFalse(courseDetail.contains("subjectManagementContextHtml"),
                "Course detail subjects must not use the subject management context because it duplicates the course");
        assertTrue(courseDetail.contains("submitCourseLiveForm")
                        && courseDetail.contains("replaceCourseRootFrom")
                        && courseDetail.contains("reloadCourseRoot")
                        && courseDetail.contains("setCourseControlLoading(trigger")
                        && courseDetail.contains("form[data-course-live-form]")
                        && associateSubjectModal.contains("data-course-live-panel=\"structure\"")
                        && associateSubjectModal.contains("data-course-associate-subject-form")
                        && associateSubjectModal.contains("data-course-associate-subject")
                        && associateSubjectModal.contains("data-course-associate-year")
                        && associateSubjectModal.contains("data-course-associate-period")
                        && associateSubjectModal.contains("required disabled")
                        && associateSubjectModal.contains("name=\"subjectId\" required")
                        && associateSubjectModal.contains("name=\"curricularYear\" required disabled")
                        && associateSubjectModal.contains("name=\"term\" required disabled")
                        && courseOccurrencesPanel.contains("data-course-live-panel=\"occurrences\"")
                        && courseOccurrencesPanel.contains("name=\"referenceYear\" type=\"hidden\" required")
                        && courseEnrollmentManagement.contains("data-course-live-panel=\"enrollments\"")
                        && courseEnrollmentManagement.contains("data-course-enrollment-form")
                        && courseEnrollmentManagement.contains("data-course-enrollment-student")
                        && courseEnrollmentManagement.contains("data-course-enrollment-occurrence")
                        && courseEnrollmentManagement.contains("name=\"studentUserId\" required")
                        && courseEnrollmentManagement.contains("name=\"courseOccurrenceId\" required disabled")
                        && courseEnrollmentManagement.contains("${currentReturnTo}#course-enrollments")
                        && courseServlet.contains("#course-enrollments")
                        && !courseServlet.contains("#course-students")
                        && courseEnrollmentCard.contains("data-course-live-panel=\"enrollments\""),
                "Course Details card mutations must submit through the partial live-update contract");
        assertTrue(courseDetail.contains("configureCourseAssociateSubjectForms")
                        && courseDetail.contains("configureCourseEnrollmentForms")
                        && courseDetail.contains("select2:select.gapeCourseDependency")
                        && courseDetail.contains("select2:clear.gapeCourseDependency")
                        && courseDetail.contains("gape-select-field.is-disabled")
                        && courseDetail.contains("gape-select-field.is-available")
                        && courseDetail.contains("is-available .select2-selection__placeholder")
                        && courseDetail.contains("courseDependentState")
                        && courseDetail.contains("select.setAttribute('aria-disabled'")
                        && courseDetail.contains("setCourseDependentSelectDisabled(occurrence, !hasStudent)")
                        && courseDetail.contains("coursePanelUrl")
                        && courseDetail.contains("'#course-enrollments': 'enrollments'")
                        && courseDetail.contains("normal-navigation fallback")
                        && courseDetail.contains("revealCoursePanel(target);"),
                "Course Details forms must preserve their current card and enforce dependent selections");
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
                        "admin-dashboard.jsp",
                        "coordinator-dashbord.jsp",
                        "coordinator-dashboard.jsp",
                        "instructor-dashbord.jsp",
                        "instructor-dashboard.jsp",
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
                "<a\\s+href=\"\\$\\{pageContext\\.request\\.contextPath}\\$\\{siteHomeHref}\"\\s+"
                        + "class=\"dashboard-sidebar__site-logo\">\\s*<img",
                Pattern.CASE_INSENSITIVE
        );

        assertTrue(sidebar.contains("siteHomeHref"),
                "Dashboard sidebar should keep the site logo target separate from the dashboard target");
        assertTrue(sidebar.contains("value=\"/dashboard\""),
                "Dashboard sidebar logo must navigate to the authenticated dashboard");
        assertTrue(logoLinkPattern.matcher(sidebar).find(),
                "Dashboard sidebar logo must render using siteHomeHref instead of dashboardHref");
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
        assertTrue(sidebar.contains("/dashboard?tab=logs&amp;userId=${adminUserContextId}"),
                "Logs must open as a user-scoped Dashboard view");
        assertFalse(sidebar.contains("href=\"${pageContext.request.contextPath}/admin/deletion-requests\""),
                "Deletion must not remain an independent admin sidebar item");
        assertFalse(sidebar.contains("/admin/activity-log"),
                "Audit must not remain an independent page or sidebar endpoint");
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
        assertTrue(users.contains("/dashboard?tab=logs&amp;userId=${user.id}"),
                "Users actions must open user-scoped Logs inside Dashboard");
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
        assertTrue(detail.contains("gape-action-audit") && detail.contains(">View Logs</a>"),
                "View Logs critical action must be blue");
    }

    @Test
    void inactiveCourseAndSubjectDetailsUseRedSecondaryPalette() throws IOException {
        String courseDetail = Files.readString(WEBAPP_DIR.resolve("admin/admin/course/admin-course-detail.jsp"));
        String subjectDetail = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subject-detail.jsp"));

        for (String detail : List.of(courseDetail, subjectDetail)) {
            assertTrue(detail.contains(".cd-shell--inactive {")
                            && detail.contains("--cd-primary: #dc2626;")
                            && detail.contains("--cd-primary-dark: #b91c1c;")
                            && detail.contains("--cd-primary-soft: #fef2f2;")
                            && detail.contains("--cd-primary-surface: #fffafa;")
                            && detail.contains("--cd-primary-rgb: 220, 38, 38;")
                            && detail.contains("rgba(var(--cd-primary-rgb), 0.14)"),
                    "Inactive detail pages must replace their blue secondary palette with red tokens");
        }
        assertTrue(courseDetail.contains("cd-shell ${course.inactive ? 'cd-shell--inactive' : ''}")
                        && subjectDetail.contains("cd-shell ${subject.inactive ? 'cd-shell--inactive' : ''}"),
                "The inactive palette must be applied only when the displayed course or subject is inactive");
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
    void subjectAssociationsUseDetailModalsWithoutStandalonePages() throws IOException {
        String subjectCourseAssociations = Files.readString(FRAGMENTS_DIR.resolve("subject-course-associations-panel.jspf"));
        String subjectDetail = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subject-detail.jsp"));
        String subjectList = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subjects.jsp"));
        String subjectServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/SubjectManagementServlet.java"));
        String courseServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/CourseManagementServlet.java"));
        String courseDetail = Files.readString(WEBAPP_DIR.resolve("admin/admin/course/admin-course-detail.jsp"));
        String associateSubjectModal = Files.readString(FRAGMENTS_DIR.resolve("course-associate-subject-modal.jsp"));

        assertFalse(Files.exists(WEBAPP_DIR.resolve("admin/admin/course/admin-course-subject-form.jsp")),
                "The standalone Associate Subject page must not exist");
        assertTrue(courseDetail.contains("data-course-lazy-modal=\"associate-subject\"")
                        && associateSubjectModal.contains("${courseBasePath}/${course.id}/subjects\" method=\"post\"")
                        && courseServlet.contains("showAssociateSubjectModalFragment"),
                "Course Details must expose the Associate Subject modal and its POST action");
        assertFalse(courseServlet.contains("COURSE_SUBJECT_FORM_JSP")
                        || courseServlet.contains("showAssociationForm")
                        || courseServlet.contains("/subjects/new"),
                "Course routes must not expose the removed standalone association page");
        assertTrue(subjectCourseAssociations.contains("/courses/${association.courseId}/delete")
                        && subjectCourseAssociations.contains("Historical class groups and records remain available.")
                        && !subjectCourseAssociations.contains("fn:length(subjectCourseAssociations) > 1"),
                "Course Associations panel must allow the last association to be closed while retaining history");
        assertTrue(subjectCourseAssociations.contains("newSubjectCourseAssociationModal")
                        && subjectCourseAssociations.contains("data-bs-target=\"#newSubjectCourseAssociationModal\"")
                        && subjectCourseAssociations.contains("data-course-year-source=\"#newSubjectCourseId\"")
                        && subjectCourseAssociations.contains("data-course-source=\"#newSubjectCourseId\"")
                        && !subjectCourseAssociations.contains("id=\"newSubjectCourseAssociation\""),
                "Subject Details must associate a course exclusively through its modal, not an inline Course form");
        assertTrue(subjectDetail.contains("gape-subject-detail-panel")
                        && subjectDetail.contains("gape-subject-panel-header")
                        && subjectDetail.contains(">Structure</h3>")
                        && subjectDetail.contains(">Class Groups</span>"),
                "Subject Details must keep the Class Groups card while naming its content tab Structure");
        assertFalse(subjectServlet.contains("initialCourseAssociationCourseId")
                        || subjectCourseAssociations.contains("association.initialCourse"),
                "Subject details must not retain an Initial Course concept");
        assertFalse(subjectCourseAssociations.contains("name=\"approvalMode\"")
                        || subjectCourseAssociations.contains("subjectEnrollmentPolicy"),
                "Course Associations must not expose the removed subject enrollment policy");
        assertFalse(subjectList.contains("canManageSubjectAssociationsById")
                        || subjectList.contains("canManageSubjectAssociationsRow")
                        || subjectList.contains("${subjectBasePath}/${subject.id}/edit#course-associations"),
                "Subjects list must not expose the removed direct Course Associations action");
    }

    @Test
    void courseDetailsKeepsAllModalGeometryStable() throws IOException {
        String courseDetail = Files.readString(WEBAPP_DIR.resolve("admin/admin/course/admin-course-detail.jsp"));
        String associateSubjectModal = Files.readString(FRAGMENTS_DIR.resolve("course-associate-subject-modal.jsp"));

        assertTrue(courseDetail.contains("prepareCourseDetailModals(document)")
                        && courseDetail.contains("modal.classList.add('gape-course-modal-root')")
                        && courseDetail.contains("dialog.classList.add('gape-course-modal-dialog')")
                        && courseDetail.contains("gape-course-modal-dialog--association")
                        && courseDetail.contains(".modal.gape-course-modal-root")
                        && courseDetail.contains("height: var(--gape-course-modal-open-height, auto);")
                        && courseDetail.contains("max-height: calc(100vh - 2rem);")
                        && courseDetail.contains("overflow-y: auto;")
                        && courseDetail.contains("scrollbar-gutter: stable;"),
                "Every Course Details modal must preserve its opening size and centered position");
        assertTrue(courseDetail.contains("gape-course-select2-modal-portal")
                        && courseDetail.contains("GapeCourseModalSelect2Portal")
                        && courseDetail.contains("options.dropdownParent = window.jQuery(portal || modalParent);")
                        && courseDetail.contains(".select2-container > .dropdown-wrapper")
                        && courseDetail.contains("lockCourseModalGeometry")
                        && courseDetail.contains("unlockCourseModalGeometry")
                        && courseDetail.contains("shown.bs.modal")
                        && courseDetail.contains("hidden.bs.modal")
                        && associateSubjectModal.contains("js-example-basic-single gape-eduall-select"),
                "Course modal Select2 menus must stay outside modal layout while remaining selectable");
    }

    @Test
    void subjectDetailsNormalizesCoordinatorAndGradeSheetPanels() throws IOException {
        String subjectDetail = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subject-detail.jsp"));
        String allocations = Files.readString(FRAGMENTS_DIR.resolve("subject-allocations-panel.jsp"));
        String associations = Files.readString(FRAGMENTS_DIR.resolve("subject-course-associations-panel.jspf"));
        String subjectCourseSelectScript = Files.readString(WEBAPP_DIR.resolve("assets/js/gape-subject-course-select.js"));
        String courseYearSelectScript = Files.readString(WEBAPP_DIR.resolve("assets/js/gape-course-year-select.js"));
        String courseTermSelectScript = Files.readString(WEBAPP_DIR.resolve("assets/js/gape-course-term-select.js"));
        String gradeSheets = Files.readString(FRAGMENTS_DIR.resolve("subject-grade-sheet-panel.jsp"));
        String gradeSheetRow = Files.readString(FRAGMENTS_DIR.resolve("subject-grade-sheet-occurrence-row.jspf"));
        String subjectServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/SubjectManagementServlet.java"));
        String subjectService = Files.readString(JAVA_DIR.resolve("pt/isel/gape/learning/service/SubjectService.java"));
        String gradeServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/GradeCertificateServlet.java"));

        for (String panel : List.of(subjectDetail, allocations, associations, gradeSheets)) {
            assertTrue(panel.contains("gape-subject-detail-panel")
                            && panel.contains("gape-subject-panel-header")
                            && panel.contains("gape-subject-panel-actions"),
                    "Every Subject Details panel must share the same panel header and action layout");
        }
        assertTrue(subjectDetail.contains(".gape-enrollment-list-header,")
                        && subjectDetail.contains(".gape-enrollment-row {")
                        && subjectDetail.contains("minmax(250px, 1.25fr) minmax(220px, 1.05fr) minmax(135px, 0.55fr) minmax(92px, auto)"),
                "Coordinator Assignments and Subject Associations must use their full-width four-column grid");
        assertTrue(subjectDetail.contains("--gape-subject-structure-columns: minmax(260px, 1.15fr) minmax(130px, 0.38fr) minmax(110px, 0.28fr) minmax(0, 0.65fr) minmax(122px, auto)")
                        && subjectDetail.contains("#subject-structure .gape-enrollment-list-header,")
                        && subjectDetail.contains("#subject-structure .gape-enrollment-row {")
                        && subjectDetail.contains("grid-template-columns: var(--gape-subject-structure-columns);")
                        && subjectDetail.contains("#subject-structure .gape-enrollment-row > :nth-child(3)")
                        && subjectDetail.contains("grid-column: 5;")
                        && subjectDetail.contains("@media (min-width: 768px) and (max-width: 1199.98px)")
                        && subjectDetail.contains("--gape-subject-structure-columns: minmax(200px, 1fr) minmax(95px, auto) minmax(110px, auto) minmax(122px, auto)")
                        && subjectDetail.contains("grid-column: 4;")
                        && subjectDetail.contains(".gape-subject-class-group-card {")
                        && subjectDetail.contains(".gape-subject-class-group-card .gape-enrollment-row {")
                        && subjectDetail.contains("margin-inline: -16px;")
                        && subjectDetail.contains("padding-left: 16px;")
                        && subjectDetail.contains("transform: translateX(-16px);")
                        && subjectDetail.contains(".gape-completed-class-groups-content > [data-subject-detail-occurrence-group] {")
                        && subjectDetail.contains("#subject-grade-sheets .gape-subject-grade-sheet-list-header,")
                        && subjectDetail.contains("#subject-grade-sheets .gape-subject-grade-sheet-row,")
                        && subjectDetail.contains(".gape-published-grade-sheets-node > .gape-structure-row")
                        && gradeSheets.contains("gape-subject-grade-sheet-list-header")
                        && gradeSheetRow.contains("gape-subject-grade-sheet-row")
                        && gradeSheetRow.contains("gape-subject-grade-sheet-actions"),
                "Subject Structure and Grade Sheets must use explicit page-scoped grids at every nesting level");
        assertTrue(allocations.contains("modal-dialog-centered gape-subject-form-modal gape-subject-form-modal--coordinator")
            && !allocations.contains("gape-subject-stable-modal")
                        && allocations.contains("gape-subject-form-modal-root")
                        && allocations.contains("Search and select a coordinator by ID, name or email.")
                        && allocations.contains("#<c:out value=\"${coordinator.id}\"/>")
                        && allocations.contains("<c:out value=\"${coordinator.name}\"/>")
                        && allocations.contains("<c:out value=\"${coordinator.email}\"/>"),
                "Assign Coordinator must use a naturally sized centered modal and searchable ID, name and email labels");
        assertTrue(associations.contains("modal-dialog-centered gape-subject-form-modal gape-subject-form-modal--association")
                        && associations.contains("gape-subject-form-modal-root")
                        && associations.contains("id=\"editSubjectCourseAssociation${subject.id}_${association.courseId}\"")
                        && associations.contains("gape-subject-association-fields")
                        && associations.contains("gape-course-year-field")
                        && subjectDetail.contains(".modal-dialog.gape-subject-form-modal--coordinator .modal-content")
                        && subjectDetail.contains(".modal-dialog.gape-subject-form-modal--coordinator .modal-content > form")
                        && subjectDetail.contains("height: var(--gape-subject-modal-open-height, auto);")
                        && subjectDetail.contains("max-height: calc(100vh - 2rem);")
                        && subjectDetail.contains("flex: 1 1 auto;")
                        && subjectDetail.contains("overflow-y: auto;")
                        && subjectDetail.contains("scrollbar-gutter: stable;")
                        && subjectDetail.contains(".modal.gape-subject-form-modal-root")
                        && subjectDetail.contains("gape-subject-select2-modal-portal")
                        && subjectDetail.contains("padding-bottom: 24px;")
                        && subjectDetail.contains(".select2-container > .dropdown-wrapper")
                        && subjectDetail.contains("GapeSubjectModalSelect2Portal")
                        && subjectCourseSelectScript.contains("GapeSubjectModalSelect2Portal")
                        && subjectDetail.contains("lockSubjectFormModalGeometry")
                        && subjectDetail.contains("shown.bs.modal")
                        && subjectDetail.contains("hidden.bs.modal"),
                "Associate Course and Assign Coordinator must keep their natural opening geometry stable while remaining safe on short viewports");
        assertTrue(allocations.contains("aria-label=\"Delete coordinator assignment\"")
                        && allocations.contains("/coordinators/${assignment.coordinatorUserId}/delete")
                        && allocations.contains("data-subject-live-panel=\"allocations\"")
                        && allocations.contains("${currentReturnTo}#subject-coordinators")
                        && subjectDetail.contains("'#subject-coordinators': 'allocations'")
                        && subjectDetail.contains("subjectPanelUrl")
                        && subjectDetail.contains("normal-navigation fallback")
                        && subjectServlet.contains("#subject-coordinators")
                        && subjectServlet.contains("removeCoordinatorAssignment")
                        && subjectService.contains("SUBJECT_COORDINATOR_REMOVE"),
                "Coordinator Assignments must preserve their card after every mutation");
        assertTrue(associations.contains("data-course-term-year-source=\"#newSubjectCourseYear\"")
                        && associations.contains("name=\"term\" required disabled")
                        && associations.contains("data-course-term-submit disabled aria-disabled=\"true\"")
                        && subjectCourseSelectScript.contains("selectionCssClass: 'gape-eduall-selection'")
                        && !subjectCourseSelectScript.contains("gape-subject-course-selection")
                        && courseYearSelectScript.contains("new Event('gape:course-year-rebuilt')")
                        && courseTermSelectScript.contains("data-course-term-year-source")
                        && courseTermSelectScript.contains("gape:course-year-rebuilt")
                        && courseTermSelectScript.contains("gape-course-term-ready")
                        && courseTermSelectScript.contains("syncRequiredSubmit(select)")
                        && courseTermSelectScript.contains("select2:select.gapeCourseTermSubmit")
                        && courseTermSelectScript.contains("select2:clear.gapeCourseTermSubmit")
                        && courseTermSelectScript.contains("courseTermSubmitBound")
                        && courseTermSelectScript.contains("field.classList.toggle('is-disabled', select.disabled)"),
                "Associate Course must unlock the required Period after Course year, including automatic single-year selection");
        assertTrue(gradeSheets.contains("Published Grade Sheets")
                        && gradeSheets.contains("publishedSubjectGradeSheets${subject.id}")
                        && gradeSheets.contains("data-subject-detail-sort-toggle")
                        && gradeSheets.contains("data-subject-detail-sort-list")
                        && gradeSheetRow.contains("data-sort-date=\"${subjectGradeGroup.newestSheetId}\"")
                        && gradeSheetRow.contains("title=\"Detail\" aria-label=\"Grade sheet details\"")
                        && gradeSheetRow.contains("title=\"Download\" aria-label=\"Download grade sheet\"")
                        && gradeSheetRow.contains("ph ph-download-simple")
                        && gradeSheetRow.contains("subjectGradeSheetOccurrenceDisplayLabels[subjectGradeGroup.occurrenceId]")
                        && gradeSheetRow.contains("text-20 text-neutral-500 hover-text-main-600 border-0 bg-transparent p-0"),
                "Grade Sheets must have the standard sort control, published section, specific occurrence labels and Coordinator-style actions");
        assertTrue(gradeSheets.contains("<section class=\"cd-surface px-22 py-22 mb-20 gape-subject-detail-surface\" id=\"subject-grade-sheets\">")
                        && gradeSheets.contains("<c:if test=\"${empty subjectGradeSheetOccurrenceGroups}\">")
                        && gradeSheets.contains("px-18 py-28 text-center text-14 text-neutral-500\">No grade sheets found.</div>")
                        && !gradeSheets.contains("<c:when test=\"${subjectGradeSheetAvailable}\">")
                        && subjectDetail.contains("${subjectGradeSheetSheetCount} grade sheets")
                        && subjectDetail.contains("Open this tab to load grade sheets."),
                "Empty Grade Sheets must retain the same panel structure and lazy-tab behavior as empty Subject Associations");
        assertTrue(subjectServlet.contains("publishedSubjectGradeSheetOccurrenceGroups")
                        && subjectServlet.contains("publishedSubjectGradeSheetCount")
                        && subjectServlet.contains("GradeSheetSubjectGroupView::isPublished")
                        && subjectServlet.contains("subjectGradeSheetOccurrenceLabels")
                        && gradeServlet.contains("visibleSubjectGradeSheetOccurrenceGroups")
                        && gradeServlet.contains("public boolean isPublished()")
                        && gradeServlet.contains("Comparator.comparingLong(GradeSheetSubjectGroupView::getNewestSheetId).reversed()"),
                "Subject grade sheets must split published sheets and default to newest creation ID first");
    }

    @Test
    void subjectStructureKeepsClassGroupsDirectlyInsideCourseOccurrences() throws IOException {
        String courseDetail = Files.readString(WEBAPP_DIR.resolve("admin/admin/course/admin-course-detail.jsp"));
        String subjectDetail = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subject-detail.jsp"));
        String occurrenceNode = Files.readString(FRAGMENTS_DIR.resolve("subject-class-group-occurrence-node.jspf"));
        String classGroupRow = Files.readString(FRAGMENTS_DIR.resolve("subject-class-group-row.jspf"));
        String subjectServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/SubjectManagementServlet.java"));
        String classGroupView = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/view/ClassGroupView.java"));
        String viewFactory = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/LearningViewFactory.java"));
        String occurrenceGroupView = Files.readString(
                JAVA_DIR.resolve("pt/isel/gape/web/view/ClassGroupOccurrenceGroupView.java")
        );

        assertTrue(occurrenceNode.contains("items=\"${occurrenceGroup.classGroups}\"")
                        && occurrenceNode.contains("subject-class-group-row.jspf")
                        && occurrenceNode.contains("data-subject-detail-sort-list")
                        && occurrenceNode.contains("occurrenceGroup.disclosureId")
                        && occurrenceNode.contains("gape-enrollment-row")
                        && occurrenceNode.contains("gape-subject-occurrence-groups-panel__heading")
                        && occurrenceNode.contains("Hide class groups")
                        && occurrenceNode.contains("Show class groups")
                        && !occurrenceNode.contains("occurrenceGroup.courseGroups")
                        && !occurrenceNode.contains("subject-class-group-course-node.jspf"),
                "Subject Structure must keep class groups directly inside each course occurrence");
        assertTrue(subjectDetail.contains("<span>Class Groups</span>")
                        && subjectDetail.contains("Class groups grouped by course occurrence and their activities.")
                        && subjectDetail.contains("occurrenceGroupStartsOpen\" value=\"true\"")
                        && classGroupRow.contains("gape-subject-class-group-card")
                        && classGroupRow.contains("gape-enrollment-row")
                        && !subjectDetail.contains(".gape-subject-course-node"),
                "Subject Structure must use the Course Enrollments hierarchy without a separate course layer");
        assertTrue(courseDetail.contains(".gape-course-enrollment-student-panel {\n"
                                + "            border-top: 1px solid var(--cd-border);\n"
                                + "            margin-top: 16px;\n"
                                + "            padding-top: 14px;")
                        && courseDetail.contains(".gape-course-enrollment-occurrence-card {")
                        && courseDetail.contains("padding: 12px 16px !important;")
                        && courseDetail.contains(".gape-completed-occurrences-content {\n"
                                + "            padding: 16px 32px 0;")
                        && subjectDetail.contains(".gape-subject-occurrence-groups-panel {\n"
                                + "            border-top: 1px solid var(--cd-border);\n"
                                + "            margin-top: 16px;\n"
                                + "            padding-top: 14px;")
                        && subjectDetail.contains(".gape-subject-occurrence-groups-content {\n"
                                + "            display: flex;\n"
                                + "            flex-direction: column;\n"
                                + "            gap: 10px;")
                        && subjectDetail.contains(".gape-subject-class-group-card {")
                        && subjectDetail.contains("margin-inline: -16px;")
                        && subjectDetail.contains("padding-left: 16px;")
                        && subjectDetail.contains("transform: translateX(-16px);")
                        && !subjectDetail.contains("margin-right: -48px;")
                        && subjectDetail.contains(".gape-completed-class-groups-content {\n"
                                + "            padding: 16px 32px 0;")
                        && subjectDetail.contains("gape-completed-class-groups-content d-flex flex-column gap-10")
                        && !subjectDetail.contains("#subject-structure .gape-structure-row > :last-child > .gape-tree-toggle"),
                "Subject Structure must retain the exact Course Enrollments spacing and unstyled action controls at every depth");
        assertTrue(occurrenceGroupView.contains("public List<ClassGroupView> getClassGroups()")
                        && occurrenceGroupView.contains("public long getDisclosureId()")
                        && !occurrenceGroupView.contains("ClassGroupCourseGroupView")
                        && !Files.exists(JAVA_DIR.resolve("pt/isel/gape/web/view/ClassGroupCourseGroupView.java")),
                "Occurrence groups must preserve their direct newest-first class-group list without a course intermediary");
        assertTrue(subjectServlet.contains("Map<Long, List<ClassGroupView>> byOccurrencePeriod")
                        && subjectServlet.contains("classGroup.getCourseOccurrencePeriodId()")
                        && subjectServlet.contains("occurrenceStructureLabel(first)")
                        && subjectServlet.contains("occurrenceLabel + \" - \" + periodLabel + \" — \" + classGroup.getCourseName()")
                        && classGroupView.contains("getOccurrencePeriodDateRangeLabel")
                        && viewFactory.contains("occurrencePeriodDateRangeLabel(context)")
                        && viewFactory.contains("Map<Long, CourseOccurrenceContext> occurrenceContexts"),
                "Subject Structure occurrences must include their full course name and remain concrete per course period");
    }

    @Test
    void activeSubjectsKeepNewGroupVisibleWhenCreationIsUnavailable() throws IOException {
        String subjectDetail = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subject-detail.jsp"));
        String subjectServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/SubjectManagementServlet.java"));

        assertTrue(subjectDetail.contains("<c:if test=\"${not subject.inactive}\">")
                        && subjectDetail.contains("<c:when test=\"${canCreateClassGroupsForSubject}\">")
                        && subjectDetail.contains("classGroupCreationUnavailableReason")
                        && subjectDetail.contains("cd-disabled-action-wrapper")
                        && subjectDetail.contains("data-bs-toggle=\"tooltip\"")
                        && subjectDetail.contains("disabled aria-disabled=\"true\""),
                "An active subject must keep New Group visible as a disabled action with an explanatory tooltip");
        assertFalse(subjectDetail.contains("not subject.inactive and canCreateClassGroupsForSubject"),
                "New Group must not disappear solely because class group creation is unavailable");
        assertTrue(subjectServlet.contains("classGroupCreationAvailabilityForSubject")
                        && subjectServlet.contains("Associate this subject with an active course before creating a class group.")
                        && subjectServlet.contains("classGroupCreationUnavailableReason"),
                "Subject details must provide the concrete reason why New Group is unavailable");
    }

    @Test
    void assessmentDetailUsesBuilderEnrollmentsAndAttemptsPrimaryCards() throws IOException {
        String detail = Files.readString(WEBAPP_DIR.resolve("WEB-INF/views/learning/assessment-builder.jsp"));
        String servlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/AssessmentManagementServlet.java"));
        String enrollmentManagement = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/assessment-enrollment-management.jspf"));
        String attemptManagement = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/assessment-attempt-management.jspf"));
        String enrollmentRows = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/assessment-enrollment-row.jspf"));
        String enrollmentService = Files.readString(JAVA_DIR.resolve("pt/isel/gape/learning/service/AssessmentEnrollmentService.java"));
        String enrollmentDao = Files.readString(JAVA_DIR.resolve("pt/isel/gape/learning/dao/AssessmentEnrollmentDAO.java"));
        String assessmentDao = Files.readString(JAVA_DIR.resolve("pt/isel/gape/learning/dao/AssessmentDAO.java"));
        String fullSeed = Files.readString(Path.of("src/main/resources/sql/seed/full.sql"));
        String studentAccessSql = assessmentDao.substring(
                assessmentDao.indexOf("public List<Assessment> findActiveAccessibleByStudent"),
                assessmentDao.indexOf("public void update(")
        );

        int builderCard = detail.indexOf("data-ad-tab=\"builder\"");
        int enrollmentsCard = detail.indexOf("data-ad-tab=\"enrollments\"", builderCard);
        int attemptsCard = detail.indexOf("data-ad-tab=\"attempts\"", enrollmentsCard);
        int builderPanel = detail.indexOf("id=\"builder\"");
        int enrollmentsPanel = detail.indexOf("id=\"enrollments\"", builderPanel);
        int attemptsPanel = detail.indexOf("id=\"attempts\"", enrollmentsPanel);
        assertTrue(builderCard > 0
                        && enrollmentsCard > builderCard
                        && attemptsCard > enrollmentsCard
                        && builderPanel > 0
                        && enrollmentsPanel > builderPanel
                        && attemptsPanel > enrollmentsPanel
                        && enrollmentManagement.contains(">Enrollments</h3>")
                        && attemptManagement.contains(">Attempts</h3>")
                        && detail.contains("data-assessment-lazy-panel=\"enrollments\"")
                        && detail.contains("data-assessment-lazy-panel=\"attempts\"")
                        && detail.contains("ad-mode-card__title")
                        && detail.contains("title.innerHTML = loadingMarkup();")
                        && !detail.contains("tab.innerHTML = loadingMarkup();")
                        && detail.contains("var loaded = await loadLazyPanel(target);")
                        && detail.contains("revealPanel(target);")
                        && detail.indexOf("var loaded = await loadLazyPanel(target);")
                                < detail.indexOf("revealPanel(target);")
                        && !detail.contains("window.setTimeout(function () { activatePanel(tab.getAttribute('data-ad-tab')); }, 0);")
                        && detail.contains("grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));")
                        && detail.contains("min-height: 126px;")
                        && detail.contains("min-width: 0;")
                        && detail.contains(".ad-hero > .d-flex > .d-flex:first-child")
                        && detail.contains(".ad-hero h2")
                        && detail.contains("ad-hero-meta")
                        && detail.contains("ad-enrollment-policy-form")
                        && detail.contains("ad-enrollment-create-form")
                        && detail.contains("showAssessmentEnrollmentCreate")
                        && detail.contains(".dashbord-body")
                        && detail.contains(".ad-table-wrap")
                        && detail.contains(".ad-data-table thead")
                        && detail.contains("ad-mode-content")
                        && detail.contains("ad-mode-description")
                        && detail.contains("ad-mode-metric")
                        && detail.contains("font-size: 20px;")
                        && detail.contains("flex-wrap: wrap;")
                        && detail.contains("flex-basis: 100%;")
                        && detail.contains("flex-direction: column;")
                        && detail.contains(".ad-enrollment-create-form .row > [class*=\"col\"]")
                        && detail.contains(".ad-hero-meta > span:nth-child(even)")
                        && detail.contains("overflow-wrap: anywhere;")
                        && detail.contains("white-space: normal !important;")
                        && detail.contains("name=\"approvalMode\"")
                        && detail.contains("#enrollments")
                        && detail.contains("panel.getAttribute('data-ad-panel') !== name")
                        && detail.contains("data-ad-enrollment-tab=\"requests\"")
                        && detail.contains("data-ad-enrollment-tab=\"enrollments\"")
                        && detail.contains("showAssessmentEnrollmentRequests")
                        && detail.contains("items=\"${pendingAssessmentEnrollments}\"")
                        && detail.contains("items=\"${managedAssessmentEnrollments}\"")
                        && detail.contains("aria-label=\"Delete enrollment\"")
                        && detail.contains("items=\"${attempts}\"")
                        && detail.contains("id=\"correctAttemptModal${attempt.id}\"")
                        && detail.contains("data-bs-target=\"#correctAttemptModal${attempt.id}\"")
                        && detail.contains("responsesByAttemptId")
                        && detail.contains("manualCorrectionForm${attempt.id}")
                        && detail.contains("aria-label=\"Auto correct response\"")
                        && detail.contains("/responses/${response.id}/auto-correct")
                        && detail.contains("/attempts/${attempt.id}/pdf")
                        && detail.contains("title=\"Download\"")
                        && detail.contains("ad-correction-download-button")
                        && detail.contains("ad-correction-choice-control--radio")
                        && detail.contains("ad-correction-choice-control--checkbox")
                        && detail.contains("ad-correction-text-answer--long")
                        && detail.contains("ad-correction-upload-answer")
                        && detail.contains("ad-correction-rating")
                        && detail.contains("ad-correction-expected-answer")
                        && detail.contains("data-ad-expected-toggle")
                        && detail.contains("data-ad-expected-panel hidden")
                        && detail.contains("aria-controls=\"attemptExpectedAnswer${attempt.id}_${response.id}\"")
                        && detail.contains("id=\"attemptExpectedAnswer${attempt.id}_${response.id}\"")
                        && detail.contains("Show expected answer")
                        && detail.contains("Hide expected answer")
                        && detail.contains("Expected answer")
                        && detail.contains("response.objectiveWithExpectedAnswer")
                        && detail.contains("response.expectedOptionTokens")
                        && detail.contains("response.objectiveAnswerToneClass")
                        && detail.contains("is-correct")
                        && detail.contains("is-incorrect")
                        && detail.contains("is-missed")
                        && detail.contains("is-neutral")
                        && detail.contains("data-ad-auto-correct-form")
                        && detail.contains("Auto all legible")
                        && detail.contains("form=\"manualCorrectionForm${attempt.id}\"")
                        && detail.contains("id=\"assessmentSetupModal\"")
                        && detail.contains("data-bs-target=\"#assessmentSetupModal\"")
                        && detail.contains("Final weight")
                        && detail.contains("assessment.finalGradeWeight")
                        && detail.contains("focusPendingScore(firstInvalid)")
                        && detail.contains("card.scrollIntoView({ behavior: 'smooth', block: 'center' })")
                        && detail.contains("background: #fff;")
                        && fullSeed.contains("Correct Attempt QA Matrix")
                        && fullSeed.contains("QA-SC-C")
                        && fullSeed.contains("QA-SC-W")
                        && fullSeed.contains("QA-SC-N")
                        && fullSeed.contains("QA-MC-MIX")
                        && fullSeed.contains("QA-MC-N")
                        && fullSeed.contains("QA-RT-C")
                        && fullSeed.contains("QA-RT-W")
                        && fullSeed.contains("QA-ST-E")
                        && fullSeed.contains("QA-PA-E")
                        && fullSeed.contains("QA-UP-E")
                        && fullSeed.contains("QA-ST-N")
                        && servlet.contains("responseViewsByAttempt(attempts)")
                        && servlet.contains("downloadAttemptPdf(")
                        && servlet.contains("renderAttemptResponsesPdf(assessmentId, attemptId)")
                        && servlet.contains("autoCorrectEligibleResponses(")
                        && servlet.contains("\"auto-correct-eligible\"")
                        && servlet.contains("writeCorrectionResult(")
                        && servlet.contains("redirect(request, response, \"/learning/assessments/\" + Long.parseLong(segments[0]) + \"#attempts\")")
                        && servlet.contains("showAssessmentEnrollmentCreate")
                        && studentAccessSql.contains("DATE(assessment.available_from)")
                        && studentAccessSql.contains("DATE(assessment.available_until)")
                        && !studentAccessSql.contains("DATE(a.available_from)")
                        && !studentAccessSql.contains("DATE(a.available_until)"),
                "Assessment Detail must expose Builder, Enrollments and Attempts as the three primary cards");
        assertTrue(enrollmentRows.contains("/approve")
                        && enrollmentRows.contains("/reject")
                        && enrollmentRows.contains("data-assessment-live-enrollment-request-action")
                        && detail.contains("fetch(withCurrentSession(form.action)")
                        && detail.contains("new URLSearchParams(new FormData(form)).toString()"),
                "Assessment enrollment requests must use the live, URL-rewritten and CSRF-safe form flow");
        assertFalse(enrollmentRows.contains("/update")
                        || enrollmentRows.contains("name=\"state\"")
                        || detail.contains("/enrollments/${enrollment.studentUserId}/update")
                        || servlet.contains("case \"update\" -> updateEnrollment")
                        || servlet.contains("private void updateEnrollment(")
                        || enrollmentService.contains("updateAssessmentEnrollment(")
                        || enrollmentDao.contains("public void updateEnrollment("),
                "Assessment enrollment state must follow the automatic lifecycle rather than a free-form update route");
        assertFalse(detail.contains(">Setup</h3>")
                        || detail.contains("data-ad-tab=\"enrollments-attempts\"")
                        || detail.contains("data-ad-panel=\"enrollments-attempts\"")
                        || detail.contains("Enrollments &amp; Attempts")
                        || detail.contains("Current assessment configuration.")
                        || detail.contains(">Attempt Queue</h3>")
                        || detail.contains("Questions, options, scores and expected answers.")
                        || detail.contains("Students, approval policy and enrollment requests.")
                        || detail.contains("Submissions, correction queue and results.")
                        || detail.contains("Maximum Grade:")
                        || detail.contains("ad-enrollment-icon-button ad-enrollment-icon-button--neutral\" title=\"Download\"")
                        || detail.contains("data-bs-dismiss=\"modal\">Close</button>")
                        || detail.contains("attemptCorrectionUrl")
                        || detail.contains("ad-correction-top-actions")
                        || detail.contains("button.disabled = controls.some")
                        || servlet.contains("ASSESSMENT_CORRECTION_JSP")
                        || servlet.contains("assessment-correction.jsp"),
                "Assessment Detail must not keep the old combined cards, setup tabs or verbose card metadata");
    }

    @Test
    void assessmentDetailKeepsEnrollmentDecisionsAndAttemptQueuesConsistent() throws IOException {
        String enrollmentManagement = Files.readString(FRAGMENTS_DIR.resolve("assessment-enrollment-management.jspf"));
        String enrollmentRows = Files.readString(FRAGMENTS_DIR.resolve("assessment-enrollment-row.jspf"));
        String attemptManagement = Files.readString(FRAGMENTS_DIR.resolve("assessment-attempt-management.jspf"));
        String attemptRows = Files.readString(FRAGMENTS_DIR.resolve("assessment-attempt-row.jspf"));
        String detail = Files.readString(WEBAPP_DIR.resolve("WEB-INF/views/learning/assessment-builder.jsp"));
        String mainCss = Files.readString(WEBAPP_DIR.resolve("assets/css/main.css"));
        String dashboardSidebar = Files.readString(FRAGMENTS_DIR.resolve("dashboard-sidebar.jspf"));
        String studentDashboardSidebar = Files.readString(FRAGMENTS_DIR.resolve("student-dashboard-sidebar.jspf"));
        String lessonManagementServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/LessonManagementServlet.java"));
        String lessonList = Files.readString(WEBAPP_DIR.resolve("WEB-INF/views/learning/lesson-list.jsp"));
        String legacyAdminDashboard = Files.readString(WEBAPP_DIR.resolve("admin/admin-dashbord.jsp"));

        assertTrue(enrollmentManagement.contains("items=\"${managedAssessmentEnrollments}\"")
                        && !enrollmentManagement.contains("Show enrollment history")
                        && !enrollmentManagement.contains("assessmentEnrollmentHistory"),
                "The Enrollments tab must render all managed enrollment states without a hidden history section");
        assertTrue(enrollmentRows.contains("ad-detail-action--decision")
                        && enrollmentRows.contains("Decide Enrollment Request")
                        && enrollmentRows.contains("data-assessment-live-enrollment-request-action")
                        && enrollmentRows.contains("ad-enrollment-modal-dialog")
                        && enrollmentRows.contains("ad-enrollment-decision-modal-dialog"),
                "Each enrollment request must expose one highlighted live decision control with a fixed-size modal");
        int assessmentDecisionAction = enrollmentRows.indexOf("ad-detail-action--decision");
        int assessmentDetailAction = enrollmentRows.indexOf("aria-label=\"Enrollment details\"");
        int assessmentDeleteAction = enrollmentRows.indexOf("aria-label=\"Delete enrollment\"");
        assertTrue(assessmentDecisionAction >= 0
                        && assessmentDetailAction > assessmentDecisionAction
                        && assessmentDeleteAction > assessmentDetailAction,
                "Assessment enrollment requests must order decision, detail and delete actions consistently");
        assertTrue(detail.contains(".ad-danger-button {")
                        && detail.contains("background: #dc2626;"),
                "Destructive enrollment decisions must use the same complete button grammar as the other modal actions");
        assertTrue(attemptManagement.contains("nonSubmittedAttemptCount")
                        && attemptManagement.contains("<c:if test=\"${not attempt.submitted}\">")
                        && attemptManagement.contains("<c:if test=\"${attempt.submitted}\">")
                        && attemptManagement.contains("Pending correction"),
                "Attempts must separate every submitted attempt into the pending-correction queue");
        assertTrue(attemptRows.contains("ad-attempt-cell--student")
                        && attemptRows.contains("ad-attempt-cell--submitted")
                        && attemptRows.contains("ad-attempt-cell--score")
                        && attemptRows.contains("ad-attempt-cell--state")
                        && attemptRows.contains("ad-attempt-cell--actions")
                        && detail.contains("#assessment-attempts .ad-attempt-row")
                        && detail.contains(".ad-attempt-row { align-items: stretch; }")
                        && detail.contains(".ad-attempt-cell--submitted,")
                        && detail.contains("#assessment-attempts .ad-attempt-cell--state {\n                align-items: flex-start;\n                display: block;")
                        && detail.contains(".ad-detail-action--decision")
                        && detail.contains("minmax(250px, 1.45fr)")
                        && detail.contains("height: 34px;")
                        && detail.contains("width: 34px;")
                        && attemptRows.contains("ad-detail-action--decision")
                        && detail.contains("function correctionFormBody(form)")
                        && detail.contains("body: correctionFormBody(form)")
                        && detail.contains("Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'")
                        && detail.contains("correctionInputs(form).forEach"),
                "Attempt rows must use one explicit fixed-action grid grammar that remains readable at narrow widths");
        assertTrue(attemptRows.contains("<span>-</span>")
                        && attemptRows.contains("<c:otherwise>-</c:otherwise>")
                        && !attemptRows.contains("Waiting for submission")
                        && !attemptRows.contains("Correction unavailable")
                        && !attemptRows.contains("Not submitted")
                        && !detail.contains("payload.scoreOverMax || 'Not assigned yet'"),
                "Assessment Attempts must use a neutral dash for unavailable cells instead of verbose status warnings");
        assertTrue(enrollmentRows.contains("ad-assignment-cell--availability")
                        && enrollmentRows.contains("ad-assignment-cell--state")
                        && enrollmentRows.contains("ad-assignment-cell--actions")
                        && detail.contains("#assessment-enrollments .ad-structure-cell-label"),
                "Enrollment rows must expose the same labelled card grammar when table headers collapse on mobile");
        assertEquals(9, dashboardSidebar.split("gape-sidebar-badged-item", -1).length - 1,
                "Every shared dashboard event span must have an explicit positioned menu-item anchor");
        assertEquals(4, studentDashboardSidebar.split("gape-sidebar-badged-item", -1).length - 1,
                "Every student dashboard event span must have an explicit positioned menu-item anchor");
        assertTrue(mainCss.contains(".gape-sidebar-badged-item > .gape-sidebar-event-badge")
                        && mainCss.contains("inset: 0 auto auto 0 !important;")
                        && mainCss.contains("margin: 0 !important;")
                        && mainCss.contains("min-height: 20px;")
                        && mainCss.contains("min-width: 20px;")
                        && mainCss.contains("padding: 1px 5px !important;")
                        && mainCss.contains("transform: none !important;"),
                "Sidebar event spans must be compact, fully visible, and anchored at the upper-left edge of their links");
        assertTrue(lessonManagementServlet.contains("request.setAttribute(\"eventListUnreadCount\", unreadEventCount);")
                        && !lessonManagementServlet.contains("request.setAttribute(\"eventUnreadCount\", unreadEventCount);")
                        && lessonList.contains("${eventListUnreadCount}"),
                "Lessons & Assessments must keep the global Events sidebar counter separate from the filtered Events-panel count");
        assertTrue(legacyAdminDashboard.contains("assets/css/main.css?v="),
                "The legacy dashboard must request the current sidebar CSS instead of a stale cached asset");
    }

    @Test
    void submittedAttemptsDoNotExposeScoresUntilCorrected() throws IOException {
        String attemptView = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/view/AttemptView.java"));
        String servlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/AssessmentManagementServlet.java"));
        String pdfService = Files.readString(JAVA_DIR.resolve("pt/isel/gape/learning/service/AssessmentPdfService.java"));
        String studentResult = Files.readString(WEBAPP_DIR.resolve("WEB-INF/views/student/assessment-result.jsp"));
        String schema = Files.readString(Path.of("src/main/resources/sql/schema.sql"));
        String baseSeed = Files.readString(Path.of("src/main/resources/sql/seed/base.sql"));
        String fullSeed = Files.readString(Path.of("src/main/resources/sql/seed/full.sql"));
        Pattern submittedAttemptWithScore = Pattern.compile(
                "\\([^)]*,\\s*[^)]*,\\s*[^)]*,\\s*[^)]*,\\s*(?!NULL\\b)[0-9]+(?:\\.[0-9]+)?,\\s*'submitted'",
                Pattern.CASE_INSENSITIVE
        );

        assertTrue(attemptView.contains("private BigDecimal visibleScore()")
                        && attemptView.contains("AttemptState.CORRECTED ? attempt.score() : null")
                        && servlet.contains("visibleAttemptScore(updatedAttempt)")
                        && servlet.contains("private static BigDecimal visibleAttemptScore(Attempt attempt)")
                        && servlet.contains("AttemptState.CORRECTED ? attempt.score() : null")
                        && pdfService.contains("attempt.state() == AttemptState.CORRECTED")
                        && pdfService.contains("responseScoreLabel(response, question, scoresVisible)")
                        && pdfService.contains("!scoresVisible || response == null || response.score() == null")
                        && studentResult.contains("attempt.corrected and response.scored")
                        && studentResult.contains("<c:otherwise>Not assigned yet</c:otherwise>")
                        && schema.contains("ck_attempt_score_requires_correction")
                        && schema.contains("Only corrected Attempt can store score"),
                "Submitted attempts must keep total and response scores hidden until the attempt is corrected");
        assertFalse(submittedAttemptWithScore.matcher(baseSeed).find()
                        || submittedAttemptWithScore.matcher(fullSeed).find(),
                "Seed data must not contain submitted attempts with a persisted total score");
    }

    @Test
    void coordinatorSidebarLinksToManagedSubjects() throws IOException {
        String sidebar = Files.readString(FRAGMENTS_DIR.resolve("dashboard-sidebar.jspf"));
        String coordinatorProfile = Files.readString(WEBAPP_DIR.resolve("coordinator/coordinator-my-profile.jsp"));
        String subjectList = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subjects.jsp"));
        String subjectDetail = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subject-detail.jsp"));
        String subjectForm = Files.readString(WEBAPP_DIR.resolve("admin/admin/subject/admin-subject-form.jsp"));
        String userForm = Files.readString(WEBAPP_DIR.resolve("admin/admin/user/admin-user-form.jsp"));
        String subjectCourseAssociations = Files.readString(FRAGMENTS_DIR.resolve("subject-course-associations-panel.jspf"));
        String subjectAllocations = Files.readString(FRAGMENTS_DIR.resolve("subject-allocations-panel.jsp"));

        assertTrue(coordinatorProfile.contains("DashboardMyProfilePageData")
                        && !coordinatorProfile.contains("/admin/admin-my-profile.jsp"),
                "Coordinator profile must be actor-owned and keep the administrator visual contract");
        assertTrue(sidebar.contains("/coordinator/subjects")
                        && sidebar.contains("coordinatorSubjectContextId")
                        && sidebar.contains("coordinatorSubjectActiveChild"),
                "Coordinator sidebar must expose the admin-like subject navigation under the coordinator route");
        assertTrue(sidebar.contains("not isCoordinatorDashboard")
                        && sidebar.contains("/coordinator/courses"),
                "Coordinator sidebar must expose coordinator-scoped courses without exposing the generic courses tab");
        assertTrue(sidebar.contains("/learning/lessons")
                        && sidebar.contains("Lessons &amp; Assessments")
                        && sidebar.contains("dashboard-learning-assessment-context.jspf"),
                "Coordinator sidebar must expose one combined lessons and assessments entry");
        int coordinatorSection = sidebar.indexOf(">Coordinator<");
        int coordinatorCoursesLink = sidebar.indexOf("/coordinator/courses", coordinatorSection);
        int coordinatorSubjectsLink = sidebar.indexOf("/coordinator/subjects", coordinatorCoursesLink);
        int coordinatorClassGroupsLink = sidebar.indexOf("/learning/class-groups", coordinatorSubjectsLink);
        int coordinatorLessonsLink = sidebar.indexOf("/learning/lessons", coordinatorClassGroupsLink);
        int messageHref = sidebar.indexOf("${messageHref}");
        int calendarHref = sidebar.indexOf("${calendarHref}", messageHref);
        int roomsHref = sidebar.indexOf("/learning/rooms", calendarHref);
        int attendanceHref = sidebar.indexOf("${attendanceHref}", coordinatorLessonsLink);
        assertTrue(messageHref < calendarHref
                        && calendarHref < roomsHref
                        && roomsHref < coordinatorSection
                        && coordinatorSection < coordinatorCoursesLink
                        && coordinatorCoursesLink < coordinatorSubjectsLink
                        && coordinatorSubjectsLink < coordinatorClassGroupsLink
                        && coordinatorClassGroupsLink < coordinatorLessonsLink
                        && coordinatorLessonsLink < attendanceHref,
                "Coordinator sidebar must keep rooms after calendar and attendance after lessons and assessments");
        assertTrue(subjectList.contains("${subjectBasePath}/${subject.id}")
                        && subjectDetail.contains("${subjectBasePath}/${subject.id}/edit")
                        && subjectDetail.contains("subject-course-associations-panel.jspf")
                        && subjectCourseAssociations.contains("${subjectBasePath}/${subject.id}/courses")
                        && subjectCourseAssociations.contains("${subjectCourseBasePath}/${association.courseId}"),
                "Shared subject views must use the request-scoped base path for admin and coordinator routes");
        assertFalse(subjectForm.contains("subject-course-associations-panel.jspf")
                        || subjectForm.contains("subject-enrollment-management.jspf"),
                "Subject edit form must not include the removed Subject Associations or Subject Enrollments tabs");
        assertTrue(subjectForm.contains("<option value=\"INACTIVE\" ${form.state == 'INACTIVE' ? 'selected' : ''}>Inactive</option>"),
                "Subject State must always offer Inactive, including when active course associations exist");
        assertFalse(subjectForm.contains("data-disabled-option-tooltip")
                        || subjectForm.contains("gapeSubjectStateTooltip")
                        || subjectForm.contains("Remove active course associations before deactivating this subject."),
                "Subject forms must not keep the removed active-association deactivation restriction");
        assertTrue(subjectCourseAssociations.contains("newSubjectCourseAssociationModal")
                        && !subjectCourseAssociations.contains("hideSubjectCourseAssociationAddForm"),
                "Subject Details must own Course Associations through its modal");
        assertFalse(subjectForm.contains("initialCourseId")
                        || subjectForm.contains("initialCurricularYear")
                        || subjectForm.contains("initialTerm")
                        || subjectForm.contains("initialMandatory"),
                "Subject forms must allow a subject to exist before it is associated with a course");
        assertFalse(subjectForm.contains("initialApprovalMode")
                        || subjectCourseAssociations.contains("approvalMode"),
                "Subject forms must not expose the removed subject enrollment approval mode");
        assertFalse(subjectForm.contains("editCoordinatorUserId")
                        || subjectForm.contains("subjectCoordinatorAssignmentForm")
                        || subjectForm.contains("coordinatorUserId"),
                "Create Subject and Edit Subject must not manage coordinator assignments");
        assertTrue(subjectAllocations.contains("newCoordinatorAssignmentModal")
                        && subjectAllocations.contains("Assign Coordinator"),
                "Subject Details Coordinators panel must be the exclusive coordinator-assignment interface");
        assertFalse(userForm.contains("Coordinator Context")
                        || userForm.contains("data-profile-type=\"COORDINATOR\""),
                "User forms must not provide a second coordinator-subject assignment interface");
        assertTrue(subjectDetail.contains("<c:if test=\"${canModifySubject}\">")
                        && subjectDetail.contains("${subjectBasePath}/${subject.id}/unarchive")
                        && subjectDetail.contains("${subject.inactive and canModifySubject}"),
                "An inactive subject must keep Edit and show Activate beside it on Subject Details");
        assertTrue(subjectForm.contains("pattern=\"[^\\|]*\""),
                "Subject form text patterns must escape the pipe for browser pattern validation");
        assertFalse(subjectForm.contains("gape-subject-required-toggle__icon"),
                "Create Subject Mandatory Subject toggle must not render the old circular icon");
    }

    @Test
    void sidebarShowsRoomsAsManagementMenu() throws IOException {
        String sidebar = Files.readString(FRAGMENTS_DIR.resolve("dashboard-sidebar.jspf"));
        String mainCss = Files.readString(ASSETS_DIR.resolve("css/main.css"));

        int messageHref = sidebar.indexOf("${messageHref}");
        int calendarHref = sidebar.indexOf("${calendarHref}", messageHref);
        int roomsHref = sidebar.indexOf("/learning/rooms", calendarHref);
        int lessonsHref = sidebar.indexOf("/learning/lessons", roomsHref);
        int attendanceHref = sidebar.indexOf("${attendanceHref}", lessonsHref);
        assertTrue(sidebar.contains("/learning/events")
                        && sidebar.contains("/student/events")
                        && sidebar.contains("<c:set var=\"calendarLabel\" value=\"Events\" />")
                        && sidebar.contains("<c:set var=\"calendarIcon\" value=\"ph ph-calendar-dots\" />")
                        && sidebar.contains("Lessons &amp; Assessments")
                        && sidebar.contains("Enrollments &amp; Certificates")
                        && sidebar.contains("not isStudentOnlyDashboard and canOpenAttendance")
                        && sidebar.contains("gape-sidebar-long-link")
                        && sidebar.contains("gape-sidebar-link-label")
                        && messageHref < calendarHref
                        && calendarHref < roomsHref
                        && roomsHref < lessonsHref
                        && lessonsHref < attendanceHref,
                "Sidebar must expose Events after Message, Rooms after Events and Attendance after Lessons & Assessments");
        assertTrue(sidebar.contains("/learning/rooms"),
                "Sidebar must expose the physical room management route");
        assertTrue(sidebar.contains("Rooms"),
                "Physical room management must be labelled as Rooms in the sidebar");
        assertFalse(sidebar.contains("Physical Rooms"),
                "Physical room management must not remain as a separately named Physical Rooms menu");
        assertTrue(sidebar.contains("or isCoordinatorDashboard or isTeacherDashboard"),
                "Rooms menu visibility must include coordinator and teacher dashboards");
        assertTrue(mainCss.contains(".dashboard-sidebar .gape-sidebar-long-link")
                        && mainCss.contains(".dashbord .dashboard-sidebar .gape-sidebar-long-link")
                        && mainCss.contains(".gape-sidebar-link-label")
                        && mainCss.contains("width: 248px;")
                        && mainCss.contains("overflow-wrap: anywhere;")
                        && mainCss.contains("white-space: normal !important;"),
                "Long combined sidebar labels must wrap instead of being clipped");

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
        String lessonSidebarContext = Files.readString(FRAGMENTS_DIR.resolve("dashboard-learning-lesson-context.jspf"));
        String assessmentSidebarContext = Files.readString(FRAGMENTS_DIR.resolve("dashboard-learning-assessment-context.jspf"));
        String roomSidebarContext = Files.readString(FRAGMENTS_DIR.resolve("dashboard-learning-room-context.jspf"));
        String dashboardTopbar = Files.readString(FRAGMENTS_DIR.resolve("dashboard-topbar.jspf"));
        String mainCss = Files.readString(ASSETS_DIR.resolve("css/main.css"));
        String lessonList = Files.readString(WEBAPP_DIR.resolve("WEB-INF/views/learning/lesson-list.jsp"));
        String lessonModalScript = Files.readString(ASSETS_DIR.resolve("js/gape-lesson-modal.js"));
        String assessmentModalScript = Files.readString(ASSETS_DIR.resolve("js/gape-assessment-modal.js"));
        String lessonForm = Files.readString(WEBAPP_DIR.resolve("WEB-INF/views/learning/lesson-form.jsp"));
        String assessmentForm = Files.readString(WEBAPP_DIR.resolve("WEB-INF/views/learning/assessment-form.jsp"));
        String studentCalendar = Files.readString(WEBAPP_DIR.resolve("student/student/calendar/student-calendar.jsp"));
        String learningAttendance = Files.readString(WEBAPP_DIR.resolve("WEB-INF/views/learning/attendance.jsp"));
        String learningAttendanceContent = Files.readString(FRAGMENTS_DIR.resolve("learning-attendance-content.jspf"));
        String learningGrades = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/learning-grades-certificates-content.jspf"));
        String learningCertificates = Files.readString(WEBAPP_DIR.resolve("WEB-INF/fragments/learning-certificates-content.jspf"));
        String studentAttendance = Files.readString(WEBAPP_DIR.resolve("student/student/attendance/student-attendance.jsp"));
        String lessonServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/LessonManagementServlet.java"));
        String assessmentServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/AssessmentManagementServlet.java"));
        String studentLessonServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/StudentLessonServlet.java"));
        String attendanceServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/AttendanceManagementServlet.java"));
        String gradeServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/GradeCertificateServlet.java"));
        String gradeSheetService = Files.readString(JAVA_DIR.resolve("pt/isel/gape/learning/service/GradeSheetService.java"));
        String attendanceService = Files.readString(JAVA_DIR.resolve("pt/isel/gape/learning/service/AttendanceRecordService.java"));
        String attachmentStorage = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/media/JustificationAttachmentStorage.java"));
        String csrfFilter = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/filter/CsrfFilter.java"));

        assertTrue(sidebar.contains("/learning/attendance")
                        && sidebar.contains("/student/attendance")
                        && sidebar.contains("Attendance")
                        && studentSidebar.contains("data-menu-key=\"attendance\"")
                        && studentSidebar.contains("/student/attendance"),
                "Attendance routes must be exposed in dashboard and student menus");
        assertTrue(dashboardTopbar.contains("gape-dashboard-mobile-menu-slot")
                        && dashboardTopbar.contains("gape-dashboard-mobile-menu-toggle")
                        && dashboardTopbar.contains("toggle-dashbord-button")
                        && dashboardTopbar.contains("${not empty pageTitle}")
                        && dashboardTopbar.contains("<c:out value=\"${pageTitle}\"/>")
                        && !dashboardTopbar.contains("gape-dashboard-topbar")
                        && !dashboardTopbar.contains("gape-dashboard-page-title")
                        && !dashboardTopbar.contains("gape-dashboard-actions")
                        && mainCss.contains(".gape-dashboard-mobile-menu-toggle")
                        && mainCss.contains(".gape-dashboard-page-heading")
                        && mainCss.contains("background: transparent;"),
                "Non-student dashboard topbars must be removed while keeping mobile sidebar access and a plain page title");
        assertTrue(lessonList.contains("learningNotifications")
                        && lessonList.contains("/learning/events")
                        && lessonList.contains("GAPE - ${calendarMode ? 'Events' : 'Lessons & Assessments'}")
                        && lessonList.contains("Operational events generated from existing records")
                        && lessonList.contains("Only selected project elements are treated as events")
                        && lessonList.contains("Show more")
                        && lessonList.contains("Event rules")
                        && lessonList.contains("A new event is created whenever one of the selected elements changes state")
                        && lessonList.contains("Draft records are never shown")
                        && lessonList.contains("Events filter")
                        && lessonList.contains("Events class group filter")
                        && lessonServlet.contains("ApplicationReadService.LearningEvents")
                        && lessonServlet.contains("readService.learningEvents()")
                        && !lessonServlet.contains("new LearningEventDAO(")
                        && lessonServlet.contains("persistedEvents")
                        && lessonServlet.contains("countPersistedEvents")
                        && lessonServlet.contains("learningNotifications")
                        && !lessonServlet.contains("listVisibleEvents")
                        && !lessonServlet.contains("createScheduleEvent")
                        && lessonServlet.contains("AssessmentService")
                        && lessonServlet.contains("managedAssessments")
                        && lessonServlet.contains("Lessons & Assessments")
                        && assessmentServlet.contains("redirect(request, response, \"/learning/lessons#assessments\")")
                        && lessonList.contains("Lessons & Assessments")
                        && lessonList.contains("data-la-tab=\"lessons\"")
                        && lessonList.contains("data-la-tab=\"assessments\"")
                        && lessonList.contains("data-la-tab=\"attendance\"")
                        && lessonList.contains("data-la-panel=\"assessments\"")
                        && lessonList.contains("data-la-panel=\"attendance\"")
                        && lessonList.contains("learning-attendance-content.jspf")
                        && !lessonList.contains("Open attendance")
                        && lessonList.contains("fetch(actionUrl")
                        && lessonList.contains("'X-Requested-With': 'XMLHttpRequest'")
                        && lessonList.contains("modal.hide()")
                        && lessonList.contains("/learning/lessons/new?returnTo=/learning/lessons")
                        && lessonList.contains(">New Lesson")
                        && lessonList.contains("/learning/assessments/new")
                        && lessonList.contains(">New Assessment")
                        && !lessonList.contains("d-flex align-items-center gap-10 flex-wrap mb-0")
                        && !learningAttendanceContent.contains("d-flex align-items-center gap-10 flex-wrap mb-0")
                        && !lessonList.contains(">Progress</th>")
                        && lessonList.contains("/learning/lessons/${lesson.id}/delete")
                        && lessonList.contains("aria-label=\"Delete lesson\"")
                        && !lessonServlet.contains("new SelectOptionView(\"questionnaire\"")
                        && !lessonServlet.contains("new SelectOptionView(\"exam\""),
                "Learning Events page must render persisted real events and explain the event scope");
        assertFalse(lessonList.contains("select id=\"schedule-event-class-groups\"")
                        || lessonList.contains("name=\"classGroupIds\" multiple")
                        || lessonList.contains("scheduleEvents")
                        || lessonList.contains("New event")
                        || lessonList.contains("Scheduled events"),
                "Learning notifications must not expose schedule-event creation or raw schedule-event lists");
        assertFalse(lessonList.contains("Total Lessons")
                        || lessonList.contains("${calendarMode ? 'Events' : 'Scheduled'}")
                        || lessonList.contains("${calendarMode ? 'Lessons' : 'Active'}"),
                "Lessons & Assessments must not render the old summary cards");
        assertTrue(lessonModalScript.contains("window.location.pathname.indexOf('/learning/')")
                        && lessonModalScript.contains("contextPath + '/learning/lessons/'")
                        && !lessonModalScript.contains("new URL('/learning/lessons/'")
                        && lessonModalScript.contains("@keyframes gape-lesson-modal-spinner-rotation")
                        && lessonModalScript.contains("animation:gape-lesson-modal-spinner-rotation .8s linear infinite")
                        && lessonModalScript.contains("gape:lesson:result")
                        && assessmentModalScript.contains("@keyframes gape-assessment-modal-spinner-rotation")
                        && assessmentModalScript.contains("animation:gape-assessment-modal-spinner-rotation .8s linear infinite")
                        && assessmentModalScript.contains("gape:assessment:result"),
                "Learning create modal triggers must preserve context and animate their loading spinners");
        assertTrue(lessonList.contains("data-learning-controls=\"lessons\"")
                        && lessonList.contains("data-learning-controls=\"assessments\"")
                        && learningAttendanceContent.contains("data-learning-controls=\"attendance\"")
                        && lessonList.contains("data-learning-sort-option")
                        && lessonList.contains("data-learning-group-option")
                        && lessonList.contains("data-group-field=\"classGroup\"")
                        && learningAttendanceContent.contains("data-group-field=\"classGroup\"")
                        && lessonList.contains("data-learning-table=\"lessons\"")
                        && lessonList.contains("data-learning-table=\"assessments\"")
                        && learningAttendanceContent.contains("data-learning-table=\"attendance\"")
                        && lessonList.contains("is-learning-grouped-by-class-group"),
                "Lessons, Assessments and Attendance must expose real-time Sort by and Group by controls with Class Groups grouping");
        assertTrue(lessonServlet.contains("LessonView::getStartsAtRaw")
                        && lessonServlet.contains("AssessmentView::getAvailableFromRaw")
                        && lessonServlet.contains("Comparator.nullsLast(Comparator.reverseOrder())")
                        && attendanceServlet.contains("AttendanceStudentGroupView::getSortDateRaw"),
                "Lessons & Assessments must default to newest records first across lessons, assessments and attendance");
        assertFalse(lessonForm.contains("data-lesson-organization")
                        || lessonForm.contains("data-lesson-organic-unit")
                        || lessonForm.contains("data-lesson-context-source")
                        || lessonForm.contains("courseOptionsFor(selectedOrganization, selectedOrganicUnit)")
                        || assessmentForm.contains("data-context-organization-filter")
                        || assessmentForm.contains("data-context-organic-unit-filter")
                        || assessmentForm.contains("Repository Assessments"),
                "Lesson and Assessment create forms must start context selection at Course and must not expose repository-copy UI");
        assertTrue(lessonForm.contains("data-lesson-course")
                        && lessonForm.contains("${creating ? 'Create Lesson' : 'Edit Lesson'}")
                        && lessonForm.contains("data-lesson-subject")
                        && lessonForm.contains("data-course-id=\"${classGroup.course.id}\"")
                        && lessonForm.contains("data-subject-id=\"${classGroup.subject.id}\"")
                        && lessonForm.contains("data-organization-acronym=\"<c:out value='${classGroup.course.organizationAcronym}'/>\"")
                        && lessonForm.contains("data-organic-unit-acronym=\"<c:out value='${classGroup.course.organicUnitAcronym}'/>\"")
                        && lessonForm.contains("data-course-acronym=\"<c:out value='${classGroup.course.acronym}'/>\"")
                        && lessonForm.contains("data-subject-acronym=\"<c:out value='${classGroup.subject.acronym}'/>\"")
                        && lessonForm.contains("data-lesson-subject-context")
                        && lessonForm.contains("data-lesson-class-group-field")
                        && lessonForm.contains("data-lesson-block-context")
                        && lessonForm.contains("col-lg-6 gape-select-field gape-lesson-context-field")
                        && lessonForm.contains("data-room-field")
                        && lessonForm.contains("data-attendance-field")
                        && lessonForm.contains("data-date-context")
                        && lessonForm.contains("data-date-action=\"fill-window\"")
                        && lessonForm.contains("syncDateContext")
                        && lessonForm.contains("endsAt.min = maxDateTime(startsAt.value || lowerBound, lowerBound)")
                        && lessonForm.contains("startsAt.addEventListener('change', validateSchedule)")
                        && lessonForm.contains("endsAt.addEventListener('change', validateSchedule)")
                        && lessonForm.contains("data-create-submit")
                        && lessonForm.contains("gape-lesson-attendance-spacer")
                        && lessonForm.contains(".gape-lesson-attendance-spacer.mb-12")
                        && lessonForm.contains("gape-lesson-attendance-control")
                        && lessonForm.contains("data-organic-unit-id=\"${room.organicUnitId}\"")
                        && lessonForm.contains("refreshSelectUi(select)")
                        && lessonForm.contains("courseDisplayLabel")
                        && lessonForm.contains("subjectDisplayLabel")
                        && lessonForm.contains("lessonCourseOptionTemplate")
                        && lessonForm.contains("lessonSubjectOptionTemplate")
                        && lessonForm.contains("lessonClassGroupOptionTemplate")
                        && lessonForm.contains("lessonBlockOptionTemplate")
                        && lessonForm.contains("lessonRoomOptionTemplate")
                        && lessonForm.contains("gape-lesson-context-option")
                        && lessonForm.contains("gape-lesson-course-option")
                        && lessonForm.contains("gape-lesson-course-option-main")
                        && lessonForm.contains("gape-lesson-course-option-context")
                        && lessonForm.contains(".select2-container--default .select2-selection--single .select2-selection__rendered .gape-lesson-context-option:not(.gape-lesson-course-option)")
                        && lessonForm.contains("flex: 1 1 0")
                        && lessonForm.contains("grid-template-columns: minmax(0, 1fr) auto")
                        && lessonForm.contains("text-overflow: ellipsis")
                        && lessonForm.contains(".gape-eduall-select-dropdown .gape-lesson-course-option")
                        && lessonForm.contains("justify-content: space-between")
                        && lessonForm.contains("function splitCourseLabel(label)")
                        && lessonForm.contains("context.textContent = label.context")
                        && lessonForm.contains("gape-lesson-context-option-row-muted")
                        && lessonForm.contains("gape-lesson-context-option-muted")
                        && lessonForm.contains("font-size: 14px;")
                        && lessonForm.contains("observeMutedCourseOptionRows")
                        && lessonForm.contains("MutationObserver")
                        && lessonForm.contains("select2-results__option.select2-results__option--selectable:hover .gape-lesson-context-option")
                        && lessonForm.contains("select2-results__option--selected:hover .gape-lesson-context-option")
                        && lessonForm.contains("select2-results__option--selected.select2-results__option--selectable:hover .gape-lesson-course-option")
                        && lessonForm.contains(".gape-eduall-select-dropdown .select2-results__option--disabled")
                        && lessonForm.contains("function currentOpenSelect2Element()")
                        && lessonForm.contains("function findCurrentContextOption(data, row)")
                        && lessonForm.contains("var selects = [course, subject, classGroup, block, room]")
                        && lessonForm.contains("function select2ResultOptionElement(row)")
                        && lessonForm.contains("utils.GetData(row, 'data')")
                        && lessonForm.contains("sourceOption.dataset.contextUnavailable === 'true'")
                        && lessonForm.contains("document.querySelectorAll('.gape-eduall-select-dropdown .gape-lesson-context-option')")
                        && lessonForm.contains("initializeContextSelectUi")
                        && lessonForm.contains("function contextSelectMatcher(params, data)")
                        && lessonForm.contains("data.element.hidden || data.element.disabled")
                        && lessonForm.contains("matcher: contextSelectMatcher")
                        && lessonForm.contains("var element = findCurrentContextOption(data, null) || data.element")
                        && lessonForm.contains("function clearSelect2OptionData(option)")
                        && lessonForm.contains("window.jQuery.fn.select2.amd.require('select2/utils')")
                        && lessonForm.contains("utils.RemoveData(option)")
                        && lessonForm.contains("clearSelect2OptionData(option)")
                        && lessonForm.contains("bindContextSelect(course, 'gapeLessonContext', syncContextFields)")
                        && lessonForm.contains("classGroupHasPedagogicalBlock")
                        && lessonForm.contains("courseHasCreationContext")
                        && lessonForm.contains("selectedCourseIsUnavailable")
                        && lessonForm.contains("selectedSubjectIsUnavailable")
                        && lessonForm.contains("selectedClassGroupIsUnavailable")
                        && lessonForm.contains("selectedBlockIsUnavailable")
                        && lessonForm.contains("var selectedCourseReady")
                        && lessonForm.contains("var selectedSubjectReady")
                        && lessonForm.contains("var selectedClassGroupReady")
                        && lessonForm.contains("var selectedBlockReady")
                        && lessonForm.contains("blockHasPhysicalRoomContext")
                        && lessonForm.contains("roomMatchesClassGroupContext")
                        && lessonForm.contains("syncContextWrapper(subjectWrapper, selectedCourseReady)")
                        && lessonForm.contains("syncContextWrapper(classGroupWrapper, selectedSubjectReady)")
                        && lessonForm.contains("syncContextWrapper(blockWrapper, selectedClassGroupReady)")
                        && lessonForm.contains("syncContextWrapper(wrapper, !online && !!blockReady && roomCount > 0)")
                        && lessonForm.contains("disabled: false")
                        && lessonForm.contains("Selected course does not have a complete lesson context.")
                        && lessonForm.contains("Selected subject does not have a complete lesson context.")
                        && lessonForm.contains("Selected class group does not have a complete lesson context.")
                        && lessonForm.contains("Selected pedagogical block does not have a complete lesson context.")
                        && lessonForm.contains("contextUnavailable: courseAvailable ? '' : 'true'")
                        && lessonForm.contains("classGroupOptionsFor")
                        && lessonForm.contains("blockOptionsFor")
                        && lessonForm.contains("roomOptionsFor")
                        && lessonForm.contains("syncContextWrapper")
                        && lessonForm.contains("selectInitialClassGroupContext")
                        && lessonForm.contains("selectInitialClassGroupContext();")
                        && lessonForm.contains("courseOptions()")
                        && assessmentForm.contains("data-context-course-filter")
                        && assessmentForm.contains("${creating ? 'Create Assessment' : 'Edit Assessment'}")
                        && assessmentForm.contains("data-assessment-course")
                        && assessmentForm.contains("col-lg-6 gape-select-field gape-assessment-context-field")
                        && assessmentForm.contains("data-assessment-subject")
                        && assessmentForm.contains("data-assessment-class-group")
                        && assessmentForm.contains("data-assessment-block")
                        && assessmentForm.contains("data-assessment-room")
                        && assessmentForm.contains(">Class Group</label>")
                        && assessmentForm.contains("id=\"classGroupIds\" name=\"classGroupIds\" required")
                        && assessmentForm.contains("gape-assessment-context-dropdown")
                        && assessmentForm.contains(".gape-eduall-select-dropdown .select2-results__option--disabled")
                        && assessmentForm.contains("assessmentCourseOptionTemplate")
                        && assessmentForm.contains("assessmentSubjectOptionTemplate")
                        && assessmentForm.contains("assessmentClassGroupOptionTemplate")
                        && assessmentForm.contains("assessmentBlockOptionTemplate")
                        && assessmentForm.contains("assessmentRoomOptionTemplate")
                        && assessmentForm.contains("normalizeContextOptionLabels")
                        && assessmentForm.contains("parts[0] + ' - ' + parts[1]")
                        && assessmentForm.contains("parts.slice(2).join(' | ')")
                        && assessmentForm.contains("function splitCourseLabel(label)")
                        && assessmentForm.contains("gape-assessment-course-option-main")
                        && assessmentForm.contains("gape-assessment-course-option-context")
                        && assessmentForm.contains(".select2-container--default .select2-selection--single .select2-selection__rendered .gape-assessment-context-option:not(.gape-assessment-course-option)")
                        && assessmentForm.contains("flex: 1 1 0")
                        && assessmentForm.contains("grid-template-columns: minmax(0, 1fr) auto")
                        && assessmentForm.contains("text-overflow: ellipsis")
                        && assessmentForm.contains(".gape-eduall-select-dropdown .gape-assessment-course-option")
                        && assessmentForm.contains("justify-content: space-between")
                        && assessmentForm.contains("select2-results__option--selected.select2-results__option--selectable:hover .gape-assessment-course-option")
                        && assessmentForm.contains("select2-results__option--selected.select2-results__option--selectable:hover .gape-assessment-context-option")
                        && assessmentForm.contains("context.textContent = label.context")
                        && assessmentForm.contains("rewriteOptionLabels(course, normalizeCourseLabel)")
                        && assessmentForm.contains("rewriteOptionLabels(classGroups, leadingContextLabel)")
                        && assessmentForm.contains("rewriteOptionLabels(block, trailingContextLabel)")
                        && assessmentForm.contains("gape-assessment-course-option")
                        && assessmentForm.contains("gape-assessment-context-option-muted")
                        && assessmentForm.contains("function currentOpenSelect2Element()")
                        && assessmentForm.contains("container.previousElementSibling")
                        && assessmentForm.contains("function findCurrentContextOption(data, row)")
                        && assessmentForm.contains("var selects = [course, subject, classGroups, block, physicalRoom]")
                        && assessmentForm.contains("option.textContent.trim() === rowText")
                        && assessmentForm.contains("function select2ResultOptionElement(row)")
                        && assessmentForm.contains("utils.GetData(row, 'data')")
                        && assessmentForm.contains("return option.value === String(data.id)")
                        && assessmentForm.contains("sourceOption.dataset.contextUnavailable === 'true'")
                        && assessmentForm.contains("document.querySelectorAll('.gape-eduall-select-dropdown .gape-assessment-context-option')")
                        && assessmentForm.contains("document.querySelector('.gape-eduall-select-dropdown .select2-results__options')")
                        && assessmentForm.contains("observeMutedCourseOptionRows")
                        && assessmentForm.contains("initializeContextSelectUi")
                        && assessmentForm.contains("function contextSelectMatcher(params, data)")
                        && assessmentForm.contains("data.element.hidden || data.element.disabled")
                        && assessmentForm.contains("matcher: contextSelectMatcher")
                        && assessmentForm.contains("if (element && element.value && (element.hidden || element.disabled))")
                        && assessmentForm.contains("var element = findCurrentContextOption(data, null) || data.element")
                        && assessmentForm.contains("function clearSelect2OptionData(option)")
                        && assessmentForm.contains("window.jQuery.fn.select2.amd.require('select2/utils')")
                        && assessmentForm.contains("utils.RemoveData(option)")
                        && assessmentForm.contains("clearSelect2OptionData(option)")
                        && assessmentForm.contains("change.gapeAssessmentContext")
                        && assessmentForm.contains("courseHasCreationContext")
                        && assessmentForm.contains("classGroupHasPedagogicalBlock")
                        && assessmentForm.contains("selectedCourseIsUnavailable")
                        && assessmentForm.contains("selectedSubjectIsUnavailable")
                        && assessmentForm.contains("selectedClassGroupIsUnavailable")
                        && assessmentForm.contains("selectedBlockIsUnavailable")
                        && assessmentForm.contains("var selectedCourseReady = !!selectedCourseValue() && !selectedCourseIsUnavailable()")
                        && assessmentForm.contains("var selectedSubjectReady = selectedCourseReady && !!selectedSubjectValue() && !selectedSubjectIsUnavailable()")
                        && assessmentForm.contains("var selectedClassGroupReady = selectedSubjectReady && !!selectedClassGroupValue() && !selectedClassGroupIsUnavailable()")
                        && assessmentForm.contains("var selectedBlockReady = selectedClassGroupReady && !!(block && block.value) && !selectedBlockIsUnavailable()")
                        && assessmentForm.contains("var courseReady = !!selectedCourseValue() && !selectedCourseIsUnavailable()")
                        && assessmentForm.contains("var subjectReady = courseReady && !!selectedSubjectValue() && !selectedSubjectIsUnavailable()")
                        && assessmentForm.contains("var classGroupReady = subjectReady && !!selectedClassGroupValue() && !selectedClassGroupIsUnavailable()")
                        && assessmentForm.contains("syncContextWrapper(subjectWrapper, courseReady)")
                        && assessmentForm.contains("syncContextWrapper(classGroupWrapper, subjectReady)")
                        && assessmentForm.contains("syncContextWrapper(blockWrapper, classGroupReady)")
                        && assessmentForm.contains("blockHasPhysicalRoomContext")
                        && assessmentForm.contains("syncPhysicalRoomContext")
                        && assessmentForm.contains("physicalRoomWrapper.classList.toggle('d-none', !isInPerson)")
                        && assessmentForm.contains("syncContextWrapper(physicalRoomWrapper, isInPerson && !!selectedBlock && roomCount > 0)")
                        && assessmentForm.contains("optionMatchesSelectedBlockContext")
                        && assessmentForm.contains("Selected course does not have a complete assessment context.")
                        && assessmentForm.contains("Selected subject does not have a complete assessment context.")
                        && assessmentForm.contains("Selected class group does not have a complete assessment context.")
                        && assessmentForm.contains("Selected pedagogical block does not have a complete assessment context.")
                        && assessmentForm.contains("Select a class group before selecting a pedagogical block.")
                        && assessmentForm.contains("option.dataset.contextUnavailable = available ? '' : 'true'")
                        && assessmentForm.contains("option.disabled = false")
                        && assessmentForm.contains("syncContextWrapper")
                        && assessmentForm.contains("data-course-ids=\"${subject.courseIds}\"")
                        && assessmentForm.contains("data-class-group-id=\"${block.classGroupId}\"")
                        && assessmentForm.contains("optionMatchesClassGroup")
                        && assessmentForm.contains("optionMatchesSelectedCourseContext")
                        && assessmentForm.contains("hydrateCourseSelection();")
                        && assessmentForm.contains("data-create-submit")
                        && assessmentForm.contains("availableUntil.min = maxDateTime(availableFrom.value || lowerBound, lowerBound)")
                        && assessmentForm.contains("availableFrom.addEventListener('change', syncAvailability)")
                        && assessmentForm.contains("availableUntil.addEventListener('change', syncAvailability)")
                        && assessmentForm.contains("form.addEventListener('submit'")
                        && lessonServlet.contains("gape:lesson:result")
                        && lessonServlet.contains("writeLessonModalResult(response, false, message)")
                        && assessmentServlet.contains("gape:assessment:result")
                        && assessmentServlet.contains("writeAssessmentModalResult(response, false, message)")
                        && assessmentServlet.contains("request.setAttribute(\"courseOptions\", courseOptions)")
                        && assessmentServlet.contains("courseOptionLabel(course)")
                        && assessmentServlet.contains("courseContextLabel(Course course)")
                        && assessmentServlet.contains("return context.isBlank() ? courseLabel(course) : courseLabel(course) + \" | \" + context")
                        && assessmentServlet.contains("subjectCourseIds(subject.id())")
                        && assessmentServlet.contains("getCourseIds()")
                        && assessmentServlet.contains("getClassGroupId()"),
                "Lesson and Assessment create forms must enforce parent-first context selection down to class group and pedagogical block");
        assertFalse(assessmentForm.contains("Applicable Class Groups")
                        || assessmentForm.contains("name=\"classGroupIds\" multiple")
                        || assessmentForm.contains("size=\"4\"")
                        || assessmentForm.contains("js-example-basic-single gape-eduall-select\" data-assessment-subject")
                        || assessmentForm.contains("js-example-basic-single gape-eduall-select\" data-assessment-class-group")
                        || assessmentForm.contains("js-example-basic-single gape-eduall-select\" data-assessment-block")
                        || assessmentForm.contains(">Builder</label>")
                        || assessmentForm.contains("Questions after save")
                        || assessmentForm.contains("var selectedBlockOption =")
                        || assessmentForm.contains("return (label || '').replace(/\\s+\\|\\s+/, ' - ').trim();")
                        || assessmentForm.contains("Select a course context before selecting a physical room."),
                "Create Assessment context must use the same singular Class Group input pattern as Create Lesson and avoid redundant builder hints");
        assertFalse(lessonForm.contains("data-lesson-class-group-context")
                        || lessonForm.contains("syncClassGroupContext")
                        || lessonForm.contains("js-example-basic-single gape-eduall-select\" data-lesson-subject")
                        || lessonForm.contains("js-example-basic-single gape-eduall-select\" data-lesson-class-group")
                        || lessonForm.contains("js-example-basic-single gape-eduall-select\" data-lesson-block")
                        || lessonForm.contains("data-class-group-context-label")
                        || lessonForm.contains("data-class-group-group")
                        || lessonForm.contains("stopSymbol")
                        || lessonForm.contains("blockedContextText")
                        || lessonForm.contains("pushContextOption(options, organization")
                        || lessonForm.contains("pushContextOption")
                        || lessonForm.contains("organicUnitContexts")
                        || lessonForm.contains("select2-results__option--highlighted:has(.gape-lesson-context-option-muted)")
                        || lessonForm.contains(String.valueOf((char) 0x26D4)),
                "Create Lesson must avoid duplicate class group context display and stop-symbol context labels");
        assertTrue(sidebar.contains("adminUserActiveChild == 'new'")
                        && sidebar.contains("/admin/users/new")
                        && sidebar.contains("adminOrganizationActiveChild == 'new'")
                        && sidebar.contains("/admin/organizations/new")
                        && sidebar.contains("Create Organization")
                        && sidebar.contains("adminCourseActiveChild == 'new'")
                        && sidebar.contains("/admin/courses/new")
                        && sidebar.contains("/coordinator/courses/new")
                        && sidebar.contains("adminSubjectActiveChild == 'new'")
                        && sidebar.contains("/admin/subjects/new")
                        && sidebar.contains("/coordinator/subjects/new")
                        && sidebar.contains("learningClassGroupActiveChild == 'new'")
                        && sidebar.contains("/learning/class-groups/new")
                        && sidebar.contains("dashboard-learning-lesson-context.jspf")
                        && sidebar.contains("activeMenu == 'assessments'")
                        && sidebar.contains("dashboard-learning-room-context.jspf")
                        && lessonSidebarContext.contains("learningLessonActiveChild == 'new'")
                        && lessonSidebarContext.contains("/learning/lessons/new")
                        && lessonSidebarContext.contains("Create Lesson")
                        && assessmentSidebarContext.contains("learningAssessmentActiveChild == 'new'")
                        && assessmentSidebarContext.contains("/learning/assessments/new")
                        && assessmentSidebarContext.contains("Create Assessment")
                        && roomSidebarContext.contains("learningRoomActiveChild == 'new'")
                        && roomSidebarContext.contains("/learning/rooms/new")
                        && lessonServlet.contains("request.setAttribute(\"learningLessonActiveChild\", \"new\")")
                        && assessmentServlet.contains("request.setAttribute(\"learningAssessmentActiveChild\", creating ? \"new\" : \"edit\")"),
                "Create pages must surface as active sidebar children below their parent menu items");
        assertTrue(studentCalendar.contains("gape-calendar-grid")
                        && studentCalendar.contains("data-event-calendar-source-item")
                        && studentCalendar.contains("eventCalendarNotifications")
                        && studentLessonServlet.contains("visibleStudentEvents")
                        && studentLessonServlet.contains("findVisible")
                        && studentLessonServlet.contains("eventCalendar(request, eventCalendarNotifications)"),
                "Student calendar must show the complete read/unread learning-event projection with the shared calendar design");
        assertTrue(attendanceServlet.contains("AttendanceRecordService")
                        && attendanceServlet.contains("AbsenceJustificationService")
                        && attendanceServlet.contains("GradeCertificateServlet")
                        && attendanceServlet.contains("@MultipartConfig")
                        && attendanceServlet.contains("JustificationAttachmentStorage")
                        && attendanceServlet.contains("attendanceCreationStateOptions")
                        && attendanceServlet.contains("attendanceStateOptions")
                        && attendanceServlet.contains("attendanceStudentGroups")
                        && attendanceServlet.contains("assessmentActivities")
                        && attendanceServlet.contains("AssessmentAttendanceSource")
                        && attendanceServlet.contains("ApplicationReadService.Attempts")
                        && attendanceServlet.contains("readService.attempts()")
                        && attendanceServlet.contains("AttendanceActivityView")
                        && attendanceServlet.contains("AttendanceStudentGroupView")
                        && attendanceServlet.contains("attendanceCreationSourceOptions")
                        && attendanceServlet.contains("recordAttendance")
                        && attendanceServlet.contains("submitJustification")
                        && attendanceServlet.contains("processJustification")
                        && attendanceServlet.contains("populateLearningAttendanceAttributes")
                        && attendanceServlet.contains("writeJustificationProcessResponse")
                        && attendanceServlet.contains("isAjaxRequest")
                        && attendanceServlet.contains("\"/learning/lessons/attendance\"")
                        && lessonServlet.contains("populateLearningAttendanceAttributes")
                        && lessonServlet.contains("\"/learning/lessons#attendance\"")
                        && lessonServlet.contains("initialLessonAssessmentPanel")
                        && attendanceService.contains("AttendanceStatus.JUSTIFIED")
                        && attendanceService.contains("AttendanceSource.AUTOMATIC")
                        && learningAttendance.contains("data-default-value")
                        && learningAttendance.contains("attendanceStateOptions")
                        && learningAttendance.contains("attendanceCreateStateOptions")
                        && learningAttendance.contains("learning-grades-certificates-content.jspf")
                        && learningAttendance.contains("learning-certificates-content.jspf")
                        && learningAttendance.contains("data-aac-tab=\"enrollments\"")
                        && learningAttendance.contains("data-aac-tab=\"grades\"")
                        && learningAttendance.contains("data-aac-tab=\"certificates\"")
                        && !learningAttendance.contains("data-aac-tab=\"attendance\"")
                        && !lessonList.contains("/learning/attendance#attendance")
                        && lessonList.contains(">Attendance</span>")
                        && learningAttendanceContent.contains("aac-attendance-table")
                        && learningAttendanceContent.contains("id=\"registerAttendanceModal\"")
                        && learningAttendanceContent.contains("${attendanceActionPath}")
                        && learningAttendanceContent.contains("data-attendance-settings-form")
                        && learningAttendanceContent.contains("data-attendance-decision=\"approve\"")
                        && learningAttendanceContent.contains("data-attendance-decision=\"reject\"")
                        && learningAttendanceContent.contains("${activity.canApprove ? '' : 'hidden'}")
                        && learningAttendanceContent.contains("${activity.canReject ? '' : 'hidden'}")
                        && learningAttendanceContent.indexOf("name=\"decisionNotes\"")
                                == learningAttendanceContent.lastIndexOf("name=\"decisionNotes\"")
                        && learningAttendanceContent.contains("${attendanceActionPath}/justifications/${activity.justification.id}/approve")
                        && learningAttendanceContent.contains("${attendanceActionPath}/justifications/${activity.justification.id}/reject")
                        && learningAttendance.contains("data-aac-default-panel=\"${attendanceOnly ? 'attendance' : 'enrollments'}\"")
                        && learningAttendance.contains("var defaultPanel = root.getAttribute('data-aac-default-panel')")
                        && learningAttendance.contains("data-aac-panel=\"enrollments\"")
                        && learningAttendance.contains("data-aac-panel=\"grades\"")
                        && learningAttendance.contains("data-aac-panel=\"certificates\"")
                        && learningAttendance.contains("data-aac-panel=\"attendance\"")
                        && learningAttendance.contains("'grade-sheets': 'grades'")
                        && learningAttendance.contains("'certificates-list': 'certificates'")
                        && learningAttendance.contains("gap: 16px;")
                        && learningAttendance.contains("min-height: 118px;")
                        && learningAttendance.contains("flex: 0 0 48px;")
                        && learningAttendance.contains("window.location.pathname + window.location.search + '#' + panelName")
                        && learningAttendance.contains("la-mode-card is-active")
                        && learningAttendance.contains("Enrollments &amp; Certificates")
                        && attendanceServlet.contains("populateEnrollmentOverviewAttributes")
                        && attendanceServlet.contains("enrollmentRows")
                        && learningAttendance.contains("aria-label=\"Show all enrollments\"")
                        && learningAttendance.contains("ph ph-caret-down")
                        && learningAttendance.contains("aac-expanded-panel")
                        && learningAttendance.contains("id=\"registerAttendanceModal\"")
                        && learningAttendance.contains("data-bs-target=\"#registerAttendanceModal\"")
                        && learningAttendance.contains("items=\"${attendanceStudentGroups}\"")
                        && learningAttendance.contains("items=\"${attendanceActivities}\"")
                        && learningAttendance.contains("aria-label=\"Show all attendance activities\"")
                        && learningAttendance.contains("aac-attendance-table")
                        && learningAttendance.contains("aac-attendance-count")
                        && learningAttendance.contains("data-bs-target=\"#attendanceGroup${group.studentUserId}\"")
                        && learningAttendance.contains("data-bs-target=\"#attendanceMobileGroup${group.studentUserId}\"")
                        && learningAttendance.contains("Attendance detail")
                        && learningAttendance.contains("Attendance settings")
                        && learningAttendance.contains("${attendanceBasePath}/justifications/${activity.justification.id}/approve")
                        && learningAttendance.contains("${attendanceBasePath}/justifications/${activity.justification.id}/reject")
                        && !learningAttendance.contains(">Status</")
                        && !learningAttendance.contains("attendanceCreateStatusOptions")
                        && !attendanceServlet.contains("attendanceCreationStatusOptions")
                        && learningGrades.contains("gradeSheetGroups")
                        && learningGrades.contains("${sheet.contextHtml}")
                        && learningGrades.contains("aac-grade-doc")
                        && learningGrades.contains("aac-grade-doc__table-wrap\"")
                        && learningGrades.contains("--aac-grade-doc-columns: ${doc.variableColumnCount};")
                        && learningGrades.contains("--aac-grade-doc-min-width: ${doc.tableMinWidthPx}px;")
                        && learningAttendance.contains("min-width: 0;")
                        && learningAttendance.contains("scrollbar-width: none;")
                        && learningAttendance.contains("overflow: visible;")
                        && learningAttendance.contains("z-index: 1080;")
                        && learningAttendance.contains(".aac-certificate-summary-row")
                        && gradeServlet.contains("float availableWidth = pageWidth - left - rightMargin;")
                        && gradeServlet.contains("finalWidth += availableWidth - tableWidth;")
                        && gradeServlet.contains("List<Integer> sharedColumnWidths = gradeDocumentExcelColumnWidths(view);")
                        && gradeServlet.contains("int minimumDocumentWidth = 760;")
                        && gradeServlet.contains(".append(sharedColGroup)")
                        && learningAttendance.contains("@media (max-width: 1199.98px)")
                        && learningGrades.contains("Automatic average")
                        && learningGrades.contains("/learning/grades/sheets/${sheet.id}/download")
                        && learningGrades.contains("/learning/grades/sheets/${sheet.id}/configure")
                        && learningGrades.contains("id=\"gradeSheetSetup${gradeManagementIdPrefix}${sheet.id}\"")
                        && learningGrades.contains("aria-label=\"Edit grade sheet weights\"")
                        && learningCertificates.contains("id=\"certificates-list-${certificateManagementScope eq 'published' ? 'published_' : 'active_'}\"")
                        && learningCertificates.contains("aac-certificate-doc")
                        && learningCertificates.contains("Subjects")
                        && learningCertificates.contains("courseDurationLabel")
                        && learningCertificates.contains("subjectCountLabel")
                        && !learningCertificates.contains("Total ECTS")
                        && !learningCertificates.contains("nascido em")
                        && !learningCertificates.contains("natural de")
                        && !learningCertificates.contains("DD-MM-AAAA")
                        && !learningGrades.contains("/learning/grades/records")
                        && !learningGrades.contains("Record grade")
                        && !learningGrades.contains("grade-record-value")
                        && !gradeServlet.contains("recordGrade(")
                        && learningGrades.contains("<c:if test=\"${not empty sheet.classGroupIds}\">")
                        && learningGrades.contains("<c:if test=\"${empty sheet.assessmentColumns}\">disabled</c:if>")
                        && !learningGrades.contains("not sheet.draft")
                        && learningGrades.contains("name=\"csrfToken\" value=\"${sessionScope['gape.auth.csrfToken']}\"")
                        && learningCertificates.contains("Email")
                        && learningCertificates.contains("&#8470; Certificates")
                        && learningCertificates.contains("Actions")
                        && learningCertificates.contains("aac-icon-button bg-main-50 text-main-600 collapsed")
                        && learningCertificates.contains("aria-label=\"Show all certificates\"")
                        && learningCertificates.contains("${group.studentLabel}")
                        && learningCertificates.contains("${group.stateSummaries}")
                        && learningCertificates.contains("data-bs-target=\"#certificateGroup${certificateManagementIdPrefix}${group.studentUserId}\"")
                        && learningCertificates.contains("data-bs-target=\"#certificateMobileGroup${certificateManagementIdPrefix}${group.studentUserId}\"")
                        && learningCertificates.contains("gape-mobile-list")
                        && learningCertificates.contains("/learning/grades/certificates/${item.id}/download")
                        && learningCertificates.contains("certificateDraftDownloadsAllowed and item.stateValue eq 'draft'")
                        && gradeServlet.contains("request.setAttribute(\"certificateDraftDownloadsAllowed\", profile != AccessProfileType.STUDENT);")
                        && gradeServlet.contains("certificate.state() != CertificateState.DRAFT")
                        && gradeServlet.contains("CertificateStateSummaryView")
                        && gradeServlet.contains("return getStudentUserId() + \" - \" + getStudentName();")
                        && !learningGrades.contains("Create grade sheet")
                        && !learningGrades.contains("/learning/grades/sheets/${sheet.id}/publish")
                        && !learningCertificates.contains("Latest certificate")
                        && !learningCertificates.contains(">Course</th>")
                        && !learningCertificates.contains("/learning/grades/certificates/${certificate.id}/download")
                        && !learningCertificates.contains("/learning/grades/certificates\" method=\"post\"")
                        && !gradeServlet.contains("createGradeSheet(")
                        && !gradeServlet.contains("issueCertificate(")
                        && gradeServlet.contains("downloadGradeSheet(")
                        && gradeServlet.contains("configureGradeSheet(")
                        && gradeSheetService.contains("return;")
                        && studentAttendance.contains("data-student-tab=\"enrollments\"")
                        && studentAttendance.contains("data-student-tab=\"grades\"")
                        && studentAttendance.contains("data-student-tab=\"certificates\"")
                        && studentAttendance.contains("data-student-tab-panel=\"enrollments\"")
                        && studentAttendance.contains("data-student-tab-panel=\"grades\"")
                        && studentAttendance.contains("data-student-tab-panel=\"certificates\"")
                        && studentAttendance.contains("studentCourseEnrollments")
                        && studentAttendance.contains("gape-student-class-group-card")
                        && attachmentStorage.contains("justifications/")
                        && attachmentStorage.contains("ALLOWED_EXTENSIONS")
                        && csrfFilter.contains("sessionManager.isValidCsrfToken")
                        && csrfFilter.contains("request.getPart(\"csrfToken\")"),
                "Enrollments, attendance, justification submission and processing must be wired to services and CSRF-protected routes");
        assertFalse(learningAttendance.contains("id=\"grades\" class=\"gape-management-card")
                        || learningAttendance.contains("id=\"attendance\" class=\"gape-management-card")
                        || learningAttendance.contains("gape-section-scroll")
                        || learningAttendance.contains("overflow-x-auto")
                        || learningGrades.contains("Automatically generated by class group assessment results")
                        || learningGrades.contains(">Configuration<")
                        || learningGrades.contains(">Assessment grades<")
                        || learningCertificates.contains("Automatically created by course enrollment")
                        || learningAttendance.contains("downloadHref")
                        || learningGrades.contains("Save Grade")
                        || sidebar.contains("activeMenu == 'reviews'")
                        || studentSidebar.contains("activeMenu == 'reviews'"),
                "Enrollments & Certificates must use four primary cards without tab scroll wrappers and assessments must not keep Reviews UI hooks");
    }

    @Test
    void instructorSharedPagesReuseAdministratorTemplates() throws IOException {
        String sidebar = Files.readString(FRAGMENTS_DIR.resolve("dashboard-sidebar.jspf"));
        String instructorProfile = Files.readString(WEBAPP_DIR.resolve("instructor/instructor-my-profile.jsp"));

        assertTrue(instructorProfile.contains("DashboardMyProfilePageData")
                        && !instructorProfile.contains("/admin/admin-my-profile.jsp"),
                "Instructor profile must be actor-owned and keep the administrator visual contract");
        assertTrue(sidebar.contains("/instructor/subjects")
                        && sidebar.contains("teacherSubjectContextId")
                        && sidebar.contains("/learning/assessments")
                        && sidebar.contains("${messageHref}")
                        && sidebar.contains("${quizAttemptsHref}")
                        && sidebar.contains("Assessments"),
                "Instructor sidebar must expose one combined assessments entry");
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
        String organizationStructureTree = Files.readString(FRAGMENTS_DIR.resolve("organization-structure-tree.jspf"));
        String organizationActivities = Files.readString(FRAGMENTS_DIR.resolve("organization-class-group-activities-panel.jspf"));
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
        assertTrue(organizationList.contains(">Sort by")
                        && organizationList.contains("data-organization-sort-toggle")
                        && organizationList.contains("gape-filter-toggle")
                        && organizationList.contains("data-organization-sort-option")
                        && organizationList.contains("data-sort-field=\"name\"")
                        && organizationList.contains("data-sort-field=\"date\"")
                        && organizationList.contains("data-sort-field=\"status\"")
                        && organizationList.contains("data-sort-normal=\"asc\"")
                        && organizationList.contains("data-sort-normal=\"desc\"")
                        && organizationList.contains("gape-sort-arrow--normal")
                        && organizationList.contains("gape-sort-arrow--reverse")
                        && organizationList.contains("data-sort-state=\"none\"")
                        && organizationList.contains("<span>Name</span>")
                        && organizationList.contains("<span>Date</span>")
                        && organizationList.contains("<span>State</span>"),
                "Organization list must expose client-side Sort by options for name, date and state");
        assertFalse(organizationList.contains("data-sort-field=\"type\"")
                        || organizationList.contains("<span>Type</span>"),
                "Organization Sort by must not expose Type");
        assertTrue(organizationList.contains("data-organization-list")
                        && organizationList.contains("data-organization-card")
                        && organizationList.contains("data-sort-index=\"${organizationLoop.index}\"")
                        && organizationList.contains("data-sort-name=\"${fn:escapeXml(organization.name)}\"")
                        && organizationList.contains("data-sort-date=\"${organization.id}\"")
                        && organizationList.contains("data-sort-status=\"${fn:escapeXml(organization.stateLabel)}\"")
                        && organizationList.contains("data-sort-type=\"${fn:escapeXml(organization.typeLabel)}\"")
                        && organizationList.contains("renderCards()")
                        && organizationList.contains("compareCards(activeField, activeDirection, first, second)")
                        && organizationList.contains("activeField = null;")
                        && organizationList.contains("activeDirection = null;")
                        && organizationList.contains("sortToggle.classList.toggle('is-active', activeField !== null)")
                        && organizationList.contains("closeDropdown(sortToggle)"),
                "Organization list must sort cards in-place and reset to the original order on the third click");
        assertTrue(organizationList.contains("<section class=\"gape-organization-card\""),
                "Organization list must render each organization as a card");
        assertTrue(organizationList.contains("gape-organization-units-panel")
                        && organizationList.contains("gape-hierarchy-node")
                        && organizationList.contains("data-gape-tree-toggle=\"organizationUnits${organization.id}\"")
                        && organizationList.contains("items=\"${organization.organicUnits}\"")
                        && organizationList.contains("/admin/organizations/${organization.id}?unitModal=create")
                        && organizationList.contains("/admin/organizations/${organization.id}?unitModal=create&amp;parentUnitId=${unit.id}")
                        && organizationList.contains("/admin/organizations/${organization.id}?unitDetail=${unit.id}")
                        && organizationList.contains("/admin/organizations/${organization.id}?unitEdit=${unit.id}")
                        && organizationList.contains("/admin/organizations/${organization.id}/units/${unit.id}/delete"),
                "Organization list must expose Show on organizations and route organic unit New, Detail and Edit actions to detail-page modals");
        assertFalse(organizationList.contains("/admin/organizations/${organization.id}/units/${unit.id}/edit"),
                "Organization list must not link organic unit edit actions to the old standalone page");
        assertFalse(organizationList.contains("data-gape-tree-toggle=\"unitCourses${unit.id}\"")
                        || organizationList.contains("items=\"${unit.courses}\"")
                        || organizationList.contains("items=\"${course.subjects}\"")
                        || organizationList.contains("items=\"${subject.classGroups}\""),
                "Organization list must not expose Show or deeper data inside organic units");
        assertTrue(organizationDetail.contains("GAPE - Organization Details")
                        && organizationDetail.contains("data-organization-tab=\"organic-units\"")
                        && organizationDetail.contains("data-organization-tab=\"administrators\"")
                        && !organizationDetail.contains("data-organization-tab=\"critical-actions\"")
                        && organizationDetail.contains("data-organization-panel=\"organic-units\"")
                        && organizationDetail.contains("data-organization-panel=\"administrators\"")
                        && !organizationDetail.contains("data-organization-panel=\"critical-actions\"")
                        && organizationDetail.contains("grid-template-columns: repeat(2, minmax(0, 1fr))")
                        && organizationDetail.contains("Critical Actions")
                        && organizationDetail.contains("og-critical-grid")
                        && organizationDetail.contains("organizationSetupModal")
                        && organizationDetail.contains("organicUnitCreateModal")
                        && organizationDetail.contains("organicUnitDetailModal")
                        && organizationDetail.contains("organicUnitEditModal")
                        && organizationDetail.contains("og-unit-modal")
                        && organizationDetail.contains("modal-dialog-centered modal-dialog-scrollable")
                        && organizationDetail.contains(".gape-organization-units-panel > .px-18.py-18")
                        && organizationDetail.contains(".gape-learning-table-photo")
                        && organizationDetail.contains("openRequestedUnitModal")
                        && organizationDetail.contains("organization-structure-tree.jspf"),
                "Organization detail must use only Organic Units and Administrators cards, with Structure and unit modals inside the detail page");
        assertFalse(organizationDetail.contains("Organic units, courses, subjects, class groups and activities."),
                "Organization detail must not render the redundant Structure summary above the organic unit tree");
        int structureHeaderIndex = organizationStructureTree.indexOf("gape-structure-list-header");
        assertTrue(structureHeaderIndex >= 0
                        && structureHeaderIndex == organizationStructureTree.lastIndexOf("gape-structure-list-header"),
                "Organization detail tree must render the column header only once");
        assertTrue(organizationStructureTree.contains("gape-organization-units-panel")
                        && organizationStructureTree.contains("<h3 class=\"text-18 fw-semibold text-neutral-800 mb-2\">Structure</h3>")
                        && organizationStructureTree.contains("N&ordm; Elements")
                        && organizationStructureTree.contains("organization.hasPhoto")
                        && organizationStructureTree.contains("organizationPhotoUrl")
                        && organizationStructureTree.contains("No organization photo")
                        && organizationStructureTree.contains("data-gape-tree-toggle=\"unitCourses${unit.id}\"")
                        && organizationStructureTree.contains("data-gape-tree-toggle=\"courseSubjects${course.id}\"")
                        && organizationStructureTree.contains("data-gape-tree-toggle=\"subjectClassGroups${course.id}_${subject.subjectId}\"")
                        && organizationStructureTree.contains("og-element-count")
                        && organizationStructureTree.contains("organizationUnitElementCountById[unit.id]")
                        && organizationStructureTree.contains("organizationCourseElementCountById[course.id]")
                        && organizationStructureTree.contains("organizationSubjectElementCountByKey[subject.treeKey]")
                        && organizationStructureTree.contains("organizationClassGroupElementCountById[classGroup.id]")
                        && organizationStructureTree.contains("course.hasPhoto")
                        && organizationStructureTree.contains("coursePhotoUrl")
                        && organizationStructureTree.contains("subject.hasPhoto")
                        && organizationStructureTree.contains("subjectPhotoUrl")
                        && organizationStructureTree.contains("gape-learning-table-photo")
                        && organizationStructureTree.contains("gape-photo-placeholder--table")
                        && organizationStructureTree.contains("data-unit-create-trigger")
                        && organizationStructureTree.contains("data-unit-detail-trigger")
                        && organizationStructureTree.contains("data-unit-edit-trigger")
                        && organizationStructureTree.contains("items=\"${unit.courses}\"")
                        && organizationStructureTree.contains("items=\"${course.subjects}\"")
                        && organizationStructureTree.contains("items=\"${subject.classGroups}\""),
                "Organization detail must expose show-more toggles and column counts for units, courses, subjects and class groups");
        assertFalse(organizationStructureTree.contains("/admin/organizations/${organization.id}/units/${unit.id}/edit"),
                "Organization detail must not link organic unit edit actions to the old standalone page");
        assertFalse(organizationStructureTree.contains("<h3 class=\"text-18 fw-semibold text-neutral-800 mb-4\">Organic Units</h3>"),
                "Organization detail tree must not repeat the Organic Units heading inside the Organic Units card");
        assertTrue(organizationStructureTree.contains("organization-class-group-activities-panel.jspf")
                        && organizationActivities.contains("og-activity-grid")
                        && organizationActivities.contains("classGroupLessonTimelineRankByClassGroup")
                        && organizationActivities.contains("classGroupAssessmentTimelineRankByClassGroup")
                        && !organizationActivities.contains("Physical Rooms")
                        && !organizationActivities.contains("classGroupRooms"),
                "Organization detail activities must use the two-column timeline without rooms");
        assertFalse(organizationList.contains("gape-organization-units-row"),
                "Organic units must not render as a separate table row");
        String organizationServlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/OrganizationManagementServlet.java"));
        String organizationView = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/view/OrganizationView.java"));
        String organizationCourseTreeView = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/view/OrganizationCourseTreeView.java"));
        String organizationSubjectTreeView = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/view/OrganizationSubjectTreeView.java"));
        assertTrue(organizationServlet.contains("courseTreesByOrganicUnit")
                        && organizationServlet.contains("OrganizationCourseTreeView")
                        && organizationServlet.contains("OrganizationSubjectTreeView")
                        && organizationServlet.contains("OrganizationClassGroupTreeView")
                        && organizationServlet.contains("exposeOrganizationElementCounts")
                        && organizationServlet.contains("?unitModal=create")
                        && organizationServlet.contains("?unitDetail=")
                        && organizationServlet.contains("?unitEdit=")
                        && organizationServlet.contains("redirectToReturnPath(request, response, \"/admin/organizations/\" + organizationId)")
                        && organizationServlet.contains("organizationUnitElementCountById")
                        && organizationServlet.contains("organizationCourseElementCountById")
                        && organizationServlet.contains("organizationSubjectElementCountByKey")
                        && organizationServlet.contains("organizationClassGroupElementCountById")
                        && organizationServlet.contains("unitElementCountById.put(unit.getId(), unit.getCourses().size())")
                        && organizationServlet.contains("courseElementCountById.put(course.getId(), course.getSubjects().size())")
                        && organizationServlet.contains("subjectElementCountByKey.put(subject.getTreeKey(), subject.getClassGroups().size())")
                        && organizationServlet.contains("renderOrganizationDetailForDynamicRequest")
                        && organizationServlet.contains("X-Requested-With")
                        && !organizationServlet.contains("courseElements += 1 + subjectElements")
                        && !organizationServlet.contains("subjectElements += 1 + classGroupElements")
                        && !organizationServlet.contains("childUnitIdsByParent"),
                "Organization servlet must build the organization tree and direct visible element counts for each Show panel");
        assertTrue(organizationServlet.contains("views.sort(Comparator.comparingLong(OrganizationView::getId).reversed())"),
                "Organization list must default to newest organizations first");
        assertTrue(organizationView.contains("MediaPathValidator.safeRelativePath(organization.photo())")
                        && organizationCourseTreeView.contains("MediaPathValidator.safeRelativePath(course.photo())")
                        && organizationCourseTreeView.contains("isHasPhoto()")
                        && organizationSubjectTreeView.contains("MediaPathValidator.safeRelativePath(subject.photo())")
                        && organizationSubjectTreeView.contains("isHasPhoto()"),
                "Organization tree organization, course and subject views must expose sanitized photos");
        assertFalse(organizationServlet.contains("sortOrganizations(views")
                        || organizationServlet.contains("\"organizationSortBy\"")
                        || organizationServlet.contains("\"organizationSortDir\""),
                "Organization sorting must stay client-side so the list can update without reloading the page");
        assertTrue(organizationDetail.contains("gape-organization-detail-photo"),
                "Organization detail must render organization photos");
        assertTrue(organizationDetail.contains("Assign Administrator")
                        && organizationDetail.contains("assignedAdministrators")
                        && organizationDetail.contains("og-data-table")
                        && organizationDetail.contains("#<c:out value=\"${administrator.id}\"/>"),
                "Organization detail must show assigned administrators and searchable administrator IDs");
        assertFalse(organizationStructureTree.contains("not unit.inactive")
                        || organizationStructureTree.contains("not course.inactive")
                        || organizationStructureTree.contains("not subject.inactive")
                        || organizationStructureTree.contains("not classGroup.completed"),
                "Organization detail must not hide actions only because an element is inactive or completed");
        assertTrue(organizationList.contains("/admin/organizations/${organization.id}?unitDetail=${unit.id}"),
                "Organization list must open each listed organic unit detail in the Organization Details modal");
        assertTrue(organizationStructureTree.contains("data-bs-target=\"#organicUnitDetailModal\""),
                "Organization detail must open each organic unit detail in a modal");
        assertTrue(organizationDetail.contains("organicUnitParentLookup")
                        && organizationDetail.contains("createUnitParentOrganicUnitId\" name=\"parentOrganicUnitId\" type=\"hidden\"")
                        && organizationDetail.contains("createUnitParentOrganicUnitDisplay\" type=\"text\" readonly")
                        && organizationDetail.contains("editUnitParentOrganicUnitId\" name=\"parentOrganicUnitId\" type=\"hidden\"")
                        && organizationDetail.contains("editUnitParentOrganicUnitDisplay\" type=\"text\" readonly")
                        && organizationDetail.contains("data-unit-dynamic-form")
                        && organizationDetail.contains("fetch(form.action")
                        && organizationDetail.contains("'X-Requested-With': 'XMLHttpRequest'")
                        && organizationDetail.contains("replaceUnitSurface")
                        && organizationDetail.contains("setSubmitLoading")
                        && organizationDetail.contains("og-button-spinner")
                        && organizationDetail.contains("Saving...")
                        && organizationDetail.contains("waitForMinimumElapsed")
                        && organizationDetail.contains("position: sticky")
                        && organizationDetail.contains(">Cancel</button>"),
                "Organic unit modals must keep Parent Unit read-only, keep footer actions visible and submit dynamically");
        assertFalse(organizationDetail.contains("<select id=\"createUnitParentOrganicUnitId\"")
                        || organizationDetail.contains("<select id=\"editUnitParentOrganicUnitId\"")
                        || organizationDetail.contains(">Close</button>"),
                "Organic unit modals must not expose editable parent selects or a hidden Close-only detail footer");
        assertTrue(organizationForm.contains("value=\"INACTIVE\""),
                "Organization edit form must expose inactive as the only disabled state");
        assertTrue(unitForm.contains("value=\"INACTIVE\""),
                "Organic unit edit form must expose inactive as the only disabled state");
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
    void publicCertificateValidationRemainsAvailableWithoutRevocation() throws IOException {
        String servlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/GradeCertificateServlet.java"));
        String service = Files.readString(JAVA_DIR.resolve("pt/isel/gape/learning/service/CertificateService.java"));
        String state = Files.readString(JAVA_DIR.resolve("pt/isel/gape/learning/model/CertificateState.java"));
        String authorization = Files.readString(JAVA_DIR.resolve("pt/isel/gape/security/authorization/AuthorizationPolicy.java"));
        String validationPage = Files.readString(WEBAPP_DIR.resolve("WEB-INF/views/public/certificate-validation.jsp"));
        String certificates = Files.readString(FRAGMENTS_DIR.resolve("learning-certificates-content.jspf"));

        assertTrue(servlet.contains("\"/certificates/validate\"")
                        && servlet.contains("showCertificateValidation(request, response)")
                        && servlet.contains("certificateService.validateCertificate(")
                        && servlet.contains("forward(request, response, VALIDATION_JSP)")
                        && service.contains("validateCertificate(String validationCode")
                        && authorization.contains("isPathOrChild(path, \"/certificates/validate\")")
                        && validationPage.contains("action=\"${pageContext.request.contextPath}/certificates/validate\"")
                        && validationPage.contains("${validation.valid}"),
                "Issued certificates must remain publicly verifiable through the dedicated validation route");
        assertFalse(state.contains("REVOKED")
                        || servlet.contains("revokeCertificate(")
                        || service.contains("revokeCertificate(")
                        || certificates.contains("/revoke"),
                "Certificate revocation must not remain in the model, services, routes or UI");
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

    @Test
    void learningManagementRowsUseACompilableForwardTarget() throws IOException {
        String servlet = Files.readString(JAVA_DIR.resolve("pt/isel/gape/web/controller/LessonManagementServlet.java"));
        String rowsPage = Files.readString(FRAGMENTS_DIR.resolve("learning-management-rows.jsp"));

        assertTrue(servlet.contains("/WEB-INF/fragments/learning-management-rows.jsp")
                        && !servlet.contains("/WEB-INF/fragments/learning-management-rows.jspf"),
                "The dynamically forwarded learning-management rows must target a JSP, not a static JSP fragment");
        assertTrue(rowsPage.contains("<%@ include file=\"/WEB-INF/fragments/learning-management-rows.jspf\" %>"),
                "The compiled rows endpoint must reuse the shared initial-render fragment");
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
        assertTrue(footer.contains("Terms & Conditions"),
                () -> "Expected dashboard footer Terms & Conditions link in " + page);
    }
}
