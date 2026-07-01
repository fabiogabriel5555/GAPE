package pt.isel.gape.web.view;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class GradeDocumentView {

    private static final int NUMBER_COLUMN_WIDTH_PX = 70;
    private static final int NAME_COLUMN_WIDTH_PX = 300;
    private static final int ASSESSMENT_COLUMN_WIDTH_PX = 118;
    private static final int FINAL_COLUMN_WIDTH_PX = 116;

    private final String title;
    private final String entityLabel;
    private final String entityName;
    private final String contextHtml;
    private final String contextTitle;
    private final String periodLabel;
    private final String imagePath;
    private final String fallbackIconClass;
    private final String emptyMessage;
    private final String alert;
    private final List<ColumnView> columns;
    private final List<RowView> rows;

    public GradeDocumentView(
            String title,
            String entityLabel,
            String entityName,
            String contextHtml,
            String contextTitle,
            String periodLabel,
            String imagePath,
            String fallbackIconClass,
            String emptyMessage,
            String alert,
            List<ColumnView> columns,
            List<RowView> rows
    ) {
        this.title = emptyLabel(title, "Pauta");
        this.entityLabel = emptyLabel(entityLabel, "Contexto");
        this.entityName = emptyLabel(entityName, "-");
        this.contextHtml = emptyLabel(contextHtml, GradeSheetView.escapeHtml(this.entityName));
        this.contextTitle = emptyLabel(contextTitle, this.entityName);
        this.periodLabel = emptyLabel(periodLabel, "__.__.____ - __.__.____");
        this.imagePath = imagePath == null || imagePath.isBlank() ? null : imagePath;
        this.fallbackIconClass = emptyLabel(fallbackIconClass, "ph ph-image");
        this.emptyMessage = emptyLabel(emptyMessage, "Sem registos de nota.");
        this.alert = alert == null ? "" : alert;
        this.columns = columns == null ? List.of() : List.copyOf(columns);
        this.rows = rows == null ? List.of() : List.copyOf(rows);
    }

    public String getTitle() {
        return title;
    }

    public String getEntityLabel() {
        return entityLabel;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getContextHtml() {
        return contextHtml;
    }

    public String getContextTitle() {
        return contextTitle;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public String getImagePath() {
        return imagePath;
    }

    public boolean isHasImage() {
        return imagePath != null && !imagePath.isBlank();
    }

    public String getFallbackIconClass() {
        return fallbackIconClass;
    }

    public String getEmptyMessage() {
        return emptyMessage;
    }

    public String getAlert() {
        return alert;
    }

    public boolean isHasAlert() {
        return alert != null && !alert.isBlank();
    }

    public List<ColumnView> getColumns() {
        return columns;
    }

    public List<RowView> getRows() {
        return rows;
    }

    public int getVariableColumnCount() {
        return Math.max(1, columns.size());
    }

    public int getDocumentColumnCount() {
        return 3 + getVariableColumnCount();
    }

    public int getTableMinWidthPx() {
        return NUMBER_COLUMN_WIDTH_PX
                + NAME_COLUMN_WIDTH_PX
                + (ASSESSMENT_COLUMN_WIDTH_PX * getVariableColumnCount())
                + FINAL_COLUMN_WIDTH_PX;
    }

    private static String emptyLabel(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static final class ColumnView {

        private final String title;
        private final String suffix;
        private final String fullTitle;

        public ColumnView(String title, String suffix) {
            this(title, suffix, title);
        }

        public ColumnView(String title, String suffix, String fullTitle) {
            this.title = emptyLabel(title, "-");
            this.suffix = suffix == null ? "" : suffix;
            this.fullTitle = emptyLabel(fullTitle, this.title);
        }

        public String getTitle() {
            return title;
        }

        public String getFullTitle() {
            return fullTitle;
        }

        public String getSuffix() {
            return suffix;
        }

        public String getHeaderLabel() {
            return suffix.isBlank() ? title : title + " (" + suffix + ")";
        }

        public String getHeaderTitle() {
            return suffix.isBlank() ? fullTitle : fullTitle + " (" + suffix + ")";
        }
    }

    public static final class RowView {

        private final long studentId;
        private final String studentName;
        private final List<String> values;
        private final String finalGrade;
        private final BigDecimal finalGradeValue;

        public RowView(
                long studentId,
                String studentName,
                List<String> values,
                BigDecimal finalGradeValue
        ) {
            this.studentId = studentId;
            this.studentName = emptyLabel(studentName, "Student");
            this.values = values == null ? List.of() : List.copyOf(values);
            this.finalGrade = GradeSheetView.gradeLabel(finalGradeValue);
            this.finalGradeValue = finalGradeValue;
        }

        public RowView(
                long studentId,
                String studentName,
                int valueColumnCount,
                BigDecimal finalGradeValue
        ) {
            this(studentId, studentName, blankValues(valueColumnCount), finalGradeValue);
        }

        public long getStudentId() {
            return studentId;
        }

        public String getStudentName() {
            return studentName;
        }

        public List<String> getValues() {
            return values;
        }

        public String getFinalGrade() {
            return finalGrade;
        }

        public BigDecimal getFinalGradeValue() {
            return finalGradeValue;
        }

        private static List<String> blankValues(int count) {
            int safeCount = Math.max(1, count);
            List<String> values = new ArrayList<>();
            for (int index = 0; index < safeCount; index++) {
                values.add("-");
            }
            return values;
        }
    }
}
