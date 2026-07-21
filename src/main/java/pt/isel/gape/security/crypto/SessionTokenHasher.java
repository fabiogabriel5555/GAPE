package pt.isel.gape.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;

/**
 * One-way storage representation for bearer session tokens. Tokens are already
 * high-entropy random values, so SHA-256 prevents a database disclosure from
 * becoming an immediately usable session disclosure without requiring token
 * recovery by the application.
 */
public final class SessionTokenHasher {

    public static final String PREFIX = "sha256:";

    public String hash(String token) {
        Objects.requireNonNull(token, "token is required");
        return PREFIX + hexadecimal(sha256(token));
    }

    public boolean matches(String storedValue, String bearerToken) {
        if (storedValue == null || bearerToken == null) {
            return false;
        }
        if (isHashed(storedValue)) {
            String expected = normalizedHash(storedValue);
            String actual = hexadecimal(sha256(bearerToken));
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    actual.getBytes(StandardCharsets.US_ASCII)
            );
        }
        // Compatibility only for sessions created before the phase-17 data
        // migration. Runtime migration replaces these values at startup.
        return MessageDigest.isEqual(
                storedValue.getBytes(StandardCharsets.UTF_8),
                bearerToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    public boolean isHashed(String storedValue) {
        if (storedValue == null) {
            return false;
        }
        String normalized = normalizedHash(storedValue);
        return normalized.length() == 64 && normalized.matches("[0-9a-f]{64}");
    }

    private static String normalizedHash(String value) {
        String trimmed = value.trim();
        if (trimmed.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            return trimmed.substring(PREFIX.length()).toLowerCase(Locale.ROOT);
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String hexadecimal(byte[] bytes) {
        StringBuilder value = new StringBuilder(bytes.length * 2);
        for (byte current : bytes) {
            value.append(String.format(Locale.ROOT, "%02x", current & 0xff));
        }
        return value.toString();
    }
}
