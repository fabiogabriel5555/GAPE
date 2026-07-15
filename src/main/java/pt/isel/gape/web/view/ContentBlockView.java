package pt.isel.gape.web.view;

import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.ContentBlockState;

public final class ContentBlockView {

    private final long id;
    private final long classGroupId;
    private final String code;
    private final String name;
    private final String description;
    private final int orderNo;
    private final ContentBlockState state;

    private ContentBlockView(ContentBlock block) {
        this.id = block.id();
        this.classGroupId = block.classGroupId();
        this.code = block.code();
        this.name = block.name();
        this.description = block.description();
        this.orderNo = block.orderNo();
        this.state = block.state();
    }

    public static ContentBlockView from(ContentBlock block) {
        return new ContentBlockView(block);
    }

    public long getId() {
        return id;
    }

    public long getClassGroupId() {
        return classGroupId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description == null || description.isBlank() ? "No description." : description;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public String getState() {
        return state.name();
    }

    public String getStateLabel() {
        return switch (state) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
        };
    }

    public String getStateBadgeClass() {
        return state == ContentBlockState.ACTIVE
                ? "bg-success-50 text-success-600"
                : "bg-danger-50 text-danger-600";
    }

    public boolean isActive() {
        return state == ContentBlockState.ACTIVE;
    }

    public boolean isInactive() {
        return state == ContentBlockState.INACTIVE;
    }
}
