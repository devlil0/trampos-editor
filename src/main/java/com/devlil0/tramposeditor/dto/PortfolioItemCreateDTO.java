package com.devlil0.tramposeditor.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

/**
 * O provedor (YouTube/Vimeo) não é enviado — é inferido da URL no service.
 */
public record PortfolioItemCreateDTO(

        @NotBlank
        @Length(max = 120)
        String title,

        @NotBlank
        @URL
        @Length(max = 500)
        String videoUrl
) {
}
