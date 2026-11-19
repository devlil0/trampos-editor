package com.devlil0.tramposeditor.notification;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Único ponto de execução e observabilidade dos envios. */
@Slf4j
@Component
class NotificationDelivery {

    enum Kind { SIGNUP, PASSWORD_RESET, EMAIL_VERIFICATION, MESSAGE, INTEREST, INTEREST_ACCEPTED, DEAL_CLOSED }

    record PreparedEmail(String recipient, EmailContent content) {
        @Override
        public String toString() {
            return "PreparedEmail";
        }
    }

    private record Attempt(UUID id, Kind kind, long startedAt) {
        Attempt(Kind kind) {
            this(UUID.randomUUID(), kind, System.nanoTime());
        }
    }

    private final EmailSender sender;
    private final boolean simulated;
    private final int shutdownMillis;
    private final ThreadPoolExecutor executor;

    NotificationDelivery(EmailSender sender,
                         @Value("${tramposeditor.notifications.transport}") String transport,
                         @Value("${tramposeditor.notifications.notice-workers:2}") int workers,
                         @Value("${tramposeditor.notifications.notice-queue-capacity:32}") int capacity,
                         @Value("${tramposeditor.notifications.notice-shutdown-ms:5000}") int shutdownMillis) {
        if (shutdownMillis < 0) {
            throw new IllegalArgumentException("A espera de encerramento deve ser não negativa.");
        }
        this.sender = sender;
        this.simulated = "log".equals(transport);
        this.shutdownMillis = shutdownMillis;
        this.executor = new ThreadPoolExecutor(workers, workers, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity), Thread.ofPlatform().daemon().name("marketplace-email-", 0).factory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    void required(Kind kind, Supplier<PreparedEmail> prepare) {
        var attempt = new Attempt(kind);
        PreparedEmail email;
        try {
            email = prepare.get();
        } catch (RuntimeException failure) {
            failed(attempt, "preparo", attempt.startedAt());
            throw new RequiredEmailException();
        }
        if (!send(attempt, email)) {
            throw new RequiredEmailException();
        }
    }

    void afterCommit(Kind kind, Supplier<PreparedEmail> prepare) {
        var attempt = new Attempt(kind);
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            failed(attempt, "sem_transacao", attempt.startedAt());
            return;
        }
        PreparedEmail email;
        try {
            email = prepare.get();
        } catch (RuntimeException failure) {
            failed(attempt, "preparo", attempt.startedAt());
            return;
        }
        if (email == null) {
            return;
        }
        // Apenas dados prontos, sem o Supplier ou entidades, chegam ao callback e à fila.
        var notice = new Notice(attempt, email);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                enqueue(notice);
            }
        });
    }

    private void enqueue(Notice notice) {
        try {
            executor.execute(notice);
        } catch (RejectedExecutionException failure) {
            failed(notice.attempt, executor.isShutdown() ? "encerramento" : "fila_cheia", notice.attempt.startedAt());
        }
    }

    private boolean send(Attempt attempt, PreparedEmail email) {
        long start = System.nanoTime();
        try {
            sender.send(email.recipient(), email.content());
        } catch (RuntimeException failure) {
            failed(attempt, "transporte", start);
            return false;
        }
        log.info("email id={} tipo={} categoria={} duracao_ms={}", attempt.id(), attempt.kind(),
                simulated ? "simulado" : "aceito_pelo_transporte", elapsedMillis(start));
        return true;
    }

    private void failed(Attempt attempt, String category, long start) {
        log.warn("email id={} tipo={} categoria={} duracao_ms={}", attempt.id(), attempt.kind(), category,
                elapsedMillis(start));
    }

    private static long elapsedMillis(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
        try {
            if (executor.awaitTermination(shutdownMillis, TimeUnit.MILLISECONDS)) {
                return;
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        for (Runnable task : executor.shutdownNow()) {
            var notice = (Notice) task;
            failed(notice.attempt, "encerramento", notice.attempt.startedAt());
        }
    }

    private final class Notice implements Runnable {
        private final Attempt attempt;
        private final PreparedEmail email;

        private Notice(Attempt attempt, PreparedEmail email) {
            this.attempt = attempt;
            this.email = email;
        }

        @Override
        public void run() {
            send(attempt, email);
        }
    }
}
