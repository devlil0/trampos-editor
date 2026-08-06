package com.devlil0.tramposeditor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Passo 2 do cadastro (V18): o código de volta.
 * <p>
 * O e-mail viaja junto porque não há sessão nesta altura — a conta ainda não existe, então
 * não há de onde inferir de quem é o código.
 */
public record SignupConfirmDTO(

        @NotBlank
        @Email
        String email,

        /**
         * Exatamente seis dígitos. O {@code Pattern} aqui é conveniência de mensagem, não
         * segurança: quem manda lixo receberia "código inválido" do serviço de qualquer
         * forma, mas gastando uma consulta no banco e uma tentativa do contador.
         */
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "O código tem seis dígitos")
        String code
) {
}
