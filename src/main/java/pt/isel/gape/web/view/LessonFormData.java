package pt.isel.gape.web.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.learning.model.Lesson;
import pt.isel.gape.learning.model.LessonState;
import pt.isel.gape.learning.model.LessonType;

public final class LessonFormData {

    private static final DateTimeFormatter INPUT_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final Long id;
    private final Long classGroupId;
    private final Long contentBlockId;
    private final String physicalRoomCode;
    private final String title;
    private final String description;
    private final String type;
    private final String provider;
    private final String accessUrl;
    private final boolean attendanceRequired;
    private final String state;
    private final String startsAt;
    private final String endsAt;

    private LessonFormData(
            Long id,
            Long classGroupId,
            Long contentBlockId,
            String physicalRoomCode,
            String title,
            String description,
            String type,
            String provider,
            String accessUrl,
            boolean attendanceRequired,
            String state,
            String startsAt,
            String endsAt
    ) {
        this.id = id;
        this.classGroupId = classGroupId;
        this.contentBlockId = contentBlockId;
        this.physicalRoomCode = physicalRoomCode;
        this.title = title;
        this.description = description;
        this.type = type;
        this.provider = provider;
        this.accessUrl = accessUrl;
        this.attendanceRequired = attendanceRequired;
        this.state = state;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public static LessonFormData blank(Long classGroupId, Long contentBlockId, String requestedType) {
        String type = normalizeType(requestedType, LessonType.ONLINE).name();
        return new LessonFormData(
                null,
                classGroupId,
                contentBlockId,
                null,
                "",
                "",
                type,
                "",
                "",
                true,
                LessonState.SCHEDULED.name(),
                "",
                ""
        );
    }

    public static LessonFormData from(Lesson lesson) {
        return new LessonFormData(
                lesson.id(),
                lesson.classGroupId(),
                lesson.contentBlockId(),
                lesson.physicalRoomCode(),
                lesson.title(),
                lesson.description(),
                lesson.type().name(),
                lesson.provider(),
                lesson.accessUrl(),
                lesson.attendanceRequired(),
                lesson.state().name(),
                INPUT_DATE_TIME.format(lesson.startsAt()),
                INPUT_DATE_TIME.format(lesson.endsAt())
        );
    }

    public static LessonFormData from(HttpServletRequest request, Long lessonId) {
        return new LessonFormData(
                lessonId,
                optionalLong(request.getParameter("classGroupId")),
                optionalLong(request.getParameter("contentBlockId")),
                normalizeText(request.getParameter("physicalRoomCode")),
                valueOrEmpty(request.getParameter("title")),
                valueOrEmpty(request.getParameter("description")),
                normalizeType(request.getParameter("type"), LessonType.ONLINE).name(),
                valueOrEmpty(request.getParameter("provider")),
                valueOrEmpty(request.getParameter("accessUrl")),
                "true".equalsIgnoreCase(request.getParameter("attendanceRequired")),
                normalizeState(request.getParameter("state"), LessonState.SCHEDULED).name(),
                valueOrEmpty(request.getParameter("startsAt")),
                valueOrEmpty(request.getParameter("endsAt"))
        );
    }

    public Long getId() {
        return id;
    }

    public Long getClassGroupId() {
        return classGroupId;
    }

    public Long getContentBlockId() {
        return contentBlockId;
    }

    public String getPhysicalRoomCode() {
        return physicalRoomCode;
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

    public String getProvider() {
        return provider;
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public boolean isAttendanceRequired() {
        return attendanceRequired;
    }

    public String getState() {
        return state;
    }

    public String getStartsAt() {
        return startsAt;
    }

    public String getEndsAt() {
        return endsAt;
    }

    public boolean isTypeSelected(String candidate) {
        return type.equalsIgnoreCase(candidate);
    }

    public boolean isStateSelected(String candidate) {
        return state.equalsIgnoreCase(candidate);
    }

    public boolean isProviderSelected(String candidate) {
        return provider != null && provider.equalsIgnoreCase(candidate);
    }

    public boolean isRoomSelected(String code) {
        return physicalRoomCode != null && physicalRoomCode.equals(code);
    }

    public boolean isBlockSelected(long blockId) {
        return contentBlockId != null && contentBlockId == blockId;
    }

    public LessonType lessonType() {
        return LessonType.valueOf(type);
    }

    public LessonState lessonState() {
        return LessonState.valueOf(state);
    }

    public LocalDateTime startsAtDateTime() {
        return LocalDateTime.parse(startsAt);
    }

    public LocalDateTime endsAtDateTime() {
        return LocalDateTime.parse(endsAt);
    }

    private static LessonType normalizeType(String value, LessonType fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim();
        for (LessonType type : LessonType.values()) {
            if (type.name().equalsIgnoreCase(normalized) || type.toDatabaseValue().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return fallback;
    }

    private static LessonState normalizeState(String value, LessonState fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim();
        for (LessonState state : LessonState.values()) {
            if (state.name().equalsIgnoreCase(normalized) || state.toDatabaseValue().equalsIgnoreCase(normalized)) {
                return state;
            }
        }
        return fallback;
    }

    private static Long optionalLong(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : Long.parseLong(normalized);
    }

    private static String valueOrEmpty(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? "" : normalized;
    }

    private static String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
