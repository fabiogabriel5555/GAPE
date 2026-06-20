package pt.isel.gape.learning.model;

public record BlockContentItem(
        ContentItem contentItem,
        String role,
        Integer orderNo,
        boolean mandatory,
        String thumbnailPath
) {
}
