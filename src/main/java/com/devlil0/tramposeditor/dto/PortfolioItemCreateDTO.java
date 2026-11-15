package com.devlil0.tramposeditor.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

/**
 * O service valida a forma da URL e deriva dela o provedor e o link canônico.
 */
public record PortfolioItemCreateDTO(

        @NotBlank
        @Length(max = 120)
        String title,

        @NotBlank
        @Length(max = 500)
        String videoUrl
) {
}
