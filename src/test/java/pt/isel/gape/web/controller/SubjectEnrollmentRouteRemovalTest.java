package pt.isel.gape.web.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class SubjectEnrollmentRouteRemovalTest {

    private static final Set<String> SOURCE_EXTENSIONS = Set.of(
            ".java", ".jsp", ".jspf", ".js", ".xml", ".sql", ".properties"
    );

    private static final Path CONTROLLER_DIRECTORY = Path.of(
            "src",
            "main",
            "java",
            "pt",
            "isel",
            "gape",
            "web",
            "controller"
    );

    private static final Map<Path, Set<String>> LEGACY_CLEANUP_TOKENS = Map.of(
            Path.of("src/main/java/pt/isel/gape/common/config/DatabaseMigrationService.java"),
            Set.of("enroll_subject", "subject_enrollment_policy"),
            Path.of("src/main/resources/sql/drop.sql"),
            Set.of("enroll_subject", "subject_enrollment_policy"),
            Path.of("src/main/resources/sql/migration/V002__remove_subject_enrollment_artifacts.sql"),
            Set.of("enroll_subject", "subject_enrollment_policy")
    );

    @Test
    void courseAndSubjectControllersDoNotExposeSubjectEnrollmentPolicyOrActions() throws Exception {
        String courseServlet = Files.readString(CONTROLLER_DIRECTORY.resolve("CourseManagementServlet.java"));
        String subjectServlet = Files.readString(CONTROLLER_DIRECTORY.resolve("SubjectManagementServlet.java"));
        String combined = courseServlet + subjectServlet;

        assertFalse(combined.contains("EnrollmentApprovalPolicyDAO"));
        assertFalse(combined.contains("EnrollmentApprovalMode"));
        assertFalse(combined.contains("updateSubjectEnrollmentPolicy"));
        assertFalse(combined.contains("subjectMode("));
        assertFalse(combined.contains("subjectEnrollmentPolicy"));
        assertFalse(combined.contains("\"policies\".equals(segments[1])"));
        assertFalse(subjectServlet.contains("\"enrollments\".equals(segments[1])"));
    }

    @Test
    void curricularCourseSubjectAssociationActionsRemainAvailable() throws Exception {
        String courseServlet = Files.readString(CONTROLLER_DIRECTORY.resolve("CourseManagementServlet.java"));
        String subjectServlet = Files.readString(CONTROLLER_DIRECTORY.resolve("SubjectManagementServlet.java"));

        assertTrue(courseServlet.contains("courseSubjectService.associateSubject("));
        assertTrue(courseServlet.contains("courseSubjectService.updateAssociation("));
        assertTrue(subjectServlet.contains("courseSubjectService.associateSubject("));
        assertTrue(subjectServlet.contains("courseSubjectService.updateAssociation("));
    }

    @Test
    void studentAdministrationOnlyAssignsCourseContexts() throws Exception {
        String userServlet = Files.readString(CONTROLLER_DIRECTORY.resolve("UserManagementServlet.java"));
        String userService = Files.readString(Path.of(
                "src/main/java/pt/isel/gape/access/service/UserService.java"
        ));
        String contextModel = Files.readString(Path.of(
                "src/main/java/pt/isel/gape/access/model/AccessProfileContextAssignment.java"
        ));
        String userForm = Files.readString(Path.of(
                "src/main/webapp/admin/admin/user/admin-user-form.jsp"
        ));

        assertTrue(userForm.contains("Student Course Context"));
        assertTrue(contextModel.contains("Student context must be a course"));
        assertFalse(userServlet.contains("STUDENT_COURSE_SUBJECT"));
        assertFalse(userService.contains("selectedStudentSubjects")
                || userService.contains("studentSubjectIdsByCourse")
                || userService.contains("synchronizeStudentEnrollments")
                || userService.contains("requireActiveCourseSubject"));
    }

    @Test
    void mainSourcesContainNoSubjectEnrollmentArtifacts() throws Exception {
        try (Stream<Path> sources = Files.walk(Path.of("src/main"))) {
            for (Path sourcePath : sources.filter(Files::isRegularFile)
                    .filter(SubjectEnrollmentRouteRemovalTest::isTextSource)
                    .toList()) {
                String source = Files.readString(sourcePath);
                for (String cleanupToken : LEGACY_CLEANUP_TOKENS.getOrDefault(sourcePath, Set.of())) {
                    source = source.replace(cleanupToken, "");
                }
                assertFalse(source.contains("SubjectEnrollment")
                                || source.contains("subject_enrollment")
                                || source.contains("enroll_subject")
                                || source.contains("subject-enrollment")
                                || (source.contains("/student/enrollments/courses/")
                                && source.contains("/subjects/")),
                        () -> "Removed subject enrollment artifact found in " + sourcePath);
            }
        }
    }

    @Test
    void legacyDatabaseArtifactsOnlyRemainAsExplicitIdempotentCleanup() throws Exception {
        String dropScript = Files.readString(Path.of("src/main/resources/sql/drop.sql"));
        String migration = Files.readString(Path.of(
                "src/main/resources/sql/migration/V002__remove_subject_enrollment_artifacts.sql"
        ));

        for (String source : List.of(dropScript, migration)) {
            assertTrue(source.contains("DROP TRIGGER IF EXISTS bi_enroll_subject_validate"));
            assertTrue(source.contains("DROP TRIGGER IF EXISTS bu_enroll_subject_validate"));
            assertTrue(source.contains("DROP TABLE IF EXISTS subject_enrollment_policy"));
            assertTrue(source.contains("DROP TABLE IF EXISTS enroll_subject"));
        }
    }

    private static boolean isTextSource(Path path) {
        String name = path.getFileName().toString();
        return SOURCE_EXTENSIONS.stream().anyMatch(name::endsWith);
    }
}
