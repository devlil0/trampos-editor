package com.devlil0.tramposeditor.dto;

/**
 * Resposta do passo 1 do cadastro (V18): o código saiu, a conta ainda não existe.
 * <p>
 * Não devolve sessão — é essa ausência que separa esta feature de "criar conta e verificar
 * depois". Enquanto o código não voltar, não há conta e não há token.
 *
 * @param email      o endereço normalizado, pra tela do código dizer pra onde ele foi
 * @param expiresIn  segundos de validade, pra interface poder contar sem repetir a regra
 */
public record SignupStartedDTO(
        String email,
        int expiresIn
) {
}
