package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.JobRequestEntity;
import com.devlil0.tramposeditor.enums.EditorNiche;
import com.devlil0.tramposeditor.enums.JobRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IJobRequestRepository extends JpaRepository<JobRequestEntity, UUID> {

    List<JobRequestEntity> findByClientIdOrderByCreatedAtDesc(UUID clientId);

    /**
     * Feed do editor: só pedidos abertos, mais recentes primeiro.
     * <p>
     * O {@code ClientIdNot} exclui o pedido de quem está lendo <b>na consulta</b>, não
     * depois. Filtrar em memória depois de paginar devolveria páginas de tamanho
     * irregular e um {@code totalElements} mentiroso.
     */
    Page<JobRequestEntity> findByStatusAndClientIdNotOrderByCreatedAtDesc(JobRequestStatus status,
                                                                          UUID clientId,
                                                                          Pageable pageable);

    /**
     * Mesmo feed, filtrado por plataforma. Dois métodos em vez de um com parâmetro
     * opcional de propósito: parâmetro nulo em JPQL já custou caro aqui (§11.5).
     */
    Page<JobRequestEntity> findByStatusAndNicheAndClientIdNotOrderByCreatedAtDesc(JobRequestStatus status,
                                                                                  EditorNiche niche,
                                                                                  UUID clientId,
                                                                                  Pageable pageable);
}
