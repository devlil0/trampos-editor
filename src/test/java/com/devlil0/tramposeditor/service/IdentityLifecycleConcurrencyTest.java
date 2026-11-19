package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.model.EmailVerificationTokenEntity;
import com.devlil0.tramposeditor.database.model.PasswordResetTokenEntity;
import com.devlil0.tramposeditor.database.model.PendingSignupEntity;
import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IEmailVerificationTokenRepository;
import com.devlil0.tramposeditor.database.repository.IPasswordResetTokenRepository;
import com.devlil0.tramposeditor.database.repository.IPendingSignupRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.dto.PasswordResetConfirmDTO;
import com.devlil0.tramposeditor.enums.UserRole;
import com.devlil0.tramposeditor.exception.BusinessException;
import com.devlil0.tramposeditor.notification.EmailSender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class IdentityLifecycleConcurrencyTest {
    @Autowired SignupService signup;
    @Autowired PasswordResetService reset;
    @Autowired EmailVerificationService verification;
    @Autowired IPendingSignupRepository pending;
    @Autowired IUserRepository users;
    @Autowired EntityManager entityManager;
    @Autowired TransactionTemplate transaction;
    @MockitoBean EmailSender sender;
    @MockitoSpyBean IPasswordResetTokenRepository resetTokens;
    @MockitoSpyBean IEmailVerificationTokenRepository verificationTokens;

    @Test
    void confirmacaoAguardaReenvioEUsaCodigoVigente() throws Exception {
        var entry = pendingSignup();
        try {
            duranteReenvio(entry, () -> signup.confirm(entry.getEmail(), TestcontainersConfiguration.CODIGO_DE_CADASTRO), true);
            assertThat(users.findByEmail(entry.getEmail())).isPresent();
            assertThat(pending.existsById(entry.getId())).isFalse();
        } finally {
            pending.deleteById(entry.getId());
            users.findByEmail(entry.getEmail()).ifPresent(users::delete);
        }
    }

    @Test
    void confirmacaoQueVenceADisputaImpedeNovoEnvio() throws Exception {
        var entry = pendingSignup();
        var confirmed = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var resendStarted = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var confirmation = pool.submit(() -> {
                transaction.executeWithoutResult(status -> {
                    signup.confirm(entry.getEmail(), "codigo-antigo-de-teste");
                    confirmed.countDown();
                    try {
                        await(release);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(interrupted);
                    }
                });
            });
            try {
                await(confirmed);
                var resend = pool.submit(() -> {
                    resendStarted.countDown();
                    signup.resend(entry.getEmail());
                });
                await(resendStarted);
                assertThatThrownBy(() -> resend.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);
                release.countDown();
                confirmation.get(15, TimeUnit.SECONDS);
                resend.get(15, TimeUnit.SECONDS);
                assertThat(users.findByEmail(entry.getEmail())).isPresent();
                assertThat(pending.existsById(entry.getId())).isFalse();
                verifyNoInteractions(sender);
            } finally {
                release.countDown();
            }
        } finally {
            pending.deleteById(entry.getId());
            users.findByEmail(entry.getEmail()).ifPresent(users::delete);
        }
    }

    @Test
    void limpezaQueDisputaComReenvioPreservaPendenciaRenovada() throws Exception {
        var entry = pendingSignup();
        try {
            LocalDateTime cutoff = entry.getExpiresAt().plusSeconds(1);
            int deleted = duranteReenvio(entry,
                    () -> transaction.execute(status -> pending.deleteExpired(cutoff)), false);
            assertThat(deleted).isZero();
            assertThat(pending.findById(entry.getId())).get()
                    .extracting(PendingSignupEntity::getExpiresAt).satisfies(expires -> assertThat(expires).isAfter(cutoff));
        } finally {
            pending.deleteById(entry.getId());
        }
    }

    @Test
    void reenvioTrocaCodigoMesmoQuandoGeradorRepeteOAnterior() {
        var entry = pendingSignup();
        entry.setCodeHash(TokenSecrets.hash(TestcontainersConfiguration.CODIGO_DE_CADASTRO));
        pending.saveAndFlush(entry);
        try {
            signup.resend(entry.getEmail());
            assertThat(pending.findById(entry.getId()).orElseThrow().getCodeHash()).isNotEqualTo(entry.getCodeHash());
            assertThatThrownBy(() -> signup.confirm(entry.getEmail(), TestcontainersConfiguration.CODIGO_DE_CADASTRO))
                    .isInstanceOf(BusinessException.class);
            assertThat(users.findByEmail(entry.getEmail())).isEmpty();
        } finally {
            pending.deleteById(entry.getId());
        }
    }

    @Test
    void consumoDeResetAlteraVersaoDaSessaoUmaVez() throws Exception {
        var user = account();
        String token = UUID.randomUUID().toString();
        String hash = TokenSecrets.hash(token);
        var entry = resetTokens.saveAndFlush(PasswordResetTokenEntity.builder().user(user).tokenHash(hash)
                .expiresAt(LocalDateTime.now().plusMinutes(10)).build());
        var reads = new CountDownLatch(2);
        doAnswer(invocation -> readFixture(PasswordResetTokenEntity.class, entry.getId(), reads))
                .when(resetTokens).findByTokenHash(hash);
        try {
            assertThat(concurrentCompletions(() -> reset.confirm(new PasswordResetConfirmDTO(token, "senha-ficticia-123"))))
                    .isEqualTo(1);
            assertThat(users.findById(user.getId()).orElseThrow().getSessionVersion()).isEqualTo(1);
        } finally {
            resetTokens.deleteById(entry.getId());
            users.deleteById(user.getId());
        }
    }

    @Test
    void consumoDeVerificacaoTemUmaUnicaTransicaoBemSucedida() throws Exception {
        var user = account();
        String token = UUID.randomUUID().toString();
        String hash = TokenSecrets.hash(token);
        var entry = verificationTokens.saveAndFlush(EmailVerificationTokenEntity.builder().user(user).tokenHash(hash)
                .expiresAt(LocalDateTime.now().plusMinutes(10)).build());
        var reads = new CountDownLatch(2);
        doAnswer(invocation -> readFixture(EmailVerificationTokenEntity.class, entry.getId(), reads))
                .when(verificationTokens).findByTokenHash(hash);
        try {
            assertThat(concurrentCompletions(() -> verification.confirm(token))).isEqualTo(1);
            assertThat(users.findById(user.getId()).orElseThrow().isEmailVerified()).isTrue();
        } finally {
            verificationTokens.deleteById(entry.getId());
            users.deleteById(user.getId());
        }
    }

    @Test
    void rollbackDeResetPreservaTokenSenhaEVersaoDaSessao() {
        var user = account();
        String token = UUID.randomUUID().toString();
        var entry = resetTokens.saveAndFlush(PasswordResetTokenEntity.builder().user(user)
                .tokenHash(TokenSecrets.hash(token)).expiresAt(LocalDateTime.now().plusMinutes(10)).build());
        var failed = new IllegalStateException("falha simulada da transação");
        try {
            var dto = new PasswordResetConfirmDTO(token, "senha-ficticia-123");
            assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                reset.confirm(dto);
                entityManager.flush();
                throw failed;
            })).isSameAs(failed);
            var unchanged = users.findById(user.getId()).orElseThrow();
            assertThat(unchanged.getPasswordHash()).isEqualTo(user.getPasswordHash());
            assertThat(unchanged.getSessionVersion()).isEqualTo(user.getSessionVersion());
            assertThat(resetTokens.findById(entry.getId()).orElseThrow().getUsedAt()).isNull();

            reset.confirm(dto);
            assertThat(resetTokens.findById(entry.getId()).orElseThrow().getUsedAt()).isNotNull();
        } finally {
            resetTokens.deleteById(entry.getId());
            users.deleteById(user.getId());
        }
    }

    @Test
    void rollbackDeVerificacaoPreservaTokenEEstadoDaConta() {
        var user = account();
        String token = UUID.randomUUID().toString();
        var entry = verificationTokens.saveAndFlush(EmailVerificationTokenEntity.builder().user(user)
                .tokenHash(TokenSecrets.hash(token)).expiresAt(LocalDateTime.now().plusMinutes(10)).build());
        var failed = new IllegalStateException("falha simulada da transação");
        try {
            assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                verification.confirm(token);
                entityManager.flush();
                throw failed;
            })).isSameAs(failed);
            assertThat(users.findById(user.getId()).orElseThrow().isEmailVerified()).isFalse();
            assertThat(verificationTokens.findById(entry.getId()).orElseThrow().getUsedAt()).isNull();

            verification.confirm(token);
            assertThat(verificationTokens.findById(entry.getId()).orElseThrow().getUsedAt()).isNotNull();
        } finally {
            verificationTokens.deleteById(entry.getId());
            users.deleteById(user.getId());
        }
    }

    private <T> java.util.Optional<T> readFixture(Class<T> type, UUID id, CountDownLatch reads) throws InterruptedException {
        var snapshot = entityManager.find(type, id);
        reads.countDown();
        reads.await(2, TimeUnit.SECONDS);
        return java.util.Optional.ofNullable(snapshot);
    }

    private int concurrentCompletions(Runnable operation) throws Exception {
        var start = new CountDownLatch(1);
        Callable<Boolean> complete = () -> {
            await(start);
            try {
                operation.run();
                return true;
            } catch (BusinessException rejected) {
                return false;
            }
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(complete);
            var second = pool.submit(complete);
            start.countDown();
            return (first.get(15, TimeUnit.SECONDS) ? 1 : 0) + (second.get(15, TimeUnit.SECONDS) ? 1 : 0);
        }
    }

    private <T> T duranteReenvio(PendingSignupEntity entry, Callable<T> operation, boolean waitForRenewal) throws Exception {
        var sending = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var started = new CountDownLatch(1);
        doAnswer(invocation -> {
            sending.countDown();
            await(release);
            return null;
        }).when(sender).send(eq(entry.getEmail()), any());
        try (var pool = Executors.newFixedThreadPool(2)) {
            var resend = pool.submit(() -> signup.resend(entry.getEmail()));
            try {
                await(sending);
                var operationResult = pool.submit(() -> {
                    started.countDown();
                    return operation.call();
                });
                await(started);
                if (waitForRenewal) {
                    assertThatThrownBy(() -> operationResult.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);
                    release.countDown();
                }
                T result = operationResult.get(15, TimeUnit.SECONDS);
                release.countDown();
                resend.get(15, TimeUnit.SECONDS);
                return result;
            } finally {
                release.countDown();
            }
        }
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    private PendingSignupEntity pendingSignup() {
        String name = testName();
        return pending.saveAndFlush(PendingSignupEntity.builder().email(name + "@example.invalid")
                .passwordHash("hash-ficticio").displayName("Pendente").username(name).roles("CLIENT")
                .codeHash(TokenSecrets.hash("codigo-antigo-de-teste")).expiresAt(LocalDateTime.now().plusMinutes(2)).build());
    }

    private UserEntity account() {
        String name = testName();
        return users.saveAndFlush(UserEntity.builder().email(name + "@example.invalid")
                .displayName("Conta de teste").username(name).roles(Set.of(UserRole.CLIENT)).active(true).build());
    }

    private static String testName() {
        return "t06-" + UUID.randomUUID().toString().substring(0, 12);
    }
}
