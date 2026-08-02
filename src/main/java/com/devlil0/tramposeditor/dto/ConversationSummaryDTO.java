package com.devlil0.tramposeditor.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Linha da caixa de entrada. Traz o <b>outro lado</b> já resolvido — quem olha quer
 * saber com quem está falando, não qual papel ocupa.
 *
 * @param counterpartSlug slug do perfil do outro lado quando ele é editor; nulo quando
 *                        o outro lado é o cliente, que não tem página pública.
 * @param jobTitle        título do pedido que originou a conversa, ou nulo se ela
 *                        nasceu do perfil na vitrine.
 */
public record ConversationSummaryDTO(
        UUID id,
        String counterpartDisplayName,
        String counterpartSlug,
        String jobTitle,
        String lastMessagePreview,
        boolean unread,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt
) {}
