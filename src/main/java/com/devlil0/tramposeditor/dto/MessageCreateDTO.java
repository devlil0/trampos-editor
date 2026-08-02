package com.devlil0.tramposeditor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageCreateDTO(
        @NotBlank(message = "A mensagem não pode ficar vazia")
        @Size(max = 2000, message = "A mensagem passa de 2000 caracteres")
        String body
) {}
