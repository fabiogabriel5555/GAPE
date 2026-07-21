package pt.isel.gape.security.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class SensitiveDataCipherTest {

    private static final String PRIMARY_KEY = encodedKey("0123456789abcdef0123456789abcdef");
    private static final String SECONDARY_KEY = encodedKey("fedcba9876543210fedcba9876543210");
    private static final String DOCUMENT_PURPOSE = "user_account.document_number";

    @Test
    void encryptsAndDecryptsWithAUniqueVersionedAesGcmEnvelope() {
        SensitiveDataCipher cipher = SensitiveDataCipher.fromBase64Key(PRIMARY_KEY);
        String cleartext = "PT-12345678";

        String firstEnvelope = cipher.encrypt(cleartext, DOCUMENT_PURPOSE);
        String secondEnvelope = cipher.encrypt(cleartext, DOCUMENT_PURPOSE);

        assertTrue(cipher.isEnabled());
        assertTrue(SensitiveDataCipher.isEncrypted(firstEnvelope));
        assertTrue(firstEnvelope.startsWith(SensitiveDataCipher.ENVELOPE_PREFIX));
        assertNotEquals(cleartext, firstEnvelope);
        assertNotEquals(firstEnvelope, secondEnvelope);
        assertEquals(cleartext, cipher.decrypt(firstEnvelope, DOCUMENT_PURPOSE));
        assertNull(cipher.encryptNullable(null, DOCUMENT_PURPOSE));
        assertNull(cipher.decryptNullable(null, DOCUMENT_PURPOSE));
        assertFalse(SensitiveDataCipher.isEncrypted(cleartext));
        assertFalse(SensitiveDataCipher.isEncrypted(null));
    }

    @Test
    void rejectsTamperingWrongPurposeAndWrongKeys() {
        SensitiveDataCipher cipher = SensitiveDataCipher.fromBase64Key(PRIMARY_KEY);
        String envelope = cipher.encrypt("absence reason", "absence_justification.reason");
        String tamperedEnvelope = tamper(envelope);

        assertThrows(
                IllegalStateException.class,
                () -> cipher.decrypt(tamperedEnvelope, "absence_justification.reason")
        );
        assertThrows(
                IllegalStateException.class,
                () -> cipher.decrypt(envelope, "absence_justification.decision_notes")
        );
        assertThrows(
                IllegalStateException.class,
                () -> SensitiveDataCipher.fromBase64Key(SECONDARY_KEY)
                        .decrypt(envelope, "absence_justification.reason")
        );
    }

    @Test
    void fingerprintsAreStablePurposeBoundAndSeparateFromTheCiphertext() {
        SensitiveDataCipher cipher = SensitiveDataCipher.fromBase64Key(PRIMARY_KEY);
        String cleartext = "PT-12345678";

        String documentFingerprint = cipher.fingerprint(cleartext, DOCUMENT_PURPOSE);
        String repeatedFingerprint = cipher.fingerprint(cleartext, DOCUMENT_PURPOSE);
        String otherPurposeFingerprint = cipher.fingerprint(cleartext, "other.document_number");
        String ciphertext = cipher.encrypt(cleartext, DOCUMENT_PURPOSE);

        assertEquals(documentFingerprint, repeatedFingerprint);
        assertNotEquals(documentFingerprint, otherPurposeFingerprint);
        assertNotEquals(documentFingerprint, ciphertext);
        assertTrue(documentFingerprint.matches("[0-9a-f]{64}"));
    }

    @Test
    void rejectsInvalidKeysAndFailsClosedOutsideDevelopment() {
        assertThrows(IllegalArgumentException.class, () -> SensitiveDataCipher.fromBase64Key("not Base64"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SensitiveDataCipher.fromBase64Key(Base64.getEncoder().encodeToString(new byte[31]))
        );
        assertThrows(
                IllegalStateException.class,
                () -> SensitiveDataCipher.fromConfiguration("production", null)
        );

        SensitiveDataCipher developmentCipher = SensitiveDataCipher.fromConfiguration("development", null);
        assertFalse(developmentCipher.isEnabled());
        assertEquals("local-only", developmentCipher.encrypt("local-only", DOCUMENT_PURPOSE));
        assertEquals("local-only", developmentCipher.decrypt("local-only", DOCUMENT_PURPOSE));
        assertThrows(
                IllegalStateException.class,
                () -> developmentCipher.fingerprint("local-only", DOCUMENT_PURPOSE)
        );
    }

    private static String encodedKey(String rawKey) {
        return Base64.getEncoder().encodeToString(rawKey.getBytes(StandardCharsets.UTF_8));
    }

    private static String tamper(String envelope) {
        int index = SensitiveDataCipher.ENVELOPE_PREFIX.length() + 5;
        char original = envelope.charAt(index);
        char replacement = original == 'A' ? 'B' : 'A';
        return envelope.substring(0, index) + replacement + envelope.substring(index + 1);
    }
}
