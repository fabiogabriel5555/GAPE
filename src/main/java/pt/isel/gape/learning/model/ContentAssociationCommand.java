package pt.isel.gape.learning.model;

public record ContentAssociationCommand(
        ContentAssociationType type,
        long targetId,
        String role,
        Integer orderNo,
        boolean mandatory
) {
}
