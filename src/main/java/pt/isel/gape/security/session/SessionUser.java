package pt.isel.gape.security.session;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import pt.isel.gape.access.model.AccessProfile;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;

public final class SessionUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long userId;
    private final String name;
    private final String email;
    private final String photo;
    private final Set<AccessProfileType> profileTypes;

    public SessionUser(long userId, String name, String email, String photo, Set<AccessProfileType> profileTypes) {
        this.userId = userId;
        this.name = Objects.requireNonNull(name, "name is required");
        this.email = Objects.requireNonNull(email, "email is required");
        this.photo = normalizePhoto(photo);
        this.profileTypes = Set.copyOf(new LinkedHashSet<>(Objects.requireNonNull(profileTypes, "profileTypes are required")));
    }

    public static SessionUser fromUser(User user) {
        Set<AccessProfileType> profileTypes = user.accessProfiles()
                .stream()
                .map(AccessProfile::type)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return new SessionUser(user.id(), user.name(), user.email(), user.photo(), profileTypes);
    }

    public long userId() {
        return userId;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public String photo() {
        return photo;
    }

    public boolean hasPhoto() {
        return photo != null && !photo.isBlank();
    }

    public Set<AccessProfileType> profileTypes() {
        return profileTypes;
    }

    private static String normalizePhoto(String photo) {
        if (photo == null || photo.isBlank()) {
            return null;
        }
        return photo.trim().replace('\\', '/');
    }
}
