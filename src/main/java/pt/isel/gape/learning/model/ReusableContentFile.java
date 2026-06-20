package pt.isel.gape.learning.model;

public record ReusableContentFile(
        long repositoryContentItemId,
        String title,
        String description,
        ContentFormat format,
        String source,
        boolean defaultMandatory
) {

    public ReusableContentFile {
        if (repositoryContentItemId <= 0) {
            throw new IllegalArgumentException("repository content item is required");
        }
        if (format == null) {
            throw new IllegalArgumentException("file format is required");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("file source is required");
        }
        title = title == null ? "" : title.trim();
        description = description == null ? "" : description.trim();
        source = source.trim();
    }
}
