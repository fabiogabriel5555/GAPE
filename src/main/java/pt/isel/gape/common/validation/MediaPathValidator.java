package pt.isel.gape.common.validation;

import java.util.Optional;
import java.util.regex.Pattern;

public final class MediaPathValidator {

    private static final Pattern SAFE_RELATIVE_PATH = Pattern.compile("[A-Za-z0-9._-]+(/[A-Za-z0-9._-]+)*");

    private MediaPathValidator() {
    }

    public static String optionalSafeRelativePath(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (!isSafeRelativePath(normalized)) {
            throw new IllegalArgumentException(fieldLabel + " contains an invalid media path");
        }
        return normalized;
    }

    public static String optionalEntityProfilePath(String value, String entityDirectory, long entityId, String fieldLabel) {
        String normalized = optionalSafeRelativePath(value, fieldLabel);
        if (normalized == null) {
            return null;
        }
        String expectedPath = entityDirectory + "/" + entityId + "/profile.webp";
        if (!expectedPath.equals(normalized)) {
            throw new IllegalArgumentException(fieldLabel + " must reference the uploaded profile image");
        }
        return normalized;
    }

    public static Optional<String> safeRelativePath(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim();
        return isSafeRelativePath(normalized) ? Optional.of(normalized) : Optional.empty();
    }

    private static boolean isSafeRelativePath(String value) {
        return SAFE_RELATIVE_PATH.matcher(value).matches()
                && !value.contains("..")
                && !value.contains("\\")
                && !value.contains(":");
    }
}
