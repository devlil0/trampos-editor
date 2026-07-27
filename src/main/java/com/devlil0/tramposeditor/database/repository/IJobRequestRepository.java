package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.JobRequestEntity;
import com.devlil0.tramposeditor.enums.EditorNiche;
import com.devlil0.tramposeditor.enums.JobRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IJobRequestRepository extends JpaRepository<JobRequestEntity, UUID> {

    List<JobRequestEntity> findByClientIdOrderByCreatedAtDesc(UUID clientId);

    /** Feed do editor: só pedidos abertos, mais recentes primeiro. */
    List<JobRequestEntity> findByStatusOrderByCreatedAtDesc(JobRequestStatus status);

    /**
     * Mesmo feed, filtrado por plataforma. Dois métodos em vez de um com parâmetro
     * opcional de propósito: parâmetro nulo em JPQL já custou caro aqui (§11.5).
     */
    List<JobRequestEntity> findByStatusAndNicheOrderByCreatedAtDesc(JobRequestStatus status,
                                                                    EditorNiche niche);
}
