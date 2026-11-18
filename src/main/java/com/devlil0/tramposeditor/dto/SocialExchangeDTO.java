package com.devlil0.tramposeditor.dto;

import com.devlil0.tramposeditor.service.BrowserLoginAttempt;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SocialExchangeDTO(

        @NotBlank
        String code,
        @NotBlank
        @Pattern(regexp = BrowserLoginAttempt.PROOF_FORMAT)
        String browserProof,

        /**
         * De onde a pessoa veio na primeira visita. Chega aqui, e não no callback do
         * provedor, porque o cookie que a guarda é do frontend — e o frontend só entra em
         * cena nesta troca. Só é aplicada quando a conta acabou de nascer (V16).
         */
        SignupOriginDTO origin
) {
}
