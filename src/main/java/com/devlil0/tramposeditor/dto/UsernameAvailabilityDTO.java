package com.devlil0.tramposeditor.dto;

/**
 * Resposta da checagem de disponibilidade, enquanto a pessoa digita.
 *
 * Devolve a mensagem junto do booleano porque "indisponível" tem motivos diferentes —
 * curto demais, caractere inválido, reservado pela plataforma, já tomado — e mandar o
 * frontend adivinhar qual foi criaria uma segunda cópia das regras do
 * {@code UsernameService}. Aqui a regra continua num lugar só.
 */
public record UsernameAvailabilityDTO(
        boolean available,
        /** Nulo quando está livre; a frase do problema quando não está. */
        String message
) {
}
