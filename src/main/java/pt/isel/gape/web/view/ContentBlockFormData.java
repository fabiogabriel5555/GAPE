package pt.isel.gape.web.view;

import jakarta.servlet.http.HttpServletRequest;
import pt.isel.gape.learning.model.ContentBlock;

public final class ContentBlockFormData {

    private final Long id;
    private final String classGroupId;
    private final String code;
    private final String name;
    private final String description;
    private final String orderNo;
    private final String accessMode;
    private final String state;
    private final String availableFrom;
    private final String availableUntil;

    private ContentBlockFormData(
            Long id,
            String classGroupId,
            String code,
            String name,
            String description,
            String orderNo,
            String accessMode,
            String state,
            String availableFrom,
            String availableUntil
    ) {
        this.id = id;
        this.classGroupId = classGroupId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.orderNo = orderNo;
        this.accessMode = accessMode == null || accessMode.isBlank() ? "OPEN" : accessMode;
        this.state = state == null || state.isBlank() ? "ACTIVE" : state;
        this.availableFrom = availableFrom;
        this.availableUntil = availableUntil;
    }

    public static ContentBlockFormData blank(long classGroupId, int nextOrder) {
        return new ContentBlockFormData(
                null,
                Long.toString(classGroupId),
                "",
                "",
                "",
                Integer.toString(nextOrder),
                "OPEN",
                "ACTIVE",
                "",
                ""
        );
    }

    public static ContentBlockFormData from(ContentBlock block) {
        return new ContentBlockFormData(
                block.id(),
                Long.toString(block.classGroupId()),
                block.code(),
                block.name(),
                block.description(),
                Integer.toString(block.orderNo()),
                block.accessMode().name(),
                block.state().name(),
                block.availableFrom() == null ? "" : block.availableFrom().toString(),
                block.availableUntil() == null ? "" : block.availableUntil().toString()
        );
    }

    public static ContentBlockFormData from(HttpServletRequest request, Long id) {
        return new ContentBlockFormData(
                id,
                text(request, "classGroupId"),
                text(request, "code"),
                text(request, "name"),
                text(request, "description"),
                text(request, "orderNo"),
                text(request, "accessMode"),
                text(request, "state"),
                text(request, "availableFrom"),
                text(request, "availableUntil")
        );
    }

    public Long getId() {
        return id;
    }

    public String getClassGroupId() {
        return classGroupId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getAccessMode() {
        return accessMode;
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
}
