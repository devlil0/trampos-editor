package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.PendingSignupEntity;
import com.devlil0.tramposeditor.database.repository.IPendingSignupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * A contabilidade de erro do código de cadastro — <b>em transação própria</b>.
 * <p>
 * Existe por causa de um bug que só um teste pegou. A primeira versão contava a tentativa e
 * lançava a exceção do código inválido dentro do mesmo {@code @Transactional}: a exceção
 * marcava a transação pra rollback, e o incremento morria junto. O contador nunca passava de
 * zero, e o limite de cinco tentativas — que é o que impede seis dígitos de serem chutados —
 * <b>não existia na prática</b>. O teste flagrou porque, depois de cinco chutes errados, o
 * código certo continuava criando a conta.
 * <p>
 * {@code REQUIRES_NEW} é o conserto: a escrita commita numa transação separada, antes de
 * quem chamou desfazer a dele. É bean à parte porque chamada entre métodos do mesmo objeto
 * não passa pelo proxy do Spring, e sem o proxy a anotação não faz nada — que seria o mesmo
 * bug de novo, só que mais difícil de ver.
 * <p>
 * Não trava contra a transação de fora: lá o pendente só foi lido, e no {@code READ
 * COMMITTED} do Postgres um SELECT simples não segura a linha.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignupAttempts {

    private final IPendingSignupRepository pendingRepository;

    /**
     * Conta o erro e, no limite, queima o cadastro inteiro.
     *
     * @return true se o cadastro foi descartado
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean registrarErro(UUID pendingId, short maxTentativas){
        PendingSignupEntity pendente = pendingRepository.findById(pendingId).orElse(null);
        if (pendente == null) return true;

        pendente.setAttempts((short) (pendente.getAttempts() + 1));

        if (pendente.getAttempts() >= maxTentativas) {
            // Queima o cadastro, não só o código: quem errou cinco vezes ou está chutando,
            // ou digitou o e-mail errado e nunca vai receber nada.
            pendingRepository.delete(pendente);
            log.warn("Cadastro pendente descartado por excesso de tentativas: {}",
                    pendente.getEmail());
            return true;
        }

        pendingRepository.save(pendente);
        return false;
    }

    /** Descarta um pendente (vencido, ou cujo e-mail virou conta por outro caminho). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void descartar(UUID pendingId){
        pendingRepository.deleteById(pendingId);
    }
}
