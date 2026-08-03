package com.devlil0.tramposeditor.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Adaptador de produção. Só entra com {@code tramposeditor.notifications.transport=smtp},
 * o que mantém dev e teste fora da rede sem nenhuma flag extra.
 * <p>
 * Texto puro, sem HTML: o produto tem cinco e-mails, todos curtos, e HTML aqui só
 * traria template, imagem e a estética de newsletter que o §6.1 recusa.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tramposeditor.notifications.transport", havingValue = "smtp")
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    @Override
    public void send(String to, String subject, String body){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
        } catch (MailException e) {
            // Engolir é deliberado: quem chamou está no meio de mandar uma mensagem ou
            // fechar um trampo. Perder o aviso é ruim; perder a ação do usuário é pior.
            log.error("Falha ao enviar e-mail para {} (assunto: {})", to, subject, e);
        }
    }
}
