package com.devlil0.tramposeditor.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Simula o envio. NotificationDelivery registra apenas o resultado operacional. */
@Component
@Profile("!prod")
@ConditionalOnProperty(name = "tramposeditor.notifications.transport",
                       havingValue = "log", matchIfMissing = true)
public class LogEmailSender implements EmailSender {

    @Override
    public void send(String to, EmailContent content) {
        // Para inspecionar uma mensagem fictícia, use um capturador SMTP local.
    }
}
