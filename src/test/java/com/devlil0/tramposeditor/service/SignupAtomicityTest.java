package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.controller.UsernameController;
import com.devlil0.tramposeditor.database.model.PendingSignupEntity;
import com.devlil0.tramposeditor.database.repository.IPendingSignupRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SignupAtomicityTest {
    private static final short MAX_ATTEMPTS = 5;

    @Autowired
    private SignupAttempts signupAttempts;

    @Autowired
    private UsernameController usernameController;

    @Autowired
    private SignupService signupService;

    @Autowired
    private UsernameService usernameService;

    @Autowired
    private IUserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transactions;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoSpyBean
    private IPendingSignupRepository pendingRepository;

    @ParameterizedTest
    @ValueSource(ints = {2, 5})
    void contabilizaCadaEscritaConcorrenteEEncerraPendenciaNoLimite(int workers) throws Exception {
        var pending = savePending(LocalDateTime.now().plusMinutes(5));
        var reads = new CountDownLatch(workers);
        var start = new CountDownLatch(1);
        try {
            doAnswer(invocation -> {
                var snapshot = entityManager.find(PendingSignupEntity.class, pending.getId());
                reads.countDown();
                // Sincroniza leituras sem bloqueio. Se a leitura for serializada,
                // o prazo permite que o primeiro trabalhador prossiga e libere a linha.
                reads.await(5, TimeUnit.SECONDS);
                return Optional.ofNullable(snapshot);
            }).when(pendingRepository).findById(pending.getId());

            try (var executor = Executors.newFixedThreadPool(workers)) {
                var writes = new ArrayList<Future<Boolean>>(workers);
                for (int i = 0; i < workers; i++) {
                    writes.add(executor.submit(() -> {
                        if (!start.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Trabalhadores não iniciaram");
                        }
                        return signupAttempts.registrarErro(pending.getId(), MAX_ATTEMPTS);
                    }));
                }
                start.countDown();
                for (var write : writes) {
                    write.get(20, TimeUnit.SECONDS);
                }
            }

            if (workers == MAX_ATTEMPTS) {
                assertThat(pendingRepository.existsById(pending.getId()))
                        .as("a pendência encerra no limite")
                        .isFalse();
            } else {
                assertThat(pendingRepository.findByEmail(pending.getEmail()))
                        .get().extracting(PendingSignupEntity::getAttempts)
                        .as("cada escrita concorrente conta uma vez")
                        .isEqualTo((short) workers);
            }
        } finally {
            pendingRepository.deleteById(pending.getId());
        }
    }

    @Test
    void reservaVigenteOcupaUsernameMasReservaVencidaNaoOcupa() {
        var vigente = savePending(LocalDateTime.now().plusMinutes(5));
        var vencida = savePending(LocalDateTime.now().minusMinutes(5));
        try {
            assertThat(usernameController.availability(vencida.getUsername()).available()).isTrue();
            assertThat(usernameController.availability(vigente.getUsername()).available()).isFalse();
            assertThat(usernameService.validarParaCadastro(vigente.getUsername(), vigente.getEmail())).isNull();
            assertThat(usernameService.validarParaCadastro(vigente.getUsername(), "outro@example.invalid")).isNotNull();
        } finally {
            pendingRepository.deleteById(vencida.getId());
            pendingRepository.deleteById(vigente.getId());
        }
    }

    @Test
    void disponibilidadeNaoMostraNomeLivreDuranteConfirmacao() throws Exception {
        var pending = savePending(LocalDateTime.now().plusMinutes(5));
        String code = TestcontainersConfiguration.CODIGO_DE_CADASTRO;
        pending.setCodeHash(TokenSecrets.hash(code));
        pendingRepository.saveAndFlush(pending);
        String username = pending.getUsername();

        try (var reader = Executors.newSingleThreadExecutor()) {
            assertThat(usernameController.availability(username).available()).isFalse();
            var readerPid = new CompletableFuture<Integer>();
            var read = transactions.execute(status -> {
                // Segura a leitura da pendência até a transação de confirmação concluir.
                jdbc.execute("lock table pending_signups in access exclusive mode");
                signupService.confirm(pending.getEmail(), code);
                var availability = reader.submit(() -> transactions.execute(snapshot -> {
                    readerPid.complete(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                    return usernameController.availability(username);
                }));
                aguardarLeituraBloqueada(readerPid);
                return availability;
            });

            assertThat(read.get(10, TimeUnit.SECONDS).available()).isFalse();
            var account = users.findByEmail(pending.getEmail()).orElseThrow();
            assertThat(usernameController.availability(username).available()).isFalse();
            assertThat(usernameService.validarParaCadastro(username, pending.getEmail())).isNotNull();
            assertThat(usernameService.validar(username, account.getId())).isNull();
        } finally {
            pendingRepository.deleteById(pending.getId());
            users.findByEmail(pending.getEmail()).ifPresent(users::delete);
        }
    }

    private void aguardarLeituraBloqueada(CompletableFuture<Integer> readerPid) {
        try {
            int pid = readerPid.get(5, TimeUnit.SECONDS);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            do {
                if (Boolean.TRUE.equals(jdbc.queryForObject(
                        "select pg_backend_pid() = any(pg_blocking_pids(?))", Boolean.class, pid))) {
                    return;
                }
                Thread.sleep(10);
            } while (System.nanoTime() < deadline);
            throw new AssertionError("A consulta concorrente não chegou ao bloqueio");
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException failure) {
            throw new AssertionError("A consulta concorrente não iniciou", failure);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private PendingSignupEntity savePending(LocalDateTime expiresAt) {
        String username = "teste-" + UUID.randomUUID().toString().substring(0, 12);
        return pendingRepository.saveAndFlush(PendingSignupEntity.builder()
                .email(username + "@example.invalid")
                .passwordHash("hash-ficticio")
                .displayName("Cadastro de teste")
                .username(username)
                .roles("CLIENT")
                .codeHash("a".repeat(64))
                .expiresAt(expiresAt)
                .build());
    }
}
