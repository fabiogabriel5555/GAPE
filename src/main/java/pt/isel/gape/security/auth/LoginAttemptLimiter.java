package pt.isel.gape.security.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class LoginAttemptLimiter {

    private static final int DEFAULT_MAX_ATTEMPTS_PER_IDENTITY_AND_IP = 5;
    private static final int DEFAULT_MAX_ATTEMPTS_PER_IP = 25;
    private static final int DEFAULT_MAX_TRACKED_KEYS = 10_000;
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(15);
    private static final Duration DEFAULT_BLOCK_DURATION = Duration.ofMinutes(15);

    private final Clock clock;
    private final int maxAttemptsPerIdentityAndIp;
    private final int maxAttemptsPerIp;
    private final int maxTrackedKeys;
    private final Duration window;
    private final Duration blockDuration;
    private final LinkedHashMap<String, AttemptState> attempts = new LinkedHashMap<>(128, 0.75f, true);

    LoginAttemptLimiter(Clock clock) {
        this(
                clock,
                DEFAULT_MAX_ATTEMPTS_PER_IDENTITY_AND_IP,
                DEFAULT_MAX_ATTEMPTS_PER_IP,
                DEFAULT_WINDOW,
                DEFAULT_BLOCK_DURATION,
                DEFAULT_MAX_TRACKED_KEYS
        );
    }

    LoginAttemptLimiter(
            Clock clock,
            int maxAttemptsPerIdentityAndIp,
            int maxAttemptsPerIp,
            Duration window,
            Duration blockDuration,
            int maxTrackedKeys
    ) {
        this.clock = Objects.requireNonNull(clock, "clock is required");
        if (maxAttemptsPerIdentityAndIp <= 0 || maxAttemptsPerIp <= 0 || maxTrackedKeys < 2) {
            throw new IllegalArgumentException("login attempt limits must be positive");
        }
        if (window == null || window.isZero() || window.isNegative()
                || blockDuration == null || blockDuration.isZero() || blockDuration.isNegative()) {
            throw new IllegalArgumentException("login attempt durations must be positive");
        }
        this.maxAttemptsPerIdentityAndIp = maxAttemptsPerIdentityAndIp;
        this.maxAttemptsPerIp = maxAttemptsPerIp;
        this.window = window;
        this.blockDuration = blockDuration;
        this.maxTrackedKeys = maxTrackedKeys;
    }

    synchronized void requireAllowed(String email, String sourceIp) {
        Instant now = clock.instant();
        pruneExpired(now);
        if (isBlocked(identityAndIpKey(email, sourceIp), now) || isBlocked(ipKey(sourceIp), now)) {
            throw new AuthenticationException(
                    AuthenticationFailureReason.TOO_MANY_ATTEMPTS,
                    "Too many authentication attempts"
            );
        }
    }

    synchronized void recordFailure(String email, String sourceIp) {
        Instant now = clock.instant();
        recordFailure(identityAndIpKey(email, sourceIp), maxAttemptsPerIdentityAndIp, now);
        recordFailure(ipKey(sourceIp), maxAttemptsPerIp, now);
        enforceCapacity(now);
    }

    synchronized void recordSuccess(String email, String sourceIp) {
        attempts.remove(identityAndIpKey(email, sourceIp));
    }

    private void recordFailure(String key, int limit, Instant now) {
        AttemptState state = attempts.get(key);
        if (state == null || state.windowStartedAt.plus(window).compareTo(now) <= 0) {
            state = new AttemptState(now);
            attempts.put(key, state);
        }
        state.failures++;
        state.lastFailureAt = now;
        if (state.failures >= limit) {
            state.blockedUntil = now.plus(blockDuration);
        }
    }

    private boolean isBlocked(String key, Instant now) {
        AttemptState state = attempts.get(key);
        return state != null && state.blockedUntil != null && state.blockedUntil.isAfter(now);
    }

    private void enforceCapacity(Instant now) {
        if (attempts.size() <= maxTrackedKeys) {
            return;
        }
        pruneExpired(now);
        Iterator<Map.Entry<String, AttemptState>> iterator = attempts.entrySet().iterator();
        while (attempts.size() > maxTrackedKeys && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private void pruneExpired(Instant now) {
        attempts.entrySet().removeIf(entry -> entry.getValue().isExpired(now, window));
    }

    private static String identityAndIpKey(String email, String sourceIp) {
        String normalizedEmail = email == null ? "<blank>" : email.trim().toLowerCase(Locale.ROOT);
        return "identity-ip:" + digest(normalizedEmail + '\0' + normalizedSourceIp(sourceIp));
    }

    private static String ipKey(String sourceIp) {
        return "ip:" + digest(normalizedSourceIp(sourceIp));
    }

    private static String normalizedSourceIp(String sourceIp) {
        return sourceIp == null || sourceIp.isBlank() ? "<unknown>" : sourceIp.trim();
    }

    private static String digest(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static final class AttemptState {
        private final Instant windowStartedAt;
        private Instant lastFailureAt;
        private Instant blockedUntil;
        private int failures;

        private AttemptState(Instant now) {
            this.windowStartedAt = now;
            this.lastFailureAt = now;
        }

        private boolean isExpired(Instant now, Duration window) {
            boolean blockExpired = blockedUntil == null || !blockedUntil.isAfter(now);
            return blockExpired && lastFailureAt.plus(window).compareTo(now) <= 0;
        }
    }
}
