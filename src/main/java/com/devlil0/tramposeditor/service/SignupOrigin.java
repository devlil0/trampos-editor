package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.dto.SignupOriginDTO;

/**
 * Aplica a origem da primeira visita numa conta (ESBOCO §3.1, V16).
 * <p>
 * Existe como classe separada porque os dois caminhos de cadastro precisam dela e nenhum
 * dos dois é dono: o {@link AuthService} grava no cadastro por senha, o
 * {@link SocialLoginService} grava na troca do código. Deixar a regra num deles faria o
 * outro importar um serviço inteiro por causa de três campos.
 */
final class SignupOrigin {

    private static final int MAX_REFERRER = 255;
    private static final int MAX_UTM_SOURCE = 120;
    private static final int MAX_LANDING = 255;

    private SignupOrigin(){
    }

    /**
     * Grava a origem, se houver alguma. Nulo não faz nada — a maioria dos cadastros vai
     * chegar sem atribuição e isso é normal, não falha.
     */
    static void apply(UserEntity user, SignupOriginDTO origin){
        if (origin == null) return;

        user.recordSignupOrigin(
                clean(origin.referrer(), MAX_REFERRER),
                clean(origin.utmSource(), MAX_UTM_SOURCE),
                clean(origin.landing(), MAX_LANDING));
    }

    /**
     * Trunca no tamanho da coluna em vez de recusar. Um {@code utm_source} absurdo é um
     * dado ruim; recusar por causa dele seria perder a conta — e conta vale mais que
     * atribuição.
     */
    private static String clean(String valor, int max){
        if (valor == null) return null;

        String limpo = valor.trim();
        if (limpo.isEmpty()) return null;

        return limpo.length() <= max ? limpo : limpo.substring(0, max);
    }
}
