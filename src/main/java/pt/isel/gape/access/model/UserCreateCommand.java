package pt.isel.gape.access.model;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record UserCreateCommand(
        String name,
        String email,
        UserState state,
        String language,
        String photo,
        String credentialHash,
        String credentialSalt,
        String documentType,
        String documentNumber,
        Set<AccessProfile> accessProfiles
) {

    public UserCreateCommand {
        Objects.requireNonNull(accessProfiles, "accessProfiles are required");
        accessProfiles = Set.copyOf(new LinkedHashSet<>(accessProfiles));
    }
}
