package pt.isel.gape.access.model;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record UserUpdateCommand(
        String name,
        String email,
        UserState state,
        String language,
        String photo,
        String documentType,
        String documentNumber,
        Set<AccessProfile> accessProfiles
) {

    public UserUpdateCommand {
        Objects.requireNonNull(accessProfiles, "accessProfiles are required");
        accessProfiles = Set.copyOf(new LinkedHashSet<>(accessProfiles));
    }
}
