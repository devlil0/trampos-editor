package com.devlil0.tramposeditor.dto;

import org.hibernate.validator.constraints.Length;

/**
 * Edição dos dados da própria conta (PATCH /v1/users/me).
 *
 * O formato do username não é validado aqui de propósito: aceitamos "@fulano" e
 * "instagram.com/fulano" coladas do navegador, e é o service que reduz ao username
 * antes de checar. O limite de 60 é só uma barreira contra texto absurdo — a URL
 * inteira cabe, e o que sobra depois da normalização tem que caber em 30.
 */
public record UserUpdateDTO(

        @Length(max = 60)
        String instagram
) {
}
