package com.devlil0.tramposeditor.service;

import org.springframework.stereotype.Component;

/**
 * De onde sai o código de confirmação do cadastro (V18).
 * <p>
 * Existe como bean, e não como chamada estática, por um motivo só: <b>teste</b>. O código
 * vai por e-mail e o banco guarda o hash, então não há como uma suíte recuperá-lo — e sem
 * poder recuperá-lo, nenhum teste conseguiria criar uma conta, o que derrubaria todas as
 * dez suítes que dependem de ter usuário.
 * <p>
 * Trocar isto por um valor fixo no contexto de teste resolve, e é uma substituição honesta:
 * o que as suítes exercitam é o fluxo de dois passos, não a aleatoriedade — que é
 * responsabilidade do {@link TokenSecrets} e não muda por causa de quem o chama.
 * <p>
 * Não é final e {@code generate} não é estático justamente pra permitir essa troca.
 */
@Component
public class SignupCodes {

    public String generate(){
        return TokenSecrets.generateCode();
    }

    String generateDifferentFrom(String previousHash) {
        String code = generate();
        while (previousHash != null && TokenSecrets.hash(code).equals(previousHash)) {
            code = TokenSecrets.generateCode();
        }
        return code;
    }
}
