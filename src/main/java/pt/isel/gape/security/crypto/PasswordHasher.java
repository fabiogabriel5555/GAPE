package pt.isel.gape.security.crypto;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public PasswordHash createHash(String password) {
        Objects.requireNonNull(password, "password is required");
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        return new PasswordHash(hashPassword(password, saltBase64), saltBase64);
    }

    public boolean matches(String password, String expectedHashBase64, String saltBase64) {
        Objects.requireNonNull(password, "password is required");
        Objects.requireNonNull(expectedHashBase64, "expectedHashBase64 is required");
        Objects.requireNonNull(saltBase64, "saltBase64 is required");

        byte[] expected = Base64.getDecoder().decode(expectedHashBase64);
        byte[] actual = Base64.getDecoder().decode(hashPassword(password, saltBase64));
        return MessageDigest.isEqual(expected, actual);
    }

    public String hashPassword(String password, String saltBase64) {
        Objects.requireNonNull(password, "password is required");
        Objects.requireNonNull(saltBase64, "saltBase64 is required");

        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] encoded = secretKeyFactory.generateSecret(keySpec).getEncoded();
            return Base64.getEncoder().encodeToString(encoded);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to hash password", exception);
        }
    }

    public record PasswordHash(String hashBase64, String saltBase64) {
    }
}
