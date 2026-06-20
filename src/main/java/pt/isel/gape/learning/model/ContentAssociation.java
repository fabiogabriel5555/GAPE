package pt.isel.gape.learning.model;

public record ContentAssociation(
        ContentAssociationType type,
        long targetId,
        long contentItemId,
        String role,
        Integer orderNo,
        boolean mandatory
) {
}
