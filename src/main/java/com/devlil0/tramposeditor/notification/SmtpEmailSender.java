package com.devlil0.tramposeditor.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Adaptador de produção. Só entra com {@code tramposeditor.notifications.transport=smtp},
 * o que mantém dev e teste fora da rede sem nenhuma flag extra.
 * <p>
 * Manda <b>texto e HTML no mesmo envio</b> ({@code multipart/alternative}): o cliente
 * escolhe o que sabe renderizar, e o texto puro continua sendo o que aparece em quem
 * bloqueia HTML, em leitor de tela e na prévia da caixa de entrada. Não é HTML <i>em vez
 * de</i> texto — são as duas faces da mesma mensagem.
 * <p>
 * Até 2026-08-05 isto era {@code SimpleMailMessage} e só texto, com a justificativa de que
 * HTML traria "estética de newsletter" (§6.1). A recusa continua valendo contra template
 * genérico e ilustração de banco de imagem — mas o §4 lista a voz fora do site como buraco
 * aberto, e chegar sem cara nenhuma não é sobriedade, é ausência. Ver {@link EmailHtml}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tramposeditor.notifications.transport", havingValue = "smtp")
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    /** Cópia de {@code frontend/public/logo-mark.png} — a claquete da marca. */
    private static final String LOGO = "email/logo-mark.png";

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    @Override
    public void send(String to, EmailContent content){
        try {
            MimeMessage message = mailSender.createMimeMessage();

            // O `true` liga o modo multipart — é o que permite ter o logo embutido junto
            // das duas versões do corpo.
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(properties.getFrom());
            helper.setTo(to);
            helper.setSubject(content.subject());
            // A ordem dos argumentos importa: texto primeiro, HTML depois. É assim que o
            // multipart/alternative declara qual é a versão "mais rica".
            helper.setText(content.toPlainText(), EmailHtml.render(content));

            // Depois do setText: antes dele o helper ainda não montou o multipart onde
            // o inline entra.
            helper.addInline(EmailHtml.LOGO_CID, new ClassPathResource(LOGO), "image/png");

            mailSender.send(message);
        } catch (MailException | MessagingException e) {
            // Engolir é deliberado: quem chamou está no meio de mandar uma mensagem ou
            // fechar um trampo. Perder o aviso é ruim; perder a ação do usuário é pior.
            log.error("Falha ao enviar e-mail para {} (assunto: {})", to, content.subject(), e);
        }
    }
}
