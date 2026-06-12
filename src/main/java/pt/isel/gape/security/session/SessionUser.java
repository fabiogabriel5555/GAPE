package pt.isel.gape.security.session;

import java.io.Serial;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import pt.isel.gape.access.model.AccessProfile;
import pt.isel.gape.access.model.AccessProfileType;
import pt.isel.gape.access.model.User;
import pt.isel.gape.security.authorization.AuthorizationPolicy;

public final class SessionUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long userId;
    private final String name;
    private final String email;
    private final String photo;
    private final Set<AccessProfileType> profileTypes;
    private final Map<AccessProfileType, Set<String>> permissionCodesByProfile;
    private final Set<String> permissionCodes;

    public SessionUser(long userId, String name, String email, String photo, Set<AccessProfileType> profileTypes) {
        this(userId, name, email, photo, profileTypes, Map.of());
    }

    public SessionUser(
            long userId,
            String name,
            String email,
            String photo,
            Set<AccessProfileType> profileTypes,
            Map<AccessProfileType, Set<String>> permissionCodesByProfile
    ) {
        this.userId = userId;
        this.name = Objects.requireNonNull(name, "name is required");
        this.email = Objects.requireNonNull(email, "email is required");
        this.photo = normalizePhoto(photo);
        this.profileTypes = Set.copyOf(new LinkedHashSet<>(Objects.requireNonNull(profileTypes, "profileTypes are required")));
        this.permissionCodesByProfile = copyPermissionCodes(permissionCodesByProfile);
        this.permissionCodes = flattenPermissionCodes(this.permissionCodesByProfile);
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

    public Set<String> permissionCodes() {
        return permissionCodes;
    }

    public Set<String> permissionCodesFor(AccessProfileType profileType) {
        return permissionCodesByProfile.getOrDefault(profileType, Set.of());
    }

    public boolean hasPermission(String permissionCode) {
        if (permissionCode == null) {
            return false;
        }
        if (permissionCodes.contains(permissionCode)) {
            return true;
        }
        String canonicalPermissionCode = AuthorizationPolicy.canonicalAdminPermission(permissionCode);
        return !canonicalPermissionCode.equals(permissionCode) && permissionCodes.contains(canonicalPermissionCode);
    }

    public Optional<AccessProfileType> primaryProfileType() {
        if (profileTypes.contains(AccessProfileType.ADMINISTRATOR)) {
            return Optional.of(AccessProfileType.ADMINISTRATOR);
        }
        if (profileTypes.contains(AccessProfileType.COORDINATOR)) {
            return Optional.of(AccessProfileType.COORDINATOR);
        }
        if (profileTypes.contains(AccessProfileType.TEACHER)) {
            return Optional.of(AccessProfileType.TEACHER);
        }
        if (profileTypes.contains(AccessProfileType.STUDENT)) {
            return Optional.of(AccessProfileType.STUDENT);
        }
        return Optional.empty();
    }

    private static String normalizePhoto(String photo) {
        if (photo == null || photo.isBlank()) {
            return null;
        }
        String normalized = photo.trim().replace('\\', '/');
        normalized = stripAfter(normalized, '?');
        normalized = stripAfter(normalized, '#');

        String lowerCasePhoto = normalized.toLowerCase(Locale.ROOT);
        if (lowerCasePhoto.startsWith("http://")
                || lowerCasePhoto.startsWith("https://")
                || lowerCasePhoto.startsWith("data:")) {
            return null;
        }

        normalized = stripBeforeKnownDirectory(normalized, "/media/");
        normalized = stripToKnownDirectory(normalized, "assets");
        normalized = stripBeforeKnownDirectory(normalized, "/uploads/");
        normalized = stripLeadingPathMarkers(normalized);
        normalized = stripKnownPrefix(normalized, "media/");
        normalized = stripKnownPrefix(normalized, "uploads/");
        normalized = stripToKnownDirectory(normalized, "assets");
        normalized = stripLeadingPathMarkers(normalized);

        if (normalized.isBlank() || normalized.contains("..")) {
            return null;
        }
        return normalized;
    }

    private static String stripAfter(String value, char marker) {
        int markerIndex = value.indexOf(marker);
        return markerIndex < 0 ? value : value.substring(0, markerIndex);
    }

    private static String stripBeforeKnownDirectory(String value, String directory) {
        String lowerCaseValue = value.toLowerCase(Locale.ROOT);
        int directoryIndex = lowerCaseValue.indexOf(directory);
        if (directoryIndex < 0) {
            return value;
        }
        return value.substring(directoryIndex + directory.length());
    }

    private static String stripToKnownDirectory(String value, String directory) {
        String marker = "/" + directory + "/";
        String lowerCaseValue = value.toLowerCase(Locale.ROOT);
        int directoryIndex = lowerCaseValue.indexOf(marker);
        if (directoryIndex < 0) {
            return value;
        }
        return value.substring(directoryIndex + 1);
    }

    private static String stripLeadingPathMarkers(String value) {
        String normalized = value;
        while (normalized.startsWith("/") || normalized.startsWith("./")) {
            normalized = normalized.startsWith("/") ? normalized.substring(1) : normalized.substring(2);
        }
        return normalized;
    }

    private static String stripKnownPrefix(String value, String prefix) {
        String normalized = value;
        while (normalized.toLowerCase(Locale.ROOT).startsWith(prefix)) {
            normalized = normalized.substring(prefix.length());
        }
        return normalized;
    }

    private static Map<AccessProfileType, Set<String>> copyPermissionCodes(
            Map<AccessProfileType, Set<String>> permissionCodesByProfile
    ) {
        Objects.requireNonNull(permissionCodesByProfile, "permissionCodesByProfile is required");
        Map<AccessProfileType, Set<String>> copy = new EnumMap<>(AccessProfileType.class);
        for (Map.Entry<AccessProfileType, Set<String>> entry : permissionCodesByProfile.entrySet()) {
            copy.put(entry.getKey(), Set.copyOf(new TreeSet<>(entry.getValue())));
        }
        return Map.copyOf(copy);
    }

    private static Set<String> flattenPermissionCodes(Map<AccessProfileType, Set<String>> permissionCodesByProfile) {
        Set<String> permissions = new TreeSet<>();
        for (Set<String> profilePermissions : permissionCodesByProfile.values()) {
            permissions.addAll(profilePermissions);
        }
        return Set.copyOf(permissions);
    }
}
