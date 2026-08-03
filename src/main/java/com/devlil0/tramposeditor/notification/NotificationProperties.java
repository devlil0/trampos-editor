package com.devlil0.tramposeditor.notification;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Config do canal. Lê por {@code @Value} como o resto do projeto (ver `SecurityConfig`),
 * em vez de trazer {@code @ConfigurationProperties} só pra dois campos.
 */
@Getter
@Component
public class NotificationProperties {

    /** Remetente dos e-mails. */
    private final String from;

    /**
     * Raiz do <b>frontend</b>, não da API: todo link de e-mail leva a pessoa pra uma
     * tela, nunca pra um JSON.
     */
    private final String baseUrl;

    public NotificationProperties(@Value("${tramposeditor.notifications.from}") String from,
                                  @Value("${tramposeditor.notifications.base-url}") String baseUrl){
        this.from = from;
        this.baseUrl = baseUrl;
    }

    /** Link absoluto pro frontend a partir de um caminho tipo {@code /painel/conversas}. */
    public String link(String path){
        String root = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return root + path;
    }
}
