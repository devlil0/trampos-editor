package com.devlil0.tramposeditor.dto;

import com.devlil0.tramposeditor.enums.JobInterestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interesse de um editor, como o <b>dono do pedido</b> vê.
 * <p>
 * O e-mail do editor só é preenchido depois do aceite — antes disso vem null.
 */
public record JobInterestDTO(
        UUID id,
        String editorDisplayName,
        String editorSlug,
        String message,
        JobInterestStatus status,
        String editorEmail,
        LocalDateTime createdAt
) {
}
