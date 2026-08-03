package com.devlil0.tramposeditor.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record PasswordResetConfirmDTO(

        @NotBlank
        String token,

        // Mesma regra do cadastro: trocar a senha não pode ser a porta dos fundos
        // pra uma senha mais fraca do que a que o registro exige.
        @NotBlank
        @Length(min = 8, max = 72)
        String password
) {
}
