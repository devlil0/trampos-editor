package com.devlil0.tramposeditor.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Segredos de uso único — o link de redefinição de senha e o código do login social.
 * <p>
 * Os dois seguem a mesma regra: o valor que sai daqui vai pro usuário e <b>só</b> pro
 * usuário; o banco guarda o hash. Vazar a tabela não pode virar vazar conta.
 */
final class TokenSecrets {

    /** 32 bytes de aleatório real — o segredo vale tanto quanto a senha enquanto vive. */
    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenSecrets(){
    }

    /** URL-safe: estes valores viajam em query string. */
    static String generate(){
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 em hexadecimal — 64 caracteres, o tamanho das colunas de hash. */
    static String hash(String token){
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 é obrigatório em toda JVM; se sumiu, o ambiente está quebrado.
            throw new IllegalStateException("SHA-256 indisponível nesta JVM", e);
        }
    }
}
