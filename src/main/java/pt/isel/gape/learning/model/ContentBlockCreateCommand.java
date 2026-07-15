package pt.isel.gape.learning.model;

public record ContentBlockCreateCommand(
        long classGroupId,
        String code,
        String name,
        String description,
        int orderNo,
        ContentBlockState state
) {
}
