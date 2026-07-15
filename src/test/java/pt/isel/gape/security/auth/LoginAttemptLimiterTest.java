package pt.isel.gape.security.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-10T12:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void blocksIdentityAndIpCombinationAfterConfiguredFailures() {
        LoginAttemptLimiter limiter = limiter(3, 20);

        limiter.recordFailure("user@gape.local", "192.0.2.10");
        limiter.recordFailure("user@gape.local", "192.0.2.10");
        assertDoesNotThrow(() -> limiter.requireAllowed("user@gape.local", "192.0.2.10"));

        limiter.recordFailure("user@gape.local", "192.0.2.10");
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> limiter.requireAllowed("USER@gape.local", "192.0.2.10")
        );

        assertEquals(AuthenticationFailureReason.TOO_MANY_ATTEMPTS, exception.reason());
    }

    @Test
    void successfulAuthenticationResetsOnlyTheIdentityAndIpCombination() {
        LoginAttemptLimiter limiter = limiter(3, 20);

        limiter.recordFailure("user@gape.local", "192.0.2.11");
        limiter.recordFailure("user@gape.local", "192.0.2.11");
        limiter.recordSuccess("user@gape.local", "192.0.2.11");

        limiter.recordFailure("user@gape.local", "192.0.2.11");
        limiter.recordFailure("user@gape.local", "192.0.2.11");

        assertDoesNotThrow(() -> limiter.requireAllowed("user@gape.local", "192.0.2.11"));
    }

    @Test
    void alsoBlocksCredentialSprayingAcrossDifferentIdentitiesFromOneIp() {
        LoginAttemptLimiter limiter = limiter(10, 3);

        limiter.recordFailure("first@gape.local", "192.0.2.12");
        limiter.recordFailure("second@gape.local", "192.0.2.12");
        limiter.recordFailure("third@gape.local", "192.0.2.12");

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> limiter.requireAllowed("fourth@gape.local", "192.0.2.12")
        );

        assertEquals(AuthenticationFailureReason.TOO_MANY_ATTEMPTS, exception.reason());
    }

    private static LoginAttemptLimiter limiter(int identityLimit, int ipLimit) {
        return new LoginAttemptLimiter(
                FIXED_CLOCK,
                identityLimit,
                ipLimit,
                Duration.ofMinutes(15),
                Duration.ofMinutes(15),
                100
        );
    }
}
