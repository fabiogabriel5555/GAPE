package pt.isel.gape.web.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import pt.isel.gape.common.time.ApplicationDateTimeFormat;
import pt.isel.gape.common.validation.MediaPathValidator;
import pt.isel.gape.learning.model.GradeSheet;
import pt.isel.gape.learning.model.GradeSheetState;
import pt.isel.gape.learning.model.GradeSheetType;

public final class GradeSheetView {

    private final GradeSheet gradeSheet;
    private final long courseId;
    private final String courseLabel;
    private final String courseName;
    private final String courseAcronym;
    private final String coursePhoto;
    private final BigDecimal courseCertificateMaxGrade;
    private final String courseContextHtml;
    private final String courseContextTitle;
    private final long organizationId;
    private final String organizationLabel;
    private final Long organicUnitId;
    private final String organicUnitLabel;
    private final long courseOccurrenceId;
    private final String courseOccurrenceLabel;
    private final String courseOccurrenceDateRangeLabel;
    private final String courseOccurrenceStateValue;
    private final Long primaryClassGroupId;
    private final boolean primaryClassGroupCompleted;
    private final String subjectLabel;
    private final String subjectName;
    private final String subjectAcronym;
    private final String subjectPhoto;
    private final BigDecimal subjectEcts;
    private final BigDecimal subjectFinalGradeMax;
    private final String subjectContextHtml;
    private final String subjectContextTitle;
    private final String classGroupLabel;
    private final String classGroupCode;
    private final String periodLabel;
    private final String contextHtml;
    private final String contextTitle;
    private final String assessmentWeightLabel;
    private final int recordCount;
    private final List<AssessmentColumnView> assessmentColumns;
    private final List<StudentGradeRowView> studentRows;

    private GradeSheetView(
            GradeSheet gradeSheet,
            long courseId,
            String courseLabel,
            String courseName,
            String courseAcronym,
            String coursePhoto,
            BigDecimal courseCertificateMaxGrade,
            String courseContextHtml,
            String courseContextTitle,
            long organizationId,
            String organizationLabel,
            Long organicUnitId,
            String organicUnitLabel,
            long courseOccurrenceId,
            String courseOccurrenceLabel,
            String courseOccurrenceDateRangeLabel,
            String courseOccurrenceStateValue,
            Long primaryClassGroupId,
            boolean primaryClassGroupCompleted,
            String subjectLabel,
            String subjectName,
            String subjectAcronym,
            String subjectPhoto,
            BigDecimal subjectEcts,
            BigDecimal subjectFinalGradeMax,
            String subjectContextHtml,
            String subjectContextTitle,
            String classGroupLabel,
            String classGroupCode,
            String periodLabel,
            String contextHtml,
            String contextTitle,
            String assessmentWeightLabel,
            int recordCount,
            List<AssessmentColumnView> assessmentColumns,
            List<StudentGradeRowView> studentRows
    ) {
        this.gradeSheet = gradeSheet;
        this.courseId = courseId;
        this.courseLabel = emptyLabel(courseLabel, "Course");
        this.courseName = emptyLabel(courseName, this.courseLabel);
        this.courseAcronym = emptyLabel(courseAcronym, this.courseName);
        this.coursePhoto = MediaPathValidator.safeRelativePath(coursePhoto).orElse(null);
        this.courseCertificateMaxGrade = courseCertificateMaxGrade;
        this.courseContextHtml = emptyLabel(courseContextHtml, escapeHtml(this.courseAcronym));
        this.courseContextTitle = emptyLabel(courseContextTitle, this.courseName);
        this.organizationId = organizationId;
        this.organizationLabel = emptyLabel(organizationLabel, "Unknown organization");
        this.organicUnitId = organicUnitId;
        this.organicUnitLabel = emptyLabel(organicUnitLabel, "No organic unit");
        this.courseOccurrenceId = courseOccurrenceId;
        this.courseOccurrenceLabel = emptyLabel(courseOccurrenceLabel, "Occurrence " + courseOccurrenceId);
        this.courseOccurrenceDateRangeLabel = emptyLabel(courseOccurrenceDateRangeLabel, "-");
        this.courseOccurrenceStateValue = emptyLabel(courseOccurrenceStateValue, "scheduled");
        this.primaryClassGroupId = primaryClassGroupId;
        this.primaryClassGroupCompleted = primaryClassGroupCompleted;
        this.subjectLabel = emptyLabel(subjectLabel, "Subject " + gradeSheet.subjectId());
        this.subjectName = emptyLabel(subjectName, this.subjectLabel);
        this.subjectAcronym = emptyLabel(subjectAcronym, this.subjectName);
        this.subjectPhoto = MediaPathValidator.safeRelativePath(subjectPhoto).orElse(null);
        this.subjectEcts = subjectEcts;
        this.subjectFinalGradeMax = subjectFinalGradeMax;
        this.subjectContextHtml = emptyLabel(subjectContextHtml, escapeHtml(this.subjectAcronym));
        this.subjectContextTitle = emptyLabel(subjectContextTitle, this.subjectName);
        this.classGroupLabel = emptyLabel(classGroupLabel, "All class groups");
        this.classGroupCode = emptyLabel(classGroupCode, this.classGroupLabel);
        this.periodLabel = emptyLabel(periodLabel, "__-__-____ - __-__-____");
        this.contextHtml = emptyLabel(contextHtml, escapeHtml(this.classGroupCode));
        this.contextTitle = emptyLabel(contextTitle, this.classGroupLabel);
        this.assessmentWeightLabel = emptyLabel(assessmentWeightLabel, "Absolute average (all assessments)");
        this.recordCount = recordCount;
        this.assessmentColumns = assessmentColumns == null ? List.of() : List.copyOf(assessmentColumns);
        this.studentRows = studentRows == null ? List.of() : List.copyOf(studentRows);
    }

    public static GradeSheetView from(
            GradeSheet gradeSheet,
            String subjectLabel,
            String classGroupLabel,
            String assessmentWeightLabel,
            int recordCount
    ) {
        return new GradeSheetView(
                gradeSheet,
                0L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0L,
                null,
                null,
                null,
                gradeSheet.courseOccurrenceId(),
                null,
                null,
                null,
                null,
                false,
                subjectLabel,
                subjectLabel,
                subjectLabel,
                null,
                null,
                null,
                null,
                null,
                classGroupLabel,
                classGroupLabel,
                null,
                null,
                null,
                assessmentWeightLabel,
                recordCount,
                List.of(),
                List.of()
        );
    }

    public static GradeSheetView from(
            GradeSheet gradeSheet,
            long courseId,
            String courseLabel,
            String courseName,
            String courseAcronym,
            String coursePhoto,
            BigDecimal courseCertificateMaxGrade,
            String courseContextHtml,
            String courseContextTitle,
            String subjectLabel,
            String subjectName,
            String subjectAcronym,
            String subjectPhoto,
            BigDecimal subjectEcts,
            BigDecimal subjectFinalGradeMax,
            String subjectContextHtml,
            String subjectContextTitle,
            String classGroupLabel,
            String classGroupCode,
            String periodLabel,
            String contextHtml,
            String contextTitle,
            String assessmentWeightLabel,
            int recordCount,
            List<AssessmentColumnView> assessmentColumns,
            List<StudentGradeRowView> studentRows
    ) {
        return from(
                gradeSheet,
                courseId,
                courseLabel,
                courseName,
                courseAcronym,
                coursePhoto,
                courseCertificateMaxGrade,
                courseContextHtml,
                courseContextTitle,
                0L,
                null,
                null,
                null,
                gradeSheet.courseOccurrenceId(),
                null,
                null,
                null,
                null,
                false,
                subjectLabel,
                subjectName,
                subjectAcronym,
                subjectPhoto,
                subjectEcts,
                subjectFinalGradeMax,
                subjectContextHtml,
                subjectContextTitle,
                classGroupLabel,
                classGroupCode,
                periodLabel,
                contextHtml,
                contextTitle,
                assessmentWeightLabel,
                recordCount,
                assessmentColumns,
                studentRows
        );
    }

    public static GradeSheetView from(
            GradeSheet gradeSheet,
            long courseId,
            String courseLabel,
            String courseName,
            String courseAcronym,
            String coursePhoto,
            BigDecimal courseCertificateMaxGrade,
            String courseContextHtml,
            String courseContextTitle,
            long organizationId,
            String organizationLabel,
            Long organicUnitId,
            String organicUnitLabel,
            long courseOccurrenceId,
            String courseOccurrenceLabel,
            String courseOccurrenceDateRangeLabel,
            String courseOccurrenceStateValue,
            Long primaryClassGroupId,
            String subjectLabel,
            String subjectName,
            String subjectAcronym,
            String subjectPhoto,
            BigDecimal subjectEcts,
            BigDecimal subjectFinalGradeMax,
            String subjectContextHtml,
            String subjectContextTitle,
            String classGroupLabel,
            String classGroupCode,
            String periodLabel,
            String contextHtml,
            String contextTitle,
            String assessmentWeightLabel,
            int recordCount,
            List<AssessmentColumnView> assessmentColumns,
            List<StudentGradeRowView> studentRows
    ) {
        return from(
                gradeSheet,
                courseId,
                courseLabel,
                courseName,
                courseAcronym,
                coursePhoto,
                courseCertificateMaxGrade,
                courseContextHtml,
                courseContextTitle,
                organizationId,
                organizationLabel,
                organicUnitId,
                organicUnitLabel,
                courseOccurrenceId,
                courseOccurrenceLabel,
                courseOccurrenceDateRangeLabel,
                courseOccurrenceStateValue,
                primaryClassGroupId,
                false,
                subjectLabel,
                subjectName,
                subjectAcronym,
                subjectPhoto,
                subjectEcts,
                subjectFinalGradeMax,
                subjectContextHtml,
                subjectContextTitle,
                classGroupLabel,
                classGroupCode,
                periodLabel,
                contextHtml,
                contextTitle,
                assessmentWeightLabel,
                recordCount,
                assessmentColumns,
                studentRows
        );
    }

    public static GradeSheetView from(
            GradeSheet gradeSheet,
            long courseId,
            String courseLabel,
            String courseName,
            String courseAcronym,
            String coursePhoto,
            BigDecimal courseCertificateMaxGrade,
            String courseContextHtml,
            String courseContextTitle,
            long organizationId,
            String organizationLabel,
            Long organicUnitId,
            String organicUnitLabel,
            long courseOccurrenceId,
            String courseOccurrenceLabel,
            String courseOccurrenceDateRangeLabel,
            String courseOccurrenceStateValue,
            Long primaryClassGroupId,
            boolean primaryClassGroupCompleted,
            String subjectLabel,
            String subjectName,
            String subjectAcronym,
            String subjectPhoto,
            BigDecimal subjectEcts,
            BigDecimal subjectFinalGradeMax,
            String subjectContextHtml,
            String subjectContextTitle,
            String classGroupLabel,
            String classGroupCode,
            String periodLabel,
            String contextHtml,
            String contextTitle,
            String assessmentWeightLabel,
            int recordCount,
            List<AssessmentColumnView> assessmentColumns,
            List<StudentGradeRowView> studentRows
    ) {
        return new GradeSheetView(
                gradeSheet,
                courseId,
                courseLabel,
                courseName,
                courseAcronym,
                coursePhoto,
                courseCertificateMaxGrade,
                courseContextHtml,
                courseContextTitle,
                organizationId,
                organizationLabel,
                organicUnitId,
                organicUnitLabel,
                courseOccurrenceId,
                courseOccurrenceLabel,
                courseOccurrenceDateRangeLabel,
                courseOccurrenceStateValue,
                primaryClassGroupId,
                primaryClassGroupCompleted,
                subjectLabel,
                subjectName,
                subjectAcronym,
                subjectPhoto,
                subjectEcts,
                subjectFinalGradeMax,
                subjectContextHtml,
                subjectContextTitle,
                classGroupLabel,
                classGroupCode,
                periodLabel,
                contextHtml,
                contextTitle,
                assessmentWeightLabel,
                recordCount,
                assessmentColumns,
                studentRows
        );
    }

    public long getId() {
        return gradeSheet.id();
    }

    public long getSubjectId() {
        return gradeSheet.subjectId();
    }

    public String getTitle() {
        return gradeSheet.title();
    }

    public String getTypeValue() {
        return gradeSheet.type().toDatabaseValue();
    }

    public String getTypeLabel() {
        return typeLabel(gradeSheet.type());
    }

    public String getMaxGrade() {
        return gradeLabel(gradeSheet.maxGrade());
    }

    public BigDecimal getMaxGradeValue() {
        return gradeSheet.maxGrade();
    }

    public String getPassingGrade() {
        return gradeLabel(gradeSheet.passingGrade());
    }

    public BigDecimal getPassingGradeValue() {
        return gradeSheet.passingGrade();
    }

    public String getReleasedAt() {
        return format(gradeSheet.releasedAt());
    }

    public String getReleasedAtSort() {
        return gradeSheet.releasedAt() == null ? "" : gradeSheet.releasedAt().toString();
    }

    public String getStateValue() {
        return gradeSheet.state().toDatabaseValue();
    }

    public String getStateLabel() {
        return switch (gradeSheet.state()) {
            case DRAFT -> "Draft";
            case PUBLISHED -> "Published";
            case CLOSED -> "Closed";
            case INACTIVE -> "Inactive";
        };
    }

    public String getStateBadgeClass() {
        return switch (gradeSheet.state()) {
            case DRAFT -> "bg-warning-50 text-warning-600";
            case PUBLISHED -> "bg-success-50 text-success-600";
            case CLOSED -> "bg-neutral-20 text-neutral-600";
            case INACTIVE -> "bg-danger-50 text-danger-600";
        };
    }

    public long getCourseId() {
        return courseId;
    }

    public String getCourseLabel() {
        return courseLabel;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getCourseAcronym() {
        return courseAcronym;
    }

    public String getCoursePhoto() {
        return coursePhoto;
    }

    public boolean isCourseHasPhoto() {
        return coursePhoto != null && !coursePhoto.isBlank();
    }

    public BigDecimal getCourseCertificateMaxGradeValue() {
        return courseCertificateMaxGrade;
    }

    public String getCourseCertificateMaxGrade() {
        return gradeLabel(courseCertificateMaxGrade);
    }

    public String getCourseContextHtml() {
        return courseContextHtml;
    }

    public String getCourseContextTitle() {
        return courseContextTitle;
    }

    public long getOrganizationId() {
        return organizationId;
    }

    public String getOrganizationLabel() {
        return organizationLabel;
    }

    public Long getOrganicUnitId() {
        return organicUnitId;
    }

    public String getOrganicUnitLabel() {
        return organicUnitLabel;
    }

    public long getCourseOccurrenceId() {
        return courseOccurrenceId;
    }

    public String getCourseOccurrenceLabel() {
        return courseOccurrenceLabel;
    }

    public String getCourseOccurrenceDateRangeLabel() {
        return courseOccurrenceDateRangeLabel;
    }

    public String getCourseOccurrenceStateValue() {
        return courseOccurrenceStateValue;
    }

    public boolean isCourseOccurrenceCompleted() {
        return "completed".equals(courseOccurrenceStateValue);
    }

    public Long getPrimaryClassGroupId() {
        return primaryClassGroupId;
    }

    public boolean isPrimaryClassGroupCompleted() {
        return primaryClassGroupCompleted;
    }

    public String getSubjectLabel() {
        return subjectLabel;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getSubjectAcronym() {
        return subjectAcronym;
    }

    public String getSubjectPhoto() {
        return subjectPhoto;
    }

    public boolean isSubjectHasPhoto() {
        return subjectPhoto != null && !subjectPhoto.isBlank();
    }

    public BigDecimal getSubjectEctsValue() {
        return subjectEcts;
    }

    public String getSubjectEcts() {
        return gradeLabel(subjectEcts);
    }

    public BigDecimal getSubjectFinalGradeMaxValue() {
        return subjectFinalGradeMax;
    }

    public String getSubjectFinalGradeMax() {
        return gradeLabel(subjectFinalGradeMax);
    }

    public String getSubjectContextHtml() {
        return subjectContextHtml;
    }

    public String getSubjectContextTitle() {
        return subjectContextTitle;
    }

    public String getClassGroupLabel() {
        return classGroupLabel;
    }

    public String getClassGroupCode() {
        return classGroupCode;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public String getContextHtml() {
        return contextHtml;
    }

    public String getContextTitle() {
        return contextTitle;
    }

    public List<Long> getClassGroupIds() {
        return gradeSheet.classGroupIds();
    }

    public String getAssessmentWeightLabel() {
        return assessmentWeightLabel;
    }

    public String getWeightAlert() {
        return gradeSheet.weightAlert();
    }

    public boolean isHasWeightAlert() {
        return gradeSheet.weightAlert() != null && !gradeSheet.weightAlert().isBlank();
    }

    public String getPublicationExplanation() {
        return gradeSheet.publicationExplanation();
    }

    public boolean isHasPublicationExplanation() {
        return gradeSheet.publicationExplanation() != null && !gradeSheet.publicationExplanation().isBlank();
    }

    public String getRemarks() {
        List<String> remarks = new ArrayList<>();
        if (isHasPublicationExplanation()) {
            remarks.add(gradeSheet.publicationExplanation().trim());
        }
        if (isHasWeightAlert()) {
            remarks.add(gradeSheet.weightAlert().trim());
        }
        return String.join(" ", remarks);
    }

    public boolean isHasRemarks() {
        return !getRemarks().isBlank();
    }

    public int getRecordCount() {
        return recordCount;
    }

    public List<AssessmentColumnView> getAssessmentColumns() {
        return assessmentColumns;
    }

    public List<StudentGradeRowView> getStudentRows() {
        return studentRows;
    }

    public GradeDocumentView getDocument() {
        List<GradeDocumentView.ColumnView> columns;
        if (assessmentColumns.isEmpty()) {
            columns = List.of(new GradeDocumentView.ColumnView("Assessments", ""));
        } else {
            columns = assessmentColumns.stream()
                    .map(column -> new GradeDocumentView.ColumnView(
                            column.getTitle(),
                            column.getWeightLabel() + "%"
                    ))
                    .toList();
        }
        List<GradeDocumentView.RowView> rows = studentRows.stream()
                .map(row -> new GradeDocumentView.RowView(
                        row.getStudentId(),
                        row.getStudentName(),
                        row.getAssessmentValues().isEmpty() ? List.of("-") : row.getAssessmentValues(),
                        row.getFinalGradeValue()
                ))
                .toList();
        String classContextHtml = removeLeadingContextPart(contextHtml, classGroupCode);
        return new GradeDocumentView(
                "Class group grade sheet",
                "Subject",
                subjectName,
                "<strong>" + escapeHtml(classGroupCode) + "</strong><span>" + classContextHtml + "</span>",
                contextTitle,
                periodLabel,
                null,
                "ph ph-users-three",
                "No grade records.",
                getRemarks(),
                columns,
                rows
        );
    }

    private static String removeLeadingContextPart(String html, String acronym) {
        String leadingPart = contextPartHtml(acronym, acronym);
        if (html == null || !html.startsWith(leadingPart)) {
            return html;
        }
        String remainder = html.substring(leadingPart.length());
        return remainder.startsWith(" | ") ? remainder.substring(3) : remainder;
    }

    public int getDocumentColumnCount() {
        return 3 + Math.max(1, assessmentColumns.size());
    }

    public boolean isDraft() {
        return gradeSheet.state() == GradeSheetState.DRAFT;
    }

    public boolean isPublished() {
        return gradeSheet.state().blocksDirectChanges();
    }

    public String getIconClass() {
        return switch (gradeSheet.type()) {
            case FINAL -> "ph ph-graduation-cap";
            case CONTINUOUS_ASSESSMENT -> "ph ph-chart-line-up";
            case EXAM -> "ph ph-exam";
            case PARTIAL -> "ph ph-chart-pie-slice";
            case OTHER -> "ph ph-table";
        };
    }

    public String getSoftClass() {
        return switch (gradeSheet.type()) {
            case FINAL -> "bg-main-50 text-main-600";
            case CONTINUOUS_ASSESSMENT -> "bg-info-50 text-info-600";
            case EXAM -> "bg-warning-50 text-warning-600";
            case PARTIAL -> "bg-success-50 text-success-600";
            case OTHER -> "bg-neutral-20 text-neutral-600";
        };
    }

    public static String gradeLabel(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    public static String format(LocalDateTime value) {
        return value == null ? "-" : ApplicationDateTimeFormat.dateTime(value);
    }

    public static String typeLabel(GradeSheetType type) {
        return switch (type) {
            case FINAL -> "Final";
            case CONTINUOUS_ASSESSMENT -> "Continuous";
            case EXAM -> "Exam";
            case PARTIAL -> "Partial";
            case OTHER -> "Other";
        };
    }

    public static String contextPartHtml(String acronym, String name) {
        String compact = acronym == null || acronym.isBlank() ? name : acronym;
        compact = compact == null || compact.isBlank() ? "-" : compact;
        return "<span class=\"gape-acronym-token\" tabindex=\"0\" title=\""
                + escapeHtml(name == null || name.isBlank() ? compact : name)
                + "\">"
                + escapeHtml(compact)
                + "</span>";
    }

    private static String emptyLabel(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public static final class AssessmentColumnView {

        private final long assessmentId;
        private final String title;
        private final BigDecimal weight;
        private final String weightLabel;

        public AssessmentColumnView(long assessmentId, String title, BigDecimal weight) {
            this.assessmentId = assessmentId;
            this.title = emptyLabel(title, "Assessment");
            this.weight = weight;
            this.weightLabel = gradeLabel(weight);
        }

        public long getAssessmentId() {
            return assessmentId;
        }

        public String getTitle() {
            return title;
        }

        public BigDecimal getWeightValue() {
            return weight;
        }

        public String getWeightLabel() {
            return weightLabel;
        }

        public String getHeaderLabel() {
            return title + " (" + weightLabel + "%)";
        }

        public String getHeaderTitle() {
            return title + " (" + weightLabel + "%)";
        }
    }

    public static final class StudentGradeRowView {

        private final int number;
        private final long studentId;
        private final String studentName;
        private final List<String> assessmentValues;
        private final String finalGrade;
        private final BigDecimal finalGradeValue;
        private final String resultLabel;

        public StudentGradeRowView(
                int number,
                long studentId,
                String studentName,
                int assessmentColumnCount,
                BigDecimal finalGrade,
                String resultLabel
        ) {
            this(number, studentId, studentName, placeholderAssessmentValues(assessmentColumnCount), finalGrade, resultLabel);
        }

        public StudentGradeRowView(
                int number,
                long studentId,
                String studentName,
                List<String> assessmentValues,
                BigDecimal finalGrade,
                String resultLabel
        ) {
            this.number = number;
            this.studentId = studentId;
            this.studentName = emptyLabel(studentName, "Student");
            this.assessmentValues = List.copyOf(assessmentValues);
            this.finalGradeValue = hasCompleteAssessmentValues(this.assessmentValues) ? finalGrade : null;
            this.finalGrade = gradeLabel(this.finalGradeValue);
            this.resultLabel = this.finalGradeValue == null ? "-" : emptyLabel(resultLabel, "-");
        }

        private static List<String> placeholderAssessmentValues(int assessmentColumnCount) {
            List<String> values = new ArrayList<>();
            for (int index = 0; index < assessmentColumnCount; index++) {
                values.add("-");
            }
            return values;
        }

        public int getNumber() {
            return number;
        }

        public long getStudentId() {
            return studentId;
        }

        public String getStudentName() {
            return studentName;
        }

        public List<String> getAssessmentValues() {
            return assessmentValues;
        }

        public String getFinalGrade() {
            return finalGrade;
        }

        public BigDecimal getFinalGradeValue() {
            return finalGradeValue;
        }

        public String getResultLabel() {
            return resultLabel;
        }

        private boolean hasCompleteDisplayedGrades() {
            return finalGradeValue != null
                    && !isMissingGrade(finalGrade)
                    && hasCompleteAssessmentValues(assessmentValues);
        }

        private static boolean hasCompleteAssessmentValues(List<String> assessmentValues) {
            if (assessmentValues == null) {
                return false;
            }
            if (assessmentValues.isEmpty()) {
                return true;
            }
            for (String assessmentValue : assessmentValues) {
                if (isMissingGrade(assessmentValue)) {
                    return false;
                }
            }
            return true;
        }

        private static boolean isMissingGrade(String value) {
            return value == null || value.isBlank() || "-".equals(value.trim());
        }
    }
}
