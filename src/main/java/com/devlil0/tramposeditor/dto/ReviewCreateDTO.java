package com.devlil0.tramposeditor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ReviewCreateDTO(
        @Min(value = 1, message = "A nota vai de 1 a 5")
        @Max(value = 5, message = "A nota vai de 1 a 5")
        short rating,

        // Sem @NotBlank de propósito: quem não quer escrever dá a nota e pronto.
        // Exigir texto faria a avaliação parar de acontecer, e a nota é o que importa.
        @Size(max = 1000, message = "O comentário passa de 1000 caracteres")
        String comment
) {}
