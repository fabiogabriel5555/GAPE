package pt.isel.gape.web.view;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.learning.model.ClassGroup;
import pt.isel.gape.learning.model.ClassGroupState;

public final class ClassGroupFormData {

    private final Long id;
    private final String courseId;
    private final String subjectId;
    private final String code;
    private final String modality;
    private final String state;
    private final String minStudents;
    private final String maxStudents;
    private final String startsAt;
    private final String endsAt;
    private final String shift;
    private final String showContentThumbnails;

    private ClassGroupFormData(
            Long id,
            String courseId,
            String subjectId,
            String code,
            String modality,
            String state,
            String minStudents,
            String maxStudents,
            String startsAt,
            String endsAt,
            String shift,
            String showContentThumbnails
    ) {
        this.id = id;
        this.courseId = courseId;
        this.subjectId = subjectId;
        this.code = code;
        this.modality = modality;
        this.state = state == null || state.isBlank() ? "DRAFT" : state;
        this.minStudents = minStudents;
        this.maxStudents = maxStudents;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.shift = shift;
        this.showContentThumbnails = "true".equalsIgnoreCase(showContentThumbnails) ? "true" : "false";
    }

    public static ClassGroupFormData blank(Long courseId, Long subjectId) {
        return new ClassGroupFormData(
                null,
                stringValue(courseId),
                stringValue(subjectId),
                "",
                "ONSITE",
                "DRAFT",
                "",
                "",
                "",
                "",
                "morning",
                "false"
        );
    }

    public static ClassGroupFormData from(ClassGroup classGroup) {
        return new ClassGroupFormData(
                classGroup.id(),
                Long.toString(classGroup.courseId()),
                Long.toString(classGroup.subjectId()),
                classGroup.code(),
                classGroup.modality().name(),
                editableState(classGroup.state()).name(),
                stringValue(classGroup.minStudents()),
                stringValue(classGroup.maxStudents()),
                classGroup.startsAt() == null ? "" : classGroup.startsAt().toString(),
                classGroup.endsAt() == null ? "" : classGroup.endsAt().toString(),
                classGroup.shift().toDatabaseValue(),
                Boolean.toString(classGroup.showContentThumbnails())
        );
    }

    public static ClassGroupFormData from(HttpServletRequest request, Long id) {
        return new ClassGroupFormData(
                id,
                text(request, "courseId"),
                text(request, "subjectId"),
                text(request, "code"),
                text(request, "modality"),
                text(request, "state"),
                text(request, "minStudents"),
                text(request, "maxStudents"),
                text(request, "startsAt"),
                text(request, "endsAt"),
                text(request, "shift"),
                Boolean.toString("true".equalsIgnoreCase(request.getParameter("showContentThumbnails")))
        );
    }

    public Long getId() {
        return id;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getCode() {
        return code;
    }

    public String getModality() {
        return modality;
    }

    public String getState() {
        return state;
    }

    public String getMinStudents() {
        return minStudents;
    }

    public String getMaxStudents() {
        return maxStudents;
    }

    public String getStartsAt() {
        return startsAt;
    }

    public String getEndsAt() {
        return endsAt;
    }

    public String getShift() {
        return shift;
    }

    public String getShowContentThumbnails() {
        return showContentThumbnails;
    }

    public boolean isShowContentThumbnails() {
        return "true".equals(showContentThumbnails);
    }

    private static String text(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static ClassGroupState editableState(ClassGroupState state) {
        return state == ClassGroupState.DRAFT ? ClassGroupState.DRAFT : ClassGroupState.SCHEDULED;
    }
}
