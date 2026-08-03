package com.devlil0.tramposeditor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Abrir um trampo dentro de uma conversa que veio da vitrine — o caminho que não tinha
 * fecho nenhum antes (ESBOCO §3.2). Quem veio do feed já ganha o trampo no aceite.
 */
public record DealCreateDTO(
        @NotBlank(message = "Diga do que é o trampo")
        @Size(max = 140, message = "O título passa de 140 caracteres")
        String title
) {}
