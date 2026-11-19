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
    public enum Operation { LOGIN, SIGNUP, PASSWORD_RESET, PASSWORD_CHANGE }

    private record Key(Operation operation, boolean account, String value) {}
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

    private synchronized void reserve(Operation operation, String account, String origin) {
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
        var policy = switch (operation) {
            case LOGIN -> limits.login();
            case SIGNUP -> limits.signup();
            case PASSWORD_RESET -> limits.passwordReset();
            case PASSWORD_CHANGE -> limits.passwordChange();
        };

        var accountKey = new Key(operation, true, account);
        var originKey = new Key(operation, false, origin);
        var accountWindow = windows.get(accountKey);
        var originWindow = windows.get(originKey);
        Instant retryAt = later(blockedUntil(accountWindow, policy.perAccount(), now),
                blockedUntil(originWindow, policy.perOrigin(), now));
        int required = (accountWindow == null ? 1 : 0) + (originWindow == null ? 1 : 0);
        if (required > limits.maxEntries() - windows.size()) {
            retryAt = later(retryAt, expiry.peek().at());
        }
        if (retryAt.isAfter(now)) {
            long seconds = Duration.between(now, retryAt).getSeconds();
            if (now.plusSeconds(seconds).isBefore(retryAt)) seconds++;
            throw new AuthRateLimitException(seconds);
        }

        Window acceptedAccount = existingOrNew(accountKey, accountWindow, now, policy.window());
        Window acceptedOrigin = existingOrNew(originKey, originWindow, now, policy.window());
        acceptedAccount.count++;
        acceptedAccount.nextAllowed = now.plus(policy.cooldown());
        acceptedOrigin.count++;
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
