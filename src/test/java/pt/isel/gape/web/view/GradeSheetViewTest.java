package pt.isel.gape.web.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import pt.isel.gape.learning.model.GradeSheet;
import pt.isel.gape.learning.model.GradeSheetState;
import pt.isel.gape.learning.model.GradeSheetType;

class GradeSheetViewTest {

    @Test
    void studentRowHidesFinalGradeWhenAnyAssessmentGradeIsMissing() {
        GradeSheetView.StudentGradeRowView row = new GradeSheetView.StudentGradeRowView(
                1,
                42L,
                "Student",
                List.of("12", "-"),
                bd("14.00"),
                "approved"
        );

        assertEquals("-", row.getFinalGrade());
        assertNull(row.getFinalGradeValue());
        assertEquals("-", row.getResultLabel());
    }

    @Test
    void studentRowKeepsFinalGradeWhenZeroRepresentsCompletedMissingAssessment() {
        GradeSheetView.StudentGradeRowView row = new GradeSheetView.StudentGradeRowView(
                1,
                42L,
                "Student",
                List.of("12", "0"),
                bd("6.00"),
                "failed"
        );

        assertEquals("6", row.getFinalGrade());
        assertEquals(0, bd("6.00").compareTo(row.getFinalGradeValue()));
        assertEquals("failed", row.getResultLabel());
    }

    @Test
    void studentRowKeepsFinalGradeWhenNoAssessmentColumnsExist() {
        GradeSheetView.StudentGradeRowView row = new GradeSheetView.StudentGradeRowView(
                1,
                42L,
                "Student",
                List.of(),
                bd("14.00"),
                "approved"
        );

        assertEquals("14", row.getFinalGrade());
        assertEquals(0, bd("14.00").compareTo(row.getFinalGradeValue()));
        assertEquals("approved", row.getResultLabel());
    }

    @Test
    void publishedSheetWithMissingDisplayedGradeIsRenderedAsDraft() {
        GradeSheetView view = GradeSheetView.from(
                new GradeSheet(
                        10L,
                        20L,
                        "Class Sheet",
                        GradeSheetType.FINAL,
                        bd("20.00"),
                        bd("9.50"),
                        null,
                        null,
                        GradeSheetState.PUBLISHED,
                        List.of(30L),
                        List.of()
                ),
                40L,
                "Course",
                "Course",
                "CRS",
                null,
                bd("20.00"),
                null,
                null,
                "Subject",
                "Subject",
                "SUB",
                null,
                bd("6.00"),
                bd("20.00"),
                null,
                null,
                "Class",
                "CLS",
                null,
                null,
                null,
                null,
                1,
                List.of(new GradeSheetView.AssessmentColumnView(50L, "Test", bd("100.00"))),
                List.of(new GradeSheetView.StudentGradeRowView(
                        1,
                        60L,
                        "Student",
                        List.of("-"),
                        bd("12.00"),
                        "approved"
                ))
        );

        assertEquals("Draft", view.getStateLabel());
    }

    @Test
    void gradeDocumentTableMinWidthTracksAssessmentColumns() {
        GradeDocumentView emptyDocument = documentWithColumns(List.of());
        GradeDocumentView document = documentWithColumns(List.of(
                new GradeDocumentView.ColumnView("Test 1", "40%"),
                new GradeDocumentView.ColumnView("Test 2", "30%"),
                new GradeDocumentView.ColumnView("Exam", "30%")
        ));

        assertEquals(604, emptyDocument.getTableMinWidthPx());
        assertEquals(840, document.getTableMinWidthPx());
        assertEquals(6, document.getDocumentColumnCount());
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static GradeDocumentView documentWithColumns(List<GradeDocumentView.ColumnView> columns) {
        return new GradeDocumentView(
                "Pauta",
                "Turma",
                "Class A",
                "Class A",
                "Class A",
                "2025/2026",
                null,
                "ph ph-users",
                "No grades",
                "",
                columns,
                List.of()
        );
    }
}
