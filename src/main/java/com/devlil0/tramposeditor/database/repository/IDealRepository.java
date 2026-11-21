package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.DealEntity;
import com.devlil0.tramposeditor.enums.DealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IDealRepository extends JpaRepository<DealEntity, UUID> {

    /** Estado terminal e avaliações do mesmo trampo mudam sob o mesmo bloqueio. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DealEntity d where d.id = :id")
    Optional<DealEntity> findForUpdateById(UUID id);

    /** O aceite antigo não reabre a contratação, mesmo depois de encerrada. */
    Optional<DealEntity> findFirstByJobRequestIdOrderByCreatedAtAsc(UUID jobRequestId);

    /** O histórico: meus trampos pelos dois lados, mais recente primeiro. */
    @Query("""
            select d from DealEntity d
            where d.client.id = :userId or d.editor.id = :userId
            order by d.createdAt desc
            """)
    List<DealEntity> findAllForUser(UUID userId);

    /** Garante o "um trampo aberto por conversa" antes de o índice parcial reclamar. */
    Optional<DealEntity> findByConversationIdAndStatus(UUID conversationId, DealStatus status);

    @Query(value = """
            select exists (
                select 1 from deal_mediations m
                where m.deal_id = :dealId and m.closed_at is null
            )
            """, nativeQuery = true)
    boolean hasActiveMediationForDeal(UUID dealId);

    @Query(value = """
            select exists (
                select 1 from deals d join deal_mediations m on m.deal_id = d.id
                where (d.client_user_id = :userId or d.editor_user_id = :userId)
                  and m.closed_at is null
            )
            """, nativeQuery = true)
    boolean hasActiveMediation(UUID userId);

    /**
     * Quantos trampos a pessoa já fechou, pelos dois lados — o número que a página pública
     * mostra. Só conta o que terminou: trampo aberto ainda não prova nada.
     */
    @Query("""
            select count(d) from DealEntity d
            where (d.client.id = :userId or d.editor.id = :userId)
              and d.status = :status
            """)
    long countClosedForUser(UUID userId, DealStatus status);
}
