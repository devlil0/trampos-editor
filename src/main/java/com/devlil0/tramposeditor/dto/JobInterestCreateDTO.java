package com.devlil0.tramposeditor.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

/** O editor só manda uma mensagem — preço e prazo se combinam no contato. */
public record JobInterestCreateDTO(

        @NotBlank
        @Length(max = 500)
        String message
) {
}
