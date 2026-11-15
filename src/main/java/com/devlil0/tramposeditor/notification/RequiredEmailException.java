package com.devlil0.tramposeditor.notification;

/** Falha segura: nunca guarda conteúdo, destinatário ou exceção do provedor. */
public class RequiredEmailException extends RuntimeException {
    public RequiredEmailException() {
        super("Não foi possível enviar o e-mail agora. Tente novamente mais tarde.");
    }
}
