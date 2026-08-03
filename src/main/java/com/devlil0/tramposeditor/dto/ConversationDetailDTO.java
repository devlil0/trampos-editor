package com.devlil0.tramposeditor.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** A conversa aberta: quem é o outro lado, de onde ela veio e o histórico. */
public record ConversationDetailDTO(
        UUID id,
        String counterpartDisplayName,
        String counterpartSlug,
        UUID jobRequestId,
        String jobTitle,
        /**
         * Quem está lendo é o lado que contrata. A tela usa isso pra decidir quem pode
         * registrar e encerrar o trampo (ESBOCO §7 item 4) — o servidor não devolve
         * botão, devolve o fato.
         */
        boolean iAmClient,
        List<MessageDTO> messages,
        LocalDateTime createdAt
) {}
