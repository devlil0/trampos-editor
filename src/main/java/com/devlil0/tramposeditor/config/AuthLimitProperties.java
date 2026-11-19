package com.devlil0.tramposeditor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@ConfigurationProperties("tramposeditor.auth-limits")
public record AuthLimitProperties(int maxEntries, List<String> trustedProxies, Policy login,
                                  Policy signup, Policy passwordReset) {
    public AuthLimitProperties {
        if (maxEntries < 2) throw new IllegalArgumentException("auth-limits.max-entries deve ser pelo menos 2");
        trustedProxies = trustedProxies == null ? List.of() : List.copyOf(trustedProxies);
        Objects.requireNonNull(login);
        Objects.requireNonNull(signup);
        Objects.requireNonNull(passwordReset);
    }

    public record Policy(int perAccount, int perOrigin, Duration window, Duration cooldown) {
        public Policy {
            if (perAccount < 1 || perOrigin < 1 || window == null || window.isZero() || window.isNegative()
                    || cooldown == null || cooldown.isNegative() || cooldown.compareTo(window) > 0) {
                throw new IllegalArgumentException("Limites de autenticação inválidos");
            }
        }
    }
}
