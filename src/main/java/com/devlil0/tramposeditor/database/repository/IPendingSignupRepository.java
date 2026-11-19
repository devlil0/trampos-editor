package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.PendingSignupEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IPendingSignupRepository extends JpaRepository<PendingSignupEntity, UUID> {

    Optional<PendingSignupEntity> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PendingSignupEntity> findLockedByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PendingSignupEntity> findLockedById(UUID id);

    @Query("select p.username from PendingSignupEntity p where p.email = :email")
    Optional<String> findUsernameByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from PendingSignupEntity p
            where p.email = :email or (p.username = :username and p.expiresAt <= :now)
            order by p.id
            """)
    List<PendingSignupEntity> findLockedForStart(String email, String username, LocalDateTime now);

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
    @Query(value = """
            delete from pending_signups
            where id in (
                select id from pending_signups
                where expires_at <= :agora
                order by expires_at, id limit 100 for update skip locked
            )
            """, nativeQuery = true)
    int deleteExpired(LocalDateTime agora);
}
