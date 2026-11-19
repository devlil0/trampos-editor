package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.PendingSignupEntity;
import com.devlil0.tramposeditor.database.repository.IPendingSignupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Serializa as escritas no contador de uma pendência, inclusive quando chamadas sem transação externa. */
@Service
@RequiredArgsConstructor
public class SignupAttempts {

    private final IPendingSignupRepository pendingRepository;

    /**
     * Conta o erro e, no limite, queima o cadastro inteiro.
     *
     * @return true se o cadastro foi descartado
     */
    @Transactional
    public boolean registrarErro(UUID pendingId, short maxTentativas){
        PendingSignupEntity pendente = pendingRepository.findLockedById(pendingId).orElse(null);
        if (pendente == null) return true;

        pendente.setAttempts((short) (pendente.getAttempts() + 1));

        if (pendente.getAttempts() >= maxTentativas) {
            // Queima o cadastro, não só o código: quem errou cinco vezes ou está chutando,
            // ou digitou o e-mail errado e nunca vai receber nada.
            pendingRepository.delete(pendente);
            return true;
        }

        pendingRepository.save(pendente);
        return false;
    }

}
