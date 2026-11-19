package com.devlil0.tramposeditor.notification;

import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(15)
class SmtpSocketTimeoutTest {
    private static final int TIMEOUT_MS = 400;

    @Test
    void leituraExpiraSeServidorLocalNaoEnviaSaudacao() throws Exception {
        try (var server = listen()) {
            var release = new CountDownLatch(1);
            var peer = new FutureTask<>(() -> {
                try (var socket = server.accept()) {
                    assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
                }
                return null;
            });
            Thread.ofPlatform().daemon().start(peer);

            var mail = mailAt(server);
            mail.getJavaMailProperties().setProperty("mail.smtp.timeout", Integer.toString(TIMEOUT_MS));
            try (var transport = mail.getSession().getTransport("smtp")) {
                long start = System.nanoTime();
                assertThatThrownBy(() -> transport.connect(mail.getHost(), mail.getPort(), null, null))
                        .isInstanceOf(MessagingException.class).hasRootCauseInstanceOf(SocketTimeoutException.class);
                assertElapsed("leitura", start);
            } finally {
                release.countDown();
            }
            peer.get(3, TimeUnit.SECONDS);
        }
    }

    @Test
    void escritaExpiraSeServidorLocalNaoLeCorpo() throws Exception {
        try (var server = listen()) {
            var release = new CountDownLatch(1);
            var data = new CountDownLatch(1);
            var peer = new FutureTask<>(() -> {
                try (var socket = server.accept()) {
                    enableData(socket);
                    data.countDown();
                    assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
                }
                return null;
            });
            Thread.ofPlatform().daemon().start(peer);

            var mail = mailAt(server);
            mail.getJavaMailProperties().setProperty("mail.smtp.writetimeout", Integer.toString(TIMEOUT_MS));
            // Maior que os buffers do loopback. O servidor para de ler após DATA.
            var message = mail.createMimeMessage();
            message.setFrom(new jakarta.mail.internet.InternetAddress("remetente@example.test"));
            message.setRecipients(jakarta.mail.Message.RecipientType.TO, "destino@example.test");
            message.setText(("x".repeat(80) + "\n").repeat(220_000));
            message.saveChanges();

            try (var transport = mail.getSession().getTransport("smtp")) {
                transport.connect(mail.getHost(), mail.getPort(), null, null);
                long start = System.nanoTime();
                assertThatThrownBy(() -> transport.sendMessage(message, message.getAllRecipients()))
                        .isInstanceOf(MessagingException.class).hasRootCauseInstanceOf(SocketException.class);
                assertThat(data.getCount()).isZero();
                assertElapsed("escrita", start);
            } finally {
                release.countDown();
            }
            peer.get(3, TimeUnit.SECONDS);
        }
    }

    private static ServerSocket listen() throws IOException {
        var server = new ServerSocket();
        server.setReceiveBufferSize(1024);
        server.bind(new InetSocketAddress("127.0.0.1", 0));
        return server;
    }

    private static JavaMailSenderImpl mailAt(ServerSocket server) {
        var mail = new JavaMailSenderImpl();
        mail.setHost("127.0.0.1");
        mail.setPort(server.getLocalPort());
        mail.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout", "2000");
        mail.getJavaMailProperties().setProperty("mail.smtp.timeout", "5000");
        return mail;
    }

    private static void enableData(Socket socket) throws IOException {
        socket.setSoTimeout(3000);
        var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
        reply(socket, "220 local SMTP");
        assertThat(reader.readLine()).startsWith("EHLO");
        reply(socket, "250 local");
        assertThat(reader.readLine()).startsWith("MAIL FROM:");
        reply(socket, "250 ok");
        assertThat(reader.readLine()).startsWith("RCPT TO:");
        reply(socket, "250 ok");
        assertThat(reader.readLine()).isEqualTo("DATA");
        reply(socket, "354 go ahead");
    }

    private static void reply(Socket socket, String text) throws IOException {
        socket.getOutputStream().write((text + "\r\n").getBytes(StandardCharsets.US_ASCII));
        socket.getOutputStream().flush();
    }

    private static void assertElapsed(String operation, long start) {
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertThat(elapsed).isBetween(300L, 4000L);
        System.out.printf("Timeout SMTP local (%s): %d ms, configurado em %d ms.%n", operation, elapsed, TIMEOUT_MS);
    }
}
