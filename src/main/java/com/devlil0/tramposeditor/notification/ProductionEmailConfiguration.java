package com.devlil0.tramposeditor.notification;

import jakarta.mail.internet.InternetAddress;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;

/** Validação local, sem abrir conexão SMTP e sem incluir valores em erros. */
@Component
@Profile("prod")
public class ProductionEmailConfiguration {
    public ProductionEmailConfiguration(Environment env) {
        try {
            require("smtp".equals(env.getProperty("tramposeditor.notifications.transport")));
            for (String key : new String[]{"spring.mail.host", "spring.mail.username",
                    "spring.mail.password", "tramposeditor.notifications.from",
                    "tramposeditor.notifications.base-url"}) {
                require(!env.getProperty(key, "").isBlank());
            }
            int port = Integer.parseInt(env.getProperty("spring.mail.port", "587"));
            require(port > 0 && port <= 65535);
            InternetAddress[] from = InternetAddress.parse(
                    env.getProperty("tramposeditor.notifications.from"), true);
            require(from.length == 1);
            from[0].validate();
            URI uri = URI.create(env.getProperty("tramposeditor.notifications.base-url"));
            require("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null);
            require(uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null);
            require(uri.getPath().isEmpty() || "/".equals(uri.getPath()));
            String host = uri.getHost().toLowerCase(java.util.Locale.ROOT);
            require(!host.equals("localhost") && !host.endsWith(".localhost")
                    && !host.equals("127.0.0.1") && !host.equals("[::1]"));
            require(!Boolean.parseBoolean(env.getProperty("spring.mail.properties.mail.debug", "false")));
        } catch (Exception ignored) {
            throw new IllegalStateException("Configuração de e-mail de produção inválida. Consulte docs/EMAIL.md.");
        }
    }

    private static void require(boolean condition) {
        if (!condition) throw new IllegalArgumentException();
    }
}