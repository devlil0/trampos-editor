package com.devlil0.tramposeditor.notification;

/** Porta de envio: a implementação deve propagar falhas sem conteúdo sensível. */
public interface EmailSender {
    /**
     * Solicita entrega ao transporte, sem garantir chegada à caixa de entrada.
     * Quem chama decide se a falha impede o fluxo obrigatório ou é de melhor esforço.
     */
    void send(String to, EmailContent content);
}