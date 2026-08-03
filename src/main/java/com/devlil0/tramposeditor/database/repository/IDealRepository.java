package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.DealEntity;
import com.devlil0.tramposeditor.enums.DealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IDealRepository extends JpaRepository<DealEntity, UUID> {

    /** O histórico: meus trampos pelos dois lados, mais recente primeiro. */
    @Query("""
            select d from DealEntity d
            where d.client.id = :userId or d.editor.id = :userId
            order by d.createdAt desc
            """)
    List<DealEntity> findAllForUser(UUID userId);

    /** Garante o "um trampo aberto por conversa" antes de o índice parcial reclamar. */
    Optional<DealEntity> findByConversationIdAndStatus(UUID conversationId, DealStatus status);
}
