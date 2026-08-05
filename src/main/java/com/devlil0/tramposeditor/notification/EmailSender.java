package com.devlil0.tramposeditor.notification;

/**
 * A saída do canal — o único ponto do sistema que toca o mundo fora do site.
 * <p>
 * É uma porta e não uma classe concreta porque o ambiente decide o adaptador: em dev
 * não existe SMTP e o e-mail vai pro log ({@link LogEmailSender}); em prod vai pra
 * rede ({@link SmtpEmailSender}). Rodar o projeto local nunca deve exigir credencial
 * de e-mail.
 */
public interface EmailSender {

    /**
     * Entrega uma mensagem. Implementações <b>não devem</b> propagar falha de entrega:
     * e-mail é efeito colateral, e derrubar o pedido do usuário porque o SMTP piscou
     * seria trocar um aviso perdido por uma ação perdida.
     * <p>
     * Recebe {@link EmailContent} e não texto pronto porque cada adaptador precisa de uma
     * forma diferente do mesmo conteúdo: o log quer texto legível, o SMTP manda texto
     * <i>e</i> HTML no mesmo envio.
     */
    void send(String to, EmailContent content);
}
