package com.devlil0.tramposeditor.dto;

import com.devlil0.tramposeditor.enums.DealStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um trampo no histórico, do ponto de vista de quem está lendo.
 * <p>
 * Traz o slug do outro lado porque é o que faz o "chamar de novo" do ESBOCO §3.2
 * funcionar em um clique — repetição de fornecedor é o gatilho mais barato do laço da
 * demanda, e ele morre se a pessoa tiver que procurar o editor de novo na vitrine.
 */
public record DealDTO(
        UUID id,
        UUID conversationId,
        String title,
        DealStatus status,
        /** Nome de quem está do outro lado. */
        String counterpartName,
        /** Slug do perfil do outro lado, quando ele é editor — null quando é o cliente. */
        String counterpartSlug,
        /** Sou o cliente deste trampo? Decide o que a tela deixa fazer. */
        boolean iAmClient,
        /** Já avaliei? A tela some com o formulário em vez de deixar tentar e falhar. */
        boolean reviewedByMe,
        /** A avaliação que eu recebi neste trampo, quando o outro lado já escreveu. */
        ReviewDTO reviewOfMe,
        LocalDateTime createdAt,
        LocalDateTime closedAt
) {
}
