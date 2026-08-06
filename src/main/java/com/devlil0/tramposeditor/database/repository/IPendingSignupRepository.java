package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.PendingSignupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IPendingSignupRepository extends JpaRepository<PendingSignupEntity, java.util.UUID> {

    Optional<PendingSignupEntity> findByEmail(String email);

    boolean existsByUsername(String username);

    void deleteByEmail(String email);

    /**
     * Limpa o que venceu.
     * <p>
     * Chamado no começo de cada novo cadastro em vez de por agendador: o volume é baixo, a
     * consulta é indexada por {@code expires_at}, e um {@code @Scheduled} exigiria pensar em
     * instância dupla na VPS pra apagar uma dúzia de linhas. Se o cadastro parar de
     * acontecer, não há lixo novo pra limpar — o problema se resolve por não existir.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PendingSignupEntity p where p.expiresAt < :agora")
    int deleteExpired(LocalDateTime agora);
}
