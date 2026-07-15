package pt.isel.gape.structure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FrontendAccessibilityStructureTest {

    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Path FRAGMENTS = WEBAPP.resolve("WEB-INF/fragments");

    @Test
    void studentSubjectsRemainCurricularWithoutIndependentEnrollmentActions() throws IOException {
        String servlet = Files.readString(Path.of(
                "src/main/java/pt/isel/gape/web/controller/StudentEnrollmentServlet.java"
        ));
        String subjects = read("student/student/subject/student-subjects.jsp");
        String subjectDetail = read("student/student/subject/student-subject-detail.jsp");
        String courseDetail = read("student/student/course/student-course-detail.jsp");
        String publicCourseDetail = read("course-details.jsp");

        assertTrue(servlet.contains("curricularSubjects")
                        && servlet.contains("activeCourseEnrollments")
                        && servlet.contains("enrollment.state() == EnrollmentState.ACTIVE")
                        && servlet.contains("courseOccurrenceContextKey(")
                        && servlet.contains("classGroup.courseOccurrenceId() == enrollment.courseOccurrenceId()"));
        for (String source : new String[]{servlet, subjects, subjectDetail, courseDetail, publicCourseDetail}) {
            assertFalse(source.contains("SubjectEnrollment"));
            assertFalse(source.contains("subjectEnrollment"));
            assertFalse(source.contains("data-gape-enrollment-target=\"subject-"));
            assertFalse(source.contains("/student/enrollments/courses/${course.id}/subjects/"));
        }
        assertFalse(Files.exists(FRAGMENTS.resolve("subject-enrollment-management.jspf")));
        assertFalse(Files.exists(FRAGMENTS.resolve("subject-enrollment-row.jspf")));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
