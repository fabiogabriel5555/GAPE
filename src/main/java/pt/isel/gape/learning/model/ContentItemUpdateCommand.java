package pt.isel.gape.learning.model;

public record ContentItemUpdateCommand(
        String title,
        String description,
        ContentFormat format,
        String source,
        ContentItemState state
) {
}
