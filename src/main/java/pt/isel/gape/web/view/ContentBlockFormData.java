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
    private final String state;

    private ContentBlockFormData(
            Long id,
            String classGroupId,
            String code,
            String name,
            String description,
            String orderNo,
            String state
    ) {
        this.id = id;
        this.classGroupId = classGroupId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.orderNo = orderNo;
        this.state = state == null || state.isBlank() ? "ACTIVE" : state;
    }

    public static ContentBlockFormData blank(long classGroupId, int nextOrder) {
        return new ContentBlockFormData(
                null,
                Long.toString(classGroupId),
                "",
                "",
                "",
                Integer.toString(nextOrder),
                "ACTIVE"
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
                block.state().name()
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
                text(request, "state")
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

    public String getState() {
        return state;
    }

    private static String text(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }
}
