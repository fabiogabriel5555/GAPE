package pt.isel.gape.access.model;

public record UserPersonalProfileUpdate(
        String name,
        String email,
        String language,
        String photo,
        String documentType,
        String documentNumber
) {
}
