package com.devlil0.tramposeditor.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialExchangeDTO(

        @NotBlank
        String code
) {
}
