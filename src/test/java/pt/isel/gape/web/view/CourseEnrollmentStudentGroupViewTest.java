package pt.isel.gape.web.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import pt.isel.gape.learning.model.CourseEnrollment;
import pt.isel.gape.learning.model.EnrollmentState;

class CourseEnrollmentStudentGroupViewTest {

    @Test
    void groupsCourseOccurrenceEnrollmentsByStudentAndKeepsEachOccurrence() {
        EnrollmentManagementView olderFabioEnrollment = enrollment(
                6511L,
                3008L,
                2025L,
                EnrollmentState.INACTIVE,
                "Fábio Gabriel Pontes Baiona",
                "fabio@example.test",
                "2025-2026"
        );
        EnrollmentManagementView newerFabioEnrollment = enrollment(
                6511L,
                3008L,
                2027L,
                EnrollmentState.ACTIVE,
                "Fábio Gabriel Pontes Baiona",
                "fabio@example.test",
                "2027-2028"
        );
        EnrollmentManagementView otherStudentEnrollment = enrollment(
                6512L,
                3008L,
                2026L,
                EnrollmentState.ACTIVE,
                "Zoe Example",
                "zoe@example.test",
                "2026-2027"
        );

        List<CourseEnrollmentStudentGroupView> groups = CourseEnrollmentStudentGroupView.group(List.of(
                olderFabioEnrollment,
                otherStudentEnrollment,
                newerFabioEnrollment
        ));

        assertEquals(2, groups.size());
        assertEquals(6512L, groups.get(0).getStudentUserId());

        CourseEnrollmentStudentGroupView fabioGroup = groups.stream()
                .filter(group -> group.getStudentUserId() == 6511L)
                .findFirst()
                .orElseThrow();
        assertEquals("Fábio Gabriel Pontes Baiona", fabioGroup.getStudentName());
        assertEquals("fabio@example.test", fabioGroup.getStudentEmail());
        assertEquals(2, fabioGroup.getEnrollmentCount());
        assertEquals("2027-09-01", fabioGroup.getLatestEnrollmentStartDateValue());
        assertIterableEquals(
                List.of(2027L, 2025L),
                fabioGroup.getEnrollments().stream()
                        .map(EnrollmentManagementView::getCourseOccurrenceId)
                        .toList()
        );
        assertEquals(2, fabioGroup.getStateSummaries().size());
        assertEquals("Active", fabioGroup.getStateSummaries().get(0).getLabel());
        assertEquals(1, fabioGroup.getStateSummaries().get(0).getCount());
        assertEquals("Inactive", fabioGroup.getStateSummaries().get(1).getLabel());
        assertEquals(1, fabioGroup.getStateSummaries().get(1).getCount());
    }

    @Test
    void groupingAnEmptyEnrollmentListProducesNoStudentRows() {
        assertTrue(CourseEnrollmentStudentGroupView.group(List.of()).isEmpty());
    }

    @Test
    void courseDetailsRendersStudentGroupsWithAnAccessibleShowControl() throws IOException {
        String servlet = Files.readString(Path.of(
                "src/main/java/pt/isel/gape/web/controller/CourseManagementServlet.java"
        ));
        String management = Files.readString(Path.of(
                "src/main/webapp/WEB-INF/fragments/course-enrollment-management.jspf"
        ));
        String enrollmentCard = Files.readString(Path.of(
                "src/main/webapp/WEB-INF/fragments/course-enrollment-card.jspf"
        ));

        assertTrue(servlet.contains("CourseEnrollmentStudentGroupView.group(currentEnrollments)"));
        assertTrue(servlet.contains("CourseEnrollmentStudentGroupView.group(completedEnrollments)"));
        assertTrue(management.contains("items=\"${currentCourseEnrollmentStudentGroups}\""));
        assertTrue(management.contains("items=\"${completedCourseEnrollmentStudentGroups}\""));
        assertTrue(management.contains("data-gape-tree-toggle=\"currentCourseStudentEnrollments"));
        assertTrue(management.contains("data-gape-tree-toggle=\"completedCourseStudentEnrollments"));
        assertTrue(management.contains("class=\"gape-tree-toggle text-20 text-neutral-500 hover-text-main-600\""));
        assertFalse(management.contains("data-gape-toggle-label>Show</span>"));
        assertTrue(enrollmentCard.contains("courseEnrollmentGrouped"));
        assertTrue(enrollmentCard.contains("gape-structure-row gape-enrollment-row"));
        assertFalse(management.contains("items=\"${currentCourseEnrollments}\""));
        assertFalse(management.contains("items=\"${completedCourseEnrollments}\""));
    }

    private static EnrollmentManagementView enrollment(
            long studentUserId,
            long courseId,
            long occurrenceId,
            EnrollmentState state,
            String studentName,
            String studentEmail,
            String occurrenceCode
    ) {
        int academicYear = Integer.parseInt(occurrenceCode.substring(0, 4));
        return EnrollmentManagementView.course(
                new CourseEnrollment(
                        studentUserId,
                        courseId,
                        occurrenceId,
                        state,
                        LocalDate.of(academicYear, 9, 1),
                        LocalDate.of(academicYear + 1, 8, 31)
                ),
                studentName,
                studentEmail,
                occurrenceCode
        );
    }
}
