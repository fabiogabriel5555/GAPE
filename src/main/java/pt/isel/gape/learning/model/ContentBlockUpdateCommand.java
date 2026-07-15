package pt.isel.gape.learning.model;

public record ContentBlockUpdateCommand(
        long classGroupId,
        String code,
        String name,
        String description,
        int orderNo,
        ContentBlockState state
) {
}
