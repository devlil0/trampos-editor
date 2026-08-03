package com.devlil0.tramposeditor.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptador de desenvolvimento: escreve o e-mail no log em vez de mandar.
 * <p>
 * É o padrão ({@code matchIfMissing = true}) porque rodar o projeto local não pode exigir
 * credencial de SMTP. Imprimir o corpo inteiro é intencional: é assim que se confere o
 * texto e os links sem precisar de caixa de entrada nenhuma.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tramposeditor.notifications.transport",
                       havingValue = "log", matchIfMissing = true)
public class LogEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String body){
        log.info("""

                ┌─ E-MAIL (dev — não enviado) ─────────────────────────
                │ para:    {}
                │ assunto: {}
                ├──────────────────────────────────────────────────────
                {}
                └──────────────────────────────────────────────────────""",
                to, subject, body);
    }
}
