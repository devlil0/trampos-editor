package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.config.AuthLimitProperties;
import com.devlil0.tramposeditor.exception.AuthRateLimitException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

@Service
public class AuthRateLimiter {
    public enum Operation { LOGIN, SIGNUP, PASSWORD_RESET, PASSWORD_CHANGE, SIGNUP_CONFIRM }

    private record Key(Operation operation, boolean account, String value) {}
    private record Quota(Key key, int limit, Duration window, Duration cooldown) {}
    private record Expiry(Key key, Instant at) {}

    private static final class Window {
        private Instant expiresAt;
        private Instant nextAllowed;
        private int count;

        private Window(Instant expiresAt, Instant now) {
            this.expiresAt = expiresAt;
            this.nextAllowed = now;
        }
    }

    private final AuthLimitProperties limits;
    private final Clock clock;
    private final Map<Key, Window> windows = new HashMap<>();
    private final PriorityQueue<Expiry> expiry = new PriorityQueue<>(Comparator.comparing(Expiry::at));

    public AuthRateLimiter(AuthLimitProperties limits, @Qualifier("authRateLimitClock") Clock clock) {
        this.limits = limits;
        this.clock = clock;
    }

    public void check(Operation operation, String email, String origin) {
        String normalized = TokenSecrets.hash(email.trim().toLowerCase(Locale.ROOT));
        reserve(operation, normalized, origin);
    }

    public void checkPasswordChange(UUID userId, String origin) {
        reserve(Operation.PASSWORD_CHANGE, TokenSecrets.hash(userId.toString()), origin);
    }

    public void checkSignupConfirmation(String origin) {
        var policy = limits.signupConfirm();
        reserve(originQuota(Operation.SIGNUP_CONFIRM, origin, policy.perOrigin(), policy.window()));
    }

    private void reserve(Operation operation, String account, String origin) {
        var policy = switch (operation) {
            case LOGIN -> limits.login();
            case SIGNUP -> limits.signup();
            case PASSWORD_RESET -> limits.passwordReset();
            case PASSWORD_CHANGE -> limits.passwordChange();
            case SIGNUP_CONFIRM -> throw new IllegalArgumentException("Signup confirmation only accepts an origin");
        };
        reserve(new Quota(new Key(operation, true, account), policy.perAccount(), policy.window(), policy.cooldown()),
                originQuota(operation, origin, policy.perOrigin(), policy.window()));
    }

    private Quota originQuota(Operation operation, String origin, int limit, Duration window) {
        return new Quota(new Key(operation, false, origin), limit, window, Duration.ZERO);
    }

    private synchronized void reserve(Quota... quotas) {
        Instant now = clock.instant();
        while (!expiry.isEmpty() && !expiry.peek().at().isAfter(now)) {
            var due = expiry.remove();
            var window = windows.get(due.key());
            Instant keepUntil = later(window.expiresAt, window.nextAllowed);
            if (keepUntil.isAfter(now)) {
                expiry.add(new Expiry(due.key(), keepUntil));
            } else {
                windows.remove(due.key());
            }
        }
        Instant retryAt = now;
        int required = 0;
        for (Quota quota : quotas) {
            Window window = windows.get(quota.key());
            retryAt = later(retryAt, blockedUntil(window, quota.limit(), now));
            if (window == null) required++;
        }
        if (required > limits.maxEntries() - windows.size()) {
            retryAt = later(retryAt, expiry.peek().at());
        }
        if (retryAt.isAfter(now)) {
            long seconds = Duration.between(now, retryAt).getSeconds();
            if (now.plusSeconds(seconds).isBefore(retryAt)) seconds++;
            throw new AuthRateLimitException(seconds);
        }

        for (Quota quota : quotas) {
            Window window = existingOrNew(quota.key(), windows.get(quota.key()), now, quota.window());
            window.count++;
            if (quota.key().account()) {
                window.nextAllowed = now.plus(quota.cooldown());
            }
        }
    }

    private Window existingOrNew(Key key, Window window, Instant now, Duration duration) {
        if (window != null) {
            if (!window.expiresAt.isAfter(now)) {
                window.expiresAt = now.plus(duration);
                window.count = 0;
            }
            return window;
        }
        var created = new Window(now.plus(duration), now);
        windows.put(key, created);
        expiry.add(new Expiry(key, created.expiresAt));
        return created;
    }

    private Instant blockedUntil(Window window, int quota, Instant now) {
        if (window == null) return now;
        return window.count >= quota && window.expiresAt.isAfter(now)
                ? later(window.expiresAt, window.nextAllowed) : window.nextAllowed;
    }

    private Instant later(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }
}
