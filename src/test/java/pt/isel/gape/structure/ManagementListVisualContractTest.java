package pt.isel.gape.structure;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ManagementListVisualContractTest {

    private static final Path WEBAPP_DIR = Path.of("src/main/webapp");
    private static final Path FRAGMENTS_DIR = WEBAPP_DIR.resolve("WEB-INF/fragments");

    @Test
    void enrollmentGradeCertificateAndAttendanceListsUseOnePageScopedColumnContract() throws IOException {
        String attendancePage = Files.readString(WEBAPP_DIR.resolve("WEB-INF/views/learning/attendance.jsp"));
        String lessonsPage = Files.readString(WEBAPP_DIR.resolve("WEB-INF/views/learning/lesson-list.jsp"));

        assertAll(
                () -> assertTrue(attendancePage.contains(".aac-page .gape-structured-management-panel {")),
                () -> assertTrue(attendancePage.contains("--aac-structure-columns:")),
                () -> assertTrue(attendancePage.contains("grid-template-columns: var(--aac-structure-columns);")),
                () -> assertTrue(attendancePage.contains(".aac-page .aac-tree-row {")),
                () -> assertTrue(attendancePage.contains("box-shadow: inset 4px 0 0 #18a34a;")),
                () -> assertTrue(lessonsPage.contains("#lessons-panel .gape-learning-management-list-header,")),
                () -> assertTrue(lessonsPage.contains("#assessments-panel .gape-learning-management-list-header,")),
                () -> assertTrue(lessonsPage.contains("#attendance-panel .gape-structured-management-panel {")),
                () -> assertTrue(lessonsPage.contains("#attendance-panel .aac-tree-row {")),
                () -> assertTrue(lessonsPage.contains("#attendance-panel .aac-icon-button[data-bs-toggle=\"collapse\"][aria-expanded=\"true\"] i"))
        );
    }

    @Test
    void nestedManagementRowsReserveStateAndActionsInSeparateColumns() throws IOException {
        String grades = Files.readString(FRAGMENTS_DIR.resolve("learning-grades-certificates-content.jspf"));
        String certificates = Files.readString(FRAGMENTS_DIR.resolve("learning-certificates-content.jspf"));
        String attendance = Files.readString(FRAGMENTS_DIR.resolve("learning-attendance-content.jspf"));
        String enrollmentAndAttendance = Files.readString(WEBAPP_DIR.resolve("WEB-INF/views/learning/attendance.jsp"));

        Pattern stateInsideActions = Pattern.compile(
                "(?s)<div class=\"aac-tree-actions\">\\s*<span class=\"\\$\\{(?:activity\\.stateBadgeClass|item\\.statusBadgeClass|subjectGroup\\.stateBadgeClass|sheet\\.stateBadgeClass)"
        );

        assertAll(
                () -> assertTrue(grades.contains("<div class=\"aac-tree-status\">")),
                () -> assertTrue(certificates.contains("<div class=\"aac-tree-status\">")),
                () -> assertTrue(attendance.contains("<div class=\"aac-tree-status\">")),
                () -> assertTrue(enrollmentAndAttendance.contains("<div class=\"aac-tree-status\">")),
                () -> assertFalse(stateInsideActions.matcher(grades).find()),
                () -> assertFalse(stateInsideActions.matcher(certificates).find()),
                () -> assertFalse(stateInsideActions.matcher(attendance).find()),
                () -> assertFalse(stateInsideActions.matcher(enrollmentAndAttendance).find())
        );
    }
}
