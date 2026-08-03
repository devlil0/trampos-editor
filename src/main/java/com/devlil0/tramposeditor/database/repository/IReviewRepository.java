package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.ReviewEntity;
import com.devlil0.tramposeditor.dto.RatingAggregateDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface IReviewRepository extends JpaRepository<ReviewEntity, UUID> {

    /**
     * Média e contagem de quem foi avaliado, para recalcular o cache do perfil.
     * <p>
     * Recalcular do zero em vez de somar incrementalmente é de propósito: é uma consulta
     * barata (índice em `target_user_id`) e não acumula erro se uma review for apagada
     * pela moderação (§2.2).
     */
    @Query("""
            select new com.devlil0.tramposeditor.dto.RatingAggregateDTO(
                coalesce(avg(r.rating), 0.0), count(r))
            from ReviewEntity r
            where r.target.id = :targetUserId
            """)
    RatingAggregateDTO aggregateFor(UUID targetUserId);
}
