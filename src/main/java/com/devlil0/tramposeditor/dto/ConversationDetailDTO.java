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
        List<MessageDTO> messages,
        LocalDateTime createdAt
) {}
