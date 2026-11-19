package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.config.AuthLimitProperties;
import com.devlil0.tramposeditor.exception.AuthRateLimitException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.devlil0.tramposeditor.service.AuthRateLimiter.Operation.LOGIN;
import static com.devlil0.tramposeditor.service.AuthRateLimiter.Operation.SIGNUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthRateLimiterTest {
    private final MutableClock clock = new MutableClock();

    @Test
    void normalizaContaESeparaOperacoesEOrigens() {
        var limiter = limiter(20, 2, 3, 0);
        limiter.check(LOGIN, "  Conta@Teste.com ", "ip-1");
        limiter.check(LOGIN, "conta@teste.com", "ip-2");
        assertThat(assertThrows(AuthRateLimitException.class,
                () -> limiter.check(LOGIN, "CONTA@TESTE.COM", "ip-3")).retryAfterSeconds()).isEqualTo(60);
        limiter.check(SIGNUP, "conta@teste.com", "ip-3");
        limiter.check(LOGIN, "outra-a@teste.com", "ip-1");
        limiter.check(LOGIN, "outra-b@teste.com", "ip-1");
        clock.now = clock.now.plusMillis(59_500);
        assertThat(assertThrows(AuthRateLimitException.class,
                () -> limiter.check(LOGIN, "outra-c@teste.com", "ip-1")).retryAfterSeconds()).isEqualTo(1);
        clock.now = clock.now.plusMillis(500);
        limiter.check(LOGIN, "outra-c@teste.com", "ip-1");
    }

    @Test
    void cooldownAtravessaOFimDaJanelaSemReiniciarAntesDaHora() {
        var limiter = limiter(10, 10, 10, 20);
        limiter.check(SIGNUP, "conta@teste.com", "ip-1");
        clock.now = clock.now.plusSeconds(55);
        limiter.check(SIGNUP, "conta@teste.com", "ip-1");
        clock.now = clock.now.plusSeconds(5);
        assertThat(assertThrows(AuthRateLimitException.class,
                () -> limiter.check(SIGNUP, "conta@teste.com", "ip-2")).retryAfterSeconds()).isEqualTo(15);
        clock.now = clock.now.plusSeconds(15);
        limiter.check(SIGNUP, "conta@teste.com", "ip-2");
    }

    @Test
    void saturacaoNaoExpulsaContadoresAtivosEAdmiteDepoisDaExpiracao() {
        var limiter = limiter(3, 1, 5, 0);
        limiter.check(LOGIN, "a@teste.com", "ip-1");
        limiter.check(LOGIN, "b@teste.com", "ip-1");
        for (int i = 0; i < 200; i++) {
            String email = "novo" + i + "@teste.com";
            assertThrows(AuthRateLimitException.class, () -> limiter.check(LOGIN, email, "ip-1"));
        }
        assertThrows(AuthRateLimitException.class, () -> limiter.check(LOGIN, "a@teste.com", "ip-1"));
        clock.now = clock.now.plusSeconds(60);
        limiter.check(LOGIN, "c@teste.com", "ip-2");
    }

    @Test
    void reservaConcorrenteNuncaUltrapassaCotaDaConta() throws Exception {
        var limiter = limiter(100, 3, 100, 0);
        var start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try (var pool = Executors.newFixedThreadPool(12)) {
            for (int i = 0; i < 12; i++) {
                String origin = "ip-" + i;
                results.add(pool.submit(() -> {
                    start.await();
                    try {
                        limiter.check(LOGIN, "mesma@teste.com", origin);
                        return true;
                    } catch (AuthRateLimitException e) {
                        return false;
                    }
                }));
            }
            start.countDown();
            int accepted = 0;
            for (var result : results) if (result.get(5, TimeUnit.SECONDS)) accepted++;
            assertThat(accepted).isEqualTo(3);
        }
    }

    private AuthRateLimiter limiter(int capacity, int account, int origin, int cooldownSeconds) {
        var policy = new AuthLimitProperties.Policy(account, origin, Duration.ofMinutes(1),
                Duration.ofSeconds(cooldownSeconds));
        return new AuthRateLimiter(new AuthLimitProperties(capacity, List.of(), policy, policy, policy), clock);
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2025-01-01T00:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { throw new UnsupportedOperationException(); }
        @Override public Instant instant() { return now; }
    }
}
