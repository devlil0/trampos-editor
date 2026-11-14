package com.devlil0.tramposeditor.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

/**
 * Adaptador de desenvolvimento: escreve o e-mail no log em vez de mandar.
 * <p>
 * É o padrão ({@code matchIfMissing = true}) porque rodar o projeto local não pode exigir
 * credencial de SMTP. Imprimir o corpo inteiro é intencional: é assim que se confere o
 * texto e os links sem precisar de caixa de entrada nenhuma.
 */
@Slf4j
@Component
@Profile("!prod")
@ConditionalOnProperty(name = "tramposeditor.notifications.transport",
                       havingValue = "log", matchIfMissing = true)
public class LogEmailSender implements EmailSender {

    @Override
    public void send(String to, EmailContent content){
        // Só a versão em texto: o HTML existe pra caixa de entrada, e despejar markup no
        // console esconderia justamente o que se vem conferir aqui — o texto e o link.
        log.info("""

                ┌─ E-MAIL (dev — não enviado) ─────────────────────────
                │ para:    {}
                │ assunto: {}
                ├──────────────────────────────────────────────────────
                {}
                └──────────────────────────────────────────────────────""",
                to, content.subject(), content.toPlainText());
    }
}
