package pt.isel.gape.access.model;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class User {

    private final long id;
    private final String name;
    private final String email;
    private final UserState state;
    private final String language;
    private final String photo;
    private final LocalDateTime createdAt;
    private final String credentialHash;
    private final String credentialSalt;
    private final String documentType;
    private final String documentNumber;
    private final Set<AccessProfile> accessProfiles;

    public User(
            long id,
            String name,
            String email,
            UserState state,
            String language,
            String photo,
            LocalDateTime createdAt,
            String credentialHash,
            String credentialSalt,
            String documentType,
            String documentNumber,
            Set<AccessProfile> accessProfiles
    ) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name is required");
        this.email = Objects.requireNonNull(email, "email is required");
        this.state = Objects.requireNonNull(state, "state is required");
        this.language = Objects.requireNonNull(language, "language is required");
        this.photo = photo;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.credentialHash = Objects.requireNonNull(credentialHash, "credentialHash is required");
        this.credentialSalt = Objects.requireNonNull(credentialSalt, "credentialSalt is required");
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.accessProfiles = Set.copyOf(new LinkedHashSet<>(Objects.requireNonNull(accessProfiles, "accessProfiles are required")));
    }

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public UserState state() {
        return state;
    }

    public String language() {
        return language;
    }

    public String photo() {
        return photo;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public String credentialHash() {
        return credentialHash;
    }

    public String credentialSalt() {
        return credentialSalt;
    }

    public String documentType() {
        return documentType;
    }

    public String documentNumber() {
        return documentNumber;
    }

    public Set<AccessProfile> accessProfiles() {
        return accessProfiles;
    }

    public boolean isActive() {
        return state == UserState.ACTIVE;
    }
}
