package pt.isel.gape.learning.model;

public record ContentItemCreateCommand(
        String title,
        String description,
        ContentFormat format,
        String source,
        ContentItemState state
) {
}
