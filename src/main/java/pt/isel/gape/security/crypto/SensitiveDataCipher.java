package pt.isel.gape.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Application-level protection for recoverable sensitive values persisted in
 * the database. Password hashes deliberately do not use this class: they are
 * one-way PBKDF2 values and must remain so.
 *
 * <p>The production key is supplied exclusively through the
 * {@value #KEY_ENVIRONMENT_VARIABLE} environment variable as Base64-encoded
 * 32-byte material. A development process without a key keeps its explicit
 * compatibility mode so disposable local data and the database test suite do
 * not silently acquire an unrecoverable process-local key. Any environment
 * other than development fails closed when the key is missing.</p>
 */
public final class SensitiveDataCipher {

    public static final String KEY_ENVIRONMENT_VARIABLE = "GAPE_SENSITIVE_DATA_KEY";
    public static final String ENVIRONMENT_ENVIRONMENT_VARIABLE = "GAPE_SECURITY_ENVIRONMENT";
    public static final String ENVELOPE_PREFIX = "gape:v1:";

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MASTER_KEY_BYTES = 32;
    private static final int GCM_NONCE_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final byte[] encryptionKey;
    private final byte[] fingerprintKey;
    private final boolean developmentCompatibilityMode;

    private SensitiveDataCipher(byte[] masterKey, boolean developmentCompatibilityMode) {
        this.developmentCompatibilityMode = developmentCompatibilityMode;
        if (developmentCompatibilityMode) {
            this.encryptionKey = null;
            this.fingerprintKey = null;
            return;
        }

        byte[] validatedKey = validateMasterKey(masterKey);
        this.encryptionKey = deriveKey(validatedKey, "GAPE:sensitive-data:aes-gcm:v1");
        this.fingerprintKey = deriveKey(validatedKey, "GAPE:sensitive-data:fingerprint:v1");
        Arrays.fill(validatedKey, (byte) 0);
    }

    /**
     * Builds a cipher from a Base64 (standard or URL-safe) 32-byte key. This
     * constructor is intentionally useful for isolated tests and offline
     * migration tooling; deployed applications use {@link #fromRuntimeConfiguration()}.
     */
    public static SensitiveDataCipher fromBase64Key(String encodedKey) {
        Objects.requireNonNull(encodedKey, "encodedKey is required");
        byte[] decodedKey = decodeMasterKey(encodedKey);
        try {
            return new SensitiveDataCipher(decodedKey, false);
        } finally {
            Arrays.fill(decodedKey, (byte) 0);
        }
    }

    /**
     * Resolves the key only from {@value #KEY_ENVIRONMENT_VARIABLE}. The
     * default environment is development; all other environments reject a
     * missing key rather than storing sensitive values in plaintext.
     */
    public static SensitiveDataCipher fromRuntimeConfiguration() {
        return RuntimeConfigurationHolder.CIPHER;
    }

    /**
     * Resolves the same policy as the runtime configuration from supplied
     * values. It exists for deterministic tests and offline administration;
     * deployed code must use {@link #fromRuntimeConfiguration()}, which reads
     * key material exclusively from {@value #KEY_ENVIRONMENT_VARIABLE}.
     */
    public static SensitiveDataCipher fromConfiguration(String securityEnvironment, String encodedKey) {
        String normalizedEnvironment = securityEnvironment == null || securityEnvironment.isBlank()
                ? "development"
                : securityEnvironment.trim().toLowerCase(Locale.ROOT);
        if (encodedKey != null && !encodedKey.isBlank()) {
            return fromBase64Key(encodedKey);
        }
        if ("development".equals(normalizedEnvironment) || "dev".equals(normalizedEnvironment)) {
            return new SensitiveDataCipher(null, true);
        }
        throw new IllegalStateException(KEY_ENVIRONMENT_VARIABLE
                + " is required when " + ENVIRONMENT_ENVIRONMENT_VARIABLE
                + " is not development");
    }

    /**
     * Forces eager validation at application start, so production
     * misconfiguration is detected before protected data is read or written.
     */
    public static void validateRuntimeConfiguration() {
        fromRuntimeConfiguration();
    }

    public boolean isEnabled() {
        return !developmentCompatibilityMode;
    }

    public String encrypt(String cleartext, String purpose) {
        Objects.requireNonNull(cleartext, "cleartext is required");
        validatePurpose(purpose);
        if (!isEnabled()) {
            return cleartext;
        }

        byte[] nonce = new byte[GCM_NONCE_BYTES];
        SECURE_RANDOM.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, nonce));
            cipher.updateAAD(purpose.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(cleartext.getBytes(StandardCharsets.UTF_8));
            byte[] envelope = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, envelope, 0, nonce.length);
            System.arraycopy(encrypted, 0, envelope, nonce.length, encrypted.length);
            return ENVELOPE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(envelope);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to encrypt sensitive data", exception);
        }
    }

    public String encryptNullable(String cleartext, String purpose) {
        return cleartext == null ? null : encrypt(cleartext, purpose);
    }

    public String decrypt(String protectedValue, String purpose) {
        Objects.requireNonNull(protectedValue, "protectedValue is required");
        validatePurpose(purpose);
        if (!isEncrypted(protectedValue)) {
            if (developmentCompatibilityMode) {
                return protectedValue;
            }
            throw new IllegalStateException("Sensitive data is not protected by a supported envelope");
        }
        if (!isEnabled()) {
            throw new IllegalStateException("Encrypted sensitive data requires " + KEY_ENVIRONMENT_VARIABLE);
        }

        try {
            byte[] envelope = Base64.getUrlDecoder().decode(protectedValue.substring(ENVELOPE_PREFIX.length()));
            if (envelope.length <= GCM_NONCE_BYTES) {
                throw new IllegalStateException("Sensitive data envelope is incomplete");
            }
            byte[] nonce = Arrays.copyOfRange(envelope, 0, GCM_NONCE_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(envelope, GCM_NONCE_BYTES, envelope.length);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, nonce));
            cipher.updateAAD(purpose.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Sensitive data envelope is invalid", exception);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Sensitive data could not be authenticated", exception);
        }
    }

    public String decryptNullable(String protectedValue, String purpose) {
        return protectedValue == null ? null : decrypt(protectedValue, purpose);
    }

    /**
     * Returns a domain-separated HMAC-SHA-256 fingerprint for equality and
     * uniqueness checks. It never replaces the encrypted database value.
     */
    public String fingerprint(String cleartext, String purpose) {
        Objects.requireNonNull(cleartext, "cleartext is required");
        validatePurpose(purpose);
        if (!isEnabled()) {
            throw new IllegalStateException("Sensitive-data fingerprints require " + KEY_ENVIRONMENT_VARIABLE);
        }

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(fingerprintKey, HMAC_ALGORITHM));
            mac.update(purpose.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) 0);
            byte[] fingerprint = mac.doFinal(cleartext.getBytes(StandardCharsets.UTF_8));
            return toHex(fingerprint);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to fingerprint sensitive data", exception);
        }
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENVELOPE_PREFIX);
    }

    private static void validatePurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("Sensitive-data purpose is required");
        }
    }

    private static byte[] decodeMasterKey(String encodedKey) {
        String normalized = encodedKey.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Sensitive-data key must not be empty");
        }
        try {
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException standardException) {
            try {
                return Base64.getUrlDecoder().decode(normalized);
            } catch (IllegalArgumentException urlException) {
                urlException.addSuppressed(standardException);
                throw new IllegalArgumentException("Sensitive-data key must be Base64", urlException);
            }
        }
    }

    private static byte[] validateMasterKey(byte[] masterKey) {
        Objects.requireNonNull(masterKey, "masterKey is required");
        if (masterKey.length != MASTER_KEY_BYTES) {
            throw new IllegalArgumentException("Sensitive-data key must decode to exactly 32 bytes");
        }
        return Arrays.copyOf(masterKey, masterKey.length);
    }

    private static byte[] deriveKey(byte[] masterKey, String label) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(masterKey, HMAC_ALGORITHM));
            return Arrays.copyOf(mac.doFinal(label.getBytes(StandardCharsets.UTF_8)), MASTER_KEY_BYTES);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to derive sensitive-data key", exception);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hexadecimal = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            hexadecimal.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        }
        return hexadecimal.toString();
    }

    private static final class RuntimeConfigurationHolder {
        private static final SensitiveDataCipher CIPHER = fromEnvironment();

        private RuntimeConfigurationHolder() {
        }

        private static SensitiveDataCipher fromEnvironment() {
            String environment = System.getenv(ENVIRONMENT_ENVIRONMENT_VARIABLE);
            String configuredKey = System.getenv(KEY_ENVIRONMENT_VARIABLE);
            return fromConfiguration(environment, configuredKey);
        }
    }
}
