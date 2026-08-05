package com.devlil0.tramposeditor.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

/**
 * Escolha ou troca do username (PUT /v1/users/me/username).
 *
 * O formato não é validado por anotação de propósito: quem tem a regra é o
 * {@code UsernameService}, e ele devolve a frase certa pro problema certo ("esse nome é
 * reservado", "já está em uso"). Duplicar aqui só produziria a mensagem genérica do Bean
 * Validation, em inglês, competindo com a boa. O 60 é barreira contra texto absurdo — o
 * que sobra depois de normalizar tem que caber em 30.
 */
public record UsernameUpdateDTO(

        @NotBlank
        @Length(max = 60)
        String username
) {
}
