package com.devlil0.tramposeditor.dto;

import com.devlil0.tramposeditor.enums.UserRole;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String email,
        String displayName,
        /** Identidade no produto e endereço da pessoa em {@code /{username}}. */
        String username,
        /**
         * Falso enquanto a pessoa não escolheu o próprio nome — só acontece em conta criada
         * pelo provedor social. É o que manda o frontend interromper a navegação e pedir a
         * escolha.
         */
        boolean usernameChosen,
        /** Última troca; nulo se nunca trocou. Alimenta o cooldown mostrado na tela. */
        LocalDateTime usernameChangedAt,
        /** Versão da foto de perfil; nulo quando não há foto. Entra na URL da imagem. */
        UUID avatarVersion,
        /** Username do Instagram, sem "@". Null enquanto a pessoa não informar. */
        String instagram,
        Set<UserRole> roles,
        boolean active,
        LocalDateTime createdAt
) {
}
