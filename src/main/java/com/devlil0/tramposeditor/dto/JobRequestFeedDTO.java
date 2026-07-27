package com.devlil0.tramposeditor.dto;

import com.devlil0.tramposeditor.enums.EditorNiche;
import com.devlil0.tramposeditor.enums.JobInterestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pedido como o <b>editor</b> vê na lista.
 * <p>
 * Nunca traz o contato do cliente por padrão: `clientEmail` só vem preenchido quando o
 * interesse deste editor foi aceito. É o que impede a lista de virar raspagem de e-mails.
 */
public record JobRequestFeedDTO(
        UUID id,
        String title,
        String description,
        EditorNiche niche,
        Integer budgetCents,
        Integer deadlineDays,
        String referenceUrl,
        String clientDisplayName,
        int interestCount,
        /** Status do interesse de quem está lendo — null se ainda não se candidatou. */
        JobInterestStatus myInterestStatus,
        String clientEmail,
        LocalDateTime createdAt
) {
}
