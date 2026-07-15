package pt.isel.gape.transversal.model;

public record MessageRecipientSummary(
        long userId,
        String name,
        String email
) {
}
