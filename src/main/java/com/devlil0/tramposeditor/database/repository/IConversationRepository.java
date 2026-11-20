package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IConversationRepository extends JpaRepository<ConversationEntity, UUID> {

    /** Serializa o registro de novos trabalhos antes de consultar o OPEN existente. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConversationEntity c where c.id = :id")
    Optional<ConversationEntity> findForUpdateById(UUID id);

    /**
     * A caixa de entrada não separa os papéis: a pessoa vê tudo em que participa, sem
     * ter que lembrar se naquela conversa ela contratava ou editava (FUNDACAO §11.9,
     * a conta é uma só).
     * <p>
     * {@code lastMessageAt} nulo (conversa aberta e ainda sem mensagem) vai pro fim
     * com {@code NULLS LAST} — sem isso o Postgres joga nulo pro topo em DESC.
     */
    @Query("""
        select c from ConversationEntity c
        where c.client.id = :userId or c.editor.id = :userId
        order by c.lastMessageAt desc nulls last, c.createdAt desc
        """)
    List<ConversationEntity> findAllForUser(UUID userId);

    /** Porta da vitrine: a conversa direta é única por par, sem pedido associado. */
    Optional<ConversationEntity> findByClientIdAndEditorIdAndJobRequestIsNull(UUID clientId, UUID editorId);

    /** Porta do pedido: uma conversa por par dentro de cada pedido. */
    Optional<ConversationEntity> findByClientIdAndEditorIdAndJobRequestId(UUID clientId, UUID editorId, UUID jobRequestId);
}
