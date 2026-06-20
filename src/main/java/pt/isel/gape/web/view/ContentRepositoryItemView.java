package pt.isel.gape.web.view;

import pt.isel.gape.learning.model.ContentFormat;
import pt.isel.gape.learning.model.ReusableContentFile;

public final class ContentRepositoryItemView {

    private final long id;
    private final String title;
    private final String description;
    private final ContentFormat format;
    private final boolean defaultMandatory;

    private ContentRepositoryItemView(ReusableContentFile reusableContentFile) {
        this.id = reusableContentFile.repositoryContentItemId();
        this.title = reusableContentFile.title();
        this.description = reusableContentFile.description();
        this.format = reusableContentFile.format();
        this.defaultMandatory = reusableContentFile.defaultMandatory();
    }

    public static ContentRepositoryItemView from(ReusableContentFile reusableContentFile) {
        return new ContentRepositoryItemView(reusableContentFile);
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

    public String getRawTitle() {
        return title == null ? "" : title;
    }

    public String getRawDescription() {
        return description == null ? "" : description;
    }

    public String getRawTitleAttribute() {
        return htmlAttribute(getRawTitle());
    }

    public String getRawDescriptionAttribute() {
        return htmlAttribute(getRawDescription());
    }

    public boolean isDefaultMandatory() {
        return defaultMandatory;
    }

    public String getFormat() {
        return format.toDatabaseValue();
    }

    public String getFormatLabel() {
        return switch (format) {
            case PDF -> "PDF";
            case TEXT -> "Text";
            case IMAGE -> "Image";
            case VIDEO -> "Video";
            case AUDIO -> "Audio";
            default -> "Content";
        };
    }

    public String getFormatIconClass() {
        return switch (format) {
            case PDF -> "ph ph-file-pdf";
            case TEXT -> "ph ph-text-aa";
            case IMAGE -> "ph ph-image";
            case VIDEO -> "ph ph-video";
            case AUDIO -> "ph ph-speaker-high";
            default -> "ph ph-file";
        };
    }

    public String getFormatBadgeClass() {
        return switch (format) {
            case PDF -> "bg-danger-600";
            case TEXT -> "bg-main-two-600";
            case IMAGE -> "bg-success-600";
            case VIDEO -> "bg-main-600";
            case AUDIO -> "bg-main-600";
            default -> "bg-neutral-600";
        };
    }

    public String getDefaultRole() {
        return switch (format) {
            case PDF -> "support_material";
            case TEXT -> "text";
            case IMAGE -> "image";
            case VIDEO -> "video";
            case AUDIO -> "audio";
            default -> "support_material";
        };
    }

    private static String htmlAttribute(String value) {
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
