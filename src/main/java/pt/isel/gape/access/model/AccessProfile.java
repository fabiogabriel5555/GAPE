package pt.isel.gape.access.model;

import java.util.Objects;

public record AccessProfile(AccessProfileType type, String code) {

    public AccessProfile {
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(code, "code is required");
    }
}
