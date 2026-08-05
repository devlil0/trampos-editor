package com.devlil0.tramposeditor.dto;

import jakarta.validation.constraints.NotBlank;

/** O token que veio no link de confirmação de e-mail. */
public record EmailVerificationConfirmDTO(

        @NotBlank
        String token
) {
}
