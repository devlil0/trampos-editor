package com.devlil0.tramposeditor.dto;

import com.devlil0.tramposeditor.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.Length;

import java.util.Set;

public record RegisterRequestDTO(

        @NotBlank
        @Email
        @Length(max = 180)
        String email,

        @NotBlank
        @Length(min = 8, max = 72)
        String password,

        @NotBlank
        @Length(max = 80)
        String displayName,

        /**
         * O endereço da pessoa em {@code /{username}}, escolhido por ela.
         * <p>
         * O formato não é validado aqui: quem tem a regra é o {@code UsernameService}, e é
         * dele que sai a frase que a pessoa lê. Ver {@link UsernameUpdateDTO}.
         */
        @NotBlank
        @Length(max = 60)
        String username,

        @NotEmpty
        Set<UserRole> roles,

        /**
         * De onde a pessoa veio na primeira visita. Opcional — quem chega direto não tem
         * origem, e o campo nunca deve poder recusar um cadastro. Ver {@link SignupOriginDTO}.
         */
        SignupOriginDTO origin
) {
}
