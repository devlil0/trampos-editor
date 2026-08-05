package com.devlil0.tramposeditor.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialExchangeDTO(

        @NotBlank
        String code,

        /**
         * De onde a pessoa veio na primeira visita. Chega aqui, e não no callback do
         * provedor, porque o cookie que a guarda é do frontend — e o frontend só entra em
         * cena nesta troca. Só é aplicada quando a conta acabou de nascer (V16).
         */
        SignupOriginDTO origin
) {
}
