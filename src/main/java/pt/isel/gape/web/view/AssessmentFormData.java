package pt.isel.gape.web.view;

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.learning.model.Assessment;
import pt.isel.gape.learning.model.AssessmentState;

public final class AssessmentFormData {

    private final Long id;
    private final String subjectId;
    private final String contentBlockId;
    private final List<String> classGroupIds;
    private final String title;
    private final String description;
    private final String type;
    private final String mode;
    private final String correctionMode;
    private final String maxGrade;
    private final String passingGrade;
    private final String finalGradeWeight;
    private final String attemptsLimit;
    private final String enrollmentMode;
    private final String state;
    private final String availableFrom;
    private final String availableUntil;

    private AssessmentFormData(
            Long id,
            String subjectId,
            String contentBlockId,
            List<String> classGroupIds,
            String title,
            String description,
            String type,
            String mode,
            String correctionMode,
            String maxGrade,
            String passingGrade,
            String finalGradeWeight,
            String attemptsLimit,
            String enrollmentMode,
            String state,
            String availableFrom,
            String availableUntil
    ) {
        this.id = id;
        this.subjectId = subjectId;
        this.contentBlockId = contentBlockId;
        this.classGroupIds = classGroupIds == null ? List.of() : List.copyOf(classGroupIds);
        this.title = title;
        this.description = description;
        this.type = normalizeType(defaultValue(type, "form"));
        this.mode = defaultValue(mode, "online");
        this.correctionMode = defaultValue(correctionMode, "automatic");
        this.maxGrade = defaultValue(maxGrade, "20.00");
        this.passingGrade = defaultValue(passingGrade, "10.00");
        this.finalGradeWeight = defaultValue(finalGradeWeight, "100.00");
        this.attemptsLimit = attemptsLimit;
        this.enrollmentMode = defaultValue(enrollmentMode, "auto_approve");
        this.state = defaultValue(state, "draft");
        this.availableFrom = availableFrom;
        this.availableUntil = availableUntil;
    }

    public static AssessmentFormData blank(Long subjectId, Long contentBlockId) {
        return blank(
                subjectId,
                contentBlockId,
                "form",
                contentBlockId == null ? "onsite" : "online",
                contentBlockId == null ? "manual" : "automatic"
        );
    }

    public static AssessmentFormData blank(
            Long subjectId,
            Long contentBlockId,
            String type,
            String mode,
            String correctionMode
    ) {
        String defaultType = "form";
        String safeType = supportedValue(type, defaultType, "form", "test", "exam", "questionnaire");
        if ("questionnaire".equals(safeType)) {
            safeType = "form";
        }
        String safeMode = supportedValue(mode, contentBlockId == null ? "onsite" : "online", "online", "onsite");
        String safeCorrectionMode = supportedValue(
                correctionMode,
                contentBlockId == null ? "manual" : "automatic",
                "automatic",
                "mixed",
                "manual"
        );
        if ("onsite".equals(safeMode)) {
            safeCorrectionMode = "manual";
        }
        return new AssessmentFormData(
                null,
                stringValue(subjectId),
                stringValue(contentBlockId),
                List.of(),
                "",
                "",
                safeType,
                safeMode,
                safeCorrectionMode,
                "20.00",
                "10.00",
                "100.00",
                "",
                "auto_approve",
                "draft",
                "",
                ""
        );
    }

    public static AssessmentFormData from(Assessment assessment) {
        return new AssessmentFormData(
                assessment.id(),
                stringValue(assessment.subjectId()),
                stringValue(assessment.contentBlockId()),
                List.of(),
                assessment.title(),
                assessment.description(),
                assessment.type().toDatabaseValue(),
                assessment.mode().toDatabaseValue(),
                assessment.correctionMode().toDatabaseValue(),
                assessment.maxGrade() == null ? "" : assessment.maxGrade().toPlainString(),
                assessment.passingGrade() == null ? "" : assessment.passingGrade().toPlainString(),
                assessment.finalGradeWeight() == null ? "" : assessment.finalGradeWeight().toPlainString(),
                stringValue(assessment.attemptsLimit()),
                assessment.enrollmentMode().toDatabaseValue(),
                editableState(assessment.state()).toDatabaseValue(),
                assessment.availableFrom() == null ? "" : assessment.availableFrom().toString(),
                assessment.availableUntil() == null ? "" : assessment.availableUntil().toString()
        );
    }

    public static AssessmentFormData from(Assessment assessment, List<Long> classGroupIds) {
        return new AssessmentFormData(
                assessment.id(),
                stringValue(assessment.subjectId()),
                stringValue(assessment.contentBlockId()),
                classGroupIds == null ? List.of() : classGroupIds.stream()
                        .map(AssessmentFormData::stringValue)
                        .toList(),
                assessment.title(),
                assessment.description(),
                assessment.type().toDatabaseValue(),
                assessment.mode().toDatabaseValue(),
                assessment.correctionMode().toDatabaseValue(),
                assessment.maxGrade() == null ? "" : assessment.maxGrade().toPlainString(),
                assessment.passingGrade() == null ? "" : assessment.passingGrade().toPlainString(),
                assessment.finalGradeWeight() == null ? "" : assessment.finalGradeWeight().toPlainString(),
                stringValue(assessment.attemptsLimit()),
                assessment.enrollmentMode().toDatabaseValue(),
                editableState(assessment.state()).toDatabaseValue(),
                assessment.availableFrom() == null ? "" : assessment.availableFrom().toString(),
                assessment.availableUntil() == null ? "" : assessment.availableUntil().toString()
        );
    }

    public static AssessmentFormData from(HttpServletRequest request, Long id) {
        return new AssessmentFormData(
                id,
                text(request, "subjectId"),
                text(request, "contentBlockId"),
                stringList(request.getParameterValues("classGroupIds")),
                text(request, "title"),
                text(request, "description"),
                text(request, "type"),
                text(request, "mode"),
                text(request, "correctionMode"),
                text(request, "maxGrade"),
                text(request, "passingGrade"),
                text(request, "finalGradeWeight"),
                text(request, "attemptsLimit"),
                text(request, "enrollmentMode"),
                text(request, "state"),
                text(request, "availableFrom"),
                text(request, "availableUntil")
        );
    }

    public Long getId() {
        return id;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getContentBlockId() {
        return contentBlockId;
    }

    public List<String> getClassGroupIds() {
        return classGroupIds;
    }

    public boolean isClassGroupSelected(String classGroupId) {
        return classGroupIds.contains(classGroupId);
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public String getMode() {
        return mode;
    }

    public String getCorrectionMode() {
        return correctionMode;
    }

    public String getMaxGrade() {
        return maxGrade;
    }

    public String getPassingGrade() {
        return passingGrade;
    }

    public String getFinalGradeWeight() {
        return finalGradeWeight;
    }

    public String getAttemptsLimit() {
        return attemptsLimit;
    }

    public String getEnrollmentMode() {
        return enrollmentMode;
    }

    public String getState() {
        return state;
    }

    public String getAvailableFrom() {
        return availableFrom;
    }

    public String getAvailableUntil() {
        return availableUntil;
    }

    private static String text(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private static List<String> stringList(String[] values) {
        if (values == null || values.length == 0) {
            return List.of();
        }
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String defaultValue(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String normalizeType(String value) {
        return "questionnaire".equalsIgnoreCase(value == null ? "" : value.trim()) ? "form" : value;
    }

    private static String supportedValue(String value, String defaultValue, String... supportedValues) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        for (String supportedValue : supportedValues) {
            if (supportedValue.equals(normalized)) {
                return supportedValue;
            }
        }
        return defaultValue;
    }

    private static AssessmentState editableState(AssessmentState state) {
        return state == AssessmentState.DRAFT ? AssessmentState.DRAFT : AssessmentState.SCHEDULED;
    }
}
