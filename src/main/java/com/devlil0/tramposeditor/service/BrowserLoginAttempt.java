package com.devlil0.tramposeditor.service;

import java.time.LocalDateTime;

/** O backend recebe somente o hash da prova até a troca do código. */
public record BrowserLoginAttempt(String challenge, LocalDateTime expiresAt) {
    public static final String PROOF_FORMAT = "[0-9a-f]{64}";

    public static String challengeOf(String proof) {
        return TokenSecrets.hash(proof);
    }

    public boolean isValidAt(LocalDateTime now) {
        return challenge != null && challenge.matches(PROOF_FORMAT)
                && expiresAt != null && expiresAt.isAfter(now);
    }
}
