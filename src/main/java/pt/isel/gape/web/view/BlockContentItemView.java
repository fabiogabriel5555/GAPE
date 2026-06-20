package pt.isel.gape.web.view;

import pt.isel.gape.learning.model.BlockContentItem;
import pt.isel.gape.learning.model.ContentFormat;
import pt.isel.gape.learning.model.ContentItem;
import pt.isel.gape.learning.model.ContentItemState;

public final class BlockContentItemView {

    private final long id;
    private final String title;
    private final String description;
    private final ContentFormat format;
    private final String source;
    private final ContentItemState state;
    private final String role;
    private final Integer orderNo;
    private final boolean mandatory;
    private final String thumbnailPath;

    private BlockContentItemView(BlockContentItem blockContentItem) {
        ContentItem contentItem = blockContentItem.contentItem();
        this.id = contentItem.id();
        this.title = contentItem.title();
        this.description = contentItem.description();
        this.format = contentItem.format();
        this.source = contentItem.source();
        this.state = contentItem.state();
        this.role = blockContentItem.role();
        this.orderNo = blockContentItem.orderNo();
        this.mandatory = blockContentItem.mandatory();
        this.thumbnailPath = blockContentItem.thumbnailPath();
    }

    public static BlockContentItemView from(BlockContentItem blockContentItem) {
        return new BlockContentItemView(blockContentItem);
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title == null || title.isBlank() ? "Untitled content" : title;
    }

    public String getDescription() {
        return description == null || description.isBlank() ? "No description." : description;
    }

    public String getFormat() {
        return format.name();
    }

    public String getFormatLabel() {
        return switch (format) {
            case TEXT -> "Text";
            case IMAGE -> "Image";
            case VIDEO -> "Video";
            case AUDIO -> "Audio";
            case PDF -> "PDF";
            case URL -> "Link";
            case SCORM -> "SCORM";
            case XAPI -> "xAPI";
            case PRESENTATION -> "Presentation";
            case EMBED -> "Embed";
            case OTHER -> "Other";
        };
    }

    public String getFormatIconClass() {
        return switch (format) {
            case TEXT -> "ph ph-text-aa";
            case IMAGE -> "ph ph-image";
            case VIDEO -> "ph ph-video";
            case AUDIO -> "ph ph-speaker-high";
            case PDF -> "ph ph-file-pdf";
            case URL -> "ph ph-link";
            case SCORM, XAPI -> "ph ph-package";
            case PRESENTATION -> "ph ph-file-ppt";
            case EMBED -> "ph ph-code";
            case OTHER -> "ph ph-file";
        };
    }

    public String getFormatBadgeClass() {
        return switch (format) {
            case PDF -> "bg-danger-50 text-danger-600";
            case URL, EMBED -> "bg-main-50 text-main-600";
            case IMAGE, VIDEO, AUDIO, PRESENTATION -> "bg-success-50 text-success-600";
            case SCORM, XAPI -> "bg-warning-30 text-warning-600";
            case TEXT, OTHER -> "bg-neutral-30 text-neutral-600";
        };
    }

    public boolean isPdf() {
        return format == ContentFormat.PDF;
    }

    public boolean isDownloadable() {
        return format == ContentFormat.PDF
                || format == ContentFormat.TEXT
                || format == ContentFormat.IMAGE
                || format == ContentFormat.VIDEO
                || format == ContentFormat.AUDIO;
    }

    public boolean isVisualThumbnailAvailable() {
        return format == ContentFormat.IMAGE || (thumbnailPath != null && !thumbnailPath.isBlank());
    }

    public boolean isUrl() {
        return format == ContentFormat.URL || format == ContentFormat.EMBED;
    }

    public boolean isLinkable() {
        String normalized = getSource().trim().toLowerCase(java.util.Locale.ROOT);
        return isUrl() && (normalized.startsWith("http://") || normalized.startsWith("https://"));
    }

    public String getSource() {
        return source == null ? "" : source;
    }

    public String getState() {
        return state.name();
    }

    public String getStateLabel() {
        return switch (state) {
            case DRAFT -> "Draft";
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case ARCHIVED -> "Archived";
        };
    }

    public String getStateBadgeClass() {
        return switch (state) {
            case DRAFT -> "bg-neutral-30 text-neutral-600";
            case ACTIVE -> "bg-success-50 text-success-600";
            case INACTIVE -> "bg-warning-30 text-warning-600";
            case ARCHIVED -> "bg-danger-50 text-danger-600";
        };
    }

    public boolean isActive() {
        return state == ContentItemState.ACTIVE;
    }

    public boolean isArchived() {
        return state == ContentItemState.ARCHIVED;
    }

    public String getRoleLabel() {
        return role == null || role.isBlank() ? "Content" : role.trim();
    }

    public String getOrderLabel() {
        return orderNo == null ? "-" : orderNo.toString();
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public String getMandatoryLabel() {
        return mandatory ? "Mandatory" : "Optional";
    }

    public String getMandatoryBadgeClass() {
        return mandatory ? "bg-main-50 text-main-600" : "bg-neutral-30 text-neutral-600";
    }
}
