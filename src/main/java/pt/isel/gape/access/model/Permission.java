package pt.isel.gape.access.model;

import java.util.Objects;

public record Permission(String code, String name, PermissionState state) {

    public Permission {
        Objects.requireNonNull(code, "code is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(state, "state is required");
    }

    public boolean isActive() {
        return state == PermissionState.ACTIVE;
    }
}
