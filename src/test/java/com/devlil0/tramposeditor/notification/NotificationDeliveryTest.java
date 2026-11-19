package com.devlil0.tramposeditor.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class NotificationDeliveryTest {
    private static final String RECIPIENT = "destino-ficticio@example.test";
    private static final EmailContent CONTENT = new EmailContent(
            "assunto-ficticio-privado", "Pessoa teste", List.of("corpo-ficticio-privado"), null, null, null);

    @Test
    void filaCheiaPreservaThreadDaRequisicaoEEscoaAposLiberacao(CapturedOutput output) throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var calls = new AtomicInteger();
        var sender = new NotificationDelivery((recipient, content) -> {
            if (calls.incrementAndGet() == 1) {
                started.countDown();
                await(release);
            }
        }, "smtp", 1, 1, 2000);
        try {
            commit(sender);
            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
            commit(sender);
            commit(sender);
            assertThat(calls).hasValue(1);
            assertThat(output.getAll()).contains("tipo=MESSAGE categoria=fila_cheia duracao_ms=");
        } finally {
            release.countDown();
            sender.shutdown();
        }
        assertThat(calls).hasValue(2);
        assertSafe(output);
    }

    @Test
    void encerramentoInterrompeWorkerEDescartaFilaSePrazoTerminar(CapturedOutput output) throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var interrupted = new CountDownLatch(1);
        var calls = new AtomicInteger();
        var sender = new NotificationDelivery((recipient, content) -> {
            calls.incrementAndGet();
            started.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                interrupted.countDown();
                throw new IllegalStateException("erro-ficticio-privado");
            }
        }, "smtp", 1, 1, 100);
        try {
            commit(sender);
            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
            commit(sender);

            sender.shutdown();

            assertThat(interrupted.await(2, TimeUnit.SECONDS)).isTrue();
            commit(sender);
            assertThat(calls).hasValue(1);
            assertThat(output.getAll()).contains("tipo=MESSAGE categoria=encerramento duracao_ms=");
            assertSafe(output);
        } finally {
            release.countDown();
            sender.shutdown();
        }
    }

    @Test
    void modoLogNaoImprimeMensagemDeIdentidade(CapturedOutput output) {
        var sender = new NotificationDelivery(new LogEmailSender(), "log", 1, 1, 0);
        try {
            sender.required(NotificationDelivery.Kind.SIGNUP,
                    () -> new NotificationDelivery.PreparedEmail(RECIPIENT, CONTENT));
        } finally {
            sender.shutdown();
        }
        assertThat(output.getAll()).containsPattern(
                "email id=[0-9a-f-]{36} tipo=SIGNUP categoria=simulado duracao_ms=\\d+");
        assertSafe(output);
    }

    private static void commit(NotificationDelivery delivery) {
        List<TransactionSynchronization> synchronizations;
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            delivery.afterCommit(NotificationDelivery.Kind.MESSAGE,
                    () -> new NotificationDelivery.PreparedEmail(RECIPIENT, CONTENT));
            synchronizations = TransactionSynchronizationManager.getSynchronizations();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
        synchronizations.forEach(TransactionSynchronization::afterCommit);
    }

    private static void await(CountDownLatch release) {
        try {
            assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Worker interrompido no teste.");
        }
    }

    private static void assertSafe(CapturedOutput output) {
        assertThat(output.getAll()).doesNotContain(RECIPIENT, CONTENT.subject(),
                CONTENT.paragraphs().getFirst(), "erro-ficticio-privado");
    }
}
