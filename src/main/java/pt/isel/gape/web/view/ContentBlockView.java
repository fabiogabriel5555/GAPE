package pt.isel.gape.web.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import pt.isel.gape.learning.model.ContentBlock;
import pt.isel.gape.learning.model.ContentBlockAccessMode;
import pt.isel.gape.learning.model.ContentBlockState;

public final class ContentBlockView {

    private static final DateTimeFormatter PORTUGUESE_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-PT"));

    private final long id;
    private final long classGroupId;
    private final String code;
    private final String name;
    private final String description;
    private final int orderNo;
    private final ContentBlockAccessMode accessMode;
    private final ContentBlockState state;
    private final LocalDateTime availableFrom;
    private final LocalDateTime availableUntil;

    private ContentBlockView(ContentBlock block) {
        this.id = block.id();
        this.classGroupId = block.classGroupId();
        this.code = block.code();
        this.name = block.name();
        this.description = block.description();
        this.orderNo = block.orderNo();
        this.accessMode = block.accessMode();
        this.state = block.state();
        this.availableFrom = block.availableFrom();
        this.availableUntil = block.availableUntil();
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

    public String getAccessMode() {
        return accessMode.name();
    }

    public String getAccessModeLabel() {
        return switch (accessMode) {
            case OPEN -> "Open";
            case RESTRICTED -> "Restricted";
            case SCHEDULED -> "Scheduled";
        };
    }

    public String getState() {
        return state.name();
    }

    public String getStateLabel() {
        return switch (state) {
            case DRAFT -> "Draft";
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
        };
    }

    public String getStateBadgeClass() {
        return switch (state) {
            case DRAFT -> "bg-neutral-30 text-neutral-600";
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-warning-30 text-warning-600";
        };
    }

    public boolean isArchived() {
        return state == ContentBlockState.INACTIVE;
    }

    public boolean isActive() {
        return state == ContentBlockState.ACTIVE;
    }

    public String getAvailableFrom() {
        return availableFrom == null ? "" : PORTUGUESE_DATE_TIME.format(availableFrom);
    }

    public String getAvailableUntil() {
        return availableUntil == null ? "" : PORTUGUESE_DATE_TIME.format(availableUntil);
    }

    public String getAvailabilityLabel() {
        if (availableFrom == null && availableUntil == null) {
            return "-";
        }
        return (availableFrom == null ? "-" : PORTUGUESE_DATE_TIME.format(availableFrom))
                + " to "
                + (availableUntil == null ? "-" : PORTUGUESE_DATE_TIME.format(availableUntil));
    }
}
